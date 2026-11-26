package com.exemplo.pomodoro.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.exp

/**
 * O deslocamento do Fundo no Vidro com parallax (§7), de -1 a 1 em cada eixo, já no sentido contrário à inclinação.
 * Recebe só a mudança de ângulo entre duas leituras do sensor, no referencial da tela: sem ângulo absoluto, sem trava de cardã.
 * [limiteRad] é a inclinação que leva o Fundo ao extremo.
 * [neutroS]: em quanto tempo a posição neutra alcança a postura atual (95%). Segurar o telefone inclinado
 * não deixa o Fundo preso na borda. Os dois são botões de calibração: confira com a mão, num aparelho real.
 */
class FiltroParallax(private val limiteRad: Float, private val neutroS: Float) {
    private var ax = 0f
    private var ay = 0f
    val x get() = -ax / limiteRad
    val y get() = -ay / limiteRad

    /** [giroX]: giro que desloca na horizontal; [giroY]: na vertical. Em radianos, desde a leitura anterior. */
    fun somar(giroX: Float, giroY: Float, dtS: Float) {
        // Integrador com vazamento: um passa-altas no ângulo. O limite vem depois da soma, então não há zona morta.
        val vaza = exp(-3f * dtS.coerceIn(0f, 1f) / neutroS)
        ax = (ax * vaza + giroX).coerceIn(-limiteRad, limiteRad)
        ay = (ay * vaza + giroY).coerceIn(-limiteRad, limiteRad)
    }
}

/** Botões de calibração do parallax (§7). ~20° de inclinação levam o Fundo ao extremo. */
private const val LIMITE_RAD = 0.35f
private const val NEUTRO_S = 3f
internal val PARALLAX_MAX = 12.dp

/** O alvo do parallax, de -1 a 1. O Vidro com parallax escreve; o Fundo da raiz segue com SpringAmbiente. */
class Parallax {
    var alvo by mutableStateOf(Offset.Zero)
}

/** Só a raiz (para o Fundo) e o Timer proveem. Na prévia, nas miniaturas e nas outras telas, o sensor nem liga. */
val LocalParallax = staticCompositionLocalOf<Parallax?> { null }

/**
 * O deslocamento do Fundo, seguindo o alvo com SpringAmbiente: o Fundo tem peso, e o spring carrega a velocidade
 * de uma leitura para a seguinte. X e Y são springs independentes.
 */
@Composable
internal fun rememberParallaxDoFundo(): Animatable<Offset, AnimationVector2D> {
    val parallax = LocalParallax.current
    val deslocamento = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    if (parallax != null) LaunchedEffect(parallax) {
        snapshotFlow { parallax.alvo }.collectLatest { deslocamento.animateTo(it, spring(dampingRatio = 1f, stiffness = 50f)) }
    }
    return deslocamento
}

/**
 * Liga o sensor de rotação enquanto o Timer está em ON_RESUME. Desliga em ON_PAUSE, ao sair do Timer, ao trocar de
 * Estilo e com o movimento reduzido; aí o Fundo volta ao centro. A cada retomada, a postura atual vira o centro.
 */
@Composable
private fun SensorParallax() {
    val parallax = LocalParallax.current ?: return
    if (rememberMovimentoReduzido()) return
    val ctx = LocalContext.current
    val view = LocalView.current
    LifecycleResumeEffect(parallax) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        // Sem magnetômetro: não salta perto de metal. O ROTATION_VECTOR é a reserva.
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val filtro = FiltroParallax(LIMITE_RAD, NEUTRO_S)
        // Os eixos do aparelho viram os da tela, também em paisagem.
        val (eixoX, eixoY) = when (view.display?.rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        val bruta = FloatArray(9)
        var atual = FloatArray(9)
        var anterior = FloatArray(9)
        val giro = FloatArray(3)
        var tAnterior = 0L
        val ouvinte = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(bruta, e.values)
                SensorManager.remapCoordinateSystem(bruta, eixoX, eixoY, atual)
                if (tAnterior != 0L) {
                    // giro[1]: em volta do eixo X da tela (desloca na vertical); giro[2]: em volta do Y (na horizontal).
                    SensorManager.getAngleChange(giro, atual, anterior)
                    // ponytail: os sinais seguem a doc do getAngleChange; confira o sentido com Virtual sensors no emulador.
                    filtro.somar(giroX = giro[2], giroY = giro[1], dtS = (e.timestamp - tAnterior) / 1e9f)
                    parallax.alvo = Offset(filtro.x, filtro.y)
                }
                tAnterior = e.timestamp
                anterior = atual.also { atual = anterior }
            }

            override fun onAccuracyChanged(s: Sensor?, precisao: Int) {}
        }
        if (sm != null && sensor != null) sm.registerListener(ouvinte, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose {
            sm?.unregisterListener(ouvinte)
            parallax.alvo = Offset.Zero
        }
    }
}

/** A memória da troca de Fase do traço. Só os efeitos mexem aqui, em ordem. */
private class MemoriaTraco(var tipo: TipoFase, var progresso: Float) {
    var job: Job? = null
    /** O reenchimento ou a volta com spring: um tique de segundo não o interrompe. */
    var especial = false
}

private val CANTO_CARTAO = 32.dp
internal const val PROPORCAO_CARTAO = 4f / 5f

/**
 * Vidro com parallax (§7): um cartão de vidro 4:5 com os números grandes, que a tela Timer desenha por cima.
 * O progresso é um traço de 2 dp na Cor de destaque ao longo do perímetro, a partir do topo central e no sentido
 * horário; ele esvazia com o tempo restante, sobre o trilho da `bordaVidro`. Sem respiração: o movimento vem da mão.
 * Pausado: o blur sobe para 36 dp (no [CartaoVidro]) e o traço cai para 50%. O parallax continua.
 */
@Composable
internal fun VidroComParallax(progresso: Float, tipo: TipoFase, destaque: Color, rodando: Boolean, modifier: Modifier) {
    SensorParallax()
    val reduzido = rememberMovimentoReduzido()
    val escopo = rememberCoroutineScope()
    val cheio = remember { Animatable(1f - progresso) } // aparece direto no progresso real (§4, regra 6)
    // A cor segue o 5.6 (SpringFase), em OkLCh. Reduzido: a cor é mantida, num crossfade de 300 ms (§9).
    val cor by animarCor(destaque, if (reduzido) tween(300, easing = EaseOut) else spring(1f, 40f))
    val memoria = remember { MemoriaTraco(tipo, progresso) }
    val alfa by animateFloatAsState(if (rodando) 1f else 0.5f, tween(DUR_MEDIA, easing = EaseOut), label = "traco")

    LaunchedEffect(tipo, progresso) {
        val novaFase = tipo != memoria.tipo
        val fim = novaFase && memoria.progresso > 0.97f // a Fase acabou sozinha; senão foi Pular
        memoria.tipo = tipo
        memoria.progresso = progresso
        val alvo = 1f - progresso
        val tique = !novaFase && alvo <= cheio.value
        if (tique && memoria.especial && memoria.job?.isActive == true) return@LaunchedEffect
        memoria.job?.cancel()
        memoria.especial = !tique
        memoria.job = escopo.launch {
            when {
                // Reduzido: crossfade curto, sem overshoot (§9).
                reduzido && !tique -> comEscalaNormal(true) { cheio.animateTo(alvo, tween(300, easing = EaseOut)) }
                // Fim de Fase: o traço volta a encher com SpringFase.
                fim -> cheio.animateTo(alvo, spring(dampingRatio = 1f, stiffness = 40f))
                // Pular, Estender, outro Perfil: SpringUi, a partir do valor exibido (§5.4, §5.5).
                !tique -> cheio.animateTo(alvo, spring(dampingRatio = 1f, stiffness = 440f))
                // Tique de segundo: segue o tempo em linha reta até o próximo tique, sem degrau.
                else -> cheio.animateTo(alvo, tween(1000, easing = LinearEasing))
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        val cartao = Modifier.fillMaxHeight().aspectRatio(PROPORCAO_CARTAO)
        CartaoVidro(cartao, embacado = !rodando)
        Box(
            cartao.drawWithCache {
                val espessura = 2.dp.toPx()
                val perimetro = perimetroDoCartao(size.width, size.height, minOf(CANTO_CARTAO.toPx(), size.minDimension / 2f), espessura / 2f)
                val medida = PathMeasure().apply { setPath(perimetro, false) }
                val trecho = Path()
                val traco = Stroke(width = espessura, cap = StrokeCap.Round)
                onDrawBehind {
                    val total = medida.length
                    val resta = total * cheio.value.coerceIn(0f, 1f)
                    if (resta < 0.5f) return@onDrawBehind
                    trecho.reset()
                    // A parte vazia cresce do topo central no sentido horário, como um ponteiro.
                    medida.getSegment(total - resta, total, trecho, true)
                    drawPath(trecho, cor.copy(alpha = alfa), style = traco)
                }
            },
        )
    }
}

/** O contorno do cartão, do topo central no sentido horário, a [recuo] da borda (o centro do traço). */
private fun perimetroDoCartao(w: Float, h: Float, canto: Float, recuo: Float) = Path().apply {
    val r = canto - recuo
    val e = recuo
    val d = w - recuo
    val b = h - recuo
    moveTo(w / 2f, e)
    lineTo(d - r, e)
    arcTo(Rect(d - 2 * r, e, d, e + 2 * r), -90f, 90f, false)
    lineTo(d, b - r)
    arcTo(Rect(d - 2 * r, b - 2 * r, d, b), 0f, 90f, false)
    lineTo(e + r, b)
    arcTo(Rect(e, b - 2 * r, e + 2 * r, b), 90f, 90f, false)
    lineTo(e, e + r)
    arcTo(Rect(e, e, e + 2 * r, e + 2 * r), 180f, 90f, false)
    close()
}

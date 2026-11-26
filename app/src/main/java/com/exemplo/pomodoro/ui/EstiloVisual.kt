package com.exemplo.pomodoro.ui

import android.graphics.RuntimeShader
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.dados.EstiloVisual
import com.exemplo.pomodoro.dados.L_MIOLO
import com.exemplo.pomodoro.dados.misturarOklch
import com.exemplo.pomodoro.dados.tomArgb
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * O contrato comum dos Estilos visuais (§7). Todo Estilo recebe só isto:
 * - [progresso]: o tempo decorrido da Fase, de 0 a 1.
 * - [tipo]: o tipo da Fase. Na troca, o Estilo faz a própria transição de cor e de forma.
 * - [destaque]: a Cor de destaque do tipo atual, já resolvida (escolhida ou do Fundo).
 * - [rodando]: falso com a Fase pausada ou parada.
 * - [nucleo]: o raio do disco dos números, que precisa ficar escuro (§3). Zero na prévia, que não tem números.
 * O [modifier] dá a área: um quadrado de 0.84 × a largura no Timer.
 * Um Estilo novo é um valor a mais no enum e um ramo a mais no `when`.
 */
@Composable
fun EstiloVisualAnimado(
    estilo: EstiloVisual,
    progresso: Float,
    tipo: TipoFase,
    destaque: Color,
    rodando: Boolean,
    modifier: Modifier = Modifier,
    nucleo: Dp = 0.dp,
) {
    // Outro Estilo: o antigo sai e o novo entra depois, em sequência. Dois visuais ricos no mesmo lugar brigam (§5.7).
    AnimatedContent(
        targetState = estilo,
        transitionSpec = {
            (fadeIn(tween(300, 200, EaseOut)) + scaleIn(tween(300, 200, EaseOut), initialScale = 0.96f))
                .togetherWith(fadeOut(tween(200, easing = EaseOut)) + scaleOut(tween(200, easing = EaseOut), targetScale = 0.96f))
        },
        modifier = modifier,
        label = "estilo",
    ) { e ->
        Box(Modifier.fillMaxSize()) {
            when (e) {
                EstiloVisual.BLOB_COM_VIDRO -> {
                    CartaoVidro(Modifier.matchParentSize(), embacado = !rodando)
                    Blob(progresso, tipo, destaque, rodando, nucleo, Modifier.matchParentSize())
                }
                EstiloVisual.BLOB_PURO -> Blob(progresso, tipo, destaque, rodando, nucleo, Modifier.matchParentSize())
                EstiloVisual.ANEL_FLUIDO -> AnelFluido(progresso, tipo, destaque, rodando, Modifier.matchParentSize())
                EstiloVisual.VIDRO_COM_PARALLAX -> VidroComParallax(progresso, tipo, destaque, rodando, Modifier.matchParentSize())
            }
        }
    }
}

/** Respiração e ruído da borda de um tipo de Fase (tabela do §7). */
internal class Forma(
    val periodo: Float,
    val escala: Float,
    val ruido: Float,
    val frequencia: Float,
    val velocidade: Float,
)

private val FOCO = Forma(7f, 0.015f, 0.04f, 2.0f, 1.0f)
private val PAUSA_CURTA = Forma(9f, 0.03f, 0.055f, 1.0f, 0.7f)
private val PAUSA_LONGA = Forma(11f, 0.04f, 0.065f, 0.8f, 0.5f)

internal fun forma(tipo: TipoFase) = when (tipo) {
    TipoFase.FOCO -> FOCO
    TipoFase.PAUSA_CURTA -> PAUSA_CURTA
    TipoFase.PAUSA_LONGA -> PAUSA_LONGA
}

internal fun misturar(a: Forma, b: Forma, t: Float) = Forma(
    a.periodo + (b.periodo - a.periodo) * t,
    a.escala + (b.escala - a.escala) * t,
    a.ruido + (b.ruido - a.ruido) * t,
    a.frequencia + (b.frequencia - a.frequencia) * t,
    a.velocidade + (b.velocidade - a.velocidade) * t,
)

/**
 * Quanto da largura do Estilo os números podem ocupar (§2, ajuste do `timer`). No blob, eles cabem no miolo escuro
 * com o blob ainda encolhendo; no anel, por dentro do brilho da faixa; no Vidro com parallax, em 88% do cartão.
 */
internal fun larguraNumeros(estilo: EstiloVisual?) = when (estilo) {
    EstiloVisual.BLOB_COM_VIDRO, EstiloVisual.BLOB_PURO -> 0.46f
    EstiloVisual.ANEL_FLUIDO -> 0.62f
    EstiloVisual.VIDRO_COM_PARALLAX -> PROPORCAO_CARTAO * 0.88f
    null -> 1f
}

/** Raio do blob cheio sobre o lado da área do Estilo (§7). Também é o raio parado da splash. */
internal const val RAIO_BLOB = 0.44f

/** O aro claro só começa além desta fração do raio mais fundo do ruído: até ali, o miolo é escuro (§3). */
private const val Q_NUCLEO = 0.84f

/** A área cai linear com o tempo restante: o "cheio" é a raiz do restante, de 1 a 0 (§7). */
private fun cheioDe(progresso: Float) = sqrt((1f - progresso).coerceIn(0f, 1f))

/**
 * O raio do blob (§7): de [rMax] (cheio 1) a rMin (cheio 0), com a respiração por cima.
 * rMin é 0.35 × rMax, ou o piso que mantém o [nucleo] (os números) dentro do miolo escuro em qualquer ponto da borda
 * ondulada: `nucleo ≤ 0.84 × raio × (1 − ruido)`. O piso vale também no vale da respiração.
 */
internal fun raioDoBlob(rMax: Float, cheio: Float, respira: Float, nucleo: Float, ruido: Float): Float {
    val piso = nucleo / (Q_NUCLEO * (1f - ruido))
    val rMin = maxOf(0.35f * rMax, piso)
    return maxOf((rMin + (rMax - rMin) * cheio) * (1f + respira), piso)
}

/**
 * O estado da troca de Fase. Só os efeitos mexem aqui, em ordem: é memória, não estado da tela.
 * A fonte única da troca é o `faseMix` (§4, regra 2): ele leva a forma e a cor de [deForma]/[deCor] até [paraForma]/[paraCor].
 */
private class Troca(tipo: TipoFase, cor: Color, var progresso: Float) {
    var tipo = tipo
    var cor = cor
    var deForma = forma(tipo)
    var paraForma = deForma
    var deCor = cor
    var paraCor = cor
    /** Quanto o halo acende no meio da troca: só na troca de tipo. */
    var lampejo = 0f
    var jobFase: Job? = null
    var jobTamanho: Job? = null
    /** O florescer ou a volta com spring: um tique de segundo não o interrompe. */
    var tamanhoEspecial = false
}

/** Relógios acumulados no desenho: a velocidade e o período mudam sem salto de fase. */
internal class Relogio {
    private var antes = -1f
    var respiracao = 0f
        private set
    var ruido = 0f
        private set

    /** Anda até o tempo [t] do Fundo, que dá a volta em 360 s, no ritmo da [forma]. Devolve o passo, em segundos. */
    fun avancar(t: Float, forma: Forma): Float {
        val dt = if (antes < 0f) 0f else (t - antes + 360f) % 360f
        antes = t
        respiracao = (respiracao + dt / forma.periodo) % 1f
        ruido = (ruido + dt * forma.velocidade) % VOLTA_RUIDO
        return dt
    }
}

/** O período do deslocamento do ruído: o shader percorre um círculo de raio 40 no espaço do ruído, sem emenda. */
internal const val VOLTA_RUIDO = (2 * PI * 40).toFloat()

/**
 * O blob (§7): luz na borda, miolo profundo (§3). Encolhe com o tempo restante, respira e ondula na borda.
 * Na troca de Fase, a cor e a forma vão juntas pelo `faseMix`. No fim natural da Fase, floresce com overshoot (§5.6).
 */
@Composable
private fun Blob(progresso: Float, tipo: TipoFase, destaque: Color, rodando: Boolean, nucleo: Dp, modifier: Modifier) {
    val reduzido = rememberMovimentoReduzido()
    // O mesmo relógio do Fundo, sempre ligado: o Movimento do Fundo não para o Estilo visual. Reduzido: 0.2× (ruído, §9).
    val tempo = rememberTempoFundo(ligado = true)
    val escopo = rememberCoroutineScope()

    val mix = remember { Animatable(1f) }
    val tamanho = remember { Animatable(cheioDe(progresso)) } // aparece direto no progresso real (§4, regra 6)
    val onda = remember { Animatable(1f) }
    val troca = remember { Troca(tipo, destaque, progresso) }
    val relogio = remember { Relogio() }

    // Pausado: a respiração e o ruído caem para a coluna "Pausado" (SpringAmbiente); a saturação cai 30% (DurMedia).
    val pausa by animateFloatAsState(if (rodando) 0f else 1f, spring(dampingRatio = 1f, stiffness = 50f), label = "pausa")
    val saturacao by animateFloatAsState(if (rodando) 1f else 0.7f, tween(DUR_MEDIA, easing = EaseOut), label = "saturacao")

    LaunchedEffect(tipo, destaque, progresso) {
        val novaFase = tipo != troca.tipo
        val fim = novaFase && troca.progresso > 0.97f // a Fase acabou sozinha; senão foi Pular
        if (novaFase || destaque != troca.cor) {
            // Começa dos valores atuais: uma troca nova redireciona a que está em curso, nunca enfileira (§4, regra 3).
            // Para antes de ler: nenhum quadro desenha a cor nova com o mix antigo.
            troca.jobFase?.cancel()
            mix.stop()
            troca.deForma = misturar(troca.deForma, troca.paraForma, mix.value)
            troca.deCor = misturarCor(troca.deCor, troca.paraCor, mix.value)
            troca.paraForma = forma(tipo)
            troca.paraCor = destaque
            troca.lampejo = if (novaFase && !reduzido) 0.8f else 0f
            val spec = specDaTroca(reduzido, fim, novaFase)
            // ponytail: o rebase com snapTo perde a velocidade numa troca interrompida. Um Animatable de vetor a manteria.
            mix.snapTo(0f)
            troca.jobFase = escopo.launch { comEscalaNormal(reduzido) { mix.animateTo(1f, spec) } }
            // Onda de Ciclo novo: só da Pausa longa para o Foco 1, no fim natural (§5.6).
            if (fim && !reduzido && troca.tipo == TipoFase.PAUSA_LONGA && tipo == TipoFase.FOCO) escopo.lancarOnda(onda)
        }
        val alvo = cheioDe(progresso)
        val tique = !novaFase && alvo <= tamanho.value
        if (!(tique && troca.tamanhoEspecial && troca.jobTamanho?.isActive == true)) {
            troca.jobTamanho?.cancel()
            troca.tamanhoEspecial = !tique
            troca.jobTamanho = escopo.launch {
                when {
                    // Reduzido: crossfade curto, sem overshoot (§9).
                    reduzido -> comEscalaNormal(true) { tamanho.animateTo(alvo, tween(300, easing = EaseOut)) }
                    // Florescer: a cor muda primeiro, e a forma se reabre 120 ms depois, com ~4% de overshoot (§5.6).
                    fim -> { delay(120); tamanho.animateTo(alvo, spring(dampingRatio = 0.7f, stiffness = 120f)) }
                    // Pular, Estender, outro Perfil: o tamanho volta com SpringUi (§5.4, §5.5).
                    !tique -> tamanho.animateTo(alvo, spring(dampingRatio = 1f, stiffness = 440f))
                    // Tique de segundo: segue o tempo em linha reta até o próximo tique, sem degrau.
                    else -> tamanho.animateTo(alvo, tween(1000, easing = LinearEasing))
                }
            }
        }
        troca.tipo = tipo
        troca.cor = destaque
        troca.progresso = progresso
    }

    val inspiro = rememberInspiro(rodando, progresso, reduzido)

    val shader = rememberShader(SRC_BLOB)

    Box(
        modifier.drawBehind {
            val m = mix.value
            val f = misturar(troca.deForma, troca.paraForma, m)
            val forma = misturar(f, Forma(14f, 0.005f, f.ruido * 0.3f, f.frequencia, 0.15f), pausa)

            // Os relógios andam pelo tempo do Fundo. O tempo dá a volta em 360 s.
            relogio.avancar(tempo.value, forma)

            val respira = if (reduzido) 0f else forma.escala * sin(2f * PI.toFloat() * relogio.respiracao)
            val rMax = RAIO_BLOB * size.minDimension
            val nucleoPx = nucleo.toPx()
            val raio = raioDoBlob(rMax, tamanho.value, (1f + respira) * inspiro.value - 1f, nucleoPx, forma.ruido)
            val cor = misturarCor(troca.deCor, troca.paraCor, m)
            val borda = tom(cor, 0.80f, saturacao)
            val miolo = tom(cor, L_MIOLO, saturacao)
            val halo = 1f + troca.lampejo * sin(PI.toFloat() * m)
            val centro = Offset(size.width / 2f, size.height / 2f)

            // Desenha além da área: o halo, a borda ondulada e o overshoot passam do quadrado.
            val margem = size.minDimension * 0.3f
            desenharBlob(shader, centro, raio, relogio.ruido, forma, halo, borda, miolo, margem, nucleoPx)

            desenharOnda(onda.value, borda, rMax, centro)
        },
    )
}

/** A animação da troca de Fase (§5.6): fim natural, Pular, ou só a cor (prévia, outro Perfil). Reduzido: crossfade curto (§9). */
internal fun specDaTroca(reduzido: Boolean, fim: Boolean, novaFase: Boolean): AnimationSpec<Float> = when {
    reduzido -> tween(300, easing = EaseOut)
    fim -> spring(dampingRatio = 1f, stiffness = 40f) // SpringFase
    novaFase -> spring(dampingRatio = 1f, stiffness = 160f) // SpringPular
    else -> tween(DUR_MEDIA, easing = EaseOut)
}

/** Iniciar: um único inspiro, 1 → 1.03 → 1 (§5.2). O impulso no spring vivo dá o pico de ~3%. Retomar não inspira. */
@Composable
internal fun rememberInspiro(rodando: Boolean, progresso: Float, reduzido: Boolean): Animatable<Float, AnimationVector1D> {
    val inspiro = remember { Animatable(1f) }
    val rodouAntes = remember { booleanArrayOf(rodando) }
    LaunchedEffect(rodando) {
        val iniciou = rodando && !rodouAntes[0] && progresso < 0.01f
        rodouAntes[0] = rodando
        if (iniciou && !reduzido) inspiro.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 120f), initialVelocity = 0.72f)
    }
    return inspiro
}

/** A onda de Ciclo novo: [onda] vai de 0 a 1, 200 ms depois do fim natural da Pausa longa (§5.6). */
internal fun CoroutineScope.lancarOnda(onda: Animatable<Float, AnimationVector1D>) = launch {
    onda.snapTo(0f)
    delay(200)
    onda.animateTo(1f, tween(900, easing = EaseOut))
}

/** Onda de Ciclo novo (decoração): um anel de luz sai do Estilo, escala 1 → 1.4, alfa 0.25 → 0, uma vez. */
internal fun DrawScope.desenharOnda(onda: Float, cor: Color, raio: Float, centro: Offset) {
    if (onda >= 1f) return
    drawCircle(cor.copy(alpha = 0.25f * (1f - onda)), radius = raio * (1f + 0.4f * onda), center = centro, style = Stroke(width = 2.dp.toPx()))
}

/**
 * O shader do blob, ou o gradiente radial se ele não compilou no aparelho. Também é o blob da splash (ticket 13).
 * [centro] é local; [margem] estende a área desenhada além das bordas, para o halo e a borda ondulada.
 * [nucleo]: o raio, em px, que fica no miolo escuro, sem a luz do aro (os números). Zero: a luz começa em 30% do raio.
 */
internal fun DrawScope.desenharBlob(
    shader: ShaderAgsl?,
    centro: Offset,
    raio: Float,
    ruidoFase: Float,
    forma: Forma,
    halo: Float,
    borda: Color,
    miolo: Color,
    margem: Float = 0f,
    nucleo: Float = 0f,
) {
    val pincel = shader?.pincel {
        setFloatUniform("centro", centro.x + margem, centro.y + margem)
        setFloatUniform("raio", raio)
        setFloatUniform("ruidoFase", ruidoFase)
        setFloatUniform("ruidoAmp", forma.ruido)
        setFloatUniform("ruidoFreq", forma.frequencia)
        setFloatUniform("halo", halo)
        setFloatUniform("nucleo", nucleo)
        setColorUniform("borda", borda.toArgb())
        setColorUniform("miolo", miolo.toArgb())
    }
    translate(-margem, -margem) {
        val area = Size(size.width + 2 * margem, size.height + 2 * margem)
        val c = centro + Offset(margem, margem)
        if (pincel != null) drawRect(pincel, size = area)
        else drawCircle(Brush.radialGradient(listOf(miolo, borda), c, raio), raio, c)
    }
}

/** Um shader AGSL e o pincel dele, para desenhar. */
internal class ShaderAgsl(private val shader: RuntimeShader) {
    private val brush = ShaderBrush(shader)

    /** Liga os uniforms e devolve o pincel, ou null se algum set falhou: aí quem desenha usa a reserva. */
    fun pincel(uniforms: RuntimeShader.() -> Unit): ShaderBrush? = brush.takeIf { runCatching { shader.uniforms() }.isSuccess }
}

/** Nulo se o AGSL não compila no aparelho: quem desenha usa a reserva, em vez de fechar o app. */
@Composable
internal fun rememberShader(src: String): ShaderAgsl? = remember(src) { runCatching { ShaderAgsl(RuntimeShader(src)) }.getOrNull() }

/** Mistura em OkLCh (§5.6): a matiz anda pelo arco curto e o croma não cai no meio, sem o cáqui da reta em Oklab. */
internal fun misturarCor(de: Color, para: Color, t: Float) = Color(misturarOklch(de.toArgb(), para.toArgb(), t))

/**
 * Uma cor que anima em OkLCh, para quem não tem um `faseMix` próprio (botão primário, navegação, traço do Vidro).
 * Uma troca no meio redireciona a partir da cor exibida. A cor anima também no movimento reduzido (§9).
 */
@Composable
internal fun animarCor(alvo: Color, spec: AnimationSpec<Float>): State<Color> {
    val reduzido = rememberMovimentoReduzido()
    val mix = remember { Animatable(1f) }
    val de = remember { mutableStateOf(alvo) }
    val para = remember { mutableStateOf(alvo) }
    LaunchedEffect(alvo) {
        if (alvo == para.value) return@LaunchedEffect
        mix.stop()
        de.value = misturarCor(de.value, para.value, mix.value)
        para.value = alvo
        mix.snapTo(0f)
        comEscalaNormal(reduzido) { mix.animateTo(1f, spec) }
    }
    return remember { derivedStateOf { misturarCor(de.value, para.value, mix.value) } }
}

/** A Cor de destaque com o L de [l] e o croma × [saturacao], em Oklab (§3: luz na borda, miolo profundo). */
internal fun tom(cor: Color, l: Float, saturacao: Float) = Color(tomArgb(cor.toArgb(), l, saturacao))

/**
 * Com "Remover animações", o Compose terminaria a animação no quadro seguinte (§9).
 * O crossfade curto do movimento reduzido roda com a escala 1.
 */
internal suspend fun comEscalaNormal(ligado: Boolean, bloco: suspend () -> Unit) =
    if (ligado) withContext(EscalaNormal) { bloco() } else bloco()

private val EscalaNormal = object : androidx.compose.ui.MotionDurationScale {
    override val scaleFactor = 1f
}

/** Ruído de valor (hash sem seno, interpolação quíntica) e o pontilhado contra faixas: comum aos shaders dos Estilos. */
internal const val RUIDO_AGSL = """
float hash(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float ruido(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float ign(float2 fc) { return fract(52.9829189 * fract(dot(fc, float2(0.06711056, 0.00583715)))); }
"""

/**
 * O blob em AGSL. Coordenadas em pixels; o raio já vem com o progresso, a respiração e o florescer.
 * A borda é ruído de valor lido num círculo (sem emenda no ângulo), em duas oitavas, deslocado em volta de um
 * círculo de raio 40 no espaço do ruído: o movimento se repete sem salto.
 * Dentro: do miolo profundo à borda clara, com um redemoinho leve e a luz batendo de cima à esquerda.
 * A luz só começa além do [nucleo] (os números), com folga para o redemoinho: ali o miolo é puro (§3).
 * Fora: um halo suave da cor da borda. A saída é pré-multiplicada, com pontilhado contra faixas.
 * A fórmula da borda se repete em `ferramentas/gerar_icone.py`, que desenha o ícone do app: mude as duas juntas.
 */
internal const val SRC_BLOB = """
uniform float2 centro;
uniform float raio;
uniform float ruidoFase;
uniform float ruidoAmp;
uniform float ruidoFreq;
uniform float halo;
uniform float nucleo;
layout(color) uniform half4 borda;
layout(color) uniform half4 miolo;

$RUIDO_AGSL

half4 main(float2 fc) {
    float2 p = fc - centro;
    float dist = length(p);
    float2 dir = dist > 0.0001 ? p / dist : float2(1.0, 0.0);
    float ang = ruidoFase / 40.0;
    float2 desloca = 40.0 * float2(cos(ang), sin(ang));
    float n = 0.65 * ruido(dir * ruidoFreq + desloca)
            + 0.35 * ruido(dir * ruidoFreq * 2.1 + desloca * 1.3 + float2(17.0, 5.0));
    float r = raio * (1.0 + ruidoAmp * (n - 0.5) * 2.0);
    float d = dist - r;
    float cobre = clamp(0.5 - d / 1.2, 0.0, 1.0);

    // Dentro: miolo escuro, luz só perto da borda; o redemoinho tira a cara de gradiente perfeito.
    float q = dist / r;
    float ini = max(0.3, nucleo / r + 0.06); // o redemoinho mexe q em ±0.04: o miolo sob os números fica puro
    float gira = ruido(p / raio * 1.6 + desloca * 0.15);
    float prof = pow(smoothstep(ini, 1.0, q + 0.08 * (gira - 0.5)), 2.6);
    float3 cor = mix(float3(miolo.rgb), float3(borda.rgb), prof);
    float aro = smoothstep(max(0.82, ini + 0.04), 0.985, q) * (1.0 - 0.3 * smoothstep(0.985, 1.0, q));
    float luz = 0.5 + 0.5 * dot(dir, float2(-0.53, -0.85));
    cor = mix(cor, float3(1.0), aro * luz * 0.22);

    // Fora: o halo da cor da borda, pré-multiplicado.
    float brilho = exp(-max(d, 0.0) / (0.18 * raio)) * 0.4 * halo * (1.0 - cobre);
    float3 rgb = cor * cobre + float3(borda.rgb) * brilho;
    float alfa = clamp(cobre + brilho, 0.0, 1.0);
    float t = ign(fc) + ign(fc + float2(5.588238, 5.588238)) - 1.0;
    rgb += alfa * t / 255.0;
    return half4(half3(rgb), half(alfa));
}
"""

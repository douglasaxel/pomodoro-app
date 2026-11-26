package com.exemplo.pomodoro.ui

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.dados.L_MIOLO
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

// Tempos da splash (DESIGN.md §5.12).
private const val PARADO_MS = 2000L
private const val CRESCE_MS = 800
/** O fade começa com o blob já cobrindo a tela, antes do fim do crescimento: uma batida só, sem fila. */
private const val FADE_APOS_MS = 600L
/** Quanto esperar pela Cor de destaque do Perfil ativo antes de usar a padrão. */
private const val ESPERA_COR_MS = 500L

/**
 * A splash animada da abertura a frio (ticket 13, §5.12). Fica por cima do Timer, que já está composto embaixo.
 * O blob sobe da borda de baixo até o centro, fica 2 s parado ondulando e cresce até cobrir a tela.
 * Um fade revela o Timer. Não dá para pular: os toques param aqui.
 * [destaque]: a Cor de destaque de Foco do Perfil ativo; null até o banco responder.
 */
@Composable
fun SplashAnimada(destaque: Int?, aoTerminar: () -> Unit) {
    val reduzido = rememberMovimentoReduzido()
    val r = remember { reduzido } // lido uma vez: a coreografia não troca de versão no meio
    // O relógio do Fundo, como o blob do Timer. Reduzido: parado, sem ondulação.
    val tempo = rememberTempoFundo(ligado = !r)
    val corAtual by rememberUpdatedState(destaque)
    val cor = remember { mutableStateOf<Color?>(null) }

    // Frações de 0 a 1. O limiar fino evita que o spring pare antes, com um salto no fim do percurso.
    val subida = remember { Animatable(if (r) 1f else 0f, visibilityThreshold = 0.0005f) }
    val cresce = remember { Animatable(0f) }
    val alfaBlob = remember { Animatable(if (r) 0f else 1f) }
    val alfa = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val argb = withTimeoutOrNull(ESPERA_COR_MS) { snapshotFlow { corAtual }.filterNotNull().first() }
        cor.value = Color(argb ?: DESTAQUES_PADRAO[0]) // presa aqui: uma troca no meio seria um salto
        if (r) {
            // Movimento reduzido (§9): aparece no centro com fade curto, fica parado e sai num crossfade.
            comEscalaNormal(true) { alfaBlob.animateTo(1f, tween(DUR_MEDIA, easing = EaseOut)) }
            delay(PARADO_MS)
            comEscalaNormal(true) { alfa.animateTo(0f, tween(DUR_LONGA, easing = EaseOut)) }
        } else {
            subida.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 120f)) // SpringVivo: chega com leve overshoot
            delay(PARADO_MS)
            coroutineScope {
                launch { cresce.animateTo(1f, tween(CRESCE_MS, easing = EaseInOut)) }
                delay(FADE_APOS_MS)
                alfa.animateTo(0f, tween(DUR_LONGA, easing = EaseOut))
            }
        }
        aoTerminar()
    }

    val shader = remember { runCatching { RuntimeShader(SRC_BLOB) }.getOrNull() }
    val brush = remember(shader) { shader?.let(::ShaderBrush) }
    val forma = forma(TipoFase.FOCO)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .graphicsLayer { alpha = alfa.value }
            .background(BaseEscura),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alfaBlob.value }
                .drawBehind {
                    val c = cor.value ?: return@drawBehind
                    val t = tempo.value
                    val respira = if (r) 0f else forma.escala * sin(2f * PI.toFloat() * t / forma.periodo)
                    // Parado, o blob tem o tamanho do blob cheio no Timer (o RAIO_BLOB da área de 0.84 × a largura).
                    val r0 = RAIO_BLOB * 0.84f * size.minDimension
                    // Cobre a tela: a borda mais funda do ruído passa dos cantos, com folga para o fade começar antes do fim.
                    val cobre = hypot(size.width, size.height) / 2f / (1f - forma.ruido) * 1.15f
                    val raio = lerp(r0, cobre, cresce.value) * (1f + respira)
                    val y = lerp(size.height + 1.6f * r0, size.height / 2f, subida.value) // começa fora, com o halo
                    desenharBlob(
                        shader, brush, Offset(size.width / 2f, y), raio,
                        ruidoFase = (t * forma.velocidade) % VOLTA_RUIDO,
                        forma = forma,
                        halo = 1f,
                        borda = tom(c, 0.80f, 1f),
                        miolo = tom(c, L_MIOLO, 1f),
                    )
                },
        )
    }
}

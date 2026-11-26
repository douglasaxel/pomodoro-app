package com.exemplo.pomodoro.ui

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.exemplo.pomodoro.dados.Aparencia
import com.exemplo.pomodoro.dados.Fundo
import com.exemplo.pomodoro.dados.alfaVeu
import com.exemplo.pomodoro.dados.arte
import com.exemplo.pomodoro.dados.arquivoDaFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Camadas 1 e 2 do §3: o Fundo e o Véu, na raiz, atrás do NavHost (§5.10).
 * [naTelaTimer]: falso fora do Timer; aí, o Fundo desacelera até parar e o Véu sobe para a intensidade de leitura.
 */
@Composable
fun FundoComVeu(aparencia: Aparencia?, naTelaTimer: Boolean) {
    val a = aparencia ?: Aparencia(fundo = Fundo.Cor(BaseEscura.toArgb())) // até o banco responder: a cor da splash
    val tempo = rememberTempoFundo(a.movimentoFundo && naTelaTimer)

    // Véu do Perfil: na troca de Perfil vai ao novo cálculo em DurFundo (§5.7). A leitura (§5.10) entra em DurMedia.
    val alfaPerfil by animateFloatAsState(alfaVeu(a.fundo, a.veuAjuste), tween(DUR_FUNDO, easing = EaseMorph), label = "veu")
    val leitura by animateFloatAsState(if (naTelaTimer) 0f else 1f, tween(DUR_MEDIA, easing = EaseOut), label = "leitura")
    val parallax = rememberParallaxDoFundo()

    Box(Modifier.fillMaxSize().background(BaseEscura)) {
        Crossfade(
            targetState = a.fundo,
            animationSpec = tween(DUR_FUNDO, easing = EaseMorph), // evita salto brusco de brilho (§5.7)
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Blur nativo de 0 a 24 dp, proporcional ao alfa (§3). Leitura: 40 dp. Lido aqui, sem recompor.
                    val alfa = veuFinal(alfaPerfil, leitura)
                    val raio = lerp(VEU_BLUR_MAX.toPx() * alfa, VEU_BLUR_LEITURA.toPx(), leitura)
                    renderEffect = if (raio < 0.5f) null else BlurEffect(raio, raio, TileMode.Clamp)
                    // Overscan de 1.06× (§3): a borda do blur e o parallax precisam de margem.
                    scaleX = 1.06f
                    scaleY = 1.06f
                    // Parallax do Vidro com parallax (§7): até ±12 dp, sem passar da margem do overscan.
                    val margem = minOf(PARALLAX_MAX.toPx(), size.width * 0.03f, size.height * 0.03f)
                    translationX = parallax.value.x * margem
                    translationY = parallax.value.y * margem
                },
            label = "fundo",
        ) { f ->
            when (f) {
                is Fundo.Cor -> Box(Modifier.fillMaxSize().background(Color(f.argb)))
                is Fundo.Gradiente -> ShaderDeFundo(SRC_GRADIENTE, f.cores, tempo)
                is Fundo.Foto -> FotoDeFundo(f.caminho)
                is Fundo.Biblioteca -> arte(f.arte).let { ShaderDeFundo(it.shader, it.paleta, tempo) }
            }
        }
        Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = veuFinal(alfaPerfil, leitura))) })
    }
}

private fun veuFinal(alfaPerfil: Float, leitura: Float) = lerp(alfaPerfil, maxOf(alfaPerfil, VEU_ALFA_LEITURA), leitura)

private const val VEU_ALFA_LEITURA = 0.78f
private val VEU_BLUR_MAX = 24.dp
private val VEU_BLUR_LEITURA = 40.dp

/** "Remover animações" do sistema. Relido ao voltar ao app, porque o ajuste muda fora dele. */
@Composable
fun rememberMovimentoReduzido(): Boolean {
    var reduzido by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    LifecycleResumeEffect(Unit) {
        reduzido = !ValueAnimator.areAnimatorsEnabled()
        onPauseOrDispose { }
    }
    return reduzido
}

/** Período em que todo o movimento do shader se repete. O tempo dá a volta aqui, sem salto e sem perder precisão. */
private const val PERIODO_S = 360f

/**
 * Tempo do shader em segundos, o único relógio do Fundo e das miniaturas da Biblioteca.
 * Avança com `withFrameNanos` só enquanto a tela está visível e a velocidade é maior que 0.
 * A velocidade vem de um spring: [ligado] falso desacelera até parar, sem tranco (§5.9). Movimento reduzido: 0.2× (§9).
 */
@Composable
fun rememberTempoFundo(ligado: Boolean): State<Float> {
    val reduzido = rememberMovimentoReduzido()
    val alvo = when {
        !ligado -> 0f
        reduzido -> 0.2f
        else -> 1f
    }
    val t = remember { mutableFloatStateOf(0f) }
    val velocidade = animateFloatAsState(alvo, spring(dampingRatio = 1f, stiffness = 50f), label = "velocidade") // SpringAmbiente
    val andando by remember { derivedStateOf { velocidade.value > 0.001f } }
    val visivel = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(andando && visivel) {
        if (!(andando && visivel)) return@LaunchedEffect
        var antes = withFrameNanos { it }
        while (true) withFrameNanos { agora ->
            t.floatValue = (t.floatValue + (agora - antes) / 1e9f * velocidade.value) % PERIODO_S
            antes = agora
        }
    }
    return t
}

/**
 * Mesh gradient: cada cor é um ponto que deriva devagar, e o pixel mistura as cores pelo inverso da distância.
 * Frequências inteiras sobre o PERIODO_S (360 s): períodos de 40 a 90 s (§7 Fundo).
 * Amplitude 0.08 da altura: no período mais curto, o ponto anda ≤ 0.08 · √2 · 2π/40 ≈ 1.8% da altura por segundo (limite: 2%).
 * Uma leve dobra do domínio deixa as bordas entre as cores orgânicas. Um pontilhado de 1/255 evita faixas.
 */
private const val SRC_GRADIENTE = """
uniform float2 size;
uniform float iTime;
uniform int n;
uniform half4 cores[6];

half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float asp = size.x / size.y;
    float w = 6.2831853 / 360.0;
    uv += 0.03 * float2(sin(uv.y * 3.0 + iTime * w * 5.0), cos(uv.x * 3.0 + iTime * w * 4.0));
    float3 soma = float3(0.0);
    float pesos = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= n) break;
        float fi = float(i);
        float ang = 6.2831853 * fi / float(n) + 0.6;
        float2 base = float2(asp * (0.5 + 0.34 * cos(ang)), 0.5 + 0.36 * sin(ang));
        float2 p = base + 0.08 * float2(sin(iTime * w * (4.0 + fi) + fi * 1.7), cos(iTime * w * (9.0 - fi) + fi * 2.3));
        float2 d = uv - p;
        float peso = 1.0 / pow(dot(d, d) + 0.015, 1.6);
        soma += float3(cores[i].rgb) * peso;
        pesos += peso;
    }
    float3 cor = soma / pesos;
    float h = fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453);
    cor += (h - 0.5) / 255.0;
    return half4(half3(cor), 1.0);
}
"""

/**
 * Desenha um shader de Fundo: o Gradiente vivo ou uma arte da Biblioteca. Todos recebem `size`, `iTime` e `cores`.
 * Só o Gradiente vivo declara `n` (quantas cores valem): o AGSL remove uniform sem uso, e o set lançaria exceção.
 * Se o shader não compilar no aparelho, desenha um gradiente parado com as mesmas cores, em vez de fechar o app.
 */
@Composable
fun ShaderDeFundo(src: String, cores: List<Int>, tempo: State<Float>, modifier: Modifier = Modifier.fillMaxSize()) {
    val shader = rememberShader(src)
    val usadas = cores.take(Fundo.CORES_MAX).ifEmpty { listOf(BaseEscura.toArgb()) }
    val rgba = remember(usadas) {
        FloatArray(Fundo.CORES_MAX * 4).also { arr ->
            usadas.forEachIndexed { i, c ->
                arr[i * 4] = (c shr 16 and 0xFF) / 255f
                arr[i * 4 + 1] = (c shr 8 and 0xFF) / 255f
                arr[i * 4 + 2] = (c and 0xFF) / 255f
                arr[i * 4 + 3] = 1f
            }
        }
    }
    Box(
        modifier.drawWithCache {
            val reserva = Brush.linearGradient(usadas.map(::Color).let { if (it.size < 2) it + it else it })
            onDrawBehind {
                val pincel = shader?.pincel {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("iTime", tempo.value) // lido no desenho: redesenha sem recompor
                    if (src == SRC_GRADIENTE) setIntUniform("n", usadas.size)
                    setFloatUniform("cores", rgba)
                }
                drawRect(pincel ?: reserva)
            }
        },
    )
}

@Composable
private fun FotoDeFundo(caminho: String) {
    val ctx = LocalContext.current
    val imagem by produceState<ImageBitmap?>(null, caminho) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(arquivoDaFoto(ctx, caminho).path)?.asImageBitmap() }
    }
    imagem?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
}

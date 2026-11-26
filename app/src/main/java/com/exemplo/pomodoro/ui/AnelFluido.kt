package com.exemplo.pomodoro.ui

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.dados.L_MIOLO
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Diâmetro do anel sobre a área do Estilo: 0.78 × a largura dentro de uma área de 0.84 × a largura (§7). */
private const val RAIO_SOBRE_AREA = 0.78f / 0.84f / 2f

/** Raio do anel no Timer de 411 dp. Abaixo dele (prévia, telas estreitas), a espessura, o brilho e os pontos encolhem junto. */
private val RAIO_REFERENCIA = 160.dp

/** A memória da troca de Fase do anel. Só os efeitos mexem aqui, em ordem; o `faseMix` leva [deForma]/[deCor] até [paraForma]/[paraCor]. */
private class TrocaAnel(tipo: TipoFase, cor: Color, var progresso: Float) {
    var tipo = tipo
    var cor = cor
    var deForma = forma(tipo)
    var paraForma = deForma
    var deCor = cor
    var paraCor = cor
    var jobFase: Job? = null
    var jobArco: Job? = null
    /** O fim de Fase ou a volta com spring: um tique de segundo não os interrompe. */
    var arcoEspecial = false
    /** Pedido do fim de Fase para o desenho, que conhece o raio: desfazer o anel em partículas no próximo quadro. */
    var dissolver = false
}

/**
 * O Anel fluido (§7): um anel de 10 dp com borda líquida que se fecha com o tempo decorrido, do topo no sentido horário.
 * Um brilho segue a ponta e solta partículas que derivam para fora. A espessura respira; o raio não muda.
 * Fim de Fase (§5.6): o arco completa a volta, a ponta brilha +40% por 300 ms, o anel se desfaz em ~40 partículas
 * e renasce vazio na cor nova com `SpringFase`. Pular e Estender: o arco recua com `SpringUi`.
 */
@Composable
internal fun AnelFluido(progresso: Float, tipo: TipoFase, destaque: Color, rodando: Boolean, modifier: Modifier) {
    val reduzido = rememberMovimentoReduzido()
    val tempo = rememberTempoFundo(ligado = true) // o relógio do blob: reduzido anda a 0.2× (ruído, §9)
    val escopo = rememberCoroutineScope()

    val mix = remember { Animatable(1f) }
    val arco = remember { Animatable(progresso) } // aparece direto no progresso real (§4, regra 6)
    val presenca = remember { Animatable(1f) }
    val lampejo = remember { Animatable(0f) }
    val inspiro = remember { Animatable(1f) }
    val onda = remember { Animatable(1f) }
    val troca = remember { TrocaAnel(tipo, destaque, progresso) }
    val relogio = remember { Relogio() }
    val particulas = remember { Particulas() }
    val rodouAntes = remember { booleanArrayOf(rodando) }

    // Pausado: o ruído desacelera para a coluna "Pausado" e o brilho da ponta cai para 40% (SpringAmbiente); a saturação cai 30%.
    val pausa by animateFloatAsState(if (rodando) 0f else 1f, spring(dampingRatio = 1f, stiffness = 50f), label = "pausa")
    val saturacao by animateFloatAsState(if (rodando) 1f else 0.7f, tween(DUR_MEDIA, easing = EaseOut), label = "saturacao")

    LaunchedEffect(tipo, destaque, progresso) {
        val novaFase = tipo != troca.tipo
        val fim = novaFase && troca.progresso > 0.97f // a Fase acabou sozinha; senão foi Pular
        if (novaFase || destaque != troca.cor) {
            // Começa dos valores atuais: uma troca nova redireciona a que está em curso (§4, regra 3).
            troca.deForma = misturar(troca.deForma, troca.paraForma, mix.value)
            troca.deCor = misturarCor(troca.deCor, troca.paraCor, mix.value)
            troca.paraForma = forma(tipo)
            troca.paraCor = destaque
            val spec: AnimationSpec<Float> = when {
                reduzido -> tween(300, easing = EaseOut)
                fim -> spring(dampingRatio = 1f, stiffness = 40f) // SpringFase
                novaFase -> spring(dampingRatio = 1f, stiffness = 160f) // SpringPular
                else -> tween(DUR_MEDIA, easing = EaseOut) // só a cor mudou (prévia, outro Perfil)
            }
            troca.jobFase?.cancel()
            mix.snapTo(0f) // já aqui: nenhum quadro desenha a cor nova antes da hora
            // No fim, a cor espera o anel quase sumir: as partículas saem na cor velha, e ele renasce na nova.
            troca.jobFase = escopo.launch {
                comEscalaNormal(reduzido) {
                    if (fim && !reduzido) delay(450)
                    mix.animateTo(1f, spec)
                }
            }
            // Onda de Ciclo novo: só da Pausa longa para o Foco 1, no fim natural (§5.6).
            if (fim && !reduzido && troca.tipo == TipoFase.PAUSA_LONGA && tipo == TipoFase.FOCO) escopo.launch {
                onda.snapTo(0f)
                delay(200)
                onda.animateTo(1f, tween(900, easing = EaseOut))
            }
        }
        val alvo = progresso
        val tique = !novaFase && alvo >= troca.progresso
        if (!(tique && troca.arcoEspecial && troca.jobArco?.isActive == true)) {
            troca.jobArco?.cancel()
            troca.arcoEspecial = !tique
            troca.jobArco = escopo.launch {
                when {
                    tique -> arco.animateTo(alvo, tween(1000, easing = LinearEasing)) // segue o tempo, sem degrau
                    // Reduzido: crossfade curto dos dois estados, sem o arco correr (§9).
                    reduzido -> comEscalaNormal(true) {
                        lampejo.snapTo(0f)
                        presenca.animateTo(0f, tween(150, easing = EaseOut))
                        arco.snapTo(troca.progresso)
                        presenca.animateTo(1f, tween(150, easing = EaseOut))
                    }
                    fim -> {
                        launch {
                            lampejo.animateTo(0.4f, tween(120, easing = EaseOut))
                            delay(180)
                            lampejo.animateTo(0f, tween(300, easing = EaseOut))
                        }
                        arco.animateTo(1f, tween(200, easing = EaseOut))
                        delay(100)
                        troca.dissolver = true
                        presenca.animateTo(0f, tween(450, easing = EaseOut))
                        arco.snapTo(troca.progresso) // invisível: renasce vazio, já no progresso real
                        presenca.animateTo(1f, spring(dampingRatio = 1f, stiffness = 40f)) // SpringFase
                    }
                    // Pular, Estender, outro Perfil: o arco recua com SpringUi a partir do exibido (§5.4, §5.5).
                    else -> {
                        troca.dissolver = false
                        launch { lampejo.animateTo(0f, spring(dampingRatio = 1f, stiffness = 440f)) }
                        launch { presenca.animateTo(1f, spring(dampingRatio = 1f, stiffness = 440f)) }
                        arco.animateTo(alvo, spring(dampingRatio = 1f, stiffness = 440f))
                    }
                }
            }
        }
        troca.tipo = tipo
        troca.cor = destaque
        troca.progresso = progresso
    }

    // Iniciar: um único inspiro, 1 → 1.03 → 1 (§5.2). Retomar não inspira.
    LaunchedEffect(rodando) {
        val iniciou = rodando && !rodouAntes[0] && progresso < 0.01f
        rodouAntes[0] = rodando
        if (iniciou && !reduzido) inspiro.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 120f), initialVelocity = 0.72f)
    }

    val shader = remember { runCatching { RuntimeShader(SRC_ANEL) }.getOrNull() }
    val brush = remember(shader) { shader?.let(::ShaderBrush) }

    Box(
        modifier.drawBehind {
            val m = mix.value
            val f = misturar(troca.deForma, troca.paraForma, m)
            // Pausado: só desacelera (período e velocidade da coluna "Pausado"); a amplitude da borda fica.
            val forma = misturar(f, Forma(14f, f.escala, f.ruido, f.frequencia, 0.15f), pausa)

            val t = tempo.value
            val dt = if (relogio.antes < 0f) 0f else (t - relogio.antes + 360f) % 360f
            relogio.antes = t
            relogio.respiracao = (relogio.respiracao + dt / forma.periodo) % 1f
            relogio.ruido = (relogio.ruido + dt * forma.velocidade) % VOLTA_RUIDO

            val rMax = size.minDimension * RAIO_SOBRE_AREA
            val k = (rMax / RAIO_REFERENCIA.toPx()).coerceAtMost(1f)
            val raio = rMax * inspiro.value
            // A espessura respira ±8% (menos, pausado); o raio não. Reduzido: sem respiração (§9).
            val respira = if (reduzido) 0f else 0.08f * (1f - 0.7f * pausa) * sin(2f * PI.toFloat() * relogio.respiracao)
            val espessura = 10.dp.toPx() * k * (1f + respira)
            val cor = misturarCor(troca.deCor, troca.paraCor, m)
            val borda = tom(cor, 0.80f, saturacao)
            val miolo = tom(cor, L_MIOLO, saturacao)
            val corPonta = tom(cor, 0.85f, saturacao)
            val brilho = (1f - 0.6f * pausa) * (1f + lampejo.value)
            val p = arco.value.coerceIn(0f, 1f)
            val anguloPonta = p * 2f * PI.toFloat()
            val centro = Offset(size.width / 2f, size.height / 2f)

            // As partículas andam em tempo real: o relógio do Fundo vai a 0.2× no reduzido. Um salto (volta do fundo) não conta.
            // Vivem em dp do Timer: numa área menor (prévia, miniatura), a deriva encolhe junto com o anel.
            val dtReal = (if (reduzido) dt * 5f else dt).coerceAtMost(0.1f)
            val escalaDp = density * k
            if (troca.dissolver) {
                troca.dissolver = false
                particulas.dissolver(raio / escalaDp)
            }
            particulas.avancar(dtReal, rodando && presenca.value > 0.9f, anguloPonta, raio / escalaDp, reduzido)

            // Desenha além da área: o brilho da ponta, a borda líquida e as partículas passam do quadrado.
            val margem = size.minDimension * 0.12f
            val ok = shader != null && brush != null && runCatching {
                shader.setFloatUniform("centro", centro.x + margem, centro.y + margem)
                shader.setFloatUniform("raio", raio)
                shader.setFloatUniform("espessura", espessura)
                shader.setFloatUniform("progresso", p)
                shader.setFloatUniform("ruidoFase", relogio.ruido)
                shader.setFloatUniform("ruidoAmp", forma.ruido * 0.5f)
                shader.setFloatUniform("ruidoFreq", forma.frequencia)
                shader.setFloatUniform("brilho", brilho)
                shader.setFloatUniform("presenca", presenca.value)
                shader.setFloatUniform("sigma", 8.dp.toPx() * k)
                shader.setColorUniform("borda", borda.toArgb())
                shader.setColorUniform("miolo", miolo.toArgb())
                shader.setColorUniform("ponta", corPonta.toArgb())
            }.isSuccess
            if (ok) translate(-margem, -margem) { drawRect(brush, size = Size(size.width + 2 * margem, size.height + 2 * margem)) }
            else {
                // Sem shader: trilho, arco de pontas redondas e o brilho da ponta, sem a borda líquida.
                drawCircle(Color.White.copy(alpha = 0.08f), raio, centro, style = Stroke(espessura))
                val tl = Offset(centro.x - raio, centro.y - raio)
                drawArc(borda.copy(alpha = presenca.value), -90f, p * 360f, false, tl, Size(2 * raio, 2 * raio), style = Stroke(espessura, cap = StrokeCap.Round))
                val pt = centro + Offset(raio * sin(anguloPonta), -raio * cos(anguloPonta))
                val alcance = 24.dp.toPx() * k
                drawCircle(Brush.radialGradient(listOf(corPonta.copy(alpha = 0.6f * brilho.coerceAtMost(1f) * presenca.value), Color.Transparent), pt, alcance), alcance, pt)
            }

            // Partículas: um ponto e um halo tênue, na cor da ponta.
            particulas.paraCada { x, y, tam, alfa ->
                val c = centro + Offset(x * escalaDp, y * escalaDp)
                val r = tam * escalaDp
                drawCircle(corPonta.copy(alpha = 0.16f * alfa), r * 2.6f, c)
                drawCircle(corPonta.copy(alpha = 0.85f * alfa), r, c)
            }

            // Onda de Ciclo novo (decoração): um anel de luz sai do anel, escala 1 → 1.4, alfa 0.25 → 0, uma vez.
            if (onda.value < 1f) {
                drawCircle(
                    borda.copy(alpha = 0.25f * (1f - onda.value)),
                    radius = rMax * (1f + 0.4f * onda.value),
                    center = centro,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        },
    )
}

/**
 * O anel em AGSL, em pixels. O centro da faixa e a espessura levam ruído lido num círculo (sem emenda no ângulo),
 * deslocado em volta de um círculo no espaço do ruído: a borda ondula como líquido e se repete sem salto.
 * O arco cobre do topo até [progresso] no sentido horário, com pontas redondas. A faixa tem luz nas bordas e o
 * meio mais fundo (§3), um brilho que corre devagar por dentro e a luz de cima à esquerda na borda de fora.
 * O trilho vazio é branco a 8%. Na ponta, um brilho radial gaussiano. Saída pré-multiplicada, com pontilhado.
 */
private const val SRC_ANEL = """
uniform float2 centro;
uniform float raio;
uniform float espessura;
uniform float progresso;
uniform float ruidoFase;
uniform float ruidoAmp;
uniform float ruidoFreq;
uniform float brilho;
uniform float presenca;
uniform float sigma;
layout(color) uniform half4 borda;
layout(color) uniform half4 miolo;
layout(color) uniform half4 ponta;

$RUIDO_AGSL

float2 desloca() {
    float ang = ruidoFase / 40.0;
    return 40.0 * float2(cos(ang), sin(ang));
}

// O raio do centro da faixa e a espessura numa direção.
float2 faixa(float2 dir, float2 d) {
    float n = 0.65 * ruido(dir * ruidoFreq + d)
            + 0.35 * ruido(dir * ruidoFreq * 2.1 + d * 1.3 + float2(17.0, 5.0));
    float m = ruido(dir * ruidoFreq * 2.7 + d * 1.7 + float2(31.0, 7.0));
    return float2(raio * (1.0 + ruidoAmp * (n - 0.5) * 2.0), espessura * (1.0 + 0.4 * (m - 0.5)));
}

half4 main(float2 fc) {
    float2 p = fc - centro;
    float dist = length(p);
    // Longe da faixa e do brilho, nada a desenhar.
    if (abs(dist - raio) > raio * ruidoAmp + espessura + 4.0 * sigma) return half4(0.0);
    float2 dir = dist > 0.0001 ? p / dist : float2(0.0, -1.0);
    float2 d = desloca();
    float2 rw = faixa(dir, d);
    float banda = abs(dist - rw.x) - rw.y * 0.5;

    // Do topo, no sentido horário (y para baixo).
    float a = fract(atan(p.x, -p.y) / 6.2831853 + 1.0);
    float angPonta = progresso * 6.2831853;
    float2 dirPonta = float2(sin(angPonta), -cos(angPonta));
    float2 rwPonta = faixa(dirPonta, d);
    float2 rwInicio = faixa(float2(0.0, -1.0), d);
    float2 pPonta = dirPonta * rwPonta.x;
    float arcoD = banda;
    if (a > progresso) {
        float dIni = length(p - float2(0.0, -rwInicio.x)) - rwInicio.y * 0.5;
        float dPon = length(p - pPonta) - rwPonta.y * 0.5;
        arcoD = min(dIni, dPon);
    }
    float arco = clamp(0.5 - arcoD / 1.2, 0.0, 1.0) * presenca;
    float trilho = clamp(0.5 - banda / 1.2, 0.0, 1.0) * (1.0 - arco);

    // Luz nas bordas, meio fundo; um brilho que corre devagar; a luz de cima à esquerda na borda de fora.
    float tt = clamp((dist - rw.x) / (rw.y * 0.5), -1.0, 1.0);
    float3 cor = mix(float3(miolo.rgb), float3(borda.rgb), 0.6 + 0.4 * abs(tt));
    cor *= 0.92 + 0.16 * ruido(dir * 5.0 + d * 0.6 + float2(3.0, 11.0));
    float luz = 0.5 + 0.5 * dot(dir, float2(-0.53, -0.85));
    cor = mix(cor, float3(1.0), smoothstep(0.2, 1.0, tt) * luz * 0.18);

    float halo = exp(-max(arcoD, 0.0) / (0.8 * espessura)) * 0.25 * (1.0 - arco) * presenca;
    float dg = length(p - pPonta);
    float g = exp(-dg * dg / (2.0 * sigma * sigma)) * 0.6 * brilho * presenca;

    float3 rgb = cor * arco + float3(0.08) * trilho + float3(borda.rgb) * halo + float3(ponta.rgb) * g;
    float alfa = clamp(arco + 0.08 * trilho + halo + g, 0.0, 1.0);
    rgb = min(rgb, float3(alfa));
    float t = ign(fc) + ign(fc + float2(5.588238, 5.588238)) - 1.0;
    rgb += alfa * t / 255.0;
    return half4(half3(rgb), half(alfa));
}
"""

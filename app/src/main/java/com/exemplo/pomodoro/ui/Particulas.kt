package com.exemplo.pomodoro.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * As partículas do Anel fluido (§7, §9), em dp e segundos, a partir do centro do anel (y para baixo).
 * Nascem na ponta e derivam para fora enquanto o alfa cai. Pausado, a emissão para e as vivas somem em 600 ms.
 * Memória do desenho, sem estado do Compose: o desenho avança e lê a cada quadro.
 */
internal class Particulas(semente: Int = 7) {
    private val aleatorio = Random(semente)
    private val x = FloatArray(CAPACIDADE)
    private val y = FloatArray(CAPACIDADE)
    private val vx = FloatArray(CAPACIDADE)
    private val vy = FloatArray(CAPACIDADE)
    private val idade = FloatArray(CAPACIDADE)
    private val vida = FloatArray(CAPACIDADE)
    private val tamanho = FloatArray(CAPACIDADE)
    private val sumico = FloatArray(CAPACIDADE)
    private var acumulado = 0f

    var vivas = 0
        private set

    /**
     * Avança [dt]. Emitindo, nasce uma partícula na ponta (ângulo [ponta] em radianos, do topo no sentido horário,
     * no [raio]) num ritmo que mantém o teto vivo: 24 com deriva de até 6 dp/s, ou 8 com 2 dp/s no movimento reduzido.
     */
    fun avancar(dt: Float, emitindo: Boolean, ponta: Float, raio: Float, reduzido: Boolean) {
        var i = 0
        while (i < vivas) {
            idade[i] += dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            if (!emitindo) sumico[i] -= dt / SUMICO_S
            if (idade[i] >= vida[i] || sumico[i] <= 0f) remover(i) else i++
        }
        if (!emitindo) {
            acumulado = 0f
            return
        }
        val teto = if (reduzido) 8 else 24
        val deriva = if (reduzido) 2f else 6f
        acumulado = (acumulado + dt * teto / VIDA_S).coerceAtMost(1f)
        while (acumulado >= 1f && vivas < teto) {
            acumulado -= 1f
            // Para fora, com até ±35° de desvio: a componente radial é sempre positiva, então nunca entra no anel.
            val desvio = (aleatorio.nextFloat() - 0.5f) * 1.2f
            nascer(ponta, raio, ponta + desvio, deriva * (0.35f + 0.65f * aleatorio.nextFloat()), VIDA_S, 0.8f + 0.8f * aleatorio.nextFloat())
        }
    }

    /** Fim de Fase: o anel inteiro se desfaz em 40 contas, espalhadas pela volta, que somem em até 500 ms. */
    fun dissolver(raio: Float) {
        repeat(40) { k ->
            val ang = (k + aleatorio.nextFloat()) / 40f * 2f * PI.toFloat()
            nascer(ang, raio, ang, 6f * (0.3f + 0.7f * aleatorio.nextFloat()), 0.3f + 0.18f * aleatorio.nextFloat(), 2f + 1.5f * aleatorio.nextFloat())
        }
    }

    /** Cada viva: posição, raio do ponto (dp) e alfa. O alfa entra em 0.2 s, cai até 0 no fim da vida e some na pausa. */
    fun paraCada(bloco: (x: Float, y: Float, tamanho: Float, alfa: Float) -> Unit) {
        for (i in 0 until vivas) {
            val entra = (idade[i] / 0.2f).coerceAtMost(1f)
            val alfa = entra * (1f - idade[i] / vida[i]) * sumico[i]
            bloco(x[i], y[i], tamanho[i] * (1f - 0.4f * idade[i] / vida[i]), alfa.coerceIn(0f, 1f))
        }
    }

    private fun nascer(angulo: Float, raio: Float, direcao: Float, velocidade: Float, vidaS: Float, tam: Float) {
        if (vivas == CAPACIDADE) return
        val i = vivas++
        x[i] = raio * sin(angulo)
        y[i] = -raio * cos(angulo)
        vx[i] = velocidade * sin(direcao)
        vy[i] = -velocidade * cos(direcao)
        idade[i] = 0f
        vida[i] = vidaS
        tamanho[i] = tam
        sumico[i] = 1f
    }

    /** Troca com a última: a ordem não importa. */
    private fun remover(i: Int) {
        val u = --vivas
        x[i] = x[u]; y[i] = y[u]; vx[i] = vx[u]; vy[i] = vy[u]
        idade[i] = idade[u]; vida[i] = vida[u]; tamanho[i] = tamanho[u]; sumico[i] = sumico[u]
    }

    private companion object {
        const val CAPACIDADE = 64
        const val VIDA_S = 2.4f
        const val SUMICO_S = 0.6f
    }
}

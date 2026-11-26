package com.exemplo.pomodoro.ui

import org.junit.Test
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Partículas do Anel fluido (DESIGN.md §7 Anel fluido, §9). Unidades: dp e segundos, a partir do centro do anel. */
class ParticulasTest {

    private val raio = 160f
    private val quadro = 1f / 60f

    /** Avança [segundos] em quadros de 60 Hz, com a ponta no topo. Devolve o máximo de vivas visto. */
    private fun Particulas.rodar(segundos: Float, emitindo: Boolean = true, reduzido: Boolean = false): Int {
        var maximo = 0
        repeat((segundos / quadro).toInt()) {
            avancar(quadro, emitindo, ponta = 0f, raio = raio, reduzido = reduzido)
            maximo = maxOf(maximo, vivas)
        }
        return maximo
    }

    private fun Particulas.distanciasDaPonta(): List<Float> {
        val d = mutableListOf<Float>()
        paraCada { x, y, _, _ -> d += hypot(x, y + raio) }
        return d
    }

    @Test
    fun `rodando, chega perto de 24 vivas e nunca passa disso`() {
        val p = Particulas()
        val maximo = p.rodar(10f)
        assertTrue(maximo <= 24, "vivas: $maximo")
        assertTrue(p.vivas >= 20, "vivas: ${p.vivas}")
    }

    @Test
    fun `saem da ponta, derivam para fora e a no maximo 6 dp por segundo`() {
        val p = Particulas()
        p.rodar(0.5f)
        assertTrue(p.vivas > 0)
        p.distanciasDaPonta().forEach { assertTrue(it <= 6f * 0.5f + 0.01f, "distância da ponta: $it") }
        p.paraCada { x, y, _, _ -> assertTrue(hypot(x, y) >= raio - 0.01f, "entrou no anel") }
    }

    @Test
    fun `pausado, a emissao para e as vivas somem em 600 ms`() {
        val p = Particulas()
        p.rodar(5f)
        val antes = mutableListOf<Float>().also { l -> p.paraCada { _, _, _, a -> l += a } }.max()
        p.rodar(0.3f, emitindo = false)
        val meio = mutableListOf<Float>().also { l -> p.paraCada { _, _, _, a -> l += a } }.max()
        assertTrue(meio < antes * 0.6f, "alfa no meio do sumiço: $meio de $antes")
        p.rodar(0.35f, emitindo = false)
        assertEquals(0, p.vivas)
    }

    @Test
    fun `movimento reduzido, ate 8 vivas e deriva de 2 dp por segundo`() {
        val p = Particulas()
        assertTrue(p.rodar(10f, reduzido = true) <= 8)
        assertTrue(p.vivas >= 6, "vivas: ${p.vivas}")
        val q = Particulas()
        q.rodar(1f, reduzido = true)
        assertTrue(q.vivas > 0)
        q.distanciasDaPonta().forEach { assertTrue(it <= 2f * 1f + 0.01f, "distância da ponta: $it") }
    }

    @Test
    fun `no fim de Fase, o anel se dissolve em 40 particulas que somem em 500 ms`() {
        val p = Particulas()
        p.dissolver(raio)
        assertEquals(40, p.vivas)
        var acima = 0
        var abaixo = 0
        p.paraCada { x, y, _, _ ->
            assertEquals(raio, hypot(x, y), 1f)
            if (y < 0) acima++ else abaixo++
        }
        assertTrue(acima >= 10 && abaixo >= 10, "espalhadas pelo anel: $acima acima, $abaixo abaixo")
        p.rodar(0.51f, emitindo = false)
        assertEquals(0, p.vivas)
    }
}

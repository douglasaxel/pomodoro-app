package com.exemplo.pomodoro.ui

import org.junit.Test
import kotlin.test.assertEquals

/** O deslocamento do Fundo no Vidro com parallax (DESIGN.md §7). */
class FiltroParallaxTest {

    @Test
    fun `comeca no centro`() {
        val f = FiltroParallax(limiteRad = 0.2f, neutroS = 1f)
        assertEquals(0f, f.x, 0.0001f)
        assertEquals(0f, f.y, 0.0001f)
    }

    @Test
    fun `inclinar ate o limite leva o Fundo ao extremo, no sentido contrario`() {
        val f = FiltroParallax(limiteRad = 0.2f, neutroS = 1f)
        f.somar(giroX = 0.2f, giroY = -0.1f, dtS = 0f)
        assertEquals(-1f, f.x, 0.0001f)
        assertEquals(0.5f, f.y, 0.0001f)
    }

    @Test
    fun `passar do limite para no extremo, e voltar responde na hora`() {
        val f = FiltroParallax(limiteRad = 0.2f, neutroS = 1f)
        f.somar(giroX = 0.6f, giroY = 0f, dtS = 0f)
        assertEquals(-1f, f.x, 0.0001f)
        f.somar(giroX = -0.1f, giroY = 0f, dtS = 0f) // sem zona morta: o excesso não fica guardado
        assertEquals(-0.5f, f.x, 0.0001f)
    }

    @Test
    fun `segurar inclinado traz o Fundo de volta ao centro no tempo do neutro`() {
        val f = FiltroParallax(limiteRad = 0.2f, neutroS = 3f)
        f.somar(giroX = 0.1f, giroY = 0.1f, dtS = 0f)
        repeat(5) { f.somar(0f, 0f, dtS = 0.02f) } // 0,1 s depois: o parallax ainda está quase todo lá
        assertEquals(-0.5f, f.x, 0.05f)
        repeat(145) { f.somar(0f, 0f, dtS = 0.02f) } // 3 s no total: chegou à postura nova
        assertEquals(0f, f.x, 0.025f)
        assertEquals(0f, f.y, 0.025f)
    }
}

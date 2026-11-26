package com.exemplo.pomodoro.dados

import org.junit.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Véu automático (DESIGN.md §3, Contraste mínimo). */
class VeuTest {

    private val branco = 0xFFFFFFFF.toInt()
    private val preto = 0xFF000000.toInt()

    @Test
    fun `luminancia relativa do cinza 808080`() {
        assertEquals(0.2159f, luminancia(0xFF808080.toInt()), 0.001f)
    }

    @Test
    fun `fundo branco pede o Veu de cerca de 0,65`() {
        assertEquals(0.65f, alfaVeu(Fundo.Cor(branco), ajuste = 0f), 0.01f)
    }

    @Test
    fun `o texto secundario fica a 4,5 para 1 sobre qualquer cinza ja com o Veu`() {
        for (g in 0..255) {
            val a = alfaVeu(Fundo.Cor(cinza(g.toFloat())), ajuste = 0f)
            // O Véu preto e o branco a 72% se misturam no sRGB codificado, como no Canvas.
            val veu = g * (1f - a)
            val texto = 0.72f * 255f + 0.28f * veu
            val c = (luminancia(cinza(texto)) + 0.05f) / (luminancia(cinza(veu)) + 0.05f)
            assertTrue(c >= 4.5f, "cinza $g: Véu $a, contraste $c")
        }
    }

    private fun cinza(v: Float): Int = v.roundToInt().let { 0xFF000000.toInt() or (it shl 16) or (it shl 8) or it }

    @Test
    fun `fundo preto dispensa o Veu`() {
        assertEquals(0f, alfaVeu(Fundo.Cor(preto), ajuste = 0f), 0.0001f)
    }

    @Test
    fun `o ajuste soma em cima do minimo e para em mais 0,35`() {
        assertEquals(0.65f + 0.2f, alfaVeu(Fundo.Cor(branco), ajuste = 0.2f), 0.01f)
        assertEquals(0.65f + 0.35f, alfaVeu(Fundo.Cor(branco), ajuste = 0.9f), 0.01f)
    }

    @Test
    fun `o ajuste nunca desce abaixo do minimo`() {
        assertEquals(0.65f, alfaVeu(Fundo.Cor(branco), ajuste = -0.3f), 0.01f)
    }

    @Test
    fun `no Gradiente vivo vale a cor mais clara`() {
        assertEquals(0.65f, alfaVeu(Fundo.Gradiente(listOf(preto, branco, preto)), ajuste = 0f), 0.01f)
    }

    @Test
    fun `na foto vale o brilho guardado na importacao`() {
        assertEquals(0.65f, alfaVeu(Fundo.Foto("fundos/a.jpg", brilho = 1f), ajuste = 0f), 0.01f)
    }

    @Test
    fun `o percentil 90 nao deixa a media esconder pontos claros`() {
        val vinteClaros = IntArray(100) { if (it < 20) branco else preto }
        assertEquals(1f, percentil90(vinteClaros), 0.001f)
    }

    @Test
    fun `o percentil 90 ignora os 10 por cento mais claros`() {
        val cincoClaros = IntArray(100) { if (it < 5) branco else preto }
        assertEquals(0f, percentil90(cincoClaros), 0.001f)
    }
}

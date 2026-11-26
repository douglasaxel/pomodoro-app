package com.exemplo.pomodoro.dados

import org.junit.Test
import kotlin.test.assertEquals

/** Véu automático (DESIGN.md §3, Contraste mínimo). */
class VeuTest {

    private val branco = 0xFFFFFFFF.toInt()
    private val preto = 0xFF000000.toInt()

    @Test
    fun `luminancia relativa do cinza 808080`() {
        assertEquals(0.2159f, luminancia(0xFF808080.toInt()), 0.001f)
    }

    @Test
    fun `fundo branco pede o Veu de cerca de 0,54`() {
        assertEquals(0.54f, alfaVeu(Fundo.Cor(branco), ajuste = 0f), 0.01f)
    }

    @Test
    fun `fundo preto dispensa o Veu`() {
        assertEquals(0f, alfaVeu(Fundo.Cor(preto), ajuste = 0f), 0.0001f)
    }

    @Test
    fun `o ajuste soma em cima do minimo e para em mais 0,35`() {
        assertEquals(0.54f + 0.2f, alfaVeu(Fundo.Cor(branco), ajuste = 0.2f), 0.01f)
        assertEquals(0.54f + 0.35f, alfaVeu(Fundo.Cor(branco), ajuste = 0.9f), 0.01f)
    }

    @Test
    fun `o ajuste nunca desce abaixo do minimo`() {
        assertEquals(0.54f, alfaVeu(Fundo.Cor(branco), ajuste = -0.3f), 0.01f)
    }

    @Test
    fun `no Gradiente vivo vale a cor mais clara`() {
        assertEquals(0.54f, alfaVeu(Fundo.Gradiente(listOf(preto, branco, preto)), ajuste = 0f), 0.01f)
    }

    @Test
    fun `na foto vale o brilho guardado na importacao`() {
        assertEquals(0.54f, alfaVeu(Fundo.Foto("fundos/a.jpg", brilho = 1f), ajuste = 0f), 0.01f)
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

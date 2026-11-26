package com.exemplo.pomodoro.dados

import com.exemplo.pomodoro.motor.TipoFase
import org.junit.Test
import kotlin.test.assertEquals

/** Cor de destaque (DESIGN.md §3). */
class DestaqueTest {

    @Test
    fun `vermelho puro em Oklab, pelos valores de referencia`() {
        // Valores publicados por Björn Ottosson para #FF0000.
        val c = argbParaOklab(0xFFFF0000.toInt())
        assertEquals(0.6279f, c.l, 0.001f)
        assertEquals(0.2249f, c.a, 0.001f)
        assertEquals(0.1258f, c.b, 0.001f)
    }

    @Test
    fun `Oklab volta ao mesmo sRGB`() {
        val coral = 0xFFFF8A65.toInt()
        assertEquals(coral, oklabParaArgb(argbParaOklab(coral)))
    }

    private val vermelho = 0xFFE53935.toInt()
    private val cinza = 0xFF808080.toInt()
    private val azulEscuro = 0xFF101A36.toInt()

    private fun graus(argb: Int) = Math.toDegrees(argbParaOklab(argb).matiz.toDouble()).toFloat()

    /** Normalizada (§3): L entre 0.70 e 0.82 e croma até 0.17 (com a folga do arredondamento para 8 bits). */
    private fun assertNormalizada(argb: Int) {
        val c = argbParaOklab(argb)
        assert(c.l in 0.695f..0.825f) { "L ${c.l}" }
        assert(c.croma <= 0.175f) { "croma ${c.croma}" }
    }

    @Test
    fun `cor solida - o Foco e a propria cor, normalizada, com a mesma matiz`() {
        val (foco, _, _) = destaquesDoFundo(Fundo.Cor(vermelho))
        assertNormalizada(foco)
        assertEquals(graus(vermelho), graus(foco), 3f)
    }

    @Test
    fun `cor solida - as Pausas sao variacoes diferentes da mesma cor`() {
        val d = destaquesDoFundo(Fundo.Cor(vermelho))
        assertEquals(3, d.toSet().size)
        d.forEach(::assertNormalizada)
    }

    @Test
    fun `sem cor nenhuma no Fundo, vale o padrao`() {
        assertEquals(DESTAQUES_PADRAO, destaquesDoFundo(Fundo.Cor(cinza)))
        assertEquals(DESTAQUES_PADRAO, destaquesDoFundo(Fundo.Gradiente(listOf(cinza, 0xFF000000.toInt()))))
    }

    @Test
    fun `gradiente - o Foco vem da cor mais viva da paleta`() {
        val (foco, _, _) = destaquesDoFundo(Fundo.Gradiente(listOf(cinza, azulEscuro, vermelho)))
        assertEquals(graus(vermelho), graus(foco), 3f)
    }

    @Test
    fun `arte da Biblioteca - sai da paleta declarada, normalizada`() {
        val d = destaquesDoFundo(Fundo.Biblioteca("aurora"))
        assertEquals(3, d.toSet().size)
        d.forEach(::assertNormalizada)
    }

    @Test
    fun `foto - segue os swatches vibrant, light vibrant e muted, nessa ordem`() {
        val verde = 0xFF2E9A4A.toInt()
        val azul = 0xFF3A6FD8.toInt()
        val (foco, curta, longa) = destaquesDoFundo(Fundo.Foto("f.jpg", 0.1f, listOf(vermelho, verde, azul)))
        assertEquals(graus(vermelho), graus(foco), 3f)
        assertEquals(graus(verde), graus(curta), 3f)
        assertEquals(graus(azul), graus(longa), 3f)
    }

    @Test
    fun `foto sem light vibrant - a Pausa curta vira uma variacao do Foco`() {
        val d = destaquesDoFundo(Fundo.Foto("f.jpg", 0.1f, listOf(vermelho, null, null)))
        assertEquals(3, d.toSet().size)
    }

    @Test
    fun `foto importada antes da extracao - vale o padrao`() {
        assertEquals(DESTAQUES_PADRAO, destaquesDoFundo(Fundo.Foto("f.jpg", 0.1f)))
    }

    @Test
    fun `o Perfil usa a cor escolhida para cada tipo de Fase`() {
        val a = Aparencia(destaqueFoco = vermelho, destaquePausaCurta = cinza, destaquePausaLonga = azulEscuro)
        assertEquals(listOf(vermelho, cinza, azulEscuro), TipoFase.entries.map(a::destaque))
    }

    @Test
    fun `o Perfil novo comeca com o padrao`() {
        assertEquals(DESTAQUES_PADRAO, TipoFase.entries.map(Aparencia()::destaque))
    }

    @Test
    fun `com Do Fundo ligado, a cor vem do Fundo e a escolhida fica guardada`() {
        val a = Aparencia(fundo = Fundo.Cor(vermelho), destaqueDoFundo = true, destaqueFoco = cinza)
        assertEquals(destaquesDoFundo(Fundo.Cor(vermelho)), TipoFase.entries.map(a::destaque))
        assertEquals(cinza, a.copy(destaqueDoFundo = false).destaque(TipoFase.FOCO))
    }

    @Test
    fun `texto sobre a Cor de destaque - o que der mais contraste`() {
        assertEquals(TEXTO_ESCURO, textoSobre(0xFFFF8A65.toInt())) // coral
        assertEquals(TEXTO_CLARO, textoSobre(azulEscuro))
    }
}

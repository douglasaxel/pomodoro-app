package com.exemplo.pomodoro.dados

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Biblioteca (ticket 08): a arte como opção do Fundo. */
class BibliotecaTest {

    private val conv = Conversores()

    @Test
    fun `a arte da Biblioteca e guardada pelo identificador e volta igual`() {
        val texto = conv.fundoParaTexto(Fundo.Biblioteca("dunas"))
        assertEquals("""{"type":"biblioteca","arte":"dunas"}""", texto)
        assertEquals(Fundo.Biblioteca("dunas"), conv.textoParaFundo(texto))
    }

    @Test
    fun `o Perfil padrao abre com uma arte da Biblioteca`() {
        val fundo = Aparencia().fundo
        assertTrue(fundo is Fundo.Biblioteca && ARTES.any { it.id == fundo.arte }, "Fundo padrão: $fundo")
    }

    @Test
    fun `um Perfil ja gravado com Gradiente vivo continua com o mesmo Gradiente`() {
        val gravado = """{"type":"gradiente","cores":[-13751701,-15771029]}"""
        assertEquals(Fundo.Gradiente(listOf(0xFF2E2A6B.toInt(), 0xFF0F5A6B.toInt())), conv.textoParaFundo(gravado))
    }

    @Test
    fun `uma arte que nao existe mais cai na primeira arte`() {
        assertEquals(ARTES.first().id, arte("arte-removida").id)
        assertEquals(alfaVeu(Fundo.Biblioteca(ARTES.first().id), 0f), alfaVeu(Fundo.Biblioteca("arte-removida"), ajuste = 0f))
    }

    @Test
    fun `a Biblioteca tem de 6 a 8 artes com identificadores unicos`() {
        assertTrue(ARTES.size in 6..8)
        assertEquals(ARTES.size, ARTES.map { it.id }.toSet().size)
    }

    @Test
    fun `cada arte declara uma paleta que cabe no uniform cores`() {
        ARTES.forEach { assertTrue(it.paleta.size in 2..Fundo.CORES_MAX, "${it.id}: ${it.paleta.size} cores") }
    }

    @Test
    fun `o Veu automatico das artes fica leve`() {
        ARTES.forEach { assertTrue(alfaVeu(Fundo.Biblioteca(it.id), ajuste = 0f) <= 0.25f, "${it.id} pede Véu forte") }
    }

    @Test
    fun `o ajuste do Veu tambem vale para a arte`() {
        val minimo = alfaVeu(Fundo.Biblioteca("aurora"), ajuste = 0f)
        assertEquals(minimo + 0.2f, alfaVeu(Fundo.Biblioteca("aurora"), ajuste = 0.2f), 0.0001f)
    }
}

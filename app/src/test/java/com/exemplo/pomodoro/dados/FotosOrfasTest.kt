package com.exemplo.pomodoro.dados

import org.junit.Test
import kotlin.test.assertEquals

class FotosOrfasTest {

    private fun foto(caminho: String) = Fundo.Foto(caminho, brilho = 0.5f)

    @Test
    fun `a foto que nenhum Perfil usa e apagada`() {
        val orfas = fotosOrfas(listOf("fundos/a.jpg", "fundos/b.jpg"), emUso = listOf(foto("fundos/b.jpg"), Fundo.PADRAO))
        assertEquals(listOf("fundos/a.jpg"), orfas)
    }

    @Test
    fun `a foto compartilhada por um Perfil duplicado fica`() {
        val orfas = fotosOrfas(listOf("fundos/a.jpg"), emUso = listOf(foto("fundos/a.jpg"), foto("fundos/a.jpg")))
        assertEquals(emptyList(), orfas)
    }

    @Test
    fun `o Fundo sobrevive a ida e volta pelo banco`() {
        val c = Conversores()
        listOf(foto("fundos/a.jpg"), Fundo.Cor(0xFF112233.toInt()), Fundo.PADRAO).forEach {
            assertEquals(it, c.textoParaFundo(c.fundoParaTexto(it)))
        }
        assertEquals(foto("x"), c.textoParaFundo("""{"type":"foto","caminho":"x","brilho":0.5}"""))
    }
}

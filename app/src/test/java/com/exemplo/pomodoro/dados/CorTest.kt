package com.exemplo.pomodoro.dados

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Conversões do seletor livre de cor: HSV, hex e ARGB. */
class CorTest {

    private val coral = 0xFFFF8A65.toInt()

    @Test
    fun `hex de 6 digitos vira ARGB opaco, com ou sem cerquilha`() {
        assertEquals(coral, hexParaArgb("FF8A65"))
        assertEquals(coral, hexParaArgb("#ff8a65"))
        assertEquals(coral, hexParaArgb("  #FF8A65 "))
    }

    @Test
    fun `ARGB vira hex de 6 digitos em maiusculas, sem o alfa`() {
        assertEquals("FF8A65", argbParaHex(coral))
        assertEquals("0B0B10", argbParaHex(0x800B0B10.toInt()))
    }

    @Test
    fun `HSV das cores puras vira o ARGB esperado`() {
        assertEquals(0xFFFF0000.toInt(), Hsv(0f, 1f, 1f).argb())
        assertEquals(0xFFFFFF00.toInt(), Hsv(60f, 1f, 1f).argb())
        assertEquals(0xFF00FF00.toInt(), Hsv(120f, 1f, 1f).argb())
        assertEquals(0xFF0000FF.toInt(), Hsv(240f, 1f, 1f).argb())
        assertEquals(0xFFFF0000.toInt(), Hsv(360f, 1f, 1f).argb())
        assertEquals(0xFFFFFFFF.toInt(), Hsv(200f, 0f, 1f).argb())
        assertEquals(0xFF000000.toInt(), Hsv(200f, 0.7f, 0f).argb())
    }

    @Test
    fun `ARGB vira HSV e o coral tem matiz 14,4, saturacao 0,604 e brilho 1`() {
        val hsv = argbParaHsv(coral)
        assertEquals(14.42f, hsv.h, 0.01f)
        assertEquals(0.604f, hsv.s, 0.001f)
        assertEquals(1f, hsv.v, 0.0001f)
    }

    @Test
    fun `cinza e preto ficam com saturacao 0`() {
        assertEquals(Hsv(0f, 0f, 0f), argbParaHsv(0xFF000000.toInt()))
        assertEquals(0f, argbParaHsv(0xFF808080.toInt()).s)
    }

    @Test
    fun `ida e volta entre ARGB e HSV preserva a cor`() {
        listOf(coral, 0xFF0B0B10.toInt(), 0xFF6FD8C0.toInt(), 0xFFA99BFF.toInt(), 0xFFF4EFE6.toInt(), 0xFFFFFFFF.toInt())
            .forEach { assertEquals(argbParaHex(it), argbParaHex(argbParaHsv(it).argb())) }
    }

    @Test
    fun `hex invalido nao vira cor`() {
        listOf("", "#", "FF8A6", "FF8A655", "GG8A65", "#FF 8A6", "-F8A65", "+F8A65", "80FF8A65").forEach {
            assertNull(hexParaArgb(it), "\"$it\" deveria ser inválido")
        }
    }

    private val menta = 0xFF6FD8C0.toInt()

    @Test
    fun `mistura em OkLCh - do coral ao menta, o meio continua vivo, sem passar pelo caqui`() {
        val meio = argbParaOklab(misturarOklch(coral, menta, 0.5f))
        val menor = minOf(argbParaOklab(coral).croma, argbParaOklab(menta).croma)
        // A reta em Oklab passaria por croma ~0.05 (um cáqui). O arco de matiz mantém o croma.
        assert(meio.croma >= 0.9f * menor) { "croma ${meio.croma}" }
        // Matiz no arco curto: entre o coral (~40°) e o menta (~170°).
        val h = Math.toDegrees(meio.matiz.toDouble())
        assert(h in 80.0..130.0) { "matiz $h" }
    }

    @Test
    fun `mistura em OkLCh - nas pontas, as proprias cores`() {
        assertEquals(coral, misturarOklch(coral, menta, 0f))
        assertEquals(menta, misturarOklch(coral, menta, 1f))
    }

    @Test
    fun `mistura em OkLCh - um cinza na ponta nao inventa matiz`() {
        val cinza = 0xFF808080.toInt()
        val meio = argbParaOklab(misturarOklch(cinza, menta, 0.5f))
        val hMenta = Math.toDegrees(argbParaOklab(menta).matiz.toDouble())
        assertEquals(hMenta, Math.toDegrees(meio.matiz.toDouble()), 3.0)
    }
}

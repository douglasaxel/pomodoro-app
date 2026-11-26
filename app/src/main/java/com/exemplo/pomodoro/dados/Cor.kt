package com.exemplo.pomodoro.dados

// Kotlin puro, sem android.*: os testes da JVM usam estas funções direto.

/** Matiz de 0 a 360, saturação e brilho de 0 a 1. É o estado do seletor livre de cor. */
data class Hsv(val h: Float, val s: Float, val v: Float) {
    /** ARGB opaco. */
    fun argb(): Int {
        fun canal(n: Int): Int {
            val k = (n + h / 60f) % 6f
            val c = v - v * s * maxOf(0f, minOf(k, 4f - k, 1f))
            return (c * 255f + 0.5f).toInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or (canal(5) shl 16) or (canal(3) shl 8) or canal(1)
    }
}

/** Ignora o alfa. Sem saturação (cinza, preto, branco), a matiz é 0. */
fun argbParaHsv(argb: Int): Hsv {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val d = max - minOf(r, g, b)
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d + 6f) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Hsv(h, if (max == 0f) 0f else d / max, max)
}

/** "RRGGBB" em maiúsculas, sem o alfa. */
fun argbParaHex(argb: Int): String = "%06X".format(argb and 0xFFFFFF)

private val HEX = Regex("#?[0-9A-Fa-f]{6}")

/** "#RRGGBB" ou "RRGGBB" vira ARGB opaco. Qualquer outra coisa é null. */
fun hexParaArgb(texto: String): Int? {
    val t = texto.trim()
    if (!HEX.matches(t)) return null
    return t.removePrefix("#").toInt(16) or 0xFF000000.toInt()
}

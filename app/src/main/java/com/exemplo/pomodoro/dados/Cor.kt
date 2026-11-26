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

/** Cor em Oklab: [l] de 0 a 1; [a] e [b] em torno de 0. É o espaço em que a Cor de destaque é normalizada (§3). */
data class Oklab(val l: Float, val a: Float, val b: Float) {
    val croma: Float get() = kotlin.math.hypot(a, b)
    /** Em radianos. */
    val matiz: Float get() = kotlin.math.atan2(b, a)
}

/** Canal sRGB de 0 a 255 para a luz linear, de 0 a 1. */
internal fun linear(c: Int): Float {
    val v = c / 255f
    return if (v <= 0.04045f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
}

/** Luz linear de 0 a 1 para o valor sRGB codificado, de 0 a 1. */
internal fun srgb(v: Float): Float =
    if (v <= 0.0031308f) 12.92f * v else 1.055f * Math.pow(v.toDouble(), 1 / 2.4).toFloat() - 0.055f

/** Ignora o alfa. Matrizes de Björn Ottosson. */
fun argbParaOklab(argb: Int): Oklab {
    val r = linear(argb shr 16 and 0xFF)
    val g = linear(argb shr 8 and 0xFF)
    val b = linear(argb and 0xFF)
    val l = Math.cbrt((0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b))
    val m = Math.cbrt((0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b))
    val s = Math.cbrt((0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b))
    return Oklab(
        (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat(),
        (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat(),
        (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat(),
    )
}

/** sRGB linear, sem prender: fora do gamut, algum canal sai de 0..1. */
private fun oklabParaLinear(c: Oklab): FloatArray {
    val l = (c.l + 0.3963377774f * c.a + 0.2158037573f * c.b).let { it * it * it }
    val m = (c.l - 0.1055613458f * c.a - 0.0638541728f * c.b).let { it * it * it }
    val s = (c.l - 0.0894841775f * c.a - 1.2914855480f * c.b).let { it * it * it }
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
    )
}

/** ARGB opaco. Fora do sRGB, o croma cai até caber, com a mesma matiz e o mesmo L. */
fun oklabParaArgb(c: Oklab): Int {
    var k = c
    var rgb = oklabParaLinear(k)
    var passos = 0
    while (passos++ < 40 && rgb.any { it !in -0.0001f..1.0001f }) {
        k = k.copy(a = k.a * 0.95f, b = k.b * 0.95f)
        rgb = oklabParaLinear(k)
    }
    fun canal(v: Float) = (srgb(v.coerceIn(0f, 1f)) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return 0xFF000000.toInt() or (canal(rgb[0]) shl 16) or (canal(rgb[1]) shl 8) or canal(rgb[2])
}

/**
 * Mistura em OkLCh (§5.6): L e croma em linha reta, a matiz pelo arco curto. A reta em Oklab passaria pelo cinza
 * entre matizes distantes (coral → menta vira cáqui no meio). Uma ponta sem croma não tem matiz: vale a da outra.
 * Ignora o alfa; [t] de 0 a 1.
 */
fun misturarOklch(de: Int, para: Int, t: Float): Int {
    if (t <= 0f) return de or 0xFF000000.toInt()
    if (t >= 1f) return para or 0xFF000000.toInt()
    val a = argbParaOklab(de)
    val b = argbParaOklab(para)
    val semMatiz = 0.02f
    val ha = if (a.croma < semMatiz) b.matiz else a.matiz
    val hb = if (b.croma < semMatiz) ha else b.matiz
    var dh = hb - ha
    if (dh > Math.PI) dh -= (2 * Math.PI).toFloat()
    if (dh < -Math.PI) dh += (2 * Math.PI).toFloat()
    val h = ha + dh * t
    val c = a.croma + (b.croma - a.croma) * t
    return oklabParaArgb(Oklab(a.l + (b.l - a.l) * t, c * kotlin.math.cos(h), c * kotlin.math.sin(h)))
}

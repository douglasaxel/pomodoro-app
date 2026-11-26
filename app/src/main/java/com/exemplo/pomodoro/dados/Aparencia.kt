package com.exemplo.pomodoro.dados

import androidx.room.TypeConverter
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// Kotlin puro, sem android.*: os testes da JVM usam estas funções direto.

/**
 * O que aparece atrás do timer. Guardado no Perfil como JSON.
 * O `@SerialName` é fixo: renomear a classe não pode quebrar os dados salvos.
 */
@Serializable
sealed interface Fundo {
    @Serializable @SerialName("cor")
    data class Cor(val argb: Int) : Fundo

    /** Gradiente vivo: de 2 a [CORES_MAX] cores. */
    @Serializable @SerialName("gradiente")
    data class Gradiente(val cores: List<Int>) : Fundo

    /**
     * [caminho] é relativo a `filesDir`. [brilho] é o percentil 90 da luminância, medido na importação.
     * [destaques]: os swatches vibrant, light vibrant e muted do `androidx.palette`, nessa ordem (null se a foto não tem).
     * Vazio nas fotos importadas antes do ticket 09.
     */
    @Serializable @SerialName("foto")
    data class Foto(val caminho: String, val brilho: Float, val destaques: List<Int?> = emptyList()) : Fundo

    /** Arte da Biblioteca. Guarda só o identificador: a arte em si vem de [arte]. */
    @Serializable @SerialName("biblioteca")
    data class Biblioteca(val arte: String) : Fundo

    companion object {
        const val CORES_MAX = 6
        /** Fundo do Perfil padrão e dos Perfis novos. Os Perfis já gravados guardam o próprio Fundo e não mudam. */
        val PADRAO: Fundo = Biblioteca("aurora")
        /** As cores ao trocar para o Gradiente vivo. */
        val GRADIENTE_PADRAO: Fundo = Gradiente(
            listOf(0xFF2E2A6B.toInt(), 0xFF0F5A6B.toInt(), 0xFF6A2C61.toInt(), 0xFF8A3B2E.toInt()),
        )
    }
}

/**
 * A forma como o timer é desenhado. O Room guarda o nome: não renomeie.
 * O Anel fluido e o Vidro com parallax entram nos tickets 10 e 11.
 */
enum class EstiloVisual { BLOB_COM_VIDRO, BLOB_PURO, VIDRO_COM_PARALLAX }

/** Parte do Perfil com a aparência: Fundo, Véu, Estilo visual e Cor de destaque. */
data class Aparencia(
    val fundo: Fundo = Fundo.PADRAO,
    /** Somado ao Véu automático, de 0 a [VEU_AJUSTE_MAX]. Zero é "Automático". */
    val veuAjuste: Float = 0f,
    val movimentoFundo: Boolean = true,
    val estilo: EstiloVisual = EstiloVisual.BLOB_COM_VIDRO,
    /** Ligado: a Cor de destaque vem do Fundo. As cores escolhidas abaixo ficam guardadas para quando desligar. */
    val destaqueDoFundo: Boolean = false,
    val destaqueFoco: Int = DESTAQUES_PADRAO[0],
    val destaquePausaCurta: Int = DESTAQUES_PADRAO[1],
    val destaquePausaLonga: Int = DESTAQUES_PADRAO[2],
) {
    /** A Cor de destaque (ARGB) do tipo de Fase. */
    fun destaque(tipo: TipoFase): Int =
        if (destaqueDoFundo) destaquesDoFundo(fundo)[tipo.ordinal]
        else when (tipo) {
            TipoFase.FOCO -> destaqueFoco
            TipoFase.PAUSA_CURTA -> destaquePausaCurta
            TipoFase.PAUSA_LONGA -> destaquePausaLonga
        }

    fun comDestaque(tipo: TipoFase, argb: Int) = when (tipo) {
        TipoFase.FOCO -> copy(destaqueFoco = argb)
        TipoFase.PAUSA_CURTA -> copy(destaquePausaCurta = argb)
        TipoFase.PAUSA_LONGA -> copy(destaquePausaLonga = argb)
    }
}

const val VEU_AJUSTE_MAX = 0.35f

private val json = Json { ignoreUnknownKeys = true }

class Conversores {
    @TypeConverter fun fundoParaTexto(f: Fundo): String = json.encodeToString(Fundo.serializer(), f)
    @TypeConverter fun textoParaFundo(s: String): Fundo = json.decodeFromString(Fundo.serializer(), s)
}

/** As [candidatas] (caminhos relativos a `filesDir`) que nenhum Fundo [emUso] referencia. */
fun fotosOrfas(candidatas: Collection<String>, emUso: List<Fundo>): List<String> {
    val usadas = emUso.mapNotNullTo(HashSet()) { (it as? Fundo.Foto)?.caminho }
    return candidatas.filter { it !in usadas }
}

/** Luminância relativa (WCAG) de uma cor sRGB, de 0 a 1. */
fun luminancia(argb: Int): Float {
    fun canal(c: Int): Float {
        val v = c / 255f
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * canal(argb shr 16 and 0xFF) + 0.7152f * canal(argb shr 8 and 0xFF) + 0.0722f * canal(argb and 0xFF)
}

/** Percentil 90 da luminância dos pixels. A média esconderia os pontos claros (§3). */
fun percentil90(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0f
    val l = FloatArray(pixels.size) { luminancia(pixels[it]) }.apply { sort() }
    return l[ceil(l.size * 0.9).toInt() - 1]
}

/**
 * Luminância de referência do Fundo. No Gradiente vivo, a cor mais clara: a mistura nunca passa dela.
 * ponytail: medida do Fundo inteiro, não da área do texto. Meça por área se o Véu ficar forte demais.
 */
fun brilho(f: Fundo): Float = when (f) {
    is Fundo.Cor -> luminancia(f.argb)
    is Fundo.Gradiente -> f.cores.maxOfOrNull(::luminancia) ?: 0f
    is Fundo.Foto -> f.brilho
    is Fundo.Biblioteca -> arte(f.arte).paleta.maxOf(::luminancia) // a arte só mistura as cores da paleta
}

/** Limite de luminância para texto branco a 4.5:1. Vale para todo o Timer: o rótulo da Fase é texto pequeno. */
private const val L_LIMITE = 0.183f

/**
 * Alfa do Véu preto (§3): o mínimo para o contraste, `1 − (Llimite / L90)^(1/2.2)`, mais o ajuste do Perfil.
 * O ajuste fica entre 0 e [VEU_AJUSTE_MAX] e nunca desce abaixo do mínimo.
 */
fun alfaVeu(f: Fundo, ajuste: Float): Float {
    val l90 = brilho(f)
    val minimo = if (l90 <= L_LIMITE) 0f else 1f - (L_LIMITE / l90).pow(1f / 2.2f)
    return (minimo + ajuste.coerceIn(0f, VEU_AJUSTE_MAX)).coerceAtMost(1f)
}

/** Cor de destaque padrão: Foco, Pausa curta e Pausa longa (§3). */
val DESTAQUES_PADRAO = listOf(0xFFFF8A65.toInt(), 0xFF6FD8C0.toInt(), 0xFFA99BFF.toInt())

/** Abaixo deste croma, a cor é cinza: não tem matiz para dar ao destaque. */
private const val CROMA_MIN = 0.02f
private const val CROMA_MAX = 0.17f
private const val L_MAX = 0.82f

/** Quanto a matiz das Pausas gira, quando o Fundo só dá uma cor. */
private const val GIRO = 0.61f // 35°

/**
 * Cor de destaque automática, para Foco, Pausa curta e Pausa longa (§3):
 * - Foto: os swatches do `androidx.palette` (vibrant, light vibrant, muted).
 * - Arte e Gradiente vivo: a paleta declarada. A cor mais viva é o Foco, a mais clara das outras é a Pausa curta,
 *   e a mais suave do que sobra é a Pausa longa.
 * - Cor sólida: a própria cor no Foco, e a matiz girada ±35° nas Pausas.
 * O que falta vira variação do Foco. Um Fundo sem cor (cinza) fica com o padrão.
 */
fun destaquesDoFundo(f: Fundo): List<Int> {
    fun comCor(argb: Int?) = argb?.let(::argbParaOklab)?.takeIf { it.croma >= CROMA_MIN }
    val (foco, curta, longa) = when (f) {
        is Fundo.Cor -> Triple(comCor(f.argb), null, null)
        is Fundo.Foto -> f.destaques.map(::comCor).let { Triple(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2)) }
        is Fundo.Gradiente -> daPaleta(f.cores.mapNotNull(::comCor))
        is Fundo.Biblioteca -> daPaleta(arte(f.arte).paleta.mapNotNull(::comCor))
    }
    val base = foco ?: curta ?: longa ?: return DESTAQUES_PADRAO
    // A Cor de destaque fica pelo menos 0.15 de L acima do Fundo já escurecido pelo Véu mínimo.
    val fundoEscuro = brilho(f) * (1f - alfaVeu(f, 0f)).pow(2.2f)
    val lMin = maxOf(0.70f, Math.cbrt(fundoEscuro.toDouble()).toFloat() + 0.15f)
    return listOf(base, curta ?: girar(base, GIRO), longa ?: girar(base, -GIRO)).map { normalizar(it, lMin) }
}

private fun daPaleta(cores: List<Oklab>): Triple<Oklab?, Oklab?, Oklab?> {
    val foco = cores.maxByOrNull { it.croma } ?: return Triple(null, null, null)
    val resto = cores - foco
    val curta = resto.maxByOrNull { it.l }
    return Triple(foco, curta, resto.filter { it !== curta }.minByOrNull { it.croma })
}

private fun girar(c: Oklab, rad: Float): Oklab {
    val m = c.matiz + rad
    return Oklab(c.l, c.croma * cos(m), c.croma * sin(m))
}

/** L entre [lMin] e 0.82 e croma até 0.17, para não virar neon (§3). */
private fun normalizar(c: Oklab, lMin: Float): Int {
    val k = minOf(1f, CROMA_MAX / c.croma)
    return oklabParaArgb(Oklab(c.l.coerceIn(lMin, maxOf(lMin, L_MAX)), c.a * k, c.b * k))
}

const val TEXTO_CLARO = 0xFFFFFFFF.toInt()
const val TEXTO_ESCURO = 0xFF101014.toInt()

private fun contraste(a: Int, b: Int): Float {
    val la = luminancia(a)
    val lb = luminancia(b)
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Texto sobre a Cor de destaque (o rótulo do botão primário): branco ou #101014, o que der mais contraste (§3). */
fun textoSobre(argb: Int): Int = if (contraste(argb, TEXTO_ESCURO) >= contraste(argb, TEXTO_CLARO)) TEXTO_ESCURO else TEXTO_CLARO

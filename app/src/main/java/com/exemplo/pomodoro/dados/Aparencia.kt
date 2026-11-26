package com.exemplo.pomodoro.dados

import androidx.room.TypeConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.pow

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

    /** [caminho] é relativo a `filesDir`. [brilho] é o percentil 90 da luminância, medido na importação. */
    @Serializable @SerialName("foto")
    data class Foto(val caminho: String, val brilho: Float) : Fundo

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

/** Parte do Perfil com a aparência. O Estilo visual e a Cor de destaque (ticket 09) entram aqui. */
data class Aparencia(
    val fundo: Fundo = Fundo.PADRAO,
    /** Somado ao Véu automático, de 0 a [VEU_AJUSTE_MAX]. Zero é "Automático". */
    val veuAjuste: Float = 0f,
    val movimentoFundo: Boolean = true,
)

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

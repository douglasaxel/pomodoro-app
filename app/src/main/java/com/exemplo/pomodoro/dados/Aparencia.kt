package com.exemplo.pomodoro.dados

import androidx.room.TypeConverter
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
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
 */
enum class EstiloVisual { BLOB_COM_VIDRO, BLOB_PURO, ANEL_FLUIDO, VIDRO_COM_PARALLAX }

/** Parte do Perfil com a aparência: Fundo, Véu, Estilo visual e Cor de destaque. */
data class Aparencia(
    val fundo: Fundo = Fundo.PADRAO,
    /** Somado ao Véu automático, de 0 a [VEU_AJUSTE_MAX]. Zero é "Automático". */
    val veuAjuste: Float = 0f,
    val movimentoFundo: Boolean = true,
    val estilo: EstiloVisual = EstiloVisual.BLOB_COM_VIDRO,
    /** Ligado: a Cor de destaque vem do Fundo (o padrão dos Perfis novos). As cores escolhidas abaixo ficam guardadas para quando desligar. */
    val destaqueDoFundo: Boolean = true,
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
fun luminancia(argb: Int): Float =
    0.2126f * linear(argb shr 16 and 0xFF) + 0.7152f * linear(argb shr 8 and 0xFF) + 0.0722f * linear(argb and 0xFF)

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

/**
 * Limite de luminância do Fundo para o texto secundário (branco a 72%) ficar a 4.5:1. Vale para todo o Timer:
 * o rótulo da Fase é texto pequeno. O branco puro pediria só 0.183, mas o branco a 72% mistura o Fundo.
 * ponytail: medido em cinza. Num Fundo muito saturado, o texto pega a matiz e o contraste cai um pouco.
 */
private const val L_LIMITE = 0.102f

/**
 * Alfa do Véu preto (§3): o mínimo para o contraste, `1 − srgb(Llimite) / srgb(L90)`, mais o ajuste do Perfil.
 * O Véu escala o valor sRGB codificado, não a luminância.
 * O ajuste fica entre 0 e [VEU_AJUSTE_MAX] e nunca desce abaixo do mínimo.
 */
fun alfaVeu(f: Fundo, ajuste: Float): Float {
    val l90 = brilho(f)
    val minimo = if (l90 <= L_LIMITE) 0f else 1f - srgb(L_LIMITE) / srgb(l90)
    return (minimo + ajuste.coerceIn(0f, VEU_AJUSTE_MAX)).coerceAtMost(1f)
}

/** Cor de destaque padrão: Foco, Pausa curta e Pausa longa (§3). */
val DESTAQUES_PADRAO = listOf(0xFFFF8A65.toInt(), 0xFF6FD8C0.toInt(), 0xFFA99BFF.toInt())

/** Abaixo deste croma, a cor é cinza: não tem matiz para dar ao destaque. */
private const val CROMA_MIN = 0.02f
/** Abaixo deste croma, a matiz da cor da paleta é instável demais para virar uma Fase. */
private const val CROMA_MATIZ = 0.04f
/** O destaque nunca fica abaixo deste croma: uma Pausa quase cinza parece desligada. */
private const val CROMA_PISO = 0.09f
private const val CROMA_MAX = 0.17f
private const val L_MAX = 0.82f

/** Quanto a matiz das Pausas gira, quando o Fundo não dá uma cor distinta. */
private const val GIRO = 0.79f // 45°
/** Duas Fases com matizes mais próximas que isto se confundem: a cor da paleta dá lugar a um giro. */
private const val DISTANCIA_MIN = 0.61f // 35°

/**
 * Cor de destaque automática, para Foco, Pausa curta e Pausa longa (§3):
 * - Foto: os swatches do `androidx.palette` (vibrant, light vibrant, muted).
 * - Arte e Gradiente vivo: a paleta declarada. A cor mais viva é o Foco; as Pausas são as cores da paleta de matiz
 *   mais distante do Foco (e uma da outra). Se a paleta não tem matiz distante, a Pausa é o Foco girado ±45°.
 * - Cor sólida: a própria cor no Foco, e a matiz girada ±45° nas Pausas.
 * O que falta vira variação do Foco. Um Fundo sem cor (cinza) fica com o padrão. O croma fica entre 0.09 e 0.17.
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

/** Distância entre duas matizes, em radianos, de 0 a π. */
private fun distancia(a: Oklab, b: Oklab): Float {
    val d = Math.abs(a.matiz - b.matiz) % (2 * PI.toFloat())
    return minOf(d, 2 * PI.toFloat() - d)
}

private fun daPaleta(cores: List<Oklab>): Triple<Oklab?, Oklab?, Oklab?> {
    val foco = cores.maxByOrNull { it.croma } ?: return Triple(null, null, null)
    val comMatiz = cores.filter { it !== foco && it.croma >= CROMA_MATIZ }
    val curta = comMatiz.maxByOrNull { distancia(it, foco) }?.takeIf { distancia(it, foco) >= DISTANCIA_MIN }
    val longa = (comMatiz - listOfNotNull(curta).toSet())
        .maxByOrNull { minOf(distancia(it, foco), curta?.let { c -> distancia(it, c) } ?: PI.toFloat()) }
        ?.takeIf { distancia(it, foco) >= DISTANCIA_MIN && (curta == null || distancia(it, curta) >= DISTANCIA_MIN) }
    // Sem Pausa curta distinta, ela gira para um lado; a longa, se faltar, gira para o lado que sobra.
    return Triple(foco, curta, longa ?: curta?.let { girarLonge(foco, it) })
}

/**
 * A Pausa longa que falta: a matiz no meio do Foco e da [curta], se as duas estão longe o bastante;
 * senão, o Foco girado para o lado oposto ao da [curta]. O meio evita a faixa amarelo-oliva, que fica suja.
 */
private fun girarLonge(foco: Oklab, curta: Oklab): Oklab {
    val delta = Math.atan2(Math.sin((curta.matiz - foco.matiz).toDouble()), Math.cos((curta.matiz - foco.matiz).toDouble())).toFloat()
    if (Math.abs(delta) >= 2 * DISTANCIA_MIN) return girar(foco, delta / 2f)
    return girar(foco, if (delta >= 0) -GIRO else GIRO)
}

private fun girar(c: Oklab, rad: Float): Oklab {
    val m = c.matiz + rad
    return Oklab(c.l, c.croma * cos(m), c.croma * sin(m))
}

/** L entre [lMin] e 0.82 e croma entre 0.09 e 0.17: nem cinza, nem neon (§3). */
private fun normalizar(c: Oklab, lMin: Float): Int {
    val k = c.croma.coerceIn(CROMA_PISO, CROMA_MAX) / c.croma
    return oklabParaArgb(Oklab(c.l.coerceIn(lMin, maxOf(lMin, L_MAX)), c.a * k, c.b * k))
}

const val TEXTO_CLARO = 0xFFFFFFFF.toInt()
const val TEXTO_ESCURO = 0xFF101014.toInt()

/** Razão de contraste WCAG entre duas cores opacas, de 1 a 21. */
fun contraste(a: Int, b: Int): Float {
    val la = luminancia(a)
    val lb = luminancia(b)
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Texto sobre a Cor de destaque (o rótulo do botão primário): branco ou #101014, o que der mais contraste (§3). */
fun textoSobre(argb: Int): Int = if (contraste(argb, TEXTO_ESCURO) >= contraste(argb, TEXTO_CLARO)) TEXTO_ESCURO else TEXTO_CLARO

/**
 * O miolo profundo do blob e do anel (§3): a Cor de destaque com L 0.42. Os números ficam só sobre ele (ou sobre o Fundo
 * já escurecido), nunca sobre o aro claro: o branco fica acima de 4.5:1 em qualquer cor.
 */
const val L_MIOLO = 0.42f

/** Alfa dos números do timer (§5.3). Pausado, cai pouco: o estado vem do texto "Pausado", e o contraste fica acima de 4.5:1. */
const val ALFA_NUMEROS = 0.96f
const val ALFA_NUMEROS_PAUSADO = 0.86f

/** [argb] com o L de [l] e o croma × [saturacao], em Oklab (luz na borda, miolo profundo). */
fun tomArgb(argb: Int, l: Float, saturacao: Float): Int {
    val c = argbParaOklab(argb)
    return oklabParaArgb(Oklab(l, c.a * saturacao, c.b * saturacao))
}


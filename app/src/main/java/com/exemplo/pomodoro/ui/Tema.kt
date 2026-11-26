package com.exemplo.pomodoro.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.motor.TipoFase

// Tokens do DESIGN.md. Só os que as telas usam hoje.

// §3 Cor
val TextoPrimario = Color.White.copy(alpha = 0.96f)
val TextoSecundario = Color.White.copy(alpha = 0.72f)
val TextoTerciario = Color.White.copy(alpha = 0.48f)
val BaseEscura = Color(0xFF0B0B10)
val VidroBarra = Color.Black.copy(alpha = 0.40f) // o blur de 24 dp chega com o vidro (ticket 09)
val VidroClaro = Color.White.copy(alpha = 0.10f)
val VidroClaroPressionado = Color.White.copy(alpha = 0.18f)
val BordaVidroTopo = Color.White.copy(alpha = 0.22f)
val BordaVidroBase = Color.White.copy(alpha = 0.06f)
val TextoSobreDestaque = Color(0xFF101014) // dá mais contraste que o branco nas três cores padrão

/** A Cor de destaque padrão. Na tela, vale a do Perfil: [com.exemplo.pomodoro.dados.Aparencia.destaque]. */
fun corDeDestaque(tipo: TipoFase) = Color(DESTAQUES_PADRAO[tipo.ordinal])

// §4 Movimento
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EaseMorph = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
const val DUR_SAIDA = 90
const val DUR_CURTA = 150
const val DUR_MEDIA = 250
const val DUR_FUNDO = 500
fun <T> springPress() = spring<T>(dampingRatio = 1f, stiffness = 1500f)
fun <T> springUi() = spring<T>(dampingRatio = 1f, stiffness = 440f)

// §2 Tipografia: Google Sans Flex variável, com peso, ROND e tamanho óptico por papel.
@OptIn(ExperimentalTextApi::class)
private fun googleSansFlex(peso: Int, rond: Float, tamanho: TextUnit) = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight(peso),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(peso),
            FontVariation.Setting("ROND", rond),
            FontVariation.Setting("opsz", tamanho.value),
        ),
    ),
)

/** Papel `timer`. O tamanho final vem da regra de ajuste em [TelaTimer]. */
val EstiloTimer = TextStyle(
    fontFamily = googleSansFlex(300, 100f, 88.sp),
    fontWeight = FontWeight(300),
    letterSpacing = (-0.03).em,
    lineHeight = 1.em,
    fontFeatureSettings = "tnum",
    color = TextoPrimario,
)

/** Papel `display`: sequência e totais do Histórico. */
val EstiloDisplay = TextStyle(
    fontFamily = googleSansFlex(500, 60f, 40.sp),
    fontWeight = FontWeight(500),
    fontSize = 40.sp,
    letterSpacing = (-0.02).em,
    lineHeight = 44.sp,
    fontFeatureSettings = "tnum",
    color = TextoPrimario,
)

val EstiloLabel = TextStyle(
    fontFamily = googleSansFlex(600, 30f, 14.sp),
    fontWeight = FontWeight(600),
    fontSize = 14.sp,
    letterSpacing = 0.01.em,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum",
    color = TextoSecundario,
)

val EstiloTitle = TextStyle(
    fontFamily = googleSansFlex(600, 30f, 22.sp),
    fontWeight = FontWeight(600),
    fontSize = 22.sp,
    letterSpacing = (-0.01).em,
    lineHeight = 28.sp,
)

val EstiloCaption = TextStyle(
    fontFamily = googleSansFlex(500, 0f, 12.sp),
    fontWeight = FontWeight(500),
    fontSize = 12.sp,
    letterSpacing = 0.02.em,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
    color = TextoSecundario,
)

val EstiloBody = TextStyle(
    fontFamily = googleSansFlex(400, 0f, 16.sp),
    fontWeight = FontWeight(400),
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

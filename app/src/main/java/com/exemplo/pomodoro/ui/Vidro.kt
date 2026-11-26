package com.exemplo.pomodoro.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/**
 * O Fundo (com o Véu) gravado numa GraphicsLayer. O Compose não tem blur do que está atrás:
 * o vidro redesenha esta camada, borrada, sob a própria forma (§3, camada 3).
 */
class FundoGravado(val camada: GraphicsLayer) {
    /** Onde o Fundo gravado começa, na raiz. */
    var origem by mutableStateOf(Offset.Zero)
}

/** O Fundo que o vidro borra. Nulo: o vidro fica só com a tinta e a borda. */
val LocalFundoGravado = staticCompositionLocalOf<FundoGravado?> { null }

@Composable
fun rememberFundoGravado(): FundoGravado {
    val camada = rememberGraphicsLayer()
    return remember(camada) { FundoGravado(camada) }
}

/** Desenha o conteúdo e o grava em [fundo], para o vidro. */
fun Modifier.gravarFundo(fundo: FundoGravado) = this
    .onPlaced { fundo.origem = it.positionInRoot() }
    .drawWithContent {
        fundo.camada.record { this@drawWithContent.drawContent() }
        drawLayer(fundo.camada)
    }

val VidroEscuro = Color.Black.copy(alpha = 0.24f)
private val FORMA_CARTAO = RoundedCornerShape(32.dp)

/**
 * Cartão de vidro estrutural (`vidroEscuro`, §3): o Fundo borrado em 28 dp, a tinta preta a 24% e a `bordaVidro`.
 * [embacado]: com a Fase pausada, o blur sobe para 36 dp, como se o vidro embaçasse (§7).
 * Sem sombra nem elevation: atravessariam o vidro.
 */
@Composable
fun CartaoVidro(modifier: Modifier, embacado: Boolean) {
    val fundo = LocalFundoGravado.current
    val raio by animateDpAsState(if (embacado) 36.dp else 28.dp, tween(DUR_MEDIA, easing = EaseOut), label = "blur")
    var posicao by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onPlaced { posicao = it.positionInRoot() }) {
        if (fundo != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val r = raio.toPx()
                        renderEffect = BlurEffect(r, r, TileMode.Clamp)
                        shape = FORMA_CARTAO
                        clip = true
                    }
                    .drawBehind {
                        // ponytail: lê a posição da última passada de layout. Se o cartão se mover, o Fundo vem um quadro atrás.
                        translate(fundo.origem.x - posicao.x, fundo.origem.y - posicao.y) { drawLayer(fundo.camada) }
                    },
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(VidroEscuro, FORMA_CARTAO)
                .border(1.dp, BordaVidro, FORMA_CARTAO),
        )
    }
}

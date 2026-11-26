package com.exemplo.pomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.Hsv
import com.exemplo.pomodoro.dados.argbParaHex
import com.exemplo.pomodoro.dados.argbParaHsv
import com.exemplo.pomodoro.dados.hexParaArgb
import com.exemplo.pomodoro.dados.luminancia
import kotlin.math.roundToInt

/** As 12 cores da paleta: atalhos do seletor. Inclui claras, que pedem Véu. */
internal val PALETA = listOf(
    0xFF0B0B10 to R.string.cor_noite,
    0xFF1E2A44 to R.string.cor_azul_noite,
    0xFF2E2A6B to R.string.cor_indigo,
    0xFF0F5A6B to R.string.cor_petroleo,
    0xFF1F6F4A to R.string.cor_floresta,
    0xFF6A2C61 to R.string.cor_ameixa,
    0xFF8A3B2E to R.string.cor_terracota,
    0xFFFF8A65 to R.string.cor_coral,
    0xFFF2C14E to R.string.cor_mostarda,
    0xFF6FD8C0 to R.string.cor_menta,
    0xFFA99BFF to R.string.cor_lavanda,
    0xFFF4EFE6 to R.string.cor_marfim,
).map { (c, nome) -> c.toInt() to nome }

private val MATIZES = List(7) { Color(Hsv(it * 60f, 1f, 1f).argb()) }

/**
 * Seletor livre de cor (ticket 12): a paleta como atalho e, abaixo, saturação e brilho, matiz e hex.
 * Serve à cor sólida do Fundo, a cada cor do Gradiente vivo e à Cor de destaque (ticket 09).
 *
 * Arrastar é manipulação direta (§10.7): o estado local segue o dedo 1:1, sem animação, e só é lido no desenho,
 * então nada recompõe por movimento. [aoEscolher] recebe a cor ao soltar, ao tocar na paleta ou com um hex válido.
 */
@Composable
fun SeletorCor(argb: Int, aoEscolher: (Int) -> Unit) {
    var hsv by remember { mutableStateOf(argbParaHsv(argb)) }
    // A cor mudou por fora (outra cor do Gradiente, outro Perfil): o seletor segue. A escolha que saiu daqui não volta
    // a converter, senão a matiz se perderia no cinza e no preto.
    LaunchedEffect(argb) { if (hsv.argb() != argb) hsv = argbParaHsv(argb) }
    val escolher by rememberUpdatedState(aoEscolher)
    val ctx = LocalContext.current

    Paleta(marcada = argb) { c -> hsv = argbParaHsv(c); escolher(c) }
    Spacer(Modifier.height(12.dp))

    // Saturação no eixo X, brilho no Y. É 2D, então segura o dedo desde o toque: aqui dentro a tela não rola.
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val h = hsv
                drawRect(Color(Hsv(h.h, 1f, 1f).argb()))
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                cursor(Offset(h.s * size.width, (1f - h.v) * size.height), h)
            }
            .pointerInput(Unit) {
                fun mover(p: Offset) {
                    hsv = hsv.copy(s = (p.x / size.width).coerceIn(0f, 1f), v = 1f - (p.y / size.height).coerceIn(0f, 1f))
                }
                awaitEachGesture {
                    val toque = awaitFirstDown()
                    toque.consume()
                    mover(toque.position) // responde no toque para baixo (§6)
                    drag(toque.id) { mover(it.position); it.consume() }
                    escolher(hsv.argb())
                }
            }
            .semantics {
                contentDescription = ctx.getString(R.string.seletor_saturacao_brilho)
                stateDescription = ctx.getString(
                    R.string.seletor_saturacao_brilho_valor, (hsv.s * 100).roundToInt(), (hsv.v * 100).roundToInt(),
                )
            },
    )
    Spacer(Modifier.height(8.dp))

    // Matiz: só o eixo X. Um arrasto vertical que começa aqui é da rolagem, e a matiz volta a ser a de antes.
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .drawBehind {
                val raio = 12.dp.toPx()
                val trilho = 16.dp.toPx()
                drawRoundRect(
                    Brush.horizontalGradient(MATIZES, startX = raio, endX = size.width - raio),
                    topLeft = Offset(0f, (size.height - trilho) / 2),
                    size = Size(size.width, trilho),
                    cornerRadius = CornerRadius(trilho / 2),
                )
                val h = hsv
                cursor(Offset(raio + h.h / 360f * (size.width - 2 * raio), size.height / 2), Hsv(h.h, 1f, 1f))
            }
            .pointerInput(Unit) {
                fun mover(x: Float) {
                    val raio = 12.dp.toPx()
                    hsv = hsv.copy(h = ((x - raio) / (size.width - 2 * raio)).coerceIn(0f, 1f) * 360f)
                }
                awaitEachGesture {
                    val toque = awaitFirstDown()
                    val antes = hsv
                    mover(toque.position.x)
                    val arrasto = awaitHorizontalTouchSlopOrCancellation(toque.id) { c, _ -> c.consume() }
                    if (arrasto != null) {
                        mover(arrasto.position.x)
                        horizontalDrag(arrasto.id) { mover(it.position.x); it.consume() }
                    } else if (currentEvent.changes.any { it.pressed }) {
                        hsv = antes // a rolagem levou o dedo
                        return@awaitEachGesture
                    }
                    escolher(hsv.argb())
                }
            }
            .semantics {
                contentDescription = ctx.getString(R.string.seletor_matiz)
                progressBarRangeInfo = ProgressBarRangeInfo(hsv.h, 0f..360f)
                setProgress { h -> hsv = hsv.copy(h = h.coerceIn(0f, 360f)); escolher(hsv.argb()); true }
            },
    )
    Spacer(Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).drawBehind { drawCircle(Color(hsv.argb())) }.border(1.dp, BordaVidroTopo, CircleShape))
        Spacer(Modifier.width(12.dp))
        CampoHex({ hsv.argb() }) { c -> hsv = argbParaHsv(c); escolher(c) }
    }
}

/** Isolado: é a única parte que recompõe durante o arrasto, para mostrar o hex da cor atual. */
@Composable
private fun CampoHex(cor: () -> Int, aoDigitar: (Int) -> Unit) {
    val atual = cor()
    var texto by remember(atual) { mutableStateOf(argbParaHex(atual)) }
    val valido = hexParaArgb(texto) != null
    OutlinedTextField(
        value = texto,
        onValueChange = { t ->
            texto = t.trim().removePrefix("#").take(6)
            hexParaArgb(texto)?.let { if (it != atual) aoDigitar(it) }
        },
        label = { Text(stringResource(R.string.seletor_hex)) },
        prefix = { Text("#") },
        isError = !valido,
        supportingText = if (valido) null else { { Text(stringResource(R.string.seletor_hex_invalido)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.width(180.dp),
    )
}

/** Cursor do seletor: a cor atual com aro branco e contorno escuro, visível do branco ao preto. */
private fun DrawScope.cursor(centro: Offset, cor: Hsv) {
    val raio = 12.dp.toPx()
    drawCircle(Color(cor.argb()), raio, centro)
    drawCircle(Color.White, raio, centro, style = Stroke(2.dp.toPx()))
    drawCircle(Color.Black.copy(alpha = 0.4f), raio + 1.dp.toPx(), centro, style = Stroke(1.dp.toPx()))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Paleta(marcada: Int, aoTocar: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PALETA.forEach { (c, nome) -> Amostra(c, c == marcada, stringResource(nome)) { aoTocar(c) } }
    }
}

/** Amostra de cor de 40 dp, com alvo de toque de 48 dp. */
@Composable
internal fun Amostra(argb: Int, marcada: Boolean, descricao: String, aoTocar: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .selectable(marcada, role = Role.RadioButton, onClick = aoTocar)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(Color(argb), CircleShape)
                .border(1.dp, if (marcada) TextoPrimario else BordaVidroTopo, CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (marcada) {
                Icon(
                    painterResource(R.drawable.ic_marcado),
                    contentDescription = null,
                    tint = if (luminancia(argb) > 0.4f) TextoSobreDestaque else TextoPrimario,
                )
            }
        }
    }
}

package com.exemplo.pomodoro.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.TimerViewModel
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.TipoFase

@Composable
fun TelaTimer(vm: TimerViewModel = viewModel()) {
    val estado = vm.estado
    val view = LocalView.current
    // O háptico dispara no handler do toque, antes da animação (DESIGN.md §6).
    fun haptico(constante: Int) = view.performHapticFeedback(constante)
    val alternar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    val aoTocarPrimario = {
        when (estado.andamento) {
            Andamento.PARADO -> { haptico(HapticFeedbackConstants.CONFIRM); vm.iniciar() }
            Andamento.RODANDO -> {
                haptico(if (alternar) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK)
                vm.pausar()
            }
            Andamento.PAUSADO -> {
                haptico(if (alternar) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK)
                vm.retomar()
            }
        }
    }
    val aoPular = { haptico(HapticFeedbackConstants.CONTEXT_CLICK); vm.pular() }

    val destaque by animateColorAsState(corDeDestaque(estado.tipo), tween(DUR_MEDIA, easing = EaseOut))

    MaterialTheme(colorScheme = darkColorScheme(primary = destaque)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(BaseEscura) // Fundo de cor sólida até o ticket 07
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val controles = @Composable {
                Controles(estado.andamento, destaque, aoTocarPrimario, aoPular)
            }
            if (maxWidth > maxHeight) {
                // Paisagem: a área do Estilo visual à esquerda; rótulo, números e controles à direita.
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxHeight().aspectRatio(1f)) // área do Estilo visual, vazia até o ticket 09
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        BarraSuperior(vm::reiniciarCiclo)
                        Spacer(Modifier.weight(1f))
                        BlocoFase(estado, comArea = false)
                        Spacer(Modifier.weight(1f))
                        controles()
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BarraSuperior(vm::reiniciarCiclo)
                    Spacer(Modifier.weight(1f))
                    BlocoFase(estado, comArea = true)
                    Spacer(Modifier.weight(1f))
                    controles()
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun BarraSuperior(aoReiniciarCiclo: () -> Unit) {
    var menuAberto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
        Box {
            BotaoVidro(R.drawable.ic_mais, stringResource(R.string.mais_opcoes), 48.dp) { menuAberto = true }
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reiniciar_ciclo), style = EstiloBody) },
                    onClick = { menuAberto = false; aoReiniciarCiclo() },
                )
            }
        }
    }
}

/** Rótulo da Fase, números e "Pausado". Na troca de Fase, o conteúdo antigo sobe e sai, e o novo sobe e entra (§5.4). */
@Composable
private fun BlocoFase(estado: EstadoCiclo, comArea: Boolean) {
    val oitoDp = with(LocalDensity.current) { 8.dp.roundToPx() }
    val pausado = estado.andamento == Andamento.PAUSADO
    val alfaNumeros by animateFloatAsState(if (pausado) 0.72f else 0.96f, tween(DUR_MEDIA, easing = EaseOut))
    val alfaPausado by animateFloatAsState(if (pausado) 1f else 0f, tween(DUR_CURTA, easing = EaseOut))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = estado,
            contentKey = { it.tipo to it.posicao }, // os segundos mudam sem animar (§10); só a troca de Fase anima
            transitionSpec = {
                (fadeIn(tween(DUR_MEDIA, 60, EaseOut)) + slideInVertically(tween(DUR_MEDIA, 60, EaseOut)) { oitoDp })
                    .togetherWith(fadeOut(tween(DUR_SAIDA)) + slideOutVertically(tween(DUR_SAIDA)) { -oitoDp })
                    .using(null)
            },
            label = "fase",
        ) { e ->
            val nome = nomeDaFase(e.tipo)
            val minutos = ((e.restanteMs + 59_999) / 60_000).toInt()
            val descricao = pluralStringResource(R.plurals.descricao_timer, minutos, nome, e.posicao, e.focosPorCiclo, minutos)
            Column(
                Modifier.clearAndSetSemantics { contentDescription = descricao },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.posicao_no_ciclo, nome, e.posicao, e.focosPorCiclo), style = EstiloLabel)
                Spacer(Modifier.height(24.dp))
                Numeros(e, Color.White.copy(alpha = alfaNumeros), comArea)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.pausado),
            style = EstiloLabel,
            modifier = Modifier
                .graphicsLayer { alpha = alfaPausado }
                .then(if (pausado) Modifier else Modifier.clearAndSetSemantics {}),
        )
    }
}

@Composable
private fun Numeros(estado: EstadoCiclo, cor: Color, comArea: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val larguraMax = maxWidth * 0.84f
        val formatoLongo = estado.duracaoMs >= 60 * 60_000L
        val medidor = rememberTextMeasurer()
        val densidade = LocalDensity.current
        // Ajuste do `timer` (§2): o menor entre 88 e o tamanho em que a string mais larga cabe em 84% da largura.
        // Ignora o fontScale de propósito. Recalcula só quando muda a largura ou o formato, nunca a cada segundo.
        val tamanho = remember(larguraMax, formatoLongo, densidade) {
            val base = with(densidade) { 88.dp.toSp() }
            val maisLarga = if (formatoLongo) "0:00:00" else "00:00"
            val largura = medidor.measure(maisLarga, EstiloTimer.copy(fontSize = base)).size.width
            val limite = with(densidade) { larguraMax.toPx() }
            if (largura <= limite) base else base * (limite / largura)
        }
        val texto = @Composable {
            Text(formatarTempo(estado.restanteMs, estado.duracaoMs), style = EstiloTimer.copy(fontSize = tamanho, color = cor))
        }
        if (comArea) {
            // Área do Estilo visual: 0.84 × largura, vazia até o ticket 09.
            Box(Modifier.size(larguraMax), contentAlignment = Alignment.Center) { texto() }
        } else {
            texto()
        }
    }
}

@Composable
private fun Controles(andamento: Andamento, destaque: Color, aoTocarPrimario: () -> Unit, aoPular: () -> Unit) {
    val dezesseisDp = with(LocalDensity.current) { 16.dp.roundToPx() }
    val descricaoPrimario = stringResource(
        when (andamento) {
            Andamento.PARADO -> R.string.iniciar
            Andamento.RODANDO -> R.string.pausar
            Andamento.PAUSADO -> R.string.retomar
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.size(56.dp)) // lugar do Estender (ticket 03)
        BotaoPrimario(andamento == Andamento.RODANDO, destaque, descricaoPrimario, aoTocarPrimario)
        Box(Modifier.size(56.dp)) {
            // Pular só aparece com a Fase rodando ou pausada, saindo de trás do botão primário (§5.2).
            androidx.compose.animation.AnimatedVisibility( // a versão sem escopo, não a do Row
                visible = andamento != Andamento.PARADO,
                enter = fadeIn(springUi()) + slideInHorizontally(springUi()) { -dezesseisDp },
                exit = fadeOut(springUi()) + slideOutHorizontally(springUi()) { -dezesseisDp },
            ) {
                BotaoVidro(R.drawable.ic_pular, stringResource(R.string.pular_fase), 56.dp, aoPular)
            }
        }
    }
}

/** Sem ripple: o toque responde com escala na descida, com SpringPress (§6). */
@Composable
private fun Modifier.pressionavel(
    interacao: MutableInteractionSource,
    escala: Float,
    descricao: String,
    aoTocar: () -> Unit,
): Modifier {
    val pressionado by interacao.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressionado) escala else 1f, springPress())
    return graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interacao, indication = null, role = Role.Button, onClick = aoTocar)
        .semantics { contentDescription = descricao }
}

@Composable
private fun BotaoPrimario(rodando: Boolean, cor: Color, descricao: String, aoTocar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val clareia by animateFloatAsState(if (pressionado) 0.08f else 0f, springPress())
    Box(
        Modifier
            .size(88.dp)
            .pressionavel(interacao, 0.96f, descricao, aoTocar)
            .background(lerp(cor, Color.White, clareia), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = rodando,
            transitionSpec = {
                (fadeIn(tween(DUR_CURTA, easing = EaseOut)) + scaleIn(tween(DUR_CURTA, easing = EaseOut), initialScale = 0.8f))
                    .togetherWith(fadeOut(tween(DUR_CURTA, easing = EaseOut)))
            },
            label = "icone",
        ) { r ->
            Icon(
                painterResource(if (r) R.drawable.ic_pausar else R.drawable.ic_iniciar),
                contentDescription = null,
                tint = TextoSobreDestaque,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun BotaoVidro(icone: Int, descricao: String, tamanho: Dp, aoTocar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val tinta by animateColorAsState(if (pressionado) VidroClaroPressionado else VidroClaro, springPress())
    Box(
        Modifier
            .size(tamanho)
            .pressionavel(interacao, 0.95f, descricao, aoTocar)
            .background(tinta, CircleShape)
            .border(1.dp, Brush.verticalGradient(listOf(BordaVidroTopo, BordaVidroBase)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icone), contentDescription = null, tint = TextoPrimario, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun nomeDaFase(tipo: TipoFase) = stringResource(
    when (tipo) {
        TipoFase.FOCO -> R.string.fase_foco
        TipoFase.PAUSA_CURTA -> R.string.fase_pausa_curta
        TipoFase.PAUSA_LONGA -> R.string.fase_pausa_longa
    },
)

/** `MM:SS` para Fases de até 59 min e `H:MM:SS` a partir de 60 min (§2). Arredonda para cima: começa em 25:00. */
private fun formatarTempo(restanteMs: Long, duracaoMs: Long): String {
    val s = (restanteMs + 999) / 1000
    return if (duracaoMs >= 60 * 60_000L) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)
}

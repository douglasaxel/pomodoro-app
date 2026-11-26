package com.exemplo.pomodoro.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.FaseRegistrada
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val formatoDia = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale.forLanguageTag("pt-BR")) // "seg., 23 de nov."

/** A lista cronológica das Fases, agrupada por dia. Os gráficos e os totais chegam no ticket 06. */
@Composable
fun TelaHistorico(margens: PaddingValues) {
    val historico = (LocalContext.current.applicationContext as AppPomodoro).historico
    // Nulo até o banco responder: sem spinner, nada aparece até os dados chegarem (§10).
    val fases by historico.todas.collectAsStateWithLifecycle(null)
    val escopo = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }
    val textoApagada = stringResource(R.string.fase_apagada)
    val textoDesfazer = stringResource(R.string.desfazer)

    fun apagar(f: FaseRegistrada) {
        historico.apagar(f.id)
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            // Short = 4 s. Desfazer devolve a Fase com o mesmo id.
            if (avisos.showSnackbar(textoApagada, textoDesfazer, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                historico.registrar(f)
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(margens)) {
        Column {
            Text(
                stringResource(R.string.aba_historico),
                style = EstiloTitle,
                color = TextoPrimario,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            )
            val lista = fases ?: return@Column
            if (lista.isEmpty()) {
                Text(
                    stringResource(R.string.historico_vazio),
                    style = EstiloBody,
                    color = TextoSecundario,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                return@Column
            }
            val zona = ZoneId.systemDefault()
            val hoje = LocalDate.now(zona)
            // A lista já vem da mais recente para a mais antiga; o groupBy mantém a ordem.
            val porDia = lista.groupBy { Instant.ofEpochMilli(it.inicioEpochMs).atZone(zona).toLocalDate() }
            // Sem animação de entrada das linhas (§10).
            LazyColumn(Modifier.widthIn(max = 560.dp)) {
                porDia.forEach { (dia, doDia) ->
                    item(key = "dia-$dia") {
                        Text(
                            when (dia) {
                                hoje -> stringResource(R.string.hoje)
                                hoje.minusDays(1) -> stringResource(R.string.ontem)
                                else -> formatoDia.format(dia)
                            },
                            style = EstiloLabel,
                            color = TextoSecundario,
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, placementSpec = springUi(), fadeOutSpec = null)
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(doDia, key = { it.id }) { f ->
                        LinhaFase(
                            f, zona,
                            aoApagar = { apagar(f) },
                            // Os vizinhos fecham o buraco enquanto a linha sai (§6).
                            modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = springUi(), fadeOutSpec = null),
                        )
                    }
                }
            }
        }
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LinhaFase(f: FaseRegistrada, zona: ZoneId, aoApagar: () -> Unit, modifier: Modifier) {
    val view = LocalView.current
    val deslize = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.4f })

    // Háptico ao cruzar o limiar e ao voltar dele (§6).
    LaunchedEffect(deslize) {
        snapshotFlow { deslize.targetValue == SwipeToDismissBoxValue.EndToStart }.drop(1).collect { armado ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> view.performHapticFeedback(
                    if (armado) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE,
                )
                armado -> view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }
    }

    SwipeToDismissBox(
        deslize,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { aoApagar() },
        backgroundContent = {
            // Só a faixa que a linha descobre fica vermelha: a linha é transparente sobre o Fundo.
            val descoberto = with(LocalDensity.current) {
                (-runCatching { deslize.requireOffset() }.getOrDefault(0f)).coerceAtLeast(0f).toDp()
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxHeight().width(descoberto).clipToBounds().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(R.drawable.ic_apagar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.wrapContentWidth(Alignment.End, unbounded = true).padding(end = 24.dp),
                )
            }
        },
    ) {
        var menuAberto by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { menuAberto = true } // o menu da linha é a opção acessível ao swipe
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Concluída ●, incompleta ◐. O texto "incompleto" diz o mesmo sem depender da cor.
            Text(
                if (f.concluida) "●" else "◐",
                style = EstiloBody,
                color = corDeDestaque(f.tipo),
                modifier = Modifier.clearAndSetSemantics {},
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(rotuloDaFase(f)), style = EstiloBody, color = TextoPrimario, maxLines = 1, softWrap = false)
            // Só o nome do Perfil trunca; horário e minutos nunca.
            Text(
                stringResource(R.string.sufixo_perfil, f.perfilNome),
                style = EstiloBody,
                color = TextoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp).weight(1f),
            )
            Text(
                formatoHora.format(Instant.ofEpochMilli(f.inicioEpochMs).atZone(zona)),
                style = EstiloBody,
                color = TextoSecundario,
                softWrap = false,
                modifier = Modifier.padding(start = 12.dp),
            )
            Text(
                stringResource(R.string.valor_min, f.minutosEfetivos),
                style = EstiloBody,
                color = TextoPrimario,
                softWrap = false,
                modifier = Modifier.padding(start = 12.dp),
            )
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.apagar)) }, onClick = { menuAberto = false; aoApagar() })
            }
        }
    }
}

private fun rotuloDaFase(f: FaseRegistrada) = when {
    f.concluida -> nomeDaFaseRes(f.tipo)
    f.tipo == TipoFase.FOCO -> R.string.fase_foco_incompleto
    f.tipo == TipoFase.PAUSA_CURTA -> R.string.fase_pausa_curta_incompleta
    else -> R.string.fase_pausa_longa_incompleta
}

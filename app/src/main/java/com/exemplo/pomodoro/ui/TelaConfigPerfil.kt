package com.exemplo.pomodoro.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.Aparencia
import com.exemplo.pomodoro.dados.NOME_MAX
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.motor.ConfigCiclo
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.delay

private const val MIN = 60_000L

/** Configuração do Perfil (§8): a prévia ao vivo fica fixa no topo, e as seções rolam por baixo. */
@Composable
fun TelaConfigPerfil(id: Long, novo: Boolean, margens: PaddingValues, aoVoltar: () -> Unit) {
    val perfis = (LocalContext.current.applicationContext as AppPomodoro).perfis
    // Cópia local: a tela edita na hora, e cada mudança vai para a fila de gravação do repositório.
    var perfil by remember { mutableStateOf<Perfil?>(null) }
    var nome by remember { mutableStateOf("") }
    LaunchedEffect(id) {
        perfil = perfis.ler(id)
        nome = perfil?.nome.orEmpty()
    }
    val p = perfil ?: return // dados locais: nada aparece até chegarem (§10)
    fun editar(novoPerfil: Perfil) {
        perfil = novoPerfil
        perfis.salvar(novoPerfil)
    }
    fun editarTempo(muda: ConfigCiclo.() -> ConfigCiclo) = editar(p.copy(tempo = p.tempo.muda()))

    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (novo) foco.requestFocus() }

    fun editarAparencia(muda: Aparencia.() -> Aparencia) {
        val atual = perfil ?: return // o mais recente: a foto chega depois de importada
        editar(atual.copy(aparencia = atual.aparencia.muda()))
    }
    var tipoPrevia by rememberSaveable { mutableStateOf(TipoFase.FOCO) }

    Column(Modifier.fillMaxSize().padding(margens)) {
        Row(Modifier.padding(start = 4.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoVoltar) {
                Icon(painterResource(R.drawable.ic_voltar), contentDescription = stringResource(R.string.voltar))
            }
            Text(nome.ifBlank { p.nome }, style = EstiloTitle, color = TextoPrimario, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        // ponytail: sem a máscara de fade sob a prévia (§8). Entra se a borda do conteúdo rolando incomodar.
        Column(Modifier.widthIn(max = 560.dp).padding(horizontal = 20.dp, vertical = 8.dp)) {
            PreviaAoVivo(p.aparencia, tipoPrevia)
            Spacer(Modifier.height(8.dp))
            FaseDaPrevia(tipoPrevia) { tipoPrevia = it }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 560.dp).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.secao_tempo), style = EstiloLabel)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nome,
                onValueChange = {
                    nome = it.take(NOME_MAX)
                    if (nome.isNotBlank()) editar(p.copy(nome = nome.trim()))
                },
                label = { Text(stringResource(R.string.nome)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
            )
            Spacer(Modifier.height(8.dp))
            Passo(stringResource(R.string.fase_foco), (p.tempo.focoMs / MIN).toInt(), 1..180, emMinutos = true) { v ->
                editarTempo { copy(focoMs = v * MIN) }
            }
            Passo(stringResource(R.string.fase_pausa_curta), (p.tempo.pausaCurtaMs / MIN).toInt(), 1..180, emMinutos = true) { v ->
                editarTempo { copy(pausaCurtaMs = v * MIN) }
            }
            Passo(stringResource(R.string.fase_pausa_longa), (p.tempo.pausaLongaMs / MIN).toInt(), 1..180, emMinutos = true) { v ->
                editarTempo { copy(pausaLongaMs = v * MIN) }
            }
            Passo(stringResource(R.string.focos_por_ciclo), p.tempo.focosPorCiclo, 1..12, emMinutos = false) { v ->
                editarTempo { copy(focosPorCiclo = v) }
            }
            Passo(stringResource(R.string.estender_soma), (p.tempo.estenderMs / MIN).toInt(), 1..30, emMinutos = true) { v ->
                editarTempo { copy(estenderMs = v * MIN) }
            }
            Chave(stringResource(R.string.auto_focos), p.tempo.autoIniciarFocos) { v -> editarTempo { copy(autoIniciarFocos = v) } }
            Chave(stringResource(R.string.auto_pausas), p.tempo.autoIniciarPausas) { v -> editarTempo { copy(autoIniciarPausas = v) } }
            SecaoEstiloECor(p.aparencia, tipoPrevia, { tipoPrevia = it }, ::editarAparencia)
            SecaoFundo(p.aparencia, ::editarAparencia)
            SecoesAlertaNotificacao(p.alerta) { editar(p.copy(alerta = it)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Controle de − e + (§8). Segurar repete e acelera; o toque no valor abre o teclado.
 * O botão do limite fica desabilitado, e chegar ao limite dá o háptico REJECT uma vez (§6).
 */
@Composable
private fun Passo(rotulo: String, valor: Int, faixa: IntRange, emMinutos: Boolean, aoMudar: (Int) -> Unit) {
    val view = LocalView.current
    val atual by rememberUpdatedState(valor)
    val mudar by rememberUpdatedState(aoMudar)
    fun passo(delta: Int) {
        val novo = (atual + delta).coerceIn(faixa)
        if (novo == atual) return
        view.performHapticFeedback(
            if (novo == faixa.first || novo == faixa.last) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.CLOCK_TICK,
        )
        mudar(novo)
    }
    var digitando by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = EstiloBody, color = TextoPrimario, modifier = Modifier.weight(1f))
        BotaoPasso(R.drawable.ic_menos, stringResource(R.string.diminuir, rotulo), valor > faixa.first) { passo(-1) }
        // Largura fixa com `tnum`: de "1 min" a "180 min", os botões não se movem (§8).
        Text(
            if (emMinutos) stringResource(R.string.valor_min, valor) else "$valor",
            style = EstiloBody.copy(fontFeatureSettings = "tnum", textAlign = TextAlign.Center),
            color = TextoPrimario,
            modifier = Modifier
                .width(80.dp)
                .clickable(role = Role.Button) { digitando = true }
                .padding(vertical = 12.dp),
        )
        BotaoPasso(R.drawable.ic_adicionar, stringResource(R.string.aumentar, rotulo), valor < faixa.last) { passo(+1) }
    }

    if (digitando) {
        var texto by remember { mutableStateOf(valor.toString()) }
        val numero = texto.toIntOrNull()
        AlertDialog(
            onDismissRequest = { digitando = false },
            title = { Text(rotulo) },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { t -> texto = t.filter(Char::isDigit).take(3) },
                    supportingText = { Text(stringResource(R.string.faixa, faixa.first, faixa.last)) },
                    isError = numero == null || numero !in faixa,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(enabled = numero != null && numero in faixa, onClick = { mudar(numero!!); digitando = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { digitando = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

@Composable
private fun BotaoPasso(icone: Int, descricao: String, habilitado: Boolean, aoPasso: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val passo by rememberUpdatedState(aoPasso)
    var repetiu by remember { mutableStateOf(false) }
    LaunchedEffect(pressionado) {
        if (!pressionado) return@LaunchedEffect
        repetiu = false
        delay(400)
        var intervalo = 180L
        while (true) {
            repetiu = true
            passo()
            delay(intervalo)
            intervalo = maxOf(40L, intervalo * 4 / 5) // acelera
        }
    }
    // Depois de segurar, a soltura não conta como mais um passo.
    IconButton(onClick = { if (repetiu) repetiu = false else passo() }, enabled = habilitado, interactionSource = interacao) {
        Icon(painterResource(icone), contentDescription = descricao)
    }
}

@Composable
internal fun Chave(rotulo: String, ligada: Boolean, aoMudar: (Boolean) -> Unit) {
    val view = LocalView.current
    val alternar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(ligada, role = Role.Switch) { v ->
                view.performHapticFeedback(
                    when {
                        !alternar -> HapticFeedbackConstants.CONTEXT_CLICK
                        v -> HapticFeedbackConstants.TOGGLE_ON
                        else -> HapticFeedbackConstants.TOGGLE_OFF
                    },
                )
                aoMudar(v)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rotulo, style = EstiloBody, color = TextoPrimario, modifier = Modifier.weight(1f))
        Switch(checked = ligada, onCheckedChange = null)
    }
}

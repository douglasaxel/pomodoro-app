package com.exemplo.pomodoro.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.NOME_MAX
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.motor.ConfigCiclo
import kotlinx.coroutines.launch

/** "25 · 5 · 15 · 4 Focos" */
@Composable
internal fun resumoTempo(t: ConfigCiclo) = pluralStringResource(
    R.plurals.resumo_tempo, t.focosPorCiclo,
    (t.focoMs / 60_000).toInt(), (t.pausaCurtaMs / 60_000).toInt(), (t.pausaLongaMs / 60_000).toInt(), t.focosPorCiclo,
)

@Composable
fun TelaPerfis(margens: PaddingValues, aoAbrir: (id: Long, novo: Boolean) -> Unit) {
    val perfis = (LocalContext.current.applicationContext as AppPomodoro).perfis
    val todos by perfis.todos.collectAsStateWithLifecycle(emptyList())
    val ativo by perfis.ativo.collectAsStateWithLifecycle(null)
    val escopo = rememberCoroutineScope()
    var renomeando by remember { mutableStateOf<Perfil?>(null) }
    var excluindo by remember { mutableStateOf<Perfil?>(null) }

    Column(Modifier.fillMaxSize().padding(margens)) {
        Row(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.aba_perfis), style = EstiloTitle, color = TextoPrimario, modifier = Modifier.weight(1f))
            IconButton(onClick = { escopo.launch { aoAbrir(perfis.criar(), true) } }) {
                Icon(painterResource(R.drawable.ic_adicionar), contentDescription = stringResource(R.string.novo_perfil))
            }
        }
        // Sem animação de entrada das linhas (§10).
        LazyColumn(Modifier.widthIn(max = 560.dp)) {
            items(todos, key = { it.id }) { p ->
                LinhaPerfil(
                    perfil = p,
                    ativo = p.id == ativo?.id,
                    unico = todos.size <= 1,
                    aoAbrir = { aoAbrir(p.id, false) },
                    aoDuplicar = { perfis.duplicar(p.id) },
                    aoRenomear = { renomeando = p },
                    aoExcluir = { excluindo = p },
                )
            }
        }
    }

    renomeando?.let { p ->
        var nome by remember(p.id) { mutableStateOf(p.nome) }
        AlertDialog(
            onDismissRequest = { renomeando = null },
            title = { Text(stringResource(R.string.renomear)) },
            text = {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(NOME_MAX) },
                    label = { Text(stringResource(R.string.nome)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(enabled = nome.isNotBlank(), onClick = { perfis.renomear(p.id, nome.trim()); renomeando = null }) {
                    Text(stringResource(R.string.salvar))
                }
            },
            dismissButton = { TextButton(onClick = { renomeando = null }) { Text(stringResource(R.string.cancelar)) } },
        )
    }

    excluindo?.let { p ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            title = { Text(stringResource(R.string.excluir_titulo, p.nome)) },
            text = { Text(stringResource(R.string.excluir_texto)) },
            confirmButton = {
                TextButton(onClick = { perfis.excluir(p.id); excluindo = null }) { Text(stringResource(R.string.excluir)) }
            },
            dismissButton = { TextButton(onClick = { excluindo = null }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaPerfil(
    perfil: Perfil,
    ativo: Boolean,
    unico: Boolean,
    aoAbrir: () -> Unit,
    aoDuplicar: () -> Unit,
    aoRenomear: () -> Unit,
    aoExcluir: () -> Unit,
) {
    val view = LocalView.current
    var menuAberto by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = aoAbrir,
                onLongClick = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); menuAberto = true },
            )
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Miniatura de 56 dp: vira o quadro estático do Perfil quando o Fundo e o Estilo visual existirem (tickets 07 e 09).
        val forma = RoundedCornerShape(16.dp)
        Box(
            Modifier
                .size(56.dp)
                .background(VidroClaro, forma)
                .border(1.dp, Brush.verticalGradient(listOf(BordaVidroTopo, BordaVidroBase)), forma),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(perfil.nome, style = EstiloBody, color = TextoPrimario, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(resumoTempo(perfil.tempo), style = EstiloCaption)
        }
        if (ativo) Text(stringResource(R.string.ativo), style = EstiloLabel, modifier = Modifier.padding(start = 8.dp))
        Box {
            IconButton(onClick = { menuAberto = true }) {
                Icon(
                    painterResource(R.drawable.ic_menu),
                    contentDescription = stringResource(R.string.opcoes_do_perfil, perfil.nome),
                    tint = TextoSecundario,
                )
            }
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.duplicar)) }, onClick = { menuAberto = false; aoDuplicar() })
                DropdownMenuItem(text = { Text(stringResource(R.string.renomear)) }, onClick = { menuAberto = false; aoRenomear() })
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(R.string.excluir))
                            if (unico) Text(stringResource(R.string.excluir_ultimo), style = EstiloCaption.copy(color = TextoTerciario))
                        }
                    },
                    enabled = !unico,
                    onClick = { menuAberto = false; aoExcluir() },
                )
            }
        }
    }
}

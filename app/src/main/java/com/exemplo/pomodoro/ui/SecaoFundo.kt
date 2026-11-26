package com.exemplo.pomodoro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.Aparencia
import com.exemplo.pomodoro.dados.Fundo
import com.exemplo.pomodoro.dados.VEU_AJUSTE_MAX
import com.exemplo.pomodoro.dados.importarFoto
import kotlinx.coroutines.launch

/**
 * Seção Fundo da Configuração do Perfil (§8): tipo do Fundo, a cor no seletor (ticket 12), Véu e Movimento do Fundo.
 * [aoMudar] recebe uma transformação, aplicada sobre o Perfil mais recente (a foto chega depois de importada).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoFundo(aparencia: Aparencia, aoMudar: (Aparencia.() -> Aparencia) -> Unit) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    // Photo Picker: sem permissão. Se a importação falhar (arquivo ilegível), o Fundo atual fica.
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            runCatching { importarFoto(ctx, uri) }.onSuccess { foto -> aoMudar { copy(fundo = foto) } }
        }
    }
    val fundo = aparencia.fundo

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.secao_fundo), style = EstiloLabel)
    Spacer(Modifier.height(8.dp))
    val opcoes = listOf(R.string.fundo_biblioteca, R.string.fundo_gradiente, R.string.fundo_cor, R.string.fundo_foto)
    val escolhida = when (fundo) {
        is Fundo.Biblioteca -> 0
        is Fundo.Gradiente -> 1
        is Fundo.Cor -> 2
        is Fundo.Foto -> 3
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opcoes.forEachIndexed { i, rotulo ->
            SegmentedButton(
                selected = i == escolhida,
                onClick = {
                    when (i) {
                        0 -> if (fundo !is Fundo.Biblioteca) aoMudar { copy(fundo = Fundo.PADRAO) }
                        1 -> if (fundo !is Fundo.Gradiente) aoMudar { copy(fundo = Fundo.GRADIENTE_PADRAO) }
                        2 -> if (fundo !is Fundo.Cor) aoMudar { copy(fundo = Fundo.Cor((fundo as? Fundo.Gradiente)?.cores?.first() ?: PALETA[0].first)) }
                        else -> escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(i, opcoes.size),
                icon = {},
                contentPadding = PADDING_SEGMENTO,
            ) { RotuloSegmento(stringResource(rotulo)) }
        }
    }
    Spacer(Modifier.height(12.dp))

    when (fundo) {
        is Fundo.Biblioteca -> MiniaturasBiblioteca(fundo.arte, aparencia.movimentoFundo) { id -> aoMudar { copy(fundo = Fundo.Biblioteca(id)) } }
        is Fundo.Cor -> SeletorCor(fundo.argb) { c -> aoMudar { copy(fundo = Fundo.Cor(c)) } }
        is Fundo.Gradiente -> EditorGradiente(fundo) { cores -> aoMudar { copy(fundo = Fundo.Gradiente(cores)) } }
        is Fundo.Foto -> TextButton(onClick = {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(stringResource(R.string.escolher_outra_foto), style = EstiloBody) }
    }

    // Véu: manipulação direta (§10.7). O valor local segue o dedo; o Perfil grava ao soltar.
    var ajuste by remember(aparencia.veuAjuste) { mutableFloatStateOf(aparencia.veuAjuste) }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.veu), style = EstiloBody, color = TextoPrimario, modifier = Modifier.weight(1f))
        Text(
            if (ajuste < 0.005f) stringResource(R.string.veu_automatico) else stringResource(R.string.veu_mais, (ajuste * 100).toInt()),
            style = EstiloLabel,
        )
    }
    Slider(
        value = ajuste,
        onValueChange = { ajuste = it },
        onValueChangeFinished = { aoMudar { copy(veuAjuste = ajuste) } },
        valueRange = 0f..VEU_AJUSTE_MAX,
        modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.veu) },
    )
    Chave(stringResource(R.string.movimento_fundo), aparencia.movimentoFundo) { v -> aoMudar { copy(movimentoFundo = v) } }
}

/** Sem o ✓ (a cor do segmento já diz qual está escolhido) e com menos respiro: cabe "Biblioteca" em 1/4 da linha. */
internal val PADDING_SEGMENTO = PaddingValues(horizontal = 8.dp)

/**
 * Rótulo de segmento: sempre uma linha, para os segmentos terem a mesma altura. Se não couber (fonte grande, tela
 * estreita), a letra encolhe até 12 dp, e nunca quebra em duas linhas.
 */
@Composable
internal fun RotuloSegmento(texto: String) {
    val estilo = EstiloLabel.copy(color = Color.Unspecified)
    val minimo = with(LocalDensity.current) { 12.dp.toSp() }
    Text(
        texto,
        style = estilo,
        maxLines = 1,
        // Com a fonte do sistema já pequena, o máximo fica abaixo do mínimo: aí não há o que encolher.
        autoSize = if (minimo < estilo.fontSize) TextAutoSize.StepBased(minFontSize = minimo, maxFontSize = estilo.fontSize) else null,
    )
}

/** Gradiente vivo: escolha qual cor editar, some ou tire cores (de 2 a [Fundo.CORES_MAX]) e edite no seletor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorGradiente(fundo: Fundo.Gradiente, aoMudar: (List<Int>) -> Unit) {
    val cores = fundo.cores
    var editando by rememberSaveable { mutableIntStateOf(0) }
    val i = editando.coerceIn(cores.indices)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.Center) {
        cores.forEachIndexed { j, c ->
            Amostra(c, j == i, stringResource(R.string.gradiente_cor, j + 1)) { editando = j }
        }
        if (cores.size < Fundo.CORES_MAX) {
            IconButton(onClick = {
                aoMudar(cores + (PALETA.map { it.first }.firstOrNull { it !in cores } ?: cores[i]))
                editando = cores.size
            }) { Icon(painterResource(R.drawable.ic_adicionar), stringResource(R.string.gradiente_adicionar)) }
        }
        if (cores.size > 2) {
            IconButton(onClick = {
                aoMudar(cores.filterIndexed { k, _ -> k != i })
                editando = (i - 1).coerceAtLeast(0)
            }) { Icon(painterResource(R.drawable.ic_apagar), stringResource(R.string.gradiente_remover, i + 1)) }
        }
    }
    Text(stringResource(R.string.gradiente_ajuda, Fundo.CORES_MAX), style = EstiloCaption)
    Spacer(Modifier.height(12.dp))
    SeletorCor(cores[i]) { c -> aoMudar(cores.toMutableList().also { it[i] = c }) }
}

package com.exemplo.pomodoro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.Aparencia
import com.exemplo.pomodoro.dados.Fundo
import com.exemplo.pomodoro.dados.VEU_AJUSTE_MAX
import com.exemplo.pomodoro.dados.importarFoto
import com.exemplo.pomodoro.dados.luminancia
import kotlinx.coroutines.launch

/** Cores oferecidas para a cor sólida e para o Gradiente vivo. Inclui claras, que pedem Véu. */
private val PALETA = listOf(
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

/**
 * Seção Fundo da Configuração do Perfil (§8): tipo do Fundo, Véu e Movimento do Fundo.
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
    val opcoes = listOf(R.string.fundo_gradiente, R.string.fundo_cor, R.string.fundo_foto)
    val escolhida = when (fundo) {
        is Fundo.Gradiente -> 0
        is Fundo.Cor -> 1
        is Fundo.Foto -> 2
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opcoes.forEachIndexed { i, rotulo ->
            SegmentedButton(
                selected = i == escolhida,
                onClick = {
                    when (i) {
                        0 -> if (fundo !is Fundo.Gradiente) aoMudar { copy(fundo = Fundo.PADRAO) }
                        1 -> if (fundo !is Fundo.Cor) aoMudar { copy(fundo = Fundo.Cor((fundo as? Fundo.Gradiente)?.cores?.first() ?: PALETA[0].first)) }
                        else -> escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(i, opcoes.size),
            ) { Text(stringResource(rotulo), style = EstiloLabel.copy(color = Color.Unspecified)) }
        }
    }
    Spacer(Modifier.height(12.dp))

    when (fundo) {
        is Fundo.Cor -> Paleta(selecionadas = listOf(fundo.argb), multipla = false) { c -> aoMudar { copy(fundo = Fundo.Cor(c)) } }
        is Fundo.Gradiente -> {
            Paleta(selecionadas = fundo.cores, multipla = true) { c ->
                val cores = if (c in fundo.cores) {
                    if (fundo.cores.size > 2) fundo.cores - c else fundo.cores // pelo menos 2 cores
                } else {
                    if (fundo.cores.size < Fundo.CORES_MAX) fundo.cores + c else fundo.cores
                }
                if (cores != fundo.cores) aoMudar { copy(fundo = Fundo.Gradiente(cores)) }
            }
            Text(stringResource(R.string.gradiente_ajuda, Fundo.CORES_MAX), style = EstiloCaption)
        }
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

/** Amostras de cor de 40 dp, com alvo de toque de 48 dp. [multipla]: o Gradiente vivo marca várias. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Paleta(selecionadas: List<Int>, multipla: Boolean, aoTocar: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PALETA.forEach { (c, @StringRes nome) ->
            val marcada = c in selecionadas
            val rotulo = stringResource(nome)
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(marcada, role = if (multipla) Role.Checkbox else Role.RadioButton) { aoTocar(c) }
                    .semantics { contentDescription = rotulo },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color(c), CircleShape)
                        .border(1.dp, if (marcada) TextoPrimario else BordaVidroTopo, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (marcada) {
                        Icon(
                            painterResource(R.drawable.ic_marcado),
                            contentDescription = null,
                            tint = if (luminancia(c) > 0.4f) TextoSobreDestaque else TextoPrimario,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

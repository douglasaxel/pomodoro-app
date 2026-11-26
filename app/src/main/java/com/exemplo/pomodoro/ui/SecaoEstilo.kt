package com.exemplo.pomodoro.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.Aparencia
import com.exemplo.pomodoro.dados.EstiloVisual
import com.exemplo.pomodoro.motor.TipoFase

private val ALTURA_PREVIA = 176.dp

/**
 * Prévia ao vivo (§5.9): o Fundo, o Véu e o Estilo visual do Perfil em edição, com o progresso fixo em 0.6.
 * Tem o próprio Fundo gravado: o vidro da prévia borra o Fundo da prévia, não o da tela.
 */
@Composable
fun PreviaAoVivo(aparencia: Aparencia, tipo: TipoFase) {
    val fundo = rememberFundoGravado()
    val descricao = stringResource(R.string.previa)
    Box(
        Modifier
            .fillMaxWidth()
            .height(ALTURA_PREVIA)
            .clip(RoundedCornerShape(32.dp))
            .clearAndSetSemantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().gravarFundo(fundo)) { FundoComVeu(aparencia, naTelaTimer = true) }
        CompositionLocalProvider(LocalFundoGravado provides fundo) {
            EstiloVisualAnimado(
                aparencia.estilo,
                progresso = 0.6f,
                tipo = tipo,
                destaque = Color(aparencia.destaque(tipo)),
                rodando = true,
                modifier = Modifier.size(ALTURA_PREVIA * 0.84f),
            )
        }
    }
}

/** Qual Fase a prévia mostra (§8). Na troca, o Estilo faz a transição de Pular (§5.9). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaseDaPrevia(tipo: TipoFase, aoMudar: (TipoFase) -> Unit) {
    val view = LocalView.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TipoFase.entries.forEachIndexed { i, t ->
            SegmentedButton(
                selected = t == tipo,
                onClick = {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK
                        else HapticFeedbackConstants.CLOCK_TICK,
                    )
                    aoMudar(t)
                },
                shape = SegmentedButtonDefaults.itemShape(i, TipoFase.entries.size),
                icon = {},
                contentPadding = PADDING_SEGMENTO,
            ) { RotuloSegmento(stringResource(nomeDaFaseRes(t))) }
        }
    }
}

/**
 * Seções Estilo visual e Cor de destaque da Configuração do Perfil (§8).
 * Tocar na cor de uma Fase também troca a Fase da prévia (§5.9). Com "Do Fundo" ligado, as amostras mostram a cor
 * extraída, e o seletor some; as cores escolhidas ficam guardadas para quando desligar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoEstiloECor(
    aparencia: Aparencia,
    tipoPrevia: TipoFase,
    aoEscolherFase: (TipoFase) -> Unit,
    aoMudar: (Aparencia.() -> Aparencia) -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.secao_estilo), style = EstiloLabel)
    Spacer(Modifier.height(8.dp))
    // Uma miniatura ao vivo por Estilo, na ordem do enum: um Estilo novo entra aqui sozinho (§8).
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EstiloVisual.entries.forEach { estilo ->
            MiniaturaEstilo(estilo, aparencia.estilo == estilo, tipoPrevia, Color(aparencia.destaque(tipoPrevia)), Modifier.weight(1f)) {
                if (aparencia.estilo != estilo) aoMudar { copy(estilo = estilo) }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.secao_destaque), style = EstiloLabel)
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        TipoFase.entries.forEach { t ->
            Amostra(
                aparencia.destaque(t),
                marcada = t == tipoPrevia,
                descricao = stringResource(R.string.destaque_de, stringResource(nomeDaFaseRes(t))),
                papel = Role.RadioButton,
            ) { aoEscolherFase(t) }
        }
    }
    Chave(stringResource(R.string.destaque_do_fundo), aparencia.destaqueDoFundo) { v -> aoMudar { copy(destaqueDoFundo = v) } }
    if (!aparencia.destaqueDoFundo) {
        Spacer(Modifier.height(8.dp))
        SeletorCor(aparencia.destaque(tipoPrevia)) { c -> aoMudar { comDestaque(tipoPrevia, c) } }
    }
}

private val FORMA_MINIATURA = RoundedCornerShape(16.dp)

/**
 * Miniatura ao vivo de um Estilo visual, com o progresso em 0.6 e a Fase da prévia.
 * Só a escolhida anima: as outras ficam no estado pausado.
 */
@Composable
private fun MiniaturaEstilo(estilo: EstiloVisual, marcada: Boolean, tipo: TipoFase, destaque: Color, modifier: Modifier, aoTocar: () -> Unit) {
    Column(
        modifier
            .clip(FORMA_MINIATURA)
            .selectable(marcada, role = Role.RadioButton, onClick = aoTocar)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(FORMA_MINIATURA)
                .border(if (marcada) 2.dp else 1.dp, if (marcada) destaque else BordaVidroTopo, FORMA_MINIATURA),
            contentAlignment = Alignment.Center,
        ) {
            // ponytail: "parado" é o estado pausado do Estilo (o blob quase não se mexe). Um quadro congelado pediria gravar a camada.
            EstiloVisualAnimado(estilo, progresso = 0.6f, tipo = tipo, destaque = destaque, rodando = marcada, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(nomeDoEstiloRes(estilo)),
            style = EstiloCaption.copy(color = if (marcada) TextoPrimario else TextoSecundario),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@StringRes
internal fun nomeDoEstiloRes(estilo: EstiloVisual) = when (estilo) {
    EstiloVisual.BLOB_COM_VIDRO -> R.string.estilo_blob_com_vidro
    EstiloVisual.BLOB_PURO -> R.string.estilo_blob_puro
    EstiloVisual.ANEL_FLUIDO -> R.string.estilo_anel_fluido
    EstiloVisual.VIDRO_COM_PARALLAX -> R.string.estilo_vidro_com_parallax
}

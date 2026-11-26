package com.exemplo.pomodoro.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.dados.ARTES

private val FORMA = RoundedCornerShape(16.dp)

/**
 * Escolha da arte da Biblioteca, com miniaturas animadas (§7 Fundo).
 * A LazyRow só compõe as miniaturas visíveis: só elas animam. Todas seguem o mesmo relógio do Fundo.
 * ponytail: a fileira continua desenhando quando a seção rola para fora da tela. Pare o tempo pela visibilidade se pesar.
 */
@Composable
fun MiniaturasBiblioteca(escolhida: String, movimento: Boolean, aoEscolher: (String) -> Unit) {
    val tempo = rememberTempoFundo(movimento)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ARTES, key = { it.id }) { arte ->
            val marcada = arte.id == escolhida
            Column(
                Modifier.selectable(marcada, role = Role.RadioButton) { aoEscolher(arte.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShaderDeFundo(
                    arte.shader,
                    arte.paleta,
                    tempo,
                    Modifier
                        .size(64.dp, 128.dp)
                        .clip(FORMA)
                        .border(if (marcada) 2.dp else 1.dp, if (marcada) TextoPrimario else BordaVidroTopo, FORMA),
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(arte.nome), style = EstiloCaption, color = if (marcada) TextoPrimario else TextoSecundario)
            }
        }
    }
}

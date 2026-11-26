package com.exemplo.pomodoro.ui

import android.app.Activity
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AlertaNotificacao
import com.exemplo.pomodoro.dados.FonteSom
import com.exemplo.pomodoro.segundoplano.SONS
import com.exemplo.pomodoro.segundoplano.tocarSom
import com.exemplo.pomodoro.segundoplano.uriEmbutido

/** Seções Alerta e Notificação da Configuração do Perfil (§8). */
@Composable
fun SecoesAlertaNotificacao(a: AlertaNotificacao, aoMudar: (AlertaNotificacao) -> Unit) {
    val ctx = LocalContext.current

    // Prévia: escolher um som embutido toca o som. Uma prévia nova para a anterior.
    var previa by remember { mutableStateOf<Ringtone?>(null) }
    DisposableEffect(Unit) { onDispose { previa?.stop() } }
    fun ouvir(uri: Uri) {
        previa?.stop()
        previa = tocarSom(ctx, uri)
    }

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (r.resultCode == Activity.RESULT_OK && uri != null) aoMudar(a.copy(fonteSom = FonteSom.SISTEMA, somSistema = uri.toString()))
    }
    val nomeDoToque = remember(a.somSistema) {
        a.somSistema?.let { RingtoneManager.getRingtone(ctx, Uri.parse(it))?.getTitle(ctx) }
    }

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.secao_alerta), style = EstiloLabel)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup()) {
        SONS.forEach { som ->
            Opcao(stringResource(som.rotulo), null, a.fonteSom == FonteSom.EMBUTIDO && a.somEmbutido == som.nome) {
                aoMudar(a.copy(fonteSom = FonteSom.EMBUTIDO, somEmbutido = som.nome))
                ouvir(uriEmbutido(ctx, som.nome))
            }
        }
        Opcao(stringResource(R.string.som_sistema), nomeDoToque ?: stringResource(R.string.som_sistema_escolher), a.fonteSom == FonteSom.SISTEMA) {
            previa?.stop()
            seletor.launch(
                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false) // "Sem som" já é uma opção aqui
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, a.somSistema?.let(Uri::parse))
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, ctx.getString(R.string.escolher_som)),
            )
        }
        Opcao(stringResource(R.string.som_nenhum), null, a.fonteSom == FonteSom.NENHUM) {
            previa?.stop()
            aoMudar(a.copy(fonteSom = FonteSom.NENHUM))
        }
    }
    Chave(stringResource(R.string.vibracao), a.vibrar) { aoMudar(a.copy(vibrar = it)) }
    Chave(stringResource(R.string.tela_cheia), a.telaCheia) { aoMudar(a.copy(telaCheia = it)) }

    // Android 14 ou mais novo: o usuário pode negar a tela cheia. Sem callback, confere de novo ao voltar.
    var podeTelaCheia by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        podeTelaCheia = NotificationManagerCompat.from(ctx).canUseFullScreenIntent()
        onPauseOrDispose { }
    }
    if (a.telaCheia && !podeTelaCheia && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.tela_cheia_sem_permissao),
                style = EstiloCaption,
                color = TextoSecundario,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")),
                )
            }) { Text(stringResource(R.string.tela_cheia_permitir)) }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.secao_notificacao), style = EstiloLabel)
    Spacer(Modifier.height(8.dp))
    Chave(stringResource(R.string.botao_pausar_retomar), a.botaoPausarRetomar) { aoMudar(a.copy(botaoPausarRetomar = it)) }
    Chave(stringResource(R.string.botao_pular), a.botaoPular) { aoMudar(a.copy(botaoPular = it)) }
    Chave(stringResource(R.string.botao_estender), a.botaoEstender) { aoMudar(a.copy(botaoEstender = it)) }
    Chave(stringResource(R.string.notificacao_discreta), a.discreta) { aoMudar(a.copy(discreta = it)) }
}

@Composable
private fun Opcao(rotulo: String, detalhe: String?, escolhida: Boolean, aoEscolher: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(escolhida, role = Role.RadioButton, onClick = aoEscolher),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(rotulo, style = EstiloBody, color = TextoPrimario)
            if (detalhe != null) Text(detalhe, style = EstiloCaption, color = TextoSecundario)
        }
        RadioButton(selected = escolhida, onClick = null)
    }
}

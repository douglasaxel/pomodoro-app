package com.exemplo.pomodoro.segundoplano

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.dados.textoSobre
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.ui.EstiloBody
import com.exemplo.pomodoro.ui.EstiloTitle
import com.exemplo.pomodoro.ui.FundoComVeu
import com.exemplo.pomodoro.ui.TextoPrimario
import com.exemplo.pomodoro.ui.TextoSecundario
import com.exemplo.pomodoro.ui.tom
import java.lang.ref.WeakReference

/**
 * Alerta em tela cheia: a full-screen intent abre esta tela, que acende a tela bloqueada
 * (`showWhenLocked` e `turnScreenOn` no manifest). Só aparece se o Perfil pedir e a permissão existir.
 * Fica sobre o Fundo e o Véu do Perfil ativo, com o botão na Cor de destaque da Fase que vem (a que o Timer mostra).
 */
class AlertaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aberta = WeakReference(this)
        mostrar()
    }

    override fun onDestroy() {
        if (aberta?.get() === this) aberta = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mostrar()
    }

    private fun mostrar() {
        val titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty()
        val texto = intent.getStringExtra(EXTRA_TEXTO).orEmpty()
        val aSeguir = intent.getStringExtra(EXTRA_TIPO)?.let(TipoFase::valueOf) ?: TipoFase.FOCO
        val perfis = (application as AppPomodoro).perfis
        setContent {
            val perfil by perfis.ativo.collectAsStateWithLifecycle(null)
            val destaque = Color(perfil?.aparencia?.destaque(aSeguir) ?: DESTAQUES_PADRAO[aSeguir.ordinal])
            // O tom claro do botão primário do Timer (§3), com o texto que der mais contraste.
            val fundoBotao = tom(destaque, 0.86f, 0.55f)
            Box(Modifier.fillMaxSize()) {
                FundoComVeu(perfil?.aparencia, naTelaTimer = true)
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(titulo, style = EstiloTitle, color = TextoPrimario, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(texto, style = EstiloBody, color = TextoSecundario, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            // O Timer não abre sobre a tela bloqueada: o sistema pede o desbloqueio antes.
                            intentDoTimer(this@AlertaActivity)?.let(::startActivity)
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = fundoBotao,
                            contentColor = Color(textoSobre(fundoBotao.toArgb())),
                        ),
                        modifier = Modifier.height(56.dp),
                    ) { Text(stringResource(R.string.abrir_timer), style = EstiloBody) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = ::finish, modifier = Modifier.height(48.dp)) {
                        Text(stringResource(R.string.fechar), style = EstiloBody, color = TextoPrimario)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_TEXTO = "texto"
        /** A Fase que vem depois do Alerta: dá a Cor de destaque do botão. */
        const val EXTRA_TIPO = "tipo"

        private var aberta: WeakReference<AlertaActivity>? = null

        /** Fecha a tela do Alerta anterior, para o botão de ligar não acender um texto velho. */
        fun fechar() {
            aberta?.get()?.finish()
        }
    }
}

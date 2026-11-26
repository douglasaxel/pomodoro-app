package com.exemplo.pomodoro.segundoplano

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.ui.BaseEscura
import com.exemplo.pomodoro.ui.EstiloBody
import com.exemplo.pomodoro.ui.EstiloTitle
import com.exemplo.pomodoro.ui.TextoPrimario
import com.exemplo.pomodoro.ui.TextoSecundario
import com.exemplo.pomodoro.ui.TextoSobreDestaque
import com.exemplo.pomodoro.ui.corDeDestaque
import java.lang.ref.WeakReference

/**
 * Alerta em tela cheia: a full-screen intent abre esta tela, que acende a tela bloqueada
 * (`showWhenLocked` e `turnScreenOn` no manifest). Só aparece se o Perfil pedir e a permissão existir.
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
        setContent {
            Column(
                Modifier.fillMaxSize().background(BaseEscura).padding(24.dp),
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
                        containerColor = corDeDestaque(TipoFase.FOCO),
                        contentColor = TextoSobreDestaque,
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

    companion object {
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_TEXTO = "texto"

        private var aberta: WeakReference<AlertaActivity>? = null

        /** Fecha a tela do Alerta anterior, para o botão de ligar não acender um texto velho. */
        fun fechar() {
            aberta?.get()?.finish()
        }
    }
}

package com.exemplo.pomodoro.segundoplano

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.motor.Andamento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service `specialUse`: roda enquanto a Fase está rodando ou pausada e mostra a Notificação de contagem.
 * Não roda na contagem discreta: o Android sobe o canal de uma notificação de foreground service para LOW,
 * e o ícone apareceria na barra de status.
 */
class ServicoContagem : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent == null: o sistema recriou o serviço depois de matar o processo. Espera o estado voltar do DataStore;
        // com o controle já pronto, o immediate roda na hora.
        // Os botões da notificação vão para o ReceptorAcoes, que passa pelo ControleCiclo e chama este serviço de novo.
        val app = application as AppPomodoro
        app.escopo.launch(Dispatchers.Main.immediate) {
            val controle = app.controle.await()
            val e = controle.estadoAgora()
            val perfil = controle.perfilAtivo
            ServiceCompat.startForeground(
                this@ServicoContagem, ID_CONTAGEM, notificacaoContagem(this@ServicoContagem, e, perfil, controle.sessao.token),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            if (e.andamento == Andamento.PARADO || perfil.alerta.discreta) stopSelf()
        }
        return START_STICKY
    }
}

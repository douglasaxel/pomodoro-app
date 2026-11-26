package com.exemplo.pomodoro.segundoplano

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.motor.Andamento

/**
 * Foreground service `specialUse`: roda enquanto a Fase está rodando ou pausada e mostra a Notificação de contagem.
 * Não roda na contagem discreta: o Android sobe o canal de uma notificação de foreground service para LOW,
 * e o ícone apareceria na barra de status.
 */
class ServicoContagem : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent == null: o sistema recriou o serviço depois de matar o processo. O estado já veio do DataStore.
        // Os botões da notificação vão para o ReceptorAcoes, que passa pelo ControleCiclo e chama este serviço de novo.
        val controle = (application as AppPomodoro).controle
        val e = controle.estadoAgora()
        val perfil = controle.perfilAtivo
        ServiceCompat.startForeground(this, ID_CONTAGEM, notificacaoContagem(this, e, perfil), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        if (e.andamento == Andamento.PARADO || perfil.alerta.discreta) stopSelf()
        return START_STICKY
    }
}

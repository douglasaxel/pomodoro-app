package com.exemplo.pomodoro.segundoplano

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AcaoNotificacao
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.ui.formatarTempo
import com.exemplo.pomodoro.ui.nomeDaFaseRes

private const val ACAO_PAUSAR_RETOMAR = "com.exemplo.pomodoro.PAUSAR_RETOMAR"
private const val ACAO_PULAR = "com.exemplo.pomodoro.PULAR"
private const val ACAO_ESTENDER = "com.exemplo.pomodoro.ESTENDER"

/** Foreground service `specialUse`: roda enquanto a Fase está rodando ou pausada e mostra a Notificação de contagem. */
class ServicoContagem : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controle = (application as AppPomodoro).controle
        // Os comandos passam pelo ControleCiclo, que grava, reagenda e chama este serviço de novo.
        when (intent?.action) {
            ACAO_PAUSAR_RETOMAR -> controle.pausarOuRetomar()
            ACAO_PULAR -> controle.pular()
            ACAO_ESTENDER -> controle.estender()
        }
        // intent == null: o sistema recriou o serviço depois de matar o processo. O estado já veio do DataStore.
        val e = controle.estadoAgora()
        ServiceCompat.startForeground(this, ID_CONTAGEM, notificacao(e, controle.perfilAtivo), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        if (e.andamento == Andamento.PARADO) stopSelf()
        return START_STICKY
    }

    private fun notificacao(e: EstadoCiclo, perfil: Perfil): Notification {
        val a = perfil.alerta
        val rodando = e.andamento == Andamento.RODANDO
        // Discreta: canal de importância mínima, sem ícone na barra de status.
        return NotificationCompat.Builder(this, if (a.discreta) CANAL_CONTAGEM_DISCRETA else CANAL_CONTAGEM)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(getString(R.string.posicao_no_ciclo, getString(nomeDaFaseRes(e.tipo)), e.posicao, e.focosPorCiclo))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(abrirApp(this))
            .apply {
                if (rodando) {
                    // Cronômetro regressivo do sistema: a notificação não é atualizada a cada segundo.
                    // O `when` é tempo de parede, então parte do restante calculado pelo Motor.
                    setWhen(System.currentTimeMillis() + e.restanteMs)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                } else {
                    setShowWhen(false)
                    setContentText(getString(R.string.pausado_restante, formatarTempo(e.restanteMs, e.duracaoMs)))
                }
            }
            .apply {
                // Os botões que o Perfil escolhe. Cada um tem um requestCode próprio.
                for (botao in a.acoes(rodando)) when (botao) {
                    AcaoNotificacao.PAUSAR -> addAction(R.drawable.ic_pausar, getString(R.string.pausar), acao(ACAO_PAUSAR_RETOMAR, 1))
                    AcaoNotificacao.RETOMAR -> addAction(R.drawable.ic_iniciar, getString(R.string.retomar), acao(ACAO_PAUSAR_RETOMAR, 1))
                    AcaoNotificacao.PULAR -> addAction(R.drawable.ic_pular, getString(R.string.pular_fase), acao(ACAO_PULAR, 2))
                    AcaoNotificacao.ESTENDER -> addAction(
                        R.drawable.ic_adicionar,
                        getString(R.string.estender_selo, (perfil.tempo.estenderMs / 60_000L).toInt()),
                        acao(ACAO_ESTENDER, 3),
                    )
                }
            }
            .build()
    }

    private fun acao(acao: String, codigo: Int) = PendingIntent.getService(
        this, codigo, Intent(this, ServicoContagem::class.java).setAction(acao), PendingIntent.FLAG_IMMUTABLE,
    )
}

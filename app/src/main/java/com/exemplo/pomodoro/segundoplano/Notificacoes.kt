package com.exemplo.pomodoro.segundoplano

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.exemplo.pomodoro.MainActivity
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AcaoNotificacao
import com.exemplo.pomodoro.dados.AlertaNotificacao
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.motor.paraMinutos
import com.exemplo.pomodoro.ui.formatarTempo
import com.exemplo.pomodoro.ui.nomeDaFaseRes

private const val CANAL_CONTAGEM = "contagem"
private const val CANAL_CONTAGEM_DISCRETA = "contagem_discreta"
private const val CANAL_ALERTA = "alerta"

const val ID_CONTAGEM = 1
private const val ID_ALERTA = 2
/** Outro id: a notificação do serviço some quando ele para, e não pode levar a discreta junto. */
const val ID_CONTAGEM_DISCRETA = 3

private const val ACAO_PAUSAR_RETOMAR = "com.exemplo.pomodoro.PAUSAR_RETOMAR"
private const val ACAO_PULAR = "com.exemplo.pomodoro.PULAR"
private const val ACAO_ESTENDER = "com.exemplo.pomodoro.ESTENDER"

/** Os três canais são fixos: o Android não deixa mudar a importância depois que o canal existe. */
fun criarCanais(ctx: Context) {
    ctx.getSystemService(NotificationManager::class.java).createNotificationChannels(
        listOf(
            NotificationChannel(CANAL_CONTAGEM, ctx.getString(R.string.canal_contagem), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
            NotificationChannel(CANAL_CONTAGEM_DISCRETA, ctx.getString(R.string.canal_contagem_discreta), NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) },
            // O canal do Alerta é mudo: o app toca o som e vibra conforme o Perfil (veja alertar).
            NotificationChannel(CANAL_ALERTA, ctx.getString(R.string.canal_alerta), NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null); enableVibration(false) },
        ),
    )
}

/**
 * O comum ao Alerta e à Notificação de contagem: o ícone, a visibilidade na tela bloqueada e o toque, que abre o app
 * onde ele estava, na tela Timer. O extra diz que não foi o launcher: sem splash animada.
 */
private fun notificacaoBase(ctx: Context, canal: String) = NotificationCompat.Builder(ctx, canal)
    .setSmallIcon(R.drawable.ic_notificacao)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setContentIntent(PendingIntent.getActivity(ctx, 0, intentDoTimer(ctx), PendingIntent.FLAG_IMMUTABLE))

/**
 * A intent do launcher, marcada como aberta pela Notificação de contagem ou pelo Alerta. Com o app aberto, a
 * MainActivity (singleTop) recebe em onNewIntent e vai para a aba Timer.
 */
fun intentDoTimer(ctx: Context): Intent? =
    ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.putExtra(MainActivity.EXTRA_PELA_NOTIFICACAO, true)

/**
 * Alerta: notificação, som e vibração, conforme o Perfil. O canal é mudo, então o som e a vibração saem daqui.
 * Com "Acender a tela bloqueada" e a permissão de tela cheia, a notificação abre a [AlertaActivity].
 * Sem a permissão, fica a notificação comum de prioridade alta.
 */
fun alertar(ctx: Context, terminou: TipoFase, aSeguir: TipoFase, a: AlertaNotificacao, som: MediaPlayer?) {
    // A notificação nova não pode atualizar a anterior: a tela cheia só abre numa notificação nova.
    dispensarAlerta(ctx)
    val titulo = ctx.getString(R.string.alerta_titulo, ctx.getString(nomeDaFaseRes(terminou)))
    val texto = ctx.getString(R.string.alerta_texto, ctx.getString(nomeDaFaseRes(aSeguir)))
    val notificacao = notificacaoBase(ctx, CANAL_ALERTA)
        .setContentTitle(titulo)
        .setContentText(texto)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .apply {
            if (a.telaCheia && NotificationManagerCompat.from(ctx).canUseFullScreenIntent()) {
                val tela = Intent(ctx, AlertaActivity::class.java)
                    .putExtra(AlertaActivity.EXTRA_TITULO, titulo)
                    .putExtra(AlertaActivity.EXTRA_TEXTO, texto)
                setFullScreenIntent(
                    PendingIntent.getActivity(ctx, 0, tela, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                    true,
                )
            }
        }
        .build()
    // Sem POST_NOTIFICATIONS o sistema descarta a notificação, mas o som e a vibração saem mesmo assim.
    ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERTA, notificacao)

    som?.tocar()
    if (a.vibrar) {
        ctx.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1),
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
        )
    }
}

/** Tira o Alerta anterior, a notificação e a tela cheia, quando a próxima Fase começa. Roda na thread principal. */
fun dispensarAlerta(ctx: Context) {
    ctx.getSystemService(NotificationManager::class.java).cancel(ID_ALERTA)
    AlertaActivity.fechar()
}

/**
 * Notificação de contagem, com o cronômetro regressivo e os botões que o Perfil escolhe.
 * Normal: é a do [ServicoContagem]. Discreta: notificação comum no canal de importância mínima, sem ícone na barra.
 */
fun notificacaoContagem(ctx: Context, e: EstadoCiclo, perfil: Perfil): Notification {
    val a = perfil.alerta
    val rodando = e.andamento == Andamento.RODANDO
    return notificacaoBase(ctx, if (a.discreta) CANAL_CONTAGEM_DISCRETA else CANAL_CONTAGEM)
        .setContentTitle(ctx.getString(R.string.posicao_no_ciclo, ctx.getString(nomeDaFaseRes(e.tipo)), e.posicao, e.focosPorCiclo))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
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
                setContentText(ctx.getString(R.string.pausado_restante, formatarTempo(e.restanteMs, e.duracaoMs)))
            }
            // Os botões que o Perfil escolhe. Cada um tem um requestCode próprio.
            for (botao in a.acoes(rodando)) when (botao) {
                AcaoNotificacao.PAUSAR -> addAction(R.drawable.ic_pausar, ctx.getString(R.string.pausar), acao(ctx, ACAO_PAUSAR_RETOMAR, 1))
                AcaoNotificacao.RETOMAR -> addAction(R.drawable.ic_iniciar, ctx.getString(R.string.retomar), acao(ctx, ACAO_PAUSAR_RETOMAR, 1))
                AcaoNotificacao.PULAR -> addAction(R.drawable.ic_pular, ctx.getString(R.string.pular_fase), acao(ctx, ACAO_PULAR, 2))
                AcaoNotificacao.ESTENDER -> addAction(
                    R.drawable.ic_adicionar,
                    ctx.getString(R.string.estender_selo, paraMinutos(perfil.tempo.estenderMs)),
                    acao(ctx, ACAO_ESTENDER, 3),
                )
            }
        }
        .build()
}

// Broadcast, não o serviço: na contagem discreta não há serviço rodando.
private fun acao(ctx: Context, acao: String, codigo: Int) = PendingIntent.getBroadcast(
    ctx, codigo, Intent(ctx, ReceptorAcoes::class.java).setAction(acao), PendingIntent.FLAG_IMMUTABLE,
)

/** Os botões da Notificação de contagem. Funcionam com ou sem o serviço, e com o processo morto. */
class ReceptorAcoes : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = comControle(ctx) { controle ->
        // O ControleCiclo grava, reagenda e refaz a notificação.
        when (intent.action) {
            ACAO_PAUSAR_RETOMAR -> controle.pausarOuRetomar()
            ACAO_PULAR -> controle.pular()
            ACAO_ESTENDER -> controle.estender()
        }
    }
}

package com.exemplo.pomodoro.segundoplano

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AlertaNotificacao
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.ui.nomeDaFaseRes

const val CANAL_CONTAGEM = "contagem"
const val CANAL_CONTAGEM_DISCRETA = "contagem_discreta"
const val CANAL_ALERTA = "alerta"

const val ID_CONTAGEM = 1
private const val ID_ALERTA = 2

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

/** Abre o app onde ele estava, na tela Timer. */
fun abrirApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
    ctx, 0, ctx.packageManager.getLaunchIntentForPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE,
)

/**
 * Alerta: notificação, som e vibração, conforme o Perfil. O canal é mudo, então o som e a vibração saem daqui.
 * Com "Acender a tela bloqueada" e a permissão de tela cheia, a notificação abre a [AlertaActivity].
 * Sem a permissão, fica a notificação comum de prioridade alta.
 */
fun alertar(ctx: Context, terminou: TipoFase, aSeguir: TipoFase, a: AlertaNotificacao, som: MediaPlayer?) {
    val titulo = ctx.getString(R.string.alerta_titulo, ctx.getString(nomeDaFaseRes(terminou)))
    val texto = ctx.getString(R.string.alerta_texto, ctx.getString(nomeDaFaseRes(aSeguir)))
    val notificacao = NotificationCompat.Builder(ctx, CANAL_ALERTA)
        .setSmallIcon(R.drawable.ic_notificacao)
        .setContentTitle(titulo)
        .setContentText(texto)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setContentIntent(abrirApp(ctx))
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

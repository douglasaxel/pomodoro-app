package com.exemplo.pomodoro.segundoplano

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.exemplo.pomodoro.R
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
            // O canal do Alerta é mudo: o app toca o som e vibra, para o Perfil poder escolher (ticket 05).
            NotificationChannel(CANAL_ALERTA, ctx.getString(R.string.canal_alerta), NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null); enableVibration(false) },
        ),
    )
}

/** Abre o app onde ele estava, na tela Timer. */
fun abrirApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
    ctx, 0, ctx.packageManager.getLaunchIntentForPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE,
)

/** Alerta: notificação, som padrão e vibração. */
fun alertar(ctx: Context, terminou: TipoFase, aSeguir: TipoFase) {
    val notificacao = NotificationCompat.Builder(ctx, CANAL_ALERTA)
        .setSmallIcon(R.drawable.ic_notificacao)
        .setContentTitle(ctx.getString(R.string.alerta_titulo, ctx.getString(nomeDaFaseRes(terminou))))
        .setContentText(ctx.getString(R.string.alerta_texto, ctx.getString(nomeDaFaseRes(aSeguir))))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setContentIntent(abrirApp(ctx))
        .build()
    // Sem POST_NOTIFICATIONS o sistema descarta a notificação, mas o som e a vibração saem mesmo assim.
    ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERTA, notificacao)

    val uso = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        ?.apply { audioAttributes = uso }
        ?.play()
    ctx.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1),
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
    )
}

package com.exemplo.pomodoro.segundoplano

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AlertaNotificacao
import com.exemplo.pomodoro.dados.FonteSom

/** Um som embutido. O [nome] é o que o Perfil guarda e o nome do arquivo em `res/raw`. */
class SomEmbutido(val nome: String, @RawRes val arquivo: Int, @StringRes val rotulo: Int)

/** Gerados por `ferramentas/gerar_sons.py`. O primeiro é o padrão ([com.exemplo.pomodoro.dados.SOM_PADRAO]). */
val SONS = listOf(
    SomEmbutido("sino", R.raw.sino, R.string.som_sino),
    SomEmbutido("sino_duplo", R.raw.sino_duplo, R.string.som_sino_duplo),
    SomEmbutido("marimba", R.raw.marimba, R.string.som_marimba),
    SomEmbutido("tigela", R.raw.tigela, R.string.som_tigela),
    SomEmbutido("kalimba", R.raw.kalimba, R.string.som_kalimba),
    SomEmbutido("carrilhao", R.raw.carrilhao, R.string.som_carrilhao),
)

/** O som toca por no máximo este tempo. Um toque do sistema longo não fica tocando. */
const val DURACAO_MAX_SOM_MS = 5_000L

fun uriEmbutido(ctx: Context, nome: String): Uri {
    val som = SONS.find { it.nome == nome } ?: SONS.first()
    return Uri.parse("android.resource://${ctx.packageName}/${som.arquivo}")
}

/** O som que o Alerta toca, ou null sem som. Sem toque do sistema escolhido, vale o som embutido. */
fun uriDoSom(ctx: Context, a: AlertaNotificacao): Uri? = when (a.fonteSom) {
    FonteSom.EMBUTIDO -> uriEmbutido(ctx, a.somEmbutido)
    FonteSom.SISTEMA -> a.somSistema?.let(Uri::parse) ?: uriEmbutido(ctx, a.somEmbutido)
    FonteSom.NENHUM -> null
}

/** Toca no volume de alarme e para sozinho depois de [DURACAO_MAX_SOM_MS]. */
fun tocarSom(ctx: Context, uri: Uri): Ringtone? = RingtoneManager.getRingtone(ctx, uri)?.apply {
    audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    play()
    Handler(Looper.getMainLooper()).postDelayed(::stop, DURACAO_MAX_SOM_MS)
}

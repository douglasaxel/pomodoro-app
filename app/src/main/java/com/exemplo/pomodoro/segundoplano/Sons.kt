package com.exemplo.pomodoro.segundoplano

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
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
private fun uriDoSom(ctx: Context, a: AlertaNotificacao): Uri? = when (a.fonteSom) {
    FonteSom.EMBUTIDO -> uriEmbutido(ctx, a.somEmbutido)
    FonteSom.SISTEMA -> a.somSistema?.let(Uri::parse) ?: uriEmbutido(ctx, a.somEmbutido)
    FonteSom.NENHUM -> null
}

private val NO_VOLUME_DE_ALARME = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/**
 * Cria e prepara o player uma vez só, já no volume de alarme. Lê o arquivo: chame fora da thread principal
 * quando puder. O Ringtone recriava o player ao receber os atributos, e o Alerta pagava o custo duas vezes.
 */
fun prepararSom(ctx: Context, uri: Uri): MediaPlayer? =
    MediaPlayer.create(ctx, uri, null, NO_VOLUME_DE_ALARME, AudioManager.AUDIO_SESSION_ID_GENERATE)

/** O som do Alerta. Se o toque do sistema não abrir (apagado, por exemplo), toca o som embutido. */
fun prepararSom(ctx: Context, a: AlertaNotificacao): MediaPlayer? =
    uriDoSom(ctx, a)?.let { prepararSom(ctx, it) ?: prepararSom(ctx, uriEmbutido(ctx, a.somEmbutido)) }

/** Toca e solta o player depois de [DURACAO_MAX_SOM_MS]. Soltar de novo (a prévia) não faz mal. */
fun MediaPlayer.tocar(): MediaPlayer = apply {
    start()
    Handler(Looper.getMainLooper()).postDelayed(::release, DURACAO_MAX_SOM_MS)
}

/** A prévia da Configuração: prepara e toca na hora. */
fun tocarSom(ctx: Context, uri: Uri): MediaPlayer? = prepararSom(ctx, uri)?.tocar()

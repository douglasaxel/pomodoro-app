package com.exemplo.pomodoro.segundoplano

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.dados.AcaoNotificacao
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.paraMinutos
import com.exemplo.pomodoro.ui.formatarTempo
import com.exemplo.pomodoro.ui.nomeDaFaseRes
import android.text.format.DateFormat
import java.util.Date

private const val ACAO_ESTENDER_SESSAO = "estender"
private const val BASE_ESCURA = 0xFF0B0B10.toInt()

/**
 * A Fase como se fosse uma faixa tocando: o Android mostra o player na tela de bloqueio, nas Configurações rápidas e
 * na Now Bar da Samsung, com a barra de progresso e os botões do Perfil. Enquanto a Fase roda, o sistema não deixa
 * dispensar o player. Fica ativa só com a Notificação de contagem normal (a discreta não aparece na tela de bloqueio).
 * Os botões chegam na thread principal e passam pelo [ControleCiclo], como os da notificação.
 */
class SessaoMidia(ctx: Context, controle: ControleCiclo) {

    private val app = ctx.applicationContext
    private val sessao = MediaSession(app, "pomodoro").apply {
        setSessionActivity(
            android.app.PendingIntent.getActivity(app, 0, intentDoTimer(app), android.app.PendingIntent.FLAG_IMMUTABLE),
        )
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() { if (controle.estadoAgora().andamento == Andamento.PAUSADO) controle.retomar() }
            override fun onPause() { if (controle.estadoAgora().andamento == Andamento.RODANDO) controle.pausar() }
            override fun onSkipToNext() = controle.pular()
            override fun onCustomAction(acao: String, extras: android.os.Bundle?) {
                if (acao == ACAO_ESTENDER_SESSAO) controle.estender()
            }
        })
    }

    val token: MediaSession.Token get() = sessao.sessionToken

    private var capa: Pair<Int, Bitmap>? = null

    fun atualizar(e: EstadoCiclo, perfil: Perfil, ativa: Boolean) {
        if (!ativa) {
            if (sessao.isActive) sessao.isActive = false
            return
        }
        val rodando = e.andamento == Andamento.RODANDO
        val destaque = perfil.aparencia.destaque(e.tipo)
        sessao.setMetadata(
            MediaMetadata.Builder()
                .putString(
                    MediaMetadata.METADATA_KEY_TITLE,
                    app.getString(R.string.posicao_no_ciclo, app.getString(nomeDaFaseRes(e.tipo)), e.posicao, e.focosPorCiclo),
                )
                // O player não mostra o tempo restante. Rodando, a hora do fim não muda: sem atualizar a cada segundo.
                .putString(
                    MediaMetadata.METADATA_KEY_ARTIST,
                    if (rodando) app.getString(R.string.player_ate, perfil.nome, horaDoFim(e.restanteMs))
                    else app.getString(R.string.player_pausado, perfil.nome, formatarTempo(e.restanteMs, e.duracaoMs)),
                )
                .putLong(MediaMetadata.METADATA_KEY_DURATION, e.duracaoMs)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, capa(destaque))
                .build(),
        )
        // Sem ACTION_SEEK_TO: a barra mostra o progresso da Fase, mas não arrasta. Com velocidade 1, o sistema
        // anda a barra sozinho a partir do updateTime (elapsedRealtime, a mesma base do Motor).
        var acoes = PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
        val estado = PlaybackState.Builder()
        for (botao in perfil.alerta.acoes(rodando)) when (botao) {
            AcaoNotificacao.PULAR -> acoes = acoes or PlaybackState.ACTION_SKIP_TO_NEXT
            AcaoNotificacao.ESTENDER -> estado.addCustomAction(
                ACAO_ESTENDER_SESSAO,
                app.getString(R.string.estender_selo, paraMinutos(perfil.tempo.estenderMs)),
                R.drawable.ic_adicionar,
            )
            else -> Unit // Pausar e Retomar: o primeiro botão do player sempre segue o estado
        }
        sessao.setPlaybackState(
            estado.setActions(acoes)
                .setState(
                    if (rodando) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    e.duracaoMs - e.restanteMs,
                    if (rodando) 1f else 0f,
                    SystemClock.elapsedRealtime(),
                )
                .build(),
        )
        if (!sessao.isActive) sessao.isActive = true
    }

    private fun horaDoFim(restanteMs: Long) =
        DateFormat.getTimeFormat(app).format(Date(System.currentTimeMillis() + restanteMs))

    /** A capa do player: o blob na Cor de destaque da Fase sobre a base escura do app. Refeita só quando a cor muda. */
    private fun capa(cor: Int): Bitmap {
        capa?.let { (c, b) -> if (c == cor) return b }
        val lado = 512
        val b = createBitmap(lado, lado)
        Canvas(b).apply {
            drawColor(BASE_ESCURA)
            val centro = lado / 2f
            val brilho = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centro, centro, lado * 0.48f,
                    intArrayOf(ColorUtils.setAlphaComponent(cor, 150), ColorUtils.setAlphaComponent(cor, 0)),
                    null, Shader.TileMode.CLAMP,
                )
            }
            drawCircle(centro, centro, lado * 0.48f, brilho)
            val miolo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centro, centro * 0.9f, lado * 0.3f,
                    intArrayOf(ColorUtils.blendARGB(cor, 0xFFFFFFFF.toInt(), 0.25f), cor, ColorUtils.blendARGB(cor, BASE_ESCURA, 0.45f)),
                    floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
                )
            }
            drawCircle(centro, centro, lado * 0.28f, miolo)
        }
        capa = cor to b
        return b
    }
}

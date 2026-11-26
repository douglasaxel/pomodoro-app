package com.exemplo.pomodoro.segundoplano

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exemplo.pomodoro.dados.FaseRegistrada
import com.exemplo.pomodoro.dados.Historico
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.dados.Perfis
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.MotorCiclo
import com.exemplo.pomodoro.motor.RegistroFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dadosDoMotor by preferencesDataStore(name = "motor")
private val CHAVE_ESTADO = stringPreferencesKey("estado")

const val EXTRA_TIPO_QUE_TERMINA = "tipo"

/**
 * Liga o Motor do Ciclo ao Android. A tela, a Notificação de contagem e o fim da Fase passam por aqui.
 * A cada comando: grava o estado, agenda o fim da Fase e liga ou desliga o [ServicoContagem].
 */
class ControleCiclo(
    private val app: Context,
    perfis: Perfis,
    private val historico: Historico,
    escopo: CoroutineScope,
) {

    // O Tempo do Perfil ativo entra antes da restauração: as durações e o início automático dependem dele.
    // A leitura é síncrona, mas fora da thread principal no alarme: o AppPomodoro cria o controle no primeiro uso.
    private var perfil = runBlocking { perfis.garantirPadrao(); perfis.ativo.first() }

    // SystemClock.elapsedRealtime: monotônico, conta o sono profundo e é a mesma base do alarme exato.
    // Cada Fase encerrada vai para o Histórico com o Perfil em que rodou.
    private val motor = MotorCiclo(perfil.tempo, SystemClock::elapsedRealtime, ::registrar)

    private fun registrar(r: RegistroFase) {
        // O elapsedRealtime conta desde o boot: a diferença para o relógio de parede leva os instantes para epoch ms.
        val paraParede = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        historico.registrar(
            FaseRegistrada(
                tipo = r.tipo,
                inicioEpochMs = r.inicioMs + paraParede,
                fimEpochMs = r.fimMs + paraParede,
                minutosEfetivos = r.minutosEfetivos,
                concluida = r.concluida,
                perfilId = perfil.id,
                perfilNome = perfil.nome,
            ),
        )
    }

    // Leitura síncrona na criação, para o estado já estar certo quando a tela, o serviço ou o receptor perguntam.
    private val _estado = MutableStateFlow(
        motor.apply { runBlocking { app.dadosDoMotor.data.first()[CHAVE_ESTADO] }?.let(::restaurar) }.estado(),
    )

    /** Muda a cada comando e no fim de cada Fase. Para o tempo de agora, use [estadoAgora]. */
    val estado: StateFlow<EstadoCiclo> = _estado.asStateFlow()

    fun estadoAgora(): EstadoCiclo = motor.estado()

    /** O Perfil ativo, para o Alerta e a Notificação de contagem. */
    val perfilAtivo: Perfil get() = perfil

    // A escrita sai da thread principal. Só vale o último estado, e uma escrita por vez mantém a ordem.
    // ponytail: um kill -9 nos milissegundos depois de um comando perde esse comando. O alarme continua agendado.
    private val gravacoes = Channel<String>(Channel.CONFLATED)

    init {
        escopo.launch(Dispatchers.IO) {
            for (s in gravacoes) app.dadosDoMotor.edit { it[CHAVE_ESTADO] = s }
        }
        escopo.launch {
            perfis.ativo.collect { p ->
                val anterior = perfil
                // Outro Perfil encerra a Fase. O mesmo Perfil editado muda o Tempo a partir da próxima Fase.
                // O [perfil] só muda depois do motor (a Fase encerrada é registrada no Perfil em que rodou)
                // e antes do sincronizar (a Notificação de contagem já sai com o Perfil novo).
                if (p.id != anterior.id) comando { motor.trocarPerfil(p.tempo); perfil = p }
                else if (p.tempo != anterior.tempo) comando { motor.atualizarConfig(p.tempo); perfil = p }
                else {
                    perfil = p
                    // Botões ou modo discreto mudaram: a Notificação de contagem é refeita na hora.
                    if (p.alerta != anterior.alerta) sincronizar()
                }
            }
        }
    }

    fun iniciar() = comando { motor.iniciar() }
    fun pausar() = comando { motor.pausar() }
    fun retomar() = comando { motor.retomar() }
    fun pular() = comando { motor.pular() }
    fun estender() = comando { motor.estender() }
    fun reiniciarCiclo() = comando { motor.reiniciarCiclo() }
    fun pausarOuRetomar() = comando {
        if (motor.estado().andamento == Andamento.RODANDO) motor.pausar() else motor.retomar()
    }

    private fun comando(acao: () -> Unit) {
        acao()
        sincronizar()
    }

    /** Grava o estado e acerta o alarme exato e o serviço. Também roda no fim da Fase. */
    fun sincronizar() {
        val e = motor.estado()
        _estado.value = e
        gravacoes.trySend(motor.serializar())
        agendarFimDaFase(e)
        val servico = Intent(app, ServicoContagem::class.java)
        if (e.andamento == Andamento.PARADO) app.stopService(servico)
        else ContextCompat.startForegroundService(app, servico)
    }

    private fun agendarFimDaFase(e: EstadoCiclo) {
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            app, 0,
            Intent(app, ReceptorFimDaFase::class.java).putExtra(EXTRA_TIPO_QUE_TERMINA, e.tipo.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // USE_EXACT_ALARM é concedida na instalação: canScheduleExactAlarms() é sempre true.
        if (e.andamento == Andamento.RODANDO) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + e.restanteMs, pi,
            )
        } else {
            am.cancel(pi)
        }
    }
}

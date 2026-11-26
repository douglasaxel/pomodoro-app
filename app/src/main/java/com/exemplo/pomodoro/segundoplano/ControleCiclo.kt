package com.exemplo.pomodoro.segundoplano

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
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
import kotlinx.coroutines.withContext

private val Context.dadosDoMotor by preferencesDataStore(name = "motor")
private val CHAVE_ESTADO = stringPreferencesKey("estado")

const val EXTRA_TIPO_QUE_TERMINA = "tipo"

/**
 * Liga o Motor do Ciclo ao Android. A tela, a Notificação de contagem e o fim da Fase passam por aqui.
 * A cada comando: grava o estado, agenda o fim da Fase e liga ou desliga o [ServicoContagem] (ou a contagem discreta).
 * Nasce por [criar], que lê o Perfil ativo e o estado gravado fora da thread principal.
 */
class ControleCiclo private constructor(
    private val app: Context,
    perfis: Perfis,
    private val historico: Historico,
    escopo: CoroutineScope,
    perfilInicial: Perfil,
    gravado: String?,
) {

    /** O Perfil ativo, também para o Alerta e a Notificação de contagem. */
    var perfilAtivo = perfilInicial
        private set

    // SystemClock.elapsedRealtime: monotônico, conta o sono profundo e é a mesma base do alarme exato.
    // Ele recomeça no boot: o BOOT_COUNT e o relógio de parede levam o estado gravado para o boot novo.
    // Cada Fase encerrada vai para o Histórico com o Perfil em que rodou.
    private val motor = MotorCiclo(
        perfilAtivo.tempo,
        SystemClock::elapsedRealtime,
        ::registrar,
        boot = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, 0),
        paraParede = ::paraParede,
    )

    /** O elapsedRealtime conta desde o boot: somar esta diferença leva os instantes do Motor para epoch ms. */
    private fun paraParede() = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    private fun registrar(r: RegistroFase) {
        val paraParede = paraParede()
        historico.registrar(
            FaseRegistrada(
                tipo = r.tipo,
                inicioEpochMs = r.inicioMs + paraParede,
                fimEpochMs = r.fimMs + paraParede,
                minutosEfetivos = r.minutosEfetivos,
                concluida = r.concluida,
                perfilId = perfilAtivo.id,
                perfilNome = perfilAtivo.nome,
            ),
        )
    }

    /** O player da tela de bloqueio. Nasce na thread principal, como o controle. */
    val sessao = SessaoMidia(app, this)

    // O Tempo do Perfil entra antes da restauração: as durações e o início automático dependem dele.
    private val _estado = MutableStateFlow(motor.apply { gravado?.let(::restaurar) }.estado())

    /** Muda a cada comando e no fim de cada Fase. Para o tempo de agora, use [estadoAgora]. */
    val estado: StateFlow<EstadoCiclo> = _estado.asStateFlow()

    fun estadoAgora(): EstadoCiclo = motor.estado()

    // A escrita sai da thread principal. Só vale o último estado, e uma escrita por vez mantém a ordem.
    // ponytail: um kill -9 nos milissegundos depois de um comando perde esse comando. O alarme continua agendado.
    private val gravacoes = Channel<String>(Channel.CONFLATED)

    init {
        escopo.launch(Dispatchers.IO) {
            for (s in gravacoes) app.dadosDoMotor.edit { it[CHAVE_ESTADO] = s }
        }
        escopo.launch {
            perfis.ativo.collect { p ->
                val anterior = perfilAtivo
                // Outro Perfil encerra a Fase. O mesmo Perfil editado muda o Tempo a partir da próxima Fase.
                // O [perfilAtivo] só muda depois do motor (a Fase encerrada é registrada no Perfil em que rodou)
                // e antes do sincronizar (a Notificação de contagem já sai com o Perfil novo).
                if (p.id != anterior.id) comando { motor.trocarPerfil(p.tempo); perfilAtivo = p }
                else if (p.tempo != anterior.tempo) comando { motor.atualizarConfig(p.tempo); perfilAtivo = p }
                else {
                    perfilAtivo = p
                    // Botões ou modo discreto mudaram: a Notificação de contagem é refeita na hora.
                    if (p.alerta != anterior.alerta) sincronizar()
                }
            }
        }
        // O processo renasceu com a Fase em andamento (serviço recriado, app aberto, atualização do app):
        // o serviço, o player da tela de bloqueio e o alarme voltam sem esperar um comando.
        if (_estado.value.andamento != Andamento.PARADO) sincronizar()
    }

    // Iniciar e Pular começam a próxima Fase: o Alerta anterior sai (o início automático sai no alertar).
    fun iniciar() = comando { motor.iniciar(); dispensarAlerta(app) }
    fun pausar() = comando { motor.pausar() }
    fun retomar() = comando { motor.retomar() }
    fun pular() = comando { motor.pular(); dispensarAlerta(app) }
    fun estender() = comando { motor.estender() }
    fun reiniciarCiclo() = comando { motor.reiniciarCiclo() }
    fun pausarOuRetomar() = comando {
        if (motor.estado().andamento == Andamento.RODANDO) motor.pausar() else motor.retomar()
    }

    private fun comando(acao: () -> Unit) {
        acao()
        sincronizar()
    }

    /** Grava o estado e acerta o alarme exato, o serviço e a Notificação de contagem. Também roda no fim da Fase. */
    fun sincronizar() {
        val e = motor.estado()
        _estado.value = e
        gravacoes.trySend(motor.serializar())
        agendarFimDaFase(e)
        val emAndamento = e.andamento != Andamento.PARADO
        // Discreta: notificação comum no canal mínimo, sem o serviço (o foreground service subiria o canal para LOW).
        // O alarme exato fecha a Fase e toca o Alerta sozinho, e o estado volta do DataStore se o processo morrer.
        val discreta = emAndamento && perfilAtivo.alerta.discreta
        sessao.atualizar(e, perfilAtivo, ativa = emAndamento && !discreta)
        val servico = Intent(app, ServicoContagem::class.java)
        if (emAndamento && !discreta) ContextCompat.startForegroundService(app, servico)
        else app.stopService(servico)
        val nm = app.getSystemService(NotificationManager::class.java)
        if (discreta) nm.notify(ID_CONTAGEM_DISCRETA, notificacaoContagem(app, e, perfilAtivo))
        else nm.cancel(ID_CONTAGEM_DISCRETA)
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

    companion object {
        /** Lê o Perfil ativo (criando o padrão na primeira abertura) e o estado gravado, fora da thread principal. */
        suspend fun criar(app: Context, perfis: Perfis, historico: Historico, escopo: CoroutineScope): ControleCiclo {
            val (perfil, gravado) = withContext(Dispatchers.IO) {
                perfis.garantirPadrao()
                perfis.ativo.first() to app.dadosDoMotor.data.first()[CHAVE_ESTADO]
            }
            return ControleCiclo(app, perfis, historico, escopo, perfil, gravado)
        }
    }
}

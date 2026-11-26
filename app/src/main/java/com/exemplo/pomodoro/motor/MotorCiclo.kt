package com.exemplo.pomodoro.motor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class TipoFase { FOCO, PAUSA_CURTA, PAUSA_LONGA }

enum class Andamento { PARADO, RODANDO, PAUSADO }

const val MS_POR_MIN = 60_000L

/** Os minutos inteiros de [ms], arredondados para baixo. */
fun paraMinutos(ms: Long) = (ms / MS_POR_MIN).toInt()

/** Parte do Perfil que o Motor usa: o Tempo (durações das Fases, N do Ciclo, Estender e início automático). */
data class ConfigCiclo(
    val focoMs: Long,
    val pausaCurtaMs: Long,
    val pausaLongaMs: Long,
    val focosPorCiclo: Int,
    val estenderMs: Long = 5 * MS_POR_MIN,
    val autoIniciarFocos: Boolean = false,
    val autoIniciarPausas: Boolean = false,
) {
    fun duracaoMs(tipo: TipoFase) = when (tipo) {
        TipoFase.FOCO -> focoMs
        TipoFase.PAUSA_CURTA -> pausaCurtaMs
        TipoFase.PAUSA_LONGA -> pausaLongaMs
    }

    companion object {
        /** Tempo do Perfil padrão (25/5/15, N = 4). */
        val PADRAO = ConfigCiclo(focoMs = 25 * MS_POR_MIN, pausaCurtaMs = 5 * MS_POR_MIN, pausaLongaMs = 15 * MS_POR_MIN, focosPorCiclo = 4)
    }
}

data class EstadoCiclo(
    val tipo: TipoFase,
    /** Número do Foco atual (ou do Foco que a Pausa segue), de 1 a [focosPorCiclo]. */
    val posicao: Int,
    val focosPorCiclo: Int,
    val duracaoMs: Long,
    val restanteMs: Long,
    val andamento: Andamento,
)

/** Uma Fase encerrada, pronta para o Histórico. [inicioMs] e [fimMs] estão na base do relógio do Motor. */
data class RegistroFase(
    val tipo: TipoFase,
    val inicioMs: Long,
    val fimMs: Long,
    val minutosEfetivos: Int,
    val concluida: Boolean,
)

/**
 * Motor do Ciclo: máquina de estados em Kotlin puro, sem Android.
 * [relogio] devolve um tempo monotônico em milissegundos.
 * [aoRegistrar] recebe cada Fase encerrada que entra no Histórico.
 */
class MotorCiclo(
    private var config: ConfigCiclo,
    private val relogio: () -> Long,
    private val aoRegistrar: (RegistroFase) -> Unit,
) {

    private var tipo = TipoFase.FOCO
    private var posicao = 1

    // A duração fica presa no começo da Fase: editar o Perfil não muda a Fase atual, e o Estender soma a ela.
    private var duracaoMs = config.duracaoMs(tipo)

    // O tempo decorrido vem do instante de início menos a soma das pausas, nunca de ticks.
    private var iniciadoEm: Long? = null
    private var pausadoEm: Long? = null
    private var pausadoTotalMs = 0L

    private fun andamento() = when {
        iniciadoEm == null -> Andamento.PARADO
        pausadoEm != null -> Andamento.PAUSADO
        else -> Andamento.RODANDO
    }

    fun iniciar() {
        fecharFaseVencida()
        if (andamento() == Andamento.PARADO) iniciadoEm = relogio()
    }

    fun pausar() {
        fecharFaseVencida()
        if (andamento() == Andamento.RODANDO) pausadoEm = relogio()
    }

    fun retomar() {
        fecharFaseVencida()
        val pausa = pausadoEm ?: return
        pausadoTotalMs += relogio() - pausa
        pausadoEm = null
    }

    /** Encerra a Fase atual e vai para a próxima Fase do Ciclo, parada. */
    fun pular() {
        fecharFaseVencida()
        registrarIncompleta()
        avancar()
    }

    /** O Perfil ativo foi editado. As durações novas valem a partir da próxima Fase, ou já, se a Fase ainda não começou. */
    fun atualizarConfig(nova: ConfigCiclo) {
        fecharFaseVencida()
        config = nova
        if (andamento() == Andamento.PARADO) duracaoMs = config.duracaoMs(tipo)
    }

    /** Encerra a Fase atual e começa um novo Ciclo do outro Perfil, parado no Foco 1. */
    fun trocarPerfil(nova: ConfigCiclo) {
        reiniciarCiclo() // a Fase se encerra e é registrada com o Tempo do Perfil anterior
        config = nova
        pararFase()
    }

    /** Soma os minutos de Estender do Perfil à Fase rodando ou pausada. */
    fun estender() {
        fecharFaseVencida()
        if (andamento() != Andamento.PARADO) duracaoMs += config.estenderMs
    }

    /**
     * Se a Fase chegou ao fim, passa para a próxima. Ela espera o Iniciar, a não ser que o início automático
     * do tipo dela esteja ligado: aí ela começa no instante em que a anterior terminou, e o laço segue.
     */
    private fun fecharFaseVencida() {
        while (iniciadoEm != null && decorridoMs() >= duracaoMs) {
            val inicio = iniciadoEm!!
            val fim = inicio + pausadoTotalMs + duracaoMs
            aoRegistrar(RegistroFase(tipo, inicio, fim, paraMinutos(duracaoMs), concluida = true))
            avancar()
            if (if (tipo == TipoFase.FOCO) config.autoIniciarFocos else config.autoIniciarPausas) iniciadoEm = fim
        }
    }

    /** A Fase encerrada antes do fim entra no Histórico se rodou pelo menos 1 minuto, sem contar o tempo pausado. */
    private fun registrarIncompleta() {
        val inicio = iniciadoEm ?: return
        val decorrido = decorridoMs()
        if (decorrido >= MS_POR_MIN) aoRegistrar(RegistroFase(tipo, inicio, relogio(), paraMinutos(decorrido), concluida = false))
    }

    private fun avancar() {
        when {
            tipo != TipoFase.FOCO -> {
                if (tipo == TipoFase.PAUSA_LONGA) posicao = 1 else posicao++
                tipo = TipoFase.FOCO
            }
            posicao < config.focosPorCiclo -> tipo = TipoFase.PAUSA_CURTA
            else -> tipo = TipoFase.PAUSA_LONGA
        }
        pararFase()
    }

    /** Encerra a Fase atual e volta ao Foco 1, parado. */
    fun reiniciarCiclo() {
        fecharFaseVencida()
        registrarIncompleta()
        tipo = TipoFase.FOCO
        posicao = 1
        pararFase()
    }

    private fun pararFase() {
        iniciadoEm = null
        pausadoEm = null
        pausadoTotalMs = 0
        duracaoMs = config.duracaoMs(tipo)
    }

    private fun decorridoMs(): Long {
        val inicio = iniciadoEm ?: return 0
        return (pausadoEm ?: relogio()) - inicio - pausadoTotalMs
    }

    fun estado(): EstadoCiclo {
        fecharFaseVencida()
        return EstadoCiclo(
            tipo = tipo,
            posicao = posicao,
            focosPorCiclo = config.focosPorCiclo,
            duracaoMs = duracaoMs,
            restanteMs = duracaoMs - decorridoMs(),
            andamento = andamento(),
        )
    }

    /** O estado em texto, para gravar. O relógio precisa ser o mesmo na restauração. */
    fun serializar(): String =
        jsonInstantaneo.encodeToString(Instantaneo(tipo, posicao, iniciadoEm, pausadoEm, pausadoTotalMs, duracaoMs))

    /** Volta ao estado gravado por [serializar]. As Fases que terminaram no meio tempo se fecham no próximo comando ou leitura. */
    fun restaurar(texto: String) {
        val i = runCatching { jsonInstantaneo.decodeFromString<Instantaneo>(texto) }.getOrNull() ?: return
        tipo = i.tipo
        posicao = i.posicao
        iniciadoEm = i.iniciadoEm
        pausadoEm = i.pausadoEm
        pausadoTotalMs = i.pausadoTotalMs
        duracaoMs = i.duracaoMs ?: config.duracaoMs(tipo)
        // Instantes no futuro: o aparelho reiniciou e o relógio monotônico voltou a zero. A Fase recomeça parada.
        val agora = relogio()
        if ((iniciadoEm ?: 0) > agora || (pausadoEm ?: 0) > agora) pararFase()
    }
}

/** O estado do Motor do Ciclo que é gravado a cada comando, para restaurar depois que o processo morre. */
@Serializable
private data class Instantaneo(
    val tipo: TipoFase,
    val posicao: Int,
    val iniciadoEm: Long? = null,
    val pausadoEm: Long? = null,
    val pausadoTotalMs: Long = 0,
    /** A duração presa no começo da Fase, já com o Estender. Nulo nos estados gravados antes do ticket 03. */
    val duracaoMs: Long? = null,
)

// Campos novos com valor padrão continuam lendo o que já foi gravado.
private val jsonInstantaneo = Json { ignoreUnknownKeys = true }

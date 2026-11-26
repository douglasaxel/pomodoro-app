package com.exemplo.pomodoro.motor

enum class TipoFase { FOCO, PAUSA_CURTA, PAUSA_LONGA }

enum class Andamento { PARADO, RODANDO, PAUSADO }

/** Parte do Perfil que o Motor usa: durações das Fases e N do Ciclo. */
data class ConfigCiclo(
    val focoMs: Long,
    val pausaCurtaMs: Long,
    val pausaLongaMs: Long,
    val focosPorCiclo: Int,
) {
    companion object {
        /** Perfil padrão (25/5/15, N = 4). Fixo no código até os Perfis existirem (ticket 03). */
        val PADRAO = ConfigCiclo(focoMs = 25 * 60_000L, pausaCurtaMs = 5 * 60_000L, pausaLongaMs = 15 * 60_000L, focosPorCiclo = 4)
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

/**
 * Motor do Ciclo: máquina de estados em Kotlin puro, sem Android.
 * [relogio] devolve um tempo monotônico em milissegundos.
 */
class MotorCiclo(private val config: ConfigCiclo, private val relogio: () -> Long) {

    private var tipo = TipoFase.FOCO
    private var posicao = 1

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
        avancar()
    }

    /** Se a Fase chegou ao fim, passa para a próxima, que espera o Iniciar. */
    private fun fecharFaseVencida() {
        if (decorridoMs() >= duracaoMs()) avancar()
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

    /** Volta ao Foco 1, parado. */
    fun reiniciarCiclo() {
        tipo = TipoFase.FOCO
        posicao = 1
        pararFase()
    }

    private fun pararFase() {
        iniciadoEm = null
        pausadoEm = null
        pausadoTotalMs = 0
    }

    private fun duracaoMs() = when (tipo) {
        TipoFase.FOCO -> config.focoMs
        TipoFase.PAUSA_CURTA -> config.pausaCurtaMs
        TipoFase.PAUSA_LONGA -> config.pausaLongaMs
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
            duracaoMs = duracaoMs(),
            restanteMs = duracaoMs() - decorridoMs(),
            andamento = andamento(),
        )
    }
}

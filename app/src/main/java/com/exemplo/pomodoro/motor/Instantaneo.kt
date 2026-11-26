package com.exemplo.pomodoro.motor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** O estado do Motor do Ciclo que é gravado a cada comando, para restaurar depois que o processo morre. */
@Serializable
internal data class Instantaneo(
    val tipo: TipoFase,
    val posicao: Int,
    val iniciadoEm: Long? = null,
    val pausadoEm: Long? = null,
    val pausadoTotalMs: Long = 0,
    /** A duração presa no começo da Fase, já com o Estender. Nulo nos estados gravados antes do ticket 03. */
    val duracaoMs: Long? = null,
)

// Campos novos com valor padrão continuam lendo o que já foi gravado.
internal val jsonInstantaneo = Json { ignoreUnknownKeys = true }

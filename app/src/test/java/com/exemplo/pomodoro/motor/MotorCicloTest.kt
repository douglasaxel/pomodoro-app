package com.exemplo.pomodoro.motor

import org.junit.Test
import kotlin.test.assertEquals

class MotorCicloTest {

    private val minuto = 60_000L
    private var agora = 0L
    private fun config(
        focoMin: Long = 25,
        focosPorCiclo: Int = 4,
        estenderMin: Long = 5,
        autoIniciarFocos: Boolean = false,
        autoIniciarPausas: Boolean = false,
    ) = ConfigCiclo(
        focoMs = focoMin * minuto, pausaCurtaMs = 5 * minuto, pausaLongaMs = 15 * minuto, focosPorCiclo = focosPorCiclo,
        estenderMs = estenderMin * minuto, autoIniciarFocos = autoIniciarFocos, autoIniciarPausas = autoIniciarPausas,
    )
    private fun motor(focosPorCiclo: Int = 4) = MotorCiclo(config(focosPorCiclo = focosPorCiclo), relogio = { agora })
    private fun motor(config: ConfigCiclo) = MotorCiclo(config, relogio = { agora })

    @Test
    fun `comeca parado no Foco 1 com a duracao inteira`() {
        val estado = motor().estado()

        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(4, estado.focosPorCiclo)
        assertEquals(25 * minuto, estado.restanteMs)
        assertEquals(Andamento.PARADO, estado.andamento)
    }

    @Test
    fun `depois de Iniciar o tempo restante cai com o relogio`() {
        agora = 1_000
        val motor = motor()

        motor.iniciar()
        agora += 10 * minuto

        val estado = motor.estado()
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(15 * minuto, estado.restanteMs)
    }

    @Test
    fun `o tempo pausado nao conta, em varias pausas`() {
        val motor = motor()
        motor.iniciar()
        agora += 5 * minuto

        motor.pausar()
        agora += 30 * minuto
        assertEquals(Andamento.PAUSADO, motor.estado().andamento)
        assertEquals(20 * minuto, motor.estado().restanteMs)

        motor.retomar()
        agora += 5 * minuto
        motor.pausar()
        agora += 60 * minuto
        motor.retomar()
        agora += 1 * minuto

        val estado = motor.estado()
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(14 * minuto, estado.restanteMs)
    }

    private fun MotorCiclo.fasesPulando(quantas: Int) = List(quantas) {
        val e = estado()
        pular()
        "${e.tipo} ${e.posicao}"
    }

    @Test
    fun `Pular percorre o Ciclo com N = 3 e recomeca no Foco 1`() {
        assertEquals(
            listOf(
                "FOCO 1", "PAUSA_CURTA 1", "FOCO 2", "PAUSA_CURTA 2", "FOCO 3", "PAUSA_LONGA 3",
                "FOCO 1", "PAUSA_CURTA 1",
            ),
            motor(focosPorCiclo = 3).fasesPulando(8),
        )
    }

    @Test
    fun `com N = 1 nao ha Pausa curta`() {
        assertEquals(
            listOf("FOCO 1", "PAUSA_LONGA 1", "FOCO 1", "PAUSA_LONGA 1"),
            motor(focosPorCiclo = 1).fasesPulando(4),
        )
    }

    @Test
    fun `Pular uma Fase rodando leva a proxima Fase parada, com a duracao dela`() {
        val motor = motor()
        motor.iniciar()
        agora += 3 * minuto

        motor.pular()

        val estado = motor.estado()
        assertEquals(TipoFase.PAUSA_CURTA, estado.tipo)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(5 * minuto, estado.duracaoMs)
        assertEquals(5 * minuto, estado.restanteMs)
    }

    @Test
    fun `uma Fase que chega ao fim da lugar a proxima Fase, parada`() {
        val motor = motor()
        motor.iniciar()
        agora += 10 * minuto
        motor.pausar()
        agora += 10 * minuto
        motor.retomar()

        agora += 15 * minuto - 1
        assertEquals(TipoFase.FOCO, motor.estado().tipo)
        assertEquals(1, motor.estado().restanteMs)

        agora += 2 * minuto
        val estado = motor.estado()
        assertEquals(TipoFase.PAUSA_CURTA, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(5 * minuto, estado.restanteMs)
    }

    @Test
    fun `um comando depois do fim da Fase vale para a Fase seguinte`() {
        val motor = motor()
        motor.iniciar()
        agora += 40 * minuto

        motor.iniciar()
        agora += 1 * minuto

        val estado = motor.estado()
        assertEquals(TipoFase.PAUSA_CURTA, estado.tipo)
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(4 * minuto, estado.restanteMs)
    }

    @Test
    fun `comandos fora de hora nao mudam nada`() {
        val motor = motor()
        motor.pausar()
        motor.retomar()
        assertEquals(Andamento.PARADO, motor.estado().andamento)

        motor.iniciar()
        agora += 5 * minuto
        motor.iniciar() // já rodando: não recomeça
        motor.retomar() // não está pausado
        assertEquals(20 * minuto, motor.estado().restanteMs)

        motor.pausar()
        agora += 5 * minuto
        motor.pausar() // já pausado: a pausa continua a mesma
        motor.iniciar() // pausado: não recomeça
        agora += 5 * minuto
        motor.retomar()
        assertEquals(Andamento.RODANDO, motor.estado().andamento)
        assertEquals(20 * minuto, motor.estado().restanteMs)
    }

    @Test
    fun `Reiniciar Ciclo volta ao Foco 1 parado, de qualquer ponto`() {
        val motor = motor()
        motor.fasesPulando(3) // agora no Foco 2
        motor.iniciar()
        agora += 7 * minuto
        motor.pausar()

        motor.reiniciarCiclo()

        val estado = motor.estado()
        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(25 * minuto, estado.restanteMs)
        assertEquals(listOf("FOCO 1", "PAUSA_CURTA 1", "FOCO 2"), motor.fasesPulando(3))
    }

    @Test
    fun `Estender soma os minutos do Perfil a Fase atual, sem recomeca-la`() {
        val motor = motor(config(estenderMin = 7))
        motor.estender() // parado: não há Fase para Estender
        assertEquals(25 * minuto, motor.estado().restanteMs)

        motor.iniciar()
        agora += 20 * minuto
        motor.estender()

        val estado = motor.estado()
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(32 * minuto, estado.duracaoMs)
        assertEquals(12 * minuto, estado.restanteMs)
    }

    @Test
    fun `uma duracao editada durante uma Fase vale so na proxima Fase`() {
        val motor = motor(config(focoMin = 25))
        motor.iniciar()
        agora += 10 * minuto

        motor.atualizarConfig(config(focoMin = 50))

        assertEquals(25 * minuto, motor.estado().duracaoMs)
        assertEquals(15 * minuto, motor.estado().restanteMs)
        motor.fasesPulando(2) // Pausa curta 1, e chega ao Foco 2
        assertEquals(TipoFase.FOCO, motor.estado().tipo)
        assertEquals(50 * minuto, motor.estado().duracaoMs)
    }

    @Test
    fun `uma duracao editada com a Fase ainda parada vale na hora`() {
        val motor = motor(config(focoMin = 25))

        motor.atualizarConfig(config(focoMin = 50))

        assertEquals(50 * minuto, motor.estado().duracaoMs)
        assertEquals(50 * minuto, motor.estado().restanteMs)
    }

    @Test
    fun `Trocar Perfil encerra a Fase atual e comeca um novo Ciclo, parado no Foco 1`() {
        val motor = motor(config(focoMin = 25, focosPorCiclo = 4))
        motor.fasesPulando(2) // agora no Foco 2
        motor.iniciar()
        agora += 10 * minuto

        motor.trocarPerfil(config(focoMin = 50, focosPorCiclo = 2))

        val estado = motor.estado()
        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(2, estado.focosPorCiclo)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(50 * minuto, estado.restanteMs)
    }

    @Test
    fun `com so o inicio automatico das Pausas, a Pausa comeca sozinha e o Foco espera`() {
        val motor = motor(config(autoIniciarPausas = true))
        motor.iniciar()

        agora += 25 * minuto + 1 * minuto // a Pausa curta começou sozinha no fim do Foco
        val pausa = motor.estado()
        assertEquals(TipoFase.PAUSA_CURTA, pausa.tipo)
        assertEquals(Andamento.RODANDO, pausa.andamento)
        assertEquals(4 * minuto, pausa.restanteMs)

        agora += 4 * minuto + 10 * minuto
        val foco = motor.estado()
        assertEquals(TipoFase.FOCO, foco.tipo)
        assertEquals(2, foco.posicao)
        assertEquals(Andamento.PARADO, foco.andamento)
        assertEquals(25 * minuto, foco.restanteMs)
    }

    @Test
    fun `com so o inicio automatico dos Focos, o Foco comeca sozinho e a Pausa espera`() {
        val motor = motor(config(autoIniciarFocos = true))
        motor.iniciar()
        agora += 30 * minuto
        assertEquals(TipoFase.PAUSA_CURTA, motor.estado().tipo)
        assertEquals(Andamento.PARADO, motor.estado().andamento)

        motor.iniciar()
        agora += 5 * minuto + 2 * minuto

        val foco = motor.estado()
        assertEquals(TipoFase.FOCO, foco.tipo)
        assertEquals(2, foco.posicao)
        assertEquals(Andamento.RODANDO, foco.andamento)
        assertEquals(23 * minuto, foco.restanteMs)
    }

    @Test
    fun `com as duas chaves ligadas, o Ciclo segue sozinho e recomeca no Foco 1`() {
        val motor = motor(config(focosPorCiclo = 2, autoIniciarFocos = true, autoIniciarPausas = true))
        motor.iniciar()

        agora += (25 + 5 + 25 + 15 + 3) * minuto

        val estado = motor.estado()
        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(22 * minuto, estado.restanteMs)
    }
}

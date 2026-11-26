package com.exemplo.pomodoro.motor

import org.junit.Test
import kotlin.test.assertEquals

class MotorCicloTest {

    private val minuto = 60_000L
    private var agora = 0L
    private fun motor(focosPorCiclo: Int = 4) = MotorCiclo(
        ConfigCiclo(focoMs = 25 * minuto, pausaCurtaMs = 5 * minuto, pausaLongaMs = 15 * minuto, focosPorCiclo = focosPorCiclo),
        relogio = { agora },
    )

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

    /** Simula a morte do processo: o estado gravado volta num Motor novo, com o mesmo relógio. */
    private fun MotorCiclo.renascer(focosPorCiclo: Int = 4) = motor(focosPorCiclo).also { it.restaurar(serializar()) }

    @Test
    fun `restaurado depois da morte do processo, a Fase segue rodando no ponto certo`() {
        val motor = motor()
        motor.pular() // Pausa curta 1
        motor.pular() // Foco 2
        motor.iniciar()
        agora += 10 * minuto

        val restaurado = motor.renascer()
        agora += 5 * minuto

        val estado = restaurado.estado()
        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(2, estado.posicao)
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(10 * minuto, estado.restanteMs)
    }

    @Test
    fun `restaurado depois de horas pausado, o tempo pausado continua sem contar`() {
        val motor = motor()
        motor.iniciar()
        agora += 5 * minuto
        motor.pausar()

        val restaurado = motor.renascer()
        agora += 3 * 60 * minuto
        assertEquals(Andamento.PAUSADO, restaurado.estado().andamento)
        assertEquals(20 * minuto, restaurado.estado().restanteMs)

        restaurado.retomar()
        agora += 1 * minuto
        assertEquals(19 * minuto, restaurado.estado().restanteMs)
    }

    @Test
    fun `sem inicio automatico, a Fase que terminou com o processo morto da lugar a proxima, parada`() {
        val motor = motor(focosPorCiclo = 2)
        motor.fasesPulando(2) // Foco 2, o último do Ciclo
        motor.iniciar()
        agora += 1 * minuto

        val restaurado = motor.renascer(focosPorCiclo = 2)
        agora += 8 * 60 * minuto

        val estado = restaurado.estado()
        assertEquals(TipoFase.PAUSA_LONGA, estado.tipo)
        assertEquals(2, estado.posicao)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(15 * minuto, estado.restanteMs)
        assertEquals(listOf("PAUSA_LONGA 2", "FOCO 1"), restaurado.fasesPulando(2))
    }

    @Test
    fun `depois de um reinicio do aparelho o relogio volta a zero, e a Fase restaurada fica parada do inicio`() {
        agora = 10 * 60 * minuto
        val motor = motor()
        motor.iniciar()
        agora += 10 * minuto

        agora = 2 * minuto // o relógio monotônico recomeça no boot
        val estado = motor.renascer().estado()

        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(Andamento.PARADO, estado.andamento)
        assertEquals(25 * minuto, estado.restanteMs)
    }

    @Test
    fun `um texto gravado ilegivel deixa o Motor como esta`() {
        val motor = motor()
        motor.restaurar("{quebrado")
        assertEquals(TipoFase.FOCO, motor.estado().tipo)
        assertEquals(Andamento.PARADO, motor.estado().andamento)
    }
}

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
    private val registros = mutableListOf<RegistroFase>()
    /** O boot do aparelho e a diferença do relógio de parede para o monotônico. Mudam juntos num reinício. */
    private var boot = 1
    private var paraParede = 1_000_000L
    private fun motor(focosPorCiclo: Int = 4) = motor(config(focosPorCiclo = focosPorCiclo))
    private fun motor(config: ConfigCiclo) =
        MotorCiclo(config, relogio = { agora }, aoRegistrar = { registros += it }, boot = boot, paraParede = { paraParede })

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

    /** Desliga o aparelho por [desligadoMs] e liga de novo: o relógio monotônico recomeça e já está em [ligadoMs]. */
    private fun reiniciarAparelho(desligadoMs: Long, ligadoMs: Long) {
        val parede = agora + paraParede + desligadoMs + ligadoMs
        boot++
        agora = ligadoMs
        paraParede = parede - agora
    }

    @Test
    fun `depois de um reinicio do aparelho, a Fase segue no tempo de parede, mesmo com o aparelho ligado ha mais tempo que o inicio gravado`() {
        agora = 1 * minuto
        val motor = motor()
        motor.iniciar()
        agora += 5 * minuto
        val gravado = motor.serializar()

        reiniciarAparelho(desligadoMs = 3 * minuto, ligadoMs = 2 * minuto)
        val estado = motor(config()).also { it.restaurar(gravado) }.estado()

        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(15 * minuto, estado.restanteMs)
    }

    @Test
    fun `a Fase que terminou com o aparelho desligado entra no Historico no horario certo e a proxima espera`() {
        agora = 1 * minuto
        val inicioParede = agora + paraParede
        val motor = motor()
        motor.iniciar()
        agora += 5 * minuto
        val gravado = motor.serializar()

        reiniciarAparelho(desligadoMs = 60 * minuto, ligadoMs = 1 * minuto)
        val estado = motor(config()).also { it.restaurar(gravado) }.estado()

        assertEquals(TipoFase.PAUSA_CURTA, estado.tipo)
        assertEquals(Andamento.PARADO, estado.andamento)
        val r = registros.single()
        assertEquals(inicioParede, r.inicioMs + paraParede)
        assertEquals(inicioParede + 25 * minuto, r.fimMs + paraParede)
    }

    @Test
    fun `pausada antes do reinicio, a Fase continua pausada com o mesmo restante`() {
        val motor = motor()
        motor.iniciar()
        agora += 5 * minuto
        motor.pausar()
        val gravado = motor.serializar()

        reiniciarAparelho(desligadoMs = 10 * minuto, ligadoMs = 90 * minuto)
        val estado = motor(config()).also { it.restaurar(gravado) }.estado()

        assertEquals(Andamento.PAUSADO, estado.andamento)
        assertEquals(20 * minuto, estado.restanteMs)
    }

    @Test
    fun `o estado diz se a Fase anterior acabou sozinha, para o Estilo visual florescer, ou se foi pulada`() {
        val motor = motor()
        assertEquals(false, motor.estado().anteriorAcabou)
        motor.iniciar()
        agora += 25 * minuto
        assertEquals(true, motor.estado().anteriorAcabou)
        motor.pular()
        assertEquals(false, motor.estado().anteriorAcabou)
    }

    @Test
    fun `restaurada, a Fase mantem a duracao vigente, com o Estender`() {
        val motor = motor()
        motor.pular() // Pausa curta 1
        motor.iniciar()
        agora += 2 * minuto
        motor.estender()

        val estado = motor.renascer().estado()

        assertEquals(TipoFase.PAUSA_CURTA, estado.tipo)
        assertEquals(10 * minuto, estado.duracaoMs)
        assertEquals(8 * minuto, estado.restanteMs)
    }

    @Test
    fun `com inicio automatico, a restauracao encadeia as Fases que terminaram com o processo morto`() {
        val config = config(focosPorCiclo = 2, autoIniciarFocos = true, autoIniciarPausas = true)
        val motor = motor(config)
        motor.iniciar()
        agora += 10 * minuto
        motor.estender() // o Foco 1 passa a terminar aos 30 min

        val restaurado = motor(config).also { it.restaurar(motor.serializar()) }
        // Cada Fase começa no fim da anterior: Foco 1 até 30, Pausa curta até 35, Foco 2 até 60,
        // Pausa longa até 75 e o Foco 1 do novo Ciclo até 100.
        agora = 98 * minuto

        val estado = restaurado.estado()
        assertEquals(TipoFase.FOCO, estado.tipo)
        assertEquals(1, estado.posicao)
        assertEquals(Andamento.RODANDO, estado.andamento)
        assertEquals(25 * minuto, estado.duracaoMs)
        assertEquals(2 * minuto, estado.restanteMs)
    }

    @Test
    fun `um texto gravado ilegivel deixa o Motor como esta`() {
        val motor = motor()
        motor.restaurar("{quebrado")
        assertEquals(TipoFase.FOCO, motor.estado().tipo)
        assertEquals(Andamento.PARADO, motor.estado().andamento)
    }

    // Registros de Fase para o Histórico

    @Test
    fun `um Foco que chega ao fim gera um registro concluido com os minutos da Fase`() {
        agora = 1_000
        val motor = motor()
        motor.iniciar()
        agora += 25 * minuto

        motor.estado()

        assertEquals(listOf(RegistroFase(TipoFase.FOCO, 1_000, 1_000 + 25 * minuto, 25, concluida = true)), registros)
    }

    @Test
    fun `Pular depois de 1 minuto gera um registro incompleto com os minutos que rodaram`() {
        val motor = motor()
        motor.iniciar()
        agora += 12 * minuto + 30_000

        motor.pular()

        assertEquals(listOf(RegistroFase(TipoFase.FOCO, 0, 12 * minuto + 30_000, 12, concluida = false)), registros)
    }

    @Test
    fun `uma Fase incompleta com menos de 1 minuto nao gera registro`() {
        val motor = motor()
        motor.pular() // nem começou
        motor.iniciar()
        agora += 59_999
        motor.pular()

        assertEquals(emptyList(), registros)
    }

    @Test
    fun `Trocar Perfil no meio de uma Fase gera um registro incompleto`() {
        val motor = motor()
        motor.iniciar()
        agora += 7 * minuto

        motor.trocarPerfil(config(focoMin = 50))

        assertEquals(listOf(RegistroFase(TipoFase.FOCO, 0, 7 * minuto, 7, concluida = false)), registros)
    }

    @Test
    fun `o tempo pausado nao conta como minutos do registro`() {
        val motor = motor()
        motor.iniciar()
        agora += 4 * minuto
        motor.pausar()
        agora += 40 * minuto
        motor.retomar()
        agora += 2 * minuto

        motor.pular()

        assertEquals(listOf(RegistroFase(TipoFase.FOCO, 0, 46 * minuto, 6, concluida = false)), registros)
    }

    @Test
    fun `uma Fase Estendida que chega ao fim conta como concluida, com os minutos reais`() {
        val motor = motor()
        motor.iniciar()
        agora += 20 * minuto
        motor.estender()
        motor.estender()
        agora += 15 * minuto

        motor.estado()

        assertEquals(listOf(RegistroFase(TipoFase.FOCO, 0, 35 * minuto, 35, concluida = true)), registros)
    }

    @Test
    fun `as Pausas tambem geram registro`() {
        val motor = motor(focosPorCiclo = 1)
        motor.pular() // Pausa longa
        motor.iniciar()
        agora += 15 * minuto
        motor.estado() // a Pausa longa termina; Foco 1, parado
        motor.pular() // Pausa longa de novo
        motor.iniciar()
        agora += 3 * minuto
        motor.pular()

        assertEquals(
            listOf(
                RegistroFase(TipoFase.PAUSA_LONGA, 0, 15 * minuto, 15, concluida = true),
                RegistroFase(TipoFase.PAUSA_LONGA, 15 * minuto, 18 * minuto, 3, concluida = false),
            ),
            registros,
        )
    }

    @Test
    fun `a restauracao registra cada Fase que terminou com o processo morto`() {
        val config = config(focosPorCiclo = 2, autoIniciarPausas = true)
        val motor = motor(config)
        motor.iniciar()
        agora += 10 * minuto

        val restaurado = motor(config).also { it.restaurar(motor.serializar()) }
        agora = 3 * 60 * minuto
        restaurado.estado()

        assertEquals(
            listOf(
                RegistroFase(TipoFase.FOCO, 0, 25 * minuto, 25, concluida = true),
                RegistroFase(TipoFase.PAUSA_CURTA, 25 * minuto, 30 * minuto, 5, concluida = true),
            ),
            registros,
        )
    }
}

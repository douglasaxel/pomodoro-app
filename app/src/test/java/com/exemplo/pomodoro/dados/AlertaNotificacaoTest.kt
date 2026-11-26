package com.exemplo.pomodoro.dados

import com.exemplo.pomodoro.dados.AcaoNotificacao.ESTENDER
import com.exemplo.pomodoro.dados.AcaoNotificacao.PAUSAR
import com.exemplo.pomodoro.dados.AcaoNotificacao.PULAR
import com.exemplo.pomodoro.dados.AcaoNotificacao.RETOMAR
import org.junit.Test
import kotlin.test.assertEquals

class AlertaNotificacaoTest {

    @Test
    fun `por padrao a Notificacao de contagem mostra Pausar, Pular e Estender`() {
        assertEquals(listOf(PAUSAR, PULAR, ESTENDER), AlertaNotificacao().acoes(rodando = true))
    }

    @Test
    fun `com a Fase pausada o primeiro botao vira Retomar`() {
        assertEquals(listOf(RETOMAR, PULAR, ESTENDER), AlertaNotificacao().acoes(rodando = false))
    }

    @Test
    fun `so aparecem os botoes que o Perfil escolhe`() {
        val soEstender = AlertaNotificacao(botaoPausarRetomar = false, botaoPular = false)
        assertEquals(listOf(ESTENDER), soEstender.acoes(rodando = true))

        val semEstender = AlertaNotificacao(botaoEstender = false)
        assertEquals(listOf(RETOMAR, PULAR), semEstender.acoes(rodando = false))

        val nenhum = AlertaNotificacao(botaoPausarRetomar = false, botaoPular = false, botaoEstender = false)
        assertEquals(emptyList(), nenhum.acoes(rodando = true))
    }
}

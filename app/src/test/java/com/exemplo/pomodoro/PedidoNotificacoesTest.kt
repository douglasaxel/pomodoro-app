package com.exemplo.pomodoro

import org.junit.Test
import kotlin.test.assertEquals

/** POST_NOTIFICATIONS é pedida só no primeiro Iniciar (história 24). */
class PedidoNotificacoesTest {

    @Test
    fun `pede no primeiro Iniciar, se a permissao falta`() {
        assertEquals(true, deveSolicitarNotificacoes(concedida = false, jaPedida = false))
    }

    @Test
    fun `nao pede de novo nos Iniciar seguintes, mesmo com a permissao faltando`() {
        assertEquals(false, deveSolicitarNotificacoes(concedida = false, jaPedida = true))
    }

    @Test
    fun `nao pede se a permissao ja existe`() {
        assertEquals(false, deveSolicitarNotificacoes(concedida = true, jaPedida = false))
    }
}

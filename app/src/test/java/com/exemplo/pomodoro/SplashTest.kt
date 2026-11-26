package com.exemplo.pomodoro

import android.content.Intent
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Quando a splash animada aparece (ticket 13): só na abertura a frio pelo launcher. */
class SplashTest {

    private val launcher = setOf(Intent.CATEGORY_LAUNCHER)

    @Test
    fun `abertura a frio pelo launcher mostra a splash`() {
        assertTrue(mostraSplash(Intent.ACTION_MAIN, launcher, flags = 0, pelaNotificacao = false, restaurada = false, primeiraDoProcesso = true))
    }

    @Test
    fun `a Notificacao de contagem e o Alerta vao direto ao Timer`() {
        // Os dois abrem com a mesma intent do launcher (MAIN + LAUNCHER): só o extra os distingue.
        assertFalse(mostraSplash(Intent.ACTION_MAIN, launcher, flags = 0, pelaNotificacao = true, restaurada = false, primeiraDoProcesso = true))
    }

    @Test
    fun `voltar de segundo plano vai direto ao Timer`() {
        // O processo morreu e a Activity volta com o estado salvo.
        assertFalse(mostraSplash(Intent.ACTION_MAIN, launcher, flags = 0, pelaNotificacao = false, restaurada = true, primeiraDoProcesso = true))
        // O processo seguiu vivo (o Voltar fechou só a Activity).
        assertFalse(mostraSplash(Intent.ACTION_MAIN, launcher, flags = 0, pelaNotificacao = false, restaurada = false, primeiraDoProcesso = false))
        // Pelos recentes, depois que o processo morreu.
        assertFalse(
            mostraSplash(
                Intent.ACTION_MAIN, launcher, flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
                pelaNotificacao = false, restaurada = false, primeiraDoProcesso = true,
            ),
        )
    }
}

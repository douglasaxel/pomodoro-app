package com.exemplo.pomodoro.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** O tamanho do blob (DESIGN.md §7): encolhe com o tempo, mas o miolo escuro sempre cobre os números (§3). */
class RaioBlobTest {

    private val rMax = 100f

    @Test
    fun `sem numeros (previa), o blob encolhe de cheio ate 35 por cento`() {
        assertEquals(100f, raioDoBlob(rMax, cheio = 1f, respira = 0f, nucleo = 0f, ruido = 0.04f), 0.01f)
        assertEquals(35f, raioDoBlob(rMax, cheio = 0f, respira = 0f, nucleo = 0f, ruido = 0.04f), 0.01f)
    }

    @Test
    fun `com numeros, a borda mais funda fica sempre alem dos numeros, com folga para o aro`() {
        val nucleo = 55f
        for (ruido in listOf(0.04f, 0.06f, 0.11f)) for (i in 0..20) for (respira in listOf(-0.04f, 0f, 0.04f)) {
            val r = raioDoBlob(rMax, cheio = i / 20f, respira = respira, nucleo = nucleo, ruido = ruido)
            // O aro claro só começa depois de 84% do raio mais fundo do ruído.
            assertTrue(nucleo <= 0.84f * r * (1f - ruido) + 0.001f, "ruído $ruido, cheio ${i / 20f}: raio $r")
        }
    }

    @Test
    fun `com numeros, o blob ainda encolhe com o tempo`() {
        val cheio = raioDoBlob(rMax, cheio = 1f, respira = 0f, nucleo = 55f, ruido = 0.04f)
        val fim = raioDoBlob(rMax, cheio = 0f, respira = 0f, nucleo = 55f, ruido = 0.04f)
        assertEquals(100f, cheio, 0.01f)
        assertTrue(fim < 0.8f * cheio, "fim $fim")
    }
}

package com.exemplo.pomodoro

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.ConfigCiclo
import com.exemplo.pomodoro.motor.MotorCiclo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ponytail: o timer só vive com o processo aberto. O segundo plano e a restauração chegam no ticket 02.
class TimerViewModel : ViewModel() {

    private val motor = MotorCiclo(ConfigCiclo.PADRAO, SystemClock::elapsedRealtime)

    var estado by mutableStateOf(motor.estado())
        private set

    private var relogioDaTela: Job? = null

    fun iniciar() = comando { motor.iniciar() }
    fun pausar() = comando { motor.pausar() }
    fun retomar() = comando { motor.retomar() }
    fun pular() = comando { motor.pular() }
    fun reiniciarCiclo() = comando { motor.reiniciarCiclo() }

    private fun comando(acao: () -> Unit) {
        acao()
        estado = motor.estado()
        relogioDaTela?.cancel()
        // Enquanto a Fase roda, relê o Motor na virada de cada segundo. O tempo vem do Motor, não destes ticks.
        relogioDaTela = viewModelScope.launch {
            while (estado.andamento == Andamento.RODANDO) {
                delay((estado.restanteMs - 1) % 1000 + 1)
                estado = motor.estado()
            }
        }
    }
}

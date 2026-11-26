package com.exemplo.pomodoro

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.pomodoro.motor.Andamento
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** A tela lê o mesmo Motor que a Notificação de contagem e o fim da Fase, pelo [AppPomodoro.controle]. */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val controle = (app as AppPomodoro).controle

    var estado by mutableStateOf(controle.estadoAgora())
        private set

    init {
        // Cada comando (da tela ou da notificação) e cada fim de Fase chega por aqui.
        viewModelScope.launch {
            controle.estado.collectLatest {
                estado = controle.estadoAgora() // o valor do fluxo é de quando foi emitido; o tempo é o de agora
                // Enquanto a Fase roda, relê o Motor na virada de cada segundo. O tempo vem do Motor, não destes ticks.
                while (estado.andamento == Andamento.RODANDO) {
                    delay((estado.restanteMs - 1) % 1000 + 1)
                    estado = controle.estadoAgora()
                }
            }
        }
    }

    fun iniciar() = controle.iniciar()
    fun pausar() = controle.pausar()
    fun retomar() = controle.retomar()
    fun pular() = controle.pular()
    fun reiniciarCiclo() = controle.reiniciarCiclo()

    /** Depois que POST_NOTIFICATIONS é concedida, mostra de novo a Notificação de contagem. */
    fun notificacoesPermitidas() = controle.sincronizar()
}

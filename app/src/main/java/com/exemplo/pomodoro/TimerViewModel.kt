package com.exemplo.pomodoro

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.ConfigCiclo
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.segundoplano.ControleCiclo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.prefsApp by preferencesDataStore(name = "app")
private val NOTIFICACOES_PEDIDAS = booleanPreferencesKey("notificacoes_pedidas")

/** POST_NOTIFICATIONS é pedida uma vez só, no primeiro Iniciar sem a permissão (história 24). */
internal fun deveSolicitarNotificacoes(concedida: Boolean, jaPedida: Boolean) = !concedida && !jaPedida

/** A tela lê o mesmo Motor que a Notificação de contagem e o fim da Fase, pelo [AppPomodoro.controle]. */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val app = app as AppPomodoro
    private var controle: ControleCiclo? = null

    private val perfis = this.app.perfis

    /** Nulo só até o banco responder na abertura. */
    val perfilAtivo = perfis.ativo.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val todosPerfis = perfis.todos

    /** O Motor e o Perfil ativo chegaram. Até lá, a splash do sistema fica na tela e o Timer não é composto. */
    var pronto by mutableStateOf(false)
        private set

    // Só vale depois do [pronto]: até lá, ninguém lê.
    var estado by mutableStateOf(EstadoCiclo(TipoFase.FOCO, 1, 4, ConfigCiclo.PADRAO.focoMs, ConfigCiclo.PADRAO.focoMs, Andamento.PARADO))
        private set

    init {
        viewModelScope.launch {
            val c = this@TimerViewModel.app.controle.await()
            perfilAtivo.filterNotNull().first()
            controle = c
            estado = c.estadoAgora()
            pronto = true
            // Cada comando (da tela ou da notificação) e cada fim de Fase chega por aqui.
            c.estado.collectLatest {
                estado = c.estadoAgora() // o valor do fluxo é de quando foi emitido; o tempo é o de agora
                // Enquanto a Fase roda, relê o Motor na virada de cada segundo. O tempo vem do Motor, não destes ticks.
                while (estado.andamento == Andamento.RODANDO) {
                    delay((estado.restanteMs - 1) % 1000 + 1)
                    estado = c.estadoAgora()
                }
            }
        }
    }

    fun iniciar() { controle?.iniciar() }
    fun pausar() { controle?.pausar() }
    fun retomar() { controle?.retomar() }
    fun pular() { controle?.pular() }
    fun estender() { controle?.estender() }
    fun reiniciarCiclo() { controle?.reiniciarCiclo() }

    /** O [controle] troca o Motor quando o novo Perfil ativo chega do DataStore. */
    fun trocarPerfil(id: Long) = perfis.ativar(id)

    /** Diz se é a hora de pedir POST_NOTIFICATIONS e já marca o pedido como feito. */
    suspend fun pedirNotificacoesAgora(concedida: Boolean): Boolean {
        var pedir = false
        app.prefsApp.edit {
            pedir = deveSolicitarNotificacoes(concedida, it[NOTIFICACOES_PEDIDAS] == true)
            if (pedir) it[NOTIFICACOES_PEDIDAS] = true
        }
        return pedir
    }

    /** Depois que POST_NOTIFICATIONS é concedida, mostra de novo a Notificação de contagem. */
    fun notificacoesPermitidas() { controle?.sincronizar() }
}

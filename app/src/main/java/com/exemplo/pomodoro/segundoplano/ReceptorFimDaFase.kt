package com.exemplo.pomodoro.segundoplano

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Recebe o alarme exato do fim da Fase. Funciona mesmo se o processo ou o serviço tiverem morrido. */
class ReceptorFimDaFase : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val terminou = TipoFase.valueOf(intent.getStringExtra(EXTRA_TIPO_QUE_TERMINA) ?: return)
        comControle(ctx) { controle ->
            val a = controle.perfilAtivo.alerta
            // O player lê o arquivo do som: prepara fora da thread principal, junto com o resto.
            val som = async(Dispatchers.IO) { prepararSom(ctx, a) }
            controle.sincronizar() // fecha a Fase vencida, grava e desliga o serviço se a próxima Fase espera o Iniciar
            alertar(ctx, terminou, controle.estado.value.tipo, a, som.await())
            // Segura o processo enquanto o som toca, com o app fechado.
            delay(DURACAO_MAX_SOM_MS)
        }
    }
}

/**
 * Roda [bloco] na thread principal com o [ControleCiclo] e segura o processo até ele acabar (goAsync).
 * Com o processo morto, o controle nasce aqui e lê o Room e o DataStore fora da thread principal.
 */
fun BroadcastReceiver.comControle(ctx: Context, bloco: suspend CoroutineScope.(ControleCiclo) -> Unit) {
    val app = ctx.applicationContext as AppPomodoro
    val pendente = goAsync()
    app.escopo.launch {
        try {
            bloco(withContext(Dispatchers.IO) { app.controle })
        } finally {
            pendente.finish()
        }
    }
}

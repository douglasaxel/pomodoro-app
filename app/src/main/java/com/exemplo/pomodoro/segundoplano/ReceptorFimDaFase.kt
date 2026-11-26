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
 * Com o processo morto, espera o controle nascer: ele lê o Room e o DataStore fora da thread principal.
 */
fun BroadcastReceiver.comControle(ctx: Context, bloco: suspend CoroutineScope.(ControleCiclo) -> Unit) {
    val app = ctx.applicationContext as AppPomodoro
    val pendente = goAsync()
    app.escopo.launch {
        try {
            bloco(app.controle.await())
        } finally {
            pendente.finish()
        }
    }
}

/**
 * Depois de um reinício do aparelho ou de uma atualização do app, que cancelam o alarme: o Motor volta do DataStore
 * no tempo de parede, e o controle, ao nascer, devolve o alarme, o serviço e o player.
 */
class ReceptorBoot : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            comControle(ctx) { it.sincronizar() }
        }
    }
}

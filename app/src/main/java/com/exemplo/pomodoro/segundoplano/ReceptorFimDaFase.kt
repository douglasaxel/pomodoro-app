package com.exemplo.pomodoro.segundoplano

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.motor.TipoFase

/** Recebe o alarme exato do fim da Fase. Funciona mesmo se o processo ou o serviço tiverem morrido. */
class ReceptorFimDaFase : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val controle = (ctx.applicationContext as AppPomodoro).controle
        controle.sincronizar() // fecha a Fase vencida, grava e desliga o serviço se a próxima Fase espera o Iniciar
        val terminou = TipoFase.valueOf(intent.getStringExtra(EXTRA_TIPO_QUE_TERMINA) ?: return)
        alertar(ctx, terminou, controle.estado.value.tipo, controle.perfilAtivo.alerta)
        // Segura o processo alguns segundos, para o som tocar até o fim com o app fechado.
        val pendente = goAsync()
        Handler(Looper.getMainLooper()).postDelayed(pendente::finish, DURACAO_MAX_SOM_MS)
    }
}

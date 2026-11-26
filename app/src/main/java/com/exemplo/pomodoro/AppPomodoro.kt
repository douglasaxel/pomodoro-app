package com.exemplo.pomodoro

import android.app.Application
import com.exemplo.pomodoro.segundoplano.ControleCiclo
import com.exemplo.pomodoro.segundoplano.criarCanais

class AppPomodoro : Application() {

    /** Único dono do Motor do Ciclo no processo. Injeção feita à mão. */
    lateinit var controle: ControleCiclo
        private set

    override fun onCreate() {
        super.onCreate()
        criarCanais(this)
        controle = ControleCiclo(this)
    }
}

package com.exemplo.pomodoro

import android.app.Application
import com.exemplo.pomodoro.dados.Perfis
import com.exemplo.pomodoro.segundoplano.ControleCiclo
import com.exemplo.pomodoro.segundoplano.criarCanais
import kotlinx.coroutines.MainScope

class AppPomodoro : Application() {

    /** Único dono do Motor do Ciclo no processo. Injeção feita à mão. */
    lateinit var controle: ControleCiclo
        private set

    lateinit var perfis: Perfis
        private set

    override fun onCreate() {
        super.onCreate()
        criarCanais(this)
        val escopo = MainScope()
        perfis = Perfis(this, escopo)
        controle = ControleCiclo(this, perfis, escopo)
    }
}

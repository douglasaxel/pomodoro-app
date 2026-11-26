package com.exemplo.pomodoro

import android.app.Application
import androidx.room.Room
import com.exemplo.pomodoro.dados.Banco
import com.exemplo.pomodoro.dados.Historico
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

    lateinit var historico: Historico
        private set

    override fun onCreate() {
        super.onCreate()
        criarCanais(this)
        val escopo = MainScope()
        val banco = Room.databaseBuilder(this, Banco::class.java, "pomodoro.db").build()
        perfis = Perfis(this, banco.perfis(), escopo)
        historico = Historico(banco.historico(), escopo)
        controle = ControleCiclo(this, perfis, historico, escopo)
    }
}

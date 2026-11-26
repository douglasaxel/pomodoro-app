package com.exemplo.pomodoro

import android.app.Application
import androidx.room.Room
import com.exemplo.pomodoro.dados.Banco
import com.exemplo.pomodoro.dados.Historico
import com.exemplo.pomodoro.dados.Perfis
import com.exemplo.pomodoro.segundoplano.ControleCiclo
import com.exemplo.pomodoro.segundoplano.criarCanais
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async

class AppPomodoro : Application() {

    /** O escopo do processo, na thread principal. */
    val escopo: CoroutineScope = MainScope()

    /**
     * Único dono do Motor do Ciclo no processo. Injeção feita à mão.
     * A criação lê o Room e o DataStore fora da thread principal: a tela, o serviço e os receptores esperam por ele.
     */
    lateinit var controle: Deferred<ControleCiclo>
        private set

    lateinit var perfis: Perfis
        private set

    lateinit var historico: Historico
        private set

    override fun onCreate() {
        super.onCreate()
        criarCanais(this)
        val banco = Room.databaseBuilder(this, Banco::class.java, "pomodoro.db").build()
        perfis = Perfis(this, banco.perfis(), escopo)
        historico = Historico(banco.historico(), escopo)
        controle = escopo.async { ControleCiclo.criar(this@AppPomodoro, perfis, historico, escopo) }
    }
}

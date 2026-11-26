package com.exemplo.pomodoro

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.ui.RaizApp
import com.exemplo.pomodoro.ui.SplashAnimada

class MainActivity : ComponentActivity() {

    private val timer: TimerViewModel by viewModels() // o mesmo da RaizApp: o dono é a Activity

    /** Cada toque na Notificação de contagem ou no botão do Alerta, com o app já aberto, leva à aba Timer. */
    private var pedidosDoTimer by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // A splash do sistema pinta só a baseEscura, a mesma cor em que a animada começa: sem salto (ticket 13).
        // Ela fica até o Motor e o Perfil ativo chegarem: o Timer nunca aparece sem o Fundo e a Cor de destaque.
        installSplashScreen().setKeepOnScreenCondition { !timer.pronto }
        // Edge-to-edge com ícones claros nas barras do sistema (DESIGN.md §8).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val splash = mostraSplash(
            intent.action,
            intent.categories,
            intent.flags,
            pelaNotificacao = intent.getBooleanExtra(EXTRA_PELA_NOTIFICACAO, false),
            restaurada = savedInstanceState != null,
            primeiraDoProcesso = primeiraDoProcesso,
        )
        primeiraDoProcesso = false
        setContent {
            if (!timer.pronto) return@setContent
            var naSplash by remember { mutableStateOf(splash) }
            // O Timer fica composto embaixo desde o começo: o fade da splash termina nele, sem tela vazia.
            // Enquanto a splash está na tela, o leitor de tela não alcança o Timer: ele não pode ser tocado nem lido.
            Box {
                Box(if (naSplash) Modifier.clearAndSetSemantics {} else Modifier) { RaizApp(pedidosDoTimer) }
                if (naSplash) {
                    val destaque = timer.perfilAtivo.value?.aparencia?.destaque(TipoFase.FOCO)
                    SplashAnimada(destaque, aoTerminar = { naSplash = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PELA_NOTIFICACAO, false)) pedidosDoTimer++
    }

    companion object {
        /** Posto pela Notificação de contagem e pelo Alerta, que abrem com a mesma intent do launcher. */
        const val EXTRA_PELA_NOTIFICACAO = "pela_notificacao"

        /** Some com o processo. Falso depois da primeira Activity: a volta com o processo vivo não repete a splash. */
        private var primeiraDoProcesso = true
    }
}

/**
 * A splash animada só aparece na abertura a frio pelo launcher (ticket 13).
 * Vai direto ao Timer quem abre pela Notificação de contagem ou pelo Alerta, pelos recentes,
 * ou volta de segundo plano (estado restaurado, ou o processo ainda vivo).
 */
internal fun mostraSplash(
    acao: String?,
    categorias: Set<String>?,
    flags: Int,
    pelaNotificacao: Boolean,
    restaurada: Boolean,
    primeiraDoProcesso: Boolean,
): Boolean = acao == Intent.ACTION_MAIN && categorias.orEmpty().contains(Intent.CATEGORY_LAUNCHER) &&
    !pelaNotificacao && !restaurada && primeiraDoProcesso && (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0

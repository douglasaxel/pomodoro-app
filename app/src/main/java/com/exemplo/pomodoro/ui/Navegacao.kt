package com.exemplo.pomodoro.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.TimerViewModel
import com.exemplo.pomodoro.motor.TipoFase

private const val ROTA_TIMER = "timer"
private const val ROTA_PERFIS = "perfis"
private const val ROTA_PERFIL = "perfil/{id}?novo={novo}"
private fun rotaPerfil(id: Long, novo: Boolean = false) = "perfil/$id?novo=$novo"

/** Timer e Perfis são abas irmãs (fade-through, §5.10). A Configuração do Perfil entra pelo eixo X (§5.8). */
@Composable
fun RaizApp() {
    val timer: TimerViewModel = viewModel() // na Activity, não no destino: o Timer sobrevive à troca de aba
    val nav = rememberNavController()
    val rota = nav.currentBackStackEntryAsState().value?.destination?.route
    val desloca = with(LocalDensity.current) { 24.dp.roundToPx() }
    val perfil by timer.perfilAtivo.collectAsStateWithLifecycle()

    // Fora do Timer, o primary é a Cor de destaque de Foco (§3). As superfícies do M3 seguem o esquema escuro.
    MaterialTheme(colorScheme = darkColorScheme(primary = corDeDestaque(TipoFase.FOCO), onPrimary = TextoSobreDestaque)) {
        // O Fundo fica parado atrás do NavHost e nunca sai da tela (§5.10).
        FundoComVeu(perfil?.aparencia, naTelaTimer = rota == ROTA_TIMER)
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = TextoPrimario,
            bottomBar = { if (rota == ROTA_TIMER || rota == ROTA_PERFIS) BarraNavegacao(rota, nav) },
        ) { margens ->
            fun AnimatedContentTransitionScope<NavBackStackEntry>.paraConfig() = targetState.destination.route == ROTA_PERFIL
            fun AnimatedContentTransitionScope<NavBackStackEntry>.daConfig() = initialState.destination.route == ROTA_PERFIL
            val entra: EnterTransition = fadeIn(tween(DUR_CURTA, easing = EaseOut))
            val sai: ExitTransition = fadeOut(tween(DUR_SAIDA))
            val entraX = fadeIn(tween(DUR_MEDIA, easing = EaseOut)) + slideInHorizontally(tween(DUR_MEDIA, easing = EaseOut)) { desloca }
            val saiX = fadeOut(tween(DUR_SAIDA)) + slideOutHorizontally(tween(DUR_SAIDA)) { desloca }
            val voltaX = fadeIn(tween(DUR_MEDIA, easing = EaseOut)) + slideInHorizontally(tween(DUR_MEDIA, easing = EaseOut)) { -desloca }
            val recuaX = fadeOut(tween(DUR_SAIDA)) + slideOutHorizontally(tween(DUR_SAIDA)) { -desloca }

            NavHost(
                nav,
                startDestination = ROTA_TIMER,
                enterTransition = { entra },
                exitTransition = { if (paraConfig()) recuaX else sai },
                popEnterTransition = { if (daConfig()) voltaX else entra },
                popExitTransition = { sai },
            ) {
                composable(ROTA_TIMER) {
                    TelaTimer(timer, margens, aoEditarPerfil = { id -> nav.navigate(rotaPerfil(id)) })
                }
                composable(ROTA_PERFIS) {
                    TelaPerfis(margens, aoAbrir = { id, novo -> nav.navigate(rotaPerfil(id, novo)) })
                }
                composable(
                    ROTA_PERFIL,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("novo") { type = NavType.BoolType; defaultValue = false },
                    ),
                    enterTransition = { entraX },
                    popExitTransition = { saiX },
                ) { entrada ->
                    val args = entrada.arguments!!
                    TelaConfigPerfil(args.getLong("id"), args.getBoolean("novo"), margens, aoVoltar = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun BarraNavegacao(rota: String?, nav: NavHostController) {
    NavigationBar(containerColor = VidroBarra) {
        listOf(
            Triple(ROTA_TIMER, R.string.aba_timer, R.drawable.ic_timer),
            Triple(ROTA_PERFIS, R.string.aba_perfis, R.drawable.ic_perfis),
        ).forEach { (destino, rotulo, icone) ->
            NavigationBarItem(
                selected = rota == destino,
                onClick = {
                    nav.navigate(destino) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(icone), contentDescription = null) },
                label = { Text(stringResource(rotulo), style = EstiloLabel.copy(color = Color.Unspecified)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = TextoSobreDestaque,
                    selectedTextColor = TextoPrimario,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario,
                ),
            )
        }
    }
}

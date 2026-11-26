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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.dados.textoSobre
import com.exemplo.pomodoro.motor.TipoFase

private const val ROTA_TIMER = "timer"
private const val ROTA_HISTORICO = "historico"
private const val ROTA_PERFIS = "perfis"
private const val ROTA_PERFIL = "perfil/{id}?novo={novo}"
private fun rotaPerfil(id: Long, novo: Boolean = false) = "perfil/$id?novo=$novo"

/**
 * Timer, Histórico e Perfis são abas irmãs (fade-through, §5.10). A Configuração do Perfil entra pelo eixo X (§5.8).
 * [pedidosDoTimer]: cada aumento vem de um toque na Notificação de contagem ou no Alerta, e leva à aba Timer.
 */
@Composable
fun RaizApp(pedidosDoTimer: Int = 0) {
    val timer: TimerViewModel = viewModel() // na Activity, não no destino: o Timer sobrevive à troca de aba
    val nav = rememberNavController()
    LaunchedEffect(pedidosDoTimer) { if (pedidosDoTimer > 0) nav.popBackStack(ROTA_TIMER, inclusive = false) }
    // Antes do NavHost compor, a pilha está vazia: a rota é a de início. Sem isso, a abertura a frio pela Notificação
    // mostra um quadro "fora do Timer" (cor de Foco, Véu de leitura, sem a barra de abas) e depois pula.
    val rota = nav.currentBackStackEntryAsState().value?.destination?.route ?: ROTA_TIMER
    val desloca = with(LocalDensity.current) { 24.dp.roundToPx() }
    val perfil by timer.perfilAtivo.collectAsStateWithLifecycle()

    // O primary é a Cor de destaque da Fase atual no Timer e a de Foco fora dele (§3): o indicador da aba Timer acompanha
    // a Fase, na mesma mistura em OkLCh do botão primário. As superfícies do M3 seguem o esquema escuro.
    // derivedStateOf: o estado muda a cada segundo, o tipo só na troca de Fase. A raiz não recompõe a cada tique.
    val tipoAtual by remember { derivedStateOf { timer.estado.tipo } }
    val tipo = if (rota == ROTA_TIMER) tipoAtual else TipoFase.FOCO
    val alvo = Color(perfil?.aparencia?.destaque(tipo) ?: DESTAQUES_PADRAO[tipo.ordinal])
    val destaque by animarCor(alvo, tween(DUR_MEDIA, easing = EaseOut))
    val primario = destaque.toArgb()
    val fundoGravado = rememberFundoGravado()
    val parallax = remember { Parallax() }
    MaterialTheme(colorScheme = darkColorScheme(primary = destaque, onPrimary = Color(textoSobre(primario)))) {
        CompositionLocalProvider(LocalFundoGravado provides fundoGravado) {
            // O Fundo fica parado atrás do NavHost e nunca sai da tela (§5.10). Fica gravado para o vidro do Timer.
            Box(Modifier.fillMaxSize().gravarFundo(fundoGravado)) {
                CompositionLocalProvider(LocalParallax provides parallax) { FundoComVeu(perfil?.aparencia, naTelaTimer = rota == ROTA_TIMER) }
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = TextoPrimario,
                bottomBar = { if (rota == ROTA_TIMER || rota == ROTA_HISTORICO || rota == ROTA_PERFIS) BarraNavegacao(rota, nav) },
            ) { margens ->
                fun AnimatedContentTransitionScope<NavBackStackEntry>.paraConfig() = targetState.destination.route == ROTA_PERFIL
                fun AnimatedContentTransitionScope<NavBackStackEntry>.daConfig() = initialState.destination.route == ROTA_PERFIL
                val entra: EnterTransition = fadeIn(tween(DUR_CURTA, easing = EaseOut))
                val sai: ExitTransition = fadeOut(tween(DUR_SAIDA, easing = EaseOut))
                val entraX = fadeIn(tween(DUR_MEDIA, easing = EaseOut)) + slideInHorizontally(tween(DUR_MEDIA, easing = EaseOut)) { desloca }
                val saiX = fadeOut(tween(DUR_SAIDA, easing = EaseOut)) + slideOutHorizontally(tween(DUR_SAIDA, easing = EaseOut)) { desloca }
                val voltaX = fadeIn(tween(DUR_MEDIA, easing = EaseOut)) + slideInHorizontally(tween(DUR_MEDIA, easing = EaseOut)) { -desloca }
                val recuaX = fadeOut(tween(DUR_SAIDA, easing = EaseOut)) + slideOutHorizontally(tween(DUR_SAIDA, easing = EaseOut)) { -desloca }

                NavHost(
                    nav,
                    startDestination = ROTA_TIMER,
                    enterTransition = { entra },
                    exitTransition = { if (paraConfig()) recuaX else sai },
                    popEnterTransition = { if (daConfig()) voltaX else entra },
                    popExitTransition = { sai },
                ) {
                    composable(ROTA_TIMER) {
                        // Só o Timer liga o sensor do parallax (§7).
                        CompositionLocalProvider(LocalParallax provides parallax) {
                            TelaTimer(timer, margens, aoEditarPerfil = { id -> nav.navigate(rotaPerfil(id)) })
                        }
                    }
                    composable(ROTA_HISTORICO) { TelaHistorico(margens) }
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
}

@Composable
private fun BarraNavegacao(rota: String?, nav: NavHostController) {
    NavigationBar(containerColor = VidroBarra) {
        listOf(
            Triple(ROTA_TIMER, R.string.aba_timer, R.drawable.ic_timer),
            Triple(ROTA_HISTORICO, R.string.aba_historico, R.drawable.ic_historico),
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
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = TextoPrimario,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario,
                ),
            )
        }
    }
}

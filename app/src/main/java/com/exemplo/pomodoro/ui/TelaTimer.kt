package com.exemplo.pomodoro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.TimerViewModel
import com.exemplo.pomodoro.dados.ALFA_NUMEROS
import com.exemplo.pomodoro.dados.ALFA_NUMEROS_PAUSADO
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.dados.Perfil
import com.exemplo.pomodoro.dados.textoSobre
import com.exemplo.pomodoro.motor.Andamento
import com.exemplo.pomodoro.motor.ConfigCiclo
import com.exemplo.pomodoro.motor.EstadoCiclo
import com.exemplo.pomodoro.motor.MS_POR_MIN
import com.exemplo.pomodoro.motor.TipoFase
import com.exemplo.pomodoro.motor.paraMinutos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/** A entrada do Estilo visual acontece uma vez por processo (§5.1). */
private var estiloJaEntrou = false

@Composable
fun TelaTimer(vm: TimerViewModel, margens: PaddingValues, aoEditarPerfil: (Long) -> Unit) {
    val contexto = LocalContext.current
    // Pedido no Iniciar, sem segurar a Fase: ela começa de qualquer jeito. Depois de duas recusas, o sistema não pergunta mais.
    val pedirNotificacoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) vm.notificacoesPermitidas()
    }
    val aoIniciar = {
        vm.iniciar()
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pedirNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Volta do segundo plano: a tela é refeita direto no estado atual, sem replay da troca de Fase (DESIGN.md §5.1, §5.6).
    var voltas by remember { mutableIntStateOf(0) }
    LifecycleStartEffect(Unit) { onStopOrDispose { voltas++ } }
    key(voltas) { ConteudoTimer(vm, margens, aoIniciar, aoEditarPerfil) }
}

@Composable
private fun ConteudoTimer(vm: TimerViewModel, margens: PaddingValues, aoIniciar: () -> Unit, aoEditarPerfil: (Long) -> Unit) {
    val estado = vm.estado
    val perfil by vm.perfilAtivo.collectAsStateWithLifecycle()
    val estenderMin = paraMinutos((perfil?.tempo ?: ConfigCiclo.PADRAO).estenderMs)
    var seletorAberto by remember { mutableStateOf(false) }
    val view = LocalView.current
    // O háptico dispara no handler do toque, antes da animação (DESIGN.md §6).
    fun haptico(constante: Int) = view.performHapticFeedback(constante)
    val alternar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    val aoTocarPrimario = {
        when (estado.andamento) {
            Andamento.PARADO -> { haptico(HapticFeedbackConstants.CONFIRM); aoIniciar() }
            Andamento.RODANDO -> {
                haptico(if (alternar) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK)
                vm.pausar()
            }
            Andamento.PAUSADO -> {
                haptico(if (alternar) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK)
                vm.retomar()
            }
        }
    }
    val aoPular = { haptico(HapticFeedbackConstants.CONTEXT_CLICK); vm.pular() }
    val aoEstender = { haptico(HapticFeedbackConstants.CLOCK_TICK); vm.estender() }

    val aparencia = perfil?.aparencia
    val alvoDestaque = Color(aparencia?.destaque(estado.tipo) ?: DESTAQUES_PADRAO[estado.tipo.ordinal])
    val destaque by animarCor(alvoDestaque, tween(DUR_MEDIA, easing = EaseOut)) // OkLCh: sem o cáqui do coral ao menta

    // Entrada do Estilo visual, só na abertura a frio (§5.1): escala 0.92 → 1 e alfa, 80 ms depois do Fundo.
    // A volta do segundo plano e a troca de aba não repetem a entrada. Reduzido: só o fade (§9).
    val reduzido = rememberMovimentoReduzido()
    val entrada = remember { Animatable(if (estiloJaEntrou) 1f else 0f) }
    LaunchedEffect(aparencia != null) {
        if (aparencia == null || estiloJaEntrou) return@LaunchedEffect
        estiloJaEntrou = true
        delay(80)
        comEscalaNormal(reduzido) {
            entrada.animateTo(1f, if (reduzido) tween(DUR_MEDIA, easing = EaseOut) else spring(dampingRatio = 1f, stiffness = 250f))
        }
    }
    val progresso = if (estado.duracaoMs > 0) 1f - estado.restanteMs.toFloat() / estado.duracaoMs else 0f
    // [nucleo]: o disco dos números, que o Estilo mantém escuro (§3). Na entrada, o blob cresce de 0.92: o núcleo compensa.
    val estiloVisual: @Composable (Modifier, Dp) -> Unit = { m, nucleo ->
        val escala = if (reduzido) 1f else 0.92f + 0.08f * entrada.value
        if (aparencia != null) EstiloVisualAnimado(
            aparencia.estilo,
            progresso,
            estado.tipo,
            alvoDestaque,
            rodando = estado.andamento == Andamento.RODANDO,
            modifier = m.graphicsLayer {
                alpha = entrada.value
                scaleX = escala
                scaleY = escala
            },
            nucleo = nucleo / escala,
        )
    }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = destaque)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(margens)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val controles = @Composable {
                Controles(estado.andamento, destaque, estenderMin, aoTocarPrimario, aoPular, aoEstender)
            }
            val barraSuperior = @Composable {
                BarraSuperior(perfil?.nome, { seletorAberto = true }, vm::reiniciarCiclo)
            }
            if (maxWidth > maxHeight) {
                // Paisagem: a área do Estilo visual à esquerda; rótulo, números e controles à direita.
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxHeight().aspectRatio(1f).padding(24.dp)) { estiloVisual(Modifier.fillMaxSize(), 0.dp) }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        barraSuperior()
                        Spacer(Modifier.weight(1f))
                        BlocoFase(estado, estiloVisual = null)
                        Spacer(Modifier.weight(1f))
                        controles()
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Ritmo vertical (§8): o grupo rótulo + Estilo fica no centro óptico, um pouco acima do meio,
                    // e os controles vêm logo abaixo, sem o vazio entre os dois. A sobra vai mais para cima.
                    barraSuperior()
                    Spacer(Modifier.weight(1f))
                    BlocoFase(estado, estiloVisual, larguraNumeros(aparencia?.estilo))
                    Spacer(Modifier.height(16.dp))
                    Spacer(Modifier.weight(0.5f))
                    controles()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    val ativo = perfil
    if (seletorAberto && ativo != null) {
        val todos by vm.todosPerfis.collectAsStateWithLifecycle(emptyList())
        SeletorPerfil(
            todos = todos,
            ativoId = ativo.id,
            faseEmCurso = estado.andamento != Andamento.PARADO,
            aoFechar = { seletorAberto = false },
            aoEscolher = { id -> haptico(HapticFeedbackConstants.CONFIRM); vm.trocarPerfil(id) },
            aoEditar = { aoEditarPerfil(ativo.id) },
        )
    }
}

@Composable
private fun BarraSuperior(nomePerfil: String?, aoAbrirSeletor: () -> Unit, aoReiniciarCiclo: () -> Unit) {
    var menuAberto by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (nomePerfil != null) {
            ChipPerfil(nomePerfil, Modifier.align(Alignment.CenterStart).widthIn(max = maxWidth * 0.7f), aoAbrirSeletor)
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            BotaoVidro(stringResource(R.string.mais_opcoes), 48.dp, { menuAberto = true }) { IconeVidro(R.drawable.ic_mais) }
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reiniciar_ciclo), style = EstiloBody) },
                    onClick = { menuAberto = false; aoReiniciarCiclo() },
                )
            }
        }
    }
}

/** Chip de Perfil: uma linha com reticências, no máximo 70% da largura (§8). Na troca, o texto faz crossfade e a largura acompanha (§5.7). */
@Composable
private fun ChipPerfil(nome: String, modifier: Modifier, aoTocar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val tinta by animateColorAsState(if (pressionado) VidroClaroPressionado else VidroClaro, springPress())
    Row(
        modifier
            .height(48.dp)
            .pressionavel(interacao, 0.95f, stringResource(R.string.trocar_perfil, nome), aoTocar)
            .background(tinta, CircleShape)
            .border(1.dp, Brush.verticalGradient(listOf(BordaVidroTopo, BordaVidroBase)), CircleShape)
            .padding(start = 16.dp, end = 10.dp)
            .animateContentSize(springUi()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = nome,
            transitionSpec = { fadeIn(tween(DUR_CURTA, easing = EaseOut)).togetherWith(fadeOut(tween(DUR_CURTA))) },
            modifier = Modifier.weight(1f, fill = false),
            label = "chip",
        ) { n ->
            Text(n, style = EstiloLabel.copy(color = TextoPrimario), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_abrir), contentDescription = null, tint = TextoSecundario, modifier = Modifier.size(20.dp))
    }
}

/** Seletor de Perfil (§5.7). O toque num Perfil já confirma a troca; o sheet fecha enquanto o Timer muda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorPerfil(
    todos: List<Perfil>,
    ativoId: Long,
    faseEmCurso: Boolean,
    aoFechar: () -> Unit,
    aoEscolher: (Long) -> Unit,
    aoEditar: () -> Unit,
) {
    val estadoSheet = rememberModalBottomSheetState()
    val escopo = rememberCoroutineScope()
    fun fechar(depois: () -> Unit = {}) {
        escopo.launch { estadoSheet.hide() }.invokeOnCompletion { aoFechar(); depois() }
    }
    val semFundo = ListItemDefaults.colors(containerColor = Color.Transparent)
    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = estadoSheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (faseEmCurso) {
                Text(
                    stringResource(R.string.aviso_troca_perfil),
                    style = EstiloCaption,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            todos.forEach { p ->
                ListItem(
                    headlineContent = { Text(p.nome, style = EstiloBody, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(resumoTempo(p.tempo), style = EstiloCaption) },
                    trailingContent = if (p.id == ativoId) {
                        { Icon(painterResource(R.drawable.ic_marcado), contentDescription = stringResource(R.string.ativo)) }
                    } else null,
                    colors = semFundo,
                    modifier = Modifier.clickable {
                        if (p.id != ativoId) aoEscolher(p.id)
                        fechar()
                    },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.editar_perfil), style = EstiloBody) },
                colors = semFundo,
                modifier = Modifier.clickable { fechar(aoEditar) },
            )
        }
    }
}

/**
 * Rótulo da Fase, números e "Pausado". Na troca de Fase, o conteúdo antigo sobe e sai, e só então o novo sobe e entra
 * (§5.4, §5.6): os dois nunca dividem um quadro.
 * [estiloVisual] fica atrás dos números, fora da troca de conteúdo: é um objeto só, que faz a própria transição.
 * Ele recebe o núcleo, o disco dos números, para manter o miolo escuro sob eles (§3).
 */
@Composable
private fun BlocoFase(estado: EstadoCiclo, estiloVisual: (@Composable (Modifier, Dp) -> Unit)?, fracaoNumeros: Float = 1f) {
    val oitoDp = with(LocalDensity.current) { 8.dp.roundToPx() }
    val pausado = estado.andamento == Andamento.PAUSADO
    val alfaNumeros by animateFloatAsState(if (pausado) ALFA_NUMEROS_PAUSADO else ALFA_NUMEROS, tween(DUR_MEDIA, easing = EaseOut))
    val alfaPausado by animateFloatAsState(if (pausado) 1f else 0f, tween(DUR_CURTA, easing = EaseOut))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // A área do Estilo visual: 0.84 × largura, alinhada com a área dos números (a última coisa da coluna).
            val area = maxWidth * 0.84f
            val (tamanho, nucleo) = medirNumeros(area * fracaoNumeros, formatoLongo(estado.duracaoMs))
            estiloVisual?.invoke(Modifier.align(Alignment.BottomCenter).size(area), nucleo)
            AnimatedContent(
                targetState = estado,
                contentKey = { it.tipo to it.posicao }, // os segundos mudam sem animar (§10); só a troca de Fase anima
                transitionSpec = {
                    // Um sai, o outro entra: o novo só começa quando o antigo terminou de sair. Dois rótulos no mesmo
                    // lugar, nem que por um quadro, se leem como um borrão. O antigo sai rápido: o atraso fica curto.
                    (fadeIn(tween(DUR_MEDIA, DUR_SAIDA, EaseOut)) + slideInVertically(tween(DUR_MEDIA, DUR_SAIDA, EaseOut)) { oitoDp })
                        .togetherWith(fadeOut(tween(DUR_SAIDA, easing = EaseOut)) + slideOutVertically(tween(DUR_SAIDA, easing = EaseOut)) { -oitoDp })
                        .using(null)
                },
                label = "fase",
            ) { e ->
                val nome = nomeDaFase(e.tipo)
                val minutos = paraMinutos(e.restanteMs + MS_POR_MIN - 1) // para cima
                val descricao = pluralStringResource(R.plurals.descricao_timer, minutos, nome, e.posicao, e.focosPorCiclo, minutos)
                Column(
                    Modifier.clearAndSetSemantics { contentDescription = descricao },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RotuloFase(nome, stringResource(R.string.posicao_ciclo, e.posicao, e.focosPorCiclo))
                    Spacer(Modifier.height(20.dp))
                    val texto = @Composable {
                        Text(
                            formatarTempo(e.restanteMs, e.duracaoMs),
                            style = EstiloTimer.copy(fontSize = tamanho, color = Color.White.copy(alpha = alfaNumeros)),
                        )
                    }
                    if (estiloVisual != null) Box(Modifier.size(area), contentAlignment = Alignment.Center) { texto() } else texto()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.pausado),
            style = EstiloLabel,
            modifier = Modifier
                .graphicsLayer { alpha = alfaPausado }
                .then(if (pausado) Modifier else Modifier.clearAndSetSemantics {}),
        )
    }
}

/** O nome da Fase manda (title, branco); a posição no Ciclo acompanha, menor e secundária, na mesma linha de base. */
@Composable
private fun RotuloFase(nome: String, posicao: String) {
    val texto = buildAnnotatedString {
        append(nome)
        append("  ")
        withStyle(EstiloLabel.toSpanStyle()) { append(posicao) }
    }
    Text(texto, style = EstiloTitle.copy(color = TextoPrimario, textAlign = TextAlign.Center))
}

/**
 * Ajuste do `timer` (§2): o menor entre 88 e o tamanho em que a string mais larga cabe em [largura].
 * Ignora o fontScale de propósito. Recalcula só quando muda a largura ou o formato, nunca a cada segundo.
 * Devolve também o núcleo: a meia diagonal da caixa dos números, o disco que o Estilo mantém escuro.
 */
@Composable
private fun medirNumeros(largura: Dp, formatoLongo: Boolean): Pair<TextUnit, Dp> {
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    return remember(largura, formatoLongo, densidade) {
        val base = with(densidade) { 88.dp.toSp() }
        val maisLarga = if (formatoLongo) "0:00:00" else "00:00"
        val caixa = medidor.measure(maisLarga, EstiloTimer.copy(fontSize = base)).size
        val limite = with(densidade) { largura.toPx() }
        val k = minOf(1f, limite / caixa.width)
        val nucleo = with(densidade) { (hypot(caixa.width / 2f, caixa.height / 2f) * k).toDp() }
        base * k to nucleo
    }
}

@Composable
private fun Controles(
    andamento: Andamento,
    destaque: Color,
    estenderMin: Int,
    aoTocarPrimario: () -> Unit,
    aoPular: () -> Unit,
    aoEstender: () -> Unit,
) {
    val dezesseisDp = with(LocalDensity.current) { 16.dp.roundToPx() }
    val doze = with(LocalDensity.current) { 12.dp.toPx() }
    val escopo = rememberCoroutineScope()
    // Selo "+5 min" (§5.5): nasce sobre o Estender, sobe 12 dp e some em 700 ms. Cada toque faz nascer um selo novo.
    val selo = remember { Animatable(1f) }
    val descricaoPrimario = stringResource(
        when (andamento) {
            Andamento.PARADO -> R.string.iniciar
            Andamento.RODANDO -> R.string.pausar
            Andamento.PAUSADO -> R.string.retomar
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.size(56.dp)) {
            // Estender e Pular só aparecem com a Fase rodando ou pausada, saindo de trás do botão primário (§5.2).
            androidx.compose.animation.AnimatedVisibility( // a versão sem escopo, não a do Row
                visible = andamento != Andamento.PARADO,
                enter = fadeIn(springUi()) + slideInHorizontally(springUi()) { dezesseisDp },
                exit = fadeOut(springUi()) + slideOutHorizontally(springUi()) { dezesseisDp },
            ) {
                val descricao = pluralStringResource(R.plurals.estender_descricao, estenderMin, estenderMin)
                BotaoVidro(descricao, 56.dp, {
                    aoEstender()
                    escopo.launch { selo.snapTo(0f); selo.animateTo(1f, tween(700, easing = EaseOut)) }
                }) {
                    Text(stringResource(R.string.estender_rotulo, estenderMin), style = EstiloLabel.copy(color = TextoPrimario))
                }
            }
            Text(
                stringResource(R.string.estender_selo, estenderMin),
                style = EstiloLabel.copy(color = TextoPrimario),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-28).dp)
                    .graphicsLayer { alpha = 1f - selo.value; translationY = -doze * selo.value }
                    .clearAndSetSemantics {},
            )
        }
        BotaoPrimario(andamento == Andamento.RODANDO, destaque, descricaoPrimario, aoTocarPrimario)
        Box(Modifier.size(56.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = andamento != Andamento.PARADO,
                enter = fadeIn(springUi()) + slideInHorizontally(springUi()) { -dezesseisDp },
                exit = fadeOut(springUi()) + slideOutHorizontally(springUi()) { -dezesseisDp },
            ) {
                BotaoVidro(stringResource(R.string.pular_fase), 56.dp, aoPular) { IconeVidro(R.drawable.ic_pular) }
            }
        }
    }
}

/** Sem ripple: o toque responde com escala na descida, com SpringPress (§6). */
@Composable
private fun Modifier.pressionavel(
    interacao: MutableInteractionSource,
    escala: Float,
    descricao: String,
    aoTocar: () -> Unit,
): Modifier {
    val pressionado by interacao.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressionado) escala else 1f, springPress())
    return graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interacao, indication = null, role = Role.Button, onClick = aoTocar)
        .semantics { contentDescription = descricao }
}

@Composable
private fun BotaoPrimario(rodando: Boolean, cor: Color, descricao: String, aoTocar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val clareia by animateFloatAsState(if (pressionado) 0.08f else 0f, springPress())
    // Um tom claro e calmo da Cor de destaque: a cor cheia brigaria com o blob, que é o ator (§1).
    val fundo = tom(cor, 0.86f, 0.55f)
    val texto = Color(textoSobre(fundo.toArgb())) // branco ou #101014, o que der mais contraste (§3)
    Box(
        Modifier
            .size(88.dp)
            .pressionavel(interacao, 0.96f, descricao, aoTocar)
            .background(lerp(fundo, Color.White, clareia), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = rodando,
            transitionSpec = {
                (fadeIn(tween(DUR_CURTA, easing = EaseOut)) + scaleIn(tween(DUR_CURTA, easing = EaseOut), initialScale = 0.8f))
                    .togetherWith(fadeOut(tween(DUR_CURTA, easing = EaseOut)))
            },
            label = "icone",
        ) { r ->
            Icon(
                painterResource(if (r) R.drawable.ic_pausar else R.drawable.ic_iniciar),
                contentDescription = null,
                tint = texto,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun BotaoVidro(descricao: String, tamanho: Dp, aoTocar: () -> Unit, conteudo: @Composable () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val tinta by animateColorAsState(if (pressionado) VidroClaroPressionado else VidroClaro, springPress())
    Box(
        Modifier
            .size(tamanho)
            .pressionavel(interacao, 0.95f, descricao, aoTocar)
            .background(tinta, CircleShape)
            .border(1.dp, Brush.verticalGradient(listOf(BordaVidroTopo, BordaVidroBase)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        conteudo()
    }
}

@Composable
private fun IconeVidro(icone: Int) =
    Icon(painterResource(icone), contentDescription = null, tint = TextoPrimario, modifier = Modifier.size(24.dp))

@Composable
private fun nomeDaFase(tipo: TipoFase) = stringResource(nomeDaFaseRes(tipo))

@StringRes
internal fun nomeDaFaseRes(tipo: TipoFase) = when (tipo) {
    TipoFase.FOCO -> R.string.fase_foco
    TipoFase.PAUSA_CURTA -> R.string.fase_pausa_curta
    TipoFase.PAUSA_LONGA -> R.string.fase_pausa_longa
}

private fun formatoLongo(duracaoMs: Long) = duracaoMs >= 60 * MS_POR_MIN

/** `MM:SS` para Fases de até 59 min e `H:MM:SS` a partir de 60 min (§2). Arredonda para cima: começa em 25:00. */
internal fun formatarTempo(restanteMs: Long, duracaoMs: Long): String {
    val s = (restanteMs + 999) / 1000
    return if (formatoLongo(duracaoMs)) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)
}

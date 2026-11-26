package com.exemplo.pomodoro.ui

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.pomodoro.AppPomodoro
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.agregacao.Barra
import com.exemplo.pomodoro.agregacao.FiltroPerfil
import com.exemplo.pomodoro.agregacao.Periodo
import com.exemplo.pomodoro.agregacao.Totais
import com.exemplo.pomodoro.agregacao.barras
import com.exemplo.pomodoro.agregacao.dia
import com.exemplo.pomodoro.agregacao.filtrar
import com.exemplo.pomodoro.agregacao.heatmap
import com.exemplo.pomodoro.agregacao.nivelHeatmap
import com.exemplo.pomodoro.agregacao.perfisExcluidos
import com.exemplo.pomodoro.agregacao.sequencia
import com.exemplo.pomodoro.agregacao.totais
import com.exemplo.pomodoro.dados.DESTAQUES_PADRAO
import com.exemplo.pomodoro.dados.FaseRegistrada
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as EstiloData
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ptBR = Locale.forLanguageTag("pt-BR")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val formatoDia = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", ptBR) // "seg., 23 de nov."
private val formatoSemana = DateTimeFormatter.ofPattern("d/MM") // "22/11"
private val formatoMes = DateTimeFormatter.ofPattern("MMM", ptBR) // "nov."
private val numero = NumberFormat.getIntegerInstance(ptBR) // "1.234"

/** A entrada das barras acontece uma vez por processo (§5.11). */
private var barrasJaEntraram = false

/** O gráfico, os totais e a sequência no topo; abaixo, a lista cronológica das Fases, agrupada por dia. Tudo segue o filtro por Perfil. */
@Composable
fun TelaHistorico(margens: PaddingValues) {
    val app = LocalContext.current.applicationContext as AppPomodoro
    val historico = app.historico
    // Nulo até o banco responder: sem spinner, nada aparece até os dados chegarem (§10).
    val fases by historico.todas.collectAsStateWithLifecycle(null)
    val perfis by app.perfis.todos.collectAsStateWithLifecycle(emptyList())
    val ativo by app.perfis.ativo.collectAsStateWithLifecycle(null)
    var filtroEscolhido by remember { mutableStateOf<FiltroPerfil>(FiltroPerfil.Todos) }
    var periodo by remember { mutableStateOf(Periodo.DIA) }
    val escopo = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }
    val textoApagada = stringResource(R.string.fase_apagada)
    val textoDesfazer = stringResource(R.string.desfazer)

    fun apagar(f: FaseRegistrada) {
        historico.apagar(f.id)
        escopo.launch {
            avisos.currentSnackbarData?.dismiss()
            // Short = 4 s. Desfazer devolve a Fase com o mesmo id.
            if (avisos.showSnackbar(textoApagada, textoDesfazer, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                historico.registrar(f)
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(margens)) {
        val lista = fases
        val excluidos = remember(lista) { perfisExcluidos(lista.orEmpty()) }
        // Um Perfil que sumiu da lista (excluído, ou o último registro dele apagado) volta o filtro para Todos.
        val filtro = when (val f = filtroEscolhido) {
            is FiltroPerfil.Existente -> if (perfis.any { it.id == f.id }) f else FiltroPerfil.Todos
            is FiltroPerfil.Excluido -> if (f.nome in excluidos) f else FiltroPerfil.Todos
            FiltroPerfil.Todos -> f
        }
        Column {
            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.aba_historico), style = EstiloTitle, color = TextoPrimario, modifier = Modifier.weight(1f))
                val opcoes = listOf(FiltroPerfil.Todos to stringResource(R.string.filtro_todos)) +
                    perfis.map { FiltroPerfil.Existente(it.id) to it.nome } +
                    excluidos.map { FiltroPerfil.Excluido(it) to stringResource(R.string.perfil_excluido, it) }
                FiltroDePerfil(opcoes, filtro, aoEscolher = { filtroEscolhido = it }, Modifier.widthIn(max = 200.dp))
            }
            lista ?: return@Column
            val zona = ZoneId.systemDefault()
            val hoje = LocalDate.now(zona)
            val filtradas = remember(lista, filtro) { filtrar(lista, filtro) }
            // A lista já vem da mais recente para a mais antiga; o groupBy mantém a ordem.
            val porDia = remember(filtradas, zona) { filtradas.groupBy { dia(it, zona) } }
            // Sem animação de entrada das linhas (§10).
            LazyColumn(Modifier.widthIn(max = 560.dp)) {
                item(key = "graficos") {
                    Graficos(filtradas, periodo, aoMudarPeriodo = { periodo = it }, contarSequencia = filtro == FiltroPerfil.Todos, zona, hoje)
                }
                if (filtradas.isEmpty()) {
                    item(key = "vazio") {
                        Text(
                            stringResource(R.string.historico_vazio),
                            style = EstiloBody,
                            color = TextoSecundario,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                porDia.forEach { (dia, doDia) ->
                    item(key = "dia-$dia") {
                        Text(
                            when (dia) {
                                hoje -> stringResource(R.string.hoje)
                                hoje.minusDays(1) -> stringResource(R.string.ontem)
                                else -> formatoDia.format(dia)
                            },
                            style = EstiloLabel,
                            color = TextoSecundario,
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, placementSpec = springUi(), fadeOutSpec = null)
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(doDia, key = { it.id }) { f ->
                        LinhaFase(
                            f, zona,
                            // As cores das Fases são as do Perfil ativo, as mesmas do Timer.
                            cor = Color(ativo?.aparencia?.destaque(f.tipo) ?: DESTAQUES_PADRAO[f.tipo.ordinal]),
                            aoApagar = { apagar(f) },
                            // Os vizinhos fecham o buraco enquanto a linha sai (§6).
                            modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = springUi(), fadeOutSpec = null),
                        )
                    }
                }
            }
        }
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter))
    }
}

/** Chip com o Perfil do filtro. Só o nome trunca; a seta fica. */
@Composable
private fun FiltroDePerfil(
    opcoes: List<Pair<FiltroPerfil, String>>,
    atual: FiltroPerfil,
    aoEscolher: (FiltroPerfil) -> Unit,
    modifier: Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    val nome = opcoes.firstOrNull { it.first == atual }?.second.orEmpty()
    Box(modifier) {
        Row(
            Modifier
                .background(VidroClaro, CircleShape)
                .border(1.dp, BordaVidro, CircleShape)
                .clickable { aberto = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val descricao = stringResource(R.string.filtrar_por_perfil, nome)
            Text(
                nome,
                style = EstiloLabel,
                color = TextoPrimario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).semantics { contentDescription = descricao },
            )
            Text(" ▾", style = EstiloLabel, color = TextoSecundario, modifier = Modifier.clearAndSetSemantics {})
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (filtro, rotulo) ->
                DropdownMenuItem(text = { Text(rotulo, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { aberto = false; aoEscolher(filtro) })
            }
        }
    }
}

@Composable
private fun Graficos(
    fases: List<FaseRegistrada>,
    periodo: Periodo,
    aoMudarPeriodo: (Periodo) -> Unit,
    contarSequencia: Boolean,
    zona: ZoneId,
    hoje: LocalDate,
) {
    val reduzido = rememberMovimentoReduzido()
    Column(Modifier.padding(horizontal = 20.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
            val rotulos = listOf(R.string.periodo_dia, R.string.periodo_semana, R.string.periodo_mes)
            Periodo.entries.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = p == periodo,
                    onClick = { aoMudarPeriodo(p) },
                    shape = SegmentedButtonDefaults.itemShape(i, Periodo.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = VidroClaroPressionado,
                        activeContentColor = TextoPrimario,
                        activeBorderColor = BordaVidroTopo,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextoSecundario,
                        inactiveBorderColor = BordaVidroTopo,
                    ),
                    icon = {},
                    label = { Text(stringResource(rotulos[i]), style = EstiloLabel) },
                )
            }
        }
        // Mudar o período troca o número de barras: crossfade do gráfico inteiro, sem crescer (§5.11).
        Crossfade(periodo, animationSpec = tween(DUR_MEDIA, easing = EaseOut), label = "periodo") { p ->
            val b = remember(fases, p, hoje) { barras(fases, p, zona, hoje) }
            GraficoBarras(b, p, reduzido)
        }
        val t = remember(fases, periodo, hoje) { totais(fases, periodo, zona, hoje) }
        BlocoTotais(t)
        Sequencia(remember(fases, hoje) { sequencia(fases, zona, hoje) }, contarSequencia, reduzido)
        Heatmap(remember(fases, hoje) { heatmap(fases, zona, hoje) })
    }
}

/** Atraso de cada barra na primeira entrada: stagger decrescente de 30, 25, 20, 15 e 10 ms; da 6ª em diante, o atraso da 6ª. */
private val ATRASOS_MS = floatArrayOf(0f, 30f, 55f, 75f, 90f, 100f)
private const val CRESCER_MS = 300f
private const val ENTRADA_MS = 400f // 100 ms do último atraso + 300 ms de crescimento

@Composable
private fun GraficoBarras(barras: List<Barra>, periodo: Periodo, reduzido: Boolean) {
    val cor = MaterialTheme.colorScheme.primary // a Cor de destaque de Foco do Perfil ativo (§3)
    // Uma linha do tempo só, em ms, para todas as barras: cada uma lê o próprio trecho dela.
    val entrada = remember { Animatable(if (barrasJaEntraram || reduzido) ENTRADA_MS else 0f) }
    val opacidade = remember { Animatable(if (barrasJaEntraram || !reduzido) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (barrasJaEntraram) return@LaunchedEffect
        barrasJaEntraram = true
        if (reduzido) {
            comEscalaNormal(true) { opacidade.animateTo(1f, tween(DUR_CURTA, easing = EaseOut)) }
        } else {
            entrada.animateTo(ENTRADA_MS, tween(ENTRADA_MS.toInt(), easing = LinearEasing))
        }
    }
    // Mudar o filtro por Perfil (mesmo número de barras): as alturas vão do valor antigo ao novo (§5.11).
    val minutos = barras.map { animateFloatAsState(it.minutosFoco.toFloat(), springUi(), label = "barra").value }
    val horasEixo = ceil((barras.maxOfOrNull { it.minutosFoco } ?: 0) / 60f).toInt().coerceAtLeast(1)
    val escala by animateFloatAsState(horasEixo * 60f, springUi(), label = "escala")

    val rotulos = barras.map { rotuloDaBarra(it.inicio, periodo) }
    val textos = barras.map { textoMinutos(it.minutosFoco) }
    val descricao = stringResource(R.string.descricao_barras, rotulos.zip(textos).joinToString { (r, t) -> "$r: $t" })
    val linha = Color.White.copy(alpha = 0.12f)
    Column(Modifier.graphicsLayer { alpha = opacidade.value }.semantics(mergeDescendants = true) { contentDescription = descricao }) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(Modifier.fillMaxSize().padding(end = LARGURA_EIXO)) {
                val base = size.height
                drawLine(linha, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(TextoTerciario, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
                val slot = size.width / barras.size
                val largura = slot * 0.56f
                val raio = minOf(4.dp.toPx(), largura / 2)
                for (i in barras.indices) {
                    val t = (entrada.value - ATRASOS_MS[minOf(i, ATRASOS_MS.lastIndex)]) / CRESCER_MS
                    val altura = base * (minutos[i] / escala).coerceIn(0f, 1f) * EaseOut.transform(t.coerceIn(0f, 1f))
                    if (altura < 0.5f) continue // 0 minutos: só a linha de base
                    drawRoundRect(cor, Offset(i * slot + (slot - largura) / 2, base - altura), Size(largura, altura), CornerRadius(raio))
                }
            }
            Text(
                stringResource(R.string.eixo_horas, horasEixo),
                style = EstiloCaption,
                color = TextoTerciario,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = (-8).dp),
            )
            Text("0", style = EstiloCaption, color = TextoTerciario, modifier = Modifier.align(Alignment.BottomEnd).offset(y = 8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(end = LARGURA_EIXO, top = 6.dp)) {
            rotulos.forEach {
                Text(it, style = EstiloCaption, color = TextoTerciario, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
            }
        }
    }
}

private val LARGURA_EIXO = 32.dp

private fun rotuloDaBarra(inicio: LocalDate, periodo: Periodo) = when (periodo) {
    Periodo.DIA -> inicio.dayOfWeek.getDisplayName(EstiloData.NARROW, ptBR) // D S T Q Q S S
    Periodo.SEMANA -> formatoSemana.format(inicio)
    Periodo.MES -> formatoMes.format(inicio).removeSuffix(".")
}

/** "40 min"; de 60 min para cima, "20 h 05 min" (§8). */
@Composable
private fun textoMinutos(minutos: Int) =
    if (minutos < 60) stringResource(R.string.valor_min, minutos) else stringResource(R.string.valor_h_min, minutos / 60, minutos % 60)

/** Os totais aparecem com a tela, sem animar os números (§10). */
@Composable
private fun BlocoTotais(t: Totais) {
    Column(Modifier.padding(top = 24.dp)) {
        Row {
            Total(textoMinutos(t.minutosFoco), R.string.total_minutos_foco, Modifier.weight(1f))
            Total(numero.format(t.focosConcluidos), R.string.total_focos_concluidos, Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 16.dp)) {
            Total(numero.format(t.focosIncompletos), R.string.total_focos_incompletos, Modifier.weight(1f))
            Total(textoMinutos(t.minutosPausa), R.string.total_minutos_pausa, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Total(valor: String, rotulo: Int, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}.padding(end = 8.dp)) {
        // "20 h 40 min" não cabe a 40 sp em meia tela: o display encolhe até caber, numa linha.
        BasicText(valor, style = EstiloDisplay, maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 40.sp))
        Text(stringResource(rotulo), style = EstiloCaption, color = TextoSecundario)
    }
}

private const val PREFS_HISTORICO = "historico"
private const val CHAVE_SEQUENCIA_VISTA = "sequencia_vista"

/**
 * Se a sequência subiu desde a última vez que foi vista, o número conta do antigo ao novo e o ícone brilha (§5.11).
 * Só conta a sequência de Todos ([contar]); com filtro, ela só aparece.
 */
@Composable
private fun Sequencia(dias: Int, contar: Boolean, reduzido: Boolean) {
    val prefs = LocalContext.current.getSharedPreferences(PREFS_HISTORICO, Context.MODE_PRIVATE)
    val mostrado = remember {
        val vista = prefs.getInt(CHAVE_SEQUENCIA_VISTA, dias)
        Animatable((if (contar && vista < dias) vista else dias).toFloat())
    }
    val brilho = remember { Animatable(0f) }
    LaunchedEffect(dias, contar) {
        if (!contar) return@LaunchedEffect mostrado.snapTo(dias.toFloat())
        prefs.edit { putInt(CHAVE_SEQUENCIA_VISTA, dias) }
        if (mostrado.value >= dias) return@LaunchedEffect mostrado.snapTo(dias.toFloat())
        launch {
            brilho.snapTo(1f)
            comEscalaNormal(true) { brilho.animateTo(0f, tween(900, easing = EaseOut)) }
        }
        // Movimento reduzido: troca de valor sem contar (§9).
        if (reduzido) mostrado.snapTo(dias.toFloat()) else mostrado.animateTo(dias.toFloat(), tween(600, easing = EaseOut))
    }

    val cor = MaterialTheme.colorScheme.primary // a Cor de destaque de Foco do Perfil ativo (§3)
    Row(Modifier.padding(top = 24.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = brilho.value }
                    .background(Brush.radialGradient(listOf(cor.copy(alpha = 0.55f), Color.Transparent)), CircleShape),
            )
            Icon(painterResource(R.drawable.ic_sequencia), contentDescription = null, tint = cor)
        }
        Spacer(Modifier.width(12.dp))
        val n = mostrado.value.roundToInt()
        if (dias == 0) {
            Text(stringResource(R.string.sequencia_nenhuma), style = EstiloBody, color = TextoSecundario)
        } else {
            Text(numero.format(n), style = EstiloDisplay)
            Spacer(Modifier.width(8.dp))
            Text(pluralStringResource(R.plurals.sequencia_dias, n), style = EstiloBody, color = TextoSecundario)
        }
    }
}

private val NIVEIS_ALFA = floatArrayOf(0.25f, 0.45f, 0.70f, 1f)
private val CELULA = 5.dp
private val PASSO = 6.dp // célula + 1 dp de espaço

/** 53 × 7, domingo no topo. Não interativo e sem animação (§5.11). Se não couber, rola e começa no fim (hoje). */
@Composable
private fun Heatmap(minutos: List<Int>) {
    val cor = MaterialTheme.colorScheme.primary // a Cor de destaque de Foco do Perfil ativo (§3)
    val vazio = Color.White.copy(alpha = 0.06f)
    val descricao = pluralStringResource(
        R.plurals.descricao_heatmap,
        minutos.count { it > 0 },
        minutos.count { it > 0 },
        textoMinutos(minutos.sum()),
    )
    val colunas = (minutos.size + 6) / 7
    Box(
        Modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState(Int.MAX_VALUE))
            .semantics { contentDescription = descricao },
    ) {
        Canvas(Modifier.size(PASSO * colunas - 1.dp, PASSO * 7 - 1.dp)) {
            val passo = PASSO.toPx()
            val lado = CELULA.toPx()
            minutos.forEachIndexed { i, m ->
                val nivel = nivelHeatmap(m)
                val canto = Offset(i / 7 * passo, i % 7 * passo)
                drawRect(if (nivel == 0) vazio else cor.copy(alpha = NIVEIS_ALFA[nivel - 1]), canto, Size(lado, lado))
                if (i == minutos.lastIndex) { // hoje: contorno de 1 dp
                    val meio = 0.5.dp.toPx()
                    drawRect(TextoPrimario, canto - Offset(meio, meio), Size(lado + 2 * meio, lado + 2 * meio), style = Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun LinhaFase(f: FaseRegistrada, zona: ZoneId, cor: Color, aoApagar: () -> Unit, modifier: Modifier) {
    val view = LocalView.current
    // remember, não rememberSaveable: o estado salvo na chave da linha voltaria dispensado quando o Desfazer
    // devolve a Fase com o mesmo id, e o SwipeToDismissBox apagaria a linha de novo.
    val deslize = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { it * 0.4f }) }

    // Háptico ao cruzar o limiar e ao voltar dele (§6).
    LaunchedEffect(deslize) {
        snapshotFlow { deslize.targetValue == SwipeToDismissBoxValue.EndToStart }.drop(1).collect { armado ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> view.performHapticFeedback(
                    if (armado) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE,
                )
                armado -> view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }
    }

    SwipeToDismissBox(
        deslize,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { aoApagar() },
        backgroundContent = {
            // Só a faixa que a linha descobre fica vermelha: a linha é transparente sobre o Fundo.
            val descoberto = with(LocalDensity.current) {
                (-runCatching { deslize.requireOffset() }.getOrDefault(0f)).coerceAtLeast(0f).toDp()
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxHeight().width(descoberto).clipToBounds().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(R.drawable.ic_apagar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.wrapContentWidth(Alignment.End, unbounded = true).padding(end = 24.dp),
                )
            }
        },
    ) {
        var menuAberto by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { menuAberto = true } // o menu da linha é a opção acessível ao swipe
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Concluída ●, incompleta ◐. O texto "incompleto" diz o mesmo sem depender da cor.
            Text(
                if (f.concluida) "●" else "◐",
                style = EstiloBody,
                color = cor,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(rotuloDaFase(f)), style = EstiloBody, color = TextoPrimario, maxLines = 1, softWrap = false)
            // Só o nome do Perfil trunca; horário e minutos nunca.
            Text(
                stringResource(R.string.sufixo_perfil, f.perfilNome),
                style = EstiloBody,
                color = TextoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp).weight(1f),
            )
            Text(
                formatoHora.format(Instant.ofEpochMilli(f.inicioEpochMs).atZone(zona)),
                style = EstiloBody,
                color = TextoSecundario,
                softWrap = false,
                modifier = Modifier.padding(start = 12.dp),
            )
            Text(
                stringResource(R.string.valor_min, f.minutosEfetivos),
                style = EstiloBody,
                color = TextoPrimario,
                softWrap = false,
                modifier = Modifier.padding(start = 12.dp),
            )
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.apagar)) }, onClick = { menuAberto = false; aoApagar() })
            }
        }
    }
}

private fun rotuloDaFase(f: FaseRegistrada) = when {
    f.concluida -> nomeDaFaseRes(f.tipo)
    f.tipo == TipoFase.FOCO -> R.string.fase_foco_incompleto
    f.tipo == TipoFase.PAUSA_CURTA -> R.string.fase_pausa_curta_incompleta
    else -> R.string.fase_pausa_longa_incompleta
}

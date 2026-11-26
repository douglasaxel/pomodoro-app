package com.exemplo.pomodoro.agregacao

import com.exemplo.pomodoro.dados.FaseRegistrada
import com.exemplo.pomodoro.motor.TipoFase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// Agregação do Histórico: funções puras sobre os registros, o fuso e a data de referência (spec, seam 2).
// Uma Fase conta no dia em que começou, como na lista.

enum class Periodo { DIA, SEMANA, MES }

/** Uma barra do gráfico: o primeiro dia do intervalo e os minutos de Foco nele. */
data class Barra(val inicio: LocalDate, val minutosFoco: Int)

/** Dia: os 7 dias da semana atual. Semana: as últimas 8 semanas. Mês: os últimos 12 meses (DESIGN §8). */
fun barras(fases: List<FaseRegistrada>, periodo: Periodo, zona: ZoneId, hoje: LocalDate): List<Barra> {
    val inicios = when (periodo) {
        Periodo.DIA -> (0L..6L).map { domingo(hoje).plusDays(it) }
        Periodo.SEMANA -> (7L downTo 0L).map { domingo(hoje).minusWeeks(it) }
        Periodo.MES -> (11L downTo 0L).map { hoje.withDayOfMonth(1).minusMonths(it) }
    }
    val porInicio = fases.filter { it.tipo == TipoFase.FOCO }
        .groupBy { inicioDoIntervalo(periodo, dia(it, zona)) }
    return inicios.map { i -> Barra(i, porInicio[i].orEmpty().sumOf { it.minutosEfetivos }) }
}

data class Totais(val minutosFoco: Int, val focosConcluidos: Int, val focosIncompletos: Int, val minutosPausa: Int)

/** Os totais do período atual: hoje, esta semana ou este mês. */
fun totais(fases: List<FaseRegistrada>, periodo: Periodo, zona: ZoneId, hoje: LocalDate): Totais {
    val atual = inicioDoIntervalo(periodo, hoje)
    val doPeriodo = fases.filter { inicioDoIntervalo(periodo, dia(it, zona)) == atual }
    val (focos, pausas) = doPeriodo.partition { it.tipo == TipoFase.FOCO }
    return Totais(
        minutosFoco = focos.sumOf { it.minutosEfetivos },
        focosConcluidos = focos.count { it.concluida },
        focosIncompletos = focos.count { !it.concluida },
        minutosPausa = pausas.sumOf { it.minutosEfetivos },
    )
}

/** Dias seguidos com pelo menos um Foco concluído. Enquanto hoje não tem um, a sequência vale até ontem. */
fun sequencia(fases: List<FaseRegistrada>, zona: ZoneId, hoje: LocalDate): Int {
    val dias = fases.filter { it.tipo == TipoFase.FOCO && it.concluida }.mapTo(HashSet()) { dia(it, zona) }
    var d = if (hoje in dias) hoje else hoje.minusDays(1)
    var n = 0
    while (d in dias) { n++; d = d.minusDays(1) }
    return n
}

/**
 * Minutos de Foco por dia, do domingo de 52 semanas atrás até hoje (a última célula).
 * O índice i fica na coluna i / 7 e na linha i % 7 (domingo no topo): 53 colunas, a última incompleta.
 */
fun heatmap(fases: List<FaseRegistrada>, zona: ZoneId, hoje: LocalDate): List<Int> {
    val inicio = domingo(hoje).minusWeeks(52)
    val dias = ChronoUnit.DAYS.between(inicio, hoje).toInt() + 1
    val minutos = IntArray(dias)
    for (f in fases) {
        if (f.tipo != TipoFase.FOCO) continue
        val i = ChronoUnit.DAYS.between(inicio, dia(f, zona)).toInt()
        if (i in 0 until dias) minutos[i] += f.minutosEfetivos
    }
    return minutos.asList()
}

/** Escala fixa do heatmap: 0, 1–25, 26–60, 61–120 e mais de 120 min viram os níveis 0 a 4. */
fun nivelHeatmap(minutos: Int) = when {
    minutos <= 0 -> 0
    minutos <= 25 -> 1
    minutos <= 60 -> 2
    minutos <= 120 -> 3
    else -> 4
}

/** Todos, um Perfil existente (pelo id) ou um Perfil excluído (pelo nome copiado). */
sealed interface FiltroPerfil {
    data object Todos : FiltroPerfil
    data class Existente(val id: Long) : FiltroPerfil
    data class Excluido(val nome: String) : FiltroPerfil
}

// ponytail: dois Perfis excluídos com o mesmo nome viram um só no filtro; guardar o id antigo resolve, se importar.
fun filtrar(fases: List<FaseRegistrada>, filtro: FiltroPerfil): List<FaseRegistrada> = when (filtro) {
    FiltroPerfil.Todos -> fases
    is FiltroPerfil.Existente -> fases.filter { it.perfilId == filtro.id }
    is FiltroPerfil.Excluido -> fases.filter { it.perfilId == null && it.perfilNome == filtro.nome }
}

/** Os nomes copiados dos Perfis excluídos que ainda têm Fases, sem repetir, em ordem alfabética. */
fun perfisExcluidos(fases: List<FaseRegistrada>): List<String> =
    fases.filter { it.perfilId == null }.map { it.perfilNome }.distinct().sorted()

/** A semana começa no domingo. */
private fun domingo(d: LocalDate) = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

private fun inicioDoIntervalo(periodo: Periodo, d: LocalDate) = when (periodo) {
    Periodo.DIA -> d
    Periodo.SEMANA -> domingo(d)
    Periodo.MES -> d.withDayOfMonth(1)
}

private fun dia(f: FaseRegistrada, zona: ZoneId) = Instant.ofEpochMilli(f.inicioEpochMs).atZone(zona).toLocalDate()

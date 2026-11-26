package com.exemplo.pomodoro.agregacao

import com.exemplo.pomodoro.dados.FaseRegistrada
import com.exemplo.pomodoro.motor.TipoFase
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

/** Agregação do Histórico (seam 2). Hoje é quinta, 26/11/2026; a semana vai de domingo 22 a sábado 28. */
class AgregacaoHistoricoTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val hoje = LocalDate.of(2026, 11, 26)

    private fun fase(
        inicio: String,
        minutos: Int,
        tipo: TipoFase = TipoFase.FOCO,
        concluida: Boolean = true,
        perfilId: Long? = 1,
        perfilNome: String = "Trabalho 25/5",
        noFuso: ZoneId = zona,
    ): FaseRegistrada {
        val ms = LocalDateTime.parse(inicio).atZone(noFuso).toInstant().toEpochMilli()
        return FaseRegistrada(
            tipo = tipo, inicioEpochMs = ms, fimEpochMs = ms + minutos * 60_000L,
            minutosEfetivos = minutos, concluida = concluida, perfilId = perfilId, perfilNome = perfilNome,
        )
    }

    @Test
    fun `Dia mostra as 7 barras da semana atual, de domingo a sabado, com os minutos de Foco`() {
        val fases = listOf(
            fase("2026-11-22T09:00", 25), // domingo
            fase("2026-11-26T10:00", 25),
            fase("2026-11-26T11:00", 12, concluida = false), // Foco incompleto conta os minutos reais
            fase("2026-11-26T11:30", 5, tipo = TipoFase.PAUSA_CURTA), // Pausa não entra
            fase("2026-11-21T09:00", 50), // sábado da semana anterior
        )
        val barras = barras(fases, Periodo.DIA, zona, hoje)
        assertEquals((22..28).map { LocalDate.of(2026, 11, it) }, barras.map { it.inicio })
        assertEquals(listOf(25, 0, 0, 0, 37, 0, 0), barras.map { it.minutosFoco })
    }

    @Test
    fun `a borda do dia segue o fuso local, nao o UTC`() {
        val fases = listOf(
            fase("2026-11-23T23:59", 25), // segunda 23:59 em São Paulo = terça 02:59 UTC
            fase("2026-11-24T00:00", 10), // terça 00:00
        )
        assertEquals(listOf(0, 25, 10, 0, 0, 0, 0), barras(fases, Periodo.DIA, zona, hoje).map { it.minutosFoco })
    }

    @Test
    fun `Semana mostra as ultimas 8 semanas, cada uma comecando no domingo`() {
        val fases = listOf(
            fase("2026-11-21T23:59", 25), // sábado: semana de 15/11
            fase("2026-11-22T00:00", 10), // domingo: semana atual
            fase("2026-10-04T12:00", 40), // domingo de 8 semanas atrás: primeira barra
            fase("2026-10-03T12:00", 99), // fora da janela
        )
        val barras = barras(fases, Periodo.SEMANA, zona, hoje)
        assertEquals(
            listOf("2026-10-04", "2026-10-11", "2026-10-18", "2026-10-25", "2026-11-01", "2026-11-08", "2026-11-15", "2026-11-22")
                .map(LocalDate::parse),
            barras.map { it.inicio },
        )
        assertEquals(listOf(40, 0, 0, 0, 0, 0, 25, 10), barras.map { it.minutosFoco })
    }

    @Test
    fun `Mes mostra os ultimos 12 meses`() {
        val fases = listOf(
            fase("2026-11-01T00:00", 25),
            fase("2026-10-31T23:59", 15),
            fase("2025-12-15T08:00", 30), // primeiro mês da janela
            fase("2025-11-30T08:00", 99), // fora da janela
        )
        val barras = barras(fases, Periodo.MES, zona, hoje)
        assertEquals(LocalDate.of(2025, 12, 1), barras.first().inicio)
        assertEquals(LocalDate.of(2026, 11, 1), barras.last().inicio)
        assertEquals(listOf(30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 25), barras.map { it.minutosFoco })
    }

    private val doMes = listOf(
        fase("2026-11-26T09:00", 25),
        fase("2026-11-26T09:25", 5, tipo = TipoFase.PAUSA_CURTA),
        fase("2026-11-26T09:30", 12, concluida = false),
        fase("2026-11-26T09:42", 3, tipo = TipoFase.PAUSA_LONGA, concluida = false),
        fase("2026-11-22T09:00", 50), // domingo: mesma semana
        fase("2026-11-22T09:50", 15, tipo = TipoFase.PAUSA_LONGA),
        fase("2026-11-03T09:00", 25), // mesmo mês
        fase("2026-10-31T09:00", 25, concluida = false), // mês anterior
    )

    @Test
    fun `totais do Dia contam so hoje`() {
        assertEquals(
            Totais(minutosFoco = 37, focosConcluidos = 1, focosIncompletos = 1, minutosPausa = 8),
            totais(doMes, Periodo.DIA, zona, hoje),
        )
    }

    @Test
    fun `totais da Semana contam a semana atual, desde domingo`() {
        assertEquals(Totais(87, 2, 1, 23), totais(doMes, Periodo.SEMANA, zona, hoje))
    }

    @Test
    fun `totais do Mes contam o mes atual`() {
        assertEquals(Totais(112, 3, 1, 23), totais(doMes, Periodo.MES, zona, hoje))
    }

    @Test
    fun `sem registros os totais sao zero`() {
        assertEquals(Totais(0, 0, 0, 0), totais(emptyList(), Periodo.MES, zona, hoje))
    }

    @Test
    fun `sequencia sem buracos conta os dias seguidos ate hoje`() {
        val fases = (0L..4L).map { fase("${hoje.minusDays(it)}T10:00", 25) } + fase("${hoje}T11:00", 25)
        assertEquals(5, sequencia(fases, zona, hoje))
    }

    @Test
    fun `um buraco corta a sequencia`() {
        val fases = listOf(
            fase("2026-11-26T10:00", 25),
            fase("2026-11-25T10:00", 25),
            fase("2026-11-23T10:00", 25), // falta o dia 24
            fase("2026-11-22T10:00", 25),
        )
        assertEquals(2, sequencia(fases, zona, hoje))
    }

    @Test
    fun `Foco incompleto e Pausa nao seguram a sequencia`() {
        val fases = listOf(
            fase("2026-11-26T10:00", 25),
            fase("2026-11-25T10:00", 20, concluida = false),
            fase("2026-11-25T11:00", 5, tipo = TipoFase.PAUSA_CURTA),
            fase("2026-11-24T10:00", 25),
        )
        assertEquals(1, sequencia(fases, zona, hoje))
    }

    @Test
    fun `sem Foco hoje a sequencia ainda vale ate ontem`() {
        val fases = listOf(fase("2026-11-25T10:00", 25), fase("2026-11-24T10:00", 25))
        assertEquals(2, sequencia(fases, zona, hoje))
    }

    @Test
    fun `sem Foco hoje nem ontem a sequencia e zero`() {
        assertEquals(0, sequencia(listOf(fase("2026-11-24T10:00", 25)), zona, hoje))
        assertEquals(0, sequencia(emptyList(), zona, hoje))
    }

    @Test
    fun `a sequencia usa os dias no fuso local`() {
        // Terça 23:30 em São Paulo já é quarta em UTC; no fuso local, segunda, terça e quarta seguem.
        val fases = listOf(
            fase("2026-11-23T10:00", 25, noFuso = zona),
            fase("2026-11-24T23:30", 25, noFuso = zona),
            fase("2026-11-25T10:00", 25, noFuso = zona),
            fase("2026-11-26T10:00", 25, noFuso = zona),
        )
        assertEquals(4, sequencia(fases, zona, hoje))
        assertEquals(2, sequencia(fases, ZoneId.of("UTC"), hoje)) // em UTC, a terça fica vazia
    }

    @Test
    fun `sequencia de 999 dias`() {
        val fases = (0L until 999L).map { fase("${hoje.minusDays(it)}T10:00", 25) }
        assertEquals(999, sequencia(fases, zona, hoje))
    }

    @Test
    fun `heatmap vai do domingo de 52 semanas atras ate hoje, com os minutos de Foco por dia`() {
        val fases = listOf(
            fase("2025-11-23T10:00", 25), // domingo de 52 semanas atrás: primeira célula
            fase("2025-11-22T10:00", 99), // fora
            fase("2026-11-26T10:00", 25),
            fase("2026-11-26T11:00", 10, concluida = false),
            fase("2026-11-26T12:00", 5, tipo = TipoFase.PAUSA_CURTA),
            fase("2026-11-23T23:59", 30), // segunda no fuso local
        )
        val mapa = heatmap(fases, zona, hoje)
        assertEquals(52 * 7 + 5, mapa.size) // 52 semanas inteiras + domingo a quinta
        assertEquals(25, mapa.first())
        assertEquals(35, mapa.last())
        assertEquals(30, mapa[52 * 7 + 1])
        assertEquals(25 + 35 + 30, mapa.sum())
    }

    @Test
    fun `heatmap sem registros fica todo vazio`() {
        assertEquals(List(52 * 7 + 5) { 0 }, heatmap(emptyList(), zona, hoje))
    }

    @Test
    fun `niveis do heatmap seguem a escala fixa`() {
        assertEquals(
            listOf(0, 1, 1, 2, 2, 3, 3, 4),
            listOf(0, 1, 25, 26, 60, 61, 120, 121).map(::nivelHeatmap),
        )
    }

    private val deVariosPerfis = listOf(
        fase("2026-11-26T09:00", 25, perfilId = 1, perfilNome = "Trabalho 25/5"),
        fase("2026-11-26T10:00", 50, perfilId = 2, perfilNome = "Estudo 50/10"),
        fase("2026-11-25T10:00", 50, perfilId = null, perfilNome = "Estudo 50/10"), // Perfil excluído, mesmo nome de um vivo
        fase("2026-11-24T10:00", 30, perfilId = null, perfilNome = "Leitura"),
        fase("2026-11-23T10:00", 30, perfilId = null, perfilNome = "Leitura"),
    )

    @Test
    fun `Todos nao filtra nada`() {
        assertEquals(deVariosPerfis, filtrar(deVariosPerfis, FiltroPerfil.Todos))
    }

    @Test
    fun `um Perfil existente filtra pelo id`() {
        assertEquals(listOf(50), filtrar(deVariosPerfis, FiltroPerfil.Existente(2)).map { it.minutosEfetivos })
    }

    @Test
    fun `um Perfil excluido filtra pelo nome copiado e nao pega o Perfil vivo de mesmo nome`() {
        val fases = filtrar(deVariosPerfis, FiltroPerfil.Excluido("Estudo 50/10"))
        assertEquals(listOf(null), fases.map { it.perfilId })
        assertEquals(2, sequencia(filtrar(deVariosPerfis, FiltroPerfil.Excluido("Leitura")), zona, LocalDate.of(2026, 11, 24)))
    }

    @Test
    fun `os Perfis excluidos aparecem uma vez cada, pelo nome`() {
        assertEquals(listOf("Estudo 50/10", "Leitura"), perfisExcluidos(deVariosPerfis))
    }
}

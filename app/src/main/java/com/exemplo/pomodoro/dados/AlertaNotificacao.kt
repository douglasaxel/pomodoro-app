package com.exemplo.pomodoro.dados

/** De onde vem o som do Alerta. */
enum class FonteSom { EMBUTIDO, SISTEMA, NENHUM }

/** Nome do som embutido padrão: o nome do arquivo em `res/raw` (veja `ferramentas/gerar_sons.py`). */
const val SOM_PADRAO = "sino"

/**
 * Grupo "Alerta e notificação" do Perfil (`@Embedded`). Kotlin puro, para os testes na JVM.
 * O som embutido e o toque do sistema ficam guardados os dois: trocar a fonte não perde a escolha anterior.
 */
data class AlertaNotificacao(
    val fonteSom: FonteSom = FonteSom.EMBUTIDO,
    val somEmbutido: String = SOM_PADRAO,
    /** URI do toque do sistema escolhido no seletor do Android. */
    val somSistema: String? = null,
    val vibrar: Boolean = true,
    /** O Alerta acende a tela bloqueada (full-screen intent). */
    val telaCheia: Boolean = false,
    val botaoPausarRetomar: Boolean = true,
    val botaoPular: Boolean = true,
    val botaoEstender: Boolean = true,
    /** Notificação de contagem no canal de importância mínima, sem ícone na barra de status. */
    val discreta: Boolean = false,
) {
    /** Os botões da Notificação de contagem, na ordem em que aparecem. */
    fun acoes(rodando: Boolean): List<AcaoNotificacao> = buildList {
        if (botaoPausarRetomar) add(if (rodando) AcaoNotificacao.PAUSAR else AcaoNotificacao.RETOMAR)
        if (botaoPular) add(AcaoNotificacao.PULAR)
        if (botaoEstender) add(AcaoNotificacao.ESTENDER)
    }
}

enum class AcaoNotificacao { PAUSAR, RETOMAR, PULAR, ESTENDER }

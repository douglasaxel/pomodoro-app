# 06: Gráficos do Histórico

**What to build:** Na tela Histórico, o usuário vê os minutos de Foco por dia, semana ou mês em barras. Ele vê os totais do período, a sequência de dias seguidos e um heatmap anual. Ele pode filtrar tudo por Perfil.

**Blocked by:** 04

**Status:** ready-for-agent

- [ ] A Agregação do Histórico é feita por funções Kotlin puras. Elas recebem os registros, o fuso horário e a data de referência.
- [ ] Os totais do período mostram os minutos de Foco, os Focos concluídos, os Focos incompletos e os minutos de Pausa.
- [ ] A sequência conta os dias seguidos com pelo menos um Foco concluído.
- [ ] O heatmap anual mostra os minutos de Foco por dia.
- [ ] O filtro por Perfil inclui os registros de Perfis excluídos, pelo nome copiado.
- [ ] A semana começa no domingo.
- [ ] Testes JVM: as bordas de dia e de semana no fuso local, a sequência com e sem buracos, o heatmap e o filtro por Perfil.
- [ ] A UI tem o seletor de período, as barras, os totais, a sequência e o heatmap, desenhados em Compose Canvas, sem biblioteca de gráficos.

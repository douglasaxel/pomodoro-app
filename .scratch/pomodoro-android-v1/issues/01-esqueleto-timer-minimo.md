# 01: Esqueleto + Timer mínimo

**What to build:** O usuário abre o app no emulador e vê a tela Timer, parada no Foco 1, sobre uma cor sólida. Ele inicia, pausa, retoma e Pula Fases, e vê um Ciclo inteiro acontecer: N Focos, Pausas curtas entre eles e uma Pausa longa no fim. O Perfil padrão (25/5/15, N = 4) fica fixo no código por enquanto. Nesta etapa, o timer só funciona com o app aberto.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Projeto Android em Kotlin + Jetpack Compose, `minSdk` 33 (ADR-0001), um módulo Gradle, com Gradle wrapper.
- [ ] O Motor do Ciclo é Kotlin puro, sem dependência de Android, e tem relógio injetado.
- [ ] O Motor aceita Iniciar, Pausar, Retomar, Pular e Reiniciar Ciclo.
- [ ] O tempo restante vem do instante de início e da soma das pausas, não de contagem de ticks.
- [ ] Testes JVM do Motor com relógio falso: a sequência do Ciclo com N variado, o tempo pausado que não conta, Pular e Reiniciar Ciclo.
- [ ] A tela Timer mostra o tempo restante, o tipo da Fase e a posição no Ciclo, por exemplo "Foco 2 de 4".
- [ ] O app abre com o timer parado.
- [ ] Os textos ficam em `strings.xml`, em pt-BR, e usam os termos do glossário.
- [ ] O app compila e roda no emulador `Pixel_9`.

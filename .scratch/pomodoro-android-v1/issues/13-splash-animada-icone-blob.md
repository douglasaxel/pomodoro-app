# 13: Splash animada e ícone do blob

**What to build:** Ao abrir o app a frio pelo ícone, o usuário vê o blob subir da borda de baixo até o centro da tela. O blob fica 2 s parado no centro, com a ondulação ativa. Depois ele avança em direção à tela, crescendo até cobri-la inteira, e um fade leva à tela Timer. O ícone do app é o blob.

**Blocked by:** 09

**Status:** ready-for-agent

- [ ] A splash aparece só na abertura a frio pelo launcher. Abrir pela Notificação de contagem ou pelo Alerta, ou voltar de segundo plano, vai direto ao Timer.
- [ ] A sequência tem três tempos: subida até o centro, 2 s parado com a ondulação ativa e crescimento até cobrir a tela, terminando num fade para o Timer. Não há como pular.
- [ ] O blob é o mesmo shader AGSL do Estilo visual Blob, com o fallback que já existe e o relógio `rememberTempoFundo`. A cor é a Cor de destaque do Foco do Perfil ativo.
- [ ] A transição para a splash do sistema (Android 12+, `core-splashscreen`) não tem salto. A splash do sistema mostra só a cor de fundo da splash animada, sem um segundo ícone piscando.
- [ ] O Timer já está composto por baixo durante o fade, sem tela vazia no fim.
- [ ] Movimento reduzido: sem subida nem crescimento. O blob aparece no centro com fade curto, fica 2 s parado sem ondulação, e um crossfade leva ao Timer.
- [ ] O ícone adaptativo é um blob em vetor, com a forma tirada da mesma fórmula do shader num instante fixo, sobre o fundo escuro do app. Tem a camada monochrome para os ícones temáticos do Android 13.
- [ ] A forma do ícone continua legível nas máscaras circular, quadrada e squircle, e em 48 dp.
- [ ] As curvas e durações vêm dos tokens do `DESIGN.md`, e a coreografia da splash entra no `DESIGN.md`.

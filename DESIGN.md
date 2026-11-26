# DESIGN.md: Pomodoro Android v1

Guia de interface para quem implementa os tickets em `.scratch/pomodoro-android-v1/issues/`. Os termos seguem o `GLOSSARY.md`. Os valores daqui são tokens: use-os como estão. Para mudar um valor, mude aqui primeiro.

Os princípios vêm das skills em `.claude/skills/` (escritas para web, React e Swift), traduzidos para Compose.

---

## 1. Direção visual

**Meta:** clean, moderno, orgânico, com movimento que encanta.

**A tensão entre elaborado e clean:** o movimento elaborado fica em dois lugares só. Um é o **ambiente**, que é lento e contínuo: o Fundo e a respiração do Estilo visual. O outro são os **momentos raros**: o fim de Fase, o começo de um Ciclo e a primeira entrada dos gráficos. O resto da interface é rápido, curto e quase invisível. A riqueza está no detalhe do shader, não na amplitude do movimento.

### Princípios

1. **O Fundo é o palco, o Estilo visual é o ator e o resto fica em silêncio.** Cada tela tem um único elemento principal. No Timer, ele é o Estilo visual com os números. Os controles e a navegação nunca disputam atenção com ele.
2. **Dois relógios que não se misturam.** O tempo do ambiente se mede em segundos: ondas, respiração, ciclos de 7 a 90 s. O tempo da interação fica em até 300 ms e só existe em resposta a um toque. Os controles nunca respiram, e o ambiente nunca dá trancos.
3. **O orçamento de encanto mora no que é raro.** Quanto mais vezes por dia algo acontece, mais curto e discreto é o seu movimento (tabela abaixo).
4. **Material com propósito.** O vidro só aparece sobre o Fundo vivo e nunca sobre outro vidro. A cor fica em camadas sólidas (o Estilo visual e o botão primário), não no vidro.
5. **A calma vem antes do espetáculo.** O app serve para concentrar. Durante um Foco, nada na visão periférica se move rápido o bastante para puxar o olho.

### Orçamento de movimento

| Frequência | Exemplos | Movimento permitido |
| --- | --- | --- |
| Contínuo, periférico | Fundo, respiração do Estilo visual, parallax | Lento (períodos ≥ 7 s), amplitude baixa, sem loops rápidos |
| Raro (≤ ~20 por dia) | Fim de Fase, novo Ciclo, primeira entrada dos gráficos, sequência que subiu | Coreografia de até ~1,2 s, com spring vivo e leve overshoot |
| Ocasional | Iniciar, Estender, Trocar Perfil, abrir a Configuração do Perfil | Até 400 ms, sem bounce, exceto no Estilo visual |
| Frequente | Pausar/Retomar, navegação, chaves, abas, linhas de lista | Até 250 ms, só fade ou escala pequena, sem stagger |
| Contínuo, funcional | Números do timer, totais, lista | **Nenhum** (ver §10) |

---

## 2. Tipografia

### Fonte

**Recomendada: Google Sans Flex** (variável, licença OFL, no Google Fonts). Por quê:
- **Eixo ROND (0 a 100):** arredonda os terminais. É o toque "orgânico" sem cair em fonte decorativa.
- **Eixo `opsz`:** desenha os números gigantes do timer com o tamanho óptico certo.
- **Família do Material 3 Expressive:** parece nativa no Android.

**Exige baixar o arquivo.** O arquivo `.ttf` variável entra em `res/font/`. Ninguém baixou ainda. O usuário precisa aprovar.

**Antes de adotar, confirme no arquivo que existe a feature `tnum`** (com fonttools ou wakamaifondue.com). Uma fonte comparativa diz que existe, mas não achei confirmação oficial. Sem `tnum`, os números tremem a cada segundo, e isso inviabiliza a fonte.

**Alternativa de sistema:** `FontFamily.Default` (Roboto) com `fontFeatureSettings = "tnum"`, sem download. Ela perde o ROND e mantém todo o resto deste guia.

### Escala

Todos os números (timer, totais, eixos, minutos da lista) usam `fontFeatureSettings = "tnum"`.

| Papel | Tamanho | Peso | Tracking | Entrelinha | ROND | Uso |
| --- | --- | --- | --- | --- | --- | --- |
| `timer` | 88 sp (ver regra de ajuste) | 300 | −0.03 em | 1.0 | 100 | Tempo restante |
| `display` | 40 sp | 500 | −0.02 em | 44 sp | 60 | Sequência, número em destaque |
| `title` | 22 sp | 600 | −0.01 em | 28 sp | 30 | Título de tela, nome do Perfil no cabeçalho |
| `body` | 16 sp | 400 | 0 | 24 sp | 0 | Texto corrido, linhas de lista |
| `label` | 14 sp | 600 | +0.01 em | 20 sp | 30 | Botões, rótulo de Fase, abas |
| `caption` | 12 sp | 500 | +0.02 em | 16 sp | 0 | Eixos, metadados, horários |

- **Texto sobre vidro:** +100 de peso e +0.01 em de tracking em relação à tabela. Fica legível sobre um fundo que muda.
- **Ajuste do `timer`:** use o menor valor entre 88 sp e o tamanho em que a string mais larga do formato cabe em 84% da largura útil. Meça com `TextMeasurer`. Calcule no começo da Fase, nunca a cada segundo.
- **Formato do tempo:**
  - Fase de até 59 min: `MM:SS` com zero à esquerda (`01:00`, `25:00`). A largura fica constante.
  - Fase de 60 min ou mais: `H:MM:SS` do começo ao fim (`3:00:00` … `0:59:59`), sem trocar de formato no meio.
  - Se um Estender levar a duração a 60 min ou mais, o formato troca nesse instante, com o crossfade do Estender.
- **Dois-pontos:** confira se ficam no centro óptico dos algarismos. Se ficarem baixos, desloque-os 0.04 em para cima.
- **Escala de fonte do sistema:** todos os papéis seguem o `fontScale`, menos o `timer`, que já é enorme e só obedece à regra de ajuste. Teste a 200%.

---

## 3. Cor e materiais

### Esquema

O app usa um único esquema: **texto claro sobre Fundo escurecido pelo Véu**, independente do claro/escuro do sistema. O Véu sempre escurece, então o texto é sempre claro. (Confirmado pelo usuário.)

| Token | Valor | Uso |
| --- | --- | --- |
| `textoPrimario` | `#FFFFFF` a 96% | Números, títulos |
| `textoSecundario` | `#FFFFFF` a 72% | Rótulos, metadados |
| `textoTerciario` | `#FFFFFF` a 48% | Desabilitado, eixos |
| `baseEscura` | `#0B0B10` | Cor da splash e do Véu, fallback do Fundo |
| `vidroEscuro` | `#000000` a 24% + blur 28 dp | Cartões estruturais (o cartão do Estilo visual) |
| `vidroClaro` | `#FFFFFF` a 10% + blur 16 dp | Elementos interativos sobre o Fundo (chip de Perfil, botões secundários) |
| `vidroBarra` | `#000000` a 40% + blur 24 dp | Barra de navegação |
| `bordaVidro` | gradiente vertical de `#FFFFFF` a 22% (topo) até `#FFFFFF` a 6% (base), 1 dp | Toda superfície de vidro. É a luz batendo na borda |

### Cor de destaque

- **Valores padrão:** Foco `#FF8A65` (coral), Pausa curta `#6FD8C0` (menta), Pausa longa `#A99BFF` (lavanda).
- **Onde aparece:** no Estilo visual, no preenchimento do botão primário, nas barras e no heatmap do Histórico (sempre a cor de Foco), no indicador da navegação e nos indicadores de foco. Nunca aparece como tinta do vidro.
- **Normalização** (vale também para a extração do Fundo, ticket 09):
  - Converta para Oklab.
  - Prenda o L entre 0.70 e 0.82 e limite o croma a 0.17, para não virar neon.
  - Se a diferença de L para o Fundo já escurecido ficar abaixo de 0.15, suba o L até chegar a 0.15.
  - Na extração automática, Foco usa o swatch *vibrant*. Pausa curta e Pausa longa usam *light vibrant* e *muted*, nessa ordem, com o croma levado à faixa acima.
- **Texto sobre a Cor de destaque** (rótulo do botão primário): branco ou `#101014`, o que der mais contraste. O mínimo é 4.5:1.
- **Esquema do Material 3:** o `primary` é a Cor de destaque do tipo de Fase atual. Fora do Timer, é a de Foco do Perfil ativo. As superfícies são transparentes, e o Fundo aparece por trás.

### Camadas, de baixo para cima

1. **Fundo:** shader, foto ou cor. Desenhado com overscan de 1.06× (o parallax precisa de margem).
2. **Véu:** tela cheia, escurece (preto com alfa) e desfoca (blur nativo de 0 a 24 dp, proporcional ao alfa).
3. **Vidro:** no máximo um cartão estrutural por tela, além dos chips e da barra de navegação, que são irmãos e nunca se empilham. O vidro é o Fundo redesenhado sob a forma do cartão com `RenderEffect` de blur, mais a tinta e a borda.
4. **Estilo visual:** camada sólida, com a Cor de destaque.
5. **Texto e controles.**

### O Estilo visual não pode apagar os números

A forma é luz na borda e tem o miolo profundo. No blob e no anel, a Cor de destaque fica com L 0.80 na borda (mais um halo externo suave) e cai até L 0.42 no centro. Os números brancos ficam sobre o miolo escuro. Quando o blob encolhe abaixo da largura dos números, eles passam para o Fundo já escurecido. Os dois lados são escuros, e o contraste se mantém.

### Profundidade e sombra

- **Nunca use `Modifier.shadow` nem elevation em superfície translúcida.** A sombra do Android aparece através do vidro e suja a superfície.
- A profundidade vem de três coisas:
  1. O blur. Superfície maior pede blur maior: o cartão usa 28 dp, o chip usa 16 dp.
  2. A `bordaVidro`.
  3. Um halo escuro desenhado *fora* da forma: gradiente radial de preto a 24% até 0%, com 32 dp de alcance.
- **Cantos:** cartão 32 dp. Chips e campos em pílula. Botões circulares.

### Contraste mínimo

| O quê | Mínimo |
| --- | --- |
| Números do timer (texto grande) | 3:1 |
| Qualquer outro texto | 4.5:1 |
| Ícones e bordas de controle | 3:1 |

- **Como medir:** contra o **percentil 90** da luminância do Fundo na área do texto, já com o Véu. A média esconde pontos claros.
- **Fórmula do Véu automático:**
  - Para texto branco, a luminância relativa do fundo precisa ficar em `Lfundo ≤ 0.183` (4.5:1) ou em `≤ 0.30` (3:1). O `textoSecundario` (branco a 72%) mistura o Fundo e pede `Lfundo ≤ 0.102` para 4.5:1. É esse o limite do Véu automático.
  - Com um Véu preto de alfa `a`, o valor sRGB codificado do fundo vira `(1−a) · srgb(L90)`.
  - Logo, `a_min = 1 − srgb(Llimite) / srgb(L90)`. Numa foto branca, isso dá `a_min ≈ 0.65`.
- **Ajuste do Perfil:** o ajuste soma em cima do mínimo e nunca desce abaixo dele. O controle mostra "Automático" no ponto zero e vai até +0.35.
- **Textos pequenos sobre o Fundo** (rótulo de Fase, "Pausado") usam o limite de 4.5:1 na área deles.

---

## 4. Tokens de movimento (Compose)

### Curvas

| Token | Valor | Quando |
| --- | --- | --- |
| `EaseOut` | `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` | Entradas, saídas e respostas a toque. É o padrão |
| `EaseInOut` | `CubicBezierEasing(0.77f, 0f, 0.175f, 1f)` | Algo que já está na tela e se move de A para B |
| `EaseMorph` | `CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)` | Crossfades longos de superfícies grandes (troca de Fundo) |
| `EaseDrawer` | `CubicBezierEasing(0.32f, 0.72f, 0f, 1f)` | Só para painel de borda, se algum for feito à mão |
| `LinearEasing` | linear | Progresso visual de algo que mede tempo |

Nunca use ease-in. Não use os padrões do Compose para estes papéis: o fade padrão do `NavHost` dura 700 ms.

### Durações (`tween`)

| Token | ms | Uso |
| --- | --- | --- |
| `DurSaida` | 90 | Saída do conteúdo na navegação. A saída é sempre mais curta que a entrada |
| `DurCurta` | 150 | Fades de troca frequente: navegação, ícone do botão, rótulo |
| `DurMedia` | 250 | Troca de conteúdo, mudança de cor na Configuração do Perfil |
| `DurLonga` | 400 | Superfícies grandes: Fundo na prévia, entrada da tela |
| `DurFundo` | 500 | Crossfade de Fundo no Timer (Trocar Perfil) |

### Springs (`spring(dampingRatio, stiffness)`)

Valores com massa 1. A coluna "~resposta" é o tempo perceptual, calculado por `stiffness = (2π / resposta)²`.

| Token | dampingRatio | stiffness | ~resposta | Uso |
| --- | --- | --- | --- | --- |
| `SpringPress` | 1f | 1500f | 0.16 s | Escala de toque |
| `SpringUi` | 1f | 440f | 0.3 s | Padrão de UI: tamanho de chip, progresso no Estender, alturas no Histórico |
| `SpringLayout` | 1f | 250f | 0.4 s | Elemento compartilhado, entrada do Estilo visual |
| `SpringLancamento` | 0.8f | 440f | 0.3 s | Fim de gesto com velocidade (swipe para apagar que volta) |
| `SpringVivo` | 0.7f | 120f | 0.57 s | Só o Estilo visual: o inspiro do Iniciar e o florescer no fim de Fase |
| `SpringFase` | 1f | 40f | ~1.0 s | Forma e cor do Estilo visual na troca de Fase |
| `SpringPular` | 1f | 160f | 0.5 s | A mesma troca, quando o usuário pede (Pular) |
| `SpringAmbiente` | 1f | 50f | ~0.9 s | Amplitude da respiração, parallax, velocidade do Fundo |

- **Bounce** (dampingRatio < 1) só no Estilo visual (que é "vivo") e no fim de um gesto. Um botão tocado não recebe bounce, porque nenhuma força foi aplicada.
- **Ambiente:** o tempo do shader é um uniform alimentado por `withFrameNanos` só enquanto a tela está visível. A respiração é `sin(2πt/T)` dentro do shader. Ela não é uma animação do Compose.

### Regra de interrupção

1. **Toda mudança de estado anima a partir do valor e da velocidade atuais.** Use `animate*AsState` ou `Animatable.animateTo`. Nunca use `snapTo` seguido de `animateTo`. A exceção é o movimento reduzido.
2. **Uma única fonte por objeto.** A troca de Fase é um `Animatable` chamado `faseMix` (0→1). Ele move a cor, os parâmetros da forma e o halo do Estilo visual. Duas animações separadas se desencontram quando há interrupção.
3. **Um evento novo cancela a coreografia em curso** (o job da coroutine) e começa a partir dos valores atuais. Um Pular duplo rápido redireciona a animação e nunca enfileira.
4. **Nada bloqueia o toque.** Todo controle visível funciona no primeiro quadro, mesmo no meio de uma animação.
5. **Nada de keyframes** (`keyframes {}`, `repeatable`) em algo que o usuário pode disparar duas vezes por segundo.
6. **O progresso nunca "corre atrás" do tempo real.** Ao abrir o app, restaurar o estado ou voltar do segundo plano, o Estilo visual aparece direto no progresso verdadeiro. A única exceção é o Estender (§5).

---

## 5. Coreografia

**Papéis:** **Líder** (o que o usuário precisa notar), **Apoio** (vem atrás, com movimento menor), **Decoração** (só fade) e **Parado**. A coluna "Início" é o atraso a partir do evento.

### 5.1 Abrir o app

- **Abertura a frio:** a splash do sistema usa fundo `baseEscura` e ícone estático. Na saída, o ícone faz fade em 150 ms.
- **Volta do segundo plano:** **sem entrada**. O estado já está na tela.

| Elemento | Papel | Início | Duração | Movimento | Relação |
| --- | --- | --- | --- | --- | --- |
| Fundo + Véu + chrome (chip, controles, navegação) | Apoio | 0 | `DurLonga` `EaseOut` | Só fade, a partir de `baseEscura` | Juntos: um container, uma entrada |
| Estilo visual | Líder | 80 ms | `SpringLayout` | Escala 0.92→1 + alfa 0→1, já no progresso real | Sobrepõe o Fundo |
| Rótulo + números | Apoio | 120 ms | `DurMedia` `EaseOut` | Fade | Junto com o Líder (uma batida) |

Termina em cerca de 500 ms, e o toque funciona desde o primeiro quadro.

### 5.2 Iniciar

| Elemento | Papel | Início | Duração | Movimento |
| --- | --- | --- | --- | --- |
| Estilo visual | Líder | 0 | `SpringVivo` | Um único inspiro: escala 1→1.03→1. A amplitude da respiração sobe do nível de parado ao de rodando com `SpringAmbiente` |
| Ícone do botão primário | Apoio | 0 | `DurCurta` | ▶→⏸: crossfade com escala 0.8→1 |
| Estender e Pular | Apoio | 40 ms | `SpringUi` | Saem de trás do botão primário: translateX ∓16 dp→0 + alfa |
| Números, rótulo, Fundo | Parado | — | — | Os números só começam a contar |

Háptico no quadro 0. No primeiro Iniciar, o pedido de `POST_NOTIFICATIONS` aparece por cima e não segura a animação.

### 5.3 Pausar e Retomar

| Elemento | Papel | Início | Duração | Movimento |
| --- | --- | --- | --- | --- |
| Ícone do botão primário | Líder | 0 | `DurCurta` | ⏸↔▶, crossfade com escala 0.8→1 |
| Estilo visual | Apoio | 0 | `SpringAmbiente` (respiração), `DurMedia` (saturação) | Pausar: a respiração cai para o nível de pausado e a saturação cai 30%. Retomar faz o caminho inverso |
| Números | Apoio | 0 | `DurMedia` | Alfa 96%→72% (pausado) |
| Texto "Pausado" | Apoio | 0 | `DurCurta` | Fade no lugar, sem deslocamento. O estado sempre aparece como texto, além da cor |
| Fundo, Estender, Pular | Parado | — | — | — |

Toques rápidos alternados redirecionam a animação a partir do valor atual.

### 5.4 Pular

É a troca de Fase do 5.6, comprimida: `SpringPular` no lugar de `SpringFase` e sem florescer com overshoot (o tamanho usa `SpringUi`).

| Elemento | Papel | Início | Duração | Movimento |
| --- | --- | --- | --- | --- |
| Estilo visual (`faseMix`) | Líder | 0 | `SpringPular` | Cor e forma da próxima Fase. O tamanho volta ao máximo |
| Números e rótulo antigos | Apoio | 0 | `DurSaida` | Sobem 8 dp + fade |
| Números e rótulo novos | Apoio | 60 ms | `DurMedia` `EaseOut` | Sobem de +8 dp até 0 + fade. Para cima significa "adiante" |
| Estender e Pular | Apoio | 0 | `SpringUi` | Se a próxima Fase começa parada, voltam para trás do botão primário (o caminho inverso do 5.2) |

### 5.5 Estender

| Elemento | Papel | Início | Duração | Movimento |
| --- | --- | --- | --- | --- |
| Progresso do Estilo visual | Líder | 0 | `SpringUi` | O blob cresce de volta e o anel recua, a partir do valor exibido. Depois volta a seguir o tempo |
| Dígitos que mudaram | Apoio | 0 | `DurMedia` `EaseOut` | Number ticker: só os dígitos alterados rolam 0.3 em para cima. É uma mudança real de valor causada pelo usuário |
| Selo "+5 min" | Decoração | 0 | 700 ms `EaseOut` | Nasce sobre o botão Estender, sobe 12 dp e some. O texto usa os minutos do Perfil |
| Fundo, rótulo | Parado | — | — | — |

### 5.6 Fim de Fase (Foco ↔ Pausa)

**É o momento principal do app.** Com o app aberto, a sequência abaixo toca. Se a Fase terminou em segundo plano, o app abre direto no estado novo, sem replay.

| Elemento | Papel | Início | Duração | Movimento | Relação |
| --- | --- | --- | --- | --- | --- |
| Cor e forma (`faseMix`) | Líder | 0 | `SpringFase` (~1 s) | A cor interpola em Oklab (`animateColorAsState` já faz isso). Os parâmetros da forma vão aos da nova Fase (§7) | Juntas: um objeto |
| Tamanho (florescer) | Líder | 120 ms | `SpringVivo` | O blob, que estava no tamanho mínimo, se reabre até o máximo com ~4% de overshoot | Sobrepõe: a cor muda primeiro e a forma floresce em seguida (causa → efeito) |
| Números e rótulo antigos | Apoio | 0 | 200 ms `EaseOut` | Fade + blur de 0 a 6 dp | — |
| Números e rótulo novos | Apoio | 180 ms | 300 ms `EaseOut` | Fade + blur de 6 dp a 0 | Crossfade com o blur cobrindo a emenda |
| Onda de Ciclo novo | Decoração | 200 ms | 900 ms `EaseOut` | **Só de Pausa longa para Foco 1:** um anel de luz sai do Estilo visual, escala 1→1.4 e alfa 0.25→0, uma vez | Junto com o florescer |
| Botão primário e laterais | Apoio | 0 | `SpringUi` | Com início automático desligado, as laterais recolhem e o ícone vira ▶. Com início automático ligado, nada muda | — |
| Fundo, Véu, chip, navegação | Parado | — | — | — | — |

No Anel fluido, o anel se fecha, o brilho da ponta sobe 40% por 300 ms, o anel se desfaz em partículas e se refaz vazio na cor nova (§7).

### 5.7 Trocar Perfil

- **Toque:** o toque num Perfil do seletor (bottom sheet) confirma a troca.
- **Legenda no topo do sheet:** se uma Fase está rodando ou pausada, o sheet mostra no topo "A Fase atual será encerrada e registrada". A troca não pede um passo extra.

| Elemento | Papel | Início | Duração | Movimento | Relação |
| --- | --- | --- | --- | --- | --- |
| Sheet | Apoio | 0 | padrão do M3 | Fecha | O Timer começa a mudar junto, sem esperar |
| Fundo | Líder | 0 | `DurFundo` `EaseMorph` | Crossfade (evita salto brusco de brilho) | — |
| Véu | Apoio | 0 | `DurFundo` | Alfa e blur vão ao novo cálculo automático | Junto com o Fundo |
| Estilo visual, mesmo Estilo | Apoio | 0 | `SpringFase` | Morph de cor e forma, como no 5.6 | Junto |
| Estilo visual, outro Estilo | Apoio | 0 / 200 ms | 200 ms + 300 ms `EaseOut` | O antigo sai (escala 1→0.96, fade, blur 8 dp) e o novo entra (escala 0.96→1). É sequência, porque dois visuais ricos no mesmo lugar brigam | Sequência curta |
| Números e rótulo | Apoio | 0 | `DurMedia` | Crossfade para Foco 1, parado | Junto |
| Chip de Perfil | Decoração | 0 | `DurCurta` + `SpringUi` | Crossfade do texto, largura com `animateContentSize` | — |

### 5.8 Abrir e fechar a Configuração do Perfil

- **Vindo da lista de Perfis:** elemento compartilhado. A miniatura da linha vira a prévia ao vivo (`SharedTransitionLayout` + `sharedBounds`, `SpringLayout`).
  - O nome do Perfil faz o mesmo e vira o título.
  - O resto da tela faz fade em `DurMedia` a partir de 100 ms. As seções não têm stagger: é uma entrada por container.
- **Vindo do seletor do Timer** ("Editar Perfil"): eixo X compartilhado.
  - Entrada: translateX 24 dp→0 + fade (`DurMedia` `EaseOut`).
  - A tela de baixo sai: translateX 0→−24 dp + fade (`DurSaida`).
- **Fechar:** é o caminho inverso, pelo mesmo eixo.
  - **Voltar preditivo:** a tela acompanha o gesto 1:1 (escala até 0.9 e cantos até 32 dp).
  - Se o gesto se confirma, a saída continua. Se é cancelado, a tela volta com `SpringUi`.

### 5.9 Prévia ao vivo

A prévia mostra Fundo, Véu e Estilo visual num cartão de 176 dp fixo no topo, com progresso fixo em 0.6.

| Mudança | Movimento |
| --- | --- |
| Cor de destaque | `animateColorAsState`, `DurMedia` `EaseOut` |
| Escolher outra Fase para ver (segmentado "Foco / Pausa curta / Pausa longa" sob a prévia, ou tocar na cor de uma Fase) | `faseMix` com `SpringPular` |
| Estilo visual | O mesmo crossfade com blur e escala de 0.96 do 5.7, em `DurMedia` |
| Fundo | Crossfade em `DurLonga` `EaseMorph` |
| Véu (controle deslizante) | **1:1, sem animação.** É manipulação direta |
| Movimento do Fundo desligado | A velocidade do tempo do shader cai até 0 com `SpringAmbiente`: desacelera, não congela de repente |

### 5.10 Navegação entre Timer, Histórico e Perfis

- **Conteúdo:** fade-through, sem deslizar. Os destinos são irmãos e não têm direção.
  - Entra com `fadeIn` em `DurCurta` `EaseOut`. Sai com `fadeOut` em `DurSaida`. Os dois ao mesmo tempo.
- **Fundo:** fica parado na raiz, atrás do `NavHost`, e nunca sai da tela.
  - Fora do Timer, o tempo do shader desacelera até 0 (`SpringAmbiente`).
  - O Véu sobe para alfa 0.78 e blur 40 dp em `DurMedia`, a intensidade de leitura das listas. Volta ao valor do Perfil ao retornar.
- **Indicador da barra:** o padrão do M3.
- **Tocar de novo na aba ativa:** rola a tela para o topo.

### 5.11 Entrada dos gráficos do Histórico

- **Primeira visita por processo:** as barras crescem a partir da linha de base (scaleY 0→1, origem na base).
  - 300 ms `EaseOut` com stagger decrescente: 30, 25, 20, 15 ms…
  - A partir da 6ª barra, todas usam o atraso da 6ª. O total fica em até cerca de 400 ms.
  - Crescer a partir de 0 vale aqui porque 0 é o valor real da base.
- **Visitas seguintes:** o gráfico simplesmente está lá, sem animação.
- **Mudar o filtro por Perfil** (mesmo número de barras): as alturas vão do valor antigo ao novo com `SpringUi`. É uma mudança real de valor.
- **Mudar o período** (o número de barras muda): crossfade do gráfico inteiro em `DurMedia`, sem crescer.
- **Sequência:** se subiu desde a última vez que foi vista (guarde o último valor visto), o número conta do antigo até o novo em 600 ms `EaseOut`, e o ícone ganha um brilho que some em 900 ms. Acontece uma vez. Nos outros casos, nada se move.
- **Heatmap e totais:** aparecem com a tela. Nenhuma célula anima.

---

## 6. Toque e feedback

### Estado pressionado

- **Resposta no toque para baixo, nunca na soltura.**
- **Botões do Timer:** sem ripple, que fica errado sobre o vidro.
  - Primário: escala 0.96, e o preenchimento clareia 8%.
  - Secundários e chip: escala 0.95, e a tinta do vidro sobe de 10% para 18%.
  - Tudo com `SpringPress`, na descida e na subida.
- **Linhas de lista** (Histórico, Perfis) e controles do M3: ripple padrão, que é o comportamento conhecido no Android.
- **Alvos de toque:** pelo menos 48 dp, com 8 dp entre alvos vizinhos.
  - O primário mede 88 dp. Os secundários medem 56 dp.
  - Pular fica a pelo menos 24 dp do primário, para evitar o Pular acidental.

### Hápticos (`HapticFeedbackConstants` via `view.performHapticFeedback`)

O háptico dispara no mesmo quadro da mudança visual: no handler do toque, antes da animação. Nunca use `FLAG_IGNORE_GLOBAL_SETTING`. A configuração de toque do sistema manda.

| Momento | Constante | API | Alternativa abaixo de 34 |
| --- | --- | --- | --- |
| Iniciar | `CONFIRM` | 30 | — |
| Pausar / Retomar | `TOGGLE_OFF` / `TOGGLE_ON` | 34 | `CONTEXT_CLICK` |
| Pular | `CONTEXT_CLICK` | 23 | — |
| Estender | `CLOCK_TICK` | 21 | — |
| Trocar Perfil (toque no sheet) | `CONFIRM` | 30 | — |
| Passo do controle de duração e de N | `CLOCK_TICK` | 21 | — |
| Controle de duração chega ao limite (1 ou 180 min) | `REJECT`, uma vez | 30 | — |
| Chaves (Switch) | `TOGGLE_ON` / `TOGGLE_OFF` | 34 | `CONTEXT_CLICK` |
| Segmentado (período, Fase da prévia) | `SEGMENT_TICK` | 34 | `CLOCK_TICK` |
| Swipe para apagar cruza o limiar / volta | `GESTURE_THRESHOLD_ACTIVATE` / `_DEACTIVATE` | 34 | `CONTEXT_CLICK` / nada |
| Toque longo numa linha de Perfil | `LONG_PRESS` | 3 | — |
| **Fim de Fase com o app aberto** | **nenhum** | — | A vibração do Alerta, se ligada no Perfil, é a única. Não duplique |

### Gestos

| Gesto | Decisão | Por quê |
| --- | --- | --- |
| Arrastar para Estender | **Não** | Fica escondido, pode disparar sem querer e o Motor não tem desfazer. Um toque no botão resolve |
| Swipe no Timer para trocar Perfil | **Não** | Trocar Perfil encerra a Fase. Precisa ser deliberado |
| Tocar no Estilo visual para pausar | **Não** | Toque acidental com o telefone na mesa |
| Voltar preditivo (Configuração do Perfil, sheets) | **Sim** | É nativo e acompanha o dedo 1:1. Ligue `android:enableOnBackInvokedCallback="true"` |
| Swipe para apagar uma Fase no Histórico | **Sim** | Use `SwipeToDismissBox`, só da direita para a esquerda, com `positionalThreshold` em 40% da largura e o limiar de velocidade padrão. Apaga com um Snackbar "Fase apagada · Desfazer" (4 s). Os vizinhos fecham o buraco *enquanto* a linha sai (`animateItem`, `SpringUi`). Também há a opção "Apagar" no menu da linha, para acessibilidade |
| Puxar o sheet do seletor de Perfil para fechar | **Sim** | Já vem no M3 |

---

## 7. Estilos visuais

Todos recebem o mesmo contrato: progresso (0–1), tipo da Fase, Cor de destaque e estado (rodando ou pausado). Os parâmetros por tipo de Fase são interpolados pelo `faseMix`.

**Respiração** (períodos e amplitudes de escala; o ruído é a deformação da borda):

| | Foco | Pausa curta | Pausa longa | Pausado |
| --- | --- | --- | --- | --- |
| Período | 7 s | 9 s | 11 s | 14 s |
| Amplitude de escala | 1.5% | 3% | 4% | 0.5% |
| Amplitude do ruído da borda (fração do raio) | 0.04 | 0.08 | 0.11 | a da Fase × 0.3 |
| Frequência do ruído | 2.0 | 1.5 | 1.2 | igual |
| Velocidade do ruído | 1.0 | 0.7 | 0.5 | 0.15 |

O Foco é compacto, firme e de respiração curta. As Pausas são soltas e lentas, como uma expiração.

### Blob com vidro / Blob puro

- **Forma:** o shader do blob, com luz na borda e miolo profundo (§3). No Blob com vidro, ele fica sobre um cartão `vidroEscuro` quadrado de cantos 32 dp e 0.84 × a largura. No Blob puro, fica direto sobre o Véu.
- **Progresso:** o blob encolhe com o tempo restante, e é a **área** que cai de forma linear: `r = rMin + (rMax − rMin) · √(restante)`, com `rMin = 0.35 · rMax`. Nunca chega a zero.
- **Pausado:** a respiração e o ruído caem para a coluna "Pausado" e a saturação cai 30%. No Blob com vidro, o blur do cartão sobe de 28 para 36 dp, como se o vidro embaçasse.
- **Fim de Fase:** o florescer do 5.6.

### Anel fluido

- **Forma:** um anel de 10 dp de espessura com borda líquida (ruído na borda, com a amplitude da tabela × 0.5). O diâmetro é 0.78 × a largura, e os números ficam no centro, sobre o Véu.
- **Progresso:** o arco **se fecha** com o tempo decorrido. Começa no topo e corre no sentido horário. O traço é a Cor de destaque. O trilho vazio é `#FFFFFF` a 8%.
- **Ponta:** um brilho radial de 24 dp na Cor de destaque (L 0.85) que segue a ponta.
  - Partículas: até 24 vivas, cada uma com 2,4 s de vida. Saem da ponta e derivam para fora a no máximo 6 dp/s, enquanto o alfa cai até 0.
- **Respiração:** a espessura respira ±8% com o período da tabela. O raio não muda.
- **Pausado:**
  - A emissão de partículas para, e as que existem somem em 600 ms.
  - O brilho da ponta cai para 40%.
  - O ruído da borda desacelera para a coluna "Pausado".
- **Fim de Fase:** o arco completa a volta, a ponta brilha +40% por 300 ms e o anel se dissolve em ~40 partículas (500 ms). Ele renasce vazio na nova cor com `SpringFase`.

### Vidro com parallax

- **Forma:** um cartão `vidroEscuro` (0.84 × a largura, proporção 4:5) com os números grandes. Este é o único Estilo visual em que o número é a forma.
- **Progresso:** um traço de 2 dp na Cor de destaque ao longo do perímetro do cartão. Começa no topo central e corre no sentido horário. O traço **esvazia** com o tempo restante. O trilho é a `bordaVidro`.
- **Parallax:**
  - Só o Fundo se desloca, até ±12 dp, no sentido contrário à inclinação. O cartão fica parado.
  - Sensor `TYPE_GAME_ROTATION_VECTOR` com `SENSOR_DELAY_GAME`, suavizado por `SpringAmbiente`.
  - A posição neutra acompanha a postura atual em ~3 s, então segurar o telefone inclinado não deixa o Fundo preso na borda.
  - O sensor é desligado fora do Timer.
- **Respiração:** **nenhuma.** O vidro é estável, e o movimento vem da mão do usuário.
- **Pausado:** o blur sobe de 28 para 36 dp, os números caem para 72% e o traço de progresso para 50% de alfa. O parallax continua, porque é resposta direta à mão.
- **Fim de Fase:** o traço volta a encher (`SpringFase`), e a cor e os números seguem o 5.6.

### Fundo (Biblioteca e Gradiente vivo)

- **Velocidade:** períodos de 40 a 90 s por ciclo de movimento. Nenhum ponto da arte se desloca mais que 2% da altura da tela por segundo.
- **Movimento do Fundo desligado:** o tempo do shader desacelera até 0 (§5.9) e fica parado.
- **Miniaturas:** só as que estão visíveis na tela animam.

---

## 8. Layout

Grade de 4 dp, margens laterais de 20 dp e largura máxima de 560 dp (centralizada em telas largas). O app é edge-to-edge: o Fundo passa por trás das barras do sistema, e os ícones da barra de status são claros.

### Timer (retrato, 411 × 914 dp)

```
┌──────────────────────────────────────┐
│ ░ barra de status (Fundo por trás) ░ │
│  ( Trabalho 25/5  ▾ )          ( ⋯ ) │  chip vidroClaro · menu: Reiniciar Ciclo
│                                      │
│            Foco · 2 de 4             │  label, textoSecundario
│                                      │
│        ╭──────────────────╮          │
│        │                  │          │  área do Estilo visual: 0.84 × largura
│        │      24:59       │          │  timer 88 sp, centrado
│        │                  │          │
│        ╰──────────────────╯          │
│               Pausado                │  só quando pausado
│                                      │
│      ( +5 )    (  ▶  )    ( ⏭ )      │  56 · 88 · 56 dp. Laterais só com a Fase rodando ou pausada
│                                      │
│ ▓▓▓ Timer ▓▓▓  Histórico    Perfis   │  vidroBarra
└──────────────────────────────────────┘
```

- **Até o ticket 09:** a área do Estilo visual fica vazia, com os números sobre a cor sólida. Os tokens já valem.
- **Paisagem:** duas colunas. O Estilo visual fica à esquerda, ocupando a altura toda. Rótulo, números e controles ficam à direita.
- **Descrições para leitor de tela:**
  - Botão primário: "Iniciar", "Pausar" ou "Retomar", conforme o estado.
  - Pular: "Pular Fase".
  - Estender: "Estender 5 minutos" (os minutos do Perfil).
  - Estilo visual: uma descrição única, "Foco 2 de 4, faltam 24 minutos", atualizada a cada minuto, não a cada segundo.

**Piores casos:**

| Caso | Comportamento |
| --- | --- |
| Nome de Perfil longo ("Leitura técnica: arquitetura de software") | Chip em uma linha, reticências no fim, no máximo 70% da largura. O nome inteiro aparece no sheet do seletor (quebra em até 2 linhas) |
| Fase de 1 min | `01:00`. A troca de Fase (~1 s) ainda cabe |
| Fase de 180 min | `3:00:00`. O ajuste encolhe o `timer` (~68 sp em 411 dp) e mantém o tamanho a Fase inteira |
| N = 12 | "Foco 12 de 12" cabe sem ajuste |
| Fonte a 200% | O rótulo quebra em 2 linhas. Os controles ficam ancorados no rodapé, e o Estilo visual encolhe para sobrar espaço, sem sobreposição |
| Tela de 360 dp | A regra de ajuste do `timer` resolve |

### Histórico

```
┌──────────────────────────────────────┐
│ Histórico              ( Todos ▾ )   │  filtro por Perfil (inclui Perfis excluídos)
│ [  Dia  |  Semana  |  Mês  ]         │  segmentado
│ ┌──────────────────────────────────┐ │
│ │ ▁ ▃ █ ▅ ▂ ▆ ▄        eixo em h   │ │  barras 200 dp, cor de Foco
│ │ D S T Q Q S S                    │ │
│ └──────────────────────────────────┘ │
│  12 h 40 min       31                │  totais 2 × 2, display / caption
│  Minutos de Foco   Focos concluídos  │
│  4                 2 h 05 min        │
│  Focos incompletos Minutos de Pausa  │
│  (ícone) 12 dias de sequência        │
│  ░░▒▒▓░░▒▓▓░ … heatmap 53 × 7        │  célula 5 dp + 1 dp de espaço = 318 dp
│ Hoje                                 │
│  ● Foco · Trabalho 25/5  14:05  25 min│
│  ◐ Foco incompleto · Est…  13:10  12 min│
└──────────────────────────────────────┘
```

- **Janelas:** Dia mostra as 7 barras da semana atual (domingo a sábado). Semana mostra as últimas 8 semanas. Mês mostra os últimos 12 meses. (Confirmado pelo usuário.)
- **Heatmap:** escala fixa de 0, 1–25, 26–60, 61–120 e >120 min. O vazio é `#FFFFFF` a 6%. Os níveis são a cor de Foco a 25%, 45%, 70% e 100%. Hoje ganha um contorno de 1 dp. O heatmap não é interativo e tem uma descrição-resumo para leitor de tela. Se não couber, rola na horizontal e começa no fim (hoje).
- **Lista:**
  - Agrupada por dia ("Hoje", "Ontem", "seg., 23 de nov.").
  - Fase concluída: ● preenchido. Fase incompleta: ◐, com o texto "incompleto", que nunca depende só da cor.
  - O horário e os minutos nunca são truncados. O nome do Perfil trunca no fim.

**Piores casos:**

| Caso | Comportamento |
| --- | --- |
| 0 registros | Barras só com a linha de base. Os totais em "0 min" e "0". A sequência diz "Nenhum dia ainda". O heatmap fica todo vazio. A lista diz "Seu primeiro Foco aparece aqui." |
| Sequência de 0 / 1 / 999 dias | Em pt-BR, o CLDR põe 0 na categoria *one* ("0 dia"). Por isso, trate o 0 com uma string própria. 1 dá "1 dia". 999 dá "999 dias" (cabe no `display`). 1.234 dias usa o separador do pt-BR (`NumberFormat`) |
| Totais grandes | De 60 min para cima, mostre "h min" ("20 h 40 min"). O eixo Y fica sempre em horas |
| Perfil excluído no filtro | Aparece com o nome copiado e o sufixo "(excluído)" |

### Perfis

```
┌──────────────────────────────────────┐
│ Perfis                          ( + )│
│ ┌──────────────────────────────────┐ │
│ │ [prévia]  Trabalho 25/5   Ativo ⋮│ │  miniatura 56 dp (quadro estático do Perfil)
│ │  56 dp    25 · 5 · 15 · 4 Focos  │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ [prévia]  Estudo 50/10          ⋮│ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

- **Tocar na linha:** abre a Configuração do Perfil. O menu ⋮ tem Duplicar, Renomear e Excluir.
- **Excluir:**
  - Com um Perfil só, a opção fica desabilitada, com a legenda "É preciso ter pelo menos um Perfil".
  - Nos outros casos, abre um diálogo: "Excluir 'X'? As Fases dele continuam no Histórico."
- **"+":** cria um Perfil a partir do padrão e abre a Configuração com o campo de nome em foco.
- **Nome:** limite de 40 caracteres no campo (sugestão: não há limite no spec). Quebra em até 2 linhas e depois usa reticências.

### Configuração do Perfil

```
┌──────────────────────────────────────┐
│ ←  Trabalho 25/5                     │  title, 2 linhas no máximo
│ ┌──────────────────────────────────┐ │
│ │      prévia ao vivo · 176 dp     │ │  fixa, o conteúdo rola por baixo (máscara de fade)
│ └──────────────────────────────────┘ │
│ [ Foco | Pausa curta | Pausa longa ] │  qual Fase a prévia mostra
│ Tempo                                │
│  Nome            [Trabalho 25/5    ] │
│  Foco            ( − )  25 min ( + ) │  segurar repete e acelera. Toque no valor abre o teclado
│  Pausa curta     ( − )   5 min ( + ) │
│  Pausa longa     ( − )  15 min ( + ) │
│  Focos por Ciclo ( − )   4     ( + ) │
│  Estender soma   ( − )   5 min ( + ) │
│  Início automático dos Focos    [  ] │
│  Início automático das Pausas   [■ ] │
│ Estilo visual   [▣][▢][▢][▢]         │  4 miniaturas, só a escolhida anima
│ Cor de destaque  ● ● ●  [ ] Do Fundo │
│ Fundo  [Biblioteca|Gradiente|Cor|Foto]│
│  Véu        Automático ───●───────   │
│  Movimento do Fundo             [■ ] │
│ Alerta · Notificação                 │
└──────────────────────────────────────┘
```

- **Até o ticket 09:** a prévia não existe, e a seção Tempo vem primeiro, sozinha.
- **Limites:** durações de 1 a 180 min, Focos por Ciclo de 1 a 12, Estender de 1 a 30 min (sugestões). O botão do limite fica desabilitado, com o háptico `REJECT`.
- **Piores casos:**
  - "1 min" e "180 min": o valor tem largura fixa (`tnum`), e os botões − e + não se movem.
  - Nome longo: o título quebra em 2 linhas, e a prévia não se move.

---

## 9. Movimento reduzido

- **Detecção:** "Remover animações" grava `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`. Leia o valor e observe mudanças com um `ContentObserver`.
- **Atenção:** com escala 0, o Compose termina as animações no quadro seguinte (`MotionDurationScale`). Os fades suaves abaixo precisam rodar num contexto com `MotionDurationScale` de 1f. Confira no emulador.
- **Shaders:** o tempo dos shaders é nosso, então aplique os fatores à mão.

| O quê | Normal | Reduzido |
| --- | --- | --- |
| Fundo (Biblioteca, Gradiente vivo) | Velocidade 1× | 0.2× |
| Respiração (escala) | Tabela do §7 | Desligada |
| Ruído da borda | Tabela do §7 | Velocidade 0.2×, mesma amplitude |
| Troca de Fase | `SpringFase`, florescer, onda | Crossfade dos dois estados em 300 ms `EaseOut`, sem overshoot nem onda |
| Partículas do Anel fluido | 24, deriva de 6 dp/s | 8, deriva de 2 dp/s |
| Parallax | ±12 dp | Desligado |
| Inspiro do Iniciar, selo "+5 min", ticker | Ligados | Só fade em `DurCurta` |
| Entrada do app, Estilo visual | Escala 0.92→1 | Só fade |
| Eixo X compartilhado, elemento compartilhado | Deslocamento ou morph | Fade em `DurMedia` |
| Gráficos | Barras crescem na primeira visita | Fade de 150 ms. A sequência troca de valor sem contar |
| Springs com bounce | dampingRatio < 1 | dampingRatio 1 |
| Escala de toque, fades de navegação, cor | Ligados | **Mantidos**: ajudam a entender e não são vestibulares |
| Voltar preditivo e swipe | 1:1 | **Mantidos**: o usuário produz o movimento. A soltura usa `SpringUi` sem bounce |

---

## 10. O que NÃO animar

1. A troca dos números a cada segundo: sem roll, fade ou pulso.
2. O progresso correndo atrás do tempo real ao abrir, restaurar ou voltar ao app.
3. A entrada de linhas nas listas do Histórico e dos Perfis: sem fade e sem stagger.
4. As células do heatmap e os números dos totais.
5. Os gráficos a cada visita (só na primeira por processo, §5.11).
6. As seções da Configuração do Perfil ao abrir: uma entrada por container.
7. O controle deslizante do Véu e os valores dos controles de duração (manipulação direta).
8. Qualquer loop de atenção: nada de pulso no "Pausado", no botão Iniciar ou no chip.
9. O Fundo fora do Timer (ele desacelera e para).
10. Diálogos, menus, sheets e Snackbar: os padrões do M3, sem personalizar.
11. A splash: o ícone é estático.
12. Indicadores de carregamento: os dados são locais. Mostre nada até chegarem, sem spinner nem shimmer.
13. Hápticos repetidos: nenhum por segundo e nenhum no fim de Fase com o app aberto (§6).
14. Ripple nos controles do Timer: a escala substitui.

---

## 11. Mapa ticket → seções

| Ticket | Seções |
| --- | --- |
| 01 Esqueleto + Timer mínimo | §2 (fonte, `timer`, formato, `tnum`), §3 (esquema, contraste), §4, §5.2, §5.3, §5.4, §6 (pressionado, hápticos de Iniciar, Pausar e Pular, alvos), §8 Timer, §10 |
| 02 Segundo plano + Notificação de contagem + Alerta | §4 regra 6 (restaurar sem correr atrás), §5.1 (volta do segundo plano), §5.6 (sem replay), §6 (Alerta não duplica o háptico) |
| 03 Perfis e Tempo | §5.5, §5.7, §5.8, §5.10, §6 (hápticos dos controles, voltar preditivo), §8 Perfis e Configuração do Perfil (Tempo), piores casos de nome, 1 e 180 min |
| 04 Histórico: registro e lista | §5.10, §6 (swipe para apagar + Desfazer), §8 Histórico (lista, 0 registros), §10 |
| 05 Configuração de Alerta e notificação | §6 (chaves), §8 Configuração do Perfil (Alerta, Notificação) |
| 06 Gráficos do Histórico | §2 (`display`, `caption`, `tnum`), §3 (cor de Foco), §5.11, §8 Histórico (janelas, heatmap, piores casos 0 e 999), §9, §10 |
| 07 Fundo: cor, Gradiente vivo, foto e Véu | §3 (camadas, Véu automático, contraste), §5.9, §5.10, §7 Fundo, §9 |
| 08 Biblioteca | §3, §5.1, §7 Fundo (velocidade, miniaturas), §9 |
| 09 Blob com vidro, Blob puro, Cor de destaque | §3 (Cor de destaque, vidro, profundidade), §4, §5.1–§5.7, §5.9, §6 (alvos 48 dp, descrições), §7 Blob e tabela de respiração, §9 |
| 10 Anel fluido | §3 (miolo profundo), §5.6, §7 Anel fluido, §9 |
| 11 Vidro com parallax | §3 (vidro), §5.6, §7 Vidro com parallax, §9 |

# Spec: App Pomodoro para Android (v1)

Status: ready-for-agent

## Problem Statement

Quem usa a técnica Pomodoro hoje escolhe entre dois tipos de app. Os apps simples têm durações fixas e visual genérico. Os apps completos parecem uma planilha. O usuário quer um timer que se ajuste ao seu jeito de trabalhar e estudar: durações, ritmo do Ciclo, sons e aparência. Ele também quer que a tela seja bonita o bastante para dar vontade de abrir o app. E precisa confiar que o app avisa no fim de cada Fase, mesmo com a tela desligada ou o app fechado.

## Solution

Um app Android nativo, em Kotlin com Jetpack Compose, para Android 13 ou mais novo. O app roda Ciclos Pomodoro: N Focos, com uma Pausa curta entre eles e uma Pausa longa no fim. Toda a configuração fica em Perfis: durações, N, início automático, som, vibração, notificação, Fundo, Véu, Estilo visual e Cor de destaque. O usuário escolhe entre quatro Estilos visuais animados. O Fundo pode ser uma arte viva da Biblioteca, uma foto do usuário, uma cor ou um Gradiente vivo. Uma Notificação de contagem mantém o timer vivo em segundo plano e oferece controles. Um Alerta avisa no fim de cada Fase. O Histórico registra cada Fase e mostra gráficos por dia, semana e mês, e um heatmap anual. Os dados ficam só no aparelho.

## User Stories

### Timer e Ciclo

1. Como usuário, quero iniciar um Foco com um toque, para começar a trabalhar sem atrito.
2. Como usuário, quero ver o tempo restante da Fase em números grandes, para saber de relance quanto falta.
3. Como usuário, quero ver o tipo da Fase atual (Foco, Pausa curta ou Pausa longa), para saber se é hora de trabalhar ou de descansar.
4. Como usuário, quero ver minha posição no Ciclo (por exemplo, "Foco 2 de 4"), para saber quanto falta até a Pausa longa.
5. Como usuário, quero pausar e retomar a Fase atual, para lidar com interrupções sem perder o progresso.
6. Como usuário, quero Pular a Fase atual, para avançar quando terminei antes ou quando quero pular uma Pausa.
7. Como usuário, quero Estender a Fase atual em alguns minutos, para terminar uma tarefa sem recomeçar o timer.
8. Como usuário, quero reiniciar o Ciclo, para recomeçar do Foco 1 quando mudar de atividade.
9. Como usuário, quero que o app abra com o timer parado, para que nada comece sem a minha ação.
10. Como usuário, quero que, ao fim da Pausa longa, um novo Ciclo comece, para seguir trabalhando no mesmo ritmo.
11. Como usuário, quero que a próxima Fase comece sozinha, se eu ligar essa opção, para não ter de tocar no telefone a cada troca.
12. Como usuário, quero chaves separadas de início automático para Focos e para Pausas, para que as Pausas comecem sozinhas e os Focos esperem por mim, ou o contrário.
13. Como usuário, quero que o timer continue certo depois de eu fechar o app ou desligar a tela, para confiar na contagem.
14. Como usuário, quero que o timer continue certo depois de o sistema matar o processo do app, para não perder uma Fase em andamento.

### Alerta

15. Como usuário, quero receber um Alerta no fim de cada Fase, para saber a hora de parar ou de voltar.
16. Como usuário, quero que o Alerta toque um som, para ouvir o aviso mesmo sem olhar o telefone.
17. Como usuário, quero escolher o som do Alerta entre os sons embutidos do app, para ter um aviso agradável.
18. Como usuário, quero escolher um toque do sistema como som do Alerta, para usar um som que já conheço.
19. Como usuário, quero desligar o som do Alerta, para usar o app em ambientes silenciosos.
20. Como usuário, quero ligar ou desligar a vibração do Alerta, para ser avisado com o telefone no silencioso.
21. Como usuário, quero que o Alerta acenda a tela bloqueada, se eu ligar essa opção, para que o aviso funcione como um despertador.
22. Como usuário com Android 14 ou mais novo, quero que o app me leve à tela de permissão do alerta em tela cheia, para conceder a permissão sem procurar.
23. Como usuário que negou essa permissão, quero que o Alerta vire uma notificação comum, para não ficar sem aviso.
24. Como usuário, quero que o app peça a permissão de notificações na primeira vez que eu iniciar uma Fase, para entender por que ela é necessária.

### Notificação de contagem

25. Como usuário, quero uma Notificação de contagem com o tempo restante enquanto a Fase roda, para acompanhar o timer sem abrir o app.
26. Como usuário, quero pausar e retomar pela Notificação de contagem, para controlar o timer pela tela bloqueada.
27. Como usuário, quero Pular pela Notificação de contagem, para avançar sem abrir o app.
28. Como usuário, quero Estender pela Notificação de contagem, para ganhar minutos sem abrir o app.
29. Como usuário, quero escolher no Perfil quais botões aparecem na Notificação de contagem, para ver só os controles que uso.
30. Como usuário, quero deixar a Notificação de contagem discreta, sem ícone na barra de status, para que ela não polua a tela.
31. Como usuário, quero que tocar na Notificação de contagem abra o app na tela do Timer, para chegar rápido ao timer.

### Perfis

32. Como usuário, quero criar Perfis com nome, por exemplo "Estudo 50/10" e "Trabalho 25/5", para alternar entre rotinas diferentes.
33. Como usuário, quero configurar no Perfil a duração do Foco, da Pausa curta e da Pausa longa, para ajustar o ritmo à atividade.
34. Como usuário, quero configurar no Perfil quantos Focos formam um Ciclo, para escolher quando vem a Pausa longa.
35. Como usuário, quero configurar no Perfil quantos minutos o Estender soma, para ajustar a extensão ao meu uso.
36. Como usuário, quero que cada Perfil guarde também a aparência (Fundo, Véu, Estilo visual e Cor de destaque), para que cada rotina tenha a sua identidade.
37. Como usuário, quero trocar o Perfil ativo pela tela do Timer, para mudar de rotina rápido.
38. Como usuário que troca de Perfil no meio de uma Fase, quero que a Fase atual seja encerrada e registrada, e que um novo Ciclo comece parado no Foco 1, para que o Histórico fique correto.
39. Como usuário que edita a duração do Perfil ativo durante uma Fase, quero que a nova duração valha a partir da próxima Fase, para não bagunçar a Fase atual.
40. Como usuário que edita a aparência do Perfil ativo, quero ver a mudança na hora, para avaliar o resultado enquanto ajusto.
41. Como usuário, quero duplicar um Perfil, para criar uma variação sem configurar tudo de novo.
42. Como usuário, quero renomear e excluir Perfis, para manter a lista organizada.
43. Como usuário, quero que o app impeça a exclusão do último Perfil, para que exista sempre um Perfil ativo.
44. Como usuário que exclui um Perfil, quero que as Fases dele continuem no Histórico com o nome do Perfil, para não perder meu registro.
45. Como usuário novo, quero um Perfil padrão já pronto (25/5/15, 4 Focos por Ciclo), para usar o app sem configurar nada.

### Fundo e Véu

46. Como usuário novo, quero que o Fundo padrão seja uma arte da Biblioteca, para que o app já fique bonito na primeira abertura.
47. Como usuário, quero escolher uma arte da Biblioteca como Fundo, para trocar o clima da tela.
48. Como usuário, quero que as artes da Biblioteca se movam devagar, para que a tela pareça viva.
49. Como usuário, quero usar uma foto minha como Fundo, para personalizar o app.
50. Como usuário, quero escolher a foto pelo seletor de fotos do sistema, sem dar permissão de acesso a todas as minhas fotos, para proteger minha privacidade.
51. Como usuário, quero que a foto continue como Fundo mesmo se eu apagar o original da galeria, para não perder a configuração.
52. Como usuário, quero usar uma cor sólida como Fundo, para ter uma tela mínima.
53. Como usuário, quero montar um Gradiente vivo com as cores que eu escolher, para ter um Fundo colorido e em movimento.
54. Como usuário, quero desligar o Movimento do Fundo, para ter uma tela parada ou poupar bateria.
55. Como usuário, quero que o app aplique um Véu automático sobre o Fundo, para que os números fiquem legíveis em qualquer imagem.
56. Como usuário, quero ajustar a intensidade do Véu, para equilibrar a legibilidade e a beleza do Fundo.

### Estilo visual e Cor de destaque

57. Como usuário, quero escolher o Estilo visual Blob com vidro, para ter uma forma fluida sobre um cartão de vidro fosco.
58. Como usuário, quero escolher o Estilo visual Blob puro, para ter só a forma fluida sobre o Fundo.
59. Como usuário, quero escolher o Estilo visual Anel fluido, para ter um anel de progresso com borda líquida, partículas e brilho na ponta.
60. Como usuário, quero escolher o Estilo visual Vidro com parallax, para ter números grandes em vidro fosco, com o Fundo se movendo levemente.
61. Como usuário, quero que o Estilo visual mostre o progresso da Fase (o blob encolhe, o anel se fecha), para sentir o tempo passar sem ler números.
62. Como usuário, quero uma transição animada e suave ao trocar de Fase, para que a mudança entre Foco e Pausa seja agradável.
63. Como usuário, quero escolher uma Cor de destaque para cada tipo de Fase, para distinguir Foco e Pausa pela cor.
64. Como usuário, quero que a Cor de destaque seja extraída do Fundo, se eu escolher essa opção, para que o timer combine com a imagem.
65. Como usuário, quero ver uma prévia ao vivo do Estilo visual na tela de configuração do Perfil, para escolher sem sair e voltar.
66. Como usuário que ligou "remover animações" no Android, quero que o app reduza o movimento, para não sentir desconforto.

### Histórico

67. Como usuário, quero que cada Foco e cada Pausa concluídos entrem no Histórico, para ter um registro real do meu dia.
68. Como usuário que Pulou uma Fase, quero que ela entre no Histórico como Fase incompleta, com os minutos que realmente rodaram, para que os números sejam honestos.
69. Como usuário, quero que Fases com menos de 1 minuto não entrem no Histórico, para que toques acidentais não poluam o registro.
70. Como usuário, quero que o tempo pausado não conte como minutos, para que o Histórico mostre só tempo real.
71. Como usuário que Estendeu uma Fase, quero que ela conte como concluída, com os minutos reais, para que a extensão não vire Fase incompleta.
72. Como usuário, quero ver os minutos de Foco por dia, por semana e por mês em gráficos de barras, para entender meu ritmo.
73. Como usuário, quero ver os totais do período (minutos de Foco, Focos concluídos, Focos incompletos e minutos de Pausa), para ter um resumo rápido.
74. Como usuário, quero ver minha sequência de dias seguidos com pelo menos um Foco concluído, para me manter motivado.
75. Como usuário, quero ver um heatmap anual dos minutos de Foco por dia, para ver meu ano de relance.
76. Como usuário, quero filtrar o Histórico por Perfil, para comparar rotinas.
77. Como usuário, quero ver uma lista cronológica das Fases, com tipo, Perfil, horário, minutos e se foi concluída ou incompleta, para conferir os detalhes.
78. Como usuário, quero apagar uma Fase do Histórico, para corrigir um registro errado.

### Geral

79. Como usuário, quero navegar entre Timer, Histórico e Perfis, para chegar a cada parte do app em um toque.
80. Como usuário, quero que todos os textos estejam em português do Brasil, para usar o app na minha língua.
81. Como usuário, quero que os controles tenham descrições para leitor de tela e alvos de toque grandes, para usar o app com acessibilidade.

## Implementation Decisions

### Plataforma e stack

- Só Android, com `minSdk` 33 (ADR-0001) e `targetSdk` igual à plataforma estável mais recente instalada.
- Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room e DataStore. Injeção de dependência feita à mão, sem framework.
- Um único módulo Gradle. O Motor do Ciclo e a Agregação do Histórico ficam em pacotes sem dependência de Android, para rodar os testes na JVM.
- Os dados ficam só no aparelho, sem conta e sem sincronização (ADR-0002).
- Textos em `strings.xml`, só em pt-BR.

### Motor do Ciclo (Kotlin puro)

- Uma máquina de estados que conhece o Ciclo, as Fases e o tempo. Ela não conhece Android.
- Entradas: os comandos Iniciar, Pausar, Retomar, Pular, Estender, Reiniciar Ciclo e Trocar Perfil, e um relógio injetado (tempo monotônico).
- Saídas: o estado atual (tipo da Fase, posição no Ciclo, duração, tempo decorrido, rodando ou pausado) e os registros de Fase prontos para o Histórico.
- O tempo restante é calculado do instante de início e da soma das pausas. Não é calculado por contagem de ticks. Isso evita deriva e permite restaurar o estado depois que o processo morre.
- O estado do Motor é serializável e é gravado a cada comando, para restauração.
- As regras do domínio ficam aqui:
  - A sequência do Ciclo: N Focos, Pausas curtas entre eles, Pausa longa no fim e um novo Ciclo.
  - O início automático, com uma chave para Focos e outra para Pausas.
  - Uma nova duração vale a partir da próxima Fase.
  - Trocar Perfil encerra a Fase atual e começa um novo Ciclo, parado no Foco 1.
  - Estender soma os minutos definidos no Perfil.
  - Uma Fase incompleta só é registrada se rodou pelo menos 1 minuto. Tempo pausado não conta.
  - Uma Fase Estendida que chega ao fim conta como concluída.

### Agregação do Histórico (Kotlin puro)

- Funções puras que recebem a lista de registros de Fase, o fuso horário e a data de referência.
- Elas calculam:
  - Os minutos de Foco por dia, semana e mês.
  - Os totais do período: minutos de Foco, Focos concluídos, Focos incompletos e minutos de Pausa.
  - A sequência de dias seguidos com pelo menos um Foco concluído.
  - O heatmap anual.
  - O filtro por Perfil.
- A semana começa no domingo, como é comum no Brasil.

### Armazenamento

- **Room, tabela de Perfis:** todos os campos de configuração do Perfil.
  - O Fundo é um tipo selado: arte da Biblioteca (com o identificador da arte), foto (com o caminho interno), cor ou Gradiente vivo (com a lista de cores).
- **Room, tabela de registros de Fase:**
  - O tipo da Fase, o início e o fim em tempo de parede, os minutos efetivos e se foi concluída ou incompleta.
  - O id do Perfil é anulável. O nome do Perfil é copiado para o registro, para que o registro sobreviva à exclusão do Perfil.
- **DataStore:** o id do Perfil ativo e o estado serializado do Motor do Ciclo.
- **Foto enviada:** escolhida pelo Photo Picker, que não exige permissão. Ela é reduzida para a resolução da tela e copiada para o armazenamento interno. A foto anterior é apagada quando nenhum Perfil a usa mais.

### Execução em segundo plano e Alerta

- Um foreground service roda enquanto uma Fase está rodando ou pausada. Ele mostra a Notificação de contagem. O tipo do serviço é `specialUse`, com justificativa no manifest.
- O fim da Fase é agendado com alarme exato pela permissão `USE_EXACT_ALARM`, que a política do Google Play permite para apps de timer. O Alerta dispara mesmo se o serviço tiver morrido.
- Os canais de notificação ficam fixos, porque o Android não deixa mudar a importância de um canal depois que ele existe:
  - Contagem normal.
  - Contagem discreta (importância mínima).
  - Alerta (importância alta).
- A Notificação de contagem usa um cronômetro do sistema (chronometer em contagem regressiva), para não atualizar a notificação a cada segundo.
- As ações da notificação (Pausar/Retomar, Pular e Estender) enviam comandos ao Motor do Ciclo. O Perfil define quais ações aparecem.
- O alerta em tela cheia usa uma full-screen intent. No Android 14 ou mais novo, o app confere `canUseFullScreenIntent`. Se a permissão faltar, o app oferece abrir a tela do sistema. Sem a permissão, o Alerta vira uma notificação comum de prioridade alta.
- A permissão `POST_NOTIFICATIONS` é pedida no primeiro Iniciar.

### Sons

- De 4 a 6 sons suaves embutidos, sintetizados por script fora do app e salvos como arquivos de áudio nos recursos.
- Também é possível escolher um toque do sistema, pelo seletor de toques do Android.

### Visual

- **Estilos visuais:** quatro, e todos recebem o mesmo contrato: o progresso (de 0 a 1), o tipo da Fase, a Cor de destaque e o estado (rodando ou pausado).
  - Blob com vidro: um shader AGSL com a forma fluida, sobre um cartão com blur nativo.
  - Blob puro: o mesmo shader, sem cartão.
  - Anel fluido: um shader AGSL com borda líquida, partículas e brilho na ponta.
  - Vidro com parallax: números grandes em vidro fosco, e o Fundo se desloca pelo sensor de movimento.
- **Biblioteca:** de 6 a 8 artes geradas por shader AGSL, com movimento lento (ADR-0003), por exemplo dunas, aurora e ondas. Cada arte declara uma paleta, usada pela Cor de destaque automática.
- **Gradiente vivo:** um mesh gradient feito com shader, com as cores escolhidas pelo usuário.
- **Movimento do Fundo:** quando está desligado, as artes e o Gradiente vivo param.
- **Véu:** uma camada que escurece e desfoca o Fundo, com blur nativo. A intensidade automática vem do brilho médio do Fundo, e o Perfil ajusta a intensidade.
- **Cor de destaque automática:** para foto, vem da extração de paleta com `androidx.palette`. Para arte e gradiente, vem da paleta declarada. Para cor sólida, vem de uma variação da própria cor.
- **Troca de Fase:** uma transição animada de cor e de forma entre os tipos de Fase.
- **Movimento reduzido:** se o sistema pedir "remover animações", o app reduz o movimento do Fundo e dos Estilos visuais, e as transições ficam curtas.

### Telas

- **Timer:** o Estilo visual sobre o Fundo, o tipo da Fase, a posição no Ciclo, os controles e o seletor de Perfil.
- **Histórico:** o seletor de período (dia, semana e mês), as barras, os totais, a sequência, o heatmap anual, o filtro por Perfil e a lista de Fases.
- **Perfis:** a lista, com criar, duplicar, renomear e excluir.
- **Configuração do Perfil:** as seções Tempo, Alerta, Notificação, Fundo, Estilo visual e Cor de destaque, com prévia ao vivo.

## Testing Decisions

- Um bom teste verifica comportamento externo, pela interface pública do módulo: comandos entram, estado e registros saem. Ele não verifica a estrutura interna, nomes privados ou a ordem das chamadas. Um refactor que mantém o comportamento não pode quebrar o teste.
- **Seam 1, Motor do Ciclo:** testes JVM com relógio falso. Eles cobrem:
  - A sequência completa do Ciclo com N variado.
  - Pausar e Retomar, conferindo que o tempo pausado não conta.
  - Pular antes e depois de 1 minuto.
  - Estender, conferindo que a Fase conta como concluída com os minutos reais.
  - O início automático, com as chaves de Foco e de Pausa.
  - A edição de duração durante uma Fase.
  - Trocar Perfil no meio de uma Fase.
  - A restauração do estado serializado depois de um intervalo longo, incluindo Fases que terminaram enquanto o processo estava morto.
- **Seam 2, Agregação do Histórico:** testes JVM com listas de registros montadas no teste. Eles cobrem:
  - Os totais por dia, semana e mês.
  - As bordas de dia e de semana no fuso local.
  - A sequência de dias, com e sem buracos.
  - O heatmap anual.
  - O filtro por Perfil, incluindo registros de Perfis excluídos.
- Visual, notificações, foreground service, alarme e permissões são verificados à mão no emulador `Pixel_9` (API 35). Não há testes instrumentados.
- Prior art: nenhum, porque o projeto começa do zero. Use JUnit e `kotlin.test` como no template padrão do Android.

## Out of Scope

- Desktop, iOS e Compose Multiplatform.
- Sincronização, contas, backup na nuvem e exportar ou importar dados.
- Idiomas além de pt-BR.
- Widgets, Quick Settings tiles, Wear OS e atalhos de launcher.
- Integração com Não Perturbe e bloqueio de apps.
- Personalização de fontes.
- Testes instrumentados e de screenshot.

## Further Notes

- Os termos seguem o `GLOSSARY.md`. Não use "sessão", "rodada", "preset", "tema" ou "alarme" no código nem na interface.
- O visual é o maior risco de esforço: são quatro Estilos visuais, mais as artes da Biblioteca e o Gradiente vivo, todos em shader. Implemente primeiro o Motor do Ciclo, a notificação e o Alerta, e confira tudo no emulador antes de polir os shaders.
- A política do Google Play para `USE_EXACT_ALARM` e para a full-screen intent pode exigir uma declaração no envio à loja. O spec não cobre a publicação na loja.

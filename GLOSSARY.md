# Glossário

## Fase

Um timer de um único tipo, com duração definida. Existem três tipos: **Foco**, **Pausa curta** e **Pausa longa**.

_Evite_: sessão, timer, rodada.

## Foco

Fase de trabalho concentrado.

_Evite_: pomodoro (como nome da Fase), trabalho.

## Pausa curta

Fase de descanso entre dois Focos do mesmo Ciclo.

## Pausa longa

Fase de descanso que encerra um Ciclo.

## Ciclo

Sequência de N Focos, com uma Pausa curta entre cada par, terminando em uma Pausa longa. Ao fim da Pausa longa, um novo Ciclo começa.

_Evite_: rodada, sessão.

## Fundo

O que aparece atrás do timer: uma imagem da Biblioteca, uma imagem enviada pelo usuário, uma cor sólida ou um gradiente.

## Biblioteca

O conjunto fixo de artes abstratas que vem dentro do app e pode ser usado como Fundo. As artes se movem devagar. Uma delas é o Fundo padrão.

## Gradiente vivo

Um Fundo de gradiente cujas cores se movem devagar.

## Movimento do Fundo

A chave do Perfil que liga ou desliga o movimento das artes da Biblioteca e do Gradiente vivo.

## Véu

Uma camada que escurece e desfoca o Fundo para o timer ficar legível. O app aplica o Véu automaticamente, e o usuário ajusta a intensidade no Perfil.

## Perfil

Um conjunto nomeado com toda a configuração do usuário: durações das Fases, N do Ciclo, início automático, som, vibração, Fundo, Estilo visual e Cor de destaque. Um Perfil está ativo por vez. Exemplos: "Estudo 50/10", "Trabalho 25/5".

_Evite_: preset, modo, configuração.

## Estilo visual

A forma como o timer é desenhado na tela. Existem quatro: **Blob com vidro**, **Blob puro**, **Anel fluido** e **Vidro com parallax**. Cada Perfil tem um Estilo visual.

_Evite_: tema, skin.

## Cor de destaque

A cor que o Estilo visual usa em cada tipo de Fase. O usuário escolhe uma cor por tipo de Fase, ou escolhe que ela seja extraída do Fundo.

## Notificação de contagem

A notificação fixa que mostra o tempo restante enquanto uma Fase roda. Ela traz os botões que o Perfil escolhe. Ela pode ficar discreta, sem ícone na barra de status. Fora do modo discreto, ela aparece como um player na tela de bloqueio, como uma música tocando, e não pode ser dispensada enquanto o Ciclo está em andamento.

_Evite_: notificação persistente, notificação do serviço.

## Alerta

O aviso de que uma Fase terminou: som, vibração e notificação. Ele pode acender a tela bloqueada, se o Perfil pedir.

_Evite_: alarme, aviso.

## Estender

Somar alguns minutos à Fase atual sem recomeçá-la. O Perfil define quantos minutos são somados.

## Pular

Encerrar a Fase atual antes do fim e ir para a próxima Fase do Ciclo.

## Fase incompleta

Uma Fase que foi pulada ou encerrada antes do fim. Conta no Histórico com os minutos que realmente rodaram, se rodou pelo menos 1 minuto. Tempo pausado não conta.

## Histórico

O registro das Fases do aparelho, concluídas e incompletas, com tipo, data, Perfil e minutos. Fica só no aparelho.

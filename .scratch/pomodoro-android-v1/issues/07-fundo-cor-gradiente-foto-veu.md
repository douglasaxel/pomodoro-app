# 07: Fundo: cor, Gradiente vivo, foto e Véu

**What to build:** No Perfil, o usuário escolhe o Fundo da tela Timer: uma cor sólida, um Gradiente vivo com as cores que ele quiser ou uma foto da galeria. Um Véu ajustável mantém os números legíveis. Uma chave desliga o Movimento do Fundo.

**Blocked by:** 03

**Status:** resolved

- [ ] O Fundo é um tipo selado guardado no Perfil: cor, Gradiente vivo (lista de cores) ou foto (caminho interno). A arte da Biblioteca vem no ticket 08.
- [ ] O Gradiente vivo é um mesh gradient feito com shader AGSL. As cores se movem devagar.
- [ ] A foto é escolhida pelo Photo Picker, sem permissão. Ela é reduzida para a resolução da tela e copiada para o armazenamento interno. A foto antiga é apagada quando nenhum Perfil a usa mais.
- [ ] O Véu escurece e desfoca o Fundo, com blur nativo. A intensidade automática vem do brilho médio do Fundo, e o Perfil ajusta a intensidade.
- [ ] Com o Movimento do Fundo desligado, o Gradiente vivo fica parado.
- [ ] Uma mudança de aparência no Perfil ativo aparece na hora, sem afetar a Fase atual.
- [ ] Verificação manual: uma foto muito clara e uma muito escura continuam legíveis com o Véu automático.

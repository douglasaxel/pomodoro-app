# 08: Biblioteca

**What to build:** No Perfil, o usuário escolhe uma arte abstrata da Biblioteca como Fundo. As artes se movem devagar, como dunas, aurora ou ondas. Um usuário novo já abre o app com uma arte como Fundo padrão.

**Blocked by:** 07

**Status:** ready-for-agent

- [ ] De 6 a 8 artes desenhadas ao vivo com shaders AGSL (ADR-0003).
- [ ] Cada arte declara uma paleta, que a Cor de destaque automática usa no ticket 09.
- [ ] A arte da Biblioteca entra como mais uma opção do Fundo, guardando o identificador da arte.
- [ ] O Perfil padrão passa a usar uma arte da Biblioteca. Os Perfis já existentes não mudam.
- [ ] Com o Movimento do Fundo desligado, as artes ficam paradas.
- [ ] A escolha de Fundo mostra miniaturas animadas das artes.
- [ ] Verificação manual: a animação fica fluida no emulador, sem engasgos visíveis.

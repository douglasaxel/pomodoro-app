# 09: Estilos Blob com vidro e Blob puro + Cor de destaque

**What to build:** A tela Timer ganha o visual principal. Uma forma fluida respira e encolhe conforme a Fase avança, sobre um cartão de vidro fosco (Blob com vidro) ou direto sobre o Fundo (Blob puro). O usuário escolhe uma Cor de destaque por tipo de Fase, ou pede que ela venha do Fundo. Na troca de Fase, a cor e a forma mudam com uma transição suave.

**Blocked by:** 07, 08

**Status:** resolved

- [ ] Os Estilos visuais seguem um contrato comum: progresso (de 0 a 1), tipo da Fase, Cor de destaque e estado (rodando ou pausado).
- [ ] O shader AGSL do blob é usado pelos Estilos Blob com vidro (cartão com blur nativo) e Blob puro.
- [ ] O Estilo visual é escolhido no Perfil. O padrão é Blob com vidro.
- [ ] A Cor de destaque pode ser uma por tipo de Fase ou automática:
  - Foto: extraída por `androidx.palette`.
  - Arte e gradiente: a paleta declarada.
  - Cor sólida: uma variação da própria cor.
- [ ] A transição animada de cor e de forma acontece entre os tipos de Fase.
- [ ] Movimento reduzido: com "remover animações" no sistema, o movimento diminui e as transições ficam curtas.
- [ ] A Configuração do Perfil mostra uma prévia ao vivo do Estilo visual.
- [ ] Os controles têm descrições para leitor de tela e alvos de toque de pelo menos 48dp.

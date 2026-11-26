# 12: Seletor livre de cor

**What to build:** Ao escolher uma cor sólida de Fundo, uma cor do Gradiente vivo ou uma Cor de destaque, o usuário pode usar uma das 12 cores da paleta como atalho ou escolher qualquer cor num seletor livre de matiz, saturação e brilho. A prévia mostra a cor na hora.

**Blocked by:** 07

**Status:** ready-for-agent

- [ ] Um seletor de cor em Compose, feito no próprio app, sem biblioteca: matiz, saturação e brilho, mais o campo hex.
- [ ] As 12 cores da paleta continuam como atalhos, acima do seletor.
- [ ] O mesmo seletor serve para a cor sólida do Fundo e para cada cor do Gradiente vivo. O ticket 09 o reaproveita para a Cor de destaque.
- [ ] Uma cor escolhida no seletor é salva no Perfil e sobrevive a reabrir o app.
- [ ] O Véu automático continua garantindo a leitura com cores muito claras, como o branco puro.
- [ ] Teste JVM da conversão entre HSV, hex e ARGB, incluindo hex inválido.
- [ ] Os controles têm descrições para leitor de tela e alvos de toque de pelo menos 48dp.

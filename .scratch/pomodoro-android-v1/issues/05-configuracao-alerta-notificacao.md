# 05: Configuração de Alerta e notificação

**What to build:** No Perfil, o usuário escolhe o som do Alerta (embutido, toque do sistema ou nenhum) e liga ou desliga a vibração. Ele escolhe quais botões aparecem na Notificação de contagem, incluindo Estender, e se ela é discreta. Ele também pode fazer o Alerta acender a tela bloqueada.

**Blocked by:** 02, 03

**Status:** ready-for-agent

- [ ] De 4 a 6 sons suaves são sintetizados por script fora do app e salvos nos recursos de áudio. O script fica no repositório.
- [ ] O seletor de toques do sistema está disponível como alternativa. Também existe a opção sem som.
- [ ] A vibração do Alerta liga e desliga.
- [ ] A seção Notificação do Perfil escolhe os botões visíveis (Pausar/Retomar, Pular e Estender) e o modo discreto, que usa o canal de importância mínima.
- [ ] A ação Estender na notificação manda o comando ao Motor.
- [ ] Alerta em tela cheia: liga e desliga. No Android 14 ou mais novo, o app confere `canUseFullScreenIntent` e oferece abrir a tela do sistema. Sem a permissão, o Alerta vira uma notificação comum de prioridade alta.
- [ ] Verificação manual no emulador: cada opção muda o comportamento real da notificação e do Alerta.

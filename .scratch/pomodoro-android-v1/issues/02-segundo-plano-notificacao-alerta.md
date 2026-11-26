# 02: Segundo plano + Notificação de contagem + Alerta

**What to build:** O usuário inicia um Foco, fecha o app e desliga a tela. A Notificação de contagem mostra o tempo restante e tem os botões Pausar/Retomar e Pular. No fim da Fase, o Alerta toca um som padrão e vibra. O timer continua certo mesmo se o sistema matar o processo do app.

**Blocked by:** 01

**Status:** resolved

- [ ] Um foreground service do tipo `specialUse` roda enquanto uma Fase está rodando ou pausada.
- [ ] A Notificação de contagem usa um cronômetro regressivo do sistema, sem atualizar a cada segundo.
- [ ] As ações Pausar/Retomar e Pular da notificação mandam comandos ao Motor do Ciclo.
- [ ] Tocar na notificação abre o app na tela Timer.
- [ ] O fim da Fase é agendado com alarme exato, pela permissão `USE_EXACT_ALARM`. O Alerta dispara mesmo sem o serviço.
- [ ] Três canais fixos são criados: contagem normal, contagem discreta e Alerta.
- [ ] A permissão `POST_NOTIFICATIONS` é pedida no primeiro Iniciar.
- [ ] O estado do Motor é serializado no DataStore a cada comando e restaurado na abertura.
- [ ] Teste JVM: o estado é restaurado depois de um intervalo longo, incluindo Fases que terminaram enquanto o processo estava morto, com e sem início automático.
- [ ] Verificação manual no emulador: com a tela bloqueada, o Alerta toca. Depois de matar o processo pelo adb, o timer continua certo.

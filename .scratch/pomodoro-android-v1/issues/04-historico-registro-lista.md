# 04: Histórico: registro e lista

**What to build:** Cada Foco e cada Pausa que o usuário faz aparece no Histórico, em uma lista cronológica. A lista mostra tipo, Perfil, horário, minutos e se a Fase foi concluída ou incompleta. O usuário pode apagar uma Fase errada. Se ele excluir um Perfil, as Fases dele continuam na lista com o nome do Perfil.

**Blocked by:** 03

**Status:** resolved

- [ ] Room com a tabela de registros de Fase: tipo, início, fim, minutos efetivos, concluída ou incompleta, id do Perfil (anulável) e cópia do nome do Perfil.
- [ ] O Motor do Ciclo emite um registro no fim de cada Fase, ao Pular e ao Trocar Perfil.
- [ ] Testes JVM:
  - Uma Fase incompleta com menos de 1 minuto não gera registro.
  - O tempo pausado não conta como minutos.
  - Uma Fase Estendida que chega ao fim conta como concluída, com os minutos reais.
  - As Pausas também geram registro.
- [ ] A tela Histórico mostra a lista cronológica, com a opção de apagar uma Fase.
- [ ] Excluir um Perfil mantém os registros dele, com o nome copiado.
- [ ] Navegação entre Timer, Histórico e Perfis.

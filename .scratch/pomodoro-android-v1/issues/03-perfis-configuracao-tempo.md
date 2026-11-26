# 03: Perfis e configuração de Tempo

**What to build:** O usuário cria Perfis como "Estudo 50/10" e "Trabalho 25/5", cada um com suas durações, N, início automático e minutos de Estender. Ele troca o Perfil ativo pela tela Timer. Ele também usa o botão Estender no Timer.

**Blocked by:** 01

**Status:** resolved

- [ ] Room com a tabela de Perfis. Na primeira abertura, o app cria o Perfil padrão (25/5/15, N = 4).
- [ ] A tela Perfis permite criar, duplicar, renomear e excluir.
- [ ] O app impede a exclusão do último Perfil.
- [ ] A tela Configuração do Perfil tem a seção Tempo: as durações dos três tipos de Fase, N, as chaves de início automático (uma para Focos, outra para Pausas) e os minutos de Estender.
- [ ] O id do Perfil ativo fica no DataStore. Um seletor de Perfil fica na tela Timer.
- [ ] O Motor aceita Estender e Trocar Perfil. Ele aplica o início automático.
- [ ] Testes JVM:
  - Uma duração editada durante uma Fase vale só na próxima Fase.
  - Trocar Perfil encerra a Fase atual e começa um novo Ciclo, parado no Foco 1.
  - Estender soma os minutos do Perfil.
  - O início automático respeita as duas chaves.
- [ ] Navegação entre as telas Timer e Perfis.

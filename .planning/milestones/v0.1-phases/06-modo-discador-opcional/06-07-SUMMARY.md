---
phase: 06-modo-discador-opcional
plan: 07
subsystem: telecom
tags: [modo-discador, instrumentado, papel-de-sistema, reversao, dia-04, emulador]
requires:
  - "06-03 (SentinelaInCallService declarada, DialerRoleManager, DialerModeState, CallSessionStore)"
  - "06-04/06-05/06-06 (telas e aviso de chamada ja no lugar — nenhuma delas foi editada aqui)"
  - "04 (ContactsTestFixture e o precedente da identidade de shell adotada)"
  - "05 (ScreeningCoordinator e o molde do teste de vinculo de servico)"
provides:
  - "InCallServiceBindTest — vinculo real ao servico de interface de chamada no aparelho virtual"
  - "DialerRoleEligibilityTest — elegibilidade ao papel de telefone padrao provada pelo caminho que a verifica"
  - "DialerScreeningIntegrationTest — DIA-04 provado: politica por contato valendo de fato, motor intocado"
  - "DialerRoleReversionTest — independencia dos papeis, estado derivado e reversao pelo seletor do sistema"
  - "InCallServiceDeathTest — chamada de saida real e limpeza de chamadas presas"
  - "TelecomShell — comandos de sistema a partir do teste, com saida de erro separada"
  - "scripts/verify-dialer-lifecycle.sh — ciclo completo do papel e morte no meio da chamada, dirigido de FORA do processo"
affects:
  - "06-08 (a suite instrumentada passou de 53 para 80 casos; o filtro do Kover continua intocado)"
  - "Phase 9 (viva-voz e One UI seguem os unicos cenarios genuinamente fisicos desta fase)"
tech-stack:
  added: []
  patterns:
    - "comando de sistema pelo caminho de execucao da instrumentacao, com saida de erro como equivalente observavel do codigo de saida"
    - "prova cujo objeto e a morte do proprio processo pertence a quem esta fora dele"
    - "forma do comando travada por caso de teste, para que um atalho que bypassa verificacao nunca substitua o comando que verifica"
key-files:
  created:
    - app/src/androidTest/java/org/sentinela/app/telecom/InCallServiceBindTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/DialerRoleEligibilityTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/DialerScreeningIntegrationTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/DialerRoleReversionTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/InCallServiceDeathTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/TelecomShell.kt
    - scripts/verify-dialer-lifecycle.sh
  modified: []
decisions:
  - "perder um papel do sistema ENCERRA o processo do aplicativo (medido); logo, reversao e morte no meio da chamada nao podem ser provadas de dentro da instrumentacao e viraram script dirigido pelo computador"
  - "nenhum teste devolve o proprio papel; o encerramento das suites restaura o detentor apenas quando isso nao provoca a propria morte"
  - "DIA-04 provado, nao implementado: zero linha do motor de decisao mudou na fase 06"
  - "bloquear com as configuracoes de fabrica produz a variante que pede para nao registrar no historico do telefone, nao a rejeicao simples"
  - "os casos de contato esperam o conjunto de chaves do processo ficar quente: consulta imediata apos insercao responde pelo conjunto antigo quando outra suite ja o aqueceu"
metrics:
  duration_minutes: 58
  tasks: 3
  files_created: 7
  files_modified: 0
  tests_added: 27
  completed: 2026-07-29
---

# Phase 6 Plan 07: Modo Discador Provado no Aparelho Virtual Summary

O modo discador deixou de ser risco declarado e passou a ser comportamento medido: o aparelho
aceita o Sentinela como telefone padrão pelo caminho que **verifica** elegibilidade, a triagem
passa a cobrir a agenda e a política por contato vale de fato **sem uma linha nova no motor de
decisão**, devolver o papel restaura o discador de fábrica sem tocar no papel de triagem, e morrer
no meio de uma ligação não derruba a ligação.

## O que foi construído

**Task 1 — vínculo e elegibilidade** (`6c651ed`)

`InCallServiceBindTest` (4 casos) pede o vínculo de verdade ao serviço de interface de chamada e o
recebe; confere que o armazém da sessão e o conjunto de colaboradores são objeto único do processo;
e confere que o serviço de chamada e o de triagem convivem vinculados ao mesmo tempo. Ele
deliberadamente **não** exercita chamada: o canal do vínculo só aceita objeto de chamada montado
internamente pela plataforma, e a máquina de estado é do núcleo puro do plano 06-01.

`DialerRoleEligibilityTest` (6 casos) concede o papel pelo comando de papel — o que **roda a
verificação de elegibilidade** — e afirma a saída sem erro, o detentor resultante, a concordância
entre sistema e aplicativo, e a convivência com o papel de triagem. O atalho de configuração de
telefonia que aponta o discador padrão sem qualificar o aplicativo **não aparece no arquivo, nem em
prosa**: ele deixaria a suíte verde com o manifesto quebrado. A forma exata do comando está travada
por caso de teste.

`TelecomShell` executa comandos de sistema a partir do teste. O caminho de execução oferecido à
instrumentação entrega o comando direto ao sistema operacional, sem interpretador no meio — não há
encadeamento, ponto-e-vírgula nem a variável do código de saída. A prova disponível, e forte, é a
**saída de erro separada**: estes comandos terminam em silêncio absoluto quando dão certo e imprimem
a exceção da plataforma quando a elegibilidade é negada. Cada asserção ainda confere o **efeito** do
comando, nunca apenas o silêncio.

**Task 2 — DIA-04 provado** (`f4ad085`)

`DialerScreeningIntegrationTest` (7 casos) prepara um contato na agenda **real**, fixa a região em
BR (precedente da Fase 4), garante o papel de telefone padrão e exercita o **coordenador de triagem
real do container** para cada política de contato: Bloquear barra, Tocar permite, Silenciar
silencia, Nunca Silenciar permite, número fora da agenda continua barrado, e o padrão de fábrica é
Tocar nas duas pontas (valor padrão do tipo e leitura do repositório). Nenhuma dessas decisões
exigiu ramo novo: a última alteração do `CallDecisionEngine` continua sendo de `d7d188b`, da Fase 5.

**Task 3 — reversão e chamada real** (`8f1e6a5`)

`DialerRoleReversionTest` (6 casos) prova o que é observável sem a perda do papel: os dois papéis
são independentes e convivem; com o papel detido o estado do modo é ativo **sem nenhuma intenção
gravada**; sem o papel o estado deixa de ser ativo **mesmo com a intenção gravada dizendo o
contrário**; a triagem barra desconhecido e a decisão não depende do papel de telefone padrão; e a
reversão é a tela de escolha do sistema, nunca uma troca forçada.

`InCallServiceDeathTest` (4 casos) origina uma chamada de saída **de verdade**, confirma que ela
chega ao sistema de telefonia com o aplicativo como telefone padrão, e confirma que a limpeza de
chamadas presas devolve o aparelho ao estado neutro.

## A descoberta que reorganizou o plano

**Perder um papel do sistema encerra o processo do aplicativo.** Medido três vezes neste plano, com
o motivo registrado pelo próprio sistema:

```
ActivityManager: Killing <pid>:<pacote>/<uid> (adj 0): Permission or app op changed
```

Vale para o papel de telefone padrão e para o papel de triagem, e vale igual quando é o **usuário**
que troca nas configurações do sistema. A concessão, ao contrário, **não** mata: o processo
sobreviveu com o mesmo identificador.

A consequência de engenharia é direta: a instrumentação roda **dentro** do processo do aplicativo,
então um teste que devolvesse o próprio papel — ou que matasse o próprio processo no meio de uma
chamada, o outro cenário desta fase — morre junto com o que quer observar. A primeira execução deste
plano provou isso da forma mais literal possível: `Instrumentation run failed due to Process
crashed`, sem asserção nenhuma. Não existe assertiva mais fraca que salve esse caso: o observador é
o observado.

E a descoberta não é contratempo — **é o argumento do desenho da fase**. Ser encerrado ao perder o
papel é exatamente por que o estado do modo discador é **derivado** de perguntas ao sistema e nunca
de valor gravado (o aplicativo sempre volta em processo novo, e um valor gravado seria mentira desde
o primeiro instante), e é também por que desligar o modo desabilitando componente próprio é
proibido para sempre.

Por isso a metade da prova cujo objeto é a própria morte foi para **fora do processo**:
`scripts/verify-dialer-lifecycle.sh`, dirigido pelo computador, com códigos de saída de verdade.
Execução completa, transcrita:

```
== 1: ponto de partida ==
ok:   telefone padrao inicial e o discador de fabrica (com.google.android.dialer)
== 2: concessao do papel de triagem e do papel de telefone padrao ==
ok:   papel de triagem concedido (codigo de saida 0)
ok:   papel de telefone padrao concedido pelo caminho que verifica elegibilidade
ok:   o aplicativo e o telefone padrao do aparelho
ok:   os dois papeis convivem no mesmo aplicativo
== 3: morte do processo no meio de uma chamada ==
ok:   chamada de saida em curso no sistema de telefonia
ok:   processo do aplicativo vivo durante a chamada (pid 10075)
ok:   o processo morreu de fato (era 10075, agora 'nenhum')
ok:   A CHAMADA SOBREVIVEU a morte do nosso processo
ok:   o sistema de telefonia religou no discador de fabrica sozinho
ok:   limpeza de chamadas presas devolveu o aparelho ao estado neutro
== 4: a chamada seguinte volta a ser nossa ==
ok:   o sistema voltou a vincular o servico de interface de chamada do aplicativo
== 5: reversao ==
ok:   devolucao do papel aceita (codigo de saida 0)
ok:   o discador de fabrica voltou a ser o telefone padrao
ok:   o papel de triagem SOBREVIVEU a reversao
ok:   a plataforma encerrou o aplicativo ao retirar o papel (era 10309, agora 'nenhum')

TODOS os passos do ciclo de vida do modo discador OK
```

Os cinco comportamentos que o plano pediu estão todos aí, provados de forma automatizada — só
mudou **quem** os afirma, e mudou porque a plataforma não permite o contrário.

## Provas de vermelho (executadas em código já commitado e restauradas por cópia)

| Sabotagem | Resultado medido |
|---|---|
| Consulta à agenda anulada no coordenador de triagem (sempre ausência) | **5 vermelhos** em `DialerScreeningIntegrationTest`: as quatro políticas de contato e o padrão de fábrica |
| Declaração do serviço de interface de chamada removida do manifesto, aplicativo reinstalado | concessão do papel **rc=255**, `java.lang.RuntimeException: Failed` — reproduz digito por digito a medição da pesquisa e mostra que a elegibilidade depende dessa declaração |

Restauração por cópia de arquivo salva antes da sabotagem, nunca por `git checkout` — lição do plano
06-02, respeitada aqui desde a primeira sabotagem.

## Verificação

```
bash scripts/run-instrumented-tests.sh        -> 80 testes, 0 falhas (53 anteriores + 27 novos)
bash scripts/verify-dialer-lifecycle.sh       -> TODOS os passos OK
bash scripts/verify-invariants.sh             -> todos os invariantes OK (8 blocos, Bloco 7 incluso)
./gradlew testDebugUnitTest --rerun-tasks     -> 603 testes, 0 falhas
./gradlew lint detekt                         -> BUILD SUCCESSFUL
./gradlew koverLog                            -> 95,4741% (gate 80)
git log -- .../domain/CallDecisionEngine.kt   -> ultima alteracao d7d188b (Fase 5)
```

Critérios de aceite por grep:

| Critério | Medido | Esperado |
|---|---|---|
| casos em `InCallServiceBindTest` | 4 | >= 3 |
| casos em `DialerRoleEligibilityTest` | 6 | >= 3 |
| comando de concessão de papel no teste de elegibilidade | 1 | >= 1 |
| atalho que bypassa a qualificação | 0 | 0 |
| encerramento no teste de elegibilidade | 1 | >= 1 |
| container construído no teste de vínculo | 0 | 0 |
| casos em `DialerScreeningIntegrationTest` | 7 | >= 6 |
| fixture de contatos no teste de integração | 6 | >= 1 |
| casos em `DialerRoleReversionTest` | 6 | >= 4 |
| casos em `InCallServiceDeathTest` | 4 | >= 3 |
| comando de limpeza de chamadas presas | 1 | >= 1 |
| linhas alteradas no motor de decisão na fase 06 | 0 | 0 |

## Deviations from Plan

### Divergências forçadas por medição

**1. [Rule 3 - Blocking] A reversão e a morte no meio da chamada saíram da instrumentação**
- **Encontrado em:** Task 1, na primeira execução (`Process crashed`)
- **Causa:** perder papel do sistema encerra o processo do aplicativo, e a instrumentação roda nele
- **Correção:** `scripts/verify-dialer-lifecycle.sh` executa o ciclo completo de fora do processo,
  com códigos de saída reais e assertivas duras; as classes instrumentadas cobrem tudo o que é
  observável sem a perda do papel e registram em prosa por que o resto não cabe nelas
- **Não foi:** substituir por assertiva mais fraca, nem mandar o cenário para a Phase 9 — ele
  continua automatizado, só em outro executor

**2. [Rule 1 - Bug no teste] Decisão esperada errada para bloqueio**
- Bloquear com as configurações de fábrica produz a variante que **pede para não registrar no
  histórico do telefone**, não a rejeição simples. O motor estava certo; a expectativa estava
  errada. O pedido em si o Android só atende para aplicativo de operadora (medido na Fase 5, e o
  papel de telefone padrão **não** destrava isso), mas a decisão de domínio é a que a configuração
  pede.

**3. [Rule 1 - Bug no teste] Casos de contato vermelhos só na suíte inteira**
- O repositório de contatos do container mantém um conjunto de chaves reconstruído por observador
  **com atraso proposital**. Quando outra suíte da execução já o aqueceu, a consulta feita no
  instante seguinte à inserção responde pelo conjunto antigo. O `@Before` passou a esperar o contato
  ficar visível ao aplicativo, com mensagem própria — falhar ali é muito melhor que falhar nos casos
  de política por um motivo que não tem nada a ver com o modo discador.

**4. [Rule 3 - Blocking] `grep -q` sobre saída grande no script**
- `grep -q` fecha o cano ao primeiro acerto, quem escreve morre de cano quebrado e o modo do shell
  reprova a linha — o caso de **sucesso** era lido como falha. Trocado por contagem em variável.
  Terceira encarnação, neste repositório, da mesma armadilha de shell.

### Deliberado, além da letra do plano

- **`TelecomShell`** existe para não repetir os comandos de sistema em cinco arquivos. É o único
  lugar do repositório onde eles aparecem.
- **Um caso por comando travando a forma exata** (concessão de papel e limpeza de chamadas presas):
  é prova mais forte que grep e sobrevive a refatoração do helper.
- **`InCallServiceBindTest` ganhou um quarto caso** — os dois serviços vinculados ao mesmo tempo —
  porque é essa convivência que sustenta a triagem no modo discador.

### Escopo intocado

Zero arquivo de produção modificado. Nenhuma permissão nova, nenhuma biblioteca nova, nenhuma
chamada de rede. `CallDecisionEngine` intocado na fase inteira. Filtro do Kover intocado (é o plano
06-08). Manifest intocado (a sabotagem foi revertida e o `git diff` confirma).

## Blockers e pendências para a Phase 9

- **Chamada de ENTRADA simulada** continua fora do alcance do processo de teste: exige o console do
  aparelho virtual, cujo segredo de acesso vive no diretório pessoal de quem executa. Substituída
  pelo exercício do coordenador real com a agenda preparada, como o plano previu.
- **Viva-voz e comportamento da One UI** seguem os únicos cenários genuinamente físicos desta fase.
- **Sugestão de cenário novo para o roteiro Samsung:** conferir, em aparelho real, que o
  encerramento do aplicativo ao devolver o papel não deixa notificação de chamada órfã na One UI.

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim.

## Self-Check: PASSED

Os 7 arquivos criados existem em disco; os 3 commits declarados (`6c651ed`, `f4ad085`, `8f1e6a5`)
existem no histórico.

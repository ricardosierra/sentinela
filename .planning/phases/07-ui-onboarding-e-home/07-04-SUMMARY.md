---
phase: 07-ui-onboarding-e-home
plan: 04
subsystem: ui-state-holders
tags: [viewmodel, home, onboarding, settings, privacidade, papel-do-sistema, datastore]

requires:
  - "settings/DataStoreSettingsRepository (Fase 3) — retrato servido de cache mantido por coletor"
  - "data/local/BlockedCallRepository (Fase 3) — contagem total e recentes"
  - "telecom/SystemRoleGate + ScreeningRoleManager + DialerRoleManager (Fases 5-6)"
  - "telecom/call/dialerModeState (Fase 6) — funcao pura de precedencia do modo discador"
  - "permissions/RuntimePermissionAsk + data/contacts/ContactsPermissionState (Fases 4-5)"
  - "phone/PhoneMask (Fase 2) — mascara unica de exibicao"
  - "07-01 — as 44 chaves pt-BR da fase, incluindo os rotulos de motivo"

provides:
  - "ui/home/StatValue — tipo fechado que torna o zero mentiroso impossivel por assinatura"
  - "ui/home/HomeUiState + LastBlockedUi + reasonLabelRes — estado da home sem digitos crus"
  - "ui/home/HomeViewModel — papel vivo, contagens, ultima bloqueada mascarada"
  - "ui/onboarding/OnboardingUiState + TOTAL_DE_PASSOS + OnboardingViewModel"
  - "ui/settings/SettingsUiState + SettingsViewModel — efeito imediato, sem funcao de salvar"
  - "settings: chave onboarding_completed com leitura e marca, fora de ScreeningSettings"

affects:
  - "07-05..07-08 — as telas consomem estes tipos e montam as fabricas a partir do container"
  - "07-09+ — a rota de partida passa a poder ler onboardingCompleted sem bloquear"

tech-stack:
  added: []
  patterns:
    - "Colaboradores por parametro, container NUNCA no construtor nem na fabrica"
    - "Consulta de papel injetada como funcao para o teste poder CONTAR invocacoes"
    - "Estado indisponivel como tipo, nunca como numero de reserva"
    - "Marca de permissao gravada ao disparar, travada por lista ordenada de eventos"

key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/home/HomeUiState.kt
    - app/src/main/java/org/sentinela/app/ui/home/HomeViewModel.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/OnboardingUiState.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/OnboardingViewModel.kt
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsViewModel.kt
    - app/src/test/java/org/sentinela/app/ui/home/HomeViewModelTest.kt
    - app/src/test/java/org/sentinela/app/ui/home/RoleLiveStateTest.kt
    - app/src/test/java/org/sentinela/app/ui/onboarding/PermissionAskOrderTest.kt
    - app/src/test/java/org/sentinela/app/ui/settings/SettingsViewModelTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt

decisions:
  - "StatValue fechado em Loaded/Unavailable/Loading: sem Int na assinatura, o zero mentiroso deixa de ser convencao e passa a ser impossivel de compilar"
  - "Papel do sistema reconsultado a cada chamada, provado por CONTADOR de invocacoes; cronometro nao prova estrutura"
  - "HomeViewModel e OnboardingViewModel recebem colaboradores um por um e NAO conhecem AppContainer — nem na fabrica"
  - "Falha de leitura do historico vira estado visivel em vez de propagar: inverso deliberado do caminho da chamada da Fase 6"
  - "onboarding_completed e chave propria, nunca o contador de aberturas, que ja tem outro dono na Fase 9"
  - "Ausencia de funcao de salvar travada por teste de reflexao sobre os nomes de metodo"
  - "Coletor de teste precisa do despachante NAO CONFINADO: o escopo de fundo do runTest usa o padrao e a leitura veria sempre o valor inicial"

metrics:
  duration: ~50min
  tasks: 3
  files: 11
  tests_added: 42
  tests_total_jvm: 698
  coverage: 96.6157%
  completed: 2026-07-30
---

# Phase 7 Plano 04: Donos de Estado da Fase de Interface Summary

Os tres donos de estado da fase (home, onboarding e Protecao) com os tipos de estado que eles
publicam — incluindo `StatValue`, que torna o "zero mentiroso" impossivel por assinatura — e os
quatro guarda-corpos que travam as decisoes do usuario: papel vivo por contador de invocacoes, zero
impossivel por tipo, efeito imediato contra o repositorio real e marca de permissao gravada ao
disparar por lista ordenada de eventos.

## O que foi entregue

**Task 1 — o zero impossivel e o papel vivo** (commit `9744caa`)

`StatValue` e um `sealed interface` com `Loaded(count)`, `Unavailable` e `Loading`. Nenhuma das duas
variantes de ausencia carrega numero, entao **nao existe caminho em que a tela renderize `0` sem
carregamento efetivo** — a regra da secao 8 do contrato de interface saiu de convencao e virou tipo.
Os quatro casos que produzem ausencia (historico desligado, retencao que nao guarda, primeiro quadro,
falha de leitura) chegam a tela como estado, nunca como numeral.

`LastBlockedUi` carrega so o texto mascarado, o recurso do rotulo e o instante. A mascara e aplicada
no dono de estado, que e o ultimo ponto do aplicativo onde os digitos existem; o tipo do estado nao
tem campo para eles. `reasonLabelRes` mapeia o motivo REAL da decisao para os rotulos de 07-01, e os
motivos que nunca viram registro de bloqueio caem em "desconhecido" — nenhum rotulo de risco ou de
spam existe como opcao, porque esse dado nao existe no aplicativo.

`HomeViewModel` recebe as consultas de papel como **funcoes**, para que o teste possa conta-las.
`reconsultarPapel()` chama as tres na hora e publica; nenhum campo guarda a resposta entre chamadas.
A leitura do historico usa `catch` e publica indisponivel mais a marca de erro — o oposto deliberado
do caminho da chamada da Fase 6, e o motivo esta no KDoc: processo morto o sistema de telefonia
detecta, home congelada ninguem detecta exceto o usuario.

**Task 2 — a chave de onboarding e a ordem da marca** (commit `bfc015c`)

`onboarding_completed` entrou no repositorio de configuracoes **fora** de `ScreeningSettings`, como
as tres marcas de permissao das Fases 4-6, com quatro casos novos provando padrao falso, sobrevivencia
a recriacao, chave textual no disco, idempotencia e ausencia no retrato do caminho quente.

`OnboardingUiState` nao redefine padrao nenhum: reflete o retrato do repositorio, cujos valores de
fabrica ja sao os dos mockups. E por isso que **pular aplica os padroes corretos sem escrever nada**.

`pedirAgenda` e `pedirNotificacao` gravam a marca e SO ENTAO disparam, na sequencia, sem esperar a
escrita — o padrao que a tela de ativacao do discador ja usa em producao.

**Task 3 — efeito imediato sem botao salvar** (commit `36cc305`)

Uma funcao por item da secao 9, todas na mesma forma. Nao existe funcao de salvar, e a **ausencia** e
verificada por reflexao sobre os nomes de metodo. Retencao "nao guardar" grava E poda na mesma
corotina, nessa ordem; desligar o historico apenas grava. `dialerModeState` da Fase 6 e reusado, nao
reescrito.

## Provas de vermelho executadas

Todas sabotaram producao ja commitada e foram restauradas por edicao manual, nunca por `git checkout`
(licao de 06-02).

| Prova | Sabotagem | Resultado |
|---|---|---|
| Zero mentiroso | publicar `Loaded(0)` com historico desligado | 2 casos vermelhos (desligado e retencao) |
| Papel guardado | memorizar a resposta na primeira consulta | **5 de 5** casos de `RoleLiveStateTest` vermelhos |
| Fronteira do numero | publicar a ultima bloqueada sem mascara | 1 caso vermelho, o de privacidade |
| Marca no retorno | mover a gravacao para depois do disparo | 2 casos vermelhos, lista de eventos invertida |
| Efeito imediato | acumular a mudanca e gravar numa funcao nova | 6 casos vermelhos, incluindo o de ausencia de salvar |
| Poda ao nao guardar | remover a chamada de poda | exatamente 1 caso vermelho |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `AppContainer` ficou FORA dos donos de estado, inclusive da fabrica**

- **Encontrado em:** Task 1
- **Problema:** o criterio de aceite exige `grep -cE 'AppContainer'` **no maximo 1** e "so na
  fabrica". Com `fun factory(container: AppContainer)` sao inevitavelmente **duas** linhas — o import
  e a assinatura — porque `grep -c` conta LINHAS. A saida obvia, escrever o nome totalmente
  qualificado para dispensar o import, e proibida: ela colocaria o identificador do aplicativo como
  literal em Kotlin, que o Bloco 2 do script de invariantes reprova inclusive em comentario.
- **Correcao:** as fabricas recebem os colaboradores um por um. A montagem a partir do container
  passa a ser da rota que hospeda cada tela (planos 07-05 em diante). O resultado satisfaz os dois
  invariantes ao mesmo tempo e e mais forte que o pedido: `grep` devolve **0**, e o dono de estado
  inteiro pode ser construido num teste de JVM sem tocar disco, banco ou agenda.
- **Arquivos:** `ui/home/HomeViewModel.kt`, `ui/onboarding/OnboardingViewModel.kt`,
  `ui/settings/SettingsViewModel.kt`
- **Commits:** `9744caa`, `bfc015c`, `36cc305`

**2. [Rule 2 - Missing critical] `HomeViewModel` ganhou `dialerRoleAvailable` e `dialerOptedIn`**

- **Encontrado em:** Task 1
- **Problema:** o plano manda `HomeUiState` carregar `dialerMode: DialerModeState` e a secao 8 exige
  o aviso de "papel de discador perdido" na home, mas a lista de colaboradores do plano so tinha
  `dialerRoleHeld`. `dialerModeState(...)` precisa de quatro sinais: sem disponibilidade e sem a
  intencao gravada, o estado `ROLE_LOST` **nunca** seria produzido e o aparelho sem o papel seria
  rotulado com o estado errado.
- **Correcao:** duas funcoes de consulta a mais no construtor, e a funcao pura da Fase 6 reusada
  inteira. Nenhuma regra nova, nenhuma dependencia nova.
- **Commit:** `9744caa`

**3. [Rule 3 - Blocking] `@Suppress("TooManyFunctions")` local no repositorio de configuracoes**

- **Encontrado em:** Task 2
- **Problema:** a chave nova levou `DataStoreSettingsRepository` a 11 funcoes, exatamente o limite do
  detekt, e o build ficou vermelho.
- **Correcao:** supressao **local**, no ponto de uso, com o motivo em KDoc — nunca afrouxar o
  `detekt.yml` compartilhado, precedente das Fases 3 e 6.
- **Commit:** `bfc015c`

**4. [Rule 3 - Blocking] `SettingsViewModel` recebeu relogio injetado**

- **Encontrado em:** Task 3
- **Problema:** "nao guardar" tem corte nulo por desenho da Fase 3 (a politica nem grava), entao
  `cutoffUtcMillis` nao serve para podar o que ja existe. A poda precisa de um instante-limite.
- **Correcao:** `clock: () -> Long = System::currentTimeMillis`, que e convencao explicita do projeto
  desde a Fase 3 ("relogio injetado em toda regra dependente de tempo"), e torna a poda verificavel
  por valor exato em vez de por "foi chamada".
- **Commit:** `36cc305`

### Armadilha nova, do teste e nao do produto

O escopo de fundo do `runTest` usa o despachante **padrao**, que so entrega ao avancar o tempo
virtual. Com ele, `stateIn(WhileSubscribed)` nunca ligava o fluxo de origem e **11 de 15 casos ficaram
vermelhos por motivo falso**, todos reportando o valor inicial `Loading`. A correcao e coletar no
despachante NAO CONFINADO amarrado ao agendador do teste. Isto e prima da armadilha da Fase 4
(`backgroundScope` nao despachado por `advanceUntilIdle`) e merece ficar registrada: **um dono de
estado que publica por `stateIn` nao tem estado nenhum sem assinante.**

### Item fora de escopo registrado, nao corrigido

`OptionCard.kt:109` apareceu no detekt com `UnusedParameter` durante a Task 2. O arquivo e do plano
07-03, executado em paralelo na mesma onda, e ja estava commitado. Registrado em
`deferred-items.md` e **nao** corrigido — o parametro pertence a contrato de tela ainda nao ligado, e
quem sabe se ele deve ser consumido ou removido e o plano que o escreveu. O 07-03 fechou o achado
por conta propria antes da verificacao final desta fase.

## Verificacao final

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                      todos os invariantes OK
./gradlew koverLog                                                     96,6157%
./gradlew koverVerify                                                  BUILD SUCCESSFUL (piso 80)
```

- **698 testes JVM** no total (era 618 no fecho da Fase 6); **42 novos** neste plano.
- `git diff --stat` do `CallDecisionEngine.kt` **vazio** — o motor de decisao segue intocado, como o
  criterio da fase exige.
- Nenhuma permissao nova, nenhuma biblioteca nova, nenhum `exclude` novo do Kover, `app/build.gradle.kts`
  nao tocado (07-11 e o dono).
- Os donos de estado vivem em `ui.*`, fora do filtro do Kover; a chave nova entra em `settings.*`, que
  E medido, e os quatro casos novos de repositorio sustentam a cobertura.

## Requisitos: deliberadamente NAO marcados

O frontmatter do plano declara `[SCR-02, UIX-02, UIX-03, UIX-10]`. Nenhum deles foi marcado aqui, e
isso e decisao, nao esquecimento:

- **UIX-02, UIX-03, UIX-10** descrevem TELAS ("Home: status...", "Tela Protecao: ...", "Estados de
  carregamento e erro em todas as telas"). Este plano entrega os donos de estado e os tipos que elas
  vao consumir; **nenhuma tela existe ainda**. Marcar agora seria exatamente o estado falsamente
  positivo que o item 11 da secao 10.3 proibe, e o precedente e do 07-01, que deixou UIX-07 e UIX-11
  pendentes pelo mesmo motivo. Eles fecham nos planos 07-05 a 07-08, com as telas.
- **SCR-02** ja estava marcado por trabalho anterior; este plano fortalece a verificacao continua do
  papel, mas nao altera o estado do requisito.

## Self-Check: PASSED

Todos os 11 arquivos declarados existem no disco e os tres commits de tarefa existem no historico.

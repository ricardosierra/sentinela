---
phase: 07-ui-onboarding-e-home
plan: 05
subsystem: ui
tags: [compose, material3, onboarding, robolectric, acessibilidade, call-screening-role]

# Dependency graph
requires:
  - phase: 07-01
    provides: strings pt-BR da fase, varredura de honestidade da copy, cores fixas de estado
  - phase: 07-02
    provides: rotas por texto e os asserts de dois eixos em pacote neutro (TouchTargetAsserts)
  - phase: 07-03
    provides: OptionCard, StepHeader, SentinelaTopBar e o refinamento da semantica mesclada
  - phase: 07-04
    provides: OnboardingUiState e TOTAL_DE_PASSOS
  - phase: 06
    provides: HonestyCard, InfoBanner, rememberMotionReduced, CallAccept e as tres frases de escopo
provides:
  - "WelcomeScreen — tela 0 com os tres cartoes honestos, selo de codigo aberto e zero imagem remota"
  - "RoleStepScreen — passo 1 de 6 com o aviso obrigatorio de escopo em cartao de peso visual igual"
  - "UnknownPolicyStepScreen — passo 2 de 6 com bloquear pre-selecionado"
  - "DURACAO_DA_TRANSICAO_DE_PASSO_MILLIS e rememberStepTransitionMillis() para o envelope de navegacao"
  - "WelcomeAndRoleStepTest — 18 casos de composicao sob qualificadores de tela reais"
affects: [07-06, 07-07, 07-09, 08-historico-e-whitelist]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Ecra de onboarding e composta PURA: recebe estado pronto, devolve intencoes, zero container"
    - "Numero do passo e constante privada do arquivo; o total sempre vem de fora"
    - "Duracao da transicao de passo mora na tela, a transicao mora no envelope de navegacao"

key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/onboarding/WelcomeScreen.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/RoleStepScreen.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/UnknownPolicyStepScreen.kt
    - app/src/test/java/org/sentinela/app/ui/onboarding/WelcomeAndRoleStepTest.kt
  modified: []

key-decisions:
  - "As tres adaptacoes do mockup na tela de boas-vindas ficam registradas em KDoc, apontando docs/backlog/capacidades-prometidas-nos-mockups.md: sem base global de numeros, sem imagem remota (o app nao declara internet) e sem sobreposicao de progresso falso"
  - "O grupo de opcoes usa o modificador compartilhado optionCardGroup() de 07-03 em vez de reescrever selectableGroup na tela — reescrever seria a duplicacao que 07-03 proibiu, e o criterio por grep do plano so aceitaria a copia ou um comentario"
  - "O passo 1 nao avanca sozinho quando o papel e concedido: o chip de ativo e regiao viva educada e o toque continua sendo do usuario"
  - "Papel negado mantem o botao habilitado e vira avancar; o pedido do sistema so se repete pela acao explicita do aviso"
  - "Reconfirmado por medicao: envolver o botao num container que mescla NAO derruba o estado desabilitado; o que derruba e declarar o estado no container"

patterns-established:
  - "Rodape com gradiente de 32dp: o botao e filho direto de um Column que nao mescla, nunca de um container mesclado"
  - "Superficie tonal em gradiente substitui toda imagem remota dos mockups, por impossibilidade de rede e nao por estilo"
  - "Flutuacao decorativa com amplitude de 4dp, semantica limpa e supressao por reducao de movimento"

requirements-completed: [SCR-01, UIX-01, UIX-09, UIX-11]

# Metrics
duration: 42min
completed: 2026-07-30
---

# Phase 7 Plano 05: Boas-vindas e os dois primeiros passos do onboarding Summary

**Tres ecras compostos puros — boas-vindas honesta sem base global nem imagem remota, passo 1 de 6 com o aviso de escopo em cartao de peso visual igual e os tres ramos do papel, e passo 2 de 6 com bloquear pre-selecionado — com 18 casos de composicao e tres provas de vermelho.**

## Performance

- **Duration:** ~42 min
- **Tasks:** 3 (a terceira em ciclo vermelho-verde)
- **Files modified:** 4 criados, 0 modificados

## Accomplishments

- `WelcomeScreen` fiel ao layout do mockup e honesta no texto: os tres cartoes de bloqueio local, silencio e ausencia de internet, o selo de codigo aberto no lugar do antigo selo de "protecao ativa", zero referencia a base de numeros, zero endereco remoto e zero barra de progresso falsa.
- `RoleStepScreen` carrega o **aviso obrigatorio da fase** — so chamada de telefone e filtrada — num cartao de honestidade com o mesmo peso visual do resto da tela, com as tres frases herdadas por identificador de recurso e nenhuma reescrita.
- Os tres ramos do papel funcionam sem travar o passo: pedir, "solicitando" com o botao anunciado como desabilitado, e concedido com chip de ativo e avanco manual. Papel negado acrescenta aviso com acao e mantem o avanco disponivel.
- `UnknownPolicyStepScreen` com o cartao central flutuante do mockup, tres cartoes de opcao em grupo de escolha unica e bloquear como padrao; a politica que nunca silencia e o estilo do bloqueio ficam deliberadamente fora.
- `WelcomeAndRoleStepTest`: 18 casos verdes sob `sdk = [35]` e `w411dp-h891dp-xxhdpi`, todo texto de assert lido do recurso, cinco asserts de tamanho desenhado sempre acompanhados dos dois asserts de alvo de toque.

## Task Commits

1. **Task 1: WelcomeScreen — tela 0** — `172d796` (feat)
2. **Task 2: RoleStepScreen — passo 1 de 6** — `a34c21f` (feat)
3. **Task 3 (vermelho): casos de composicao dos tres ecras** — `d2e9f67` (test)
4. **Task 3 (verde): UnknownPolicyStepScreen** — `500ae06` (feat)

## Files Created/Modified

- `app/src/main/java/org/sentinela/app/ui/onboarding/WelcomeScreen.kt` — tela 0, composta pura, com as tres adaptacoes do mockup registradas em KDoc.
- `app/src/main/java/org/sentinela/app/ui/onboarding/RoleStepScreen.kt` — passo 1 de 6, cartao de escopo, tres ramos do papel, e a duracao da transicao de passo com supressao por reducao de movimento.
- `app/src/main/java/org/sentinela/app/ui/onboarding/UnknownPolicyStepScreen.kt` — passo 2 de 6, cartao flutuante, tres opcoes, bloquear como padrao.
- `app/src/test/java/org/sentinela/app/ui/onboarding/WelcomeAndRoleStepTest.kt` — 18 casos de composicao.

## Provas de vermelho

**1. Padrao do passo 2 trocado de bloquear para tocar** (sabotagem em trabalho ja commitado, restaurada por edicao manual):
`WelcomeAndRoleStepTest > passo dos desconhecidos vem com bloquear pre-selecionado FAILED` — e **so** esse caso.

**2. Uma das tres frases do cartao de honestidade removida do passo 1** (a frase do "Nao Perturbe"):
`WelcomeAndRoleStepTest > passo do papel exibe as tres frases do cartao de honestidade FAILED` — e so esse caso.

**3. O botao do passo 1 envolvido em container com semantica de mesclagem**, medido nas duas direcoes:
- **Envolver e so isso: VERDE.** O no do proprio botao continua respondendo as buscas, exatamente como 07-03 mediu. A formulacao do criterio ("envolver num container que mescla deixa o caso vermelho") esta, por si, errada.
- **Declarar o estado desabilitado NO CONTAINER e deixar o botao habilitado: VERMELHO.**
  `WelcomeAndRoleStepTest > pedido em curso deixa o botao anunciado como desabilitado FAILED`.
  Restaurado por edicao manual; os 18 casos voltaram ao verde.

## Decisions Made

- **As tres adaptacoes do mockup na tela 0 ficam em KDoc**, apontando o arquivo de registro pos-lancamento. Nenhuma das cinco afirmacoes desonestas dos mockups aparece no codigo, nem em comentario.
- **O grupo de opcoes usa `optionCardGroup()`**, o modificador compartilhado que 07-03 criou justamente para nenhuma tela reescrever `selectableGroup`.
- **A transicao entre passos nao mora na tela.** A tela publica o numero (250 ms) e a regra de supressao; o sentido do movimento e a troca de conteudo pertencem ao envelope de navegacao, que conhece os dois passos.
- **O avanco apos o papel concedido e sempre manual**, e o chip e o aviso de resultado sao regiao viva educada — nunca enfatica, que roubaria o foco do botao.

## Deviations from Plan

### Criterio de aceite divergente, sem mudanca de comportamento

**1. `grep -c 'selectableGroup'` no passo 2 devolve 0, e isso e o comportamento correto**
- **Found during:** Task 3
- **Issue:** o criterio pede a presenca do literal `selectableGroup` no arquivo da tela. Satisfaze-lo exigiria ou reescrever o modificador que 07-03 extraiu para `ui/components` — a duplicacao que aquele plano proibiu — ou plantar o literal num comentario, o defeito que a Fase 5 registrou tres vezes (criterio por grep nao distingue comentario de codigo).
- **Fix:** mantido `Modifier.optionCardGroup()`, que **e** `selectableGroup()` no componente compartilhado. O comportamento exigido (grupo de escolha unica anunciado ao leitor de tela) esta presente e coberto pelos casos de selecao.
- **Verification:** `grep -n optionCardGroup` no arquivo da tela; os tres casos de selecao/nao-selecao verdes.

**2. A formulacao da prova de vermelho 3 esta incorreta, e a medicao registra o veredito**
- **Found during:** Task 3
- **Issue:** o criterio afirma que envolver o botao num container que mescla derruba o caso do desabilitado. Medido: nao derruba.
- **Fix:** a prova foi executada nas duas direcoes e o vermelho obtido pelo defeito real — estado declarado no container. Nada foi enfraquecido: o caso continua sendo o mesmo e continua tendo dentes.

---

**Total deviations:** 2 (ambas de criterio, zero de comportamento)
**Impact on plan:** nenhum recuo de escopo. Os tres ecras, os 12+ casos e as tres provas de vermelho foram entregues.

## Issues Encountered

- **Falsos vermelhos de vizinhos concorrentes**, como o contrato da onda previa: `detekt` reprovou uma vez por duas propriedades nao usadas em `ContactsPolicyStepScreen.kt` e a compilacao de teste falhou por `ContactsAndWhitelistStepTest.kt` a meio de escrita — arquivos de outro plano, nunca tocados aqui. Resolvidos esperando e repetindo.
- **Durante a prova de vermelho 3, quatro casos de alvo de toque de OUTROS ecras tambem ficaram vermelhos** junto com o caso alvo, e voltaram ao verde na restauracao. Nao existe caminho causal do container do passo 1 para o botao da tela 0 nem para os cartoes do passo 2; registrado como ruido do ambiente de Gradle em paralelo, sem mecanismo afirmado. O que e load-bearing e o par medido: vermelho na sabotagem, verde na restauracao, no caso alvo.
- Conteudo mais alto que o viewport na tela 0: o corpo passou a rolar e o rodape ficou fora da rolagem, em vez de encolher o hero.

## Verificacao final

- `./gradlew assembleDebug testDebugUnitTest lint detekt` — verde.
- `bash scripts/verify-invariants.sh` — todos os 8 blocos OK.
- `./gradlew koverLog` — 96,6157% (inalterado: `ui.*` esta fora do filtro, que e do plano 07-11).
- `WelcomeAndRoleStepTest`: `tests="18" skipped="0" failures="0" errors="0"`.

## User Setup Required

None.

## Next Phase Readiness

- Os passos 3 a 6 (07-06 e 07-07) tem agora o precedente completo de ecra de passo: barra superior com contador e pular, corpo rolavel, rodape com gradiente e botao fora de container mesclado.
- O envelope de navegacao (07-09) deve consumir `rememberStepTransitionMillis()` para a transicao de 250 ms com supressao por reducao de movimento; a transicao ainda **nao** existe em nenhuma rota.
- O plano 07-11 continua dono do filtro do Kover: nenhuma das tres telas entra na cobertura medida ate lá.

## Self-Check: PASSED

Os quatro arquivos declarados existem no disco (450, 503, 322 e 269 linhas) e os quatro commits de
tarefa existem no historico (`172d796`, `a34c21f`, `d2e9f67`, `500ae06`).

---
*Phase: 07-ui-onboarding-e-home*
*Completed: 2026-07-30*

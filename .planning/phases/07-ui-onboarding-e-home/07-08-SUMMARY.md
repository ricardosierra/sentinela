---
phase: 07-ui-onboarding-e-home
plan: 08
subsystem: ui-home
tags: [compose, home, acessibilidade, privacidade, estados-degradados]
requires:
  - "07-03: SentinelaTopBar, SentinelaBottomBar, BottomBarItem"
  - "07-04: HomeUiState, StatValue, LastBlockedUi, reasonLabelRes"
  - "07-01: StatusAttention/OnStatusAttention/StatusBlocked, 269 chaves pt-BR"
  - "Fase 6: InfoBanner, PhoneMask, TouchTargetAsserts"
provides:
  - "StatusHeroCard: cartao principal com interruptor de PREFERENCIA e estado no proprio no"
  - "StatCard: estatistica fechada por tipo, sem sobrecarga numerica"
  - "LastBlockedCard: ultima bloqueada, numero JA mascarado, zero rotulo de risco"
  - "QuickActionRow: atalho com altura exigida e motivo textual quando desabilitado"
  - "HomeScreen: a home inteira, com os oito estados degradados e a precedencia de avisos"
  - "relativeTimeLabel: tempo relativo por plurais reais"
affects:
  - "07-10: a rota monta a home e faz a reconsulta viva do papel na retomada"
  - "07-11: fechamento da fase, cobertura e evidencia"
tech-stack:
  added: []
  patterns:
    - "estado de controle sempre no no do PROPRIO controle, nunca no container que o envolve"
    - "ordem de travessia declarada por bloco (isTraversalGroup + traversalIndex)"
    - "varredura da arvore semantica como prova, nas DUAS arvores (mesclada e nao mesclada)"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/home/StatusHeroCard.kt
    - app/src/main/java/org/sentinela/app/ui/home/StatCard.kt
    - app/src/main/java/org/sentinela/app/ui/home/LastBlockedCard.kt
    - app/src/main/java/org/sentinela/app/ui/home/QuickActionRow.kt
    - app/src/main/java/org/sentinela/app/ui/home/HomeScreen.kt
    - app/src/test/java/org/sentinela/app/ui/home/HomeScreenStateTest.kt
    - app/src/test/java/org/sentinela/app/ui/home/HomePrivacyTest.kt
  modified:
    - app/src/main/res/values/strings.xml
decisions:
  - "o interruptor alterna a preferencia de protecao; o papel e somente-leitura no aviso separado"
  - "o zero mentiroso e impossivel por TIPO e provado por varredura, nao por convencao"
  - "botao de correcao do papel some quando o aparelho nao oferece o papel"
  - "teto de dois avisos, com excedente virando uma linha que leva a Protecao"
metrics:
  tasks: 3
  tests_added: 29
  duration: ~1h
  completed: 2026-07-30
---

# Phase 07 Plan 08: Home com os oito estados degradados — Summary

A home inteira em Compose puro: cartao principal cuja chave alterna a **preferencia** de protecao
(nunca o papel do sistema), estatisticas cujo valor e um tipo fechado — o que torna o "zero
mentiroso" impossivel de renderizar em vez de proibido por convencao —, ultima bloqueada exibida
**apenas mascarada**, atalhos, barra inferior e os **oito estados degradados** da secao 8 do contrato
de interface, com precedencia de avisos e teto de dois.

## O que foi entregue

| Arquivo | Papel |
|---|---|
| `StatusHeroCard.kt` | cartao principal; cores de significado por literal fora do esquema; interruptor como no proprio com papel e descricao de estado; ponto de estado com pulsacao suprimida por reducao de movimento e limpo da arvore |
| `StatCard.kt` | `when` exaustivo sobre `StatValue`; nenhum parametro numerico na assinatura; esqueleto tonal no carregando; descricao de conteudo = rotulo + valor |
| `LastBlockedCard.kt` | recebe `maskedNumber` e nao mascara nada; motivo REAL da decisao; cartao inteiro leva ao historico; `relativeTimeLabel` com granularidade agora/min/h/ontem/data |
| `QuickActionRow.kt` | `requiredHeight(72dp)`; motivo textual no proprio no quando desabilitado |
| `HomeScreen.kt` | composta pura de 11 blocos; oito estados; precedencia e teto de avisos; tela inteira rolavel; ordem de travessia declarada |

## As tres decisoes delicadas, como ficaram

**1. O interruptor alterna a preferencia, nunca o papel.** Revogar papel encerra o processo (medido
tres vezes na Fase 6) e o aplicativo nem pode revoga-lo. O papel e estado somente-leitura no aviso,
com botao que abre o seletor do sistema.

**2. O zero e impossivel nos tres estados de ausencia.** `StatValue` nao carrega numero em
`Unavailable`/`Loading`, e o teste varre a arvore semantica inteira afirmando que o caractere zero
nao existe em nenhum no — nem em texto, nem em descricao de conteudo, nem em descricao de estado.
Afirmar apenas que o traco aparece seria fraco: a tela poderia exibir os dois.

**3. A ultima bloqueada e mascarada, e a prova e por composicao.** `HomePrivacyTest` produz a mascara
pela mascara UNICA do aplicativo sobre `+5511912341234`, compoe a home e varre TODOS os nos das duas
arvores exigindo que a sequencia completa de digitos nao exista. Varredura de fonte nao serviria.

## Ponto de risco (a): o interruptor dentro do cartao

O refinamento de 07-03 e 07-07 se confirmou na tela mais dificil da fase: **envolver o controle nao
derruba nada; declarar o estado no container derruba.** O arquivo do cartao nao tem uma unica
ocorrencia de mesclagem de descendentes, e a descricao de estado mora no modificador do proprio
`Switch`. E o caso do interruptor foi escrito nas DUAS arvores — a nao mesclada mede a estrutura, a
mesclada mede o que o leitor de tela recebe.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Funcionalidade critica ausente] Seis chaves de texto novas**
- **Found during:** Task 1 e 2
- **Issue:** o contrato pede tempo relativo por plurais, uma linha de excedente de avisos, a leitura
  da ultima bloqueada e uma acao de "tentar de novo"; nenhuma existia entre as 269 chaves de 07-01.
  Reaproveitar `onboarding_role_retry` ("Conceder agora") como acao de nova tentativa de leitura
  seria texto errado na tela.
- **Fix:** `time_now`, `time_yesterday`, `time_date_short_pattern`, plurais `time_minutes_ago` e
  `time_hours_ago`, `dashboard_more_warnings`, `dashboard_last_blocked_description`, `action_retry`.
  **A contagem de 07-01 subiu de 269 para 275 `<string name=` mais tres `<plurals>`** (as duas de
  tempo novas e a de limpar historico). Os plurais sao plurais de verdade, nunca concatenacao.
- **Commit:** `0d4aefd`, `e5c9bd2`

**2. [Rule 1 - Bug] Nome totalmente qualificado do tema no teste derrubava o Bloco 2**
- **Found during:** verificacao final
- **Issue:** `org.sentinela.app.ui.theme.SentinelaTheme(...)` escrito por extenso e literal do
  identificador do aplicativo em Kotlin — o invariante o rejeita mesmo fora de producao. Quinta
  encarnacao da armadilha registrada desde a Fase 3.
- **Fix:** import normal.
- **Commit:** `3a724db`

**3. [Rule 3 - Bloqueio] Icone com sobrecarga espelhada e limites do detekt**
- Icone de lista trocado pela variante espelhada (o build reprova a antiga por depreciacao);
  `@file:Suppress("LongParameterList", "TooManyFunctions")` no arquivo da home, no precedente de
  07-02 e 07-06 — nunca afrouxando o `detekt.yml` compartilhado.

## As quatro provas de vermelho

Todas executadas sobre codigo **JA COMMITADO** e restauradas por edicao manual, nunca por
`git checkout` (licao de 06-02).

**PROVA 1 — zero mentiroso no ramo indisponivel.** Ramos `Unavailable` e `Loading` do `StatCard`
passaram a devolver `"0"`:
```
HomeScreenStateTest > carregando nao renderiza zero ... FAILED
HomeScreenStateTest > historico desligado nao renderiza zero ... FAILED
HomeScreenStateTest > falha de leitura nao renderiza zero ... FAILED
24 tests completed, 3 failed
```
Os tres casos proibidos ficaram vermelhos e o caso do zero VERDADEIRO seguiu verde — exatamente o
par que justifica te-los lado a lado. Registro extra: no ramo do carregamento o zero foi pego pela
**descricao de conteudo**, nao por texto visivel; um assert restrito a texto teria passado.

**PROVA 2 — fronteira do numero.** `maskedNumber` recebeu o numero completo:
```
HomePrivacyTest > a home exibe o texto mascarado da ultima bloqueada FAILED
HomePrivacyTest > nenhum no da arvore contem a sequencia completa de digitos FAILED
HomePrivacyTest > a descricao de conteudo ... carrega mascara motivo e tempo FAILED
5 tests completed, 3 failed
```

**PROVA 3 — interruptor no no mesclado.** A descricao de estado saiu do `Switch` e passou a ser
declarada num container do cartao que mescla descendentes:
```
HomeScreenStateTest > o interruptor e anunciado com papel de interruptor e descricao de estado FAILED
HomeScreenStateTest > a descricao de estado do interruptor difere entre ligado e desligado FAILED
24 tests completed, 2 failed
```
Quarta medicao do mesmo achado, agora na home: o no do interruptor continuou existindo e alcancavel,
e o que se perdeu foi o **estado** — ele ficou num ancestral onde ninguem pergunta.

**PROVA 4 — botao de correcao sem papel disponivel.** A condicional de disponibilidade foi removida:
```
HomeScreenStateTest > papel indisponivel no aparelho nao exibe botao de correcao FAILED
24 tests completed, 1 failed
```

## Verification

- `./gradlew assembleDebug lint detekt` — verde
- `./gradlew testDebugUnitTest --tests "*HomeScreenStateTest" --tests "*HomePrivacyTest"` — verde,
  **29 casos** (24 + 5), acima do minimo de 20
- `scripts/verify-invariants.sh` — todos os blocos verdes **para os arquivos deste plano**

## Deferred Issues (fora do escopo deste plano)

- `app/src/main/java/org/sentinela/app/ui/settings/SettingsScreen.kt:730` usa o identificador do
  aplicativo por extenso em Kotlin e derruba o Bloco 2 do verificador; e arquivo do plano 07-09, em
  execucao concorrente, e **nao foi tocado**. Quatro casos de `ProtectionScreenTest` (07-09) tambem
  estavam vermelhos no fim desta execucao, pelo mesmo motivo de concorrencia.
- `./gradlew koverLog` nao pudo ser lido: a execucao paralela sobre o mesmo `app/build` derrubou o
  arquivo binario de resultados (falha de ambiente ja registrada em 07-01). Cobertura e gate sao do
  plano 07-11.

## Self-Check: PASSED

Sete arquivos criados conferidos no disco; quatro commits (`0d4aefd`, `e5c9bd2`, `daed73b`,
`3a724db`) conferidos no historico. `HomeScreen.kt` com 541 linhas e `HomeScreenStateTest.kt` com
461, acima dos minimos de 220 e 180.

---
phase: 05-triagem-telecom-modo-filtro
plan: 01
subsystem: domain+data+settings
tags: [scr-12, decision-engine, history, datastore, eng-01]
requires:
  - CallDecisionEngine (Fase 2)
  - BlockedCallRepository + Room v1 (Fase 3)
  - DataStoreSettingsRepository (Fase 3)
provides:
  - RepeatedCallLookup
  - REPEATED_CALL_WINDOW_MILLIS
  - DecisionReason.REPEATED_CALL
  - ScreeningSettings.repeatedCallBypassEnabled
  - BlockedCallRepository.hasRecentBlock
  - BlockedCallDao.countBlockedSince
affects:
  - UnknownCallScreeningService (planos 05-03+, precisa alimentar repeatedCall)
  - Tela de configurações (Fase 7, interruptor da exceção)
tech-stack:
  added: []
  patterns:
    - "Regra de decisão nova nasce no motor puro, nunca no Service"
    - "Consulta de histórico degrada para LOOKUP_FAILED e nunca bloqueia por falha"
    - "Teste estrutural que lê fonte do disco exige o diretório declarado como input do Gradle"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/domain/RepeatedCall.kt
  modified:
    - app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt
    - app/src/main/java/org/sentinela/app/domain/DecisionReason.kt
    - app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/main/java/org/sentinela/app/data/local/db/BlockedCallDao.kt
    - app/src/main/java/org/sentinela/app/data/local/BlockedCallRepository.kt
    - app/src/main/java/org/sentinela/app/data/local/RoomBlockedCallRepository.kt
    - app/src/main/java/org/sentinela/app/SentinelaApp.kt
    - app/src/main/java/org/sentinela/app/ui/MainActivity.kt
    - app/build.gradle.kts
    - app/src/test/java/org/sentinela/app/domain/DecisionMatrixTest.kt
    - app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt
    - app/src/test/java/org/sentinela/app/domain/DecisionReasonTest.kt
    - app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt
    - app/src/test/java/org/sentinela/app/settings/AppOpenCounterTest.kt
    - app/src/test/java/org/sentinela/app/data/local/FakeBlockedCallDao.kt
    - app/src/test/java/org/sentinela/app/data/local/RoomBlockedCallRepositoryTest.kt
decisions:
  - "Janela da chamada repetida: 5 minutos, constante nomeada REPEATED_CALL_WINDOW_MILLIS"
  - "Corte da janela é INCLUSIVO (>=), espelho coerente do corte estrito (<) da poda"
  - "Nenhum índice novo em blocked_call: índice mudaria o schema e exigiria migração v2"
  - "hasRecentBlock é a única exceção ao contrato de propagar exceção do DAO"
  - "Contagem de abertura migrou de Application.onCreate para MainActivity.onCreate"
metrics:
  duration: ~50min
  tasks: 3
  completed: 2026-07-29
---

# Phase 5 Plan 01: Motor, histórico e contador Summary

Exceção de chamada repetida (SCR-12) entregue no motor puro com janela nomeada de 5 minutos,
alimentada por uma contagem nova sobre o histórico Room existente — sem schema v2, sem
armazenamento novo — mais a correção do contador de aberturas, que deixou de contar starts de
processo disparados pelo sistema.

## O que foi construído

**Task 1 — regra SCR-12 no motor.** `RepeatedCall.kt` traz `RepeatedCallLookup { HIT, MISS,
LOOKUP_FAILED }` e `REPEATED_CALL_WINDOW_MILLIS = 5L * 60L * 1000L`, sem nenhum import da
plataforma. `CallDecisionEngine.decide` ganhou `repeatedCall: RepeatedCallLookup =
RepeatedCallLookup.MISS` como último parâmetro e um nível novo de precedência **entre** a
whitelist e o gatilho de fallback — a precedência passou de 7 para 8 níveis. A exceção só
produz `CallDecision.Allow(DecisionReason.REPEATED_CALL)`: ela nunca transforma um resultado
permissivo em bloqueio, e `LOOKUP_FAILED` cai na política normal.
`ScreeningSettings.repeatedCallBypassEnabled` nasce `true` e persiste na chave
`repeated_call_bypass`.

**Task 2 — consulta de bloqueio recente.** `BlockedCallDao.countBlockedSince` conta registros
do mesmo `number_e164` a partir do corte, com `>=` (registro exatamente no limite conta como
dentro da janela). `RoomBlockedCallRepository.hasRecentBlock` calcula o corte a partir da
constante nomeada, roda em `withContext(io)` e envolve a chamada em `runCatching`: falha vira
`LOOKUP_FAILED`, nunca propaga. Número nulo ou em branco responde `MISS` sem tocar no banco.
Schema continua v1 — `app/schemas/.../` segue com um único arquivo.

**Task 3 — contador de aberturas.** `SentinelaApp` não conta mais nada; a contagem foi para
`MainActivity.onCreate`, guardada por `savedInstanceState == null` para rotação de tela não
contar duas vezes.

## Verificação

- Suíte JVM completa verde: **343 casos de teste**, `koverLog` em **96,8254%**.
- `lint`, `detekt` e `scripts/verify-invariants.sh` limpos para os arquivos deste plano
  (`== todos os invariantes OK ==`).
- `DecisionMatrixTest` ganhou **12 casos novos** (`RepeatedCallMatrixTest`), com tabela escrita
  à mão, cobrindo os 8 comportamentos exigidos mais precedência de saída, proteção desligada,
  privado e fallback.
- `RoomBlockedCallRepositoryTest` ganhou **8 testes novos** com relógio fixo em
  `1_700_000_000_000L`.
- `DataStoreSettingsRepositoryTest`: default `true` em arquivo vazio + round-trip de `false`.
- `DecisionReasonTest`: contagem travada em 10 e assert novo de que nenhum reason code contém
  dígito.

### Prova de vermelho 1 — regra do bypass

Invertendo a condição para `repeatedCall != RepeatedCallLookup.HIT`:

```
RepeatedCallMatrixTest > [repetida com bypass ligado toca em vez de bloquear] FAILED
RepeatedCallMatrixTest > [sem repeticao segue a politica de desconhecidos] FAILED
RepeatedCallMatrixTest > [falha da consulta de historico equivale a ausencia de repeticao] FAILED
RepeatedCallMatrixTest > [bypass entra antes do fallback por falha de consulta local] FAILED
RepeatedCallMatrixTest > [bypass nunca transforma um allow em bloqueio] FAILED
RepeatedCallMatrixTest > [repetida de numero invalido tambem toca] FAILED
12 tests completed, 6 failed
```

Restaurado e reconfirmado verde.

### Prova de vermelho 2 — corte da janela

Trocando `>=` por `>` no SQL e no fake:

```
RoomBlockedCallRepositoryTest > registro exatamente no limite da janela ainda conta como repeticao FAILED
26 tests completed, 1 failed
```

Restaurado e reconfirmado verde.

### Prova de vermelho 3 — guarda de rotação (não pedida, feita mesmo assim)

Trocando `if (savedInstanceState == null)` por `if (true)`:

```
AppOpenCounterTest > a contagem de abertura vive na Activity e nao na criacao do processo FAILED
7 tests completed, 1 failed
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Bloqueio] Detekt `MatchingDeclarationName` em `RepeatedCall.kt`**
- **Found during:** Task 1
- **Issue:** o plano fixa o nome do arquivo `RepeatedCall.kt`, mas o único tipo de topo é
  `RepeatedCallLookup`; o detekt derrubou o build.
- **Fix:** `@file:Suppress("MatchingDeclarationName")` com comentário explicando que o arquivo
  agrupa o enum e a constante da janela, que só fazem sentido juntos.
- **Commit:** cd49f78

**2. [Rule 3 - Bloqueio] Detekt `LongParameterList`/`LongMethod` na tabela parametrizada**
- **Found during:** Task 1
- **Issue:** um teste parametrizado com 7 parâmetros de construtor (e depois uma factory com 6)
  estoura os limites do `detekt.yml` compartilhado.
- **Fix:** a linha da tabela virou a data class `RepeatedCallCase`, com defaults descrevendo o
  cenário base, e `cases()` foi partida em `casosDoBypass()` + `casosDePrecedencia()`. Nenhuma
  supressão e nenhum afrouxamento de regra — a tabela continua escrita à mão.
- **Commit:** cd49f78

**3. [Rule 2 - Correção] Fonte declarado como input do Gradle**
- **Found during:** Task 3
- **Issue:** o teste estrutural do contador lê `SentinelaApp.kt` e `MainActivity.kt` do disco;
  pela lição da Fase 3, teste que lê arquivo sem input declarado pode ir UP-TO-DATE e dar falso
  verde.
- **Fix:** `inputs.dir("src/main/java")` acrescentado ao bloco `tasks.withType<Test>` que já
  declarava `schemas/`.
- **Commit:** 166b41a

**4. [Rule 3 - Bloqueio] Nome de variável privada em maiúsculas**
- **Found during:** Task 2
- **Issue:** `private val NUMERO` violou `VariableNaming` do detekt.
- **Fix:** renomeada para `numeroDeTeste`.
- **Commit:** cf96da7

### Fora de escopo (não tocado)

Durante a execução, `detekt` acusou `LongParameterList` em
`app/src/test/java/org/sentinela/app/telecom/CallResponseFactoryTest.kt` e execuções pontuais de
`lint`/`testDebugUnitTest` falharam em `ScreenedCallFactoryTest`. Ambos pertencem ao plano 05-02,
executado em paralelo por outro agente — deixados intactos de propósito.

## Pendências para os próximos planos

- O Service (planos 05-03+) precisa chamar `hasRecentBlock` dentro do mesmo timeout interno e
  passar o resultado como último argumento de `decide`.
- A tela de configurações (Fase 7) precisa expor o interruptor de `repeatedCallBypassEnabled`.
- `koverVerify` volta a ser cobrado no plano 05-07.

## Self-Check: PASSED

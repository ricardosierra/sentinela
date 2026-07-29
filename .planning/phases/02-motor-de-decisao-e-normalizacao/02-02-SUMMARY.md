---
phase: 02-motor-de-decisao-e-normalizacao
plan: 02
subsystem: domain
tags: [testes, motor-de-decisao, privacidade, cobertura]
requires:
  - org.sentinela.app.domain.CallDecisionEngine
  - org.sentinela.app.settings.ScreeningSettings
provides:
  - DecisionMatrixTest (matriz parametrizada 48 casos)
  - DecisionEdgeCasesTest (privado, invalido, fallback)
  - DecisionReasonTest (DEC-04)
affects:
  - app/src/test/java/org/sentinela/app/domain/
tech-stack:
  added: []
  patterns:
    - "JUnit4 @RunWith(Parameterized::class) com produto cartesiano gerado programaticamente"
    - "Tabela esperada escrita a mao, nunca derivada da implementacao sob teste"
key-files:
  created:
    - app/src/test/java/org/sentinela/app/domain/DecisionMatrixTest.kt
    - app/src/test/java/org/sentinela/app/domain/DecisionReasonTest.kt
  modified:
    - app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt
decisions:
  - "A matriz usa uma tabela esperada escrita a mao (expectedDecision) em vez de reimplementar apply()/block() — se o motor mudar, o teste falha em vez de acompanhar o bug"
  - "Casos nao parametrizados foram para uma segunda classe DecisionEdgeCasesTest no mesmo arquivo, ja que JUnit4 aceita um unico runner por classe"
  - "DecisionReasonTest trava a contagem em 9 entradas — reason code novo so entra com revisao de privacidade explicita"
  - "Com ContactLookup.UNAVAILABLE + WhitelistLookup.HIT a whitelist vence: o if de falha vem depois do hit de whitelist no motor. Comportamento agora e contratual, coberto por teste"
metrics:
  duration: ~12min
  completed: 2026-07-29
  tasks: 2
  files: 3
  coverage: 94.74% (application line coverage, koverLog)
---

# Phase 02 Plano 02: Cobertura da Matriz de Decisao Summary

Matriz parametrizada de 48 combinacoes origem x politica x modo de bloqueio x ocultacao de
historico, mais 13 casos de borda e a prova de que nenhum reason code carrega dado pessoal.

## O que foi feito

**Task 1 — `DecisionMatrixTest.kt`** (commit `f907fdf`)

- `@RunWith(Parameterized::class)` com os parametros gerados por produto cartesiano de
  `CallOrigin.entries` x `OriginPolicy.entries` x `BlockMode.entries` x `{true,false}` = **48 casos**,
  todos verdes. Nenhuma lista copiada a mao.
- Helper `expectedDecision(policy, blockMode, hideLog, reason)` e a tabela esperada escrita
  independentemente do motor — ele nao le `apply()`/`block()`.
- Cobre explicitamente que `SILENT_VOICEMAIL` vence `hideFromNativeCallLog` (voicemail em ambos
  os valores do flag) e que `REJECT` + `hide=false` cai em `Reject` simples.
- `DecisionEdgeCasesTest` na mesma unidade de arquivo, **13 casos**: numero invalido nas 4
  politicas (reason `INVALID_NUMBER`), privado na sub-matriz de bloqueio completa + o caso
  permitido, e os 4 casos de fallback (2 gatilhos x `FallbackPolicy`).

**Task 2 — reason codes e precedencia** (commit `522de6f`)

- `DecisionReasonTest.kt`: todo `code` casa `Regex("[a-z_]+")` (sem digito, acento, espaco ou
  maiuscula), nenhum vazio, todos unicos, exatamente 9 entradas.
- `CallDecisionEngineTest.kt` passou de 24 para **29 testes**, com os 5 casos de precedencia que
  faltavam: saida vence protecao desligada, saida vence privado, protecao off vence privado,
  politica de contato (BLOCK) vence whitelist (RING), e contato HIT vence
  `WhitelistLookup.LOOKUP_FAILED`.

## Verificacao

| Suite | tests | failures | errors |
|-------|-------|----------|--------|
| `DecisionMatrixTest` | 48 | 0 | 0 |
| `DecisionEdgeCasesTest` | 13 | 0 | 0 |
| `DecisionReasonTest` | 3 | 0 | 0 |
| `CallDecisionEngineTest` | 29 | 0 | 0 |

- `./gradlew detekt` → BUILD SUCCESSFUL
- `./gradlew koverLog` → **application line coverage: 94.7368%** (gate de 80% do plano 02-05 folgado)
- Nenhum `import android` e nenhum Robolectric nos arquivos novos — JVM pura.

## Deviations from Plan

None - plan executed exactly as written.

Observacao operacional (nao e desvio de escopo): as duas primeiras invocacoes do Gradle falharam
em `compileDebugUnitTestKotlin` por causa de `app/src/test/java/org/sentinela/app/phone/TestMetadataSentinelTest.kt`,
arquivo em edicao pelo plano 02-01 concorrente. Conforme instruido, aguardei e repeti a execucao
em vez de tocar em arquivos fora de `domain/`. Nenhum arquivo de build foi modificado por este plano.

## Deferred Issues

Nenhum.

## Self-Check: PASSED

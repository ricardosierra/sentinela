---
phase: 01-fundacao-compilavel
plan: 02
subsystem: qualidade-e-invariantes
tags: [invariantes, privacidade, permissoes, design-tokens, testes]
requires: []
provides:
  - "scripts/verify-invariants.sh — gate reexecutavel de manifest, rebranding e dominio puro"
  - "ThemeTokensTest — trava JVM dos tokens Silent Guardian e do wiring do darkColorScheme"
  - "Matriz OriginPolicy x origem fechada no CallDecisionEngineTest"
affects:
  - "Phase 4 (READ_CONTACTS) e Phase 6 (ROLE_DIALER) — o script FALHA ate a permissao ser autorizada na fase dela"
tech-stack:
  added: []
  patterns:
    - "Checagem de permissao por ALLOWLIST sobre o manifest MERGEADO, nunca por cardinalidade sobre o fonte"
    - "Teste de design token em JVM pura (Color e value class), sem Robolectric"
key-files:
  created:
    - scripts/verify-invariants.sh
    - app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/theme/Theme.kt
    - app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt
decisions:
  - "POST_NOTIFICATIONS e DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION formam a allowlist da Phase 1; a segunda e injetada pelo androidx-core e nao e removivel"
  - "DarkColors passou de private para internal — menor superficie possivel para o teste assertar o wiring"
  - "Bloco 4 (detekt/lint) e informativo nesta fase: reporta skip quando o relatorio nao existe; o gate real e o plano 03"
metrics:
  tasks: 3
  tests_total: 28
  completed: 2026-07-29
---

# Phase 01 Plano 02: Invariantes Verificaveis Summary

Converteu os criterios 3, 4 e 5 da fase — antes verificados por greps ad-hoc digitados a mao — em tres artefatos reexecutaveis: um script de invariantes que falha quando alguem antecipa permissao de fase futura, um teste JVM dos 26 tokens Silent Guardian, e o fechamento da matriz `OriginPolicy` x origem no motor de decisao.

## O que foi feito

**Task 1 — `scripts/verify-invariants.sh` (commit `3337952`)**
Script executavel, `set -uo pipefail` (sem `set -e`, que abortaria em `grep -c` com resultado 0), acumulando falhas e saindo 1 no final. Quatro blocos:

1. **PRV-01 / manifest MERGEADO** (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`): zero `INTERNET`; conjunto real de `uses-permission` comparado contra allowlist literal; bloqueio ruidoso de `READ_CONTACTS|READ_CALL_LOG|READ_PHONE_STATE|READ_SMS|CALL_PHONE|BIND_INCALL_SERVICE|SYSTEM_ALERT_WINDOW`; presenca de `BIND_SCREENING_SERVICE` e da action `android.telecom.CallScreeningService`.
2. **UIX-12 / rebranding**: nenhum `org.sentinela.app` literal em Kotlin fora de package/import, nenhuma string hardcoded, nenhuma `Color(0x` fora de `ui/theme`, `sentinelaApplicationId` usado 3x, `app_name` presente.
3. **Criterio 4 / dominio puro**: nenhum `import android` em `domain/`.
4. **QLT-02**: detekt/lint reportados quando os XMLs existem, `skip:` quando ausentes.

**Teste negativo obrigatorio (executado e revertido):** injetado `<uses-permission android:name="android.permission.READ_CONTACTS" />` no manifest fonte + `assembleDebug`. O script saiu com codigo **1** disparando DOIS checks independentes (allowlist e bloqueio de fase futura). Edicao revertida com `git checkout`; `git diff app/src/main/AndroidManifest.xml` vazio ao final.

**Task 2 — `ThemeTokensTest` (commit `1b98497`)**
4 metodos `@Test` em JUnit4 puro, sem Robolectric e sem `@Config`. Assertam os hex dos tokens de superficie e de acento, e que `DarkColors` consome os tokens (surface, onSurface, surfaceContainerLowest/Highest, primary, error, outline) em vez de literais soltos. `Theme.kt` alterado em exatamente 1 linha (`private` -> `internal val DarkColors`); `Color.kt` intocado.

Os 20 hex de `Color.kt` foram conferidos contra `docs/design/DESIGN.md`: **nenhuma divergencia** — todos os valores assertados existem no design system.

**Task 3 — matriz de precedencia (commit `8715235`)**
4 `@Test` novos preenchendo as lacunas reais (20 -> 24 no arquivo): contato `NEVER_SILENCE` -> `Allow(CONTACT)`; whitelist `RING` -> `Allow(PERSONAL_WHITELIST)`; whitelist `BLOCK` -> `BlockWithoutTrace(PERSONAL_WHITELIST)`; desconhecido `NEVER_SILENCE` -> `Allow(UNKNOWN_NUMBER)`. Nenhum caso duplicado — o unico ja coberto era whitelist `NEVER_SILENCE`, que e o default e ja tinha teste (`whitelist permite por padrao`). Motor, `CallDecision`, `DecisionReason` e `ScreeningSettings` intocados.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Padrao `<issue` casava a raiz `<issues>` do relatorio de lint**
- **Found during:** Task 1
- **Issue:** O literal do plano (`grep -c "<issue"`) contava o elemento raiz `<issues format="6">` de um relatorio VAZIO, produzindo falso positivo permanente.
- **Fix:** Padrao passou a ser `"<issue "` (com espaco), que so casa o elemento de issue real.
- **Commit:** `3337952`

**2. [Rule 1 - Bug] `grep -c || echo 0` duplicava a contagem**
- **Found during:** Task 1
- **Issue:** `grep -c` imprime `0` **e** sai com codigo 1 quando nao ha match, entao o `|| echo 0` acrescentava um segundo `0` — a variavel virava `"0\n0"` e o `[ -eq ]` quebrava com `integer expression expected`. E a mesma armadilha que motivou a proibicao de `set -e` no plano, num lugar que o plano nao previu.
- **Fix:** Contagem via atribuicao (`n=$(grep -c ...)`), que descarta o status de saida, com fallback `${n:-0}`.
- **Commit:** `3337952`

**3. [Rule 3 - Blocking] Corrida com o build do plano 01-01 apagando `app/build/reports/`**
- **Found during:** Task 1
- **Issue:** O agente do plano 01-01 rodava `assembleDebug` em paralelo, apagando os XMLs de relatorio entre o `[ -f ]` e o `grep` — o script falhava por motivo inexistente.
- **Fix:** Checagem de relatorio isolada em `count_in_report()`, que retorna status 1 (-> `skip:`) quando o arquivo nao existe, em vez de tratar ausencia como issue.
- **Commit:** `3337952`

Nenhum desvio de escopo: nenhum arquivo fora de `files_modified` foi tocado (o `app/build.gradle.kts` e os mipmaps do plano 01-01 foram apenas lidos por grep).

## Verificacao

```
bash scripts/verify-invariants.sh   -> exit 0, "== todos os invariantes OK =="
(com READ_CONTACTS injetado)        -> exit 1, 2 FAIL
./gradlew testDebugUnitTest detekt  -> BUILD SUCCESSFUL
tests="24" failures="0" errors="0"  (CallDecisionEngineTest)
tests="4"  failures="0" errors="0"  (ThemeTokensTest)
Total: 28 testes (eram 20), detekt 0 issues, lint 0 issues
```

## Observacoes para as proximas fases

- **Phase 4 (READ_CONTACTS)** e **Phase 6 (ROLE_DIALER/BIND_INCALL_SERVICE/CALL_PHONE)** vao FAZER o script falhar de proposito. O procedimento correto e: atualizar `docs/PERMISSOES.md`, depois mover a permissao da lista de bloqueio para a `ALLOWLIST` do script no mesmo commit. Nunca afrouxar o script antes da doc.
- O total de 28 testes ficou abaixo dos 29 que o criterio do plano projetava (20 + ThemeTokensTest + 4): whitelist `NEVER_SILENCE` ja estava coberta pelo teste de default, entao 4 casos novos de precedencia, nao 5.

- Requisitos marcados como completos: **PRV-01, UIX-08, UIX-12**. **QLT-02 foi deliberadamente deixado em aberto** — o Bloco 4 do script apenas reporta detekt/lint, o gate que faz o build falhar e escopo do plano 03 (que declara o mesmo requisito).

## Self-Check: PASSED

- FOUND: scripts/verify-invariants.sh (executavel)
- FOUND: app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt
- FOUND: commit 3337952, 1b98497, 8715235

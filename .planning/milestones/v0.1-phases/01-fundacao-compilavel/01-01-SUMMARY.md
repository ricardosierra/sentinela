---
phase: 01-fundacao-compilavel
plan: 01
subsystem: infra
tags: [android, gradle, lint, agp9, mipmap]

requires:
  - phase: bootstrap
    provides: esqueleto Gradle KTS com AGP 9.3.0, minSdk 29 e recursos pre-escritos das Fases 5-9
provides:
  - Politica de lint declarada em um unico lugar (bloco `lint { }` no app/build.gradle.kts) com justificativa auditavel por comentario
  - abortOnError = true explicito — issue de lint quebra o build
  - Correcao real do ObsoleteSdkInt (qualificador de API obsoleto removido do mipmap adaptativo)
  - Relatorio de lint com zero issues (QLT-02 fechado na letra)
affects: [09-validacao-fisica, fases de UI que consomem strings pre-escritas]

tech-stack:
  added: []
  patterns:
    - "Supressao de lint sempre acompanhada de comentario justificando e de gatilho de reavaliacao (Phase 9)"
    - "Sem lint-baseline.xml — a politica e explicita no build script"

key-files:
  created: []
  modified:
    - app/build.gradle.kts
    - app/src/main/res/mipmap/ic_launcher.xml
    - app/src/main/res/mipmap/ic_launcher_round.xml

key-decisions:
  - "UnusedResources, Typos e AndroidGradlePluginVersion desabilitados com justificativa; nenhum recurso de res/values apagado"
  - "ObsoleteSdkInt corrigido de verdade, nao suprimido"
  - "Diretorio final e res/mipmap/ (sem qualificador) — AAPT2 do AGP 9 nao resolveu @mipmap/ic_launcher a partir de res/mipmap-anydpi/"

patterns-established:
  - "Politica de qualidade centralizada no modulo app, nunca em baseline gerado"

requirements-completed: [QLT-02]

duration: 12min
completed: 2026-07-29
---

# Phase 01 Plan 01: Politica de Lint e Zero Issues Summary

**Bloco `lint { }` com tres supressoes justificadas por comentario mais remocao real do qualificador `-v26` do mipmap adaptativo, levando `./gradlew lint` de 137 warnings a zero issues.**

## Performance

- **Duration:** ~12 min
- **Tasks:** 2
- **Files modified:** 3 (1 editado, 2 renomeados)

## Accomplishments

- `app/build.gradle.kts` ganhou um unico bloco `lint { }` com `abortOnError = true` e `disable += setOf("UnusedResources", "Typos", "AndroidGradlePluginVersion")`, cada entrada precedida da justificativa exigida pelo CONTEXT.md.
- Nenhuma string ou cor de `res/values/` foi apagada — os ativos das Fases 5-9 seguem intactos (`git diff` em `res/values/` vazio).
- `ObsoleteSdkInt` eliminado por correcao real: o diretorio do icone adaptativo perdeu o qualificador de API obsoleto, preservado no historico via `git mv`.
- `app/build/reports/lint-results-debug.xml` tem 0 ocorrencias de `<issue` e `./gradlew assembleDebug lint` termina `BUILD SUCCESSFUL` com o APK debug gerado.

## Task Commits

1. **Task 1: Declarar a politica de lint no app/build.gradle.kts** — `4fa0be9` (chore)
2. **Task 2: Corrigir ObsoleteSdkInt removendo o qualificador obsoleto do mipmap** — `a823979` (fix)

## Files Created/Modified

- `app/build.gradle.kts` — bloco `lint { }` com politica declarada e comentada
- `app/src/main/res/mipmap/ic_launcher.xml` — icone adaptativo (movido de `mipmap-anydpi-v26/`)
- `app/src/main/res/mipmap/ic_launcher_round.xml` — icone redondo (movido de `mipmap-anydpi-v26/`)

## Decisions Made

- Nenhum `lint-baseline.xml`: a politica fica visivel e revisavel no build script, sem regeneracao por fase.
- `ObsoleteSdkInt` fora da lista de `disable` — correcao estrutural em vez de supressao.
- Destino final do rename foi `res/mipmap/` e nao `res/mipmap-anydpi/` (ver deviacao abaixo).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Diretorio destino do rename ajustado de `mipmap-anydpi/` para `mipmap/`**
- **Found during:** Task 2
- **Issue:** O plano mandava `git mv .../mipmap-anydpi-v26 .../mipmap-anydpi`. Feito exatamente assim, `./gradlew clean assembleDebug` falhou em `:app:processDebugResources` com `AAPT: error: resource mipmap/ic_launcher (aka org.sentinela.app:mipmap/ic_launcher) not found` — o AAPT2 do AGP 9 nao resolveu a referencia do manifest a partir de `mipmap-anydpi/` sem qualificador de versao. Falha reproduzida apos `clean`, portanto nao era cache incremental.
- **Fix:** `git mv app/src/main/res/mipmap-anydpi app/src/main/res/mipmap`. Objetivo do plano preservado: qualificador de API obsoleto removido (o `-v26` era a causa do `ObsoleteSdkInt`), sem supressao, sem `-v29`, sem copia em outro qualificador e sem editar o XML. O nome do recurso (`@mipmap/ic_launcher`) permanece identico e `AndroidManifest.xml` nao foi tocado. Com minSdk 29 nao ha risco: o icone adaptativo so precisa de API 26+.
- **Files modified:** `app/src/main/res/mipmap/ic_launcher.xml`, `app/src/main/res/mipmap/ic_launcher_round.xml`
- **Verification:** `git status` mostra os dois arquivos como `R` (renomeados); `./gradlew assembleDebug lint` = `BUILD SUCCESSFUL`; `grep -o '<issue ' lint-results-debug.xml | wc -l` = 0; APK debug presente em `app/build/outputs/apk/debug/app-debug.apk`.
- **Committed in:** `a823979`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Ajuste minimo de caminho para destravar o build. Intencao, criterios de sucesso e proibicoes do plano preservados integralmente. Sem scope creep.

**Nota para consumidores do plano:** o `must_haves.artifacts` do PLAN citava `app/src/main/res/mipmap-anydpi/ic_launcher.xml`; o caminho real entregue e `app/src/main/res/mipmap/ic_launcher.xml`.

## Issues Encountered

- O relatorio XML do lint fica em `app/build/reports/lint-results-debug.xml`, mas o log do Gradle so anuncia HTML e SARIF — o arquivo existe e foi usado para a verificacao.

## User Setup Required

None — nenhuma configuracao externa necessaria.

## Next Phase Readiness

- QLT-02 fechado na letra: lint com zero issues e `abortOnError = true`, entao qualquer regressao de qualidade quebra o build a partir de agora.
- Reavaliar `disable "UnusedResources"` na Phase 9, quando as telas reais consumirem as strings pre-escritas.
- Sem blockers introduzidos por este plano.

## Self-Check: PASSED

Todos os arquivos declarados existem e os commits `4fa0be9` e `a823979` estao no historico.

---
*Phase: 01-fundacao-compilavel*
*Completed: 2026-07-29*

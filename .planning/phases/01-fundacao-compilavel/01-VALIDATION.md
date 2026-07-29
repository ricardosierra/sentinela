---
phase: 1
slug: fundacao-compilavel
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-29
updated: 2026-07-29
---

# Phase 1 — Validation Strategy

> Contrato de validacao da fase: como cada tarefa produz feedback automatico durante a execucao.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (`junit:junit:4.13.2`) sobre Gradle/AGP 9.3.0 — testes JVM puros. MockK 1.14.11, Turbine 1.2.1, coroutines-test e Robolectric 4.16.1 declarados, mas **nenhum teste Robolectric existe nesta fase** (Robolectric 4.16.1 nao suporta SDK 37) |
| **Config file** | `app/build.gradle.kts` (`testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }`) + `gradle/libs.versions.toml` |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew assembleDebug testDebugUnitTest lint detekt` |
| **Estimated runtime** | quick ~15 s (incremental) · full ~45 s pos-`clean` |
| **Pre-requisito** | `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"` — nao existe `local.properties`. JDK 17 via `gradle.properties` |
| **Relatorios de evidencia** | `app/build/test-results/testDebugUnitTest/*.xml`, `app/build/reports/lint-results-debug.xml`, `app/build/reports/detekt/detekt.xml`, manifest mergeado em `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` |

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest` (+ `bash scripts/verify-invariants.sh` a partir do plano 02)
- **Apos cada wave:** `./gradlew assembleDebug testDebugUnitTest lint detekt`
- **Antes de `/gsd:verify-work`:** suite completa verde **pos-`clean`** — um run incremental
  devolve `BUILD SUCCESSFUL` com tudo `UP-TO-DATE` e nao prova nada. O log so vale com
  `N actionable tasks: M executed`, M > 0.
- **Latencia maxima de feedback:** < 60 s (build Android; nao ha watch mode neste projeto)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | QLT-02 | static analysis | `./gradlew lint` + `grep -c 'id="UnusedResources"' app/build/reports/lint-results-debug.xml` = 0 | ✅ | ⬜ pending |
| 1-01-02 | 01 | 1 | QLT-02 | build + static analysis | `./gradlew assembleDebug lint` + `grep -c '<issue' app/build/reports/lint-results-debug.xml` = 0 | ✅ | ⬜ pending |
| 1-02-01 | 02 | 1 | PRV-01, UIX-12 | script de invariantes (manifest mergeado + rebranding) | `./gradlew assembleDebug && bash scripts/verify-invariants.sh` | ❌ criado pela propria task | ⬜ pending |
| 1-02-02 | 02 | 1 | UIX-08 | unit (JVM puro) | `./gradlew testDebugUnitTest detekt` — `ThemeTokensTest` | ❌ criado pela propria task | ⬜ pending |
| 1-02-03 | 02 | 1 | QLT-02 (casos de dominio), criterio 4 | unit (JVM puro) | `./gradlew testDebugUnitTest --tests "*CallDecisionEngine*"` + `grep -c '@Test' .../CallDecisionEngineTest.kt` >= 24 | ✅ (arquivo existe, 20 testes) | ⬜ pending |
| 1-03-01 | 03 | 2 | PRV-01, UIX-12 | doc/grep (reconciliacao + pendencias) | `grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml` = 1 e `grep -cE '^\| 3[1-4] \|' docs/TESTE-FISICO-SAMSUNG.md` = 4 | ✅ | ⬜ pending |
| 1-03-02 | 03 | 2 | PRV-01, QLT-02, UIX-08, UIX-12 | evidencia pos-`clean` | `./gradlew clean && ./gradlew assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` | ❌ `01-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| PRV-01 | 1-02-01 (allowlist sobre manifest mergeado, INTERNET = 0), 1-03-01, 1-03-02 |
| QLT-02 | 1-01-01, 1-01-02 (lint 0 issues), 1-02-03 (dominio testado), 1-03-02 (detekt 0 errors) |
| UIX-08 | 1-02-02 (`ThemeTokensTest` + wiring `DarkColors`), 1-03-02 |
| UIX-12 | 1-02-01 (bloco 2: `sentinelaApplicationId`, sem literal de UI em Kotlin, sem `Color(0x` fora de `ui/theme`), 1-03-01/02 |

---

## Wave 0 Requirements

Nenhum. A infraestrutura de teste ja existe e roda: JUnit 4 configurado no
`app/build.gradle.kts`, `CallDecisionEngineTest.kt` com 20 testes verdes medidos em 2026-07-29,
e `./gradlew testDebugUnitTest` funcional. Os arquivos marcados `❌` no mapa acima
(`scripts/verify-invariants.sh`, `ThemeTokensTest.kt`, `01-EVIDENCE.md`) sao **produtos das
proprias tasks**, nao dependencias de Wave 0 — cada uma cria e executa seu artefato dentro da
mesma task.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Renderizacao real do tema dark Silent Guardian no aparelho | UIX-08 | Aparencia em tela so se confirma com o APK instalado; a politica de validacao fisica do ROADMAP (2026-07-28) proibe `checkpoint:human-*` nas Fases 1-8 | Cenarios 31-34 de `docs/TESTE-FISICO-SAMSUNG.md`, executados pelo mantenedor na Phase 9. O verifier desta fase trata como **deferred to Phase 9**, nunca como gap |
| Instalabilidade do APK em Samsung/One UI | criterio 2 | Idem | Cenario 31 do mesmo roteiro |

Tudo o mais tem verificacao automatizada: a parte mecanica do criterio 2 (APK produzido +
tokens aplicados) e provada por `test -f app/build/outputs/apk/debug/app-debug.apk`,
`ThemeTokensTest` e grep de `SentinelaTheme` no `MainActivity.kt`.

---

## Validation Sign-Off

- [x] Todas as tasks tem `<automated>` no `<verify>` — nenhuma referencia `MISSING`
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 nao e necessario (infraestrutura existente cobre a fase); nenhuma referencia MISSING pendente
- [x] Nenhuma flag de watch mode em comando algum
- [x] Latencia de feedback dentro do orcamento do projeto (< 60 s para o comando rapido)
- [x] Todo requisito da fase (PRV-01, QLT-02, UIX-08, UIX-12) mapeado a pelo menos uma task
- [x] Itens que exigem aparelho fisico registrados como diferidos, sem `checkpoint:human-*`
- [x] `nyquist_compliant: true` definido no frontmatter

**Approval:** approved 2026-07-29

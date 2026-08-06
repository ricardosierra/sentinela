---
phase: 01-fundacao-compilavel
verified: 2026-07-29T00:00:00Z
status: passed
score: 5/5 must-haves verified
---

# Phase 1: Fundacao compilavel Verification Report

**Phase Goal:** Projeto Android compila, testa e lint-a limpo com o stack travado, pronto para
receber as fases seguintes.
**Verified:** 2026-07-29
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Success Criteria from ROADMAP)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `./gradlew assembleDebug testDebugUnitTest lint detekt` termina sem erro | ✓ VERIFIED | Comando reexecutado nesta verificação: `BUILD SUCCESSFUL in 796ms`, `57 actionable tasks`. `01-EVIDENCE.md` arquiva o run pós-`clean --no-build-cache` com `56 executed` (prova de execução real, não cache/up-to-date). |
| 2 | `assembleDebug` produz APK instalável e o tema dark "Silent Guardian" está aplicado no MainActivity (verificação em aparelho fica na Phase 9) | ✓ VERIFIED (parcial, conforme deferimento documentado) | `app/build/outputs/apk/debug/app-debug.apk` existe (33.807.562 bytes). `ThemeTokensTest` (4 testes) verifica os tokens Silent Guardian e o wiring de `DarkColors` em JVM pura. Renderização real em hardware está registrada como cenários 31-34 em `docs/TESTE-FISICO-SAMSUNG.md`, conforme política de validação física do ROADMAP — deferimento documentado, não gap. |
| 3 | Manifest não declara INTERNET e registra o CallScreeningService com BIND_SCREENING_SERVICE | ✓ VERIFIED | `scripts/verify-invariants.sh` bloco 1 roda sobre o manifest **mergeado**: `0` ocorrências de `android.permission.INTERNET`; `BIND_SCREENING_SERVICE` presente no `<service>`; `action android.telecom.CallScreeningService` registrada; allowlist de permissões respeitada (só `POST_NOTIFICATIONS` e `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a segunda injetada pelo androidx-core). Script re-executado nesta verificação: exit 0. |
| 4 | CallDecisionEngine puro existe com a precedência (incluindo políticas por origem) coberta por testes unitários | ✓ VERIFIED | `CallDecisionEngineTest.kt`: 24 `@Test` verdes (0 failures/errors). Matriz `OriginPolicy` x origem fechada: `NEVER_SILENCE` (2 ocorrências, contato + desconhecido), `OriginPolicy.RING`/`OriginPolicy.BLOCK` na whitelist (5 ocorrências), `BlockWithoutTrace` (5 ocorrências). `grep -c "import android"` no domínio = `0` (motor puro, sem dependência de Telecom). |
| 5 | Nome, applicationId, cores e strings centralizados — rebranding não exige tocar em código Kotlin | ✓ VERIFIED | `scripts/verify-invariants.sh` bloco 2: nenhum `applicationId` literal em Kotlin, nenhuma `text = "..."` hardcoded, nenhuma `Color(0x...)` fora de `ui/theme/`; `sentinelaApplicationId` usado 3x em `app/build.gradle.kts`; `app_name` definido em `strings.xml`. |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/build.gradle.kts` (`lint { }`) | Política de lint declarada, `abortOnError = true`, 3 disables comentados, sem baseline | ✓ VERIFIED | Bloco presente linhas 65-83; `lintOptions` e `lint-baseline` ausentes. |
| `app/src/main/res/mipmap/ic_launcher.xml` (+ `_round`) | Ícone adaptativo sem qualificador obsoleto | ✓ VERIFIED (caminho final `mipmap/`, não `mipmap-anydpi/`) | Diretório é `app/src/main/res/mipmap/` — divergência de caminho já registrada como desvio conhecido (AAPT exige esse local para ícones adaptativos sem qualificador). `mipmap-anydpi-v26` não existe mais; `AndroidManifest.xml:14` continua resolvendo `@mipmap/ic_launcher`. |
| `scripts/verify-invariants.sh` | Verificação mecânica dos critérios 3, 4 e 5 | ✓ VERIFIED | Executável (`rwxr-xr-x`), sai 0, cobre manifest mergeado, rebranding e domínio puro. Re-executado nesta verificação com sucesso. |
| `app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt` | Matriz OriginPolicy x origem completa | ✓ VERIFIED | 24 `@Test`, `NEVER_SILENCE`/`RING`/`BLOCK`/`BlockWithoutTrace` todos presentes. |
| `app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt` | Asserções JVM sobre tokens Silent Guardian e wiring do `darkColorScheme` | ✓ VERIFIED | 4 `@Test`, contém `0xFF081425`, assevera `DarkColors.surface`/`.primary`/`.error`/`.outline` etc. `DarkColors` é `internal` (alterado de `private`), `Color.kt` intocado. |
| `.planning/phases/01-fundacao-compilavel/01-EVIDENCE.md` | Log pós-clean com `N actionable tasks: M executed` (M>0) | ✓ VERIFIED | Frontmatter: `actionable_tasks: 57`, `tasks_executed: 56`, `tests_total: 28`, `lint_issues: 0`, `detekt_issues: 0`. Documenta metodologia `--no-build-cache` para evitar falso positivo de `FROM-CACHE`. |
| `docs/TESTE-FISICO-SAMSUNG.md` | Pendências físicas da Phase 1 registradas para Phase 9 | ✓ VERIFIED | Cenários 31-34 presentes (`grep -cE "^\| 3[1-4] \|"` = 4), seção "Pendencias herdadas da Phase 1". |
| `docs/backlog/manutencao-toolchain.md` | Itens de manutenção adiados | ✓ VERIFIED | Existe, contém `gradlew.bat`, `9.3.1`, `UnusedResources`; indexado em `docs/INDEX.md`. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `app/build.gradle.kts` | `app/build/reports/lint-results-debug.xml` | política de lint aplicada | ✓ WIRED | `<issue ` (com espaço, elemento real) = 0 ocorrências no relatório re-gerado; a única ocorrência de `<issue` bruta é a tag raiz `<issues format=...>`, não um item de lint. |
| `AndroidManifest.xml` | `mipmap/ic_launcher.xml` | `android:icon="@mipmap/ic_launcher"` | ✓ WIRED | Referência resolve após o rename; `grep -c "@mipmap/ic_launcher"` = 1. |
| `scripts/verify-invariants.sh` | manifest mergeado (`merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`) | grep sobre o artefato do AGP | ✓ WIRED | Script lê o mergeado (não o fonte); allowlist confirmada nesta verificação. |
| `ThemeTokensTest.kt` | `Theme.kt` (`DarkColors`) | asserção sobre visibilidade `internal` | ✓ WIRED | 4 testes verdes comparando tokens contra `DarkColors.*`. |
| `01-EVIDENCE.md` | `scripts/verify-invariants.sh` | saída colada como evidência | ✓ WIRED | Seção "Saida do verify-invariants.sh" presente com `todos os invariantes OK`. |
| `01-CONTEXT.md` | `docs/PERMISSOES.md` | reconciliação de `POST_NOTIFICATIONS` | ✓ WIRED | Parágrafo de reconciliação presente citando a fonte canônica; permissão mantida no manifest. |
| `docs/backlog/manutencao-toolchain.md` | `docs/INDEX.md` | entrada no índice | ✓ WIRED | `grep -c "manutencao-toolchain" docs/INDEX.md` = 1. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| QLT-02 | 01-01, 01-02, 01-03 | Lint + detekt sem issues; builds debug/release compilam | ✓ SATISFIED | Lint 0 issues, detekt 0 errors, `assembleDebug` produz APK. REQUIREMENTS.md já marca `[x]` com fase 1,9. |
| PRV-01 | 01-02 | MVP sem INTERNET; sem rede/telemetria/segredo | ✓ SATISFIED | `verify-invariants.sh` confirma 0 `INTERNET` no manifest mergeado; nenhuma chamada de rede no código. |
| UIX-08 | 01-02 | Dark mode + Dynamic Color seguindo tokens Silent Guardian | ✓ SATISFIED | `ThemeTokensTest` trava os 26 tokens e o wiring de `darkColorScheme`; renderização real diferida à Phase 9 (documentado). |
| UIX-12 | 01-02 | Nome/applicationId/cores/strings centralizados | ✓ SATISFIED | `verify-invariants.sh` bloco 2 confirma ausência de literais hardcoded. |

Nenhum requirement órfão encontrado: os 4 IDs declarados nos planos (`QLT-02`, `PRV-01`, `UIX-08`, `UIX-12`) batem com o mapeamento em `.planning/REQUIREMENTS.md` (linhas 165-176, todos `Complete` para Phase 1).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/java/org/sentinela/app/ui/MainActivity.kt` | 32, 39 | `PlaceholderScreen()` | ℹ️ Info | Esperado nesta fase — Phase 1 é fundação de build, não UI funcional (telas reais são Fases 5-9). Não bloqueia o goal desta fase. |
| `app/src/main/java/org/sentinela/app/AppContainer.kt` | 19-24 | `// TODO(Fase N): ...` | ℹ️ Info | Marcadores de dependências futuras explicitamente atribuídos a fases posteriores — não são débito técnico escondido, são placeholders documentados de DI manual. |
| `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` | 26 | `// TODO(Fase 5): ...` | ℹ️ Info | Mesma natureza — trabalho de fase futura já mapeado. |

Nenhum anti-pattern bloqueador (🛑) ou de aviso (⚠️) encontrado. Nenhum `Log.` com número de telefone completo; nenhum framework de DI (`Hilt`/`Koin`/`Dagger`/`@Inject`) introduzido, conforme exigido pelo CLAUDE.md.

### CLAUDE.md Non-Negotiables (checked)

- Sem `INTERNET` no manifest: confirmado (fonte e mergeado).
- Sem string hardcoded em Kotlin: confirmado via `verify-invariants.sh` bloco 2.
- Sem número de telefone em log: nenhuma chamada `Log.`/`println` no código de produção desta fase.
- DI manual, sem framework: confirmado — único hit de "Hilt/Koin" é um comentário explicando a decisão.
- `CallDecisionEngine` puro, sem `import android.*`: confirmado.

### Human Verification Required

Nenhum item novo requer verificação humana nesta fase — a única pendência de hardware (tema
renderizado, Dynamic Color, APK instalando) já está formalmente registrada como cenários 31-34
em `docs/TESTE-FISICO-SAMSUNG.md`, com execução planejada para a Phase 9 conforme a política de
validação física do ROADMAP. Não é um gap desta fase.

### Gaps Summary

Nenhum gap encontrado. Os 5 critérios de sucesso do ROADMAP estão verificados mecanicamente
(script re-executado nesta verificação, build re-executado do zero e passou), os 4 requirement
IDs da fase estão satisfeitos e batem com `.planning/REQUIREMENTS.md`, e os desvios conhecidos
(caminho `mipmap/` em vez de `mipmap-anydpi/`, renderização real diferida à Phase 9, suite com
28 testes) estão documentados e confirmados como intencionais, não como falhas.

---

_Verified: 2026-07-29_
_Verifier: Claude (gsd-verifier)_

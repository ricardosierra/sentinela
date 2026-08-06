---
phase: 2
slug: motor-de-decisao-e-normalizacao
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
updated: 2026-07-29
---

# Phase 2 — Validation Strategy

> Contrato de validacao da fase: como cada task produz feedback automatico durante a execucao.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 `4.13.2` (+ `org.junit.runners.Parameterized`, que ja vem no artefato) sobre AGP 9.3.0 / Gradle 9.6.1 / JDK 17 — testes JVM puros. **Nenhum teste Robolectric** (4.16.1 nao suporta compileSdk 37) e **nenhum teste instrumentado** nesta fase |
| **Config file** | `app/build.gradle.kts` — `testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }`. O `isIncludeAndroidResources = true` e **pre-requisito do carregamento dos metadados do libphonenumber em teste — nao remover** |
| **Cobertura** | Kover `0.9.9` (plugin `org.jetbrains.kotlinx.kover`), filtro `classes("org.sentinela.app.domain.*", "org.sentinela.app.phone.*")`, regra `minBound(80)` ativada no plano 02-05 |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh` |
| **Estimated runtime** | quick ~3–15 s incremental · `koverVerify` ~22 s a frio · full ~60–90 s pos-`clean` |
| **Pre-requisitos** | `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"` (nao existe `local.properties`); JDK 17 via `gradle.properties`; **`-XX:MaxMetaspaceSize=1g`** — com os 512m atuais o plugin Kover mata o build com `Failed to notify build listener > Metaspace` (reproduzido na pesquisa) |
| **Relatorios de evidencia** | `app/build/test-results/testDebugUnitTest/*.xml`, `app/build/reports/kover/html/index.html`, `app/build/reports/kover/report.xml`, `app/build/reports/lint-results-debug.xml`, `app/build/reports/detekt/detekt.xml`, manifest mergeado em `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` |

**Fixture critica:** `app/src/test/java/org/sentinela/app/phone/TestMetadata.kt` (criada na task
2-01-02). Sem ela **nenhum** teste de normalizacao ou de mascara roda. Ela localiza os metadados
do AAR via `com/android/tools/test_config.properties` → chave `android_merged_assets` (caminho
**relativo ao diretorio do modulo `app/`**, que e o working dir do teste), com fallback para
`build/intermediates/assets/debug/mergeDebugAssets` e `check()` explicito sobre
`PhoneNumberMetadataProto_BR` — falha alto em vez de virar falso-verde por metadados vazios.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest` (< 60 s)
- **Apos cada wave:** `./gradlew testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh`
  (nas waves 1–3 o `koverVerify` ainda nao tem regra: usar `./gradlew koverLog` e conferir ≥ 80% manualmente)
- **Phase gate (antes de `/gsd:verify-work`):** suite completa verde **pos-`clean`**, executada com
  `--no-build-cache`, com `N actionable tasks: M executed` e **M > 0**. `UP-TO-DATE` e `FROM-CACHE`
  tem o mesmo defeito probatorio (regra fixada na Phase 1) — arquivado em `02-EVIDENCE.md`
- **Latencia maxima de feedback:** < 60 s
- **Nenhum comando usa watch mode.**

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 1 | QLT-07 | build + coverage config | `./gradlew testDebugUnitTest koverLog` (imprime percentual; `tasks --all` lista `koverVerify`) | ❌ criado pela task | ⬜ pending |
| 2-01-02 | 01 | 1 | NRM-01 | unit (JVM puro, fixture) | `./gradlew testDebugUnitTest --tests "*TestMetadataSentinelTest"` | ❌ **Wave 0** — bloqueia 02-04 | ⬜ pending |
| 2-01-03 | 01 | 1 | DEC-01 | script de invariantes (pureza `domain/` + `phone/`) | `./gradlew assembleDebug lint detekt && bash scripts/verify-invariants.sh` (+ teste negativo com arquivo temporario importando `android.content.Context`) | ✅ (estende script da Phase 1) | ⬜ pending |
| 2-02-01 | 02 | 1 | DEC-02, DEC-03, DEC-05, CTT-03, WLT-08, QLT-01 | unit parametrizado (48 casos + privados + invalidos + fallback) | `./gradlew testDebugUnitTest --tests "*DecisionMatrixTest" --tests "*DecisionEdgeCasesTest"` — XML com `tests` ≥ 48, `failures="0"` | ❌ criado pela task | ⬜ pending |
| 2-02-02 | 02 | 1 | DEC-04, DEC-02, DEC-01 | unit | `./gradlew testDebugUnitTest --tests "*DecisionReasonTest" --tests "*CallDecisionEngineTest"` + `[ "$(grep -c '@Test' .../CallDecisionEngineTest.kt)" -ge 29 ]` | ✅ parcial (24 testes) / ❌ `DecisionReasonTest` | ⬜ pending |
| 2-03-01 | 03 | 2 | NRM-01, NRM-02 | unit com fakes (12+ casos de cascata) | `./gradlew testDebugUnitTest --tests "*CascadingRegionProviderTest" detekt` | ❌ criado pela task | ⬜ pending |
| 2-03-02 | 03 | 2 | NRM-01 | build + invariantes (permissoes + pureza) | `./gradlew assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` — allowlist intacta, `READ_PHONE_STATE` ausente | ❌ criado pela task | ⬜ pending |
| 2-04-01 | 04 | 3 | NRM-04, QLT-01 | unit + teste de propriedade | `./gradlew testDebugUnitTest --tests "*PhoneMaskTest" detekt` — inclui assert literal de `+55 11 9****-1234` e `!masked.contains(nsnCompleto)` | ❌ criado pela task | ⬜ pending |
| 2-04-02 | 04 | 3 | NRM-01, NRM-02, NRM-03 | unit (tabela medida na pesquisa) | `./gradlew testDebugUnitTest --tests "*LibPhoneNumberNormalizerTest" detekt` — ≥ 14 `@Test` | ❌ criado pela task | ⬜ pending |
| 2-04-03 | 04 | 3 | NRM-02, NRM-03, QLT-01 | unit (regras BR a mao, com caso negativo) | `./gradlew testDebugUnitTest --tests "*BrazilianRulesNormalizerTest" --tests "*PhoneMaskTest"` — inclui assert negativo de `"+55190"` e consistencia normalize×mask para `"190"` | ❌ criado pela task | ⬜ pending |
| 2-05-01 | 05 | 4 | NRM-01, DEC-01 | build + invariantes (wiring) | `./gradlew assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` | ❌ criado pela task | ⬜ pending |
| 2-05-02 | 05 | 4 | QLT-07 | gate de cobertura + evidencia pos-`clean` | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh` | ❌ `02-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| DEC-01 | 2-01-03 (invariante de pureza `domain/` + `phone/`), 2-02-02, 2-05-01 |
| DEC-02 | 2-02-01 (matriz), 2-02-02 (precedencia entre os 7 niveis) |
| DEC-03 | 2-02-01 (sub-matriz `blockMode` × `hideFromNativeCallLog` → Reject / BlockWithoutTrace / SendSilentlyToVoicemail) |
| DEC-04 | 2-02-02 (`DecisionReasonTest`: todo `code` casa `[a-z_]+`, 9 entradas, codes unicos) |
| DEC-05 | 2-02-01 (2 gatilhos × 2 `FallbackPolicy` = 4 casos) |
| NRM-01 | 2-01-02 (fixture de metadados), 2-03-01/02 (regiao), 2-04-02, 2-05-01 (wiring) |
| NRM-02 | 2-03-01 (regiao mundial), 2-04-02 (BR/internacional), 2-04-03 (9o digito) |
| NRM-03 | 2-04-02 (E.164 e a verdade; `parse` bem-sucedido nao e criterio), 2-04-03 (excecao documentada: codigos curtos usam digitos crus) |
| NRM-04 | 2-04-01 (mascara unica, teste de propriedade, entradas hostis sem excecao) |
| CTT-03 | 2-02-01 (4 `OriginPolicy` sobre `contactsPolicy`) |
| WLT-08 | 2-02-01 (4 `OriginPolicy` sobre `whitelistPolicy`) |
| QLT-01 | 2-02-01, 2-04-01, 2-04-03 (casos obrigatorios da §13 do PROMPT-MVP que sao puros; os de repositorio/timeout/Room ficam nas Phases 3 e 5) |
| QLT-07 | 2-01-01 (plugin + filtro), 2-05-02 (gate `minBound(80)` provado falhando em 99) |

---

## Wave 0 Requirements

Itens de infraestrutura que **bloqueiam** as tasks seguintes e por isso vivem no plano 02-01
(wave 1), executado antes de qualquer trabalho de normalizacao:

- [ ] `gradle.properties` — `-XX:MaxMetaspaceSize=1g` (**primeiro item; sem ele o plugin Kover mata o build**)
- [ ] `gradle/libs.versions.toml` + `app/build.gradle.kts` — plugin Kover 0.9.9 e bloco
      `kover { reports { filters { includes { classes("org.sentinela.app.domain.*", "org.sentinela.app.phone.*") } } } }`
      **sem** a regra `verify` (o gate so entra no plano 02-05, depois que os testes de
      normalizacao existirem — liga-lo antes falharia o build por conta do codigo novo sem cobertura)
- [ ] `app/src/test/java/org/sentinela/app/phone/TestMetadata.kt` — `MetadataLoader` de teste;
      fixture compartilhada de **todos** os testes de normalizacao e mascara
- [ ] `scripts/verify-invariants.sh` — checagem de pureza estendida de `domain/` para `phone/`

Instalacao de framework: **nenhuma**. JUnit 4 ja esta configurado e verde (28 testes na Phase 1);
`Parameterized` vem no proprio artefato do JUnit 4; nenhuma dependencia de teste nova
(`libphonenumber-android` ja e `implementation` e roda em JVM pura — comprovado na pesquisa).

Os demais arquivos marcados `❌` no mapa acima sao **produto das proprias tasks**, nao dependencias
de Wave 0.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Leitura real de `simCountryIso`/`networkCountryIso` em aparelho com SIM | NRM-01/02 | `AndroidRegionProvider` toca `TelephonyManager`; testa-lo em JVM exigiria Robolectric (proibido: 4.16.1 nao suporta SDK 37). A politica de validacao fisica do ROADMAP (2026-07-28) proibe `checkpoint:human-*` nas Fases 1–8 | Cenario a acrescentar em `docs/TESTE-FISICO-SAMSUNG.md` na Phase 9: instalar o APK em Samsung com SIM brasileiro e confirmar que um numero digitado sem DDI normaliza para `+55…`; repetir em modo aviao (queda para o fallback). O verifier desta fase trata como **deferred to Phase 9**, nunca como gap |

Tudo o mais tem verificacao automatizada. A pureza de `AndroidRegionProvider` e garantida
indiretamente: a logica de cascata que poderia errar e **pura** (`CascadingRegionProvider`, 12+
testes) e o arquivo Android e uma delegacao de tres linhas dentro de `runCatching`.

---

## Validation Sign-Off

- [x] Todas as tasks tem `<automated>` no `<verify>` — nenhuma referencia `MISSING`
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 identificado e sequenciado primeiro (plano 02-01, wave 1), com o gate de cobertura
      deliberadamente adiado para o plano 02-05 — ativa-lo antes dos testes existirem quebraria o build
- [x] Nenhuma flag de watch mode em comando algum
- [x] Latencia de feedback dentro do orcamento do projeto (< 60 s para o comando rapido)
- [x] Todo requisito da fase (DEC-01..05, NRM-01..04, CTT-03, WLT-08, QLT-01, QLT-07) mapeado a
      pelo menos uma task
- [x] Itens que exigem aparelho fisico registrados como diferidos; **nenhum `checkpoint:human-*`**
      em nenhum plano da fase
- [x] Gate probatorio pos-`clean` com `--no-build-cache` exigido antes de `/gsd:verify-work`
- [x] `nyquist_compliant: true` definido no frontmatter

**Approval:** approved 2026-07-29

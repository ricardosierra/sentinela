---
phase: 3
slug: dados-locais
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
updated: 2026-07-29
---

# Phase 3 — Validation Strategy

> Contrato de validacao da fase: como cada task produz feedback automatico durante a execucao.
> Diferenca central em relacao a Phase 2: esta fase tem **duas** suites — JVM pura e
> **instrumentada em emulador**, executada de verdade (decisao do usuario, 2026-07-29).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (JVM)** | JUnit 4 `4.13.2` sobre AGP 9.3.0 / Gradle 9.6.1 / JDK 17 — testes JVM puros, herdado da Phase 2. **Sem Robolectric** (4.16.1 nao suporta compileSdk 37 — blocker no STATE) |
| **Framework (instrumentado)** | AndroidX Test com `androidx.test.runner.AndroidJUnitRunner` (**ja configurado** em `defaultConfig` — nada a mudar), `androidx.test.ext:junit-ktx 1.3.0`, `androidx.test:core-ktx 1.7.0`, `androidx.room:room-testing 2.8.4` |
| **Config file** | `app/build.gradle.kts` — `testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }` (**nao remover**: pre-requisito do libphonenumber em teste, Phase 2) + `sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")` (Wave 0 desta fase; sem ele `MigrationTestHelper` falha com "Cannot find the schema file") |
| **Cobertura** | Kover `0.9.9`. Filtro atual: `domain.*` + `phone.*` (97,619%). Ampliado **so no plano 03-07** para `+ data.* + settings.*`, **excluindo** `org.sentinela.app.data.local.db.*`, `*_Impl` e `annotatedBy(androidx.room.Dao/Database)` — o gerado pelo Room so roda instrumentado e o Kover nao o mede, entao inclui-lo derrubaria o gate com falso-vermelho |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` (sobe o AVD `Medium_Phone_API_35` headless, poll em `sys.boot_completed`, roda `:app:connectedDebugAndroidTest`, `trap` de `emu kill`) |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Pre-requisitos** | `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"` (nao existe `local.properties`); JDK 17 via `gradle.properties`; `-XX:MaxMetaspaceSize=1g` (**ja presente** — 512m mata o build com o plugin Kover) |
| **Estimated runtime** | quick ~15 s · instrumentado ~15 s incremental / ~55 s a frio **+ 2–4 min de boot do emulador a frio** · full ~4–6 min |
| **Relatorios de evidencia** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/html/index.html` e `report.xml` · lint: `app/build/reports/lint-results-debug.xml` · detekt: `app/build/reports/detekt/detekt.xml` · **androidTest XML:** `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` · **logcat por teste (onde saem os percentis):** `app/build/outputs/androidTest-results/connected/debug/Medium_Phone_API_35(AVD) - 15/logcat-<classe>-<metodo>.txt` · exit code: `.../connected/debug/test-result-exit-code.txt` |

**Armadilha de script confirmada:** o nome do XML do androidTest contem parenteses e
espacos (`TEST-Medium_Phone_API_35(AVD) - 15-_app-.xml`). Sempre entre aspas ou via glob
`TEST-*.xml` — nunca montado a mao.

**Fixture herdada:** `app/src/test/java/org/sentinela/app/phone/TestMetadata.kt` (Phase 2)
continua sendo pre-requisito de qualquer teste que normalize numero. Esta fase nao normaliza
na camada de dados (a chave chega pronta), entao nenhuma task depende dela — mas os testes
da Phase 2 seguem no mesmo `testDebugUnitTest` e nao podem quebrar.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest` (< 30 s). Tasks que tocam
  **so** codigo instrumentado adicionam `bash scripts/run-instrumented-tests.sh --tests "*Padrao"`
  com o emulador **ja de pe** (o script reaproveita device conectado).
- **Apos cada wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
  Nas waves 1–4 o `koverVerify` ainda mede so `domain.*`/`phone.*`: usar `./gradlew koverLog`
  e conferir o percentual manualmente. O filtro so e ampliado no plano **03-07**.
- **Phase gate (antes de `/gsd:verify-work`):** suite JVM **e** instrumentada verdes
  **pos-`clean`**, executadas com `--no-build-cache`, com `N actionable tasks: M executed`
  e **M > 0**. `UP-TO-DATE` e `FROM-CACHE` tem o mesmo defeito probatorio (regra da Phase 1).
  Arquivado em `03-EVIDENCE.md`: o `TEST-*.xml` do androidTest, os percentis do logcat
  (`SENTINELA|contains|...`), a saida do EQP (`SENTINELA|EQP|...`) e o `koverVerify`.
- **Latencia maxima de feedback:** < 60 s no comando rapido. O emulador sobe **uma vez**
  por sessao de execucao, nao por task.
- **Nenhum comando usa watch mode.**

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 3-01-01 | 01 | 1 | QLT-06 | build config (deps androidTest + assets de schema) | `./gradlew :app:assembleDebugAndroidTest` | ❌ **Wave 0** — bloqueia 03-03..07 | ⬜ pending |
| 3-01-02 | 01 | 1 | QLT-06 | script (boot polling + trap) | `bash -n scripts/run-instrumented-tests.sh && grep -q sys.boot_completed scripts/run-instrumented-tests.sh` | ❌ **Wave 0** — bloqueia toda task instrumentada | ⬜ pending |
| 3-01-03 | 01 | 1 | QLT-06 | instrumentado (fumaca) | `bash scripts/run-instrumented-tests.sh --tests "*InstrumentationSmokeTest"` — XML com `failures="0"` | ❌ criado pela task | ⬜ pending |
| 3-02-01 | 02 | 1 | PRV-03, HST-06 | build + lint sobre XML de backup | `./gradlew assembleDebug lint` + greps de `path="."` e ausencia de `<include>` | ✅ (XMLs existem, a corrigir) | ⬜ pending |
| 3-02-02 | 02 | 1 | PRV-03, HST-06 | **unit (le o XML por DOM)** | `./gradlew testDebugUnitTest --tests "*BackupRulesTest"` — 5 testes, com falha demonstrada ao remover um `<exclude>` | ❌ criado pela task | ⬜ pending |
| 3-03-01 | 03 | 2 | WLT-04, HST-01, HST-05 | build (KSP) + unit (conversores) | `./gradlew :app:compileDebugKotlin testDebugUnitTest --tests "*ConvertersTest"` + `ls app/schemas/*/1.json` + grep `"unique": true` | ❌ criado pela task | ⬜ pending |
| 3-03-02 | 03 | 2 | QLT-03 | **unit (le o schema exportado)** | `./gradlew testDebugUnitTest --tests "*SchemaExportTest"` — falha demonstrada com `exportSchema = false` | ❌ criado pela task | ⬜ pending |
| 3-03-03 | 03 | 2 | QLT-03, PRV-03 | script de invariantes | `./gradlew assembleDebug lint detekt && bash scripts/verify-invariants.sh` — Bloco 5: destrutivo, main-thread, schema v1, nome de contato | ✅ (estende script das Fases 1–2) | ⬜ pending |
| 3-04-01 | 04 | 3 | WLT-01, WLT-02, WLT-04, QLT-01 | unit (fake DAO) | `./gradlew testDebugUnitTest --tests "*RoomWhitelistRepositoryTest"` — ≥ 10 `@Test`, inclui falha de repositorio | ❌ criado pela task | ⬜ pending |
| 3-04-02 | 04 | 3 | WLT-01, WLT-02, WLT-03, WLT-04, QLT-06 | instrumentado (DAO) | `bash scripts/run-instrumented-tests.sh --tests "*WhitelistDaoTest"` — ≥ 9 `@Test`, inclui `SQLiteConstraintException` e codigo curto `"190"` | ❌ criado pela task | ⬜ pending |
| 3-04-03 | 04 | 3 | WLT-07 | instrumentado (**EQP** + benchmark) | `bash scripts/run-instrumented-tests.sh --tests "*WhitelistPerformanceTest"` — assert de `USING INDEX index_whitelist_number_key`, `p50 < 1 ms` (primario) e `p95 < 5 ms` (declarado) | ❌ **prova deterministica do indice; substitui o cronometro** | ⬜ pending |
| 3-05-01 | 05 | 3 | HST-02 | **unit (regra pura)** | `./gradlew testDebugUnitTest --tests "*RetentionPolicyTest"` — ≥ 8 `@Test`, lista travada em 5 politicas com `id` literal | ❌ criado pela task | ⬜ pending |
| 3-05-02 | 05 | 3 | HST-01, HST-02, HST-03, HST-04, QLT-01 | unit (fakes de DAO e Settings, relogio injetado) | `./gradlew testDebugUnitTest --tests "*RoomBlockedCallRepositoryTest"` — ≥ 12 `@Test` | ❌ criado pela task | ⬜ pending |
| 3-05-03 | 05 | 3 | HST-01, HST-02, HST-03, HST-05, QLT-06 | instrumentado (DAO) | `bash scripts/run-instrumented-tests.sh --tests "*BlockedCallDaoTest"` — ≥ 8 `@Test`, limite do cutoff travado | ❌ criado pela task | ⬜ pending |
| 3-06-01 | 06 | 4 | ENG-01, HST-02, QLT-01 | build + detekt (cache volatil, catch de IOException) | `./gradlew :app:compileDebugKotlin detekt lint` + greps de `@Volatile`, `emptyPreferences()` e ausencia de `ordinal`/`valueOf(` | ❌ criado pela task | ⬜ pending |
| 3-06-02 | 06 | 4 | ENG-01, HST-02, QLT-01 | **unit (DataStore em `TemporaryFolder`, JVM pura)** | `./gradlew testDebugUnitTest --tests "*DataStoreSettingsRepositoryTest" --tests "*AppOpenCounterTest"` — ≥ 16 + ≥ 5 `@Test` | ❌ criado pela task | ⬜ pending |
| 3-07-01 | 07 | 5 | WLT-07, HST-02, ENG-01 | build + invariantes (wiring de singletons) | `./gradlew assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` | ❌ criado pela task | ⬜ pending |
| 3-07-02 | 07 | 5 | QLT-03, QLT-06 | instrumentado (`MigrationTestHelper`) | `bash scripts/run-instrumented-tests.sh --tests "*MigrationHarnessTest"` — v1 aberta pelo helper, 2 tabelas confirmadas no `sqlite_master` | ❌ criado pela task | ⬜ pending |
| 3-07-03 | 07 | 5 | QLT-01, QLT-06 | gate de cobertura + evidencia pos-`clean` | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` | ❌ `03-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| WLT-01 | 3-04-01 (mapper + upsert com fake), 3-04-02 (insert + observeAll no emulador) |
| WLT-02 | 3-04-01 (`enabled` afeta `contains`), 3-04-02 (update de descricao, toggle, deleteById) |
| WLT-03 | 3-04-02 (`search` por trecho de numero **e** por descricao) |
| WLT-04 | 3-03-01 (indice UNICO no schema exportado), 3-04-01 (id resolvido antes do upsert), 3-04-02 (2 inserts → 1 linha + `SQLiteConstraintException` no insert cru) |
| WLT-07 | 3-04-03 (**EQP** `USING INDEX index_whitelist_number_key` + `p50 < 1 ms` / `p95 < 5 ms` com warmup de 300 e 500 amostras), 3-07-01 (DAO nao-suspend ligado no container) |
| HST-01 | 3-03-01 (entidade minima, sem coluna de nome de contato), 3-05-02, 3-05-03 |
| HST-02 | 3-05-01 (5 politicas puras: NEVER_STORE / 7 / 30 / 90 / MANUAL), 3-05-02 (poda apos gravacao, MANUAL nao poda), 3-05-03 (poda efetiva no banco), 3-06-01/02 (persistencia da politica), 3-07-01 (poda na abertura do app) |
| HST-03 | 3-05-02 (delegacao), 3-05-03 (`clearAll` zera `observeTotalCount`, `deleteById` remove 1) |
| HST-04 | 3-03-01 (coluna `number_e164`), 3-05-02 (`numberE164` preservado no mapper) |
| HST-05 | 3-03-01 (coluna `classification` por `name`), 3-05-03 (`updateClassification` persiste UNWANTED) |
| HST-06 | 3-02-01 (XMLs corrigidos), 3-02-02 (`BackupRulesTest` le o XML e falha se um `<exclude>` sumir) |
| ENG-01 | 3-06-01 (`incrementAppOpenCount` atomico no `edit`), 3-06-02 (`AppOpenCounterTest`: 0→1→2→3 e persistencia entre instancias), 3-07-01 (chamado em `onAppOpened`) |
| PRV-03 | 3-02-01/02 (backup em nuvem **e** device-transfer, nos dois formatos), 3-03-03 (invariante de ausencia de nome de contato na camada de dados) |
| QLT-01 (dados) | 3-04-01 (falha de repositorio, duplicado, `enabled=false`), 3-05-01/02 (retencao, historico desligado, NEVER_STORE, MANUAL), 3-06-02 (arquivo corrompido → defaults seguros; enum invalido → default) |
| QLT-03 | 3-03-02 (`SchemaExportTest`: v1 existe, versionada, com as 2 tabelas e o indice unico), 3-03-03 (invariante `fallbackToDestructiveMigration` + `app/schemas/*/1.json`), 3-07-02 (`MigrationTestHelper` abre a v1) |
| QLT-06 (parcial) | 3-01-01/02/03 (infra + fumaca), 3-04-02/03, 3-05-03, 3-07-02, 3-07-03 (suite instrumentada completa verde com XML arquivado) |

Nenhum requisito da fase fica sem task. Nenhuma task fica sem `<automated>`.

---

## Wave 0 Requirements

Infraestrutura que **bloqueia** as tasks seguintes e por isso vive no plano **03-01** (wave 1),
executado antes de qualquer trabalho de Room ou DataStore:

- [ ] `app/build.gradle.kts` — `androidTestImplementation(libs.room.testing)` e
      `androidTestImplementation(libs.androidx.test.core)`. Hoje as duas estao apenas em
      `testImplementation`, onde `MigrationTestHelper` e inutil (precisa de instrumentacao)
- [ ] `app/build.gradle.kts` — `sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")`.
      **Sem isso o `MigrationTestHelper` falha com "Cannot find the schema file"**
- [ ] `scripts/run-instrumented-tests.sh` — boot headless do AVD `Medium_Phone_API_35`,
      **polling em `sys.boot_completed`** (`adb wait-for-device` sozinho NAO basta: o device
      fica `offline` por varios segundos), `connectedDebugAndroidTest`, `trap` de `emu kill`
      que roda tambem em falha, e reaproveitamento de device ja conectado.
      **Nenhum `checkpoint:human-action`** — subir emulador e automatizavel (decisao do usuario)
- [ ] `app/src/androidTest/.../InstrumentationSmokeTest.kt` — prova de que o ciclo roda

**Nao e Wave 0, deliberadamente:**

- Os invariantes de `fallbackToDestructiveMigration` e `app/schemas/*/1.json` ficam no
  plano **03-03**, junto com a criacao do banco: liga-los antes do `@Database` existir
  deixaria `verify-invariants.sh` vermelho sem defeito real.
- A correcao dos XMLs de backup fica no plano **03-02** (wave 1, paralelo), porque precede
  o `BackupRulesTest` mas nao bloqueia nada de Room.
- A ampliacao do filtro do Kover para `data.*`/`settings.*` fica no **ultimo** plano da fase
  (**03-07**). Liga-la antes dos testes existirem quebra o build — licao literal da Phase 2,
  onde o gate foi adiado de 02-01 para 02-05. E o gate so e aceito depois de demonstrado
  falhando (bound temporario em 99).

**Instalacao de framework: nenhuma.** Room 2.8.4, DataStore 1.2.1, KSP 2.3.10 e AndroidX Test
ja estao no catalogo e foram verificados funcionando por build real na pesquisa (incluindo o
`ksp { arg("room.schemaLocation", ...) }` legado, que **nao** deve migrar para o Room Gradle
Plugin). A fase apenas move duas dependencias para a configuracao `androidTest`.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Comportamento real de backup em nuvem / device-transfer em aparelho Samsung com conta Google | PRV-03, HST-06 | Exige `bmgr`/conta real e restauracao em outro aparelho; a politica de validacao fisica do ROADMAP (2026-07-28) proibe `checkpoint:human-*` nas Fases 1–8. A verificacao automatizada cobre a **declaracao** (o XML lido por teste), nao a execucao do backup pelo sistema | Cenario a acrescentar em `docs/TESTE-FISICO-SAMSUNG.md` na Phase 9: `adb shell bmgr backupnow org.sentinela.app`, restaurar em outro aparelho e confirmar que whitelist, historico e configuracoes voltam VAZIOS. O verifier desta fase trata como **deferred to Phase 9**, nunca como gap |
| Percentil de `contains()` em aparelho fisico (vs emulador) | WLT-07 | O numero de gate e medido no emulador, que e o ambiente reprodutivel. Aparelho fisico e mais rapido, mas o comportamento de I/O do OEM so se conhece em campo | Cenario da Phase 9: instalar o APK release e registrar os percentis do `SENTINELA\|contains\|` em logcat no Samsung. Diferido, nao gap |

**Nao entra nesta tabela:** rodar testes de DAO e de migracao. Eles executam **de verdade**
em emulador nesta fase (decisao do usuario, 2026-07-29) — excecao deliberada e restrita a
politica de validacao fisica, porque emulador para SQLite/Room e **infraestrutura de teste**,
nao validacao de campo. Comportamento dependente de OEM/telefonia (Samsung, DND, One UI)
continua integralmente na Phase 9. **Emulador que nao sobe e blocker reportado no SUMMARY,
nunca troca silenciosa por teste JVM.**

---

## Validation Sign-Off

- [x] Todas as 18 tasks tem `<automated>` no `<verify>` — nenhuma referencia `MISSING`
- [x] Todas as tasks tem `<read_first>` e `<acceptance_criteria>` verificaveis por grep/comando
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 identificado e sequenciado primeiro (plano 03-01, wave 1); o gate do Kover
      deliberadamente adiado para o plano 03-07 — ampliar o filtro antes dos testes existirem
      quebraria o build
- [x] As duas afirmacoes "comprovadamente/medida" do ROADMAP tem teste que **falha** quando a
      propriedade quebra: backup (`BackupRulesTest`, falha ao remover um `<exclude>`) e indice
      (`EXPLAIN QUERY PLAN`, falha ao remover o `@Index`). O cronometro **nao** e usado como
      prova de indice — medido: 0,047 ms full scan vs 0,032 ms indexado com 1.000 linhas,
      indistinguivel de ruido
- [x] Nenhuma flag de watch mode em comando algum
- [x] Latencia de feedback dentro do orcamento (< 60 s no comando rapido; emulador sobe 1x por sessao)
- [x] Todo requisito da fase (WLT-01..04, WLT-07, HST-01..06, ENG-01, QLT-01, QLT-03, QLT-06, PRV-03)
      mapeado a pelo menos uma task
- [x] Itens que exigem aparelho fisico registrados como diferidos; **nenhum `checkpoint:human-*`**
      em nenhum dos 7 planos da fase
- [x] Gate probatorio pos-`clean` com `--no-build-cache` exigido antes de `/gsd:verify-work`
      (`FROM-CACHE` tem o mesmo defeito probatorio que `UP-TO-DATE`)
- [x] `nyquist_compliant: true` definido no frontmatter

**Approval:** approved 2026-07-29

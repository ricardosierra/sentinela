---
phase: 4
slug: contatos-do-aparelho
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
updated: 2026-07-29
---

# Phase 4 — Validation Strategy

> Contrato de validacao da fase: como cada task produz feedback automatico durante a execucao.
> Diferenca central em relacao a Phase 3: esta fase toca **dado pessoal de terceiros** pela
> primeira vez. O risco nao e performance — a pesquisa mediu a consulta direta ao provider em
> p50 ~2 ms com 5.000 contatos, folgado nos 200 ms. O risco e **privacidade**, e por isso a
> prova exigida e automatizada (invariante + teste de schema), nunca revisao de codigo.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (JVM)** | JUnit 4 `4.13.2` sobre AGP 9.3.0 / Gradle 9.6.1 / JDK 17 — JVM puro. **Sem Robolectric** (4.16.1 nao suporta compileSdk 37 — blocker no STATE) |
| **Framework (instrumentado)** | AndroidX Test `AndroidJUnitRunner` (ja em `defaultConfig`), `androidx.test.ext:junit-ktx 1.3.0`, `androidx.test:core-ktx 1.7.0`, `androidx.test:rules 1.7.0` |
| **Config file** | `app/build.gradle.kts` — `testOptions`, `sourceSets androidTest assets`, bloco `kover` |
| **Cobertura** | Kover `0.9.9`, gate `koverVerify minBound(80)`; base medida na pesquisa: **97,2881%**. `data.contacts.*` **ja entra** pelo include `data.*` existente — **nenhuma edicao de `includes`**. Um unico `excludes` novo, classe **nomeada**: `org.sentinela.app.data.contacts.ContactsContractLookupSource` (so executa em teste instrumentado, que o Kover nao mede — mesma razao do gerado pelo Room na Fase 3) |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` (AVD `Medium_Phone_API_35` headless, poll em `sys.boot_completed`, `trap` de `emu kill`). **`connectedDebugAndroidTest` NAO aceita `--tests`** — o script traduz para `tests_regex` |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Pre-requisitos** | `ANDROID_HOME`; JDK 17 via `gradle.properties`; `-XX:MaxMetaspaceSize=1g` (ja presente); AVD `Medium_Phone_API_35` |
| **Estimated runtime** | quick ~15–20 s · instrumentado ~30 s incremental (**+ 7–14 s medidos por classe que usa a agenda de 5.000 contatos**) + 2–4 min de boot a frio · full ~5–8 min |
| **Relatorios de evidencia** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/{html/index.html,report.xml}` · lint: `app/build/reports/lint-results-debug.xml` · detekt: `app/build/reports/detekt/detekt.xml` · androidTest: `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` · **logcat por teste (onde saem os percentis):** `.../connected/debug/Medium_Phone_API_35(AVD) - 15/logcat-<classe>-<metodo>.txt` |

**Dependencia nova: nenhuma.** `adoptShellPermissionIdentity` vem do `Instrumentation`;
`Flow.debounce` vem das coroutines ja no projeto. Nenhum framework a instalar.

**Armadilhas de script herdadas e ainda validas:**
- Nome do XML do androidTest contem parenteses e espacos — sempre glob `TEST-*.xml`, nunca
  caminho montado a mao.
- `grep -c` sai com codigo 1 quando o resultado e 0. `set -e` continua **proibido** em
  `scripts/verify-invariants.sh`, e **nunca** acrescentar `|| echo 0` (duplicaria a saida).
  Padrao correto: `[ "$(grep -c ... )" -eq 0 ]`.
- `schemas/` e input declarado das `Test` tasks (Fase 3). Teste que le arquivo de disco sem
  input declarado vai `UP-TO-DATE` e reporta verde falso.

**Armadilha NOVA desta fase — auto-sabotagem de invariante:** o Bloco 6 procura literais
(`ContactsContract`, identificadores de nome/foto/lookup do provider, `DataStore`, `edit {`)
dentro de `app/src/main/java`. Um **comentario** do proprio codigo que repita esses literais
derruba o invariante sem defeito real. Por isso todo KDoc da fase descreve a proibicao em
portugues ("nunca projetar nome nem foto do contato"), sem escrever os identificadores. Mesma
regra vale para o comentario do `AndroidManifest.xml` diante da variavel `FUTURE`.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest` (< 30 s). Tasks que tocam **so**
  codigo instrumentado acrescentam `bash scripts/run-instrumented-tests.sh --tests "*Padrao"`
  com o emulador ja de pe.
- **Apos cada wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
  Nas waves 1–3 o `koverVerify` ainda nao tem o exclude da fonte do provider: usar
  `./gradlew koverLog` e conferir o percentual manualmente. O exclude so entra no plano **04-05**.
- **Phase gate (antes de `/gsd:verify-work`):** suite JVM **e** instrumentada verdes
  **pos-`clean`**, com `--no-build-cache`, `N actionable tasks: M executed` e **M > 0**.
  Arquivado em `04-EVIDENCE.md`.
- **Latencia maxima de feedback:** < 60 s no comando rapido. Emulador sobe **uma vez** por sessao.
- **Nenhum comando usa watch mode.**

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | CTT-01, CTT-04 | script (manifest mesclado + allowlist + FUTURE) | `./gradlew assembleDebug && bash scripts/verify-invariants.sh` | ✅ estende script das Fases 1–3 (**2 edicoes obrigatorias**) | ⬜ pending |
| 4-01-02 | 01 | 1 | CTT-01, CTT-04 | instrumentado (fixture de agenda) | `bash scripts/run-instrumented-tests.sh --tests "*ContactsFixtureSmokeTest"` | ❌ **Wave 0** — bloqueia o plano 04-04 inteiro | ⬜ pending |
| 4-02-01 | 02 | 1 | CTT-01 | **unit (funcao pura, 4 estados)** | `./gradlew testDebugUnitTest --tests "*ContactsPermissionStateTest"` — ≥ 8 `@Test`, enum travado em 4 entradas | ❌ criado pela task | ⬜ pending |
| 4-02-02 | 02 | 1 | CTT-01 | unit (DataStore em `TemporaryFolder`) | `./gradlew testDebugUnitTest --tests "*DataStoreSettingsRepositoryTest"` — +≥ 5 `@Test` | ✅ estender | ⬜ pending |
| 4-02-03 | 02 | 1 | CTT-01 | build + detekt/lint (camada fina sem decisao) | `./gradlew :app:compileDebugKotlin testDebugUnitTest detekt lint` + `grep -c` de zero ramo de decisao | ❌ criado pela task | ⬜ pending |
| 4-03-01 | 03 | 2 | CTT-02 | unit (normalizador com `TestMetadata`) + invariante de pureza | `./gradlew testDebugUnitTest --tests "*LibPhoneNumberNormalizerTest" && bash scripts/verify-invariants.sh` — +≥ 6 `@Test` | ✅ estender | ⬜ pending |
| 4-03-02 | 03 | 2 | CTT-02, CTT-04 | build + greps de projecao minima e observer correto | `./gradlew :app:compileDebugKotlin detekt lint && bash scripts/verify-invariants.sh` | ❌ criado pela task | ⬜ pending |
| 4-03-03 | 03 | 2 | CTT-02, CTT-04 | **unit (fonte falsa + contador de consultas)** | `./gradlew testDebugUnitTest --tests "*ContactKeyCacheTest" --tests "*DefaultContactLookupRepositoryTest"` — ≥ 6 e ≥ 10 `@Test`; **prova estrutural por contador, cronometro proibido no arquivo** | ❌ criado pela task | ⬜ pending |
| 4-04-01 | 04 | 3 | CTT-02 | instrumentado (provider real, **sonda dupla**) | `bash scripts/run-instrumented-tests.sh --tests "*ContactLookupSourceTest"` — ≥ 7 `@Test`, inclui contato gravado como `(11) 91234-5678`; **falha demonstrada com sonda unica** | ❌ criado pela task | ⬜ pending |
| 4-04-02 | 04 | 3 | CTT-02 | instrumentado (observer + debounce) | `bash scripts/run-instrumented-tests.sh --tests "*ContactsObserverTest"` — ≥ 4 `@Test`, `CountDownLatch`; contagem de callbacks **reportada, nao afirmada** | ❌ criado pela task | ⬜ pending |
| 4-04-03 | 04 | 3 | CTT-02 | instrumentado (5.000 contatos, percentis) | `bash scripts/run-instrumented-tests.sh --tests "*ContactLookupPerformanceTest"` — `@BeforeClass`; assert primario **mediana** (`p50 < 10 ms` quente, `p50 < 50 ms` direto); **p95/max reportados, nao afirmados** | ❌ criado pela task | ⬜ pending |
| 4-05-01 | 05 | 4 | CTT-04 | script (Bloco 6) + **unit que le o schema exportado** | `./gradlew assembleDebug testDebugUnitTest --tests "*SchemaExportTest" lint detekt && bash scripts/verify-invariants.sh` — falha demonstrada com coluna `display_name` simulada | ✅ estender script + `SchemaExportTest` | ⬜ pending |
| 4-05-02 | 05 | 4 | CTT-01, CTT-02 | build + gate de cobertura (wiring + exclude nomeado) | `./gradlew assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh` — gate demonstrado falhando com `minBound(99)` | ❌ criado pela task | ⬜ pending |
| 4-05-03 | 05 | 4 | CTT-01, CTT-02, CTT-04 | evidencia pos-`clean` + pendencias da Phase 9 | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` | ❌ `04-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| **CTT-01** | 4-01-01 (`READ_CONTACTS` no manifest mesclado + allowlist + gravacao de agenda no `FUTURE`, com vermelho demonstrado), 4-02-01 (4 estados por funcao pura; enum travado em 4 entradas), 4-02-02 (flag `contacts_permission_asked` no DataStore **existente**, persistindo entre instancias), 4-02-03 (camada fina com atalho para as Configuracoes e zero decisao propria), 4-03-03 (sem permissao ⇒ `UNAVAILABLE`, **nunca** `MISS`, com a fonte nem consultada), 4-05-02 (repositorio ligado no `AppContainer`, nada em `Application.onCreate`) |
| **CTT-02** | 4-03-01 (`nationalDigits` — insumo da segunda sonda), 4-03-02 (sonda dupla com `Uri.encode`, projecao minima, observer em `AUTHORITY_URI` com `notifyForDescendants = true`), 4-03-03 (cache preguicoso nunca aguardado; **uso do cache provado por contador**; invalidacao com debounce de 750 ms), 4-04-01 (HIT/MISS reais, incluindo grafia nacional), 4-04-02 (invalidacao efetiva: contato novo vira HIT, removido volta a MISS; rajada ⇒ 1 reconstrucao), 4-04-03 (mediana dentro do orcamento com 5.000 contatos + assert estrutural do cache), 4-05-02 (exclude nomeado mantem a logica pura dentro do gate de 80%) |
| **CTT-04** | 4-01-01 (gravacao na agenda barrada para sempre no `FUTURE`), 4-01-02 (fixture insere contatos **sem** permissao nova em manifest algum), 4-03-02 (nenhuma coluna de identidade projetada; so `cursor.count`; `ContactsContract` confinado em `data/contacts/`), 4-03-03 (cache guarda **somente** `Set<String>` de chaves; log so com cardinalidade e resultado), 4-05-01 (**Bloco 6**: schema exportado sem coluna de contato, fronteira de import, zero projecao de identidade, `data/contacts/` sem persistencia — com vermelho demonstrado), `BackupRulesTest` da Fase 3 continua verde (nenhum arquivo novo a excluir do backup) |

**CTT-03 nao gera task:** politicas por origem no `CallDecisionEngine` estao completas desde a
Phase 2 (matriz parametrizada de 48 casos). Reabrir seria retrabalho.

Nenhum requisito da fase fica sem task. Nenhuma task fica sem `<automated>`.

---

## Wave 0 Requirements

Infraestrutura que **bloqueia** as tasks seguintes, concentrada no plano **04-01** (wave 1):

- [ ] `app/src/main/AndroidManifest.xml` — `READ_CONTACTS` (`docs/PERMISSOES.md:14` e a fonte
      canonica e ja autoriza a Fase 4; leitura **bloqueante**, conteudo **nao muda**)
- [ ] `scripts/verify-invariants.sh` — as **DUAS** edicoes no mesmo commit: `READ_CONTACTS` na
      `ALLOWLIST` **e** removida do `FUTURE`, com a permissao de gravacao de agenda acrescentada
      ao `FUTURE`. Medido: sem as duas, o script devolve `== 2 invariante(s) violado(s) ==` e
      **todo build verde da fase fica bloqueado**
- [ ] `app/src/androidTest/.../ContactsTestFixture.kt` — `adoptShellPermissionIdentity` +
      `applyBatch` em lotes de 300 + `wipe`. **Bloqueia todas as tasks do plano 04-04.**
      Medido e **reprovado**: permissao de gravacao no manifest de `androidTest` **nao funciona**
      (a instrumentacao roda no uid do app; `GrantPermissionRule` nao concede o que o pacote nao
      declara). Nenhum `app/src/androidTest/AndroidManifest.xml` deve ser criado

Em paralelo (wave 1, sem conflito de arquivo), o plano **04-02** entrega o enum + a funcao pura
de estado de permissao e o flag persistido, que a Phase 7 consome.

**Nao e Wave 0, deliberadamente:**

- O `excludes` do Kover fica no **ultimo** plano da fase (**04-05**). Liga-lo antes de a classe
  existir nao faz sentido, e ligar gate antes dos testes quebra o build — licao literal das
  Phases 2 e 3. O gate so e aceito depois de demonstrado falhando (`minBound(99)` temporario).
- O **Bloco 6** de invariantes fica em **04-05**: as checagens 6.2/6.3/6.4 apontam para
  `data/contacts/`, que so ganha implementacao no plano 04-03. Liga-las antes deixaria
  `verify-invariants.sh` vermelho sem defeito real (mesmo padrao da Fase 3, que adiou os
  invariantes de Room para o plano que criou o banco).

**Instalacao de framework: nenhuma.** Nenhuma dependencia nova entra no catalogo.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Tela de onboarding que pede `READ_CONTACTS` com explicacao antes do dialogo do sistema | CTT-01 (criterio 1 do ROADMAP) | **Nao e manual: e outra fase.** A tela e Compose e pertence a **Phase 7** por decisao do CONTEXT. Esta fase entrega manifest, maquina de estado e a string `contacts_permission_rationale`, ja existente em `res/values/strings.xml` | O verifier desta fase trata como **deferred to Phase 7**, nunca como gap. Nenhum plano da fase cria Compose |
| Negacao permanente real ("nao perguntar de novo") no aparelho | CTT-01 | Exige interacao humana com o dialogo do sistema duas vezes; a politica do ROADMAP (2026-07-28) proibe `checkpoint:human-*` nas Fases 1–8. A parte automatizavel — a **regra** de decisao — e coberta por funcao pura com 4 estados | Cenario da Phase 9: negar duas vezes, confirmar atalho para as Configuracoes e ausencia de nova pergunta na abertura seguinte. Estado inspecionavel por `adb shell dumpsys package org.sentinela.app` (`USER_SET` / `USER_FIXED`); reset por `adb shell pm clear-permission-flags org.sentinela.app android.permission.READ_CONTACTS user-set user-fixed`. **Deferred, nao gap** |
| Fracao de uma agenda BR real com numero sem DDI / normalizado nulo pelo provider | CTT-02 | A pesquisa provou o **mecanismo** no emulador (contato nacional estrangeiro devolve 0 linhas por E.164, e um fixo do Rio virou `+12132165498`), mas a **frequencia** em aparelho BR real e desconhecida | Cenario da Phase 9: contato importado de vCard sem DDI na agenda real do Samsung deve ser reconhecido como HIT. A sonda dupla e implementada de qualquer forma (custo ~2 ms medido) |
| p95 e max do lookup em aparelho fisico | CTT-02 | O emulador mede o scheduler do host tanto quanto o provider — a Phase 3 tirou um p95 do assert por falhar ~1 em 5 execucoes. O assert primario e a **mediana** | Cenario da Phase 9: registrar as linhas `SENTINELA\|contacts\|` do logcat no Samsung com agenda real. No CI o p95 e **reportado, nao afirmado** — o numero nao foi afrouxado |
| Provider de contatos do One UI (Samsung substitui o app de Contatos; o `ContactsProvider2` e AOSP) | CTT-02 | Risco baixo, nao zero; `CLAUDE.md` proibe hack preventivo de OEM sem prova em aparelho | Cenario da Phase 9. Divergencia observada vira entrada em `docs/LIMITACOES.md` |

**Nao entra nesta tabela:** rodar os testes de agenda no emulador. Eles executam **de verdade**
(mesma excecao aberta e validada na Fase 3: emulador para provider/SQLite e **infraestrutura de
teste**, nao validacao de campo). **Emulador que nao sobe e blocker reportado no SUMMARY, nunca
troca silenciosa por teste JVM.**

---

## Validation Sign-Off

- [x] Todas as 13 tasks tem `<automated>` no `<verify>` — nenhuma referencia `MISSING`
- [x] Todas as tasks tem `<read_first>` e `<acceptance_criteria>` verificaveis por grep/comando,
      com strings exatas e nenhum criterio subjetivo
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 identificado e sequenciado primeiro (plano 04-01, wave 1): manifest + as **duas**
      edicoes do script + fixture de agenda. Bloco 6 e exclude do Kover deliberadamente adiados
      para o plano 04-05 — liga-los antes das classes existirem deixaria o build vermelho sem
      defeito real
- [x] Todas as afirmacoes "medida/comprovadamente" do ROADMAP tem teste que **falha** quando a
      propriedade quebra: sonda dupla (`ContactLookupSourceTest` fica vermelho com sonda unica),
      uso do cache (contador de consultas, nao cronometro), nao-vazamento (Bloco 6.1 + o caso
      novo do `SchemaExportTest` ficam vermelhos com uma coluna `display_name` simulada),
      allowlist de permissao (script vermelho ao remover a linha), gate de cobertura
      (`minBound(99)` temporario)
- [x] **Cronometro nunca e usado como prova de estrutura** (licao da Phase 3): o assert de "usa
      cache" e um contador de consultas ao provider; o cache e justificado no codigo por
      **correcao de chave e corte de cauda**, nao por velocidade — a sonda direta ja cabe no
      orcamento (p50 1,95 ms com 5.000 contatos, medido)
- [x] Asserts primarios sempre na **mediana**; p95 e max reportados em logcat e transformados em
      cenario da Phase 9, sem afrouxar numero
- [x] Nenhuma flag de watch mode em comando algum
- [x] Latencia de feedback dentro do orcamento (< 60 s no comando rapido; emulador sobe 1x por
      sessao; fixture de 5.000 contatos isolado em `@BeforeClass` de uma unica classe)
- [x] Todo requisito da fase (CTT-01, CTT-02, CTT-04) mapeado a pelo menos uma task; CTT-03
      registrado como ja completo desde a Phase 2
- [x] Itens que exigem aparelho fisico ou UI registrados como diferidos (Phase 7 para a tela,
      Phase 9 para o aparelho); **nenhum `checkpoint:human-*`** em nenhum dos 5 planos
- [x] Nenhuma permissao nova alem de `READ_CONTACTS`; gravacao na agenda barrada em manifest
      algum e travada pelo `FUTURE`
- [x] Gate probatorio pos-`clean` com `--no-build-cache` exigido antes de `/gsd:verify-work`
      (`FROM-CACHE` tem o mesmo defeito probatorio que `UP-TO-DATE`)
- [x] `nyquist_compliant: true` definido no frontmatter

**Approval:** approved 2026-07-29

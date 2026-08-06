---
phase: 03-dados-locais
plan: 07
subsystem: dados-locais
tags: [appcontainer, di-manual, room, datastore, migracao, kover, evidencia]
requires:
  - "03-02 (XMLs de backup corrigidos)"
  - "03-04 (RoomWhitelistRepository + WhitelistPerformanceTest)"
  - "03-06 (DataStoreSettingsRepository + incrementAppOpenCount)"
provides:
  - "AppContainer com instancia UNICA de SentinelaDatabase e de DataStore"
  - "settingsRepository, whitelistRepository e blockedCallRepository como singletons by lazy"
  - "AppContainer.onAppOpened(): incrementa o contador (ENG-01) e poda a retencao (HST-02)"
  - "MigrationHarnessTest: MigrationTestHelper abrindo a v1 exportada + banco de producao na v1"
  - "Gate koverVerify ampliado para data.* e settings.*, com o gerado pelo Room excluido"
  - "03-EVIDENCE.md: saidas reais pos-clean --no-build-cache, percentis, EQP e invariantes"
affects:
  - app/src/main/java/org/sentinela/app/SentinelaApp.kt
  - app/build.gradle.kts
  - gradle/libs.versions.toml
  - docs/TESTE-FISICO-SAMSUNG.md
tech-stack:
  added: []
  patterns:
    - "Instancia unica de DataStore e contrato de runtime, nao estilo: duas sobre o mesmo arquivo derrubam o processo"
    - "onCreate da Application nunca faz I/O sincrono: onAppOpened so lanca no escopo de IO"
    - "Codigo gerado pelo Room fica FORA do denominador do Kover: so roda instrumentado, que o Kover nao mede"
    - "Resolucao consistente do AGP propaga a versao do runtime principal para o androidTest: piso de dependencia so-de-teste precisa ser declarado no principal"
    - "Assert que fica vermelho sem regressao real corroi a suite: medir no emulador, cobrar em hardware"
key-files:
  created:
    - app/src/androidTest/java/org/sentinela/app/data/local/db/MigrationHarnessTest.kt
    - .planning/phases/03-dados-locais/03-EVIDENCE.md
  modified:
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/src/main/java/org/sentinela/app/SentinelaApp.kt
    - app/src/androidTest/java/org/sentinela/app/data/local/db/WhitelistPerformanceTest.kt
    - app/build.gradle.kts
    - gradle/libs.versions.toml
    - docs/TESTE-FISICO-SAMSUNG.md
    - .planning/phases/03-dados-locais/03-VALIDATION.md
decisions:
  - "p95 da whitelist sai do assert do emulador e vira cenario 35 da validacao fisica (Phase 9); o numero de 5 ms NAO foi afrouxado — decisao do usuario, 2026-07-29"
  - "p50 < 1 ms e o EXPLAIN QUERY PLAN continuam quebrando o build: o sinal estavel e a prova estrutural ficam no CI"
  - "Kover exclui data.local.db.*, *_Impl e annotatedBy(Room.Dao/Database): o gerado so roda instrumentado e daria falso-vermelho"
  - "Piso de kotlinx-serialization 1.8.1 declarado no runtime PRINCIPAL: a resolucao consistente do AGP nao deixa o androidTest divergir"
  - "MigrationTestHelper(Instrumentation, Class<out RoomDatabase>) e a sobrecarga nao-deprecada do Room 2.8.4 (as deprecadas recebem assetsFolder: String)"
  - "regionProvider mantem o TODO de persistencia da preferencia de regiao: nao e requisito da Phase 3"
metrics:
  duration: ~35 min
  tasks: 3
  files: 9
  completed: 2026-07-29
---

# Phase 3 Plan 07: Wiring, Harness de Migracao e Evidencia Summary

O `AppContainer` passou a compor o banco, o DataStore e os tres repositorios como
instancias unicas, abrir o app agora incrementa o contador e poda o historico fora da
main thread, o harness de migracao esta de pe no emulador e o gate de cobertura mede
`data/` e `settings/` sem falso-vermelho do codigo gerado — tudo arquivado em
`03-EVIDENCE.md` com saida real pos-`clean --no-build-cache`.

## O que foi feito

**Task 1 — wiring dos singletons** (`51d7e5c`)
- `SentinelaDatabase` construido uma unica vez, com `addMigrations(*SENTINELA_MIGRATIONS)`.
  Instanciar de novo custaria abertura de SQLite no caminho quente do Service (Fase 5).
- `DataStore` criado por `PreferenceDataStoreFactory.create` sobre
  `files/datastore/sentinela_settings.preferences_pb`. **Nao** por delegate de Context: o
  runtime derruba o processo com duas instancias sobre o mesmo arquivo, entao o singleton
  aqui e contrato de execucao. O diretorio `datastore` inteiro ja esta fora do backup pela
  exclusao recursiva dos dois XMLs (plano 03-02), entao o nome do arquivo nao vira contrato.
- `appScope` com `SupervisorJob`: uma falha na poda nao pode cancelar o collector que
  aquece o cache das configuracoes, que e quem serve o caminho quente.
- `onAppOpened()` liga os dois metodos que existiam sem chamador — `incrementAppOpenCount()`
  (ENG-01) e `pruneNow()` (HST-02) — e `SentinelaApp.onCreate` so o **lanca**: `onCreate`
  roda na main thread e define o cold start do Service, entao I/O sincrono ali sairia do
  orcamento de resposta ao Telecom.
- `database` fica privado; so os repositorios sao expostos.

**Task 2 — harness de migracao** (`65c7903`)
- `MigrationHarnessTest` com 2 testes verdes: o helper cria e abre a v1 a partir do JSON
  exportado e confirma `whitelist` e `blocked_call` no `sqlite_master`; e o banco de
  producao, com a mesma cadeia de migracoes que o container monta, abre reportando versao 1.
- Sobrecarga usada: `MigrationTestHelper(Instrumentation, Class<out RoomDatabase>)`,
  confirmada **nao-deprecada** por `javap -v` no artefato resolvido do Room 2.8.4 — as
  deprecadas sao as que recebem `assetsFolder: String`. O plano pedia esse registro.
- Com so a v1 nao ha migracao a testar; o que se prova e que o **harness** funciona, para
  que a primeira migracao real ja nasca verificavel. Inventar uma migracao falsa nao
  provaria nada.

**Task 3 — gate de cobertura e evidencia** (`ab62f3e`)
- Filtro do Kover ampliado para `domain.* + phone.* + data.* + settings.*`, excluindo
  `data.local.db.*`, `*_Impl` e `annotatedBy(androidx.room.Dao/Database)`.
- **97,2881%** com o denominador novo (era 97,619% so com domain+phone). A queda de 0,33
  ponto mostra que as camadas novas entraram bem cobertas.
- Gate demonstrado falhando antes de ser aceito, como na Phase 2: com `minBound(99)`,
  `> Rule ... violated: lines covered percentage is 97.288100, but expected minimum is 99`.
  Restaurado para 80 — o bound **nunca** foi afrouxado.
- `03-EVIDENCE.md` criado com a sequencia completa, os percentis e o EQP do logcat real.

## Evidencia

```
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
BUILD SUCCESSFUL — 71 actionable tasks: 71 executed
```
**71 de 71 executadas**: nenhuma task reaproveitada, entao o verde nasceu desta execucao.

- JVM: **245 testes, 0 falhas** (`app/build/test-results/testDebugUnitTest/*.xml`)
- Instrumentado: `<testsuites tests="30" failures="0" errors="0" skipped="0">`
- `bash scripts/verify-invariants.sh` → `== todos os invariantes OK ==`, com os 4
  invariantes do Bloco 5 verdes e a allowlist de permissoes intacta (nenhuma permissao
  nova entrou nesta fase)

Logcat real do `WhitelistPerformanceTest`:
```
SENTINELA|contains|entries=1000|p50=0.228375|p95=5.188167|p99=8.717959
SENTINELA|EQP|SCAN CONSTANT ROW
SCALAR SUBQUERY 1
SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)
```

## Decisao do usuario aplicada: o p95 da whitelist

O usuario decidiu (2026-07-29) resolver o concern que o plano 03-04 deixou aberto:

- `p50 < 1 ms` **continua** sendo assert que quebra o build (medido 0,190–0,228 ms, ~4x de folga).
- `p95 < 5 ms` deixa de quebrar o build **no emulador** e vira o **cenario 35** de
  `docs/TESTE-FISICO-SAMSUNG.md`, medido em Samsung fisico na Phase 9. O teste imprime o
  numero e um aviso quando estoura, mas nao falha.
- O `EXPLAIN QUERY PLAN` **continua** quebrando o build — e ele a garantia real do indice.

A execucao desta sessao ilustrou o problema com precisao incomum: **p95 = 5,188 ms**. Sob o
assert antigo o build teria ficado vermelho sem nenhuma regressao — com o p50 em 0,228 ms e
o indice comprovadamente em uso. O numero de 5 ms **nao foi afrouxado**: continua sendo o
compromisso de produto, so que cobrado onde a medicao significa alguma coisa. Num emulador o
p95 mede o scheduler do host tanto quanto o SQLite, e aumentar a amostragem piorou a cauda
(6,21 ms com 1.000/2.000) em vez de estabiliza-la.

Registrado em `03-EVIDENCE.md` secao 4.1, no cenario 35 do roteiro fisico e na linha 3-04-03
de `03-VALIDATION.md`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Detekt reprovou o spread operator exigido pelo criterio de aceite**
- **Found during:** Task 1
- **Issue:** `addMigrations(*SENTINELA_MIGRATIONS)` dispara `SpreadOperator` no `detekt.yml`
  compartilhado — mas o criterio de aceite do plano exige exatamente essa linha literal.
- **Fix:** `@Suppress("SpreadOperator")` na propriedade `database`, com a justificativa no
  KDoc: o spread copia a cadeia uma unica vez, na criacao do banco, fora do caminho quente,
  e e a assinatura que o Room oferece. Suprimido no ponto de uso em vez de afrouxar a regra
  global — mesma disciplina do `MagicNumber` no plano 03-05.
- **Files modified:** `app/src/main/java/org/sentinela/app/AppContainer.kt`
- **Commit:** `51d7e5c`

**2. [Rule 3 - Blocking] `MigrationTestHelper` quebrava com AbstractMethodError de serializacao**
- **Found during:** Task 2
- **Issue:** o primeiro `MigrationHarnessTest` falhou ao ler o schema exportado com
  `java.lang.AbstractMethodError: abstract method "kotlinx.serialization.KSerializer[]
  GeneratedSerializer.typeParametersSerializers()"` dentro de `SchemaBundle.deserialize`.
  Causa: o `room-migration` 2.8.4 tem serializadores compilados contra a API 1.8+, mas o
  runtime resolvido era **1.7.3**, arrastado pelo `lifecycle-viewmodel-savedstate 2.11.0`.
  O detalhe que importa: o `debugAndroidTestRuntimeClasspath` **nao** pode divergir do
  `debugRuntimeClasspath` — o AGP aplica resolucao consistente e marca as versoes como
  `{strictly}`. Um piso declarado so em `androidTestImplementation` seria ignorado.
- **Fix:** `constraints { implementation(libs.kotlinx.serialization.core) }` com piso 1.8.1
  (a versao que o proprio `room-migration` pede) no bloco principal de dependencias, com
  `because(...)`. **Nao acrescenta biblioteca ao APK**: `kotlinx-serialization-core` ja
  entrava via lifecycle — muda so a versao. Entrada nova no version catalog, com o motivo
  registrado ali tambem.
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Commit:** `65c7903`

Nenhum checkpoint humano foi emitido. Nenhuma permissao nova, nenhum `fallbackToDestructive-
Migration`, nenhum WorkManager, nenhum Hilt/Koin, nenhuma chamada de rede, nenhum numero
completo em log.

## Fora de escopo, deliberadamente nao mexido

- **`AppContainer.regionProvider`** mantem `userPreference = RegionProvider { null }` e o TODO
  apontando para a persistencia da preferencia de regiao. **Nao e requisito da Phase 3** — o
  plano manda explicitamente deixar como esta e reportar. Persistir uma preferencia que
  nenhuma tela le ainda seria escopo inventado; entra com a UI.
- Os TODOs das Fases 4, 5 e 6 no container seguem intactos (contatos, notificador, discador).

## Para as proximas fases

- O container esta pronto para a Fase 5: `decisionEngine`, `phoneNumberNormalizer`,
  `settingsRepository`, `whitelistRepository` e `blockedCallRepository` sao todos `by lazy`,
  entao o Service so paga o que tocar.
- A poda roda na abertura do app e apos cada gravacao. Nenhum agendador em segundo plano
  existe nem deve existir no MVP.
- `search()` do `RoomWhitelistRepository` segue fora da interface `PersonalWhitelistRepository`
  (o container expoe o tipo da interface): a Fase 8 decide o contrato quando a tela existir.
- O gate de cobertura agora cobre `data/` e `settings/` — codigo novo nesses pacotes exige teste.

## Self-Check: PASSED

- `app/src/androidTest/java/org/sentinela/app/data/local/db/MigrationHarnessTest.kt` — FOUND
- `.planning/phases/03-dados-locais/03-EVIDENCE.md` — FOUND
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — FOUND, modificado
- Commits `51d7e5c`, `65c7903`, `ab62f3e` — todos FOUND em `git log`
- 245 testes JVM + 30 instrumentados, 0 falhas; `koverVerify`, `lint`, `detekt` e os
  invariantes verdes; `git status` limpo ao fim

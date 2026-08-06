---
phase: 03-dados-locais
plan: 06
subsystem: dados-locais
tags: [datastore, settings, cache, privacidade, tdd, eng-01]
requires:
  - "02-xx (SettingsRepository, ScreeningSettings, OriginPolicy/BlockMode/FallbackPolicy)"
  - "03-05 (ScreeningSettings.historyEnabled + retentionPolicy, RetentionPolicy.fromId)"
provides:
  - "DataStoreSettingsRepository: implementacao real de SettingsRepository sobre DataStore Preferences"
  - "snapshot() servido de cache @Volatile aquecido por collector no init"
  - "Leitura tolerante: IOException/lixo binario e enum desconhecido caem nos defaults seguros do MVP"
  - "appOpenCount: Flow<Int> + incrementAppOpenCount(): Int (ENG-01)"
  - "27 testes JVM puros de DataStore com TemporaryFolder, sem Robolectric"
affects:
  - app/src/main/java/org/sentinela/app/settings/
tech-stack:
  added: []
  patterns:
    - "DataStore recebido por construtor, nunca por delegate de Context: a instancia unica e contrato de runtime"
    - "Cache @Volatile no caminho quente do Service; disco so na primeira leitura"
    - "Enum persistido por nome (ou id textual), com leitura tolerante em vez de conversao crua"
    - "Teste de DataStore em JVM: arquivo novo por teste + scope cancelado no @After"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt
    - app/src/test/java/org/sentinela/app/settings/AppOpenCounterTest.kt
  modified: []
decisions:
  - "Contador de aberturas mora no DataStore junto das configuracoes — nao merece tabela Room"
  - "snapshot() nunca toca disco depois do aquecimento: cache @Volatile alimentado por onEach no proprio Flow"
  - "Chave textual do Preferences e contrato de disco: renomear chave descarta a configuracao do usuario"
  - "SettingsRepository ficou intacto: as 3 assinaturas nao mudaram; o contador e API extra da implementacao"
metrics:
  duration: ~12 min
  tasks: 2
  files: 3
  completed: 2026-07-29
---

# Phase 3 Plan 06: Configuracoes em DataStore Summary

As configuracoes de triagem agora persistem de verdade em DataStore Preferences, com o
`snapshot()` do caminho quente servido de memoria, leitura que degrada para os defaults
seguros do MVP diante de arquivo corrompido ou valor desconhecido, e o contador de
aberturas do app morando no mesmo arquivo — 27 testes JVM puros, sem instrumentacao.

## O que foi feito

**Task 1 — repositorio DataStore com cache volatil** (RED `7b301e6`, GREEN `204ebf4`)
- `DataStoreSettingsRepository` recebe o `DataStore<Preferences>` pronto pelo construtor.
  Nenhum delegate `preferencesDataStore(...)` de Context: duas instancias sobre o mesmo
  arquivo derrubam o processo, entao a instancia unica do `AppContainer` (plano 03-07) e
  contrato de execucao, nao preferencia de estilo.
- `snapshot()` devolve o cache `@Volatile` aquecido por um collector no `init`. A pesquisa
  mediu 10,9 ms na primeira leitura de disco e p95 3,87 ms nas seguintes; somado a Room e
  normalizacao isso comeria metade do orcamento de 200 ms do Service.
- `catch { IOException -> emptyPreferences() }` **antes** do `map`: arquivo corrompido cai
  nos defaults seguros em vez de matar o Flow — e o Flow que morre derruba a decisao.
- Os 11 campos mapeados: 6 booleanos, 3 `OriginPolicy`/`BlockMode`/`FallbackPolicy` por
  `name`, `RetentionPolicy` pelo `id` textual. Leitura sempre com fallback ao default do
  campo (`entries.firstOrNull { it.name == raw } ?: padrao`) — nunca conversao crua, que
  lancaria em dado antigo, e nunca a posicao da constante, que mudaria de significado ao
  reordenar o enum.
- Contador de aberturas (ENG-01) na mesma classe: `appOpenCount: Flow<Int>` e
  `incrementAppOpenCount(): Int`, atomico pelo `edit()` transacional do DataStore.
- RED provado: `:app:compileDebugUnitTestKotlin` falhou antes da classe existir.

**Task 2 — suite JVM com TemporaryFolder** (`1226620`)
- `DataStoreSettingsRepositoryTest`: **21 testes** (plano pedia 16). Cobre os defaults do
  MVP, o round-trip dos 11 campos, a recriacao do repositorio sobre o MESMO arquivo com
  todos os campos em valor nao-default, updates que compoem, emissao do Flow via turbine e
  o cache (`assertSame` entre dois `snapshot()`).
- Dois testes de degradacao: enum desconhecido gravado direto no arquivo
  (`POLITICA_DO_FUTURO`, `???`, `""`, `eterno`) volta nos defaults sem lancar; e um arquivo
  com lixo binario tambem volta em `ScreeningSettings()`.
- Um teste le o `Preferences` cru depois do `update` e afirma `"SILENCE"` e `"7d"` no disco:
  se alguem trocar a persistencia para a posicao da constante, isso quebra.
- `AppOpenCounterTest`: **6 testes** (plano pedia 5), incluindo a persistencia real — o
  contador continua de 2 para 3 depois de recriar o repositorio sobre o mesmo arquivo.
- Infra obrigatoria: arquivo novo por teste e `scope.cancel()` no `@After`. Sem isso o
  DataStore mantem o arquivo travado e o teste seguinte quebra.

## Evidencia

```
./gradlew testDebugUnitTest lint detekt --rerun-tasks
BUILD SUCCESSFUL — 41 actionable tasks: 41 executed
```
Nenhuma task UP-TO-DATE ou FROM-CACHE: o verde foi produzido nesta execucao.

Somatorio dos `TEST-*.xml`: **245 testes, 0 falhas**, dos quais
`DataStoreSettingsRepositoryTest tests="21" failures="0"` e
`AppOpenCounterTest tests="6" failures="0"`.

`./gradlew koverLog` → `application line coverage: 97.619%` (inalterado: o filtro do Kover
ainda cobre so `domain` + `phone`; ampliar para `data.*`/`settings.*` e do plano 03-07, e
foi deliberadamente **nao** tocado aqui).

`bash scripts/verify-invariants.sh` → `== todos os invariantes OK ==`.

Criterios mecanicos do plano, todos verdes: `@Volatile private var cached`,
`emptyPreferences()` e `incrementAppOpenCount` presentes; contagem **0** para
`preferencesDataStore(`, para posicao-de-constante e para conversao crua de enum;
contagem 0 de Robolectric/`@Config` e de pausa de thread nos testes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `@get:Rule` combinado com `@JvmField` desliga a rule do JUnit**
- **Found during:** Task 1
- **Issue:** o `TemporaryFolder` anotado com `@get:Rule` **e** `@JvmField` compila, mas a
  rule nunca roda — os 5 testes falharam com `IllegalStateException` na primeira chamada de
  `newFile()`, porque a pasta nunca foi criada.
- **Fix:** so `@get:Rule` com tipo explicito (`val tmp: TemporaryFolder = TemporaryFolder()`).
  O alvo do getter e o que o JUnit enxerga; `@JvmField` elimina o getter e a anotacao se perde.
- **Files modified:** `app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt`
- **Commit:** `204ebf4`

Nenhum checkpoint humano foi emitido. Nenhuma dependencia nova (DataStore e turbine ja
estavam no catalogo), nenhuma permissao nova, nenhuma chamada de rede, nenhum valor de
configuracao ou numero em log, nenhuma string de UI criada.

## Para as proximas fases

- `DataStoreSettingsRepository` ainda **nao** esta no `AppContainer`: a instancia unica do
  `DataStore` (via `PreferenceDataStoreFactory.create` apontando para
  `files/datastore/settings.preferences_pb`) e a ligacao do repositorio sao do plano 03-07,
  junto da chamada de `pruneNow()` na abertura e da ampliacao do filtro do Kover.
- `incrementAppOpenCount()` esta pronto mas ainda nao e chamado por ninguem — quem chama e a
  abertura do app (03-07 / Fase 8); o convite de avaliacao a cada 5 aberturas e da Fase 9.
- Nenhuma tela le essas configuracoes ainda: os controles de historico e retencao sao Fase 8,
  com os rotulos em `strings.xml` pt-BR.

## Self-Check: PASSED

- `app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt` — FOUND
- `app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt` — FOUND
- `app/src/test/java/org/sentinela/app/settings/AppOpenCounterTest.kt` — FOUND
- Commits `7b301e6`, `204ebf4`, `1226620` — FOUND em `git log`
- 245 testes JVM, 0 falhas; `lint`, `detekt` e invariantes verdes

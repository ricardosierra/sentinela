---
phase: 03-dados-locais
plan: 03
subsystem: dados-locais
tags: [room, schema, migracao, invariantes, privacidade]
requires:
  - "03-01 (androidTest + app/schemas como asset, para os planos 04/05)"
provides:
  - "SentinelaDatabase v1 (sentinela.db) com whitelist e blocked_call"
  - "WhitelistDao com containsBlocking NAO-suspend (caminho quente da Fase 5)"
  - "BlockedCallDao com record/observe/prune/updateClassification"
  - "Converters por code/name (nunca ordinal), tolerantes na leitura"
  - "SENTINELA_MIGRATIONS: cadeia explicita de migracao (vazia na v1)"
  - "app/schemas/org.sentinela.app.data.local.db.SentinelaDatabase/1.json versionado"
  - "Bloco 5 de scripts/verify-invariants.sh: integridade do dado local"
affects:
  - app/build.gradle.kts
  - scripts/verify-invariants.sh
tech-stack:
  added: []
  patterns:
    - "DAO nao-suspend no caminho quente: o custo medido e o dispatch de corrotina, nao o SQLite"
    - "Enum persistido por code/name estavel; ordinal nunca toca o disco"
    - "Leitura de enum tolerante a valor desconhecido — uma linha corrompida nao derruba a tela"
    - "Arquivo lido por teste precisa ser declarado como input da Test task, senao o cache falsifica o verde"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/data/local/db/SentinelaDatabase.kt
    - app/src/main/java/org/sentinela/app/data/local/db/WhitelistEntity.kt
    - app/src/main/java/org/sentinela/app/data/local/db/WhitelistDao.kt
    - app/src/main/java/org/sentinela/app/data/local/db/BlockedCallEntity.kt
    - app/src/main/java/org/sentinela/app/data/local/db/BlockedCallDao.kt
    - app/src/main/java/org/sentinela/app/data/local/db/Converters.kt
    - app/src/main/java/org/sentinela/app/data/local/db/Migrations.kt
    - app/src/test/java/org/sentinela/app/data/local/db/ConvertersTest.kt
    - app/src/test/java/org/sentinela/app/data/local/db/SchemaExportTest.kt
    - app/schemas/org.sentinela.app.data.local.db.SentinelaDatabase/1.json
  modified:
    - app/build.gradle.kts
    - scripts/verify-invariants.sh
decisions:
  - "Migrations.kt descreve a migracao destrutiva em vez de escrever o nome do metodo: o invariante casa ate em comentario, porque linha comentada vira linha ativa"
  - "schemas/ declarado como input das Test tasks — sem isso SchemaExportTest passava verde com o schema apagado"
  - "Leitura de enum desconhecido cai em UNKNOWN_NUMBER/UNCLASSIFIED em vez de lancar"
metrics:
  duration: ~14 min
  tasks: 3
  files: 12
  completed: 2026-07-29
---

# Phase 3 Plan 03: Schema v1 do Banco Local Summary

O `sentinela.db` v1 existe como contrato versionado: duas tabelas (`whitelist` com indice
UNICO em `number_key`, `blocked_call` com indice de retencao), DAOs cujo caminho quente e
deliberadamente nao-suspend, conversores que persistem enums por `code`/`name` e um schema
JSON exportado que quatro invariantes de shell e cinco testes se recusam a deixar sumir.

## O que foi feito

**Task 1 — entidades, DAOs, conversores e o `@Database` v1** (`44b83c9`)
- Pacote `data/local/db/` criado com os 6 arquivos de producao. O subpacote isola o codigo
  gerado pelo Room (`*_Impl`) para o filtro do Kover poder exclui-lo por pacote no plano 03-07.
- `WhitelistDao.containsBlocking` e **nao-suspend** por medicao, nao por estilo: a pesquisa da
  fase mediu p95 9,12 ms com `suspend` contra 3,59 ms sem — o gargalo e o dispatch de corrotina.
- `BlockedCallDao` usa `@Insert` **sem** `onConflict = REPLACE`: REPLACE deleta e reinsere,
  trocando o id que a UI da Fase 8 vai referenciar.
- `Converters` mapeia `DecisionReason` pelo `code` e `CallClassification` pelo `name`.
  A leitura e tolerante — valor desconhecido cai em `UNKNOWN_NUMBER`/`UNCLASSIFIED` em vez de
  lancar, porque explodir ali derrubaria a tela de historico inteira por causa de uma linha.
- `ConvertersTest`: 6 testes cobrindo round-trip das 9 entradas de `DecisionReason`, das 3 de
  `CallClassification`, os dois fallbacks, unicidade dos codes e um assert de que o valor
  persistido nunca e numerico (ordinal disfarcado).
- RED provado antes: a primeira execucao falhou em `compileDebugUnitTestKotlin` (Converters
  inexistente). Depois: `BUILD SUCCESSFUL`, `tests="6" failures="0" errors="0"`.

**Task 2 — o schema exportado como contrato** (`4849494`)
- `SchemaExportTest`: 5 testes JVM puros (sem Room, sem Android) sobre `File("schemas")` —
  existencia e tamanho do `1.json`, `"version": 1`, as duas tabelas, o indice unico e a
  ausencia de banco de rascunho sobrando.
- `1.json` **nao** esta em `.gitignore` (`git check-ignore` sai 1) e foi commitado.

**Task 3 — Bloco 5 dos invariantes** (`a7e617a`)
- Quatro checagens novas: migracao destrutiva, `allowMainThreadQueries`, schema v1 presente e
  coluna de nome de contato na camada de dados. Blocos 1-4 nao foram movidos nem reescritos
  (so um comentario ajustado, ver deviations).

## Evidencia

Verificacao do plano, com `clean` **e** `--no-build-cache` (58 tasks executadas, nenhuma
`FROM-CACHE`/`UP-TO-DATE`):

```
BUILD SUCCESSFUL in 12s
58 actionable tasks: 58 executed
```
`testDebugUnitTest`: **172 tests, 0 failures**. `koverVerify` continua verde (o filtro cobre
`domain`+`phone`; `data.local.db` fica fora ate o plano 03-07, como manda o contexto da fase).

Saida do bloco novo:
```
== Bloco 5: integridade do dado local ==
ok:   sem fallbackToDestructiveMigration (migracao explicita obrigatoria)
ok:   sem allowMainThreadQueries
ok:   schema Room v1 exportado (app/schemas/*/1.json)
ok:   nenhuma coluna de nome de contato na camada de dados
== todos os invariantes OK ==
```

### Provas de falha (o teste so vale se souber ficar vermelho)

**`exportSchema = false`** — com o export desligado e `app/schemas/<db>/` removido:
```
> Task :app:testDebugUnitTest FAILED
SchemaExportTest > schema v1 tem as duas tabelas FAILED
SchemaExportTest > nenhum schema de rascunho versionado FAILED
SchemaExportTest > schemas exportados existem FAILED
SchemaExportTest > whitelist tem indice unico na chave FAILED
SchemaExportTest > schema v1 declara versao 1 FAILED
5 tests completed, 5 failed
BUILD FAILED
```
Revertido para `true`: `BUILD SUCCESSFUL`, `1.json` regenerado byte a byte identico (4045 B).

**Invariantes** (3 das 4 checagens novas provadas, cada uma com `exit 1` real):
- migracao destrutiva reintroduzida (comentada!) →
  `FAIL: fallbackToDestructiveMigration proibido (Fase 3)`, `EXIT=1`
- `app/schemas/<db>/` movido para fora → `FAIL: app/schemas/<db>/1.json ausente`, `EXIT=1`
- `// val contactName: String? = null` na entidade →
  `FAIL: nome de contato na camada de dados`, `EXIT=1`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `SchemaExportTest` passava VERDE com o schema apagado**
- **Found during:** Task 2, ao reverter a prova de falha
- **Issue:** o teste le `File("schemas")` direto do disco, mas o Gradle nao tem como saber
  disso. Com o diretorio apagado, `testDebugUnitTest` foi resolvido como `FROM-CACHE`/
  `UP-TO-DATE` e reportou `BUILD SUCCESSFUL` — falso verde exatamente na propriedade que o
  teste existe para proteger. E o mesmo defeito probatorio que a Phase 1 documentou para o
  cache de build, agora manifestado dentro de um teste.
- **Fix:** `tasks.withType<Test>().configureEach { inputs.dir(.../"schemas") }` em
  `app/build.gradle.kts`. Provado: apagando o schema **sem** `--rerun-tasks`, os 5 testes
  falham; restaurando, voltam ao verde.
- **Files modified:** `app/build.gradle.kts`
- **Commit:** `4849494`

**2. [Rule 3 - Blocking] O comentario de `Migrations.kt` disparava o proprio invariante**
- **Found during:** Task 1
- **Issue:** o texto ditado pelo plano para `Migrations.kt` escreve o nome do metodo proibido
  para explicar que ele e proibido. Isso violava o criterio de aceite da Task 1 (contagem 0 em
  `app/src/main`) e faria o Bloco 5 falhar sozinho no estado limpo do repo.
- **Fix:** o comentario passou a **descrever** o metodo ("o fallback que recria o banco do zero
  quando falta um caminho de upgrade") em vez de nomea-lo, e registra por que o invariante casa
  ate em comentario. O grep foi mantido **estrito de proposito** — a prova de falha do proprio
  plano insere a chamada comentada, entao afrouxar para ignorar comentarios tornaria o
  invariante decorativo. Mesma classe do achado 3 da wave 1.
- **Files modified:** `app/src/main/java/org/sentinela/app/data/local/db/Migrations.kt`
- **Commit:** `44b83c9`

**3. [Rule 3 - Blocking] `|| echo 0` sobrevivia num comentario do Bloco 4**
- **Found during:** Task 3
- **Issue:** o criterio de aceite exige `grep -c '|| echo 0'` igual a 0, mas a unica ocorrencia
  era um comentario **alertando contra** o padrao, escrito na Phase 1.
- **Fix:** comentario reescrito preservando integralmente o sentido ("NAO acrescentar um
  fallback com `echo` no ramo de erro: o zero ja foi impresso e a contagem sairia duplicada").
  Edicao de comentario apenas — nenhum bloco 1-4 foi movido ou teve logica alterada.
- **Files modified:** `scripts/verify-invariants.sh`
- **Commit:** `a7e617a`

Nenhum checkpoint humano foi emitido. Nenhuma permissao nova, nenhuma dependencia nova, nenhum
`fallbackToDestructiveMigration`, nenhuma chamada de rede.

## Notas de escopo

- O criterio da Task 1 `grep -q '"tableName": "whitelist"'` inicialmente pareceu falhar: era erro
  de aspas na minha propria linha de comando (glob guardado em variavel), nao defeito do schema.
  Reexecutado corretamente, os quatro padroes estao presentes.
- `Converters` esta declarado em `@TypeConverters` mas as colunas de `BlockedCallEntity` sao
  `String` por decisao do plano — o conversor serve o mapeamento entidade↔dominio dos
  repositorios (planos 04/05). Nenhum aviso do Room no build.
- Os DAOs ainda nao tem teste instrumentado: `WhitelistDaoTest`/`BlockedCallDaoTest` e a prova
  de indice por `EXPLAIN QUERY PLAN` sao dos planos 04 e 05, que agora podem rodar em paralelo.

## Self-Check: PASSED

- 10 arquivos criados — todos FOUND (7 de producao, 2 de teste, 1 schema JSON)
- `app/build.gradle.kts`, `scripts/verify-invariants.sh` — FOUND, modificados
- Commits `44b83c9`, `4849494`, `a7e617a` — FOUND em `git log`
- `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest lint detekt` →
  BUILD SUCCESSFUL, 172 testes, 0 falhas
- `bash scripts/verify-invariants.sh` → `== todos os invariantes OK ==`, exit 0

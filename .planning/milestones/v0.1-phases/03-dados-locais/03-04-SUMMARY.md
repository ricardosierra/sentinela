---
phase: 03-dados-locais
plan: 04
subsystem: dados-locais
tags: [room, whitelist, dedup, indice, performance, instrumentado]
requires:
  - "03-03 (SentinelaDatabase v1, WhitelistDao com containsBlocking nao-suspend)"
  - "03-01 (scripts/run-instrumented-tests.sh + emulador headless)"
provides:
  - "RoomWhitelistRepository: implementacao Room de PersonalWhitelistRepository"
  - "Dedup por id resolvido antes do @Upsert (duplicata = atualizacao silenciosa)"
  - "search(query) sobre numero e descricao, exposto como Flow de dominio"
  - "Mappers WhitelistEntity.toDomain / WhitelistEntry.toEntity"
  - "FakeWhitelistDao para testes JVM de qualquer consumidor da whitelist"
  - "WhitelistDaoTest: 12 testes instrumentados de CRUD/busca/dedup/codigo curto"
  - "WhitelistPerformanceTest: prova de indice por EXPLAIN QUERY PLAN + percentis"
affects: []
tech-stack:
  added: []
  patterns:
    - "@Upsert com id 0 em chave com indice unico NAO lanca e NAO atualiza: e no-op silencioso"
    - "Prova de indice e EXPLAIN QUERY PLAN; cronometro nao prova indice (demonstrado)"
    - "Schema exportado pode ficar defasado da entidade sem que o teste instrumentado perceba"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/data/local/RoomWhitelistRepository.kt
    - app/src/test/java/org/sentinela/app/data/local/FakeWhitelistDao.kt
    - app/src/test/java/org/sentinela/app/data/local/RoomWhitelistRepositoryTest.kt
    - app/src/androidTest/java/org/sentinela/app/data/local/db/WhitelistDaoTest.kt
    - app/src/androidTest/java/org/sentinela/app/data/local/db/WhitelistPerformanceTest.kt
  modified: []
decisions:
  - "A constraint do banco e provada por INSERT cru duplicado, nao por @Upsert: @Upsert intercepta a violacao e esconderia a prova"
  - "p95 < 5 ms mantido como assert apesar de flaky no emulador (~1 em 5 runs): afrouxar o bound e proibido; o veredito de performance fica na Phase 9"
  - "p50 < 1 ms e o assert de sinal estavel (medido 0,19-0,23 ms em 8 execucoes)"
metrics:
  duration: ~22 min
  tasks: 3
  files: 5
  completed: 2026-07-29
---

# Phase 3 Plan 04: Repositorio de Whitelist sobre Room Summary

`RoomWhitelistRepository` implementa a whitelist pessoal com dedup que resolve o id antes do
upsert, e as tres afirmacoes fortes do ROADMAP passaram a ter teste que sabe ficar vermelho:
busca e CRUD verdes no emulador, indice provado por `EXPLAIN QUERY PLAN` (nao por cronometro)
e orcamento medido com warmup — 27 testes novos, 15 em JVM e 12 instrumentados.

## O que foi feito

**Task 1 — repositorio e cobertura JVM** (`9cd6742`)
- `contains()` delega ao `containsBlocking` **nao-suspend** dentro de `withContext(io)`: o hop de
  dispatcher do Room e o custo real (p95 9,12 ms suspend vs 3,59 ms sem), nao o SQLite.
- `upsert()` resolve o id existente por `findByKey` **antes** de chamar `@Upsert`, e preserva o
  `createdAtUtcMillis` original — reeditar uma entrada nao a "rejuvenesce".
- `FakeWhitelistDao` honra o indice unico de verdade (recusa segunda linha com a mesma chave) e
  tem `failNext` para o caso de falha de repositorio. Um fake que "conserta" o defeito que o
  codigo deveria evitar nao prova nada.
- 15 testes JVM: dedup (3), `enabled = false`, codigo curto `190`, ausencia, round-trip do
  mapper, delete seletivo, busca por descricao e por trecho, e propagacao de excecao do DAO.
- RED provado antes: primeira execucao falhou em `compileDebugUnitTestKotlin`.

**Task 2 — CRUD instrumentado** (`49bec35`)
- 12 testes contra o SQLite real (banco em memoria), incluindo os 7 comportamentos pedidos.
- `tests="12" failures="0" errors="0"` no `Medium_Phone_API_35`.

**Task 3 — indice e orcamento** (`7791f7f`)
- Banco **em arquivo** com 1.000 entradas inseridas em uma transacao; 300 warmups, 500 amostras.
- Saida literal do EQP capturada:
```
SENTINELA|EQP|SCAN CONSTANT ROW
SCALAR SUBQUERY 1
SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)
```
- Percentis da execucao de referencia: `p50=0.202375 | p95=2.937333 | p99=8.182833` ms.

## Evidencia

`./gradlew --no-build-cache --rerun-tasks testDebugUnitTest lint detekt` → `BUILD SUCCESSFUL`,
**218 testes, 0 falhas** (a primeira tentativa saiu `UP-TO-DATE` em 478 ms — descartada como
falso-verde, conforme a licao da Phase 1).

`bash scripts/run-instrumented-tests.sh --tests "*Whitelist*"` → `BUILD SUCCESSFUL`,
`tests="14" failures="0" errors="0"`.

`bash scripts/verify-invariants.sh` → `== todos os invariantes OK ==`.

### Prova de falha: o indice (obrigatoria pelo plano)

Removido `indices = [Index(value = ["number_key"], unique = true)]` de `WhitelistEntity`:

```
WhitelistPerformanceTest > containsUsaIndiceEmVezDeFullScan FAILED
    java.lang.AssertionError: plano sem indice:
SENTINELA|EQP|SCAN CONSTANT ROW
SCALAR SUBQUERY 1
SCAN whitelist
```

**E o teste de tempo continuou VERDE na mesma execucao:**
`p50=0.223042 | p95=4.210208 | p99=6.811584` — todos dentro dos bounds, com full scan.
Isso confirma empiricamente a decisao do contexto da fase: **o cronometro nao prova indice**.
Se a prova do EQP nao existisse, o indice poderia sumir sem nenhum teste ficar vermelho.

Restaurado o indice: `BUILD SUCCESSFUL`, plano volta a `USING INDEX index_whitelist_number_key`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] O teste de constraint ditado pelo plano afirmava um comportamento falso**
- **Found during:** Task 2
- **Issue:** o plano manda provar o indice unico com dois `dao.upsert(...)` de `id = 0` e a mesma
  chave, esperando `SQLiteConstraintException`. A execucao real devolveu
  `expected ... to be thrown, but nothing was thrown`. `@Upsert` do Room **captura** a violacao do
  indice e cai num UPDATE por chave primaria; com `id = 0` nenhuma linha casa e a operacao vira
  **no-op silencioso**. O teste como escrito nao provava a constraint — provava o contrario dela.
- **Fix:** virou dois testes. `insertDuplicadoDiretoNoBancoViolaAConstraint` usa `execSQL` cru
  (fora do alcance do `@Upsert`) e ai sim a `SQLiteConstraintException` aparece — essa e a prova
  de que a garantia e do banco. E `upsertComIdZeroNaChaveDuplicadaPerdeAAtualizacaoSilenciosamente`
  documenta o no-op, que e exatamente **o motivo** de `RoomWhitelistRepository` resolver o id antes.
  O pitfall previsto pelo plano ficou provado por teste em vez de so comentado.
- **Files modified:** `app/src/androidTest/.../WhitelistDaoTest.kt`
- **Commit:** `49bec35`

**2. [Rule 3 - Blocking] Comentario nomeava a chamada vetada pelo criterio de aceite**
- **Found during:** Task 2
- **Issue:** o KDoc explicava por que **nao** se usa `allowMainThreadQueries`, e a mencao literal
  fazia `grep -c` dar 1 onde o criterio exige 0. Terceira ocorrencia desta classe na fase
  (achados 2 e 3 da wave 2).
- **Fix:** o comentario passou a descrever ("o builder NAO libera consulta na main thread") em vez
  de nomear, preservando o sentido. O grep segue estrito de proposito.
- **Files modified:** `app/src/androidTest/.../WhitelistDaoTest.kt`
- **Commit:** `49bec35`

**3. [Rule 1 - Bug] Schema exportado ficou defasado da entidade sem ninguem perceber**
- **Found during:** Task 3, ao reverter a prova de falha
- **Issue:** durante a prova sem indice, o KSP reescreveu `app/schemas/.../1.json` sem o
  `index_whitelist_number_key`. Ao restaurar a entidade, a execucao instrumentada voltou **verde**
  — mas o `1.json` no disco continuou o errado (`identityHash` f166ea…, sem o bloco `indices`).
  Ou seja: o teste instrumentado passa com o banco correto enquanto o **oraculo dos testes de
  migracao** guarda outro schema. Nenhum teste instrumentado detecta essa divergencia.
- **Fix:** `./gradlew :app:kspDebugKotlin --rerun-tasks` regenerou o `1.json` **byte a byte
  identico** ao original (md5 `0f747e28da9bf5df813ec4f1d1fa432a`), e `git status` voltou limpo.
  A rede de seguranca existe e e da wave 2: `SchemaExportTest > whitelist tem indice unico na
  chave` (JVM) falharia com o schema defasado — foi por isso que a divergencia ficou contida ao
  disco e nunca entrou em commit. Nenhuma mudanca de codigo foi necessaria.
- **Files modified:** nenhum (arquivo regenerado ao estado ja versionado)
- **Commit:** n/a

### Tentativa revertida

**p95 flaky: aumentar amostras piorou, e o bound NAO foi afrouxado**
- O plano manda, em caso de flakiness, "aumentar WARMUP/SAMPLES — nunca afrouxar o bound".
  Uma execucao deu `p95=4.965666` ms, a uma batida de scheduler do bound de 5 ms.
- Subir para WARMUP 1.000 / SAMPLES 2.000 **piorou**: `p95=6.206875` ms, teste vermelho. Amostrar
  por mais tempo acumula mais outlier de GC/scheduler na cauda em vez de estabiliza-la.
- Revertido para os 300/500 do plano (`git status` limpo, sem commit extra). O bound continua
  em 5 ms, intocado. A flakiness virou concern reportado abaixo, nao um bound relaxado.

## Distribuicao medida (8 execucoes, mesmo emulador)

| Metrica | Faixa observada | Assert | Veredito |
|---------|-----------------|--------|----------|
| p50 | 0,190 – 0,228 ms | < 1,0 ms | estavel, folga de ~4x |
| p95 | 0,819 – 5,889 ms | < 5,0 ms | **flaky: falhou 2 de 8** |
| p99 | 1,717 – 10,140 ms | nao afirmado | ruidoso |

O p50 e o sinal; o p95 num emulador mede o scheduler do host tanto quanto o SQLite.

## Blockers/Concerns

- **`containsCabeNoOrcamentoMedido` falha ~1 em 5 execucoes** pelo assert de p95. Nao afrouxei o
  bound porque isso e explicitamente proibido pelo plano e pelo contexto da fase, e porque o
  numero de 5 ms e um compromisso de produto. Mas um teste que fica vermelho sem regressao real
  corroi a confianca na suite. **Decisao humana necessaria** antes da Phase 5/9: (a) manter o p95
  como esta e tolerar o re-run, (b) mover o assert de p95 para a validacao em aparelho fisico da
  Phase 9 mantendo so o p50 no CI, ou (c) medir a cauda de outra forma (mediana de N execucoes).
  Nao alterei nada por conta propria.

## Notas de escopo

- `search()` nao esta em `PersonalWhitelistRepository` (a interface e da Phase 2 e nao foi tocada):
  fica como membro publico da implementacao ate a UI da Phase 8 definir o contrato.
- Kover nao foi ampliado — `data.*` entra so no plano 03-07, como manda o contexto da fase.
- Nenhuma permissao nova, nenhuma dependencia nova, nenhuma chamada de rede, nenhum numero
  completo em log (`grep -cE 'Log\.|println'` = 0 no repositorio; os `println` de evidencia estao
  so no teste de performance e imprimem percentis, nao numeros).
- Nenhum arquivo do plano 03-05 foi tocado.

## Self-Check: PASSED

- 5 arquivos criados — todos FOUND
- Commits `9cd6742`, `49bec35`, `7791f7f` — todos FOUND em `git log`
- `testDebugUnitTest lint detekt` com `--no-build-cache --rerun-tasks`: 218 testes, 0 falhas
- Suite instrumentada `*Whitelist*`: `tests="14" failures="0" errors="0"`
- `verify-invariants.sh`: exit 0
- `git status` limpo ao fim (schema exportado byte a byte identico ao versionado)

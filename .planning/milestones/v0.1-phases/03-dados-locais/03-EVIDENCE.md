# Phase 3 — Evidência de execução (dados locais)

> Saídas reais colhidas em 2026-07-29 no fecho do plano 03-07.
> Regra probatória da Phase 1: só vale execução **pós-`clean`** e com
> `--no-build-cache`. `UP-TO-DATE` e `FROM-CACHE` têm o mesmo defeito probatório —
> provam que o Gradle reaproveitou um resultado, não que o resultado é verdadeiro.

---

## 1. Sequência executada

```bash
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
bash scripts/verify-invariants.sh
bash scripts/run-instrumented-tests.sh
```

### 1.1 `./gradlew clean`

```
BUILD SUCCESSFUL in 548ms
1 actionable task: 1 executed
```

### 1.2 Suite JVM + gate de cobertura + estática, sem cache de build

```
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
BUILD SUCCESSFUL in 18s
71 actionable tasks: 71 executed
```

**71 de 71 tasks executadas — nenhuma reaproveitada.** É esta linha que dá valor
probatório ao verde: o resultado nasceu desta execução, do zero.

### 1.3 Invariantes

`bash scripts/verify-invariants.sh` → `== todos os invariantes OK ==`, com os
**4 invariantes do Bloco 5** (a razão de ser da fase: o risco aqui é *perder dado do
usuário*) verdes:

```
== Bloco 1: permissoes no manifest mergeado ==
ok:   sem android.permission.INTERNET (PRV-01)
ok:   permissao autorizada: android.permission.POST_NOTIFICATIONS
ok:   permissao autorizada: org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
ok:   nenhuma permissao fora da allowlist
ok:   nenhuma permissao de fase futura antecipada
ok:   servico protegido por BIND_SCREENING_SERVICE
ok:   action android.telecom.CallScreeningService registrada
== Bloco 2: rebranding centralizado ==
ok:   nenhum applicationId literal em Kotlin
ok:   nenhuma string hardcoded (text = "...") em Kotlin
ok:   nenhuma cor literal fora de ui/theme
ok:   sentinelaApplicationId usado 3x em app/build.gradle.kts
ok:   app_name definido em strings.xml
== Bloco 3: dominio e normalizacao puros ==
ok:   domain sem import de android.*
ok:   phone sem import de android.*
== Bloco 4: relatorios de qualidade ==
ok:   detekt sem issues
ok:   lint sem issues
== Bloco 5: integridade do dado local ==
ok:   sem fallbackToDestructiveMigration (migracao explicita obrigatoria)
ok:   sem allowMainThreadQueries
ok:   schema Room v1 exportado (app/schemas/*/1.json)
ok:   nenhuma coluna de nome de contato na camada de dados
== todos os invariantes OK ==
```

Confirma de uma vez: **nenhuma permissão nova entrou** no manifest mergeado nesta
fase (a allowlist segue com exatamente 2 entradas), a migração destrutiva continua
proibida, o schema v1 está versionado e nenhum nome de contato existe na camada de dados.

### 1.4 Suite instrumentada completa

```
bash scripts/run-instrumented-tests.sh
Starting 30 tests on Medium_Phone_API_35(AVD) - 15
Medium_Phone_API_35(AVD) - 15 Tests 30/30 completed. (0 skipped) (0 failed)
BUILD SUCCESSFUL in 25s
```

---

## 2. Contagem de testes

### 2.1 JVM — `app/build/test-results/testDebugUnitTest/*.xml`

```
JVM total: tests=245 failures=0 errors=0
```

### 2.2 Instrumentado — `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_35(AVD) - 15-_app-.xml`

```xml
<testsuites tests="30" failures="0" errors="0" skipped="0" time="10.867" timestamp="2026-07-29T12:16:46"
```

> O script apaga os `TEST-*.xml` antes de rodar: relatório antigo tem o mesmo defeito
> probatório que `UP-TO-DATE`. O XML acima nasceu desta execução.
> O nome do arquivo contém parênteses e espaços — sempre entre aspas ou via glob.

**Total da fase: 245 testes JVM + 30 instrumentados = 275, com 0 falhas.**

---

## 3. Gate de cobertura (Kover)

### 3.1 Denominador ampliado

O filtro passou de `domain.* + phone.*` para
`domain.* + phone.* + data.* + settings.*`, **excluindo** o código gerado pelo Room
(`data.local.db.*`, `*_Impl`, `annotatedBy(androidx.room.Dao/Database)`). O gerado só
executa em teste instrumentado, que o Kover não mede: incluí-lo derrubaria o gate com
falso-vermelho mesmo com 100% do código humano coberto. Esse código está coberto pelos
30 testes instrumentados acima.

```
./gradlew koverLog
application line coverage: 97.2881%
```

Antes da ampliação (Phase 2, só `domain`+`phone`): 97,619%. A queda de 0,33 ponto com
`data.*` e `settings.*` no denominador mostra que as camadas novas entraram bem cobertas.

### 3.2 Prova de que o gate sabe ficar vermelho

Um gate nunca demonstrado falhando não é gate. Com `minBound` temporariamente em 99:

```
./gradlew koverVerify
FAILURE: Build failed with an exception.
> Rule 'Cobertura minima de dominio, normalizacao e dados' violated:
  lines covered percentage is 97.288100, but expected minimum is 99
```

Restaurado para `minBound(80)`:

```
./gradlew koverVerify
BUILD SUCCESSFUL in 467ms
```

O bound **não foi afrouxado** em momento algum — 80 é o mesmo valor da Phase 2.

---

## 4. Percentis e plano de query (logcat real)

Linhas copiadas do `adb logcat` de uma execução real de `WhitelistPerformanceTest`
(1.000 entradas, 300 warmups, 500 amostras, banco **em arquivo**):

```
07-29 09:17:36.110  2476  2920 I System.out: SENTINELA|contains|entries=1000|p50=0.228375|p95=5.188167|p99=8.717959
07-29 09:17:36.110  2476  2920 I System.out: SENTINELA|contains|AVISO p95=5.188167 ms >= 5.0 ms (nao falha no emulador; veredito na Phase 9 em aparelho fisico)
07-29 09:17:36.572  2476  2920 I System.out: SENTINELA|EQP|SCAN CONSTANT ROW
07-29 09:17:36.572  2476  2920 I System.out: SCALAR SUBQUERY 1
07-29 09:17:36.572  2476  2920 I System.out: SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)
```

O plano de query é literal: `USING INDEX index_whitelist_number_key`. Essa é a prova
**determinística** do índice, e continua quebrando o build se o índice sumir — medido no
plano 03-04: sem o `@Index`, o EQP fica vermelho enquanto o teste de tempo permanece
**verde** (p95 4,21 ms com full scan). O cronômetro não prova índice.

### 4.1 O p95 no emulador — decisão humana de 2026-07-29

Esta execução é a ilustração exata do problema: **p95 = 5,188 ms**. Sob o assert antigo
o build teria ficado vermelho — sem nenhuma regressão real, com o p50 em 0,228 ms
(4x de folga) e o índice comprovadamente em uso.

Distribuição medida em 8 execuções no mesmo emulador (plano 03-04):

| Métrica | Faixa observada | Situação no CI | Veredito |
|---------|-----------------|----------------|----------|
| p50 | 0,190 – 0,228 ms | **assert < 1 ms — quebra o build** | estável, folga de ~4x |
| p95 | 0,819 – 5,889 ms | **reportado, não quebra** | falhou 2 de 8 sem regressão |
| p99 | 1,717 – 10,140 ms | não afirmado | ruidoso |

**Decisão do usuário:** o p50 continua sendo assert que falha o build; o p95 sai do
emulador e vira o **cenário 35** de `docs/TESTE-FISICO-SAMSUNG.md`, medido em Samsung
físico na Phase 9. O número de **5 ms não foi afrouxado** — segue sendo o compromisso de
produto, só que cobrado onde a medição significa alguma coisa. Num emulador o p95 mede o
scheduler do host tanto quanto o SQLite, e aumentar a amostragem **piorou** a cauda
(6,21 ms com 1.000/2.000 amostras) em vez de estabilizá-la. A prova estrutural do índice
(`EXPLAIN QUERY PLAN`) **continua quebrando o build** — é ela que pega regressão de verdade.

---

## 5. Harness de migração (QLT-03)

`MigrationHarnessTest`, 2 testes verdes no emulador:

- `schemaV1AbrePeloHelper` — `MigrationTestHelper` cria e abre o banco na v1 a partir do
  JSON exportado e confirma `whitelist` e `blocked_call` no `sqlite_master`.
- `bancoDeProducaoAbreNaVersao1` — o banco real, com a mesma cadeia `SENTINELA_MIGRATIONS`
  que o `AppContainer` monta, abre sem erro de validação de schema e reporta versão 1.

Sobrecarga usada no Room 2.8.4: `MigrationTestHelper(Instrumentation, Class<out RoomDatabase>)`
— confirmada **não-deprecada** por `javap -v` no artefato resolvido (as sobrecargas
deprecadas são as que recebem `assetsFolder: String`).

Com só a v1 não existe migração a testar; o que se prova é que o **harness** funciona, de
modo que a primeira migração real já nasça verificável. Inventar uma migração falsa não
provaria nada.

---

## 6. Pendências diferidas para a Phase 9

**Nenhum comportamento de OEM ou de telefonia foi validado nesta fase.** O emulador aqui é
*infraestrutura de teste* para SQLite/DataStore, não validação de campo — o que ele prova é
que as consultas, as constraints, a retenção e a persistência funcionam contra um SQLite e
um DataStore reais.

| Item | Requisito | Onde fica |
|------|-----------|-----------|
| p95 de `contains()` em hardware real (< 5 ms) | WLT-07 | `docs/TESTE-FISICO-SAMSUNG.md` cenário **35** (novo) |
| Backup em nuvem / device-transfer com conta Google real (`bmgr backupnow` + restauração) | PRV-03, HST-06 | Phase 9 — a automação cobre a **declaração** (XML lido por DOM), não a execução do backup pelo sistema |
| Comportamento de `setSkipCallLog` / notificação nativa / DND em One UI | Phase 5 | Phase 9 |

Nenhum destes é gap de cobertura: são verificações que **exigem** aparelho físico e estão
registradas no roteiro, conforme a política de validação física do ROADMAP (2026-07-28).

---

## 7. Fora de escopo, deliberadamente não corrigido

`AppContainer.regionProvider` mantém `userPreference = RegionProvider { null }` com o TODO
apontando para a persistência da preferência de região. **Não é requisito da Phase 3** — o
TODO foi mantido intacto e a preferência entra com a UI. Ampliar o escopo aqui teria
adicionado uma configuração sem tela que a leia.

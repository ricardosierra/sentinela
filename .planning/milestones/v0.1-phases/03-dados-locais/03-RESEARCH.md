# Phase 3: Dados Locais — Research

**Researched:** 2026-07-29
**Domain:** Persistência local Android (Room 2.8.4 + KSP, DataStore Preferences 1.2.1), exclusão de backup, testes instrumentados em emulador
**Confidence:** HIGH (os pontos críticos foram **medidos neste repositório**, não presumidos)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Whitelist**
- **Dedup:** índice **único** em `numberE164`. Inserir número já presente faz **upsert** da entrada
  existente (atualiza descrição/enabled), sem erro visível ao usuário e sem criar linha duplicada.
- **Códigos curtos são aceitos.** A chave segue o contrato fechado na Phase 2: E.164 para números
  normais, **dígitos crus** para valores com menos de `PhoneNumbers.LIMIAR_CURTO` (6) dígitos.
  O usuário precisa conseguir pôr `190` na whitelist. A coluna guarda a chave que o
  `PhoneNumberNormalizer` devolve — a normalização acontece **antes** de chegar ao repositório.
- **`enabled = false` faz `contains()` retornar `false`.** Desabilitar é equivalente funcional a
  remover, mas preserva a entrada para o usuário religar depois. A query de `contains()` filtra
  por `enabled = 1`.
- **Orçamento medido, não presumido:** teste que popula 1.000 entradas e exige **p95 < 5 ms** em
  `contains()`. Folga larga dentro do orçamento de 200 ms do Service. O teste deve falhar de
  verdade se o índice for removido — provar isso.

**Histórico**
- **Guarda o E.164 completo** em coluna própria, além da máscara. Log, notificação e crash report
  continuam usando **somente a máscara**.
- **Retenção padrão: 30 dias.** Opções expostas: nunca / 7 / 30 / 90 / manual.
- **A poda roda na abertura do app e após cada gravação.** Sem WorkManager.
- **Histórico ligado por padrão.** Desligável nas configurações; desligado, `record()` não grava nada.

**Persistência, backup e migração**
- **Um único banco Room `sentinela.db`** com as duas tabelas. Um schema, uma cadeia de migração.
- **`exportSchema = true`** com schemas versionados em `app/schemas/` e migrações **explícitas**.
  `fallbackToDestructiveMigration` é **proibido**.
- **Contador de aberturas vive no DataStore**, junto das configurações.
- **Backup:** `dataExtractionRules` (API 31+) **e** `fullBackupContent` (API 29–30) excluindo
  `sentinela.db`, seus `-wal`/`-shm` e o arquivo do DataStore. Verificado por **teste que lê o XML**.

**Testes instrumentados (decisão do usuário, 2026-07-29)**
- Executados **de verdade nesta fase, em emulador**. AVD: **`Medium_Phone_API_35`**.
- Exceção deliberada e restrita à política de validação física do ROADMAP (Phase 9 cobre OEM real).
- O executor sobe o emulador headless, roda `connectedDebugAndroidTest` e arquiva a saída real.
  Emulador que não sobe é **blocker reportado**, não troca silenciosa por teste JVM.
- **Nenhum plano deve emitir `checkpoint:human-action`** para isso.

### Claude's Discretion
- Nomes de tabelas/colunas, organização dos DAOs, formato exato do teste de performance e a
  estrutura dos arquivos de migração ficam a critério do executor, desde que os 6 critérios de
  sucesso passem e as decisões acima sejam honradas.

### Deferred Ideas (OUT OF SCOPE)
- UI de whitelist e histórico — Phase 8.
- Uso real dos repositórios no `CallScreeningService` — Phase 5.
- Leitura de contatos do aparelho — Phase 4.
- Sync com backend / fonte remota da whitelist — v0.2.0.
- Validação em Samsung físico — Phase 9.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Descrição | Suporte da pesquisa |
|----|-----------|---------------------|
| WLT-01 | Adicionar número com país/DDI e descrição local | Entidade Room + `upsert`; normalização já existe (Phase 2). Chave = saída do `PhoneNumberNormalizer` |
| WLT-02 | Editar, ativar/desativar, excluir | `@Update`/`@Delete` + coluna `enabled`; `contains()` filtra `enabled = 1` |
| WLT-03 | Pesquisar por número ou descrição | `@Query` com `LIKE` sobre `number_key` e `description` — **fora** do caminho quente, sem exigência de índice |
| WLT-04 | Duplicidade detectada e recusada | Índice `UNIQUE` + `@Upsert` (§ "Dedup por upsert") |
| WLT-07 | Consulta indexada e dentro do orçamento | **Medido:** `EXPLAIN QUERY PLAN` prova o índice; DAO **não-suspend** p50 0,20 ms / p95 3,59 ms em emulador (§ Medições) |
| HST-01 | Registro mínimo | `BlockedCallEntry` já modelado (máscara + E.164 + reason + notificationShown + classification) |
| HST-02 | Retenção nunca/7/30/90/manual | Enum no `ScreeningSettings` (DataStore) + `pruneOlderThan` (já na interface) |
| HST-03 | Limpar tudo / excluir individual | `clearAll()` / `deleteById()` (já na interface) |
| HST-04 | Adicionar à whitelist a partir do histórico | Coluna `numberE164` no histórico (decisão travada); **UI é Phase 8** — aqui só o dado |
| HST-05 | Marcar legítimo/indesejado | `CallClassification` já modelado; `@Query` de update de status |
| HST-06 | Banco excluído do backup | XML de backup + teste JVM que lê o XML (§ Backup) |
| ENG-01 | Contador local de aberturas | Chave `intPreferencesKey` no mesmo DataStore |
| QLT-01 (dados) | Casos obrigatórios da §13 relativos a repositório/retenção/duplicado | Testes de retenção, duplicado, falha de repositório |
| QLT-03 | Testes de migração do Room | `MigrationTestHelper` + schemas em assets do androidTest (§ Migração) |
| QLT-06 (parcial) | `connectedDebugAndroidTest` verde | **Comprovado neste repo** — o alvo roda no `Medium_Phone_API_35` (§ Emulador) |
| PRV-03 | Room/DataStore fora de backup e device-transfer | § Backup — caminho real do DataStore confirmado por exceção real do runtime |
</phase_requirements>

---

## Summary

Esta fase tem **menos risco de ferramenta do que o esperado e mais risco de premissa**. A boa
notícia: toda a cadeia Room + KSP + export de schema **já está montada e funcionando** no
`app/build.gradle.kts` — comprovado nesta pesquisa criando um `@Database` de rascunho, buildando e
vendo `app/schemas/<db>/1.json` nascer. Nenhuma mudança de plugin, nenhuma migração para o Room
Gradle Plugin, nenhum ajuste de versão de KSP é necessário.

A má notícia, e é o achado central: **duas premissas do CONTEXT.md não sobrevivem à medição.**
(1) O alvo de `contains()` p95 < 5 ms **falha** se o DAO for `suspend` — medido p95 = 9,1 ms e
p99 = 26,4 ms no emulador, porque o custo é o *dispatch de corrotina* do Room, não o SQLite (o
SQLite indexado custa 0,03 ms p50). Com um DAO **não-suspend** o p95 cai para 3,59 ms.
(2) O teste de tempo **não consegue provar o índice**: com 1.000 linhas, o full scan mediu p50
0,047 ms contra 0,032 ms do indexado — indistinguível de ruído. A prova do índice tem que ser
`EXPLAIN QUERY PLAN`, que é determinística e retorna literalmente
`SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)`.

O terceiro achado: `PreferenceDataStoreFactory.create` **lança `IllegalStateException` se houver
duas instâncias sobre o mesmo arquivo** — a mensagem de erro confirmou de quebra o caminho real do
arquivo (`/data/data/<pkg>/files/datastore/<nome>.preferences_pb`), validando que o
`data_extraction_rules.xml` atual já exclui o lugar certo. Isso torna o singleton no `AppContainer`
obrigatório por contrato do runtime, não por estilo.

**Primary recommendation:** DAO de `contains()` **não-suspend**, chamado pelo Service; snapshot de
settings servido de um **cache `@Volatile` em memória** alimentado por um collector no
`AppContainer` (a leitura direta do DataStore mediu p95 3,87 ms e cold 10,9 ms — cabe nos 200 ms,
mas é desperdício no caminho quente); prova de índice por `EXPLAIN QUERY PLAN`; prova de
performance com **budget realista de 5 ms sobre o DAO não-suspend**, medida com warmup e percentil.

---

## Standard Stack

### Core — tudo já declarado no catálogo, nada a adicionar

| Library | Version | Purpose | Status neste repo |
|---------|---------|---------|-------------------|
| `androidx.room:room-runtime` | 2.8.4 | Banco local | ✅ já em `implementation` |
| `androidx.room:room-ktx` | 2.8.4 | `Flow` + `suspend` nos DAOs | ✅ já em `implementation` |
| `androidx.room:room-compiler` | 2.8.4 | Geração via KSP | ✅ já em `ksp(...)` |
| `androidx.datastore:datastore-preferences` | 1.2.1 | Configurações + contador | ✅ já em `implementation` |
| `com.google.devtools.ksp` (plugin) | 2.3.10 | Processador | ✅ já aplicado e **verificado funcionando** |

**Verificação de versão (executada, não lembrada):**

```
$ ./gradlew -q buildEnvironment | grep ksp
com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.10
  \--- com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.10
       +--- com.google.devtools.ksp:symbol-processing-api:2.3.10
# (AGP 9.3.0 pede 2.2.10-2.0.2; o catálogo sobrepõe para 2.3.10 — resolve limpo)
```

### Supporting — o que a fase PRECISA adicionar

| Library | Coordenada | Onde | Por quê |
|---------|-----------|------|---------|
| `room-testing` | `libs.room.testing` (já no catálogo) | **`androidTestImplementation`** | `MigrationTestHelper` só funciona instrumentado |
| `androidx.test:core-ktx` | `libs.androidx.test.core` (já no catálogo) | **`androidTestImplementation`** | `ApplicationProvider` nos testes de DAO |

Ambas já existem no `libs.versions.toml`; hoje `room-testing` está apenas em `testImplementation`
(inútil — `MigrationTestHelper` precisa de instrumentação) e `androidx-test-core` só em
`testImplementation`. **Verificado nesta pesquisa:** adicionar as duas em `androidTestImplementation`
faz o `connectedDebugAndroidTest` compilar e rodar.

**Nada mais.** Sem Hilt/Koin/Dagger, sem WorkManager, sem SQLCipher, sem Room Gradle Plugin.

### Alternatives Considered

| Em vez de | Poderia usar | Tradeoff / veredito |
|-----------|--------------|---------------------|
| `ksp { arg("room.schemaLocation", ...) }` | Plugin `androidx.room` + `room { schemaDirectory(...) }` | **Ficar no `ksp arg`.** Verificado: exporta o schema corretamente com AGP 9.3.0 / Room 2.8.4, build limpo, sem warning. O plugin traria uma dependência a mais e um vetor de incompatibilidade com AGP 9 sem ganho nenhum aqui. Só valeria com múltiplos variants/flavors — não é o caso |
| DAO `suspend fun contains()` | DAO `fun contains()` não-suspend, chamado de dispatcher próprio | **Não-suspend.** Medido: suspend p95 9,1 ms / p99 26,4 ms vs não-suspend p95 3,59 ms. O overhead é do `withContext` para o executor do Room |
| `runBlocking { dataStore.data.first() }` no `snapshot()` | Cache `@Volatile` alimentado por collector | **Cache.** Ver § Pitfall 2 |
| Dois bancos Room | Um `sentinela.db` | Locked pelo usuário: um schema, uma cadeia de migração |
| Robolectric para o DAO | Instrumentado no emulador | Robolectric 4.16.1 não suporta compileSdk 37 (blocker já registrado no STATE) — e o usuário travou emulador |

---

## Architecture Patterns

### Estrutura recomendada

```
app/src/main/java/org/sentinela/app/
├── data/local/
│   ├── PersonalWhitelistRepository.kt   # interface (JÁ EXISTE — não mexer no contrato)
│   ├── BlockedCallRepository.kt         # interface (JÁ EXISTE)
│   ├── RoomWhitelistRepository.kt       # impl: DAO -> domínio
│   ├── RoomBlockedCallRepository.kt     # impl: DAO -> domínio + poda
│   └── db/                              # <-- SUBPACOTE: tudo que o KSP gera vive aqui
│       ├── SentinelaDatabase.kt
│       ├── WhitelistEntity.kt / WhitelistDao.kt
│       ├── BlockedCallEntity.kt / BlockedCallDao.kt
│       ├── Converters.kt                # DecisionReason/CallClassification <-> String
│       └── Migrations.kt                # MIGRATION_1_2... (vazio na v1)
└── settings/
    ├── SettingsRepository.kt            # interface (JÁ EXISTE)
    ├── ScreeningSettings.kt             # + RetentionPolicy, historyEnabled (JÁ EXISTE, estender)
    └── DataStoreSettingsRepository.kt   # impl + cache @Volatile + contador de aberturas
```

**Por que o subpacote `db/`:** isola o código gerado pelo Room (`*_Impl`) para o filtro do Kover
poder excluí-lo por pacote em vez de por padrão frágil de nome. Ver § Kover.

### Pattern 1: `contains()` não-suspend no caminho quente

**What:** o DAO consultado pelo `CallScreeningService` é declarado **sem `suspend`**.
**When:** exclusivamente para `contains()`. Todo o resto (CRUD, listagens) continua `suspend`/`Flow`.
**Why:** medido — o `suspend` do Room custa ~1,4 ms de mediana e cauda de 26 ms; o SELECT indexado
custa 0,03 ms. O overhead **é o dispatch**, não o banco.

```kotlin
// db/WhitelistDao.kt
@Dao
interface WhitelistDao {

    /**
     * Caminho quente do CallScreeningService (Fase 5) — deliberadamente NÃO-suspend.
     * Medido na pesquisa da Fase 3 (emulador API 35, 1.000 entradas):
     *   suspend      p50 1,46 ms | p95 9,12 ms | p99 26,39 ms
     *   nao-suspend  p50 0,20 ms | p95 3,59 ms | p99  5,46 ms
     * O chamador é responsável por não invocar na main thread.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE number_key = :key AND enabled = 1)")
    fun containsBlocking(key: String): Boolean

    @Upsert
    suspend fun upsert(entity: WhitelistEntity)

    @Query("SELECT * FROM whitelist ORDER BY created_at_utc_millis DESC")
    fun observeAll(): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist WHERE number_key LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<WhitelistEntity>>
}
```

A interface pública `PersonalWhitelistRepository.contains()` **continua `suspend`** (não mudar o
contrato já usado pelo domínio); a implementação faz `withContext(Dispatchers.IO) { dao.containsBlocking(k) }`
— ou chama direto se o Service já estiver fora da main thread. A economia real vem de o Room não
fazer seu próprio hop interno de dispatcher.

### Pattern 2: Dedup por `@Upsert` sobre índice único

```kotlin
@Entity(
    tableName = "whitelist",
    indices = [Index(value = ["number_key"], unique = true)],
)
data class WhitelistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Chave do PhoneNumberNormalizer: E.164, ou dígitos crus se < LIMIAR_CURTO (6). */
    @ColumnInfo(name = "number_key") val numberKey: String,
    val description: String? = null,
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long,
)
```

⚠️ **Armadilha real:** `@Upsert` com `id = 0` (autoGenerate) **não** casa pelo índice único — o
Room tenta INSERT, colide com o UNIQUE e faz UPDATE **por chave primária**, que com `id = 0` não
acha linha nenhuma. O padrão correto e testável é resolver o id antes:

```kotlin
override suspend fun upsert(entry: WhitelistEntry) = withContext(io) {
    val existingId = dao.findIdByKey(entry.numberE164)
    dao.upsert(entry.toEntity(id = existingId ?: 0))
}
```
Com `@Query("SELECT id FROM whitelist WHERE number_key = :key")`. Envolver em
`@Transaction` na implementação do DAO. **Teste obrigatório:** inserir a mesma chave 2× com
descrições diferentes → `observeAll()` devolve 1 linha, com a descrição nova e o `createdAt` original.

### Pattern 3: Snapshot de settings a partir de cache em memória

```kotlin
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,   // singleton — ver Pitfall 3
    scope: CoroutineScope,
) : SettingsRepository {

    @Volatile private var cached: ScreeningSettings? = null

    override val settings: Flow<ScreeningSettings> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it.toScreeningSettings() }
            .onEach { cached = it }

    init { scope.launch { settings.collect() } }   // aquece e mantém o cache

    /** Caminho quente: nunca toca disco se o cache já estiver quente. */
    override suspend fun snapshot(): ScreeningSettings =
        cached ?: settings.first().also { cached = it }

    override suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings) {
        dataStore.edit { prefs -> transform(prefs.toScreeningSettings()).writeInto(prefs) }
    }
}
```

O `catch { IOException -> emptyPreferences() }` é obrigatório: sem ele, arquivo corrompido derruba
o Flow e, por tabela, a decisão. Com ele, cai nos defaults do `ScreeningSettings` — que são os
defaults seguros do MVP.

### Pattern 4: Poda de retenção sem WorkManager

```kotlin
enum class RetentionPolicy(val days: Int?) {
    NEVER_STORE(0), DAYS_7(7), DAYS_30(30), DAYS_90(90), MANUAL(null)
}
```
- `record()` retorna cedo se `!historyEnabled` **ou** `retention == NEVER_STORE`.
- Depois de cada `record()` bem-sucedido e no `onCreate` do app: `pruneOlderThan(now - days*86_400_000L)`;
  `MANUAL` → não poda.
- O cálculo do cutoff é **função pura** → testável em JVM, sem Room. Coloque em
  `settings/RetentionPolicy.kt` (ou `domain/`) e teste lá; é o único jeito de essa regra entrar no
  gate de cobertura.

### Anti-Patterns to Avoid

- **`fallbackToDestructiveMigration()`** — proibido pelo usuário; apaga a whitelist numa atualização.
  Adicionar grep no `scripts/verify-invariants.sh` (§ Invariantes).
- **`allowMainThreadQueries()`** — mascara o problema de dispatch em vez de resolvê-lo.
- **Provar índice por cronômetro** — matematicamente inviável com 1.000 linhas (medido). Use EQP.
- **Persistir nome de contato** — `BlockedCallEntry` não tem campo de nome e **não deve ganhar um**.
- **`by preferencesDataStore(name=...)` como extensão de Context em duas classes** — o delegate é
  por-Context e é fácil duplicar. Instancie explicitamente **uma vez** no `AppContainer`.
- **`enum.ordinal` como valor persistido** — reordenar o enum corrompe dados antigos silenciosamente.
  Persistir o `name` (ou o `code` já existente do `DecisionReason`) via `@TypeConverter`, e ter um
  teste que trava a lista (padrão já usado na Phase 2 para `DecisionReason`).

---

## Don't Hand-Roll

| Problema | Não construa | Use | Por quê |
|----------|--------------|-----|---------|
| Dedup por número | `SELECT` + `if` antes do insert | `@Index(unique = true)` + resolução de id em `@Transaction` | Corrida entre UI e import; o banco é a única garantia atômica |
| Migração de schema | Escrever DDL na mão sem baseline | `exportSchema = true` + `MigrationTestHelper` | O JSON exportado é o oráculo; sem ele a migração não é verificável |
| Persistir configurações | `SharedPreferences` + listener | DataStore Preferences | Já é dependência; transacional, sem `apply()` silencioso, expõe `Flow` |
| Serializar enums | `ordinal` | `@TypeConverter` sobre `name`/`code` + teste que trava a lista | Reordenar enum corrompe histórico do usuário |
| Provar uso de índice | Benchmark de tempo | `EXPLAIN QUERY PLAN` | Medido: a diferença some no ruído com 1.000 linhas |
| Percentil de latência | `System.currentTimeMillis()` e média | `System.nanoTime()`, warmup, array ordenado | Média esconde a cauda; foi exatamente a cauda que reprovou o DAO suspend |

**Key insight:** nesta fase, "custom" quase sempre significa "não verificável". Cada afirmação do
ROADMAP ("comprovadamente excluído", "medida") só vale se existir um teste que **falha** quando a
propriedade quebra.

---

## Medições (executadas neste repositório, 2026-07-29)

Ambiente: AVD `Medium_Phone_API_35` (API 35, arm64-v8a, google_apis_playstore), headless
`-no-window -gpu swiftshader_indirect`, macOS arm64. Tabela `whitelist` com **1.000 entradas**,
índice único em `number_key`, 300 iterações de warmup + 500 medidas, `System.nanoTime()`.

| Cenário | p50 (ms) | p95 (ms) | p99 (ms) | max (ms) |
|---------|---------:|---------:|---------:|---------:|
| DAO `suspend` indexado | 1,461 | 9,118 | 26,386 | 48,626 |
| **DAO não-suspend indexado** | **0,203** | **3,591** | **5,456** | **6,805** |
| SQLite cru indexado | 0,032 | 0,323 | 1,895 | 3,468 |
| SQLite cru full scan | 0,047 | 1,666 | 3,809 | 4,845 |
| DataStore `data.first()` (cache quente) | 0,470 | 3,870 | — | 6,825 |
| DataStore `data.first()` **primeira leitura** | 10,896 | — | — | — |

**Leituras obrigatórias desses números:**

1. O gargalo é o **dispatch de corrotina**, não o SQLite (45× de diferença entre DAO suspend e
   SQLite cru).
2. **`p95 < 5 ms` só é atingível com o DAO não-suspend**, e mesmo assim com folga de apenas 1,4 ms
   no emulador. Aparelho físico é mais rápido, mas o CI/emulador é onde o gate roda.
   **Recomendação:** manter o alvo declarado em 5 ms (locked pelo usuário) **usando o DAO
   não-suspend**, e adicionar um assert secundário de `p50 < 1 ms` que é o sinal estável.
3. **O índice não é detectável por tempo** com n=1.000 (0,047 vs 0,032 ms). Qualquer teste que
   pretenda "falhar se o índice for removido" via cronômetro será um falso-verde. Use EQP.

**Saída literal do `EXPLAIN QUERY PLAN`** (é isto que o teste deve afirmar):
```
SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)
```
Note o nome auto-gerado do índice pelo Room: `index_<tabela>_<coluna>`.

---

## Common Pitfalls

### Pitfall 1: DAO `suspend` estoura o orçamento de p95
**O que dá errado:** `contains()` declarado `suspend` mede p95 9,1 ms / p99 26,4 ms — 2× o alvo.
**Por quê:** o Room roteia a query `suspend` pelo seu `TransactionExecutor` via `withContext`; o
custo é agendamento, não I/O.
**Como evitar:** DAO não-suspend para o caminho quente; a interface pública fica `suspend`.
**Sinal de alerta:** p50 acima de 1 ms com o banco quente.

### Pitfall 2: `snapshot()` lendo o DataStore direto no caminho quente
**O que dá errado:** primeira leitura mediu **10,9 ms**; leituras seguintes p95 3,87 ms. Cabe nos
200 ms, mas somado ao Room + normalização come metade do orçamento de cold start do Service.
**Como evitar:** cache `@Volatile` alimentado por um collector iniciado no `AppContainer`, com
fallback para `settings.first()` na primeira chamada.
**Sinal de alerta:** `snapshot()` aparecendo no perfil de `onScreenCall` na Phase 5.

### Pitfall 3: duas instâncias de DataStore sobre o mesmo arquivo → crash
**O que dá errado (reproduzido, mensagem literal):**
```
java.lang.IllegalStateException: There are multiple DataStores active for the same file:
/data/data/org.sentinela.app/files/datastore/scratch_settings.preferences_pb.
You should either maintain your DataStore as a singleton ...
    at androidx.datastore.core.FileStorage.createConnection(FileStorage.kt:52)
```
**Por quê:** o DataStore trava o arquivo por processo.
**Como evitar:** instância única no `AppContainer` (`by lazy`). **Nos testes**, criar sempre sobre
um arquivo único por teste (`TemporaryFolder` / `newFile()`) e cancelar o scope no `@After`.
**Bônus:** esse erro confirmou o caminho real do arquivo — insumo direto para a regra de backup.

### Pitfall 4: `@Upsert` com `id = 0` não deduplica
Ver Pattern 2. Sem resolver o id antes, o segundo insert da mesma chave lança
`SQLiteConstraintException` ou cria comportamento surpresa. **Teste obrigatório.**

### Pitfall 5: `path` ausente nos `<exclude>` de backup
A documentação oficial diz que `domain` **e** `path` são obrigatórios em `<include>`/`<exclude>`
nos dois formatos. O `data_extraction_rules.xml` atual tem `<exclude domain="database" />`
**sem `path`**. Não quebrou o build, mas é comportamento não especificado.
**Correção:** `path="."` explícito em todos os `<exclude>`, nos dois arquivos (§ Backup).

### Pitfall 6: Kover reprovando por código gerado pelo Room
Se `org.sentinela.app.data.*` entrar no filtro do Kover sem exclusões, as classes `*_Impl`
geradas pelo KSP (centenas de linhas, cobertas **só** por teste instrumentado, que o Kover **não**
mede) entram no denominador e derrubam o gate de 80%. Ver § Kover.

### Pitfall 7: emulador na primeira execução
`adb devices` mostrou `emulator-5554 offline` por vários segundos após o start. Polling em
`sys.boot_completed` é o único sinal confiável — `wait-for-device` sozinho **não** basta.

---

## Backup — configuração exata e verificação

### Caminhos reais (confirmados em runtime na API 35)

| Dado | Caminho | Domínio |
|------|---------|---------|
| `sentinela.db`, `-wal`, `-shm` | `/data/data/<pkg>/databases/` | `database` |
| DataStore Preferences | `/data/data/<pkg>/files/datastore/<name>.preferences_pb` | `file`, subpasta `datastore/` |
| SharedPreferences (não usado, mas excluir por segurança) | `/data/data/<pkg>/shared_prefs/` | `sharedpref` |

`<exclude domain="database" path="."/>` cobre **recursivamente** todo o diretório — portanto os
arquivos `-wal` e `-shm` estão cobertos sem precisar listá-los. Isso é importante: eles são
transientes e o nome exato não deve virar contrato.

### `app/src/main/res/xml/data_extraction_rules.xml` (API 31+)

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Privacidade: whitelist, historico e configuracoes NAO saem do aparelho via
     backup em nuvem nem transferencia device-to-device (docs/PRIVACIDADE.md). -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="file" path="datastore" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="file" path="datastore" />
    </device-transfer>
</data-extraction-rules>
```

### `app/src/main/res/xml/full_backup_content.xml` (API 29–30)

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Equivalente legado (API < 31) do data_extraction_rules.xml. -->
<full-backup-content>
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="file" path="datastore" />
</full-backup-content>
```

Diferença em relação ao que existe hoje: `path="."` explícito nos excludes de `database`/`sharedpref`
do `data_extraction_rules.xml`, e `datastore` sem barra final (a barra é aceita, mas sem ela o teste
fica mais fácil de escrever com igualdade exata).

O manifest **já** aponta para os dois arquivos — nada a mudar lá.

### Teste JVM que lê o XML (`app/src/test/.../BackupRulesTest.kt`)

O working dir dos testes é o diretório do módulo `app/` (fato já estabelecido na Phase 2 pelo
`TestMetadata`), então o caminho relativo funciona sem fixture nenhuma. Use
`javax.xml.parsers.DocumentBuilderFactory`, que é JDK puro — **não** parsear com regex.

```kotlin
class BackupRulesTest {

    private fun excludes(file: String, parent: String): Set<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/xml/$file"))
        val scope = doc.getElementsByTagName(parent).item(0) as Element  // ou o root
        val nodes = scope.getElementsByTagName("exclude")
        return (0 until nodes.length).map { i ->
            val e = nodes.item(i) as Element
            e.getAttribute("domain") to e.getAttribute("path")
        }.toSet()
    }

    private val required = setOf(
        "database" to ".",
        "sharedpref" to ".",
        "file" to "datastore",
    )

    @Test fun cloudBackupExcluiDadosSensiveis() =
        assertTrue(excludes("data_extraction_rules.xml", "cloud-backup").containsAll(required))

    @Test fun deviceTransferExcluiDadosSensiveis() =
        assertTrue(excludes("data_extraction_rules.xml", "device-transfer").containsAll(required))

    @Test fun fullBackupLegadoExcluiDadosSensiveis() =
        assertTrue(excludes("full_backup_content.xml", "full-backup-content").containsAll(required))

    /** Nenhuma regra de include pode reintroduzir o que foi excluido. */
    @Test fun naoExisteIncludeNosArquivosDeBackup() { /* getElementsByTagName("include").length == 0 */ }

    /** O manifest tem que apontar para os DOIS arquivos. */
    @Test fun manifestApontaParaAmbasAsRegras() {
        val m = File("src/main/AndroidManifest.xml").readText()
        assertTrue(m.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(m.contains("android:fullBackupContent=\"@xml/full_backup_content\""))
    }
}
```

**Prova de que o teste falha de verdade:** antes de commitar, remover uma linha `<exclude>` e
confirmar vermelho — mesma disciplina que a Phase 2 aplicou ao `koverVerify`.

---

## Migração e schemas

### Estado verificado

- `app/schemas/` já existe com `.gitkeep`.
- `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` já está no `app/build.gradle.kts` e
  **funciona**: o build de rascunho produziu `app/schemas/<fqcn-do-database>/1.json`.
- Nenhum warning de AGP 9 sobre `schemaLocation` não ser input de task.

### O que a fase precisa acrescentar

```kotlin
// app/build.gradle.kts, dentro de android { }
sourceSets {
    getByName("androidTest").assets.srcDir("$projectDir/schemas")
}
```
Sem isso o `MigrationTestHelper` não acha o JSON e falha com "Cannot find the schema file".

```kotlin
dependencies {
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.core)
}
```

### Harness de migração (v1 ainda não tem migração — o que testar)

Com apenas a versão 1, QLT-03 se cumpre montando o **harness verificável** e travando o invariante,
não inventando uma migração falsa. Três testes:

1. **Schema exportado existe e está versionado.** Teste JVM: `File("schemas").listFiles()` contém
   `<fqcn>/1.json` e o arquivo não está vazio. Falha se alguém desligar `exportSchema`.
2. **`MigrationTestHelper` abre a versão 1** (instrumentado) — prova que o JSON é consumível e o
   harness está de pé para a primeira migração real:
   ```kotlin
   @get:Rule val helper = MigrationTestHelper(
       InstrumentationRegistry.getInstrumentation(),
       SentinelaDatabase::class.java,
   )

   @Test fun schemaV1Abre() {
       helper.createDatabase(TEST_DB, 1).close()
   }
   ```
   ⚠️ A assinatura exata do construtor mudou entre Room 2.5/2.6/2.8 (há sobrecargas com
   `SupportSQLiteOpenHelper.Factory`, com `Class<out RoomDatabase>` e com `KClass`). **Confidence:
   MEDIUM** — o executor deve confirmar contra o autocomplete/erro do compilador na primeira
   compilação; é feedback imediato, não risco de plano.
3. **Invariante de ausência de destrutivo** (§ abaixo).

### Invariante em `scripts/verify-invariants.sh`

Acrescentar, no espírito das checagens já existentes:

```bash
# Fase 3: fallbackToDestructiveMigration apagaria a whitelist do usuario numa atualizacao.
if grep -rn "fallbackToDestructiveMigration" app/src/main --include="*.kt" >/dev/null 2>&1; then
  fail "fallbackToDestructiveMigration proibido (Fase 3): migracao tem que ser explicita"
else
  ok "sem fallbackToDestructiveMigration"
fi

# Schema da v1 exportado e versionado
if ls app/schemas/*/1.json >/dev/null 2>&1; then
  ok "schema Room v1 exportado"
else
  fail "app/schemas/<db>/1.json ausente — exportSchema desligado?"
fi
```

---

## Kover — o que incluir sem criar falso-vermelho

**Problema medido conceitualmente:** as classes `*_Impl` geradas pelo Room só executam em teste
instrumentado, e o Kover mede **apenas** `testDebugUnitTest`. Incluí-las no denominador reprova o
gate mesmo com o código humano 100% coberto.

**Recomendação concreta** para `app/build.gradle.kts`:

```kotlin
kover {
    reports {
        filters {
            includes {
                classes(
                    "org.sentinela.app.domain.*",
                    "org.sentinela.app.phone.*",
                    "org.sentinela.app.data.*",
                    "org.sentinela.app.settings.*",
                )
            }
            excludes {
                // Codigo gerado pelo Room (KSP): so executa em teste instrumentado,
                // que o Kover nao instrumenta. Fica coberto por connectedDebugAndroidTest.
                classes("org.sentinela.app.data.local.db.*")
                classes("*_Impl", "*_Impl\$*")
                // Entidades/DAOs sao contrato declarativo, nao logica.
                annotatedBy("androidx.room.Dao", "androidx.room.Database")
            }
        }
        verify { rule("Cobertura minima de dominio e dados") { minBound(80) } }
    }
}
```

O que **fica** no denominador e portanto precisa de teste JVM: `RoomWhitelistRepository` /
`RoomBlockedCallRepository` (mappers entidade↔domínio, resolução de dedup, política de poda),
`DataStoreSettingsRepository` (mapeamento Preferences↔`ScreeningSettings`, defaults, cache) e
`RetentionPolicy`. Todos são testáveis com **fakes de DAO** em JVM pura — sem Room, sem Android.

> **Ordem obrigatória (lição da Phase 2):** ampliar o filtro do Kover **depois** que os testes
> existirem, num plano posterior da fase. Ligar antes quebra o build. E o gate só é aceito depois
> de demonstrado falhando (subir o bound temporariamente).

**Sanity check antes de fechar:** rodar `./gradlew koverLog` e conferir o percentual novo; se cair
abaixo de 80, a causa quase certamente é código gerado escapando das exclusões — inspecionar
`app/build/reports/kover/html/index.html` classe a classe, não afrouxar o bound.

---

## Emulador — procedimento verificado ponta a ponta

Executado com sucesso nesta pesquisa (build, install, run, report, kill).

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

# 1. Subir headless em background
nohup "$ANDROID_HOME/emulator/emulator" -avd Medium_Phone_API_35 \
  -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect \
  > /tmp/emu.log 2>&1 &

# 2. Esperar boot DE VERDADE (wait-for-device sozinho NAO basta: o device
#    aparece como "offline" por varios segundos)
"$ANDROID_HOME/platform-tools/adb" wait-for-device shell \
  'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done; echo BOOTED'

# 3. Rodar
./gradlew :app:connectedDebugAndroidTest

# 4. Derrubar (sempre, inclusive em falha — use trap)
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 emu kill
```

**Tempos reais medidos:** boot ~2–4 min a frio (sem snapshot); `connectedDebugAndroidTest`
incremental 12–14 s; primeira execução (com `assembleDebugAndroidTest`) ~55 s.

**AVD:** `emulator -list-avds` devolve `Medium_Phone_API_35`, mas o diretório em disco é
`~/.android/avd/Medium_Phone.avd` (o `.ini` é que carrega o AvdId). Sempre usar o **AvdId**,
`Medium_Phone_API_35`, nos comandos.

**Onde ficam as evidências** (caminhos reais confirmados):

| Artefato | Caminho |
|----------|---------|
| **JUnit XML (arquivar isto)** | `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_35(AVD) - 15-_app-.xml` |
| Relatório HTML | `app/build/reports/androidTests/connected/debug/index.html` |
| **Logcat por teste** (onde saem os `println` de medição) | `app/build/outputs/androidTest-results/connected/debug/Medium_Phone_API_35(AVD) - 15/logcat-<classe>-<metodo>.txt` |
| Exit code | `.../connected/debug/test-result-exit-code.txt` |

⚠️ O nome do XML contém parênteses e espaços — em script, sempre entre aspas ou via glob
`app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`.

**Runner:** `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` **já está**
configurado no `defaultConfig`. Nada a mudar.

**Como emitir a medição de performance:** `println(...)` do teste vai para o logcat capturado por
teste. Formato recomendado, fácil de grepar como evidência:
`println("SENTINELA|contains|p50=..|p95=..|p99=..")`.

---

## Code Examples

### Teste de performance instrumentado, não-flaky

```kotlin
@RunWith(AndroidJUnit4::class)
class WhitelistPerformanceTest {

    private lateinit var db: SentinelaDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(TEST_DB)
        db = Room.databaseBuilder(ctx, SentinelaDatabase::class.java, TEST_DB).build()
        db.openHelper.writableDatabase.run {          // insert em UMA transacao: ~1000x mais rapido
            beginTransaction()
            try {
                repeat(ENTRIES) { i ->
                    execSQL("INSERT INTO whitelist (number_key, description, enabled, created_at_utc_millis) " +
                            "VALUES ('+55119${"%08d".format(i)}', NULL, 1, 0)")
                }
                setTransactionSuccessful()
            } finally { endTransaction() }
        }
    }

    @After fun tearDown() = db.close()

    @Test fun containsFicaDentroDoOrcamento() {
        val dao = db.whitelistDao()
        val key = "+5511900000500"
        repeat(WARMUP) { dao.containsBlocking(key) }        // JIT + page cache + prepared statement

        val samples = LongArray(SAMPLES)
        for (i in 0 until SAMPLES) {
            val t0 = System.nanoTime()
            dao.containsBlocking(key)
            samples[i] = System.nanoTime() - t0
        }
        samples.sort()
        val p50 = samples[SAMPLES / 2] / 1_000_000.0
        val p95 = samples[(SAMPLES * 0.95).toInt()] / 1_000_000.0
        println("SENTINELA|contains|entries=$ENTRIES|p50=$p50|p95=$p95")

        assertTrue("p95=$p95 ms acima do orcamento de 5 ms", p95 < 5.0)
        assertTrue("p50=$p50 ms — sinal estavel, esperado < 1 ms", p50 < 1.0)
    }

    private companion object {
        const val TEST_DB = "perf-test.db"
        const val ENTRIES = 1_000
        const val WARMUP = 300     // medido: sem warmup a cauda dobra
        const val SAMPLES = 500
    }
}
```

### Teste que realmente prova o índice

```kotlin
@Test fun containsUsaIndiceEmVezDeFullScan() {
    val sql = "SELECT EXISTS(SELECT 1 FROM whitelist WHERE number_key = ? AND enabled = 1)"
    val plan = buildString {
        db.openHelper.writableDatabase
            .query("EXPLAIN QUERY PLAN $sql", arrayOf("+5511999999999"))
            .use { c -> while (c.moveToNext()) appendLine(c.getString(c.columnCount - 1)) }
    }
    println("SENTINELA|EQP|$plan")
    assertTrue("plano sem indice:\n$plan", plan.contains("USING INDEX index_whitelist_number_key"))
    assertFalse("full scan detectado:\n$plan", plan.contains("SCAN whitelist"))
}
```
Saída real observada na pesquisa:
```
SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)
```
Remova o `@Index(unique = true)` e este teste fica vermelho **de forma determinística** — que é
exatamente o que o CONTEXT.md exige e que o teste de cronômetro **não** entrega.

### Teste de DataStore em JVM (sem instrumentação)

`PreferenceDataStoreFactory.create { file }` recebe um `File` arbitrário e não precisa de Context —
então os testes de mapeamento Preferences↔`ScreeningSettings` rodam em **JVM pura**, e contam para
o Kover.

```kotlin
@get:Rule val tmp = TemporaryFolder()

@Test fun defaultsDoMvpQuandoArquivoVazio() = runTest {
    val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    val ds = PreferenceDataStoreFactory.create(scope = scope) {
        tmp.newFile("s.preferences_pb").also { it.delete() }
    }
    val repo = DataStoreSettingsRepository(ds, scope)
    assertEquals(ScreeningSettings(), repo.snapshot())
    scope.cancel()   // OBRIGATORIO: sem isso o proximo teste bate em "multiple DataStores"
}
```

---

## State of the Art

| Abordagem antiga | Abordagem atual | Quando mudou | Impacto aqui |
|------------------|-----------------|--------------|--------------|
| `SharedPreferences` | DataStore Preferences | 2020+ | Já é a escolha; nada a migrar |
| `kapt` para Room | KSP | Room 2.5+ | Já configurado e verificado |
| `ksp { arg("room.schemaLocation") }` | Plugin `androidx.room` + `room { schemaDirectory() }` | Room 2.6 | O antigo **continua funcionando** com AGP 9.3.0 / Room 2.8.4 (verificado). **Não migrar** sem motivo |
| `@Insert(onConflict = REPLACE)` | `@Upsert` | Room 2.5+ | `REPLACE` **deleta e reinsere**, mudando o `id` e disparando `ON DELETE CASCADE`; `@Upsert` faz UPDATE de verdade |
| `fallbackToDestructiveMigration()` | Migrações explícitas + `MigrationTestHelper` | sempre foi o certo | **Proibido** nesta fase |

---

## Open Questions

1. **Assinatura exata do construtor de `MigrationTestHelper` no Room 2.8.4**
   - Sabemos: a classe existe em `room-testing` e precisa de `androidTestImplementation` + schemas
     nos assets do androidTest.
   - Não confirmado: qual das sobrecargas (`Class<*>` vs `KClass` vs com `SupportSQLiteOpenHelper.Factory`)
     é a não-deprecada em 2.8.4.
   - **Recomendação:** não bloqueia o plano. O compilador resolve na primeira compilação da task;
     feedback em segundos. Confidence: MEDIUM.

2. **Margem do p95 em CI vs máquina local**
   - Medido 3,59 ms num Mac arm64 com emulador headless — 28% de folga sobre 5 ms.
   - Não sabemos como se comporta numa máquina mais lenta.
   - **Recomendação:** o assert primário de gate deve ser `p50 < 1 ms` (folga de 5×, medido 0,20 ms);
     manter `p95 < 5 ms` como assert declarado, e se ficar flaky, aumentar `WARMUP`/`SAMPLES` antes
     de afrouxar o bound. Registrar em `03-EVIDENCE.md` os números da execução real.

3. **`enabled = 0` e o índice**
   - O índice é só em `number_key`; o filtro `enabled = 1` é avaliado após a busca indexada (EQP
     confirma `SEARCH ... USING INDEX`). Com índice único em `number_key`, no máximo 1 linha é
     examinada — não há ganho em índice composto.
   - **Recomendação:** índice simples. Não adicionar `enabled` ao índice.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (JVM) | JUnit 4 `4.13.2`, testes JVM puros — herdado da Phase 2. Sem Robolectric (4.16.1 não suporta compileSdk 37) |
| Framework (instrumentado) | AndroidX Test — `androidx.test.runner.AndroidJUnitRunner` (**já configurado** no `defaultConfig`), `androidx.test.ext:junit-ktx 1.3.0`, `androidx.test:core-ktx 1.7.0`, `androidx.room:room-testing 2.8.4` |
| Config file | `app/build.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true` — **não remover**, é pré-requisito do libphonenumber em teste) |
| Cobertura | Kover `0.9.9`; filtro a ampliar para `data.*` + `settings.*` com exclusão do pacote `data.local.db.*` e de `*_Impl` (§ Kover) |
| Quick run command | `./gradlew testDebugUnitTest` |
| Instrumented command | `./gradlew :app:connectedDebugAndroidTest` (exige emulador booted — § Emulador) |
| Full suite command | `./gradlew assembleDebug testDebugUnitTest koverVerify lint detekt && ./gradlew :app:connectedDebugAndroidTest && bash scripts/verify-invariants.sh` |
| Pré-requisitos | `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"`; JDK 17 via `gradle.properties`; `-XX:MaxMetaspaceSize=1g` (já presente) |
| Runtime estimado | quick ~15 s · instrumentado ~15 s incremental / ~55 s a frio + 2–4 min de boot do emulador · full ~4–6 min |

### Phase Requirements → Test Map

| Req | Comportamento | Tipo | Comando automatizado | Arquivo existe? |
|-----|---------------|------|----------------------|-----------------|
| WLT-01/02 | CRUD + enabled | instrumentado (DAO) | `./gradlew :app:connectedDebugAndroidTest --tests "*WhitelistDaoTest"` | ❌ criado pela task |
| WLT-01/02 | mapper entidade↔domínio | unit (fake DAO) | `./gradlew testDebugUnitTest --tests "*RoomWhitelistRepositoryTest"` | ❌ criado pela task |
| WLT-03 | busca por número/descrição | instrumentado | `... --tests "*WhitelistDaoTest"` | ❌ criado pela task |
| WLT-04 | dedup/upsert por chave única | instrumentado | `... --tests "*WhitelistDedupTest"` (2 inserts, 1 linha, descrição nova) | ❌ criado pela task |
| WLT-07 | índice usado | instrumentado (EQP) | `... --tests "*WhitelistPerformanceTest"` — assert `USING INDEX index_whitelist_number_key` | ❌ **prova determinística; substitui o cronômetro** |
| WLT-07 | orçamento p95 | instrumentado (benchmark) | `... --tests "*WhitelistPerformanceTest"` — p95 < 5 ms **e** p50 < 1 ms, com `println("SENTINELA\|contains\|...")` no logcat | ❌ criado pela task |
| HST-01/03/05 | registro, delete, clear, classificação | instrumentado (DAO) | `... --tests "*BlockedCallDaoTest"` | ❌ criado pela task |
| HST-02 | retenção nunca/7/30/90/manual | **unit (pura)** | `./gradlew testDebugUnitTest --tests "*RetentionPolicyTest"` — cutoff por política | ❌ criado pela task |
| HST-02 | poda efetiva | instrumentado | `... --tests "*BlockedCallDaoTest"` — insere datado, poda, conta | ❌ criado pela task |
| HST-04 | E.164 disponível para whitelist | unit | `--tests "*RoomBlockedCallRepositoryTest"` | ❌ criado pela task |
| HST-06 / PRV-03 | backup exclui os dados | **unit (lê XML)** | `./gradlew testDebugUnitTest --tests "*BackupRulesTest"` | ❌ criado pela task |
| ENG-01 | contador de aberturas | unit (DataStore em `TemporaryFolder`) | `--tests "*DataStoreSettingsRepositoryTest"` | ❌ criado pela task |
| QLT-03 | schema exportado + harness | unit + instrumentado + script | `--tests "*SchemaExportTest"`, `--tests "*MigrationHarnessTest"`, `bash scripts/verify-invariants.sh` | ❌ criado pela task |
| QLT-01 | falha de repositório / duplicado / retenção | unit (fakes lançando) | `--tests "*RoomWhitelistRepositoryTest"` | ❌ criado pela task |
| QLT-06 | suíte instrumentada verde | instrumentado | `./gradlew :app:connectedDebugAndroidTest` + arquivar `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` | ❌ evidência criada pela task |

### Sampling Rate

- **Por commit de task:** `./gradlew testDebugUnitTest` (< 30 s) — tasks que só tocam código
  instrumentado adicionam `./gradlew :app:connectedDebugAndroidTest` com o emulador **já de pé**.
- **Por merge de wave:** `./gradlew testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh`
  (nas waves anteriores ao plano que amplia o filtro do Kover, usar `koverLog` e conferir manualmente).
- **Phase gate:** pós-`clean`, com `--no-build-cache`, suíte JVM **e** instrumentada verdes, com
  `N actionable tasks: M executed` e `M > 0`. `UP-TO-DATE` e `FROM-CACHE` têm o mesmo defeito
  probatório (regra da Phase 1). Arquivar em `03-EVIDENCE.md`: o XML do androidTest, o percentil
  medido do logcat e a saída do EQP.
- **Latência máxima de feedback:** < 60 s (JVM); o emulador sobe **uma vez** por sessão de execução.
- **Nenhum comando usa watch mode.**

### Wave 0 Gaps

Infraestrutura que bloqueia as tasks seguintes e precisa vir primeiro:

- [ ] `app/build.gradle.kts` — `androidTestImplementation(libs.room.testing)` e
      `androidTestImplementation(libs.androidx.test.core)`
- [ ] `app/build.gradle.kts` — `sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")`
      (sem isso `MigrationTestHelper` não acha o schema)
- [ ] Script de emulador (`scripts/run-instrumented-tests.sh`) — boot headless + polling em
      `sys.boot_completed` + `connectedDebugAndroidTest` + `trap` de `emu kill`.
      **Sem `checkpoint:human-action`** (decisão do usuário)
- [ ] `scripts/verify-invariants.sh` — invariantes de `fallbackToDestructiveMigration` e de
      `app/schemas/*/1.json`
- [ ] Correção do `path="."` nos XMLs de backup **antes** de escrever o `BackupRulesTest`

**Instalação de framework: nenhuma.** Room, DataStore, KSP e AndroidX Test já estão no catálogo e
comprovadamente funcionais; a fase só move duas dependências de configuração.

**Não é Wave 0:** ampliar o filtro do Kover para `data.*`/`settings.*`. Isso vai para o **último**
plano da fase — ligar antes dos testes existirem quebra o build (lição literal da Phase 2, onde o
gate foi deliberadamente adiado do plano 02-01 para o 02-05).

---

## Sources

### Primary (HIGH confidence) — verificação empírica neste repositório
- Build de rascunho com `@Database`/`@Entity`/`@Dao` → `BUILD SUCCESSFUL`, `app/schemas/<db>/1.json`
  gerado. Prova: KSP 2.3.10 + Room 2.8.4 + AGP 9.3.0 + `ksp arg("room.schemaLocation")` funcionam.
- `./gradlew -q buildEnvironment` → `symbol-processing-gradle-plugin:2.3.10` resolvido.
- `./gradlew :app:connectedDebugAndroidTest` no AVD `Medium_Phone_API_35` → executado, com relatório
  XML + logcat por teste. Tabela de medições da § Medições.
- `EXPLAIN QUERY PLAN` real → `SEARCH whitelist USING INDEX index_whitelist_number_key (number_key=?)`.
- `IllegalStateException` real do DataStore → caminho
  `/data/data/<pkg>/files/datastore/<name>.preferences_pb` e obrigatoriedade do singleton.
- Arquivos do repo: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`,
  `app/src/main/res/xml/*.xml`, interfaces em `data/local/` e `settings/`.

### Secondary (MEDIUM confidence)
- developer.android.com — *Back up user data with Auto Backup*: domínios válidos (`root`, `file`,
  `database`, `sharedpref`, `external`, `device_*`), `path` documentado como **obrigatório**,
  `path="."` válido, exclusão de diretório é recursiva. Base da correção da § Backup.
- `.planning/phases/02-.../02-VALIDATION.md` e `.planning/STATE.md` — baseline de infraestrutura de
  teste, gate do Kover, regra probatória de `--no-build-cache`.
- `docs/PROMPT-MVP.md` §6/§7 e `docs/PRIVACIDADE.md` — campos mínimos do histórico e opções de retenção.

### Tertiary (LOW confidence — sinalizado para validação no build)
- Assinatura exata do construtor de `MigrationTestHelper` no Room 2.8.4 (Open Question 1).

---

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** — nada a adicionar; toolchain verificada por build real.
- Arquitetura / performance: **HIGH** — números medidos no alvo real, com duas premissas do CONTEXT
  corrigidas por evidência (DAO não-suspend; índice provado por EQP, não por cronômetro).
- Backup: **HIGH** para o caminho do DataStore (confirmado por exceção do runtime); **MEDIUM** para
  a exigência formal de `path` — por isso a recomendação é a conservadora (sempre explicitar).
- Emulador / testes instrumentados: **HIGH** — ciclo completo executado, caminhos de relatório reais.
- Kover: **MEDIUM** — a estratégia de exclusão é sólida, mas o percentual final só se conhece com o
  código escrito; por isso o gate fica no último plano da fase.

**Research date:** 2026-07-29
**Valid until:** ~2026-08-28 (30 dias — stack estável e travada por catálogo)

**Nota de higiene:** todos os artefatos de rascunho criados durante esta pesquisa (pacote
`scratchroom` em `main` e `androidTest`, schema exportado do banco de rascunho, dependências
temporárias de `androidTest`) foram removidos e o `app/build.gradle.kts` revertido —
`git status` limpo ao final.

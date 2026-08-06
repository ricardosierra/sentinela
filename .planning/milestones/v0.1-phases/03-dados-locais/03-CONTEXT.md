# Phase 3: Dados Locais - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Configurações, whitelist, histórico e contador de aberturas persistem **localmente**, com
retenção aplicada e **fora do backup automático** do Android — e a consulta da whitelist cabe
no orçamento de performance do Service.

Entregas:
- `SettingsRepository` real com DataStore Preferences (Flow + snapshot rápido, defaults do MVP).
- `PersonalWhitelistRepository` real com Room: CRUD, busca, dedup por chave, `contains()` indexado e medido.
- `BlockedCallRepository` real com Room: registro mínimo, retenção e limpeza total/individual.
- Contador de aberturas persistido.
- `dataExtractionRules` / `fullBackupContent` excluindo os dados, verificado por teste.
- Schemas exportados em `app/schemas/` e testes instrumentados de DAO **executados**.

Fora do escopo: leitura de contatos do aparelho (Phase 4), integração real do Service (Phase 5),
qualquer UI de whitelist/histórico (Phase 8). Nenhuma permissão nova.

</domain>

<decisions>
## Implementation Decisions

### Whitelist

- **Dedup:** índice **único** em `numberE164`. Inserir número já presente faz **upsert** da entrada
  existente (atualiza descrição/enabled), sem erro visível ao usuário e sem criar linha duplicada.
- **Códigos curtos são aceitos.** A chave segue o contrato fechado na Phase 2: E.164 para números
  normais, **dígitos crus** para valores com menos de `PhoneNumbers.LIMIAR_CURTO` (6) dígitos.
  O usuário precisa conseguir pôr `190` na whitelist. A coluna guarda a chave que o
  `PhoneNumberNormalizer` devolve — a normalização acontece **antes** de chegar ao repositório.
- **`enabled = false` faz `contains()` retornar `false`.** Desabilitar é equivalente funcional a
  remover, mas preserva a entrada para o usuário religar depois. A query de `contains()` filtra
  por `enabled = 1`.
- **Orçamento medido, não presumido — CORRIGIDO APÓS PESQUISA (2026-07-29):**
  - Medição real no emulador com 1.000 entradas: DAO **`suspend`** dá p50 1,46 / p95 9,12 /
    p99 26,39 ms — **estoura o alvo**. DAO **não-suspend** dá p50 0,20 / p95 3,59 / p99 5,46 ms.
    O gargalo é o dispatch de corrotina do Room, não o SQLite (query crua: p50 0,032 ms).
    **Decisão: `contains()` no caminho quente é DAO não-suspend**, chamado de um contexto de IO
    pelo chamador. A interface `PersonalWhitelistRepository` mantém `suspend fun contains` como
    contrato público; o custo fica fora do caminho da decisão.
  - Assert primário `p50 < 1 ms` (folga 5×, robusto em máquina lenta), mantendo `p95 < 5 ms`
    como alvo declarado.
  - **O teste de tempo NÃO prova o índice.** Medido: full scan com 1.000 linhas dá p50 0,047 ms
    contra 0,032 ms do indexado — indistinguível de ruído. A prova do índice é
    **`EXPLAIN QUERY PLAN`**, determinística, exigindo a string
    `SEARCH ... USING INDEX` na saída. É esse o teste que deve falhar se o índice sumir.

### Histórico

- **Guarda o E.164 completo** em coluna própria, além da máscara. É dado local necessário para o
  usuário conseguir "adicionar à whitelist" a partir do histórico (Phase 8). Isso **não** afrouxa
  a regra de privacidade: log, notificação e crash report continuam usando **somente a máscara**.
  O banco fica fora do backup (ver abaixo), e nenhum dado sai do aparelho.
- **Retenção padrão: 30 dias.** Opções expostas: nunca / 7 / 30 / 90 / manual.
- **A poda roda na abertura do app e após cada gravação.** Sem WorkManager — a dependência e o
  custo de cold start não se justificam para uma tabela local pequena.
- **Histórico ligado por padrão.** É o que dá auditabilidade ao bloqueio; desligável nas
  configurações. Com ele desligado, `record()` não grava nada.

### Persistência, backup e migração

- **Um único banco Room `sentinela.db`** com as duas tabelas (whitelist e histórico). Um schema,
  uma cadeia de migração.
- **`exportSchema = true`** com os schemas versionados em `app/schemas/`, e migrações **explícitas**.
  `fallbackToDestructiveMigration` é **proibido** — apagaria a whitelist do usuário numa
  atualização. Se não houver migração válida, o build/teste deve falhar, não o dado do usuário.
- **Contador de aberturas vive no DataStore**, junto das configurações — não merece tabela.
- **Backup:** `dataExtractionRules` (API 31+) **e** `fullBackupContent` (API 29–30, ambos dentro
  do range de minSdk 29) excluindo `sentinela.db`, seus arquivos `-wal`/`-shm` e o arquivo do
  DataStore. Verificado por **teste que lê o XML** e afirma as exclusões — não basta declarar.

### Testes instrumentados — DECISÃO DO USUÁRIO (2026-07-29)

- Os testes instrumentados de DAO são **executados de verdade nesta fase, em emulador**, não
  adiados. AVD disponível e confirmado: **`Medium_Phone_API_35`** (API 35 ≥ minSdk 29).
- Isso é uma exceção **deliberada e restrita** à política de validação física do ROADMAP: ela
  cobre comportamento dependente de **OEM/aparelho real** (Samsung, telefonia, DND), que continua
  na Phase 9. Emulador para SQLite/Room é infraestrutura de teste, não validação de campo.
- O executor sobe o emulador headless, roda `connectedDebugAndroidTest` e arquiva a saída real
  como evidência. Se o emulador não subir no ambiente, isso é **blocker reportado**, não motivo
  para trocar silenciosamente por teste JVM.
- Nenhum plano deve emitir `checkpoint:human-action` para isso — subir emulador é automatizável.

### Claude's Discretion

- Nomes de tabelas/colunas, organização dos DAOs, formato exato do teste de performance e a
  estrutura dos arquivos de migração ficam a critério do executor, desde que os 6 critérios de
  sucesso passem e as decisões acima sejam honradas.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/org/sentinela/app/data/local/PersonalWhitelistRepository.kt` — interface +
  `WhitelistEntry` (id, numberE164, description, enabled, createdAtUtcMillis) já definidos.
- `app/src/main/java/org/sentinela/app/data/local/BlockedCallRepository.kt` — interface +
  `BlockedCallEntry` (id, maskedNumber, numberE164, timestampUtcMillis, reason,
  notificationShown, classification) e `CallClassification`. Note que `numberE164` já é nullable
  e a máscara já é campo separado — o modelo **já antecipa** a decisão acima.
- `app/src/main/java/org/sentinela/app/settings/SettingsRepository.kt` — interface com
  `settings: Flow`, `snapshot()` e `update(transform)`.
- `app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt` — modelo com `OriginPolicy`,
  `BlockMode`, `FallbackPolicy`.
- `app/src/main/java/org/sentinela/app/phone/PhoneNumberNormalizer.kt` +
  `LibPhoneNumberNormalizer` (Phase 2) — produz a chave que a whitelist persiste.
- `app/src/main/java/org/sentinela/app/phone/PhoneNumbers.kt` — `LIMIAR_CURTO = 6`.
- `scripts/verify-invariants.sh` — checa permissões (allowlist sobre o manifest mergeado) e a
  pureza de `domain/` e `phone/`. Se a fase criar invariante novo, é aqui que entra.
- `app/src/test/java/org/sentinela/app/phone/TestMetadata.kt` — fixture que roda libphonenumber
  em JVM pura, caso algum teste de dados precise normalizar.

### Established Patterns
- DI manual via `AppContainer`, com `by lazy` para singletons caros (ver `phoneNumberNormalizer`).
- Domínio e `phone/` sem `import android.*`; acesso a plataforma isolado em `platform/`.
- Strings em `res/values/strings.xml` (pt-BR), nunca hardcoded em Kotlin.
- Nenhum número completo em log — sempre `PhoneMask.mask`.
- Kover com gate `minBound(80)` sobre `domain.*` + `phone.*`. **Resolvido pela pesquisa:** incluir
  `data.*` e `settings.*`, mas **excluir** `data.local.db.*` e `*_Impl` — código gerado pelo Room
  só roda instrumentado e o Kover não o mede; incluí-lo derrubaria o gate com falso-vermelho.
  Ampliar o filtro **só no último plano da fase** (lição literal da Phase 2).
- **Toolchain já pronta (verificado por build real):** KSP 2.3.10 + Room 2.8.4 + AGP 9.3.0 com o
  `ksp { arg("room.schemaLocation", ...) }` legado **funcionam** e geram `app/schemas/`.
  **Não** migrar para o Room Gradle Plugin.
- **DataStore:** caminho real confirmado em runtime —
  `/data/data/<pkg>/files/datastore/<name>.preferences_pb`. O `data_extraction_rules.xml` atual já
  exclui o lugar certo; falta apenas `path="."` explícito nos `<exclude>`. O runtime lança
  `There are multiple DataStores active for the same file` se houver mais de uma instância — o
  singleton no `AppContainer` é obrigatório por contrato, não por estilo.
- **Emulador:** `adb wait-for-device` **não basta** (fica `offline` por vários segundos); é
  preciso poll em `sys.boot_completed`.
- Evidência de build só vale com `clean` **e** `--no-build-cache` — `FROM-CACHE` tem o mesmo
  defeito probatório que `UP-TO-DATE` (aprendido na Phase 1).

### Integration Points
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — ponto de composição dos repositórios.
- `app/build.gradle.kts` + `gradle/libs.versions.toml` — Room, KSP, DataStore, `exportSchema`.
- `app/src/main/AndroidManifest.xml` — `dataExtractionRules` e `fullBackupContent`.
- `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` — consumidor real
  a partir da Phase 5; nesta fase continua pass-through.

</code_context>

<specifics>
## Specific Ideas

- O risco desta fase é **perder dado do usuário**, não performance. Daí a proibição de
  `fallbackToDestructiveMigration` e a exigência de teste de migração.
- Duas afirmações do ROADMAP são "comprovadamente"/"medida" — backup excluído e `contains()`
  dentro do orçamento. Ambas precisam de teste que **falhe** se a propriedade for quebrada, não
  de inspeção manual.

</specifics>

<deferred>
## Deferred Ideas

- UI de whitelist e histórico — Phase 8.
- Uso real dos repositórios no `CallScreeningService` — Phase 5.
- Leitura de contatos do aparelho — Phase 4.
- Sync com backend / fonte remota da whitelist — v0.2.0, fora do MVP.
- Validação em Samsung físico (telefonia, DND, comportamento de OEM) — Phase 9.

</deferred>

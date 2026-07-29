# Phase 4: Contatos do Aparelho - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

O Sentinela sabe — **local e instantaneamente** — se quem liga está na agenda, **sem nunca
armazenar ou vazar** dado de contato.

Entregas:
- `READ_CONTACTS` declarada no manifest (esta é **a fase dela**, conforme `docs/PERMISSOES.md:14`)
  e a máquina de estado de permissão (concedida / negada / negada permanentemente).
- `ContactLookupRepository` real: HIT / MISS / UNAVAILABLE por E.164, com cache em memória
  invalidado por `ContentObserver`.
- Lookup medido dentro do orçamento da decisão, incluindo cold start.
- Prova automatizada de que nome/dado de contato não entra em banco, log nem backup.

**Fora do escopo:** a **tela** de onboarding que pede a permissão é da **Phase 7** — aqui entram
manifest, repositório, máquina de estado e testes, não Compose. Integração real do Service é
Phase 5. Políticas por contato no motor (CTT-03) já existem desde a Phase 2.

</domain>

<decisions>
## Implementation Decisions

### Lookup e cache

- **Consultar via `PhoneLookup.CONTENT_FILTER_URI`** — é a API que o próprio Android usa e faz
  o matching de número por conta própria, incluindo variações de formatação. Não varrer a agenda
  inteira normalizando número a número.
- **O cache guarda SOMENTE o conjunto de chaves E.164 normalizadas** (`Set<String>`), em memória.
  **Nunca** nome, foto, ID de contato ou qualquer outro campo. O motor recebe apenas
  HIT/MISS/UNAVAILABLE — nada mais atravessa a fronteira.
- **Cache construído preguiçosamente**, na primeira consulta, e invalidado por `ContentObserver`
  sobre `ContactsContract`. **Nada** no `Application.onCreate` — cold start do Service é orçamento
  crítico e já foi protegido nas fases anteriores.
- **Agenda grande:** medir com ~5.000 contatos e exigir **p50 < 10 ms** no cache quente. O cold
  path pode consultar direto via `PhoneLookup` sem esperar a construção do cache inteiro —
  correção antes de otimização.
- Aplicar a lição da Phase 3: **cronômetro não prova estrutura**. Se houver afirmação de "usa
  índice/cache", ela precisa de prova determinística, não de tempo. E o assert primário deve ser
  a mediana (estável), não um percentil de cauda (flaky) — ver a decisão de p95 tomada na Phase 3.

### Permissão negada

- **Sem `READ_CONTACTS` → `ContactLookup.UNAVAILABLE`**, nunca `MISS`. O motor já trata isso com
  `FallbackPolicy` desde a Phase 2. Devolver `MISS` faria contato conhecido ser tratado como
  desconhecido — falha perigosa e silenciosa.
- **Negação permanente** ("não perguntar de novo") é detectada e o app oferece atalho para as
  configurações do sistema, **sem insistir**. Nunca repedir a permissão a cada abertura.
- **O app é 100% utilizável sem a permissão, no modo filtro.** Isso não é degradação inventada:
  quando o Sentinela não é o discador padrão, o Android já não entrega chamadas de contatos ao
  `onScreenCall`. Onboarding **não** pode ser bloqueado pela negação.
- **Momento do pedido:** passo próprio do onboarding, com a explicação **antes** do diálogo do
  sistema. A tela é da Phase 7; esta fase entrega a permissão no manifest e a máquina de estado
  que a tela vai consumir.

### Privacidade e prova

- **Prova de que nome não vaza:** teste que inspeciona o **schema exportado do Room** e falha se
  aparecer qualquer coluna de nome/dado de contato, **mais** um invariante novo em
  `scripts/verify-invariants.sh`. Revisão de código não conta como prova.
- **Backup:** `READ_CONTACTS` não cria arquivo próprio — nada novo a excluir. O `BackupRulesTest`
  da Phase 3 deve continuar verde; se quebrar, é sinal de vazamento.
- **Log do lookup:** apenas cardinalidade e resultado (`HIT`/`MISS`/`UNAVAILABLE`). **Nunca**
  número, nunca nome. Segue a regra geral do `CLAUDE.md`.
- **Testes do repositório são instrumentados**, no emulador `Medium_Phone_API_35`, com contatos
  inseridos no `ContactsContract` de teste. Segue a exceção já aberta e validada na Phase 3
  (emulador para infraestrutura de teste ≠ validação de campo, que continua na Phase 9).

### Permissão — regra de processo obrigatória

- `READ_CONTACTS` entra na **allowlist de `scripts/verify-invariants.sh` no mesmo commit** em que
  entra no manifest, e `docs/PERMISSOES.md` deve ser conferido antes. Essa é a regra registrada na
  Phase 1: nunca afrouxar o script antes da documentação.

### Claude's Discretion

- Estrutura interna do cache, formato do `ContentObserver`, organização dos arquivos de teste e
  como exatamente a máquina de estado de permissão é modelada ficam a critério do executor,
  desde que os 4 critérios de sucesso passem.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/org/sentinela/app/data/contacts/ContactLookupRepository.kt` — interface já
  existe com `suspend fun lookup(numberE164: String): ContactLookup` e as regras no KDoc.
- `app/src/main/java/org/sentinela/app/domain/` — `ContactLookup` (HIT/MISS/UNAVAILABLE) já é
  consumido pelo `CallDecisionEngine`, com `FallbackPolicy` cobrindo UNAVAILABLE por teste.
- `app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt` — produz a chave E.164
  (ou dígitos crus para códigos curtos) que o cache deve usar. Mesma chave da whitelist.
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — DI manual, `by lazy` para singletons,
  e `onAppOpened()` já existe como gancho de abertura.
- `scripts/verify-invariants.sh` — allowlist de permissões sobre o **manifest mesclado**, pureza
  de `domain/`+`phone/`, guarda contra `fallbackToDestructiveMigration`.
- `scripts/run-instrumented-tests.sh` — sobe o emulador headless com polling em
  `sys.boot_completed` e apaga XMLs antigos. **`connectedDebugAndroidTest` não aceita `--tests`**;
  use `bash scripts/run-instrumented-tests.sh --tests "*AlgumTest"`.
- `app/src/test/java/org/sentinela/app/privacy/BackupRulesTest.kt` — teste de backup por DOM.

### Established Patterns
- DI manual; nada de Hilt/Koin/Dagger/WorkManager.
- `domain/` e `phone/` sem `import android.*`; acesso a plataforma isolado em `platform/`.
- Strings em `res/values/strings.xml` (pt-BR).
- Nenhum número completo em log — sempre `PhoneMask.mask`.
- Kover com gate `minBound(80)` cobrindo `domain.*`, `phone.*`, `data.*`, `settings.*`, excluindo
  `data.local.db.*` e `*_Impl`. **Ampliar filtro só no último plano da fase.**
- Evidência só vale com `clean` **e** `--no-build-cache`.
- Todo guarda-corpo precisa de prova de vermelho: quebrar de propósito, ver falhar, restaurar.

### Integration Points
- `app/src/main/AndroidManifest.xml` — entrada de `READ_CONTACTS`.
- `docs/PERMISSOES.md` — fonte canônica; conferir antes de tocar no manifest.
- `app/build.gradle.kts` — filtro do Kover.
- `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` — consumidor real
  a partir da Phase 5; nesta fase continua pass-through.
- `docs/TESTE-FISICO-SAMSUNG.md` — destino de qualquer medição que dependa de hardware real
  (já tem os cenários 31–35).

</code_context>

<specifics>
## Specific Ideas

- O risco desta fase é **privacidade**, não performance: é a primeira vez que o app toca em dado
  pessoal de terceiros. Daí a exigência de prova automatizada, não inspeção.
- O critério 1 fala em "explicação clara", que é UI — mas a UI é da Phase 7. Esta fase entrega o
  contrato que a tela consome; o plano não deve criar tela nem emitir `checkpoint:human-*`.

</specifics>

<deferred>
## Deferred Ideas

- Tela de onboarding com a explicação e o pedido da permissão — Phase 7.
- Uso real do `ContactLookupRepository` no `CallScreeningService` — Phase 5.
- Políticas por contato **individual** (não por origem) — dependem do modo discador, Phase 6.
- Medição de lookup em Samsung físico com agenda real — Phase 9.

</deferred>

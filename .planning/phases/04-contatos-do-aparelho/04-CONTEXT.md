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

- **CORRIGIDO APÓS PESQUISA (2026-07-29) — `PhoneLookup` NÃO resolve variação de formato sozinho.**
  Medido no emulador: o provider casa por `NORMALIZED_NUMBER`, coluna que **ele** calcula na
  escrita usando o país do aparelho. Duas falhas reais reproduzidas:
  - Contato salvo como `(11) 91234-5678` num aparelho com SIM estrangeiro → `NORMALIZED_NUMBER`
    fica **`null`** → consulta por `+5511912345678` devolve **0 linhas**. Contato conhecido vira
    MISS — exatamente a falha silenciosa que este contexto proíbe.
  - Pior: num aparelho com SIM dos EUA, o fixo do Rio `(21) 3216-5498` foi gravado como
    **`+12132165498`**. Confiar nessa coluna produz **falso HIT** — o app trataria um
    desconhecido como contato.
  **Decisões que decorrem disso:**
  - `PhoneLookup` continua sendo usado, mas com **dupla sondagem**: E.164 **e** número nacional
    significativo (~2 ms cada, medido).
  - O **cache é construído a partir de `Phone.NUMBER` cru**, normalizado pelo
    **nosso** `LibPhoneNumberNormalizer` — nunca a partir de `NORMALIZED_NUMBER`. Isso também dá
    paridade de chave com a whitelist.
- **O cache guarda SOMENTE o conjunto de chaves E.164 normalizadas** (`Set<String>`), em memória.
  **Nunca** nome, foto, ID de contato ou qualquer outro campo. O motor recebe apenas
  HIT/MISS/UNAVAILABLE — nada mais atravessa a fronteira.
- **Cache construído preguiçosamente**, na primeira consulta, e invalidado por `ContentObserver`
  sobre `ContactsContract`. **Nada** no `Application.onCreate` — cold start do Service é orçamento
  crítico e já foi protegido nas fases anteriores.
- **Agenda grande — MEDIDO (5.000 contatos, emulador):** consulta direta ao `PhoneLookup` já dá
  p50 **1,95 ms** (HIT) / 2,45 ms (MISS), p95 ~8 ms, máximo 74 ms — **já cabe** no orçamento de
  200 ms. Cache quente (`HashSet`): p50 **1,08 µs**. Construção do cache: **1,5–1,8 s**.
  Consequência: **o cache não se justifica por velocidade, e sim por correção de chave e por
  cortar a cauda.** Os planos devem dizer isso explicitamente, em vez de repetir o erro da
  Phase 3 de tratar cronômetro como prova de estrutura. A construção de 1,5 s **nunca** pode
  bloquear uma consulta — o cold path consulta direto enquanto o cache aquece.
- **Debounce do `ContentObserver` é obrigatório, não otimização.** Medido: o provider disparou
  51 callbacks para 50 transações numa execução e 1–2 para 30 em outra — a coalescência existe
  mas **não é garantida**. Observar `ContactsContract.AUTHORITY_URI` com
  `notifyForDescendants = true` foi a única combinação que pegou tudo.
- Aplicar a lição da Phase 3: **cronômetro não prova estrutura**. Se houver afirmação de "usa
  índice/cache", ela precisa de prova determinística, não de tempo. E o assert primário deve ser
  a mediana (estável), não um percentil de cauda (flaky) — ver a decisão de p95 tomada na Phase 3.

### Permissão negada

- **Sem `READ_CONTACTS` → `ContactLookup.UNAVAILABLE`**, nunca `MISS`. O motor já trata isso com
  `FallbackPolicy` desde a Phase 2. Devolver `MISS` faria contato conhecido ser tratado como
  desconhecido — falha perigosa e silenciosa.
- **Negação permanente** ("não perguntar de novo") é detectada e o app oferece atalho para as
  configurações do sistema, **sem insistir**. Nunca repedir a permissão a cada abertura.
- **Armadilha confirmada pela pesquisa:** `shouldShowRequestPermissionRationale` devolve `false`
  nos **dois** extremos — antes do primeiro pedido **e** depois da negação permanente. Distinguir
  os estados exige um flag persistido `contacts_permission_asked`, que vai para o **DataStore de
  configurações que já existe** (Phase 3). A regra de decisão dos estados deve ser uma função
  **pura**, testável em JVM, separada da chamada de plataforma.
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
- **CORRIGIDO APÓS PESQUISA — `WRITE_CONTACTS` no manifest de androidTest NÃO funciona.** A
  instrumentação roda sob o **uid do app**; reproduzido:
  `Permission Denial: writing ContactsProvider2 ... requires WRITE_CONTACTS`, e
  `GrantPermissionRule` não concede o que o pacote não declara. O caminho correto, verificado
  funcionando, é `uiAutomation.adoptShellPermissionIdentity(...)` — e ele deixa `WRITE_CONTACTS`
  fora de **todos** os manifests. `WRITE_CONTACTS` entra na lista de permissões **proibidas** do
  `verify-invariants.sh`.
- **Padrão de vazamento verificado:** `LEAK_PAT='(^|_)(name|display|contact|photo|lookup|nome|agenda)'`
  aplicado aos **valores** de `"columnName"` no schema exportado — zero falso-positivo nas 11
  colunas atuais e pega os 5 vazamentos simulados. Aplicar às **chaves** falsaria positivo, já
  que o JSON é cheio de `"name"`.
- **Adicionar `READ_CONTACTS` produz DOIS vermelhos** no `verify-invariants.sh` (`ALLOWLIST` e
  `FUTURE`), não um. Ambas as edições no mesmo commit.
- **Kover:** `data.contacts.*` já está dentro do include `data.*` existente — nada a incluir.
  Excluir **exatamente uma classe nomeada** (a que fala com o `ContactsContract`), nunca o pacote,
  para que a lógica pura de estado e cache continue contando para o gate de 80%.

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

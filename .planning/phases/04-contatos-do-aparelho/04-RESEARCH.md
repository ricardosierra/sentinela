# Phase 4: Contatos do Aparelho — Research

**Researched:** 2026-07-29
**Domain:** `ContactsContract` / `PhoneLookup` / permissão de runtime / privacidade verificável
**Confidence:** HIGH nos pontos medidos no emulador · MEDIUM no comportamento em aparelho BR real

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Lookup e cache**

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
  a mediana (estável), não um percentil de cauda (flaky).

**Permissão negada**

- **Sem `READ_CONTACTS` → `ContactLookup.UNAVAILABLE`**, nunca `MISS`.
- **Negação permanente** ("não perguntar de novo") é detectada e o app oferece atalho para as
  configurações do sistema, **sem insistir**. Nunca repedir a permissão a cada abertura.
- **O app é 100% utilizável sem a permissão, no modo filtro.** Onboarding **não** pode ser
  bloqueado pela negação.
- **Momento do pedido:** passo próprio do onboarding, com a explicação **antes** do diálogo do
  sistema. A tela é da Phase 7; esta fase entrega a permissão no manifest e a máquina de estado.

**Privacidade e prova**

- **Prova de que nome não vaza:** teste que inspeciona o **schema exportado do Room** e falha se
  aparecer qualquer coluna de nome/dado de contato, **mais** um invariante novo em
  `scripts/verify-invariants.sh`. Revisão de código não conta como prova.
- **Backup:** `READ_CONTACTS` não cria arquivo próprio — nada novo a excluir. O `BackupRulesTest`
  da Phase 3 deve continuar verde.
- **Log do lookup:** apenas cardinalidade e resultado (`HIT`/`MISS`/`UNAVAILABLE`). **Nunca**
  número, nunca nome.
- **Testes do repositório são instrumentados**, no emulador `Medium_Phone_API_35`, com contatos
  inseridos no `ContactsContract` de teste.

**Permissão — regra de processo obrigatória**

- `READ_CONTACTS` entra na **allowlist de `scripts/verify-invariants.sh` no mesmo commit** em que
  entra no manifest, e `docs/PERMISSOES.md` deve ser conferido antes.

### Claude's Discretion

- Estrutura interna do cache, formato do `ContentObserver`, organização dos arquivos de teste e
  como exatamente a máquina de estado de permissão é modelada ficam a critério do executor,
  desde que os 4 critérios de sucesso passem.

### Deferred Ideas (OUT OF SCOPE)

- Tela de onboarding com a explicação e o pedido da permissão — Phase 7.
- Uso real do `ContactLookupRepository` no `CallScreeningService` — Phase 5.
- Políticas por contato **individual** (não por origem) — dependem do modo discador, Phase 6.
- Medição de lookup em Samsung físico com agenda real — Phase 9.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Descrição | Suporte da pesquisa |
|----|-----------|---------------------|
| **CTT-01** | `READ_CONTACTS` solicitada em runtime com explicação clara; app permanece funcional no modo filtro se negada | §"Máquina de estado de permissão" (algoritmo correto com flag persistida no DataStore existente) + §"Regra de processo do manifest" (as **duas** edições obrigatórias em `verify-invariants.sh`, medidas) |
| **CTT-02** | Consulta local e rápida (cache em memória invalidado por ContentObserver), dentro do orçamento de p95 da decisão | §"Números medidos" (build de cache, p50 quente, `PhoneLookup` direto) + §"ContentObserver" (URI correta e coalescência medida) |
| **CTT-04** | Nomes e dados de contato nunca persistidos nem enviados; uso apenas em memória | §"Provar que nada é persistido" (padrões de grep exatos, testados contra vazamento simulado) + §"Projeção mínima medida" |

**CTT-03 já está completo desde a Phase 2** (políticas por origem no `CallDecisionEngine`) — não
gera task nesta fase.
</phase_requirements>

---

## Summary

Esta pesquisa **mediu tudo no emulador `Medium_Phone_API_35` (API 35)**, com contatos reais
inseridos no `ContactsContract`, e derrubou duas premissas do CONTEXT.

**Premissa derrubada nº 1 — `PhoneLookup` NÃO "faz o matching por conta própria, incluindo
variações de formatação".** Ele normaliza os dois lados usando o **país do aparelho** (SIM/rede) e
compara. Quando o contato foi gravado em formato **nacional de um país diferente** do país do
aparelho, o `NORMALIZED_NUMBER` fica `null` — **e a consulta por E.164 devolve zero linhas**.
Medido: contato `(11) 91234-5678` num aparelho de SIM `us` não é encontrado por
`+5511912345678`. Isso é exatamente o "MISS silencioso e perigoso" que o CONTEXT proíbe: contato
conhecido tratado como desconhecido. A mitigação (duas sondas: E.164 + número nacional
significativo) está especificada abaixo e é barata (~2 ms cada).

**Premissa derrubada nº 2 — `Phone.NORMALIZED_NUMBER` não serve como chave de cache.** Além de
poder ser `null`, ele pode estar **silenciosamente errado**: medido, o fixo brasileiro
`(21) 3216-5498` foi gravado pelo provider como `+12132165498` (um número dos EUA). Construir o
cache a partir dessa coluna produziria HIT/MISS falsos. O cache tem que ser construído a partir
de `Phone.NUMBER` cru normalizado pelo **`LibPhoneNumberNormalizer` do próprio app** — o que, de
quebra, dá **paridade de chave com a whitelist** (mesma cascata de região da Phase 2).

**Terceiro achado, de infraestrutura:** `WRITE_CONTACTS` **não é necessário em manifest nenhum**.
Declarar `WRITE_CONTACTS` no `AndroidManifest.xml` do `androidTest` foi medido e **não funciona** —
a instrumentação roda no **uid do app sob teste** (`u0a207 / org.sentinela.app`), e
`GrantPermissionRule` não concede uma permissão que o app não declara. A solução correta é
`uiAutomation.adoptShellPermissionIdentity(WRITE_CONTACTS, READ_CONTACTS)`: funciona, é
padrão AndroidX, e deixa o manifest mesclado **inteiramente livre** de `WRITE_CONTACTS`.

**Primary recommendation:** manter `PhoneLookup` como fonte do cold path, mas com **sonda dupla**
(E.164 + nacional) para não gerar MISS falso; construir o cache em background a partir de
`Phone.NUMBER` normalizado pelo `LibPhoneNumberNormalizer`; projetar **apenas `PhoneLookup._ID`**;
observar `ContactsContract.AUTHORITY_URI` com debounce; e provar a não-persistência por grep sobre
o **schema exportado** + fronteira de import.

---

## Números medidos

Emulador `Medium_Phone_API_35` (API 35, headless, `swiftshader_indirect`), 5.000 contatos
inseridos via `applyBatch`. Todos os valores abaixo saíram de logcat de teste instrumentado real.

| Medição | Valor | Consequência |
|---------|-------|--------------|
| Inserir 5.000 contatos (`applyBatch` em lotes de 300 ops) | **7,1 s – 14,0 s** | O fixture pesado **não** pode rodar em `@Before` de cada teste; use `@BeforeClass`/lote único |
| Build de cache lendo `Phone.CONTENT_URI` (só `NUMBER`) | **54 ms frio / 29 ms quente** | Barato |
| Build de cache lendo `NORMALIZED_NUMBER`+`NUMBER` | **35 ms frio / 16–18 ms quente** | Barato — mas a coluna é **inconfiável** (ver abaixo) |
| Build de cache **normalizando com `LibPhoneNumberNormalizer`** | **1.764 ms frio / 1.494 ms quente** (~0,30 ms/número) | **Não pode bloquear a primeira consulta.** Vai para background |
| Construir o `LibPhoneNumberNormalizer` | **32 ms** | Já é singleton no `AppContainer` (Phase 2) — nada a mudar |
| Lookup quente em `HashSet<String>` | **p50 1,08 µs · p95 1,17 µs** | ~9.000× abaixo da meta de p50 < 10 ms do CONTEXT |
| `PhoneLookup` direto, HIT, 5.000 contatos | **p50 1,95 ms · p95 8,17 ms · max 35,8 ms** | Cabe folgado nos 200 ms da decisão |
| `PhoneLookup` direto, MISS, 5.000 contatos | **p50 2,45 ms · p95 6,87 ms · max 73,9 ms** | MISS custa mais que HIT; a cauda é real |
| Primeiro `PhoneLookup` do processo (cold, agenda de 1 contato) | **6,2 ms** | Bind do provider ≈ 6 ms; segunda consulta 0,000125 ms (cursor cacheado) |

### O cache é necessário?

**Para o orçamento de p95 < 200 ms, não.** `PhoneLookup` direto entrega p50 ≈ 2 ms e p95 ≈ 8 ms
com 5.000 contatos. O cache **continua justificado**, mas por dois motivos que **não são
performance** e devem ser escritos assim no plano, para não repetir o erro da Phase 3 (afirmar
estrutura com cronômetro):

1. **Correção da chave** — o cache é o único lugar onde conseguimos aplicar o
   `LibPhoneNumberNormalizer` do app e escapar da normalização errada do provider.
2. **Corte da cauda** — a cauda medida (`max` 35–74 ms) é do binder + SQLite do provider; o cache
   a elimina.

O CONTEXT já trava o desenho certo: **cold path consulta direto, cache aquece depois**. A medição
confirma que isso é obrigatório, não opcional — 1,5 s de build **jamais** pode ficar na frente de
um `onScreenCall`.

---

## Semântica real do `PhoneLookup.CONTENT_FILTER_URI` (API 35)

### Matriz medida (aparelho com `simCountryIso=us`, `networkCountryIso=us`)

| Contato gravado como | `NORMALIZED_NUMBER` do provider | Query `+E.164` | Query nacional | Query com máscara |
|---|---|---|---|---|
| `+14155552671` (E.164 do país do SIM) | `+14155552671` | ✅ 1 | ✅ 1 (`4155552671`) | ✅ 1 (`(415) 555-2671`) |
| `(415) 555-2671` (nacional do país do SIM) | `+14155552671` | ✅ 1 | ✅ 1 | ✅ 1 |
| `+5511912345678` (E.164 **estrangeiro**) | `+5511912345678` | ✅ 1 | ❌ **0** (`11912345678`) | — |
| `(11) 91234-5678` (nacional **estrangeiro**) | **`null`** | ❌ **0** | ✅ 1 (`11912345678` e `912345678`) | ✅ 1 |

**Regra derivada (HIGH — medida):**

1. O provider calcula `NORMALIZED_NUMBER` **na escrita**, com `PhoneNumberUtils.formatNumberToE164`
   usando o **país do aparelho**. Fica `null` se não parsear.
2. Uma query iniciada por `+` casa **só por igualdade de `NORMALIZED_NUMBER`**. Linhas com
   `NORMALIZED_NUMBER` nulo **não são alcançadas** — o fallback de "min match" (últimos 7 dígitos)
   **não** foi observado nesse caminho.
3. Uma query sem `+` casa por comparação de dígitos e alcança linhas com normalizado nulo — mas
   **não** alcança a linha gravada em E.164 estrangeiro.

**Corolário perigoso, medido:** `NORMALIZED_NUMBER` pode estar **errado**, não só nulo. Com SIM
`us`, o fixo brasileiro `(21) 3216-5498` virou `+12132165498`. Um cache construído sobre essa
coluna daria HIT para o número americano `+1 213 216 5498` e MISS para o brasileiro real.

**Quando isso morde o usuário brasileiro real (MEDIUM — não medido em BR físico):** num aparelho
com SIM BR, a maioria dos contatos nacionais normaliza certo e o `PhoneLookup` por E.164 funciona.
O buraco aparece em: contatos sincronizados de uma conta Google criada em outro país, contatos
gravados enquanto o usuário estava em roaming/chip estrangeiro, dual-SIM com chip primário
estrangeiro, e contatos importados de vCard sem DDI. Nesses casos o Sentinela trataria o contato
como desconhecido — a falha exata que o CONTEXT chama de "perigosa e silenciosa".

### Mitigação obrigatória: sonda dupla

```kotlin
// Fonte: matriz medida acima (emulador API 35). Custo: ~2 ms por sonda.
// PROBE 1 — E.164 (alcança contatos que o provider normalizou)
// PROBE 2 — número nacional significativo (alcança contatos com normalized = null)
private fun probeUris(e164: String, nationalDigits: String?): List<Uri> = buildList {
    add(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(e164)))
    if (nationalDigits != null && nationalDigits != e164) {
        add(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(nationalDigits)))
    }
}
```

O número nacional sai do `LibPhoneNumberNormalizer` (Phase 2 já tem a região por cascata
aparelho → preferência → BR). HIT em qualquer sonda ⇒ `ContactLookup.HIT`.

### Encoding e projeção (medidos)

- **`Uri.encode` é obrigatório na prática, não por gosto.** O caminho cru
  (`CONTENT_FILTER_URI + "/" + "(11) 91234-5678"`) até funcionou no teste, mas números com `#`
  (short codes, ramais) truncariam a URI no fragment. Sempre `Uri.withAppendedPath(..., Uri.encode(n))`.
- **Projeção mínima: `arrayOf(PhoneLookup._ID)` funciona.** Medido: a query aceita e retorna
  `columnNames = [_id, number]`.
  ⚠️ **O provider devolve a coluna `number` mesmo sem ser pedida** — o código **nunca** pode ler
  esse índice. `PhoneLookup.CONTACT_ID` também funciona.
- Projeção `arrayOf("1")` (truque de `SELECT 1`) **não** funciona:
  `IllegalArgumentException: Non-token detected in '1'`.
- **Nunca** projetar `DISPLAY_NAME`, `PHOTO_URI` ou `LOOKUP_KEY` — nenhum é necessário para
  HIT/MISS, e a ausência deles é o que o invariante de privacidade vai grepar.
- **Só `cursor.count` importa.** Nunca `moveToFirst()` + `getString`.

---

## Máquina de estado de permissão (API 29–35)

### A armadilha, confirmada na documentação oficial

`shouldShowRequestPermissionRationale()` retorna **`false` nos dois extremos**: antes do primeiro
pedido **e** depois da negação permanente. A doc do Android é explícita:

> "if the user taps Deny for a specific permission more than once during your app's lifetime of
> installation on a device, the user will no longer see the system permissions dialog... The
> user's action implies 'don't ask again', and is considered a permanent denial."

Não existe API pública para distinguir os dois casos. **É obrigatório persistir um flag "já
pedimos alguma vez"** — sem ele o app ou reperguntaria para sempre a quem negou de vez, ou nunca
mostraria o atalho para as Configurações.

### Algoritmo correto

```kotlin
enum class ContactsPermissionState { GRANTED, NEVER_ASKED, DENIED_ONCE, DENIED_PERMANENTLY }

// activity: precisa de Activity, não Context — shouldShowRequestPermissionRationale é de Activity.
fun state(activity: Activity, alreadyAsked: Boolean): ContactsPermissionState = when {
    ContextCompat.checkSelfPermission(activity, READ_CONTACTS) == PERMISSION_GRANTED ->
        GRANTED
    !alreadyAsked ->
        NEVER_ASKED                       // rationale=false porque nunca perguntamos
    ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_CONTACTS) ->
        DENIED_ONCE                       // dá para pedir de novo
    else ->
        DENIED_PERMANENTLY                // rationale=false COM alreadyAsked=true ⇒ "não perguntar de novo"
}
```

- `alreadyAsked` é gravado **no momento em que lançamos o launcher**, não no callback (o usuário
  pode matar o app com o diálogo aberto).
- **Onde mora o flag:** no **DataStore de configurações já existente** (`DataStoreSettingsRepository`,
  Phase 3), como um `booleanPreferencesKey("contacts_permission_asked")`. Nada de `SharedPreferences`
  novo, nada de tabela Room, nada de segundo `DataStore` — a Phase 3 provou que duas instâncias
  sobre o mesmo arquivo derrubam o processo.
- A regra pura (`state(...)`) deve ficar numa **função pura testável em JVM** que receba
  `granted: Boolean`, `alreadyAsked: Boolean`, `rationale: Boolean` — assim o Kover mede e o teste
  é determinístico. O wrapper que chama `ActivityCompat` é a camada fina, não testada.
- **Atalho para as Configurações** (só no estado `DENIED_PERMANENTLY`, e sem insistir):
  `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))`.
- **Reset em teste manual:** `adb shell pm clear-permission-flags org.sentinela.app android.permission.READ_CONTACTS user-set user-fixed`.
  Verificação de estado: `adb shell dumpsys package org.sentinela.app` mostra os flags
  `USER_SET` (negada uma vez) e `USER_FIXED` (negada permanentemente).
- **Sem a permissão, o repositório devolve `UNAVAILABLE` — nunca `MISS`.** Não confiar em
  `SecurityException`: `checkSelfPermission` **antes** da query, e `runCatching` em volta da query
  mesmo assim (revogação pode acontecer entre o check e o uso).

---

## `ContentObserver` sobre `ContactsContract`

### Qual URI observar — medido, e é contraintuitivo

Registrei **seis** observers ao mesmo tempo e disparei 30 `applyBatch` de inserção:

| URI observada | `notifyForDescendants` | Callbacks |
|---|---|---|
| `ContactsContract.AUTHORITY_URI` | `false` | 1 |
| **`ContactsContract.AUTHORITY_URI`** | **`true`** | **2** |
| `Contacts.CONTENT_URI` | `true` | 1 |
| `Phone.CONTENT_URI` | `true` | 1 |
| `Data.CONTENT_URI` | `true` | 1 |
| `RawContacts.CONTENT_URI` | `true` | 1 |

Todas as URIs funcionam, mas o provider notifica na **raiz** (`content://com.android.contacts`) —
observado diretamente nos callbacks de outra corrida:

```
E5 cb#1 uri=content://com.android.contacts/provider_status selfChange=false
E5 cb#2 uri=content://com.android.contacts                 selfChange=false
```

**Recomendação:** `ContactsContract.AUTHORITY_URI` com **`notifyForDescendants = true`**. É a única
combinação que pegou tudo (2 callbacks contra 1 das demais), e é a que o próprio app de Contatos
do AOSP usa. `notifyForDescendants = false` sobre uma URI filha corre risco real de nunca disparar,
porque a notificação chega na raiz.

### Ruído e debounce — medido

Duas corridas com resultados **diferentes**, e é isso que importa:

- 50 `applyBatch` intercalados, janela de 6 s → **51 callbacks** (≈ 1 por transação).
- 30 `applyBatch` em rajada, janela de 5 s → **1–2 callbacks** (coalescidos).

Ou seja: **a coalescência do provider existe mas não é garantida**. Uma sincronização de conta
Google numa agenda grande pode gerar dezenas de callbacks. Cada callback invalidando o cache
dispararia um rebuild de **1,5 s** (medido).

**Debounce é obrigatório, não otimização.** Padrão recomendado, sem dependência nova:

```kotlin
// Fonte: coalescência medida (51 callbacks / 50 transações no pior caso).
private val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 64,
    onBufferOverflow = BufferOverflow.DROP_OLDEST)

init {
    scope.launch {
        invalidations.debounce(DEBOUNCE_MS).collect { cache = null } // 500–1000 ms
    }
}
```

Invalidação **preguiçosa** (`cache = null`) em vez de rebuild imediato: quem pagar o rebuild é o
próximo consumidor em background, não o observer.

### Thread e vazamento

- O `ContentObserver` precisa de um `Handler`; **não use o main looper** (o callback abriria a
  porta para trabalho de agenda na UI thread). Use um `HandlerThread` dedicado, ou
  `Handler(Looper.getMainLooper())` **só** para postar num `Flow` — a primeira opção é mais limpa.
- **Precisa desregistrar?** O repositório é singleton de processo no `AppContainer`; a registro
  vive no `ContentService` do sistema, chaveado pelo processo, e morre com ele. Para **correção**
  não é necessário. Mas: (a) o `unregisterContentObserver` foi medido funcionando (`delta=0` após
  desregistrar) e (b) os **testes instrumentados** precisam dele para não vazarem entre casos.
  Portanto: expor `fun close()` e **usá-lo nos testes**; em produção nunca é chamado, e isso deve
  estar escrito em KDoc para não parecer esquecimento.
- **Registro só na primeira consulta**, junto com o cache preguiçoso — nunca em
  `Application.onCreate` (regra de cold start do CONTEXT).

---

## Provar que nada de contato é persistido (CTT-04)

### Bloco novo para `scripts/verify-invariants.sh`

Padrões **testados** contra o código atual (zero falso-positivo) e contra um vazamento simulado
(pegou os cinco casos):

```bash
# ---------------------------------------------------------------------------
# Bloco 6 — Fase 4: dado de contato nunca sai da memoria
# ---------------------------------------------------------------------------
echo "== Bloco 6: contatos apenas em memoria =="

# 6.1 — O schema EXPORTADO e o oraculo, nao o codigo. So os VALORES de
# "columnName" sao lidos: as chaves do JSON contem "name" e dariam falso-positivo.
LEAK_PAT='(^|_)(name|display|contact|photo|lookup|nome|agenda)'
LEAKED=$(grep -ohE '"columnName": "[^"]*"' app/schemas/*/*.json \
  | sed 's/.*: "//;s/"//' | sort -u | grep -E "$LEAK_PAT")
if [ -z "$LEAKED" ]; then
  ok "nenhuma coluna de dado de contato no schema exportado"
else
  echo "$LEAKED" | sed 's/^/      /'
  fail "coluna de dado de contato no schema Room — proibido (CTT-04, docs/PRIVACIDADE.md)"
fi

# 6.2 — Fronteira: SO data/contacts/ conhece ContactsContract.
OUTSIDE=$(grep -rln "ContactsContract" app/src/main/java --include="*.kt" \
  | grep -v "^app/src/main/java/org/sentinela/app/data/contacts/")
if [ -z "$OUTSIDE" ]; then
  ok "ContactsContract confinado em data/contacts/"
else
  echo "$OUTSIDE" | sed 's/^/      /'
  fail "ContactsContract fora de data/contacts/ — dado de agenda vazando de camada"
fi

# 6.3 — Colunas de identidade nunca sao projetadas. HIT/MISS so precisa de _ID.
IDENTITY=$(grep -rnE "DISPLAY_NAME|PHOTO_URI|PHOTO_THUMBNAIL_URI|LOOKUP_KEY|Contacts\.CONTENT_LOOKUP_URI" \
  app/src/main/java --include="*.kt")
if [ -z "$IDENTITY" ]; then
  ok "nenhuma coluna de identidade de contato projetada"
else
  echo "$IDENTITY" | sed 's/^/      /'
  fail "projecao de nome/foto/lookup de contato — so PhoneLookup._ID e permitido (CTT-04)"
fi

# 6.4 — data/contacts/ nao pode escrever em lugar nenhum.
PERSIST=$(grep -rnE "@Entity|@Dao|Room\.|DataStore|edit \{|openFileOutput|SharedPreferences" \
  app/src/main/java/org/sentinela/app/data/contacts 2>/dev/null)
if [ -z "$PERSIST" ]; then
  ok "data/contacts/ nao persiste nada"
else
  echo "$PERSIST" | sed 's/^/      /'
  fail "data/contacts/ tocando persistencia — contatos vivem so em memoria (CTT-04)"
fi

# 6.5 — WRITE_CONTACTS nunca, em manifest nenhum.
if [ "$(grep -c "WRITE_CONTACTS" "$M")" -eq 0 ]; then
  ok "sem WRITE_CONTACTS no manifest mergeado"
else
  fail "WRITE_CONTACTS no manifest — testes usam adoptShellPermissionIdentity, nao permissao"
fi
```

**Verificação executada:** os valores de `columnName` do schema atual são
`id, number_key, description, enabled, created_at_utc_millis, masked_number, number_e164,
timestamp_utc_millis, reason_code, notification_shown, classification` — nenhum casa com
`LEAK_PAT`. O mesmo padrão contra `display_name / contact_name / photo_uri / lookup_key /
nome_contato` casou **os cinco**. O bloco 5 existente (`contactName|contact_name|displayName|
display_name` em `data/local`) continua válido e **complementar** — 6.1 olha o artefato
(o schema), 5 olha o fonte.

### Teste de schema (JVM, complementa o script)

`SchemaExportTest` da Phase 3 já lê `app/schemas/*/1.json`. Estender com um caso
`schemaNaoTemColunaDeContato()` aplicando o mesmo `LEAK_PAT` sobre os `columnName` do JSON. Lembrar
da lição da Phase 3: `schemas/` já está declarado como `inputs.dir` das `Test` tasks — sem isso o
teste ficaria `UP-TO-DATE` e passaria verde com o schema apagado.

### Prova de vermelho (regra do projeto)

Cada guarda-corpo acima precisa ser demonstrado falhando: acrescentar temporariamente uma coluna
`display_name` a uma `@Entity`, rodar, ver 6.1 **e** o teste de schema vermelhos, e restaurar.

---

## Testes instrumentados com contatos

### Como inserir — a única forma que funciona sem permissão nova

**Medido e reprovado:** `WRITE_CONTACTS` no `app/src/androidTest/AndroidManifest.xml` **não
funciona**. A instrumentação roda no uid do app sob teste:

```
Permission Denial: writing ContactsProvider2 ... from pid=2268, uid=10207
requires android.permission.WRITE_CONTACTS
```

`GrantPermissionRule.grant(WRITE_CONTACTS)` também não resolve — não se concede o que o pacote não
declara. (Confirmado que `READ_CONTACTS`, esse sim declarado no manifest principal, **é** concedido
pela rule: o erro migrou de leitura para escrita.)

**A forma correta, medida funcionando:**

```kotlin
// Fonte: medido no emulador API 35. Nenhum manifest ganha WRITE_CONTACTS.
@Before fun setUp() {
    val inst = InstrumentationRegistry.getInstrumentation()
    inst.uiAutomation.adoptShellPermissionIdentity(
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CONTACTS,
    )
    cr.delete(ContactsContract.RawContacts.CONTENT_URI, null, null) // limpeza defensiva
}

@After fun tearDown() {
    cr.delete(ContactsContract.RawContacts.CONTENT_URI, null, null)
    InstrumentationRegistry.getInstrumentation().uiAutomation.dropShellPermissionIdentity()
}
```

Inserção via `applyBatch` de três ops (`RawContacts` + `StructuredName` + `Phone`) com
`withValueBackReference(Data.RAW_CONTACT_ID, 0)`. Limpeza: `delete` em `RawContacts.CONTENT_URI`
apaga em cascata — medido (`wipe deleted=4` após 4 contatos).

**Merge do manifest — verificado:** o `androidTest` produz um manifest **separado**
(`app/build/intermediates/packaged_manifests/debugAndroidTest/.../AndroidManifest.xml`, que contém
só `REORDER_TASKS` injetada pelo Espresso) e **não** alimenta
`merged_manifest/debug/processDebugMainManifest/` — que é o arquivo lido por
`verify-invariants.sh`. Com `adoptShellPermissionIdentity` a questão nem se coloca: `WRITE_CONTACTS`
não aparece em lugar nenhum. O invariante 6.5 acima trava isso para sempre.

**Ainda assim, `READ_CONTACTS` precisa estar no manifest principal** — é o app sob teste que
consulta. Isto é a fase dela (`docs/PERMISSOES.md:14`), então está correto.

### Custo de fixture

Inserir 5.000 contatos custa **7–14 s**. Nunca em `@Before`. Use `@BeforeClass` num teste de
performance dedicado (`ContactLookupPerformanceTest`), separado dos testes de comportamento (que
precisam de 3–5 contatos e rodam em ~1 s).

---

## Regra de processo do manifest — as DUAS edições obrigatórias

Adicionar `READ_CONTACTS` ao manifest e rodar `verify-invariants.sh` produz **duas** falhas
(medido, saída literal):

```
FAIL: permissao fora da allowlist: android.permission.READ_CONTACTS — ver docs/PERMISSOES.md
      READ_CONTACTS
FAIL: permissao de fase futura antecipada — ver docs/PERMISSOES.md
== 2 invariante(s) violado(s) ==
```

Os **dois** pontos do script precisam ser editados no **mesmo commit** do manifest:

1. Acrescentar `android.permission.READ_CONTACTS` à variável `ALLOWLIST` (linha ~39).
2. Remover `READ_CONTACTS|` da variável `FUTURE` (linha ~56) — e **acrescentar `WRITE_CONTACTS`**
   a ela, que continua proibida para sempre.

`docs/PERMISSOES.md` já autoriza `READ_CONTACTS` na Fase 4 e **não precisa mudar de conteúdo** —
mas a leitura é bloqueante e a conferência tem que estar registrada. Nenhuma outra permissão entra.

---

## Arquitetura recomendada

```
app/src/main/java/org/sentinela/app/
├── data/contacts/
│   ├── ContactLookupRepository.kt        # interface (JÁ EXISTE, não mudar a assinatura)
│   ├── ContactsPermissionState.kt        # enum + função PURA de estado (JVM-testável, Kover mede)
│   ├── ContactKeyCache.kt                # Set<String> + debounce; SEM import android.* de agenda
│   └── ContactsContractLookupSource.kt   # ÚNICA classe que conhece ContactsContract (Kover exclui)
```

### Pattern 1: fonte fina + regra pura

**O quê:** `ContactsContractLookupSource` só sabe fazer duas coisas — "existe alguma linha para
esta string?" e "me devolva todos os `Phone.NUMBER`". Nada de decisão, nada de cache, nada de
normalização.
**Por quê:** é o único jeito de o Kover medir a lógica sem falso-vermelho (ver §Kover) e o único
jeito de o invariante 6.2 significar alguma coisa.

### Pattern 2: cold path direto, cache assíncrono

```kotlin
// Fonte: medições acima — build 1,5 s vs consulta direta 2 ms.
override suspend fun lookup(numberE164: String): ContactLookup {
    if (!source.hasPermission()) return ContactLookup.UNAVAILABLE
    cache.get()?.let { keys ->                       // cache quente: p50 ~1 µs
        return if (numberE164 in keys) ContactLookup.HIT else ContactLookup.MISS
    }
    cache.warmInBackground()                          // 1,5 s, NUNCA aguardado aqui
    return runCatching { source.probe(numberE164) }   // sonda dupla, ~2-4 ms
        .getOrElse { ContactLookup.UNAVAILABLE }      // nunca MISS em erro (DEC/FallbackPolicy)
}
```

⚠️ O `MISS` do cache quente também está sujeito ao problema de normalização — por isso o cache
**tem** que ser construído com o `LibPhoneNumberNormalizer`, não com `NORMALIZED_NUMBER`.

### Anti-patterns

- **Construir o cache a partir de `Phone.NORMALIZED_NUMBER`.** Medido errado (`+12132165498`) e
  nulo. Já é o bug pronto.
- **`await` no build do cache dentro do `lookup`.** 1,5 s medidos contra 200 ms de orçamento.
- **Registrar o observer em `Application.onCreate`.** Viola a regra de cold start do CONTEXT e
  arrasta o bind do provider (6,2 ms medidos) para o caminho crítico do Service.
- **Confiar em `SecurityException` para detectar ausência de permissão.** Faz o erro virar exceção
  no caminho quente; `checkSelfPermission` custa microssegundos.
- **Ler `cursor.getString(...)` do `PhoneLookup`.** `count` basta, e o provider devolve a coluna
  `number` mesmo sem ser pedida — ler é vazamento gratuito.
- **Sonda única por E.164.** MISS falso comprovado.

---

## Don't Hand-Roll

| Problema | Não construa | Use | Por quê |
|---|---|---|---|
| Comparar números de telefone | Comparação de sufixo / "últimos 8 dígitos" própria | `LibPhoneNumberNormalizer` (Phase 2) + `PhoneLookup` | O 9º dígito BR, DDI, DDD e short codes já estão resolvidos e testados desde a Phase 2; a chave tem que ser **a mesma** da whitelist |
| Normalizar contato para E.164 | `PhoneNumberUtils.formatNumberToE164` | `LibPhoneNumberNormalizer` | É exatamente o que o provider usa e foi medido produzindo `+12132165498` para um fixo do Rio |
| Detectar mudança na agenda | Polling / `AlarmManager` / `WorkManager` | `ContentObserver` em `AUTHORITY_URI` | Sem custo de bateria, sem dependência nova (WorkManager é proibido no MVP) |
| Debounce | `Handler.postDelayed` com token manual | `Flow.debounce` (coroutines já no projeto) | Cancelamento e concorrência corretos de graça |
| Conceder permissão em teste | `WRITE_CONTACTS` em manifest de teste | `uiAutomation.adoptShellPermissionIdentity` | Medido: o manifest de androidTest **não funciona**, e a alternativa não suja o manifest mesclado |
| Máscara de número em log | Nova função | `PhoneMask.mask` (Phase 2) | Já é a única máscara do projeto |

---

## Common Pitfalls

### Pitfall 1: MISS falso por país do aparelho ≠ país do contato
**O que dá errado:** contato conhecido é bloqueado como desconhecido.
**Causa raiz:** `NORMALIZED_NUMBER` nulo/errado; query E.164 não alcança a linha.
**Como evitar:** sonda dupla (E.164 + nacional) e cache normalizado pelo app.
**Sinal de alerta:** teste com contato gravado em formato nacional de outra região devolve MISS.
**Teste que trava:** inserir `(11) 91234-5678` **e** `+5511912345678` e exigir HIT nas duas grafias
com a mesma query — este teste **falha hoje** com implementação ingênua de sonda única.

### Pitfall 2: rebuild de cache em tempestade de sincronização
**O que dá errado:** dezenas de rebuilds de 1,5 s consecutivos.
**Causa raiz:** coalescência do provider não é garantida (51 callbacks / 50 transações medidos).
**Como evitar:** `debounce` 500–1000 ms + invalidação preguiçosa.

### Pitfall 3: `notifyForDescendants = false` numa URI filha
**O que dá errado:** o cache nunca invalida; contato novo nunca é reconhecido.
**Causa raiz:** o provider notifica na raiz `content://com.android.contacts`.
**Como evitar:** `AUTHORITY_URI` + `true` (medido: única combinação com 2 callbacks).

### Pitfall 4: `shouldShowRequestPermissionRationale` como único sinal
**O que dá errado:** ou repergunta eternamente, ou nunca oferece o atalho para Configurações.
**Causa raiz:** retorna `false` antes do 1º pedido **e** após negação permanente.
**Como evitar:** flag `contacts_permission_asked` no DataStore existente.

### Pitfall 5: afrouxar `verify-invariants.sh` antes da documentação
**Causa raiz:** o script falha em **dois** lugares, e é tentador só mexer na `ALLOWLIST`.
**Como evitar:** as duas edições + `WRITE_CONTACTS` acrescentado ao `FUTURE`, no mesmo commit.

### Pitfall 6: fixture de 5.000 contatos em `@Before`
**O que dá errado:** suíte instrumentada de ~15 s vira minutos.
**Como evitar:** `@BeforeClass` em classe de performance separada.

---

## State of the Art

| Antes | Agora | Impacto |
|---|---|---|
| `PhoneLookup` "resolve formatação sozinho" (crença geral, e do CONTEXT) | Casa por `NORMALIZED_NUMBER` calculado com o país do aparelho | Exige sonda dupla |
| `WRITE_CONTACTS` no manifest de androidTest | `adoptShellPermissionIdentity` (AndroidX Test) | Manifest mesclado limpo |
| `Handler.postDelayed` para debounce | `Flow.debounce` | Já no projeto |

**Depreciado/irrelevante nesta fase:** `Contacts.CONTENT_FILTER_URI` (busca por nome — não é o
caso de uso e leria nome), `PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI` (perfil de trabalho; fora
de escopo do MVP e exige `INTERACT_ACROSS_USERS` em alguns caminhos).

---

## Kover — recomendação concreta

O filtro atual já inclui `org.sentinela.app.data.*`, então **`data.contacts.*` entra
automaticamente — nenhuma edição de `includes` é necessária.** O risco é o oposto: as classes que
tocam `ContentResolver` só rodam em teste **instrumentado**, que o Kover não mede, e derrubariam o
gate com falso-vermelho — exatamente o que aconteceu com o gerado pelo Room na Phase 3.

**Recomendação:** manter os `includes` como estão e acrescentar **um único** `excludes`, seguindo
o precedente literal de `data.local.db.*`:

```kotlin
excludes {
    classes("org.sentinela.app.data.local.db.*")
    classes("*_Impl", "*_Impl\$*")
    annotatedBy("androidx.room.Dao", "androidx.room.Database")
    // Fase 4: a fonte do ContactsContract so executa em teste INSTRUMENTADO
    // (mesma razao do gerado pelo Room). A logica pura — estado de permissao,
    // cache e decisao HIT/MISS/UNAVAILABLE — fica FORA deste exclude e continua medida.
    classes("org.sentinela.app.data.contacts.ContactsContractLookupSource")
}
```

Excluir a **classe nomeada**, nunca `data.contacts.*` inteiro: o enum de permissão, a função pura
de estado e o cache são JVM-testáveis e **têm** que contar para os 80%.

**Ordem obrigatória (lição da Phase 2 e da Phase 3):** o ajuste do Kover é o **último plano da
fase**, depois de os testes existirem, e o gate só é aceito depois de demonstrado falhando (subir
`minBound` temporariamente). Cobertura atual medida nesta pesquisa: **97,2881%**.

---

## Validation Architecture

### Test Framework

| Property | Value |
|---|---|
| Framework (JVM) | JUnit 4 `4.13.2`, AGP 9.3.0 / Gradle 9.6.1 / JDK 17. **Sem Robolectric** (4.16.1 não suporta compileSdk 37) |
| Framework (instrumentado) | AndroidX Test `AndroidJUnitRunner`, `androidx.test.ext:junit-ktx 1.3.0`, `androidx.test:core-ktx 1.7.0`, `androidx.test:rules 1.7.0` |
| Config file | `app/build.gradle.kts` (`testOptions`, `sourceSets androidTest assets`, `kover`) |
| Cobertura | Kover `0.9.9`, gate `koverVerify` `minBound(80)`; atual **97,2881%** |
| Quick run command | `./gradlew testDebugUnitTest` (~15 s) |
| Instrumented command | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` |
| Full suite command | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| Pré-requisitos | `ANDROID_HOME`; JDK 17; `-XX:MaxMetaspaceSize=1g`; AVD `Medium_Phone_API_35` |
| Evidência | logcat por teste em `app/build/outputs/androidTest-results/connected/debug/<AVD>/logcat-<classe>-<metodo>.txt` (é de lá que saem os percentis) |

**Dependência nova: nenhuma.** `androidx.test:rules` e `core-ktx` já estão em `androidTest`;
`adoptShellPermissionIdentity` vem do `Instrumentation`, sem biblioteca.

### Phase Requirements → Test Map

| Req | Comportamento | Tipo | Comando automatizado | Existe? |
|---|---|---|---|---|
| CTT-01 | Estado de permissão: GRANTED / NEVER_ASKED / DENIED_ONCE / DENIED_PERMANENTLY (função pura) | unit | `./gradlew testDebugUnitTest --tests "*ContactsPermissionStateTest"` | ❌ Wave 0 |
| CTT-01 | Flag `contacts_permission_asked` persiste no DataStore existente | unit (DataStore em `TemporaryFolder`) | `./gradlew testDebugUnitTest --tests "*DataStoreSettingsRepositoryTest"` | ✅ estender |
| CTT-01 | Sem permissão ⇒ `UNAVAILABLE`, nunca `MISS` | unit (fake source) | `./gradlew testDebugUnitTest --tests "*ContactLookupRepositoryTest"` | ❌ Wave 0 |
| CTT-01 | `READ_CONTACTS` no manifest mesclado + allowlist + `WRITE_CONTACTS` ainda barrada | script | `./gradlew assembleDebug && bash scripts/verify-invariants.sh` | ✅ estender (2 pontos) |
| CTT-02 | HIT/MISS reais contra `ContactsContract`, incluindo contato gravado em formato nacional | instrumentado | `bash scripts/run-instrumented-tests.sh --tests "*ContactLookupSourceTest"` | ❌ Wave 0 |
| CTT-02 | p50 do cache quente < 10 ms com 5.000 contatos (**assert primário: mediana**) | instrumentado | `bash scripts/run-instrumented-tests.sh --tests "*ContactLookupPerformanceTest"` | ❌ Wave 0 |
| CTT-02 | Cache é realmente usado — **prova estrutural, não cronômetro**: contador de queries no source fake não incrementa no 2º lookup | unit | `./gradlew testDebugUnitTest --tests "*ContactKeyCacheTest"` | ❌ Wave 0 |
| CTT-02 | Invalidação por `ContentObserver` + debounce | instrumentado | `bash scripts/run-instrumented-tests.sh --tests "*ContactsObserverTest"` | ❌ Wave 0 |
| CTT-04 | Schema exportado sem coluna de dado de contato | unit (lê o JSON) | `./gradlew testDebugUnitTest --tests "*SchemaExportTest"` | ✅ estender |
| CTT-04 | `ContactsContract` confinado; nenhuma projeção de nome/foto; `data/contacts/` sem persistência | script (Bloco 6) | `bash scripts/verify-invariants.sh` | ❌ Wave 0 |
| CTT-04 | Backup continua sem arquivo novo | unit | `./gradlew testDebugUnitTest --tests "*BackupRulesTest"` | ✅ existe |

**Assert primário sempre a mediana** (decisão da Phase 3). O p95 do `PhoneLookup` direto medido
(6,9–8,2 ms, com `max` de até 73,9 ms) é **reportado no logcat, não afirmado** — e vira cenário
novo em `docs/TESTE-FISICO-SAMSUNG.md` para a Phase 9, junto com "agenda real do usuário, contato
importado sem DDI".

### Sampling Rate

- **Por commit de task:** `./gradlew testDebugUnitTest` (< 30 s). Tasks só-instrumentadas
  acrescentam `bash scripts/run-instrumented-tests.sh --tests "*Padrao"` com o emulador de pé.
- **Por wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
- **Phase gate:** suíte completa pós-`clean` com `--no-build-cache`, `koverVerify` verde, logcat
  dos percentis arquivado em `04-EVIDENCE.md`.
- Emulador sobe **uma vez** por sessão. Sem watch mode.

### Wave 0 Gaps

- [ ] `scripts/verify-invariants.sh` — as **duas** edições de permissão (`ALLOWLIST` +
      `FUTURE`) no mesmo commit do manifest. **Bloqueia todo build verde da fase.**
- [ ] `app/src/main/AndroidManifest.xml` — `READ_CONTACTS`.
- [ ] `app/src/androidTest/.../ContactsTestFixture.kt` — helper de
      `adoptShellPermissionIdentity` + `applyBatch` de inserção + `wipe`. **Bloqueia toda task
      instrumentada da fase.**
- [ ] Enum + função pura de estado de permissão (consumida pela Phase 7).

**Não é Wave 0, deliberadamente:** o `excludes` do Kover fica no **último** plano da fase — ligá-lo
antes de as classes existirem não faz sentido, e ligar o gate antes dos testes quebra o build
(lição literal das Phases 2 e 3). Nenhuma instalação de framework é necessária.

---

## Open Questions

1. **Comportamento em aparelho BR físico com agenda real.**
   - Sabemos: com SIM `us`, contato nacional BR não é achado por E.164 (medido).
   - Não sabemos: qual fração de uma agenda BR real num aparelho BR tem `NORMALIZED_NUMBER` nulo.
   - Recomendação: implementar a sonda dupla de qualquer jeito (custo ~2 ms) e acrescentar cenário
     à Phase 9 — "contato importado de vCard sem DDI é reconhecido".

2. **Samsung / One UI e o provider de contatos.**
   - Samsung substitui o app de Contatos, mas o `ContactsProvider2` é AOSP. Risco baixo, não zero.
   - Recomendação: cenário na Phase 9, não hack preventivo (regra do `CLAUDE.md`).

3. **Discrepância de contagem em `Phone.CONTENT_URI` com 5.000 linhas.**
   - Uma corrida devolveu 5.000 linhas com projeção `NUMBER` e 2.501 com projeção
     `NORMALIZED_NUMBER` isolada. Não reproduzi de forma limpa.
   - Impacto: **nenhum no desenho recomendado** (que lê `NUMBER`), mas é mais uma razão para não
     depender de `NORMALIZED_NUMBER`.
   - Recomendação: o teste de build de cache deve **assertar o tamanho do conjunto**, não confiar
     no cursor.

---

## Sources

### Primary (HIGH)
- **Medição própria**, emulador `Medium_Phone_API_35` (API 35), testes instrumentados descartados
  após a coleta (`git status` limpo): matriz de `PhoneLookup`, `NORMALIZED_NUMBER`, percentis de
  lookup, custo de build de cache, contagem de callbacks de `ContentObserver`, falha de
  `WRITE_CONTACTS` em manifest de androidTest, sucesso de `adoptShellPermissionIdentity`.
- **Execução própria** de `scripts/verify-invariants.sh` com `READ_CONTACTS` no manifest (2 falhas
  reproduzidas) e do `LEAK_PAT` contra o schema atual e contra vazamento simulado.
- `./gradlew koverLog` — 97,2881%.
- https://developer.android.com/training/permissions/requesting — semântica de
  `shouldShowRequestPermissionRationale`, negação permanente, `pm clear-permission-flags`.

### Project (HIGH)
- `docs/PERMISSOES.md:14` (Fase 4 é a fase do `READ_CONTACTS`), `docs/PRIVACIDADE.md`,
  `CLAUDE.md`, `.planning/STATE.md` (decisões das Phases 1–3),
  `.planning/phases/03-dados-locais/03-VALIDATION.md`.

### Secondary (MEDIUM)
- Comportamento em aparelho BR real — inferido da medição no emulador, **não** verificado em campo.

---

## Metadata

**Confiança por área:**
- Semântica do `PhoneLookup`: **HIGH** — matriz medida com 4 formas de gravação × 4 de consulta.
- Números de performance: **HIGH** no emulador, **MEDIUM** como previsão de aparelho físico.
- `ContentObserver`: **HIGH** — 6 combinações de URI medidas simultaneamente.
- Máquina de estado de permissão: **HIGH** — doc oficial + trap confirmada.
- Infra de teste com contatos: **HIGH** — caminho errado reprovado e caminho certo aprovado por execução.
- Impacto real no usuário BR: **MEDIUM** — o mecanismo está provado, a frequência não.

**Research date:** 2026-07-29
**Valid until:** ~2026-08-28 (API de plataforma estável; revalidar se `compileSdk` subir)

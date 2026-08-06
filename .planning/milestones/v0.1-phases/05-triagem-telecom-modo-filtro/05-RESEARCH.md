# Phase 5: Triagem Telecom Modo Filtro - Research

**Researched:** 2026-07-29
**Domain:** `android.telecom.CallScreeningService`, `RoleManager.ROLE_CALL_SCREENING`, notificação silenciosa
**Confidence:** HIGH (fonte AOSP lida diretamente + 6 medições executadas neste repo)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Resposta única e resiliência**

- **Garantia de 1×:** `AtomicBoolean` por chamada **mais** um `try/finally` que responde de forma
  permissiva se nada respondeu. Testado com exceção **injetada em cada ponto** do caminho, não
  apenas no topo. Este é o critério 3 do ROADMAP e o invariante mais importante da classe.
- **Timeout interno de 1 s** para o conjunto das consultas locais — 5× de folga sobre o limite de
  5 s da plataforma. Ao estourar, decide com `ContactLookup.UNAVAILABLE` e deixa a `FallbackPolicy`
  (que já existe e é testada desde a Phase 2) resolver. Não inventar caminho novo.
- **Exceção inesperada → PERMITIR a chamada.** Bloquear por bug é pior que deixar passar: o
  usuário perde uma ligação importante e não tem como descobrir o motivo. O valor do produto é
  não interromper, não bloquear a qualquer custo.
- **O Service usa o `ContactLookupRepository` real**, não `MISS` hardcoded. No modo filtro o
  Android já não entrega contatos ao `onScreenCall`, então o custo é baixo — e o comportamento
  continua correto se essa premissa da plataforma mudar ou se o modo discador (Phase 6) entrar.

**Tradução `CallDecision` → `CallResponse`**

- `Reject` → `setDisallowCall(true)` + `setRejectCall(true)` + `setSkipCallLog` conforme
  configuração + `setSkipNotification(true)`.
- `Silence` → `setSilenceCall(true)` **sem** `disallowCall`: toca mudo, a tela de chamada aparece
  e o registro entra no log nativo. É comportamento diferente de bloquear — não confundir.
- `SendSilentlyToVoicemail` → `disallowCall` + `rejectCall` + `skipNotification`. **A ida à caixa
  postal depende da operadora** — a UI e a documentação **não podem prometer** que sempre cai lá.
- `BlockWithoutTrace` → `disallowCall` + `rejectCall` + `skipCallLog(true)` + `skipNotification(true)`.
  `skipCallLog` **varia por OEM** — vai obrigatoriamente para o roteiro Samsung da Phase 9, e o
  app não deve afirmar garantia.
- `Allow` → resposta vazia (não interferir).

**Notificação e histórico**

- **Ordem inegociável:** `respondToCall` **primeiro**, sempre. Notificação e histórico depois, em
  corrotina desacoplada que **não pode** atrasar nem derrubar a resposta. Uma falha ao gravar o
  histórico jamais pode virar uma chamada não respondida.
- **Notificação desligada por padrão** — opt-in explícito.
- **`POST_NOTIFICATIONS` pedida em runtime somente quando o usuário liga a notificação**, nunca no
  onboarding. A permissão já está **declarada** no manifest desde a Phase 1.
- **Conteúdo:** número **mascarado** por padrão (`PhoneMask.mask`, a mesma função única), com opção
  de "sem identificação". Nunca o número completo.
- Canal com `IMPORTANCE_LOW`: sem som, vibração, heads-up ou full-screen.

**Onde cada critério é provado — DECISÃO DO USUÁRIO (2026-07-29)**

Escolhido: **bench + emulador agora, comportamento de OEM na Phase 9.**

- **Agora, falhando o build:** bench da decisão com assert na **mediana** (nunca em percentil de
  cauda); testes instrumentados do Service no emulador cobrindo a tradução de resposta, a garantia
  de 1× e a ordem das operações.
- **Phase 9, roteiro Samsung:** o bloqueio real de chamada (critérios 1 e 2), o comportamento de
  `skipCallLog`, a interação com Não Perturbe e a caixa postal (critério 6).
- O p95 < 200 ms continua sendo o compromisso declarado do produto — apenas é **verificado em
  hardware real**. A mediana é que trava o build.
- Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`.

### Claude's Discretion

- Estrutura interna do Service, nome das classes auxiliares, como o timeout é implementado e a
  organização dos testes ficam a critério do executor, desde que os invariantes acima e os 6
  critérios de sucesso sejam honrados.

### Deferred Ideas (OUT OF SCOPE)

- Modo discador (`ROLE_DIALER` + `InCallService`) e políticas por contato individual — Phase 6.
- Telas de onboarding, home e Proteção — Phase 7.
- Telas de whitelist e histórico — Phase 8.
- Validação do bloqueio real, `skipCallLog`, Não Perturbe e caixa postal em Samsung físico,
  além do p95 < 200 ms em hardware — Phase 9.
</user_constraints>

---

## Summary

Esta pesquisa leu a **fonte AOSP** de `CallScreeningService.java` (SDK local android-35, idêntica
ao `main` do AOSP) e de `CallScreeningServiceFilter.java` / `IncomingCallFilterGraph.java` /
`Timeouts.java` / `DndCallFilter.java` do `packages/services/Telecomm`, e executou **6 medições**
neste repositório (Robolectric em JVM + emulador `Medium_Phone_API_35`). O resultado derruba
**quatro** premissas registradas no projeto e resolve o problema de testabilidade que era o maior
risco da fase.

As duas descobertas que mudam o plano: (1) **`setSkipCallLog` é um no-op para o Sentinela** — o
código do Telecom só honra o parâmetro para pacotes `PACKAGE_TYPE_CARRIER`; para um app de
terceiros escolhido pelo usuário a chamada bloqueada **sempre** entra no log nativo como
`BLOCKED_TYPE`. Não é variação de OEM: é AOSP. (2) **Com `READ_CONTACTS` concedida (Phase 4), o
`onScreenCall` passa a receber TAMBÉM as chamadas de contatos**, mesmo no modo filtro — o guard do
Telecom é literalmente `if (contactExists && !hasReadContactsPermission()) skip bind`. A premissa
de bootstrap "o modo filtro só recebe não-contatos" deixou de valer no instante em que a Phase 4
entregou. Isso transfere o critério de sucesso 2 ("contato toca normalmente") da plataforma para o
**nosso** código.

A terceira descoberta é boa notícia: o **Service real é 100% testável em JVM**. Medi que
`Robolectric.buildService(...)` hospeda um `CallScreeningService`, que `Call.Details` é mockável
com MockK, e que injetando um `java.lang.reflect.Proxy` de `ICallScreeningAdapter` no campo
privado `mCallScreeningAdapter` é possível **contar e inspecionar cada `ParcelableCallResponse`
emitida** — provando resposta-única, tradução e ordem sem emulador e sem chamada real, em ~2 s.
A mesma medição provou que `respondToCall` chamado 2× **não lança e não derruba o processo**:
emite dois IPCs silenciosamente. Ou seja, a guarda `AtomicBoolean` é a *única* proteção existente.

**Primary recommendation:** extrair toda a lógica para um colaborador puro (`ScreeningCoordinator`)
com uma costura `(CallResponse) -> Unit`, deixar o Service com ~10 linhas, provar os invariantes com
o harness Robolectric medido abaixo, e **corrigir a tradução e a documentação** para refletir que
`skipCallLog` não funciona e que contatos agora chegam ao Service.

---

<phase_requirements>
## Phase Requirements

| ID | Descrição | Suporte da pesquisa |
|----|-----------|---------------------|
| SCR-01 | Onboarding solicita `ROLE_CALL_SCREENING` via RoleManager | §Ciclo de vida do papel — `createRequestRoleIntent` + `ActivityResultLauncher`; `ScreeningRoleManager` já pronto |
| SCR-02 | App verifica continuamente o papel e oferece correção | §Ciclo de vida do papel — **não existe callback/observer**; só polling em `onResume` (verificado) |
| SCR-03 | Desconhecido bloqueado antes de tocar | §Tradução — `setDisallowCall(true)` + `setRejectCall(true)`; medido que a validação é idêntica de API 29 a 35 |
| SCR-04 | Privado/restrito bloqueado por padrão | ⚠️ **Impossível no modo filtro** — §Pitfall 3. `PRESENTATION_RESTRICTED/UNKNOWN/UNAVAILABLE/PAYPHONE` **não são entregues** ao Service |
| SCR-05 | `respondToCall` exatamente 1×, antes de 5 s | §Limite de 5 s + §Harness — dupla resposta medida como no-op silencioso; guarda é responsabilidade nossa |
| SCR-06 | Rejeitar vs. encaminhar para caixa postal | §Tradução — mesma `CallResponse`; diferença é da operadora, não da API. UI não pode prometer |
| SCR-07 | Chamada bloqueada fora do histórico nativo (`setSkipCallLog`) | ⚠️ **No-op para nós** — §Descoberta 1. Requer reescrita do requisito e de `docs/LIMITACOES.md` |
| SCR-08 | Notificação nativa de perdida suprimida | §Tradução — `setSkipNotification(true)` é honrado (`setShouldShowNotification(!skipNotification)`, sem gate de pacote) |
| SCR-09 | Chamadas de saída sem interferência | §Descoberta 4 — o framework responde sozinho para `DIRECTION_OUTGOING`; `respondToCall` é ignorado. Retornar cedo |
| SCR-10 | Service protegido contra 10 modos de falha | §Arquitetura + §Harness — matriz de injeção de exceção testável em JVM |
| SCR-11 | p95 < 200 ms no cold path | §Orçamento medido — caminho quente medido em **23,3 ms**; pior componente frio 19,0 ms |
| DEC-01..05 | Integração do motor existente | §Arquitetura — Service não contém regra; só monta entrada e traduz saída |
| QLT-01 | Casos obrigatórios da seção 13 | §Validation Architecture — mapa de testes |
| QLT-06 (parcial) | Instrumentados verdes | §Validation Architecture — bind do Service via `ServiceTestRule` |
| NTF-01..06 | Notificação própria | §Notificação — canal, runtime permission, lock screen |
</phase_requirements>

---

## Descobertas que mudam o plano

### Descoberta 1 — `setSkipCallLog` é NO-OP para o Sentinela (HIGH, fonte AOSP)

Não é "best-effort" nem variação de OEM. É a lógica do Telecom, lida em
`packages/services/Telecomm/.../CallScreeningServiceFilter.java`:

```java
// no ramo disallowCall(...)
.setShouldAddToCallLog(!response.shouldSkipCallLog()
        || packageTypeShouldAdd(mPackagetype))
...
private boolean packageTypeShouldAdd(int packageType) {
    return packageType != PACKAGE_TYPE_CARRIER;
}
```

O Sentinela é `PACKAGE_TYPE_USER_CHOSEN` (2) — nem `CARRIER` (0), nem `DEFAULT_DIALER` (1). Logo
`packageTypeShouldAdd` devolve `true`, o `||` curto-circuita, e `shouldAddToCallLog` é **sempre
`true`**, qualquer que seja `setSkipCallLog`. O javadoc confirma em duas frases:

> "Note: Calls will still be logged with type `android.provider.CallLog.Calls#BLOCKED_TYPE`,
> regardless of how this property is set."
> "Note: Only the carrier and system call screening apps can use this parameter; this parameter
> is ignored otherwise."

**Discrepância honesta:** o javadoc diz "carrier **and system**", mas o código só isenta
`CARRIER` — nem o discador padrão consegue pular o log. Isso é relevante para a Phase 6: virar
`ROLE_DIALER` **não** destrava `skipCallLog`.

**Consequências para o plano:**
- `BlockWithoutTrace` e `Reject` produzem, na prática, o **mesmo efeito visível** no log nativo.
  A chamada aparece como `BLOCKED_TYPE` com `BLOCK_REASON_CALL_SCREENING_SERVICE`.
- Continuar chamando `setSkipCallLog(true)` é correto (declara a intenção, é honrado se o produto
  um dia for pacote de operadora, e não custa nada), mas **a UI não pode oferecer isso como
  funcionalidade** e `docs/LIMITACOES.md` item 3 precisa ser reescrito: de "best-effort, varia por
  OEM" para "**não funciona para apps de terceiros — é AOSP, não OEM**".
- SCR-07 ("não aparece no histórico nativo por padrão") **não é atingível**. O planner deve
  registrar isso como limitação e ajustar o texto do requisito, não tentar contornar.
- O que **é** atingível e vale destacar na UI: a chamada não toca, não mostra tela e **não gera
  notificação de perdida** (`skipNotification` funciona). Entrada silenciosa no log é o preço.

### Descoberta 2 — Contatos AGORA chegam ao `onScreenCall` (HIGH, fonte AOSP)

`CallScreeningServiceFilter.startFilterLookup`:

```java
if (priorStageResult.contactExists && (!hasReadContactsPermission())) {
    // Binding to the call screening service will be skipped if it does NOT hold
    // READ_CONTACTS permission and the number is in the user's contacts
    return CompletableFuture.completedFuture(priorStageResult);
}
```

e o javadoc de `onScreenCall`:

> "only calls which are not in the user's contacts are passed for screening, **unless the
> `CallScreeningService` has been granted `READ_CONTACTS` permission by the user**."

A Phase 4 concedeu `READ_CONTACTS`. Portanto, no modo filtro:

| `READ_CONTACTS` | Chamada de contato chega ao `onScreenCall`? |
|-----------------|---------------------------------------------|
| Concedida | **SIM** — decidimos nós |
| Negada / revogada | NÃO — o Telecom nem faz o bind; o contato toca nativo |

**Consequências:**
- A decisão do CONTEXT de usar o `ContactLookupRepository` real deixa de ser defensiva e passa a
  ser **obrigatória e crítica**. Um `MISS` errado bloqueia a chamada de um contato — a pior falha
  possível do produto.
- O critério de sucesso 2 ("contato da agenda toca normalmente no modo filtro") deixa de ser
  garantido pela plataforma e passa a depender do nosso lookup + da política `contactsPolicy`
  (padrão `RING` → `Allow`). Precisa de teste dedicado.
- As **políticas por origem para contatos já valem parcialmente no modo filtro** — o que
  `docs/LIMITACOES.md` item 2 nega hoje. Corrigir: elas valem *quando `READ_CONTACTS` está
  concedida*; sem a permissão, o contato nunca chega.
- Cenário novo, silencioso e perigoso: o usuário **revoga** `READ_CONTACTS` depois. O
  comportamento muda de "nós decidimos" para "plataforma decide" sem nenhum sinal. O motor já
  cobre o lado seguro (`UNAVAILABLE` → `FallbackPolicy`), mas a UI da Phase 7 precisa saber.

### Descoberta 3 — O workaround do Robolectric registrado no STATE não funciona (MEDIDO)

`.planning/STATE.md` registra: *"Robolectric 4.16.1 suporta até SDK 36 — com compileSdk 37, fixar
`@Config(sdk = [36])` até o 4.17 estável"*. Medido neste repo:

```
java.lang.UnsupportedOperationException: Failed to create a Robolectric sandbox:
Android SDK 36 requires Java 21 (have Java 17)
```

O projeto está travado em **JDK 17** (`gradle.properties`, invariante de stack). Logo `sdk = [36]`
é inutilizável. **Medido funcionando: `sdk = [35]`, `[34]`, `[33]` e `[29]`** — todos verdes.
O planner deve usar `@Config(sdk = [35])` (e `[29]` para provar o piso do minSdk), e o STATE deve
ser corrigido.

### Descoberta 4 — Chamadas de saída se resolvem sozinhas (HIGH, fonte AOSP)

No `handleMessage` do próprio `CallScreeningService`:

```java
onScreenCall(callDetails);
if (callDetails.getCallDirection() == Call.Details.DIRECTION_OUTGOING) {
    mCallScreeningAdapter.onScreeningResponse(..., null);   // resposta nula automática
}
```

E `respondToCall` é documentado: *"Calls to this method are ignored unless the
`Call.Details#getCallDirection()` is `DIRECTION_INCOMING`."* Confirmado no adapter do Telecom:
`if (callResponse == null) { "Null responses are only supposed to happen for outgoing calls"; return; }`

**Consequência:** SCR-09 é satisfeito pela plataforma. O Service deve **retornar imediatamente**
para `DIRECTION_OUTGOING`, sem chamar `respondToCall` e sem gastar orçamento. O motor já devolve
`Allow(OUTGOING_CALL)`, mas o caminho barato é o early-return no Service — e o teste deve provar
que **zero** respostas são emitidas para saída.

---

## Semântica de `CallResponse` — API 29 a 35 (MEDIDO)

Executei a matriz de combinações sob Robolectric nos SDKs **29, 33, 34 e 35**. O resultado foi
**idêntico nos quatro** — não há deriva de versão neste contrato.

Validação real, do construtor privado (`CallScreeningService.java`, AOSP):

```java
if (!shouldDisallowCall
        && (shouldRejectCall || shouldSkipCallLog || shouldSkipNotification)) {
    throw new IllegalStateException("Invalid response state for allowed call.");
}
```

| Combinação | Resultado medido |
|------------|------------------|
| `rejectCall` sem `disallowCall` | `IllegalStateException` |
| `skipCallLog` sem `disallowCall` | `IllegalStateException` |
| `skipNotification` sem `disallowCall` | `IllegalStateException` |
| `silenceCall` + `rejectCall` | `IllegalStateException` |
| `silenceCall` + `skipNotification` | `IllegalStateException` |
| `silenceCall` sozinho | **legal** |
| `disallowCall` + `rejectCall` + `skipCallLog` + `skipNotification` | **legal** |
| `disallowCall` + `silenceCall` | **legal, mas não fazer** — ver abaixo |

`disallowCall + silenceCall` compila e não lança, mas o Telecom testa na ordem
`if (shouldDisallowCall) ... else if (shouldSilenceCall)`: **disallow vence e o silence é
ignorado**. Combinação enganosa — o plano deve proibi-la explicitamente.

### O que cada campo realmente faz (fonte: `CallScreeningServiceFilter`)

| Campo | Efeito real | Gate |
|-------|-------------|------|
| `setDisallowCall(true)` | `shouldAllowCall = false` — a chamada não toca nem aparece | nenhum |
| `setRejectCall(true)` | `shouldReject` — desconecta como se o usuário recusasse (vs. deixar tocar até a operadora desistir) | exige `disallowCall` |
| `setSilenceCall(true)` | `shouldSilence=true`, `shouldAllowCall=true`, **`shouldAddToCallLog=true`**, **`shouldShowNotification=true`** — forçados pelo framework | exige `disallowCall=false` |
| `setSkipCallLog(true)` | **NO-OP** para nós (Descoberta 1) | só `PACKAGE_TYPE_CARRIER` |
| `setSkipNotification(true)` | `shouldShowNotification = !skipNotification` — **funciona** | exige `disallowCall` |

Note que no ramo `silenceCall` o framework **força** `addToCallLog(true)` e
`showNotification(true)` — a política Silenciar sempre deixa rastro nativo. Isso é por desenho e
está alinhado com o CONTEXT.

### Tabela de tradução final recomendada

| `CallDecision` | `CallResponse` | Observação |
|----------------|----------------|------------|
| `Allow` | `Builder().build()` (tudo `false`) | não interferir |
| `Silence` | `setSilenceCall(true)` | e **nada mais** — qualquer outro campo lança |
| `Reject` | `disallow` + `reject` + `skipNotification` (+ `skipCallLog` conforme config, ciente do no-op) | |
| `SendSilentlyToVoicemail` | `disallow` + `reject` + `skipNotification` | **idêntica a `Reject`** na API — a diferença é da operadora |
| `BlockWithoutTrace` | `disallow` + `reject` + `skipCallLog(true)` + `skipNotification(true)` | `skipCallLog` sem efeito prático |

⚠️ **`Reject` e `SendSilentlyToVoicemail` produzem `CallResponse` iguais.** O plano deve manter as
duas decisões separadas no domínio (elas têm reason code e histórico distintos) mas **não** fingir
que a API as distingue. `docs/LIMITACOES.md` item 6 já está correto e deve ser reforçado.

---

## O limite de 5 s (HIGH, fonte AOSP)

**Constante oficial** (`packages/services/Telecomm/.../Timeouts.java`):

```java
public static long getCallScreeningTimeoutMillis(ContentResolver contentResolver) {
    return get(contentResolver, "call_screening_timeout", 5000L /* 5 seconds */);
}
```

É um `Settings` sobrescrevível por chave `call_screening_timeout` — **um OEM ou operadora pode
reduzir esse valor**. Mais uma razão para o timeout interno de 1 s do CONTEXT ser generoso.

**O que acontece ao estourar** (`IncomingCallFilterGraph.performFiltering`): um `postDelayed` com
esse timeout dispara, loga `FILTERING_TIMED_OUT`, combina o que terminou sobre o `DEFAULT_RESULT`
e faz `unbindCallScreeningService()`. O `DEFAULT_RESULT` é:

```java
new CallFilteringResult.Builder()
    .setShouldAllowCall(true)
    .setShouldReject(false)
    .setShouldAddToCallLog(true)
    .setShouldShowNotification(true)
    .setDndSuppressed(false).build();
```

**Fail-open confirmado:** perder o prazo = chamada permitida, logada e notificada. Alinhado com a
`FallbackPolicy.ALLOW` e com `docs/LIMITACOES.md` item 4.

**Detalhe crítico de orçamento:** o cronômetro dispara em `performFiltering()`, **antes** do bind.
Os 5 s incluem criação do processo, bind, e `onScreenCall`. Nosso orçamento de 200 ms é só da
decisão; o cold start do processo vive dentro do mesmo teto.

### `respondToCall` chamado duas vezes (MEDIDO)

`respondToCall` é `final` e não tem guarda alguma:

```java
public final void respondToCall(@NonNull Call.Details callDetails, @NonNull CallResponse response) {
    try {
        mCallScreeningAdapter.onScreeningResponse(...);
    } catch (RemoteException e) { Log.e(...); }
}
```

**Medido no harness:** duas chamadas seguidas produzem **duas** `ParcelableCallResponse` no
adapter, **sem exceção e sem crash**. Do lado do Telecom, a segunda cai num
`CompletableFuture.complete()` já completado → no-op silencioso; a primeira resposta vence.

Riscos reais que a guarda precisa cobrir:
1. **NPE:** `mCallScreeningAdapter` é campo de instância. Chamar `respondToCall` fora de um
   `onScreenCall` (ex.: watchdog atrasado após unbind) → `NullPointerException`. O `try/finally`
   permissivo do CONTEXT **precisa** estar dentro de `runCatching`.
2. **Campo compartilhado:** `mCallScreeningAdapter` é sobrescrito a cada `screenCall`. Duas
   chamadas simultâneas (dual SIM, chamada em espera) fazem a segunda sobrescrever o adapter da
   primeira. Por isso o `AtomicBoolean` deve ser **por chamada** (capturado no escopo do
   `onScreenCall`), nunca um campo do Service — o CONTEXT já acertou nisso.
3. `RemoteException` é engolida pelo framework: uma resposta pode ser silenciosamente perdida
   se o bind morreu. Não há como detectar; é fail-open.

---

## Ciclo de vida do `ROLE_CALL_SCREENING`

| Pergunta | Resposta | Confiança |
|----------|----------|-----------|
| Como pedir | `RoleManager.createRequestRoleIntent(ROLE_CALL_SCREENING)` num `ActivityResultLauncher` (o javadoc ainda mostra `startActivityForResult`, deprecado) | HIGH |
| Como detectar | `RoleManager.isRoleHeld(ROLE_CALL_SCREENING)` | HIGH |
| Disponibilidade | `isRoleAvailable(...)` — pode ser `false` em aparelhos sem telefonia | HIGH |
| Elegibilidade API 29+ | Declarar o `<service>` com `android:permission="android.permission.BIND_SCREENING_SERVICE"` e o `<intent-filter>` de `android.telecom.CallScreeningService`. **Ambos já estão no manifest desde a Phase 1** | HIGH |
| **Existe callback/observer?** | **NÃO.** Não há broadcast público nem listener de mudança de papel para apps de terceiros (`RoleManager.addOnRoleHoldersChangedListener` é `@SystemApi` e exige `MANAGE_ROLE_HOLDERS`, permissão de sistema — **proibida aqui**). Só **polling** | HIGH |
| Papel perdido | Outro app assumir o papel é silencioso: o Telecom simplesmente para de fazer o bind. Nenhum sinal chega ao app | HIGH |

**Implicação para SCR-02:** "verifica continuamente" só pode significar `isRoleHeld()` em cada
`onResume` da tela (Phase 7). Não gastar plano tentando achar um observer — ele não existe para
apps de terceiros. `ScreeningRoleManager` já expõe as três funções necessárias; esta fase provavelmente
não precisa mudá-lo, apenas exercitá-lo.

Um segundo pedido do papel quando ele já é detido geralmente retorna `RESULT_OK` sem diálogo —
não é caminho de erro.

---

## Testar o Service — a resposta concreta (MEDIDO)

Esta era a pergunta mais difícil da fase. **Resolvida: o Service real roda em JVM.**

### Harness medido funcionando

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])           // sdk 36 é IMPOSSÍVEL em JDK 17 — ver Descoberta 3
class ScreeningServiceTest {

    private val captured = mutableListOf<Any?>()

    private fun hostService(): UnknownCallScreeningService {
        val svc = Robolectric.buildService(UnknownCallScreeningService::class.java).create().get()
        val adapterCls = Class.forName("com.android.internal.telecom.ICallScreeningAdapter")
        val adapter = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(adapterCls)) { _, m, args ->
            if (m.name == "onScreeningResponse") captured.add(args?.getOrNull(2))
            null
        }
        CallScreeningService::class.java
            .getDeclaredField("mCallScreeningAdapter")
            .apply { isAccessible = true }
            .set(svc, adapter)
        return svc
    }

    @Test fun respondeExatamenteUmaVez() {
        val details = mockk<Call.Details>(relaxed = true)
        every { details.callDirection } returns Call.Details.DIRECTION_INCOMING
        every { details.handle } returns Uri.parse("tel:+5511999998888")
        hostService().onScreenCall(details)
        assertEquals(1, captured.size)
    }
}
```

Medições que sustentam cada peça:

| Peça | Medido |
|------|--------|
| `Robolectric.buildService(CallScreeningService)` | ✅ funciona (sem ele: `NullPointerException: ... "this.mBase" is null`) |
| `mockk<Call.Details>(relaxed = true)` com `callDirection`/`handle` | ✅ funciona em JVM, sem Robolectric |
| `Proxy` de `com.android.internal.telecom.ICallScreeningAdapter` | ✅ a classe existe no android-all do Robolectric |
| Campo privado `mCallScreeningAdapter` | ✅ presente e acessível (`declaredFields` = `__robo_data__, SERVICE_INTERFACE, MSG_SCREEN_CALL, mHandler, mCallScreeningAdapter`) |
| Captura das respostas | ✅ `responses=2 types=[ParcelableCallResponse, ParcelableCallResponse]` na prova de dupla resposta |

`ParcelableCallResponse` tem getters públicos (`shouldDisallowCall()`, `shouldRejectCall()`,
`shouldSilenceCall()`, `shouldSkipCallLog()`, `shouldSkipNotification()`) — a **tabela de tradução
inteira é assertável** a partir dos objetos capturados.

**Isto cobre, em JVM e em segundos, os critérios 3 e 4 e boa parte de SCR-10:** resposta única,
tradução correta, ordem das operações (comparar timestamps/ordem de um `MutableList` de eventos) e
a matriz de injeção de exceção em cada ponto.

### A ressalva honesta

O harness usa **reflexão sobre um campo privado e uma interface `com.android.internal`**. É
test-only, mas pode quebrar numa atualização do Robolectric. Por isso a recomendação é
**arquitetural, não só de teste**:

> Extraia a lógica para um `ScreeningCoordinator` puro que recebe uma costura
> `respond: (CallResponse) -> Unit`. O `UnknownCallScreeningService` vira ~10 linhas de delegação.

Com a costura, 90% dos testes não precisam do harness — chamam o coordinator direto com um lambda
que grava num `MutableList`. O harness fica reservado para **um punhado** de testes que provam
que o Service real está ligado ao coordinator e que `respondToCall` de verdade é atingido. Se o
Robolectric quebrar, perdemos poucos testes, não a suíte.

Sugestão de fatiamento (dentro da discricionariedade do executor):

| Classe | Responsabilidade | Testabilidade |
|--------|------------------|---------------|
| `ScreenedCallFactory` | `Call.Details` → `ScreenedCall` (direção, handle nulo, scheme, normalização) | JVM + MockK, sem Robolectric |
| `ScreeningCoordinator` | orquestra lookups com `withTimeout`, chama o motor, garante 1× resposta, dispara pós-resposta | JVM puro, **sem android.*** |
| `CallResponseFactory` | `CallDecision` → `CallResponse` | Robolectric `sdk=[29,33,35]` (o construtor real valida) |
| `UnknownCallScreeningService` | delega; nada mais | harness Robolectric, poucos testes |

`ScreeningCoordinator` sem `android.*` também entra naturalmente no Kover (o gate de 80% não
sofre) — ao contrário do Service, que provavelmente precisará de `excludes` por **nome de classe**
(regra da Phase 3/4: nunca por pacote).

### `ServiceTestRule` — o que ele resolve e o que não resolve

`androidx.test.rule.ServiceTestRule` (já em `androidTestImplementation`) faz `bindService` e
devolve o `IBinder`. Serve para provar QLT-06 ("bind do `CallScreeningService`"): que o serviço
está declarado, exported e retorna binder não-nulo. **Não** serve para exercitar `onScreenCall`:
o `IBinder` é a `CallScreeningBinder` interna, e `screenCall(adapter, ParcelableCall)` exige um
`ParcelableCall`, que é `@hide` e impossível de montar a partir de androidTest. Use `ServiceTestRule`
para o smoke de bind e o harness Robolectric para o comportamento.

---

## Orçamento de cold start — MEDIDO no emulador

Medido em `Medium_Phone_API_35`, primeiro toque de cada `by lazy` do `AppContainer`:

| Componente | Primeiro toque | Toque seguinte |
|------------|---------------:|---------------:|
| `AppContainer` (referência do app) | 0,001 ms | — |
| `decisionEngine` | 0,65 ms | — |
| `phoneNumberNormalizer` (constrói `PhoneNumberUtil`) | **7,8 ms** (19,0 ms em container frio) | — |
| `normalize(...)` | 1,2 ms (3,5 ms frio) | 0,25–0,47 ms |
| `settingsRepository` / `snapshot()` | ~0 ms (já aquecido) / 14,4 ms se frio | 0,002 ms |
| `whitelistRepository.contains(...)` | **10,4 ms** (abre o SQLite) | 4,99 ms |
| `contactLookupRepository` | 2,6 ms | — |
| `blockedCallRepository` | 0,001 ms | — |
| **Caminho de decisão completo, tudo quente** | **23,3 ms** | — |

**Veredito:** mesmo somando os piores números frios observados (19,0 + 3,5 + 14,4 + 10,4 + 2,6
≈ **50 ms**), sobra ~4× de margem sobre os 200 ms. **O `AppContainer` não é o gargalo que se temia.**
O `by lazy` do bootstrap fez o trabalho.

Ressalvas honestas:
- São amostras únicas sob carga de instrumentação, não percentis. `contains` deu 4,99 ms aqui
  contra o p50 de 0,19–0,23 ms medido na Phase 3 — a variância do emulador é a de sempre. **O
  veredito da cauda é da Phase 9**, e o assert do CI vai na mediana (decisão do CONTEXT).
- Nada aqui mede a criação do processo nem o bind, que ocorrem antes e também consomem os 5 s.
- O `ContactKeyCache` custa 2,57 s para construir (Phase 4) e **nunca** é aguardado. Continua
  válido nesta fase: o cache frio responde pela sonda direta.

### Defeito encontrado ao medir: `onAppOpened()` roda a cada início de processo

```kotlin
override fun onCreate() {
    super.onCreate()
    container.onAppOpened()      // incrementa contador + poda histórico
}
```

`Application.onCreate` roda em **todo** início de processo — inclusive quando o processo é criado
pelo bind do Telecom para uma chamada recebida. Consequências:

1. **ENG-01 fica errado:** `incrementAppOpenCount()` conta chamadas recebidas como "aberturas do
   app". O convite de avaliação da 5ª abertura (Phase 9) dispararia por telefonemas, não por uso.
2. Uma poda do histórico é disparada em `Dispatchers.IO` concorrendo com o caminho da decisão.
   Não bloqueia a main thread, mas compete por I/O no momento mais sensível do produto.

Isso não é escopo declarado da Phase 5, mas a fase **é** quem introduz o start-por-Telecom de
verdade. Recomendação: mover `onAppOpened()` para o `onCreate` da `MainActivity` (ou uma chamada
explícita da UI), não da `Application`. Se o planner considerar fora de escopo, deve virar item em
`docs/backlog/` conforme a regra de escopo do `CLAUDE.md` — mas o ideal é resolver aqui, porque a
correção é de duas linhas e o defeito só existe por causa desta fase.

**Confirmação empírica do invariante de instância única:** construir um segundo `AppContainer` no
mesmo processo derruba o teste com
`IllegalStateException: There are multiple DataStores active for the same file: .../sentinela_settings.preferences_pb`.
O Service **deve** obter o container por `(application as SentinelaApp).container`, jamais
construir o seu.

### Consequência de arquitetura: `onScreenCall` roda na MAIN THREAD

```java
private final Handler mHandler = new Handler(Looper.getMainLooper()) { ... onScreenCall(callDetails); ... }
```

E `settingsRepository.snapshot()`, `whitelistRepository.contains()` e
`contactLookupRepository.lookup()` são **todos `suspend`** (verificado no código; o CONTEXT
descreve `containsBlocking` e um `snapshot()` não-suspend — **não existem**, o planner não deve
planejar contra essa API).

Portanto: **não** usar `runBlocking` na main thread. O padrão correto é lançar em
`Dispatchers.Default`/`IO` com `withTimeout(1_000)` e responder de dentro da corrotina —
`respondToCall` é só uma chamada Binder e pode vir de qualquer thread. O Service permanece vivo até
o unbind, então isso é seguro. A guarda `AtomicBoolean` passa a ser ainda mais essencial, porque
agora existe uma corrida real entre a corrotina e o watchdog de timeout.

---

## Não Perturbe (DND) — a resposta honesta é NÃO

`packages/services/Telecomm/.../DndCallFilter.java`:

```java
boolean shouldSuppress = !mRinger.shouldRingForContact(mCall);
resultFuture.complete(new CallFilteringResult.Builder()
    .setShouldAllowCall(true).setShouldAddToCallLog(true)
    .setShouldShowNotification(true).setDndSuppressed(shouldSuppress).build());
```

O DND é avaliado por um filtro **separado e paralelo** ao nosso, que consulta o
`NotificationManager` (`matchesCallFilter`). **Nenhum campo de `CallResponse` influencia
`dndSuppressed`.** Um `CallScreeningService` pode fazer uma chamada tocar menos (silenciar,
bloquear); **não pode fazê-la tocar mais**.

Os únicos mecanismos de bypass de DND no Android são:
- `NotificationManager.setNotificationPolicy(...)` / `setInterruptionFilter(...)` — exige
  **`ACCESS_NOTIFICATION_POLICY`**, que **não está na allowlist** de `docs/PERMISSOES.md` nem em
  `scripts/verify-invariants.sh`. **Proibida — não propor.** Além disso alteraria a política
  *global* do usuário, não uma chamada específica: seria um dark pattern.
- O usuário marcar o contato como favorito/estrelado e configurar o DND para permitir favoritos —
  **ação do usuário no sistema**, fora do app.

**Conclusão para o plano:**
- `OriginPolicy.NEVER_SILENCE` já decide como `RING`/`Allow` no motor, e isso está **correto**.
  Não existe implementação adicional a fazer na camada telecom — a Phase 5 não deve gastar nenhuma
  task tentando.
- `docs/LIMITACOES.md` item 7 diz que "a semântica exata é confirmada na pesquisa da Fase 6".
  **Está confirmada agora e a resposta é negativa.** Reescrever: o app **não consegue** furar o Não
  Perturbe; o nome "Nunca Silenciar" significa "o Sentinela nunca silencia esta origem", não "toca
  mesmo no Não Perturbe". O texto da UI (Phases 7/8) precisa dessa formulação, e o rótulo atual é
  ambíguo o bastante para merecer revisão.
- Cenário de validação física: confirmar em Samsung que uma origem `NEVER_SILENCE` continua
  suprimida pelo DND — para documentar, não para consertar.

---

## Notificação própria

### Canal `IMPORTANCE_LOW`

| Propriedade | Comportamento | Confiança |
|-------------|---------------|-----------|
| `IMPORTANCE_LOW` | aparece na sombra e na status bar; **sem som, sem vibração, sem heads-up** | HIGH |
| Imutabilidade | após `createNotificationChannel`, importância/som/vibração **não podem** ser alterados por código — só pelo usuário. Acertar de primeira ou versionar o `channelId` | HIGH |
| `ensureChannel()` | idempotente; chamar antes de qualquer `notify`. Fazer no ponto de opt-in, **não** em `Application.onCreate` (cold start) | HIGH |
| Reforços coerentes com NTF-03 | `setSilent(true)`, `setVibrate(null)`, `setSound(null)`, **nunca** `setFullScreenIntent` | HIGH |

### `POST_NOTIFICATIONS` em runtime (API 33+)

- `minSdk 29` → o pedido **só existe** em API ≥ 33. Em API 29–32 a notificação funciona sem pedido.
  O código precisa do branch de versão; o teste precisa cobrir os dois.
- `ActivityCompat.requestPermissions` / `ActivityResultContracts.RequestPermission` — mas a camada
  que toca `ActivityCompat` **vive em `platform/`** (decisão da Phase 4, para não gerar
  falso-vermelho no Kover). Reaproveitar a máquina de estado de permissão da Phase 4 em vez de
  criar uma nova.
- Gravar a flag "já perguntei" **ao disparar o launcher**, nunca no callback (lição da Phase 4: o
  usuário pode matar o app com o diálogo aberto).
- Se a permissão for negada, a opção de notificação deve **voltar a desligada** — não deixar um
  toggle ligado que não produz nada.
- Negação dupla em API 33+ marca `USER_FIXED`: o app não pode mais perguntar, só oferecer atalho
  para as Configurações.

### Tela bloqueada — o ponto que faz NTF-04 valer de verdade

Três níveis independentes:

| Nível | Quem controla |
|-------|---------------|
| `Notification.visibility` (`PUBLIC`/`PRIVATE`/`SECRET`) | o app |
| `Notification.setPublicVersion(...)` | o app — a versão exibida quando o sistema esconde conteúdo sensível |
| Configuração do usuário para a tela bloqueada | o usuário |

`VISIBILITY_PRIVATE` **não é garantia**: se o usuário configurou "mostrar todo o conteúdo", o texto
completo aparece na tela bloqueada. A garantia real de NTF-04 é **nunca colocar o número completo
no objeto `Notification`**, em nenhum campo, em nenhuma versão. Como o conteúdo já é
`PhoneMask.mask(...)` ou "sem identificação", NTF-04 se sustenta pelo conteúdo, não pela
`visibility`. Recomendação: usar `VISIBILITY_PRIVATE` + `setPublicVersion` sem identificação alguma
como camada extra, e **provar por teste que nenhuma string do `Notification` contém a sequência
completa de dígitos** — esse é o invariante que vale, e ele é assertável.

### NTF-05 — tocar na notificação abre o registro interno

`PendingIntent` para a `MainActivity` com um extra de id do `BlockedCallEntry`. **Obrigatório
`FLAG_IMMUTABLE`** (exigido a partir da API 31; o app tem `targetSdk` bem acima disso). A tela de
histórico é da Phase 8 — nesta fase basta o deep link carregar o id e a Phase 8 consumi-lo.

---

## Don't Hand-Roll

| Problema | Não construir | Usar | Por quê |
|----------|---------------|------|---------|
| Timeout das consultas | `Handler.postDelayed` + flags manuais | `withTimeout` / `withTimeoutOrNull` das coroutines | cancelamento cooperativo correto, já no projeto |
| Resposta única | `synchronized` + `Boolean` | `AtomicBoolean.compareAndSet` | CAS é o primitivo certo e testável |
| Mascarar número | qualquer `substring`/regex nova | `PhoneMask.mask` | função **única** do projeto (Phase 2) |
| Normalizar handle | `Uri.getSchemeSpecificPart` + limpeza manual | `LibPhoneNumberNormalizer` | cascata de região e 9º dígito BR já resolvidos |
| Estado de permissão | nova máquina de estado | a da Phase 4, em `platform/` | evita falso-vermelho no Kover e duplicar a lição do `USER_FIXED` |
| Detectar perda do papel | broadcast/listener | polling `isRoleHeld()` em `onResume` | o listener é `@SystemApi` — não existe para nós |
| Agendar pós-resposta | `WorkManager`, novo `CoroutineScope` | `appScope` do `AppContainer` | `WorkManager` é proibido; um escopo novo vaza |

**Key insight:** a Phase 5 é quase toda **fiação**. Todo componente que ela consome já existe e já
foi medido nas Phases 2–4. O código novo deve ser a costura, a guarda de resposta única e a
tradução — nada mais. Qualquer regra de decisão nova é sinal de que algo foi para o lugar errado.

---

## Common Pitfalls

### Pitfall 1 — Construir um segundo `AppContainer`
**O que dá errado:** `IllegalStateException: There are multiple DataStores active for the same file`.
**Reproduzido nesta pesquisa.** O Service deve usar `(application as SentinelaApp).container`.
**Sinal:** crash no primeiro `snapshot()`, não na construção.

### Pitfall 2 — `runBlocking` na main thread
`onScreenCall` roda na main thread e os três lookups são `suspend`. `runBlocking(1s)` congela a UI
e o próprio `Looper` que entregaria outros eventos. Use corrotina + `withTimeout` e responda de lá.

### Pitfall 3 — Achar que dá para bloquear número privado
`PRESENTATION_RESTRICTED`, `PRESENTATION_UNKNOWN`, `PRESENTATION_UNAVAILABLE` e
`PRESENTATION_PAYPHONE` **não são entregues** ao `CallScreeningService` (javadoc de `onScreenCall`).
Além disso `getHandlePresentation()` nem sequer está entre os campos preenchidos. **SCR-04 não é
atingível no modo filtro.** `ScreenedNumber.Private` só ocorreria por `handle == null`, um caso que
a plataforma diz não entregar. Manter o ramo no motor (custa zero, e o modo discador da Phase 6 pode
mudar o quadro), mas **não** prometer na UI e registrar em `docs/LIMITACOES.md`.

### Pitfall 4 — Confiar em campos não preenchidos de `Call.Details`
Só estes são garantidos: `getCallDirection()`, `getCallerNumberVerificationStatus()`,
`getConnectTimeMillis()`, `getCreationTimeMillis()`, `getHandle()`. Todo o resto vem em default/null.
Ler qualquer outro é bug latente. Só chegam handles com scheme `tel:`.

### Pitfall 5 — `disallowCall` + `silenceCall`
Legal no construtor (medido), mas o Telecom testa `disallow` primeiro e ignora o `silence`.
Combinação enganosa: proibir explicitamente.

### Pitfall 6 — Montar `CallResponse` em teste JVM sem Robolectric
`testOptions.unitTests.isReturnDefaultValues = true` faz cada método do `Builder` devolver `null`
→ NPE no encadeamento. A validação real do construtor **só** aparece sob Robolectric (ou
instrumentado). Todo teste da tradução precisa de `@RunWith(RobolectricTestRunner::class)`.

### Pitfall 7 — `@Config(sdk = [36])`
**Medido:** `Failed to create a Robolectric sandbox: Android SDK 36 requires Java 21 (have Java 17)`.
Usar `[35]` (e `[29]` para o piso). O STATE precisa ser corrigido.

### Pitfall 8 — Notificação/histórico antes da resposta
Qualquer `notify` ou gravação antes de `respondToCall` gasta orçamento e pode lançar, virando uma
chamada não respondida (fail-open → o desconhecido toca). A ordem é assertável no harness pela
ordem dos eventos capturados — e é exatamente o tipo de regressão silenciosa que o CONTEXT teme.

### Pitfall 9 — Auto-sabotagem de invariante (recorrente nas Phases 3 e 4)
`scripts/verify-invariants.sh` casa literais em `app/src/main/java`. Um KDoc que escreva
`INTERNET`, o nome de uma permissão proibida ou o identificador vigiado derruba o bloco sem defeito
real. **Descrever proibições em prosa portuguesa, nunca com o identificador.**

### Pitfall 10 — Kover
Classes que só rodam instrumentadas/Robolectric devem ser excluídas **por nome de classe**, nunca
por pacote, e só no **último plano da fase** (regra das Phases 3 e 4). Se `ScreeningCoordinator`
ficar puro (sem `android.*`), o gate de 80% não sofre e talvez nenhum exclude novo seja necessário.

---

## State of the Art

| Premissa antiga do projeto | Realidade verificada | Impacto |
|----------------------------|----------------------|---------|
| "modo filtro só recebe não-contatos" (STATE, bootstrap; `LIMITACOES` item 2; KDoc de `ScreenedCall`) | Falso desde a Phase 4: com `READ_CONTACTS` o Service recebe **tudo** | O lookup de contatos vira crítico; critério 2 é nosso |
| "`setSkipCallLog` é best-effort, varia por OEM" (`LIMITACOES` item 3) | É **no-op** para apps de terceiros — decisão do AOSP, não do OEM | SCR-07 inatingível; reescrever doc e UI |
| "Nunca Silenciar depende de mecanismos que variam por versão; confirmar na Fase 6" (`LIMITACOES` item 7) | Confirmado **agora**: nenhum app de screening fura o DND | Nada a implementar; reescrever o rótulo e a doc |
| "Robolectric: fixar `@Config(sdk = [36])`" (STATE, blockers) | Impossível com JDK 17 | Usar `[35]` |
| CONTEXT: `snapshot()` não-suspend, `containsBlocking` | Ambos são `suspend`; `containsBlocking` não existe | Arquitetura assíncrona obrigatória |
| "Service pode ser testado só com chamada real" | Falso — harness Robolectric medido funcionando | A fase é verificável em CI |

---

## Open Questions

1. **Quando o guard de `READ_CONTACTS` entrou no AOSP?**
   - Sabemos: está no `main` atual e no SDK 35 local.
   - Não confirmado: se vale já na API 29. A tentativa de ler o histórico do Gitiles não retornou.
   - Recomendação: tratar como comportamento **possível em qualquer versão suportada** e escrever o
     código correto para os dois casos (é o que a decisão do CONTEXT já faz). Nenhum caminho novo.

2. **A Samsung altera `shouldAddToCallLog` na One UI?**
   - Sabemos: no AOSP, sempre `true` para nós.
   - Não sabemos: se a One UI mostra o `BLOCKED_TYPE` numa aba separada ("Bloqueadas") ou misturado
     ao histórico, o que muda muito a percepção do usuário.
   - Recomendação: cenário 41 da Phase 9. **Não** implementar nada preventivo.

3. **Chamada em espera / dual SIM com duas triagens simultâneas.**
   - Risco real: `mCallScreeningAdapter` é um campo único e é sobrescrito.
   - Não é reproduzível no emulador nem no harness com fidelidade.
   - Recomendação: guarda `AtomicBoolean` por chamada (já decidido) + cenário físico.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (JVM) | JUnit 4 `4.13.2` + MockK + `kotlinx-coroutines-test` — AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| Framework (JVM + framework real) | **Robolectric `4.16.1` com `@Config(sdk = [35])`** — obrigatório para `CallResponse`; `[36]` é impossível (medido) |
| Framework (instrumentado) | `AndroidJUnitRunner` + `androidx.test:rules` (`ServiceTestRule`) |
| Config file | `app/build.gradle.kts` (`testOptions.unitTests.isReturnDefaultValues = true` — ver Pitfall 6) |
| Quick run command | `./gradlew testDebugUnitTest` |
| Instrumented command | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` (AVD `Medium_Phone_API_35`; `connectedDebugAndroidTest` **não** aceita `--tests`) |
| Full suite command | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| Cobertura | Kover `0.9.9`, gate `koverVerify minBound(80)` (atual 96,68%). Excludes novos **por nome de classe**, só no último plano |
| Evidência de percentis | logcat por teste em `app/build/outputs/androidTest-results/connected/debug/Medium_Phone_API_35(AVD) - 15/logcat-<classe>-<metodo>.txt` |
| Dependências novas | **nenhuma** — Robolectric, MockK, `ServiceTestRule` e coroutines-test já estão declarados |

### Phase Requirements → Test Map

| Req | Comportamento | Tipo | Comando | Existe? |
|-----|---------------|------|---------|---------|
| SCR-05 | `respondToCall` exatamente 1× no caminho feliz | Robolectric (harness) | `./gradlew testDebugUnitTest --tests "*ScreeningServiceTest"` | ❌ Wave 0 |
| SCR-05/SCR-10 | 1× com exceção injetada em **cada** ponto (fábrica, settings, contatos, whitelist, motor, tradução, notificação, histórico) | JVM (coordinator + costura) | `--tests "*ScreeningCoordinatorFailureTest"` | ❌ Wave 0 |
| SCR-05/SCR-10 | 1× quando o timeout interno de 1 s estoura → `UNAVAILABLE` → `FallbackPolicy` | JVM (`runTest` + `TestDispatcher`) | `--tests "*ScreeningCoordinatorTimeoutTest"` | ❌ Wave 0 |
| SCR-10 | Exceção inesperada → **PERMITIR** (resposta vazia) | JVM | `--tests "*ScreeningCoordinatorFailureTest"` | ❌ Wave 0 |
| SCR-03/06/07/08 | Tabela de tradução completa, 5 decisões × campos | Robolectric `sdk=[29,35]` | `--tests "*CallResponseFactoryTest"` | ❌ Wave 0 |
| — | Combinações contraditórias lançam `IllegalStateException` (5 casos medidos) | Robolectric | `--tests "*CallResponseFactoryTest"` | ❌ Wave 0 |
| SCR-09 | Saída → **zero** respostas emitidas | Robolectric (harness) | `--tests "*ScreeningServiceTest"` | ❌ Wave 0 |
| SCR-04 | Handle nulo / número inválido não derrubam o Service | JVM | `--tests "*ScreenedCallFactoryTest"` | ❌ Wave 0 |
| DEC-01 | Nenhuma condição de bloqueio fora do motor | script | `bash scripts/verify-invariants.sh` (bloco novo) | ✅ estender |
| NTF-06 | `respondToCall` **antes** de notificar/gravar (ordem, não tempo) | JVM (lista de eventos ordenada) | `--tests "*ScreeningCoordinatorOrderTest"` | ❌ Wave 0 |
| NTF-06 | Falha ao gravar histórico **não** afeta a resposta | JVM | `--tests "*ScreeningCoordinatorFailureTest"` | ❌ Wave 0 |
| NTF-01/02 | Notificação off por padrão; permissão pedida só no opt-in | JVM | `--tests "*NotificationPermissionStateTest"` | ❌ Wave 0 |
| NTF-03 | Canal com `IMPORTANCE_LOW`, sem som/vibração/full-screen | Robolectric (`ShadowNotificationManager`) | `--tests "*BlockedCallNotifierTest"` | ❌ Wave 0 |
| NTF-04 | **Nenhum campo do `Notification` contém o número completo** | Robolectric | `--tests "*BlockedCallNotifierTest"` | ❌ Wave 0 |
| NTF-05 | `PendingIntent` `FLAG_IMMUTABLE` com o id do registro | Robolectric | `--tests "*BlockedCallNotifierTest"` | ❌ Wave 0 |
| SCR-01/02 | `isRoleHeld` / `buildRequestIntent` | Robolectric (`ShadowRoleManager`) | `--tests "*ScreeningRoleManagerTest"` | ❌ Wave 0 |
| SCR-11 | Bench da decisão, **assert na mediana**, cauda só reportada | instrumentado | `bash scripts/run-instrumented-tests.sh --tests "*DecisionPerformanceTest"` | ❌ Wave 0 |
| QLT-06 | Bind real do Service (binder não-nulo) | instrumentado (`ServiceTestRule`) | `bash scripts/run-instrumented-tests.sh --tests "*ScreeningServiceBindTest"` | ❌ Wave 0 |
| — | Container é o do app (nunca um segundo) | instrumentado | `--tests "*ScreeningServiceBindTest"` | ❌ Wave 0 |

**Manual-only (Phase 9, `docs/TESTE-FISICO-SAMSUNG.md`, a partir do cenário 40):**
critérios 1, 2 e 6 (bloqueio real, contato tocando, caixa postal), comportamento do
`skipCallLog`/`BLOCKED_TYPE` na One UI, interação com o Não Perturbe, e o p95 < 200 ms em hardware.
Justificativa: o emulador não tem rádio nem operadora, e o comportamento é de OEM.

### Sampling Rate

- **Após cada commit de task:** `./gradlew testDebugUnitTest` (< 30 s — inclui os testes
  Robolectric, medidos em ~2 s por classe).
- **Após cada wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
  `koverVerify` só depois que o exclude do último plano entrar; até lá, `./gradlew koverLog`.
- **Phase gate:** suíte JVM **e** instrumentada verdes pós-`clean` com `--no-build-cache`,
  `N actionable tasks: M executed` com **M > 0**. Arquivado em `05-EVIDENCE.md`.
- **Nenhum watch mode.** Emulador sobe uma vez por sessão.
- **Prova de vermelho obrigatória:** cada guarda-corpo (resposta única, ordem, timeout, ausência do
  número completo) precisa ser quebrado de propósito, visto falhar, e restaurado.

### Wave 0 Gaps

- [ ] `ScreeningCoordinator` + costura `(CallResponse) -> Unit` — pré-requisito de quase toda a suíte
- [ ] Harness Robolectric do Service (`buildService` + `Proxy` de `ICallScreeningAdapter` +
      reflexão em `mCallScreeningAdapter`) — **receita já medida funcionando nesta pesquisa**
- [ ] `FakeCallDetails` / builder MockK de `Call.Details` (direção, handle, handle nulo)
- [ ] `app/src/test/.../telecom/CallResponseFactoryTest.kt` — SCR-03/06/07/08
- [ ] `app/src/test/.../telecom/ScreeningCoordinator*Test.kt` — SCR-05, SCR-10, NTF-06
- [ ] `app/src/test/.../notifications/BlockedCallNotifierTest.kt` — NTF-03/04/05
- [ ] `app/src/androidTest/.../telecom/ScreeningServiceBindTest.kt` — QLT-06
- [ ] `app/src/androidTest/.../DecisionPerformanceTest.kt` — SCR-11 (mediana no assert)
- [ ] Bloco novo em `scripts/verify-invariants.sh` — DEC-01, descrito **em prosa** (Pitfall 9)
- [ ] Instalação de framework: **nenhuma**

### Correções de documentação que esta fase deve carregar

- `docs/LIMITACOES.md` itens 2, 3 e 7 — os três estão factualmente errados (Descobertas 1 e 2, §DND).
- `.planning/STATE.md` — blocker do Robolectric (`[36]` → `[35]`) e a decisão de bootstrap sobre
  "onScreenCall só recebe não-contatos".
- KDoc de `ScreenedCall.ContactLookup` e de `UnknownCallScreeningService` — repetem a premissa errada.
- `.planning/REQUIREMENTS.md` SCR-04 e SCR-07 — inatingíveis como redigidos.
- `docs/TESTE-FISICO-SAMSUNG.md` — cenários novos a partir do **40** (existem até o 39).

### Cenários propostos para a Phase 9 (a acrescentar em `docs/TESTE-FISICO-SAMSUNG.md`)

| # | Cenário | Ação | Esperado |
|---|---------|------|----------|
| 40 | Bloqueio real de desconhecido | Ligar de um número fora da agenda e fora da whitelist | Não toca, não vibra, não mostra tela de chamada, **nenhuma** notificação nativa de perdida |
| 41 | `BLOCKED_TYPE` na One UI | Após o cenário 40, abrir o histórico nativo do telefone Samsung | Registrar **onde** a chamada aparece (aba "Bloqueadas"? misturada?). Confirma o no-op do `skipCallLog`; é registro, não falha |
| 42 | Contato toca no modo filtro | Com `READ_CONTACTS` concedida, ligar de um contato da agenda | Toca normalmente. **Este cenário agora exercita o nosso lookup**, não a plataforma (Descoberta 2) |
| 43 | `READ_CONTACTS` revogada | Revogar a permissão e repetir o cenário 42 | Toca normalmente (o Telecom nem faz o bind). Registrar se o app detecta e avisa |
| 44 | Caixa postal | Política Bloquear = "encaminhar silenciosamente"; ligar de desconhecido | Registrar o que o **chamador** ouve: caixa postal ou tom de não atendida. Depende da operadora |
| 45 | Não Perturbe ativo | Com DND ligado, ligar de origem `NEVER_SILENCE` (whitelist) | **Esperado: continua suprimida pelo DND.** Confirma a limitação; não é bug |
| 46 | Silenciar | Política Silenciar para desconhecidos; ligar | Tela de chamada aparece, **sem som e sem vibração**; entra no log nativo |
| 47 | p95 da decisão em hardware | `tests_regex=DecisionPerformanceTest` com o Galaxy conectado; ler `SENTINELA\|decision\|` no logcat | **p95 < 200 ms**. Registrar p50/p95/max. Só aqui a cauda tem veredito |
| 48 | Cold start por chamada | Forçar parada do app (`adb shell am force-stop`) e ligar em seguida | A chamada é bloqueada mesmo com o processo morto — o start pelo bind cabe nos 5 s |
| 49 | Perda do papel | Instalar outro app de bloqueio e conceder o papel a ele; reabrir o Sentinela | A home detecta a perda e oferece correção (SCR-02) |
| 50 | Notificação na tela bloqueada | Habilitar a notificação, bloquear uma chamada com a tela travada | Silenciosa, sem heads-up, e **sem o número completo** em nenhuma configuração de privacidade da tela bloqueada |
| 51 | Dual SIM / chamada em espera | Durante uma chamada ativa, receber uma de desconhecido | Bloqueada corretamente, sem afetar a chamada em curso |

---

## Sources

### Primary (HIGH — fonte lida diretamente)
- `~/Library/Android/sdk/sources/android-35/android/telecom/CallScreeningService.java` — SDK local
- `platform/frameworks/base` `main` — `telecomm/java/android/telecom/CallScreeningService.java`
  (idêntico ao local; confirma javadoc de `setSkipCallLog` e de `onScreenCall`)
- `platform/packages/services/Telecomm` `main`:
  - `.../callfiltering/CallScreeningServiceFilter.java` — gate de `skipCallLog`, gate de
    `READ_CONTACTS`, ramos `allowCall`/`disallowCall`/`silenceCall`
  - `.../callfiltering/IncomingCallFilterGraph.java` — `DEFAULT_RESULT` e o `postDelayed` do timeout
  - `.../callfiltering/DndCallFilter.java` — DND avaliado fora do nosso controle
  - `.../Timeouts.java` — `call_screening_timeout`, default `5000L`

### Medições executadas neste repositório (HIGH)
- Matriz de validação de `CallResponse` sob Robolectric, SDKs **29 / 33 / 34 / 35** — idêntica
- `@Config(sdk = [36])` → `Android SDK 36 requires Java 21 (have Java 17)`
- `Robolectric.buildService` + `Proxy` de `ICallScreeningAdapter` → `responses=2` para duas
  chamadas de `respondToCall`, sem exceção
- `mockk<Call.Details>(relaxed = true)` com `callDirection` e `handle` → funciona em JVM
- Cold start do `AppContainer` no AVD `Medium_Phone_API_35` — tabela de §Orçamento
- Segundo `AppContainer` → `IllegalStateException: There are multiple DataStores active`

### Secondary (MEDIUM)
- Documentação oficial "Screen calls" e a referência de `CallScreeningService` no
  developer.android.com — concordam com a fonte, sem detalhar os gates de pacote

---

## Metadata

**Confidence breakdown:**
- Semântica de `CallResponse`: **HIGH** — fonte AOSP + matriz medida em 4 níveis de API
- `setSkipCallLog` no-op: **HIGH** — código do Telecom + javadoc concordam
- Contatos chegam ao `onScreenCall`: **HIGH** — código do Telecom + javadoc concordam
- Timeout e fail-open: **HIGH** — constante e `DEFAULT_RESULT` lidos na fonte
- DND não é contornável: **HIGH** — filtro separado, e o bypass exige permissão proibida
- Estratégia de teste: **HIGH** — harness executado com sucesso, não teorizado
- Orçamento de cold start: **MEDIUM** — medido, mas amostra única em emulador; cauda é da Phase 9
- Comportamento Samsung: **LOW** — nada verificável fora do aparelho; virou roteiro, não código

**Research date:** 2026-07-29
**Valid until:** 2026-08-28 (30 dias — APIs de Telecom são estáveis; reavaliar se o compileSdk
ou a versão do Robolectric mudar)

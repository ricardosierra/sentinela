# Phase 6: Modo Discador Opcional - Research (metade TÉCNICA)

**Researched:** 2026-07-29
**Domain:** `RoleManager.ROLE_DIALER`, `android.telecom.InCallService`, `android.telecom.Call`,
notificação de chamada em tela cheia, degradação e reversão
**Confidence:** HIGH — fonte AOSP local (`android-35`) lida diretamente + **14 medições executadas**
em emulador API 35 e em JVM/Robolectric neste repositório

> **Escopo deste documento:** Telecom, papéis, ciclo de vida, permissões e estratégia de teste.
> A metade de UI/design é do `06-RESEARCH-UI.md`, escrito por outro agente. Nada aqui especifica
> layout, tokens ou composição visual.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Escopo da UI — DECISÃO DO USUÁRIO (2026-07-29), corrigindo a proposta inicial**

> "Ui minima, precisa ser UI completa e polida desde o inicio. Entreguei um desenho visual pra
> isso. Servir de base"

- **A UI desta fase é completa e polida desde o início**, não um mínimo funcional a ser refinado
  depois. Isso **substitui** a recomendação original de "só o mínimo que o `InCallService` exige".
- **Base visual:** o design system "Silent Guardian" em `docs/design/DESIGN.md` e os mockups
  entregues em `docs/design/telas/` (8 telas), com os tokens já implementados em
  `app/src/main/java/org/sentinela/app/ui/theme/`.
- **Ponto a confirmar com o usuário se ele discordar:** os mockups entregues **não incluem** tela
  de chamada nem de discagem. A instrução foi lida como *"o desenho entregue serve de base"* — ou
  seja, derivar as telas novas do mesmo sistema visual, com acabamento equivalente ao das telas
  existentes. Nada de placeholder, nada de "refina depois".
- **Tela cheia na chamada recebida é obrigatória** para um discador padrão. Implementar com
  `setFullScreenIntent` no canal de chamada — **nunca** `SYSTEM_ALERT_WINDOW`, que é proibido
  pelo `CLAUDE.md`.
- **Chamadas simultâneas, em espera e conferência ficam fora do MVP:** uma chamada por vez,
  documentado como limitação em `docs/LIMITACOES.md`.
- **Mudo, viva-voz e DTMF são obrigatórios** — critério de sucesso 2 explícito.
- Estados que a UI precisa cobrir com o mesmo acabamento: tocando (recebida), discando (saída),
  ativa, em mudo, em viva-voz, teclado DTMF aberto, encerrando e erro.
- Acessibilidade não é opcional num app de telefone: alvos de toque ≥ 48dp, `contentDescription`
  em todo controle, contraste conferido, e a tela de chamada precisa funcionar com TalkBack.

**Ativação e reversão**

- **`READ_CONTACTS` é pré-requisito**: sem ela as políticas por contato não funcionam e o app
  passaria a bloquear contatos. O modo discador não é oferecido enquanto a permissão não for
  concedida.
- **Texto de ativação honesto e explícito.** Deve dizer o que muda **e o que não melhora**: o
  registro no histórico do telefone continua acontecendo (provado na Phase 5 — o `ROLE_DIALER`
  **não** destrava isso) e o Não Perturbe continua valendo. Nada de texto vendedor.
- **Reversão:** botão que abre o seletor do sistema; o app **nunca** força a troca. Depois de
  reverter, o modo filtro continua operante sem reinstalação nem reconfiguração.
- **Perda silenciosa do papel:** detectar na abertura e na home, e degradar para modo filtro sem
  quebrar nada nem alarmar.

**Políticas por contato e risco**

- **Nada muda no `CallDecisionEngine`.** A precedência já trata `ContactLookup.HIT` com
  `contactsPolicy` desde a Phase 2, com 48 casos parametrizados. Qualquer ramo novo no motor nesta
  fase é sinal de erro de desenho.
- **Padrão da política de contatos continua Tocar.** Ativar o modo discador não pode, sozinho,
  começar a bloquear contatos.
- **Falha no `InCallService` não pode deixar o usuário sem telefone.** Precisa de caminho de
  degradação testado e documentado — é a pior falha possível desta fase.
- **Onde cada coisa é provada:** lógica, ciclo de vida e tradução de estado em JVM/Robolectric; a
  telefonia real vai para o roteiro Samsung da **Phase 9**, continuando a numeração dos cenários
  (hoje em 51). O emulador não reproduz troca de discador padrão de forma fiel.
- Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`.

**Permissões**

- Entram **nesta fase e só nela**: `ROLE_DIALER`, `BIND_INCALL_SERVICE` e `CALL_PHONE`,
  conforme `docs/PERMISSOES.md`. Cada uma entra no manifest **e** na allowlist do
  `scripts/verify-invariants.sh` **no mesmo commit**, com a doc conferida antes. Lembrar que a
  Phase 4 provou que adicionar uma permissão gera **dois** vermelhos no script.

### Claude's Discretion

- Organização dos arquivos de UI, nomes dos composables, estrutura da máquina de estado da chamada
  e como o `InCallService` se comunica com a UI ficam a critério do executor, desde que os 5
  critérios de sucesso passem e o acabamento visual seja equivalente ao dos mockups.

### Deferred Ideas (OUT OF SCOPE)

- Chamadas simultâneas, em espera e conferência — fora do MVP.
- Onboarding e home — Phase 7.
- Whitelist e histórico — Phase 8.
- Validação de telefonia real, viva-voz, DTMF, reversão e comportamento One UI em Samsung físico
  — Phase 9.

> ⚠️ **Duas decisões do CONTEXT foram FALSIFICADAS por medição.** Ver §Descobertas 5 e 6. Elas não
> mudam o objetivo da fase, mas mudam o plano de teste e o desenho da degradação. O planner deve
> tratar as versões medidas como as corretas e o CONTEXT como superado nesses dois pontos —
> exatamente como a Phase 5 fez com a premissa de bootstrap sobre contatos.
</user_constraints>

---

## Summary

Esta pesquisa leu a fonte AOSP local (`~/Library/Android/sdk/sources/android-35/android/telecom/`)
de `InCallService.java`, `Call.java`, `TelecomManager.java` e `DefaultDialerManager.java`, cruzou
com o `IncomingCallFilterGraph` / `CallScreeningServiceFilter` do `packages/services/Telecomm`, e
**executou 14 medições**: um emulador API 35 foi criado do zero (a máquina não tinha AVD nem
`avdmanager` — o `config.ini` foi escrito à mão), o app foi construído com uma sonda de
`InCallService`, instalado, promovido a discador padrão, submetido a chamadas de entrada e de saída
simuladas, morto no meio de uma chamada ativa, e revertido.

O resultado é bem melhor do que o risco registrado no `STATE.md` sugeria, e **inverte a análise de
risco da fase**. Três achados dominam o plano:

**(1) A elegibilidade ao papel foi isolada experimentalmente.** Com apenas a Activity de
`ACTION_DIAL` declarada, `cmd role add-role-holder android.app.role.DIALER` **falhou**. Declarando
também o `<service>` de `InCallService`, o mesmo comando **passou**. Não é dedução: são dois
builds, duas instalações e dois códigos de retorno. A lista mínima está na §Elegibilidade.

**(2) A pior falha da fase — "deixar o usuário sem telefone" — já é resolvida pela plataforma, e
foi medida.** Com o Sentinela como discador padrão e uma chamada **ativa**, um
`am force-stop org.sentinela.app` **não derrubou a chamada**: o Telecom detectou o `onDisconnected`
e religou imediatamente em `com.google.android.dialer/com.android.incallui.InCallServiceImpl` via
`EmergencyInCallServiceConnection`. As duas chamadas em curso continuaram `ACTIVE` e `ON_HOLD`. Na
chamada seguinte o Telecom voltou a se ligar à nossa sonda normalmente. **Morrer é seguro.** O modo
de falha realmente perigoso é o oposto: um serviço que faz bind com sucesso e mostra uma tela
travada ou em branco — aí o Telecom **não** tem como perceber e **não** faz o fallback. Isso
reorienta todo o esforço de degradação.

**(3) A triagem continua valendo no modo discador — e o critério de sucesso 3 funciona, mas não
pelo motivo que o CONTEXT supõe.** Medido: com o app segurando `ROLE_DIALER` e **ninguém** segurando
`ROLE_CALL_SCREENING`, o Telecom mesmo assim vinculou o `UnknownCallScreeningService` e honrou o
`Reject`. O `CallScreeningServiceFilter` roda **antes** do `InCallService` ser informado: uma chamada
bloqueada nunca chega ao `onCallAdded`. E quando o app segura os **dois** papéis, o grafo continua
com **um único** bind — não há triagem dupla.

**Primary recommendation:** declarar o manifest mínimo medido (§Elegibilidade), espelhar a
arquitetura vencedora da Phase 5 (um coordenador **puro** com costura, serviço fino de ~30 linhas),
e gastar o orçamento de teste na **degradação e na reversão**, não na tela — porque a tela é
verificável e o caminho de falha silenciosa não é. A fase é muito mais testável em CI do que o
CONTEXT assumia: o `InCallService` real roda em Robolectric **sem nenhuma reflexão** (ao contrário
do `CallScreeningService` da Phase 5), e o emulador reproduz o modo discador com fidelidade
suficiente para provar 4 dos 5 critérios.

---

<phase_requirements>
## Phase Requirements

| ID | Descrição | Suporte da pesquisa |
|----|-----------|---------------------|
| DIA-01 | Modo discador opcional via `ROLE_DIALER` com explicação clara | §Elegibilidade (manifest mínimo **medido**) + §Ciclo de vida do papel (pedir, detectar, perder). Texto honesto: §Honestidade obrigatória |
| DIA-02 | `InCallService` com UI própria: atender, recusar, encerrar, mudo, viva-voz, DTMF | §APIs de controle — todas em `Call`/`InCallService`, **nenhuma exige permissão**. §Ciclo de vida do `InCallService`. ⚠️ viva-voz **não é verificável no emulador** (§Descoberta 7) |
| DIA-03 | Discagem mínima: handler de `ACTION_DIAL` + tela de discagem | §Elegibilidade — os **dois** intent-filters (esquema vazio **e** `tel:`) são exigidos. Originar chamada: `TelecomManager.placeCall` com `CALL_PHONE` (§Permissões) |
| DIA-04 | Triagem cobre todas as chamadas, inclusive contatos, aplicando CTT | §Descoberta 3 — **medido**: o papel de discador sozinho já faz o Telecom vincular a triagem. Zero código novo no motor |
| DIA-05 | Reversão limpa; telefonia nunca quebrada; app segue no modo filtro | §Reversão (**medido**: remover o papel devolve o padrão ao discador nativo e o papel de triagem sobrevive) + §Degradação (**medido**: fallback automático do Telecom) |
| QLT-06 (parcial) | Instrumentados verdes, incluindo fluxo mínimo do `InCallService` | §Validation Architecture — `ServiceTestRule` para o bind e Robolectric para o comportamento |
| CTT-* (exercício) | Políticas por contato passam a valer de fato | §Descoberta 3 — a regra já existe e é testada desde a Phase 2; esta fase só a exercita |
| SCR-04 (destrava?) | Número privado/restrito | §Open Question 1 — **não confirmado**. Não prometer na UI antes de medir |
</phase_requirements>

---

## Descobertas que mudam o plano

### Descoberta 1 — A elegibilidade ao `ROLE_DIALER` foi ISOLADA por experimento (HIGH, MEDIDO)

Dois builds, duas instalações, no mesmo emulador API 35 (imagem `google_apis_playstore`):

| Variante | Manifest | `cmd role add-role-holder android.app.role.DIALER` |
|----------|----------|---------------------------------------------------|
| **A** | Activity com os dois intent-filters de `ACTION_DIAL` (esquema vazio + `tel:`), **sem** `InCallService` declarado | ❌ `java.util.concurrent.ExecutionException: java.lang.RuntimeException: Failed` — **rc=255**, papel permaneceu com `com.google.android.dialer` |
| **B** | O mesmo **mais** o `<service>` de `InCallService` (exported, `BIND_INCALL_SERVICE`, `<action android:name="android.telecom.InCallService"/>`, meta-data `IN_CALL_SERVICE_UI`) | ✅ **rc=0**, `get-role-holders` passou a devolver `org.sentinela.app` |

Isto **prova** que a Activity de discagem sozinha não basta e que o `InCallService` declarado é
parte da qualificação verificada pelo sistema de papéis, não apenas uma recomendação de javadoc.

Fonte concordante — javadoc de `InCallService` (android-35, verbatim):

> In order to fill the `RoleManager#ROLE_DIALER` role, an app must meet a number of requirements:
> - It must handle the `Intent#ACTION_DIAL` intent. This means the app must provide a dial pad UI
>   for the user to initiate outgoing calls.
> - It must fully implement the `InCallService` API and provide both an incoming call UI, as well
>   as an ongoing call UI.

E `DefaultDialerManager.getInstalledDialerApplications` (@hide, mas é o filtro de listagem):

```java
// In order to appear in the list, a dialer application must implement an intent-filter with
// the DIAL intent for the following schemes:
// 1) Empty scheme
// 2) tel Uri scheme
Intent intent = new Intent(Intent.ACTION_DIAL);
... queryIntentActivitiesAsUser(intent, 0, userId);          // esquema vazio
final Intent dialIntentWithTelScheme = new Intent(Intent.ACTION_DIAL);
dialIntentWithTelScheme.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, "", null));
return filterByIntent(context, packageNames, dialIntentWithTelScheme, userId);   // e tel:
```

Ou seja: **os dois intent-filters são necessários**, não um só. Um app que declare apenas
`<data android:scheme="tel"/>` não passa no primeiro filtro; um que declare apenas o esquema vazio
não passa no segundo.

**Aviso do próprio AOSP, que vira invariante de projeto** (javadoc de `InCallService`):

> If your app fills `ROLE_DIALER` and makes changes at runtime which cause it to no longer fulfil
> the requirements of this role, `RoleManager` will automatically remove your app from the role and
> **close your app**.

Consequência dura: **nunca** desabilitar o `InCallService` ou a Activity de discagem por
`setComponentEnabledSetting`. Um "desligar o modo discador desabilitando o componente" derruba o
processo do usuário. A reversão correta é a do §Reversão.

### Descoberta 2 — Morrer no meio de uma chamada é SEGURO (HIGH, MEDIDO) — e isso inverte o risco

Roteiro executado: Sentinela como discador padrão → chamada de saída ativa (`TC@2`) → segunda
chamada (`TC@4`) → `adb shell am force-stop org.sentinela.app` durante a chamada ativa.

Logcat do Telecom, verbatim:

```
InCallController: onDisconnected from ComponentInfo{org.sentinela.app/....ProbeInCallService}
InCallController: ICSBC#disconnect: unbinding after 51090 ms; [...ProbeInCallService...]. isCrashed: false
InCallController$EmergencyInCallServiceConnection: Attempting to bind to InCall
    [ComponentInfo{com.google.android.dialer/com.android.incallui.InCallServiceImpl} ...]
```

`dumpsys telecom` imediatamente depois:

```
[Call id=TC@4, state=ACTIVE,   ... handle=tel:**********, ...]
[Call id=TC@2, state=ON_HOLD,  ... handle=tel:**********, ...]
```

**As chamadas sobreviveram.** O Telecom religou sozinho no discador pré-instalado. E na chamada
seguinte, com o processo já reiniciado, o bind voltou a ser o nosso:

```
SENTINELA_PROBE: onCallAdded state=1 dir=1
SENTINELA_PROBE: onStateChanged state=4
```

Isso é o comportamento documentado, agora confirmado empiricamente (javadoc de `InCallService`):

> If the app filling `ROLE_DIALER` returns a `null` `InCallService` during binding, the Telecom
> framework will automatically fall back to using the dialer app preloaded on the device. The
> system will display a notification to the user to let them know that their call was continued
> using the preloaded dialer app.

**A reorientação que o planner precisa absorver:**

| Modo de falha | O Telecom percebe? | Usuário perde o telefone? | Risco real |
|---------------|--------------------|---------------------------|-----------|
| Processo morre / crash não tratado | **SIM** (`onDisconnected`) | **NÃO** — fallback automático medido | BAIXO |
| `onBind` devolve `null` | **SIM** (documentado) | NÃO — fallback + notificação do sistema | BAIXO |
| Bind OK, mas a Activity de chamada **não abre** (full-screen intent negado, tela travada) | **NÃO** | **SIM, na prática** | **ALTO** |
| Bind OK, UI abre mas congela / ANR na main thread | **NÃO** | **SIM, na prática** | **ALTO** |
| `Call.answer()` nunca chamado porque a UI engoliu o toque | **NÃO** | **SIM, na prática** | **ALTO** |

Conclusão para o desenho: **não construir um "modo de segurança" que capture exceções e siga em
frente com a tela quebrada.** Isso converte uma falha que a plataforma sabe consertar numa falha que
ela não sabe. A regra deve ser a **oposta** da Phase 5:

> Na triagem, exceção inesperada → **permitir a chamada** (rede permissiva).
> No `InCallService`, exceção que impeça mostrar a UI de chamada → **deixar propagar** e morrer, para
> que o Telecom faça o fallback documentado. Engolir é pior.

O único trabalho de degradação que vale a pena escrever é o **watchdog de exibição**: se o
`onCallAdded` de uma chamada em `STATE_RINGING` não conseguir apresentar a UI em um prazo curto,
falhar alto em vez de ficar em silêncio. Isso é testável em JVM.

### Descoberta 3 — Segurar `ROLE_DIALER` já liga a triagem, sozinho (HIGH, MEDIDO)

Estado do aparelho no momento do teste: `get-role-holders android.app.role.DIALER` →
`org.sentinela.app`; `get-role-holders android.app.role.CALL_SCREENING` → **vazio**.

Chamada de entrada simulada (`gsm call`), logcat:

```
IncomingCallFilterGraph: Filter ...CallScreeningServiceFilter@cfde5b4: null scheduled.
IncomingCallFilterGraph: Filter ...CallScreeningServiceFilter@14fd5dd: org.sentinela.app scheduled.
Event: RecordEntry TC@1: SCREENING_BOUND, ComponentInfo{org.sentinela.app/...UnknownCallScreeningService}
Event: RecordEntry TC@1: SCREENING_COMPLETED, [Reject, logged, mCallBlockReason = 1, ...]
CallsManager: onCallFilteringCompleted: blocked call, rejecting.
CallsManager: onCallScreeningCompleted: blocked call, adding to call log.
```

Três leituras, todas importantes:

1. **O papel de discador basta.** O `CallScreeningServiceFilter` tem três tipos de pacote
   (`PACKAGE_TYPE_CARRIER=0`, `PACKAGE_TYPE_DEFAULT_DIALER=1`, `PACKAGE_TYPE_USER_CHOSEN=2`) e o
   Telecom monta um filtro para cada. Com o papel de discador, caímos no slot `DEFAULT_DIALER`.
   **DIA-04 não exige código novo nenhum** — é consequência de segurar o papel.
2. **A triagem roda ANTES do `InCallService`.** `onCallFilteringCompleted: blocked call, rejecting`
   acontece antes de qualquer bind de in-call. Uma chamada bloqueada **nunca** chega ao
   `onCallAdded` — confirmado pela ausência total de log da sonda nesse teste. Isso é ótimo: a UI de
   chamada não precisa de nenhum ramo "e se for bloqueada".
3. **Segurar os dois papéis NÃO causa triagem dupla.** Repeti o teste após conceder também
   `ROLE_CALL_SCREENING` ao mesmo pacote: continuaram exatamente **dois** filtros no grafo (um com
   pacote `null` — o slot de operadora, vazio no emulador — e um com `org.sentinela.app`) e **um**
   `SCREENING_BOUND`. O risco que eu havia levantado de o campo `mCallScreeningAdapter` ser
   sobrescrito por dois binds concorrentes **não se materializa** neste caminho.

Um detalhe que confirma a Phase 5 e a honestidade exigida pelo CONTEXT: mesmo como discador padrão,
o log traz `onCallScreeningCompleted: blocked call, adding to call log` seguido de
`LogCall; logged callId=TC@1`. **O papel de discador não destrava o `setSkipCallLog`** —
`packageTypeShouldAdd` só isenta `PACKAGE_TYPE_CARRIER`. O texto de ativação não pode insinuar o
contrário.

### Descoberta 4 — `USE_FULL_SCREEN_INTENT` é uma permissão NOVA que não está na matriz (HIGH, MEDIDO)

O CONTEXT trava a lista de permissões desta fase em `ROLE_DIALER`, `BIND_INCALL_SERVICE` e
`CALL_PHONE`. **Falta uma.** A tela cheia de chamada recebida — que o CONTEXT declara obrigatória —
depende de `setFullScreenIntent`, e a partir do Android 14 isso exige
`android.permission.USE_FULL_SCREEN_INTENT`.

Javadoc de `Notification.Builder.setFullScreenIntent` (android-35, verbatim):

> If the posting app holds `USE_FULL_SCREEN_INTENT`, then the heads up notification will appear
> persistently until the user dismisses or snoozes it, or the app cancels it. If the posting app
> does **not** hold `USE_FULL_SCREEN_INTENT`, then the notification will appear as heads up
> notification even when the screen is locked or turned off, and this notification will only be
> persistent for **60 seconds**.
> To be launched as a full screen intent, the notification must also be posted to a channel with
> importance level set to **IMPORTANCE_HIGH or higher**.

Mudança de comportamento do Android 14 (developer.android.com):

> For apps targeting Android 14 (API level 34) or higher, apps that are allowed to use this
> permission are limited to those that provide **calling and alarms** only.

**Medido neste repositório:** com o `InCallService` declarado e `targetSdk 37`, a permissão foi
**concedida no install, sem diálogo**:

```
android.permission.USE_FULL_SCREEN_INTENT: granted=true
android.permission.CALL_PHONE: granted=false, flags=[ USER_SENSITIVE_WHEN_GRANTED|... ]
```

Ou seja: o Sentinela se qualifica como "calling app" e recebe a permissão automaticamente. Mas ela
**precisa ser declarada** e, porque é uma permissão nova fora da lista fechada, esta fase tem uma
obrigação de processo antes de qualquer código:

- [ ] `docs/PERMISSOES.md` ganha a linha de `USE_FULL_SCREEN_INTENT` na tabela do modo discador,
      com justificativa (leitura **bloqueante** antes de tocar no manifest).
- [ ] `ALLOWLIST` de `scripts/verify-invariants.sh` ganha a permissão **no mesmo commit**.
- [ ] O texto de `docs/PERMISSOES.md` já previa isto: *"qualquer permissão adicional que ela revele
      passa por PR + esta matriz"*. Esta é ela.

Além disso, o código **não pode assumir a concessão**. O usuário pode revogá-la em Configurações.
Usar `NotificationManager.canUseFullScreenIntent()` (API 34+) e, quando `false`, degradar para
notificação heads-up de 60 s com ações de atender/recusar — que ainda é um caminho funcional. Nunca
apontar o usuário para `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` de forma insistente: é uma tela de
Configurações, e o produto não usa dark pattern.

### Descoberta 5 — O `InCallService` é MUITO mais testável que o `CallScreeningService` (HIGH, MEDIDO)

O CONTEXT diz que "a Phase 5 provou que `Robolectric.buildService` + proxy do adapter funciona para
Service de telecom". Correto — e nesta fase **o proxy nem é necessário**. Cinco testes escritos e
executados, todos verdes:

| Sonda | Resultado medido |
|-------|------------------|
| `Robolectric.buildService(InCallService::class.java).create().get()` | ✅ `ProbeInCallService@5b47872f` |
| `mockk<Call>(relaxed = true)` — apesar de `Call` ser `public final class` | ✅ funciona; `answer(0)`, `disconnect()` e `playDtmfTone('5')` são **verificáveis** com `verify {}` |
| `svc.onCallAdded(mockCall)` chamado direto no Service real | ✅ o `registerCallback` interno é observável |
| Construtores públicos de `Call.Details` | ✅ **2 construtores públicos**, um deles com 21 parâmetros — dá para montar `Details` real, sem reflexão |
| `svc.setMuted(true)` / `svc.getCalls()` num Service sem `Phone` | ⚠️ **não lança**; `getCalls()` devolve `[]` |

Duas consequências:

1. **Não existe justificativa para reflexão nesta fase.** O harness da Phase 5 precisou de
   `Proxy` sobre `com.android.internal.telecom.ICallScreeningAdapter` e de acesso ao campo privado
   `mCallScreeningAdapter` porque `respondToCall` só é observável por lá. Aqui, `onCallAdded` é um
   método público sobrescrito por nós e `Call` é mockável. **Se um plano propuser reflexão no
   `InCallService`, está errado.**
2. **Armadilha de teste vacuoso, encontrada ao medir.** `setMuted` num Service sem `Phone` é um
   **no-op silencioso** (`if (mPhone != null)` no AOSP). Um teste que chame `svc.setMuted(true)` e
   não asserte nada passa sem provar coisa alguma — exatamente a classe de falso-verde que já pegou
   este projeto três vezes (Phase 3 `@Upsert`, Phase 3 índice, Phase 4 cache). O mudo e o viva-voz
   têm de ser provados **na costura**, não no Service.

### Descoberta 6 — O emulador reproduz o modo discador MUITO melhor do que o CONTEXT assume (HIGH, MEDIDO)

O CONTEXT afirma: *"O emulador não reproduz troca de discador padrão de forma fiel."* **Falso, nos
pontos que importam.** Medido no `Medium_Phone_API_35`:

| Capacidade | Emulador | Comando |
|------------|----------|---------|
| Conceder o papel de discador de verdade (com verificação de elegibilidade) | ✅ | `adb shell cmd role add-role-holder android.app.role.DIALER <pkg>` |
| Remover o papel e ver o padrão voltar ao nativo | ✅ | `adb shell cmd role remove-role-holder ...` |
| Override de teste do discador padrão | ✅ | `adb shell telecom set-default-dialer <pkg>` |
| Chamada de **entrada** real pelo rádio simulado | ✅ | console do emulador: `auth <token>` + `gsm call <num>` |
| Chamada de **saída** real | ✅ | `am start -a android.intent.action.CALL -d tel:<num>` |
| Máquina de estados completa (`CONNECTING→DIALING→ACTIVE→ON_HOLD`) | ✅ | observada na sonda |
| Triagem + bloqueio ponta a ponta como discador | ✅ | observado |
| Morte do processo mid-call e fallback do Telecom | ✅ | `am force-stop` |
| **Roteamento de áudio / viva-voz** | ❌ | `supportedRouteMask: SPEAKER` **apenas** — sem fone, sem earpiece |
| Comportamento da One UI ao trocar o app de telefone | ❌ | só Samsung físico |

O token do console fica em `~/.emulator_console_auth_token` e é obrigatório antes de `gsm call`.

**Impacto no plano:** boa parte de DIA-01, DIA-02, DIA-03, DIA-04 e DIA-05 pode virar teste
instrumentado automatizado, em vez de item de roteiro humano. Só **viva-voz** e **One UI** ficam
genuinamente para a Phase 9. Isso é uma diferença grande de cobertura para a fase de maior risco do
MVP, e o planner deve aproveitá-la.

*(Nota operacional: a máquina de pesquisa não tinha AVD nem `cmdline-tools`/`avdmanager`. O AVD
`Medium_Phone_API_35` que `scripts/run-instrumented-tests.sh` espera foi recriado à mão a partir da
imagem `system-images/android-35/google_apis_playstore/arm64-v8a` e permanece em `~/.android/avd/`.
Nada disso toca o repositório.)*

### Descoberta 7 — `ACTION_DIAL` NÃO é sequestrado quando não somos o padrão (MEDIDO)

Depois de remover o papel, `am start -a android.intent.action.DIAL -d tel:...` resolveu para
`com.google.android.dialer/com.android.dialer.main.impl.MainActivity`, não para nós — mesmo com os
nossos intent-filters ainda declarados no manifest.

**Consequência tranquilizadora:** declarar os filtros de `ACTION_DIAL` (obrigatório para a
elegibilidade) **não** faz o app se intrometer na discagem de quem não ativou o modo discador. Não é
preciso inventar nenhum guard para isso, e nenhum plano deve gastar task tentando. O que ainda deve
ser tratado é o caso de o usuário **abrir a tela de discagem sem o papel**: a tela precisa se
comportar (explicar, ou delegar), não travar.

---

## Elegibilidade ao `ROLE_DIALER` — manifest mínimo completo

Confirmado por experimento (§Descoberta 1) e pelo javadoc de `InCallService`.

```xml
<!-- Fase 6, modo discador OPCIONAL. Ver docs/PERMISSOES.md antes de mexer. -->
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<!-- (1) O servico de UI de chamada. Sem ele o papel e NEGADO (medido: rc=255). -->
<service
    android:name=".telecom.SentinelaInCallService"
    android:exported="true"
    android:permission="android.permission.BIND_INCALL_SERVICE">
    <meta-data android:name="android.telecom.IN_CALL_SERVICE_UI" android:value="true" />
    <intent-filter>
        <action android:name="android.telecom.InCallService" />
    </intent-filter>
</service>

<!-- (2) A Activity de discagem, com os DOIS intent-filters. Um so nao basta. -->
<activity android:name=".ui.dialer.DialerActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="tel" />
    </intent-filter>
</activity>
```

| Item | Obrigatório? | Evidência |
|------|--------------|-----------|
| `<service>` de `InCallService` declarado | **SIM** — sem ele o papel é negado | medido (variante A × B) |
| `android:exported="true"` no serviço | **SIM** | javadoc: *"You should NOT mark your InCallService with `android:exported="false"`; doing so can result in a failure to bind"* |
| `android:permission="android.permission.BIND_INCALL_SERVICE"` | **SIM** | javadoc + `docs/PERMISSOES.md` (só o sistema pode se conectar) |
| `<action android:name="android.telecom.InCallService"/>` | **SIM** | `InCallService.SERVICE_INTERFACE` |
| meta-data `android.telecom.IN_CALL_SERVICE_UI = true` | **SIM na prática** — declara que substituímos a UI de chamada nativa | javadoc. Foi o que a variante B usou |
| meta-data `android.telecom.IN_CALL_SERVICE_RINGING` | **NÃO** — e **não declarar** | ver abaixo |
| Activity com `ACTION_DIAL`, esquema vazio | **SIM** | `DefaultDialerManager` filtro 1 |
| Activity com `ACTION_DIAL`, `<data android:scheme="tel"/>` | **SIM** | `DefaultDialerManager` filtro 2 |
| `CALL_PHONE` | **SIM** para originar chamada | `@RequiresPermission` de `placeCall` |
| `USE_FULL_SCREEN_INTENT` | **SIM** para a tela cheia | §Descoberta 4 |

### Sobre `IN_CALL_SERVICE_RINGING` — decisão recomendada: **não declarar**

Javadoc: *"The meta-data `METADATA_IN_CALL_SERVICE_RINGING` indicates that this `InCallService` will
play the ringtone for incoming calls."* Declará-lo transfere para o app a responsabilidade de tocar
o toque, via canal de notificação com som próprio.

Não declarar é a escolha certa para este produto por três razões: (a) o sistema continua tocando o
toque escolhido pelo usuário, que é o comportamento esperado de um telefone; (b) evita ter de
replicar volume, vibração, escalonamento e Não Perturbe — exatamente o tipo de coisa que a seção
§Don't Hand-Roll proíbe; (c) o produto se define por **não interromper**, não por customizar o
toque. Um plano que declare esse meta-data está ampliando escopo sem pedido do usuário.

---

## Ciclo de vida do `InCallService` — como ligar a UI sem vazar

### O contrato, verificado

| Evento | Quando | Thread | Observado |
|--------|--------|--------|-----------|
| `onBind` / criação do processo | Telecom vincula quando existe uma chamada | — | sim (medido em call frio) |
| `onCallAdded(Call)` | uma vez por chamada | main | `state=9 (CONNECTING) dir=1 (OUTGOING)` |
| `onCallAudioStateChanged` | logo após o add e a cada mudança | main | `route: SPEAKER, mask: SPEAKER` |
| `Call.Callback.onStateChanged` | a cada transição | main | `9 → 1 (DIALING) → 4 (ACTIVE)`; com 2 chamadas: `→ 3 (ON_HOLD)` |
| `onCallRemoved(Call)` | ao terminar | main | sim |
| `onUnbind` | quando não há mais chamadas | main | sim (`unbinding after 51090 ms`) |

Estados relevantes de `Call` (android-35): `STATE_NEW=0`, `STATE_DIALING=1`, `STATE_RINGING=2`,
`STATE_HOLDING=3`, `STATE_ACTIVE=4`, `STATE_DISCONNECTED=7`, `STATE_SELECT_PHONE_ACCOUNT=8`,
`STATE_CONNECTING=9`, `STATE_DISCONNECTING=10`, `STATE_PULLING_CALL=11`, `STATE_AUDIO_PROCESSING=12`,
`STATE_SIMULATED_RINGING=13`.

⚠️ Com "uma chamada por vez" no escopo, os estados **não** cobertos pela UI ainda precisam ser
tratados de forma segura: `STATE_HOLDING`, `STATE_SELECT_PHONE_ACCOUNT` (dual SIM! o Galaxy do
usuário pode ter dois chips) e `STATE_AUDIO_PROCESSING`. "Fora de escopo" não pode virar
`else -> {}` que deixa a tela em branco — ver Descoberta 2, esse é o modo de falha perigoso.

### Arquitetura recomendada (espelha o que deu certo na Phase 5)

O problema é que `InCallService` e a Activity são dois componentes com ciclos de vida
independentes, e a chamada sobrevive à Activity (rotação, morte de processo, usuário sai da tela).
Amarrar a Activity ao Service, ou o contrário, é a origem de todo vazamento em app de discador.

O padrão que resolve, e que já é o padrão deste repositório:

```
SentinelaInCallService  (fino, ~30 linhas, só plataforma)
        ↕  registra/remove no
CallSessionStore        (objeto de processo, singleton no AppContainer)
        │   StateFlow<CallUiState>   ← a UI observa
        │   CallControls             ← a UI comanda
        ↓
CallSessionCoordinator  (PURO — zero import de android.*, todo o comportamento e a máquina de estado)
```

| Regra | Por quê |
|-------|---------|
| O `Call` **nunca** sai da camada telecom | `Call` é um handle de Binder; guardá-lo na UI é vazamento garantido |
| A UI observa um `StateFlow<CallUiState>` de domínio | sobrevive a rotação e a morte de processo de graça; nenhum `SavedStateHandle` |
| A UI comanda por uma interface `CallControls` (atender/recusar/encerrar/mudo/viva-voz/DTMF) | é a **costura** — é onde mudo e viva-voz são provados (§Descoberta 5, armadilha do no-op) |
| `Call.Callback` é registrado em `onCallAdded` e **removido** em `onCallRemoved` | vazamento clássico; a sonda desta pesquisa já fazia os dois |
| O `CallSessionStore` vive no `AppContainer` existente, `by lazy` | instância única do processo — a Phase 5 mediu que um segundo `AppContainer` derruba o processo (`multiple DataStores active`) |
| `CallSessionCoordinator` sem `android.*` | entra no Kover sem exclude, como o `ScreeningCoordinator` (que levou a cobertura de 96,68% a 97,64%) |
| Nada de `WorkManager`, Hilt, Koin, Dagger | proibido no projeto |

A Activity de chamada é lançada pelo full-screen intent e apenas **observa** o store. Se o processo
morrer, o Telecom faz o fallback (Descoberta 2) — não há estado a restaurar, o que é uma
simplificação enorme e deve ser dita explicitamente no plano para ninguém inventar persistência de
chamada.

---

## APIs de controle — o que cada botão chama e o que exige

Verificado linha a linha em `Call.java` e `InCallService.java` (android-35). **Nenhum destes métodos
tem anotação `@RequiresPermission`** — a autorização vem do bind privilegiado do Telecom, não de uma
permissão do app.

| Ação da UI | API | Permissão | Nível | Observação |
|------------|-----|-----------|-------|-----------|
| Atender | `Call.answer(VideoProfile.STATE_AUDIO_ONLY)` | **nenhuma** | 23+ | válido só em `STATE_RINGING` |
| Recusar | `Call.reject(Call.REJECT_REASON_DECLINED)` | **nenhuma** | **34+** | ver abaixo |
| Recusar (compatível) | `Call.reject(false, null)` | **nenhuma** | 23+ | usar em API < 34 |
| Encerrar | `Call.disconnect()` | **nenhuma** | 23+ | vale em ativa e em discando |
| Mudo | `InCallService.setMuted(boolean)` | **nenhuma** | 23+ | **no-op silencioso sem `Phone`** — armadilha de teste |
| Viva-voz | `InCallService.setAudioRoute(CallAudioState.ROUTE_SPEAKER / ROUTE_EARPIECE)` | **nenhuma** | 23+ | consultar `getCallAudioState().supportedRouteMask` antes |
| DTMF | `Call.playDtmfTone(char)` + `Call.stopDtmfTone()` | **nenhuma** | 23+ | **obrigatoriamente pareados** |
| Originar chamada | `TelecomManager.placeCall(Uri, Bundle)` | **`CALL_PHONE`** (runtime) | 23+ | única API da fase que exige permissão |
| Estado do áudio | `InCallService.getCallAudioState()` / `onCallAudioStateChanged` | **nenhuma** | 23+ | `getAudioState()` está deprecado |

Três avisos concretos:

- **`Call.reject(int rejectReason)` é API 34+.** `REJECT_REASON_DECLINED` e `REJECT_REASON_UNWANTED`
  também. `minSdk` é 29 → o código **precisa** do branch de versão, e o teste precisa cobrir os dois.
  Este é o tipo de detalhe que só aparece em aparelho antigo, tarde demais.
- **DTMF pareado.** Javadoc: *"You must ensure that any call to `playDtmfTone(char)` is followed by a
  matching call to `stopDtmfTone()` and that each tone is stopped before a new one is started."*
  Toque rápido em duas teclas, ou saída da tela com o dedo pressionado, quebra isso. É um invariante
  assertável na costura — e merece o mesmo tratamento que a resposta-única da Phase 5.
- **Emergência é sempre do discador pré-instalado.** Javadoc de `InCallService`: *"The preloaded
  dialer will ALWAYS be used when the user places an emergency call, even if your app fills the
  `ROLE_DIALER` role."* Se a discagem própria for usada para 190/192/193, o AOSP recomenda
  `TelecomManager.placeCall`, porque `ACTION_CALL` de um discador não pré-instalado é *"raised to the
  preloaded dialer app using `ACTION_DIAL` for confirmation; this is a suboptimal user experience"*.
  **Usar `placeCall`, nunca `ACTION_CALL`.** E a UI não deve prometer nada sobre emergência.

---

## Tela cheia de chamada recebida — o caminho correto, sem overlay

`SYSTEM_ALERT_WINDOW` é proibido pelo `CLAUDE.md` e **confirmadamente desnecessário**. O caminho
oficial, citado no próprio javadoc de `InCallService`, é notificação + `setFullScreenIntent`.

| Requisito | Valor | Nível | Fonte |
|-----------|-------|-------|-------|
| Canal dedicado, `IMPORTANCE_HIGH` **ou maior** | obrigatório para a tela cheia disparar | 26+ | javadoc de `setFullScreenIntent` |
| `setFullScreenIntent(pendingIntent, true)` | o mecanismo | 21+ | idem |
| `USE_FULL_SCREEN_INTENT` declarada | obrigatório em 34+ | 29+ (declarar sempre) | §Descoberta 4 |
| `Notification.CallStyle.forIncomingCall(person, declineIntent, answerIntent)` | recomendado | **31+** | `Notification.java` android-35 |
| `Notification.CallStyle.forOngoingCall(person, hangUpIntent)` | recomendado para a chamada ativa | **31+** | idem |
| Caminho para **API 29–30** | notificação comum com `addAction` de atender/recusar + `setFullScreenIntent` + `setOngoing(true)` + `PRIORITY_HIGH` | 29–30 | javadoc de `InCallService` traz exatamente este exemplo |
| `PendingIntent` | `FLAG_IMMUTABLE` quando não precisar de extras mutáveis | 31+ obrigatório | regra já usada na Phase 5 (NTF-05) |
| Intent da Activity | `FLAG_ACTIVITY_NEW_TASK` + `FLAG_ACTIVITY_NO_USER_ACTION` | — | exemplo do javadoc |

⚠️ **Canal separado, sem reaproveitar o da Phase 5.** O canal de chamada bloqueada é
`IMPORTANCE_LOW` por decisão travada de produto, e a importância de um canal é **imutável após a
criação** (lição já registrada na Phase 5). Tentar reutilizá-lo produz uma tela cheia que nunca
dispara. São dois canais distintos, criados em pontos distintos.

⚠️ **Privacidade vale aqui também.** O invariante da Phase 5 — *nenhum campo do `Notification`
contém o número completo* — foi provado varrendo extras e versão pública. A notificação de chamada
recebida é **mais** exposta (tela bloqueada, heads-up). No modo discador, porém, o usuário está
atendendo a chamada: exibir o número na **Activity** de chamada é legítimo e necessário. A regra a
manter é a existente, sem inventar exceção: usar `PhoneMask` onde a Phase 5 usa, e decidir
explicitamente — com o usuário, se necessário — o que aparece na notificação de tela bloqueada.
Isto é um ponto de contato com o `06-RESEARCH-UI.md` e merece um alinhamento no plano.

---

## Reversão e perda do papel

### Reversão (DIA-05) — medido

```
$ adb shell cmd role remove-role-holder android.app.role.DIALER org.sentinela.app   → rc=0
$ adb shell cmd role get-role-holders android.app.role.DIALER      → com.google.android.dialer
$ adb shell cmd role get-role-holders android.app.role.CALL_SCREENING → org.sentinela.app
```

E, em seguida, uma chamada de entrada simulada continuou sendo triada e bloqueada:

```
Event: RecordEntry TC@6: SCREENING_BOUND, ...UnknownCallScreeningService
Event: RecordEntry TC@6: SCREENING_COMPLETED, [Reject, ...]
```

**DIA-05 verificado no emulador:** o papel de discador volta sozinho ao nativo, o papel de triagem
é independente e sobrevive, e o modo filtro continua operante sem reconfiguração.

**Como o app reverte, na prática:** não existe API pública para um app remover o próprio papel
(`removeRoleHolderAsUser` é `@SystemApi` e exige `MANAGE_ROLE_HOLDERS`, permissão de sistema —
proibida). O único caminho honesto é **abrir o seletor do sistema** e deixar o usuário escolher
outro app de telefone:

```kotlin
// abre a tela de escolha do app de telefone padrão
Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)   // ou o seletor de papel
```

Isso é exatamente o que o CONTEXT já decidiu (*"botão que abre o seletor do sistema; o app nunca
força a troca"*) — e agora está confirmado que **é a única opção existente**, não uma escolha de
estilo. Nenhum plano deve gastar task procurando uma API de auto-remoção.

**O que precisa ser limpo na reversão:** quase nada, e isso é bom. As chamadas são do Telecom, não
nossas; não há PhoneAccount registrada (não somos `ConnectionService`); não há estado de telefonia
persistido. A lista real é curta:

- [ ] Cancelar qualquer notificação de chamada ativa/recebida ainda postada.
- [ ] Fechar a Activity de chamada se estiver visível.
- [ ] Marcar a configuração interna "modo discador" como desligada (sem gravá-la como fonte da
      verdade — ver abaixo).
- [ ] **Nunca** desabilitar componentes por `setComponentEnabledSetting` (Descoberta 1: o
      `RoleManager` fecha o app).

### Detectar o papel — não existe observer, de novo

Idêntico ao `ROLE_CALL_SCREENING` da Phase 5, e pela mesma razão:
`RoleManager.addOnRoleHoldersChangedListener` é `@SystemApi` e exige `MANAGE_ROLE_HOLDERS`.

| Pergunta | Resposta | Confiança |
|----------|----------|-----------|
| Pedir | `RoleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)` num `ActivityResultLauncher` | HIGH |
| Consultar | `RoleManager.isRoleHeld(ROLE_DIALER)` | HIGH |
| Disponibilidade | `RoleManager.isRoleAvailable(ROLE_DIALER)` — `false` em aparelho sem telefonia | HIGH |
| Callback de mudança | **NÃO EXISTE** para app comum | HIGH |
| Detectar perda | **polling** em `onResume` | HIGH |

**A fonte da verdade é `isRoleHeld(ROLE_DIALER)`, sempre — nunca uma flag no DataStore.** Uma flag
"modo discador ligado" persistida vira mentira no instante em que o usuário troca o discador nas
Configurações do sistema, e o app passa a mostrar uma tela que não corresponde à realidade. Se o
plano precisar de uma preferência gravada (por exemplo, para lembrar a intenção do usuário), ela é
**secundária** e a checagem do papel sempre vence.

`ScreeningRoleManager` (Phase 5) é o precedente exato: mesma forma, papel diferente. Generalizá-lo
para receber o papel como parâmetro, ou criar um irmão, é discricionário — mas **duplicar** a lógica
não é.

---

## Degradação — o desenho concreto que o CONTEXT pede

O CONTEXT diz que deixar o usuário sem telefone é "a pior falha possível desta fase". A medição da
Descoberta 2 mostra que a plataforma **já resolve** o caso do crash. O trabalho de engenharia,
portanto, muda de alvo:

| Cenário | Comportamento desejado | Como provar |
|---------|------------------------|-------------|
| Exceção ao montar o estado da chamada | **propagar**, deixar o processo morrer, Telecom faz fallback | JVM: a costura relança; teste assegura que o coordenador **não** engole |
| `canUseFullScreenIntent() == false` | notificação heads-up com ações atender/recusar (60 s), sem tela cheia | JVM/Robolectric na costura de notificação |
| Estado de `Call` não coberto pela UI (`HOLDING`, `SELECT_PHONE_ACCOUNT`, `AUDIO_PROCESSING`) | estado de domínio explícito "não suportado" com tela informativa **e** com o botão de encerrar funcionando | JVM: mapa exaustivo `Int → CallUiState`, sem `else` mudo |
| Segunda chamada durante uma ativa | fora do escopo, mas **nunca** tela em branco: mostrar a primeira e permitir encerrar | JVM (coordenador recebe dois `onCallAdded`) |
| Papel perdido enquanto o app está aberto | degradar para modo filtro na retomada, sem alarme | JVM (`isRoleHeld` falso) + emulador |
| Processo morto mid-call | não fazer nada — a plataforma resolve | emulador (`am force-stop`), roteiro reproduzido nesta pesquisa |

**O invariante desta fase, análogo à "resposta exatamente uma vez" da Phase 5:**

> Toda chamada em `STATE_RINGING` entregue ao `onCallAdded` **precisa** produzir, dentro de um prazo
> curto, ou uma UI apresentada ao usuário, ou uma falha alta. Silêncio é proibido.

Esse é o guarda-corpo que merece prova de vermelho (quebrar, ver falhar, restaurar), como manda a
prática acumulada do projeto.

---

## Honestidade obrigatória no texto de ativação

Reforçado por medição desta fase, não só herdado da Phase 5:

| O usuário pode achar que… | Verdade medida |
|---------------------------|----------------|
| "vira discador padrão → some do histórico do telefone" | **NÃO.** `onCallScreeningCompleted: blocked call, adding to call log` observado **com o papel de discador ativo**. A isenção é só de app de operadora |
| "vira discador padrão → fura o Não Perturbe" | **NÃO.** Filtro paralelo, sem influência da nossa resposta (Phase 5, `DndCallFilter`) |
| "vira discador padrão → chamadas de emergência passam pelo app" | **NÃO.** O discador pré-instalado é sempre usado em emergência (javadoc) |
| "vira discador padrão → filtra WhatsApp/VoIP" | **NÃO.** Nunca prometer isso, em nenhuma fase |
| "vira discador padrão → contatos passam a ser triados" | **SIM** — e já eram, no modo filtro, desde a Phase 4 (Descoberta 2 da Phase 5). O ganho real é menor do que parece e o texto deve dizer isso |

O último ponto merece cuidado de produto: como a Phase 5 descobriu que contatos **já** chegam à
triagem no modo filtro (com `READ_CONTACTS` concedida), o benefício exclusivo do modo discador é
principalmente **a experiência de chamada própria**, não "destravar as políticas de contato". Vender
o contrário seria a mesma desonestidade que motivou o plano 05-07. Vale confirmar o enquadramento com
o usuário antes de escrever a string.

---

## Don't Hand-Roll

| Problema | Não construir | Usar | Por quê |
|----------|---------------|------|---------|
| Tela cheia sobre a tela bloqueada | overlay, `SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY` | canal `IMPORTANCE_HIGH` + `setFullScreenIntent` | proibido pelo `CLAUDE.md` e desnecessário — o caminho oficial existe e funciona |
| Tocar o toque de chamada | `Ringtone`/`MediaPlayer` próprios | **não declarar** `IN_CALL_SERVICE_RINGING`; o sistema toca | replicaria volume, vibração, escalonamento e Não Perturbe |
| Layout de notificação de chamada | `RemoteViews` custom | `Notification.CallStyle` (31+), `addAction` (29–30) | acessibilidade, Auto/Wear e One UI de graça |
| Sobreviver a rotação/morte de processo | `SavedStateHandle`, persistência de chamada | `StateFlow` num store de processo; o Telecom refaz o bind | medido: o Telecom religa sozinho e reentrega a chamada |
| Detectar perda do papel | broadcast, listener, serviço em foreground | polling `isRoleHeld()` em `onResume` | o listener é `@SystemApi`; precedente da Phase 5 |
| Reverter o papel | qualquer API de auto-remoção | abrir o seletor do sistema | `removeRoleHolderAsUser` é `@SystemApi` |
| Estado da chamada | máquina de estado nova, ad hoc | mapa **exaustivo** dos `Call.STATE_*` para um estado de domínio | `else -> {}` é a origem da tela em branco (o modo de falha perigoso) |
| Guardar a chamada | `Call` em ViewModel/companion/estático | id opaco no store; `Call` só na camada telecom | `Call` é handle de Binder |
| Recusar chamada | escolher `reject(int)` sem checar versão | branch: `reject(reason)` em 34+, `reject(false, null)` abaixo | `REJECT_REASON_*` é 34+ e `minSdk` é 29 |
| Normalizar/mascarar número na tela | qualquer regex nova | `LibPhoneNumberNormalizer` + `PhoneMask` | funções únicas do projeto |
| Trabalho pós-chamada | `WorkManager`, escopo novo | `appScope` do `AppContainer` | proibido/vaza (regra da Phase 5) |

**Key insight:** como a Phase 5, esta fase é majoritariamente **fiação e apresentação**. O motor de
decisão não muda (o CONTEXT está certo: ramo novo no motor = erro de desenho), a triagem passa a
receber contatos só por segurarmos o papel, e a plataforma já implementa a degradação. O código
genuinamente novo é: o serviço fino, o store, o coordenador puro, a notificação de chamada e a UI.

---

## Common Pitfalls

### Pitfall 1 — Achar que a Activity de `ACTION_DIAL` basta para o papel
**Medido:** `add-role-holder` falha com rc=255. É a primeira coisa que trava a fase, e o erro do
sistema (`RuntimeException: Failed`) não diz o motivo. Declarar o `InCallService` junto.

### Pitfall 2 — Declarar só um dos dois intent-filters de `ACTION_DIAL`
`DefaultDialerManager` aplica dois filtros em sequência: `ACTION_DIAL` com esquema vazio **e**
`ACTION_DIAL` com `tel:`. Passar num só não é passar.

### Pitfall 3 — `setComponentEnabledSetting` para "desligar" o modo discador
O javadoc é explícito: o `RoleManager` remove o papel **e fecha o app**. Reversão é pelo seletor do
sistema.

### Pitfall 4 — Reaproveitar o canal de notificação da Phase 5
Aquele canal é `IMPORTANCE_LOW` por decisão travada, e a importância é imutável após a criação. A
tela cheia exige `IMPORTANCE_HIGH` ou maior. São dois canais.

### Pitfall 5 — Engolir exceção no `InCallService` "para não quebrar"
Inverte a lógica da Phase 5 e produz o pior resultado: o Telecom acha que estamos vivos, não faz o
fallback, e o usuário fica com uma tela morta e uma chamada tocando. Falhar alto é a degradação
correta aqui.

### Pitfall 6 — Testar mudo/viva-voz chamando `setMuted` no Service
**Medido:** sem `Phone` vinculado, `setMuted` não lança e `getCalls()` devolve `[]`. O teste passa
sem provar nada. Provar na costura `CallControls`, com dublê.

### Pitfall 7 — `Call.reject(int)` em `minSdk 29`
API 34+. Sem branch de versão, `NoSuchMethodError` em Android 10–13 — e o emulador API 35 do CI
nunca mostra isso. Cobrir com `@Config(sdk = [29])`, como a Phase 5 fez para a tradução.

### Pitfall 8 — DTMF sem `stopDtmfTone`
Contrato explícito do javadoc. Toque rápido em duas teclas ou saída da tela com o dedo pressionado
quebra o pareamento. Invariante assertável.

### Pitfall 9 — `else -> {}` no mapa de estados
`STATE_HOLDING`, `STATE_SELECT_PHONE_ACCOUNT` (dual SIM — o Galaxy do usuário) e
`STATE_AUDIO_PROCESSING` não estão no escopo da UI, mas **chegam**. Mapa exaustivo com um estado
"não suportado" visível e com botão de encerrar funcional.

### Pitfall 10 — Reflexão desnecessária no harness
A Phase 5 precisou de `Proxy` + campo privado porque `respondToCall` só é observável por lá.
`onCallAdded` é público e `Call` é mockável (**medido**). Reflexão aqui é complexidade sem ganho.

### Pitfall 11 — `USE_FULL_SCREEN_INTENT` esquecida na matriz e no script
A Phase 4 já provou que uma permissão nova gera **dois** vermelhos em `verify-invariants.sh`
(`ALLOWLIST` e a lista `FUTURE` de fases futuras). Aqui entram **quatro** permissões/atributos novos
(`CALL_PHONE`, `BIND_INCALL_SERVICE`, `USE_FULL_SCREEN_INTENT` + o papel), e três deles estão hoje
na variável `FUTURE`. Um único commit precisa ajustar manifest, `ALLOWLIST`, `FUTURE` e
`docs/PERMISSOES.md`.

### Pitfall 12 — Auto-sabotagem por grep (recorrente nas Phases 3, 4 e 5 — quatro executores)
`scripts/verify-invariants.sh` casa literais em `app/src/main/java`. Um KDoc que escreva o nome de
uma permissão proibida derruba o bloco sem defeito real. **Descrever proibições em prosa
portuguesa, nunca com o identificador.** Esta fase escreve muito KDoc sobre permissões — o risco é
maior que o normal.

### Pitfall 13 — `UP-TO-DATE` mascarando teste novo
Aconteceu **nesta pesquisa**: a primeira execução das sondas voltou `testDebugUnitTest UP-TO-DATE`
com zero saída, e o resultado só apareceu com `--rerun-tasks`. É a mesma lição das Phases 1 e 3, e
ela continua ativa.

---

## State of the Art

| Premissa registrada no projeto | Realidade verificada | Impacto |
|---------------------------------|----------------------|---------|
| CONTEXT: "o emulador não reproduz troca de discador padrão de forma fiel" | **Falso nos pontos que importam** — papel, chamada de entrada/saída, triagem, morte mid-call e reversão são todos reproduzíveis (§Descoberta 6) | Muito mais cobertura automatizada; só viva-voz e One UI vão para a Phase 9 |
| CONTEXT: falha do `InCallService` pode deixar o usuário sem telefone | **Falso para crash/morte** — fallback automático do Telecom, medido. Verdadeiro para **UI travada**, que ninguém tinha considerado (§Descoberta 2) | O esforço de degradação muda de alvo |
| CONTEXT/`PERMISSOES.md`: as permissões da fase são 3 | São **4** — falta `USE_FULL_SCREEN_INTENT` (§Descoberta 4) | Atualizar a matriz **antes** do código |
| `PERMISSOES.md`: "a lista exata de requisitos de elegibilidade é confirmada na pesquisa da Fase 6" | **Confirmada e isolada por experimento** (§Descoberta 1) | Substituir a nota pela lista |
| `LIMITACOES.md` item 8: número privado "só tem efeito real no modo discador (Fase 6)" | **Não verificado** — ver Open Question 1 | Não prometer na UI antes de medir |
| STATE: "modo discador é o maior risco técnico do MVP" | Continua sendo o de maior superfície, mas o **risco de deixar sem telefone caiu muito** | Repriorizar o esforço para a UI travada, não para o crash |
| Phase 5: papel de discador não destrava `setSkipCallLog` | **Reconfirmado em execução** com o papel ativo | Texto de ativação honesto |

---

## Open Questions

1. **O modo discador destrava mesmo o número privado/restrito (SCR-04)?**
   - Sabemos: no modo filtro `PRESENTATION_RESTRICTED` **não** é entregue ao `CallScreeningService`
     (Phase 5). `docs/LIMITACOES.md` item 8 afirma que no modo discador vale.
   - Não sabemos: se a triagem passa a receber essas chamadas por segurarmos o papel, ou se apenas o
     `InCallService` as vê (o que seria tarde demais para bloquear).
   - Por que não foi medido: o console do emulador não simula `gsm call` com apresentação
     restrita de forma confiável.
   - Recomendação: **não prometer na UI**. Tratar como cenário de Phase 9 e manter `LIMITACOES.md`
     item 8 com a ressalva de que não está verificado.

2. **Comportamento da One UI ao trocar o app de telefone.**
   - Sabemos: no AOSP a troca é limpa e a reversão devolve o padrão nativo (medido).
   - Não sabemos: se a Samsung insere confirmação extra, se o app nativo se reapropria do papel após
     atualização do sistema, e se a otimização de bateria agressiva mata o `InCallService`.
   - Recomendação: cenários de Phase 9. **Nenhum hack preventivo** — regra do `CLAUDE.md`.

3. **Numeração dos cenários físicos — defeito de documento encontrado.**
   - `docs/TESTE-FISICO-SAMSUNG.md` **já tem** os cenários **23–30** dedicados ao modo discador, na
     seção "Modo discador (executar após a Fase 6)". As Phases 3–5 adicionaram cenários 31–51 em
     seções herdadas, criando duas numerações que se cruzam.
   - O CONTEXT manda "continuar a partir de 52", o que **duplicaria** cobertura já escrita.
   - Recomendação: **revisar 23–30 no lugar** (eles já estão certos em espírito) e usar **52+** só
     para o que é novo nesta pesquisa. Sugestão na §Validation Architecture.

4. **Quantos `InCallService` o Telecom vincula por chamada?**
   - Observado: além do nosso, `com.google.android.bluetooth/BluetoothInCallService`,
     `com.google.android.gms/BankScamCallDetectionService` e um serviço de legendas do
     `com.google.android.as`.
   - Consequência: **só quem tem `IN_CALL_SERVICE_UI` mostra a tela**; os outros observam. Não há
     conflito, mas explica ruído no logcat durante a depuração e deve estar no KDoc para não assustar
     o próximo executor.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (JVM puro) | JUnit 4 `4.13.2` + MockK + `kotlinx-coroutines-test` + Turbine — AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| Framework (JVM + plataforma real) | **Robolectric `4.16.1` com `@Config(sdk = [35])`**; `[29]` para provar o piso do `minSdk` (branch de `Call.reject`). `[36]` é **impossível** em JDK 17 |
| Framework (instrumentado) | `AndroidJUnitRunner` + `androidx.test:rules` (`ServiceTestRule`), AVD `Medium_Phone_API_35` |
| Config file | `app/build.gradle.kts` (`testOptions.unitTests`, bloco `kover`) |
| Quick run command | `./gradlew testDebugUnitTest` |
| Instrumented command | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` |
| Full suite command | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| Cobertura | Kover `0.9.9`, gate `koverVerify minBound(80)` (atual **97,64%**). `CallSessionCoordinator` deve ser **puro** e entrar sem exclude, como o `ScreeningCoordinator`. Excludes novos só por **nome de classe** e só no último plano |
| **Dependências novas** | **nenhuma** — Robolectric, MockK, coroutines-test e `ServiceTestRule` já estão declarados. Um plano que acrescente dependência está errado |
| Pré-requisito de ambiente | AVD `Medium_Phone_API_35` (recriado nesta pesquisa em `~/.android/avd/`). A máquina **não** tem `cmdline-tools`/`avdmanager` — se o AVD sumir, é preciso recriar o `config.ini` à mão |

### Ferramental de emulador validado nesta pesquisa

| Necessidade | Comando verificado |
|-------------|--------------------|
| Conceder o papel de discador (com verificação de elegibilidade real) | `adb shell cmd role add-role-holder android.app.role.DIALER org.sentinela.app` |
| Remover o papel | `adb shell cmd role remove-role-holder android.app.role.DIALER org.sentinela.app` |
| Consultar | `adb shell cmd role get-role-holders android.app.role.DIALER` |
| Override de discador padrão (bypassa a qualificação — **não** serve para testar elegibilidade) | `adb shell telecom set-default-dialer <pkg>` |
| Chamada de **entrada** simulada | `TOKEN=$(cat ~/.emulator_console_auth_token)`; `(echo "auth $TOKEN"; echo "gsm call 5551234567") \| nc localhost 5554` |
| Chamada de **saída** | `adb shell am start -a android.intent.action.CALL -d tel:5551234567` |
| Matar o app mid-call | `adb shell am force-stop org.sentinela.app` |
| Limpar chamadas presas | `adb shell telecom cleanup-stuck-calls` |
| Conferir concessão de permissão | `adb shell dumpsys package org.sentinela.app \| grep -i FULL_SCREEN` |

### Phase Requirements → Test Map

| Req | Comportamento | Tipo | Comando | Existe? |
|-----|---------------|------|---------|---------|
| DIA-01 | Manifest declara os 2 filtros de `ACTION_DIAL` + o `InCallService` exported com `BIND_INCALL_SERVICE` | estrutural (manifest **mergeado**) | `bash scripts/verify-invariants.sh` (bloco novo) | ❌ Wave 0 |
| DIA-01 | Permissões novas dentro da allowlist e **nenhuma** fora | script | `bash scripts/verify-invariants.sh` | ✅ estender (ALLOWLIST + FUTURE) |
| DIA-01 | `isRoleHeld` / `isRoleAvailable` / `buildRequestIntent` do papel de discador | Robolectric (`ShadowRoleManager`) | `--tests "*DialerRoleManagerTest"` | ❌ Wave 0 |
| DIA-01 | **Papel concedido de verdade** (prova de elegibilidade) | instrumentado | `--tests "*DialerRoleEligibilityTest"` (usa `cmd role add-role-holder` via shell identity) | ❌ Wave 0 |
| DIA-01 | Estado do modo vem de `isRoleHeld`, **nunca** de flag persistida | JVM | `--tests "*DialerModeStateTest"` | ❌ Wave 0 |
| DIA-02 | Mapa **exaustivo** `Call.STATE_*` → `CallUiState`, sem `else` mudo (13 estados) | JVM parametrizado | `--tests "*CallStateMapperTest"` | ❌ Wave 0 |
| DIA-02 | Atender / recusar / encerrar chegam à costura `CallControls` | JVM (dublê) | `--tests "*CallSessionCoordinatorTest"` | ❌ Wave 0 |
| DIA-02 | `Call.reject` usa a sobrecarga certa por nível de API | Robolectric `sdk=[29,35]` | `--tests "*CallRejectCompatTest"` | ❌ Wave 0 |
| DIA-02 | Mudo e viva-voz alteram o estado observável (**na costura**, não no Service) | JVM | `--tests "*CallSessionCoordinatorTest"` | ❌ Wave 0 |
| DIA-02 | DTMF: todo `play` tem `stop` pareado; tom novo nunca antes do anterior parar | JVM (lista ordenada de eventos) | `--tests "*DtmfPairingTest"` | ❌ Wave 0 |
| DIA-02 | `Call.Callback` registrado em `onCallAdded` e **removido** em `onCallRemoved` | Robolectric (Service real + `mockk<Call>`) | `--tests "*SentinelaInCallServiceTest"` | ❌ Wave 0 |
| DIA-02 | Canal da chamada é `IMPORTANCE_HIGH`+ e **distinto** do canal da Phase 5 | Robolectric (`ShadowNotificationManager`) | `--tests "*IncomingCallNotifierTest"` | ❌ Wave 0 |
| DIA-02 | `setFullScreenIntent` presente; `PendingIntent` com as flags corretas | Robolectric | `--tests "*IncomingCallNotifierTest"` | ❌ Wave 0 |
| DIA-02 | `canUseFullScreenIntent() == false` → degrada para heads-up com ações | Robolectric | `--tests "*IncomingCallNotifierTest"` | ❌ Wave 0 |
| DIA-03 | Discagem usa `TelecomManager.placeCall`, **nunca** `ACTION_CALL` | JVM/estrutural | `--tests "*DialerPlaceCallTest"` + bloco do script | ❌ Wave 0 |
| DIA-03 | `CALL_PHONE` pedida em runtime, reusando `RuntimePermissionAsk` da Phase 5 | JVM | `--tests "*CallPhonePermissionTest"` | ❌ Wave 0 |
| DIA-03 | Discagem sem o papel se comporta (não trava, não promete) | JVM | `--tests "*DialerScreenStateTest"` | ❌ Wave 0 |
| DIA-04 | **Nenhum ramo novo no `CallDecisionEngine`** | script (bloco 7, já existe) | `bash scripts/verify-invariants.sh` | ✅ existente |
| DIA-04 | Política de contato aplicada de fato com o papel de discador | instrumentado (emulador + `gsm call`) | `--tests "*DialerScreeningIntegrationTest"` | ❌ Wave 0 |
| DIA-05 | Reversão: remover o papel devolve o nativo e o modo filtro segue triando | instrumentado | `--tests "*DialerRoleReversionTest"` | ❌ Wave 0 |
| DIA-05 | Reversão cancela notificação e fecha a tela de chamada | JVM | `--tests "*DialerModeStateTest"` | ❌ Wave 0 |
| DIA-05 | **Nenhum `setComponentEnabledSetting` no código** | script (bloco novo, descrito **em prosa**) | `bash scripts/verify-invariants.sh` | ❌ Wave 0 |
| Degradação | Exceção no caminho da UI **propaga** (não é engolida) | JVM (matriz de injeção, molde do 05-03) | `--tests "*CallSessionFailureTest"` | ❌ Wave 0 |
| Degradação | Chamada `RINGING` sem UI apresentada em prazo curto → falha alta | JVM (`runTest` + `TestDispatcher`) | `--tests "*CallSessionWatchdogTest"` | ❌ Wave 0 |
| Degradação | Morte do processo mid-call não derruba a chamada | instrumentado | `--tests "*InCallServiceDeathTest"` | ❌ Wave 0 |
| QLT-06 | Bind real do `InCallService` (binder não-nulo) | instrumentado (`ServiceTestRule`) | `--tests "*InCallServiceBindTest"` | ❌ Wave 0 |
| QLT-06 | Service usa o container do app, nunca constrói um segundo | instrumentado | `--tests "*InCallServiceBindTest"` | ❌ Wave 0 |
| Privacidade | Nenhum número completo em log da camada de chamada | JVM/estrutural | `--tests "*CallLoggingPrivacyTest"` + bloco do script | ❌ Wave 0 |

**Manual-only (Phase 9):** viva-voz e roteamento de áudio real (o emulador só suporta
`ROUTE_SPEAKER` — medido), qualidade de áudio, comportamento da One UI ao trocar e reverter o app de
telefone, tela de chamada sobre a tela bloqueada do Galaxy, dual SIM / `STATE_SELECT_PHONE_ACCOUNT`,
otimização de bateria matando o serviço, e número privado/restrito (Open Question 1).

### Sampling Rate

- **Após cada commit de task:** `./gradlew testDebugUnitTest` (< 40 s; classes Robolectric ~2 s cada).
- **Após cada wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
  `koverVerify` só depois que o exclude do último plano entrar; até lá, `./gradlew koverLog`.
- **Phase gate:** suíte JVM **e** instrumentada verdes pós-`clean` com `--no-build-cache`,
  `N actionable tasks: M executed` com **M > 0**. Arquivado em `06-EVIDENCE.md`.
- **Cuidado medido nesta pesquisa:** `testDebugUnitTest --tests "*X"` voltou **UP-TO-DATE** com zero
  saída na primeira execução de uma classe nova. Toda evidência precisa de `--rerun-tasks` ou de
  `clean`.
- **Prova de vermelho obrigatória** para cada guarda-corpo: mapa exaustivo de estados, pareamento de
  DTMF, watchdog de exibição, não-engolir-exceção e ausência de número completo em log. Quebrar, ver
  falhar, restaurar.

### Wave 0 Gaps

- [ ] Atualizar `docs/PERMISSOES.md` com `USE_FULL_SCREEN_INTENT` — **leitura/edição bloqueante,
      antes de qualquer manifest**
- [ ] `scripts/verify-invariants.sh`: `ALLOWLIST` ganha `CALL_PHONE`, `BIND_INCALL_SERVICE` e
      `USE_FULL_SCREEN_INTENT`; a variável `FUTURE` perde as três — **mesmo commit do manifest**
- [ ] `SentinelaInCallService` (fino) + `CallSessionStore` no `AppContainer` + `CallSessionCoordinator`
      **puro** — pré-requisito de quase toda a suíte
- [ ] Costura `CallControls` (atender/recusar/encerrar/mudo/viva-voz/DTMF) com dublê de teste
- [ ] `FakeCall` / builder MockK de `Call` e `Call.Details` (**receita medida nesta pesquisa**;
      `Call.Details` tem construtor público de 21 parâmetros — reflexão é desnecessária)
- [ ] `app/src/test/.../telecom/CallStateMapperTest.kt` — 13 estados, exaustivo
- [ ] `app/src/test/.../telecom/CallSessionCoordinator*Test.kt` — controles, falhas, watchdog
- [ ] `app/src/test/.../notifications/IncomingCallNotifierTest.kt` — canal, full-screen, degradação
- [ ] `app/src/androidTest/.../telecom/InCallServiceBindTest.kt` — QLT-06
- [ ] `app/src/androidTest/.../telecom/DialerRole*Test.kt` — elegibilidade e reversão
- [ ] `app/src/androidTest/.../telecom/InCallServiceDeathTest.kt` — degradação medida
- [ ] Blocos novos em `verify-invariants.sh` (manifest do discador; ausência de desabilitação de
      componente; ausência de `ACTION_CALL`) — descritos **em prosa**, nunca com o identificador
      (Pitfall 12)
- [ ] Instalação de framework: **nenhuma**

### Cenários propostos para a Phase 9

> ⚠️ Ver Open Question 3: os cenários **23–30** de `docs/TESTE-FISICO-SAMSUNG.md` já cobrem o modo
> discador. A recomendação é **revisá-los no lugar** e usar 52+ apenas para o que é novo.

| # | Cenário | Ação | Esperado |
|---|---------|------|----------|
| 52 | Viva-voz e roteamento real | Em chamada ativa, alternar viva-voz, fone e Bluetooth | Áudio troca de rota; a UI reflete a rota ativa. **Único ponto de DIA-02 impossível no emulador** (`supportedRouteMask: SPEAKER` apenas) |
| 53 | Tela cheia sobre a tela bloqueada da One UI | Aparelho bloqueado, receber chamada de contato | A tela de chamada do Sentinela aparece por cima; atender e recusar funcionam com o aparelho travado |
| 54 | `USE_FULL_SCREEN_INTENT` revogada | Revogar em Configurações e receber chamada | Degrada para heads-up com ações de atender/recusar; **nunca** fica em silêncio |
| 55 | Crash mid-call em hardware real | Com chamada ativa, `adb shell am force-stop org.sentinela.app` | A chamada **continua**; o sistema assume com o discador da Samsung e mostra o aviso. Confirma no aparelho o que foi medido no emulador |
| 56 | Dual SIM / `STATE_SELECT_PHONE_ACCOUNT` | Com dois chips, originar chamada sem SIM padrão definido | Estado tratado com tela informativa e botão de encerrar funcional — **nunca** tela em branco |
| 57 | Papel roubado por atualização do sistema | Após atualização da One UI ou instalação de outro discador, reabrir o app | A home detecta a perda na retomada e degrada para modo filtro sem alarme e sem quebrar |
| 58 | Otimização de bateria agressiva | Colocar o Sentinela em "suspender atividade" da Samsung e receber chamada | Registrar se o `InCallService` ainda é vinculado. Se não for, é limitação de OEM para `LIMITACOES.md`, não bug |
| 59 | Número privado/restrito no modo discador | Ligar com identificação bloqueada | **Registrar** se chega à triagem ou só ao `InCallService`. Resolve a Open Question 1 e decide o texto do item 8 de `LIMITACOES.md` |
| 60 | Histórico nativo como discador padrão | Bloquear uma chamada com o modo discador ativo e abrir o histórico da Samsung | A chamada **aparece** como bloqueada. Confirma no aparelho que o papel de discador não destrava o pulo do registro |

---

## Sources

### Primary (HIGH — fonte lida diretamente)

- `~/Library/Android/sdk/sources/android-35/android/telecom/InCallService.java` — requisitos do
  papel, exemplo de manifest, meta-data, fallback para o discador pré-instalado, regra de emergência,
  exemplo de notificação de chamada recebida, `setMuted`/`setAudioRoute`/`getCallAudioState`
- `.../android/telecom/Call.java` — 13 estados, `Callback`, `answer`/`reject`/`disconnect`/
  `playDtmfTone`/`stopDtmfTone`, `REJECT_REASON_*` (34+), ausência de `@RequiresPermission`
- `.../android/telecom/TelecomManager.java` — `placeCall` com
  `@RequiresPermission(anyOf = {CALL_PHONE, MANAGE_OWN_CALLS})`, nota sobre MMI e discador padrão
- `.../android/telecom/DefaultDialerManager.java` — os **dois** filtros de `ACTION_DIAL`
- `.../android/app/Notification.java` — `CallStyle.forIncomingCall`/`forOngoingCall` (31+),
  javadoc de `setFullScreenIntent` (`USE_FULL_SCREEN_INTENT`, `IMPORTANCE_HIGH`, 60 s)
- `platform/packages/services/Telecomm` `main` — `CallScreeningServiceFilter` (`PACKAGE_TYPE_*`,
  gate de `READ_CONTACTS`), `IncomingCallFilterGraph`, `CallsManager`
- developer.android.com, "Behavior changes: Apps targeting Android 14" — restrição de
  `USE_FULL_SCREEN_INTENT` a apps de chamada e alarme

### Medições executadas nesta pesquisa (HIGH)

Emulador `Medium_Phone_API_35` (`google_apis_playstore`, arm64), recriado do zero:

1. `add-role-holder DIALER` **sem** `InCallService` declarado → **falha, rc=255**
2. `add-role-holder DIALER` **com** `InCallService` declarado → **sucesso, rc=0**
3. Chamada de entrada com papel de discador e **sem** papel de triagem → `SCREENING_BOUND` +
   `SCREENING_COMPLETED [Reject]` + `blocked call, rejecting`
4. Chamada bloqueada **nunca** atinge `onCallAdded` (ausência total de log da sonda)
5. Com **os dois** papéis no mesmo pacote → continuam **2** filtros e **1** bind (sem triagem dupla)
6. `blocked call, adding to call log` + `LogCall; logged` **com o papel de discador** → reconfirma o
   no-op de `setSkipCallLog`
7. Chamada de saída → `onCallAdded state=9 dir=1` → `onStateChanged 1` → `4`
8. Segunda chamada → primeira vai a `ON_HOLD` (estado 3)
9. `am force-stop` mid-call → chamadas **sobrevivem** (`ACTIVE` + `ON_HOLD`); Telecom religa em
   `com.google.android.dialer/...InCallServiceImpl` via `EmergencyInCallServiceConnection`
10. Chamada seguinte após a morte → bind volta para a nossa sonda
11. `remove-role-holder DIALER` → padrão volta ao nativo; papel de triagem **sobrevive**; modo filtro
    segue bloqueando
12. `ACTION_DIAL` sem o papel → resolve para o Google Dialer, **não** para nós
13. `USE_FULL_SCREEN_INTENT: granted=true` no install (targetSdk 37, app com `InCallService`);
    `CALL_PHONE: granted=false`
14. `supportedRouteMask: SPEAKER` apenas — viva-voz não é verificável no emulador

JVM/Robolectric (`@Config(sdk = [35])`), 5 testes verdes:

15. `Robolectric.buildService(InCallService)` funciona
16. `mockk<Call>` funciona apesar de `Call` ser `final`; `answer`/`disconnect`/`playDtmfTone`
    verificáveis
17. `svc.onCallAdded(mockCall)` direto no Service real funciona — **sem reflexão**
18. `Call.Details` tem **2 construtores públicos** (aridade 0 e 21)
19. `setMuted` sem `Phone` **não lança**; `getCalls()` devolve `[]` (armadilha de teste vacuoso)

*Todas as alterações de sonda foram revertidas; `git status` está limpo exceto pelos documentos de
planejamento.*

### Secondary (MEDIUM)

- Resumo automatizado de `CallsManager.java` (construção dos três `CallScreeningServiceFilter`) —
  o resumo trouxe erro de digitação no nome da classe, então a **substância** foi aceita apenas
  porque o comportamento correspondente foi **observado em execução** (medições 3 e 5)

---

## Metadata

**Confidence breakdown:**

- Elegibilidade ao `ROLE_DIALER`: **HIGH** — isolada por experimento controlado (A × B) + javadoc
- Ciclo de vida do `InCallService`: **HIGH** — transições observadas em execução + fonte AOSP
- APIs de controle e permissões: **HIGH** — anotações lidas na fonte; `CALL_PHONE` confirmado
  `granted=false` no install
- Degradação / fallback do Telecom: **HIGH** — medido em chamada ativa, com log do sistema
- Triagem no modo discador: **HIGH** — medida com e sem o papel de triagem
- `USE_FULL_SCREEN_INTENT`: **HIGH** — javadoc + doc de behavior change + concessão medida
- Estratégia de teste: **HIGH** — harness executado, não teorizado
- Tela cheia funcionando de fato sobre a tela bloqueada: **MEDIUM** — o mecanismo está confirmado;
  a experiência real não foi observada (emulador headless)
- Número privado/restrito no modo discador: **LOW** — Open Question 1, não verificado
- Comportamento Samsung / One UI: **LOW** — nada verificável fora do aparelho; virou roteiro, não
  código

**Research date:** 2026-07-29
**Valid until:** 2026-08-28 (30 dias — as APIs de Telecom são estáveis; reavaliar se o `compileSdk`,
o `targetSdk` ou a versão do Robolectric mudarem)

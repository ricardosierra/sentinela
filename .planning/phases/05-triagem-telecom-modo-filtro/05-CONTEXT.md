# Phase 5: Triagem Telecom Modo Filtro - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

**Esta é a fase central do produto.** Chamada de número desconhecido é bloqueada **de verdade**,
antes de tocar, com o Service fino, resiliente e dentro do orçamento.

Entregas:
- `UnknownCallScreeningService` real: monta `ScreenedCall`, consulta configurações + contatos +
  whitelist com timeout interno, decide via `CallDecisionEngine` e traduz `CallDecision` para
  `CallResponse`.
- Garantia dura de `respondToCall` **exatamente 1×** em todos os caminhos, inclusive exceção
  e timeout.
- Papel `ROLE_CALL_SCREENING` solicitado e observado.
- `BlockedCallNotifier` real: notificação própria, silenciosa, opt-in, **depois** da resposta.
- Gravação no histórico (repositório da Phase 3), também depois da resposta.
- Bench de decisão medido.

Fora do escopo: modo discador / `ROLE_DIALER` (Phase 6), telas de onboarding e home (Phase 7),
telas de whitelist e histórico (Phase 8).

</domain>

<decisions>
## Implementation Decisions

### Resposta única e resiliência

- **Garantia de 1×:** `AtomicBoolean` por chamada **mais** um `try/finally` que responde de forma
  permissiva se nada respondeu. Testado com exceção **injetada em cada ponto** do caminho, não
  apenas no topo. Este é o critério 3 do ROADMAP e o invariante mais importante da classe.
- **Timeout interno de 1 s** para o conjunto das consultas locais — 5× de folga sobre o limite de
  5 s da plataforma. Ao estourar, decide com `ContactLookup.UNAVAILABLE` e deixa a `FallbackPolicy`
  (que já existe e é testada desde a Phase 2) resolver. Não inventar caminho novo.
- **Exceção inesperada → PERMITIR a chamada.** Bloquear por bug é pior que deixar passar: o
  usuário perde uma ligação importante e não tem como descobrir o motivo. O valor do produto é
  não interromper, não bloquear a qualquer custo.
- **O Service usa o `ContactLookupRepository` real**, não `MISS` hardcoded. Isto deixou de ser
  precaução e virou **obrigatório**: a pesquisa provou que, com `READ_CONTACTS` concedida na
  Phase 4, contatos **passam a chegar** ao `onScreenCall`. Um `MISS` hardcoded bloquearia a
  ligação de um contato.
- **`snapshot()`, `contains()` e `lookup()` são todos `suspend`, e `onScreenCall` roda na main
  thread.** `runBlocking` está fora de questão. O `containsBlocking` e o `snapshot()` não-suspend
  mencionados em fases anteriores não existem na forma assumida — conferir as assinaturas reais
  antes de planejar.

### Tradução `CallDecision` → `CallResponse`

- `Reject` → `setDisallowCall(true)` + `setRejectCall(true)` + `setSkipCallLog` conforme
  configuração + `setSkipNotification(true)`.
- `Silence` → `setSilenceCall(true)` **sem** `disallowCall`: toca mudo, a tela de chamada aparece
  e o registro entra no log nativo. É comportamento diferente de bloquear — não confundir.
- `SendSilentlyToVoicemail` → `disallowCall` + `rejectCall` + `skipNotification`. **A ida à caixa
  postal depende da operadora** — a UI e a documentação **não podem prometer** que sempre cai lá.
- `BlockWithoutTrace` → `disallowCall` + `rejectCall` + `skipNotification(true)`.
  **CORRIGIDO PELA PESQUISA:** `setSkipCallLog` é **no-op** para este app (ver achados abaixo) —
  não é variação de OEM. Enviar o campo é inofensivo, mas nem o rótulo nem a documentação podem
  afirmar ausência de rastro.
- `Allow` → resposta vazia (não interferir).

### Notificação e histórico

- **Ordem inegociável:** `respondToCall` **primeiro**, sempre. Notificação e histórico depois, em
  corrotina desacoplada que **não pode** atrasar nem derrubar a resposta. Uma falha ao gravar o
  histórico jamais pode virar uma chamada não respondida.
- **Notificação desligada por padrão** — opt-in explícito. O valor do produto é "não interromper";
  ligar notificação por padrão contradiz isso.
- **`POST_NOTIFICATIONS` pedida em runtime somente quando o usuário liga a notificação**, nunca no
  onboarding. A permissão já está **declarada** no manifest desde a Phase 1 (`docs/PERMISSOES.md`
  é a fonte canônica e autoriza a declaração na Fase 1, com o pedido em runtime nesta fase).
- **Conteúdo:** número **mascarado** por padrão (`PhoneMask.mask`, a mesma função única), com opção
  de "sem identificação" para quem não quer nada visível na tela bloqueada. Nunca o número completo.
- Canal com `IMPORTANCE_LOW`: sem som, vibração, heads-up ou full-screen.

### Onde cada critério é provado — DECISÃO DO USUÁRIO (2026-07-29)

Escolhido: **bench + emulador agora, comportamento de OEM na Phase 9.**

- **Agora, falhando o build:** bench da decisão com assert na **mediana** (nunca em percentil de
  cauda — a Phase 3 já teve de realocar um p95 flaky, e a Phase 4 mediu 30 ms virando 140 ms sem
  mudar uma linha); testes instrumentados do Service no emulador cobrindo a tradução de resposta,
  a garantia de 1× e a ordem das operações.
- **Phase 9, roteiro Samsung:** o bloqueio real de chamada (critérios 1 e 2), o comportamento de
  `skipCallLog`, a interação com Não Perturbe e a caixa postal (critério 6). São exatamente os
  pontos que dependem de OEM e que o emulador não reproduz.
- O p95 < 200 ms continua sendo o compromisso declarado do produto — apenas é **verificado em
  hardware real**, não afirmado no emulador. A mediana é que trava o build.
- Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`.

### Achados da pesquisa reforçada e decisões do usuário (2026-07-29)

A pesquisa leu o AOSP (`frameworks/base` + `packages/services/Telecomm`) e derrubou quatro
premissas do projeto. As decisões abaixo são **do usuário** e substituem qualquer texto anterior.

- **SCR-07 (`skipCallLog`, "bloquear sem rastro") é INATINGÍVEL.** `CallScreeningServiceFilter`
  calcula `setShouldAddToCallLog(!skipCallLog || packageType != PACKAGE_TYPE_CARRIER)`. O Sentinela
  é `PACKAGE_TYPE_USER_CHOSEN`, o `||` curto-circuita e a chamada **sempre** entra no log nativo
  como `BLOCKED_TYPE`. Virar `ROLE_DIALER` na Phase 6 **não** destrava — só `CARRIER` é isento.
  **DECISÃO: marcar inatingível e ser honesto na UI.** Registrar em `docs/LIMITACOES.md` com a
  fonte do AOSP; **remover** a opção "sem rastro" da UI ou renomeá-la para descrever o que de fato
  acontece; marcar SCR-07 como *Won't Fix* com justificativa em `REQUIREMENTS.md`. O app nunca
  promete o que não entrega. A `CallDecision.BlockWithoutTrace` pode continuar existindo no
  domínio, mas o rótulo e a documentação não podem afirmar ausência de rastro.
- **SCR-04 (números privados/ocultos) é INATINGÍVEL no modo filtro** —
  `PRESENTATION_RESTRICTED/UNKNOWN/UNAVAILABLE/PAYPHONE` nunca são entregues ao Service.
  **DECISÃO: manter o código e marcar parcial.** A lógica já existe e é testada no motor desde a
  Phase 2; ela vale para o **modo discador** (Phase 6), onde todas as chamadas passam pelo app.
  SCR-04 vira "parcial: só no modo discador", documentado. Não remover a opção.
- **Não Perturbe NÃO pode ser burlado.** O DND é avaliado num filtro paralelo via
  `NotificationManager`; nenhum campo de `CallResponse` o alcança. O único bypass exigiria
  `ACCESS_NOTIFICATION_POLICY`, fora da lista permitida e mutando política global do usuário.
  `NEVER_SILENCE` decidir como `RING` **já está correto** — zero trabalho de implementação. O que
  muda é texto: `docs/LIMITACOES.md` e o rótulo da política precisam parar de sugerir bypass.
- **Contatos AGORA chegam ao `onScreenCall`.** O gate do Telecom é
  `if (contactExists && !hasReadContactsPermission()) não vincula` — a Phase 4 concedeu
  `READ_CONTACTS`, então a premissa de bootstrap ("modo filtro só recebe não-contatos") morreu.
  **Consequência crítica:** o critério 2 saiu da plataforma e passou a ser responsabilidade do
  **nosso** código. Um `MISS` errado agora **bloqueia a ligação de um contato**. O caminho de
  contato precisa de teste próprio e explícito.
- **O Service é 100% testável em JVM** — harness já provado: `Robolectric.buildService(...)` +
  `Call.Details` mockado + um `Proxy` de `ICallScreeningAdapter` injetado no campo privado
  `mCallScreeningAdapter`, capturando e afirmando cada `ParcelableCallResponse`. **Usar
  `@Config(sdk = [35])`** — o `sdk = [36]` registrado no STATE **não funciona** (exige Java 21 e o
  projeto está travado em JDK 17).
- **Chamar `respondToCall` duas vezes NÃO lança nem derruba o processo** — emite dois IPCs em
  silêncio. Ou seja, o `AtomicBoolean` é a **única** proteção que existe. Isso eleva, não reduz,
  a importância do critério 3.
- **Cold start não é o gargalo temido:** caminho quente 23,3 ms, pior soma a frio ~50 ms — ~4× de
  folga sob os 200 ms.

### Defeito a corrigir nesta fase + feature nova (decisão do usuário)

- **DEFEITO:** `SentinelaApp.onCreate` chama `onAppOpened()`, que roda em **todo** start de
  processo — inclusive os disparados pelo Telecom numa chamada recebida. Chamadas recebidas estão
  sendo contadas como "aberturas do app", corrompendo ENG-01 e o convite de avaliação da Phase 9.
  **DECISÃO: corrigir agora.** Contar abertura apenas quando houver Activity, não em start de
  processo.
- **FEATURE NOVA — chamada repetida toca (pedida pelo usuário, 2026-07-29):**
  > "duas ligações seguidas deve tocar, caso habilitado nas configurações (vem habilitado por padrão)"

  Se o **mesmo número** ligar novamente pouco depois de ter sido bloqueado, a segunda chamada
  **toca**. Racional: emergência real insiste; spam automatizado normalmente não. Regras:
  - **Habilitada por padrão** (`repeatedCallBypassEnabled = true` em `ScreeningSettings`).
  - A janela de tempo é configurável em código; padrão sugerido **5 minutos** — o executor pode
    ajustar, mas o valor precisa ser uma constante nomeada e testada, nunca literal solto.
  - A fonte da informação é o **histórico já existente** (`BlockedCallRepository`, Phase 3), que
    grava `numberE164` + `timestampUtcMillis`. Não criar armazenamento novo.
  - **A regra vive no `CallDecisionEngine`**, não no Service — é regra de decisão, e o `CLAUDE.md`
    proíbe condição de bloqueio espalhada. O motor recebe um dado novo de entrada (houve bloqueio
    recente deste número?), decide, e continua puro e determinístico.
  - **Precedência:** o bypass entra **depois** de contato e whitelist (que já tocam) e **antes** da
    política de desconhecidos — é uma exceção que faz tocar, nunca que faz bloquear.
  - Consulta ao histórico entra no mesmo orçamento e no mesmo timeout interno; falha de consulta
    → trata como "não houve chamada repetida" e segue a política normal.
  - Precisa de requisito próprio em `REQUIREMENTS.md` (não existe hoje) e de casos na matriz
    parametrizada do motor.

### Claude's Discretion

- Estrutura interna do Service, nome das classes auxiliares, como o timeout é implementado e a
  organização dos testes ficam a critério do executor, desde que os invariantes acima e os 6
  critérios de sucesso sejam honrados.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets — tudo que o Service precisa já existe e está testado
- `app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt` — precedência de 7 níveis,
  48 casos parametrizados + casos de borda. O Service **não** deve conter regra de decisão.
- `app/src/main/java/org/sentinela/app/domain/` — `CallDecision` (Allow/Silence/Reject/
  SendSilentlyToVoicemail/BlockWithoutTrace), `DecisionReason`, `ScreenedCall`,
  `ScreenedNumber.Private/.Invalid`, `CallDirection`, `ContactLookup`, `WhitelistLookup`.
- `app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt` + `PhoneMask` — chave
  E.164 (ou dígitos crus para códigos curtos) e a **função única** de máscara para log e UI.
- `app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt` — `snapshot()`
  com cache `@Volatile`, feito para o caminho frio do Service.
- `app/src/main/java/org/sentinela/app/data/contacts/` — repositório real com sonda dupla e cache
  de chaves; devolve UNAVAILABLE sem permissão.
- `app/src/main/java/org/sentinela/app/data/local/` — whitelist (`containsBlocking` não-suspend,
  índice provado por `EXPLAIN QUERY PLAN`) e histórico com retenção.
- `app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt` — stub do papel.
- `app/src/main/java/org/sentinela/app/notifications/BlockedCallNotifier.kt` — interface pronta.
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — DI manual com `by lazy` e `onAppOpened()`.

### Established Patterns — lições acumuladas que valem para esta fase
- **Cronômetro não prova estrutura.** Toda afirmação de "usa X" precisa de prova determinística
  (contador, `EXPLAIN QUERY PLAN`, etc.), nunca de tempo.
- **Assert primário na mediana**, cauda só reportada. Percentil de cauda em emulador é flaky.
- **Todo guarda-corpo precisa de prova de vermelho:** quebrar de propósito, ver falhar, restaurar.
- Evidência só vale com `clean` **e** `--no-build-cache`.
- Testes que leem arquivo do disco podem ir UP-TO-DATE/FROM-CACHE e dar falso verde — declarar
  inputs no Gradle.
- Armadilha recorrente: o próprio comentário/KDoc ditado pelo plano casando com um grep de
  contagem-zero do mesmo plano. Descrever identificadores proibidos em prosa.
- `connectedDebugAndroidTest` **não aceita `--tests`**; usar
  `bash scripts/run-instrumented-tests.sh --tests "*AlgumTest"`.
- DI manual; nada de Hilt/Koin/Dagger/WorkManager.
- Kover com gate `minBound(80)`; classes que só rodam instrumentadas são excluídas **por nome**,
  nunca por pacote. Ampliar filtro só no último plano da fase.

### Integration Points
- `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` — hoje pass-through
  com um TODO detalhado que descreve exatamente o que esta fase deve construir.
- `app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt` — pedido do papel.
- `app/src/main/AndroidManifest.xml` — `POST_NOTIFICATIONS` já declarada; `BIND_SCREENING_SERVICE`
  e o intent-filter já registrados desde a Phase 1.
- `scripts/verify-invariants.sh` — allowlist de permissões, pureza de `domain/`+`phone/`, guardas
  de privacidade (Bloco 6). Invariante novo desta fase entra aqui.
- `docs/TESTE-FISICO-SAMSUNG.md` — já tem os cenários 31–39; os desta fase entram na sequência.

</code_context>

<specifics>
## Specific Ideas

- O `TODO(Fase 5)` já no `UnknownCallScreeningService` descreve a construção esperada. Ele é um bom
  ponto de partida, mas não substitui o plano.
- O risco desta fase é **regressão silenciosa**: um caminho que não responde, ou que responde duas
  vezes, não aparece em teste de caminho feliz. Daí a exigência de injetar exceção em cada ponto.
- O produto **nunca** deve afirmar bloqueio "100% garantido", nem que filtra WhatsApp/VoIP — regra
  do `CLAUDE.md` que vale especialmente para os textos que esta fase introduzir.

</specifics>

<deferred>
## Deferred Ideas

- Modo discador (`ROLE_DIALER` + `InCallService`) e políticas por contato individual — Phase 6.
- Telas de onboarding, home e Proteção — Phase 7.
- Telas de whitelist e histórico — Phase 8.
- Validação do bloqueio real, `skipCallLog`, Não Perturbe e caixa postal em Samsung físico,
  além do p95 < 200 ms em hardware — Phase 9.

</deferred>

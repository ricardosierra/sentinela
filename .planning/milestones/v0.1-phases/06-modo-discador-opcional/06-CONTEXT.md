# Phase 6: Modo Discador Opcional - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

O usuário que **optar** pode tornar o Sentinela o telefone padrão — o que habilita as políticas
por origem também para **contatos** — com experiência de chamada própria e **reversão limpa**.

Entregas:
- Ativação do modo discador via `ROLE_DIALER`, com explicação honesta e `READ_CONTACTS` exigida.
- `InCallService` com UI própria: chamada recebida, chamada ativa, atender, recusar, encerrar,
  mudo, viva-voz e teclado DTMF.
- Tela de discagem com handler de `ACTION_DIAL` (requisito de elegibilidade ao papel).
- Políticas por contato passando a valer de fato.
- Reversão para o discador nativo sem quebrar telefonia; modo filtro segue operante.

Fora do escopo: onboarding e home (Phase 7), whitelist e histórico (Phase 8), chamadas
simultâneas/em espera/conferência.

**Este é o maior risco técnico do MVP** (registrado no `STATE.md`). Pesquisa reforçada é
obrigatória antes do planejamento: elegibilidade ao papel, ciclo de vida do `InCallService`,
e comportamento da One UI ao trocar o app de telefone.

</domain>

<decisions>
## Implementation Decisions

### Escopo da UI — DECISÃO DO USUÁRIO (2026-07-29), corrigindo a proposta inicial

> "Ui minima, precisa ser UI completa e polida desde o inicio. Entreguei um desenho visual pra
> isso. Servir de base"

- **A UI desta fase é completa e polida desde o início**, não um mínimo funcional a ser refinado
  depois. Isso **substitui** a recomendação original de "só o mínimo que o `InCallService` exige".
- **Base visual:** o design system "Silent Guardian" em [`docs/design/DESIGN.md`](../../../docs/design/DESIGN.md)
  e os mockups entregues em `docs/design/telas/` (8 telas: boas-vindas, onboarding, dashboard,
  whitelist, histórico e as três de configuração), com os tokens já implementados em
  `app/src/main/java/org/sentinela/app/ui/theme/`.
- **Ponto a confirmar com o usuário se ele discordar:** os mockups entregues **não incluem** tela
  de chamada nem de discagem. A instrução foi lida como *"o desenho entregue serve de base"* —
  ou seja, derivar as telas novas do mesmo sistema visual (paleta `#081425`/`#ADC6FF`, Inter para
  texto e **Geist para os números de telefone**, grid de 8dp, radius 8/16/24, chips e CTAs em
  pill, camadas tonais em vez de sombra pesada, glassmorphism com parcimônia), com acabamento
  equivalente ao das telas existentes. Nada de placeholder, nada de "refina depois".
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

### Ativação e reversão

- **`READ_CONTACTS` é pré-requisito**: sem ela as políticas por contato não funcionam e o app
  passaria a bloquear contatos. O modo discador não é oferecido enquanto a permissão não for
  concedida.
- **Texto de ativação honesto e explícito.** Deve dizer o que muda (o Sentinela vira seu telefone;
  a tela de chamada passa a ser a dele; a triagem passa a ver todas as chamadas) **e o que não
  melhora**: o registro no histórico do telefone continua acontecendo (provado na Phase 5 —
  o `ROLE_DIALER` **não** destrava isso, só apps de operadora são isentos) e o Não Perturbe
  continua valendo. Nada de texto vendedor.
- **Reversão:** botão que abre o seletor do sistema; o app **nunca** força a troca. Depois de
  reverter, o modo filtro continua operante sem reinstalação nem reconfiguração.
- **Perda silenciosa do papel:** detectar na abertura e na home, e degradar para modo filtro sem
  quebrar nada nem alarmar.

### Políticas por contato e risco

- **Nada muda no `CallDecisionEngine`.** A precedência já trata `ContactLookup.HIT` com
  `contactsPolicy` desde a Phase 2, com 48 casos parametrizados. No modo discador essa regra
  simplesmente **passa a ser exercida**, porque todas as chamadas chegam ao app. Qualquer ramo
  novo no motor nesta fase é sinal de erro de desenho.
- **Padrão da política de contatos continua Tocar.** Ativar o modo discador não pode, sozinho,
  começar a bloquear contatos.
- **Falha no `InCallService` não pode deixar o usuário sem telefone.** Precisa de caminho de
  degradação testado e documentado — é a pior falha possível desta fase.
- **Onde cada coisa é provada:** lógica, ciclo de vida e tradução de estado em JVM/Robolectric
  (a Phase 5 provou que `Robolectric.buildService` + proxy do adapter funciona para Service de
  telecom); a telefonia real — atender, viva-voz, DTMF, reversão, comportamento da One UI — vai
  para o roteiro Samsung da **Phase 9**, continuando a numeração dos cenários (hoje em 51).
  O emulador não reproduz troca de discador padrão de forma fiel.
- Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`.

### Permissões

- Entram **nesta fase e só nela**: `ROLE_DIALER`, `BIND_INCALL_SERVICE` e `CALL_PHONE`,
  conforme `docs/PERMISSOES.md`. Cada uma entra no manifest **e** na allowlist do
  `scripts/verify-invariants.sh` **no mesmo commit**, com a doc conferida antes — regra registrada
  desde a Phase 1. Lembrar que a Phase 4 provou que adicionar uma permissão gera **dois**
  vermelhos no script (allowlist e lista de fases futuras).

### Achados da pesquisa reforçada (2026-07-29) — medidos, não supostos

A pesquisa criou um AVD do zero, virou discador padrão e dirigiu chamadas reais (14 medições),
além de ler o AOSP local. Duas decisões deste contexto foram **falsificadas**.

- **Elegibilidade ao `ROLE_DIALER` isolada por experimento controlado:** só a activity de
  `ACTION_DIAL` → `cmd role add-role-holder DIALER` **falha (rc=255)**. Somando o `<service>` do
  `InCallService` → **rc=0**. Os **dois** filtros de `ACTION_DIAL` (esquema vazio **e** `tel:`) são
  exigidos por `DefaultDialerManager`. O manifest mínimo completo está no `06-RESEARCH.md`.
- **O "pior fracasso" da fase já é resolvido pela plataforma, e o perigo real é o oposto.**
  `am force-stop` durante chamada ATIVA **não** derrubou a ligação: o Telecom registrou
  `onDisconnected` e religou no discador do sistema via `EmergencyInCallServiceConnection`; as
  chamadas seguiram `ACTIVE`/`ON_HOLD` e a seguinte voltou para nós. **Morrer é seguro; travar
  não.** Uma UI vinculada mas congelada o Telecom não detecta e não substitui.
  → **Consequência de desenho: o `InCallService` deve falhar ALTO (crash rápido), o inverso da
  rede permissiva da Phase 5.** O esforço de degradação muda de alvo: não é "não morrer", é
  "nunca travar vivo".
- **DIA-04 (política por contato) não precisa de código novo.** Com `ROLE_DIALER` em nossas mãos e
  `ROLE_CALL_SCREENING` com ninguém, o Telecom ainda vinculou nosso serviço de triagem e honrou o
  `Reject`. A triagem roda **antes** do `onCallAdded`, então chamada bloqueada nunca chega à UI de
  chamada. Ter os dois papéis produz **um** bind — sem risco de dupla triagem.
- **Testabilidade resolvida, e melhor que na Phase 5:** cinco sondas Robolectric verdes.
  `buildService(InCallService)` funciona, `mockk<Call>` funciona apesar de `Call` ser `final`
  (`answer`/`disconnect`/`playDtmfTone` verificáveis), e `onCallAdded` pode ser invocado direto no
  Service real — **sem reflexão**, diferente do proxy de `ICallScreeningAdapter` da Phase 5.
  **Armadilha de teste vacuoso:** `setMuted()` num Service sem `Phone` é **no-op silencioso** —
  mudo e viva-voz precisam ser provados na costura, não na chamada direta.
- **FALSIFICADO — "o emulador não reproduz troca de discador padrão de forma fiel".** É falso onde
  importa: concessão e remoção do papel, chamadas de entrada (`gsm call`) e saída, a máquina de
  estado completa, a triagem ponta a ponta, morte no meio da chamada e reversão são todos
  reproduzíveis. **Só viva-voz** (`supportedRouteMask` traz apenas `SPEAKER`) **e One UI** exigem
  hardware de verdade. Muito mais desta fase é verificável em CI do que este contexto supôs.
- **Defeito de documento:** `docs/TESTE-FISICO-SAMSUNG.md` **já tem os cenários 23–30** para o modo
  discador. Revisar 23–30 no lugar e usar 52+ apenas para o que é novo — não duplicar.
- **Questão aberta:** não foi verificado se o modo discador realmente destrava números
  privados/restritos (SCR-04). O item 8 de `docs/LIMITACOES.md` hoje **afirma** que destrava.
  Não prometer isso na UI até haver prova.

### Permissão adicional — DECISÃO DO USUÁRIO (2026-07-29)

- A tela cheia obrigatória da chamada recebida exige **`USE_FULL_SCREEN_INTENT`** (Android 14+),
  que **não constava** da lista fechada de `docs/PERMISSOES.md`. Medida como `granted=true` na
  instalação, porque o app qualifica como app de chamadas.
- **DECISÃO: autorizada e documentada.** Entra na matriz de `docs/PERMISSOES.md` como permissão da
  **Fase 6**, restrita ao modo discador, com a justificativa funcional: sem ela, com a tela
  bloqueada, o usuário não vê a chamada chegando — é o mecanismo que o próprio Android indica
  para telefonia (`Notification.CallStyle` + `fullScreenIntent`). Continua **proibido**
  `SYSTEM_ALERT_WINDOW`.
- `CALL_PHONE` foi medida como `granted=false` e **precisa ser pedida** em runtime.
- Cada permissão entra no manifest **e** na allowlist do `verify-invariants.sh` no mesmo commit,
  com `docs/PERMISSOES.md` atualizado **antes**. São agora **quatro**: `ROLE_DIALER`,
  `BIND_INCALL_SERVICE`, `CALL_PHONE`, `USE_FULL_SCREEN_INTENT`.

### Número na tela — DECISÃO DO USUÁRIO (2026-07-29)

- **Nas telas de chamada e de discagem o número aparece COMPLETO.** Ali o número é o produto: o
  usuário precisa reconhecer quem liga para decidir atender.
- **Em todo o resto continua mascarado:** log, notificação, histórico e crash report usam
  exclusivamente `PhoneMask.mask`. A fronteira é testável e deve ser travada por invariante.

### Claude's Discretion

- Organização dos arquivos de UI, nomes dos composables, estrutura da máquina de estado da
  chamada e como o `InCallService` se comunica com a UI ficam a critério do executor, desde que
  os 5 critérios de sucesso passem e o acabamento visual seja equivalente ao dos mockups.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/org/sentinela/app/ui/theme/` — `Color.kt` (26 tokens Silent Guardian),
  `Theme.kt` (`DarkColors`, `SentinelaTheme`), `Type.kt`. Travados por `ThemeTokensTest`.
- `docs/design/DESIGN.md` + `docs/design/telas/*/` — sistema visual e os 8 mockups de referência
  (HTML + PNG). **Não há mockup de chamada nem de discagem** — derivar do sistema.
- `app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt` — motor completo, incluindo
  `contactsPolicy` e a regra SCR-12 de chamada repetida. **Não alterar nesta fase.**
- `app/src/main/java/org/sentinela/app/telecom/ScreeningCoordinator.kt` — puro, com prazo interno
  de 1 s, resposta única e rede permissiva dupla.
- `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` — camada fina,
  57 linhas, delega ao coordenador.
- `app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt` — consulta e pedido do
  papel de triagem; base para o papel de discador.
- `app/src/main/java/org/sentinela/app/data/contacts/` — lookup com sonda dupla e cache de chaves.
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — DI manual, singletons `by lazy`,
  `ScreeningDependencies`.
- `app/src/main/java/org/sentinela/app/ui/MainActivity.kt` — hoje com `PlaceholderScreen()`.

### Established Patterns — lições acumuladas
- **Cronômetro não prova estrutura**; assert primário na **mediana**, cauda só reportada.
- **Todo guarda-corpo precisa de prova de vermelho:** quebrar, ver falhar, restaurar.
- Evidência só vale com `clean` **e** `--no-build-cache`.
- Testes que leem arquivo do disco vão UP-TO-DATE e dão falso verde — declarar inputs no Gradle.
- **Armadilha que já pegou quatro executores:** o próprio KDoc/comentário ditado pelo plano casando
  com um grep de contagem-zero do mesmo plano. Descrever identificadores proibidos em prosa.
- `connectedDebugAndroidTest` **não aceita `--tests`**; usar `scripts/run-instrumented-tests.sh`.
- Robolectric: `@Config(sdk = [35])` — `[36]` exige Java 21 e o projeto está em JDK 17.
- DI manual; nada de Hilt/Koin/Dagger/WorkManager.
- Kover `minBound(80)`; excluir por **nome de classe**, nunca por pacote; ampliar filtro só no
  último plano da fase. Cobertura atual: 97,6%.
- Strings sempre em `res/values/strings.xml` (pt-BR); nenhum texto hardcoded em Kotlin.
- Nenhum número completo em log — sempre `PhoneMask.mask`.

### Integration Points
- `app/src/main/AndroidManifest.xml` — `ROLE_DIALER`, `BIND_INCALL_SERVICE`, `CALL_PHONE`,
  o `InCallService` e os intent-filters de `ACTION_DIAL`/`tel:`.
- `scripts/verify-invariants.sh` — allowlist de permissões e os 7 blocos de invariantes.
- `docs/PERMISSOES.md` — leitura **bloqueante** antes de tocar no manifest.
- `docs/LIMITACOES.md` — destino das limitações desta fase (uma chamada por vez, log nativo).
- `docs/TESTE-FISICO-SAMSUNG.md` — cenários vão de 1 a 51; os desta fase continuam a partir de 52.
- `docs/design/TELAS.md` — precisa ganhar as telas novas de chamada e discagem.

</code_context>

<specifics>
## Specific Ideas

- O usuário pediu explicitamente acabamento completo desde o início nesta fase. Tratar polimento
  visual como critério de aceite, não como refino futuro.
- A Phase 5 já provou que o `ROLE_DIALER` **não** destrava o `setSkipCallLog`. O texto de ativação
  não pode insinuar o contrário — é a mesma regra de honestidade que motivou o plano 05-07.
- O maior risco não é a UI: é deixar o usuário sem telefone funcional. Degradação e reversão
  merecem mais teste que a tela bonita.

</specifics>

<deferred>
## Deferred Ideas

- Chamadas simultâneas, em espera e conferência — fora do MVP.
- Onboarding e home — Phase 7 (que também vai integrar a ativação do modo discador ao fluxo).
- Whitelist e histórico — Phase 8.
- Validação de telefonia real, viva-voz, DTMF, reversão e comportamento One UI em Samsung
  físico — Phase 9, cenários a partir de 52.

</deferred>

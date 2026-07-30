# Phase 7: UI Onboarding e Home - Context

**Gathered:** 2026-07-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Usuário sai do zero ao protegido em **menos de 2 minutos**, entendendo o que o app faz, o que cada
política significa e o que o modo discador oferece.

Entregas:
- Onboarding: boas-vindas, explicação, pedido do papel `ROLE_CALL_SCREENING`, política de
  desconhecidos, política de contatos (com pedido de `READ_CONTACTS`), tratamento da whitelist,
  opt-in de notificação e verificação final — com os defaults dos mockups.
- Home/dashboard: status real do papel e da proteção, botão de correção funcional, contagem de
  bloqueadas, última bloqueada respeitando privacidade, atalhos.
- Tela Proteção: altera cada configuração — incluindo o modo discador — com explicação clara e
  efeito imediato na triagem.
- TalkBack navega o fluxo inteiro; strings 100% em resources pt-BR.

Fora do escopo: telas de whitelist e histórico (Phase 8); apoio/avaliação/privacidade/release
(Phase 9). As telas de chamada e discagem já existem (Phase 6).

**Vantagem desta fase:** os mockups reais já existem em `docs/design/telas/` para exatamente estas
telas — boas-vindas, onboarding, dashboard e as três de configuração. Diferente da Phase 6, aqui
não há tela a derivar: há tela a implementar com fidelidade.

</domain>

<decisions>
## Implementation Decisions

### Fluxo do onboarding

- **Pode ser pulado.** Botão "pular" leva à home com os defaults dos mockups aplicados. Forçar o
  usuário a atravessar o fluxo é dark pattern, proibido pelo `CLAUDE.md`.
- **Se o papel for negado**, seguir para a home mostrando o **estado real** e o botão de correção.
  Nunca travar num passo nem repetir o pedido em loop.
- **Modo discador NÃO entra no onboarding.** É opcional, é o maior risco técnico do produto e muda
  o telefone padrão do usuário — pertence à tela Proteção, apresentado sem pressa e com a copy
  honesta que a Phase 6 já produziu. A tela `DialerActivationScreen` já existe como composable puro
  com quatro callbacks; **esta fase a liga à navegação** (pendência registrada em 06-05).
- **Aparece só na primeira abertura.** Depois, tudo está acessível na tela Proteção. Não reexibir a
  cada atualização.

### Home e estado real

- **O status vem de consulta viva ao sistema a cada retomada — nunca de flag persistida.** A Phase 6
  mediu que **perder um papel do sistema mata o processo**
  (`Killing <pid> (adj 0): Permission or app op changed`), inclusive quando o próprio usuário troca
  o app de telefone nas configurações. Estado guardado mente. `DialerModeState` já é derivado assim
  — a home segue o mesmo princípio para o papel de triagem.
- **"Última bloqueada" na home usa número MASCARADO** (`PhoneMask.mask`). A home pode estar visível
  a terceiros. A fronteira estabelecida na Phase 6 continua valendo: número completo **somente** nas
  telas de chamada e discagem.
- **Proteção ativa sem `READ_CONTACTS`:** avisar explicitamente que contatos podem ser tratados como
  desconhecidos, com atalho para conceder. A Phase 5 provou que contatos chegam ao `onScreenCall`
  quando a permissão existe — sem ela, o lookup devolve `UNAVAILABLE` e cai na `FallbackPolicy`.
  Silenciar isso deixaria o usuário sem entender por que perdeu uma ligação.
- **Contagem de bloqueadas** vem do histórico local. Se o histórico estiver **desligado**, mostrar
  esse estado em vez de `0` — zero seria mentira, e mentira em número é pior que ausência de número.

### Tela Proteção e efeito imediato

- **Efeito imediato, sem botão "salvar".** O `snapshot()` do DataStore com cache `@Volatile` já
  alimenta a triagem; a mudança vale na próxima chamada.
- **Explicação curta e permanente sob cada opção**, não tooltip escondido. O objetivo declarado da
  fase é o usuário *entender* o que cada política significa.
- **Confirmar só o que perde dado** (ex.: limpar histórico). Trocar política não pede confirmação —
  é reversível e confirmação excessiva treina o usuário a clicar em "sim" sem ler.
- **Honestidade herdada, escrita na tela:** o registro no histórico do telefone **sempre** acontece
  (AOSP, provado na Phase 5 — e o `ROLE_DIALER` não destrava), o Não Perturbe **não** é contornável,
  e o app **não** filtra WhatsApp/VoIP. Omitir isso "para não assustar" seria exatamente a desonestidade
  que o plano 05-07 existiu para corrigir. As strings honestas já existem desde a Phase 6.

### Acessibilidade e strings

- **TalkBack navega o fluxo inteiro** — é critério de sucesso 4, não item de qualidade opcional.
  Alvos ≥ 48dp, `contentDescription` em todo controle, estado nunca comunicado só por cor.
- **100% das strings em `res/values/strings.xml` (pt-BR)**, nada hardcoded em Kotlin. A Phase 6 já
  adicionou 74 chaves; reaproveitar antes de criar.
- Lição da Phase 6 para os testes de toque: **precisam de dois eixos.** O Compose expande a área de
  toque ao mínimo da plataforma por conta própria, então afirmar só a área de toque mede a garantia
  da biblioteca, não o nosso layout — afirmar também o tamanho **desenhado**. Isso pegou quatro bugs
  reais de layout na Phase 6 (um botão de 72dp comprimido a 23dp, outro a altura zero).
- Testes de composição precisam de qualificadores reais de tela (`w411dp-h891dp-xxhdpi`) — o
  dispositivo padrão do Robolectric é pequeno demais e produz falha por motivo falso.

### Achados da pesquisa (2026-07-30) — medidos no repositório

- **Zero dependência a adicionar.** `navigation-compose 2.9.8`, `lifecycle-viewmodel-compose 2.11.0`,
  `lifecycle-runtime-compose 2.11.0` e `savedstate 1.4.0` **já estão no APK** desde o bootstrap,
  declaradas e nunca consumidas. A pergunta "adicionar navegação viola a regra de cold start?"
  se dissolve — não há o que adicionar, e o cold start medido já as inclui.
- **Navegação type-safe é falso-verde de compilação — reproduzido.** `composable<Route>()` com
  `@Serializable` compila limpo e **estoura em runtime**:
  `SerializationException: Serializer for class 'X' is not found`. O `javap` confirma: sem
  `$serializer`, sem `Companion`. O Kotlin embutido do AGP 9 **não** traz o plugin de serialização,
  então `@Serializable` compilou como anotação vazia. **Usar rotas por string** — isto é
  guarda-corpo, não nota de pé de página.
- **A consulta viva do papel é gratuita:** o trio por retomada mede **p50 29,9 µs**, máximo 284 µs
  em 200 amostras — três ordens de grandeza abaixo de um frame. Não existe argumento de performance
  para cachear, e cachear compraria de volta a mentira que este contexto proíbe. E o **callback de
  resultado do pedido de papel é insuficiente**, porque perder papel mata o processo (medido 3× na
  Phase 6): o callback nunca roda justamente no caminho que importa.
- **Supressão de lint da Phase 1 — o número real é 133** (131 strings + 1 cor + 1 mipmap), e a
  premissa era errada: com `UnusedResources` reabilitado, `lintDebug` **saiu 0** — é severidade de
  aviso e `warningsAsErrors` não está ligado. A supressão nunca foi o que mantinha o build verde.
  Estreitar via `app/lint.xml` com `<ignore regexp>` foi **testado: 133 → 81**, silenciando
  exatamente os 52 que pertencem às Phases 8–9. ~79 pertencem a esta fase.
- **O fluxo multi-tela é testável em JVM:** `NavHost` real sob `createComposeRule` + Robolectric
  funciona — `navigate`, `popUpTo`, `currentDestination` e `currentBackStack` verificados. Diferente
  da Phase 6, o núcleo desta fase **não precisa de emulador**. Detalhe medido: a entrada `NavGraph`
  tem rota `null`, então asserção de back-stack precisa de `mapNotNull`.
- **Cold start: mediana 680 ms** (612–917 ms, 8 execuções). Nada precisa ser diferido. Duas regras a
  preservar: **nunca** tocar `contactLookupRepository` na UI desta fase (a construção do cache leva
  2,57 s) e **nunca** usar `runBlocking` para escolher a `startDestination`.
- **Wave 0 real:** as três asserções de toque de dois eixos vivem **dentro** de
  `CallScreenSemanticsTest.kt`, no pacote de chamada. As telas desta fase estão em outros pacotes —
  extrair para arquivo de apoio neutro, **não duplicar** (cópias deixariam o eixo com dentes divergir).
- **Dois defeitos incidentais achados:** `strings.xml:266` tem `%` não escapado
  (`dialer_activation_unchanged_4`) — crash latente se algum dia for formatada; e
  `app/build.gradle.kts` tem um bloco `testImplementation` **duplicado**.

### Copy dos mockups — DECISÃO DO USUÁRIO (2026-07-30)

Os mockups entregues contêm cinco afirmações que o MVP **não** entrega: "Base Global — milhões de
números", "processamento local criptografado", "filtros inteligentes", "Provável Fraude Financeira"
e "seguro contra spam conhecido". Duas telas também carregam imagens de `googleusercontent.com`,
impossível sem `INTERNET`.

> "Substituir por copy honesta e o que ficou de fora adicionar em planos futuros do GSD para versões
> depois do MVP"

- **No MVP:** manter layout e visual dos mockups, trocando **somente os textos** por equivalentes
  verdadeiros. A Phase 1 já escreveu essas substituições em `strings.xml`. Imagens remotas viram
  superfície tonal, como o próprio mockup do passo 1 do onboarding já faz.
- **Pós-MVP:** as cinco capacidades estão registradas, uma a uma e com dependências, em
  [`docs/backlog/capacidades-prometidas-nos-mockups.md`](../../../docs/backlog/capacidades-prometidas-nos-mockups.md),
  indexado em `docs/INDEX.md`. Cada uma vira planejamento próprio depois do `v0.1.0` — a de base
  global depende do backend já previsto como `v0.2.0`.
- **Regra permanente:** enquanto a capacidade não existir e não estiver medida, a UI não pode
  sugerir que existe. Mesmo princípio que já governa `docs/LIMITACOES.md`.

### Switch do card de proteção — DECISÃO DO USUÁRIO (2026-07-30)

- O switch principal da home liga/desliga a **preferência `protectionEnabled`** (configuração
  própria, efeito imediato na triagem), **não** o papel do sistema.
- O papel do sistema aparece como **estado somente-leitura** num banner separado, com botão de
  correção que abre o seletor. Motivo concreto: revogar papel **mata o processo** — o usuário veria
  o app fechar sozinho ao desligar um switch.

### Cores novas fixas fora do Dynamic Color

- `StatusAttention`, `OnStatusAttention` e `StatusBlocked` entram como **literais**, pelo mesmo
  motivo da Phase 6: `Theme.kt` substitui o esquema **inteiro** por `dynamicDark/LightColorScheme`
  em API 31+, então o papel de parede decidiria a diferença entre "proteção ativa" e "desligada".
  `CallAccept`/`OnCallAccept` são reaproveitados.

### Claude's Discretion

- Estrutura de navegação, organização dos composables, nomes de ViewModel/state holder e como o
  onboarding guarda o progresso ficam a critério do executor, desde que os 4 critérios de sucesso
  passem e a fidelidade aos mockups seja mantida.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets — muita coisa pronta
- `docs/design/telas/{boas_vindas_ao_sentinela,onboarding,dashboard,configura_o_desconhecidos,
  configura_o_contatos,configura_o_whitelist}/` — mockups HTML + PNG **destas exatas telas**.
- `docs/design/DESIGN.md` + `docs/design/TELAS.md` (§11 reescrita na Phase 6 e serve de modelo de
  contrato) + `app/src/main/java/org/sentinela/app/ui/theme/` (Color, Theme, Type, Shapes).
- `app/src/main/java/org/sentinela/app/ui/components/` — nove componentes acessíveis da Phase 6.
- `app/src/main/java/org/sentinela/app/ui/dialer/DialerActivationScreen.kt` — composable puro com
  quatro callbacks, **precisa ser ligado à navegação nesta fase**.
- `app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt` +
  `platform/SystemRoleGate.kt` + `telecom/call/DialerModeState.kt` — consulta e pedido de papéis.
- `app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt` — todas as
  configurações, o contador de aberturas e o flag `contacts_permission_asked`.
- `app/src/main/java/org/sentinela/app/data/contacts/ContactsPermissionState.kt` +
  `RuntimePermissionAsk` — máquina de estado genérica de permissão em runtime (4 estados, pura).
- `app/src/main/java/org/sentinela/app/data/local/` — whitelist e histórico (contagem, recentes).
- `app/src/main/java/org/sentinela/app/phone/PhoneMask.kt` — máscara única.
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — DI manual; `onAppOpened()` já existe.
- `app/src/main/java/org/sentinela/app/ui/MainActivity.kt` — hoje ainda com `PlaceholderScreen()`;
  já conta a abertura do app com guarda de `savedInstanceState == null`.

### Established Patterns — lições acumuladas
- **Cronômetro não prova estrutura**; assert primário na **mediana**, cauda só reportada.
- **Todo guarda-corpo precisa de prova de vermelho**, e a sabotagem deve incidir sobre código **já
  commitado** — um executor da Phase 6 perdeu 74 strings usando `git checkout` sobre trabalho novo.
- Evidência só vale com `clean` **e** `--no-build-cache`.
- **Armadilha que já pegou seis executores:** o próprio KDoc/comentário casando com um grep de
  contagem-zero do mesmo plano. E o Bloco 2 (applicationId literal) já pegou **três** executores
  escrevendo o literal em comentário ou em nome totalmente qualificado — usar imports, descrever em
  prosa.
- `connectedDebugAndroidTest` **não aceita `--tests`**; usar `scripts/run-instrumented-tests.sh`.
- Robolectric `@Config(sdk = [35])`; `[36]` exige Java 21 e o projeto está em JDK 17.
- DI manual; nada de Hilt/Koin/Dagger/WorkManager.
- Kover `minBound(80)`, cobertura atual **96,7%**; excluir por **nome de classe**, nunca por pacote;
  ampliar filtro só no último plano da fase.

### Integration Points
- `app/src/main/java/org/sentinela/app/ui/MainActivity.kt` — ponto de entrada da navegação.
- `AppContainer` — fonte dos repositórios e dos gates de papel.
- `scripts/verify-invariants.sh` — 8 blocos, 38 checagens; invariante novo entra aqui.
- `docs/design/TELAS.md` — precisa refletir as telas desta fase.
- `docs/TESTE-FISICO-SAMSUNG.md` — 60 cenários hoje; os desta fase continuam de 61.

</code_context>

<specifics>
## Specific Ideas

- O objetivo "zero ao protegido em menos de 2 minutos" é mensurável: vale contar os passos e os
  toques do fluxo e registrar o número, em vez de afirmar que é rápido.
- Esta é a primeira fase em que o app deixa de ser um esqueleto para o usuário — `MainActivity`
  ainda mostra `PlaceholderScreen()`. É também a fase que finalmente consome as 132 strings pt-BR
  que a Phase 1 teve de suprimir do lint como `UnusedResources`. **Reavaliar aquela supressão aqui**
  é escopo legítimo e estava previsto no contexto da Phase 1.

</specifics>

<deferred>
## Deferred Ideas

- Telas de whitelist e histórico — Phase 8.
- Convite de avaliação, tela de apoio/doação, política de privacidade embutida, release R8 —
  Phase 9.
- Assets de fonte Inter/Geist — pendência registrada em `docs/backlog/`; hoje os estilos numéricos
  caem na monoespaçada do sistema.
- Blocos técnicos do CHANGELOG para as Phases 2–5 — registrado para o fechamento de versão.
- Validação do fluxo em Samsung físico com TalkBack real — Phase 9, cenários a partir de 61.

</deferred>

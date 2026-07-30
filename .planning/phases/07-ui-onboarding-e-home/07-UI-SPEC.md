---
phase: 7
slug: ui-onboarding-e-home
status: draft
shadcn_initialized: false
preset: none
created: 2026-07-30
---

# Phase 7 — Contrato de Design da UI (onboarding, home e Proteção)

> **Natureza deste documento: extração de fidelidade, não derivação.** Os mockups destas telas
> existem em `docs/design/telas/`. Onde o mockup fala, este contrato transcreve. Onde o mockup é
> ambíguo, silencioso ou **desonesto**, este contrato marca o ponto na
> [§12 Ambiguidades](#12-ambiguidades-dos-mockups--proposta-para-o-usuário-corrigir) e propõe —
> nunca inventa em silêncio.
>
> Escopo: **visual, interação, copy e semântica de acessibilidade**. Estrutura de navegação,
> state holders, ViewModels, persistência de progresso e estratégia de teste vivem no
> `07-RESEARCH.md`, produzido em paralelo. Este documento não nomeia biblioteca de navegação
> nem padrão de state holder — por decisão explícita do `07-CONTEXT.md`, isso é discrição do
> executor.

---

## 1. Mapa mockup → tela

| Mockup (`docs/design/telas/…`) | Tela que especifica | Fidelidade |
|---|---|---|
| `boas_vindas_ao_sentinela/{code.html,screen.png}` | **Boas-vindas** (tela 0, pré-onboarding) | estrutura e CTA fiéis; 3 feature-cards e o badge substituídos (§12.3) |
| `onboarding/{code.html,screen.png}` | **Onboarding passo 1 — papel `ROLE_CALL_SCREENING`** | fiel; é a fonte dos 3 feature-cards honestos e do disclaimer |
| `configura_o_desconhecidos/{code.html,screen.png}` | **Onboarding passo 2 — política de desconhecidos** | fiel; contador de passos corrigido (§12.1) |
| `configura_o_contatos/{code.html,screen.png}` | **Onboarding passo 3 — política de contatos + `READ_CONTACTS`** | fiel; descrição de "Nunca Silenciar" corrigida (§12.4) |
| `configura_o_whitelist/{code.html,screen.png}` | **Onboarding passo 4 — tratamento da whitelist** | fiel; imagem remota removida (§12.7) |
| `dashboard/{code.html,screen.png}` | **Home / Início** | layout fiel; rótulo de spam e copy do hero substituídos (§12.5) |
| — **sem mockup** | **Onboarding passo 5 — notificação (opt-in)** | derivado (§6.6) |
| — **sem mockup** | **Onboarding passo 6 — verificação final** | derivado (§6.7) |
| — **sem mockup** | **Tela Proteção** | derivado de `TELAS.md` §10 (§7) |
| — **sem mockup** | **Estados degradados da home** | derivados (§8) |
| `hist_rico_de_bloqueios/`, `whitelist_pessoal/` | **fora desta fase** (Phase 8) | — |

Nenhum mockup cobre a tela de ativação do modo discador: ela **já existe** como
`ui/dialer/DialerActivationScreen.kt` (contrato fechado no `06-UI-SPEC.md`). Esta fase apenas a
**liga** a partir de Proteção (§7, item 9) — não a redesenha.

---

## 2. Design System

| Propriedade | Valor |
|---|---|
| Ferramenta | nenhuma (Android nativo — Jetpack Compose + Material 3) |
| Preset | não se aplica (shadcn é irrelevante em Android) |
| Componentes | `androidx.compose.material3` + `ui/theme/` + `ui/components/` (Phase 6) |
| Ícones | Material Symbols Outlined (`material-icons-extended`), mesma família dos mockups |
| Fontes | escala M3 sobre a família do sistema; Inter/Geist seguem em `docs/backlog/fontes-inter-geist.md` |
| Formas | `SentinelaShapes` / `ShapeSmall` 8dp, `ShapeMedium` 16dp, `ShapeLarge` 24dp, `ShapePill` |
| Tema | `SentinelaTheme` dark-first, Dynamic Color ligado (ver §4.2) |

### 2.1 Inventário reutilizável (nada de duplicar)

Componentes que **já existem** e esta fase consome como estão:

| Componente | Arquivo | Uso nesta fase |
|---|---|---|
| `HonestyCard(title, items, itemIcon, itemIconTint)` | `ui/components/HonestyCard.kt` | card "O que o Sentinela faz / não faz" no passo 1; card de limitações em Proteção; card "O que é a Whitelist?" no passo 4 |
| `InfoBanner(text, actionLabel, onAction)` | `ui/components/InfoBanner.kt` | **todos** os avisos da home: papel negado, `READ_CONTACTS` negada, histórico desligado, papel de discador perdido |
| `SentinelaWatermark()` | `ui/components/SentinelaWatermark.kt` | **não usar** — é marca d'água de tela de chamada; a home tem `TopAppBar` com a marca |
| `DialerActivationScreen(state, onRequestRole, onRevert, onGrantContacts, onBack)` | `ui/dialer/DialerActivationScreen.kt` | destino do item 9 de Proteção; assinatura **não muda** |
| `ShapeSmall/Medium/Large/Pill`, `SentinelaShapes` | `ui/theme/Shape.kt` | todas as telas |
| `Typography.numberXl/numberLg/timer` | `ui/theme/Type.kt` | nenhuma; a home usa número **mascarado** em `bodyLarge` (§5) |
| Tokens `Surface…`, `Primary…`, `Error…`, `CallAccept`/`OnCallAccept` | `ui/theme/Color.kt` | §4 |

Componentes de `ui/call/` (13 arquivos) e `ui/dialer/` (6 arquivos) **não são tocados por esta
fase**. O executor não deve reimplementar option-card, banner nem card de honestidade: os três
padrões já existem ou saem do inventário novo da §11.

---

## 3. Escala de espaçamento

Grid de 8dp, idêntica aos mockups (`spacing` do tailwind.config: xs 4 / sm 8 / md 16 / lg 24 /
xl 32; `margin-mobile` 16).

| Token | Valor | Uso nesta fase |
|---|---|---|
| xs | 4dp | gap ícone↔label em chip, gap título↔descrição de option-card |
| sm | 8dp | gap entre option-cards no passo 2 (mockup `space-y-sm`), gap título↔subtítulo |
| md | 16dp | **margem lateral de toda tela**, padding interno de card, gap entre option-cards nos passos 3 e 4 (`gap-md`/`gap-3`) |
| lg | 24dp | espaço entre blocos (hero ↔ lista de opções, seção ↔ seção da home = mockup `space-y-6`) |
| xl | 32dp | respiro antes do CTA fixo; folga superior/inferior do conteúdo do onboarding (`py-xl`) |

**Exceções declaradas (todas múltiplas de 4):**

| Elemento | Medida | Origem |
|---|---|---|
| `TopAppBar` | altura 64dp (`h-16`) | todos os mockups |
| CTA pill de largura total | altura **56dp** (`h-14`) | boas-vindas, passo 1, passo 4 |
| Botão secundário "Voltar" / "Pular" | altura **48dp** (`h-12`) | passo 4 |
| Círculo do hero (boas-vindas) | 128dp (`w-32 h-32`), ícone 64dp | boas-vindas |
| Círculo do hero (passo 1) | 96dp (`w-24 h-24`), ícone 64dp | passo 1 |
| Círculo do hero (passo 2) | 64dp (`w-16 h-16`), ícone 32dp | passo 2 |
| Ícone-container do option-card | 40dp (passos 2 e 4) / 48dp (passo 3) | mockups |
| Card de estatística da home | altura **128dp** (`h-32`) | dashboard |
| Avatar do "última bloqueada" | 48dp | dashboard |
| Ícone-container de atalho rápido | 40dp, padding do card 20dp (`p-5`) | dashboard |
| Barra de progresso do passo | altura 4dp, largura 96dp | passo 2 |
| Item de bottom nav | alvo **≥ 48dp × 48dp** | ver §12.10 |
| Linha de configuração em Proteção | altura mínima **56dp**; com explicação, altura livre e padding vertical 12dp | `DESIGN.md` |
| Alvo de toque mínimo | **48dp** em todo controle, sem exceção | critério 4 da fase |

**Raios:** card de conteúdo 16dp (`rounded-xl`/`rounded-2xl` dos mockups → `ShapeMedium`);
hero da home e cards de estatística **24dp** (`rounded-3xl` → `ShapeLarge`); CTA e chips de
status **pill**; option-card 16dp (`ShapeMedium`).

---

## 4. Cor

### 4.1 Contrato 60/30/10

| Papel | Token | Uso |
|---|---|---|
| **Dominante (60%)** | `Surface` `#081425` | fundo de todas as telas desta fase |
| **Secundária (30%)** | `SurfaceContainerLow` `#111C2D`, `SurfaceContainer` `#152031`, `SurfaceContainerHigh` `#1F2A3C`, `SurfaceContainerHighest` `#2A3548` | option-cards, cards de estatística, cards de atalho, linhas de Proteção, bottom nav, ícone-containers |
| **Acento (10%)** | `Primary` `#ADC6FF` + `PrimaryContainer` `#4D8EFF` | ver lista de reserva abaixo |
| **Destrutiva** | `Error` `#FFB4AB` / `ErrorContainer` `#93000A` | apenas ícone de "bloqueado", proteção desligada e confirmação de perda de dado |

**Acento reservado exclusivamente para:**

1. Marca no `TopAppBar` (escudo + "Sentinela") — `Primary`.
2. CTA primário de cada tela (um por tela): "Começar Configuração", "Configurar Agora",
   "Próximo", "Finalizar Configuração", "Corrigir configuração".
3. `check_circle` do option-card **selecionado** e sua borda de seleção 2dp.
4. Preenchimento da barra de progresso do passo e o rótulo "Passo N de 6".
5. Card hero da home (`PrimaryContainer` de fundo, `OnPrimaryContainer` de conteúdo) — é o
   único bloco saturado da tela, por desenho do mockup.
6. Número grande das estatísticas ("Total bloqueado") e ícone da aba ativa da bottom nav
   (`SecondaryContainer` de fundo + `OnSecondaryContainer`, conforme mockup).
7. Borda esquerda 4dp do `InfoBanner`.

**Nunca** usar `Primary` em: texto corrido, ícones inativos, borda de container em repouso,
fundo de option-card não selecionado, ícone de atalho rápido.

### 4.2 Cores que precisam ser FIXAS fora do Dynamic Color

Achado da Phase 6 que recorre aqui: `Theme.kt` substitui o **esquema inteiro** por
`dynamicDark/LightColorScheme` em API ≥ 31. Ou seja, **qualquer cor lida de `colorScheme` é
decidida pelo papel de parede**. Para as cores que carregam *significado*, isso é inaceitável:
um wallpaper laranja pode fazer o indicador de "proteção ativa" e o de "proteção desligada"
ficarem indistinguíveis.

Cores desta fase que **devem** vir de constantes de `Color.kt`, nunca de `colorScheme`:

| Significado | Token | Valor | Justificativa |
|---|---|---|---|
| Proteção **ativa** (dot de status, ícone `verified_user` do hero) | `CallAccept` (reuso) | `#1E6E42` | o mockup usa `bg-green-500`, literal, exatamente porque é semântico |
| Conteúdo sobre o verde de status | `OnCallAccept` (reuso) | `#D9F2E3` | contraste ≥ 7:1 |
| Proteção **desligada** / atenção (fundo do hero em OFF, ícone de aviso) | **`StatusAttention` (novo)** | `#93000A` | hoje seria `colorScheme.errorContainer` — dinâmico |
| Conteúdo sobre atenção | **`OnStatusAttention` (novo)** | `#FFDAD6` | contraste ≥ 7:1 |
| Ícone `phone_disabled` do item "última bloqueada" | **`StatusBlocked` (novo)** | `#FFB4AB` sobre `#93000A` @ 30% | "bloqueado" é semântico e aparece ao lado de estados neutros |

`CallAccept`/`OnCallAccept` já existem. Os três novos entram em `Color.kt` **sem alterar
nenhum token existente** (`ThemeTokensTest` trava os atuais) e são documentados no KDoc com o
motivo — não como preferência estética, como requisito de segurança de leitura.

**Regra geral, verificável:** todo par cor/significado desta fase satisfaz (a) valor fixo, e
(b) um segundo canal não-cromático — ícone distinto **e** texto de estado. O mockup do
dashboard já faz isso ("dot verde" **+** "Monitoramento em tempo real"); mantido e estendido a
todos os estados.

---

## 5. Fronteira do número de telefone (herdada, não negociável)

Estabelecida no `06-UI-SPEC.md` §"Regra de privacidade":

| Superfície | Formato |
|---|---|
| Tela de chamada, discagem, DTMF (Phase 6) | número **completo** |
| **Home — "última chamada bloqueada"** | **`PhoneMask.mask`** → `+55 11 9****-1234` |
| Home — `contentDescription` do item | **máscara**, lida dígito a dígito só nos dígitos visíveis |
| Onboarding, Proteção, banners, snackbars | nenhum número |
| Log, notificação, crash report | máscara, sempre |

A home é a tela mais provável de estar visível a terceiros (na mesa, no metrô). O mockup do
dashboard **já** exibe mascarado (`+55 11 9****-1234`) — fidelidade e privacidade coincidem
aqui. Origem privada/oculta: `history_private_number` ("Privado") / `history_private_id`
("ID Oculto"); nunca "Desconhecido", nunca dígito inventado.

---

## 6. Onboarding — 6 passos

Fluxo canônico (`TELAS.md` §"Fluxo geral"), com o contador unificado (§12.1):

```
Boas-vindas (tela 0, sem contador)
  → 1/6 papel de triagem
  → 2/6 desconhecidos
  → 3/6 contatos (+ READ_CONTACTS)
  → 4/6 whitelist
  → 5/6 notificação (opt-in)
  → 6/6 verificação final
  → Home
```

### 6.0 Regras válidas em todos os passos

- **Pular sempre disponível.** Ação de texto "Pular" no canto direito do `TopAppBar`
  (`TextButton`, alvo 48dp, `OnSurfaceVariant`), presente em **todos** os passos 1–5. Aplica
  os defaults e vai direto para a home. **Sem diálogo de confirmação e sem tom de censura** —
  confirmar aqui seria o dark pattern que o `CLAUDE.md` proíbe. Não aparece no passo 6, onde
  o único caminho já é "concluir".
- **Nenhum passo é bloqueante.** Papel negado, contatos negados ou notificação negada avançam
  normalmente. Nunca repetir o pedido do sistema em loop: um pedido por passo, por sessão de
  onboarding.
- **Cabeçalho de progresso** (passos 1–6), fixo no `TopAppBar` à esquerda do "Pular":
  rótulo `onboarding_step_indicator` ("Passo %1$d de %2$d") em `labelMedium` `Primary` +
  barra de 4dp × 96dp, trilho `SurfaceContainerHighest`, preenchimento `Primary`,
  `RoundedCornerShape(50)`. A barra é **decorativa**
  (`clearAndSetSemantics {}`) — a informação está no texto, que o TalkBack lê.
- **Transição entre passos:** `slideInHorizontally` + fade, 250 ms `FastOutSlowInEasing`
  (avanço); espelhado no retorno. Com "reduzir animações" ligado, troca instantânea.
- **CTA fixo no rodapé**, altura 56dp, pill, `Primary`/`OnPrimary`, largura total, margem
  lateral 16dp, gradiente `Surface`→transparente de 32dp acima (mockups dos passos 3 e 4).
  Nunca desabilitado por falta de escolha: **todo passo tem default pré-selecionado**.
- **Defaults, extraídos dos mockups e já travados em `ScreeningSettings`:**

  | Passo | Configuração | Default do mockup | Enum |
  |---|---|---|---|
  | 2 | desconhecidos | **Bloquear** (`option-block` recebe `option-selected` no `DOMContentLoaded`) | `OriginPolicy.BLOCK` |
  | 3 | contatos | **Tocar** (badge "Padrão", classe `active`) | `OriginPolicy.RING` |
  | 4 | whitelist | **Nunca Silenciar** (classe `active-ring`) | `OriginPolicy.NEVER_SILENCE` |
  | 3 | bloquear privados | **ligado** (não está no mockup — §12.6) | `blockPrivateNumbers = true` |
  | 5 | notificação | **desligada** | `showOwnNotification = false` |

  Os três primeiros **coincidem** com os defaults de `ScreeningSettings.kt`. O onboarding não
  redefine default nenhum: ele reflete o estado atual do repositório.

### 6.0.1 O `OptionCard` — padrão único dos passos 2, 3 e 4

Os três mockups desenham o mesmo componente com três acabamentos diferentes (§12.2). Contrato
unificado, um componente só:

```
Row (min 72dp de altura, padding 16dp, ShapeMedium)
  [ ícone-container 40dp, circular ]  16dp  [ Column: título + descrição ]  weight  [ check 24dp ]
```

| Estado | Fundo | Borda | Ícone-container | Trailing |
|---|---|---|---|---|
| Não selecionado | `SurfaceContainerLow` | 1dp `OutlineVariant` | `SurfaceContainerHighest`, ícone `OnSurfaceVariant` | `check_circle` outline `Outline` @ 0 de alfa (ocupa espaço, invisível) |
| Selecionado | `SurfaceContainerHigh` | **2dp `Primary`** | `SecondaryContainer`, ícone `OnSecondaryContainer` | `check_circle` **filled** `Primary` |
| Pressionado | ripple padrão + `scale 0.98` por 100 ms | — | — | — |

- Título: `titleMedium` `OnSurface`. Descrição: `bodyMedium` `OnSurfaceVariant` — a descrição é
  **permanente**, nunca tooltip. Badge "Padrão" (`contacts_default_badge`): pill
  `SecondaryContainer`/`OnSecondaryContainer`, `labelSmall`, 8dp à direita do título.
- **Semântica:** o `Row` inteiro é um alvo único com
  `Modifier.selectable(selected, role = Role.RadioButton)` sobre `semantics(mergeDescendants = true)`.
  O grupo dos cards recebe `Modifier.semantics { selectableGroup() }`.
  O `check_circle` é decorativo (`contentDescription = null`) — a seleção é anunciada pelo
  `Role.RadioButton`, não pelo ícone.
- **Armadilha da Phase 6, aplicada aqui:** ao mesclar semântica de um `Row` que contém ícone,
  título, descrição e badge, um estado do filho **desaparece silenciosamente**. Duas
  consequências obrigatórias: (a) nunca colocar um controle interativo dentro de um
  `OptionCard` (o card é o controle); (b) se algum card puder ficar `enabled = false`
  — ver §6.4, opção de contatos sem permissão — o `enabled` vai no **modificador do próprio
  `Row`** e o motivo vai em `stateDescription`, nunca só no filho. Teste de acessibilidade
  deve afirmar `Role.RadioButton` **e** o `stateDescription` de indisponibilidade.
- **Toque:** alvo desenhado ≥ 72dp de altura. Afirmar nos testes **os dois eixos** — área de
  toque **e** tamanho desenhado (lição da Phase 6: só a área de toque mede o Compose, não o
  nosso layout).

---

### 6.1 Boas-vindas (tela 0) — `WelcomeScreen`

Fonte: `boas_vindas_ao_sentinela/`, com os cards do mockup `onboarding/`.

```
TopAppBar 64dp, transparente sobre Surface
  escudo Primary 28dp + "Sentinela" headlineSmall bold Primary      [ ícone info 48dp → Sobre ]
[ 32dp ]
  hero: círculo 128dp SurfaceContainer, borda 2dp Primary @30%, ícone security 64dp Primary
[ 32dp ]
  H1 .............. headlineMedium (28sp/36, w600), OnSurface, centralizado, máx 280dp
                    welcome_headline
[ 16dp ]
  subtítulo ....... bodyLarge OnSurfaceVariant, centralizado, máx 320dp
                    welcome_subtitle
[ 48dp ]
  bento 3 cards ... card largo (col-span-2) + dois cards 1/2, gap 16dp
                    1) verified_user Primary   — welcome_feature_local_*
                    2) notifications_off Secondary — welcome_feature_silent_*
                    3) cloud_off Tertiary      — welcome_feature_offline_*
                    cada card: SurfaceContainerLow, ShapeMedium, borda 1dp OutlineVariant,
                    padding 16dp, ícone-container 40dp, título labelLarge, desc labelMedium
[ 24dp ]
  selo open source  chip pill SurfaceContainerHighest, ícone code 16dp + welcome_open_source (NOVO)
[ ------------ weight(1f) ------------ ]
CTA fixo no rodapé (gradiente 32dp acima):
  botão pill 56dp Primary/OnPrimary: welcome_cta + ícone arrow_forward
[ 16dp ]
  microcopy labelMedium OnSurfaceVariant centralizada: welcome_cta_hint
[ 32dp + navigationBars insets ]
```

- **Sem** o hero fotográfico do mockup e **sem** o badge "Proteção Ativa" sobre ele (§12.7,
  §12.8). O selo open source substitui o badge — é exigência `UIX-13`/`ENG-03` registrada em
  `TELAS.md` §1 e nunca foi implementado.
- Animação `float` do mockup (translate ±10px, 6s): mantida com amplitude **reduzida a 4dp**
  e suprimida quando "reduzir animações" está ligado. É decoração; nada depende dela.
- **Sem** o overlay "Preparando Escudo…" do mockup: não há trabalho a fazer entre a tela de
  boas-vindas e o passo 1. Uma barra de progresso falsa é dark pattern.
- **TalkBack:** ordem `marca → H1 (heading) → subtítulo → cards → selo → CTA → microcopy`.
  H1 com `semantics { heading() }`. O hero e o `float` com `clearAndSetSemantics {}`.
  O ícone `info` do app bar: `contentDescription = about_title`.

**Strings:** existentes `welcome_headline`, `welcome_subtitle`,
`welcome_feature_{local,silent,offline}_{title,desc}`, `welcome_cta`, `welcome_cta_hint`,
`app_name`, `about_title`. **Novas:** `welcome_open_source`.
`welcome_badge_native` **fica sem uso nesta tela** (é do passo 1 — §6.2).

---

### 6.2 Passo 1/6 — papel de triagem — `RoleStepScreen`

Fonte: `onboarding/`.

```
TopAppBar: marca | [ Passo 1 de 6 + barra ] | [ Pular ]
[ 32dp ]
  hero: círculo 96dp PrimaryContainer @20%, ícone shield filled 64dp Primary, float 4dp
[ 24dp ]
  H1 headlineMedium OnSurface centralizado: onboarding_role_title (NOVO)
[ 16dp ]
  bodyLarge OnSurfaceVariant centralizado, máx 85%: onboarding_role_intro (NOVO)
[ 24dp ]
  faixa de contexto: SurfaceContainerLow, ShapeMedium, 80dp de altura, gradiente tonal
                     SurfaceContainerLow→PrimaryContainer@20%, ícone security 16dp +
                     welcome_badge_native em labelMedium Primary
[ 24dp ]
  HonestyCard — AVISO OBRIGATÓRIO DA FASE
      title = onboarding_scope_title (NOVO)
      itemIcon = info, itemIconTint = OnSurfaceVariant
      items = [ dialer_activation_unchanged_3,   // WhatsApp/Telegram fora do alcance
                onboarding_scope_dnd (NOVO),     // Não Perturbe continua valendo
                settings_hide_native_log_desc ]  // registro no histórico do telefone
[ ------------ weight(1f) ------------ ]
CTA fixo: pill 56dp Primary/OnPrimary — onboarding_role_cta + arrow_forward
          enquanto o diálogo do sistema está aberto: label vira onboarding_role_requesting
          com CircularProgressIndicator 20dp à esquerda, botão enabled = false
[ 16dp ]
  disclaimer bodyMedium OnSurfaceVariant centralizado: onboarding_role_disclaimer
[ 32dp + insets ]
```

**Este é o passo que carrega o aviso obrigatório de que só chamadas de telefone são
filtradas.** Ele não é rodapé em cinza: é um `HonestyCard` com o mesmo peso visual do resto da
tela, exatamente como o `DialerActivationScreen` faz com os dois cards de peso igual. As três
frases já existem em `strings.xml` (escritas na Phase 5/6 a partir da fonte AOSP) e **não
devem ser reescritas** — reescrever é o caminho de volta à promessa falsa.

- Estado **papel concedido** (usuário voltou do diálogo com sucesso): CTA vira
  `onboarding_next`; chip pill `CallAccept`@20% + ícone `check_circle` + `dialer_active_chip`
  ("Ativo") sob o H1. Avanço **não** é automático — o usuário lê a confirmação e toca.
- Estado **papel negado**: `InfoBanner(text = onboarding_role_denied (NOVO), actionLabel = onboarding_role_retry (NOVO))`
  acima do CTA; CTA vira `onboarding_next`. **Nunca** travar aqui; nunca repetir o diálogo
  sem toque explícito.
- **Botão desabilitado + semântica mesclada:** o CTA em "Solicitando permissão…" tem
  `enabled = false`. Se ele for envolvido por um container com `mergeDescendants = true`, o
  estado desabilitado se perde (achado da Phase 6). O CTA fica **fora** de qualquer container
  mesclado e o teste afirma `assertIsNotEnabled()` no nó do botão.
- **TalkBack:** `liveRegion = Polite` no chip/banner de resultado, para anunciar a transição
  concedido/negado sem roubar o foco do CTA.

**Strings:** existentes `onboarding_step_indicator`, `onboarding_role_cta`,
`onboarding_role_requesting`, `onboarding_role_disclaimer`, `onboarding_next`,
`welcome_badge_native`, `dialer_activation_unchanged_3`, `settings_hide_native_log_desc`,
`dialer_active_chip`, `welcome_feature_*`. **Novas:** `onboarding_role_title`,
`onboarding_role_intro`, `onboarding_scope_title`, `onboarding_scope_dnd`,
`onboarding_role_denied`, `onboarding_role_retry`, `onboarding_skip`.

---

### 6.3 Passo 2/6 — desconhecidos — `UnknownPolicyStepScreen`

Fonte: `configura_o_desconhecidos/`. O mockup encapsula tudo num card central flutuante; o
contrato mantém o card (é a assinatura visual deste passo) com `SurfaceContainerLow`,
`ShapeMedium`, borda 1dp `OutlineVariant` @30%, padding 16dp.

```
TopAppBar: marca | [ Passo 2 de 6 + barra ] | [ Pular ]
[ centralizado verticalmente, card máx 400dp de largura ]
  círculo 64dp PrimaryContainer@20%, ícone no_sim 32dp Primary
[ 4dp ]
  H1 headlineMedium OnSurface centralizado: unknown_title
[ 4dp ]
  bodyMedium OnSurfaceVariant centralizado: unknown_question
[ 24dp ]
  OptionCard × 3, gap 8dp (grupo com selectableGroup):
    block   — ícone block,             ErrorContainer@20% / Error       → OriginPolicy.BLOCK  [DEFAULT]
    silence — ícone notifications_off, SecondaryContainer@20% / Secondary → OriginPolicy.SILENCE
    allow   — ícone call,              TertiaryContainer@20% / Tertiary  → OriginPolicy.RING
[ 32dp ]
  CTA pill 56dp Primary/OnPrimary: onboarding_next + arrow_forward
[ 16dp ]
  microcopy labelMedium OnSurfaceVariant @60%: onboarding_change_later
```

O ícone colorido por opção vem do mockup e é **decoração semântica redundante**, não portadora
de estado: quem carrega o estado é `Role.RadioButton` + `check_circle` + borda 2dp.

- **`OriginPolicy.NEVER_SILENCE` não é oferecida para desconhecidos** — não faria sentido, e o
  mockup não a oferece. Fidelidade e domínio coincidem.
- O estilo do bloqueio (rejeitar × caixa postal, `BlockMode`) **não** aparece neste passo:
  vive em Proteção (`TELAS.md` §3). Não introduzir aqui.

**Strings:** todas existentes — `unknown_title`, `unknown_question`,
`unknown_option_{block,silence,allow}` + `_desc`, `onboarding_next`,
`onboarding_change_later`, `onboarding_step_indicator`.

---

### 6.4 Passo 3/6 — contatos + `READ_CONTACTS` — `ContactsPolicyStepScreen`

Fonte: `configura_o_contatos/`. Este mockup usa layout de página cheia (não card), com o
contador de passo **em caixa alta à esquerda** e a barra à direita — acabamento diferente do
passo 2 (§12.2). Unificado: o contador vai para o `TopAppBar`, como em todos os passos.

```
TopAppBar: marca | [ Passo 3 de 6 + barra ] | [ Pular ]
[ 24dp ]
  H1 headlineMedium OnSurface, alinhado à esquerda: contacts_title
[ 16dp ]
  bodyLarge OnSurfaceVariant: contacts_explainer
[ 24dp ]
  CARD de permissão — varia com ContactsPermissionState (tabela abaixo)
[ 24dp ]
  OptionCard × 4, gap 16dp:
    ring          — notifications_active [badge "Padrão"] → OriginPolicy.RING  [DEFAULT]
    block         — block                                 → OriginPolicy.BLOCK
    silence       — notifications_off                     → OriginPolicy.SILENCE
    never_silence — priority_high                         → OriginPolicy.NEVER_SILENCE
[ 24dp ]
  Switch row: settings_block_private  (ligado por default)
              explicação permanente bodyMedium: settings_block_private_desc (NOVO)
[ ---- weight ---- ]
CTA fixo: onboarding_next
```

**Card de permissão, por estado (`ContactsPermissionState`, os quatro):**

| Estado | Tratamento |
|---|---|
| `NEVER_ASKED` | `HonestyCard`-like: `SurfaceContainerLow`, ícone `contacts` `Primary`, texto `contacts_permission_rationale`, botão tonal `SecondaryContainer` "Permitir leitura da agenda" (`dialer_activation_grant_contacts`, existente). Toque dispara o diálogo do sistema — **uma vez**. |
| `GRANTED` | Card colapsa em chip pill `CallAccept`@20%, ícone `check_circle`, texto `contacts_permission_granted` (NOVO). |
| `DENIED_ONCE` | `InfoBanner(contacts_permission_denied (NOVO), actionLabel = dialer_activation_grant_contacts)` — permite **um** novo pedido com explicação. |
| `PERMANENTLY_DENIED` | `InfoBanner(contacts_permission_blocked (NOVO), actionLabel = about_open_app_settings)` → Configurações do sistema. Sem novo diálogo (a plataforma não o mostra mais). |

**Consequência honesta, obrigatória na tela** (não em rodapé): sem `READ_CONTACTS` o lookup
devolve `UNAVAILABLE` e a chamada cai na `FallbackPolicy` — contatos **podem ser tratados como
desconhecidos**. Texto: `contacts_permission_denied`. Sem a permissão, as quatro opções
continuam **habilitadas e editáveis** (a escolha é preferência persistida, e vale no momento
em que a permissão for concedida); o que muda é o aviso. Alternativa considerada e rejeitada:
desabilitar os cards — desabilitar sem explicar é pior, e desabilitar dentro de semântica
mesclada é justamente onde o estado se perde.

**Strings:** existentes `contacts_title`, `contacts_explainer`,
`contacts_permission_rationale`, `contacts_option_*` + `_desc`, `contacts_default_badge`,
`settings_block_private`, `dialer_activation_grant_contacts`, `about_open_app_settings`,
`onboarding_next`. **Novas:** `settings_block_private_desc`, `contacts_permission_granted`,
`contacts_permission_denied`, `contacts_permission_blocked`.

---

### 6.5 Passo 4/6 — whitelist — `WhitelistPolicyStepScreen`

Fonte: `configura_o_whitelist/`.

```
TopAppBar: marca | [ Passo 4 de 6 + barra ] | [ Pular ]
[ 24dp ]
  H1 headlineMedium OnSurface: whitelist_setup_title
[ 8dp ]
  bodyMedium OnSurfaceVariant: whitelist_setup_desc (NOVO — o mockup usa texto de wizard)
[ 24dp ]
  CARD explicativo (substitui o quadrado ilustrado remoto do mockup — §12.7):
    SurfaceContainerLow, ShapeMedium, padding 24dp, conteúdo centralizado
    círculo 80dp PrimaryContainer, ícone verified_user filled 40dp OnPrimaryContainer
    [ 16dp ] titleLarge OnSurface: whitelist_setup_what_title
    [ 8dp  ] bodyMedium OnSurfaceVariant: whitelist_setup_what_desc
[ 24dp ]
  labelLarge OnSurfaceVariant: whitelist_setup_question
[ 16dp ]
  OptionCard × 4, gap 16dp:
    never_silence — notifications_active [badge "Padrão"] → NEVER_SILENCE  [DEFAULT]
    ring          — volume_up                            → RING
    block         — block                                → BLOCK
    silence       — notifications_off                    → SILENCE
[ 32dp ]
  CTA pill 56dp Primary/OnPrimary: onboarding_next   (NÃO "Finalizar" — §12.1)
[ 8dp ]
  botão de texto 48dp Primary: onboarding_back
[ 16dp ]
  hint fixo bodyMedium OnSurfaceVariant: whitelist_setup_hint
```

- O mockup traz o hint como snackbar que **desliza depois de 1s**. Convertido em **texto
  permanente** no rodapé: informação que aparece por conta própria e desaparece é informação
  que o usuário perde, e um snackbar temporizado é hostil ao TalkBack.
- O mockup rotula o default como "Padrão do sistema" — impreciso: é o padrão **do Sentinela**.
  Usar o badge `contacts_default_badge` ("Padrão") + a descrição honesta já existente
  `whitelist_option_never_silence_desc`, que diz que o "Não Perturbe" continua valendo.

**Strings:** existentes `whitelist_setup_title`, `whitelist_setup_what_title`,
`whitelist_setup_what_desc`, `whitelist_setup_question`, `whitelist_option_*` + `_desc`,
`whitelist_setup_hint`, `contacts_default_badge`, `onboarding_next`, `onboarding_back`.
**Nova:** `whitelist_setup_desc`.

---

### 6.6 Passo 5/6 — notificação (opt-in) — `NotificationStepScreen` — **sem mockup**

Derivado. Mantém o esqueleto dos passos 3–4 (H1 + explicação + controles + CTA).

```
TopAppBar: marca | [ Passo 5 de 6 + barra ] | [ Pular ]
[ 24dp ]  H1 headlineMedium: settings_notification_enable
[ 16dp ]  bodyLarge OnSurfaceVariant: settings_notification_enable_desc
[ 24dp ]  Switch row 56dp: settings_notification_enable  — DEFAULT DESLIGADO
          ao ligar: dispara POST_NOTIFICATIONS via RuntimePermissionAsk
          rationale (quando DENIED_ONCE): notification_permission_rationale
[ 16dp ]  sub-opções, visíveis SOMENTE com o switch ligado, animação expand/collapse 200 ms:
          OptionCard × 2 (Role.RadioButton):
            settings_notification_identification_masked     [DEFAULT]
            settings_notification_identification_anonymous
[ ---- weight ---- ]
CTA: onboarding_next
```

- Default **desligado** é decisão de produto já escrita em `settings_notification_enable_desc`
  ("Vem desligado"). O passo **não** pressiona: nenhuma palavra de recomendação, nenhum
  destaque no switch.
- Nenhuma das duas identificações mostra o número completo (`MASKED` usa `PhoneMask`).
- **Semântica:** o switch tem `Role.Switch` e `stateDescription` explícito; a explicação
  permanente é irmã, não filha mesclada — assim o TalkBack lê "Avisar quando bloquear uma
  chamada, desativado" e depois a explicação, em vez de um bloco único ilegível.
- Sub-opções aparecendo/desaparecendo: `liveRegion = Polite` no container, para o TalkBack
  anunciar que novas opções surgiram.

**Strings:** todas existentes — `settings_notification_enable`,
`settings_notification_enable_desc`, `settings_notification_identification_masked`,
`settings_notification_identification_anonymous`, `notification_permission_rationale`.

---

### 6.7 Passo 6/6 — verificação final — `SummaryStepScreen` — **sem mockup**

Derivado. Fecha o critério "o usuário entende o que ficou configurado".

```
TopAppBar: marca | [ Passo 6 de 6 + barra ]          (sem "Pular": já é o fim)
[ 24dp ]  círculo 96dp, ícone shield filled 48dp — cor conforme o veredito (abaixo)
[ 16dp ]  H1 headlineMedium: onboarding_summary_title_ok / _partial (NOVOS)
[ 8dp  ]  bodyLarge OnSurfaceVariant: onboarding_summary_body (NOVO)
[ 24dp ]  LISTA DE VERIFICAÇÃO — 4 linhas, cada uma 56dp:
            ícone (check_circle CallAccept | error StatusAttention) + rótulo + valor/estado
            1. Filtro de chamadas padrão .... concedido | ausente + ação "Corrigir"
            2. Leitura da agenda ............ concedida | ausente + ação "Permitir"
            3. Números desconhecidos ........ <opção escolhida>
            4. Contatos / Whitelist ......... <opções escolhidas>
[ 24dp ]  HonestyCard "O que o Sentinela não faz" — MESMOS itens do passo 1, mesmo estilo
[ ---- weight ---- ]
CTA pill 56dp Primary/OnPrimary: onboarding_finish
```

- **Veredito nunca é falsamente positivo.** Se o papel está ausente, o ícone do hero usa
  `StatusAttention`, o H1 é a variante `_partial` e a linha 1 traz a ação de correção. O
  Sentinela não escreve "tudo pronto" quando não está — é a mesma regra do "0 bloqueadas".
- Cada linha comunica estado por **ícone + texto**, nunca por cor sozinha.
- Repetir o `HonestyCard` do passo 1 é deliberado: é a última tela antes de o usuário confiar
  no app, e a única em que ele já viu o app inteiro.
- **TalkBack:** a lista é `semantics { collectionInfo }`; cada linha é um nó com
  `contentDescription` = "rótulo, estado"; as ações de correção são botões separados,
  focáveis, **fora** do nó mesclado da linha.

**Strings novas:** `onboarding_summary_title_ok`, `onboarding_summary_title_partial`,
`onboarding_summary_body`, `onboarding_check_role`, `onboarding_check_contacts`,
`onboarding_check_unknown`, `onboarding_check_origins`, `onboarding_check_granted`,
`onboarding_check_missing`. Existentes: `onboarding_finish`,
`dashboard_fix_configuration`, `dialer_activation_grant_contacts`.

---

## 7. Home / Início — `HomeScreen`

Fonte: `dashboard/`. Layout do mockup mantido linha por linha; a copy do hero e o rótulo de
spam trocados (§12.5).

```
TopAppBar 64dp, transparente sobre Surface, blur/elevação tonal ao rolar
  escudo Primary 28dp + "Sentinela" headlineSmall bold Primary   [ settings 48dp → Proteção ]
[ 24dp ]   ← pt-24 do mockup, sob o app bar

═══ HERO DE PROTEÇÃO ═══  ShapeLarge (24dp), padding 24dp
  ON : fundo PrimaryContainer, conteúdo OnPrimaryContainer
  OFF: fundo StatusAttention (FIXO), conteúdo OnStatusAttention (FIXO)
  ┌ titleLarge bold: dashboard_protection_active | dashboard_protection_inactive
  │ [ 4dp ] linha de status: dot 12dp (CallAccept FIXO, com ping | Outline, estático)
  │         + labelMedium: dashboard_monitoring | dashboard_protection_off_hint (NOVO)
  │ [ à direita, alinhado ao topo ] Switch M3, alvo 48dp
  │         → alterna ScreeningSettings.protectionEnabled (§12.9)
  └ [ 16dp ] faixa interna: fundo do conteúdo @10%, ShapeMedium, padding 16dp
             ícone verified_user filled + bodyMedium: dashboard_device_safe

[ 24dp ]
═══ BANNERS DE ESTADO ═══  zero ou mais InfoBanner, na ordem de severidade (§8)

[ 24dp ]
═══ ESTATÍSTICAS ═══  2 colunas, gap 16dp, cada card 128dp, ShapeLarge, padding 20dp
  card A: ícone block Primary (topo)  |  labelMedium OnSurfaceVariant: dashboard_total_blocked
                                        headlineLarge (32sp) Primary: <n>
  card B: ícone today Secondary       |  labelMedium: dashboard_blocked_today
                                        headlineLarge Secondary: <n>
  fundo SurfaceContainerLow, borda 1dp OutlineVariant  (glass → tonal, §12.8)

[ 24dp ]
═══ ÚLTIMA BLOQUEADA ═══
  linha de cabeçalho: titleLarge OnSurface (dashboard_last_blocked)
                      + labelMedium OnSurfaceVariant à direita: tempo relativo
  [ 12dp ] card ShapeMedium, padding 16dp, SurfaceContainerLow, borda 1dp OutlineVariant:
      avatar 48dp circular, StatusBlocked@30%, ícone phone_disabled StatusBlocked (FIXO)
      [16dp] Column: bodyLarge OnSurface — NÚMERO MASCARADO (PhoneMask.mask)
                     labelMedium OnSurfaceVariant — motivo REAL da decisão
      [weight] ícone chevron_right Outline (o card todo é clicável → Histórico)

[ 24dp ]
═══ ATALHOS ═══  1 coluna, gap 12dp, cada card 72dp, ShapeMedium, padding 20dp
  [ ícone-container 40dp SecondaryContainer + format_list_bulleted ] dashboard_quick_whitelist  >
  [ ícone-container 40dp SurfaceContainerHighest + history ]         dashboard_quick_history    >
  fundo SurfaceContainer, borda 1dp OutlineVariant

[ 32dp + insets ]

═══ BOTTOM NAV ═══  4 itens, SurfaceContainer, cantos superiores 16dp
  Início (ativa: pill SecondaryContainer/OnSecondaryContainer, ícone filled) | Permitidos |
  Histórico | Ajustes         ← nav_home / nav_whitelist / nav_history / nav_settings
```

**Motivo real da última bloqueada** — o MVP **não classifica spam**. Mapeamento permitido:

| Origem da decisão | Rótulo |
|---|---|
| desconhecido | `history_unknown_number` ("Número Desconhecido") |
| privado/oculto | `history_private_number` / `history_private_id` |
| contato (modo discador) | `call_origin_contact` ("Contato") |
| whitelist | `call_origin_whitelist` ("Permitido") |

Proibido: "Provável Fraude Financeira", "ALTO RISCO", "spam conhecido" ou qualquer rótulo de
risco. Nenhum deles existe como dado no app.

**Status vem de consulta viva** (decisão do `07-CONTEXT.md`): o papel de triagem e o
`DialerModeState` são reconsultados **em cada retomada** (`ON_RESUME`), nunca lidos de flag
persistida. A Phase 6 mediu que perder um papel **mata o processo** — estado guardado mente.

**TalkBack na home:**
- `traversalIndex` explícito: `marca → hero (título+status como um nó) → switch → banners →
  estatísticas → última bloqueada → atalhos → bottom nav`.
- Hero: `contentDescription` = "Proteção ativa" / "Proteção desativada" **como texto**; o dot e
  a animação de ping com `clearAndSetSemantics {}`.
- Switch: nó **próprio**, `Role.Switch` + `stateDescription`, **fora** do nó mesclado do hero —
  aqui é exatamente onde a armadilha da Phase 6 morde: mesclar o card do hero com o switch
  dentro faz o TalkBack perder o estado ligado/desligado e o `enabled`.
- Estatísticas: cada card é um nó único com `contentDescription` = "Total bloqueado, 42".
  Ler "42" solto não significa nada.
- Última bloqueada: `contentDescription` = "Última chamada bloqueada, <máscara lida dígito a
  dígito>, Número Desconhecido, há 15 minutos". **Nunca** o número completo.
- Bottom nav: `Role.Tab` + `selected`; alvo ≥ 48dp em cada item.

---

## 8. Estados vazios e degradados da home (obrigatórios)

Nenhum deles existe no mockup. Todos são requisito da fase.

| Estado | Detecção | Tratamento visual | Strings |
|---|---|---|---|
| **Papel de triagem ausente** | consulta viva em `ON_RESUME` | Hero **OFF** (`StatusAttention` fixo, dot `Outline` estático) + `InfoBanner` no topo dos banners com `actionLabel` que **abre o pedido de papel de verdade** e reconsulta ao voltar | `dashboard_role_missing`, `dashboard_fix_configuration` |
| **Proteção desligada pelo usuário** | `protectionEnabled = false` | Hero OFF; **sem** banner (não é erro, é escolha); estatísticas continuam visíveis | `dashboard_protection_inactive`, `dashboard_protection_off_hint` (NOVO) |
| **`READ_CONTACTS` negada** | `ContactsPermissionState != GRANTED` | `InfoBanner` com ação: `DENIED_ONCE` → novo pedido; `PERMANENTLY_DENIED` → Configurações do app. Texto explica que contatos podem cair na política de erro | `dashboard_contacts_missing` (NOVO), `dialer_activation_grant_contacts`, `about_open_app_settings` |
| **Histórico desligado** (`historyEnabled = false` ou `RetentionPolicy.NEVER_STORE`) | settings | **Os dois cards de estatística NÃO mostram número.** Mostram, no lugar do numeral, `titleMedium` `OnSurfaceVariant` = `dashboard_history_off_value` (NOVO, "—") e, abaixo, um `InfoBanner` com ação "Ligar histórico" → Proteção. O bloco "Última bloqueada" **não é renderizado**. | `dashboard_history_off` (NOVO), `dashboard_history_off_value` (NOVO), `dashboard_history_off_action` (NOVO) |
| **Histórico ligado, nenhum bloqueio ainda** | contagem = 0 **e** `historyEnabled` | Estatísticas mostram `0` — aqui zero é **verdade**. Bloco "Última bloqueada" vira estado vazio: card `SurfaceContainerLow`, ícone `shield` `OnSurfaceVariant`, `bodyMedium` = `history_empty` | `history_empty` (existente) |
| **Papel de discador perdido** | `DialerModeState` mudou de ativo para não-ativo | `InfoBanner` informativo (tom neutro, **sem** vermelho — nada quebrou) com ação para a tela de ativação | `dialer_role_lost_title`, `dialer_role_lost_body`, `dialer_role_lost_action` (todas existentes) |
| **Carregando (primeiro frame)** | contagens ainda não emitidas | esqueleto tonal `SurfaceContainerLow` nos dois cards + `state_loading` como `contentDescription`. **Nunca** exibir `0` como placeholder de "ainda não sei" | `state_loading` (existente) |
| **Falha de leitura do histórico** | exceção no repositório | `InfoBanner` com `state_error` + ação "Tentar de novo"; estatísticas em "—" | `state_error` (existente) |

**A regra do "0 mentiroso", explícita e testável:** `0` só pode ser renderizado quando
`historyEnabled == true`, a retenção não é `NEVER_STORE` e a contagem foi **efetivamente
carregada**. Nos outros três casos (desligado, carregando, erro) a tela mostra "—" mais a
explicação. Isso vale um teste de composição por caso e merece um invariante em
`scripts/verify-invariants.sh`.

Ordem de precedência quando há vários banners: papel de triagem ausente → `READ_CONTACTS`
negada → histórico desligado → papel de discador perdido. **No máximo dois banners
simultâneos**; o excedente vira uma linha "Ver todos os avisos" que rola até Proteção — três
banners empilhados empurram o conteúdo real da home para fora da tela.

---

## 9. Tela Proteção — `SettingsScreen` — **sem mockup**

Derivada de `TELAS.md` §10, com a regra do `07-CONTEXT.md`: **explicação curta e permanente
sob cada opção; efeito imediato, sem botão salvar.**

```
TopAppBar 64dp com back + settings_title ("Proteção")
conteúdo rolável, margem lateral 16dp, gap entre grupos 24dp
cada GRUPO: card SurfaceContainer, ShapeMedium, padding 16dp,
            cabeçalho labelLarge Primary + itens separados por 16dp
```

| # | Item | Controle | Estado | Explicação permanente |
|---|---|---|---|---|
| 1 | `settings_protection_toggle` | Switch | `protectionEnabled` | `settings_protection_toggle_desc` (NOVO). OFF pinta o card com `StatusAttention` @15% |
| 1b | (status do papel) | linha informativa + ação | consulta viva | `dashboard_role_missing` + `dashboard_fix_configuration` — a mesma verdade da home, no mesmo lugar em que se conserta |
| 2 | `settings_unknown_policy` | 3 `OptionCard` | `unknownPolicy` | as três `unknown_option_*_desc` |
| 3 | `settings_contacts_policy` | 4 `OptionCard` | `contactsPolicy` | as quatro `contacts_option_*_desc` + nota `settings_contacts_policy_note` (NOVO): vale enquanto a leitura da agenda estiver concedida |
| 4 | `settings_whitelist_policy` | 4 `OptionCard` | `whitelistPolicy` | as quatro `whitelist_option_*_desc` |
| 5 | `settings_block_private` | Switch | `blockPrivateNumbers` | `settings_block_private_desc` (NOVO) |
| 6 | Modo de bloqueio | 2 `OptionCard` | `blockMode` | `settings_mode_reject`, `settings_mode_silent_voicemail` + `settings_block_mode_desc` (NOVO), descrevendo o efeito **para quem liga** |
| 7 | `settings_hide_native_log` | Switch | `hideFromNativeCallLog` | **`settings_hide_native_log_desc` literal, sem cortar.** É o texto que impede a promessa falsa; encurtar aqui é regressão |
| 8 | `settings_show_notification` / `settings_notification_enable` | Switch + 2 `OptionCard` | `showOwnNotification`, `notificationIdentification` | `settings_notification_enable_desc`; sub-opções só com o switch ligado |
| 9 | `settings_dialer_mode` | linha navegável `>` | `DialerModeState` | `settings_dialer_mode_desc`. **Abre `DialerActivationScreen`** com os quatro callbacks já existentes. `UNAVAILABLE` → linha desabilitada + `dialer_activation_unavailable`; `BLOCKED_BY_CONTACTS` → habilitada, e a própria tela explica o pré-requisito |
| 10 | Chamada repetida toca | Switch | `repeatedCallBypassEnabled` | `settings_repeated_call` + `_desc` (NOVOS) |
| 11 | Histórico ligado | Switch | `historyEnabled` | `settings_history_enabled` + `_desc` (NOVOS). **Desligar não apaga**; apagar é o item 13 |
| 12 | Retenção | 5 `OptionCard` | `retentionPolicy` | `settings_retention_*` (NOVOS): nunca guardar / 7 / 30 / 90 dias / até eu excluir |
| 13 | `history_clear_all` | botão de texto `Error` | ação | **confirma** (§9.2) |
| 14 | `settings_fallback_policy` | 2 `OptionCard` | `fallbackPolicy` | `settings_fallback_allow`, `settings_fallback_block` + `settings_fallback_desc` (NOVO), com o trade-off honesto |
| 15 | Card de limitações | `HonestyCard` | — | `dialer_activation_unchanged_1..4` — as mesmas frases do passo 1 e da ativação do discador. Fonte única de verdade, texto único |
| 16 | Privacidade e sobre | linha navegável `>` | — | `about_title` — **destino da Phase 9**; nesta fase, item presente e desabilitado, ou navegando para um placeholder, a critério do executor |

### 9.1 Efeito imediato

Toda mudança grava no DataStore na hora e a triagem passa a valer **na próxima chamada** —
`snapshot()` com cache `@Volatile` já existe. Feedback: o próprio controle se move; **sem**
snackbar de "salvo" (ruído a cada toque) e **sem** botão salvar. Nenhuma troca de política
pede confirmação: é reversível, e confirmação excessiva ensina o usuário a tocar em "sim" sem
ler.

### 9.2 Confirmações — apenas o que perde dado

| Ação | Tratamento |
|---|---|
| `history_clear_all` (limpar histórico) | `AlertDialog`: título `history_clear_all`, corpo `settings_clear_history_confirm` (NOVO) dizendo **quantos registros** serão apagados e que a ação é irreversível; confirmar = `action_confirm` em `Error`; cancelar = `action_cancel`. Foco inicial no **cancelar** |
| Retenção → `NEVER_STORE` | `AlertDialog`: muda comportamento **e** poda o que existe. Corpo `settings_retention_never_confirm` (NOVO) |
| Desligar histórico | **sem diálogo** — não apaga nada; o snackbar informa que os registros existentes permanecem (`settings_history_off_kept`, NOVO) |
| Trocar qualquer política | **sem diálogo** |
| Desligar proteção | **sem diálogo**; o hero muda visivelmente na home e aqui |
| Ativar/reverter modo discador | **sem diálogo próprio** — o seletor do sistema é a confirmação (contrato da Phase 6) |

### 9.3 Semântica de Proteção

- Cada linha de switch: **três nós** — rótulo (`heading()` não; texto simples), switch
  (`Role.Switch` + `stateDescription`), explicação (texto). O switch **não** entra num
  container com `mergeDescendants = true`, sob pena de perder `enabled`/estado (Phase 6).
- Grupos de `OptionCard`: `selectableGroup()` + rótulo do grupo como `heading()`.
- Linha desabilitada (item 9 em `UNAVAILABLE`): `enabled = false` **no nó da linha** +
  `stateDescription` com o motivo. Teste afirma `assertIsNotEnabled()` **e** o
  `stateDescription`.
- Ordem de travessia = ordem visual; nenhum item de decoração focável.

---

## 10. Copywriting Contract

### 10.1 Reuso antes de criar

A Phase 1 já escreveu **132 strings** pt-BR (hoje suprimidas do lint como `UnusedResources`) e
a Phase 6 acrescentou **74**. As tabelas das §6–§9 citam, por chave, tudo que é reaproveitado.
Saldo:

| Tela | Chaves existentes reusadas | Chaves novas |
|---|---|---|
| Boas-vindas | 12 | 1 |
| Passo 1 (papel) | 10 | 7 |
| Passo 2 (desconhecidos) | 9 | 0 |
| Passo 3 (contatos) | 14 | 4 |
| Passo 4 (whitelist) | 13 | 1 |
| Passo 5 (notificação) | 5 | 0 |
| Passo 6 (verificação) | 3 | 9 |
| Home | 18 | 5 |
| Proteção | 30 | 16 |

**Esta é também a fase que fecha a pendência da Phase 1:** com as telas consumindo as strings,
a supressão de `UnusedResources` deve ser **reavaliada e reduzida** (escopo legítimo, previsto
no contexto da Phase 1). O que restar sem uso ao fim da fase pertence às Phases 8–9 e deve ser
listado nominalmente, não suprimido em bloco.

### 10.2 Novas strings propostas (pt-BR)

```xml
<!-- Boas-vindas -->
<string name="welcome_open_source">100% open source — sem propaganda, sem telemetria</string>

<!-- Onboarding: navegação e papel -->
<string name="onboarding_skip">Pular</string>
<string name="onboarding_role_title">Deixe o Sentinela cuidar da triagem</string>
<string name="onboarding_role_intro">Para filtrar chamadas, o Sentinela precisa ser o filtro de chamadas padrão do Android. Quem concede é você, e você pode desfazer nas configurações do sistema quando quiser.</string>
<string name="onboarding_role_denied">O Sentinela não recebeu o papel de filtro de chamadas. Nenhuma chamada será triada até você conceder — você pode fazer isso agora ou depois, na tela Proteção.</string>
<string name="onboarding_role_retry">Conceder agora</string>
<string name="onboarding_scope_title">O que o Sentinela não faz</string>
<string name="onboarding_scope_dnd">O \"Não Perturbe\" do sistema continua valendo por cima das configurações do Sentinela.</string>

<!-- Onboarding: contatos e privados -->
<string name="contacts_permission_granted">Leitura da agenda concedida</string>
<string name="contacts_permission_denied">Sem a leitura da agenda, o Sentinela não consegue saber quem está nos seus contatos: essas chamadas podem ser tratadas como desconhecidas.</string>
<string name="contacts_permission_blocked">A leitura da agenda foi negada de vez. Para conceder, abra as configurações do aplicativo.</string>
<string name="settings_block_private_desc">Chamadas sem identificação de número seguem a política de bloqueio que você escolheu. Não é possível saber quem ligou.</string>

<!-- Onboarding: whitelist -->
<string name="whitelist_setup_desc">A whitelist é sua lista pessoal de números confiáveis, separada da agenda do telefone.</string>

<!-- Onboarding: verificação final -->
<string name="onboarding_summary_title_ok">Tudo pronto</string>
<string name="onboarding_summary_title_partial">Quase pronto</string>
<string name="onboarding_summary_body">Confira o que ficou configurado. Você pode mudar qualquer item depois, na tela Proteção.</string>
<string name="onboarding_check_role">Filtro de chamadas padrão</string>
<string name="onboarding_check_contacts">Leitura da agenda</string>
<string name="onboarding_check_unknown">Números desconhecidos</string>
<string name="onboarding_check_origins">Contatos e whitelist</string>
<string name="onboarding_check_granted">Concedido</string>
<string name="onboarding_check_missing">Ausente</string>

<!-- Home -->
<string name="dashboard_protection_off_hint">Nenhuma chamada está sendo triada.</string>
<string name="dashboard_contacts_missing">O Sentinela não pode ler sua agenda. Chamadas de contatos podem ser tratadas como desconhecidas.</string>
<string name="dashboard_history_off">O histórico está desligado, então não há contagem de chamadas bloqueadas para mostrar.</string>
<string name="dashboard_history_off_value">—</string>
<string name="dashboard_history_off_action">Ligar histórico</string>

<!-- Proteção -->
<string name="settings_protection_toggle_desc">Desligado, o Sentinela não bloqueia, não silencia e não registra nada. As chamadas seguem o comportamento normal do telefone.</string>
<string name="settings_contacts_policy_note">Esta política vale enquanto a leitura da agenda estiver concedida. Sem ela, contatos caem na política de erro.</string>
<string name="settings_block_mode_desc">Rejeitar imediatamente dá sinal de ocupado a quem liga. Encaminhar silenciosamente manda a chamada para a caixa postal, se a sua operadora tiver esse serviço.</string>
<string name="settings_repeated_call">Segunda chamada seguida toca</string>
<string name="settings_repeated_call_desc">Se o mesmo número voltar a ligar poucos minutos depois de ser bloqueado, a segunda chamada toca. Serve para emergências de quem não está na sua agenda.</string>
<string name="settings_history_enabled">Guardar histórico de bloqueios</string>
<string name="settings_history_enabled_desc">O histórico fica só neste aparelho e é o que permite auditar o que o Sentinela bloqueou. Desligar não apaga o que já foi guardado.</string>
<string name="settings_history_off_kept">Histórico desligado. Os registros já guardados continuam no aparelho.</string>
<string name="settings_retention_title">Guardar registros por</string>
<string name="settings_retention_never">Não guardar</string>
<string name="settings_retention_7">7 dias</string>
<string name="settings_retention_30">30 dias</string>
<string name="settings_retention_90">90 dias</string>
<string name="settings_retention_manual">Até eu excluir</string>
<string name="settings_retention_never_confirm">Escolher \"Não guardar\" apaga os registros existentes e o Sentinela deixa de registrar novos bloqueios. Não é possível desfazer.</string>
<string name="settings_clear_history_confirm">Isso apaga %1$d registro(s) de chamadas bloqueadas deste aparelho. Não é possível desfazer.</string>
<string name="settings_fallback_desc">Se a consulta local falhar, o Sentinela precisa decidir sozinho. Permitir arrisca deixar passar uma chamada indesejada; bloquear arrisca perder uma chamada que você queria receber.</string>
```

### 10.3 Proibições de copy — verificáveis por revisão e por grep

Herdadas do `06-UI-SPEC.md` e ampliadas com o que os mockups desta fase tentaram dizer:

1. Nunca "100%", "garantido", "total", "infalível" sobre bloqueio. (O único "100%" permitido é
   "100% offline" e "100% open source" — ambos verdadeiros e verificáveis no manifest.)
2. Nunca prometer filtragem de WhatsApp/Telegram/VoIP.
3. Nunca sugerir ausência de registro no histórico do telefone.
4. Nunca sugerir que o app contorna o "Não Perturbe".
5. **Nunca rótulo de risco ou classificação de spam** — "Provável Fraude Financeira", "ALTO
   RISCO", "spam conhecido", "número denunciado". O MVP não tem esse dado. (O mockup do
   dashboard e o de boas-vindas ambos violam isto — §12.5, §12.3.)
6. **Nunca base de dados, nuvem, "milhões de números", "base global"** — o app é offline.
7. Nunca "criptografado" sobre o processamento local: o dado é local, não cifrado. Dizer
   "criptografado" é reivindicar uma propriedade que o código não tem.
8. Nunca superlativo de marketing ("o melhor", "inteligente", "definitivo", "poderoso").
   O mockup de boas-vindas usa "Filtros inteligentes automáticos" — não entra.
9. Nenhuma pressão sobre opt-in: sem "recomendado", "ative agora", contador, urgência ou
   destaque desigual entre aceitar e recusar.
10. Nenhum número exibido além da máscara fora das telas de chamada/discagem.
11. Nenhum estado falsamente positivo: "Tudo pronto" só quando está; `0` só quando é verdade.

---

## 11. Inventário de componentes novos

| Composable | Arquivo sugerido | Tokens / notas |
|---|---|---|
| `WelcomeScreen` | `ui/onboarding/WelcomeScreen.kt` | `Surface`, `SurfaceContainer`, `Primary`, `ShapePill`, `ShapeMedium` |
| `RoleStepScreen` | `ui/onboarding/RoleStepScreen.kt` | reusa `HonestyCard`, `InfoBanner` |
| `UnknownPolicyStepScreen` | `ui/onboarding/UnknownPolicyStepScreen.kt` | reusa `OptionCard` |
| `ContactsPolicyStepScreen` | `ui/onboarding/ContactsPolicyStepScreen.kt` | reusa `OptionCard`, `InfoBanner`, `SettingSwitchRow` |
| `WhitelistPolicyStepScreen` | `ui/onboarding/WhitelistPolicyStepScreen.kt` | reusa `OptionCard` |
| `NotificationStepScreen` | `ui/onboarding/NotificationStepScreen.kt` | reusa `SettingSwitchRow`, `OptionCard` |
| `SummaryStepScreen` | `ui/onboarding/SummaryStepScreen.kt` | reusa `HonestyCard`, `CheckRow` |
| **`OptionCard`** | `ui/components/OptionCard.kt` | **componente-chave da fase** (§6.0.1): `SurfaceContainerLow`/`SurfaceContainerHigh`, borda 2dp `Primary` em seleção, `Role.RadioButton`, alvo ≥ 72dp |
| **`StepHeader`** | `ui/components/StepHeader.kt` | contador `onboarding_step_indicator` + barra 4dp; barra com `clearAndSetSemantics` |
| **`SentinelaTopBar`** | `ui/components/SentinelaTopBar.kt` | marca (escudo `Primary` + "Sentinela"), slot de ação; usada em boas-vindas, onboarding e home |
| **`SettingSwitchRow`** | `ui/components/SettingSwitchRow.kt` | rótulo + explicação **permanente** + Switch em nó próprio (`Role.Switch`, `stateDescription`); nunca mesclado |
| **`StatusHeroCard`** | `ui/home/StatusHeroCard.kt` | `PrimaryContainer` (ON) × `StatusAttention` fixo (OFF); dot `CallAccept` fixo + texto |
| **`StatCard`** | `ui/home/StatCard.kt` | 128dp, `SurfaceContainerLow`, `headlineLarge`; aceita `value: StatValue` = `Loaded(n)` \| `Unavailable` \| `Loading` — **impede por tipo** renderizar `0` quando o valor é desconhecido |
| **`LastBlockedCard`** | `ui/home/LastBlockedCard.kt` | `StatusBlocked` fixo, número via `PhoneMask.mask` |
| **`QuickActionRow`** | `ui/home/QuickActionRow.kt` | `SurfaceContainer`, ícone-container 40dp, `chevron_right` |
| **`CheckRow`** | `ui/components/CheckRow.kt` | ícone `check_circle`/`error` + rótulo + estado textual + ação opcional |
| `HomeScreen` | `ui/home/HomeScreen.kt` | §7 |
| `SettingsScreen` + `SettingsGroup` | `ui/settings/SettingsScreen.kt` | §9 |
| `SentinelaBottomBar` | `ui/components/SentinelaBottomBar.kt` | `Role.Tab`, itens ≥ 48dp, `nav_*` |

**Extensões do tema:** `Color.kt` recebe `StatusAttention`, `OnStatusAttention`,
`StatusBlocked` (§4.2). **Nenhum token existente muda** — `ThemeTokensTest` trava os atuais e
deve ser estendido para travar os três novos.

`MainActivity` deixa de mostrar `PlaceholderScreen()` — esta é a fase em que o app deixa de ser
esqueleto. A guarda `savedInstanceState == null` do `onAppOpened()` permanece.

---

## 12. Ambiguidades dos mockups — proposta (para o usuário corrigir)

Cada item: **o que o mockup faz**, **por que é problema**, **o que este contrato propõe**.

### 12.1 O contador de passos se contradiz nos três mockups
`desconhecidos` diz "Passo 1 de 4"; `contatos` diz "Passo 2 de 4"; `whitelist` diz "Passo 3 de
3" e o CTA "Finalizar Configuração". Três totais diferentes (4, 4, 3) e o passo do papel de
triagem não aparece em contagem nenhuma. `TELAS.md` já registra a divergência.
**Proposta:** contador único **"Passo N de 6"** com a ordem da §6, o CTA "Próximo" em 1–5 e
"Finalizar Configuração" apenas no passo 6. Alternativa, se preferir fluxo mais curto: fundir
notificação e verificação num passo só ("de 5") — mas então o opt-in de notificação perde a
tela dedicada e tende a virar um switch escondido no fim.

### 12.2 Dois mockups de boas-vindas, e três acabamentos para o mesmo wizard
`boas_vindas_ao_sentinela` e `onboarding` são **duas telas de boas-vindas** com hero, bento de
3 cards e CTA — conteúdo quase duplicado. E os passos usam três progressos diferentes (barra +
texto; label caixa-alta + barra; três pontinhos) e dois enquadramentos (card flutuante ×
página cheia).
**Proposta:** `boas_vindas` = tela 0 (marketing honesto, CTA "Começar Configuração");
`onboarding` = passo 1 (papel, CTA "Configurar Agora"), como `TELAS.md` §1–2 já mapeia. Um
único `StepHeader` (barra + texto no app bar) para os 6 passos; enquadramento de página cheia
em todos, mantendo o card central **só** no passo 2, onde ele é a assinatura do mockup.
Alternativa: descartar `boas_vindas` e começar no passo 1 — o fluxo fica ~1 toque mais curto.

### 12.3 O mockup de boas-vindas promete uma base de dados que não existe
"**Base Global** — Milhões de números identificados", "Privacidade — Processamento local
**criptografado**", "Bloqueio — **Filtros inteligentes** automáticos", e o subtítulo "identifica
e bloqueia chamadas indesejadas". O app é offline, não tem base, não cifra o processamento e
não classifica nada.
**Proposta:** usar os três cards honestos do mockup `onboarding` (**Bloqueio Local /
Silencioso / Sem Internet**) — que é exatamente o que a Phase 1 já escreveu em
`welcome_feature_*`. O card "Base Global" **não entra**. Já registrado como adaptação
obrigatória em `TELAS.md` §1; aqui fica travado.

### 12.4 "Nunca Silenciar — Ignora o modo 'Não Perturbe'" é falso
O mockup de contatos descreve a opção assim. O "Não Perturbe" **não** é contornável (medido na
fonte do Android na Phase 5, `LIMITACOES.md`).
**Proposta:** usar `contacts_option_never_silence_desc`, já corrigida na Phase 1 ("O Sentinela
nunca silencia sua lista de contatos. O 'Não Perturbe' do sistema continua valendo."). Sem
alternativa: a frase do mockup é uma promessa impossível.

Igualmente, `contacts_option_block_desc` do mockup ("enviando para a caixa postal") só é
verdade com `BlockMode.SILENT_VOICEMAIL`; a string existente já foi corrigida para "conforme o
modo de bloqueio em Proteção".

### 12.5 O dashboard classifica spam e "garante" segurança
"Provável Fraude Financeira" sob o número, e "Seu dispositivo está **seguro contra chamadas de
spam conhecidas**". O MVP não classifica spam nem conhece base de spam.
**Proposta:** motivo = rótulo real da decisão (`history_unknown_number` /
`history_private_number` / `call_origin_*`), e hero = `dashboard_device_safe` já corrigida
("protegido contra chamadas de números desconhecidos"). O ícone `info` ao lado do item, no
mockup, abriria o detalhe da classificação — **proposta:** o card inteiro navega para o
Histórico e o `info` sai (nada a explicar que a tela já não diga).

### 12.6 Configurações reais que nenhum mockup expõe
Não há mockup para: opt-in de notificação, verificação final, tela Proteção inteira, toggle
"Bloquear números privados" (que `TELAS.md` §4 exige no passo de contatos), retenção,
`BlockMode`, `FallbackPolicy`, chamada repetida, e todos os estados degradados.
**Proposta:** §6.6, §6.7, §7, §8 e §9 deste documento, derivados do design system com
acabamento equivalente. Se o usuário preferir mockups Stitch para Proteção antes da
implementação, este é o ponto de parar.

### 12.7 Imagens remotas em três mockups
`boas_vindas` (hero fotográfico) e `configura_o_whitelist` (fundo ilustrado) carregam
`lh3.googleusercontent.com`. **O app não declara `INTERNET`** — imagem remota é impossível, e
empacotar essas duas fotos custa peso de APK por decoração pura.
**Proposta:** substituir por superfície tonal com gradiente `SurfaceContainerLow` →
`PrimaryContainer` @20% mais ícone vetorial (é o que o próprio mockup `onboarding` faz na
"Integração Nativa": `linear-gradient`, sem foto). Nenhuma informação se perde.

### 12.8 Glassmorphism, blur e parallax
Os mockups usam `backdrop-filter: blur(10–12px)`, `bg-mesh` radial, `animate-float`,
`animate-ping` e **parallax no `mousemove`**.
**Proposta:** (a) parallax sai — não existe cursor em Android; (b) glass vira **camada tonal**
(`SurfaceContainerLow` + borda 1dp `OutlineVariant`), porque blur em tempo real na home custa
frame e a API só existe em SDK 31+; (c) `float` fica com amplitude 4dp e `ping` fica no dot de
status — ambos suprimidos com "reduzir animações", e nenhuma informação depende deles;
(d) o blur do `TopAppBar` ao rolar vira elevação tonal M3.

### 12.9 O que o switch do hero desliga, exatamente?
O mockup mostra um switch no card "Proteção Ativa" e o script troca o texto para "Proteção
desativada". Não diz se ele revoga o papel do sistema (impossível pelo app) ou desliga a
triagem interna.
**Proposta:** ele alterna `ScreeningSettings.protectionEnabled` — a triagem interna, imediata,
reversível. O **papel** do sistema é estado somente-leitura, mostrado no `InfoBanner` com o
botão "Corrigir configuração". Assim o switch nunca fica em desacordo com o mundo real: o
papel se consulta, a preferência se alterna.

### 12.10 Detalhes de dimensão que o mockup não fixa
- **Número das estatísticas:** o HTML usa a classe `text-display-lg-mobile`, que **não existe**
  na config — o PNG renderizou com tamanho de fallback pequeno. **Proposta:** `headlineLarge`
  32sp/40, w600.
- **Itens da bottom nav:** `px-5 py-1` dá altura desenhada ~32dp, **abaixo do mínimo de 48dp**.
  **Proposta:** item 56dp de altura, ícone 24dp + label 12sp, alvo 48×48 garantido — e teste
  afirmando **os dois eixos** (desenhado e área de toque), pela lição da Phase 6.
- **Granularidade do "há 15 min":** **proposta** — "agora" (<1 min), "há N min" (<1 h),
  "há N h" (<24 h), "ontem", depois data curta. Plurais via `plurals`, não concatenação.
- **Rodapé "Sentinela Guardian"** no mockup do passo 1: **sai** — branding é exatamente
  "Sentinela" (`CLAUDE.md`).
- Labels da bottom nav em inglês nos mockups → `nav_*` em pt-BR (já decidido em `TELAS.md`).

---

## 13. Acessibilidade — critérios de aceite (critério 4 da fase)

1. **TalkBack atravessa o fluxo inteiro** — boas-vindas → 6 passos → home → Proteção → volta —
   sem nó órfão, sem foco preso e sem controle inalcançável. É o critério de sucesso 4, não
   item de polimento.
2. **Alvos ≥ 48dp** em todo controle. Medidos: CTA 56dp, botão secundário 48dp, `OptionCard`
   ≥ 72dp, switch 48dp, item de bottom nav 56dp, ação do app bar 48dp, ação de banner 48dp.
   **Cada teste de toque afirma dois eixos:** área de toque **e** tamanho desenhado. A Phase 6
   pegou quatro bugs reais de layout exatamente assim (um botão de 72dp comprimido a 23dp e
   outro com altura zero) — afirmar só a área de toque mede o Compose, não o nosso layout.
3. **`contentDescription` em todo ícone acionável**, sempre de `strings.xml`. Ícone decorativo
   recebe `null` ou `clearAndSetSemantics {}` (hero, dot, barra de progresso, `check_circle`
   do option-card, marca d'água).
4. **`stateDescription` em todo controle de estado:** switches (`Role.Switch`), option-cards
   (`Role.RadioButton`), abas (`Role.Tab`), linha desabilitada (motivo textual).
5. **Nunca só cor.** Proteção ativa/desativada: cor **+** ícone **+** texto. Linhas de
   verificação: ícone **+** texto. Seleção de option-card: borda **+** `check_circle` **+**
   `Role.RadioButton`.
6. **Risco recorrente da semântica mesclada — pontos de atenção nomeados.** Envolver um
   componente com `mergeDescendants = true` faz o estado `enabled`/`checked` do filho
   desaparecer em silêncio (achado da Phase 6). Nesta fase o risco recorre em **cinco** lugares,
   e cada um deve ter teste explícito:
   (a) `StatusHeroCard` com o Switch dentro; (b) `SettingSwitchRow` (rótulo + explicação +
   switch); (c) `OptionCard` desabilitado; (d) CTA do passo 1 em "Solicitando permissão…"
   (`enabled = false`); (e) `CheckRow` com botão de ação embutido.
   Regra: **controle interativo nunca fica dentro de nó mesclado**; ou ele é o nó, ou fica
   irmão dele.
7. **Cabeçalhos:** o H1 de cada tela usa `semantics { heading() }`; cada grupo de Proteção
   também. TalkBack navega por cabeçalho — sem isso a tela Proteção é intransitável.
8. **`liveRegion = Polite`** em: resultado do pedido de papel, resultado do pedido de contatos,
   banners que aparecem depois da retomada, sub-opções que expandem. Nunca `Assertive`
   (roubaria o foco).
9. **Contraste ≥ 4.5:1** (corpo) e **≥ 3:1** (≥ 24sp e ícones), validado também **sob Dynamic
   Color**. Se um par cair abaixo, a tela cai para os tokens fixos do contrato. Os pares
   semânticos (§4.2) estão ≥ 7:1 por construção.
10. **`fontScale` 200%:** nenhum texto cortado, nenhum CTA fora da tela. O `OptionCard` cresce
    em altura (nunca elipsa a descrição); os cards de estatística crescem; a bottom nav mantém
    o ícone e elipsa o label. **Toda tela desta fase é rolável** — nenhuma depende de caber.
11. **Movimento:** com "reduzir animações", suprimir `float`, `ping`, `scale` de toque,
    transições de passo e expand/collapse. Nenhuma informação depende de animação.
12. **Testes de composição com qualificador real de tela** (`w411dp-h891dp-xxhdpi`): o
    dispositivo padrão do Robolectric é pequeno demais e reprova por motivo falso.
    Robolectric `@Config(sdk = [35])` — `[36]` exige Java 21 e o projeto está em JDK 17.

---

## 14. Fora de escopo desta fase (não é falha)

Telas de whitelist e histórico (Phase 8). Convite de avaliação, apoio/doação, privacidade
embutida, release R8, validação em Samsung físico com TalkBack real (Phase 9). Assets de fonte
Inter/Geist (`docs/backlog/fontes-inter-geist.md`). Redesenho das telas de chamada/discagem
(fechadas na Phase 6).

`docs/design/TELAS.md` §§1–6 e §10 devem ser reescritas a partir deste contrato no fechamento
da fase, no mesmo padrão da §11 (que a Phase 6 já converteu em contrato). Novos invariantes
candidatos a `scripts/verify-invariants.sh`: (a) nenhum literal de texto de UI em
`ui/onboarding|home|settings` fora de `stringResource`; (b) `PhoneMask` como único caminho de
exibição de número fora de `ui/call` e `ui/dialer`; (c) nenhum `0` renderizado como contagem
sem `historyEnabled`. Como todo guarda-corpo do projeto, cada um precisa de **prova de
vermelho** sobre código **já commitado**.

---

## 15. Registry Safety

| Registry | Blocos usados | Safety Gate |
|---|---|---|
| — | nenhum | não se aplica: projeto Android nativo, sem shadcn e sem registry de terceiros. Nenhuma dependência nova de UI nesta fase: os ícones vêm de `material-icons-extended` (já no projeto) e **nenhum asset é baixado em runtime** — o app não declara `INTERNET`. As duas imagens remotas dos mockups foram substituídas por superfície tonal (§12.7). |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending

---
phase: 6
slug: modo-discador-opcional
status: draft
shadcn_initialized: false
preset: none
created: 2026-07-29
---

# Phase 6 — Contrato de Design da UI (modo discador)

> Contrato visual e de interação das telas de chamada e discagem do Sentinela.
> Derivado do design system **"Silent Guardian"** (`docs/design/DESIGN.md`) e dos 8 mockups
> entregues em `docs/design/telas/`. **Não existe mockup de chamada nem de discagem** — estas
> telas são derivadas do mesmo sistema, com acabamento equivalente, por decisão explícita do
> usuário registrada em `06-CONTEXT.md` ("UI completa e polida desde o início").
>
> Escopo deste documento: **visual e interação apenas**. APIs de Telecom, ciclo de vida do
> `InCallService`, elegibilidade de papel e degradação técnica vivem no `06-RESEARCH.md`.

---

## Design System

| Propriedade | Valor |
|-------------|-------|
| Ferramenta | nenhuma (Android nativo — Jetpack Compose + Material 3) |
| Preset | não se aplica (shadcn é irrelevante em Android) |
| Biblioteca de componentes | `androidx.compose.material3` + tokens próprios em `ui/theme/` |
| Biblioteca de ícones | **Material Symbols Outlined** (mesma dos mockups), via `material-icons-extended` ou drawables vetoriais |
| Fontes | **Inter** (texto) e **Geist** (números de telefone e labels) |
| Tema | dark-first; `SentinelaTheme` com Dynamic Color (Monet) ligado |

**Pendência herdada que esta fase resolve:** `Type.kt` hoje é `Typography()` padrão do M3 —
sem Inter/Geist. Esta fase **é** a fase de UI: os assets de fonte entram aqui (ver
[Tipografia](#tipografia)), porque o número de telefone em Geist é requisito visual das telas
de chamada e de discagem, não enfeite.

### Dynamic Color nas telas de chamada — regra de segurança

Dynamic Color continua ligado, **exceto** para as três cores funcionais da chamada
(atender / recusar / encerrar). Essas usam valores fixos do contrato, nunca `colorScheme`,
porque um wallpaper pode aproximar o verde de atender do vermelho de recusar e produzir um
erro irreversível (recusar por engano uma chamada real). Documentar isso no KDoc do
composable que define as cores.

---

## Escala de espaçamento

Grid Android de 8dp, idêntica aos mockups (`DESIGN-TOKENS-ORIGINAL.md`).

| Token | Valor | Uso nesta fase |
|-------|-------|----------------|
| xs | 4dp | gap ícone↔label, padding interno de chip |
| sm | 8dp | gutter, espaço entre teclas do teclado |
| md | 16dp | margem lateral da tela, padding interno de card |
| lg | 24dp | espaço entre blocos (identidade ↔ controles) |
| xl | 32dp | respiro entre a fileira de controles e a barra de ação |
| 2xl | 48dp | folga superior do bloco de identidade na chamada recebida |
| 3xl | 64dp | folga inferior da barra atender/recusar em tela cheia |

**Exceções declaradas (todas múltiplas de 4):**
- Botão circular de atender/recusar: **72dp** de diâmetro (acima do mínimo de 64dp do
  `TELAS.md` §11; erro nesses dois botões é caro demais).
- Botão de encerrar na chamada ativa: **64dp**.
- Tecla do teclado (discagem e DTMF): **72dp** de diâmetro, gap 8dp entre teclas.
- Controle secundário (mudo/viva-voz/teclado): **56dp** de diâmetro, alvo de toque 56dp.
- Avatar/monograma da identidade na chamada: **96dp**.
- Linha de lista densa: 56dp (herdado do design system).

---

## Tipografia

Escala idêntica à dos mockups. **Inter** para texto, **Geist** para número de telefone e labels.

| Papel | Família | Tamanho | Peso | Line height | Uso nesta fase |
|-------|---------|---------|------|-------------|----------------|
| display-lg | Inter | 57sp | 600 | 64sp | não usado nesta fase |
| headline-lg-mobile | Inter | 28sp | 600 | 36sp | H1 da tela de ativação do modo discador |
| headline-md | Inter | 28sp | 500 | 36sp | nome do contato na chamada recebida |
| title-lg | Inter | 22sp | 500 | 28sp | título de seção, rótulo de estado ("Chamando…") |
| body-lg | Inter | 16sp | 400 | 24sp (ls 0.5) | corpo explicativo da ativação |
| body-md | Inter | 14sp | 400 | 20sp (ls 0.25) | descrição secundária, notas honestas |
| label-lg | **Geist** | 14sp | 500 | 20sp (ls 0.1) | labels dos controles (Mudo, Viva-voz…) |
| label-md | **Geist** | 12sp | 500 | 16sp (ls 0.5) | chip de origem, microcopy |
| **number-xl** (novo) | **Geist** | 32sp | 500 | 40sp (ls 0.5) | número digitado na tela de discagem |
| **number-lg** (novo) | **Geist** | 24sp | 500 | 32sp (ls 0.5) | número na tela de chamada (quando é o identificador principal) |
| **timer** (novo) | **Geist** | 16sp | 500 | 24sp, **tabular figures** | cronômetro da chamada ativa |

Regras:
- `number-xl`, `number-lg` e `timer` entram como extensões do `SentinelaTypography`
  (nomeadas, não estilos inline), e usam **figuras tabulares** — sem isso o cronômetro
  "pula" a cada segundo, o que é visível e sujo.
- Corpo mínimo 14sp em qualquer tela.
- Todo texto respeita `fontScale` do sistema até 200%: as telas de chamada e discagem usam
  layout que **não corta** o número nem os controles em fonte grande (número quebra em duas
  linhas com `autosize`/`BasicText` reduzindo até 20sp; controles nunca somem).

---

## Cor

Paleta dark canônica (`Color.kt`, travada por `ThemeTokensTest`).

| Papel | Valor | Uso |
|-------|-------|-----|
| Dominante (60%) | `#081425` `Surface` | fundo das telas de chamada e discagem |
| Secundária (30%) | `#152031` / `#1F2A3C` / `#2A3548` (`SurfaceContainer*`) | teclas, cards, controles inativos, sheet |
| Acento (10%) | `#ADC6FF` `Primary` | ver lista de reserva abaixo |
| Destrutiva | `#FFB4AB` `Error` / `#93000A` `ErrorContainer` | apenas recusar, encerrar e confirmação de reversão |

**Acento (`Primary`) reservado exclusivamente para:**
1. Botão de chamar da tela de discagem (`CallButton`).
2. CTA primário da tela de ativação do modo discador.
3. Estado **ativo** de um controle da chamada (mudo ON, viva-voz ON, teclado aberto):
   fundo `Primary`, ícone `OnPrimary`.
4. Anel/monograma do avatar quando quem liga é **contato conhecido**.
5. Ícone do escudo Sentinela na marca d'água da tela de chamada.

Nunca usar `Primary` em: texto corrido, ícones inativos, bordas de container, teclas do
teclado em repouso, chips de origem.

### Cores funcionais da chamada (fixas, fora do Dynamic Color)

| Ação | Fundo | Conteúdo | Origem |
|------|-------|----------|--------|
| Atender | `#1E6E42` (verde de ação, baixa chroma) | `#D9F2E3` | "Action Colors: verde permitido", `DESIGN.md` |
| Recusar / Encerrar | `#93000A` `ErrorContainer` | `#FFDAD6` `OnErrorContainer` | token existente |

Contraste verificado: conteúdo sobre fundo ≥ 7:1 em ambos (AAA para texto grande e ícones).
Ambos os botões têm, além da cor, **ícone distinto** (`call` × `call_end`) e **label textual**
sob o botão — estado nunca comunicado só por cor (`DESIGN.md`, acessibilidade).

### Chips de origem (pill, cor a ~15% + texto saturado)

| Origem | Fundo | Texto/Ícone | Label |
|--------|-------|-------------|-------|
| Contato | `SecondaryContainer` | `OnSecondaryContainer` | "Contato" (`person`) |
| Whitelist | `Primary` @ 15% | `Primary` | "Permitido" (`verified_user`) |
| Desconhecido | `SurfaceContainerHighest` | `OnSurfaceVariant` | "Desconhecido" (`help`) |
| Privado / sem identificação | `SurfaceContainerHighest` | `OnSurfaceVariant` | "Número privado" (`visibility_off`) |

---

## Regra de privacidade na exibição do número — decisão explícita

**Nas telas de chamada e de discagem o número aparece COMPLETO e formatado.**

Justificativa: é a chamada do próprio usuário, acontecendo agora, e ele precisa do número
inteiro para decidir se atende. Um discador que mascara o número que está tocando é inútil.
O `CLAUDE.md` proíbe número completo em **log, notificação e crash report** — não na tela de
chamada, que é exatamente o local onde o número é o produto.

Fronteira, obrigatória e testável:
- Tela de chamada / discagem / DTMF → número **completo**, `PhoneNumbers` formatado, Geist.
- Log, notificação, histórico, `contentDescription` capturado em bugreport, telemetria de
  crash → **`PhoneMask.mask`**, sempre.
- Chamada de origem **privada/oculta** → nunca inventar rótulo: `history_private_number`
  ("Privado") + `history_private_id` ("ID Oculto"), nada de "Desconhecido".

---

## Telas e estados

Convenções gerais das telas de chamada:
- Fundo `Surface` puro, sem glass. Glassmorphism é para conteúdo rolável sob app bar; a tela
  de chamada não rola e não pode ter blur competindo com legibilidade a 1 metro de distância.
- Marca d'água: escudo `shield` 24dp + "Sentinela" em `label-md`, `OnSurfaceVariant` @ 60%,
  topo, centralizado, 16dp abaixo da status bar. Serve para o usuário entender **por que** a
  tela de chamada mudou.
- Sem gesto de voltar (`BackHandler` consome) nas telas de chamada em curso.
- Sem nenhuma ação destrutiva a menos de 24dp de outra ação (evita recusa acidental).

---

### 1. Chamada recebida — tela cheia (`IncomingCallScreen`)

Tela de maior risco do app. Aparece via `setFullScreenIntent` no canal de chamada
(**nunca** `SYSTEM_ALERT_WINDOW`), inclusive sobre a tela bloqueada.

Layout (coluna, 100% altura, margem lateral 16dp):

```
[ 48dp folga ]
  marca d'água Sentinela
[ 48dp ]
  rótulo de estado ...... title-lg, OnSurfaceVariant ... "Chamada recebida"
[ 24dp ]
  avatar 96dp ........... circular
[ 16dp ]
  identidade primária ... headline-md, OnSurface
[ 8dp ]
  identidade secundária . number-lg (Geist), OnSurfaceVariant
[ 8dp ]
  chip de origem ........ pill, label-md
[ ---- weight(1f), empurra as ações para baixo ---- ]
  linha de ações ........ Recusar (esq.) | Atender (dir.), 72dp cada,
                          espaçados por weight, com label sob cada botão
[ 64dp folga inferior + navigationBars insets ]
```

**Variantes de identidade (as três, obrigatórias):**

| Caso | Avatar | Primária (headline-md) | Secundária (number-lg) | Chip |
|------|--------|------------------------|------------------------|------|
| Contato da agenda | foto da agenda; se ausente, monograma das iniciais sobre `SecondaryContainer`, anel 2dp `Primary` | nome do contato | número completo formatado | "Contato" |
| Número na whitelist | ícone `verified_user` sobre `Primary` @ 15% | descrição da entrada da whitelist, se houver; senão o número completo | número completo (omitido se já for a primária) | "Permitido" |
| Desconhecido com número | ícone `person` sobre `SurfaceContainerHighest` | **número completo formatado** (`number-lg` promovido a headline; único caso em que o número é a linha primária) | região/operadora **não** é exibida (o MVP não a conhece — nunca inventar) | "Desconhecido" |
| Privado / sem identificação | ícone `visibility_off` sobre `SurfaceContainerHighest` | "Privado" | "ID Oculto" | "Número privado" |

A foto do contato é lida **em memória** no momento da chamada e nunca cacheada em disco
(`CLAUDE.md`, privacidade). Se o `READ_CONTACTS` estiver revogado, a tela degrada para o caso
"Desconhecido com número" — sem erro, sem alerta.

**Política por contato nesta tela — decisão de design:**
A tela de chamada recebida **não expõe nenhum controle de política**. O chip de origem é
passivo: informa *por que* está tocando, e nada mais. Motivos: (a) mudar política durante um
telefone tocando é decisão sob pressão, exatamente o padrão escuro que o projeto proíbe;
(b) qualquer toque perto dos botões de atender/recusar aumenta a chance de erro irreversível.
Ajuste de política vive em Proteção (Phase 7) e no item do histórico (Phase 8).

**Ações:**

| Ação | Forma | Cor | Ícone | Label sob o botão | contentDescription |
|------|-------|-----|-------|-------------------|--------------------|
| Recusar | círculo 72dp | fundo `#93000A`, ícone `#FFDAD6` | `call_end` | "Recusar" | "Recusar chamada" |
| Atender | círculo 72dp | fundo `#1E6E42`, ícone `#D9F2E3` | `call` | "Atender" | "Atender chamada" |

- Feedback de toque: `scale 0.95` (mesma micro-interação dos mockups), 120 ms, + haptic
  `CONFIRM`.
- **Não** implementar swipe-to-answer no MVP: gesto sem affordance clara é pior que dois
  botões grandes e falha com TalkBack. Registrar como ideia futura, não como falta.
- O toque só dispara na **liberação** dentro do alvo (comportamento padrão de `Button` no
  Compose) — evita atender por encostar ao tirar o telefone do bolso.

**TalkBack:** a tela anuncia, na ordem: marca ("Sentinela"), estado ("Chamada recebida"),
identidade, chip de origem, e então os dois botões. Ordem de foco explícita via
`traversalIndex`. `liveRegion = Polite` no rótulo de estado, para anunciar transições
(recebida → ativa → encerrada) sem roubar o foco dos botões.

---

### 2. Chamada de saída / discando (`OutgoingCallScreen`)

Mesmo esqueleto da recebida, com três diferenças:
- Rótulo de estado: "Chamando…" e depois "Tocando…" (`liveRegion = Polite`).
- Indicador de progresso: três pontos de 6dp em `Primary` com animação de fade sequencial
  (1200 ms, loop), abaixo do rótulo. Nunca um `CircularProgressIndicator` girando — ruído
  visual em tela cheia.
- Uma única ação: **Encerrar**, círculo 64dp centralizado, cor destrutiva.
- Os controles de mudo/viva-voz já aparecem, **habilitados**, na fileira secundária (viva-voz
  antes de atender é comportamento esperado de discador).

---

### 3. Chamada ativa (`ActiveCallScreen`)

```
[ 48dp ]  marca d'água
[ 32dp ]  identidade (mesma composição da recebida, avatar 96dp)
[ 8dp  ]  cronômetro ...... timer (Geist tabular), OnSurfaceVariant, "MM:SS" e "H:MM:SS" após 1h
[ ---- weight(1f) ---- ]
          fileira de controles: 3 colunas × 56dp, gap 24dp
            [ Mudo ]  [ Teclado ]  [ Viva-voz ]
[ 32dp ]
          botão Encerrar 64dp, centralizado, cor destrutiva, label "Encerrar"
[ 64dp + insets ]
```

**Controles secundários (`CallControlButton`):**

| Controle | Ícone OFF | Ícone ON | Label | contentDescription (estado incluído) |
|----------|-----------|----------|-------|--------------------------------------|
| Mudo | `mic` | `mic_off` | "Mudo" | "Mudo, desativado" / "Mudo, ativado" |
| Teclado | `dialpad` | `dialpad` | "Teclado" | "Teclado numérico, fechado" / "…, aberto" |
| Viva-voz | `volume_up` | `volume_up` | "Viva-voz" | "Viva-voz, desativado" / "Viva-voz, ativado" |

Especificação do botão:
- Círculo 56dp; alvo de toque 56dp (≥ 48dp exigido).
- **OFF:** fundo `SurfaceContainerHighest`, ícone `OnSurfaceVariant`.
- **ON:** fundo `Primary`, ícone `OnPrimary`, + anel externo 2dp `Primary` @ 40%.
- **Desabilitado** (rota de áudio indisponível): fundo `SurfaceContainerLow`, ícone
  `OnSurfaceVariant` @ 38%, `enabled = false`, `stateDescription = "indisponível"`.
- Label em `label-lg` (Geist) 8dp abaixo do círculo, `OnSurfaceVariant`; em ON vira `Primary`.
- Estado ON é comunicado por **cor + ícone (quando existe variante) + `Modifier.toggleable`
  com `Role.Switch`** — TalkBack anuncia "ativado/desativado" sozinho. Nunca só por cor.
- Transição OFF↔ON: `animateColorAsState` 150 ms `FastOutSlowInEasing` + haptic
  `TOGGLE_ON`/`TOGGLE_OFF`.

**Se houver mais de uma rota de áudio (fone Bluetooth conectado):** o botão de viva-voz vira
seletor — toque longo, ou toque simples quando há ≥ 3 rotas, abre um bottom sheet
(radius 24dp, `SurfaceContainerHigh`) listando "Alto-falante / Telefone / <dispositivo BT>"
com rádio-item e `check` na rota ativa. Sem popup flutuante.

---

### 4. Teclado DTMF sobre a chamada ativa (`DtmfKeypadSheet`)

- Aparece como painel **ancorado ao rodapé**, ocupando ~70% da altura, `SurfaceContainerLow`,
  cantos superiores 24dp. Não é modal de sistema: o cronômetro e o botão Encerrar continuam
  visíveis e clicáveis acima dele.
- Entrada: `slideInVertically` 250 ms `FastOutSlowInEasing` + fade. Saída: 200 ms.
- Topo do painel: linha de dígitos enviados, `number-lg` (Geist), alinhada à direita,
  scroll horizontal, `OnSurface`. Vazia no início — **sem placeholder**.
- Grade 3×4 de teclas 72dp, gap 8dp, centralizada, margem lateral 16dp:
  `1 2 3 / 4 5 6 / 7 8 9 / * 0 #`.
- Tecla: círculo 72dp, fundo `SurfaceContainerHighest`, dígito em `number-xl` (Geist) e
  letras (`ABC`, `DEF`…) em `label-md` `OnSurfaceVariant` sob o dígito. `0` mostra `+`
  como letra secundária.
- Feedback: `scale 0.92` 100 ms + haptic `KEYBOARD_TAP` + o tom DTMF real (o tom é
  responsabilidade do Telecom, não da UI).
- Fechar: botão `keyboard_arrow_down` 48dp centralizado no topo do painel
  (contentDescription "Fechar teclado"), + toque no botão Teclado da fileira, + back gesture.
- TalkBack: cada tecla com `contentDescription` = o dígito falado ("um", "dois", "asterisco",
  "jogo da velha" → usar "tecla asterisco" / "tecla sustenido").

---

### 5. Tela de discagem (`DialpadScreen`) — alvo de `ACTION_DIAL`

Entrada do app como discador. Precisa ficar boa nos dois contextos: aberta pelo usuário e
aberta pelo sistema com um `tel:` pré-preenchido.

```
TopAppBar (h 64dp, transparente sobre Surface; blur ao rolar)
   escudo + "Sentinela"                      [ícone contatos, 48dp, opcional]
[ ---- weight(1f) ---- ]
  campo do número .... number-xl (Geist), centralizado, OnSurface,
                       autosize 32sp→20sp, quebra em 2 linhas no máximo
                       vazio: nada (sem placeholder, sem cursor piscando)
[ 8dp ]
  sugestão ........... quando o número casa com contato/whitelist:
                       "Nome do contato" em body-md Primary; nunca inventar
[ 24dp ]
  grade 3×4 de teclas 72dp, gap 8dp  (idêntica à do DTMF)
[ 24dp ]
  linha de ação:  [ vazio 56dp ]  [ Chamar 72dp ]  [ Apagar 56dp ]
[ 32dp + insets ]
```

- **Chamar:** círculo 72dp, fundo `Primary`, ícone `call` `OnPrimary`.
  Desabilitado (alpha 38%, `enabled = false`) enquanto o campo estiver vazio.
  contentDescription: "Ligar para <número lido dígito a dígito>".
- **Apagar:** ícone `backspace` 56dp, `OnSurfaceVariant`, sem fundo. Toque = 1 dígito;
  toque longo = limpa tudo (com haptic `LONG_PRESS`). Invisível (`alpha 0`, não clicável)
  quando o campo está vazio — não some do layout, para a grade não pular.
- Toque longo em `0` insere `+`. Toque longo em `1` **não** faz nada no MVP (sem caixa postal).
- O campo do número é **somente saída** — não abre o teclado do sistema; foco de acessibilidade
  o expõe como `Role.Text` com `liveRegion = Polite` (TalkBack anuncia o número a cada dígito).
- Formatação progressiva pt-BR conforme digita (`(11) 91234-5678`); se o padrão não casar,
  exibe os dígitos crus sem tentar adivinhar.

---

### 6. Tela de ativação do modo discador (`DialerActivationScreen`)

Alcançada por Proteção → "Modo discador (avançado)". Copy honesta é o requisito principal.

```
TopAppBar com back + "Modo discador"
[ 24dp ]
  ícone dial 48dp sobre círculo 96dp SurfaceContainerHigh
[ 16dp ]
  H1 headline-lg-mobile: "Tornar o Sentinela seu telefone padrão?"
[ 8dp ]
  body-lg OnSurfaceVariant: parágrafo de contexto
[ 24dp ]
  CARD "O que muda"        — SurfaceContainerLow, radius 16dp, padding 16dp,
                             borda 1dp OutlineVariant, ícone check_circle Primary por item
[ 16dp ]
  CARD "O que NÃO muda"    — SurfaceContainerLow, radius 16dp, padding 16dp,
                             borda 1dp OutlineVariant, ícone info OnSurfaceVariant por item
[ 16dp ]
  CARD de pré-requisito    — só quando READ_CONTACTS está negada:
                             ErrorContainer @ 15%, ícone contacts, texto + botão tonal
[ 32dp ]
  CTA pill full-width h-56dp: "Escolher o Sentinela" (Primary/OnPrimary)
  desabilitado enquanto READ_CONTACTS estiver negada
[ 8dp ]
  microcopy label-md centralizada: "Quem decide é o Android. Você pode voltar quando quiser."
```

**Quando o modo já está ativo**, a mesma tela vira o painel de reversão:
- Chip "Ativo" (`Primary` @ 15%) sob o H1.
- Card único "O que está valendo agora".
- CTA vira **botão tonal** (`SecondaryContainer`/`OnSecondaryContainer`), não destrutivo:
  "Escolher outro telefone". Reverter não é destruir dado — não usar cor de erro.
- Ao tocar, abre o seletor do sistema. O app **nunca** força a troca.

**Estado "papel perdido silenciosamente":** banner `SurfaceContainerHigh` com borda esquerda
4dp `Primary`, ícone `info`, texto "O Sentinela deixou de ser o telefone padrão. O modo filtro
continua funcionando." + link "Ver como reativar". Tom informativo, sem alarme, sem vermelho —
nada quebrou.

---

### 7. Estados de erro e degradação

| Estado | Onde aparece | Tratamento visual |
|--------|--------------|-------------------|
| Falha ao iniciar a chamada de saída | `DialpadScreen` | Snackbar `SurfaceContainerHigh`, 4 s, texto + ação "Tentar de novo". Número **permanece** no campo — nunca apagar o que o usuário digitou. |
| Chamada caiu / encerrada pelo outro lado | tela de chamada | rótulo de estado vira "Chamada encerrada", cronômetro congela, controles somem com fade 200 ms, tela fecha após **1200 ms** (tempo de ler; não fechar instantâneo) |
| Recusada pela rede / ocupado | tela de chamada | mesmo tratamento, rótulo "Não foi possível completar a chamada" |
| Rota de áudio indisponível | chamada ativa | botão desabilitado (spec acima), sem diálogo |
| `READ_CONTACTS` revogada durante uso | tela de chamada | degrada para identidade "Desconhecido com número". **Nenhum aviso durante a chamada.** O aviso aparece na home (Phase 7). |
| Papel de discador perdido | ativação + home | banner informativo (§6) |
| Falha do `InCallService` | — | fora da UI: caminho de degradação técnica, `06-RESEARCH.md`. A UI **nunca** mostra tela de crash sobre uma chamada. |

Nenhum estado de erro usa diálogo modal sobre uma chamada em curso.

---

## Copywriting Contract

Toda string abaixo vai para `res/values/strings.xml` em pt-BR. **Nenhum texto hardcoded em
Kotlin.** Nomes de recurso propostos (o executor pode renomear, mas não pode inline).

### Telas de chamada

| Chave | Texto |
|-------|-------|
| `call_incoming_state` | Chamada recebida |
| `call_dialing_state` | Chamando… |
| `call_ringing_state` | Tocando… |
| `call_active_state` | Em chamada |
| `call_ended_state` | Chamada encerrada |
| `call_failed_state` | Não foi possível completar a chamada |
| `call_action_answer` | Atender |
| `call_action_reject` | Recusar |
| `call_action_hangup` | Encerrar |
| `call_control_mute` | Mudo |
| `call_control_speaker` | Viva-voz |
| `call_control_keypad` | Teclado |
| `call_origin_contact` | Contato |
| `call_origin_whitelist` | Permitido |
| `call_origin_unknown` | Desconhecido |
| `call_origin_private` | Número privado |
| `call_audio_route_title` | Saída de áudio |
| `call_audio_route_earpiece` | Telefone |
| `call_audio_route_speaker` | Alto-falante |

### Discagem

| Chave | Texto |
|-------|-------|
| `dialpad_title` | Discar |
| `dialpad_call` | Ligar |
| `dialpad_error_failed` | Não foi possível iniciar a chamada. |
| `dialpad_error_retry` | Tentar de novo |

### Ativação do modo discador — copy honesta (bloco crítico)

| Chave | Texto |
|-------|-------|
| `dialer_activation_title` | Tornar o Sentinela seu telefone padrão? |
| `dialer_activation_intro` | No modo discador, o Sentinela passa a receber todas as chamadas — inclusive as de quem está na sua agenda. É o único jeito de as políticas por origem valerem também para os seus contatos. |
| `dialer_activation_changes_title` | O que muda |
| `dialer_activation_change_1` | O Sentinela vira seu app de telefone: a tela de chamada e o teclado passam a ser os dele. |
| `dialer_activation_change_2` | A triagem passa a ver todas as chamadas, não só as de números fora da agenda. |
| `dialer_activation_change_3` | A política que você escolheu para contatos passa a ser aplicada de fato. O padrão continua sendo **Tocar** — ativar o modo discador, sozinho, não bloqueia ninguém. |
| `dialer_activation_unchanged_title` | O que não muda |
| `dialer_activation_unchanged_1` | As chamadas continuam sendo registradas no histórico do telefone. Ser o app de telefone padrão não dá ao Sentinela permissão para omitir esse registro — no Android, só apps de operadora podem. |
| `dialer_activation_unchanged_2` | O "Não Perturbe" do sistema continua valendo por cima das configurações do Sentinela. |
| `dialer_activation_unchanged_3` | Chamadas de WhatsApp, Telegram e outros apps de internet continuam fora do alcance do Sentinela. |
| `dialer_activation_unchanged_4` | O Sentinela continua 100% offline: nenhuma chamada, número ou contato sai do aparelho. |
| `dialer_activation_limits_title` | Limitações |
| `dialer_activation_limit_calls` | Uma chamada por vez: chamada em espera e conferência não são suportadas nesta versão. |
| `dialer_activation_contacts_required` | O modo discador precisa da permissão de leitura da agenda. Sem ela, o Sentinela não consegue distinguir seus contatos e passaria a tratar todo mundo como desconhecido. |
| `dialer_activation_grant_contacts` | Permitir leitura da agenda |
| `dialer_activation_cta` | Escolher o Sentinela |
| `dialer_activation_cta_hint` | Quem decide é o Android. Você pode voltar quando quiser. |
| `dialer_active_chip` | Ativo |
| `dialer_active_title` | O Sentinela é seu telefone padrão |
| `dialer_revert_cta` | Escolher outro telefone |
| `dialer_revert_hint` | Ao voltar para outro app de telefone, o modo filtro do Sentinela continua funcionando normalmente — sem reinstalar nem reconfigurar nada. |
| `dialer_role_lost_title` | O Sentinela deixou de ser o telefone padrão. |
| `dialer_role_lost_body` | O modo filtro continua funcionando: chamadas de números fora da agenda seguem sendo triadas. |
| `dialer_role_lost_action` | Ver como reativar |

**Proibições de copy nesta fase, verificáveis por revisão:**
- Nunca escrever que o bloqueio é garantido, total, 100% ou infalível.
- Nunca sugerir que o modo discador esconde a chamada do histórico do telefone.
- Nunca prometer filtragem de WhatsApp/VoIP.
- Nenhum superlativo de marketing ("o melhor", "definitivo", "poderoso").
- Nenhum texto que pressione a ativação ("recomendado", "ative agora", contador, urgência).
  A ativação é opt-in neutro. A tela apresenta custo e benefício com o mesmo peso visual —
  os cards "O que muda" e "O que não muda" têm **estilo idêntico**, deliberadamente.

### Confirmações destrutivas

| Ação | Tratamento |
|------|-----------|
| Recusar chamada | **sem confirmação** — botão único, 72dp, separado 24dp+ do atender. Confirmar aqui seria pior. |
| Encerrar chamada | sem confirmação, mesmo motivo. |
| Limpar número digitado (toque longo no backspace) | sem diálogo; haptic longo é o feedback. |
| Reverter o modo discador | sem diálogo próprio — o seletor do sistema **é** a confirmação. |

---

## Acessibilidade — requisitos verificáveis

Não é checklist decorativo: cada item abaixo é critério de aceite da fase.

1. **Alvos de toque:** ≥ 48dp em todo controle. Medidos: atender/recusar 72dp, encerrar 64dp,
   teclas 72dp, controles secundários 56dp, backspace 56dp, fechar teclado 48dp.
2. **contentDescription:** todo ícone acionável tem descrição em `strings.xml`, incluindo o
   estado quando o controle é toggle (`stateDescription` ou `Role.Switch`).
3. **Nunca só cor:** atender/recusar diferem por ícone e label; mudo/viva-voz/teclado diferem
   por ícone e/ou `stateDescription`.
4. **Contraste:** todo par texto/fundo ≥ 4.5:1 (corpo) e ≥ 3:1 (texto ≥ 24sp e ícones).
   Os pares funcionais verde/vermelho estão ≥ 7:1. Validar também sob Dynamic Color: se um
   par cair abaixo do mínimo, a tela cai para os tokens fixos do contrato.
5. **TalkBack na chamada recebida:** ordem de foco declarada por `traversalIndex`
   (marca → estado → identidade → chip → recusar → atender). Nenhum elemento decorativo
   focável (`clearAndSetSemantics` na marca d'água e no indicador de progresso).
6. **Número lido corretamente:** o `contentDescription` do número usa dígitos separados
   (`"9 1 2 3 4"`), não a leitura como valor numérico. Vale na discagem e na chamada.
7. **`fontScale` 200%:** nenhum controle sai da tela nem se sobrepõe; o número reduz por
   autosize, os botões **não** reduzem.
8. **Movimento:** com "reduzir animações" ligado no sistema, o ponto pulsante, o fade
   sequencial dos três pontos e o `scale` de toque são suprimidos; transições viram troca
   instantânea. Nenhuma informação depende de animação.
9. **Tela bloqueada:** a chamada recebida é legível e operável sem desbloquear; nenhuma
   informação além do que a tela de chamada já mostra vaza para a lock screen.
10. **Landscape:** a chamada recebida e a ativa têm layout de duas colunas (identidade à
    esquerda, ações à direita) — nunca cortar os botões de ação.

---

## Inventário de componentes

Cada composable novo, o arquivo sugerido e os tokens do tema que consome.

| Composable | Arquivo sugerido | Tokens usados |
|------------|------------------|---------------|
| `IncomingCallScreen` | `ui/call/IncomingCallScreen.kt` | `Surface`, `OnSurface`, `OnSurfaceVariant`; tipografia `title-lg`, `headline-md`, `number-lg` |
| `OutgoingCallScreen` | `ui/call/OutgoingCallScreen.kt` | idem + `Primary` (pontos de progresso) |
| `ActiveCallScreen` | `ui/call/ActiveCallScreen.kt` | idem + `timer` |
| `CallerIdentity` | `ui/call/CallerIdentity.kt` | `SecondaryContainer`, `OnSecondaryContainer`, `SurfaceContainerHighest`, `Primary` (anel), `headline-md`, `number-lg` |
| `CallOriginChip` | `ui/call/CallOriginChip.kt` | `SecondaryContainer`/`OnSecondaryContainer`, `Primary` @15%, `SurfaceContainerHighest`/`OnSurfaceVariant`, `label-md`, pill |
| `AnswerRejectBar` | `ui/call/AnswerRejectBar.kt` | verde fixo `#1E6E42`/`#D9F2E3`, `ErrorContainer`/`OnErrorContainer`, `label-lg` |
| `CallActionButton` | `ui/call/CallActionButton.kt` | cor recebida por parâmetro; forma circular; `label-lg` |
| `CallControlButton` | `ui/call/CallControlButton.kt` | `SurfaceContainerHighest`/`OnSurfaceVariant` (OFF), `Primary`/`OnPrimary` (ON), `label-lg` |
| `CallTimer` | `ui/call/CallTimer.kt` | `OnSurfaceVariant`, tipografia `timer` (tabular) |
| `DtmfKeypadSheet` | `ui/call/DtmfKeypadSheet.kt` | `SurfaceContainerLow`, radius 24dp topo, `number-lg` |
| `AudioRouteSheet` | `ui/call/AudioRouteSheet.kt` | `SurfaceContainerHigh`, radius 24dp, `body-lg`, `Primary` (check) |
| `DialpadScreen` | `ui/dialer/DialpadScreen.kt` | `Surface`, `number-xl`, `Primary`/`OnPrimary` (chamar) |
| `DialpadGrid` | `ui/dialer/DialpadGrid.kt` | `SurfaceContainerHighest`, `OnSurface`, `OnSurfaceVariant`, `number-xl`, `label-md` |
| `DialpadKey` | `ui/dialer/DialpadKey.kt` | idem, círculo 72dp |
| `NumberDisplay` | `ui/dialer/NumberDisplay.kt` | `OnSurface`, `number-xl` autosize, `Primary` (sugestão) |
| `DialerActivationScreen` | `ui/dialer/DialerActivationScreen.kt` | `SurfaceContainerLow`, `OutlineVariant`, `Primary`/`OnPrimary`, `SecondaryContainer`, `headline-lg-mobile`, `body-lg`, `body-md` |
| `HonestyCard` | `ui/components/HonestyCard.kt` | `SurfaceContainerLow`, `OutlineVariant`, radius 16dp, padding 16dp — reutilizável nas Phases 7/8 |
| `InfoBanner` | `ui/components/InfoBanner.kt` | `SurfaceContainerHigh`, borda esquerda 4dp `Primary`, `body-md` |
| `SentinelaWatermark` | `ui/components/SentinelaWatermark.kt` | `OnSurfaceVariant` @60%, `label-md`, `clearAndSetSemantics` |

**Extensões necessárias no tema (arquivos existentes):**
- `Type.kt` — adicionar as famílias Inter e Geist (assets em `res/font/`) e os estilos
  `numberXl`, `numberLg`, `timer` (com `FontFeatureSetting("tnum")`). Atenção: `ThemeTokensTest`
  trava os tokens atuais; a extensão não pode alterar nenhum valor existente.
- `Color.kt` — adicionar `CallAccept` (`#1E6E42`) e `OnCallAccept` (`#D9F2E3`). Nenhum token
  existente muda.
- `Shape.kt` (novo) — 8dp / 16dp / 24dp / pill, para parar de espalhar `RoundedCornerShape`.

---

## Fora de escopo desta fase (não é falha)

Chamada em espera, segunda chamada, conferência, transferência, vídeo, gravação, mensagem
rápida ao recusar, swipe-to-answer, histórico de chamadas do discador, busca de contatos na
tela de discagem. "Uma chamada por vez" vai para `docs/LIMITACOES.md` no mesmo trabalho.

`docs/design/TELAS.md` §11 deve ser reescrita a partir deste contrato no fechamento da fase —
hoje ela é um esboço de 6 linhas.

---

## Registry Safety

| Registry | Blocos usados | Safety Gate |
|----------|---------------|-------------|
| — | nenhum | não se aplica: projeto Android nativo, sem shadcn e sem registry de terceiros. Nenhuma dependência nova de UI além dos assets de fonte (Inter/Geist, SIL OFL, empacotadas localmente — não baixadas em runtime, o app não tem `INTERNET`). |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending

# Design System — "Silent Guardian"

> Adaptado do design system dos mockups Stitch (tokens originais preservados em
> [`DESIGN-TOKENS-ORIGINAL.md`](DESIGN-TOKENS-ORIGINAL.md)).
> Tokens implementados em `app/src/main/java/org/sentinela/app/ui/theme/`.

## Conceito

"O Guardião Silencioso": estética **minimalista/corporate modern**, discreta como uma
extensão do sistema — invisível até ser necessária. Resposta emocional: alívio e controle.
Sem decoração desnecessária; indicadores de status claros e fluxos de alta utilidade.
Dark-first (o app "mora" em segundo plano), com Dynamic Color (Monet) respeitando contraste.

## Cores (dark — paleta canônica)

| Token M3 | Hex | Uso |
|----------|-----|-----|
| surface / background | `#081425` | Fundo base ("Deepest Blue") |
| surface-container-lowest | `#040E1F` | Camada mais funda |
| surface-container-low | `#111C2D` | Cards discretos |
| surface-container | `#152031` | Cards padrão / glass base |
| surface-container-high | `#1F2A3C` | Elevação de diálogo |
| surface-container-highest | `#2A3548` | Chips/inputs |
| on-surface | `#D8E3FB` | Texto principal |
| on-surface-variant | `#C2C6D6` | Texto secundário |
| outline / outline-variant | `#8C909F` / `#424754` | Bordas 1px de cards |
| **primary** | `#ADC6FF` | "Security Blue" — proteção ativa, ações primárias |
| on-primary | `#002E6A` | Texto sobre primary |
| primary-container | `#4D8EFF` | Card hero da proteção |
| on-primary-container | `#00285D` | Texto do hero |
| secondary / secondary-container | `#B7C8E1` / `#3A4A5F` | Estrutura, aba ativa |
| tertiary / tertiary-container | `#BEC6E0` / `#8990A8` | Apoio |
| error / error-container | `#FFB4AB` / `#93000A` | Bloqueado / destrutivo |

Cores de ação funcionais: verde (permitido) e vermelho (bloqueado) com fundo em baixa
chroma (~15% opacidade) + acento saturado — profissional, não alarmista.

## Tipografia

- **Inter** — display/headline/title/body (legibilidade em tamanhos pequenos).
- **Geist** — labels e dados monoespaçados (números de telefone!).
- Escala Material 3; em mobile, `headline-lg` cai para 28/36. Corpo mínimo 14sp.
- No código: escala M3 padrão até os assets de fonte entrarem na fase de UI (`Type.kt`).

## Espaçamento e grid

Grid Android de **8dp** (unit 8; xs 4 / sm 8 / md 16 / lg 24 / xl 32). Margem lateral 16dp
(12dp em listas densas de histórico). Grid fluido 4 colunas (8 em tablet). Blocos com padding
interno 16dp.

## Elevação e profundidade

Camadas tonais em vez de sombra pesada. Glassmorphism com parcimônia: top app bar com
backdrop blur 20 ao rolar; cards glass `rgba(21,32,49,0.6)` + blur 12. FAB/bottom sheet com
sombra ambiente 12% / blur 16.

## Formas

- Botões/inputs: radius 8dp. Cards/diálogos: 16dp. Bottom sheets/onboarding: 24dp.
- Chips de status e CTAs principais: pill (full).
- Rounding progressivo distingue interativo × container.

## Componentes

- **Botões**: primário high-contrast (primary + texto escuro on-primary); tonal no onboarding;
  `active:scale-95` como feedback de toque.
- **Cards de histórico**: flat com outline 1px `outline-variant`, sem sombra.
- **Switches M3**: track com glow sutil de primary quando ON; neutro quando OFF.
- **Status chips**: pill, cor de ação a 15% + texto saturado.
- **Listas**: densas, linha 56dp, leading icon (avatar/escudo) + trailing timestamp.
- **Inputs**: outlined; foco engrossa borda para 2px primary.
- **Option-cards** (onboarding): single-select com `check_circle` preenchido.

## Acessibilidade (gate de fase de UI)

- Contraste AA nos pares acima (validar Dynamic Color com contraste mínimo).
- Alvos de toque ≥ 48dp; TalkBack com contentDescription em todo ícone de ação.
- Nunca comunicar estado só por cor (dot + texto "Proteção Ativa/desativada").

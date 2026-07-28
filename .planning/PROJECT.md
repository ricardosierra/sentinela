# Sentinela — Bloqueador Local de Chamadas Desconhecidas

## Current State

**Milestone v0.1.0 MVP em andamento — Phase 1 (Fundação) iniciada**

Esqueleto Android criado em 2026-07-27: Gradle KTS + Version Catalog (AGP 9.3.0, Kotlin 2.4.10,
Compose BOM 2026.06.01, compileSdk 37, minSdk 29), pacotes da arquitetura com stubs,
`CallDecisionEngine` puro com precedência implementada e testada, manifest com
`CallScreeningService` registrado (pass-through seguro), tema Compose com tokens do design
system, documentação completa em `docs/` e travas em `CLAUDE.md`/`AGENTS.md`.

**Próximo:** validar o build do esqueleto e planejar a Phase 2 (motor de decisão completo + normalização).

---

## What This Is

App Android nativo (Kotlin + Jetpack Compose + Material 3) que impede chamadas telefônicas de
números desconhecidos de interromperem o usuário. Usa exclusivamente as APIs oficiais do
Android Telecom (`CallScreeningService` + `ROLE_CALL_SCREENING`): a chamada desconhecida é
bloqueada antes de tocar, sem tela de chamada, sem som, sem notificação nativa de perdida.
Tudo 100% local — sem servidor, login, analytics, publicidade ou telemetria.

## Core Value

**"Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."**

---

## Requirements

### Validated

(None yet — ship to validate)

### Active

Detalhe completo em [`REQUIREMENTS.md`](REQUIREMENTS.md) — 65 requisitos v1. Por categoria:

- [ ] **SCR** (11): triagem — bloquear desconhecido antes de tocar, papel de call screening, resposta única < 5 s, resiliência do Service
- [ ] **DEC** (5): motor de decisão puro com precedência, reason codes e fallback explícito
- [ ] **NRM** (4): normalização E.164 (libphonenumber), padrão BR, máscara segura
- [ ] **WLT** (7): whitelist pessoal — CRUD, busca, dedup, import/export, consulta indexada
- [ ] **HST** (7): histórico opcional — mínimo necessário, retenção, ações, fora de backup
- [ ] **NTF** (6): notificação silenciosa opt-in, canal IMPORTANCE_LOW, mascarada, pós-resposta
- [ ] **UIX** (12): 6 telas, pt-BR em resources, dark+dynamic color, acessibilidade, honestidade, rebranding centralizado
- [ ] **PRV** (7): sem INTERNET, logs mascarados, backup exclusion, R8, validação de import, política de privacidade, limpar tudo
- [ ] **QLT** (6): suíte da seção 13 do prompt, lint/detekt/builds, migrações Room, roteiro Samsung, entregáveis, testes instrumentados

### Out of Scope

- Fonte remota de whitelist / Supabase — v2; MVP prepara interfaces, sem rede (`docs/backlog/supabase-v2.md`)
- Identificação de quem liga (caller ID / base global de spam) — o produto não precisa saber quem é, só se interrompe
- Substituir o discador padrão — proibido pelo prompt; muda completamente o perfil de risco
- AccessibilityService, overlays, hacks de OEM — proibidos; só APIs oficiais
- Política por contato (bloquear/silenciar contatos da agenda) — a plataforma não entrega chamadas de contatos ao filtro sem discador padrão; ver `docs/LIMITACOES.md`
- Gravação de chamadas — proibido
- Filtrar chamadas de WhatsApp/Telegram/VoIP — fora do alcance do CallScreeningService; nunca prometer isso na UI

---

## Context

### Origem

Prompt completo do MVP em [`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md) (fonte de verdade do
escopo) + 8 telas Stitch em [`docs/design/telas/`](../docs/design/telas/) com design system
"Silent Guardian" ([`docs/design/DESIGN.md`](../docs/design/DESIGN.md)). Mockups oscilam entre
os nomes "Sentinela" e "Ultrathink" — branding unificado: **Sentinela**.

### Comportamento-chave da plataforma

Quando o app de call screening **não** é o discador padrão, `onScreenCall()` só recebe
chamadas de números **fora da agenda** do usuário. Contatos tocam normalmente sem o app pedir
`READ_CONTACTS` — a proposta inteira do produto se apoia nesse contrato
(docs oficiais: developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls).

### Stack Definida

- **Linguagem/UI**: Kotlin 2.4.10, Jetpack Compose (BOM 2026.06.01), Material 3
- **Build**: Gradle 9.6.1 + AGP 9.3.0, Kotlin DSL, Version Catalog, JDK 17 (Homebrew)
- **SDK**: minSdk 29 (exigência do ROLE_CALL_SCREENING), target/compileSdk 37
- **Dados**: Room 2.8.4 (whitelist/histórico), DataStore Preferences 1.2.1 (configurações)
- **Telefonia**: libphonenumber-android 9.0.34 (E.164), Telecom Framework
- **Qualidade**: detekt 1.23.8, Android Lint, JUnit4 + MockK + Turbine + Robolectric
- **DI**: manual (sem Hilt/Koin) — cold start do Service é orçamento crítico

### Nomenclatura

- applicationId/namespace: `org.sentinela.app` (centralizado em `app/build.gradle.kts` para rebranding)
- Pacotes por camada: `telecom/`, `domain/`, `data/local/`, `ui/`, `notifications/`, `phone/`, `settings/`
- Componentes canônicos: `UnknownCallScreeningService`, `CallDecisionEngine`, `CallDecision`,
  `ScreeningSettings`, `SettingsRepository`, `PersonalWhitelistRepository`,
  `BlockedCallRepository`, `PhoneNumberNormalizer`, `BlockedCallNotifier`, `ScreeningRoleManager`

---

## Constraints

- **Privacidade**: nenhuma permissão de INTERNET; nenhum número completo em log; dados fora de backup em nuvem — Why: é a promessa central do produto e diferencial de confiança
- **Permissões**: lista fechada (`docs/PERMISSOES.md`); proibidos READ_CONTACTS, READ_CALL_LOG, READ_SMS, READ_PHONE_STATE sem prova — Why: exigência explícita do prompt e política de loja
- **Performance**: decisão de triagem p95 < 200 ms no cold path; `respondToCall` exatamente 1× muito antes dos 5 s — Why: limite da plataforma; estourar = chamada toca
- **Plataforma**: só APIs oficiais do Telecom; nada de hack Samsung antes de provar necessidade em aparelho físico — Why: manutenção e previsibilidade
- **Idioma**: UI pt-BR via resources, código em inglês, docs/commits em português — Why: convenção do ecossistema do usuário

---

## Key Decisions

| Decisão | Razão | Resultado |
|---------|-------|-----------|
| Branding unificado "Sentinela" | Mockups oscilam Sentinela/Ultrathink; nome do produto é Sentinela | — Confirmado 2026-07-27 |
| Bloquear desconhecidos sem READ_CONTACTS via contrato da plataforma | Sem discador padrão, onScreenCall só recebe não-contatos | — Confirmado na doc oficial |
| minSdk 29 | ROLE_CALL_SCREENING existe a partir do Android 10 | — Confirmado |
| DI manual, sem Hilt/Koin | Cold start mínimo e previsível no CallScreeningService | — Confirmado |
| Tela "contatos" do mockup vira passo informativo | Opções bloquear/silenciar contato exigiriam discador padrão (fora de escopo) | — Confirmado; ver docs/LIMITACOES.md |
| Onboarding em 4 passos (boas-vindas → papel → desconhecidos → whitelist) | Mockups divergem (4 vs 3 passos); 4 cobre o fluxo completo | — Pending validação de UX |
| AGP 9.3.0 + Kotlin 2.4.10 + compileSdk 37 | Estáveis mais recentes em 2026-07 (fontes em research/STACK.md) | — Pending validação do build |
| Links GitHub `ricardosierra/sentinela` | Repo ainda sem remote; convenção de compare-links exige URL | — Pending criação do remote |

---
*Last updated: 2026-07-27 — bootstrap do projeto (esqueleto + docs + GSD)*

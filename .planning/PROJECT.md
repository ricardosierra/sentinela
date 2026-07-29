# Sentinela — Bloqueador Local de Chamadas Desconhecidas

## Current State

**Milestone v0.1.0 MVP em andamento — Phase 1 (Fundação) iniciada**

Esqueleto Android criado em 2026-07-27 e revisado em 2026-07-28 com os adendos do produto:
Gradle KTS + Version Catalog (AGP 9.3.0 com Kotlin embutido, Compose BOM 2026.06.01,
compileSdk 37, minSdk 29), pacotes da arquitetura com stubs, `CallDecisionEngine` puro com
políticas por origem (contato/whitelist/desconhecido) e 20 testes verdes, manifest com
`CallScreeningService` registrado (pass-through seguro), tema Compose com tokens do design
system, documentação completa em `docs/` e travas em `CLAUDE.md`/`AGENTS.md`.

**Próximo:** fechar a Phase 1 e planejar a Phase 2 (motor completo + normalização).

---

## What This Is

App Android nativo e **open source** (Kotlin + Jetpack Compose + Material 3) que impede
chamadas telefônicas de números desconhecidos de interromperem o usuário — sem propaganda,
sem telemetria, sem envio de dados para a nuvem, 100% offline no aparelho. Dois modos:

- **Modo filtro (padrão)**: papel `ROLE_CALL_SCREENING`; desconhecidos são bloqueados antes
  de tocar, contatos tocam normalmente.
- **Modo discador (opcional)**: o Sentinela substitui o app de telefone padrão
  (`ROLE_DIALER` + `InCallService` próprio), passando a aplicar políticas também a contatos
  (Tocar/Bloquear/Silenciar/Nunca Silenciar).

A leitura de contatos é local e efêmera: nomes e números da agenda nunca são armazenados nem
saem do aparelho. Sincronização com backend (listas, números recebidos) é etapa futura
(v0.2.0) — sempre opt-in, sempre assíncrona, nunca no caminho da decisão.

## Core Value

**"Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."**

---

## Requirements

### Validated

(None yet — ship to validate)

### Active

Detalhe completo em [`REQUIREMENTS.md`](REQUIREMENTS.md) — 81 requisitos v1. Por categoria:

- [ ] **SCR** (11): triagem — bloquear desconhecido antes de tocar, papel de call screening, resposta única < 5 s, resiliência do Service
- [ ] **DEC** (5): motor de decisão puro com precedência por origem, reason codes e fallback explícito
- [ ] **NRM** (4): normalização E.164 (libphonenumber), padrão BR, máscara segura
- [ ] **CTT** (4): leitura local de contatos com permissão explicada, cache rápido e políticas por contato
- [ ] **WLT** (8): whitelist pessoal — CRUD, busca, dedup, import/export, tratamento configurável
- [ ] **HST** (7): histórico opcional — mínimo necessário, retenção, ações, fora de backup
- [ ] **NTF** (6): notificação silenciosa opt-in, canal IMPORTANCE_LOW, mascarada, pós-resposta
- [ ] **DIA** (5): modo discador opcional — ROLE_DIALER, InCallService mínimo, discagem, reversão limpa
- [ ] **UIX** (13): telas do MVP, pt-BR em resources, dark+dynamic color, acessibilidade, honestidade, rebranding centralizado, seção de apoio
- [ ] **ENG** (4): convite de avaliação/apoio na 5ª abertura (repete a cada 5 até aceitar), open source em destaque, doação Bitcoin
- [ ] **PRV** (7): sem INTERNET no MVP, logs mascarados, backup exclusion, R8, validação de import, política de privacidade, limpar tudo
- [ ] **QLT** (7): suíte da seção 13 + novos casos, lint/detekt/builds, migrações Room, roteiro Samsung, entregáveis, instrumentados, cobertura ≥ 80%

### Out of Scope

- Caller ID / base global de spam — o produto não precisa saber quem liga, só se interrompe
- AccessibilityService, overlays, hacks de OEM — proibidos; só APIs oficiais
- Filtrar chamadas de WhatsApp/Telegram/VoIP — fora do alcance do CallScreeningService; nunca prometer isso na UI
- Gravação de chamadas — proibido
- Rede no caminho da decisão — decisão é sempre local; sync (v0.2) é assíncrona e opt-in
- Servidor, login, analytics, publicidade, telemetria no MVP

---

## Context

### Origem

Prompt completo do MVP em [`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md) — **com a seção de
adendos de 2026-07-28 no topo** (contatos, modo discador, apoio/avaliação, offline-first).
8 telas Stitch em [`docs/design/telas/`](../docs/design/telas/) com design system
"Silent Guardian" ([`docs/design/DESIGN.md`](../docs/design/DESIGN.md)); branding único:
**Sentinela**.

### Comportamento-chave da plataforma

- **Modo filtro**: quando o app de call screening não é o discador padrão, `onScreenCall()`
  só recebe chamadas de números **fora da agenda** — contatos tocam sem passar pelo app
  (developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls).
- **Modo discador**: como app de telefone padrão, o Sentinela recebe **todas** as chamadas
  no screening e conduz a experiência de chamada via `InCallService` próprio — é o que
  habilita políticas por contato; exige `READ_CONTACTS` para distinguir agenda de
  desconhecido.

### Stack Definida

- **Linguagem/UI**: Kotlin (embutido no AGP 9), Jetpack Compose (BOM 2026.06.01), Material 3
- **Build**: Gradle 9.6.1 + AGP 9.3.0, Kotlin DSL, Version Catalog, JDK 17 (Homebrew)
- **SDK**: minSdk 29 (exigência do ROLE_CALL_SCREENING), target/compileSdk 37
- **Dados**: Room 2.8.4 (whitelist/histórico), DataStore Preferences 1.2.1 (configurações + contador de aberturas)
- **Telefonia**: libphonenumber-android 9.0.34 (E.164), Telecom Framework (CallScreeningService + InCallService)
- **Qualidade**: detekt 1.23.8, Android Lint, JUnit4 + MockK + Turbine + Robolectric, Kover (cobertura ≥ 80% em domain/dados)
- **DI**: manual (sem Hilt/Koin) — cold start do Service é orçamento crítico

### Nomenclatura

- applicationId/namespace: `org.sentinela.app` (centralizado em `app/build.gradle.kts` para rebranding)
- Pacotes por camada: `telecom/`, `domain/`, `data/local/`, `data/contacts/`, `ui/`, `notifications/`, `phone/`, `settings/`
- Componentes canônicos: `UnknownCallScreeningService`, `CallDecisionEngine`, `CallDecision`,
  `ScreeningSettings` (+ `OriginPolicy`), `SettingsRepository`, `PersonalWhitelistRepository`,
  `BlockedCallRepository`, `ContactLookupRepository`, `PhoneNumberNormalizer`,
  `BlockedCallNotifier`, `ScreeningRoleManager`

---

## Constraints

- **Privacidade**: sem permissão de INTERNET no MVP; nenhum número completo em log; contatos lidos apenas em memória (nunca persistidos/enviados); dados fora de backup em nuvem — Why: é a promessa central do produto (open source, 100% offline) e diferencial de confiança
- **Permissões**: lista fechada em `docs/PERMISSOES.md` (agora inclui READ_CONTACTS e, no modo discador opcional, ROLE_DIALER/CALL_PHONE); proibidos READ_CALL_LOG, READ_SMS, READ_PHONE_STATE sem prova — Why: mínimo necessário por função, com fase definida para cada permissão entrar
- **Performance**: exemplar — decisão p95 < 200 ms no cold path; `respondToCall` exatamente 1× muito antes dos 5 s; cold start mínimo (DI manual) — Why: estourar o limite = chamada toca; performance é requisito do produto
- **Qualidade**: máximo de testes viável — suíte da seção 13 + novos casos, instrumentados, migrações Room, cobertura ≥ 80% em domain/dados — Why: exigência explícita do produto
- **Offline-first**: toda funcionalidade opera 100% offline; sync futura é opt-in e assíncrona — Why: exigência explícita do produto
- **Plataforma**: só APIs oficiais do Telecom; nada de hack Samsung antes de provar necessidade em aparelho físico — Why: manutenção e previsibilidade
- **Idioma**: UI pt-BR via resources, código em inglês, docs/commits em português — Why: convenção do ecossistema do usuário

---

## Key Decisions

| Decisão | Razão | Resultado |
|---------|-------|-----------|
| Dois modos: filtro (padrão) e discador (opcional) | Modo filtro dá bloqueio de desconhecidos com permissão mínima; modo discador habilita políticas por contato | — Confirmado 2026-07-28 (adendo do produto) |
| READ_CONTACTS entra no MVP, uso só em memória | Necessária para distinguir contato×desconhecido no modo discador; nomes nunca persistidos | — Confirmado 2026-07-28 |
| Políticas por origem (OriginPolicy) no motor | Espelha as opções dos mockups (Tocar/Bloquear/Silenciar/Nunca Silenciar) para contatos, whitelist e desconhecidos | — Confirmado 2026-07-28; implementado no esqueleto |
| Convite de avaliação na 5ª abertura, repetindo a cada 5 até aceite | Pedido do produto; sem nag após aceite | — Confirmado 2026-07-28 |
| Open source, sem telemetria, doação Bitcoin em "Apoie" | Posicionamento do produto | — Confirmado 2026-07-28; licença a escolher |
| Branding único "Sentinela" | Nome antigo dos mockups foi eliminado de todos os arquivos | — Confirmado 2026-07-28 |
| minSdk 29 | ROLE_CALL_SCREENING existe a partir do Android 10 | — Confirmado |
| DI manual, sem Hilt/Koin | Cold start mínimo e previsível no CallScreeningService | — Confirmado |
| AGP 9.3.0 + Kotlin embutido + compileSdk 37 | Estáveis mais recentes; build validado (não aplicar plugin kotlin-android) | ✓ Good — validado no build 2026-07-27 |
| Links GitHub `ricardosierra/sentinela` | Repo ainda sem remote; convenção de compare-links exige URL | — Pending criação do remote |

---
*Last updated: 2026-07-28 — adendos do produto (contatos, modo discador, apoio, offline-first); roadmap 9 fases, 81 requisitos*

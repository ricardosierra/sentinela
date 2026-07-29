---
gsd_state_version: 1.0
milestone: v0.1.0
milestone_name: MVP
status: active
stopped_at: "Adendos do produto incorporados (contatos, modo discador, apoio); roadmap 9 fases; falta commit inicial e fechar a Phase 1"
last_updated: "2026-07-28T02:40:00.000Z"
progress:
  total_phases: 9
  completed_phases: 0
  planned_phases: 9
  deferred_phases: 0
  total_plans: 0
  completed_plans: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-07-28)
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
**Current focus:** Phase 1 — Fundação Compilável
Last activity: 2026-07-28 - Adendos do produto incorporados em todo o planejamento, docs e esqueleto

## Current Position

Phase: 1 of 9 (Fundação Compilável)
Plan: nenhum plano gerado ainda (`.planning/phases/` ainda não existe)
Status: In progress — esqueleto criado e validado: `assembleDebug` + `testDebugUnitTest`
(20/20, incluindo políticas por origem) + `lint` + `detekt` verdes em 2026-07-28. Restam:
commit inicial e fechamento formal da fase (success criterion 2 — instalar o APK num
aparelho — ainda não exercitado)
Last activity: 2026-07-28

## Snapshot

- **Esqueleto:** Gradle KTS + catalog, AGP 9.3.0 (Kotlin embutido), Compose BOM 2026.06.01, compileSdk 37 / minSdk 29
- **Domínio:** `CallDecisionEngine` com precedência saída→proteção→privado→contato→whitelist→falha→desconhecido e políticas por origem (`OriginPolicy`) + 20 testes
- **Telecom:** `UnknownCallScreeningService` registrado em modo pass-through seguro (não interfere até a Phase 5)
- **Git:** repo local com 5 commits, sem remote; branch `master`
- **Última tag git:** nenhuma (primeira release será `v0.1.0`)

## Decisions

- [Adendos 2026-07-28]: **Dois modos de operação** — filtro (padrão, permissão mínima) e discador (opcional, `ROLE_DIALER` + `InCallService`, habilita políticas por contato). Substituir o discador nativo agora É escopo do MVP
- [Adendos 2026-07-28]: **READ_CONTACTS entra no MVP** — uso exclusivamente local/em memória; nomes nunca persistidos nem enviados
- [Adendos 2026-07-28]: Políticas por origem no motor (contatos: Tocar padrão; whitelist: Nunca Silenciar padrão; desconhecidos: Bloquear padrão) — espelham os mockups
- [Adendos 2026-07-28]: Convite de avaliação/apoio na 5ª abertura, repetindo a cada 5 (10ª, 15ª…) até aceite; seção "Apoie" com open source em destaque + doação Bitcoin
- [Adendos 2026-07-28]: Offline-first permanente — MVP sem INTERNET; sync (v0.2.0) opt-in/assíncrona, inclui envio opcional da lista de números recebidos
- [Adendos 2026-07-28]: Nome antigo dos mockups eliminado de todos os arquivos (docs + HTMLs); branding único Sentinela
- [Bootstrap 2026-07-27]: Bloqueio de desconhecidos no modo filtro apoiado no contrato da plataforma (onScreenCall só recebe não-contatos sem discador padrão) — confirmado na doc oficial
- [Bootstrap 2026-07-27]: DI manual, sem Hilt/Koin — cold start do Service é orçamento crítico
- [Bootstrap 2026-07-27]: AGP 9 tem Kotlin embutido — plugin `org.jetbrains.kotlin.android` NÃO deve ser aplicado (erro se aplicar)
- [Bootstrap 2026-07-27]: Links GitHub no CHANGELOG usam `ricardosierra/sentinela` como placeholder até o remote existir

## Convenções operacionais do GSD

- Toda fase exige `$gsd-discuss-phase`, com perguntas formuladas e respondidas, antes de qualquer planejamento, inclusive no modo autônomo.
- Pesquisa permanece obrigatória e habilitada antes do planejamento (config `research: true`).
- Phase 5 (Telecom) e Phase 6 (Modo Discador) têm pesquisa obrigatória reforçada: semântica exata de `setSkipCallLog`/`setSilenceCall`/DND por versão, elegibilidade ao `ROLE_DIALER`, ciclo de vida do `InCallService` e comportamento Samsung.

## Pending Todos

- Fazer o commit inicial do bootstrap (nada commitado ainda; usuário decide a hora)
- Criar remote no GitHub e ajustar links do CHANGELOG se o slug divergir de `ricardosierra/sentinela`
- **Escolher licença open source** (sugestão: GPL-3.0 ou MIT) e adicionar `LICENSE` — produto será divulgado como open source
- **Obter endereço Bitcoin real do mantenedor** para a doação (string `support_bitcoin_address` está vazia de propósito — nunca publicar com placeholder)
- Decidir arte final do ícone (placeholder vetorial de escudo no esqueleto)

## Blockers/Concerns

- **Validação física obrigatória** — `setSkipCallLog`/notificação nativa variam por OEM; critérios de aceite centrais só fecham em Samsung físico (Phase 9)
- **Modo discador é o maior risco técnico do MVP** — `InCallService` + elegibilidade ao papel + UX de chamada; pesquisa reforçada antes da Phase 6
- **Robolectric 4.16.1 suporta até SDK 36** — com compileSdk 37, fixar `@Config(sdk = [36])` até o 4.17 estável

## Accumulated Context

### Decisions

(ver seção Decisions acima — consolidar aqui a partir da Phase 2)

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

### Roadmap Evolution

- Roadmap inicial criado com 7 fases para o milestone v0.1.0 MVP (2026-07-27)
- Roadmap expandido para 9 fases (2026-07-28): + Phase 4 Contatos do Aparelho e Phase 6 Modo Discador; fase final ganhou apoio/avaliação; requisitos de 65 → 81 (CTT, DIA, ENG, WLT-08, UIX-13, QLT-06..07)

## Session Continuity

Last session: 2026-07-28
Stopped at: Adendos do produto propagados em planejamento, docs, travas e esqueleto (build verde, 20 testes)
Resume file: None — próximo passo natural: `/gsd:plan-phase 1` (ou fechar a Phase 1 direto e seguir para a 2)

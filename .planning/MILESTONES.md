# Milestones

> Histórico de releases do Sentinela. Convenção: primeira release é `v0.1.0`;
> `v1.0.0` fica reservado para produção madura com muitos usuários (regra global).

## v0.1.0 — MVP Bloqueador Local (em curso)

**Phases planned:** 9 (Fundação → Domínio → Dados → Contatos → Telecom → Modo Discador →
UI ×2 → Apoio/Release/Validação)
**Goal:** app open source instalável que bloqueia chamadas de números desconhecidos antes de
tocar, 100% offline, com políticas por origem (contatos/whitelist/desconhecidos), modo
discador opcional, whitelist pessoal, histórico opcional, notificação silenciosa opt-in e
fluxo de apoio/avaliação.

**Critérios de fechamento:** os 13 critérios de aceite da seção 16 de
[`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md) **+ adendos de 2026-07-28** (topo do mesmo
arquivo), validados ou explicitamente documentados como dependentes de aparelho físico.

## v0.2.0 — Sincronização & Backend (planejado)

> **Status:** backlog. Nada de rede no MVP; interfaces preparadas
> (`PersonalWhitelistRepository` estável para fonte remota plugável). A decisão de bloqueio
> nunca espera rede — sync é sempre opt-in e assíncrona.

**Capacidades planejadas:** sincronização de listas e configurações entre aparelhos, envio
opcional da lista de números recebidos/bloqueados para o backend, backup opt-in
criptografado. Detalhes em [`docs/backlog/supabase-v2.md`](../docs/backlog/supabase-v2.md).

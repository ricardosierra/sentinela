# Milestones

> Histórico de releases do Sentinela. Convenção: primeira release é `v0.1.0`;
> `v1.0.0` fica reservado para produção madura com muitos usuários (regra global).

## v0.1.0 — MVP Bloqueador Local (em curso)

**Phases planned:** 7 (Fundação → Domínio → Dados → Telecom → UI ×2 → Release/Validação)
**Goal:** app instalável que bloqueia chamadas de números desconhecidos antes de tocar,
100% local, com whitelist pessoal, histórico opcional e notificação silenciosa opt-in.

**Critérios de fechamento:** os 13 critérios de aceite da seção 16 de
[`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md), validados ou explicitamente documentados
como dependentes de aparelho físico.

## v0.2.0 — Supabase & Sincronização (planejado)

> **Status:** backlog. Nada de rede no MVP; interfaces preparadas
> (`PersonalWhitelistRepository` estável para fonte remota plugável).

**Capacidades planejadas:** whitelist remota compartilhável, backup opt-in criptografado,
sincronização de configurações. Detalhes em [`docs/backlog/supabase-v2.md`](../docs/backlog/supabase-v2.md).

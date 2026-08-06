# Milestones

## v0.1 MVP (Shipped: 2026-08-06)

**Phases completed:** 9 phases, 46 plans, 27 tasks

**Key accomplishments:**

- (none recorded)

---

> Histórico de releases do Sentinela. Convenção: primeira release é `v0.1.0`;
> `v1.0.0` fica reservado para produção madura com muitos usuários (regra global).

## v0.2.0 — Auditoria e correções do MVP (2026-08-06)

**Entregue:** o MVP auditado de ponta a ponta e com os defeitos que tinham escapado corrigidos.

As Fases 8 e 9 tinham sido implementadas por commits diretos, fora do fluxo de planejamento, e
a v0.1.0 foi tagueada **sem nunca passar por verificação**. A auditoria retroativa de
2026-08-06 encontrou 10 defeitos e os corrigiu:

- **Resposta única ao sistema restaurada** — um commit de UI da Fase 8 tinha criado um segundo
  ponto de `respondToCall`; uma falha depois da decisão somava uma resposta "permitir" à mesma
  chamada, desfazendo em silêncio um bloqueio já decidido
- **Endereço de doação corrigido** — a v0.1.0 publicou um endereço Bitcoin que o próprio
  arquivo mandava não publicar; agora são os endereços reais do mantenedor (Bitcoin on-chain e
  Liquid), com o checksum conferido em teste sobre o valor que vai para o APK
- **Backup da whitelist passou a funcionar** — exportar gravava lista vazia e importar
  descartava o arquivo; as classes existiam testadas e nunca eram chamadas
- **Histórico ficou honesto e útil** — filtro por decisão, tempo real de cada evento (era
  "Agora" fixo em toda linha) e motivo em português (era o reason code interno cru)
- **Notificação abre o registro** e o botão "Sobre" da tela inicial leva à tela Sobre
- **Higiene** — 557 arquivos de `.venv/` saíram do versionamento

**Portões:** testes, lint, detekt, Kover e `assembleRelease` verdes;
`scripts/verify-invariants.sh` verde nos 10 blocos (era vermelho em 3).

**Pendência aberta:** validação em Samsung físico (51 cenários) — só o mantenedor pode fechar.

Detalhes em [`milestones/v0.1-MILESTONE-AUDIT.md`](milestones/v0.1-MILESTONE-AUDIT.md).

## v0.1.0 — MVP Bloqueador Local (2026-08-04)

**Status:** ✅ shipped — 9 de 9 fases. Ver a v0.2.0 acima: esta release saiu sem verificação e
os defeitos encontrados depois estão registrados lá.

**Phases planned:** 9 (Fundação → Domínio → Dados → Contatos → Telecom → Modo Discador →
UI ×2 → Apoio/Release/Validação)
**Goal:** app open source instalável que bloqueia chamadas de números desconhecidos antes de
tocar, 100% offline, com políticas por origem (contatos/whitelist/desconhecidos), modo
discador opcional, whitelist pessoal, histórico opcional, notificação silenciosa opt-in e
fluxo de apoio/avaliação.

**Critérios de fechamento:** os 13 critérios de aceite da seção 16 de
[`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md) **+ adendos de 2026-07-28** (topo do mesmo
arquivo), validados ou explicitamente documentados como dependentes de aparelho físico.

## v0.3.0 — Sincronização & Backend (planejado)

> **Renumerado de v0.2.0 em 2026-08-06.** O número v0.2.0 foi consumido pela release de
> auditoria e correções do MVP, que saiu antes desta etapa. O escopo abaixo não mudou.

> **Status:** backlog. Nada de rede no MVP; interfaces preparadas
> (`PersonalWhitelistRepository` estável para fonte remota plugável). A decisão de bloqueio
> nunca espera rede — sync é sempre opt-in e assíncrona.

**Capacidades planejadas:** sincronização de listas e configurações entre aparelhos, envio
opcional da lista de números recebidos/bloqueados para o backend, backup opt-in
criptografado. Detalhes em [`docs/backlog/supabase-v2.md`](../docs/backlog/supabase-v2.md).

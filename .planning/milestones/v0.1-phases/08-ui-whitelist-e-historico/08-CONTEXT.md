# Phase 8: UI Whitelist e Historico - Context

**Gathered:** 2026-08-06 (retroativo)
**Status:** Reconciliado — a fase foi implementada fora do fluxo GSD e este contexto foi
reconstruído a partir do código entregue, dos commits e da auditoria de 2026-08-06.

<domain>
## Phase Boundary

Usuário gerencia exceções (whitelist pessoal) e audita bloqueios (histórico) sem sair do app.
Entram: CRUD da whitelist com normalização E.164, busca, detecção de duplicado, backup por
arquivo local (export/import via SAF), lista de histórico com filtros e as três ações por
registro (permitir → whitelist, marcar indesejado, excluir), e a máscara de número em toda
superfície de exibição.

Não entram: sincronização com backend (v0.2.0), bloqueio por prefixo/padrão (pós-MVP) e
qualquer validação que exija aparelho físico — essa vai para o roteiro da Fase 9.
</domain>

<decisions>
## Implementation Decisions

### Reconciliação retroativa

Esta fase não passou por discuss → plan → execute. Os artefatos abaixo registram as decisões
que o código entregue **de fato** tomou, mais as que a auditoria precisou corrigir. Onde a
implementação divergia do critério do ROADMAP, vale o critério, e a correção está registrada
em `08-SUMMARY.md`.

### Whitelist

- Normalização acontece no ViewModel, antes do repositório, usando o `PhoneNumberNormalizer`
  da Fase 2 — o repositório recebe a chave já pronta.
- Duplicado é detectado por `repository.contains(key)` antes do upsert, e **precisa chegar ao
  usuário**: descartar em silêncio não cumpre "detecta duplicado".
- Busca com `debounce` de 300 ms, alternando entre `observeAll()` e `search(query)`.
- Backup em JSON com envelope `{version, whitelist[]}`, via `WhitelistExporter` /
  `WhitelistImporter` (já testados). A rota faz apenas a IO do SAF; a serialização e a
  validação ficam nas classes de dados.
- Importação pede confirmação antes de mesclar, porque altera a lista do usuário, e tem teto
  de leitura para o seletor do sistema não conseguir derrubar o app com um arquivo grande.

### Histórico

- Filtro em dois eixos independentes: período (tudo/hoje/7/30 dias) e decisão.
- O eixo "decisão" é a **classificação dada pelo usuário** (sem classificação / legítima /
  indesejada), e não o tipo da decisão do motor: `PostScreeningWork` só grava chamada
  efetivamente barrada, então um eixo bloqueada/silenciada/permitida teria duas gavetas
  sempre vazias.
- O reason code (`DecisionReason`) é interno e nunca aparece cru na tela — a lista mostra
  rótulo em pt-BR vindo de `strings.xml`.
- Tempo de cada registro é relativo (agora / há N min / há N h / há N d), com relógio
  injetável para o teste não depender da hora da máquina.

### Privacidade e texto

- Número aparece sempre mascarado nas listas; o Bloco 10 de `verify-invariants.sh` trava isso
  proibindo `Text(...numberE164...)` nas telas da fase.
- Nenhum texto de interface em Kotlin: tudo em `res/values/strings.xml`, pt-BR. Vale também
  para texto gravado no banco (o rótulo do registro criado a partir do histórico).

### Claude's Discretion

Escolha de componentes Material 3 (bottom sheet do filtro, snackbar do aviso, menu suspenso
das ações do registro) e a organização interna dos arquivos de UI.
</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets

- `PersonalWhitelistRepository` / `RoomWhitelistRepository` e `BlockedCallRepository` (Fase 3).
- `PhoneNumberNormalizer` e `PhoneMask` (Fase 2), já ligados no `AppContainer`.
- `WhitelistExporter` / `WhitelistImporter` com suíte própria em `WhitelistImportExportTest`.
- Componentes compartilhados da Fase 7: `SentinelaTopBar`, `CheckRow`, tokens de tema.

### Established Patterns

- DI manual pelo `AppContainer`; ViewModels recebem colaboradores por construtor.
- Estado de tela como `sealed interface` + `StateFlow` com `stateIn`.
- Rota (`*Route.kt`) faz plataforma e navegação; tela (`*Screen.kt`) é pura de Compose.

### Integration Points

- `SentinelaNavHost` e a bottom bar de quatro abas (Home, Whitelist, Histórico, Proteção).
- `AppContainer.whitelistRepository`, `.blockedCallRepository`, `.phoneNumberNormalizer`.
</code_context>

<specifics>
## Specific Ideas

O backup precisa ser um arquivo que o usuário controla (SAF), não um export para nuvem —
coerente com o "100% offline" do produto.
</specifics>

<deferred>
## Deferred Ideas

- Sincronização da whitelist com backend — v0.2.0, já registrado em `docs/backlog/`.
- Bloqueio por prefixo/padrão (ex.: 0303) — candidato pós-MVP, já no CHANGELOG em `[Futuro]`.
- Exercitar import/export e as ações do histórico em aparelho físico — roteiro da Fase 9.
</deferred>

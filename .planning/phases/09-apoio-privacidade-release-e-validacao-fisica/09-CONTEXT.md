# Phase 9: Apoio Privacidade Release e Validacao Fisica - Context

**Gathered:** 2026-08-06 (retroativo)
**Status:** Reconciliado — implementada fora do fluxo GSD; contexto reconstruído a partir do
código entregue, dos commits e da auditoria de 2026-08-06.

<domain>
## Phase Boundary

Fechar o MVP como produto instalável, auditável e honesto: convite de avaliação respeitoso,
seção de apoio, tela de privacidade e sobre com limpar-tudo, build de release assinado e
minificado, e o roteiro de validação em Samsung físico.

Não entra: qualquer coisa de rede, conta ou sincronização (v0.2.0). Não entra publicar na
loja — a fase entrega o APK e o roteiro.
</domain>

<decisions>
## Implementation Decisions

### Avaliação e apoio

- Convite na 5ª abertura, repetindo a cada 5 até o aceite; o aceite encerra os convites.
  Contador em DataStore (`AppOpenCounter`), nunca interrompe onboarding nem chamada.
- Seção "Apoie o Sentinela" destaca open source, sem propaganda, sem telemetria, sem nuvem,
  100% offline, com comentário de apoio na loja.
- **Doação em Bitcoin:** só existe com endereço real do mantenedor, gerado em carteira sob a
  custódia dele. Enquanto não houver, o botão **não é publicado**. Publicar endereço inventado
  ou placeholder é proibido pelo CLAUDE.md, e o erro é irreversível: a doação vai para um
  terceiro e não volta. Decisão tomada na auditoria de 2026-08-06 (ver `09-VERIFICATION.md`).

### Privacidade e sobre

- Lista dados guardados, permissões pedidas, política de retenção, versão e limitações reais.
- Limpar tudo com **duas** confirmações, porque a ação é irreversível.

### Release

- `assembleRelease` com R8: `isMinifyEnabled` e `isShrinkResources` ligados, assinatura por
  `app/keystore.properties` (fora do versionamento).
- `-assumenosideeffects` sobre `android.util.Log` remove log sensível do release.
- `koverVerify` com piso de 80% continua sendo portão de build.

### Validação física

- Roteiro único em `docs/TESTE-FISICO-SAMSUNG.md`, executado manualmente pelo mantenedor.
- Critério aceita **resultados registrados ou pendências documentadas** — o roteiro declara
  explicitamente que cada cenário é veredito pendente até rodar no aparelho.

### Claude's Discretion

Layout da tela Sobre e ordem das seções.
</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets

- `DataStoreSettingsRepository` (Fase 3) para o contador de aberturas e a marca de aceite.
- Componentes e tema da Fase 7.

### Established Patterns

- Strings em `res/values/strings.xml`, pt-BR.
- Rota faz plataforma; tela é Compose puro.

### Integration Points

- `HomeViewModel.onRatingAccepted` / `onRatingDismissed`, `RatingBottomSheet` na Home.
- `AboutRoute` / `AboutScreen` / `AboutViewModel`, alcançáveis pela navegação.
</code_context>

<specifics>
## Specific Ideas

O texto da tela Sobre precisa ser honesto sobre limitações: não afirmar que filtra WhatsApp
ou VoIP, nem que o bloqueio é "100% garantido".
</specifics>

<deferred>
## Deferred Ideas

- Publicar na Play Store — fora do MVP.
- Endereço de doação em Bitcoin — bloqueado aguardando endereço real do mantenedor.
</deferred>

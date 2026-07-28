# Arquitetura do Sentinela

## Em 30 segundos

O Android entrega ao app (detentor do papel `ROLE_CALL_SCREENING`) cada chamada recebida de
número **fora da agenda** — contatos nem chegam ao app quando ele não é o discador padrão.
O `UnknownCallScreeningService` monta uma entrada pura, o `CallDecisionEngine` decide
(permitir, rejeitar, caixa postal silenciosa ou bloquear sem rastro) consultando apenas dados
locais, e o Service traduz a decisão em `respondToCall` — uma única vez, muito antes do limite
de 5 segundos da plataforma.

```
Android Telecom
      │  onScreenCall(Call.Details)        [só números fora da agenda]
      ▼
telecom/UnknownCallScreeningService  ←→  telecom/ScreeningRoleManager (papel)
      │ normaliza (phone/PhoneNumberNormalizer)
      │ snapshot (settings/SettingsRepository ── DataStore)
      │ lookup   (data/local/PersonalWhitelistRepository ── Room)
      ▼
domain/CallDecisionEngine ──► domain/CallDecision (+ DecisionReason)
      │
      ▼
respondToCall(CallResponse)   [exatamente 1×; p95 < 200 ms]
      │  depois da resposta:
      ├─► data/local/BlockedCallRepository (histórico opcional)
      └─► notifications/BlockedCallNotifier (notificação silenciosa opt-in)
```

## Fatos da plataforma que sustentam o desenho

1. **Contatos nunca passam pelo filtro** quando o app não é o discador padrão (doc oficial de
   screen-calls). É por isso que o MVP cumpre "contatos tocam normalmente" sem `READ_CONTACTS`.
2. **Janela de ~5 s para `respondToCall`** — estourou, a chamada segue. Todo I/O da decisão tem
   timeout interno folgado e a falha cai na política de fallback configurada.
3. **`CallResponse`** controla tudo que o produto precisa: `setDisallowCall`, `setRejectCall`,
   `setSilenceCall`, `setSkipCallLog`, `setSkipNotification`.

## Regras de dependência (invioláveis)

- `domain/` não importa nada de `android.telecom.*` — entrada é `ScreenedCall`, saída é `CallDecision`.
- Toda regra de triagem vive no `CallDecisionEngine`; o Service é orquestração de I/O.
- `ui/` (Compose) nunca importa `telecom/`; fala com repositórios e ViewModels.
- Histórico e notificação própria só **depois** do `respondToCall`.
- Repositórios são interfaces — a fonte remota de whitelist (v2) pluga sem tocar no domínio.

## Precedência da decisão (espelho do §5 do prompt)

1. Chamada de saída → `Allow(outgoing_call)`
2. Proteção desabilitada → `Allow(protection_disabled)`
3. Número privado/sem handle → bloqueio conforme config (padrão: bloquear) — `private_number`
4. Whitelist pessoal → `Allow(personal_whitelist)`
5. Falha na consulta local → política de fallback explícita — `local_lookup_failure`/`fallback_policy`
6. Desconhecido/inválido → bloqueio conforme config — `unknown_number`/`invalid_number`

Modos de bloqueio: `Reject` (rejeita já), `SendSilentlyToVoicemail` (encaminha em silêncio),
`BlockWithoutTrace` (rejeita + `setSkipCallLog` + `setSkipNotification`).
`setSkipNotification(true)` é usado em **todo** bloqueio para suprimir a notificação nativa de
chamada perdida (a notificação própria, quando habilitada, substitui).

## Resiliência do Service (Phase 4)

| Risco | Proteção |
|-------|----------|
| Resposta duplicada | Guarda de resposta única por chamada |
| Exceção em normalização/repos | try/catch amplo → fallback configurado |
| Banco frio/indisponível | Timeout interno (~1,5 s) → `LOOKUP_FAILED` |
| Cold start do processo | DI manual lazy; zero frameworks; nada pesado no `Application` |
| Corrida config × chamada | Snapshot atômico das settings no início da triagem |
| Dual SIM | Decisão independe da SIM; SIM só registrada se disponível sem permissão extra |
| Papel perdido | `ScreeningRoleManager.isRoleHeld()` revalidado na home; sem papel, nada acontece (sistema não chama o service) |

## Orçamento de performance

- p95 < 200 ms no cold path (processo recém-criado + consulta Room/DataStore).
- Warm path esperado em poucos ms (settings em memória + índice de whitelist).
- Medição entra como bench na Phase 4 (critério de sucesso 5 da fase).

## Estrutura de pacotes

`org.sentinela.app` (applicationId centralizado em `app/build.gradle.kts` para rebranding):

| Pacote | Conteúdo |
|--------|----------|
| `telecom/` | `UnknownCallScreeningService`, `ScreeningRoleManager` |
| `domain/` | `CallDecisionEngine`, `CallDecision`, `DecisionReason`, `ScreenedCall` |
| `settings/` | `ScreeningSettings`, `SettingsRepository` (DataStore na Phase 3) |
| `data/local/` | `PersonalWhitelistRepository`, `BlockedCallRepository` (Room na Phase 3) |
| `phone/` | `PhoneNumberNormalizer` (libphonenumber na Phase 2) |
| `notifications/` | `BlockedCallNotifier` (canal silencioso na Phase 4) |
| `ui/` | `MainActivity`, telas Compose, `ui/theme/` (tokens do design system) |

# Arquitetura — contrato da plataforma e desenho das camadas

## Dois modos de operação

- **Modo filtro (padrão)**: app detém só `ROLE_CALL_SCREENING`. A plataforma entrega ao
  `onScreenCall()` apenas números fora da agenda; contatos tocam nativo. O Service passa
  `ContactLookup.MISS` ao motor.
- **Modo discador (opcional, Fase 6)**: app detém `ROLE_DIALER` (+ `InCallService` próprio e
  handler de `ACTION_DIAL`). Todas as chamadas passam pelo screening; o
  `ContactLookupRepository` (READ_CONTACTS) resolve HIT/MISS/UNAVAILABLE e as políticas por
  contato (CTT-03) passam a valer de fato.

## Contrato do CallScreeningService (confirmado na doc oficial)

- **Escopo de triagem sem discador padrão:** "Call the `onScreenCall()` function for any new
  incoming or outgoing calls **when the number is not in the user's contact list**." — ou seja,
  no modo filtro, contatos NUNCA passam pelo app; tocam normalmente.
- **Registro:** service no manifest com `android.permission.BIND_SCREENING_SERVICE` +
  intent-filter `android.telecom.CallScreeningService`, `exported=true` (bind é restrito ao
  sistema pela permissão).
- **Papel:** `RoleManager.ROLE_CALL_SCREENING` (API 29+) solicitado com
  `createRequestRoleIntent` + ActivityResultLauncher; pode ser perdido a qualquer momento.
- **Janela de resposta:** `respondToCall` deve sair em ~5 s ou a chamada segue; chamar
  exatamente uma vez.
- **Controles do CallResponse:** `setDisallowCall`, `setRejectCall`, `setSilenceCall`,
  `setSkipCallLog`, `setSkipNotification`. Semântica fina de `setSkipCallLog` para chamadas
  rejeitadas: verificar por versão/OEM na Phase 4 (pesquisa) e Phase 7 (aparelho).
- **Direção:** `Call.Details.getCallDirection()` distingue entrada/saída — saída nunca sofre
  interferência.
- **Verificação STIR/SHAKEN:** `callerNumberVerificationStatus` disponível; não é usada na
  decisão do MVP (produto não classifica spam, só desconhecido×whitelist).

## Camadas (regra: dependências apontam para o domínio)

```
telecom/  UnknownCallScreeningService (fino) ── ScreeningRoleManager
   │  monta ScreenedCall, aplica timeout interno, traduz CallDecision → CallResponse
   │  (Fase 6: + InCallService/ROLE_DIALER para o modo discador)
   ▼
domain/   CallDecisionEngine (puro) · CallDecision · DecisionReason · ScreenedCall
   ▲              ▲
   │              │ (interfaces)
settings/ SettingsRepository (DataStore)     data/local/ PersonalWhitelistRepository,
phone/    PhoneNumberNormalizer (libphonenumber)          BlockedCallRepository (Room)
data/contacts/ ContactLookupRepository (READ_CONTACTS, cache em memória, nunca persiste)
notifications/ BlockedCallNotifier (canal silencioso, pós-resposta)
ui/       Compose + ViewModels — nunca importa telecom/
```

Invariantes:
- Toda regra de triagem vive no `CallDecisionEngine`; Service só orquestra I/O.
- Domínio não importa nenhum tipo `android.telecom.*`.
- Compose não conhece Telecom; fala com repositórios/ViewModels.
- Registro em histórico e notificação própria acontecem **depois** do `respondToCall`.
- Interfaces de repositório estáveis para fonte remota futura (v2) sem tocar no domínio.

## Fluxo da decisão (Phase 5; modo discador estende na Phase 6)

1. `onScreenCall` recebe `Call.Details`; saída → responde vazio e retorna.
2. Handle nulo → `ScreenedNumber.Private`; senão normaliza (falha → `Invalid`).
3. Snapshot de settings + lookup de contato (modo filtro: MISS direto; modo discador:
   `ContactLookupRepository`) + lookup whitelist, tudo com timeout interno curto (ex.: 1,5 s)
   — estouro/exceção → `LOOKUP_FAILED`/`UNAVAILABLE` → política de fallback.
4. `CallDecisionEngine.decide(call, settings, contact, whitelist)` → `CallDecision`.
   Precedência: saída → proteção → privado → contato (política) → whitelist (política) →
   falha de consulta → desconhecido (política).
5. Tradução: Allow → builder vazio; Silence → `setSilenceCall(true)`;
   Reject/BlockWithoutTrace → disallow+reject com skipNotification sempre true e skipCallLog
   conforme configuração; SendSilentlyToVoicemail → comportamento oficial de silêncio
   (detalhar na pesquisa da fase). NEVER_SILENCE → Allow + bypass de DND na camada de
   toque/notificação (pesquisa da Phase 6).
6. Pós-resposta (fire-and-forget): histórico (se habilitado) + notificação (se habilitada).

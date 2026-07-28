# Arquitetura — contrato da plataforma e desenho das camadas

## Contrato do CallScreeningService (confirmado na doc oficial)

- **Escopo de triagem sem discador padrão:** "Call the `onScreenCall()` function for any new
  incoming or outgoing calls **when the number is not in the user's contact list**." — ou seja,
  contatos NUNCA passam pelo app; tocam normalmente. Sem `READ_CONTACTS`, sem exceção.
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
   ▼
domain/   CallDecisionEngine (puro) · CallDecision · DecisionReason · ScreenedCall
   ▲              ▲
   │              │ (interfaces)
settings/ SettingsRepository (DataStore)     data/local/ PersonalWhitelistRepository,
phone/    PhoneNumberNormalizer (libphonenumber)          BlockedCallRepository (Room)
notifications/ BlockedCallNotifier (canal silencioso, pós-resposta)
ui/       Compose + ViewModels — nunca importa telecom/
```

Invariantes:
- Toda regra de triagem vive no `CallDecisionEngine`; Service só orquestra I/O.
- Domínio não importa nenhum tipo `android.telecom.*`.
- Compose não conhece Telecom; fala com repositórios/ViewModels.
- Registro em histórico e notificação própria acontecem **depois** do `respondToCall`.
- Interfaces de repositório estáveis para fonte remota futura (v2) sem tocar no domínio.

## Fluxo da decisão (Phase 4)

1. `onScreenCall` recebe `Call.Details`; saída → responde vazio e retorna.
2. Handle nulo → `ScreenedNumber.Private`; senão normaliza (falha → `Invalid`).
3. Snapshot de settings + lookup whitelist com timeout interno curto (ex.: 1,5 s) —
   estouro/exceção → `WhitelistLookup.LOOKUP_FAILED` → política de fallback.
4. `CallDecisionEngine.decide()` → `CallDecision`.
5. Tradução: Allow → builder vazio; Reject/BlockWithoutTrace → disallow+reject com
   skipNotification sempre true e skipCallLog conforme configuração;
   SendSilentlyToVoicemail → comportamento oficial de silêncio (detalhar na pesquisa da fase).
6. Pós-resposta (fire-and-forget): histórico (se habilitado) + notificação (se habilitada).

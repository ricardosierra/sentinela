# Arquitetura do Sentinela

## Em 30 segundos

O Sentinela opera em dois modos. No **modo filtro** (padrão), o Android entrega ao app
(detentor do papel `ROLE_CALL_SCREENING`) as chamadas recebidas — inclusive de contatos,
**enquanto a leitura da agenda estiver concedida**; o Android só dispensa a triagem de quem
está na agenda quando o app não consegue consultá-la. No **modo discador** (opcional), o
Sentinela vira o app de telefone padrão (`ROLE_DIALER` + `InCallService` próprio) e passa a
receber também as chamadas sem handle (número oculto). Em ambos, o
`UnknownCallScreeningService` monta uma entrada pura, o `CallDecisionEngine` decide
(permitir, silenciar, rejeitar ou encaminhar à caixa postal) consultando apenas dados
locais, e o Service traduz a decisão em `respondToCall` — uma única vez, muito antes do
limite de 5 segundos da plataforma. O registro no histórico do telefone **não** é evitável
(ver item 3 de [`LIMITACOES.md`](LIMITACOES.md)).

```
Android Telecom
      │  onScreenCall(Call.Details)   [entrada; contatos incluídos se a agenda for legível]
      ▼
telecom/UnknownCallScreeningService  ←→  telecom/ScreeningRoleManager (papéis)
      │ normaliza (phone/PhoneNumberNormalizer)          (Fase 6: + InCallService próprio)
      │ snapshot  (settings/SettingsRepository ── DataStore)
      │ contato?  (data/contacts/ContactLookupRepository ── READ_CONTACTS, só memória)
      │ whitelist (data/local/PersonalWhitelistRepository ── Room)
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

1. **Modo filtro: contatos nunca passam pelo app** quando ele não é o discador padrão (doc
   oficial de screen-calls) — "contatos tocam normalmente" sai de graça, e o
   `ContactLookup` entra como `MISS` no motor.
2. **Modo discador: o app de telefone padrão recebe todas as chamadas** no screening e conduz
   a experiência via `InCallService` — é o que torna as políticas por contato reais. Exige
   `READ_CONTACTS` (lookup local) e elegibilidade ao `ROLE_DIALER` (handler `ACTION_DIAL` +
   `InCallService` declarado).
3. **Janela de ~5 s para `respondToCall`** — estourou, a chamada segue. Todo I/O da decisão
   tem timeout interno folgado e a falha cai na política de fallback configurada.
4. **`CallResponse`** controla tudo que o produto precisa: `setDisallowCall`,
   `setRejectCall`, `setSilenceCall`, `setSkipCallLog`, `setSkipNotification`.

## Regras de dependência (invioláveis)

- `domain/` não importa nada de `android.telecom.*` — entrada é `ScreenedCall` +
  `ContactLookup`/`WhitelistLookup`, saída é `CallDecision`.
- Toda regra de triagem vive no `CallDecisionEngine`; o Service é orquestração de I/O.
- `ui/` (Compose) nunca importa `telecom/`; fala com repositórios e ViewModels.
- Dados de contato (nome/foto) nunca são persistidos nem saem do processo — o motor só
  enxerga HIT/MISS/UNAVAILABLE.
- Histórico e notificação própria só **depois** do `respondToCall`.
- Repositórios são interfaces — a fonte remota de sincronização (v0.2) pluga sem tocar no
  domínio, e a decisão nunca espera rede.

## Precedência da decisão (§5 do prompt + adendos 2026-07-28)

1. Chamada de saída → `Allow(outgoing_call)`
2. Proteção desabilitada → `Allow(protection_disabled)`
3. Número privado/sem handle → bloqueio conforme config (padrão: bloquear) — `private_number`
4. Contato da agenda → **política de contatos** (Tocar padrão / Bloquear / Silenciar / Nunca Silenciar) — `contact`
5. Whitelist pessoal → **política da whitelist** (Nunca Silenciar padrão / Tocar / Bloquear / Silenciar) — `personal_whitelist`
6. Falha na consulta local (contatos indisponíveis ou whitelist falhou) → política de fallback — `local_lookup_failure`/`fallback_policy`
7. Desconhecido/inválido → **política de desconhecidos** (Bloquear padrão / Silenciar / Permitir) — `unknown_number`/`invalid_number`

Saídas do motor: `Allow`, `Silence` (toca sem som/vibração via `setSilenceCall`),
`Reject` (rejeita já), `SendSilentlyToVoicemail` (encaminha em silêncio),
`BlockWithoutTrace` (rejeita + `setSkipCallLog` + `setSkipNotification`).
`setSkipNotification(true)` é usado em **todo** bloqueio para suprimir a notificação nativa
de chamada perdida. "Nunca Silenciar" decide como Allow — o bypass de Não Perturbe é
responsabilidade da camada de toque/notificação (semântica confirmada na pesquisa da Fase 6).

## Resiliência do Service (Fase 5)

| Risco | Proteção |
|-------|----------|
| Resposta duplicada | Guarda de resposta única por chamada |
| Exceção em normalização/repos | try/catch amplo → fallback configurado |
| Banco/contatos frios ou indisponíveis | Timeout interno (~1,5 s) → `LOOKUP_FAILED`/`UNAVAILABLE` |
| Cold start do processo | DI manual lazy; zero frameworks; nada pesado no `Application` |
| Corrida config × chamada | Snapshot atômico das settings no início da triagem |
| Dual SIM | Decisão independe da SIM; SIM só registrada se disponível sem permissão extra |
| Papéis perdidos | `ScreeningRoleManager` revalida na home; sem papel, o sistema nem chama o service |
| Modo discador sem READ_CONTACTS | Ativação do modo exige a permissão; em runtime, `UNAVAILABLE` → fallback |

## Orçamento de performance (exigência: exemplar)

- p95 < 200 ms no cold path (processo recém-criado + consulta contatos/Room/DataStore).
- Warm path esperado em poucos ms (settings em memória + caches de contatos e whitelist).
- Cold start do processo mínimo: DI manual lazy, sem reflexão, sem frameworks.
- Medição entra como bench nas Fases 4 (lookup de contatos) e 5 (decisão fim a fim);
  cobertura e benchmarks são gate de release (Fase 9).

## Estrutura de pacotes

`org.sentinela.app` (applicationId centralizado em `app/build.gradle.kts` para rebranding):

| Pacote | Conteúdo |
|--------|----------|
| `telecom/` | `UnknownCallScreeningService`, `ScreeningRoleManager` (+ Fase 6: `InCallService` próprio) |
| `domain/` | `CallDecisionEngine`, `CallDecision`, `DecisionReason`, `ScreenedCall`, `ContactLookup`, `WhitelistLookup` |
| `settings/` | `ScreeningSettings`, `OriginPolicy`, `SettingsRepository` (DataStore na Fase 3) |
| `data/local/` | `PersonalWhitelistRepository`, `BlockedCallRepository` (Room na Fase 3) |
| `data/contacts/` | `ContactLookupRepository` (READ_CONTACTS + cache em memória, Fase 4) |
| `phone/` | `PhoneNumberNormalizer` (libphonenumber na Fase 2) |
| `notifications/` | `BlockedCallNotifier` (canal silencioso na Fase 5) |
| `ui/` | `MainActivity`, telas Compose, `ui/theme/` (tokens do design system) |

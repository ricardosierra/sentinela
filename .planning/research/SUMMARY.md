# Research Summary — Sentinela (bootstrap 2026-07-27)

Pesquisa de projeto feita durante o bootstrap: stack estável de julho/2026 verificada na web,
comportamento da plataforma confirmado na documentação oficial do Android e mapeamento das
8 telas Stitch. Documentos:

- [`STACK.md`](STACK.md) — versões estáveis e compatíveis entre si, com fontes
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — contrato do CallScreeningService e desenho das camadas
- [`PITFALLS.md`](PITFALLS.md) — armadilhas conhecidas (OEM, toolchain, plataforma)
- [`FEATURES.md`](FEATURES.md) — telas do MVP e divergências dos mockups

## Os 4 fatos que sustentam o produto

1. **Modo filtro: sem ser discador padrão, `onScreenCall()` só recebe chamadas de números
   fora da agenda.** Contatos tocam normalmente sem passar pelo app.
   (developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls)

2. **Modo discador: como app de telefone padrão (`ROLE_DIALER`), o app recebe TODAS as
   chamadas** no screening e conduz a chamada via `InCallService` próprio — é o que habilita
   políticas por contato (com `READ_CONTACTS` para distinguir agenda de desconhecido).
   Elegibilidade ao papel exige handlers próprios (ACTION_DIAL) — pesquisa reforçada na Phase 6.

3. **`respondToCall` tem janela de ~5 s** — depois disso a chamada segue como permitida.
   Por isso o orçamento interno de p95 < 200 ms e a política de fallback explícita.

4. **`CallResponse` oferece exatamente os controles que o produto precisa:**
   `setDisallowCall`, `setRejectCall`, `setSilenceCall`, `setSkipCallLog`, `setSkipNotification`.
   A semântica fina de `setSkipCallLog` em chamadas rejeitadas varia por versão/OEM — pesquisa
   obrigatória na Phase 5 + validação física na Phase 9.

## Riscos priorizados

| Risco | Mitigação |
|-------|-----------|
| Modo discador (InCallService + elegibilidade ROLE_DIALER) é o maior risco técnico | Fase dedicada (6) com pesquisa obrigatória reforçada; modo é opcional e reversível |
| OEM Samsung diverge no call log/notificação nativa | Roteiro físico dedicado (docs/TESTE-FISICO-SAMSUNG.md); nada de hack preventivo |
| AGP 9 mudou o modelo de plugins (Kotlin embutido) | Confirmado no bootstrap: não aplicar `org.jetbrains.kotlin.android`; ver PITFALLS |
| Robolectric ainda não suporta SDK 37 | `@Config(sdk = [36])` até Robolectric 4.17 estável |
| detekt 1.23.8 pré-data o Kotlin 2.4 | Validado no bootstrap: plugin funciona no Gradle 9.6.1 |

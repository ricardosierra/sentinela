---
phase: 05-triagem-telecom-modo-filtro
verified: 2026-07-29T17:33:45Z
status: human_needed
score: 6/6 must-haves verified (median-only automated); 3 criteria deferred by design to Phase 9 hardware
human_verification:
  - test: "Cenarios 40, 41, 42, 46 de docs/TESTE-FISICO-SAMSUNG.md — bloqueio real de desconhecido nao toca/nao vibra/nao mostra tela/sem notificacao nativa de perdida; contato toca normalmente; Silenciar toca sem som"
    expected: "Comportamento descrito nos cenarios, em Galaxy fisico"
    why_human: "Comportamento de Telecom real de fabricante (One UI) so e observavel em hardware; deliberadamente fora do escopo automatizado desta fase, conforme CONTEXT/ROADMAP"
  - test: "Cenario 44 — Encaminhar silenciosamente cai na caixa postal"
    expected: "Chamador ouve caixa postal (depende da operadora, nao prometido na UI)"
    why_human: "Depende de operadora real; nao simulavel em emulador"
  - test: "Cenario 47 — p95 da decisao em hardware real"
    expected: "p95 < 200 ms no Galaxy conectado"
    why_human: "Por decisao documentada da fase (DecisionPerformanceTest.kt), apenas a mediana (p50 < 50 ms, 4x de folga) trava o build em CI/emulador; o veredito de cauda pertence à Fase 9 em hardware físico"
---

# Phase 5: Triagem Telecom (modo filtro) Verification Report

**Phase Goal:** Chamada de número desconhecido é bloqueada de verdade antes de tocar, com o
Service fino, resiliente e dentro do orçamento.
**Verified:** 2026-07-29T17:33:45Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Service é fino, zero condição de bloqueio fora do motor | ✓ VERIFIED | `UnknownCallScreeningService.kt` só monta chamada, delega ao `ScreeningCoordinator`, traduz resposta. `scripts/verify-invariants.sh` Bloco 7 (5 checagens automatizadas) passa: nenhuma `OriginPolicy`/`BlockMode` na camada telecom (exceto `CallResponseFactory`, tradução legítima), nenhum `CallDecision.Reject/Silence/BlockWithoutTrace/SendSilentlyToVoicemail(` construído fora de `domain/` |
| 2 | Resposta ao sistema exatamente 1× em todo caminho, inclusive exceção/timeout | ✓ VERIFIED | `ScreeningCoordinator.emit()` usa `AtomicBoolean.compareAndSet(false, true)` como guarda; `ScreeningCoordinatorFailureTest.kt` injeta falha em CADA colaborador individualmente (settings, contatos, whitelist, histórico, motor, própria costura de resposta, trabalho pós-resposta, timeout) e em combinações, sempre afirmando contagem exata (`resposta.total == 1`) e o tipo da decisão |
| 3 | Defeito inesperado sempre libera a chamada (nunca bloqueia por bug) | ✓ VERIFIED | `catch (error: Throwable) { emit(permissive(), ...) }` + rede permissiva final no `finally`; testado em `ScreeningCoordinatorFailureTest` para todos os pontos de falha |
| 4 | Notificação própria: off por padrão, só após respondToCall, número nunca completo | ✓ VERIFIED | `ScreeningSettings.showOwnNotification = false`; `postScreeningWork` só roda em `afterResponse`, chamado depois do `emit()`; `AndroidBlockedCallNotifier` só lê `entry.maskedNumber` (nunca o campo cru), usa `VISIBILITY_PRIVATE` + `setPublicVersion` com texto anônimo, `IMPORTANCE_LOW`, `setSilent(true)`, sem som/vibração/luz |
| 5 | Chamada repetida (SCR-12): motor puro, precedência correta, padrão ligado, janela nomeada, falha de histórico = MISS | ✓ VERIFIED | `CallDecisionEngine.decide()` linha 55: bypass entre whitelist (linha 52) e a política de desconhecidos (linha 61-66), como documentado na precedência do cabeçalho; `REPEATED_CALL_WINDOW_MILLIS` constante nomeada em `RepeatedCall.kt`; `repeatedCallBypassEnabled = true` por padrão; `RepeatedCallLookup.LOOKUP_FAILED` tratado como `MISS` (engine só reage a `HIT`) |
| 6 | Contador de abertura do app não conta chamada recebida | ✓ VERIFIED | `AppOpenCounterTest` prova estruturalmente que `onAppOpened()` só é chamado em `MainActivity` (guardado por `savedInstanceState == null`) e nunca em `SentinelaApp.kt` (processo) |

**Score:** 6/6 truths automaticamente verificáveis, VERIFIED. Critérios 1, 2, 6 do ROADMAP e o
veredito de cauda de performance (critério 5, p95) são **deferidos por desenho** à Phase 9
(hardware Samsung) — ver `human_verification` acima.

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `domain/CallDecisionEngine.kt` | motor puro com toda regra de precedência | ✓ VERIFIED | 8 níveis de precedência documentados e implementados; sem import android.* |
| `domain/RepeatedCall.kt` | enum + janela nomeada | ✓ VERIFIED | `RepeatedCallLookup` + `REPEATED_CALL_WINDOW_MILLIS = 5min` |
| `telecom/ScreeningCoordinator.kt` | orquestração pura, resposta única | ✓ VERIFIED | 154 linhas, `AtomicBoolean`, sem tipo de plataforma, `decisionEngine.decide` chamado 1x |
| `telecom/UnknownCallScreeningService.kt` | delegação fina | ✓ VERIFIED | 71 linhas, usa `SentinelaApp.container`, retorno antecipado em OUTGOING, sem regra própria |
| `telecom/ScreenedCallFactory.kt` / `CallResponseFactory.kt` | tradução Call.Details↔CallDecision | ✓ VERIFIED | existem, usados pelo Service e pelo container |
| `AppContainer.kt` | fiação única do processo | ✓ VERIFIED | `screeningCoordinator`, `postScreeningWork`, `blockedCallNotifier` todos `by lazy`, singleton |
| `notifications/AndroidBlockedCallNotifier.kt` | canal silencioso, conteúdo mascarado | ✓ VERIFIED | `IMPORTANCE_LOW`, `PhoneMask`, `runCatching` em todo o corpo |
| `androidTest/DecisionPerformanceTest.kt` | percentis, assert só na mediana | ✓ VERIFIED | `P50_MAX_MS = 50.0` (4x de folga sobre orçamento de 200ms); p95/max só logados |
| `androidTest/ScreeningServiceBindTest.kt` | vínculo real ao serviço | ✓ VERIFIED | existe em `app/src/androidTest/.../telecom/` |
| `scripts/verify-invariants.sh` Bloco 7 | prova estrutural de regra concentrada no motor | ✓ VERIFIED | executado, 5/5 checagens ok |
| `docs/LIMITACOES.md` itens 2,3,7,8 | limites documentados com fonte | ✓ VERIFIED | todos citam a origem no código do Telecom/AOSP |
| `docs/TESTE-FISICO-SAMSUNG.md` cenários 40-51 | roteiro físico da fase | ✓ VERIFIED | 12 cenários presentes, cobrindo os itens deferidos |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `UnknownCallScreeningService` | `ScreeningCoordinator.screen` | `deps.screeningCoordinator.screen(...)` | ✓ WIRED | dentro de `deps.launchAfterResponse` |
| `ScreeningCoordinator` | `CallDecisionEngine.decide` | chamada única dentro de `withTimeout` | ✓ WIRED | uma única invocação no caminho feliz, uma no caminho de timeout |
| `ScreeningCoordinator` | `BlockedCallRepository.hasRecentBlock` | insumo SCR-12 | ✓ WIRED | `async { ... blockedCalls.hasRecentBlock(key, clock()) }` |
| `respond` callback | `respondToCall` | fábrica de respostas | ✓ WIRED | `respondToCall(callDetails, deps.callResponseFactory.toResponse(decisao, configuracoes))` chamado só dentro do `respond` do coordenador, guardado pelo `AtomicBoolean` |
| `AndroidBlockedCallNotifier` | `PhoneMask.mask` | conteúdo mascarado | ✓ WIRED | `entry.maskedNumber` já vem mascarado do `PostScreeningWork`/`AppContainer` |
| Outgoing call | Service | retorno antecipado, zero resposta | ✓ WIRED | `if (chamada.direction == CallDirection.OUTGOING) return` no Service, e também guarda redundante no `ScreeningCoordinator.screen` |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| SCR-01/02/03 | 05-05 | Service vinculável, papel detectável, entrada pura sem crash | ✓ SATISFIED | `ScreeningServiceBindTest`, `ScreeningRoleManager*`, `ScreenedCallFactory` |
| SCR-04 | 05-07 | Número privado bloqueável (parcial, só discador) | ✓ SATISFIED (parcial documentado) | `docs/LIMITACOES.md` item 8; motor honra a config, sem efeito prático no filtro |
| SCR-05 | 05-03/05-05 | Resposta única | ✓ SATISFIED | `AtomicBoolean` + Bloco 7.5 |
| SCR-06 | 05-02/05-07 | Combinação contraditória nunca emitida | ✓ SATISFIED | `CallResponseFactoryTest` |
| SCR-07 | — | Ocultar do histórico nativo | ✓ WON'T FIX confirmado | `docs/LIMITACOES.md` item 3, com fonte AOSP; label da UI não promete ausência de rastro (`settings_hide_native_log_desc`) |
| SCR-08/09 | 05-02/05-03/05-05 | Handle/número inválido não derruba; contato continua tocando | ✓ SATISFIED | `ScreenedCallFactoryTest`, `ScreeningCoordinatorOrderTest` |
| SCR-10 | 05-06 | Vínculo real em aparelho virtual | ✓ SATISFIED | `ScreeningServiceBindTest.kt` |
| SCR-11 | 05-06 | p95 no orçamento | ? NEEDS HUMAN (mediana automatizada; cauda deferida à Fase 9) | `DecisionPerformanceTest.kt` |
| SCR-12 | 05-01/05-03/05-05 | Bypass de chamada repetida | ✓ SATISFIED | ver Truth 5 |
| NTF-01..06 | 05-04/05-05 | Notificação silenciosa, opt-in, mascarada | ✓ SATISFIED | ver Truth 4 |
| DEC-01..05 (integração) | vários | Motor integrado end-to-end | ✓ SATISFIED | `ScreeningCoordinatorTest`, `DecisionMatrixTest` |
| QLT-01 | todos | Cobertura/testes por comportamento novo | ✓ SATISFIED | testes extensos por colaborador e por falha injetada |
| QLT-06 | 05-06/05-07 | Suite limpa, script de invariante | ✓ SATISFIED | `./gradlew testDebugUnitTest` verde; `scripts/verify-invariants.sh` 100% ok, incluindo detekt/lint |

Nenhum requisito órfão encontrado — todos os IDs declarados no escopo da fase aparecem nos
frontmatters dos planos 05-01 a 05-07.

### Anti-Patterns Found

Nenhum bloqueador. Buscas por `TODO|FIXME|XXX|HACK|PLACEHOLDER`, `console.log`-equivalentes
(`Log.` sensível) e implementações vazias nos arquivos centrais da fase não retornaram
ocorrências relevantes. Único `TODO` no código é `AppContainer.kt:202`
(`// TODO(Fase 6): componentes do modo discador`), fora de escopo desta fase e correto.

### Human Verification Required

Ver frontmatter `human_verification`. Resumo: os critérios de sucesso 1, 2 e 6 do ROADMAP (toque
real, ausência de notificação nativa, políticas Silenciar/Encaminhar) e a cauda de p95 (critério 5)
dependem de comportamento real de Telecom/OEM e são **deliberadamente** adiados para a validação
física da Fase 9 (`docs/TESTE-FISICO-SAMSUNG.md`, cenários 40-51). Isto é uma decisão documentada
do projeto, não uma lacuna de implementação — confirmado que os cenários existem e cobrem
exatamente esses pontos, e que apenas a mediana trava o build automatizado.

### Gaps Summary

Nenhum gap de implementação encontrado. Toda a arquitetura da Fase 5 está de acordo com o
CLAUDE.md: motor puro, Service fino, resposta única garantida por `AtomicBoolean` com exceção
injetada em todos os pontos do pipeline (não só no topo), defeito inesperado sempre libera a
chamada, notificação nunca carrega número completo e é opt-in, SCR-12 implementado no motor com
a precedência correta, janela nomeada, leitura do repositório de histórico existente sem
mudança de esquema, e falha de consulta tratada como MISS. O contador de abertura do app não
conta mais chamadas recebidas. Nenhuma permissão nova além do já declarado (POST_NOTIFICATIONS
desde a Fase 1, READ_CONTACTS desde a Fase 4); nenhuma permissão proibida; sem Hilt/Koin/Dagger;
sem WorkManager; sem rede; domínio e normalização livres de import android.*; nenhuma string
hardcoded; nenhum número completo em log (nenhum log sensível existe nos pacotes telecom/
notifications). As decisões deliberadas (SCR-07 won't fix, SCR-04 parcial, DND sem bypass,
outgoing sem resposta dupla) foram confirmadas como implementadas e documentadas corretamente,
sem alegação falsa em UI ou docs. O único item pendente é a validação em hardware físico da Fase
9, que é escopo explícito daquela fase, não desta.

---

_Verified: 2026-07-29T17:33:45Z_
_Verifier: Claude (gsd-verifier)_

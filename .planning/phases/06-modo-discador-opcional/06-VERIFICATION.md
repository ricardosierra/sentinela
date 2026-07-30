---
phase: 06-modo-discador-opcional
verified: 2026-07-30T00:15:13Z
status: gaps_found
score: 4/5 truths verified
gaps:
  - truth: "Chamada recebida usa a UI própria: atender, recusar, encerrar, mudo, viva-voz e DTMF funcionam"
    status: partial
    reason: >
      Answer/reject/hangUp/DTMF are proven at the platform seam (TelecomCallControls calls the
      real android.telecom.Call API, fail-fast, no swallowed exceptions). Mute and speaker are
      NOT proven at the seam. TelecomCallControls.setMuted/setSpeakerOn — which translate
      CallSessionCoordinator commands into InCallService.setMuted()/setAudioRoute() calls — are
      never instantiated or exercised by any test (JVM or instrumented). The class is excluded
      wholesale from the Kover gate with the justification "só faz efeito com um objeto de
      chamada montado pela própria plataforma, que nenhum teste em JVM pode construir" — but
      that justification only covers answer()/hangUp()/playDtmf(), which need a live Call.
      setMuted/setSpeakerOn operate on InCallService, which mockk already proves is mockable in
      this codebase (SentinelaInCallServiceTest mocks android.telecom.Call the same way). The
      pure helper audioRoutesFromMask(), which decides whether speaker is even offered, is also
      untested anywhere despite being a trivial pure function. What IS proven: the coordinator's
      internal logic (mute/speaker toggling, availability gating) against a FAKE CallControls in
      CallSessionCoordinatorTest — this proves the state machine, not the platform translation.
      This is exactly the "calling a Service method and asserting nothing threw" failure mode
      the phase's own risk brief warns against, except here there isn't even a call-and-assert-
      no-throw test — there is no test at all touching TelecomCallControls.
    artifacts:
      - path: "app/src/main/java/org/sentinela/app/telecom/call/TelecomCallControls.kt"
        issue: "setMuted/setSpeakerOn/audioRoutesFromMask never referenced by any test file (app/src/test or app/src/androidTest); class excluded from Kover coverage"
    missing:
      - "Unit test constructing TelecomCallControls with a mockk<InCallService> (and a mockk<Call> for symmetry with existing SentinelaInCallServiceTest style) that verifies setMuted(true)/setMuted(false) actually invokes service.setMuted(...) with the right value"
      - "Unit test for setSpeakerOn that verifies the ROUTE_SPEAKER gating logic (both the case where supportedRouteMask lacks ROUTE_SPEAKER — no-op — and the case where it is offered — calls setAudioRoute)"
      - "Unit test for audioRoutesFromMask covering all four route bits, since it is a pure function with zero test coverage"
      - "Consider narrowing the Kover exclude for TelecomCallControls (by method, or by proving it's now covered) instead of excluding the whole class, per the project's stated policy of 'exclude sempre por nome de classe, jamais... classe pura com cobertura baixa se resolve escrevendo teste'"
human_verification:
  - test: "SPEAKERPHONE audio routing on a real device during an active call"
    expected: "Tapping viva-voz actually routes call audio to the loudspeaker and back"
    why_human: "Emulator only exposes the speaker route (registered, deliberate Phase 6 deferral to Phase 9); real audio hardware routing cannot be verified in CI/emulator"
  - test: "One UI dialer-default switch dialog, role survival across OS update, aggressive battery optimization interaction with the bound InCallService"
    expected: "Samsung's own UI does not silently break the role grant or kill the process outside the measured 'role revoked' path"
    why_human: "OEM-specific behavior explicitly out of scope for Phase 6, deferred to Phase 9 physical test scenarios 23, 57, 58"
  - test: "Private/withheld number in dialer mode (docs/LIMITACOES.md item 8, scenario 59)"
    expected: "Either screening intercepts it or it reaches only the call UI too late to block — currently unknown"
    why_human: "Requires simulating an incoming call with suppressed caller ID via the emulator console, which needs local credentials not reachable from the test process; correctly documented as UNVERIFIED rather than claimed"
---

# Phase 6: Modo discador opcional — Verification Report

**Phase Goal:** Usuário que optar pode tornar o Sentinela o telefone padrão — habilitando
políticas também para contatos — com experiência de chamada própria e reversão limpa.
**Verified:** 2026-07-30T00:15:13Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Ativação solicita `ROLE_DIALER` com explicação honesta e exige `READ_CONTACTS` concedida | ✓ VERIFIED | `DialerActivationScreen` (5 branches), `DialerRoleManager`/`SystemRoleGate`, honest copy strings 264-266 in `strings.xml`, `docs/PERMISSOES.md` §Elegibilidade confirmed by experiment |
| 2 | Chamada recebida usa a UI própria: atender, recusar, encerrar, mudo, viva-voz e DTMF funcionam | ⚠️ PARTIAL | Answer/reject/hangUp/DTMF proven at the real platform seam (`TelecomCallControls`, no swallowed exceptions). Mute/speaker logic proven only against a fake `CallControls` in `CallSessionCoordinatorTest`; the real translation to `InCallService.setMuted`/`setAudioRoute` in `TelecomCallControls` and the pure `audioRoutesFromMask` helper are never exercised by any test |
| 3 | Política por contato é aplicada de verdade a chamadas de contatos | ✓ VERIFIED | `git log` confirms `CallDecisionEngine.kt` untouched since `d7d188b` (Phase 5); `DialerScreeningIntegrationTest.kt` exercises the real `ScreeningCoordinator` with a real contacts fixture and the dialer role actually held on the emulator; EVIDENCE.md documents a genuine red (`contatoComPoliticaSilenciarESilenciado` failing before a test-side race fix, product logic confirmed correct) |
| 4 | Discar um número pela tela de discagem funciona (`ACTION_DIAL` atendido) | ✓ VERIFIED | Manifest declares two `ACTION_DIAL` intent filters (`tel` scheme + no-scheme), Bloco 8 of `verify-invariants.sh` locks this down with a sabotage-proof red, `OutgoingCallPlacer`/`DialerActivity` read the dialed number from the intent and originate via `TelecomManager` |
| 5 | Reverter para o nativo restaura tudo sem quebrar telefonia; modo filtro continua operante | ✓ VERIFIED | `scripts/verify-dialer-lifecycle.sh` (external, out-of-process) proves: process dies mid-call → call survives → system rebinds to factory dialer → next call routes to Sentinela again → role reversion returns default dialer to factory app → screening role survives reversion, all independently reproduced with different PIDs across two runs (06-07 and 06-08 evidence) |

**Score:** 4/5 truths fully verified, 1 partial (mute/speaker unproven at the platform seam)

### Required Artifacts (spot-checked at all three levels)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `telecom/call/CallSessionCoordinator.kt` | Pure state machine, fail-fast (no swallowed exceptions), presentation watchdog | ✓ VERIFIED | Read in full; watchdog (`armarPrazo`/`CallPresentationTimeoutException`) is real, 2s deadline, throws on timeout; no try/catch anywhere in the file |
| `telecom/call/TelecomCallControls.kt` | Platform seam translation for all 8 commands | ⚠️ PARTIAL/ORPHANED (mute/speaker only) | answer/reject/hangUp/DTMF exist, are wired, no exception swallowing; `setMuted`/`setSpeakerOn`/`audioRoutesFromMask` exist and are wired in production (`SentinelaInCallService.kt:73`) but are never referenced by any test file — untested, not "orphaned" in the wiring sense, but unproven |
| `ui/call/CallActivity.kt` | Single declaration of `EXTRA_CALL_ACTION` contract, malformed-intent safety | ✓ VERIFIED | Sole declaration site; `IncomingCallNotifier.kt` re-exports via `import ... as` aliases, no independent second declaration; `callActionOf()` returns `null` on unknown/absent value, which only opens the screen — cannot answer/reject a real call by intent forgery |
| `ui/call/CallActionButton.kt` | Accept/reject colors fixed outside Dynamic Color | ✓ VERIFIED | `callAcceptColors()`/`callRejectColors()` return literals from `ui/theme/Color.kt` (`CallAccept`/`CallReject`); `CallActionButton` composable contains zero references to `MaterialTheme.colorScheme` |
| `AndroidManifest.xml` | `CALL_PHONE`, `USE_FULL_SCREEN_INTENT`, `BIND_INCALL_SERVICE` present; `SYSTEM_ALERT_WINDOW` absent | ✓ VERIFIED | All four confirmed by direct read; `SYSTEM_ALERT_WINDOW` absent |
| `docs/PERMISSOES.md` | Documents all four Phase 6 permissions/roles | ✓ VERIFIED | `ROLE_DIALER`, `BIND_INCALL_SERVICE`, `CALL_PHONE`, `USE_FULL_SCREEN_INTENT` all documented with phase, trigger and rationale; `SYSTEM_ALERT_WINDOW` explicitly reconfirmed as still forbidden |
| `docs/design/TELAS.md` §11 | Rewritten as closed contract for call/dialer UI | ✓ VERIFIED | Section spans lines 165-286 (~120 lines), matches SUMMARY claim |
| `docs/TESTE-FISICO-SAMSUNG.md` | 60 scenarios, 23-30 revised, 52-60 new | ✓ VERIFIED | 60 numbered rows confirmed by count; dedicated "Modo discador" section for the Phase 6 scenarios |
| `domain/CallDecisionEngine.kt` | Untouched throughout Phase 6 | ✓ VERIFIED | `git log` shows last touch `d7d188b`, Phase 5; zero Phase 6 commits touch `domain/` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `CallSessionCoordinator.answer/reject/hangUp/pressDigit` | `TelecomCallControls` → `android.telecom.Call` | direct method calls, no interception | ✓ WIRED | No try/catch; verified by reading full source |
| `CallSessionCoordinator.setMuted/setSpeakerOn` | `TelecomCallControls` → `InCallService.setMuted/setAudioRoute` | direct method calls | ⚠️ WIRED BUT UNTESTED | Production wiring confirmed by grep of instantiation site; no test exercises this path |
| `SentinelaInCallService` | `CallSessionStore`/`CallSessionCoordinator` | `store.attach(TelecomCallControls(call, this))` | ✓ WIRED | `SentinelaInCallService.kt:73`; covered by `SentinelaInCallServiceTest` (Robolectric) and `InCallServiceBindTest`/`InCallServiceDeathTest` (instrumented) |
| `IncomingCallNotifier` action taps | `CallActivity.EXTRA_CALL_ACTION` contract | import alias re-export | ✓ WIRED | Single declaration confirmed, no duplicate literal |
| `ScreeningCoordinator` (Phase 5) | `CallDecisionEngine` (Phase 2/3, untouched) | contact policy lookup, same code path regardless of dialer role | ✓ WIRED | `DialerScreeningIntegrationTest` exercises the real coordinator with the dialer role actually held |
| Manifest `ACTION_DIAL` filters | `DialerActivity` → `OutgoingCallPlacer` → `TelecomManager.placeCall` | intent extraction, `platform/CallPhonePermissionChecker` runtime gate | ✓ WIRED | Confirmed by source read and Bloco 8 invariants |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| DIA-01 | 06-03, 06-05 | Ativação de `ROLE_DIALER` com explicação honesta | ✓ SATISFIED | `DialerActivationScreen`, `DialerRoleManager` |
| DIA-02 | 06-01, 06-02, 06-04, 06-06 | `InCallService` com UI própria (atender/recusar/encerrar/mudo/viva-voz/DTMF) | ⚠️ PARTIAL | Answer/reject/hangUp/DTMF proven at seam; mute/speaker only proven at coordinator level, not platform seam |
| DIA-03 | 06-05 | Discagem mínima via `ACTION_DIAL` | ✓ SATISFIED | Manifest filters, `DialerActivity`, `OutgoingCallPlacer` |
| DIA-04 | 06-07 | Triagem cobre contatos no modo discador, motor intocado | ✓ SATISFIED | `git log` on `CallDecisionEngine.kt`, `DialerScreeningIntegrationTest` |
| DIA-05 | 06-03, 06-07, 06-08 | Reversão limpa, telefonia nunca quebra | ✓ SATISFIED | `scripts/verify-dialer-lifecycle.sh`, independently reproduced |
| QLT-06 | 06-07, 06-08 | Testes instrumentados verdes, incluindo InCallService | ✓ SATISFIED | 80/80 instrumented tests green (EVIDENCE.md §2), including `InCallServiceBindTest`, `InCallServiceDeathTest` — but these do not cover mute/speaker |

No orphaned requirements found — every DIA-*/QLT-06 ID mapped to this phase in REQUIREMENTS.md is claimed by at least one plan.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `telecom/call/TelecomCallControls.kt` | 45-50 | `setMuted`/`setSpeakerOn` implemented but zero test references anywhere in the tree | ⚠️ Warning | Not a stub — the code is real and wired into production — but it is unverified at the one seam that matters for this phase's stated risk (a Service method call whose failure mode is silent no-op, not exception) |
| — | — | No `TODO`/`FIXME`/placeholder strings found in any Phase 6 file inspected | — | — |

No blocker-level anti-patterns found. No stub composables, no empty handlers, no console.log-only implementations detected in the files read.

### Human Verification Required

See `human_verification` in frontmatter — all three items are registered, deliberate Phase 9 deferrals (speaker/Bluetooth audio routing, One UI-specific role behavior, private-number-in-dialer-mode scenario 59), correctly documented as unverified rather than claimed. These are not gaps; they are honestly scoped out.

### Gaps Summary

Phase 6 delivers on four of its five success criteria with strong, independently-reproduced evidence — including the two highest-risk claims (DIA-04 proven without touching the decision engine, and clean reversion proven by external process-death observation). The privacy boundary (full number only on call/dialpad screens), the accept/reject color fix outside Dynamic Color, the single `EXTRA_CALL_ACTION` declaration, and the malformed-intent safety property were all independently confirmed in source, not just in the SUMMARY narrative.

The one real gap is narrow but matches exactly the risk pattern this phase was warned about: **mute and speaker are wired into the platform seam (`TelecomCallControls.setMuted`/`setSpeakerOn`) but never exercised by any test — JVM or instrumented.** The class is excluded wholesale from the Kover coverage gate with a justification that is only valid for the `Call`-dependent methods (`answer`/`hangUp`/`playDtmf`), not for the `InCallService`-dependent methods, which this same codebase already knows how to mock (see `SentinelaInCallServiceTest`, which mocks `Call` with `mockk`). The pure `audioRoutesFromMask` helper — which gates whether speaker is even offered — is also completely untested despite requiring no platform object at all. This does not mean mute/speaker are broken; it means their correctness rests on manual/emulator observation rather than an automated, sabotage-provable test, unlike every other call control in this phase.

---

_Verified: 2026-07-30T00:15:13Z_
_Verifier: Claude (gsd-verifier)_

---
phase: 06-modo-discador-opcional
verified: 2026-07-30T01:30:00Z
status: passed
score: 5/5 truths verified
re_verification:
  previous_status: gaps_found
  previous_score: 4/5
  gaps_closed:
    - "Chamada recebida usa a UI própria: atender, recusar, encerrar, mudo, viva-voz e DTMF funcionam"
  gaps_remaining: []
  regressions: []
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

# Phase 6: Modo discador opcional — Verification Report (Re-verification)

**Phase Goal:** Usuário que optar pode tornar o Sentinela o telefone padrão — habilitando
políticas também para contatos — com experiência de chamada própria e reversão limpa.
**Verified:** 2026-07-30T01:30:00Z
**Status:** passed
**Re-verification:** Yes — after gap closure (plan 06-09)

## Gap Closure Verification

The single gap from the previous verification — mute/speaker unproven at the platform seam —
was closed by plan 06-09. Re-checked against the actual codebase, not the SUMMARY narrative:

- **`app/src/test/java/org/sentinela/app/telecom/call/TelecomCallControlsTest.kt`** exists
  (read in full, 202 lines). Two Robolectric test classes: `TelecomCallControlsTest`
  (`@Config(sdk = [35])`, 13 `@Test` methods) and `TelecomCallControlsRecusaAntigaTest`
  (`@Config(sdk = [33])`, 1 `@Test` method).
- **Delegation is asserted, not absence-of-exception.** Every mute/speaker case uses
  `verify(exactly = 1) { service.setMuted(...) }` or
  `verify(exactly = 1) { service.setAudioRoute(ROUTE_SPEAKER/ROUTE_EARPIECE) }` with the exact
  expected argument — confirmed by direct read of lines 44–108. Both guard branches (no audio
  state published; mask lacks `ROUTE_SPEAKER`) use `verify(exactly = 0) { service.setAudioRoute(any()) }`,
  proving the no-op path is also asserted, not merely unexercised.
- **`audioRoutesFromMask` covered.** Empty mask (line 141), each of the four bits isolated
  (lines 146–160), all four bits together (lines 163–178), and the emulator-measured
  SPEAKER-only case delegating correctly (lines 101–108) are all present as distinct test cases.
- **`Call` object claims confirmed.** `answer()` → `call.answer(VideoProfile.STATE_AUDIO_ONLY)`,
  `hangUp()` → `call.disconnect()`, `reject()` on modern SDK → `call.reject(REJECT_REASON_DECLINED)`,
  reject on legacy SDK (separate `@Config(sdk=[33])` class) → `call.reject(false, null)`, and
  `playDtmf`/`stopDtmf` → `call.playDtmfTone('5')`/`call.stopDtmfTone()` — all verified with
  `mockk<Call>(relaxed = true)`, exactly as claimed.
- **Kover exclude removed for `TelecomCallControls`.** `grep` of `app/build.gradle.kts` confirms
  no `classes("org.sentinela.app.telecom.call.TelecomCallControls...")` exclude remains — only
  `SentinelaInCallService` (and its nested classes), `*_Impl`/Room-generated code, and the
  contacts provider source remain excluded.
- **`SentinelaInCallService` exclude justification is now lifecycle-only**, and that is true for
  what it covers: bind/death/rebind are observed out-of-process by `InCallServiceBindTest`,
  `InCallServiceDeathTest`, and `scripts/verify-dialer-lifecycle.sh` — none of which depend on a
  platform-constructed `Call` object being unmockable (the false premise the old exclude relied on).
- **Non-vacuity: sabotage-and-revert.** Commit `c4e6e08` (test) is followed directly by
  `8fd3084` (chore, removes the exclude) and `d3678fa` (docs) — no intermediate sabotage commit
  exists, consistent with the SUMMARY's description of the sabotage as applied and reverted
  locally via `git checkout --` without ever entering history. This cannot be independently
  re-run without redoing the sabotage. The SUMMARY's specific, self-critical, falsifiable claim
  that the multi-bit mask case did NOT catch translator sabotage (while the bit-by-bit case did)
  is treated as credible corroborating detail, not standalone proof.

## Actual Test/Build Run (executed fresh, not trusted from SUMMARY)

```
./gradlew testDebugUnitTest --rerun   → BUILD SUCCESSFUL, 618 total JVM tests (verified by
                                          summing tests="N" across all test-results/*.xml)
./gradlew koverVerify                  → BUILD SUCCESSFUL, app/build/reports/kover/verify.err
                                          is empty (no rule violations at minBound 80)
./gradlew lint detekt                  → BUILD SUCCESSFUL (lintDebug, detekt both ran clean)
```

`app/build/reports/kover/report.xml` (line coverage) confirms 878 covered / 30 missed lines
= 96.6960...% ≈ 96.69%, matching the SUMMARY's claimed figure exactly. Within
`TelecomCallControls.kt`'s `<sourcefile>` block, every `<line>` entry has `mi="0"` (zero missed
instructions) — full coverage of the seam, as claimed.

## Regression Check (Observable Truths 1, 3, 4, 5)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Ativação solicita `ROLE_DIALER` com explicação honesta e exige `READ_CONTACTS` concedida | ✓ VERIFIED | No files under dialer activation scope touched since prior verification; `git diff 869244a..HEAD` limited to `app/build.gradle.kts`, the new test file, and 06-09 planning docs |
| 2 | Chamada recebida usa a UI própria: atender, recusar, encerrar, mudo, viva-voz e DTMF funcionam | ✓ VERIFIED (gap closed) | All 8 commands now proven at the platform seam with delegation assertions, see Gap Closure section above |
| 3 | Política por contato é aplicada de verdade a chamadas de contatos | ✓ VERIFIED (no regression) | `CallDecisionEngine.kt` last touched at `d7d188b` (Phase 5); `git log` confirms zero commits since, including 06-09 |
| 4 | Discar um número pela tela de discagem funciona (`ACTION_DIAL` atendido) | ✓ VERIFIED (no regression) | `git diff 869244a..HEAD -- app/src/main/AndroidManifest.xml` shows zero diff |
| 5 | Reverter para o nativo restaura tudo sem quebrar telefonia; modo filtro continua operante | ✓ VERIFIED (no regression) | `SentinelaInCallService` and lifecycle test files untouched by 06-09; only its Kover exclude comment was reworded, not the exclude scope |

**Score:** 5/5 truths verified.

## Regression Scan (fail-loud property, permissions, libraries)

- **No exception swallowing added.** `grep -n "try\s*{"` across `telecom/call/*.kt` and
  `SentinelaInCallService.kt` returns zero matches — no try/catch exists in the call path,
  before or after 06-09.
- **No new permission or manifest change.** `git diff 869244a..HEAD -- app/src/main/AndroidManifest.xml`
  is empty. `gradle/libs.versions.toml` also unchanged in that range.
- **`app/build.gradle.kts` diff** is exactly the Kover exclude rewrite described in the
  SUMMARY (exclude removed for `TelecomCallControls`, comment rewritten for
  `SentinelaInCallService`) — no other build config changed, no new dependency added.
- **No test weakened.** `TelecomCallControlsTest.kt` is purely additive (201 new lines, new
  file); no existing test file was modified in the 06-09 commits (`git show c4e6e08 --stat`
  shows only the new test file; `git show 8fd3084 --stat` shows only `app/build.gradle.kts`).

## Anti-Patterns Found

None. No `TODO`/`FIXME`/placeholder strings in the new test file or the modified build file.
No stub assertions, no vacuous "didn't throw" tests remain for mute/speaker.

## Human Verification Required

Unchanged from the previous verification — all three items remain registered, deliberate
Phase 9 deferrals (see frontmatter `human_verification`), not gaps:
1. Real-hardware speakerphone/audio routing.
2. One UI-specific dialer-role and battery-optimization behavior.
3. Private/withheld number in dialer mode (scenario 59).

## Registered Deferrals (confirmed still correctly out of scope, not gaps)

- Speakerphone-on-real-hardware and One UI behavior — owned by Phase 9.
- Inter/Geist font assets absent, numeric styles fall back to system monospace — logged in
  `docs/backlog/`.
- CHANGELOG.md lacks technical blocks for Phases 2-5 — logged for version close.

## Conclusion

The single gap from the initial verification — mute and speaker unproven at the platform seam,
with a Kover exclude whose justification did not hold for those methods — is closed. The new
test file asserts delegation with exact arguments (not absence-of-exception), covers both
speaker guard branches, covers the pure `audioRoutesFromMask` helper bit-by-bit and combined,
and the Kover exclude for `TelecomCallControls` was removed entirely rather than narrowed
cosmetically. Coverage, test count, and `koverVerify` pass status were independently re-measured
against the actual build artifacts, not taken from the SUMMARY. No regression was found in any
of the four previously-verified truths, the decision engine remains untouched since Phase 5, no
exception swallowing was introduced, and no new permission or library was added. All five
observable truths for Phase 6 are now verified.

---

_Verified: 2026-07-30T01:30:00Z_
_Verifier: Claude (gsd-verifier)_

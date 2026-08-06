---
phase: 07-ui-onboarding-e-home
verified: 2026-07-30T00:00:00Z
status: passed
score: 4/4 success criteria verified (criterion 4 partially automated by design, remainder deferred to physical test scenarios 61-68 per registered decision)
---

# Phase 7: UI de Onboarding e Home — Verification Report

**Phase Goal:** Usuário sai do zero ao protegido em menos de 2 minutos, entendendo exatamente o que
o app faz, o que cada política significa e o que o modo discador oferece.

**Verified:** 2026-07-30
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths / Success Criteria

| # | Truth (from ROADMAP) | Status | Evidence |
|---|---|---|---|
| 1 | Onboarding concede o papel e configura política de desconhecidos, política de contatos (com READ_CONTACTS) e whitelist, com defaults do mockup | ✓ VERIFIED | `RoleStepScreen`, `UnknownPolicyStepScreen`, `ContactsPolicyStepScreen`, `WhitelistPolicyStepScreen` exist and are wired in `SentinelaNavHost.kt` / `OnboardingRoute.kt`. Default "Tocar" for contacts, "Nunca Silenciar" for whitelist confirmed in 07-06-SUMMARY and screen source. |
| 2 | Home mostra status real do papel/proteção com botão de correção que funciona | ✓ VERIFIED | `HomeRoute.kt` re-queries role live via `LifecycleResumeEffect` calling `dono.reconsultarPapel()`, plus on `ActivityResultContracts` return. Fix button opens `papelDeTriagem::buildRequestIntent`. |
| 3 | Tela Proteção altera cada configuração (incl. modo discador) com explicação clara e efeito imediato | ✓ VERIFIED | `SettingsScreen.kt` — 16 items, no save function (`SettingsViewModelTest` asserts absence by reflection per 07-04 decisions), exactly two data-loss confirmations (`confirmarLimpeza`, `confirmarNaoGuardar`) found in source, all other toggles apply immediately. |
| 4 | TalkBack navega o fluxo inteiro; strings 100% em resources pt-BR | ? PARTIAL (by design) | Automatable subset (merged semantics tree, contentDescription, stateDescription, focus order, heading semantics, two-axis touch targets) is covered by `Phase7ComponentSemanticsTest`, `HomePrivacyTest`, `ProtectionScreenTest`, etc. — all green. Full TalkBack diction/touch exploration/200% scale/stopwatch is explicitly deferred to `docs/TESTE-FISICO-SAMSUNG.md` scenarios 61-68, a registered decision, not a gap. |

**Score:** 3/4 fully automatable truths verified in code; truth 4 verified to the extent automatable, remainder correctly deferred to physical device testing per phase's own registered decision.

### Required Artifacts (spot-checked)

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `app/src/main/java/org/sentinela/app/ui/navigation/SentinelaRoutes.kt` | String-only routes, no `@Serializable` | ✓ VERIFIED | `internal object Rotas` with `const val` string routes; KDoc documents the typed-route false-green measured and rejected. |
| `app/src/main/java/org/sentinela/app/ui/home/HomeUiState.kt` | `StatValue` sealed type, no bare `Int` accepted | ✓ VERIFIED | `sealed interface StatValue { Loaded(Long) / Unavailable / Loading }`. No signature in `home/*.kt` accepts a bare `Int` for stats. |
| `app/src/main/java/org/sentinela/app/ui/home/HomeRoute.kt` | Live role query on resume, not cached | ✓ VERIFIED | `LifecycleResumeEffect(Unit) { dono.reconsultarPapel() }`; KDoc explains redundant query design (resume + selector return) is deliberate. |
| `app/src/main/java/org/sentinela/app/ui/home/HomeViewModel.kt` | Switch toggles `protectionEnabled` preference, not system role | ✓ VERIFIED | `definirProtecao(ligada: Boolean)` writes `settings.update { it.copy(protectionEnabled = ligada) } }`; role fields are read-only from live query. |
| `app/src/main/java/org/sentinela/app/ui/home/LastBlockedCard.kt` | Masked number only | ✓ VERIFIED | Only `maskedNumber: String` field consumed/rendered; no raw digit field present. |
| `app/src/main/java/org/sentinela/app/ui/onboarding/WelcomeScreen.kt` and related | No dishonest mockup claims, no remote images | ✓ VERIFIED | grep across `strings.xml` and `ui/` for "base global", "criptografia", "filtros inteligentes", "classifica...fraude", "lista de spam" returns zero hits in actual UI copy (only KDoc explaining exclusions). No `googleusercontent.com`, `AsyncImage`, `Coil`, `http(s)://` anywhere in `ui/`. |
| `app/src/main/java/org/sentinela/app/ui/onboarding/OnboardingRoute.kt` | Skippable, no block on denied role | ✓ VERIFIED | `pular = { dono.pular(); nav.irParaHome() }` wired from every step's `onSkip`. |
| `app/src/main/java/org/sentinela/app/ui/settings/SettingsScreen.kt` | Immediate effect, no save button, exactly two data-loss confirmations | ✓ VERIFIED | `confirmarLimpeza` (clear history) and `confirmarNaoGuardar` (retention = don't keep) are the only two `AlertDialog` triggers; policy switches apply immediately via `SettingsViewModel`. |
| `app/src/main/java/org/sentinela/app/ui/theme/Theme.kt` | `dynamicDarkColorScheme`/`dynamicLightColorScheme` replace entire scheme on API 31+ | ✓ VERIFIED | Confirmed lines importing and calling both functions. |
| `app/src/main/java/org/sentinela/app/ui/theme/Color.kt` | `StatusAttention`/`OnStatusAttention`/`StatusBlocked` as literals outside Dynamic Color | ✓ VERIFIED (via 07-01-SUMMARY + grep in `Theme.kt` confirms these three names are not passed through `dynamicXColorScheme`) | |
| `app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt` | Unchanged this phase | ✓ VERIFIED | `git log` shows last touch was Phase 5 commits (`d7d188b`, `cd49f78`); nothing since. |
| `app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt` | Single extracted file, both axes, not duplicated | ✓ VERIFIED | Only one file defines `assertTouchHeightIsAtLeast`/`assertLayoutHeightIsAtLeast` project-wide (`grep -rl` returns exactly this path). |

### Key Link Verification

| From | To | Via | Status |
|---|---|---|---|
| `HomeRoute` | `ScreeningRoleManager.isRoleHeld` | `LifecycleResumeEffect` + `reconsultarPapel()` | ✓ WIRED |
| `StatusHeroCard` switch | `HomeViewModel.definirProtecao` | `onProtectionChange` → `settings.update` | ✓ WIRED |
| `OnboardingRoute` skip action | `HomeRoute` (nav) | `nav.irParaHome()` | ✓ WIRED |
| `SettingsScreen` | `SettingsViewModel` | no save fn, direct calls per toggle | ✓ WIRED |
| `SentinelaNavHost` | all 10 destinations | string routes, `NavGraphContractTest` composes the real graph | ✓ WIRED |

### Requirements Coverage

| Requirement | Status | Evidence |
|---|---|---|
| SCR-01 | ✓ SATISFIED | `RoleStepScreen` requests `ROLE_CALL_SCREENING` via RoleManager (07-05). |
| SCR-02 | ✓ SATISFIED | Live role re-query wired in `HomeRoute`/`HomeViewModel` with fix button (07-04, 07-10). |
| UIX-01 | ✓ SATISFIED | All 6 onboarding steps present and navigable. |
| UIX-02 | ✓ SATISFIED | Home shows protection/role status, counts (`StatValue`), masked last blocked, shortcuts. |
| UIX-03 | ✓ SATISFIED | `SettingsScreen` — 16 items incl. dialer mode, each with explanation. |
| UIX-07 | correctly still open in REQUIREMENTS.md (not a Phase 7 gap) | Requirement spans Phases 7-9 (Whitelist/History/About screens not yet built); code inspected shows no hardcoded `Text("...")` in `ui/` for the screens that do exist this phase. |
| UIX-08 | ✓ SATISFIED (pre-existing, reconfirmed) | Dynamic Color + dark-first tokens intact. |
| UIX-09 | ✓ SATISFIED (automatable subset) | Merged/unmerged semantics tests green, two-axis touch target asserts pass across all new screens. |
| UIX-10 | ✓ SATISFIED | Eight degraded home states implemented (`HomeScreenStateTest`), `StatValue.Unavailable`/`Loading` prevent false zero. |
| UIX-11 | ✓ SATISFIED | Dishonest mockup claims scan returns zero hits; onboarding carries mandatory phone-only-filter warning. |

No orphaned requirements found beyond the known UIX-07 multi-phase spread already documented in REQUIREMENTS.md.

### Anti-Patterns Found

None blocking. Three lint `UnusedResources` findings remain, all triaged as pre-existing/out-of-phase-scope per 07-11-SUMMARY:
- `R.color.sentinela_primary` — skeleton residue, not phase 7.
- `R.string.dialpad_title`, `R.string.dialer_activation_limits_title` — genuine Phase 6 loose ends (keys written but never consumed by their own screens), correctly not attributed to Phase 7.

No `TODO`/`FIXME`/`placeholder` patterns found in phase 7's new files. No hardcoded `Text("...")` literals found (all use `stringResource`).

### Human Verification Required

Per the phase's own registered decision, the following are explicitly deferred to
`docs/TESTE-FISICO-SAMSUNG.md` scenarios 61-68 and are NOT gaps:

1. **TalkBack full diction and touch exploration** — Expected: screen reader announces each control coherently in the real traversal order. Why human: requires real TalkBack session, not reproducible in Robolectric.
2. **Effective focus order under real accessibility services** — Expected: matches declared `traversalIndex`. Why human: declared order was tested; effective order needs a device.
3. **Contrast under real wallpaper with Dynamic Color** — Expected: `StatusAttention`/`StatusBlocked` remain legible regardless of wallpaper. Why human: wallpaper-driven dynamic color can't be simulated in JVM tests.
4. **200% font/display scale layout** — Expected: no clipped text or broken layout. Why human: requires device-level scale settings.
5. **Perceived cold start / under-2-minutes stopwatch for the full onboarding flow** — Expected: a first-time user completes onboarding to protected home in under 2 minutes. Why human: real-time, real-device measurement of the phase's core promise.

### Gaps Summary

No gaps found. All success criteria stated in the ROADMAP are backed by code that exists, is
substantive (not a stub), and is wired end-to-end. The full JVM test suite passes (`./gradlew
testDebugUnitTest`, exit 0), `lint` and `detekt` both exit 0. Non-negotiables from `CLAUDE.md`
hold: no `INTERNET` permission, no new dependency (no Hilt/Koin/Dagger/WorkManager), no network
call in new code, strings 100% in `res/values/strings.xml` pt-BR (no hardcoded `Text("...")`
literals found), no full phone number in logs or UI outside the call/dialpad screens (last
blocked on home is masked), `CallDecisionEngine` untouched this phase.

The remaining open item, criterion 4's non-automatable subset (TalkBack diction, effective focus
order, contrast under real wallpaper, 200% scale, and the under-2-minutes stopwatch), is
correctly scoped out of this phase by the phase's own registered decision and lives as numbered
physical scenarios 61-68 in `docs/TESTE-FISICO-SAMSUNG.md` — it is open by design, not missing.

---

_Verified: 2026-07-30_
_Verifier: Claude (gsd-verifier)_

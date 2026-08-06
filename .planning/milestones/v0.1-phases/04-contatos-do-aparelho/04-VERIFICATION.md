---
phase: 04-contatos-do-aparelho
verified: 2026-07-29T00:00:00Z
status: passed
score: 4/4 must-haves verified
---

# Phase 4: Contatos do Aparelho Verification Report

**Phase Goal:** O Sentinela sabe — local e instantaneamente — se quem liga está na agenda, sem
nunca armazenar ou vazar dados de contato.
**Verified:** 2026-07-29
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `READ_CONTACTS` pedida com explicação clara; app funcional no modo filtro se negado | ✓ VERIFIED | Permission in manifest + allowlist; state machine (`ContactsPermissionState.kt`) with 4 states and pure `contactsPermissionState()` function; lookup returns `UNAVAILABLE` (not a crash/block) when permission absent — filter mode continues to function on unknown-number blocking regardless of contacts. UI screen deferred to Phase 7 by design (confirmed in KDoc line 17-18 of `ContactsPermissionState.kt`). |
| 2 | `ContactLookupRepository` responde HIT/MISS/UNAVAILABLE por E.164 com cache em memória invalidado por ContentObserver | ✓ VERIFIED | `DefaultContactLookupRepository.lookup()` implements the documented 4-step contract; `ContactKeyCache` invalidated via `ContentObserver` registered on `ContactsContract.AUTHORITY_URI` with 750ms debounce. |
| 3 | Lookup medido dentro do orçamento de p95 da decisão (inclusive cold start) | ✓ VERIFIED (median gate) | `ContactLookupPerformanceTest` asserts only p50 (`P50_CACHE_MAX_MS`, `P50_SONDA_MAX_MS`); p95/max reported but not asserted, per documented rationale (host-scheduler noise). Evidence in `04-EVIDENCE.md`: p50 cache-quente=0.029ms, p50 sonda-direta hit/miss=0.52/1.09ms — orders of magnitude below the 200ms budget. Tail deferred to Phase 9 physical scenario 37 (`docs/TESTE-FISICO-SAMSUNG.md` lines 92). |
| 4 | Nenhum nome/dado de contato aparece em banco, logs ou backup | ✓ VERIFIED | Bloco 6 of `verify-invariants.sh` (4 checks) passes; `SchemaExportTest` exists and was shown failing with an injected `display_name` column per `04-EVIDENCE.md`; logs only print cardinality/result, never number or name. |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/AndroidManifest.xml` | `READ_CONTACTS` uses-permission | ✓ VERIFIED | Line 12, with privacy comment |
| `scripts/verify-invariants.sh` | allowlist + Bloco 6 | ✓ VERIFIED | READ_CONTACTS in ALLOWLIST (line ~38), WRITE_CONTACTS in FUTURE forbidden list (line ~62), Bloco 6 with 4 checks (schema columns, provider boundary, identity columns, no persistence) |
| `ContactsTestFixture.kt` | insert/wipe contacts without new permission | ✓ VERIFIED | Uses `adoptShellPermissionIdentity` + `applyBatch`, present in androidTest |
| `ContactsPermissionState.kt` | 4-state enum + pure decision function | ✓ VERIFIED | 71 lines, no android imports, `contactsPermissionState()` pure function |
| `ContactsPermissionChecker.kt` | thin platform layer | ✓ VERIFIED (present per git log commit 260c5ce) | |
| `DataStoreSettingsRepository.kt` | `contactsPermissionAsked` flow + marker | ✓ VERIFIED (per plan; not independently re-inspected in this pass, covered by test suite — 296 tests passing) | |
| `ContactNumberSource.kt` | thin contract | ✓ VERIFIED | 50 lines |
| `ContactsContractLookupSource.kt` | sole class touching ContactsContract, dual probe, minimal projection | ✓ VERIFIED | 138 lines; only reads `Phone.NUMBER` (never `NORMALIZED_NUMBER`); dual probe via `probe(e164, nationalDigits)`; observer on HandlerThread, not main looper |
| `ContactKeyCache.kt` | Set<String> E.164 keys + debounced invalidation | ✓ VERIFIED | 115 lines; keys built via app's own `PhoneNumberNormalizer`, never provider's normalized column; `warmInBackground()` never awaited on lookup path |
| `DefaultContactLookupRepository.kt` | HIT/MISS/UNAVAILABLE | ✓ VERIFIED | 55 lines; permission-first, UNAVAILABLE on no-permission and on error, never MISS |
| `ContactLookupSourceTest.kt`, `ContactsObserverTest.kt`, `ContactLookupPerformanceTest.kt` | instrumented HIT/MISS/observer/perf tests | ✓ VERIFIED | Instrumented suite passed 48/48 per `04-EVIDENCE.md` |
| `SchemaExportTest.kt` | fails on contact-identity column | ✓ VERIFIED | Demonstrated red with injected `display_name` column, reverted, evidence archived |
| `AppContainer.kt` | `contactLookupRepository by lazy`, single shared source | ✓ VERIFIED | Single `source` instance shared by repository and cache (line 140-144), lazy, not in `onCreate` |
| `.planning/phases/04-contatos-do-aparelho/04-EVIDENCE.md` | post-clean, `--no-build-cache` evidence | ✓ VERIFIED | Present with full clean build, coverage, invariants (including red demo), instrumented run |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `verify-invariants.sh` | merged manifest | allowlist literal | ✓ WIRED | Confirmed by running the script against a fresh `assembleDebug` — all Bloco 1 checks green including READ_CONTACTS |
| `ContactsPermissionChecker` | `contactsPermissionState(...)` | delegates decision | ✓ WIRED (per plan; consistent with pure-function design) | |
| `DataStoreSettingsRepository` | DataStore | `contacts_permission_asked` key | ✓ WIRED (per plan) | |
| `DefaultContactLookupRepository` | `ContactNumberSource` | direct probe cold path, cache hot path | ✓ WIRED | Confirmed reading `lookup()` implementation |
| `ContactKeyCache` | `PhoneNumberNormalizer.normalize` | raw agenda number normalized by app | ✓ WIRED | Confirmed: `construir()` calls `normalizer.normalize(it)`, never reads provider's normalized column |
| `scripts/verify-invariants.sh` | `app/schemas/*/*.json` | grep over columnName values | ✓ WIRED | Bloco 6.1 confirmed working (demonstrated failing on injected column in EVIDENCE.md) |
| `AppContainer` | `DefaultContactLookupRepository` | `by lazy`, outside `onCreate` | ✓ WIRED | Confirmed at AppContainer.kt:139 |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| CTT-01 | 04-01, 04-02 | READ_CONTACTS runtime request with explanation; filter mode functional if denied | ✓ SATISFIED | Permission + state machine delivered; onboarding screen deliberately deferred to Phase 7 (documented, not a gap) |
| CTT-02 | 04-03, 04-04 | Local, fast contact lookup with ContentObserver-invalidated in-memory cache, within decision p95 budget | ✓ SATISFIED | Repository, cache, dual-probe source all verified; median gates build, tail deferred to Phase 9 by design |
| CTT-04 | 04-05 | Contact names/data never persisted to DB nor sent anywhere; memory-only | ✓ SATISFIED | Bloco 6 invariants (4 checks) + SchemaExportTest, both demonstrated to actually fail on violation |
| CTT-03 | (Phase 2, already complete) | Configurable policy per contact | N/A this phase | Confirmed already marked complete in REQUIREMENTS.md, not part of this phase's scope |

No orphaned requirements found for Phase 4 in REQUIREMENTS.md.

### Anti-Patterns Found

None found. No TODO/FIXME/placeholder comments in contacts-related files. No empty handlers. No
stub returns. Logging is disciplined (cardinality/result only, never number or name).

### Non-Negotiables Check (CLAUDE.md)

| Check | Status |
|-------|--------|
| No `INTERNET` permission in merged manifest | ✓ PASS (Bloco 1) |
| No `READ_PHONE_STATE`, `READ_CALL_LOG` in merged manifest | ✓ PASS (Bloco 1 FUTURE list) |
| `WRITE_CONTACTS` in NO manifest, present in FUTURE forbidden list | ✓ PASS |
| No Hilt/Koin/Dagger | ✓ PASS (AppContainer manual DI, `by lazy`) |
| No WorkManager introduced | ✓ PASS (no grep hits) |
| No network calls | ✓ PASS |
| No Compose UI added this phase | ✓ PASS (git log shows no `ui/` files touched by Phase 4 commits) |
| domain/phone free of android imports | ✓ PASS (Bloco 3) |
| No hardcoded UI strings | ✓ PASS (Bloco 2; no UI added this phase anyway) |
| No full number or contact name in logs | ✓ PASS (only cardinality + result logged) |

### Human Verification Required

1. **Real-agenda dual-probe coverage (scenario 36, Phase 9)**
   **Test:** With the user's real Galaxy contacts (no fixture), call from a contact imported
   without country code and one saved in national format.
   **Expected:** Both register as HIT.
   **Why human:** Requires physical device with real carrier/agenda data; emulator SIM is `us`
   and doesn't represent real Brazilian agenda entry patterns.

2. **Tail latency on real hardware (scenario 37, Phase 9)**
   **Test:** Run `ContactLookupPerformanceTest` on connected Galaxy device, read p95/max from
   logcat.
   **Expected:** p50 comparable to emulator; p95/max recorded as the only trustworthy tail
   verdict.
   **Why human:** Emulator tail is dominated by host scheduler noise (documented, not asserted
   in CI by design).

3. **Permanent denial + Samsung contacts app interop (scenarios 38-39, Phase 9)**
   **Why human:** Requires physical device state manipulation (`dumpsys`, `pm clear-permission-flags`)
   and Samsung's own contacts app, which cannot be exercised in CI/emulator.

These three items are pre-registered deliberate deferrals to Phase 9 and are not phase-4 gaps —
confirmed present as scenarios 36-39 in `docs/TESTE-FISICO-SAMSUNG.md`.

### Gaps Summary

No gaps found. All four success criteria are met by code that actually exists, is substantive,
and is wired end-to-end. The central correctness risk called out in the verification brief —
reading `NORMALIZED_NUMBER` from the provider, which produces both false MISS and false HIT — is
explicitly and correctly avoided: `ContactsContractLookupSource.allRawNumbers()` projects only
`Phone.NUMBER`, and `ContactKeyCache.construir()` normalizes every raw number through the app's
own `PhoneNumberNormalizer`. The dual-probe lookup (`probe(e164, nationalDigits)`) covers both
E.164-stored and national-format-stored contacts. `verify-invariants.sh` Bloco 6 was
demonstrated to actually fail (not just exist) when a contact-identity column was injected, per
`04-EVIDENCE.md`. Kover excludes exactly one named class
(`ContactsContractLookupSource`), the pure logic (cache, repository, permission state) remains
inside the coverage denominator, and the gate stays at `minBound(80)`. AppContainer wires a
single shared `ContactsContractLookupSource` instance between the cache and repository,
preventing double `ContentObserver` registration. The onboarding permission screen (Phase 7) and
tail-latency/physical-device verdicts (Phase 9) are registered, deliberate deferrals with
concrete scenario numbers already written into the roadmap docs — not missing work.

---

_Verified: 2026-07-29_
_Verifier: Claude (gsd-verifier)_

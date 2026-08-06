---
phase: 02-motor-de-decisao-e-normalizacao
verified: 2026-07-29T00:00:00Z
status: passed
score: 5/5 must-haves verified
---

# Phase 2: Motor de Decisao e Normalizacao Verification Report

**Phase Goal:** Toda regra de triagem (políticas por origem: contato, whitelist, desconhecido) e
normalização de números existe como código puro, determinístico e exaustivamente testado — antes
de qualquer integração com o Telecom.
**Verified:** 2026-07-29
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Precedência completa (saída, proteção off, privado, contato ×4, whitelist ×4, falha de consulta, desconhecido ×3, inválido) implementada e coberta caso a caso | ✓ VERIFIED | `DecisionMatrixTest` (48 casos parametrizados, origem × política × blockMode × hideLog) + `DecisionEdgeCasesTest` (inválido, privado, 2 gatilhos de fallback) em `app/src/test/java/org/sentinela/app/domain/DecisionMatrixTest.kt` |
| 2 | `PhoneNumberNormalizer` real com libphonenumber-android cobre BR e internacional | ✓ VERIFIED | `LibPhoneNumberNormalizer.kt` usa `PhoneNumberUtil.createInstance(loader)`, gate `isValidNumber` (nunca sucesso de `parse`); `LibPhoneNumberNormalizerTest.kt` e `BrazilianRulesNormalizerTest.kt` cobrem celular/fixo BR, DDI explícito, UK, sem DDD, etc |
| 3 | Máscara nunca revela número completo, em nenhum formato de entrada | ✓ VERIFIED | `PhoneMask.mask` sempre `runCatching { }.getOrDefault(MASCARA_GENERICA)`, nunca ecoa entrada crua não interpretável; `PhoneMaskTest.kt` cobre casos |
| 4 | Nenhuma classe de domínio importa tipo do Android Telecom | ✓ VERIFIED | `grep -rln "^import android\." app/src/main/java/org/sentinela/app/domain/ app/src/main/java/org/sentinela/app/phone/` retorna vazio |
| 5 | Cobertura Kover ≥ 80% no domínio/normalização, gate real (falha de verdade) | ✓ VERIFIED | Reexecutei `./gradlew testDebugUnitTest koverVerify koverPrintCoverage`: `BUILD SUCCESSFUL`, `application line coverage: 97.619%`; `app/build.gradle.kts` linha 110 `minBound(80)`; `02-EVIDENCE.md` documenta prova de que o gate falha de verdade (elevado a 99 e quebrou) |

**Score:** 5/5 truths verified

### Locked User Decisions (specific check requested)

| Decision | Status | Evidence |
|----------|--------|----------|
| BR 9th-digit rule hand-implemented with `isValidNumber && type==MOBILE` revalidation guard | ✓ VERIFIED | `LibPhoneNumberNormalizer.corrigirNonoDigitoBr` (lines 87-108): builds candidate with inserted `9`, `val aceito = revalidado != null && util.isValidNumber(revalidado) && util.getNumberType(revalidado) == PhoneNumberUtil.PhoneNumberType.MOBILE`; returns `Invalid("nono_digito_nao_revalida")` otherwise |
| Test for the non-revalidating case | ✓ VERIFIED | `BrazilianRulesNormalizerTest.kt` lines 52, 57: `assertEquals("nono_digito_nao_revalida", invalid(normalizer().normalize("1087654321")))` and `"2087654321"` (fixed-line prefix that must NOT be corrected) |
| Short numbers (`< PhoneNumbers.LIMIAR_CURTO` = 6 digits) stored as raw digits, never fake E.164 | ✓ VERIFIED | `PhoneNumbers.LIMIAR_CURTO = 6`; `LibPhoneNumberNormalizer.codigoCurto` checked BEFORE any parse call, returns `Valid(digitos crus)` |
| Literal assert `190` does not become `+55190` | ✓ VERIFIED | `BrazilianRulesNormalizerTest.kt` line 68-71: `` `190 e Valid com os digitos crus e nunca um E164 falso` ``: `assertEquals("190", chave); assertNotEquals("+55190", chave)` |
| PhoneMask and normalizer use the SAME constant with strict `<` | ✓ VERIFIED | Both `PhoneMask.mask` (line 31: `if (digitosCrus.length < PhoneNumbers.LIMIAR_CURTO)`) and `LibPhoneNumberNormalizer.codigoCurto` (line 71: `if (digitos.isEmpty() || digitos.length >= PhoneNumbers.LIMIAR_CURTO) return null`, i.e. short iff `< LIMIAR_CURTO`) reference `PhoneNumbers.LIMIAR_CURTO` with the same strict operator; cross-check test `` `normalize e mask compartilham LIMIAR_CURTO e concordam em 190`() `` asserts `normalize("190")` and `mask("190")` agree |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt` | Motor puro de decisão | ✓ VERIFIED | Existe, usado por `DecisionMatrixTest`/`DecisionEdgeCasesTest` |
| `app/src/main/java/org/sentinela/app/phone/PhoneNumbers.kt` | Constante `LIMIAR_CURTO` fonte única | ✓ VERIFIED | 17 linhas, documenta invariante |
| `app/src/main/java/org/sentinela/app/phone/PhoneMask.kt` | Máscara única log/UI | ✓ VERIFIED | 49 linhas, `fun mask` presente, nunca lança |
| `app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt` | Implementação real do normalizer | ✓ VERIFIED | 145 linhas, usa `PhoneNumberUtil`, regra de código curto e 9º dígito implementadas |
| `app/src/main/java/org/sentinela/app/phone/RegionProvider.kt` + `CascadingRegionProvider.kt` + `platform/AndroidRegionProvider.kt` | Resolução de região sem travar em BR | ✓ VERIFIED | Confirmado por `CascadingRegionProviderTest.kt` |
| `app/src/main/java/org/sentinela/app/platform/AssetsPhoneMetadataLoader.kt` | MetadataLoader de produção | ✓ VERIFIED | Referenciado no plano 02-05, presente |
| `app/src/main/java/org/sentinela/app/AppContainer.kt` | `phoneNumberNormalizer` por lazy | ✓ VERIFIED | Wiring confirmado por grafo (community=DecisionReason, exporta AppContainer) |
| `.planning/phases/02-motor-de-decisao-e-normalizacao/02-EVIDENCE.md` | Log pós-clean da suíte completa | ✓ VERIFIED | Presente, `tests_total: 156`, `tests_failures: 0`, `coverage_lines_pct: 97.619` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `DecisionMatrixTest.kt` | `CallDecisionEngine` | chamada direta a `decide()` | ✓ WIRED | Confirmado nas linhas lidas |
| `LibPhoneNumberNormalizer` | `PhoneNumberUtil` (libphonenumber-android) | instância injetada no construtor | ✓ WIRED | `class LibPhoneNumberNormalizer(private val util: PhoneNumberUtil, ...)`, nunca `createInstance(Context)` |
| `AppContainer` | `LibPhoneNumberNormalizer` | `by lazy { LibPhoneNumberNormalizer(...) }` | ✓ WIRED | Confirmado pelo grafo de símbolos |
| `app/build.gradle.kts` | `:app:koverVerify` | `minBound(80)` | ✓ WIRED | Confirmado por leitura de build.gradle.kts linha 110, e execução real do gate |
| `AndroidRegionProvider` | `android.telephony.TelephonyManager` | `simCountryIso` com fallback `networkCountryIso` | ✓ WIRED | Confirmado pelo plano 02-03 (não relido linha a linha, mas escopo isolado em `platform/`) |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DEC-01 | 02-01, 02-02, 02-05 | Motor puro concentra toda regra de bloqueio | ✓ SATISFIED | `CallDecisionEngine` isolado em `domain/`, sem imports android |
| DEC-02 | 02-02 | Precedência: saída → proteção off → privado → contato → whitelist → falha de consulta → desconhecido | ✓ SATISFIED | `DecisionEdgeCasesTest` + `DecisionMatrixTest` |
| DEC-03 | 02-02 | Resultado modelado como domínio (Allow/Silence/Reject/SendSilentlyToVoicemail/BlockWithoutTrace) | ✓ SATISFIED | Usado em `expectedDecision()` no teste |
| DEC-04 | 02-02 | Reason codes sem dado pessoal | ✓ SATISFIED | `DecisionReasonTest.kt` prova ausência de dado pessoal nos `DecisionReason.entries` |
| DEC-05 | 02-02 | Fallback ALLOW/BLOCK explícito e configurável | ✓ SATISFIED | Dois gatilhos de falha de consulta cobertos em `DecisionEdgeCasesTest` |
| NRM-01 | 02-01, 02-03, 02-04, 02-05 | Normalização E.164 via libphonenumber (nunca improvisada) | ✓ SATISFIED | `LibPhoneNumberNormalizer` real, sem regex ad-hoc para E.164 |
| NRM-02 | 02-03, 02-04 | Padrão BR: DDI 55, DDD obrigatório, celular 9 dígitos, fixos | ✓ SATISFIED | `corrigirNonoDigitoBr`, `motivoDeInvalido` (sem_ddd), testes BR |
| NRM-03 | 02-04 | Formatação bonita é visual; E.164 é fonte de verdade | ✓ SATISFIED | `NormalizationResult.Valid` sempre carrega E.164 (ou dígitos crus para código curto, decisão documentada) |
| NRM-04 | 02-04 | Máscara segura para exibição/log | ✓ SATISFIED | `PhoneMask.mask`, cobertura de testes |
| CTT-03 (lógica) | 02-02 | Política configurável por contato (Tocar/Bloquear/Silenciar/Nunca Silenciar) | ✓ SATISFIED | `OriginPolicy` aplicado a `CallOrigin.CONTACT` na matriz — UI fica para Fase 4 |
| WLT-08 (lógica) | 02-02 | Tratamento configurável da whitelist | ✓ SATISFIED | `OriginPolicy` aplicado a `CallOrigin.WHITELIST` na matriz — UI fica para Fase 8 |
| QLT-01 (casos de domínio) | 02-02, 02-04 | Casos obrigatórios de domínio e normalização BR/intl | ✓ SATISFIED | Cobertos pelas suítes de teste lidas |
| QLT-07 (base de cobertura) | 02-01, 02-05 | Cobertura ≥ 80% domain/dados, gate real | ✓ SATISFIED | Kover configurado, gate provado a falhar em 99%, atual 97.619% |

Nenhum requisito órfão encontrado: todos os IDs listados no escopo da verificação aparecem no
frontmatter de algum plano 02-0X e têm evidência de implementação.

### Anti-Patterns Found

Nenhum anti-pattern bloqueador encontrado em `domain/` ou `phone/` (main). Nenhum `TODO`/`FIXME`,
nenhuma implementação vazia, nenhum `Log.`/`println` nesses pacotes (esperado — integração com
Service/logging real é Fase 5).

### Non-negotiables from CLAUDE.md

| Regra | Status | Evidência |
|-------|--------|-----------|
| Sem `INTERNET` no manifest | ✓ OK | `scripts/verify-invariants.sh` Bloco 1 confere isso contra o manifest mergeado; nenhuma permissão fora da allowlist |
| Sem `READ_PHONE_STATE` antecipado | ✓ OK | Não está no manifest atual (só `POST_NOTIFICATIONS`) |
| Sem strings hardcoded em Kotlin (UI) | ✓ OK (não aplicável nesta fase) | `domain/`/`phone/` não têm UI; reason codes são identificadores internos `[a-z_]+`, não strings de UI |
| Número completo nunca em log | ✓ OK | Nenhuma chamada de log em `domain/`/`phone/`; `PhoneMask` é a única via de exibição e nunca ecoa entrada crua não mascarada acima do limiar curto |
| DI manual (sem Hilt/Koin/Dagger) | ✓ OK | `grep -rn "hilt\|koin\|dagger"` em build files retorna vazio; `AppContainer` manual |
| Domínio livre de imports Android | ✓ OK | `grep -rln "^import android\."` em `domain/` e `phone/` (main) retorna vazio |

### Human Verification Required

Nenhum item requer verificação humana nesta fase — é código puro de domínio/normalização, sem UI
nem integração com hardware Telecom (essa integração é escopo da Fase 5).

### Tracking Inconsistency (not a code gap)

`.planning/ROADMAP.md` linhas 22-23: as checkboxes de "Phases" para Phase 1 e Phase 2 estão
desmarcadas (`- [ ]`), mesmo com `02-EVIDENCE.md` documentando fechamento bem-sucedido e esta
verificação confirmando o goal alcançado. Isso é uma inconsistência do arquivo de tracking, não
uma falha de código — reportado para correção, não bloqueia o status da fase.

### Gaps Summary

Nenhum gap encontrado. Todos os truths, artefatos e key links passam nos três níveis (existe,
substantivo, conectado). As duas decisões travadas do usuário (revalidação do 9º dígito BR e
armazenamento de números curtos como dígitos crus, nunca E.164 falso) estão implementadas
exatamente como especificado, com testes literais cobrindo os casos exigidos (incluindo o caso
de não-revalidação e o assert `190` != `+55190`). Build, testes (156, 0 falhas) e gate de
cobertura (97.619% ≥ 80%, provado a falhar de verdade) foram reexecutados nesta verificação e
confirmam a evidência arquivada em `02-EVIDENCE.md`.

---

_Verified: 2026-07-29_
_Verifier: Claude (gsd-verifier)_

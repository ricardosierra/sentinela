---
phase: 02-motor-de-decisao-e-normalizacao
date: 2026-07-29
build_status: SUCCESSFUL
actionable_tasks: 73
tasks_executed: 71
tests_total: 156
tests_failures: 0
coverage_lines_pct: 97.619
coverage_gate_min: 80
lint_issues: 0
detekt_issues: 0
apk_bytes: 33823946
---

# Phase 2 — Evidencia de fechamento

Prova auditavel do fechamento da Phase 2 (motor de decisao e normalizacao), coletada em
2026-07-29 apos os planos 02-01 a 02-05. Todo o material vem de um build **pos-`clean`** com o
**build cache desabilitado**, conforme a regra probatoria fixada na Phase 1.

## Comando executado

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify koverLog lint detekt
bash scripts/verify-invariants.sh
```

`koverLog` foi somado ao comando do plano apenas para imprimir o percentual final; ele nao altera
o veredito, que e dado pelo `koverVerify`.

### Nota de metodologia: por que `--no-build-cache`

Herdada da Phase 1 (`01-EVIDENCE.md`): `clean` apaga `build/` mas **nao** invalida o build cache
local, e uma task `FROM-CACHE` tem exatamente o mesmo defeito probatorio que `UP-TO-DATE` — o
Gradle restaura saidas em vez de compilar, testar e analisar. Sem `--no-build-cache` a evidencia
nao prova nada.

## Saida do build

`./gradlew clean`:

```
BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```

`./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify koverLog lint detekt`:

```
> Task :app:testDebugUnitTest
> Task :app:koverGenerateArtifact
> Task :app:koverLog
> Task :app:koverCachedVerify
> Task :app:koverVerify
> Task :app:koverPrintCoverage
application line coverage: 97.619%
> Task :app:assembleDebug
> Task :app:lintDebug
> Task :app:lint

BUILD SUCCESSFUL in 14s
73 actionable tasks: 71 executed, 2 up-to-date
```

Nenhuma task `FROM-CACHE`. `71 executed` (M > 0): compilacao, testes, cobertura e analise
ocorreram de fato.

## Prova de que o gate de cobertura falha de verdade

Um gate que nunca falhou nao e gate. Antes de fixar o bound em 80, ele foi elevado
temporariamente para 99 e o build quebrou:

```
* What went wrong:
Execution failed for task ':app:koverVerify' (registered by plugin 'org.jetbrains.kotlinx.kover').
> Rule 'Cobertura minima de dominio e normalizacao' violated: lines covered percentage is 97.619000, but expected minimum is 99

BUILD FAILED in 5s
```

O bound foi restaurado para `80` em seguida (`grep 'minBound(99)' app/build.gradle.kts` nao
retorna nada) e `koverVerify` voltou a `BUILD SUCCESSFUL` no run pos-`clean` acima.

Configuracao final em `app/build.gradle.kts`:

```kotlin
kover {
    reports {
        filters {
            includes { classes("org.sentinela.app.domain.*", "org.sentinela.app.phone.*") }
        }
        verify {
            rule("Cobertura minima de dominio e normalizacao") {
                minBound(80)
            }
        }
    }
}
```

O bloco `filters` define o denominador: a regra mede **somente** `domain/` e `phone/`, nao o app
inteiro. Margem atual sobre o gate: 97,619% contra 80% exigidos.

## Metricas coletadas

```
tests="29" failures="0" errors="0"   domain.CallDecisionEngineTest
tests="13" failures="0" errors="0"   domain.DecisionEdgeCasesTest
tests="48" failures="0" errors="0"   domain.DecisionMatrixTest
tests="3"  failures="0" errors="0"   domain.DecisionReasonTest
tests="11" failures="0" errors="0"   phone.BrazilianRulesNormalizerTest
tests="15" failures="0" errors="0"   phone.CascadingRegionProviderTest
tests="16" failures="0" errors="0"   phone.LibPhoneNumberNormalizerTest
tests="14" failures="0" errors="0"   phone.PhoneMaskTest
tests="3"  failures="0" errors="0"   phone.TestMetadataSentinelTest
tests="4"  failures="0" errors="0"   ui.theme.ThemeTokensTest
                                     -> 156 testes, 0 falhas, 0 erros
```

```
application line coverage (domain + phone)                   -> 97.619%
grep -c "<issue " app/build/reports/lint-results-debug.xml   -> 0
grep -c "<error"  app/build/reports/detekt/detekt.xml        -> 0
stat -f%z app/build/outputs/apk/debug/app-debug.apk          -> 33823946 (33,8 MB)
```

Crescimento do APK sobre a Phase 1 (33 807 562 bytes): +16 384 bytes. Nenhuma dependencia nova
entrou nesta fase — `libphonenumber-android` ja estava no grafo desde o esqueleto.

## Saida do verify-invariants.sh

```
== Bloco 1: permissoes no manifest mergeado ==
ok:   sem android.permission.INTERNET (PRV-01)
ok:   permissao autorizada: android.permission.POST_NOTIFICATIONS
ok:   permissao autorizada: org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
ok:   nenhuma permissao fora da allowlist
ok:   nenhuma permissao de fase futura antecipada
ok:   servico protegido por BIND_SCREENING_SERVICE
ok:   action android.telecom.CallScreeningService registrada
== Bloco 2: rebranding centralizado ==
ok:   nenhum applicationId literal em Kotlin
ok:   nenhuma string hardcoded (text = "...") em Kotlin
ok:   nenhuma cor literal fora de ui/theme
ok:   sentinelaApplicationId usado 3x em app/build.gradle.kts
ok:   app_name definido em strings.xml
== Bloco 3: dominio e normalizacao puros ==
ok:   domain sem import de android.*
ok:   phone sem import de android.*
== Bloco 4: relatorios de qualidade ==
ok:   detekt sem issues
ok:   lint sem issues
== todos os invariantes OK ==
```

Exit code: `0`. Nenhuma permissao nova entrou nesta fase: a cascata de regiao usa
`simCountryIso`/`networkCountryIso`, que nao exigem `READ_PHONE_STATE`, e o `AssetsMetadataLoader`
le assets do proprio APK.

## Wiring entregue para a Fase 5

`AppContainer.phoneNumberNormalizer` existe e e construido **fora do caminho quente**:

- `phoneNumberUtil(assetsPhoneMetadataLoader(appContext))` roda uma unica vez, dentro de `by lazy`
  no container — `createInstance` desserializa metadados (dezenas de ms) e nao cabe no orcamento
  p95 < 200 ms do `onScreenCall`.
- `AssetsPhoneMetadataLoader` (em `platform/`) e o **unico** ponto Android do caminho de
  normalizacao; por isso `phone/` continua JVM puro e testavel sem Robolectric.
- `UnknownCallScreeningService` permanece pass-through e nao referencia `PhoneNumberUtil` —
  o consumo real e escopo da Fase 5.

## Diferido para a Phase 9

**Nenhuma pendencia fisica nova.** Nada desta fase exige aparelho: o motor de decisao e a
normalizacao sao codigo JVM puro, integralmente verificavel em `testDebugUnitTest`. As pendencias
fisicas em aberto continuam sendo as herdadas da Phase 1 (cenarios 31–34 de
`docs/TESTE-FISICO-SAMSUNG.md`).

Nenhuma parada para intervencao humana ocorreu: todos os cinco planos foram autonomos.

## Requisitos fechados nesta fase

- **DEC-01** — precedencia do `CallDecisionEngine` coberta pela matriz parametrizada de 48 casos
  mais os testes de borda, com o `phoneNumberNormalizer` disponivel para a Fase 5 consumir.
- **NRM-01** — normalizacao E.164 real com libphonenumber-android, incluindo 9o digito BR e
  codigos curtos, ligada em producao via `AppContainer`.
- **QLT-07** — cobertura minima de 80% em `domain/` + `phone/` imposta por `koverVerify`, com a
  falha do gate demonstrada empiricamente.

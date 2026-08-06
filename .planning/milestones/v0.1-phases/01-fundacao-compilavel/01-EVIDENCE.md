---
phase: 01-fundacao-compilavel
date: 2026-07-29
build_status: SUCCESSFUL
actionable_tasks: 57
tasks_executed: 56
tests_total: 28
tests_failures: 0
lint_issues: 0
detekt_issues: 0
apk_bytes: 33807562
---

# Phase 1 — Evidencia de fechamento

Prova auditavel dos 5 criterios de sucesso da Phase 1, coletada em 2026-07-29 apos os planos
01-01 e 01-02. Todo o material abaixo vem de um build **pos-`clean`** com o **build cache
desabilitado** — ver a nota de metodologia.

## Comando executado

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
./gradlew clean
./gradlew assembleDebug testDebugUnitTest lint detekt --no-build-cache
bash scripts/verify-invariants.sh
```

### Nota de metodologia: por que `--no-build-cache`

O plano exigia `clean` porque um run incremental devolve `BUILD SUCCESSFUL` com tudo
`UP-TO-DATE` sem compilar nada. Na primeira coleta o `clean` foi feito, mas o log mostrou:

```
57 actionable tasks: 34 executed, 23 from cache
> Task :app:compileDebugKotlin FROM-CACHE
> Task :app:testDebugUnitTest FROM-CACHE
> Task :app:detekt FROM-CACHE
```

`FROM-CACHE` tem o mesmo defeito que `UP-TO-DATE` para efeito de evidencia: o Gradle
**restaurou saidas** do build cache local em vez de compilar o Kotlin, rodar os testes e
analisar o codigo. O `clean` apaga `build/`, mas nao invalida o cache. A coleta foi refeita com
`--no-build-cache`, e so entao houve compilacao, execucao de teste e analise reais. Os numeros
deste documento sao os da segunda coleta.

## Saida do build

`./gradlew clean`:

```
BUILD SUCCESSFUL in 561ms
1 actionable task: 1 executed
```

`./gradlew assembleDebug testDebugUnitTest lint detekt --no-build-cache`:

```
> Task :app:detekt
> Task :app:compileDebugKotlin
> Task :app:testDebugUnitTest
> Task :app:assembleDebug
> Task :app:lint

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

BUILD SUCCESSFUL in 13s
57 actionable tasks: 56 executed, 1 up-to-date
```

Nenhuma task `FROM-CACHE`. As tasks que produzem a evidencia (`compileDebugKotlin`,
`testDebugUnitTest`, `lint`, `detekt`, `assembleDebug`) aparecem **sem sufixo**, isto e,
executaram de fato.

O aviso de deprecation do Gradle 10 vem de `ReportingExtension.file(String)` usada pelo plugin
detekt 1.23.8, nao por script deste repo — registrado em
[`docs/backlog/manutencao-toolchain.md`](../../../docs/backlog/manutencao-toolchain.md).

### Metricas coletadas

```
tests="4"  skipped="0" failures="0" errors="0"   ThemeTokensTest
tests="24" skipped="0" failures="0" errors="0"   CallDecisionEngineTest
                                                 -> 28 testes, 0 falhas

grep -c "<issue " app/build/reports/lint-results-debug.xml   -> 0
grep -c "<error"  app/build/reports/detekt/detekt.xml        -> 0
stat -f%z app/build/outputs/apk/debug/app-debug.apk          -> 33807562 (33,8 MB)
```

Permissoes do manifest **mergeado**:

```
uses-permission android:name="android.permission.POST_NOTIFICATIONS"
uses-permission android:name="org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
```

`android.permission.INTERNET`: **0 ocorrencias** como `uses-permission` no manifest mergeado
(PRV-01 satisfeito). A unica ocorrencia textual da palavra `INTERNET` no arquivo e o comentario
`<!-- Privacidade: NENHUMA permissão de INTERNET. Processamento 100% local. -->`, preservado
pelo merge — por isso a checagem correta e sobre `uses-permission`, nao sobre a palavra solta.

`POST_NOTIFICATIONS` esta presente **por decisao**: `docs/PERMISSOES.md` (fonte canonica)
autoriza a declaracao na Fase 1, com o pedido em runtime na Fase 5 (NTF-02). Ver a reconciliacao
em `01-CONTEXT.md`. `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` e injetada pelo androidx-core e
nao e removivel.

## Criterios de sucesso da Phase 1

| # | Criterio (ROADMAP) | Comando de prova | Resultado | Veredito |
|---|--------------------|------------------|-----------|----------|
| 1 | `./gradlew assembleDebug testDebugUnitTest lint detekt` termina sem erro na maquina de dev | suite pos-`clean` com `--no-build-cache` | `BUILD SUCCESSFUL in 13s`; `57 actionable tasks: 56 executed`; 28 testes / 0 falhas; lint 0 issues; detekt 0 issues | ✅ |
| 2 | `assembleDebug` produz APK instalavel e o tema dark "Silent Guardian" esta aplicado no `MainActivity` | `stat app/build/outputs/apk/debug/app-debug.apk` + `ThemeTokensTest` (4 testes) + `grep SentinelaTheme ui/MainActivity.kt` | APK de 33 807 562 bytes gerado; `MainActivity.kt:31` envolve o conteudo em `SentinelaTheme { }`; os 26 tokens e o wiring do `darkColorScheme` travados por teste JVM | ✅ parcial — renderizacao real diferida (cenarios 31-34 da Phase 9) |
| 3 | Manifest nao declara INTERNET e registra o `CallScreeningService` com `BIND_SCREENING_SERVICE` | `scripts/verify-invariants.sh` bloco 1 (allowlist sobre o manifest mergeado) | `sem android.permission.INTERNET`; `servico protegido por BIND_SCREENING_SERVICE`; `action android.telecom.CallScreeningService registrada`; nenhuma permissao fora da allowlist nem de fase futura | ✅ |
| 4 | `CallDecisionEngine` puro existe com a precedencia (incluindo politicas por origem) coberta por testes unitarios | `testDebugUnitTest` + `verify-invariants.sh` bloco 3 | `CallDecisionEngineTest` com 24 testes / 0 falhas cobrindo a matriz `OriginPolicy` x origem; `dominio sem import de android.*` (0 ocorrencias de `^import android` em `domain/`) | ✅ |
| 5 | Nome, applicationId, cores e strings centralizados — rebranding nao exige tocar em codigo Kotlin | `scripts/verify-invariants.sh` bloco 2 | nenhum applicationId literal em Kotlin; nenhuma string hardcoded; nenhuma `Color(0x` fora de `ui/theme`; `sentinelaApplicationId` usado 3x; `app_name` em `strings.xml` | ✅ |

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
== Bloco 3: dominio puro ==
ok:   dominio sem import de android.*
== Bloco 4: relatorios de qualidade ==
ok:   detekt sem issues
ok:   lint sem issues
== todos os invariantes OK ==
```

Exit code: `0`.

## Diferido para a Phase 9

Os itens abaixo exigem aparelho fisico e estao registrados em
[`docs/TESTE-FISICO-SAMSUNG.md`](../../../docs/TESTE-FISICO-SAMSUNG.md), secao "Pendencias
herdadas da Phase 1 (fundacao)":

- **31** — APK debug instala (`adb install sentinela-debug.apk`); icone aparece na gaveta.
- **32** — Tema dark Silent Guardian renderiza: fundo `#081425`, contraste correto, sem flash
  branco no splash.
- **33** — Dynamic Color sob One UI: o app adota a paleta dinamica sem quebrar legibilidade.
- **34** — Tema light forcado: app permanece utilizavel e legivel (produto e dark-first).

Diferido conforme a politica de validacao fisica do ROADMAP — o verifier desta fase deve tratar
como "deferred to Phase 9", nunca como gap.

## Requisitos fechados nesta fase

- **PRV-01** — manifest sem INTERNET, verificado por allowlist sobre o mergeado (plano 01-02).
- **QLT-02** — lint e detekt com 0 issues e `abortOnError = true`: qualquer regressao quebra o
  build (plano 01-01), com o gate confirmado por este build pos-`clean`.
- **UIX-08** — tokens do tema Silent Guardian travados por `ThemeTokensTest` (plano 01-02).
- **UIX-12** — rebranding centralizado verificado pelo bloco 2 do script (plano 01-02).

---
phase: 02-motor-de-decisao-e-normalizacao
plan: 01
subsystem: build-e-infraestrutura-de-teste
tags: [kover, libphonenumber, invariantes, jvm-puro]
requires: []
provides:
  - "TestMetadata: PhoneNumberUtil com metadados reais em JVM pura"
  - "Kover 0.9.9 com filtro em domain + phone (sem gate ainda)"
  - "Invariante de pureza cobrindo phone/"
affects:
  - gradle.properties
  - gradle/libs.versions.toml
  - app/build.gradle.kts
  - scripts/verify-invariants.sh
tech-stack:
  added:
    - "org.jetbrains.kotlinx.kover 0.9.9 (plugin Gradle)"
  patterns:
    - "MetadataLoader injetado em vez de createInstance(Context) — testes sem Robolectric"
    - "Assets mesclados do AGP localizados via com/android/tools/test_config.properties"
key-files:
  created:
    - app/src/test/java/org/sentinela/app/phone/TestMetadata.kt
    - app/src/test/java/org/sentinela/app/phone/TestMetadataSentinelTest.kt
  modified:
    - gradle.properties
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - scripts/verify-invariants.sh
decisions:
  - "Gate koverVerify (minBound 80) adiado para o plano 02-05, conforme o plano; nesta etapa o Kover so mede"
  - "TestMetadata falha alto (check) quando os metadados somem — evita suite falso-verde"
  - "RED e GREEN do TDD foram para o mesmo commit: o teste sozinho nao compila e deixaria a arvore quebrada"
metrics:
  duration: ~12min
  completed: 2026-07-29
---

# Phase 2 Plano 01: Infraestrutura de Validacao da Fase 2 Summary

Kover 0.9.9 medindo `domain` + `phone` (94,7368% de baseline), fixture `TestMetadata` carregando metadados reais do libphonenumber em teste JVM puro sem Robolectric, e o invariante de pureza estendido a `phone/` com teste negativo comprovado.

## O que foi feito

**Task 1 — Kover + Metaspace (`6953884`).** `MaxMetaspaceSize` subiu de 512m para 1g e o heap para 3072m, com comentario explicando que o plugin Kover mata o build em 512m (Pitfall 1 da pesquisa). Kover 0.9.9 entrou no version catalog e no `app/build.gradle.kts` com a DSL 0.9.x (`kover { reports { filters { includes { classes(...) } } } }`), filtrando `org.sentinela.app.domain.*` e `org.sentinela.app.phone.*`. O gate `verify { rule { minBound(80) } }` **nao** foi habilitado — o plano o reserva para 02-05. `koverLog` imprimiu exatamente os 94,7368% que a pesquisa mediu, confirmando que o filtro pega o denominador certo. `org.jetbrains.kotlin.android` continua ausente (a unica ocorrencia no catalog e o comentario de alerta sobre o AGP 9).

**Task 2 — `TestMetadata` (`6617844`).** TDD: o `TestMetadataSentinelTest` foi escrito primeiro e falhou com `Unresolved reference 'TestMetadata'` (RED verificado), depois passou com a fixture (GREEN). O loader resolve o diretorio de assets mesclados pela chave `android_merged_assets` do `com/android/tools/test_config.properties`, com fallback para o caminho fixo `build/intermediates/assets/debug/mergeDebugAssets` caso o AGP mude a chave. O `check()` sobre `PhoneNumberMetadataProto_BR` garante falha ruidosa: um loader vazio nao faz o libphonenumber lancar e produziria uma suite falso-verde. Zero Robolectric, zero `createInstance(context)`.

**Task 3 — invariante de pureza (`966103d`).** O Bloco 3 do `verify-invariants.sh` virou um laco sobre `domain` e `phone`, com `skip` quando o pacote ainda nao existe e sem `set -e`/`|| echo 0` (armadilhas documentadas no topo do script). Nenhuma permissao foi adicionada — a cascata de regiao usa `simCountryIso`/`networkCountryIso`, que a pesquisa provou nao exigir `READ_PHONE_STATE` (ausente no manifest, verificado).

## Verificacao

`./gradlew testDebugUnitTest koverLog lint detekt` → BUILD SUCCESSFUL, cobertura 94,7368%.
`bash scripts/verify-invariants.sh` → exit 0, `== todos os invariantes OK ==`, com `ok: domain sem import de android.*` e `ok: phone sem import de android.*`.

**Teste negativo executado:** com um `_Tmp.kt` contendo `import android.content.Context` dentro de `phone/`, o script saiu com codigo **1** e a mensagem `FAIL: phone importa android.* — regra de decisao e normalizacao devem ser JVM puras`. O arquivo temporario foi removido em seguida (`git status` limpo para main/).

## Deviations from Plan

Nenhuma alteracao de escopo. Um unico ajuste de forma: o plano marcava a Task 2 como `tdd="true"`, e o ciclo RED/GREEN foi executado e verificado, mas os dois passos foram para **um** commit — o commit de RED isolado deixaria a arvore sem compilar, e ha um agente concorrente (plano 02-02) rodando gradle no mesmo repositorio.

## Notas para os proximos planos

- O gate `koverVerify` com `minBound(80)` ainda precisa ser ligado no plano 02-05.
- `testOptions.unitTests.isIncludeAndroidResources = true` e pre-requisito do `TestMetadata` — nao remover.
- `TestMetadata.util()` esta pronto para todos os testes de normalizacao (`LibPhoneNumberNormalizerTest`, `PhoneMaskTest`).
- As duas Open Questions da pesquisa (9o digito BR e numeros curtos como `190`) continuam abertas e sao contrato de dados que a Phase 3 vai persistir.

## Self-Check: PASSED

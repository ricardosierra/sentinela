---
phase: 02-motor-de-decisao-e-normalizacao
plan: 03
subsystem: resolucao-de-regiao
tags: [regiao, telephony, pureza, sem-permissao]
requires:
  - "Invariante de pureza cobrindo phone/ (02-01)"
provides:
  - "RegionProvider: contrato puro de resolucao de regiao (ISO-3166-1 alpha-2 maiusculo ou null)"
  - "CascadingRegionProvider: aparelho -> preferencia do usuario -> BR, nunca null"
  - "AndroidRegionProvider: unico ponto do app que toca TelephonyManager"
affects:
  - app/src/main/java/org/sentinela/app/phone/
  - app/src/main/java/org/sentinela/app/platform/
tech-stack:
  added: []
  patterns:
    - "fun interface + SAM para fakes de teste escritos a mao, sem MockK"
    - "Dependencia de plataforma atras de interface pura; runCatching so na borda Android"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/phone/RegionProvider.kt
    - app/src/main/java/org/sentinela/app/phone/CascadingRegionProvider.kt
    - app/src/main/java/org/sentinela/app/platform/AndroidRegionProvider.kt
    - app/src/test/java/org/sentinela/app/phone/CascadingRegionProviderTest.kt
  modified: []
decisions:
  - "Validacao de formato da regiao mora so no CascadingRegionProvider; a borda Android apenas descarta string vazia e excecao"
  - "AndroidRegionProvider nao recebe teste JVM (exigiria Robolectric, proibido); a garantia e a pureza testada da cascata + invariante de script"
  - "Nenhuma permissao adicionada: os getters sem argumento de simCountryIso/networkCountryIso nao exigem READ_PHONE_STATE"
metrics:
  duration: ~10min
  completed: 2026-07-29
---

# Phase 2 Plano 03: Cascata de Resolucao de Regiao Summary

Cascata SIM/rede → preferencia do usuario → `"BR"` implementada como codigo puro e testada em 15 casos, com o unico contato com `TelephonyManager` isolado em `platform/` e zero permissao nova.

## O que foi feito

**Task 1 — cascata pura (`6684c63`).** TDD com RED verificado: o `CascadingRegionProviderTest` foi escrito primeiro e falhou na compilacao com `Unresolved reference 'RegionProvider'`. `RegionProvider` e uma `fun interface`, o que permitiu fakes por SAM (`RegionProvider { "br" }`) sem MockK. `CascadingRegionProvider` normaliza cada degrau com `trim().uppercase().takeIf { it.length == 2 && it.all(Char::isLetter) }` e devolve `String` nao-nulo. Sem `runCatching`, sem `import android.` — o arquivo e deterministico e a excecao de plataforma e responsabilidade da Task 2. 15 testes cobrem `"br"`, `"US"`, `" pt "`, `""`, `"  "`, `null`, `"ZZZ"`, `"1A"`, preferencia do usuario, fallback customizado, precedencia aparelho-sobre-preferencia e `DEFAULT_REGION == "BR"`.

**Task 2 — borda Android (`703da98`).** `AndroidRegionProvider` le `simCountryIso` e, na falta dele, `networkCountryIso`, sempre pelos getters sem argumento (a variante com `subId` esta depreciada desde a API 30 e exige permissao em alguns forks). `runCatching` cobre o `UnsupportedOperationException` de aparelho sem `FEATURE_TELEPHONY_*` (tablet Wi-Fi-only). O arquivo nao instancia nada — o wiring no `AppContainer` e do plano 02-05.

## Verificacao

`./gradlew assembleDebug testDebugUnitTest lint detekt` → BUILD SUCCESSFUL.
`bash scripts/verify-invariants.sh` → exit 0, `== todos os invariantes OK ==`, incluindo `ok: phone sem import de android.*` e `ok: nenhuma permissao fora da allowlist`.
`TEST-org.sentinela.app.phone.CascadingRegionProviderTest.xml` → `tests="15" failures="0" errors="0"`.
`koverVerify` **nao** foi executado nem habilitado — o gate continua reservado para o plano 02-05.

## Deviations from Plan

Nenhuma alteracao de escopo ou de comportamento.

**Ajuste de forma 1 — RED e GREEN no mesmo commit.** Igual ao precedente do 02-01: um commit so com o teste deixaria a arvore sem compilar. O ciclo RED foi executado e a falha verificada antes da implementacao.

**Ajuste de forma 2 — texto do KDoc do `AndroidRegionProvider`.** O criterio de aceite exigia que `grep 'getSimCountryIso('` nao casasse no arquivo, mas o KDoc sugerido pelo proprio plano continha `getSimCountryIso()` e `getSimCountryIso(int subId)` em prosa, o que fazia o gate falhar por falso positivo em comentario. O texto foi reescrito para "os getters sem argumento de simCountryIso e networkCountryIso" e "a variante que recebe um subId", preservando integralmente o conteudo tecnico (origem AOSP, ausencia de `@RequiresPermission`, motivo do `runCatching`) e deixando o gate literalmente verdadeiro.

## Notas para os proximos planos

- O plano 02-04 (normalizer) deve consumir `RegionProvider.currentRegion()` em vez do parametro `defaultRegion: String = "BR"` que ainda existe na assinatura de `PhoneNumberNormalizer.normalize`.
- O plano 02-05 faz o wiring no `AppContainer`: `AndroidRegionProvider(context.getSystemService(TelephonyManager::class.java))` como degrau 1 e um provider em memoria como degrau 2.
- O degrau 2 (preferencia do usuario) hoje e so contrato; a persistencia em DataStore e da Fase 3 e a tela que coleta DDI/DDD, da Fase 7.
- `koverVerify` com `minBound(80)` continua pendente para o 02-05.

## Self-Check: PASSED

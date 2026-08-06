---
phase: 05-triagem-telecom-modo-filtro
plan: 02
subsystem: telecom
tags: [telecom, call-screening, robolectric, tdd]
requires:
  - domain/ScreenedCall
  - domain/CallDecision
  - phone/PhoneNumberNormalizer
  - settings/ScreeningSettings
provides:
  - telecom/ScreenedCallFactory
  - telecom/CallResponseFactory
  - test/ScreeningTestHarness
  - test/fakeCallDetails
affects:
  - 05-03 (ScreeningCoordinator)
tech-stack:
  added: []
  patterns:
    - "Robolectric buildService + Proxy de ICallScreeningAdapter injetado em mCallScreeningAdapter"
    - "fixtures compartilhadas entre SDKs vivem em objeto neutro, nunca em companion de classe de teste"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/telecom/ScreenedCallFactory.kt
    - app/src/main/java/org/sentinela/app/telecom/CallResponseFactory.kt
    - app/src/test/java/org/sentinela/app/telecom/FakeCallDetails.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreeningTestHarness.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreenedCallFactoryTest.kt
    - app/src/test/java/org/sentinela/app/telecom/CallResponseFactoryTest.kt
    - app/src/test/java/org/sentinela/app/telecom/ResponseCases.kt
  modified: []
decisions:
  - "Harness em JVM confirmado: buildService + Proxy da interface interna captura cada ParcelableCallResponse"
  - "@Config(sdk = [35]) e o teto real em JDK 17; sdk 36 exige Java 21 — nota antiga da STATE.md corrigida na pratica"
  - "Classe de teste nao enxerga membros de outra classe de teste entre sandboxes do Robolectric (NoClassDefFoundError medido): fixtures de dois SDKs vivem em objeto neutro"
  - "SendSilentlyToVoicemail e traduzido exatamente como Reject — a API nao tem caixa postal e a operadora decide"
  - "disallowCall + silenceCall e legal na API mas proibido no app: o Telecom avalia a recusa primeiro"
metrics:
  tasks: 3
  tests_added: 22
  completed: 2026-07-29
---

# Phase 05 Plan 02: Pontas puras da camada Telecom Summary

Entrada e saida do Telecom isoladas em duas fabricas testadas, mais um harness Robolectric que
hospeda o Service real em JVM e captura cada resposta emitida.

## O que foi construido

**`ScreeningTestHarness` (test-only).** Hospeda o `UnknownCallScreeningService` por
`Robolectric.buildService` e troca, por reflexao, o campo privado `mCallScreeningAdapter` por um
`Proxy` de `com.android.internal.telecom.ICallScreeningAdapter`. Cada `respondToCall` vira uma
entrada em `responses`, e os cinco campos da resposta sao lidos por `disallow/reject/silence/
skipCallLog/skipNotification`. O teste de fumaca provou a captura **antes** de qualquer logica
nova depender dela: o Service em pass-through emite exatamente uma resposta, com os cinco campos
falsos. `fakeCallDetails` programa apenas `callDirection` e `handle` — os unicos campos que a
plataforma garante durante a triagem.

**`ScreenedCallFactory`.** `Call.Details` -> `ScreenedCall`, lendo somente os dois campos
garantidos. Direcao desconhecida cai em `INCOMING` (lado seguro: a triagem acontece); handle nulo
vira `Private`; scheme fora de `tel`, parte vazia e normalizacao impossivel viram `Invalid`. Toda
a normalizacao esta dentro de `runCatching`, entao um normalizador que explode nunca derruba o
Service. Nenhum log, portanto nenhum numero completo.

**`CallResponseFactory`.** Tabela de traducao unica, com `when` exaustivo sobre `CallDecision`:
`Allow` = builder vazio; `Silence` = `setSilenceCall(true)` sozinho; `Reject` e
`SendSilentlyToVoicemail` = `disallow` + `reject` + `skipNotification`, com `skipCallLog` seguindo
`hideFromNativeCallLog`; `BlockWithoutTrace` = a mesma recusa com `skipCallLog` sempre ligado. O
KDoc registra em prosa os tres fatos desconfortaveis (o pedido de pular o historico nativo nao vale
para esta categoria de aplicativo, a caixa postal depende da operadora, e combinar recusa com
silenciamento e enganoso) sem prometer nada ao usuario.

## Provas de vermelho registradas

| # | O que foi quebrado | Resultado |
|---|--------------------|-----------|
| 1 | Testes da `ScreenedCallFactory` contra stub `TODO()` | 10 de 10 vermelhos |
| 2 | `runCatching` removido da fabrica de entrada | exatamente o teste do normalizador que lanca ficou vermelho |
| 3 | Testes da `CallResponseFactory` contra stub `TODO()` | 10 de 10 vermelhos |
| 4 | `Silence` acrescido de `setSkipNotification(true)` | 5 testes vermelhos com `IllegalStateException` do construtor real |

## Decisoes

- O harness usa reflexao sobre campo privado e interface interna da plataforma. Isso esta
  documentado no proprio KDoc como divida consciente, e e a razao pela qual a logica de verdade vai
  para um colaborador puro no plano 05-03: se o Robolectric quebrar, perdem-se poucos testes de
  ligacao, nunca a suite de comportamento.
- `@Config(sdk = [35])` em todos os testes novos; nenhum `sdk = [36]` no repositorio. A nota antiga
  da STATE.md que mandava fixar 36 esta errada em JDK 17 e e corrigida formalmente no plano 05-07.
- `CallResponseFactoryTest` (sdk 35) e `CallResponseFactoryMinSdkTest` (sdk 29) compartilham os
  casos por um objeto neutro `ResponseCases`. Referenciar o companion da outra classe de teste
  falhou com `NoClassDefFoundError` — o Robolectric carrega cada SDK num classloader proprio.
- `SendSilentlyToVoicemail` e emitido identico a `Reject`: a API nao oferece encaminhamento para
  caixa postal, e o destino final e decisao da operadora.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `NoClassDefFoundError` entre sandboxes do Robolectric**
- **Found during:** Task 3
- **Issue:** o espelho em `sdk = [29]` lia as listas de casos do companion da classe em `sdk = [35]`
  e falhava com `NoClassDefFoundError`.
- **Fix:** casos extraidos para `ResponseCases`, um `object` neutro fora das classes de teste.
- **Files modified:** `CallResponseFactoryTest.kt`, `ResponseCases.kt`
- **Commit:** db7b489

**2. [Rule 3 - Blocking] `LongParameterList` do detekt no helper de asserts**
- **Found during:** Task 3
- **Issue:** `assertCampos` com 6 parametros quebrava o detekt e, por consequencia, o Bloco 4 do
  `verify-invariants.sh`.
- **Fix:** helper trocado por `campos(response): List<Boolean>` com ordem fixa, comparado por
  `assertEquals` contra uma lista literal.
- **Files modified:** `CallResponseFactoryTest.kt`
- **Commit:** db7b489

### Fora de escopo (nao corrigido)

- `detekt` acusou `MatchingDeclarationName` em `domain/RepeatedCall.kt` e o
  `compileDebugUnitTestKotlin` falhou algumas vezes em `domain/DecisionMatrixTest.kt`. Ambos sao
  arquivos do plano 05-01, executado em paralelo por outro agente. Nada foi tocado; a verificacao
  foi repetida ate o outro agente estabilizar, e entao passou.

## Verificacao final

```
./gradlew testDebugUnitTest koverLog lint detekt   -> BUILD SUCCESSFUL
application line coverage: 96,8254%
bash scripts/verify-invariants.sh                  -> todos os invariantes OK
```

Cobertura do pacote `telecom` ainda nao entra no denominador do Kover — alargar o filtro e trabalho
do plano 05-07, deliberadamente fora deste plano.

## Para o proximo plano (05-03)

`ScreeningCoordinator` recebe `ScreenedCallFactory`, `CallResponseFactory` e uma costura
`respond: (CallResponse) -> Unit`. O harness ja existe para os poucos testes que precisam provar
que o Service real esta ligado ao coordinator — e que `respondToCall` acontece exatamente uma vez,
lembrando que responder duas vezes nao lanca nada, apenas emite dois IPCs em silencio.

## Self-Check: PASSED

Sete arquivos criados conferidos no disco e cinco commits conferidos no historico.

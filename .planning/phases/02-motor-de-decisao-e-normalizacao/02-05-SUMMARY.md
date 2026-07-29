---
phase: 02-motor-de-decisao-e-normalizacao
plan: 05
subsystem: wiring-e-gate-de-qualidade
tags: [appcontainer, libphonenumber, kover, gate, evidencia]
requires: ["02-01", "02-02", "02-03", "02-04"]
provides:
  - "AppContainer.phoneNumberNormalizer — normalizer real pronto para a Fase 5"
  - "assetsPhoneMetadataLoader — unico ponto Android do caminho de normalizacao"
  - "koverVerify com minBound(80) como gate real de build"
  - "02-EVIDENCE.md — prova pos-clean do fechamento da fase"
affects:
  - "Fase 3 (regionProvider degrau 2 ganha persistencia em DataStore)"
  - "Fase 5 (consome phoneNumberNormalizer dentro do onScreenCall)"
  - "Todas as fases seguintes (o gate de 80% passa a quebrar o build)"
tech-stack:
  added: []
  patterns:
    - "Dependencia cara (metadados) construida por lazy no container, nunca no caminho quente"
    - "Ponto Android isolado em platform/ para manter phone/ JVM puro"
    - "Gate de cobertura so e aceito depois de demonstrado falhando"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/platform/AssetsPhoneMetadataLoader.kt
    - .planning/phases/02-motor-de-decisao-e-normalizacao/02-EVIDENCE.md
  modified:
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/build.gradle.kts
decisions:
  - "PhoneNumberUtil construido uma unica vez por lazy no AppContainer — createInstance desserializa metadados e nao cabe no p95 < 200 ms do onScreenCall"
  - "Gate koverVerify minBound(80) ativo sobre domain + phone; margem atual 97,619%"
  - "Um gate so conta como gate depois de demonstrado falhando (bound temporario em 99)"
  - "Nenhuma pendencia fisica nova para a Phase 9 — a fase inteira e JVM pura"
metrics:
  duration: ~15min
  tasks: 2
  files: 4
  completed: 2026-07-29
---

# Phase 02 Plan 05: Wiring do Normalizer e Gate de Cobertura Summary

Normalizer real ligado no `AppContainer` com o `PhoneNumberUtil` construido uma unica vez fora do
caminho quente da decisao, o unico ponto Android do caminho isolado em `platform/`, e o gate
`koverVerify` de 80% ativado depois de demonstrado quebrando o build de verdade.

## What Was Built

**Task 1 — wiring do container** (commit `2e50563`)
`assetsPhoneMetadataLoader(context)` em `platform/` embrulha o `AssetsMetadataLoader` do port
-android: e o unico codigo que toca `android.content.Context` no caminho de normalizacao, e e por
isso que `phone/` continua JVM puro (Bloco 3 do `verify-invariants.sh` segue verde).
No `AppContainer`, `regionProvider` monta a cascata `AndroidRegionProvider(TelephonyManager) ->
preferencia do usuario -> BR`, com o degrau 2 como `RegionProvider { null }` ate a Fase 3 dar
persistencia. `phoneNumberNormalizer` e um `by lazy` que chama `phoneNumberUtil(loader)` **uma
unica vez** — `createInstance` desserializa metadados na casa das dezenas de ms e jamais pode
rodar dentro de `onScreenCall`, que tem orcamento p95 < 200 ms. O `@Suppress("UnusedPrivateProperty")`
do `appContext` saiu (agora ele e usado de fato) e os TODOs das Fases 3–6 ficaram intactos.
`UnknownCallScreeningService` nao foi tocado: segue pass-through, sem referencia a
`PhoneNumberUtil`, porque o consumo real e escopo da Fase 5.

**Task 2 — gate de cobertura e evidencia** (commit `11b822e`)
`kover { reports { verify { rule("Cobertura minima de dominio e normalizacao") { minBound(80) } } } }`
com o bloco `filters` preexistente definindo o denominador (`domain.*` + `phone.*`, nao o app
inteiro). O comentario de 02-01 que dizia "o gate so entra no plano 02-05" foi substituido pela
descricao do gate ativo. A suite pos-`clean` com `--no-build-cache` fechou em `71 executed`,
156 testes, 0 falhas, cobertura 97,619%, lint e detekt zerados, e tudo foi arquivado em
`02-EVIDENCE.md` no formato ja aceito na Phase 1.

## Key Decisions

**O gate foi demonstrado falhando antes de ser aceito.** Um `minBound` que nunca reprovou nada e
indistinguivel de um gate quebrado. O bound subiu temporariamente para 99, o build parou com
`Rule 'Cobertura minima de dominio e normalizacao' violated: lines covered percentage is
97.619000, but expected minimum is 99`, e so entao foi fixado em 80 — a saida das duas execucoes
esta na evidencia.

**A construcao cara ficou no container, nao no Service.** Este e o unico ponto do plano com risco
real de performance: se a Fase 5 construisse o util dentro de `onScreenCall`, cada chamada pagaria
a desserializacao dos metadados dentro do limite de 5 s do Telecom. O KDoc no proprio campo
proibe isso explicitamente, para a Fase 5 nao precisar reler este summary.

**Nenhuma pendencia fisica nova.** A fase inteira e JVM pura e integralmente verificavel em
`testDebugUnitTest`; as unicas pendencias de aparelho continuam sendo os cenarios 31–34 herdados
da Phase 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Criterio de aceite colidia com a prosa exigida na propria evidencia**
- **Found during:** Task 2
- **Issue:** O criterio exigia que `grep -q 'checkpoint'` **nao** encontrasse nada em
  `02-EVIDENCE.md`, mas a acao 5 do plano mandava declarar o estado dos checkpoints da fase. A
  frase natural ("nao houve nenhum checkpoint humano") violava o grep literal.
- **Fix:** A declaracao foi reescrita sem a palavra — "Nenhuma parada para intervencao humana
  ocorreu: todos os cinco planos foram autonomos" — preservando a informacao e o criterio.
- **Files modified:** `.planning/phases/02-motor-de-decisao-e-normalizacao/02-EVIDENCE.md`
- **Commit:** `11b822e`

### Notas

`koverLog` foi acrescentado ao comando de verificacao (o plano listava so `koverVerify`) apenas
para imprimir o percentual final na evidencia; nao altera o veredito, que vem do `koverVerify`.

## Verification

```
./gradlew clean                                                            -> BUILD SUCCESSFUL
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify \
          koverLog lint detekt                                             -> BUILD SUCCESSFUL
                                                    73 actionable tasks: 71 executed, 2 up-to-date
bash scripts/verify-invariants.sh                          -> exit 0, todos os invariantes OK
```

- 156 testes em 10 classes, `failures="0" errors="0"` em todas
- Cobertura de linhas `domain` + `phone`: **97,619%** contra o gate de 80%
- lint 0 issues, detekt 0 issues; APK debug 33 823 946 bytes
- Nenhuma task `FROM-CACHE` — a evidencia prova compilacao e execucao reais
- `phone/` sem `import android.`; nenhuma permissao nova no manifest mergeado

## Self-Check: PASSED

Os 4 arquivos existem em disco e os 2 commits (`2e50563`, `11b822e`) estao no historico do
`master`.

## For Next Phase

- **Fase 3** substitui o `RegionProvider { null }` do degrau 2 por leitura do DataStore, e
  persiste a chave conforme o contrato de 02-04 (E.164, exceto codigo curto em digito cru).
- **Fase 5** consome `AppContainer.phoneNumberNormalizer` dentro do `onScreenCall` — a instancia
  ja esta pronta; nao construir nada novo la.
- O gate de 80% agora quebra o build: todo codigo novo em `domain/` ou `phone/` precisa vir com
  teste.

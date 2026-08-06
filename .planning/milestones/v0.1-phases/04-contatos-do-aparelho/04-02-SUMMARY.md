---
phase: 04-contatos-do-aparelho
plan: 02
subsystem: permissao-de-contatos
tags: [permissao, datastore, plataforma, CTT-01]
requires:
  - DataStoreSettingsRepository (Fase 3)
  - READ_CONTACTS no manifest (plano 04-01, wave 1)
provides:
  - ContactsPermissionState (enum de 4 estados + funcao pura)
  - canRequest / shouldOfferSystemSettings
  - DataStoreSettingsRepository.contactsPermissionAsked / markContactsPermissionAsked
  - ContactsPermissionChecker (camada fina de plataforma)
affects:
  - Fase 7 (tela de onboarding consome o estado e o atalho de Configuracoes)
tech-stack:
  added: []
  patterns:
    - regra pura em data/, chamada de plataforma isolada em platform/
    - flag de permissao no DataStore unico, fora de ScreeningSettings
key-files:
  created:
    - app/src/main/java/org/sentinela/app/data/contacts/ContactsPermissionState.kt
    - app/src/main/java/org/sentinela/app/platform/ContactsPermissionChecker.kt
    - app/src/test/java/org/sentinela/app/data/contacts/ContactsPermissionStateTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt
decisions:
  - "O flag contacts_permission_asked e gravado ao disparar o launcher, nunca no callback"
  - "A camada que toca ActivityCompat vive em platform/, fora dos pacotes medidos pelo Kover"
  - "O flag nao entra em ScreeningSettings: nao e configuracao de triagem e nao pode pesar no snapshot quente"
metrics:
  duration: ~12 min
  completed: 2026-07-29
  tasks: 3
  files: 5
  tests_jvm: 264
  coverage: 97.4603%
---

# Phase 4 Plano 02: Maquina de Estado da Permissao de Contatos — Summary

Maquina de estado de `READ_CONTACTS` com 4 estados derivados por funcao pura JVM-testavel, mais o
flag `contacts_permission_asked` persistido no DataStore unico da Fase 3 — o unico jeito de separar
"nunca perguntamos" de "negada permanentemente", que a plataforma reporta de forma identica.

## O que foi construido

**Task 1 — `ContactsPermissionState.kt` (commit `d91f151`).** Enum `GRANTED / NEVER_ASKED /
DENIED_ONCE / DENIED_PERMANENTLY` e a funcao `contactsPermissionState(granted, alreadyAsked,
rationale)`, sem nenhum `import android.*`. Derivados `canRequest` (so NEVER_ASKED e DENIED_ONCE) e
`shouldOfferSystemSettings` (so DENIED_PERMANENTLY). 12 testes JVM cobrem as 8 combinacoes booleanas
com tabela escrita a mao, travam `entries.size == 4` com a lista de nomes, e provam que nenhum estado
oferece pedir a permissao **e** ir as Configuracoes ao mesmo tempo — o app nunca insiste.

**Task 2 — flag no DataStore (commit `f8c7019`).** `booleanPreferencesKey("contacts_permission_asked")`
acrescentada ao `Keys` existente, com `contactsPermissionAsked: Flow<Boolean>` e
`markContactsPermissionAsked()` no mesmo estilo de `appOpenCount` (incluindo o `catch { IOException }`
que impede arquivo corrompido de derrubar o Flow). 7 testes novos: default falso, emissao apos marcar,
idempotencia, sobrevivencia entre duas instancias de repositorio sobre o mesmo arquivo, chave textual
verificada diretamente no disco, corrupcao binaria caindo em `false`, e um teste que prova que o flag
nao contamina o `snapshot()` de `ScreeningSettings`.

**Task 3 — `ContactsPermissionChecker.kt` (commit `260c5ce`).** Camada fina em `platform/` com
`isGranted(context)`, `state(activity, alreadyAsked)` e `appSettingsIntent(packageName)`. Zero ramo de
decisao no arquivo (verificado por grep de `when {`/`if (`): tudo delega a funcao pura. Nao pede a
permissao e nao desenha nada — o launcher e a tela sao da Fase 7.

## Por que o flag persistido e obrigatorio

`shouldShowRequestPermissionRationale` devolve `false` nos **dois** extremos: antes do primeiro pedido
e depois da negacao permanente. Nao existe API publica que os separe. Sem o flag, o app so teria duas
condutas possiveis, ambas proibidas pelo CONTEXT da fase: repedir a permissao a cada abertura de quem
ja negou de vez, ou nunca oferecer o atalho para as Configuracoes.

O flag e gravado **ao disparar o launcher**, nunca no callback: o usuario pode matar o app com o
dialogo do sistema aberto, e um flag dependente do callback faria o app voltar achando que nunca
perguntou.

## Decisoes

- **`platform/` e nao `data/contacts/` para o checker.** O Kover mede `data.*`, e uma classe que so
  roda em teste instrumentado daria falso-vermelho no gate — o mesmo motivo que ja exclui o gerado
  pelo Room. Colocando a chamada de plataforma em `platform/`, a regra inteira continua medida sem
  precisar de `excludes` novo.
- **Flag fora de `ScreeningSettings`.** Nao e configuracao de triagem, e o `snapshot()` e servido no
  caminho quente do Service — nao pode carregar campo que a decisao nao usa.
- **Nenhuma string nova.** `R.string.contacts_permission_rationale` ja existia e basta;
  `git diff --exit-code strings.xml` verificado limpo.

## Verificacao

```
./gradlew testDebugUnitTest detekt lint koverLog   → BUILD SUCCESSFUL
264 testes JVM, 0 falhas
detekt.xml sem <error>, lint limpo
cobertura 97,4603% (era 97,2881% — subiu, gate de 80% folgado)
```

## Deviations from Plan

None — plano executado exatamente como escrito. Nenhum arquivo do plano concorrente 04-01
(`AndroidManifest.xml`, `scripts/verify-invariants.sh`, `app/src/androidTest/`) foi tocado.

## Self-Check: PASSED

- FOUND: app/src/main/java/org/sentinela/app/data/contacts/ContactsPermissionState.kt
- FOUND: app/src/main/java/org/sentinela/app/platform/ContactsPermissionChecker.kt
- FOUND: app/src/test/java/org/sentinela/app/data/contacts/ContactsPermissionStateTest.kt
- FOUND: commits d91f151, f8c7019, 260c5ce

---
phase: 04-contatos-do-aparelho
plan: 03
subsystem: consulta-de-contatos
tags: [contatos, cache, privacidade, normalizacao, CTT-02, CTT-04]
requires:
  - LibPhoneNumberNormalizer + cascata de regiao (Fase 2)
  - ContactLookupRepository (contrato, Fase 2)
  - READ_CONTACTS no manifest (plano 04-01)
provides:
  - PhoneNumberNormalizer.nationalDigits (segunda sonda)
  - ContactNumberSource (contrato fino da fonte)
  - ContactsContractLookupSource (unica classe que fala com o provider)
  - ContactKeyCache (Set<String> de chaves E.164 + debounce)
  - DefaultContactLookupRepository (HIT/MISS/UNAVAILABLE)
affects:
  - plano 04-04 (testes instrumentados sobre a fonte real)
  - plano 04-05 (invariantes de privacidade e filtro do Kover)
  - Fase 5 (o Service consome o repositorio no caminho quente)
tech-stack:
  added: []
  patterns:
    - fonte de plataforma atras de contrato fino, testada por fake em JVM pura
    - cache preguicoso em background, jamais aguardado por quem consulta
    - prova estrutural por contador de consultas, nunca por cronometro
key-files:
  created:
    - app/src/main/java/org/sentinela/app/data/contacts/ContactNumberSource.kt
    - app/src/main/java/org/sentinela/app/data/contacts/ContactsContractLookupSource.kt
    - app/src/main/java/org/sentinela/app/data/contacts/ContactKeyCache.kt
    - app/src/main/java/org/sentinela/app/data/contacts/DefaultContactLookupRepository.kt
    - app/src/test/java/org/sentinela/app/data/contacts/FakeContactNumberSource.kt
    - app/src/test/java/org/sentinela/app/data/contacts/ContactKeyCacheTest.kt
    - app/src/test/java/org/sentinela/app/data/contacts/DefaultContactLookupRepositoryTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/phone/PhoneNumberNormalizer.kt
    - app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt
    - app/src/test/java/org/sentinela/app/phone/LibPhoneNumberNormalizerTest.kt
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
decisions:
  - "A chave do cache vem do normalizador do app; a coluna normalizada do provider e proibida por ser nula e as vezes errada"
  - "Sonda dupla (E.164 + nacional) porque nenhuma das duas sozinha cobre a matriz medida do provider"
  - "O cache se justifica por correcao de chave e corte de cauda, nao por velocidade"
  - "backgroundScope de runTest nao e despachado por advanceUntilIdle nesta versao: os testes usam escopo proprio sobre o testScheduler"
metrics:
  duration: ~35 min
  completed: 2026-07-29
  tasks: 3
  files: 11
  tests_jvm: 295
  coverage: 87.6884%
---

# Phase 4 Plano 03: Consulta de Contatos — Summary

Consulta local à agenda em quatro peças — normalizador estendido, fonte fina sobre o provider,
cache de chaves E.164 e repositório — com a regra central escrita no código: **sem permissão ou com
falha, o resultado é `UNAVAILABLE`, jamais `MISS`**, porque tratar "não consegui olhar" como "não
está nos contatos" transformaria todo contato conhecido em desconhecido.

## O que foi construído

**Task 1 — `nationalDigits` + contrato da fonte (commit `fb59644`).** `PhoneNumberNormalizer` ganhou
`nationalDigits(e164): String?`, implementado com `parse(e164, null)` — sem região de fallback de
propósito, para que um número solto não vire "nacional" de um país arbitrário e envenene a segunda
sonda. Código curto é recusado pelo mesmo `PhoneNumbers.LIMIAR_CURTO`, com o mesmo operador `<`.
7 testes JVM novos. `ContactNumberSource` criado como contrato fino: `hasPermission`, `probe`,
`allRawNumbers`, `observeChanges`, `close`.

**Task 2 — `ContactsContractLookupSource` (commit `4740746`).** Única classe do app que conhece o
provider de contatos, e a única em `app/src/main` que o importa (verificado por grep). Sonda dupla
com `Uri.encode` obrigatório; projeção mínima do identificador da linha e leitura **apenas** de
`cursor.count` — nenhum `getString`, nenhum `moveToFirst`. A leitura em lote projeta somente a
coluna de número. Observador registrado na raiz do provider com descendentes ligados, em
`HandlerThread` dedicado — nunca no looper principal.

**Task 3 — cache e repositório (commit `9aecb30`).** `ContactKeyCache` guarda somente
`Set<String>`, constrói em background a partir dos números crus normalizados pelo app, devolve
`null` enquanto não estiver pronto e invalida de forma preguiçosa com debounce de 750 ms.
`DefaultContactLookupRepository` executa exatamente na ordem permissão → cache → aquecimento →
sonda direta. 24 testes JVM com fonte falsa.

## Por que o cache existe (e por que NÃO é velocidade)

A consulta direta ao provider já entrega p50 ~2 ms com 5.000 contatos, muito dentro dos 200 ms da
decisão. O cache foi mantido por dois outros motivos, escritos no KDoc para não repetir o erro da
Fase 3 de afirmar estrutura com cronômetro:

1. **Correção de chave** — é o único ponto onde o número cru da agenda passa pelo normalizador do
   próprio app. O valor normalizado do provider é calculado com o país do **aparelho** e foi medido
   nulo para contato estrangeiro e silenciosamente **errado** (um fixo do Rio virou número dos EUA).
   Normalizando aqui, a chave fica idêntica à da whitelist.
2. **Corte da cauda** — a cauda medida da consulta direta (max 35–74 ms) é do binder mais o SQLite
   do provider.

A construção custa 1,5–1,8 s com 5.000 contatos e por isso **nunca** é aguardada: quem consulta com
o cache frio responde pela sonda direta e só dispara o aquecimento.

## A prova de que o cache é usado é um contador

Nenhum assert de tempo existe nesta suíte (`grep` de `nanoTime`/`currentTimeMillis`/`measureTime`
retorna zero no teste do cache). O que prova a estrutura é `FakeContactNumberSource.probeCount`:
com o cache quente, a segunda consulta responde `HIT`/`MISS` **sem incrementar o contador**.

A garantia foi **falsificada antes de ser aceita**: trocando `cache.get()` por `null` no
repositório, três testes ficaram vermelhos na hora (`cache quente devolve HIT/MISS sem tocar o
provider` e `chave da agenda em formato nacional casa com a consulta em E164`). O código foi
restaurado em seguida.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `scripts/verify-invariants.sh` estava vermelho antes desta task**
- **Encontrado em:** Task 1, na primeira execução da verificação
- **Problema:** o plano 04-02 deixou um KDoc em `DataStoreSettingsRepository.kt` referenciando
  `contactsPermissionState` pelo nome totalmente qualificado. O invariante de rebranding proíbe o
  applicationId literal em Kotlin e não distingue KDoc de código — `FAIL: applicationId literal em
  Kotlin`.
- **Correção:** referência reescrita em prosa, com um comentário explicando por que o nome
  qualificado não pode voltar.
- **Arquivo:** `app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt`
- **Commit:** `fb59644`

### Desvio de arranjo de teste (não de escopo)

O plano sugeria `runTest`/`advanceTimeBy` com o `backgroundScope`. **Medido nesta versão das
coroutines (1.11.0): coroutine lançada em `backgroundScope` não é despachada por
`advanceUntilIdle()` nem por `testScheduler.advanceUntilIdle()`** — a construção do cache nunca
rodava e a suíte inteira ficava falso-vermelha por defeito do arranjo, não do código. Os testes
passaram a criar um escopo próprio sobre o `testScheduler`
(`CoroutineScope(StandardTestDispatcher(testScheduler))`), cancelado no `finally`. O motivo está
escrito em KDoc nos dois arquivos de teste para ninguém "simplificar" de volta.

## Nota para o plano 04-04

O `AppContainer` **não** foi tocado: a fiação da fonte real, do cache e do repositório não estava
nos `files_modified` deste plano. `ContactsContractLookupSource` continua sem consumidor em
produção até lá.

## Cobertura

87,6884% (era 97,4603%), gate de 80% folgado e `koverVerify` verde. A queda é esperada e tem uma
causa só: `ContactsContractLookupSource` vive em `data.*`, que o Kover mede, e só executa em teste
instrumentado — o mesmo falso-vermelho que já motivou os excludes do código gerado pelo Room. O
ajuste do filtro é do plano 04-05 e **não** foi antecipado aqui.

## Verificação

```
./gradlew testDebugUnitTest detekt lint   → BUILD SUCCESSFUL (295 testes JVM, 0 falhas)
bash scripts/verify-invariants.sh         → == todos os invariantes OK ==
./gradlew koverVerify                     → BUILD SUCCESSFUL (87,6884% vs gate 80)
detekt.xml sem <error>, lint limpo
```

Greps de privacidade, todos zerados em `app/src/main`: coluna normalizada do provider, `getString`
e `moveToFirst` na fonte, looper principal no observador, identificadores de nome/foto/chave de
contato, e `ContactsContract` fora de `data/contacts/`.

## Self-Check: PASSED

- FOUND: app/src/main/java/org/sentinela/app/data/contacts/ContactNumberSource.kt
- FOUND: app/src/main/java/org/sentinela/app/data/contacts/ContactsContractLookupSource.kt
- FOUND: app/src/main/java/org/sentinela/app/data/contacts/ContactKeyCache.kt
- FOUND: app/src/main/java/org/sentinela/app/data/contacts/DefaultContactLookupRepository.kt
- FOUND: app/src/test/java/org/sentinela/app/data/contacts/FakeContactNumberSource.kt
- FOUND: app/src/test/java/org/sentinela/app/data/contacts/ContactKeyCacheTest.kt
- FOUND: app/src/test/java/org/sentinela/app/data/contacts/DefaultContactLookupRepositoryTest.kt
- FOUND: commits fb59644, 4740746, 9aecb30

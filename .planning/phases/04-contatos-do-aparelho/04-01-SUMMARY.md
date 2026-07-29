---
phase: 04-contatos-do-aparelho
plan: 01
subsystem: permissoes-e-fixture-de-contatos
tags: [permissoes, manifest, invariantes, testes-instrumentados]
requires: []
provides:
  - "READ_CONTACTS no manifest principal (Fase 4)"
  - "verify-invariants.sh com allowlist atualizada e gravacao de agenda barrada para sempre"
  - "ContactsTestFixture para inserir/apagar contatos em teste instrumentado"
affects:
  - "todas as tasks instrumentadas da Fase 4"
tech-stack:
  added: []
  patterns:
    - "uiAutomation.adoptShellPermissionIdentity para preparar dados de agenda em teste"
    - "applyBatch em lotes de 300 operacoes"
key-files:
  created:
    - app/src/androidTest/java/org/sentinela/app/data/contacts/ContactsTestFixture.kt
    - app/src/androidTest/java/org/sentinela/app/data/contacts/ContactsFixtureSmokeTest.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - scripts/verify-invariants.sh
decisions:
  - "READ_CONTACTS entra no manifest no MESMO commit das duas edicoes do verify-invariants.sh"
  - "Gravacao na agenda fica no FUTURE do script permanentemente; testes usam identidade de shell"
  - "Nenhum app/src/androidTest/AndroidManifest.xml e criado"
metrics:
  duration: ~15min
  tasks: 2
  files: 4
  completed: 2026-07-29
---

# Phase 04 Plan 01: Permissao de Contatos e Fixture Instrumentada Summary

`READ_CONTACTS` entrou no manifest principal junto com as duas edicoes obrigatorias de
`scripts/verify-invariants.sh`, e a agenda passou a ser preparavel em teste instrumentado por
identidade de shell — sem nenhuma permissao de gravacao declarada em manifest algum.

## O que foi feito

**Task 1 — manifest + invariantes** (commit `2f34288`)
- `app/src/main/AndroidManifest.xml`: `uses-permission` de `READ_CONTACTS` com comentario de
  privacidade. O comentario descreve a gravacao na agenda em prosa portuguesa, nunca pelo
  identificador — o `FUTURE` do script faz grep de texto puro sobre o manifest mesclado e um
  comentario derrubaria o proprio invariante.
- `ALLOWLIST` ganhou a terceira linha `android.permission.READ_CONTACTS` (formato multiline
  preservado, porque a comparacao e `grep -qx` de linha inteira).
- `FUTURE` perdeu `READ_CONTACTS|` e ganhou a entrada de gravacao de agenda ao final:
  `FUTURE="READ_CALL_LOG|READ_PHONE_STATE|READ_SMS|CALL_PHONE|BIND_INCALL_SERVICE|SYSTEM_ALERT_WINDOW|WRITE_CONTACTS"`.
- `docs/PERMISSOES.md` lido (linha 14 atribui `READ_CONTACTS` a Fase 4) e mantido byte-identico
  (`git diff --exit-code docs/PERMISSOES.md` limpo).

**Task 2 — fixture instrumentada** (commit `1b972e8`)
- `ContactsTestFixture`: `adoptShell()`/`dropShell()`, `insert()` com `applyBatch` de 3 ops
  (`RawContacts` + `StructuredName` + `Phone`, com `withValueBackReference`), `insertMany()` em
  lotes de 300 ops e `wipe()` por `delete` em `RawContacts.CONTENT_URI` (cascata).
- `ContactsFixtureSmokeTest`: `@Before` adota shell + limpa, `@After` limpa + solta. Dois testes:
  3 contatos inseridos aparecem numa consulta a `Phone.CONTENT_URI` projetando apenas
  `Phone.NUMBER`; apos `wipe`, a mesma consulta devolve 0.

## Prova de vermelho (obrigatoria)

Com a linha `android.permission.READ_CONTACTS` removida temporariamente da `ALLOWLIST`:

```
FAIL: permissao fora da allowlist: android.permission.READ_CONTACTS — ver docs/PERMISSOES.md
== 1 invariante(s) violado(s) ==
EXITCODE=1
```

(Uma falha e nao duas porque a edicao do `FUTURE` ja estava aplicada — foi exatamente o
invariante alvo que caiu.) Apos restaurar a linha:

```
ok:   permissao autorizada: android.permission.READ_CONTACTS
ok:   nenhuma permissao de fase futura antecipada
== todos os invariantes OK ==
```

## Verificacao

- `./gradlew assembleDebug lint detekt` verde; `bash scripts/verify-invariants.sh` exit 0 com
  `== todos os invariantes OK ==` (detekt e lint sem issues).
- `bash scripts/run-instrumented-tests.sh --tests "*ContactsFixtureSmokeTest"` — BUILD SUCCESSFUL,
  relatorio com `tests="2" failures="0" errors="0" skipped="0"`.
- `ls app/src/androidTest/AndroidManifest.xml` -> arquivo nao existe.
- `grep -rlE 'WRITE_CONTACTS' app/src/main app/src/test | wc -l` -> `0`.

## Deviations from Plan

None - plano executado exatamente como escrito.

## Self-Check: PASSED

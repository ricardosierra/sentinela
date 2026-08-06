---
phase: 03-dados-locais
plan: 02
subsystem: privacidade/backup
tags: [backup, privacidade, teste-jvm, PRV-03, HST-06]
requires: []
provides:
  - "Regras de backup com path explicito nos dois formatos (API 31+ e API 29-30)"
  - "BackupRulesTest — teste JVM que le os XMLs por DOM e fica vermelho se um exclude sumir"
affects:
  - "Criterio 5 do ROADMAP da Fase 3 (backup comprovadamente exclui os dados)"
tech-stack:
  added: []
  patterns:
    - "Afirmacao de privacidade so vale com teste que falha quando a propriedade quebra"
    - "Parse de XML de configuracao por javax.xml.parsers.DocumentBuilderFactory (JDK puro), nunca regex"
key-files:
  created:
    - app/src/test/java/org/sentinela/app/privacy/BackupRulesTest.kt
  modified:
    - app/src/main/res/xml/data_extraction_rules.xml
    - app/src/main/res/xml/full_backup_content.xml
decisions:
  - "path=\".\" explicito em todos os <exclude> — a doc oficial trata domain e path como obrigatorios"
  - "datastore sem barra final nos dois arquivos, para o assert do teste usar igualdade exata de par (domain, path)"
  - "-wal e -shm nao sao listados: exclusao de diretorio e recursiva e nomes transientes nao devem virar contrato"
  - "Zero <include> nos arquivos de backup — um include reintroduziria o que foi excluido"
metrics:
  duration: ~7 min
  tasks: 2
  files: 3
  completed: 2026-07-29
---

# Phase 03 Plan 02: Regras de Backup Verificadas por Teste Summary

Backup em nuvem e device-transfer passam a excluir banco, sharedpref e o arquivo do DataStore com
`path` explicito nos dois formatos, e a propriedade fica travada por `BackupRulesTest` — 5 testes JVM
que leem os XMLs por DOM e ficam vermelhos se um `<exclude>` for removido.

## O que foi feito

### Task 1 — XMLs de backup corrigidos (`12c68be`)

- `data_extraction_rules.xml`: `path="."` acrescentado aos excludes de `database` e `sharedpref` em
  `<cloud-backup>` e `<device-transfer>` (antes vinham sem `path`, comportamento nao especificado);
  `datastore/` virou `datastore`.
- `full_backup_content.xml`: `datastore/` virou `datastore`; comentario atualizado para citar API 29-30
  e o minSdk 29.
- Manifest **nao** foi tocado: ja declarava `android:dataExtractionRules` e `android:fullBackupContent`.
- Nenhum `<include>` em nenhum dos dois arquivos.

### Task 2 — `BackupRulesTest` (`d98059f`)

`app/src/test/java/org/sentinela/app/privacy/BackupRulesTest.kt`, 5 testes, parse via
`DocumentBuilderFactory` (zero uso de `Regex`/`toRegex`), caminho relativo ao modulo `app/`:

1. `cloudBackupExcluiDadosSensiveis`
2. `deviceTransferExcluiDadosSensiveis`
3. `fullBackupLegadoExcluiDadosSensiveis`
4. `nenhumIncludeReintroduzDadoExcluido`
5. `manifestApontaParaAsDuasRegras` (assert do texto exato dos dois atributos, sem afrouxar para
   `contains("dataExtractionRules")`)

Conjunto exigido em todos os escopos: `(database, .)`, `(sharedpref, .)`, `(file, datastore)`.

## Prova de que o teste falha de verdade

Removendo temporariamente `<exclude domain="file" path="datastore" />` do `<cloud-backup>`:

```
> Task :app:testDebugUnitTest FAILED
BackupRulesTest > cloudBackupExcluiDadosSensiveis FAILED
    java.lang.AssertionError at BackupRulesTest.kt:41
BUILD FAILED in 2s
```

Linha restaurada, verde:

```
tests="5" skipped="0" failures="0" errors="0"
BUILD SUCCESSFUL
```

(`app/build/test-results/testDebugUnitTest/TEST-org.sentinela.app.privacy.BackupRulesTest.xml`)

## Verificacao

- `./gradlew assembleDebug testDebugUnitTest lint detekt` — BUILD SUCCESSFUL
- `bash scripts/verify-invariants.sh` — `== todos os invariantes OK ==`, incluindo
  "sem android.permission.INTERNET" e "nenhuma permissao fora da allowlist". **Nenhuma permissao nova.**

## Deviations from Plan

None - plan executed exactly as written.

## Notas para as proximas fases

- Se algum plano futuro adicionar `<include>` a esses XMLs, o teste 4 quebra de proposito: a decisao
  precisa ser revisada em privacidade, nao contornada.
- O caminho real do DataStore (`files/datastore/<nome>.preferences_pb`) esta coberto pela exclusao de
  `file`/`datastore`; o plano que criar o DataStore singleton nao precisa mexer no backup.

## Self-Check: PASSED

Arquivos criados/modificados presentes em disco; commits `12c68be` e `d98059f` presentes no historico.

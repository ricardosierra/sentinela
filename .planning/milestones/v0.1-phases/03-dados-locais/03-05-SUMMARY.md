---
phase: 03-dados-locais
plan: 05
subsystem: dados-locais
tags: [historico, retencao, room, privacidade, tdd]
requires:
  - "03-01 (scripts/run-instrumented-tests.sh + androidTest com Room)"
  - "03-03 (SentinelaDatabase v1, BlockedCallDao, BlockedCallEntity, Converters)"
provides:
  - "RetentionPolicy: enum puro nunca/7/30/90/manual com cutoff calculado e id persistido estavel"
  - "ScreeningSettings.historyEnabled (true) e ScreeningSettings.retentionPolicy (DAYS_30)"
  - "RoomBlockedCallRepository: gravacao condicional, poda apos cada escrita e pruneNow() para a abertura do app"
  - "RoomBlockedCallRepository.updateClassification (HST-05)"
  - "Mappers BlockedCallEntity.toDomain / BlockedCallEntry.toEntity com leitura tolerante"
  - "FakeBlockedCallDao reutilizavel pelos testes JVM das proximas fases"
  - "BlockedCallDaoTest instrumentado: 12 testes contra o SQLite real"
affects:
  - app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt
tech-stack:
  added: []
  patterns:
    - "Relogio injetado (clock: () -> Long) em toda regra dependente de tempo — retencao testada com now fixo"
    - "Poda acoplada a escrita em vez de agendador em segundo plano: tabela local pequena nao paga cold start de dependencia nova"
    - "Repositorio nao engole excecao do DAO: quem decide degradar e o Service da Fase 5"
    - "Comentario que NOMEIA o construto proibido dispara o proprio criterio de aceite — descrever, nunca nomear"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/settings/RetentionPolicy.kt
    - app/src/main/java/org/sentinela/app/data/local/RoomBlockedCallRepository.kt
    - app/src/test/java/org/sentinela/app/settings/RetentionPolicyTest.kt
    - app/src/test/java/org/sentinela/app/data/local/FakeBlockedCallDao.kt
    - app/src/test/java/org/sentinela/app/data/local/RoomBlockedCallRepositoryTest.kt
    - app/src/androidTest/java/org/sentinela/app/data/local/db/BlockedCallDaoTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt
decisions:
  - "Retencao persistida por id textual (never/7d/30d/90d/manual), nunca pela posicao da constante"
  - "NEVER_STORE e MANUAL dao ambos cutoff nulo; o que os distingue e shouldStore"
  - "Poda com corte ESTRITO (< cutoff): registro exatamente no limite sobrevive, travado por assert instrumentado"
  - "Janelas de retencao viraram constantes nomeadas (UMA_SEMANA/UM_MES/TRES_MESES) em vez de afrouxar MagicNumber no detekt.yml compartilhado"
metrics:
  duration: ~11 min
  tasks: 3
  files: 7
  completed: 2026-07-29
---

# Phase 3 Plan 05: Historico Local e Retencao Summary

O historico existe com a retencao como regra pura de 5 politicas travadas por teste, um
repositorio Room que decide gravar antes de tocar no banco e poda logo apos cada escrita
sem agendador em segundo plano, e um limite de corte estrito provado no SQLite real —
tudo com relogio injetado, para que "30 dias" signifique a mesma coisa amanha.

## O que foi feito

**Task 1 — retencao como regra pura** (`ff5a7e6`)
- `RetentionPolicy` com `id` textual estavel (`never`/`7d`/`30d`/`90d`/`manual`). O valor que
  vai ao disco e o `id`, nunca a posicao da constante: reordenar o enum reinterpretaria
  silenciosamente a configuracao ja gravada do usuario.
- `NEVER_STORE` e `MANUAL` retornam `cutoffUtcMillis == null` — sao casos diferentes com a
  mesma aritmetica, e quem os separa e `shouldStore`. Um nao grava; o outro grava e nunca poda.
- `fromId` e tolerante: `null` ou valor desconhecido cai em `DAYS_30` sem lancar, para que uma
  config escrita por versao mais nova do app nao derrube a antiga.
- `ScreeningSettings` ganhou `historyEnabled = true` e `retentionPolicy = DAYS_30` **no final**
  da data class — os 9 campos da Phase 2 nao foram removidos nem reordenados, entao nenhuma
  chamada nomeada existente quebrou.
- RED provado: `compileDebugUnitTestKotlin` falhou antes de `RetentionPolicy` existir.
- 13 testes JVM (plano pedia 8), incluindo o assert que trava a lista em 5 entradas e a lista
  literal dos 5 ids.

**Task 2 — repositorio Room do historico** (`5f0373b`)
- `RoomBlockedCallRepository.record` consulta `settings.snapshot()` **antes** de tocar no DAO:
  com historico desligado ou `NEVER_STORE`, nada e gravado e nada e podado.
- A poda roda logo apos a gravacao bem-sucedida, pelo cutoff da politica vigente; `pruneNow()`
  atende a abertura do app (a ligacao no `AppContainer` e do plano 03-07).
- Excecao do DAO **propaga** de proposito: se o disco falhar, o Service da Fase 5 precisa saber
  que o registro nao aconteceu. Um repositorio que engole erro mente para quem depende dele.
- Mappers com leitura tolerante (reason code / classificacao desconhecidos caem em
  `UNKNOWN_NUMBER` / `UNCLASSIFIED`), coerentes com os `Converters` da wave 2.
- 18 testes JVM (plano pedia 12) sobre `FakeBlockedCallDao` + fake de `SettingsRepository`, com
  `clock = { AGORA }` fixo. O fake conta chamadas de `pruneOlderThan`, o que permite afirmar a
  **ausencia** de poda no caso `MANUAL` — nao so a presenca dela nos outros.
- RED provado antes da implementacao.

**Task 3 — DAO contra o SQLite real** (`131554e`)
- `BlockedCallDaoTest`: 12 testes instrumentados (plano pedia 8) em banco em memoria.
- O teste central da poda insere em `cutoff - 1`, `cutoff` e `cutoff + 1`, e afirma que
  **exatamente 1** foi apagado e que o registro no limite sobreviveu. Trocar o `<` por `<=` na
  query apagaria dado que o usuario ainda tem direito de ver — agora isso quebra o build.
- Round-trip dos **9** `DecisionReason` pelo banco, e o `numberE164` completo volta intacto
  (HST-04, insumo da Fase 8 para "adicionar a whitelist"). Numero privado grava so a mascara,
  com `number_e164` nulo.
- `allowMainThreadQueries` ausente: o builder nao afrouxa a checagem de thread.

## Evidencia

Instrumentado, no emulador `Medium_Phone_API_35`:
```
Starting 12 tests on Medium_Phone_API_35(AVD) - 15
Medium_Phone_API_35(AVD) - 15 Tests 12/12 completed. (0 skipped) (0 failed)
BUILD SUCCESSFUL in 29s
```
`TEST-Medium_Phone_API_35(AVD) - 15-_app-.xml`:
`<testsuites tests="12" failures="0" errors="0" skipped="0">`.

JVM, com `--rerun-tasks` para nao aceitar cache como prova (**31 de 31 tasks executadas**):
```
RetentionPolicyTest:           tests="13" failures="0" errors="0"
RoomBlockedCallRepositoryTest: tests="18" failures="0" errors="0"
```
`./gradlew lint detekt` → `BUILD SUCCESSFUL`. `bash scripts/verify-invariants.sh` →
`== todos os invariantes OK ==`.

Criterios de aceite mecanicos, todos verdes: `import android` e posicao-de-constante ausentes de
`RetentionPolicy.kt`; agendador em segundo plano, chamada de logger e escrita em saida padrao
ausentes de `RoomBlockedCallRepository.kt`; `allowMainThreadQueries` ausente do teste
instrumentado.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Detekt reprovou as janelas de retencao como numeros magicos**
- **Found during:** Task 1
- **Issue:** `DAYS_7("7d", 7)` e os pares de 30 e 90 disparam `MagicNumber`. O `detekt.yml` da
  raiz e compartilhado e nao tem excecao para enums.
- **Fix:** os valores viraram constantes privadas nomeadas no proprio arquivo
  (`UMA_SEMANA`/`UM_MES`/`TRES_MESES`). Ficaram **fora** do enum porque uma entrada nao pode ler
  o companion antes de existir. Afrouxar a regra no `detekt.yml` teria pago uma divida de
  qualidade global para resolver um caso local.
- **Files modified:** `app/src/main/java/org/sentinela/app/settings/RetentionPolicy.kt`
- **Commit:** `ff5a7e6`

**2. [Rule 3 - Blocking] Os comentarios do repositorio disparavam os proprios criterios de aceite**
- **Found during:** Task 2
- **Issue:** o KDoc que eu escrevi explicava a decisao **nomeando** os construtos proibidos
  ("Sem WorkManager", "nao existe `Log.` nem `println` nesta classe"). Os greps do plano exigem
  contagem 0 desses padroes no arquivo — e eles nao distinguem comentario de codigo, o que e
  proposital desde a wave 2 (linha comentada vira linha ativa com um teclado).
- **Fix:** os comentarios passaram a **descrever** ("nenhum agendador em segundo plano", "esta
  classe nao chama o logger da plataforma nem escreve na saida padrao"), preservando o sentido
  integral. Terceira ocorrencia desta mesma classe na fase — ja virou padrao registrado.
- **Files modified:** `app/src/main/java/org/sentinela/app/data/local/RoomBlockedCallRepository.kt`
- **Commit:** `5f0373b`

**3. [Rule 3 - Blocking] `LongParameterList` no helper do teste e opt-in do dispatcher**
- **Found during:** Task 2
- **Issue:** o helper `entry(...)` chegou a 7 parametros (limite 6) e `UnconfinedTestDispatcher`
  exige opt-in explicito.
- **Fix:** o parametro `classification` foi removido — nenhum teste o usava, porque **toda**
  gravacao nasce `UNCLASSIFIED` (classificar e ato do usuario, nunca do Service), e o helper
  agora documenta isso. `@OptIn(ExperimentalCoroutinesApi::class)` na classe de teste.
- **Files modified:** `app/src/test/java/org/sentinela/app/data/local/RoomBlockedCallRepositoryTest.kt`
- **Commit:** `5f0373b`

Nenhum checkpoint humano foi emitido. Nenhuma permissao nova, nenhuma dependencia nova, nenhum
`fallbackToDestructiveMigration`, nenhuma chamada de rede, nenhum numero completo em log.

## Fora de escopo (nao corrigido, de proposito)

Na verificacao final, `SchemaExportTest > whitelist tem indice unico na chave` falhou. **Nao e
defeito deste plano:** o `1.json` esta modificado na arvore de trabalho e
`WhitelistPerformanceTest.kt` esta sem rastreio — ambos sao trabalho **em voo** do plano 03-04,
que roda em paralelo e e o dono da prova de indice da whitelist. Nenhum arquivo desse plano foi
tocado e nenhum foi incluido nos meus commits (cada commit staged arquivo a arquivo). O resto da
suite esta verde; a falha se resolve quando o 03-04 fechar.

**Confirmado depois:** o 03-04 commitou (`49bec35`, `7791f7f`) e a arvore ficou limpa.
`./gradlew testDebugUnitTest --rerun-tasks` → `BUILD SUCCESSFUL`, **31 de 31 tasks executadas,
218 testes, 0 falhas**, com `lint`, `detekt` e os invariantes verdes. Nenhuma correcao minha foi
necessaria — o diagnostico de "trabalho em voo do plano vizinho" estava certo.

## Para as proximas fases

- `RoomBlockedCallRepository` ainda **nao** esta ligado no `AppContainer` — a composicao e a
  chamada de `pruneNow()` na abertura do app sao do plano 03-07, junto da ampliacao do filtro
  do Kover para `data.*` e `settings.*`.
- `historyEnabled` e `retentionPolicy` existem no modelo mas ainda nao sao persistidos pelo
  `SettingsRepository` real nem expostos em tela: DataStore no 03-07 se necessario, UI na Fase 8.
- Os rotulos das 5 politicas em `strings.xml` (pt-BR) sao da Fase 8; nenhuma string de UI foi
  criada aqui.
- `FakeBlockedCallDao` esta pronto para o teste do Service na Fase 5.

## Self-Check: PASSED

- 6 arquivos criados — todos FOUND
- `app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt` — FOUND, modificado
- Commits `ff5a7e6`, `5f0373b`, `131554e` — FOUND em `git log`
- 31 testes JVM + 12 instrumentados, 0 falhas; `lint` e `detekt` verdes; invariantes OK

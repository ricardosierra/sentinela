---
phase: 01-fundacao-compilavel
plan: 03
subsystem: evidencia-e-fechamento
tags: [evidencia, build, permissoes, backlog, changelog]
requires:
  - phase: 01-01
    provides: politica de lint com abortOnError e lint 0 issues
  - phase: 01-02
    provides: scripts/verify-invariants.sh, ThemeTokensTest, matriz OriginPolicy
provides:
  - "01-EVIDENCE.md — prova auditavel dos 5 criterios da Phase 1 a partir de build pos-clean SEM build cache"
  - "Reconciliacao escrita do conflito POST_NOTIFICATIONS a favor de docs/PERMISSOES.md"
  - "Cenarios fisicos 31-34 registrados como pendencia da Phase 9"
  - "docs/backlog/manutencao-toolchain.md — 4 itens de manutencao adiados com gatilho de reavaliacao"
affects:
  - "Phase 9 — herda os cenarios 31-34 e a reavaliacao de UnusedResources, gradlew.bat e AGP 9.3.1"
tech-stack:
  added: []
  patterns:
    - "Evidencia de build so vale sem build cache: FROM-CACHE tem o mesmo defeito probatorio que UP-TO-DATE"
key-files:
  created:
    - .planning/phases/01-fundacao-compilavel/01-EVIDENCE.md
    - docs/backlog/manutencao-toolchain.md
  modified:
    - .planning/phases/01-fundacao-compilavel/01-CONTEXT.md
    - docs/TESTE-FISICO-SAMSUNG.md
    - docs/INDEX.md
    - CHANGELOG.md
decisions:
  - "Evidencia coletada com --no-build-cache alem do clean: o clean apaga build/ mas nao invalida o cache, e a primeira coleta veio com compileDebugKotlin/testDebugUnitTest/detekt FROM-CACHE"
  - "POST_NOTIFICATIONS permanece declarada no manifest — docs/PERMISSOES.md e a fonte canonica e remover quebraria a Phase 5"
  - "A checagem de INTERNET e sobre uses-permission, nao sobre a palavra solta: o comentario de privacidade do manifest contem a palavra INTERNET e sobrevive ao merge"
metrics:
  tasks: 2
  tests_total: 28
  duration: ~18min
  completed: 2026-07-29
requirements-completed: [PRV-01, QLT-02, UIX-08, UIX-12]
---

# Phase 01 Plano 03: Evidencia e Fechamento Summary

**Fechou a Phase 1 com evidencia que realmente prova o que afirma — um build pos-`clean` e sem build cache (56 de 57 tasks executadas, 28 testes, lint 0, detekt 0) — mais a reconciliacao por escrito do conflito `POST_NOTIFICATIONS` e o registro de tudo que foi diferido para a Phase 9.**

## Performance

- **Duration:** ~18 min
- **Tasks:** 2
- **Files created:** 2 | **modified:** 4
- **Nenhum arquivo de `app/` tocado** (restricao central do plano, verificada a cada task)

## Accomplishments

### Task 1 — Reconciliacao e pendencias (`cdb23e6`)

- `01-CONTEXT.md` ganhou o paragrafo de reconciliacao citando `docs/PERMISSOES.md` como fonte canonica: a **declaracao** de `POST_NOTIFICATIONS` e autorizada na Fase 1, o **pedido em runtime** fica na Fase 5 (NTF-02). A permissao permanece no manifest — `git diff app/src/main/AndroidManifest.xml` vazio. O verifier nao deve mais marcar falso gap.
- `docs/TESTE-FISICO-SAMSUNG.md` recebeu a secao "Pendencias herdadas da Phase 1 (fundacao)" com os cenarios **31-34** (instalacao do APK, tema dark, Dynamic Color sob One UI, light mode forcado), no mesmo formato de tabela do resto do roteiro e continuando a numeracao a partir de 30.
- `docs/backlog/manutencao-toolchain.md` criado com 4 itens, cada um com o que se sabe, por que foi adiado e **gatilho de reavaliacao**: `gradlew.bat` ausente, AGP 9.3.1 sem release notes, `disable "UnusedResources"` a reativar na Phase 9, e o aviso de deprecation do Gradle 10 (origem: `ReportingExtension.file(String)` do detekt 1.23.8, nao deste repo).
- `docs/INDEX.md` indexa o arquivo novo (exigencia do `CLAUDE.md`) e `CHANGELOG.md` ganhou o bloco "Phase 1 — Fundação compilável" em `## [Unreleased]` > `### 🔧 Técnico`, todos os itens em checkbox `- [x]`, formato Release Notes preservado (0 ocorrencias de Keep a Changelog).

### Task 2 — Evidencia arquivada (`0faa66f`)

`01-EVIDENCE.md` traz o comando verbatim, o log com `BUILD SUCCESSFUL in 13s` e `57 actionable tasks: 56 executed, 1 up-to-date`, as metricas, a tabela dos 5 criterios do ROADMAP mapeados a comando + resultado + veredito, a saida integral do `verify-invariants.sh` (exit 0) e a lista dos itens diferidos.

Numeros reais medidos: **28 testes / 0 falhas** (24 `CallDecisionEngineTest` + 4 `ThemeTokensTest`), **lint 0 issues**, **detekt 0 issues**, APK debug **33 807 562 bytes**. Permissoes do manifest mergeado: apenas `POST_NOTIFICATIONS` e `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `clean` sozinho nao produzia evidencia valida — o build cache serviu as tasks probatorias**

- **Found during:** Task 2
- **Issue:** O plano tratava `clean` como suficiente contra o Pitfall 3 (`UP-TO-DATE` nao prova nada). A primeira coleta seguiu o plano a letra e devolveu `57 actionable tasks: 34 executed, 23 from cache`, com `compileDebugKotlin FROM-CACHE`, `testDebugUnitTest FROM-CACHE` e `detekt FROM-CACHE`. `FROM-CACHE` tem exatamente o mesmo defeito probatorio que `UP-TO-DATE`: o Gradle **restaurou saidas** do cache local em vez de compilar o Kotlin, executar os testes e analisar o codigo. O `clean` apaga `build/`, mas nao invalida o build cache — a evidencia teria sido um verde emprestado de um run anterior.
- **Fix:** Coleta refeita com `./gradlew clean` seguido de `assembleDebug testDebugUnitTest lint detekt --no-build-cache`. Resultado: `56 executed, 1 up-to-date`, **zero** tasks `FROM-CACHE`, com as tasks probatorias aparecendo sem sufixo. A metodologia e a razao dela estao documentadas em secao propria do `01-EVIDENCE.md`, nao escondidas.
- **Nota de escopo:** o plano proibia `--rerun-tasks` *no lugar do* `clean`. `--no-build-cache` e aditivo ao `clean`, nao substituto, entao a proibicao foi respeitada.
- **Files modified:** `.planning/phases/01-fundacao-compilavel/01-EVIDENCE.md`
- **Commit:** `0faa66f`

### Correcoes de numeros herdados do plano

- O plano projetava `tests >= 27`; o valor real e **28** (conforme ja apontado pelo 01-02). O EVIDENCE registra 28.
- O plano citava o baseline `56 executed, 1 up-to-date`; a coleta final bateu exatamente esse numero — coincidencia util, mas o valor foi medido, nao copiado.
- O `must_haves` do 01-01 citava `res/mipmap-anydpi/`; o caminho real entregue e `res/mipmap/`. Nenhum artefato deste plano referencia o caminho antigo.

**Total deviations:** 1 auto-fixed (1 bug de metodologia de evidencia). Nenhum scope creep; nenhum arquivo de codigo tocado.

## Decisions Made

- **Evidencia exige `--no-build-cache`**, nao so `clean`. Fica como padrao para as proximas fases que precisarem provar um build.
- **`POST_NOTIFICATIONS` mantida** no manifest, conflito resolvido a favor da fonte canonica.
- **Checagem de `INTERNET` e sobre `uses-permission`**, nunca sobre a palavra solta: o comentario de privacidade do manifest (`<!-- Privacidade: NENHUMA permissão de INTERNET... -->`) sobrevive ao merge e faz um `grep -c INTERNET` ingenuo retornar 1. O `verify-invariants.sh` ja acerta isso; o EVIDENCE explicita a armadilha para quem auditar depois.

## Issues Encountered

- `.planning/config.json` aparecia **staged** no indice antes deste plano comecar (alteracao do orquestrador, `_auto_chain_active`). Os dois commits usaram pathspec explicito (`git commit -- <arquivos>`) para nao arrastar essa mudanca de terceiro para dentro dos commits do plano. O arquivo segue staged e intocado.

## User Setup Required

None.

## Next Phase Readiness

- **Phase 1 fechada** nos 5 criterios, com o criterio 2 marcado ✅ parcial: a renderizacao real do tema depende de aparelho e virou os cenarios 31-34 da Phase 9. Isso e **diferimento registrado, nao gap**.
- **QLT-02 agora fecha de verdade:** `abortOnError = true` (01-01) + build pos-clean sem cache com lint/detekt em 0 (este plano). O Bloco 4 do `verify-invariants.sh`, que o 01-02 deixou informativo, esta confirmado por execucao real.
- **Phase 9 herda:** cenarios 31-34, reativacao de `UnusedResources`, `gradlew.bat` e a decisao sobre AGP 9.3.1.
- **Phase 4 e Phase 6** continuam com a obrigacao registrada pelo 01-02: atualizar `docs/PERMISSOES.md` e a allowlist do script no mesmo commit ao introduzir permissao nova.

## Self-Check: PASSED

- FOUND: `.planning/phases/01-fundacao-compilavel/01-EVIDENCE.md`
- FOUND: `docs/backlog/manutencao-toolchain.md`
- FOUND: commit `cdb23e6` (docs — reconciliacao e pendencias)
- FOUND: commit `0faa66f` (docs — evidencia)
- VERIFICADO: `git status --porcelain app/` vazio — nenhum arquivo de codigo tocado
- VERIFICADO: `grep -c "android.permission.POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml` = 1
- VERIFICADO: `grep -cE "^\| 3[1-4] \|" docs/TESTE-FISICO-SAMSUNG.md` = 4
- VERIFICADO: `grep -c "Keep a Changelog\|## Added\|## Changed" CHANGELOG.md` = 0

---
*Phase: 01-fundacao-compilavel*
*Completed: 2026-07-29*

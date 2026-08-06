---
phase: 5
slug: triagem-telecom-modo-filtro
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
updated: 2026-07-29
---

# Phase 5 — Validation Strategy

> Contrato de validacao da **fase central do produto**. Diferenca em relacao as Fases 1-4: o risco
> aqui nao e performance nem privacidade — a pesquisa mediu o caminho de decisao em 23,3 ms com
> ~4x de folga, e o dado pessoal ja foi travado na Fase 4. O risco e **regressao silenciosa**:
> um caminho que nao responde, ou que responde duas vezes, nao aparece em teste de caminho feliz.
> A pesquisa mediu que responder duas vezes **nao lanca e nao derruba o processo** — emite dois
> IPCs em silencio. A guarda do coordenador e a UNICA protecao que existe.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (JVM puro)** | JUnit 4 `4.13.2` + MockK `1.14.11` + `kotlinx-coroutines-test` + Turbine, sobre AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| **Framework (JVM + plataforma real)** | **Robolectric `4.16.1` com `@Config(sdk = [35])`** — obrigatorio para montar respostas de triagem e notificacoes. `sdk = [36]` e **impossivel**: exige Java 21 e o projeto esta travado em JDK 17 (medido). O blocker registrado no STATE esta errado e e corrigido no plano 05-07 |
| **Framework (instrumentado)** | `AndroidJUnitRunner` + `androidx.test:rules` (`ServiceTestRule`), AVD `Medium_Phone_API_35` |
| **Config file** | `app/build.gradle.kts` — `testOptions.unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true }` e o bloco `kover` |
| **Cobertura** | Kover `0.9.9`, gate `koverVerify minBound(80)` (atual 96,68%). `telecom.ScreeningCoordinator` e **puro** e entra no denominador sem exclude. Exclude novo, se necessario, so por **nome de classe** e so no plano **05-07** |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` — `connectedDebugAndroidTest` **nao aceita** `--tests`; o script traduz para `tests_regex` |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Estimated runtime** | quick ~25-35 s (as classes Robolectric custam ~2 s cada) · instrumentado ~40 s incremental + 2-4 min de boot a frio · full ~6-9 min |
| **Relatorios de evidencia** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/` · lint/detekt: `app/build/reports/` · androidTest: `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` · **percentis:** `.../connected/debug/Medium_Phone_API_35(AVD) - 15/logcat-<classe>-<metodo>.txt` |

**Dependencia nova: nenhuma.** Robolectric, MockK, coroutines-test e `ServiceTestRule` ja estao
declarados. A pesquisa confirmou que a fase inteira roda com o catalogo atual. Um plano que
acrescente dependencia esta errado.

**Permissao nova: nenhuma.** `POST_NOTIFICATIONS` esta **declarada** desde a Fase 1; esta fase
acrescenta somente o pedido em runtime, e so no momento do opt-in.

**Armadilhas herdadas e ainda validas:**
- `grep -c` sai com codigo 1 quando o resultado e zero. `set -e` continua **proibido** em
  `scripts/verify-invariants.sh` e **nunca** usar `|| echo 0`. Padrao: `[ "$(grep -c ...)" -eq 0 ]`.
- Teste que le arquivo de disco sem input declarado vai `UP-TO-DATE` e reporta verde falso.
- Evidencia so vale com `clean` **e** `--no-build-cache`: `FROM-CACHE` tem o mesmo defeito
  probatorio que `UP-TO-DATE`.
- Nome do XML do androidTest tem parenteses e espacos — sempre glob `TEST-*.xml`.
- **Auto-sabotagem de invariante:** o comentario ditado por um plano casando com o grep de
  contagem-zero do mesmo plano. Todos os KDocs desta fase descrevem proibicoes em prosa
  portuguesa, sem escrever os identificadores vigiados.
- Segundo container no mesmo processo derruba com duas instancias do armazenamento de
  preferencias sobre o mesmo arquivo (reproduzido). Todo teste usa o container da aplicacao ou
  fakes — nunca constroi outro real.

**Armadilha NOVA desta fase:** montar uma resposta de triagem em teste JVM **sem** Robolectric.
Com `isReturnDefaultValues = true` cada metodo do construtor devolve `null` e a cadeia explode em
NPE; e a validacao real do construtor (que lanca em combinacao contraditoria) so aparece sob
Robolectric. Todo teste de traducao precisa de `@RunWith(RobolectricTestRunner::class)`.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest` (< 40 s). Tasks que tocam so codigo
  instrumentado acrescentam `bash scripts/run-instrumented-tests.sh --tests "*Padrao"` com o
  emulador ja de pe.
- **Apos cada wave:** `./gradlew testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh`.
  Nas waves 1-4 usar `./gradlew koverLog` e conferir o percentual manualmente; `koverVerify` volta
  a ser cobrado no plano **05-07**.
- **Phase gate (antes de `/gsd:verify-work`):** suite JVM **e** instrumentada verdes
  **pos-`clean`**, com `--no-build-cache` e `N actionable tasks: M executed` com **M > 0**.
  Arquivado em `05-EVIDENCE.md`.
- **Nenhum comando usa watch mode.** Emulador sobe uma vez por sessao.
- **Prova de vermelho obrigatoria:** cada guarda-corpo desta fase e quebrado de proposito, visto
  falhar e restaurado. Sao 14 provas, tabuladas mais abaixo.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-01 | 01 | 1 | SCR-12, QLT-01 | unit (matriz parametrizada) | `./gradlew testDebugUnitTest --tests "*DecisionMatrixTest" --tests "*CallDecisionEngineTest" --tests "*DecisionReasonTest"` — +≥ 8 casos | ✅ estender | ⬜ pending |
| 5-01-02 | 01 | 1 | SCR-12 | unit (DAO falso + relogio fixo) | `./gradlew testDebugUnitTest --tests "*RoomBlockedCallRepositoryTest" --tests "*SchemaExportTest"` — +≥ 7 `@Test`, schema v1 intacto | ✅ estender | ⬜ pending |
| 5-01-03 | 01 | 1 | QLT-01 (contador de aberturas) | unit + build | `./gradlew testDebugUnitTest --tests "*AppOpenCounterTest" && ./gradlew assembleDebug` | ✅ estender | ⬜ pending |
| 5-02-01 | 02 | 1 | QLT-01 | **Robolectric (harness com captura de respostas)** | `./gradlew testDebugUnitTest --tests "*ScreeningTestHarness*"` | ❌ **Wave 0** — bloqueia 05-05 | ⬜ pending |
| 5-02-02 | 02 | 1 | SCR-04, SCR-09 | Robolectric (`Uri`) + MockK | `./gradlew testDebugUnitTest --tests "*ScreenedCallFactoryTest"` — ≥ 9 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-02-03 | 02 | 1 | SCR-03, SCR-06, SCR-08 | Robolectric `sdk=[35]` **e** `[29]` | `./gradlew testDebugUnitTest --tests "*CallResponseFactoryTest"` — ≥ 8 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-03-01 | 03 | 2 | SCR-05, SCR-09, SCR-12 | unit puro (`runTest` + costura) | `./gradlew testDebugUnitTest --tests "*ScreeningCoordinatorTest"` — ≥ 8 `@Test`, inclui o caminho de contato | ❌ criado pela task | ⬜ pending |
| 5-03-02 | 03 | 2 | SCR-05, SCR-10 | unit (injecao de excecao em CADA ponto) | `./gradlew testDebugUnitTest --tests "*ScreeningCoordinatorFailureTest"` — ≥ 10 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-03-03 | 03 | 2 | NTF-06 | unit (lista ordenada de eventos, **sem cronometro**) | `./gradlew testDebugUnitTest --tests "*ScreeningCoordinatorOrderTest"` — ≥ 5 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-04-01 | 04 | 2 | NTF-01, NTF-02 | unit (funcao pura de 4 estados + DataStore) | `./gradlew testDebugUnitTest --tests "*RuntimePermissionAskTest" --tests "*ContactsPermissionStateTest" --tests "*DataStoreSettingsRepositoryTest"` — ≥ 8 + ≥ 4 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-04-02 | 04 | 2 | NTF-03, NTF-04, NTF-05 | Robolectric (gerenciador de notificacoes sombreado) | `./gradlew testDebugUnitTest --tests "*BlockedCallNotifierTest"` — ≥ 8 `@Test`, inclui varredura de vazamento | ❌ criado pela task | ⬜ pending |
| 5-04-03 | 04 | 2 | NTF-01, NTF-02 | build + lint (strings pt-BR) | `./gradlew assembleDebug lint detekt && bash scripts/verify-invariants.sh` | ✅ estender | ⬜ pending |
| 5-05-01 | 05 | 3 | NTF-01, NTF-06 | build + greps de fiacao preguicosa | `./gradlew assembleDebug testDebugUnitTest detekt lint && bash scripts/verify-invariants.sh` | ✅ estender | ⬜ pending |
| 5-05-02 | 05 | 3 | SCR-03, SCR-05, SCR-08, SCR-09, SCR-12 | **Robolectric (harness sobre o Service real)** | `./gradlew testDebugUnitTest --tests "*ScreeningServiceTest"` — ≥ 7 `@Test`, inclui saida com zero respostas | ❌ criado pela task | ⬜ pending |
| 5-05-03 | 05 | 3 | SCR-01, SCR-02 | Robolectric (gerenciador de papeis sombreado) | `./gradlew testDebugUnitTest --tests "*ScreeningRoleManagerTest"` — ≥ 5 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-06-01 | 06 | 4 | QLT-06 | instrumentado (`ServiceTestRule`) | `bash scripts/run-instrumented-tests.sh --tests "*ScreeningServiceBindTest"` — ≥ 3 `@Test` | ❌ criado pela task | ⬜ pending |
| 5-06-02 | 06 | 4 | SCR-11 | instrumentado (percentis, **assert na mediana**) | `bash scripts/run-instrumented-tests.sh --tests "*DecisionPerformanceTest"` — p50 < 50 ms; p95/max reportados | ❌ criado pela task | ⬜ pending |
| 5-06-03 | 06 | 4 | SCR-10 | script (Bloco 7) | `bash scripts/verify-invariants.sh` — 4 checagens, 4 vermelhos demonstrados | ✅ estender | ⬜ pending |
| 5-07-01 | 07 | 5 | SCR-04, SCR-06 | greps de honestidade + build | `./gradlew assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` | ✅ estender docs + KDocs | ⬜ pending |
| 5-07-02 | 07 | 5 | QLT-01 | grep de completude dos cenarios | `[ "$(grep -cE '^\| (4[0-9]\|5[01]) \|' docs/TESTE-FISICO-SAMSUNG.md)" -eq 12 ]` | ✅ estender | ⬜ pending |
| 5-07-03 | 07 | 5 | QLT-06 | gate de cobertura + evidencia pos-`clean` | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` | ❌ `05-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| **SCR-01** | 5-05-03 (pedido do papel com intencao condicionada a disponibilidade) |
| **SCR-02** | 5-05-03 (consulta pontual de posse do papel; nao existe observador para aplicativo de terceiros — registrado em KDoc) |
| **SCR-03** | 5-02-03 (recusa + supressao do aviso nativo na traducao), 5-05-02 (resposta real capturada no adaptador) |
| **SCR-04** | 5-02-02 (handle nulo vira numero privado sem derrubar o Service), 5-07-01 (documentado como parcial: so no modo discador — a plataforma nao entrega numero oculto no modo filtro) |
| **SCR-05** | 5-03-01 (guarda por comparacao-e-troca, local a cada chamada), 5-03-02 (excecao em cada ponto), 5-05-02 (contagem no adaptador real) |
| **SCR-06** | 5-02-03 (recusa e encaminhamento produzem a mesma resposta na API — a diferenca e da operadora), 5-07-01 (documentacao nao promete caixa postal) |
| **SCR-08** | 5-02-03, 5-05-02 (supressao do aviso nativo de perdida) |
| **SCR-09** | 5-02-02 (direcao), 5-03-01 (retorno antecipado sem emitir), 5-05-02 (zero respostas capturadas) |
| **SCR-10** | 5-03-02 (matriz de 10 modos de falha), 5-06-03 (Bloco 7 impede regra migrar do motor) |
| **SCR-11** | 5-06-02 (mediana no CI; cauda diferida ao cenario 47 da Phase 9) |
| **SCR-12** | 5-01-01 (regra no motor, janela como constante nomeada, habilitada por padrao), 5-01-02 (consulta de bloqueio recente no historico ja existente), 5-03-01 (integracao com prazo e degradacao para "nao houve") |
| **NTF-01** | 5-04-01 (padrao desligado conferido), 5-04-03, 5-05-01 |
| **NTF-02** | 5-04-01 (pedido so no opt-in; flag gravado ao disparar, nao no retorno) |
| **NTF-03** | 5-04-02 (canal de importancia baixa; sem som, vibracao ou tela cheia) |
| **NTF-04** | 5-04-02 (varredura de TODOS os campos do objeto de notificacao pela sequencia completa de digitos — a garantia real e o conteudo, nao a visibilidade escolhida) |
| **NTF-05** | 5-04-02 (intencao pendente imutavel com o identificador do registro) |
| **NTF-06** | 5-03-03 (ordem provada por indice em lista de eventos), 5-05-02 |
| **QLT-01** | 5-01-01, 5-01-02, 5-01-03, 5-02-01/02/03, 5-03-01/02/03, 5-07-02 |
| **QLT-06** | 5-06-01 (vinculo real ao servico), 5-07-03 (suite instrumentada verde pos-`clean`) |

**SCR-07 nao gera task de implementacao:** e **WON'T FIX**, inatingivel por decisao do proprio
Android (o calculo de registro no historico so isenta aplicativos de operadora, e o Sentinela e do
tipo escolhido pelo usuario; virar discador padrao na Fase 6 nao destrava). O trabalho que ele
ainda exige e de **honestidade** e esta em 5-07-01: corrigir `docs/LIMITACOES.md`, os rotulos e os
KDocs para que nada afirme ausencia de registro. Planejar implementacao para SCR-07 seria erro.

Nenhum requisito da fase fica sem task. Nenhuma task fica sem `<automated>`.

---

## Wave 0 Requirements

Infraestrutura que **bloqueia** as tasks seguintes, concentrada na wave 1 (planos 05-01 e 05-02,
que nao compartilham arquivo e rodam em paralelo):

- [ ] `domain/RepeatedCall.kt` + a extensao do motor (05-01) — o coordenador do plano 05-03
      chama a assinatura nova; sem ela, metade da wave 2 nao compila
- [ ] `BlockedCallRepository.hasRecentBlock` (05-01) — insumo do bypass; **sem alterar o schema**
- [ ] `app/src/test/.../telecom/ScreeningTestHarness.kt` (05-02) — hospedagem do Service real em
      JVM com captura de cada resposta emitida. **Receita ja medida funcionando na pesquisa.**
      Bloqueia o plano 05-05 inteiro
- [ ] `app/src/test/.../telecom/FakeCallDetails.kt` (05-02) — objeto de chamada por MockK
- [ ] `ScreenedCallFactory` e `CallResponseFactory` (05-02) — as duas pontas que o coordenador e
      o Service consomem
- [ ] Instalacao de framework: **nenhuma**

**Nao e Wave 0, deliberadamente:**

- O **Bloco 7** de `scripts/verify-invariants.sh` fica no plano **05-06**: ele aponta para
  arquivos de `telecom/` que so ganham conteudo nos planos 05-03 e 05-05. Liga-lo antes deixaria
  o script vermelho sem defeito real — mesmo padrao das Fases 3 e 4.
- O `koverVerify` e qualquer exclude ficam no **ultimo** plano da fase (**05-07**). Ligar gate
  antes dos testes existirem quebra o build; ate la, `./gradlew koverLog`.
- As correcoes de documentacao ficam em **05-07** porque so fazem sentido depois que o codigo
  novo prova o comportamento — mas sao **trabalho de fase**, nao faxina opcional.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Numero fora da agenda de fato nao toca, nao vibra e nao mostra tela de chamada | SCR-03 (criterio 1 do ROADMAP) | O emulador nao tem radio nem operadora: nao existe chamada real para bloquear | Cenario **40** da Phase 9. **Deferred, nao gap** |
| Onde a chamada bloqueada aparece no historico do fabricante | SCR-07 (won't fix) | Comportamento de interface do fabricante; o registro em si e certo por codigo do Android | Cenario **41** — e registro de percepcao do usuario, nao criterio de falha |
| Contato da agenda toca normalmente | criterio 2 do ROADMAP | Exige chamada real de um contato. **Mudou de natureza:** desde a Fase 4 este cenario exercita o NOSSO lookup, nao mais a plataforma | Cenarios **42** e **43**. A parte automatizavel — o lookup responder presente e o motor decidir tocar — esta coberta em 5-03-01 e 5-05-02 |
| Caixa postal | SCR-06 (criterio 6) | Depende da operadora da linha; a API nao distingue recusar de encaminhar | Cenario **44**. Registrar o que o **chamador** ouve |
| Interacao com o modo de nao perturbar | criterio 6 | Filtro paralelo da plataforma, fora do alcance de qualquer resposta de triagem | Cenario **45**. **Esperado: continua suprimida.** E confirmacao de limitacao, nao conserto |
| Politica de silenciar em chamada real | criterio 6 | Exige chamada real | Cenario **46** |
| Percentil de cauda da decisao | SCR-11 (criterio 5) | O emulador mede o escalonador do host tanto quanto o aplicativo: a Fase 3 tirou um percentil de cauda do assert por instabilidade e a Fase 4 viu 30 ms virarem 140 ms sem mudanca de codigo | Cenario **47**. No CI a mediana e afirmada e a cauda e **reportada** — o numero de 200 ms **nao foi afrouxado** |
| Inicio a frio por chamada, perda do papel, notificacao na tela bloqueada, dois chips | SCR-02, SCR-10, NTF-04 | Exigem aparelho, segundo aplicativo de bloqueio ou dois chips | Cenarios **48** a **51** |
| Telas do opt-in de notificacao, do pedido do papel e do aviso de leitura de agenda revogada | NTF-02, SCR-02 | **Nao e manual: e outra fase.** Toda a interface e da Phase 7 por desenho | Esta fase entrega a maquina de estado, a camada fina de plataforma, a configuracao e as strings. O verifier trata como **deferred to Phase 7**, nunca como gap. Nenhum plano desta fase cria interface |

**Nao entra nesta tabela:** rodar o vinculo do servico e o teste de percentis no emulador. Eles
executam de verdade (mesma excecao ja aberta nas Fases 3 e 4: emulador para infraestrutura de
teste, nao para validacao de campo). **Emulador que nao sobe e blocker reportado no SUMMARY,
nunca troca silenciosa por teste em JVM.**

**Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`** —
politica de validacao fisica do ROADMAP, decidida em 2026-07-28.

---

## Provas de vermelho obrigatorias (14)

| # | Plano | Guarda-corpo | Como quebrar |
|---|-------|--------------|--------------|
| 1 | 05-01 | Regra de chamada repetida | Inverter a condicao do novo `if` no motor |
| 2 | 05-01 | Corte da janela | Trocar `>=` por `>` na consulta do historico |
| 3 | 05-02 | Entrada resiliente | Remover o `runCatching` da fabrica de entrada |
| 4 | 05-02 | Traducao valida | Acrescentar supressao de aviso a decisao de silenciar |
| 5 | 05-03 | Resposta unica | Remover a comparacao-e-troca |
| 6 | 05-03 | Rede permissiva | Remover a emissao do bloco final |
| 7 | 05-03 | Ordem das operacoes | Chamar o trabalho pos-resposta antes de responder |
| 8 | 05-04 | Sem numero completo na notificacao | Trocar o texto para usar o numero inteiro |
| 9 | 05-05 | Chamada de saida | Remover o retorno antecipado |
| 10 | 05-06 | Bloco 7.1 | Introduzir politica por origem no coordenador |
| 11 | 05-06 | Bloco 7.2 | Construir uma decisao de recusa dentro do Service |
| 12 | 05-06 | Bloco 7.3 | Acrescentar import de plataforma no coordenador |
| 13 | 05-06 | Bloco 7.4 | Responder ao sistema duas vezes no Service |
| 14 | 05-07 | Gate de cobertura | `minBound(99)` temporario |

---

## Validation Sign-Off

- [x] Todas as 21 tasks tem `<automated>` no `<verify>` — nenhuma referencia `MISSING`
- [x] Todas as tasks tem `<read_first>` (incluindo o arquivo modificado) e
      `<acceptance_criteria>` verificaveis por grep/comando, com strings exatas e nenhum
      criterio subjetivo
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 identificado e sequenciado primeiro (planos 05-01 e 05-02, wave 1, sem conflito de
      arquivo): regra no motor + consulta de historico + harness de captura + as duas fabricas.
      Bloco 7 e gate de cobertura deliberadamente adiados
- [x] Todas as afirmacoes "medida/comprovadamente" do ROADMAP tem teste que **falha** quando a
      propriedade quebra — 14 provas de vermelho tabuladas acima
- [x] **Cronometro nunca e usado como prova de estrutura**: a ordem "responder primeiro" e provada
      por indice em lista de eventos, e a concentracao da regra no motor pelo Bloco 7 do script
- [x] Assert primario sempre na **mediana**; percentil de cauda e maximo reportados em logcat e
      transformados no cenario 47 da Phase 9, sem afrouxar o numero de 200 ms
- [x] Nenhuma flag de watch mode em comando algum
- [x] Latencia de feedback dentro do orcamento (< 40 s no comando rapido; emulador sobe 1x por
      sessao)
- [x] Todo requisito da fase mapeado a pelo menos uma task; SCR-07 registrado como **WON'T FIX**
      com o trabalho de honestidade que ele exige planejado em 5-07-01
- [x] Itens que exigem aparelho ou interface registrados como diferidos (Phase 7 para as telas,
      Phase 9 para os cenarios 40-51); **nenhum `checkpoint:human-*`** em nenhum dos 7 planos
- [x] Nenhuma permissao nova; nenhuma dependencia nova; nenhum arquivo de interface Compose
- [x] Gate probatorio pos-`clean` com `--no-build-cache` exigido antes de `/gsd:verify-work`
- [x] `nyquist_compliant: true` definido no frontmatter

**Approval:** approved 2026-07-29

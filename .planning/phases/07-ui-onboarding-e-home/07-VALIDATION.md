---
phase: 7
slug: ui-onboarding-e-home
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-30
updated: 2026-07-30
---

# Phase 7 — Validation Strategy

> Contrato de validacao da fase em que o aplicativo deixa de ser esqueleto para o usuario. Esta e a
> fase **menos dependente de aparelho** de todo o projeto: a pesquisa mediu que o grafo de navegacao
> REAL roda sob Robolectric em JVM, com `navigate`, descarte inclusivo, destino corrente e leitura da
> pilha todos funcionando. O emulador nao e necessario para nenhum criterio central. Em troca, esta e
> a fase com o maior numero de guarda-corpos de HONESTIDADE: o texto dos mockups promete cinco
> capacidades que o MVP nao entrega, e a decisao do usuario foi manter o desenho e trocar so o texto.
>
> A armadilha central da fase e um **falso-verde de compilacao medido neste repositorio**: a rota
> tipada da biblioteca de navegacao compila limpa e estoura na primeira composicao do grafo, porque o
> compilador de Kotlin embutido na ferramenta de build nao traz o complemento de serializacao. Nenhum
> assert de compilacao pega isso — so um teste que COMPOE o grafo.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (JVM puro)** | JUnit 4 `4.13.2` + MockK `1.14.11` + `kotlinx-coroutines-test 1.11.0` + Turbine `1.2.1`, sobre AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| **Framework (Compose + navegacao)** | `createComposeRule` sob Robolectric `4.16.1`, **`@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")`**. `NavHost` real funciona em JVM — medido. Nenhuma captura de imagem. `[36]` e **impossivel** em JDK 17 |
| **Framework (instrumentado)** | `AndroidJUnitRunner` + `androidx.test:rules`, AVD `Medium_Phone_API_35`. **Nenhum caso novo nesta fase** — decisao registrada, nao lacuna |
| **Config file** | `app/build.gradle.kts` (`testOptions.unitTests`, bloco de lint, bloco de cobertura, inputs de `Test`) e, a partir de 07-11, `app/lint.xml` |
| **Cobertura** | Kover `0.9.9`, gate `koverVerify minBound(80)`, atual **96,69%**. `ui.*` fica **fora** do filtro; a chave nova de onboarding entra em `settings.*`, que E medido. Exclude novo so por **nome de classe** e so no plano **07-11** |
| **Quick run command** | `./gradlew testDebugUnitTest --rerun-tasks` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` — `connectedDebugAndroidTest` **nao aceita** `--tests` |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Estimated runtime** | quick ~40-60 s · instrumentado ~60 s incremental + 2-4 min de boot a frio · full ~8-12 min |
| **Relatorios** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/` · lint: `app/build/reports/lint-results-debug.{html,sarif}` (**o SARIF e o que permite contagem por regra**, usado em 07-11) · androidTest: `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` |

**Dependencia nova: NENHUMA.** `navigation-compose 2.9.8`, `lifecycle-viewmodel-compose 2.11.0`,
`lifecycle-runtime-compose 2.11.0`, `savedstate 1.4.0` e `activity-compose 1.13.0` **ja estao no
classpath e ja entram no APK** desde o bootstrap, declarados e nunca consumidos. Runtime e teste,
ambos completos. Um plano que acrescente biblioteca, plugin ou entrada no catalogo de versoes esta
errado — inclusive, e principalmente, o complemento de serializacao.

**Permissao nova: NENHUMA.** `READ_CONTACTS` e `POST_NOTIFICATIONS` ja declaradas e ja na lista
autorizada do script. Um plano que edite as permissoes do manifest esta errado.

**Inputs de task ja declarados e suficientes:** `schemas`, `src/main/java` e `src/main/res` ja sao
inputs de todas as tasks de teste. Nenhuma edicao nova e necessaria. **Mas** `--rerun-tasks` e
obrigatorio nos comandos rapidos: os testes desta fase leem recursos do disco, e teste que le arquivo
sem input declarado vai para o estado de atualizado e da verde falso.

**Armadilhas herdadas e ainda validas:**
- `grep -c` sai com codigo 1 quando conta zero. Padrao: `[ "$(grep -c ...)" -eq 0 ]`. **Nunca**
  `|| echo 0`. O modo de abortar do shell continua **proibido** em `scripts/verify-invariants.sh`
- Evidencia so vale com limpeza **e** sem cache de build: reaproveitamento de cache tem o mesmo
  defeito probatorio que tarefa considerada atualizada
- **Auto-sabotagem por grep: SEIS executores ja cairam nela.** Proibicao descrita em **prosa
  portuguesa**, sem escrever o identificador vigiado; criterio que precise falar de identificador
  afirma sobre **recurso lido** ou **objeto composto em tempo de teste**, nunca sobre existencia de
  texto no fonte. O Bloco 2 (identificador do aplicativo literal) ja pegou **tres**
- Prova de vermelho **sempre sobre codigo JA COMMITADO**, restaurada por edicao manual. Um executor
  da Fase 6 perdeu 74 strings usando descarte de arquivo sobre trabalho novo
- Segundo container no mesmo processo derruba a aplicacao: todo teste constroi o dono de estado com
  dubles e **nunca** o container
- `@get:Rule` combinado com `@JvmField` desliga a regra do JUnit
- Gravar e consultar o retrato no instante seguinte e corrida **no teste**, nao no produto: o retrato
  vem de cache mantido por coletor assincrono (Fase 3)

**Armadilhas NOVAS desta fase, todas medidas:**
1. **Rota tipada e falso-verde de compilacao.** Compila limpa; estoura na primeira composicao com
   excecao de serializacao. Inspecao do bytecode confirma a ausencia do serializador gerado.
   **Rotas por texto**, e o guarda-corpo e um teste que compoe o grafo (07-02).
2. **Destino inicial decidido a partir de fluxo assincrono nao re-navega.** Trocar o destino inicial
   depois de o grafo composto nao faz nada. Resolver ANTES de compor, com estado de carregamento
   explicito. **Proibido** bloquear a thread principal para decidir (07-10).
3. **A entrada do proprio grafo tem rota nula** em `currentBackStack` — todo assert de pilha filtra
   nulo, ou a lista esperada ganha um elemento fantasma.
4. **Aparelho padrao do Robolectric e pequeno demais** para tela inteira: sem os qualificadores reais,
   todo assert de exibicao fica vermelho por motivo falso.
5. **O repositorio de consulta de contatos e proibido para a UI desta fase.** Ele registra observador
   da agenda e dispara construcao de cache **medida em 2,57 s**. O estado de permissao vem do
   verificador de plataforma, que nao o toca. Travado pela checagem 9.3 (07-10).
6. **A supressao de lint da Fase 1 nunca segurou o build.** Com a regra reabilitada, o lint saiu com
   codigo 0 — severidade de aviso, conversao de aviso em erro desligada. Dizer isso com franqueza em
   07-11 em vez de repetir a premissa antiga; e **fazer o bloco de desabilitados voltar nao e prova de
   vermelho**.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest --rerun-tasks` (< 60 s).
- **Apos cada wave:** `./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt &&
  bash scripts/verify-invariants.sh`. Nas waves 1 a 4 usar `./gradlew koverLog`; `koverVerify` volta a
  ser cobrado no plano **07-11**.
- **Phase gate:** suite JVM verde pos-limpeza, com `--no-build-cache` e a linha de resumo mostrando
  tarefas **executadas** e nao apenas atualizadas; suite instrumentada EXISTENTE reexecutada para
  provar que a fase nao a quebrou. Arquivado em `07-EVIDENCE.md`.
- **Nenhum comando em modo de observacao.** Emulador sobe uma vez por sessao, e so no plano final.
- **Prova de vermelho obrigatoria** para cada guarda-corpo: quebrar, ver falhar, restaurar,
  transcrever no SUMMARY. Sao **32** provas, tabuladas abaixo.
- **Assercao primaria sempre na mediana** em qualquer medida de tempo — e esta fase **nao tem nenhuma
  assercao de tempo nova**. A consulta de papel e provada por **contador de invocacoes**, jamais por
  cronometro; a partida a frio de 680 ms e linha de base reportada, e o veredito de desempenho fica no
  roteiro fisico.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 7-01-01 | 01 | 1 | UIX-08 | unit puro (tokens) + Robolectric (fixacao nos 3 esquemas) | `./gradlew testDebugUnitTest --tests "*ThemeTokensTest" --tests "*CallColorFixationTest" --rerun-tasks` — >= 3 `@Test` novos | ✅ estender | ⬜ pending |
| 7-01-02 | 01 | 1 | UIX-07 | contagem de chaves + lint | `./gradlew testDebugUnitTest --tests "*Phase7StringsTest" --rerun-tasks && ./gradlew :app:lintDebug --rerun-tasks` — 269 chaves | ✅ estender | ⬜ pending |
| 7-01-03 | 01 | 1 | **UIX-11** | Robolectric (le o TEXTO dos recursos) | `./gradlew testDebugUnitTest --tests "*Phase7StringsTest" --rerun-tasks` — >= 8 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 7-02-01 | 02 | 1 | UIX-09 | refatoracao provada pela suite da Fase 6 | `./gradlew testDebugUnitTest --tests "*CallScreenSemanticsTest" --rerun-tasks` — mesma contagem | ❌ **Wave 0** | ⬜ pending |
| 7-02-02 | 02 | 1 | UIX-01 | Compose+Robolectric (grafo REAL composto) | `./gradlew testDebugUnitTest --tests "*NavGraphContractTest" --rerun-tasks` — >= 6 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 7-03-01 | 03 | 2 | UIX-08, UIX-09 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ **Wave 0** | ⬜ pending |
| 7-03-02 | 03 | 2 | UIX-09, UIX-10 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ **Wave 0** | ⬜ pending |
| 7-03-03 | 03 | 2 | **UIX-09** | Compose+Robolectric (dois eixos, papeis, estado) | `./gradlew testDebugUnitTest --tests "*Phase7ComponentSemanticsTest" --rerun-tasks` — >= 14 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 7-04-01 | 04 | 2 | **SCR-02**, UIX-02, UIX-10 | unit puro (tipo fechado + contador de consultas) | `./gradlew testDebugUnitTest --tests "*HomeViewModelTest" --tests "*RoleLiveStateTest" --rerun-tasks` — >= 10 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 7-04-02 | 04 | 2 | UIX-01 | unit puro (lista ordenada de eventos) | `./gradlew testDebugUnitTest --tests "*PermissionAskOrderTest" --tests "*DataStoreSettingsRepositoryTest" --rerun-tasks` — >= 6 `@Test` novos | ❌ **Wave 0** | ⬜ pending |
| 7-04-03 | 04 | 2 | **UIX-03** | unit + repositorio REAL em pasta temporaria | `./gradlew testDebugUnitTest --tests "*SettingsViewModelTest" --rerun-tasks` — >= 8 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 7-05-01 | 05 | 3 | UIX-01, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-05-02 | 05 | 3 | **SCR-01**, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-05-03 | 05 | 3 | SCR-01, UIX-01, UIX-09 | Compose+Robolectric | `./gradlew testDebugUnitTest --tests "*WelcomeAndRoleStepTest" --rerun-tasks` — >= 12 `@Test` | ❌ criado pela task | ⬜ pending |
| 7-06-01 | 06 | 3 | UIX-01, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-06-02 | 06 | 3 | UIX-01 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-06-03 | 06 | 3 | UIX-01, UIX-09 | Compose+Robolectric (4 ramos de permissao) | `./gradlew testDebugUnitTest --tests "*ContactsAndWhitelistStepTest" --rerun-tasks` — >= 14 `@Test` | ❌ criado pela task | ⬜ pending |
| 7-07-01 | 07 | 3 | UIX-01, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-07-02 | 07 | 3 | UIX-01, UIX-10 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-07-03 | 07 | 3 | UIX-01, UIX-09, UIX-10 | Compose+Robolectric (veredito honesto) | `./gradlew testDebugUnitTest --tests "*NotificationAndSummaryStepTest" --rerun-tasks` — >= 14 `@Test` | ❌ criado pela task | ⬜ pending |
| 7-08-01 | 08 | 3 | UIX-02, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-08-02 | 08 | 3 | **UIX-02**, UIX-10 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-08-03 | 08 | 3 | **SCR-02**, UIX-02, UIX-09, UIX-10 | Compose+Robolectric (8 estados + varredura da arvore semantica) | `./gradlew testDebugUnitTest --tests "*HomeScreenStateTest" --tests "*HomePrivacyTest" --rerun-tasks` — >= 20 `@Test` | ❌ criado pela task | ⬜ pending |
| 7-09-01 | 09 | 3 | UIX-03, UIX-09 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-09-02 | 09 | 3 | **UIX-03**, UIX-11 | build + lint | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-09-03 | 09 | 3 | **UIX-03**, UIX-09, UIX-10 | Compose+Robolectric (completude dos 16 itens) | `./gradlew testDebugUnitTest --tests "*ProtectionScreenTest" --rerun-tasks` — >= 22 `@Test` | ❌ criado pela task | ⬜ pending |
| 7-10-01 | 10 | 4 | SCR-01, UIX-01, UIX-03 | build + contrato do grafo | `./gradlew assembleDebug lint detekt --rerun-tasks && ./gradlew testDebugUnitTest --tests "*NavGraphContractTest" --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 7-10-02 | 10 | 4 | UIX-01 | build + script (Bloco 2) | `./gradlew assembleDebug lint detekt --rerun-tasks && bash scripts/verify-invariants.sh` | ✅ reescrever | ⬜ pending |
| 7-10-03 | 10 | 4 | **UIX-01**, **UIX-07**, UIX-03 | Compose+Robolectric (fluxo REAL) + script (Bloco 9) | `./gradlew testDebugUnitTest --tests "*OnboardingFlowTest" --tests "*DialerRouteTest" --rerun-tasks && bash scripts/verify-invariants.sh` — >= 15 `@Test` | ✅ estender script | ⬜ pending |
| 7-11-01 | 11 | 5 | **UIX-07** | lint + contagem no SARIF | `./gradlew :app:lintDebug --rerun-tasks && ./gradlew --rerun-tasks assembleDebug testDebugUnitTest detekt` | ✅ estender + `app/lint.xml` novo | ⬜ pending |
| 7-11-02 | 11 | 5 | UIX-09, UIX-11 | grep de completude dos cenarios fisicos + script | `bash scripts/verify-invariants.sh && [ "$(grep -cE '^\| 6[1-9] \|' docs/TESTE-FISICO-SAMSUNG.md)" -ge 6 ]` | ✅ estender docs | ⬜ pending |
| 7-11-03 | 11 | 5 | UIX-07, UIX-09, UIX-11 | gate de cobertura + evidencia pos-limpeza | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` | ❌ `07-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| **SCR-01** | 7-05-02 (o passo pede o papel com explicacao honesta e os tres ramos de resultado; negado nao trava e nao repete o dialogo), 7-05-03 (os ramos provados em composicao), 7-10-01 (o seletor do sistema lancado de verdade, e o retorno apenas reconsulta) |
| **SCR-02** | 7-04-01 (papel reconsultado a cada chamada, provado por **contador de invocacoes** e nao por cronometro; nenhum campo guarda a resposta), 7-08-03 (a home exibe o estado real e o botao de correcao, e nao o exibe quando o aparelho nao oferece o papel), 7-10-01 (reconsulta na retomada da home e da Protecao, mais a redundancia deliberada no retorno do seletor) |
| **UIX-01** | 7-02-02 (dez rotas por texto, contagem travada, grafo composto de verdade), 7-04-02 (progresso, chave de onboarding concluido, marca gravada ao disparar), 7-05-01/02/03 (boas-vindas e passos 1-2), 7-06-01/02/03 (passos 3-4 com os quatro ramos de permissao), 7-07-01/02/03 (passos 5-6), 7-10-03 (fluxo de ponta a ponta, pular, e pilha sem retorno ao onboarding) |
| **UIX-02** | 7-04-01 (as quatro fontes combinadas e o tipo fechado do valor), 7-08-01 (os quatro componentes da home, com o valor da estatistica fechado por tipo e o numero recebido ja mascarado), 7-08-02 (os oito estados degradados e a precedencia dos avisos), 7-08-03 (um caso por estado, mais a varredura de privacidade sobre a arvore semantica inteira) |
| **UIX-03** | 7-04-03 (efeito imediato contra o repositorio real, sem funcao de salvar), 7-09-01/02/03 (os 16 itens com explicacao permanente, as duas confirmacoes por perda de dado, e o caso de **completude** que pega item esquecido), 7-10-01 e 7-10-03 (rota para a tela de ativacao do modo discador nos cinco ramos — pendencia de 06-05 fechada) |
| **UIX-07** | 7-01-02 (as 43 chaves novas e a contagem de 269), 7-10-03 (**Bloco 9.1**: zero literal de texto de interface nas tres pastas da fase), 7-11-01 (supressao reabilitada e estreitada nominalmente, com prova de que nao silencia esta fase) |
| **UIX-08** | 7-01-01 (as tres cores semanticas fixas fora da cor dinamica, travadas em JVM pura e fixadas nos tres esquemas), 7-03-01/02 (componentes sobre os tokens existentes, sem token novo alem dos tres) |
| **UIX-09** | 7-02-01 (os tres asserts de dois eixos extraidos para pacote neutro, sem duplicacao), 7-03-03 (papeis, descricao de estado e quatro dos cinco pontos de risco de semantica mesclada), 7-05-03 / 7-06-03 / 7-07-03 / 7-08-03 / 7-09-03 (dois eixos em cada tela; o quinto ponto de risco em 7-08-03), 7-10-03 (fluxo percorrivel somente por nos com texto ou descricao de recurso), 7-11-02 (locucao, gestos e ordem de foco efetiva como cenarios fisicos) |
| **UIX-10** | 7-03-02/03 (linha desabilitada com motivo textual), 7-04-01/03 (estado de carregamento explicito; nenhum bloqueio da thread principal), 7-07-03 (veredito parcial em vez de falsamente positivo), 7-08-02/03 (carregando, erro e historico desligado — os tres em que zero e **proibido**), 7-09-03 (linha do modo discador indisponivel com motivo), 7-10-01 (destinos das Phases 8-9 com estado comunicado, nunca tela em branco) |
| **UIX-11** | 7-01-03 (varredura de honestidade sobre o **texto dos recursos** das chaves da fase, com tres provas de vermelho: fraude, base global e processamento cifrado), 7-05-01/02 (as cinco capacidades desonestas fora da copy; imagens remotas viram superficie tonal), 7-06-01 (descricao de nunca silenciar preservada — a do mockup e falsa), 7-08-01/02 (zero rotulo de risco na home; motivo real da decisao), 7-09-02 (cartao de limitacoes com as quatro frases originais, fonte unica de verdade), 7-11-02 (documento de telas sem afirmacao nao medida) |

Nenhum requisito da fase fica sem task. Nenhuma task fica sem `<automated>`.

**Requisito que NAO ganha implementacao nova:** UIX-08 ja esta completo desde a Fase 1 no que diz
respeito a tema e cor dinamica; esta fase apenas acrescenta os **tres** tokens semanticos e nao toca
nenhum token existente — diferenca vazia em `Theme.kt` e criterio de aceite de 7-01-01.

**Arquivo que NAO pode ser tocado na fase inteira:** `domain/CallDecisionEngine.kt`. Diferenca vazia e
criterio de aceite de 7-04-03.

---

## Wave 0 Requirements

Infraestrutura que **bloqueia** as tasks seguintes, concentrada na wave 1 (planos 07-01 e 07-02, que
nao compartilham nenhum arquivo e rodam em paralelo) e na wave 2 (07-03 e 07-04, idem):

- [ ] `app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt` (07-02) — **extrair** os tres
      asserts de dois eixos de dentro do teste de semantica da Fase 6, sem mudar comportamento, e
      reapontar aquele teste. **Bloqueia todo assert de acessibilidade da fase.** Duplicar seria a
      alternativa errada: o eixo com dentes divergiria entre as copias
- [ ] `ui/navigation/SentinelaRoutes.kt` (07-02) — as dez constantes por texto; toda tela e todo teste
      de fluxo dependem delas
- [ ] `app/src/test/.../ui/navigation/NavGraphContractTest.kt` (07-02) — o guarda-corpo da rota tipada
      precisa existir antes de o grafo real ser escrito
- [ ] As **43 chaves** pt-BR em `res/values/strings.xml` (07-01) — nenhuma tela pode ser escrita antes,
      porque texto embutido em Kotlin e proibido
- [ ] `ui/theme/Color.kt` com os tres tokens semanticos (07-01) — as telas das waves 3 os consomem
- [ ] `app/src/test/.../ui/Phase7StringsTest.kt` (07-01) — a varredura de honestidade precisa existir
      **antes** de as telas serem escritas, precedente de 06-02
- [ ] Os seis componentes compartilhados (07-03) — as cinco telas de onboarding, a home e a Protecao os
      consomem; escrever um cartao de opcao por tela multiplicaria por cinco a chance de perder um
      estado na semantica mesclada
- [ ] `ui/home/HomeUiState.kt` com o **tipo fechado do valor de estatistica** (07-04) — e o que torna o
      zero mentiroso impossivel, e a home depende dele
- [ ] Os tres donos de estado e as fabricas manuais (07-04) — sem eles nenhuma tela e testavel sem
      construir container
- [ ] Chave de onboarding concluido no repositorio de preferencias (07-04) — o destino inicial depende
      dela
- [ ] Instalacao de framework: **NENHUMA**. Dependencia nova: **NENHUMA**. Permissao nova: **NENHUMA**

**Deliberadamente NAO e Wave 0:**

- **`ui/navigation/SentinelaNavHost.kt`** fica em **07-10**, junto das camadas de rota e das telas que
  ele referencia: um grafo que aponte para telas que nao existem nao compila. O que e Wave 0 sao as
  **constantes** e o **teste de contrato**, e eles bastam para o guarda-corpo da rota tipada existir
  desde a wave 1
- **Bloco 9 de `scripts/verify-invariants.sh`** fica em **07-10**, o plano que cria as pastas que ele
  vigia. Liga-lo antes deixaria o script vermelho sem defeito real — precedente das Fases 3, 4, 5 e 6
- **A politica de lint** fica no **ultimo** plano (07-11), no precedente de 06-08/06-09. Reabilitar
  antes de as telas consumirem as chaves deixaria o relatorio artificialmente ruim e nada provaria
- **`koverVerify` e qualquer exclude** ficam em **07-11**; ate la, `./gradlew koverLog`
- **Reescrita de `docs/design/TELAS.md` e os cenarios fisicos a partir de 61** ficam em 07-11, porque
  so fazem sentido depois de os testes dizerem o que de fato foi verificado — mas sao **trabalho de
  fase**, nao faxina
- **A faxina dos dois defeitos incidentais** e dividida por dono de arquivo: o porcento cru no arquivo
  de recursos vai para **07-01** (o plano que edita recursos), e o bloco de dependencias duplicado vai
  para **07-11** (o plano que edita o arquivo de build). Junta-los criaria conflito de arquivo entre
  waves

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Locucao, ritmo e verbosidade reais do leitor de tela | UIX-09 (criterio 4) | O sintetizador e do sistema. O que e automatizavel — arvore semantica mesclada, descricao de conteudo, descricao de estado, ordem de travessia **declarada**, cabecalhos — fica NESTA fase, em 7-03-03 e nos testes de cada tela | Cenario **61**. Deferred, nao gap |
| Gestos do leitor de tela (explorar por toque, deslizar) | UIX-09 | O varredor de acessibilidade e do sistema | Cenario **62** |
| Ordem de foco **efetiva** | UIX-09 | A declarada e automatizada; a efetiva depende do varredor do sistema | Cenario **63** |
| Contraste medido sob cor dinamica com papel de parede real | UIX-09 | Medir razao de contraste seria escopo novo; o tema esta fixado por teste desde as Fases 1 e 6, e os pares semanticos desta fase sao literais por construcao | Cenario **64** |
| Escala de fonte a 200% em aparelho | UIX-09, UIX-10 | Toda tela da fase e rolavel por contrato, e isso e verificavel; a percepcao com a escala real do sistema nao e | Cenario **65** |
| Percepcao da partida a frio em hardware | — | Mediana de **680 ms** medida em aparelho virtual, onde a cauda mede o hospedeiro tanto quanto o aplicativo. Assert de tempo novo seria fragil, e **cronometro nao prova estrutura** — a garantia estrutural (nada construido na thread principal, nenhum bloqueio para decidir o destino inicial) fica em 7-10-02 | Cenario **66** |
| Fluxo completo em Samsung com o leitor de tela ligado | UIX-01, UIX-09 | Validacao de campo | Cenario **67** |

**Nao entra nesta tabela, e a diferenca em relacao a Fase 6 e o achado central da pesquisa:** o fluxo
multi-tela **inteiro** — grafo real, `navigate`, descarte inclusivo, destino corrente, leitura da
pilha, quatro ramos de permissao, oito estados degradados da home, os 16 itens da Protecao — roda em
**JVM**, medido. Nenhum criterio central desta fase depende de emulador, e **nenhum plano da fase
acrescenta caso instrumentado**. Isso e decisao registrada, nao lacuna: a suite instrumentada
existente e reexecutada em 7-11-03 apenas para provar que a fase nao a quebrou. **Emulador que nao
sobe e blocker reportado no SUMMARY, nunca troca silenciosa por teste em JVM.**

**Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`** — politica de
validacao fisica do ROADMAP, decidida em 2026-07-28. O verifier trata os cenarios 61 a 67 como
**deferred to Phase 9**.

---

## Provas de vermelho obrigatorias (32)

| # | Plano | Guarda-corpo | Como quebrar |
|---|-------|--------------|--------------|
| 1 | 07-01 | Cor semantica fixa | Alterar um digito do token de atencao |
| 2 | 07-01 | Honestidade: classificacao de spam | Chave nova afirmando fraude |
| 3 | 07-01 | Honestidade: base de numeros | Chave nova prometendo base global de milhoes |
| 4 | 07-01 | Honestidade: processamento cifrado | Chave nova afirmando processamento local criptografado |
| 5 | 07-02 | Rota tipada proibida | Sonda com destino por objeto anotado ⇒ vermelho em EXECUCAO, compilador verde |
| 6 | 07-02 | Pilha sem retorno ao onboarding | Remover o descarte inclusivo |
| 7 | 07-03 | Alvo de toque, eixo desenhado | Reduzir o item da barra inferior de 56dp para 40dp ⇒ so o eixo desenhado pega |
| 8 | 07-03 | Semantica mesclada engolindo estado | Envolver o interruptor da linha de configuracao com mesclagem |
| 9 | 07-03 | Estado nunca so por cor | Remover a descricao de estado do cartao desabilitado |
| 10 | 07-04 | Zero mentiroso (dono de estado) | Publicar valor carregado zero com o historico desligado |
| 11 | 07-04 | Papel como estado vivo | Memorizar a resposta do papel num campo ⇒ o contador de consultas cai |
| 12 | 07-04 | Fronteira do numero | Publicar a ultima bloqueada sem mascara |
| 13 | 07-04 | Marca de permissao ao disparar | Mover a gravacao para depois do disparo ⇒ a lista de eventos inverte |
| 14 | 07-04 | Efeito imediato sem salvar | Acumular a mudanca num campo e gravar so numa funcao nova |
| 15 | 07-04 | Poda ao escolher nao guardar | Remover a chamada de poda |
| 16 | 07-05 | Padrao do passo de desconhecidos | Trocar bloquear por tocar |
| 17 | 07-05 | Aviso de escopo presente | Remover uma das tres frases do cartao de honestidade |
| 18 | 07-05 | Botao desabilitado anunciado | Envolver o botao do passo 1 com mesclagem |
| 19 | 07-06 | Negacao definitiva sem dialogo inutil | Oferecer o botao de permitir no ramo definitivo |
| 20 | 07-06 | Opcoes sempre editaveis | Desabilitar as quatro opcoes sem a permissao |
| 21 | 07-06 | Padrao do passo de whitelist | Trocar nunca silenciar por tocar |
| 22 | 07-07 | Veredito nunca falsamente positivo | Fazer o titulo ser sempre o de tudo pronto |
| 23 | 07-07 | Opt-in desligado por padrao | Fazer o interruptor vir ligado |
| 24 | 07-07 | Acao fora do no mesclado | Mover o botao de correcao para dentro do no da linha |
| 25 | 07-08 | Zero mentiroso (tela) | Renderizar zero no ramo indisponivel do cartao de estatistica |
| 26 | 07-08 | Fronteira do numero na home | Exibir o numero sem mascara |
| 27 | 07-08 | Interruptor do cartao principal fora da mesclagem | Envolver o cartao com mesclagem contendo o interruptor |
| 28 | 07-08 | Botao de correcao so com papel disponivel | Exibi-lo mesmo sem o papel no aparelho |
| 29 | 07-09 | Completude dos 16 itens | Remover o item de chamada repetida |
| 30 | 07-09 | Confirmacao so por perda de dado | Acrescentar dialogo a troca de politica; e remover o dialogo de limpar historico |
| 31 | 07-10 | Bloco 9 (texto embutido, fronteira do numero, repositorio proibido) | Uma sabotagem por checagem, sobre codigo JA COMMITADO |
| 32 | 07-11 | Estreitamento do lint | Chave nova com prefixo DESTA fase sem uso ⇒ o achado deve aparecer. Fazer o bloco de desabilitados voltar **nao** e prova |

Cada prova e quebrada, vista falhar e **restaurada por edicao manual**, com a saida transcrita no
SUMMARY do plano. Prova adicional obrigatoria em 07-10: confirmar que o Bloco 9 continua **verde** com
todos os comentarios e KDoc da fase no lugar — e a prova de que o criterio nao casa a propria prosa,
armadilha que ja pegou seis executores.

---

## Validation Sign-Off

- [x] Todas as tasks tem `<automated>` verify ou dependencia de Wave 0 declarada
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 cobre todos os arquivos que bloqueiam waves seguintes
- [x] Nenhum comando em modo de observacao
- [x] Latencia de feedback < 60 s no comando rapido
- [x] Nenhuma assercao de tempo nova; o estado vivo do papel e provado por contador, nao por cronometro
- [x] `nyquist_compliant: true` no frontmatter

**Approval:** approved 2026-07-30

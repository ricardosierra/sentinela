---
phase: 6
slug: modo-discador-opcional
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
updated: 2026-07-29
---

# Phase 6 — Validation Strategy

> Contrato de validacao da fase de **maior risco tecnico do MVP**. A pesquisa mediu 14 vezes em
> aparelho virtual e INVERTEU a analise de risco: morrer no meio de uma chamada e **seguro** (o
> sistema de telecomunicacoes detecta a desconexao e assume com o discador que vem no aparelho —
> medido, com as chamadas continuando ativas). O que ninguem detecta e uma interface **vinculada e
> travada**. Por isso a estrategia desta fase e o **inverso deliberado** da rede permissiva da
> Fase 5: no caminho da chamada, excecao PROPAGA, e o guarda-corpo e um prazo de apresentacao que
> falha alto.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (JVM puro)** | JUnit 4 `4.13.2` + MockK `1.14.11` + `kotlinx-coroutines-test` + Turbine, sobre AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| **Framework (JVM + plataforma real)** | **Robolectric `4.16.1` com `@Config(sdk = [35])`**; `[29]` obrigatorio para provar o piso do projeto (ramo de recusa de chamada e caminho de notificacao sem estilo de chamada). `[36]` e **impossivel** em JDK 17 |
| **Framework (Compose)** | `createComposeRule` sob Robolectric — semantica, alvos de toque e ordem de foco. Nenhuma captura de imagem |
| **Framework (instrumentado)** | `AndroidJUnitRunner` + `androidx.test:rules` (`ServiceTestRule`), AVD `Medium_Phone_API_35` |
| **Config file** | `app/build.gradle.kts` (`testOptions.unitTests`, bloco `kover`) |
| **Cobertura** | Kover `0.9.9`, gate `koverVerify minBound(80)`, atual **97,64%**. `CallSessionCoordinator`, `CallStateMapper`, `CallUiState` e `DialerModeState` sao **puros** e entram sem exclude. Exclude novo so por **nome de classe** e so no plano **06-08** |
| **Quick run command** | `./gradlew testDebugUnitTest --rerun-tasks` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` — `connectedDebugAndroidTest` **nao aceita** `--tests` |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Estimated runtime** | quick ~35-50 s · instrumentado ~60 s incremental + 2-4 min de boot a frio · full ~8-12 min |
| **Relatorios de evidencia** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/` · lint/detekt: `app/build/reports/` · androidTest: `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` |

**Dependencia nova: nenhuma.** Robolectric, MockK, coroutines-test, Turbine e `ServiceTestRule` ja
estao declarados; a regra de teste de Compose vem do BOM ja em uso. Um plano que acrescente
dependencia esta errado.

**Permissao nova: quatro** — `ROLE_DIALER` (papel), `BIND_INCALL_SERVICE` (atributo do servico),
`CALL_PHONE` (runtime, medida como **nao concedida** no install) e `USE_FULL_SCREEN_INTENT`
(autorizada por decisao do usuario em 2026-07-29; medida como concedida no install porque o
aplicativo se qualifica como aplicativo de chamada). `docs/PERMISSOES.md` e atualizado **antes** e a
lista autorizada do script no **mesmo commit** (plano 06-03, tasks 1 e 2). A Fase 4 provou que uma
permissao nova gera **dois** vermelhos no script — aqui sao tres permissoes movidas da lista de fases
futuras para a lista autorizada.

**Armadilhas herdadas e ainda validas:**
- `grep -c` sai com codigo 1 quando o resultado e zero. `set -e` continua **proibido** em
  `scripts/verify-invariants.sh` e **nunca** usar `|| echo 0`. Padrao: `[ "$(grep -c ...)" -eq 0 ]`.
- Evidencia so vale com `clean` **e** `--no-build-cache`: `FROM-CACHE` tem o mesmo defeito probatorio
  que `UP-TO-DATE`.
- Teste que le arquivo de disco sem input declarado vai `UP-TO-DATE` e da verde falso. **Aconteceu na
  propria pesquisa desta fase:** a primeira execucao das sondas voltou `UP-TO-DATE` com zero saida e o
  resultado so apareceu com `--rerun-tasks`. Por isso todos os comandos rapidos desta fase levam
  `--rerun-tasks`.
- Nome do XML do androidTest tem parenteses e espacos — sempre glob `TEST-*.xml`.
- Segundo container no mesmo processo derruba a aplicacao (duas instancias do armazenamento de
  preferencias sobre o mesmo arquivo). Todo teste usa o container da aplicacao ou dubles.
- **Auto-sabotagem por grep: cinco executores ja cairam nela neste projeto.** O comentario ou KDoc
  ditado por um plano casando com o grep de contagem-zero do mesmo plano. Nesta fase o risco e maior
  que o normal, porque ela escreve muito KDoc sobre permissoes e proibicoes. Regra: proibicao descrita
  em **prosa portuguesa**, sem escrever o identificador vigiado; e criterio de aceite que precise
  falar de identificador afirma sobre o **bloco de inclusao/exclusao** ou sobre objetos construidos em
  tempo de teste, nunca sobre a existencia de texto no fonte.

**Armadilhas NOVAS desta fase, ambas medidas:**
1. **Teste vacuoso de mudo e viva-voz.** Chamar o metodo de mudo num servico de chamada sem telefone
   vinculado **nao lanca e nao faz nada** (medido: a lista de chamadas volta vazia). Um teste que chame
   e nao asserte nada passa sem provar coisa alguma — mesma classe de falso-verde que ja pegou este
   projeto tres vezes. **Mudo e viva-voz sao provados na costura de comandos, com duble.**
2. **Reflexao e desnecessaria.** A Fase 5 precisou de proxy sobre interface interna e acesso a campo
   privado. Aqui o metodo que recebe a chamada e publico e o objeto de chamada e mockavel apesar de a
   classe ser final (medido). Reflexao nesta fase e sinal de desenho errado.

---

## Sampling Rate

- **Apos cada commit de task:** `./gradlew testDebugUnitTest --rerun-tasks` (< 50 s).
- **Apos cada wave:** `./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt &&
  bash scripts/verify-invariants.sh`. Nas waves 1 a 4 usar `./gradlew koverLog`; `koverVerify` volta a
  ser cobrado no plano **06-08**.
- **Phase gate:** suite JVM **e** instrumentada verdes pos-`clean`, com `--no-build-cache` e
  `N actionable tasks: M executed` com **M > 0**. Arquivado em `06-EVIDENCE.md`.
- **Nenhum comando usa modo de observacao.** Emulador sobe uma vez por sessao.
- **Prova de vermelho obrigatoria** para cada guarda-corpo: quebrar, ver falhar, restaurar. Sao 15
  provas, tabuladas abaixo.
- **Assercao primaria sempre na mediana** quando houver medida de tempo — e esta fase nao tem nenhuma
  assercao de tempo nova: o prazo de apresentacao e provado com relogio VIRTUAL, nao com cronometro.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 6-01-01 | 01 | 1 | DIA-02 | unit parametrizado (13 estados, tabela a mao) | `./gradlew testDebugUnitTest --tests "*CallStateMapperTest" --rerun-tasks` — >= 14 casos | ❌ **Wave 0** | ⬜ pending |
| 6-01-02 | 01 | 1 | DIA-02 | unit puro (duble + lista ordenada de eventos) | `./gradlew testDebugUnitTest --tests "*CallSessionCoordinatorTest" --tests "*DtmfPairingTest" --rerun-tasks` — >= 15 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 6-01-03 | 01 | 1 | DIA-02 | unit (injecao de defeito + relogio virtual) | `./gradlew testDebugUnitTest --tests "*CallSessionFailureTest" --tests "*CallSessionWatchdogTest" --rerun-tasks` — >= 11 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 6-02-01 | 02 | 1 | DIA-02 | unit (tokens de tema) | `./gradlew testDebugUnitTest --tests "*ThemeTokensTest" --rerun-tasks` — >= 6 `@Test` novos | ✅ estender | ⬜ pending |
| 6-02-02 | 02 | 1 | DIA-02 | Robolectric (recursos reais + varredura de honestidade) | `./gradlew testDebugUnitTest --tests "*CallStringsTest" --rerun-tasks` — >= 8 `@Test` | ❌ **Wave 0** | ⬜ pending |
| 6-02-03 | 02 | 1 | DIA-02 | build + lint (componentes e alvos de toque) | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 6-03-01 | 03 | 2 | DIA-01 | script (matriz + lista autorizada) | `bash scripts/verify-invariants.sh` | ✅ estender | ⬜ pending |
| 6-03-02 | 03 | 2 | DIA-01, DIA-02 | Robolectric (servico real + objeto de chamada mockado) + script | `./gradlew testDebugUnitTest --tests "*SentinelaInCallServiceTest" --tests "*CallRejectCompatTest" --rerun-tasks && bash scripts/verify-invariants.sh` — >= 10 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-03-03 | 03 | 2 | DIA-01, DIA-05 | Robolectric (gerenciador de papeis sombreado) + unit puro + script (Bloco 8) | `./gradlew testDebugUnitTest --tests "*DialerRoleManagerTest" --tests "*DialerModeStateTest" --rerun-tasks && bash scripts/verify-invariants.sh` — >= 13 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-04-01 | 04 | 3 | DIA-02 | build + lint (tela cheia de chamada recebida) | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 6-04-02 | 04 | 3 | DIA-02 | build + lint (saida, ativa, tons, rota) | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 6-04-03 | 04 | 3 | DIA-02 | Compose sob Robolectric + unit de privacidade | `./gradlew testDebugUnitTest --tests "*CallScreenSemanticsTest" --tests "*CallLoggingPrivacyTest" --rerun-tasks` — >= 13 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-05-01 | 05 | 3 | DIA-03 | Robolectric (gerenciador de telecomunicacoes dublado) | `./gradlew testDebugUnitTest --tests "*DialerPlaceCallTest" --tests "*CallPhonePermissionTest" --rerun-tasks` — >= 11 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-05-02 | 05 | 3 | DIA-03 | Compose sob Robolectric | `./gradlew testDebugUnitTest --tests "*DialerScreenStateTest" --rerun-tasks && ./gradlew assembleDebug lint detekt` — >= 8 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-05-03 | 05 | 3 | DIA-01 | build + varredura de honestidade das strings | `./gradlew assembleDebug testDebugUnitTest --tests "*CallStringsTest" lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 6-06-01 | 06 | 3 | DIA-02 | build + lint (canal e tela cheia) | `./gradlew assembleDebug lint detekt --rerun-tasks` | ❌ criado pela task | ⬜ pending |
| 6-06-02 | 06 | 3 | DIA-02 | Robolectric `sdk=[35]` **e** `[29]` (notificacoes sombreadas) | `./gradlew testDebugUnitTest --tests "*IncomingCallNotifierTest" --rerun-tasks` — >= 9 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-06-03 | 06 | 3 | DIA-02 | build + suite + script (fiacao preguicosa) | `./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt && bash scripts/verify-invariants.sh` | ✅ estender | ⬜ pending |
| 6-07-01 | 07 | 4 | QLT-06, DIA-01 | instrumentado (`ServiceTestRule` + concessao real do papel) | `bash scripts/run-instrumented-tests.sh --tests "*InCallServiceBindTest"` — >= 6 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-07-02 | 07 | 4 | **DIA-04** | instrumentado (agenda real + coordenador real do container) | `bash scripts/run-instrumented-tests.sh --tests "*DialerScreeningIntegrationTest"` — >= 6 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-07-03 | 07 | 4 | DIA-05 | instrumentado (reversao + morte no meio da chamada) | `bash scripts/run-instrumented-tests.sh --tests "*DialerRoleReversionTest"` — >= 7 `@Test` | ❌ criado pela task | ⬜ pending |
| 6-08-01 | 08 | 5 | DIA-05 | greps de honestidade + script | `bash scripts/verify-invariants.sh && ./gradlew testDebugUnitTest --tests "*CallStringsTest" --rerun-tasks` | ✅ estender docs | ⬜ pending |
| 6-08-02 | 08 | 5 | DIA-05 | grep de completude dos cenarios fisicos | `[ "$(grep -cE '^\| (5[2-9]\|60) \|' docs/TESTE-FISICO-SAMSUNG.md)" -eq 9 ]` | ✅ estender | ⬜ pending |
| 6-08-03 | 08 | 5 | QLT-06 | gate de cobertura + evidencia pos-`clean` | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` | ❌ `06-EVIDENCE.md` criado pela task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Cobertura requisito → task

| Requirement | Coberto por |
|-------------|-------------|
| **DIA-01** | 6-03-01 (matriz de permissoes + lista autorizada), 6-03-02 (manifest minimo medido: servico de chamada declarado e os DOIS filtros de discagem), 6-03-03 (papel: disponibilidade, posse, pedido; estado do modo vindo do sistema e nunca de valor gravado), 6-05-03 (texto de ativacao honesto com o que muda e o que NAO muda), 6-07-01 (papel concedido de verdade pelo caminho que verifica elegibilidade) |
| **DIA-02** | 6-01-01 (mapa exaustivo de 13 estados, sem ramo mudo), 6-01-02 (atender/recusar/encerrar/mudo/viva-voz/teclado na costura + pareamento de tom), 6-01-03 (falhar alto + prazo de apresentacao), 6-02-01/02/03 (tema, strings, componentes com alvo >= 48dp), 6-03-02 (servico fino, observador registrado e removido, ramo de recusa por nivel de API), 6-04-01/02/03 (as tres telas + tons + rota + semantica + fronteira do numero), 6-06-01/02/03 (tela cheia oficial, canal de importancia alta, degradacao) |
| **DIA-03** | 6-05-01 (origem pelo gerenciador de telecomunicacoes, nunca por acao direta de ligar; permissao pedida em runtime), 6-05-02 (tela de discagem nos dois contextos de abertura), 6-03-02 (os dois filtros de discagem no manifest) |
| **DIA-04** | 6-07-02 — **provado, nao implementado.** Segurar o papel de telefone padrao faz o sistema vincular a triagem (medido); a regra de politica por contato existe no motor desde a Fase 2 com 48 casos parametrizados. O criterio de aceite inclui diferenca vazia no arquivo do motor em toda a fase |
| **DIA-05** | 6-03-03 (estado "papel perdido" informativo; reversao pelo seletor do sistema; proibicao permanente de desabilitar componente proprio, travada no Bloco 8), 6-07-03 (reversao real: detentor volta ao nativo, papel de triagem sobrevive, modo filtro continua triando; morte no meio da chamada nao derruba a chamada), 6-08-01 (limitacoes documentadas) |
| **QLT-06** | 6-07-01 (vinculo real do servico de chamada), 6-07-02, 6-07-03 (fluxo minimo do servico de chamada exercitado no aparelho virtual), 6-08-03 (suite instrumentada verde pos-`clean`) |

Nenhum requisito da fase fica sem task. Nenhuma task fica sem `<automated>`.

**Requisito que NAO ganha implementacao:** DIA-04. Planejar codigo novo para ele — em especial ramo
novo no motor de decisao — seria erro de desenho, e a diferenca vazia do arquivo do motor e criterio
de aceite explicito do plano 06-07.

---

## Wave 0 Requirements

Infraestrutura que **bloqueia** as tasks seguintes, concentrada na wave 1 (planos 06-01 e 06-02, que
nao compartilham nenhum arquivo e rodam em paralelo):

- [ ] `telecom/call/CallUiState.kt` + `CallStateMapper.kt` (06-01) — todo o resto da fase consome
      estes tipos; sem eles nada da wave 2 compila
- [ ] `telecom/call/CallControls.kt` (06-01) — a costura onde mudo e viva-voz podem ser provados;
      **e o antidoto da armadilha de teste vacuoso medida na pesquisa**
- [ ] `telecom/call/CallSessionCoordinator.kt` (06-01) — maquina de estado pura com prazo de
      apresentacao; bloqueia 06-03, 06-04 e 06-06
- [ ] `ui/theme/Type.kt` com os estilos numericos e `Color.kt` com as cores funcionais fixas (06-02)
      — as telas dos planos 06-04 e 06-05 os consomem
- [ ] `ui/theme/Shape.kt` (06-02) — novo arquivo
- [ ] As 46 strings pt-BR em `res/values/strings.xml` (06-02) — nenhuma tela pode ser escrita antes,
      porque texto embutido em Kotlin e proibido
- [ ] `ui/dialer/DialpadGrid.kt` e `DialpadKey.kt` (06-02) — grade compartilhada pelo teclado de tons
      (06-04) e pela tela de discagem (06-05); ficar num plano so evita conflito de arquivo
- [ ] `app/src/test/.../ui/CallStringsTest.kt` (06-02) — a varredura de honestidade que os planos
      06-05 e 06-08 reexecutam
- [ ] Instalacao de framework: **nenhuma**

**Nao e Wave 0, deliberadamente:**

- O **Bloco 8** de `scripts/verify-invariants.sh` fica no plano **06-03**: ele aponta para arquivos que
  so ganham conteudo naquele plano. Liga-lo antes deixaria o script vermelho sem defeito real —
  precedente das Fases 3, 4 e 5.
- O manifest fica no plano **06-03**, junto do servico e das Activities que ele referencia: declarar
  componente que nao existe quebra o build. A matriz de permissoes e editada na task ANTERIOR do mesmo
  plano, e a lista autorizada do script entra no mesmo trabalho.
- `koverVerify` e qualquer exclude ficam no **ultimo** plano (**06-08**); ate la, `./gradlew koverLog`.
- As correcoes de documentacao ficam em **06-08** porque so fazem sentido depois que os testes
  instrumentados dizem o que de fato foi verificado — mas sao **trabalho de fase**, nao faxina.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Viva-voz e roteamento de audio real | DIA-02 (criterio 2) | O aparelho virtual expoe **somente** a rota de alto-falante na mascara de rotas suportadas — medido. Nao existe fone nem receptor para alternar | Cenario **52**. **Unico ponto de DIA-02 impossivel de automatizar.** Deferred, nao gap |
| Tela de chamada sobre a tela bloqueada do fabricante | DIA-02 | O mecanismo de tela cheia esta confirmado e testado; a experiencia real na interface do fabricante nao e observavel em emulador sem tela | Cenario **53** |
| Degradacao com a permissao de tela cheia revogada, em aparelho real | DIA-02 | A degradacao E testada em JVM (6-06-02); o que falta e a percepcao do usuario | Cenario **54** |
| Morte no meio da chamada em aparelho real | DIA-05 | Reproduzida no aparelho virtual (6-07-03); confirmar no aparelho e validacao de campo | Cenario **55** |
| Dois chips e estado de escolha de conta de telefone | DIA-02 | Exige dois chips fisicos. O estado tem tela informativa com encerrar funcional, provado em JVM (6-01-01) | Cenario **56** |
| Papel tomado por atualizacao do sistema ou por outro discador | DIA-01, DIA-05 | Exige atualizacao real do sistema ou segundo aplicativo de telefone | Cenario **57** |
| Otimizacao de bateria agressiva do fabricante matando o servico de chamada | DIA-02 | Comportamento exclusivo do fabricante. **Nenhum ajuste preventivo entra no codigo** antes de falhar ali | Cenario **58** |
| Numero privado/restrito no modo discador | SCR-04 (parcial) | **Questao aberta.** Nao se sabe se a triagem passa a receber a chamada sem identificacao por segurarmos o papel, ou se apenas a interface de chamada a ve — tarde demais para bloquear. O console do aparelho virtual nao simula apresentacao restrita de forma confiavel | Cenario **59**. Ate haver resultado, `docs/LIMITACOES.md` para de AFIRMAR que destrava (plano 06-08) e a interface nao promete nada |
| Historico do fabricante com o modo discador ativo | DIA-01 (honestidade) | Onde a chamada bloqueada aparece na interface do fabricante. QUE ela aparece ja e certo: reconfirmado em execucao nesta pesquisa, com o papel de telefone padrao ativo | Cenario **60** |
| Telas de onboarding e de Protecao que levam ao modo discador | DIA-01 | **Nao e manual: e outra fase.** O ponto de entrada em Protecao e da Phase 7 por desenho. Esta fase entrega a tela de ativacao/reversao, o estado do modo e as strings | O verifier trata como **deferred to Phase 7**, nunca como gap |

**Nao entra nesta tabela:** conceder o papel de telefone padrao, originar chamada, matar o processo no
meio da chamada e reverter — a pesquisa mediu que o aparelho virtual reproduz TODOS esses, e eles
viraram teste instrumentado no plano 06-07. Isso falsifica a premissa registrada no CONTEXT de que o
emulador nao reproduz a troca de discador padrao. **Emulador que nao sobe e blocker reportado no
SUMMARY, nunca troca silenciosa por teste em JVM.**

**Nenhum plano desta fase emite `checkpoint:human-action` ou `checkpoint:human-verify`** — politica de
validacao fisica do ROADMAP, decidida em 2026-07-28.

---

## Provas de vermelho obrigatorias (15)

| # | Plano | Guarda-corpo | Como quebrar |
|---|-------|--------------|--------------|
| 1 | 06-01 | Mapa exaustivo de estados | Trocar o ramo final por um estado de chamada encerrada |
| 2 | 06-01 | Pareamento do tom do teclado | Remover o encerramento do tom pendente antes de iniciar o novo |
| 3 | 06-01 | Falhar alto no caminho da chamada | Envolver um comando da costura em captura de excecao |
| 4 | 06-01 | Prazo de apresentacao | Nunca armar o prazo para chamada recebida |
| 5 | 06-02 | Cores funcionais fixas | Alterar um digito da cor de atender |
| 6 | 06-02 | Varredura de honestidade das strings | Acrescentar string com promessa de bloqueio total |
| 7 | 06-03 | Lista autorizada de permissoes | Declarar no manifest uma permissao fora da matriz |
| 8 | 06-03 | Bloco 8.1 — dois filtros de discagem | Remover o filtro com esquema de telefone |
| 9 | 06-03 | Bloco 8.2 — nunca desabilitar componente proprio | Acrescentar a chamada proibida num arquivo de producao |
| 10 | 06-03 | Bloco 8.3 — origem pelo gerenciador de telecomunicacoes | Acrescentar a acao direta de ligar num arquivo de producao |
| 11 | 06-04 | Fronteira do numero: mascarado fora da tela | Fazer a camada de notificacao receber o numero sem mascara |
| 12 | 06-04 | Alvo de toque minimo | Reduzir um controle secundario para 40dp |
| 13 | 06-05 | Permissao de originar chamada respeitada | Fazer o originador ignorar o estado da permissao |
| 14 | 06-06 | Importancia do canal de chamada | Baixar a importancia do canal novo |
| 15 | 06-06 | Degradacao sem tela cheia | Fazer a degradacao publicar sem acoes |

Cada prova e quebrada, vista falhar e restaurada, com a saida transcrita no SUMMARY do plano.

---

## Validation Sign-Off

- [x] Todas as tasks tem `<automated>` verify ou dependencia de Wave 0 declarada
- [x] Continuidade de amostragem: nenhuma sequencia de 3 tasks sem verificacao automatizada
- [x] Wave 0 cobre todos os arquivos que bloqueiam waves seguintes
- [x] Nenhum comando em modo de observacao
- [x] Latencia de feedback < 50 s no comando rapido
- [x] `nyquist_compliant: true` no frontmatter

**Approval:** approved 2026-07-29

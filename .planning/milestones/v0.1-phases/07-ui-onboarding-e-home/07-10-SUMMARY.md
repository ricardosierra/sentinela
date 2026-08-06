---
phase: 07-ui-onboarding-e-home
plan: 10
subsystem: ui-navigation
tags: [navegacao, onboarding, home, protecao, modo-discador, invariantes, acessibilidade]

# Dependency graph
requires:
  - phase: 07-02
    provides: as dez rotas por TEXTO e o contrato do grafo que COMPOE o NavHost
  - phase: 07-04
    provides: os tres donos de estado com colaboradores um por um (container fica na rota)
  - phase: 07-05
    provides: boas-vindas, passo 1, passo 2 e rememberStepTransitionMillis
  - phase: 07-06
    provides: passos 3 e 4
  - phase: 07-07
    provides: passos 5 e 6
  - phase: 07-08
    provides: a home com os oito estados degradados
  - phase: 07-09
    provides: a tela Protecao com os 16 itens e as duas confirmacoes
  - phase: 06-05
    provides: DialerActivationScreen com os cinco ramos, ate agora sem ponto de entrada
provides:
  - "SentinelaNavHost — o grafo com os dez destinos por texto e a transicao de passo suprimivel"
  - "WelcomeRoute, OnboardingRoute, HomeRoute, SettingsRoute, DialerActivationRoute — as unicas camadas que conhecem o container"
  - "PassoDoOnboarding + AcoesDoPasso — desvio de passo puro, componivel em JVM sem container"
  - "MarcasDePermissao, estadoAtual, irParaHome, DestinoEmPreparacao — fiacao compartilhada das rotas"
  - "MainActivity hospedando o grafo com destino inicial resolvido sem bloquear a thread principal"
  - "Bloco 9 de verify-invariants.sh — texto embutido, fronteira do numero e repositorio da agenda"
  - "HomeViewModel: definirProtecao, religarHistorico, tentarLerNovamente, pedirAgenda"
affects:
  - "07-11 — o fechamento da fase mede cobertura e fecha UIX-07 sobre codigo que agora esta ligado"
  - "Phase 8 e Phase 9 — cada uma acrescenta destino e mexe na contagem travada do grafo"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Camada de rota fina conhece o container; composta de tela nunca conhece"
    - "A pilha de navegacao e a unica fonte da verdade do passo do onboarding"
    - "Destino inicial resolvido ANTES de compor o grafo, com estado de espera explicito"
    - "Desvio de passo extraido da fiacao para o teste compor o codigo REAL de producao"
    - "Destino de fase futura vira aviso honesto, nunca tela em branco nem controle inerte"

key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/navigation/SentinelaNavHost.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/OnboardingRoute.kt
    - app/src/main/java/org/sentinela/app/ui/home/HomeRoute.kt
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsRoute.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialerActivationRoute.kt
    - app/src/test/java/org/sentinela/app/ui/onboarding/OnboardingFlowTest.kt
    - app/src/test/java/org/sentinela/app/ui/settings/DialerRouteTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/MainActivity.kt
    - app/src/main/java/org/sentinela/app/ui/home/HomeViewModel.kt
    - scripts/verify-invariants.sh
    - .planning/phases/07-ui-onboarding-e-home/deferred-items.md

key-decisions:
  - "A pilha de navegacao e a fonte da verdade do passo, nao o contador do dono de estado: quem responde ao gesto de voltar e a pilha, e duas contagens vivas divergiriam no primeiro retorno"
  - "Os dez destinos escritos um por um, sem laco: a contagem e ponto de revisao de navegacao e um laco a esconderia de quem le e de quem verifica de fora"
  - "Desvio de passo extraido para PassoDoOnboarding + AcoesDoPasso — e o que permite ao teste de fluxo compor o codigo de producao sem container"
  - "Destino inicial por produceState, com espera anunciada: trocar startDestination depois do grafo composto NAO re-navega"
  - "Bloco 9.2 exclui os donos de estado por desenho de 07-04 — e neles que a mascara e aplicada, e o tipo publicado nao tem campo para digitos"
  - "Whitelist, historico e privacidade/sobre nao ganham destino: aviso honesto pela frase que ja existe em recurso, contagem do grafo intacta em dez"
  - "Intencao de ativar o modo discador derivada da marca do pedido de originar chamada — sem chave nova, com o caminho de codigo registrado e a chave dedicada adiada"

metrics:
  duration: ~2h
  tasks: 3
  files: 11
  tests_added: 21
  tests_total_jvm: 845
  coverage: 96.6157%
  completed: 2026-07-30
---

# Phase 7 Plano 10: A Fiacao Summary

O aplicativo deixou de ser esqueleto: o grafo real com os dez destinos por texto, as cinco camadas de
rota que sao as unicas a conhecer o container, a hospedeira que resolve o destino inicial sem bloquear a
thread principal, o Bloco 9 de invariantes com tres checagens e cinco provas de vermelho, e o fluxo de
ponta a ponta provado em maquina virtual pura — incluindo a pendencia de 06-05, que fecha aqui: a tela de
ativacao do modo discador finalmente tem ponto de entrada.

## O que foi entregue

### Task 1 — as camadas de rota e o grafo (`e474637`)

**`SentinelaNavHost`** com os **dez** destinos, todos por rota de TEXTO vinda das constantes de 07-02, e
escritos **um por um, sem laco**: a contagem de destinos e ponto de revisao de navegacao nesta base de
codigo, e um laco a esconderia de quem le o arquivo e de quem o verifica de fora. `NavGraphContractTest`
continua verde com a contagem travada em dez.

A transicao entre passos vive no grafo, como animacao de destino: deslizamento horizontal mais
dissolucao, espelhada no retorno, com a duracao vinda de `rememberStepTransitionMillis()` — **a pendencia
que 07-05 deixou registrada, e que ate agora nenhuma rota consumia.** Reducao de movimento ligada devolve
zero: supressao, nao animacao mais curta.

**Cinco camadas de rota**, e a regra que as mantem finas e o que sustenta a fase inteira: *composta de
tela recebe estado e retornos de chamada e nunca recebe container; so a rota conhece o container.*

- **`WelcomeRoute`** — a unica sem dono de estado, porque a tela de boas-vindas nao consulta nada.
- **`OnboardingRoute`** — monta o dono de estado pela fabrica, os dois lancadores de permissao, o seletor
  de papel (cujo retorno **apenas reconsulta**), a reconsulta na retomada, e despacha os seis passos.
- **`HomeRoute`** — dono de estado, reconsulta na retomada mais reconsulta no retorno do seletor
  (redundancia deliberada, no molde das duas redes da Fase 5), e o botao de conserto que so dispara
  quando a intencao de pedido nao e nula.
- **`SettingsRoute`** — os dezenove retornos de chamada da tela Protecao ligados, e os **dois** papeis
  reconsultados a cada retomada.
- **`DialerActivationRoute`** — **fecha a pendencia de 06-05.** A tela nao foi tocada: `git diff` do
  arquivo dela esta vazio. Reverter abre o seletor do sistema, nunca desabilita componente proprio.

O passo chega a `OnboardingRoute` como parametro, vindo do destino. **A pilha de navegacao e a unica
fonte da verdade do passo**, porque e ela quem responde ao gesto de voltar do sistema; o contador do dono
de estado continuaria existindo em paralelo e divergiria no primeiro retorno, produzindo a tela de um
passo com o cabecalho de outro.

### Task 2 — a hospedeira sem tela de espera falsa (`93739c5`)

`MainActivity` perdeu a composta de espera e ganhou o grafo. O destino inicial vem da chave
`onboarding_completed` por `produceState`, e e resolvido **antes** de compor: trocar `startDestination`
com o grafo ja composto **nao re-navega**. Enquanto a resposta nao chega, a hospedeira mostra
`state_loading` em regiao viva educada — estado de verdade, nao tela em branco.

Zero bloqueio da thread principal, e as duas razoes apontam para a mesma linha: a primeira leitura do
repositorio custa disco (10,9 ms medidos na Fase 3) e bloquear a partida a frio e o jeito mais provavel
de estragar o orcamento que o projeto protege desde a Fase 1.

A guarda `savedInstanceState == null` em torno da contagem de abertura **permanece**: inicio de processo
pelo sistema de telecomunicacoes nao e abertura, e rotacao nao e abertura nova.

O KDoc registra em prosa portuguesa, por importacao e nunca por nome totalmente qualificado, que o
identificador do aplicativo e o nome visivel jamais aparecem como literal em Kotlin.

### Task 3 — Bloco 9 e o fluxo de ponta a ponta (`183c578`)

**Bloco 9** com tres checagens, restritas as pastas `ui/onboarding`, `ui/home` e `ui/settings`:

- **9.1** — nenhum texto de interface em Kotlin. O padrao casa aspas com duas ou mais letras seguidas de
  espaco (assinatura de frase; nome de chave e anotacao de supressao nao tem espaco dentro das aspas) e
  exclui linha de comentario e de documentacao **antes** da contagem.
- **9.2** — a fronteira do numero: nenhuma tela enxerga o campo do numero em formato internacional. Os
  donos de estado ficam fora do escopo por desenho de 07-04 — e neles que a mascara e aplicada, e o tipo
  publicado nao tem campo para digitos.
- **9.3** — nenhuma tela cita o repositorio de consulta da agenda (cache medido em 2,57 s).

Aritmetica na forma obrigatoria: `[ -z "$VAR" ]` sobre a saida, sem `set -e` e sem `|| echo 0` — ambos
continuam em **zero** ocorrencias no arquivo.

**`OnboardingFlowTest`** (15 casos) compõe o grafo com as rotas reais, a lista de passos de producao, o
**desvio de passo de producao** e o descarte inclusivo de producao. Para isso o desvio foi extraido da
fiacao para `PassoDoOnboarding` + `AcoesDoPasso`: o teste exercita o codigo real em vez de reimplementa-lo,
e sem container.

**`DialerRouteTest`** (6 casos) prova o ponto de entrada nos cinco ramos: quatro navegam, e o
indisponivel e anunciado desabilitado e **nao** navega.

## Provas de vermelho

Todas sabotaram producao **ja commitada** e foram restauradas por edicao manual, jamais por descarte de
arquivo (licao de 06-02). Ao final, `git status` mostrava apenas os arquivos do proprio trabalho.

### Prova 1 — 9.1, texto de interface embutido

Constante com frase de interface em `HomeScreen.kt`:

```
      app/src/main/java/org/sentinela/app/ui/home/HomeScreen.kt:113:private const val SABOTAGEM_9_1 = "Nenhum bloqueio hoje"
FAIL: texto de interface escrito em Kotlin — toda frase vive em res/values/strings.xml, em pt-BR
== 1 invariante(s) violado(s) ==
```

### Prova 2 — 9.2, fronteira do numero

`LastBlockedCard.kt` passou a ler o campo do numero completo do registro:

```
      app/src/main/java/org/sentinela/app/ui/home/LastBlockedCard.kt:4:    { registro -> registro.numberE164 }
FAIL: tela da fase referencia o campo do numero completo — a exibicao passa somente pela mascara unica aplicada no dono de estado
== 2 invariante(s) violado(s) ==
```

**Descoberta:** foram DOIS invariantes, e nao um. A sabotagem usou o nome totalmente qualificado do tipo
do registro, e o **Bloco 2 pegou o identificador do aplicativo literal em Kotlin no mesmo instante** —
exatamente a armadilha que o contexto deste plano avisou ter derrubado quatro executores. Ela funciona.

### Prova 3 — 9.3, repositorio da agenda numa rota da fase

```
      app/src/main/java/org/sentinela/app/ui/home/HomeRoute.kt:58:    val sabotagem93 = container.contactLookupRepository
FAIL: tela da fase cita o repositorio de consulta da agenda — ele constroi cache medido em mais de dois segundos e meio e nenhuma tela desta fase precisa dele
== 1 invariante(s) violado(s) ==
```

### Prova 4 — a pilha sem o descarte inclusivo

Removido `popUpTo(Rotas.BOAS_VINDAS) { inclusive = true }` da funcao de producao `irParaHome()`:

```
15 tests completed, 8 failed
  o caminho inteiro e percorrivel tocando somente em nos com texto de recurso FAILED
  pular no passo 1..5 leva a home com a pilha de um unico elemento FAILED  (cinco casos)
  concluir no ultimo passo deixa a pilha com um unico elemento FAILED
  o gesto de voltar na home nao devolve o usuario ao onboarding FAILED
```

Oito casos vermelhos, incluindo o do retorno que o produto proibe. Restaurado: 21 de 21 verdes.

### Prova 5 — nao auto-sabotagem, verificada

Com **todos** os comentarios e todo o KDoc desta fase no lugar — inclusive os que descrevem em prosa
portuguesa exatamente o que o Bloco 9 procura, e inclusive a documentacao nova das cinco rotas:

```
== Bloco 9: interface sem texto embutido, sem numero e sem consulta a agenda ==
ok:   nenhum texto de interface embutido em Kotlin nas telas da fase
ok:   nenhuma tela da fase enxerga o numero completo do registro de bloqueio
ok:   nenhuma tela da fase cita o repositorio de consulta da agenda
== todos os invariantes OK ==
```

O criterio nao casa a propria prosa que o descreve. A exclusao de linha de comentario **antes** da
contagem e o que garante isso, e ela e a diferenca entre este bloco e as seis auto-sabotagens que as
Fases 3, 5, 6 e 7 registraram.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] `HomeViewModel` ganhou os quatro comandos que a home exigia**

- **Encontrado em:** Task 1
- **Problema:** a tela da home (07-08) expoe interruptor de protecao, botao de ativar historico, botao de
  tentar de novo e botao de conceder agenda. **Nenhum deles tinha funcao no dono de estado** — ligados a
  lambdas vazias, seriam quatro controles inertes, e o usuario conclui que o aplicativo travou.
- **Correcao:** `definirProtecao`, `religarHistorico`, `tentarLerNovamente` e `pedirAgenda`, todas na
  forma que o projeto ja usa. Duas merecem nota:
  - **`religarHistorico` mexe em DUAS configuracoes.** O interruptor do historico e a retencao "nao
    guardar" desligam o historico de forma independente, e a segunda mantem o interruptor ligado sem
    gravar nada. Ligar so o interruptor deixaria o aviso reaparecer no quadro seguinte. A retencao volta
    ao padrao do produto **somente** quando a que estava valendo nao guardava. Nada e apagado.
  - **`tentarLerNovamente` REINSCREVE o fluxo de origem** (contador de tentativa mais `flatMapLatest`),
    passando por carregando antes do novo resultado. Um botao de tentar de novo que apenas recalcula o
    mesmo resultado devolve a mesma falha e ensina o usuario a desconfiar do aplicativo.
- **Arquivo:** `ui/home/HomeViewModel.kt` — **`@Suppress("TooManyFunctions")` local**, no ponto de uso,
  nunca no `detekt.yml` compartilhado (precedente das Fases 3 e 6 e do repositorio de configuracoes).
- **Commit:** `e474637`

**2. [Rule 3 - Blocking] Whitelist, historico e privacidade/sobre viraram aviso, nao destino**

- **Encontrado em:** Task 1
- **Problema:** o plano pede composta de espera para esses destinos **e** exige `composable(` igual a
  dez, com a contagem do grafo travada em dez desde 07-02. As duas coisas nao cabem juntas: nenhuma
  dessas telas tem constante de rota, e cria-las levaria o grafo a treze destinos.
- **Correcao:** os dez destinos permanecem, e os tres atalhos abrem `DestinoEmPreparacao`, que diz com a
  frase que **ja existe em recurso** que a tela chega numa etapa seguinte. Os dois atalhos das listas ja
  nasciam anunciados como indisponiveis na home; a linha de privacidade e sobre, porem, e ATIVA na tela
  Protecao, e um toque sem efeito ali seria defeito silencioso.
- **Por que nao foi resolvido desabilitando a linha:** `ProtectionScreenTest` afirma que ela e clicavel e
  invoca o retorno de chamada. Desabilita-la sabotaria trabalho ja commitado e verde.
- **Registrado em:** `deferred-items.md`, com dono nomeado (Phase 8 e Phase 9).
- **Commit:** `e474637`

**3. [Rule 3 - Blocking] Intencao de ativar o modo discador sem chave nova**

- **Encontrado em:** Task 1
- **Problema:** a funcao de precedencia da Fase 6 precisa da intencao gravada do usuario para produzir o
  estado de **papel perdido**; sem ela, aparelho que perdeu o papel seria rotulado como apenas
  **oferecido**. Nao existe chave persistida, e 07-09 confirmou que nenhuma era necessaria para a tela.
- **Correcao:** o sinal e derivado da marca do pedido da permissao de originar chamada, cujo unico
  caminho de disparo e o toque em ligar da tela de discagem propria — que o sistema so encaminha a este
  aplicativo quando ele detem o papel de telefone padrao. A derivacao esta registrada em prosa no ponto
  de uso, e a chave dedicada ficou em `deferred-items.md`.
- **Commit:** `e474637`

**4. [Rule 3 - Blocking] `grep -c 'LifecycleResumeEffect'` igual a 1 e inatingivel**

- **Encontrado em:** Task 1
- **Problema:** com a importacao mais a chamada sao inevitavelmente **duas** linhas, porque `grep -c`
  conta LINHAS. Mesma classe de defeito de criterio que 07-04 encontrou com o container, e a saida obvia
  — nome totalmente qualificado — e feia e sem ganho.
- **Correcao:** a intencao do criterio (uma reconsulta por rota) foi verificada excluindo as linhas de
  importacao: **1 em cada uma das quatro rotas**, incluindo a do modo discador, que o criterio nem pedia.
- **Commit:** `e474637`

**5. [Rule 3 - Blocking] `performScrollTo` reprova controle de rodape fixo**

- **Encontrado em:** Task 3
- **Problema:** 14 de 15 casos ficaram vermelhos com `Semantic Node has no parent layout with a Scroll
  SemanticsAction`. Parte dos controles da fase vive em rodape fixo, fora de area rolavel.
- **Correcao:** a rolagem passou a ser tentativa tolerada; **o toque continua obrigatorio**, entao no
  ausente ou nao clicavel segue vermelho. Registrado em prosa no proprio auxiliar.
- **Commit:** `183c578`

**6. [Rule 1 - Bug no teste] O gesto de voltar na home esvazia a pilha, e isso e o comportamento certo**

- **Encontrado em:** Task 3
- **Problema:** o caso afirmava que a home continuava sendo o destino corrente depois do gesto de voltar.
  Medido: com a pilha de um unico elemento, o destino corrente fica **nulo** — o sistema encerra a tela.
- **Correcao:** o caso passou a afirmar o que o produto exige, que e mais forte: nem a pilha nem o destino
  corrente podem ser de onboarding depois do gesto. Sair do aplicativo e o comportamento correto; reabrir
  o fluxo que o usuario ja respondeu e o proibido.
- **Commit:** `183c578`

**7. [Rule 3 - Blocking] Duas supressoes locais do detekt**

- **Encontrado em:** verificacao final
- **Problema:** `AcoesDoPasso` tem nove parametros (limite 7) e `comporGrafo` do teste tem 71 linhas
  (limite 60) — os dez destinos do grafo nao cabem em 60 linhas.
- **Correcao:** `@Suppress` local nos dois pontos de uso, nunca no `detekt.yml` compartilhado.
- **Commit:** `183c578`

## Verificacao final

```
./gradlew --no-build-cache assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
./gradlew detekt lint                                                    BUILD SUCCESSFUL (zero issues)
bash scripts/verify-invariants.sh                                        todos os invariantes OK (9 blocos)
./gradlew koverLog                                                       96,6157%
./gradlew koverVerify                                                    BUILD SUCCESSFUL (piso 80)
```

- **845 casos JVM**, zero falhas (eram 824 no fecho de 07-09); **21 novos** neste plano (15 + 6).
- `grep -c 'composable('` no grafo: **10**. `grep -rciE 'serializable'` na pasta de navegacao: **0**.
- `grep -rc 'runBlocking'` e `grep -rc 'contactLookupRepository'` em `ui/`: **0** em todo arquivo.
- `git diff --stat` de `DialerActivationScreen.kt`: **vazio** — a tela de 06-05 nao foi tocada.
- `git diff --stat` de `CallDecisionEngine.kt`: **vazio** — o motor segue intocado.
- `git diff --stat` de `app/build.gradle.kts`: **vazio** — 07-11 e o dono, e o filtro do Kover nao foi
  tocado.
- Nenhuma permissao nova, nenhuma biblioteca nova, nenhuma chave nova em `strings.xml`.
- Cobertura identica a de 07-04 porque tudo o que esta fase escreveu vive em `ui.*`, fora do filtro do
  Kover.

## Requisitos

`SCR-01`, `UIX-01` e `UIX-03` **ja estavam marcados** por trabalho anterior; este plano os torna
alcancaveis pelo usuario, mas nao altera o estado deles.

**`UIX-07` fica PENDENTE de proposito.** Ele exige *todas* as strings em recurso, e o Bloco 9.1 vigia
tres pastas — as desta fase. As pastas de chamada, de discagem e de componentes compartilhados nao estao
no escopo dele, e marcar o requisito agora seria o estado falsamente positivo que 07-01 e 07-07 ja
recusaram duas vezes nesta fase. Ele fecha em 07-11, que e o plano de fechamento.

## Notas para o plano seguinte

- Tela nova exige mexer em `DESTINOS_ESPERADOS` **e** na contagem de `composable(`, de proposito.
- `PassoDoOnboarding` + `AcoesDoPasso` e o ponto de entrada para qualquer teste que precise compor o
  onboarding real sem container.
- O aviso de destino em preparacao tem dois donos futuros nomeados em `deferred-items.md`.

## Self-Check: PASSED

Os sete arquivos criados e os quatro modificados existem no disco. Os quatro commits deste plano
(`e474637`, `93739c5`, `183c578`, mais o de itens adiados) estao no historico. Nenhuma sabotagem das
cinco provas permaneceu na arvore.

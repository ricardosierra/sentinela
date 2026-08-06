---
phase: 07-ui-onboarding-e-home
plan: 03
subsystem: ui-components
tags: [componentes, acessibilidade, semantica-mesclada, alvo-de-toque]
requires:
  - "asserts de alvo de toque em dois eixos de 07-02"
  - "cores funcionais de estado e as 269 chaves pt-BR de 07-01"
provides:
  - "os seis componentes compartilhados das cinco telas de onboarding, da home e da Protecao"
  - "quatro dos cinco pontos de risco de semantica mesclada cobertos por caso vermelho demonstrado"
  - "modificador de agrupamento selecionavel dos cartoes de opcao"
affects:
  - "todas as telas da fase (07-05 a 07-09) consomem estes componentes sem reimplementar nenhum"
tech-stack:
  added: []
  patterns:
    - "o componente compartilhado E o controle: estado no proprio no, nunca num ancestral que mescla"
    - "altura EXIGIDA em contrato de alvo minimo, nunca altura negociavel"
    - "estado sempre por papel e texto, jamais so por cor"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/components/OptionCard.kt
    - app/src/main/java/org/sentinela/app/ui/components/StepHeader.kt
    - app/src/main/java/org/sentinela/app/ui/components/SentinelaTopBar.kt
    - app/src/main/java/org/sentinela/app/ui/components/SettingSwitchRow.kt
    - app/src/main/java/org/sentinela/app/ui/components/CheckRow.kt
    - app/src/main/java/org/sentinela/app/ui/components/SentinelaBottomBar.kt
    - app/src/test/java/org/sentinela/app/ui/components/Phase7ComponentSemanticsTest.kt
  modified:
    - app/src/main/res/values/strings.xml
decisions:
  - "envolver o controle com container que mescla NAO derruba o estado por si; o que derruba e DECLARAR o estado no container — medido nas duas direcoes"
  - "a barra inferior entrega os quatro destinos, com os dois da Phase 8 desabilitados e com motivo textual"
  - "requiredSizeIn antes de requiredHeight: a ordem inversa devolveu 48dp desenhados onde o contrato pedia 56dp"
metrics:
  duration: ~75min
  tasks: 3
  files: 8
  completed: 2026-07-30
---

# Phase 7 Plan 03: Componentes compartilhados da fase Summary

Os seis componentes que as cinco telas de onboarding, a home e a Protecao consomem nasceram com a
semantica de acessibilidade correta e com 17 casos que travam os dois eixos de toque, os papeis e a
descricao de estado — e a armadilha da semantica mesclada foi reproduzida aqui nas DUAS direcoes,
o que corrigiu o entendimento que a fase tinha dela.

## O que foi feito

### Task 1 — cartao de opcao e cabecalho de passo (`89aeba7`)

`OptionCard` com a assinatura completa do plano. A linha inteira e um alvo unico por
`Modifier.selectable(role = Role.RadioButton)`, altura minima de 72dp, padding 16dp, forma media;
icone-container circular de 40dp, 16dp de folga, coluna de titulo (`titleMedium` `OnSurface`) e
descricao PERMANENTE (`bodyMedium` `OnSurfaceVariant`), peso 1, e o icone de confirmacao de 24dp que
OCUPA espaco com alfa 0 quando nao selecionado — o layout nao pula no instante da escolha. Estado
selecionado: fundo mais alto, borda 2dp `Primary`, icone-container `SecondaryContainer`, confirmacao
preenchida. Pressionado: ondulacao padrao mais escala 0,98 em 100 ms, suprimida quando a reducao de
movimento esta ligada (reusa `rememberMotionReduced` da Fase 6, sem duplicar).

Nenhum texto nasce dentro do componente: titulo, descricao, selo e motivo de indisponibilidade
chegam por parametro. O `grep` de literal de interface no arquivo devolve zero fora de KDoc.

O desabilitado vai no modificador do PROPRIO no e o motivo vai como descricao de estado do mesmo no.
O KDoc registra em portugues as duas consequencias medidas na Fase 6: controle interativo nunca pode
ser filho do cartao, e estado declarado num ancestral que mescla nao e visto por quem consulta.

`StepHeader` com o contador vindo de `onboarding_step_indicator` formatado com os dois argumentos e a
barra de 4dp por 96dp declarada DECORATIVA por `clearAndSetSemantics {}`. O total chega por parametro
(as telas passam 6) para que a decisao do contador unico fique visivel na tela, e nao escondida no
componente.

### Task 2 — barra superior, linha de interruptor e linha de verificacao (`8d1cd06`)

`SentinelaTopBar` de 64dp: escudo de 28dp decorativo mais o nome do aplicativo vindo do RECURSO —
invariante de rebranding — num no unico de leitura, slot central para o cabecalho de passo e slot de
acoes. Duas acoes prontas (`SentinelaTopBarTextAction` para o pular, `SentinelaTopBarIconAction` para
o icone), a segunda com alvo EXIGIDO de 48dp por 48dp.

`SettingSwitchRow` com altura minima de 56dp e TRES nos de leitura — rotulo, interruptor e explicacao
permanente. `grep -c mergeDescendants` no arquivo devolve **0**. O interruptor recebe `Role.Switch` e
descricao de estado explicita; desabilitado, a descricao passa a ser o motivo. O KDoc registra os dois
motivos de a explicacao ser IRMA e nunca filha: mesclada, a linha perde o estado do filho, e a leitura
vira um bloco unico ilegivel em vez de "rotulo, desativado" seguido da explicacao.

`CheckRow` de 56dp: confirmacao em `CallAccept` ou erro em `StatusAttention`, sempre acompanhado do
texto de estado — estado nunca so por cor. O bloco de icone e textos e um no unico anunciado como
"rotulo, estado" (por `state_label_with_value`); quando existe acao, o botao dela e no FOCAVEL
SEPARADO, irmao do no da linha, com altura exigida de 48dp.

Quatro strings novas, todas varridas pela honestidade de 07-01 sem exigir isencao: `nav_unavailable`,
`state_on`, `state_off` e `state_label_with_value`.

### Task 3 — barra inferior e o teste de semantica (`af11045` vermelho, `b11d7f3` verde)

O ciclo foi executado de verdade. O commit `af11045` traz os 17 casos junto de uma barra inferior
PROVISORIA — sem papel de aba, sem alvo garantido e sem motivo textual — e a suite fecha em
**17 casos, 3 falhas**, exatamente os tres da barra:

```
a barra inferior tem quatro itens com papel de aba e um selecionado FAILED
    java.lang.AssertionError: expected:<4> but was:<0>
os destinos que ainda nao existem ficam desabilitados com motivo textual FAILED
    java.lang.AssertionError: expected:<Esta tela chega em uma etapa seguinte do aplicativo.> but was:<null>
cada item da barra inferior tem alvo de toque e tamanho desenhado acima do minimo FAILED
    java.lang.AssertionError: controle desenhado com altura de 40.0.dp, abaixo do minimo de 56.0.dp
```

`b11d7f3` entrega a barra de verdade: `Surface` `SurfaceContainer` com cantos superiores de 16dp,
`BottomBarItem` com rotulo, icone, selecao, habilitado, motivo e clique, item de 56dp por
`requiredHeight`, icone de 24dp, rotulo de 12sp elipsado em uma linha, `Role.Tab` e selecao no no do
item. Os quatro destinos aparecem; os dois da Phase 8 chegam desabilitados com o motivo em texto,
porque aba que leva a tela em branco sem explicacao esta proibida por `UIX-10`. Habilitar os dois na
Phase 8 e mudar a lista na tela que chama, sem tocar no componente.

`Phase7ComponentSemanticsTest`: Robolectric `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")`,
17 casos (minimo do plano: 14), sete usos de `assertLayoutHeightIsAtLeast` e sete de
`assertTouchHeightIsAtLeast`, sempre no mesmo caso. Os tres asserts sao IMPORTADOS de
`org.sentinela.app.ui`; nenhum foi redeclarado. Todo texto de assert vem de `context.getString`.

## Provas de vermelho

Todas sobre codigo JA COMMITADO, e todas restauradas por edicao manual. Nenhum `git checkout`.

### Prova 1 — eixo desenhado (altura do item da barra de 56dp para 40dp)

```
cada item da barra inferior tem alvo de toque e tamanho desenhado acima do minimo FAILED
    java.lang.AssertionError: controle desenhado com altura de 40.0.dp, abaixo do minimo de 56.0.dp
        at org.sentinela.app.ui.TouchTargetAssertsKt.assertLayoutHeightIsAtLeast
17 tests completed, 1 failed
```

**O eixo de toque continuou VERDE.** Os dois asserts de alvo (altura e largura) rodam ANTES do assert
do desenho no mesmo caso, e os dois passaram com o item desenhado em 40dp: o Compose expande sozinho
o alvo de toque de qualquer componente interativo ate o minimo da plataforma. Sem o segundo eixo a
suite mediria a garantia da biblioteca, e nao o nosso layout — a licao medida na Fase 6, medida de
novo aqui.

### Prova 2 — semantica mesclada, e uma correcao de entendimento

A primeira tentativa foi envolver o interruptor num container com semantica de mesclagem mantendo o
papel e a descricao de estado no PROPRIO interruptor. Resultado: **17 casos, 0 falhas — verde.** A
mesclagem por si nao derrubou nada; o no consultado seguiu carregando o estado.

A sabotagem que reproduz o defeito e mover a declaracao para o container: papel, descricao de estado e
desabilitado no container que mescla, e o interruptor sem nada.

```
linha de interruptor ligada anuncia papel de interruptor e o estado ligado FAILED
    java.lang.AssertionError: expected:<Ligado> but was:<null>
linha de interruptor desligada anuncia o estado desligado FAILED
    java.lang.AssertionError: expected:<Desligado> but was:<null>
linha de interruptor desabilitada e anunciada como desabilitada com motivo FAILED
    java.lang.AssertionError: Failed to assert the following: (is not enabled)
17 tests completed, 3 failed
```

**Descoberta desta prova, e ela refina o registro da Fase 6:** o defeito nao e a existencia do
container que mescla — e DECLARAR o estado nele. O no do proprio controle e uma fronteira de mesclagem
e continua sendo quem responde as buscas, entao o estado escrito no ancestral fica onde ninguem
consulta, exatamente como o controle desenhado em opacidade de desabilitado que seguia sendo anunciado
como habilitado. A regra pratica que sai daqui e mais precisa que a antiga: **o estado mora no no do
proprio controle, sempre.**

### Prova 3 — estado so por cor (descricao de estado removida do cartao desabilitado)

```
cartao desabilitado e anunciado como desabilitado e carrega o motivo em texto FAILED
    java.lang.AssertionError: expected:<Sem a leitura da agenda, o Sentinela nao consegue saber quem
    esta nos seus contatos: essas chamadas podem ser tratadas como desconhecidas.> but was:<null>
17 tests completed, 1 failed
```

Sem a descricao de estado, o cartao continua desabilitado e continua desenhado em opacidade reduzida —
e nada disso chega ao leitor de tela como motivo. O caso tem dentes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `LongMethod` do detekt reprovava o cartao de opcao**

- **Found during:** Task 1
- **Issue:** `OptionCard` fechou em 67 linhas e o limite compartilhado e 60.
- **Fix:** conteudo da linha extraido para `ConteudoDoCartao` privado. Nenhum `@Suppress` novo e
  nenhuma folga no `detekt.yml` compartilhado.
- **Commit:** `89aeba7`

**2. [Rule 3 - Blocking] ordem de `requiredSizeIn` e `requiredHeight` na barra inferior**

- **Found during:** Task 3
- **Issue:** com `requiredHeight(56.dp)` ANTES de `requiredSizeIn(min 48dp)`, o item media 48dp
  desenhados e o caso ficava vermelho por motivo legitimo — a restricao de minimo aplicada por
  ultimo passou a mandar no tamanho final.
- **Fix:** piso primeiro, altura exigida depois. Medido, nao deduzido.
- **Commit:** `b11d7f3`

**3. [Rule 2 - Missing critical] quatro strings de estado**

- **Found during:** Tasks 2 e 3
- **Issue:** nao havia texto para "ligado", "desligado", para o anuncio "rotulo, estado" da linha de
  verificacao, nem para o motivo dos destinos que ainda nao existem. Sem eles o estado sairia por cor
  ou por literal em Kotlin — as duas coisas proibidas.
- **Fix:** `state_on`, `state_off`, `state_label_with_value` e `nav_unavailable` no
  `res/values/strings.xml`. A varredura de 07-01 aceita as quatro sem isencao nominal, e o assert de
  contagem minima dela continua verde.
- **Commit:** `8d1cd06`

### Fora de escopo, nao corrigido

Durante a Task 3 o `detekt` acusou `TooManyFunctions` em
`app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt`, arquivo NAO commitado e
em edicao pelo plano 07-04, que roda em paralelo nesta onda. Fora do escopo deste plano por regra de
fronteira; nao foi tocado. Na verificacao final, com o arquivo ja ajustado pelo outro plano, o
`detekt` fechou verde.

Registro de ambiente, no molde de 07-01 e 07-02: a execucao concorrente apagou o XML de resultados
desta suite entre duas leituras (o diretorio de resultados e compartilhado). A contagem foi relida com
a suite reexecutada; nenhuma alteracao de codigo foi necessaria.

## Verification

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
suite JVM completa: 685 casos, 0 falhas (era 639 ao fim de 07-02; inclui os casos de 07-04)
Phase7ComponentSemanticsTest: 17 casos, 0 falhas
bash scripts/verify-invariants.sh                                    todos os invariantes OK (8 blocos)
./gradlew koverLog                                                   96,6157%
grep -c 'Role.RadioButton'  OptionCard.kt                            1
grep -c 'clearAndSetSemantics' StepHeader.kt                         2
grep -c 'Role.Switch' / 'stateDescription'  SettingSwitchRow.kt      1 / 2
grep -c 'mergeDescendants'  SettingSwitchRow.kt                      0
grep -c 'app_name'  SentinelaTopBar.kt                               1
grep -c 'requiredHeight'  SentinelaBottomBar.kt                      2
grep -c 'assertLayoutHeightIsAtLeast'  Phase7ComponentSemanticsTest  7
```

`app/build.gradle.kts` nao foi tocado e o filtro do Kover nao foi alargado — todos os arquivos desta
task vivem em `ui.*`, fora dos pacotes medidos, e nenhum exclude novo foi criado.

## Notas para os planos seguintes

- Os cinco pontos de risco de semantica mesclada do contrato: (b), (c) e (e) fechados aqui, e (d) fica
  coberto pelo mesmo padrao no CTA do passo 1. O quinto — o cartao hero da home com o interruptor
  dentro — e de 07-08, e a regra a aplicar la e a refinada pela Prova 2: o estado mora no no do
  proprio interruptor, e nunca no cartao que o envolve.
- Tela que precisar de grupo de cartoes usa `Modifier.optionCardGroup()`; ele ja declara o
  agrupamento selecionavel que o teste cobra.
- A barra inferior recebe a lista de destinos por parametro: a Phase 8 habilita os dois que faltam na
  tela que chama, e nao no componente.
- Nenhuma tela deve reimplementar cartao de opcao, linha de interruptor ou linha de verificacao. Era
  esse o motivo do plano: cinco copias multiplicariam por cinco a chance de perder um estado.

## Self-Check: PASSED

Os sete arquivos criados existem no disco e os quatro commits (`89aeba7`, `8d1cd06`, `af11045`,
`b11d7f3`) estao no historico. Os componentes no disco sao identicos ao ultimo commit: as tres
restauracoes das provas de vermelho fecharam sem diferenca residual.

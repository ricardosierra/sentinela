---
phase: 06-modo-discador-opcional
plan: 02
subsystem: ui
tags: [tema, tipografia, cor, acessibilidade, copy, compose]
requires: []
provides:
  - "tokens funcionais fixos da chamada (CallAccept/OnCallAccept/CallReject/OnCallReject)"
  - "estilos numberXl, numberLg e timer com figuras de largura fixa"
  - "formas centralizadas do tema (8/16/24dp e pilula)"
  - "74 strings pt-BR das telas de chamada, discagem e ativacao"
  - "nove componentes reutilizaveis das telas de chamada e discagem"
  - "callAcceptColors()/callRejectColors() como unico caminho de cor das tres acoes funcionais"
affects:
  - "06-04 (telas de chamada) e 06-05 (telas de discagem e ativacao) consomem tudo daqui"
  - "Fases 7 e 8 reutilizam HonestyCard e InfoBanner"
tech-stack:
  added:
    - "androidx.compose.material:material-icons-extended (reservado no version catalog desde o bootstrap para as Fases 5-6)"
  patterns:
    - "cor funcional de seguranca por parametro, nunca por papel do esquema de tema"
    - "varredura de honestidade da copy sobre o TEXTO DOS RECURSOS, nunca sobre o arquivo fonte"
    - "res/ declarado como input das Test tasks (mesma licao de schemas/ e src/main/java)"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/theme/Shape.kt
    - app/src/main/java/org/sentinela/app/ui/components/SentinelaWatermark.kt
    - app/src/main/java/org/sentinela/app/ui/components/HonestyCard.kt
    - app/src/main/java/org/sentinela/app/ui/components/InfoBanner.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallActionButton.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallControlButton.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallOriginChip.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallTimer.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialpadKey.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialpadGrid.kt
    - app/src/test/java/org/sentinela/app/ui/CallStringsTest.kt
    - docs/backlog/fontes-inter-geist.md
  modified:
    - app/src/main/java/org/sentinela/app/ui/theme/Color.kt
    - app/src/main/java/org/sentinela/app/ui/theme/Type.kt
    - app/src/main/java/org/sentinela/app/ui/theme/Theme.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt
    - app/build.gradle.kts
    - docs/INDEX.md
decisions:
  - "As TRES cores funcionais da chamada saem por literal do arquivo de cores e chegam por parametro; o vermelho tambem, sem excecao"
  - "CallReject/OnCallReject sao apelido digito por digito dos tokens destrutivos existentes — nenhuma cor nova"
  - "A fixacao das cores mora em classe Robolectric separada; a classe de tokens continua em JVM pura"
  - "Fontes Inter/Geist caem na reserva monoespacada do sistema; pendencia registrada em docs/backlog/"
  - "A varredura de honestidade e restrita as chaves desta fase — rotulos legitimos de fases anteriores usariam vocabulario aqui proibido"
metrics:
  duration: ~50min
  tasks: 3
  files: 19
  tests_added: 23
  completed: 2026-07-29
---

# Phase 6 Plano 02: Fundacao Visual das Telas de Chamada e Discagem — Summary

Tema com tipografia numerica de largura fixa, as tres cores funcionais da chamada fixadas por
literal fora do alcance da cor dinamica do papel de parede, 74 strings pt-BR varridas por teste
contra promessa desonesta, e nove componentes com alvo >= 48dp e descricao de conteudo em recurso.

## O que foi entregue

### Task 1 — tema (commit `abf04a8`)

- `Color.kt`: quatro tokens novos, nenhum dos 26 existentes tocado.
  `CallAccept = 0xFF1E6E42`, `OnCallAccept = 0xFFD9F2E3`, `CallReject = 0xFF93000A`,
  `OnCallReject = 0xFFFFDAD6`. KDoc em prosa explica por que o vermelho tambem ganha apelido.
- `Type.kt`: `numberXl` (32/40, peso 500, ls 0.5), `numberLg` (24/32, peso 500, ls 0.5) e
  `timer` (16/24, peso 500, `fontFeatureSettings = "tnum"`), como extensoes nomeadas de
  `SentinelaTypography`. Nenhum estilo inline no ponto de uso.
- `Shape.kt` (novo): 8dp / 16dp / 24dp / pilula, ligados ao `MaterialTheme` via `SentinelaShapes`.
- `Theme.kt`: **uma linha** — `shapes = SentinelaShapes`. A montagem do esquema de cor nao foi
  tocada, entao a cor dinamica continua ligada para o resto do aplicativo, como o contrato pede.
- `ThemeTokensTest`: 12 casos (8 novos) + nova classe `CallColorFixationTest` (2 casos) sob
  Robolectric sdk 35, que monta os tres esquemas possiveis e afirma que os quatro tokens
  continuam iguais aos literais em todos eles.

### Task 2 — copy (commit `8faaffd`)

- **74 chaves novas** com os prefixos desta fase (48 do contrato de design + 26 descricoes de
  conteudo de controles e teclas). Todas em pt-BR, nenhuma hardcoded em Kotlin.
- `CallStringsTest` com 13 casos: cinco varreduras (promessa de bloqueio garantido, VoIP,
  historico do telefone, superlativo de marketing, pressao de urgencia) e afirmacoes positivas de
  que a copy diz a verdade — o registro no historico **continua**, o nao perturbe **continua
  valendo**, e chamadas de internet ficam **fora do alcance**.
- `src/main/res` declarado como input das Test tasks: sem isso, mudar so o `strings.xml` deixaria
  o teste UP-TO-DATE e o verde antigo valeria para texto novo nunca varrido.

### Task 3 — componentes (commit `d8d99ed`)

Nove arquivos, cada um com `@Preview`:

| Componente | Ponto que importa |
|---|---|
| `CallActionButton` | zero mencao a papel de cor do tema no arquivo; cor por parametro; escala 0.95 em 120ms + retorno tatil; label textual sob o botao |
| `callAcceptColors()` / `callRejectColors()` | unico caminho de cor das tres acoes funcionais, ambos devolvendo literais do arquivo de cores |
| `CallControlButton` | 56dp, `Modifier.toggleable` com `Role.Switch`, anel externo 2dp a 40% no estado ligado, `stateDescription` de indisponivel, transicao de cor 150ms |
| `CallOriginChip` | pilula passiva, sem `onClick` — mudar politica com o telefone tocando seria decisao sob pressao |
| `CallTimer` | estilo `timer` do tema, relogio injetado, `liveRegion` educado |
| `DialpadKey` | 72dp, digito em `numberXl`, escala 0.92 em 100ms, `onPressStart`/`onPressEnd` separados para o tom DTMF do plano 06-01 |
| `DialpadGrid` | ordem 1 2 3 / 4 5 6 / 7 8 9 / * 0 #, gap 8dp, toque longo em `0` inserindo `+` |
| `SentinelaWatermark` | decorativo: `clearAndSetSemantics {}` o tira da arvore de acessibilidade |
| `HonestyCard` | estilo **identico** para "o que muda" e "o que nao muda", por decisao de produto |
| `InfoBanner` | container alto, barra de acento 4dp, tom informativo — nunca cor destrutiva |

Dimensoes travadas como literais nomeados: `CallActionDiameterPrimary` 72dp,
`CallActionDiameterHangup` 64dp, `CallControlDiameter` 56dp, `DialpadKeyDiameter` 72dp.

## Provas de vermelho executadas

Todas as tres exigidas pelo plano, executadas e restauradas.

**1. Um digito de `CallAccept` alterado** (`0xFF1E6E42` -> `0xFF1E6E43`):

```
CallColorFixationTest > tokens funcionais nao mudam sob tema claro escuro nem papel de parede FAILED
ThemeTokensTest > os quatro tokens funcionais da chamada valem os literais do contrato FAILED
14 tests completed, 2 failed
```

**2. Um digito de `CallReject` alterado** (`0xFF93000A` -> `0xFF93000B`):

```
CallColorFixationTest > o esquema derivado do papel de parede nao serve como fonte de recusar FAILED
CallColorFixationTest > tokens funcionais nao mudam sob tema claro escuro nem papel de parede FAILED
ThemeTokensTest > os quatro tokens funcionais da chamada valem os literais do contrato FAILED
ThemeTokensTest > o apelido de recusar nao introduziu cor nova na paleta FAILED
14 tests completed, 4 failed
```

O quarto vermelho e o interessante: o apelido deixou de casar com o token destrutivo existente, ou
seja o teste tambem detecta a introducao de uma cor nova disfarcada de apelido.

**3. String com promessa desonesta acrescentada** (`"O bloqueio total é garantido: ative agora e
nunca mais será incomodado."`):

```
CallStringsTest > nenhuma string promete bloqueio garantido total ou infalivel FAILED
CallStringsTest > nenhuma string pressiona a ativacao do modo discador FAILED
13 tests completed, 2 failed
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Bloqueio] `material-icons-extended` precisou entrar no `build.gradle.kts`**
- **Found during:** Task 3
- **Issue:** o conjunto nucleo de icones do Compose nao tem `call_end`, `mic`, `mic_off`,
  `volume_up`, `dialpad`, `shield`, `verified_user` nem `visibility_off` — todos exigidos pelo
  contrato de design. Sem eles, atender e recusar nao teriam icones distintos, o que quebra a
  regra de acessibilidade "nunca so cor".
- **Fix:** declarada a dependencia `libs.compose.material.icons.extended`, que o version catalog
  **ja reservava desde o bootstrap** com o comentario "Reservado para as Fases 5-6 (UI) —
  adicionar ao app/build.gradle.kts quando usado". Nao e biblioteca nova nem versao nova: entra na
  versao do Compose BOM que ja esta no projeto.
- **Commit:** `d8d99ed`

**2. [Rule 3 - Bloqueio] detekt reprovou dois componentes por tamanho e complexidade**
- **Found during:** Task 3 (`LongMethod` em `CallControlButton`, `CyclomaticComplexMethod` em
  `CallOriginChip`)
- **Fix:** extraidos `activeRing`, `controlContainerColor`, `controlContentColor` e a tabela de
  chips `originStyle` para funcoes privadas. Nenhum afrouxamento do `detekt.yml`, que e
  compartilhado — precedente da Fase 3.
- **Commit:** `d8d99ed`

**3. [Rule 3 - Bloqueio, autoinfligido] `git checkout` reverteu as 74 strings ainda nao commitadas**
- **Found during:** Task 2, ao restaurar a sabotagem da prova de vermelho
- **Issue:** usei `git checkout <arquivo>` para desfazer a string sabotada. Como o bloco novo
  inteiro ainda estava fora do indice, o comando o levou junto.
- **Fix:** bloco reescrito e commitado antes de qualquer nova sabotagem.
- **Licao:** prova de vermelho sabota trabalho **ja commitado**, ou desfaz a sabotagem por edicao
  pontual — nunca por `git checkout` de arquivo com trabalho novo dentro.

### Divergencias deliberadas do texto do plano

- **`Theme.kt` foi tocado**, com a unica linha `shapes = SentinelaShapes`. O plano previa
  exatamente isso ("apenas a passagem das formas ao `MaterialTheme`, se houver"). A montagem do
  esquema de cor esta intacta.
- **A fixacao dos tres esquemas vive em classe separada** (`CallColorFixationTest`), no mesmo
  arquivo. O plano pedia o caso dentro de `ThemeTokensTest`, mas montar esquema derivado de papel
  de parede exige `Context` e portanto Robolectric, e a Fase 1 decidiu de proposito manter
  `ThemeTokensTest` em JVM pura. Manter as duas coisas exigia duas classes. Os 8 casos novos
  pedidos estao na classe de tokens; os 2 de fixacao, na vizinha.
- **`**Tocar**` de `dialer_activation_change_3` entrou sem os asteriscos.** Markdown nao e
  renderizado por recurso de string no Android, e deixar os asteriscos apareceria literalmente na
  tela. Se o negrito for desejado, ele vira `AnnotatedString` na tela do plano 06-05.
- **74 chaves em vez de 46.** As 46 do enunciado sao o piso; o contrato tem 48 no corpo da tabela
  e a lista de descricoes de conteudo do proprio plano pede as outras 26.
- **Fontes Inter/Geist caíram na reserva.** `res/font/` nao existe e os arquivos nao estao no
  repositorio. Familia de texto do sistema para texto, monoespacada do sistema para os tres
  estilos numericos — o monoespacado ja cumpre o requisito funcional do cronometro. Pendencia em
  `docs/backlog/fontes-inter-geist.md`, indexada em `docs/INDEX.md`. Nenhuma fonte e resolvida em
  tempo de execucao.

### Fora de escopo, nao tocado

- Nenhuma permissao nova no manifest (plano 06-03).
- Filtro do Kover intacto (plano 06-08); `ui.*` segue fora do denominador, entao estes nove
  componentes nao mexem no percentual.
- Nenhum arquivo de `telecom/` tocado (plano 06-01, executado em paralelo e ja concluido).

## Verificacao

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                    -> todos os invariantes OK
```

509 testes JVM no total (23 novos neste plano: 10 de tema/fixacao e 13 de copy), lint e detekt
zerados, 7 blocos de invariantes verdes.

Criterios de aceite por grep, todos conferidos:

| Criterio | Medido |
|---|---|
| `Role.Switch` em `CallControlButton.kt` | 2 |
| `clearAndSetSemantics` em `SentinelaWatermark.kt` | 3 |
| literais de texto visivel em Kotlin (call/dialer/components) | 0 arquivos |
| `colorScheme` em `CallActionButton.kt` | 0 |
| `callAcceptColors`/`callRejectColors` em `CallActionButton.kt` | 6 |
| `CallReject`/`OnCallReject` em `CallActionButton.kt` | 3 |
| papel destrutivo do esquema lido em call/dialer/components | 0 arquivos |
| arquivos com `@Preview` | 9 de 9 |

## Pendencias que este plano deixa

- Empacotar Inter e Geist (`docs/backlog/fontes-inter-geist.md`) — cosmetico.
- `docs/design/TELAS.md` §11 precisa ser reescrita a partir do contrato no fechamento da fase.
- "Uma chamada por vez" precisa entrar em `docs/LIMITACOES.md` no fechamento da fase.
- Contraste sob cor dinamica so tem veredito visual em aparelho fisico (Phase 9). Os pares
  funcionais estao fora da cor dinamica de proposito e nao dependem desse veredito.

## Self-Check: PASSED

Todos os 12 arquivos criados existem no disco e os tres commits estao no historico
(`abf04a8`, `8faaffd`, `d8d99ed`).

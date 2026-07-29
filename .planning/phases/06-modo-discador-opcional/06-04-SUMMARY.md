---
phase: 06-modo-discador-opcional
plan: 04
subsystem: ui
tags: [chamada, acessibilidade, privacidade, compose, robolectric]
requires:
  - "06-01 (CallSnapshot, CallUiState, CallSessionCoordinator, confirmPresented)"
  - "06-02 (tokens funcionais, tipografia numerica, componentes, 74 strings)"
  - "06-03 (CallSessionStore como instancia unica e CallActivity hospedeira)"
provides:
  - "IncomingCallScreen com as quatro variantes de identidade e ordem de foco declarada"
  - "OutgoingCallScreen com progresso por tres pontos, suprimido com reducao de movimento"
  - "ActiveCallScreen com cronometro congelavel e ramo do estado nao suportado"
  - "DtmfKeypadSheet e AudioRouteSheet ancorados ao rodape"
  - "CallerIdentity, AnswerRejectBar e CallControlsRow reutilizaveis"
  - "CallActivity finalizada: when exaustivo, confirmacao de apresentacao, gesto de voltar consumido"
  - "contrato do extra de acao da notificacao (EXTRA_CALL_ACTION + tres valores)"
  - "fronteira do numero travada por teste nas duas direcoes"
affects:
  - "06-06 (le EXTRA_CALL_ACTION/CALL_ACTION_* de CallActivity.kt; nao redeclarar a chave)"
  - "06-08 (Kover: as classes de ui.* seguem fora do denominador)"
  - "Phase 9 (contraste sob cor dinamica e leitor de tela real em aparelho fisico)"
tech-stack:
  added:
    - "compose ui-test-junit4 e ui-test-manifest no conjunto de TESTE UNITARIO (ja estavam no catalog e no instrumentado)"
  patterns:
    - "assert de alvo de toque em DOIS eixos: alvo de toque e tamanho DESENHADO"
    - "requiredSize em controle de chamada: o pai nunca comprime o alvo"
    - "chave de extra montada a partir do identificador do aplicativo, valor fixado por teste"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/call/CallerIdentity.kt
    - app/src/main/java/org/sentinela/app/ui/call/AnswerRejectBar.kt
    - app/src/main/java/org/sentinela/app/ui/call/IncomingCallScreen.kt
    - app/src/main/java/org/sentinela/app/ui/call/OutgoingCallScreen.kt
    - app/src/main/java/org/sentinela/app/ui/call/ActiveCallScreen.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallControlsRow.kt
    - app/src/main/java/org/sentinela/app/ui/call/DtmfKeypadSheet.kt
    - app/src/main/java/org/sentinela/app/ui/call/AudioRouteSheet.kt
    - app/src/test/java/org/sentinela/app/ui/call/CallScreenSemanticsTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/CallLoggingPrivacyTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/call/CallActivity.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallActionButton.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallControlButton.kt
    - app/src/main/res/values/strings.xml
    - app/build.gradle.kts
decisions:
  - "A chave do extra e montada a partir do identificador do aplicativo: o literal ditado quebraria o invariante de rebranding do Bloco 1, e o valor exato ficou travado por teste"
  - "assertTouchHeightIsAtLeast e escrito neste projeto: a biblioteca so oferece igualdade, e igualdade e o assert errado para um contrato de minimo"
  - "O eixo do tamanho DESENHADO foi acrescentado depois de uma prova de vermelho falhar — o Compose expande o alvo de toque sozinho e o assert de alvo media a garantia da biblioteca"
  - "requiredSize substitui size nos dois botoes de chamada: o pai comprimia o circulo de 72dp para 23dp em tela curta"
  - "A faixa superior da chamada ativa encolhe ao proprio conteudo em vez de tomar 30% fixos: 30% colapsava o encerrar para altura zero"
  - "O painel de tons rola na vertical: sem rolagem a ultima fileira de teclas ficava inalcancavel em tela baixa"
  - "O numero e agrupado para exibicao apenas quando tem codigo do pais brasileiro; qualquer outro aparece como a telefonia entregou, nunca adivinhado"
metrics:
  duration_minutes: 74
  tasks: 3
  files_created: 10
  files_modified: 5
  tests_added: 20
  previews: 23
  completed: 2026-07-29
---

# Phase 6 Plan 04: Telas de Chamada Summary

As três telas de chamada com o acabamento contratado, o painel de tons e o seletor de rota, mais a
Activity ligada ao armazém — e, o mais importante, **quatro defeitos reais de layout que só um teste
de alvo de toque encontraria**: o círculo de atender comprimido a 23dp, o botão de encerrar
colapsado a altura zero com o teclado aberto, a última fileira de teclas fora de alcance e a
sabotagem que ficava verde porque o assert media a biblioteca em vez do nosso layout.

## O que foi construído

**Task 1 — chamada recebida** (`68db7c6`)

`CallerIdentity` implementa as quatro variantes com avatar de 96dp, monograma das iniciais como
reserva e anel de 2dp para contato. A degradação está em função nomeada e testável: contato **sem
nome resolvido** vira desconhecido, sem erro e sem alerta — leitura da agenda revogada no meio de
uma ligação é a pior hora possível para pedir permissão. Região e operadora não são exibidas em caso
nenhum.

`AnswerRejectBar` põe recusar à esquerda e atender à direita, cada um com o diâmetro primário, ícone
distinto, rótulo textual e descrição própria. O KDoc registra em prosa por que **não** existe gesto
de arrastar para atender: gesto sem indicação visual clara é pior que dois botões grandes e
simplesmente não existe para quem usa leitor de tela.

`IncomingCallScreen` monta o layout do contrato com ordem de foco declarada por índice de travessia
(marca → estado → identidade → ações), rótulo de estado como região viva educada, marca d'água fora
da árvore de acessibilidade e layout de duas colunas em paisagem. Seis pré-visualizações, incluindo
fonte em 200% e paisagem. **Zero menção a política de origem no arquivo** — o chip é passivo.

**Task 2 — saída, ativa, tons e rota** (`e27b11d`)

`OutgoingCallScreen` usa três pontos de 6dp em fade sequencial de 1200ms, com a animação suprimida
quando a redução de movimento do sistema está ligada (lida da escala de duração de animação do
aparelho). Zero indicador circular girando, verificado por critério.

`ActiveCallScreen` tem o ramo do estado **não suportado** com texto informativo e o encerrar
habilitado — o guarda-corpo da fase. O cronômetro **congela** nos estados finais em vez de
desaparecer, e os controles secundários saem com fade de 200ms.

`DtmfKeypadSheet` reutiliza a grade da tela de discagem com as duas bordas do gesto separadas; o
painel não decide nada sobre tom. `AudioRouteSheet` é painel ancorado ao rodapé, nunca janela
flutuante, com rádio **e** marca de conferido **e** descrição de estado na rota ativa.

**Task 3 — Activity, semântica e fronteira** (`e7c615b`)

`CallActivity` faz `when` exaustivo sobre os sete estados, confirma a apresentação no primeiro
efeito de composição, consome o gesto de voltar enquanto a chamada não é terminal e se fecha depois
do literal nomeado `CALL_ENDED_DISMISS_MILLIS`. O tratamento da intenção cobre `onNewIntent` **e** a
intenção inicial de `onCreate`, traduzindo o valor por função nomeada — valor ausente ou desconhecido
devolve nenhuma ação e a tela só abre.

## Provas de vermelho (executadas e restauradas)

| Sabotagem | Resultado medido |
|---|---|
| Camada de notificação lendo a forma canônica em vez da máscara | **1 vermelho**: "nenhum campo do objeto de notificacao carrega a sequencia completa"; restaurado |
| Controle secundário reduzido de 56dp para 40dp | **2 vermelhos**, exatamente nos casos de mudo e de viva-voz/teclado; restaurado |

**A segunda sabotagem falhou na primeira tentativa, e isso é o achado mais importante deste plano.**
Com 40dp, os dez casos ficaram **verdes**: o Compose expande sozinho o alvo de toque de qualquer
componente interativo até o mínimo da plataforma, então o assert de alvo de toque estava medindo a
garantia da biblioteca, não o nosso layout. Um teste que só media isso passaria para sempre. Foi
acrescentado o segundo eixo — `assertLayoutHeightIsAtLeast`, sobre o tamanho **desenhado** — e só
então a sabotagem ficou vermelha nos dois casos certos. É a lição das "duas redes redundantes" da
Fase 5 aparecendo de novo, por um caminho novo.

## Defeitos reais encontrados pelos testes

Nenhum destes apareceria em revisão de código nem em pré-visualização de tela grande.

1. **Círculo de atender comprimido a 23dp** (contrato: 72dp). O `size()` negocia com o pai; numa
   tela de 470 unidades a coluna não cabia e comprimia os botões em silêncio. Corrigido com
   `requiredSize`, que não negocia — e o contrato diz textualmente que os botões não reduzem.
2. **Encerrar com altura zero com o teclado aberto.** Reservar 30% fixos da tela para a faixa
   superior colapsava exatamente o controle que precisa continuar clicável. A faixa passou a tomar o
   tamanho do próprio conteúdo e o painel fica com o resto.
3. **Última fileira de teclas inalcançável** em tela baixa: o painel agora rola na vertical, e o
   caso de teste rola até a tecla antes de medir — a rolagem faz parte da prova.
4. **Controle secundário comprimido pelo mesmo mecanismo do item 1**, corrigido junto.

## Verificação

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest detekt   -> BUILD SUCCESSFUL
./gradlew lint                                                   -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                -> todos os invariantes OK (8 blocos)
```

**20 casos novos** (14 de semântica, 6 de fronteira do número) e **23 pré-visualizações** nos oito
arquivos de tela. Critérios de aceite por grep, todos conferidos:

| Critério | Medido | Esperado |
|---|---|---|
| pré-visualizações em `IncomingCallScreen` | 6 | >= 4 |
| pré-visualizações em `ActiveCallScreen` | 7 | >= 5 |
| índice de travessia em `IncomingCallScreen` | 9 | >= 4 |
| diâmetro primário citado em `AnswerRejectBar` | 1 | >= 1 |
| indicador circular girando na chamada de saída | 0 | 0 |
| reuso da grade de teclas no painel de tons | 3 | >= 1 |
| política de origem em `IncomingCallScreen` | 0 | 0 |
| literais de texto visível em Kotlin (`ui/call/`) | 0 arquivos | 0 |
| casos em `CallScreenSemanticsTest` | 14 | >= 8 |
| casos em `CallLoggingPrivacyTest` | 6 | >= 5 |
| asserts de alvo de toque | 18 | >= 4 |
| gesto de voltar / confirmação / nova intenção em `CallActivity` | 2 / 1 / 3 | >= 1 cada |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Círculo de ação e controle comprimidos pelo pai**
- **Found during:** Task 3, vermelho legítimo de `CallScreenSemanticsTest`
- **Issue:** medido 23dp num alvo de 72dp e 12dp num alvo de 56dp em tela de 470 unidades.
- **Fix:** `requiredSize` em `CallActionButton` e `CallControlButton`, com o motivo em prosa no KDoc.
- **Commit:** `e7c615b`

**2. [Rule 1 - Bug] Encerrar colapsado e última fileira de teclas inalcançável**
- **Found during:** Task 3, mesmo caso de teste
- **Fix:** faixa superior com altura de conteúdo em vez de fração fixa; painel de tons rolável.
- **Commit:** `e7c615b`

**3. [Rule 3 - Bloqueio] A biblioteca de teste do Compose não estava no conjunto unitário**
- **Issue:** `ui-test-junit4` e `ui-test-manifest` só valiam para o conjunto instrumentado.
- **Fix:** declaradas em `testImplementation`. **Não são dependências novas:** já estavam no version
  catalog e no conjunto instrumentado desde o bootstrap; medir alvo de toque no emulador tornaria o
  critério caro demais para rodar sempre.
- **Commit:** `e7c615b`

**4. [Rule 1 - Higiene] Ícone de viva-voz obsoleto no arquivo novo**
- **Fix:** versão espelhável do ícone em `CallControlsRow`. Os avisos iguais em arquivos de 06-02 e
  06-05 ficaram intocados — são de outros planos.

### Divergências deliberadas do texto do plano

- **A chave do extra é montada, não escrita por extenso.** O plano dita o literal e o critério de
  aceite manda encontrá-lo por grep em `CallActivity.kt`. Isso é **impossível** neste projeto: o
  Bloco 1 de `verify-invariants.sh` proíbe o identificador do aplicativo literal em Kotlin de
  produção, e o critério de grep não distinguiria código de comentário — nem escrevê-lo em prosa
  seria possível. A chave passou a ser montada a partir do identificador (o valor resultante é
  idêntico, byte por byte) e o contrato ficou travado por um **caso de teste** que afirma o valor por
  extenso, no conjunto de teste, onde o invariante não se aplica. Prova mais forte que grep, que
  provaria presença de texto e não igualdade de valor.
- **`assertTouchHeightIsAtLeast` foi escrito neste projeto.** A versão do Compose só oferece a
  comparação por igualdade. Igualdade é o assert errado aqui: o mínimo é 48dp, mas atender vale 72dp
  e encerrar 64dp, então o assert quebraria a cada acerto de acabamento.
- **Um arquivo além da lista do plano:** `CallControlsRow.kt`. Nasceu de dois reprovados do detekt
  (`LongMethod` e `TooManyFunctions`) e é reuso real: a fileira vale para a chamada de saída e para a
  ativa, e duplicá-la faria as duas telas divergirem onde a divergência não aparece em revisão.
  Nenhum afrouxamento do `detekt.yml`, que é compartilhado.
- **Cinco strings novas** (`call_unsupported_state`, `call_unsupported_body`, e os três rótulos de
  rota que faltavam). O contrato de design exige tela informativa no estado não suportado e nomes
  para todas as rotas; sem elas haveria texto em Kotlin ou rota anônima.
- **Agrupamento do número restrito ao código do país brasileiro.** O contrato pede "número completo
  formatado", mas adivinhar agrupamento de país desconhecido produziria número visualmente errado,
  que é pior que número sem espaços. A formatação progressiva da discagem é de 06-05.
- **Foto do contato e lista de rotas ainda não chegam à Activity.** Ambas entram por parâmetro dos
  composables — a costura existe e está documentada —, mas a identidade que o serviço resolve carrega
  só o que a própria ligação informa (decisão registrada em 06-03). Nenhuma foto é cacheada.
- **Bloco 8 do script não recebeu nada**, como o plano manda: a fronteira é provada por teste, porque
  grep provaria ausência de texto e não ausência de comportamento.

### Nota sobre execução concorrente

Durante a Task 1 e a Task 2, `./gradlew lint` e `./gradlew detekt` reprovaram em arquivos de 06-05
(`telecom/OutgoingCallPlacer.kt`) e de 06-06 (`notifications/IncomingCallNotifierTest.kt`), ambos em
edição naquele momento. **Não foram tocados** — são de outros planos. A verificação final, depois de
os dois concluírem, passa limpa.

### Escopo intocado, como mandado

`CallDecisionEngine` não foi tocado. Nenhuma permissão nova, nenhuma biblioteca nova, nenhuma chamada
de rede, nenhum injetor de dependência. Filtro do Kover intacto (é o plano 06-08). Arquivos de 06-05
e 06-06 intocados.

## Para os planos seguintes

- **06-06 deve importar `EXTRA_CALL_ACTION`, `CALL_ACTION_ANSWER`, `CALL_ACTION_REJECT` e
  `CALL_ACTION_HANGUP` de `ui/call/CallActivity.kt`** — não redeclarar a chave. Escrever a chave duas
  vezes não quebra compilação: só faz o botão da notificação parar de funcionar em silêncio.
- **`requiredSize` é o padrão para qualquer controle de chamada novo.** `size()` negocia com o pai e
  o pai comprime.
- **Todo assert novo de alvo de toque precisa dos dois eixos.** Só o alvo de toque mede a garantia do
  Compose, não o nosso layout.
- A resolução de identidade em memória (nome e foto do contato no instante da chamada) continua
  pendente; `CallerIdentity` já a recebe por parâmetro.
- Contraste sob cor dinâmica e leitor de tela real só têm veredito em aparelho físico (Phase 9).

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim.

## Self-Check: PASSED

Os 10 arquivos criados e os 5 modificados existem em disco; os 3 commits declarados
(`68db7c6`, `e27b11d`, `e7c615b`) existem no histórico.

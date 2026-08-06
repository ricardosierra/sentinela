---
phase: 07-ui-onboarding-e-home
plan: 07
subsystem: ui-onboarding
tags: [compose, onboarding, acessibilidade, opt-in, honestidade]
requires:
  - "07-01: StatusAttention/CallAccept como literais fora do Dynamic Color; 269 chaves pt-BR"
  - "07-03: OptionCard, StepHeader, SentinelaTopBar, SettingSwitchRow, CheckRow"
  - "06-02: HonestyCard, ShapePill, rememberMotionReduced"
  - "05: RuntimePermissionAsk; 04: ContactsPermissionState; settings: NotificationIdentification/OriginPolicy"
provides:
  - "NotificationStepScreen: passo 5 de 6, opt-in de notificacao sem nenhuma pressao"
  - "SummaryStepScreen: passo 6 de 6, veredito honesto em dois ramos com quatro linhas de verificacao"
  - "NotificationAndSummaryStepTest: 20 casos de composicao com tres provas de vermelho"
affects:
  - "07-09 (rota do onboarding): as duas telas sao composables puras, montagem fica na rota"
tech-stack:
  added: []
  patterns:
    - "AnimatedVisibility com duracao zero sob reducao de movimento, em vez de ramo separado"
    - "liveRegion Polite no container que GANHA filhos, nunca no filho"
    - "titulo e cor do veredito derivados do MESMO booleano, para nao permitir combinacao mentirosa"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/onboarding/NotificationStepScreen.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/SummaryStepScreen.kt
    - app/src/test/java/org/sentinela/app/ui/onboarding/NotificationAndSummaryStepTest.kt
  modified: []
decisions:
  - "assert de no separado so tem dentes na arvore MESCLADA: a nao mesclada preserva o no do botao mesmo com o ancestral limpando a semantica"
  - "as duas identificacoes reusam texto de notificacao existente como descricao, em vez de criar chave nova"
  - "NEVER_SILENCE cai no rotulo de permitir na linha de desconhecidos, porque o passo 2 nunca ofereceu um quarto rotulo"
  - "UIX-10 NAO foi marcado: estados de carregamento e erro sao da home, e marcar aqui seria o estado falsamente positivo que a fase proibe"
metrics:
  duration: ~50min
  tasks: 3
  files: 3
  tests_added: 20
  coverage: 96.6157%
  completed: 2026-07-30
---

# Phase 7 Plan 7: Passos 5 e 6 do Onboarding Summary

Opt-in de notificação sem uma palavra de pressão e verificação final cujo veredito nunca é
falsamente positivo — com o assert de nó separado reescrito depois de uma prova de vermelho revelar
que ele media a árvore errada.

## O que foi entregue

**`NotificationStepScreen` (passo 5 de 6).** Interruptor DESLIGADO por padrão, com a explicação
permanente como nó irmão (nunca filha mesclada), e as duas identificações em cartão de opção com
papel de botão de rádio, aparecendo somente com o aviso ligado, dentro de um container declarado
como região viva educada. A justificativa da permissão aparece só na negação simples. A tela **não
dispara o pedido do sistema**: ela chama `onEnabledChange(true)` e o dono de estado grava a marca
antes de pedir, nessa ordem — contrato das Fases 4, 5 e 6, travado em 07-04. Zero palavra de
recomendação, zero destaque desigual entre ligar e não ligar, zero contador, zero urgência.

**`SummaryStepScreen` (passo 6 de 6).** Dois vereditos com título e cor saindo do MESMO booleano do
papel de triagem: papel detido dá "Tudo pronto" em `CallAccept`; papel ausente dá "Quase pronto" em
`StatusAttention` **e** a ação de correção na primeira linha. Quatro linhas de verificação por
`CheckRow` (papel, agenda, desconhecidos, contatos+whitelist), cada uma comunicando estado por ícone
E texto, com informação de coleção declarada e as ações de correção como nós focáveis separados. Sem
ação de pular — já é o fim. O cartão de honestidade repete os três itens do passo 1 pelos mesmos
identificadores de recurso, sem uma palavra reescrita.

**`NotificationAndSummaryStepTest`.** 20 casos (mínimo pedido: 14), todo texto lido do recurso, sob
`w411dp-h891dp-xxhdpi` e `@Config(sdk = [35])`, com os três asserts de dois eixos importados de
`TouchTargetAsserts.kt` — nunca duplicados — em 7 controles.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] O assert de "ação em nó separado" media a árvore errada e passava sob sabotagem**

- **Found during:** Task 3, prova de vermelho 3
- **Issue:** O caso dedicado usava apenas `useUnmergedTree` (`onAllNodes(hasClickAction(),
  useUnmergedTree = true)` filtrado por descendente com o rótulo) mais `assertHasNoClickAction()` no
  nó da linha. Com o botão de correção movido para DENTRO do nó mesclado, três outros casos ficaram
  vermelhos — e **o caso dedicado ficou verde**. A árvore não mesclada preserva o nó do botão mesmo
  quando o ancestral limpa a semântica, e o nó da linha continua sem clique próprio nas duas
  configurações. O assert media a existência do botão no lugar onde ele sempre existe, em vez de
  medir a árvore que o leitor de tela consome.
- **Fix:** Acrescentados dois asserts sobre a árvore MESCLADA no mesmo caso —
  `onAllNodesWithText(rótulo).assertCountEquals(1)` e `onNodeWithText(rótulo).assertHasClickAction()`
  — mantendo o assert da árvore não mesclada, que continua provando que o clique é do próprio botão
  e não herdado de um ancestral. Com a correção, a sabotagem deixa o caso dedicado VERMELHO.
- **Files modified:** `NotificationAndSummaryStepTest.kt`
- **Commit:** 2d9cd3b

**2. [Rule 3 - Blocking] `OptionCard` exige descrição e as duas identificações só têm título**

- **Found during:** Task 1
- **Issue:** `settings_notification_identification_masked` e `_anonymous` existem apenas como
  título; o contrato de design não previu descrição, e `OptionCard` a exige (a descrição é
  permanente por decisão de 07-03). Criar chave nova estava fora de escopo: `strings.xml` não é
  arquivo deste plano e quatro agentes escrevem em paralelo.
- **Fix:** Reuso de texto existente e verdadeiro para cada opção — `notification_channel_blocked_desc`
  na mascarada (a discrição do aviso) e `notification_blocked_anonymous` na anônima (literalmente o
  texto que aparecerá). Nenhuma frase nova, nenhuma promessa nova, varredura de honestidade intacta.
- **Files modified:** `NotificationStepScreen.kt`
- **Commit:** 854087b

**3. [Rule 3 - Blocking] `NotificationStepScreen` estourou o limite de tamanho de função do detekt**

- **Found during:** Task 1, verificação automatizada
- **Issue:** `LongMethod` (68 linhas, máximo 60).
- **Fix:** Título/explicação e o botão de avanço extraídos para composables privados nomeados —
  precedente de 06-01 e da tela de ativação, em vez de afrouxar o `detekt.yml` compartilhado.
- **Files modified:** `NotificationStepScreen.kt`
- **Commit:** 854087b

### Falsos vermelhos de paralelismo (nenhum é sinal de código)

Cinco execuções falharam por arquivos EM VOO de outros planos da mesma onda
(`WelcomeAndRoleStepTest.kt`, `ContactsAndWhitelistStepTest.kt`, `RoleStepScreen.kt`). Resolvidos
esperando e repetindo, exatamente como 07-01 registrou. Nenhum arquivo alheio foi tocado para
"consertar" o build.

## Provas de vermelho executadas

Todas sabotaram código de produção JÁ COMMITADO e foram restauradas por **edição manual**, nunca por
`git checkout` — precedente de 06-02. `git diff --quiet` confirmou a restauração.

| # | Sabotagem | Veredito |
|---|---|---|
| 1 | Título do veredito sempre a variante "tudo pronto" | 1 vermelho: o caso do veredito parcial |
| 2 | Interruptor da notificação chegando ligado (`checked = true`) | 2 vermelhos: o do padrão desligado e o do pedido de ligar |
| 3 | Botão de correção movido para dentro do nó mesclado da linha (`CheckRow.kt`) | 3 vermelhos, e o caso DEDICADO verde → defeito real do teste, corrigido; depois da correção, 4 vermelhos |

A prova 3 é a que valeu a pena: ela não confirmou o teste, ela o consertou.

## Decisões técnicas

1. **O assert de semântica mesclada precisa medir a árvore mesclada.** O registro de 07-03 dizia que
   o estado mora no nó do próprio controle; este plano acrescenta o corolário para o lado do teste —
   `useUnmergedTree` prova que o clique é do próprio botão, mas **só** a árvore mesclada prova que o
   botão é alcançável. Um assert de acessibilidade que só olha a árvore não mesclada mede a estrutura
   de composição, não o que o leitor de tela recebe.

2. **Título e cor do veredito saem do mesmo booleano.** Não existe caminho no código em que o círculo
   fique verde sobre um título parcial, porque não há dois booleanos para divergir. É a mesma
   estratégia do `StatValue` de 07-04: tornar o estado mentiroso impossível por construção em vez de
   proibi-lo por convenção.

3. **`NEVER_SILENCE` cai no rótulo de "permitir" na linha de desconhecidos.** O enum tem quatro
   entradas e o passo 2 oferece três opções. Inventar um quarto rótulo mostraria ao usuário, na tela
   de conferência, uma palavra que ele nunca viu na tela em que escolheu — pior que agrupar. Contatos
   e whitelist têm os quatro rótulos e usam o `when` completo.

4. **A repetição do cartão de honestidade é contrato, não descuido.** Registrada em KDoc com o
   motivo: no passo 1 o usuário leu as três frases sem contexto para avaliá-las; aqui ele já viu o
   aplicativo inteiro e está a um toque de confiar nele.

5. **UIX-10 continua PENDENTE.** Estados de carregamento e erro em todas as telas e a proteção
   desativada comunicada com destaque são da home (07-08). Marcá-lo aqui repetiria o erro que 07-01
   evitou com UIX-07/UIX-11: requisito marcado antes da tela existir é exatamente o estado
   falsamente positivo que esta fase proíbe — e seria irônico neste plano.

## Verificação

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                     todos os invariantes OK (8 blocos)
./gradlew koverLog                                                    96.6157% (inalterado: ui.* fora do filtro, 07-11 é dono)
TEST-...NotificationAndSummaryStepTest.xml                            tests="20" failures="0" errors="0"
```

Critérios de aceite por grep, todos conferidos: zero palavra de pressão, `Polite` presente, zero
menção a pedido de permissão do sistema na tela, `onboarding_skip` ausente do passo 6, os dois
títulos de veredito uma vez cada, os três identificadores do cartão de honestidade uma vez cada,
`collectionInfo` presente, quatro usos de `CheckRow`, `useUnmergedTree` presente, sete
`assertLayoutHeightIsAtLeast` pareados com `assertTouchHeightIsAtLeast`.

## Commits

| Task | Commit | Descrição |
|---|---|---|
| 1 | 854087b | passo 5 com opt-in sem pressão |
| 2 | 88903e4 | passo 6 com veredito honesto |
| 3 | 2d9cd3b | 20 casos de composição e três provas de vermelho |

## Self-Check: PASSED

Três arquivos criados conferidos no disco e três commits conferidos no histórico.

## Para os próximos planos

- **07-09 (navegação):** as duas telas são composables puras; `NotificationStepScreen` recebe
  `enabled`, `identification`, `permission` e três callbacks, e `SummaryStepScreen` recebe
  `roleHeld`, três `OriginPolicy`, o estado da agenda e três callbacks. A ordem "gravar a marca,
  depois pedir a permissão" é responsabilidade do dono de estado, não da tela.
- **07-11 (cobertura):** `ui.onboarding.*` segue fora do filtro do Kover; se o filtro alargar, estas
  duas telas entram com 20 casos de composição já escritos.
- **Fase 9 (validação física):** o veredito parcial precisa ser visto em aparelho real com o papel
  negado — o caso automatizado prova o texto e a ação, não a leitura do TalkBack em voz alta.

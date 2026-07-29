---
phase: 06-modo-discador-opcional
plan: 05
subsystem: ui
tags: [modo-discador, discagem, permissao-runtime, copy-honesta, compose, robolectric]
requires:
  - "06-02 (HonestyCard, InfoBanner, DialpadGrid, CallActionButton, 74 strings)"
  - "06-03 (DialerModeState, DialerRoleManager/SystemRoleGate, manifest finalizado)"
provides:
  - "OutgoingCallPlacer — origem de chamada pelo gerenciador de telecomunicacoes, com resultados nomeados"
  - "CallPhonePermissionChecker — camada fina da permissao de originar chamada, em platform/"
  - "sinalizador call_phone_permission_asked no DataStore, fora de ScreeningSettings"
  - "NumberDisplay — campo somente saida com formatacao progressiva e regiao viva educada"
  - "DialpadScreen — tela de discagem completa, alvo da acao de discagem do sistema"
  - "DialerActivity real: le o endereco de telefone da intencao e pede a permissao no toque"
  - "DialerActivationScreen — cinco ramos de estado com a copy honesta contratada"
affects:
  - "06-07 (a tela de ativacao ja consome DialerModeState; falta so liga-la a navegacao das configuracoes)"
  - "06-08 (ui.* segue fora do denominador do Kover; platform/ tambem, pelo precedente da Fase 4)"
tech-stack:
  added:
    - "androidx.compose.ui:ui-test-junit4 e ui-test-manifest no lado testImplementation (mesma dependencia que o androidTest ja usava)"
  patterns:
    - "formatacao progressiva delegada ao formatador da propria biblioteca; forma nacional canonica quando o numero fica valido"
    - "estado desabilitado declarado com clearAndSetSemantics quando o componente compartilhado nao tem parametro de habilitacao"
    - "controle invisivel que ocupa espaco sai da arvore de acessibilidade no mesmo gesto"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/telecom/OutgoingCallPlacer.kt
    - app/src/main/java/org/sentinela/app/platform/CallPhonePermissionChecker.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/NumberDisplay.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialpadScreen.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialerActivationScreen.kt
    - app/src/test/java/org/sentinela/app/telecom/DialerPlaceCallTest.kt
    - app/src/test/java/org/sentinela/app/telecom/CallPhonePermissionTest.kt
    - app/src/test/java/org/sentinela/app/ui/dialer/DialerScreenStateTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/dialer/DialerActivity.kt
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/src/main/res/values/strings.xml
    - app/build.gradle.kts
decisions:
  - "A acao direta de ligar por intencao continua proibida em producao; a origem e sempre o gerenciador de telecomunicacoes (invariante 8.3 verde)"
  - "Falha ao originar volta como resultado nomeado, nunca excecao — o inverso do nucleo da sessao de chamada, porque aqui o usuario esta com o dedo no botao"
  - "A recusa da plataforma e capturada por tipo (SecurityException/IllegalStateException) e nao registrada: a mensagem pode conter o numero"
  - "O formatador progressivo da biblioteca nao fecha parenteses no Brasil; a forma nacional canonica entra no instante em que o numero fica valido"
  - "O estado desabilitado do botao de ligar e declarado por clearAndSetSemantics no proprio ramo da discagem, sem editar o componente de acao de chamada (dono: plano 06-04)"
  - "O botao de apagar desaparece da arvore de acessibilidade mas mantem os 56dp: o botao de ligar nao pode se mover sob o dedo"
  - "call_phone_permission_asked fora de ScreeningSettings e gravado ao disparar o launcher, terceiro par do mesmo padrao das Fases 4 e 5"
metrics:
  duration_minutes: 78
  tasks: 3
  files_created: 8
  files_modified: 5
  tests_added: 27
  completed: 2026-07-29
---

# Phase 6 Plan 05: Discagem, Origem de Chamada e Ativação Honesta Summary

O Sentinela passa a discar de verdade: teclado completo, chamada originada pelo caminho oficial da
plataforma, permissão de originar pedida no toque em ligar — e uma tela de ativação que apresenta
custo e benefício com o mesmo peso visual, sem prometer o que a Fase 5 provou ser impossível.

## O que foi construído

**Task 1 — origem de chamada e permissão em runtime** (`fc36e77`)

`OutgoingCallPlacer` recebe o gerenciador de telecomunicações, o normalizador do projeto e a leitura
da permissão como função — lida **a cada toque**, porque a permissão pode ser revogada nas
Configurações com a tela aberta. `place()` devolve quatro resultados nomeados (originada, permissão
ausente, número inválido, falha da plataforma), nenhum deles carregando dado pessoal. O KDoc registra
em prosa por que a ação direta de ligar por intenção não é usada (para um discador que não vem
instalado, ela é reencaminhada ao discador do sistema para confirmação) e que chamada de emergência é
sempre do discador do aparelho — motivo pelo qual a interface não promete nada sobre ela.

`CallPhonePermissionChecker` é o molde exato dos verificadores da agenda e das notificações: camada
fina em `platform/`, zero regra própria, tudo delegado à função pura de quatro estados. O sinalizador
`call_phone_permission_asked` entrou no DataStore existente, **fora** de `ScreeningSettings` — é o
terceiro par do mesmo padrão, e o motivo continua o mesmo das Fases 4 e 5: não é configuração de
triagem e não pode pesar no instantâneo do caminho quente.

**Task 2 — tela de discagem** (`3c0412b`)

`NumberDisplay` é campo **somente saída**: não abre teclado do sistema, não mostra cursor e, vazio,
não mostra nada — nem exemplo, que é lido como número de verdade por quem está com pressa.
Autodimensionamento em degraus (32 → 26 → 20sp), no máximo duas linhas, região viva educada e
descrição de conteúdo com os dígitos separados por espaço, para o leitor de tela dizer "um um" e não
"onze".

`DialpadScreen` reutiliza a grade do plano 06-02 e monta a linha de ação do contrato. A barra de
mensagem de falha mantém o número no campo e oferece "Tentar de novo". `DialerActivity` lê o endereço
de telefone da intenção recebida, **sem discar sozinho**, e pede a permissão de originar no momento
do toque — gravando o sinalizador antes do `launch`, nunca no retorno.

**Task 3 — ativação e reversão** (`0829029`)

Cinco ramos de estado, os dois cards de honestidade usando o **mesmo** componente com o **mesmo**
estilo, card de pré-requisito da agenda com o convite desabilitado (não escondido — esconder daria a
impressão de que o modo não existe), painel de reversão com chip "Ativo", card único e botão **tonal**
(reverter não destrói dado), e banner informativo no papel perdido. Zero diálogo próprio: o seletor do
sistema **é** a confirmação.

O KDoc registra as três verdades medidas e uma quarta por omissão deliberada: **nada nesta tela
promete bloquear número privado neste modo**, porque a hipótese segue não verificada, e prometer o que
não foi medido é o mesmo defeito das outras três.

## Provas de vermelho

**Task 1 — o originador ignorando o estado da permissão** (sabotagem em arquivo **já commitado**,
restaurada por `git checkout` justamente porque o trabalho estava no índice — a lição do plano 06-02
aplicada de propósito):

```
DialerPlaceCallTest > sem a permissao concedida nada e pedido a plataforma FAILED
DialerPlaceCallTest > sem a permissao concedida devolve falha e nao lanca FAILED
8 tests completed, 2 failed
```

**Task 1 — fase vermelha do ciclo TDD:** os dois arquivos de teste foram escritos antes da produção e
o build parou em `Unresolved reference 'OutgoingCallPlacer'`.

## Verificação

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt  -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                    -> == todos os invariantes OK ==
./gradlew koverLog                                                   -> 95,5531%
```

**603 testes JVM** no total, **27 novos** neste plano (8 de origem de chamada, 8 de permissão, 11 da
tela de discagem) — acima dos 19 exigidos. Lint e detekt zerados; o invariante 8.3 ("chamada originada
apenas pelo gerenciador de telecomunicações") segue verde **com** o originador novo no repositório, que
é a prova que importa.

Critérios de aceite por grep, todos conferidos:

| Critério | Medido | Esperado |
|---|---|---|
| casos em `DialerPlaceCallTest` | 8 | >= 6 |
| casos em `CallPhonePermissionTest` | 8 | >= 5 |
| regra de permissão em runtime citada no teste de permissão | presente | >= 1 |
| ação direta de ligar em código de produção | 0 arquivos | 0 |
| casos em `DialerScreenStateTest` | 11 | >= 8 |
| `@Preview` em `DialpadScreen.kt` | 5 | >= 5 |
| expressão regular em `NumberDisplay.kt` | 0 | 0 |
| literais de texto visível em discagem | 0 arquivos | 0 |
| `@Preview` em `DialerActivationScreen.kt` | 6 | >= 5 |
| ramos de estado na tela de ativação | 17 menções (5 ramos) | >= 5 |
| `HonestyCard` na tela de ativação | 4 | >= 2 |
| literais de texto visível na ativação | 0 | 0 |
| diálogo próprio de confirmação | 0 | 0 |
| `CallStringsTest` (varredura de honestidade) | verde | verde |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] O estado desabilitado do botão de ligar não chegava ao leitor de tela**
- **Found during:** Task 2, dois casos legitimamente vermelhos
- **Issue:** o componente compartilhado de ação de chamada não tem parâmetro de habilitação (e é do
  plano 06-04, que roda em paralelo — editá-lo era proibido). Envolvê-lo num nó com semântica de
  mesclagem não funcionou: o nó interno do próprio botão já mescla e é ele quem responde às buscas,
  de modo que o estado ficava num ancestral que ninguém consulta. Na prática: opacidade de 38% na
  tela e, para quem usa leitor de tela, um botão anunciado como normal.
- **Fix:** `clearAndSetSemantics` no ramo da discagem, definindo **um** nó com descrição, papel,
  estado desabilitado e — importante — a ação de clique redeclarada, para o controle não sair da
  árvore de acessibilidade.
- **Commit:** `3c0412b`

**2. [Rule 1 - Bug] O formatador progressivo não fecha parênteses no Brasil**
- **Found during:** Task 2, três casos vermelhos
- **Issue:** medido com os metadados reais — `11912345678` sai como `11 91234-5678` do formatador
  progressivo, e o contrato de design pede `(11) 91234-5678`. O formatador é otimizado para não mexer
  no que o dedo acabou de digitar e por isso não fecha parênteses retroativamente.
- **Fix:** enquanto digita, o formatador progressivo; no instante em que o número fica **válido**, a
  forma nacional canônica da mesma biblioteca. Nenhuma expressão regular e nenhuma formatação própria.
- **Commit:** `3c0412b`

**3. [Rule 3 - Blocking] Lint reprovou a origem da chamada por permissão**
- **Found during:** Task 2, `./gradlew lint` (`MissingPermission`, **erro**)
- **Issue:** a verificação da permissão chega ao originador por função injetada, e nenhuma ferramenta
  estática enxerga isso; `runCatching` também não conta como tratamento explícito.
- **Fix:** captura por tipo (`SecurityException` e `IllegalStateException`). A exceção **não** é
  registrada, de propósito: a mensagem da plataforma pode conter o número, e número completo nunca
  entra em log. Motivo escrito no KDoc, com `@Suppress("SwallowedException")` local.
- **Commit:** `3c0412b`

**4. [Rule 3 - Blocking] A tela abria fora do viewport no teste**
- **Found during:** Task 2 — o botão de ligar aparecia com 11px de altura e o campo do número ficava
  fora da tela
- **Issue:** o aparelho padrão do Robolectric é pequeno demais para uma tela de discagem inteira.
- **Fix:** qualificadores de tela reais na configuração do teste (`w411dp-h891dp-xxhdpi`).
- **Commit:** `3c0412b`

**5. [Rule 3 - Blocking] detekt reprovou a tela de ativação por número de funções**
- **Fix:** supressão **local** no arquivo, com motivo escrito, no molde do plano 06-01 — o
  `detekt.yml` é compartilhado e não foi afrouxado. A contagem alta é consequência do próprio
  contrato: um composable pequeno por ramo de estado e seis pré-visualizações.
- **Commit:** `0829029`

### Divergências deliberadas do texto do plano

- **Três arquivos além da lista:** `DataStoreSettingsRepository.kt` (o sinalizador novo, que o próprio
  plano manda guardar "no DataStore existente"), `AppContainer.kt` e `app/build.gradle.kts`.
- **`AppContainer`: dois campos deixaram de ser privados** (o util de telefonia e o provedor de
  região). A tela precisa do formatador progressivo da biblioteca e de uma região; construir um
  segundo util recarregaria os metadados inteiros, e travar a região em `BR` contrariaria a cascata
  decidida na Fase 2. A instância continua sendo **uma** no processo, que é o invariante real.
- **Dependência de teste de composição no lado JVM.** Não é biblioteca nova: é a **mesma** que o
  `androidTest` já usa, declarada no catálogo desde o bootstrap e resolvida pelo Compose BOM que já
  está no projeto. Sem ela não existe regra de teste de composição em JVM, que o plano exige.
- **Três strings novas** (`action_back`, `dialer_active_changes_title`,
  `dialer_activation_unavailable`). O contrato pede barra superior com voltar, card único do modo ativo
  e um ramo para aparelho sem o papel; nenhuma dessas três existia, e texto em Kotlin é proibido. As
  duas com prefixo da fase passam pela varredura de honestidade.
- **11 casos na tela de discagem em vez de 8**, incluindo o número pré-preenchido pela intenção e a
  sugestão de nome.
- **A tela de ativação ainda não tem hospedeira própria.** Ela é um composable puro com quatro
  retornos de chamada; ligá-la à navegação das configurações é do plano 06-07, e criar uma Activity
  agora produziria arquivo para ser reescrito (mesmo raciocínio que o plano 06-03 aplicou às telas).

### Escopo intocado, como mandado

`CallDecisionEngine` não foi tocado. `AndroidManifest.xml` não foi tocado. Filtro do Kover intacto
(cobertura 95,5531%, gate em 80). Nenhum arquivo de `ui/call/`, `notifications/`,
`SentinelaInCallService.kt` ou `call/CallSessionStore.kt` — territórios dos planos 06-04 e 06-06, que
rodaram em paralelo. Nenhuma biblioteca nova de produção, nenhuma chamada de rede.

## Para os planos seguintes

- **06-07** liga `DialerActivationScreen(state, onRequestRole, onRevert, onGrantContacts, onBack)` à
  navegação: `state` vem de `dialerModeState(...)`, `onRequestRole` do pedido do papel de discador,
  `onRevert` da intenção do seletor do sistema, e `onGrantContacts` do pedido da agenda. A tela não
  consulta nada por conta própria de propósito.
- **A sugestão de nome na discagem** entra por `suggestionFor`, que hoje chega vazia da Activity:
  ligá-la à consulta local de contatos é trabalho de quem tiver a tela de contatos em mão. Nenhum dado
  da agenda pode ser persistido nesse caminho.
- **Pendência física:** o comportamento real da ação de discagem com e sem o papel, e a chamada de
  emergência, só têm veredito em Samsung físico (Phase 9).

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim.

## Self-Check: PASSED

Os 8 arquivos criados e os 5 modificados existem em disco; os 3 commits declarados (`fc36e77`,
`3c0412b`, `0829029`) existem no histórico.

---
phase: 06-modo-discador-opcional
plan: 06
subsystem: notifications
tags: [modo-discador, notificacao, tela-cheia, privacidade, robolectric, degradacao]
requires:
  - "06-03 (CallSessionStore com CallSessionObserver, SentinelaInCallService, CallActivity declarada, permissao de tela cheia na matriz)"
  - "06-01 (CallUiState/CallSnapshot/CallIdentity puros)"
provides:
  - "CallNotificationChannels — canal de chamada de importancia ALTA, separado do canal discreto da Fase 5"
  - "IncomingCallNotifier — chamada recebida em tela cheia, chamada em curso e cancelamento; implementa CallNotificationCanceller"
  - "contrato do extra de acao consumido pela tela de chamada (chave composta + tres valores)"
  - "MaskedCallIdentity — tipo de fronteira: nome + numero JA mascarado"
  - "OngoingCallNotifier — costura pura da troca para o aviso de chamada em curso"
  - "AppContainer.incomingCallNotifier e AppContainer.maskNumber"
affects:
  - "06-04 (le o extra de acao na CallActivity; deve usar IncomingCallNotifier.EXTRA_CALL_ACTION, nao um literal)"
  - "06-07 (reversao do modo discador ja tem o cancelador implementado)"
  - "06-08 (notifications.* ganhou duas classes, ambas cobertas em JVM)"
tech-stack:
  added: []
  patterns:
    - "capacidade da plataforma consultada por costura injetavel, com a consulta real como valor padrao"
    - "tipo de fronteira para dado mascarado, em vez de confiar em disciplina de quem chama"
    - "chave de extra composta do identificador do aplicativo em vez de literal"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/notifications/CallNotificationChannels.kt
    - app/src/main/java/org/sentinela/app/notifications/IncomingCallNotifier.kt
    - app/src/test/java/org/sentinela/app/notifications/IncomingCallNotifierTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/src/main/java/org/sentinela/app/telecom/SentinelaInCallService.kt
    - app/src/main/java/org/sentinela/app/telecom/call/CallSessionStore.kt
    - app/src/main/res/values/strings.xml
    - app/build.gradle.kts
decisions:
  - "a troca para o aviso de chamada em curso vive no CallSessionStore, porque o fato 'o estado mudou' pertence a ele; o servico so publica a chamada recebida e cancela"
  - "a troca acontece na TRANSICAO para ativa, nunca a cada retrato: mudo/viva-voz/teclado republicam o estado ativo varias vezes por chamada"
  - "a capacidade de ocupar a tela entra por costura porque a consulta da versao 34 nao tem sombra no Robolectric 4.16.1 — sem isso o caso de degradacao seria intestavel"
  - "as acoes sao adicionadas a mao SO no piso da plataforma; no ramo moderno elas vem do estilo de chamada, e somar as duas fontes daria quatro botoes"
  - "um unico identificador de notificacao: a chamada em curso SUBSTITUI o aviso da chamada recebida"
  - "chave do extra composta de BuildConfig.APPLICATION_ID — literal do identificador em Kotlin e reprovado pelo Bloco 2 do script de invariantes"
metrics:
  duration_minutes: 62
  tasks: 3
  files_created: 3
  files_modified: 5
  tests_added: 16
  completed: 2026-07-29
---

# Phase 6 Plan 06: Notificação de Chamada em Tela Cheia Summary

Com a tela bloqueada, a chamada recebida agora aparece pelo caminho **oficial** da plataforma —
canal próprio de importância alta com intenção de tela cheia — degrada para aviso com atender e
recusar quando a permissão de ocupar a tela está revogada, mantém a notificação mascarada no mesmo
nível travado na Fase 5, e está ligada ao caminho real da chamada: aparece quando a chamada chega,
troca quando ela fica ativa e desaparece quando ela é perdida.

## O que foi construído

**Task 1 — canal e notificador** (`b6121a6`, corrigido em `26c5508`)

`CallNotificationChannels` cria o canal `ongoing_calls` com importância **alta**. O KDoc registra em
prosa por que o canal discreto da fase anterior não serve e **nunca poderá servir**: a importância
de um canal é imutável depois de criado, e reaproveitar o canal de importância baixa produziria um
pedido de tela cheia que jamais dispara — defeito em que o código parece correto e o usuário
simplesmente não vê a chamada chegando.

`IncomingCallNotifier` expõe `notifyIncoming`, `notifyOngoing` e `cancel`, e implementa
`CallNotificationCanceller` (a costura que 06-03 deixou pronta em `DialerModeState.kt`, usada pela
reversão do modo). Ramo por versão: estilo de chamada da plataforma a partir do nível 31; no piso,
notificação contínua com prioridade alta e as duas ações adicionadas à mão. A intenção de tela cheia
só é anexada depois de a capacidade ser consultada.

**Nenhum receptor de transmissão foi criado e o manifest não foi tocado** (verificado por critério, e
`git diff` do manifest vazio nos três commits). Todas as intenções pendentes são de **Activity** para
a tela de chamada, imutáveis, cada uma com código de pedido próprio — sem isso a plataforma
devolveria a mesma intenção para todas as ações.

**Task 2 — prova** (`e796d6a`)

`IncomingCallNotifierTest`: **16 casos**, 14 no nível 35 e **2 no nível 29**, todos lendo de volta o
objeto realmente publicado pelo gerenciador de notificações sombreado. Cobrem canal, tela cheia,
degradação, resolução de cada ação, imutabilidade, substituição do aviso, cancelamento pelas duas
portas (`cancel` e `cancelCallNotification`) e a varredura de privacidade.

**Task 3 — fiação** (`2377392`)

`AppContainer` ganhou `incomingCallNotifier` (singleton **preguiçoso**: no modo filtro, que é o
padrão, ele nunca nasce) e expôs `maskNumber`, a máscara única com os metadados já resolvidos, que
`PostScreeningWork` passou a reusar em vez de repetir a lambda. Nada em `SentinelaApp.onCreate`
(verificado em 0 ocorrências).

No `SentinelaInCallService`, `notifyIncoming` fica **no mesmo ponto em que o observador da chamada é
registrado**, antes de qualquer abertura de tela, e `cancel()` fica no ponto em que o observador é
removido. A máscara é aplicada ali, na fronteira, pela função `maskedIdentityOf` — o notificador
nunca recebe número cru. Zero captura de defeito no arquivo, como a fase exige.

### A escolha pedida pelo plano, em uma frase

**A troca para o aviso de chamada em curso ficou no `CallSessionStore`**, não no serviço: o retorno
de mudança de estado pertence ao armazém (é o que 06-03 mediu e registrou), e a transição para ativa
chega por ali como retrato com `state == Active`. Colocá-la no serviço criaria um segundo caminho
para o mesmo fato. O armazém recebe a costura pura `OngoingCallNotifier` e a máscara por construtor —
continua sem conhecer contexto de Android. E a publicação acontece **só na transição**: mudo,
viva-voz e teclado republicam o estado ativo várias vezes por chamada, e republicar o aviso a cada
um faria a barra de avisos piscar durante a ligação.

## Contrato do extra de ação — leitura obrigatória para 06-04

O valor resolvido é exatamente o combinado (`<identificador do app>.extra.CALL_ACTION`, com os
valores `answer`, `reject` e `hangup`), mas ele **não pode ser escrito como literal em Kotlin**: o
Bloco 2 do script de invariantes reprova o identificador do aplicativo literal em código, e reprovou
a primeira versão deste arquivo. A chave passou a ser composta de `BuildConfig.APPLICATION_ID`.

**Quem lê deve usar `IncomingCallNotifier.EXTRA_CALL_ACTION`, `ACTION_ANSWER`, `ACTION_REJECT` e
`ACTION_HANGUP`** — um literal repetido na tela de chamada derruba o mesmo invariante.

## Provas de vermelho (as quatro, executadas em código já commitado e restauradas)

| Sabotagem | Resultado medido |
|---|---|
| Importância do canal novo baixada de alta para intermediária | **1 vermelho**: "o canal de chamada e criado com importancia alta" |
| Número sem máscara no texto da notificação | **2 vermelhos**: a varredura de privacidade e o caso do nível 29 |
| Degradação publicando sem ações (estilo de chamada e ações à mão removidos) | **3 vermelhos**: degradação sem tela cheia, resolução das ações e o caso do nível 29 |
| Uma ação apontando para componente inexistente | **2 vermelhos**: resolução das ações e da intenção de tela cheia |

Restauração por cópia de arquivo salva antes da sabotagem, **não** por `git checkout` — a lição do
plano 06-02 foi reaprendida da forma difícil aqui: um `git checkout` de arquivo de produção reverteu
um refinamento ainda fora do índice, que precisou ser reescrito. Da segunda sabotagem em diante
todas usaram cópia e restauração explícitas.

## Verificação

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest   -> BUILD SUCCESSFUL, 583 testes, 0 falhas
./gradlew lint detekt                                     -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                         -> todos os invariantes OK (8 blocos)
./gradlew koverLog                                        -> 95,5531% (gate 80)
```

Suítes diretamente relevantes, contadas no relatório: `IncomingCallNotifierTest` 16/16 e
`SentinelaInCallServiceTest` **9/9 verdes com a fiação no lugar** — é essa a prova pedida de que a
notificação entrou no caminho real da chamada sem alterar o comportamento de registro e remoção do
observador que 06-03 travou. `BlockedCallNotifierTest` (Fase 5) segue intacto: os dois canais e os
dois notificadores convivem sem se tocar.

Critérios de aceite por grep, todos conferidos:

| Critério | Medido | Esperado |
|---|---|---|
| importância alta no arquivo do canal | 1 | >= 1 |
| intenção pendente imutável | 1 | >= 1 |
| pedido de tela cheia | 1 | >= 1 |
| consulta da capacidade de tela cheia | 1 | >= 1 |
| intenções pendentes de Activity | 1 | >= 1 |
| intenções pendentes de outro tipo | 0 | 0 |
| arquivo de receptor de transmissão | ausente | ausente |
| manifest alterado por este plano | nada | nada |
| normalizador ou número cru no notificador | 0 | 0 |
| menção a janela sobre outros aplicativos no notificador | 0 | 0 |
| `incomingCallNotifier` no container | 2 | >= 1 |
| `by lazy` no container | 18 | >= 12 |
| `incomingCallNotifier` em `SentinelaApp.kt` | 0 | 0 |
| publicação, troca e cancelamento na camada de telefonia | 1 / 1 / 3 arquivos | >= 1 cada |
| captura de defeito no serviço | 0 | 0 |
| container construído no serviço | 0 | 0 |
| casos de teste | 16 | >= 10 |
| casos no nível 29 | 2 | >= 1 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Chave do extra reprovada pelo invariante de rebranding**
- **Found during:** Task 3, `scripts/verify-invariants.sh`
- **Issue:** o plano ditou a chave do extra como literal, e o Bloco 2 proíbe o identificador do
  aplicativo literal em Kotlin (fora de `package`/`import`) — o KDoc **e** a constante foram
  apanhados. Critério por grep não distingue comentário de código: a mesma lição da Fase 5, agora
  atingindo prosa ditada por um plano.
- **Fix:** a chave passou a ser composta de `BuildConfig.APPLICATION_ID` e o KDoc descreve o
  identificador em prosa em vez de escrevê-lo. O valor resolvido é idêntico ao contrato, e agora
  acompanha qualquer renomeação futura — é a correção certa, não o silenciamento do script.
- **Files modified:** `notifications/IncomingCallNotifier.kt`, `app/build.gradle.kts`
- **Commit:** `26c5508`

**2. [Rule 3 - Blocking] `BuildConfig` não existia no projeto**
- **Found during:** Task 3, compilação
- **Issue:** o AGP desta versão não gera a classe de configuração de compilação por padrão.
- **Fix:** `buildConfig = true` em `buildFeatures`, comentado no ponto com o motivo. Nenhum campo
  próprio declarado — só o identificador do aplicativo, que já era da configuração.
- **Commit:** `26c5508`

**3. [Rule 1 - Bug] Ações duplicadas no ramo moderno**
- **Found during:** Task 1, ao escrever os casos de teste
- **Issue:** o estilo de chamada da plataforma constrói a própria lista de ações; somar as ações
  adicionadas à mão daria quatro botões no aviso a partir do nível 31.
- **Fix:** as ações à mão passaram a existir **apenas** no ramo do piso da plataforma, onde o estilo
  de chamada não existe. Provado nos dois níveis: as duas ações aparecem no 35 (vindas do estilo) e
  no 29 (à mão).
- **Commit:** `e796d6a`

### Divergências deliberadas do texto do plano

- **A capacidade de ocupar a tela entra por costura injetável** (`fullScreenAllowed`), com a consulta
  real da plataforma como valor padrão. O plano pedia para testar a degradação "desligando a
  capacidade no ambiente sombreado", e isso **não existe**: o Robolectric 4.16.1 não tem sombra para
  a consulta da versão 34. Sem a costura, o caso mais importante desta suíte seria intestável — e a
  consulta real continua no arquivo de produção, conferida por critério.
- **`MaskedCallIdentity` e `OngoingCallNotifier` foram criados em `CallSessionStore.kt`**, que é
  arquivo previsto pelo plano. O plano falava de "identidade já mascarada" sem dar tipo; um tipo
  próprio torna a fronteira visível no compilador em vez de depender da disciplina de quem chama.
- **`maskNumber` virou membro público do container** e `PostScreeningWork` passou a reusá-lo. Duas
  lambdas de máscara no mesmo container seriam duas máscaras — exatamente o que `PhoneMask` existe
  para impedir.
- **Cinco strings novas em pt-BR** (canal, dois títulos e o texto de quem não se identificou), com
  prefixos fora da varredura de honestidade da fase por não serem copy de tela.

### Escopo intocado, como mandado

`CallDecisionEngine` não foi tocado. Nenhuma permissão nova, nenhuma biblioteca nova, nenhuma
chamada de rede, nenhum `checkpoint`. Filtro do Kover intacto (alargá-lo é o plano 06-08). Manifest
intacto. Nenhum arquivo dos planos 06-04 e 06-05 foi editado.

## Concorrência com os planos da mesma onda

Os planos 06-04 e 06-05 rodaram ao mesmo tempo, e o repositório passou por longos intervalos em que
**a compilação estava quebrada por arquivos deles** (telas de chamada e de discagem em edição). Cada
verificação deste plano foi repetida até a árvore compilar; nenhuma delas foi aceita com falha de
terceiro. As duas falhas de terceiro observadas e **não** corrigidas aqui, por estarem fora do
escopo: erro de lint de permissão em `telecom/OutgoingCallPlacer.kt` (06-05) e achados de detekt em
`ui/call/ActiveCallScreen.kt` (06-04). Ambas já estavam resolvidas por seus donos na verificação
final, que fechou verde.

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim.

## Self-Check: PASSED

Os 3 arquivos criados e os 5 modificados existem em disco; os 4 commits declarados
(`b6121a6`, `e796d6a`, `26c5508`, `2377392`) existem no histórico.

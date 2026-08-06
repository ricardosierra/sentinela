---
phase: 06-modo-discador-opcional
plan: 03
subsystem: telecom
tags: [modo-discador, papel-do-sistema, manifest, permissoes, invariantes, robolectric]
requires:
  - "06-01 (CallSessionCoordinator, CallControls, CallUiState, CallStateMapper)"
  - "06-02 (componentes visuais de chamada e discagem, strings pt-BR)"
provides:
  - "elegibilidade real ao papel de telefone padrao (manifest minimo medido)"
  - "SentinelaInCallService — camada fina entre plataforma e nucleo puro"
  - "CallSessionStore — instancia unica do processo com o estado observavel da chamada"
  - "TelecomCallControls — traducao de cada comando, com ramo por versao na recusa"
  - "SystemRoleGate + DialerRoleManager — consulta, pedido e intencao de reversao"
  - "DialerModeState — funcao pura de precedencia do modo discador"
  - "Bloco 8 de scripts/verify-invariants.sh (4 checagens)"
affects:
  - "06-04 (tela de chamada substitui o composable inicial de CallActivity.kt)"
  - "06-05 (tela de discagem e ativacao; pedido de CALL_PHONE em runtime)"
  - "06-06 (notificacao: registra CallSessionObserver em CallSessionStore e implementa CallNotificationCanceller)"
  - "06-08 (filtro do Kover: telecom.call.* novo derrubou a cobertura de 97,70% para 96,08%)"
tech-stack:
  added: []
  patterns:
    - "armazem de processo com espelho de fluxo e publicacao sincrona do retrato final"
    - "consulta de papel parametrizada pelo papel, uma base para os dois papeis"
    - "ramo por versao isolado em funcao anotada, testado nos dois niveis"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/telecom/SentinelaInCallService.kt
    - app/src/main/java/org/sentinela/app/telecom/SystemRoleGate.kt
    - app/src/main/java/org/sentinela/app/telecom/DialerRoleManager.kt
    - app/src/main/java/org/sentinela/app/telecom/call/CallSessionStore.kt
    - app/src/main/java/org/sentinela/app/telecom/call/TelecomCallControls.kt
    - app/src/main/java/org/sentinela/app/telecom/call/DialerModeState.kt
    - app/src/main/java/org/sentinela/app/ui/call/CallActivity.kt
    - app/src/main/java/org/sentinela/app/ui/dialer/DialerActivity.kt
    - app/src/test/java/org/sentinela/app/telecom/SentinelaInCallServiceTest.kt
    - app/src/test/java/org/sentinela/app/telecom/CallRejectCompatTest.kt
    - app/src/test/java/org/sentinela/app/telecom/DialerRoleManagerTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/DialerModeStateTest.kt
  modified:
    - docs/PERMISSOES.md
    - scripts/verify-invariants.sh
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt
decisions:
  - "detach() publica o retrato final ANTES de cancelar o espelho: sem isso o encerramento se perdia numa corrida e a tela ficava mostrando chamada ativa ja terminada"
  - "uses-feature de telefonia com required=false: lint reprovava a permissao nova sem isso, e o aplicativo de fato roda sem radio (modo discador indisponivel)"
  - "prova de ausencia da sobrecarga moderna no piso da plataforma usa confirmVerified, nao verify(exactly=0): a assinatura NAO EXISTE no nivel 29 e verifica-la estoura com erro de metodo ausente"
  - "SystemRoleGate extraido e ScreeningRoleManager reduzido a uma linha, em vez de duplicar a forma da consulta para o segundo papel"
  - "identificador opaco da chamada derivado da identidade do objeto em memoria: sem numero, sem nome, com o tempo de vida do processo"
metrics:
  duration_minutes: 34
  tasks: 3
  files_created: 12
  files_modified: 5
  tests_added: 31
  completed: 2026-07-29
---

# Phase 6 Plan 03: Elegibilidade ao Papel de Telefone Padrão Summary

O aparelho passa a poder aceitar o Sentinela como telefone padrão: manifest com o serviço de
chamada protegido e os **dois** filtros de discagem, serviço fino que não intercepta defeito,
armazém de sessão como instância única do processo, papel consultado sempre ao sistema e quatro
invariantes de script travando o que não aparece em revisão de código.

## O que foi construído

**Task 1 — matriz de permissões antes do código** (`4f264f3`)

`docs/PERMISSOES.md` ganhou a linha de `USE_FULL_SCREEN_INTENT` (normal, concedida no install a
aplicativo de chamada, nunca pedida em diálogo, revogável nas Configurações — o código consulta
antes de usar e degrada para aviso com ações). A nota que dizia "a lista de elegibilidade é
confirmada na pesquisa da Fase 6" foi **substituída pela lista confirmada**, com os quatro itens e
o registro explícito de que declarar só a tela de discagem faz o pedido do papel **falhar**. A
proibição da janela sobre outros aplicativos foi reafirmada, apontando o caminho oficial de
notificação como substituto.

No script, a lista autorizada ganhou as três permissões com comentário citando a fase e a matriz, e
a variável de fases futuras perdeu as três — ficando com registro de chamadas, estado do telefone,
mensagens, janela sobre outros aplicativos e gravação na agenda. As duas últimas estão anotadas
como proibidas **para sempre**, não até a fase delas.

**Transcrição pedida pelo plano:** o script rodou **verde antes e depois** desta task. Isso é o
esperado e é a prova de que a ordem estava certa: afrouxar a lista autorizada não pode quebrar
nada, porque nenhuma permissão havia entrado no manifest ainda.

**Task 2 — manifest, serviço fino, armazém e hospedeiras** (`6794d6d`)

Manifest copiado do bloco medido, comentado em prosa apontando para a matriz. O meta-dado que
transferiria ao aplicativo a responsabilidade de tocar o toque de chamada **não** foi declarado
(critério verificado em 0 ocorrências). `CallActivity` recebeu os atributos de tela bloqueada e
**nenhum** filtro de intenção.

`CallSessionStore` guarda estado de domínio, costura e identificador opaco — nenhum objeto de
chamada da plataforma entra no arquivo. `TelecomCallControls` traduz os sete comandos, consultando
a máscara de rotas antes de oferecer alto-falante. `SentinelaInCallService` tem **zero** captura de
defeito, registra o observador ao receber a chamada e o remove ao perdê-la, e pede o container da
aplicação em vez de construir um próprio (ambos verificados por critério).

O KDoc do serviço registra os dois avisos que o plano pediu: por que falhar alto é a degradação
correta desta camada, e que o sistema vincula **vários** serviços de chamada à mesma ligação — só
quem declara a substituição da interface mostra tela — para o próximo executor não se assustar com
o ruído no diagnóstico.

**Task 3 — papel, estado do modo e Bloco 8** (`83c6f8d`)

`SystemRoleGate` parametriza a consulta pelo papel; `ScreeningRoleManager` virou uma linha e
`DialerRoleManager` acrescenta apenas a intenção de reversão. `DialerModeState` é pura, com a
precedência travada por teste e o caso explícito de que **papel detido vence intenção gravada**.
Bloco 8 acrescentou quatro checagens antes do somatório de falhas.

## Provas de vermelho do Bloco 8 (as quatro, executadas e restauradas)

| Sabotagem | Resultado medido |
|---|---|
| 8.1 — segundo filtro de discagem removido do manifest, com re-merge real | **2 vermelhos**: "manifest mergeado tem 1 filtro(s) da acao de discagem (esperado 2)" e "nenhum filtro de discagem com o esquema de telefone"; restaurado e re-mergeado |
| 8.2 — chamada ao método do gerenciador de pacotes que desabilita componente próprio | **1 vermelho** com a mensagem em prosa sobre o sistema remover o papel e encerrar o aplicativo; restaurado |
| 8.3 — construção de intenção pela ação direta de ligar | **1 vermelho** apontando o gerenciador de telecomunicações como origem correta; restaurado |
| 8.4 — import da camada de interface no armazém **e** import da plataforma no coordenador | **2 vermelhos**, um por checagem; restaurado |

A sabotagem 8.1 foi feita reeditando o manifest fonte e rodando o build de novo, não editando o
manifest mesclado: o invariante lê o artefato mesclado de propósito, e falsificar o artefato
provaria o grep, não o invariante.

Todas as sabotagens foram aplicadas em arquivos **já commitados**, e a restauração de 8.2 a 8.4 foi
por `git checkout` do arquivo — seguro justamente porque o trabalho novo já estava no índice. É a
lição registrada no plano 06-02, aplicada aqui de propósito.

## Verificação

```
./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest lint detekt
  -> BUILD SUCCESSFUL, 58 actionable tasks: 58 executed
bash scripts/verify-invariants.sh   -> 38 "ok", todos os invariantes OK (8 blocos)
./gradlew koverLog                  -> 96,0759%
```

**540 testes JVM** no total, **31 novos** neste plano: 9 no serviço de chamada, 4 na compatibilidade
da recusa (dois níveis de plataforma), 7 no papel de discador e 11 no estado do modo. Nenhuma
reflexão em nenhum deles — critério verificado em 0 ocorrências, e a pesquisa já havia medido que
ela é desnecessária nesta camada.

Critérios de aceite por grep, todos conferidos:

| Critério | Medido | Esperado |
|---|---|---|
| filtros da ação de discagem no manifest | 2 | 2 |
| esquema de telefone | 1 | 1 |
| permissão de vínculo do serviço de chamada | 1 | >= 1 |
| meta-dado de toque de chamada | 0 | 0 |
| container construído dentro do serviço | 0 | 0 |
| interceptação de defeito no serviço | 0 | 0 |
| tipo de chamada da plataforma na camada de interface | 0 arquivos | 0 |
| reflexão no teste do serviço | 0 | 0 |
| marcador de pendência da fase no container | 0 | 0 |
| casos em `DialerModeStateTest` | 11 | >= 8 |
| casos em `DialerRoleManagerTest` | 7 | >= 5 |
| `import android.` em `DialerModeState.kt` | 0 | 0 |
| checagens do Bloco 8 | 4 | 4 |

A prova mais importante: **o script fica verde COM as quatro permissões presentes no manifest
mesclado**, com as três de `uses-permission` reportadas nominalmente como autorizadas. É a evidência
de que a task 1 e a task 2 pertencem ao mesmo trabalho, como a Fase 4 exigiu.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrida perdia o retrato final da chamada**
- **Found during:** Task 2, no caso "perder a chamada limpa o armazém" — vermelho legítimo
- **Issue:** o espelho do armazém roda num escopo real. Cancelá-lo em `detach()` logo depois de a
  sessão ir ao estado final fazia o último retrato — o encerramento — às vezes nunca chegar a quem
  observa. Quem estivesse com a tela aberta ficaria olhando uma chamada ativa já terminada, que é
  exatamente a categoria de falha que esta fase existe para evitar.
- **Fix:** `detach()` publica o retrato final de forma síncrona antes de cancelar o espelho, e a
  publicação foi extraída para uma função única usada pelos dois caminhos.
- **Files modified:** `telecom/call/CallSessionStore.kt`
- **Commit:** `6794d6d`

**2. [Rule 3 - Blocking] Lint reprovou a permissão nova sem a declaração de telefonia**
- **Found during:** Task 2, `./gradlew lint`
- **Issue:** `PermissionImpliesUnsupportedChromeOsHardware` — **erro**, não aviso: originar chamada
  implica capacidade de telefonia, e sem declará-la como não obrigatória o aplicativo sairia da
  loja para aparelhos sem rádio.
- **Fix:** `<uses-feature android:name="android.hardware.telephony" android:required="false" />`,
  comentado em prosa. É a declaração correta e não apenas o silenciamento do lint: o aplicativo de
  fato roda sem telefonia, só sem o modo discador — e `DialerModeState.UNAVAILABLE` já é o estado
  desse aparelho.
- **Commit:** `6794d6d`

**3. [Rule 1 - Bug no teste] Verificar a sobrecarga moderna no piso da plataforma estoura**
- **Found during:** Task 2, `CallRejectLegacyCompatTest`
- **Issue:** afirmar `verify(exactly = 0)` sobre a recusa com motivo declarado, no nível 29, lança
  erro de método ausente — a assinatura **não existe** naquele nível. O vermelho não era do
  produto: era o próprio teste tropeçando na incompatibilidade que ele deveria provar.
- **Fix:** o caso passou a afirmar que a recusa antiga aconteceu uma vez e que **nenhuma outra
  interação** com a chamada ocorreu (`confirmVerified`). Prova mais forte e que não depende de uma
  assinatura inexistente.
- **Commit:** `6794d6d`

### Divergências deliberadas do texto do plano

- **Dois arquivos além da lista do plano:** `SystemRoleGate.kt` (novo) e `ScreeningRoleManager.kt`
  (reduzido a uma linha). O plano manda explicitamente não duplicar a lógica do gerenciador de
  triagem e autoriza "extrair o que der para uma base comum ou parametrizar pelo papel"; a base
  ficou em arquivo próprio por clareza, em vez de escondida no arquivo do discador. A superfície
  pública de `ScreeningRoleManager` não mudou e os seis casos da Fase 5 passam sem edição.
- **`uses-feature` de telefonia** no manifest, que o bloco de interfaces não previa — motivo no
  desvio 2 acima.
- **`CallActivity` e `DialerActivity` hospedam composables privados no próprio arquivo**, em vez de
  arquivos de tela separados. As telas de verdade são de 06-04 e 06-05; criar arquivos de tela
  agora só produziria arquivos para serem reescritos.
- **Cobertura caiu de 97,70% para 96,08%.** Esperado e não corrigido aqui: os três arquivos novos
  que falam com a plataforma (`SentinelaInCallService`, `TelecomCallControls`, `CallSessionStore`)
  vivem em `telecom.*`, que **está** no denominador do Kover, e parte deles só é exercitável em
  aparelho. O gate é 80% e continua folgado. Ajustar o filtro é o plano 06-08, e **alargá-lo aqui
  era proibido** pelo próprio plano.

### Escopo intocado, como mandado

`CallDecisionEngine` não foi tocado. Nenhuma biblioteca nova. Nenhuma chamada de rede. Nenhum
`checkpoint`. Filtro do Kover intacto.

## Para os planos da onda 3

- **Quem é dono do retorno de mudança de estado:** `app/src/main/java/org/sentinela/app/telecom/call/CallSessionStore.kt`.
  Ele é o único lugar que sabe quando o retrato mudou, e expõe `CallSessionObserver` com
  `addObserver`/`removeObserver` justamente para isso. **O plano 06-06 registra o notificador ali,
  não dentro do serviço da plataforma** — o serviço precisa continuar fino. Ao registrar, o
  observador recebe imediatamente o retrato corrente, então não há janela cega.
- **A transição para chamada ativa** é observável pelo mesmo caminho: o retrato chega com
  `state == CallUiState.Active` e `startedAtMillis` preenchido. Não existe retorno separado só para
  ela, e criar um seria um segundo caminho para o mesmo fato.
- **`onDialerModeReverted(store, notifications)`** já existe em `DialerModeState.kt` com a costura
  pura `CallNotificationCanceller`; 06-06 só precisa implementá-la.
- **06-05** herda `DialerActivity.placeCall`, que hoje origina a chamada **apenas** se a permissão
  já estiver concedida. O pedido em runtime e o tratamento de erro de discagem são daquele plano.
- **06-04** substitui o composable `CallScreen` de `CallActivity.kt`. Duas coisas não podem sumir
  na substituição: a confirmação de apresentação (sem ela a sessão falha alto em 2 s por desenho) e
  o consumo do gesto de voltar durante chamada em curso.
- `identityOf` no serviço resolve só o que a própria ligação informa. Distinguir contato de número
  liberado é da tela, em memória — nenhum dado da agenda pode chegar ao armazém.

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim, como o contexto da fase prevê.

## Self-Check: PASSED

Os 12 arquivos criados e os 5 modificados existem em disco; os 3 commits declarados
(`4f264f3`, `6794d6d`, `83c6f8d`) existem no histórico.

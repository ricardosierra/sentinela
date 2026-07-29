---
phase: 05-triagem-telecom-modo-filtro
plan: 04
subsystem: notifications+permissions+settings
tags: [ntf-01, ntf-02, ntf-03, ntf-04, ntf-05, privacidade]
requires:
  - BlockedCallEntry + BlockedCallRepository (Fase 3)
  - PhoneMask (Fase 2)
  - ContactsPermissionState (Fase 4)
  - POST_NOTIFICATIONS declarada no manifest (Fase 1)
provides:
  - RuntimePermissionAsk (regra generica de permissao em runtime)
  - NotificationPermissionChecker
  - AndroidBlockedCallNotifier
  - ScreeningSettings.notificationIdentification + NotificationIdentification
  - DataStoreSettingsRepository.notificationPermissionAsked / markNotificationPermissionAsked
affects:
  - ScreeningCoordinator (plano 05-03, chama notifyBlocked depois do respondToCall)
  - Tela de configuracoes (Fase 7, interruptor de opt-in + escolha de identificacao)
  - Tela de historico (Fase 8, consome EXTRA_ENTRY_ID da notificacao)
tech-stack:
  added: []
  patterns:
    - "Regra de permissao em runtime e uma so; enums nomeados viram fachada que delega"
    - "Camada que toca ActivityCompat/ContextCompat vive em platform/, fora do gate do Kover"
    - "Privacidade da notificacao e provada varrendo os extras do objeto postado, nao por revisao"
    - "Enum persistido pelo name, nunca pela posicao; leitura tolerante cai no padrao"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/permissions/RuntimePermissionAsk.kt
    - app/src/main/java/org/sentinela/app/platform/NotificationPermissionChecker.kt
    - app/src/main/java/org/sentinela/app/notifications/AndroidBlockedCallNotifier.kt
    - app/src/test/java/org/sentinela/app/permissions/RuntimePermissionAskTest.kt
    - app/src/test/java/org/sentinela/app/notifications/BlockedCallNotifierTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/data/contacts/ContactsPermissionState.kt
    - app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/main/java/org/sentinela/app/notifications/BlockedCallNotifier.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/sentinela/app/settings/DataStoreSettingsRepositoryTest.kt
decisions:
  - "Regra de permissao em runtime extraida para RuntimePermissionAsk; ContactsPermissionState virou fachada que delega"
  - "AndroidBlockedCallNotifier nao recebe PhoneNumberUtil: a mascara ja vem pronta no registro"
  - "notification_permission_asked fora de ScreeningSettings, espelhando a decisao da Fase 4"
  - "Versao publica da notificacao usa sempre o texto anonimo, independente da configuracao"
  - "Id da notificacao derivado do id do registro (base 1000 + id % 1000)"
metrics:
  duration: ~40min
  tasks: 3
  completed: 2026-07-29
---

# Phase 5 Plan 04: Notificação própria opcional e silenciosa Summary

Notificação própria entregue como opt-in silencioso que **nunca** carrega a sequência completa
de dígitos do número em campo algum do objeto postado — provado por varredura dos extras, não
por revisão — mais a extração da regra de permissão em runtime para um único lugar, agora
compartilhado entre agenda (Fase 4) e notificações (Fase 5).

## O que foi construído

**Task 1 — regra única de permissão e configuração de identificação.**
`permissions/RuntimePermissionAsk.kt` traz o enum de 4 estados (`GRANTED`, `NEVER_ASKED`,
`DENIED_ONCE`, `DENIED_PERMANENTLY`), a função pura `runtimePermissionAsk(granted,
alreadyAsked, rationale)` e as extensões `canRequest` / `shouldOfferSystemSettings`, sem nenhuma
importação da plataforma. `contactsPermissionState(...)` passou a **delegar** a ela com um `when`
de mapeamento de 4 linhas — os testes da Fase 4 seguiram verdes **sem uma única edição**.
`platform/NotificationPermissionChecker` espelha o `ContactsPermissionChecker`, com a diferença
de que `isGranted` responde `true` sem consultar nada abaixo da API 33 (minSdk 29: a permissão
nem existe lá). `ScreeningSettings` ganhou `notificationIdentification` com padrão `MASKED` e o
enum `NotificationIdentification { MASKED, ANONYMOUS }`; `showOwnNotification` foi **conferido e
permanece `false`** — a notificação continua desligada por padrão. No DataStore entraram
`notification_identification` (persistida pelo `name`, leitura tolerante) e
`notification_permission_asked`, com `markNotificationPermissionAsked()` documentado como
"marcar ao disparar o launcher, nunca no callback".

**Task 2 — `AndroidBlockedCallNotifier`.** `ensureChannel()` cria o canal com importância baixa,
`setSound(null, null)`, `enableVibration(false)`, `enableLights(false)` e `setShowBadge(false)`;
é idempotente por contrato da plataforma e chamado no opt-in e antes de cada envio, nunca em
`Application.onCreate`. `notifyBlocked(entry)` monta o conteúdo a partir de `entry.maskedNumber`
ou do texto anônimo, com `setSilent(true)`, `setVibrate(null)`, `setSound(null)`,
`VISIBILITY_PRIVATE`, uma versão pública que usa **sempre** o texto anônimo e um `PendingIntent`
para `MainActivity` com `FLAG_IMMUTABLE`, carregando apenas `EXTRA_ENTRY_ID`. Nunca há
`setFullScreenIntent`. Todo o corpo vive dentro de `runCatching`: falha ao notificar não escapa
para o caminho da triagem.

**Task 3 — strings pt-BR.** Nenhuma das quatro strings já existentes foi duplicada. Entraram
`notification_channel_blocked_desc`, `settings_notification_enable`,
`settings_notification_enable_desc`, `settings_notification_identification_masked`,
`settings_notification_identification_anonymous` e `notification_permission_rationale`. Nenhum
texto promete bloqueio garantido, ausência de registro no histórico do telefone ou filtragem de
aplicativos de mensagem/voz.

## Verificação

- `./gradlew assembleDebug testDebugUnitTest koverLog lint detekt` **verde**:
  **401 casos de teste**, cobertura **96,6921%**, lint e detekt zerados.
- `bash scripts/verify-invariants.sh`: `== todos os invariantes OK ==`.
- **Nenhuma permissão nova**: Bloco 1 do verificador inalterado. `POST_NOTIFICATIONS` já estava
  declarada desde a Fase 1; este plano acrescentou só o caminho do pedido em runtime.
- `RuntimePermissionAskTest`: 11 `@Test`, incluindo a tabela das 8 combinações booleanas e o
  travamento do enum em 4 entradas na ordem contratada.
- `BlockedCallNotifierTest`: 11 `@Test` sob Robolectric `@Config(sdk = [35])`.
- `DataStoreSettingsRepositoryTest`: 8 testes novos (padrão `MASKED`, round-trip `ANONYMOUS`,
  persistência pelo `name`, valor desconhecido em disco caindo no padrão, flag sobrevivendo à
  recriação, chave textual no disco, flag não contaminando o snapshot, e um assert explícito de
  que a notificação continua desligada por padrão).

### Prova de vermelho — vazamento do número completo

Trocando o conteúdo mascarado pelo número completo (`entry.numberE164.orEmpty()`):

```
BlockedCallNotifierTest > com identificacao mascarada o texto usa a mascara do registro FAILED
BlockedCallNotifierTest > nenhum campo da notificacao carrega a sequencia completa do numero FAILED
11 tests completed, 2 failed
```

Restaurado e reconfirmado verde. O teste de vazamento coleta título, texto, todos os extras, os
extras da versão pública e o ticker, e afirma que nenhum contém `5511999998888`, `11999998888`
nem `999998888` — nas duas configurações de identificação.

### Prova de que a Fase 4 não regrediu

`git diff --name-only` sobre `ContactsPermissionStateTest.kt` devolveu **0 linhas**: o arquivo de
teste da Fase 4 não foi tocado, e a suíte passou com a regra já delegada.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Bloqueio] Autossabotagem do KDoc no critério de aceite**
- **Found during:** Task 1
- **Issue:** o critério exige zero ocorrências de `import android.` em `RuntimePermissionAsk.kt`,
  e o próprio KDoc que eu havia escrito continha a frase "sem nenhum `import android.*`" — o
  grep não distingue documentação de código, e o critério ficou vermelho por causa do comentário.
- **Fix:** a frase virou "sem nenhuma importação da plataforma", preservando o sentido.
- **Commit:** 94fa8e0

**2. [Rule 3 - Bloqueio] Mesma armadilha com `IMPORTANCE_LOW`**
- **Found during:** Task 2
- **Issue:** o critério exige **exatamente 1** ocorrência de `IMPORTANCE_LOW`; um comentário
  meu citava a constante, elevando a contagem para 2.
- **Fix:** o comentário passou a dizer "importância baixa" em português.
- **Commit:** 64f4fe6

**3. [Rule 3 - Bloqueio] String do canal necessária antes da Task 3**
- **Found during:** Task 2
- **Issue:** `ensureChannel()` precisa de `notification_channel_blocked_desc`, que o plano só
  previa na Task 3 — o código não compilaria.
- **Fix:** essa string entrou no commit da Task 2; as demais seguiram na Task 3 como planejado.
- **Commit:** 64f4fe6

### Desvio de assinatura (deliberado)

O plano sugeria `AndroidBlockedCallNotifier(context, phoneNumberUtil, settingsSnapshotProvider)`.
O `PhoneNumberUtil` foi **omitido**: o conteúdo da notificação sai de `entry.maskedNumber`, que já
chega mascarado do registro, e o critério de aceite proíbe justamente ler `numberE164` aqui. Um
parâmetro nunca usado só criaria a tentação futura de remascarar dentro do notificador. O vínculo
com a máscara única continua real e verificável: `PhoneMask.MASCARA_GENERICA` é o texto usado
quando o registro chega sem máscara utilizável — nunca eco da entrada crua.

### Fora de escopo (não tocado)

Durante a execução, `detekt` acusou 4 issues em
`app/src/main/java/org/sentinela/app/telecom/ScreeningCoordinator.kt` (`LongParameterList`,
`SwallowedException` x2, `EmptyCatchBlock`) e algumas execuções do Gradle falharam com
`EOFException` / erro transitório de `compileDebugKotlin`. Todos pertencem ao plano 05-03,
executado em paralelo por outro agente — deixados intactos de propósito; os comandos foram
repetidos e fecharam verdes depois que o outro agente estabilizou seus arquivos.

## Pendências para os próximos planos

- O plano 05-03 é dono da ordem: `notifyBlocked` só pode ser chamado **depois** do
  `respondToCall`, e apenas quando `showOwnNotification` estiver ligado.
- A Fase 7 precisa ligar o interruptor de opt-in ao `NotificationPermissionChecker`, gravando
  `markNotificationPermissionAsked()` **ao disparar** o launcher, e expor a escolha entre
  `MASKED` e `ANONYMOUS`.
- A Fase 8 consome `AndroidBlockedCallNotifier.EXTRA_ENTRY_ID` para abrir o registro do histórico.
- O `AppContainer` ainda não constrói o notificador — a ligação pertence ao plano que fizer o
  Service usá-lo.
- Ícone pequeno da notificação usa `ic_launcher_foreground` por ora; arte definitiva é decisão
  pendente já registrada no STATE.

## Self-Check: PASSED

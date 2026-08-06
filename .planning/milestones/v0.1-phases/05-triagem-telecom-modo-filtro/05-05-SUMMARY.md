---
phase: 05-triagem-telecom-modo-filtro
plan: 05
subsystem: telecom
tags: [scr-01, scr-02, scr-03, scr-05, scr-08, scr-09, scr-12, ntf-01, ntf-06, ligacao]
requires:
  - telecom/ScreeningCoordinator (plano 05-03)
  - telecom/ScreenedCallFactory + CallResponseFactory + ScreeningTestHarness (plano 05-02)
  - notifications/AndroidBlockedCallNotifier (plano 05-04)
  - data/local/BlockedCallRepository (Fase 3)
provides:
  - telecom/ScreeningDependencies (contrato do container para o Service)
  - telecom/PostScreeningWork (historico + notificacao depois da resposta)
  - domain/blocksCall
  - AppContainer.screeningCoordinator / blockedCallNotifier / launchAfterResponse
  - test/AmbienteDeTriagem (container de teste com dubles)
affects:
  - 05-06 (validacao fim a fim)
  - 05-07 (filtro do Kover sobre telecom.*)
  - Fase 7 (tela inicial consulta o papel a cada retomada)
  - Fase 8 (historico consome o id devolvido por record)
tech-stack:
  added: []
  patterns:
    - "Container implementa um contrato pequeno; o Service pede colaboradores, nunca constroi"
    - "Costura de resposta carrega a decisao E as configuracoes que a produziram"
    - "Trabalho pos-resposta isolado em colaborador proprio, fora do arquivo do Service"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/telecom/ScreeningDependencies.kt
    - app/src/main/java/org/sentinela/app/telecom/PostScreeningWork.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreeningServiceTest.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreeningRoleManagerTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt
    - app/src/main/java/org/sentinela/app/telecom/ScreeningCoordinator.kt
    - app/src/main/java/org/sentinela/app/telecom/ScreeningRoleManager.kt
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/src/main/java/org/sentinela/app/domain/CallDecision.kt
    - app/src/main/java/org/sentinela/app/data/local/BlockedCallRepository.kt
    - app/src/main/java/org/sentinela/app/data/local/RoomBlockedCallRepository.kt
    - app/src/main/java/org/sentinela/app/settings/DataStoreSettingsRepository.kt
    - app/src/test/java/org/sentinela/app/telecom/FakeScreeningDependencies.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreeningTestHarness.kt
    - app/src/test/java/org/sentinela/app/telecom/ScreeningCoordinatorOrderTest.kt
decisions:
  - "Contrato ScreeningDependencies em vez de o Service conhecer o AppContainer: e o que permite hospedar o Service real na JVM sem construir um segundo container"
  - "Costura de resposta passou a (CallDecision, ScreeningSettings): traducao usa exatamente as configuracoes que o motor usou, sem segunda leitura do repositorio"
  - "Historico e notificacao sairam do arquivo do Service para PostScreeningWork — o Service ficou com 57 linhas e zero condicao de destino de chamada"
  - "record() devolve o id da linha (0 quando nao ha rastro): sem ele a notificacao nao abre o registro certo e todas colidiriam no mesmo id"
  - "blocksCall vive no dominio: a camada da plataforma nao pode carregar condicao propria sobre o destino de uma chamada"
  - "buildRequestIntent condicionada a isRoleAvailable: o sistema devolve intencao mesmo em aparelho que nao oferece o papel"
metrics:
  tasks: 3
  tests_added: 16
  completed: 2026-07-29
---

# Phase 05 Plan 05: Service ligado ao coordenador Summary

Este e o commit em que o produto passa a bloquear de verdade: o `UnknownCallScreeningService`
deixou de ser passagem livre e entrega ao sistema exatamente a decisao do motor, uma unica vez por
chamada, provado no adaptador real capturado pelo harness.

## O que foi construido

**Task 1 — fiacao no container.** `AppContainer` ganhou quatro colaboradores preguicosos
(`screenedCallFactory`, `callResponseFactory`, `blockedCallNotifier`, `screeningCoordinator`), mais
`postScreeningWork` e `launchAfterResponse`, que empresta o escopo do processo em vez de expor o
campo. Nada foi para `Application.onCreate`. O container passou a cumprir o contrato
`ScreeningDependencies`, criado neste plano: e ele que permite ao Service pedir colaboradores sem
nunca construir os seus — construir um segundo derruba o processo, fato ja medido na Fase 3.

**Task 2 — Service delegando.** `onScreenCall` monta a chamada pela fabrica, sai cedo em chamada de
saida e delega ao coordenador no escopo do processo, traduzindo a decisao pela fabrica de respostas.
O arquivo tem **57 linhas**, uma unica ocorrencia de `respondToCall`, zero condicao de bloqueio e
zero pendencia anotada. O KDoc antigo, que afirmava que o Service so recebe numeros fora da agenda e
que passa "ausente" ao motor, era **falso** desde que o aplicativo passou a poder consultar a
agenda — foi reescrito em prosa, sem citar nome de permissao (a armadilha que ja derrubou tres
executores nesta fase).

**Task 3 — papel de triagem.** `buildRequestIntent` passou a devolver nada quando o aparelho nao
oferece o papel; o KDoc registra que **nao existe** aviso de mudanca de papel para aplicativo
comum (o unico ouvinte e de sistema e exige permissao fora da lista permitida), e que por isso a
verificacao so pode ser pergunta pontual na retomada da tela, na Fase 7.

## Provas de vermelho registradas

| # | O que foi quebrado | Resultado |
|---|--------------------|-----------|
| 1 | `ScreeningServiceTest` contra o Service ainda em passagem livre | **5 de 10** vermelhos |
| 2 | Retorno antecipado da chamada de saida removido do Service | **0** vermelhos — ver nota |
| 3 | `buildRequestIntent` sem a condicao de disponibilidade | 1 de 6 vermelho, exatamente o caso do aparelho sem o papel |

**Nota honesta sobre a prova 2.** Remover o retorno antecipado do Service **nao** deixou nenhum
teste vermelho, e isso e fato de desenho, nao falha do teste: o coordenador tambem sai sem emitir
nada em chamada de saida, entao as duas guardas sao redundantes de proposito. A do Service continua
valendo a pena porque evita montar corrotina e consultar dado local por nada; a do coordenador e a
que garante zero respostas. Mesmo padrao registrado na prova 2 do plano 05-03. Restaurada e
reconfirmada verde.

## Cobertura de comportamento

`ScreeningServiceTest`, 10 casos sobre o adaptador real, `@Config(sdk = [35])`: desconhecido barrado
com uma resposta (recusa + supressao da notificacao nativa), contato da agenda tocando com uma
resposta sem recusa, chamada de saida com **zero** respostas, chamada sem identificacao respondida
uma vez sem derrubar o servico, defeito interno respondendo uma vez de forma permissiva, duas
chamadas seguidas produzindo duas respostas (uma para cada), historico gravado so em chamada
barrada, nada gravado quando a chamada passa, e a notificacao acontecendo somente com o interruptor
ligado — com o id devolvido pela gravacao chegando ao objeto notificado.

`ScreeningRoleManagerTest`, 6 casos com o gerenciador de papeis sombreado, incluindo aparelho sem o
servico de sistema, em que tudo responde negativo sem lancar.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Funcionalidade critica] `record` nao devolvia o identificador da linha**
- **Found during:** Task 1
- **Issue:** a notificacao precisa do id do registro para abrir a entrada certa do historico
  (Fase 8). Com a assinatura antiga, toda notificacao carregaria id zero e todas colidiriam no
  mesmo identificador, sobrescrevendo uma a outra.
- **Fix:** `BlockedCallRepository.record` passou a devolver `Long` — o id da linha, ou zero quando a
  configuracao manda nao guardar rastro. Implementacao Room e dubles atualizados.
- **Commit:** 5165308

**2. [Rule 3 - Bloqueio] Traducao da resposta precisava das configuracoes**
- **Found during:** Task 2
- **Issue:** a fabrica de respostas exige as configuracoes, e a costura do coordenador entregava so
  a decisao. Ler o repositorio de novo dentro da costura e impossivel (a costura nao suspende) e
  arriscaria traduzir com valor diferente do que o motor usou.
- **Fix:** costura passou a `(CallDecision, ScreeningSettings) -> Unit`, exatamente como o plano
  autorizava. Os testes do plano 05-03 foram atualizados e seguem verdes, sem perder nenhum caso.
- **Commit:** 30d04a6

**3. [Rule 3 - Bloqueio] Teste de fumaca do harness media o modo de passagem livre**
- **Found during:** Task 2
- **Issue:** com o Service delegando, o teste de fumaca do plano 05-02 passou a produzir zero
  respostas, porque nao havia colaborador algum ligado.
- **Fix:** o teste passou a usar o container de teste com um contato da agenda — a chamada passa,
  os cinco campos continuam falsos e o que ele mede segue sendo a captura, nao a decisao.
- **Commit:** 30d04a6

### Desvios de estrutura (deliberados)

- **Dois arquivos novos que o plano nao listava.** `ScreeningDependencies` existe para o Service
  nunca conhecer o container concreto — sem ele, hospedar o Service real na JVM exigiria construir
  a infraestrutura inteira, o que derruba o processo do teste. `PostScreeningWork` existe para o
  Service caber no limite de linhas mantendo o registro e a notificacao testaveis fora dele.
- **`blocksCall` no dominio.** Decidir se houve rastro a guardar e pergunta de dominio; deixar essa
  condicao no arquivo do Service violaria a regra de que nenhuma condicao sobre o destino de uma
  chamada mora ali.
- **`cachedSnapshot()` no repositorio de configuracoes.** O notificador le a configuracao por uma
  funcao que nao suspende; cache frio devolve os padroes do MVP, que sao os mais conservadores.
  Nenhuma decisao de triagem usa esse caminho.

## Verificacao final

```
./gradlew assembleDebug testDebugUnitTest koverLog lint detekt   -> BUILD SUCCESSFUL
application line coverage: 96,5174%
bash scripts/verify-invariants.sh                                -> == todos os invariantes OK ==
```

Criterios medidos no `UnknownCallScreeningService.kt`: `as SentinelaApp` = 1, construcao de
container = 0, `respondToCall` = 1, condicao de bloqueio = 0, pendencia anotada = 0, 57 linhas.
No `AppContainer.kt`: colaboradores preguicosos = 4, `launchAfterResponse` = 1, agendador em
segundo plano = 0, pendencia da Fase 5 = 0. No `ScreeningRoleManager.kt`: observador de papel = 0,
permissao proibida = 0.

O pacote `telecom.*` continua fora do denominador do Kover — alargar o filtro e trabalho do plano
05-07, e o `kover { }` nao foi tocado aqui.

## Pendencias para os proximos planos

- **05-06:** a validacao fim a fim agora tem um Service que decide de verdade; o caminho de saida
  tem guarda dupla (Service e coordenador) por desenho.
- **05-07:** incluir `org.sentinela.app.telecom.*` no filtro do Kover e corrigir formalmente a nota
  da STATE.md sobre o SDK do Robolectric.
- **Fase 7:** a tela inicial consulta o papel a cada retomada; nao existe observador a procurar.
- **Fase 8:** o id devolvido por `record` e o que liga a notificacao ao registro do historico.

## Self-Check: PASSED

Quatro arquivos criados conferidos no disco e tres commits conferidos no historico.

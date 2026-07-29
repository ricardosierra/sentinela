---
phase: 05-triagem-telecom-modo-filtro
plan: 06
subsystem: telecom
tags: [scr-09, scr-10, scr-11, qlt-06, invariantes, performance, aosp]
requires:
  - telecom/UnknownCallScreeningService ligado ao coordenador (plano 05-05)
  - telecom/ScreeningCoordinator (plano 05-03)
  - scripts/run-instrumented-tests.sh (Fase 3)
  - data/contacts/ContactsTestFixture (Fase 4)
provides:
  - androidTest/ScreeningServiceBindTest (vinculo real + container unico)
  - androidTest/DecisionPerformanceTest (percentis do caminho de decisao)
  - scripts/verify-invariants.sh Bloco 7 (regra de decisao concentrada no motor)
affects:
  - 05-07 (filtro do Kover sobre telecom.*)
  - Fase 6 (o modo discador nao pode levar regra de bloqueio para a camada de telefonia)
  - Fase 9 (cauda do caminho de decisao vira cenario de validacao fisica)
tech-stack:
  added: []
  patterns:
    - "Assert de tempo apenas na mediana; cauda medida, logada e diferida para aparelho fisico"
    - "Teste de tempo acompanhado de um assert estrutural que prova QUAL caminho foi medido"
    - "Invariante de arquitetura por grep sobre app/src/main/java, com o script fora do proprio escopo"
key-files:
  created:
    - app/src/androidTest/java/org/sentinela/app/telecom/ScreeningServiceBindTest.kt
    - app/src/androidTest/java/org/sentinela/app/telecom/DecisionPerformanceTest.kt
  modified:
    - scripts/verify-invariants.sh
    - app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt
decisions:
  - "Nao responder ao sistema em chamada de saida esta CORRETO e confirmado na fonte do AOSP: a classe base envia sozinha uma resposta nula assim que onScreenCall retorna, e respondToCall e documentadamente ignorado fora de DIRECTION_INCOMING"
  - "O invariante de resposta unica foi reescrito para valer sobre chamadas de ENTRADA — a formulacao antiga (todos os caminhos) codificava uma regra falsa"
  - "Bloco 7.2 e 7.4 exigem o parenteses de construcao/chamada: citar o nome em prosa e legitimo e nao pode derrubar o invariante"
  - "O comentario sobre o modo de abortar do shell passou a DESCREVER o literal em vez de escreve-lo, pelo precedente do Migrations.kt: criterio por grep nao distingue comentario de codigo"
  - "O teste de tempo ganhou um assert estrutural sobre a decisao medida — sem ele o cronometro poderia estar medindo um caminho curto sem ninguem notar"
metrics:
  tasks: 3
  tests_added: 5
  duration: ~50min
  completed: 2026-07-29
---

# Phase 05 Plan 06: Prova automatizada da triagem Summary

Fecha a prova automatizada da fase: o sistema consegue se ligar ao servico de verdade no aparelho
virtual, o caminho de decisao tem medida reproduzivel com assercao estavel na mediana, e migrar
regra de bloqueio para fora do motor passou a quebrar o build.

## A questao aberta do plano: chamada de saida sem resposta

O plano 05-05 fez o servico retornar em chamada de saida **sem** chamar `respondToCall`, o que
contradizia o invariante escrito no proprio arquivo ("resposta exatamente uma vez em todos os
caminhos"). A instrucao era verificar na fonte, nao no palpite. Foi verificado em
`~/Library/Android/sdk/sources/android-35/android/telecom/CallScreeningService.java`.

**Veredito: nao responder esta correto — e responder e que seria errado.** Duas evidencias diretas
da fonte:

1. No tratador de mensagens da classe base (linhas 140-145), logo depois de `onScreenCall(...)`
   retornar:

   ```java
   onScreenCall(callDetails);
   if (callDetails.getCallDirection() == Call.Details.DIRECTION_OUTGOING) {
       mCallScreeningAdapter.onScreeningResponse(
               callDetails.getTelecomCallId(), new ComponentName(...), null);
   }
   ```

   A plataforma envia sozinha uma resposta nula para toda chamada de saida. Nao existe temporizador
   pendente, nao existe aviso e nao existe desvinculo punitivo: a resposta sai no mesmo instante em
   que o nosso metodo retorna.

2. A documentacao de `respondToCall` (linhas 651-663) e explicita nos dois pontos:
   *"Calls to this method are ignored unless the `Call.Details#getCallDirection()` is
   `DIRECTION_INCOMING`"* e *"**For incoming calls**, a CallScreeningService MUST call this method
   within 5 seconds"*. O prazo de cinco segundos e cobrado de chamada de entrada, so.

Responder em chamada de saida somaria uma resposta descartada a resposta automatica da plataforma
— exatamente o cenario de resposta dupla que o resto da fase existe para impedir.

**O que mudou por causa disso.** O KDoc do `UnknownCallScreeningService` ganhou um paragrafo em
prosa com essa justificativa, e o invariante foi corrigido: passou de "exatamente uma vez em todos
os caminhos" para "em toda chamada de **entrada**, exatamente uma vez, inclusive nos caminhos de
falha", mais uma linha dizendo que em chamada de saida este arquivo nao responde nada. A formulacao
antiga codificava uma regra falsa. Pelo mesmo motivo, a checagem 7.4 do Bloco 7 carrega um
comentario dizendo que a contagem vale para chamada de entrada e por que.

Nenhum teste novo foi necessario: `ScreeningServiceTest` (plano 05-05) ja trava **zero respostas**
em chamada de saida, e agora esse assert tem justificativa de fonte em vez de intuicao.

## O que foi construido

**Task 1 — `ScreeningServiceBindTest`, 3 casos.** `ServiceTestRule` pede o vinculo de verdade ao
servico e espera um canal nao nulo — o unico jeito de cobrir manifesto, exportacao e permissao de
vinculo, que o compilador nao verifica. Os outros dois casos travam a unicidade do container por
identidade (`===`) e leem as configuracoes pelo container do aplicativo. O arquivo nao constroi
nenhum colaborador (criterio: zero construcoes de container) e registra em KDoc por que **nao**
tenta exercitar a triagem: o canal do vinculo so aceita um objeto de chamada que a plataforma monta
internamente, e o comportamento fica no hospedeiro em JVM do plano 05-02.

**Task 2 — `DecisionPerformanceTest`, 2 casos.** Uma triagem a frio reportada, 20 de aquecimento e
100 medidas sobre o container real, cronometrando do inicio de `screen` ate a resposta traduzida
sair. Um unico assert de tempo, sobre a mediana. O segundo caso e estrutural e existe porque
cronometro nao prova estrutura: ele verifica que a decisao medida **barrou** a chamada, isto e, que
o caminho cronometrado foi o mais longo, e nao um atalho.

**Task 3 — Bloco 7 de `scripts/verify-invariants.sh`, 5 checagens** (o plano pedia quatro; a quarta
virou duas porque "fora do arquivo" e "uma vez dentro do arquivo" sao perguntas diferentes e as
mensagens de erro precisam ser diferentes).

## Percentis medidos

| Execucao | Frio | p50 | p95 | max |
|---|---|---|---|---|
| Classe isolada, 1a | 522,9 ms | **28,7 ms** | 106,9 ms | 180,8 ms |
| Classe isolada, 2a | 666,4 ms | **15,5 ms** | 41,7 ms | 71,6 ms |
| Suite completa (53 testes) | 23,9 ms | **0,79 ms** | 1,50 ms | 1,92 ms |

Orcamento declarado do produto: 200 ms. Limite do assert: 50 ms sobre a mediana (folga de 4x).

Duas leituras honestas destes numeros. Primeira: **so a mediana e sinal**. Entre duas execucoes da
mesma classe, sem uma linha de codigo alterada, o p95 variou de 41,7 ms para 106,9 ms e o maximo de
71,6 ms para 180,8 ms — a mesma instabilidade que a Fase 3 mediu na whitelist e a Fase 4 mediu no
lookup de contatos. **O veredito sobre p95 e maximo pertence a validacao em aparelho fisico da
Phase 9**, como cenario, e nao foi afrouxado: mudou de lugar. Segunda: o numero "a frio" mede o
estado do processo, nao do codigo — 522 ms quando a classe roda sozinha (primeira leitura do
DataStore, abertura do banco, primeira sonda a agenda) e 23,9 ms quando a suite inteira ja aqueceu
tudo. Ele e diagnostico, jamais assercao.

## Provas de vermelho registradas

| # | O que foi quebrado | Saida |
|---|---|---|
| 1 | `<service>` removido do manifesto | `ScreeningServiceBindTest > sistemaConsegueSeVincularAoServicoDeTriagem FAILED` — vinculo devolveu nulo (linha 53). Os outros 2 casos seguiram verdes |
| 2 | `P50_MAX_MS` baixado para 1,0 | `AssertionError: p50=10.452667 ms — ... esperado < 1.0 ms` |
| 3 | Direcao da chamada medida trocada para saida | `AssertionError: a triagem medida nao produziu decisao alguma` — o assert estrutural pegou |
| 4a | `when` sobre a politica de origem no coordenador | `FAIL: politica de triagem citada na camada de telefonia — ela pertence ao CallDecisionEngine` |
| 4b | `CallDecision.Reject(...)` construida no servico | `FAIL: decisao de bloqueio construida fora do dominio` |
| 4c | `import android.telecom.Call` no coordenador | `FAIL: coordenador da triagem importa tipo da plataforma` |
| 4d | Resposta ao sistema chamada duas vezes no servico | `FAIL: servico de triagem tem 2 pontos de resposta ao sistema (esperado 1)` |

Todas restauradas e reconfirmadas verdes.

**Nota sobre a prova 4a:** ela derrubou **dois** invariantes, nao um. Alem do Bloco 7.1, o Bloco 2
reclamou do identificador literal do aplicativo em Kotlin, porque a sabotagem usou o nome
totalmente qualificado do tipo. Efeito colateral do metodo de sabotagem, nao defeito do Bloco 7.

**Nota sobre as provas 2 e 3:** foram executadas em rodadas separadas de proposito. Quebradas
juntas, elas se anulam — com a chamada de saida o cronometro nunca e disparado, a mediana fica em
zero e o assert de tempo passa mesmo com o limite em 1 ms. Isso, por si so, e a razao de o assert
estrutural existir.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Criterio de aceite do proprio plano era insatisfazivel**

- **Found during:** Task 3
- **Issue:** o criterio exigia zero ocorrencias do literal do modo "abortar no primeiro erro" em
  `scripts/verify-invariants.sh`, mas o arquivo ja continha uma ocorrencia desde a Fase 1 — dentro
  do comentario que **proibe** aquele modo. Criterio por grep nao distingue comentario de codigo;
  e a mesma armadilha registrada na STATE.md na propria Fase 5.
- **Fix:** o comentario passou a descrever o modo em vez de escrever o literal, exatamente o
  precedente ja adotado em `Migrations.kt` na Fase 3. Nenhuma informacao foi perdida — o texto
  ficou mais explicito, inclusive sobre por que o literal nao aparece.
- **Commit:** 63dd57f

### Desvios de estrutura (deliberados)

- **Cinco checagens no Bloco 7, nao quatro.** A quarta exigencia do plano tem duas perguntas
  distintas ("ninguem responde fora do servico" e "o servico responde num unico ponto") com
  diagnosticos distintos; unificar produziria uma mensagem de erro que nao diz o que fazer.
- **Padroes de 7.2 e 7.4 exigem parenteses.** Tres arquivos do projeto citam o nome da funcao de
  resposta em prosa, para registrar que rodam **depois** dela — informacao correta que um invariante
  ingenuo transformaria em violacao permanente.
- **Um assert estrutural dentro do teste de tempo.** O plano proibia usar cronometro como prova de
  estrutura, e este teste nao o faz: o assert estrutural nao mede tempo, verifica qual decisao saiu.
  Sem ele, a medicao poderia silenciosamente cronometrar um caminho curto.
- **`ContactsTestFixture.adoptShell` no teste de performance.** Sem a leitura da agenda concedida, a
  consulta devolve indisponivel, a decisao cai na politica de reserva e a chamada **passa** — outro
  caminho, mais curto. A rota de shell e a unica que concede leitura aqui, ja registrada na Fase 4.

## Verificacao final

```
./gradlew assembleDebug testDebugUnitTest lint detekt   -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                        -> == todos os invariantes OK ==
bash scripts/run-instrumented-tests.sh                   -> 53 testes, 0 falhas
```

Criterios medidos: `Bloco 7` = 2 no script, literal do modo de abortar = 0, `|| echo 0` = 0;
`SENTINELA|decision|` = 2 e `p50` = 4 no teste de performance, com **zero** asserts sobre p95 ou
maximo; `ServiceTestRule` = 1 e construcao de container = 0 no teste de vinculo.

O pacote `telecom.*` continua fora do denominador do Kover — alargar o filtro e do plano 05-07, e o
`kover { }` nao foi tocado aqui.

## Pendencias para os proximos planos

- **05-07:** incluir `org.sentinela.app.telecom.*` no filtro do Kover; corrigir a nota da STATE.md
  sobre o SDK do Robolectric.
- **Fase 6:** o modo discador entrega **todas** as chamadas ao servico. O Bloco 7 ja esta no lugar
  para impedir que a regra nova nasca na camada de telefonia.
- **Fase 9:** cauda do caminho de decisao (p95 e maximo) vira cenario de validacao em Samsung
  fisico, junto do cenario 35 da whitelist e do 37 do lookup de contatos.

## Self-Check: PASSED

Dois arquivos criados conferidos no disco; tres commits (83da631, 8e42622, 63dd57f) conferidos no
historico.

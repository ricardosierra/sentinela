---
phase: 06-modo-discador-opcional
plan: 01
subsystem: telecom
tags: [modo-discador, maquina-de-estado, dtmf, falha-alta, jvm]
requires:
  - "kotlinx.coroutines (StateFlow, TestDispatcher) — já no projeto"
provides:
  - "CallUiState / CallSnapshot / CallOrigin / CallAudioRoute — estados de domínio da chamada"
  - "CallStateMapper + PlatformCallStateMapper — tradução exaustiva do código de estado"
  - "CallControls — costura de comandos da interface para a telefonia"
  - "CallSessionCoordinator — máquina de estado pura com prazo de apresentação de 2 s"
affects:
  - "06-02 (UI da chamada consome CallSnapshot e chama confirmPresented)"
  - "06-04/06-05 (InCallService implementa CallControls e alimenta as entradas)"
tech-stack:
  added: []
  patterns:
    - "coordenador puro com relógio injetado e prazo nomeado (precedente da Fase 5)"
    - "falha ALTA no caminho da chamada: exceção propaga, zero captura"
    - "dublê de costura com lista ordenada de eventos, nunca contador"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/telecom/call/CallUiState.kt
    - app/src/main/java/org/sentinela/app/telecom/call/CallStateMapper.kt
    - app/src/main/java/org/sentinela/app/telecom/call/CallControls.kt
    - app/src/main/java/org/sentinela/app/telecom/call/CallSessionCoordinator.kt
    - app/src/test/java/org/sentinela/app/telecom/call/CallStateMapperTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/CallSessionCoordinatorTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/DtmfPairingTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/CallSessionFailureTest.kt
    - app/src/test/java/org/sentinela/app/telecom/call/CallSessionWatchdogTest.kt
  modified: []
decisions:
  - "Fluxo observável é StateFlow<CallSnapshot> (estado + identidade + controles), não StateFlow<CallUiState> puro — o plano autoriza o invólucro"
  - "hangUpEnabled é true em Unsupported e false em Incoming/Ended/Failed: em chamada recebida o botão é recusar"
  - "Prazo de apresentação = 2 s em PRESENTATION_DEADLINE_MILLIS; escopo é opcional no construtor (sem escopo, sem vigia — usado pelos testes de lógica)"
  - "assertThrows do JUnit 4.13 em lugar de assertFailsWith: kotlin-test não está no classpath e o plano não autoriza dependência nova"
  - "@Suppress(TooManyFunctions) local e justificado em vez de afrouxar o detekt.yml compartilhado (precedente da Fase 3)"
metrics:
  duration_minutes: 38
  tasks: 3
  files_created: 9
  tests_added: 61
  completed: 2026-07-29
---

# Phase 6 Plan 01: Núcleo Puro do Modo Discador Summary

Máquina de estado pura da sessão de chamada — 12 códigos de estado da telefonia traduzidos com
ramo final visível, sete comandos numa costura verificável por lista ordenada de eventos,
pareamento garantido do tom de teclado e prazo nomeado de 2 s que falha ALTO em vez de deixar a
interface viva e travada.

## O que foi construído

**Task 1 — estados e tradução** (`a7769a4`)
`CallUiState` selado (`Incoming`, `Dialing`, `Ringing`, `Active`, `Ended`, `Failed`,
`Unsupported(rawState)`), `CallSnapshot` com identidade e controles, `CallOrigin` de quatro
entradas espelhando os chips do design e `CallAudioRoute`. `PlatformCallStateMapper` traduz por
`when` sobre constantes nomeadas em português (nenhum identificador da plataforma escrito), com
ramo final delegando a uma função nomeada — nunca vazio. Tabela de teste com **16 linhas escritas
à mão**, incluindo os buracos 5 e 6 da numeração pública, o 99 e o -1.

**Task 2 — costura e coordenador** (`3696d90`)
`CallControls` com os sete comandos exatos do plano. `CallSessionCoordinator` puro recebe
`controls`, `mapper`, `clock` e expõe `state: StateFlow<CallSnapshot>`. Guarda de comando por
comparação do estado corrente (atender/recusar só em chamada recebida; encerrar em qualquer
estado com encerramento habilitado, inclusive `Unsupported`). Mudo e viva-voz são idempotentes:
pedir o valor que já vale não reenvia comando. Rota de áudio única reporta viva-voz indisponível
e bloqueia a delegação. Pareamento do tom: dígito novo encerra o pendente antes de iniciar;
saída da sessão e transição terminal encerram o tom pendente.

**Task 3 — falha alta e prazo** (`c216eac`)
`PRESENTATION_DEADLINE_MILLIS = 2_000L`, `confirmPresented()` e vigia local à sessão que lança
`CallPresentationTimeoutException` se o prazo vencer numa chamada recebida sem confirmação.
Armado só para chamada recebida; desarmado por confirmação, transição terminal ou remoção da
chamada. Zero captura de exceção no arquivo do coordenador — comprovado por critério de grep.

## Provas de vermelho (executadas e restauradas)

| Sabotagem | Resultado |
|-----------|-----------|
| Ramo final da tradução devolvendo `Ended` em vez de `Unsupported` | **5 vermelhos** (casos 5, 6, 99, -1 e o contrato de encerramento habilitado); restaurado |
| `pressDigit` sem encerrar o tom pendente | **1 vermelho** em `DtmfPairingTest` ("digito novo sem encerrar o anterior encerra o anterior primeiro"); restaurado |
| `controls.answer()` embrulhado em captura de exceção | **1 vermelho** em `CallSessionFailureTest` ("defeito ao atender propaga para quem chamou"); restaurado |
| *(extra)* `confirmPresented()` transformado em no-op | **1 vermelho** em `CallSessionWatchdogTest` — prova que o vigia não é vacuoso |

A quarta sabotagem foi acrescentada porque as duas defesas do vigia (bandeira de apresentação
**e** cancelamento do trabalho) são redundantes de propósito, como as duas redes da Fase 5:
quebrar só uma delas deixa tudo verde. Só quebrando as duas o teste fica vermelho.

## Verificação

```
./gradlew --rerun-tasks testDebugUnitTest detekt lint   → BUILD SUCCESSFUL
./gradlew koverLog                                      → 97,6978% (era 97,6351%)
bash scripts/verify-invariants.sh                       → todos os invariantes OK
```

509 testes JVM no total; **61 novos** nas cinco classes deste plano (35 na tradução, 13 no
coordenador, 7 no pareamento, 9 na matriz de falhas, 5 no prazo, mais 3 de contrato da tradução).
Cobertura **subiu** sem nenhum exclude novo no Kover — consequência direta de os quatro arquivos
de produção serem puros, exatamente como o coordenador da Fase 5.

Zero `import android.` nos quatro arquivos de produção. Zero `Thread.sleep` nos testes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `detekt` reprovou a superfície do coordenador**
- **Found during:** verificação final da Task 3
- **Issue:** `TooManyFunctions` — 18 funções contra o limite de 11 do `detekt.yml` compartilhado.
  A superfície é o contrato do plano (4 entradas da telefonia + 8 comandos + confirmação).
- **Fix:** `@Suppress("TooManyFunctions")` local com justificativa em prosa, em vez de afrouxar a
  configuração compartilhada — precedente da Fase 3 (constante nomeada em vez de relaxar
  `MagicNumber`). Agrupar comandos num objeto de intenção esconderia a guarda por estado
  corrente, que é justamente o que precisa ficar visível.
- **Files modified:** `CallSessionCoordinator.kt`
- **Commit:** `d460c49`

**2. [Rule 1 - Hygiene] Avisos de opt-in experimental no teste do prazo**
- **Issue:** `advanceTimeBy` da biblioteca de teste de corrotinas exige opt-in explícito; seis
  avisos de compilação no arquivo novo.
- **Fix:** `@OptIn(ExperimentalCoroutinesApi::class)` na classe de teste.
- **Commit:** `d460c49`

### Desvios de forma (declarados, não corrigidos)

- **Fluxo exposto é `StateFlow<CallSnapshot>`**, não `StateFlow<CallUiState>` como o texto do
  objetivo sugeria. O próprio plano autoriza o invólucro ("vivem num `CallSnapshot` que envolve o
  estado"); `CallSnapshot` mora em `CallUiState.kt` e reexpõe `hangUpEnabled`.
- **`assertThrows` do JUnit em lugar de `assertFailsWith`**: `kotlin-test` não está no classpath
  de teste e o plano não autoriza dependência nova. O critério de aceite aceita as duas formas
  (11 ocorrências contra o mínimo de 7).
- **Ciclo TDD registrado por sabotagem, não por commit vermelho separado.** Cada task foi
  entregue num commit único (produção + teste) e o vermelho foi provado quebrando o guarda-corpo
  e medindo a falha, que é a evidência que o plano exige nos critérios de aceite. Commit de teste
  isolado teria produzido vermelho de **compilação**, evidência mais fraca.

### Escopo intocado, como mandado

`CallDecisionEngine`, `AndroidManifest.xml`, `verify-invariants.sh`, filtro do Kover e os
arquivos do plano 06-02 (tema, tipografia, strings, componentes de interface) não foram tocados.

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim, como o contexto da fase prevê.

## Para o próximo plano

- A interface **precisa** chamar `confirmPresented()` ao desenhar a chamada recebida; sem isso a
  sessão falha alto em 2 s por desenho.
- O `InCallService` deve construir o coordenador com um escopo real; escopo nulo desliga o vigia
  e existe só para os testes de lógica pura.
- `CallSnapshot.sentDigits` já acumula os dígitos enviados — a linha de dígitos do teclado da
  especificação de interface não precisa de estado próprio.

## Self-Check: PASSED

9 arquivos declarados existem em disco; 4 commits declarados existem no histórico.

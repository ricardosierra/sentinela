---
phase: 07-ui-onboarding-e-home
plan: 02
subsystem: ui-navigation
tags: [navegacao, acessibilidade, infraestrutura-de-teste]
requires: []
provides:
  - "asserts de alvo de toque em dois eixos consumiveis por qualquer pacote de teste"
  - "constantes de rota por texto para as dez telas da fase"
  - "contrato do grafo de navegacao provado em execucao"
affects:
  - "todo plano da fase que escrever teste de acessibilidade ou navegar entre telas"
tech-stack:
  added: []
  patterns:
    - "rota de navegacao por texto, nunca por objeto anotado"
    - "guarda-corpo de defeito invisivel ao compilador vira teste que COMPOE"
    - "helper de teste compartilhado vive em arquivo neutro, nunca duplicado"
key-files:
  created:
    - app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt
    - app/src/main/java/org/sentinela/app/ui/navigation/SentinelaRoutes.kt
    - app/src/test/java/org/sentinela/app/ui/navigation/NavGraphContractTest.kt
  modified:
    - app/src/test/java/org/sentinela/app/ui/call/CallScreenSemanticsTest.kt
decisions:
  - "rota tipada e falso-verde de COMPILACAO reproduzido: o guarda-corpo tem de compor o grafo"
  - "objeto anotado privado falha por acesso reflexivo, nao por serializacao — a prova exige visibilidade nao privada"
  - "MatchingDeclarationName desligada no proprio arquivo, nunca na configuracao compartilhada"
metrics:
  duration: ~50min
  tasks: 2
  files: 4
  completed: 2026-07-30
---

# Phase 7 Plan 02: Infraestrutura de navegacao e acessibilidade Summary

Os tres asserts de alvo de toque em dois eixos passaram a viver em arquivo neutro sem nenhuma copia, e
as dez rotas da fase nasceram como texto com um contrato provado COMPONDO o grafo de verdade — porque a
rota tipada foi reproduzida aqui como falso-verde de compilacao.

## O que foi feito

### Task 1 — extracao dos asserts de dois eixos (`c8c230b`)

`assertTouchHeightIsAtLeast`, `assertTouchWidthIsAtLeast`, `assertLayoutHeightIsAtLeast` e o auxiliar
privado que os dois primeiros compartilham sairam de dentro de `CallScreenSemanticsTest.kt` e foram para
`app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt`, pacote `org.sentinela.app.ui`. Nenhuma
linha de comportamento mudou; o KDoc inteiro foi junto, inclusive a explicacao de que o eixo do tamanho
DESENHADO e a memoria de uma prova de vermelho que falhou na Fase 6.

Dois motivos ficaram registrados em prosa no arquivo, e os dois continuam valendo: as telas desta fase
estao em outros pacotes (duplicar deixaria o eixo com dentes divergir — proibicao permanente escrita no
arquivo), e classe de teste nao enxerga membros de outra classe de teste entre sandboxes de SDK do
Robolectric, registro da Fase 5.

`CallScreenSemanticsTest.kt` perdeu as quatro funcoes e ganhou tres imports. **Contagem lida do XML de
resultados: 14 casos antes da extracao, 14 casos depois, zero falhas** — a suite da Fase 6 nao mudou de
tamanho nem de conteudo. `grep` confirma a definicao de `assertLayoutHeightIsAtLeast` em um unico
arquivo e zero `internal fun SemanticsNodeInteraction` restantes no arquivo da Fase 6.

### Task 2 — rotas por texto e contrato do grafo (`3ca1763`)

`SentinelaRoutes.kt` com o objeto `Rotas` e dez `const val`: boas-vindas, os seis passos (papel,
desconhecidos, contatos, whitelist, notificacao, resumo), home, protecao e modo discador. Minusculas com
sublinhado, sem barra e sem argumento — nenhuma tela da fase recebe parametro de rota. O KDoc registra em
prosa portuguesa, sem escrever o nome da anotacao vigiada, por que as rotas sao texto por medicao e por
que o guarda-corpo e um teste que compoe.

`NavGraphContractTest.kt` monta um `NavHost` REAL sob `@Config(sdk = [35], qualifiers =
"w411dp-h891dp-xxhdpi")`, cada destino com um `Text` vindo de `stringResource` (zero literal em Kotlin),
com o `NavHostController` capturado em `lateinit var`. Seis casos, todos verdes:

1. o grafo compoe sem excecao e comeca em boas-vindas;
2. toda rota do grafo e texto nao vazio;
3. a contagem de destinos esta travada em dez;
4. navegar para o primeiro passo troca o destino corrente;
5. ir para a home com descarte inclusivo deixa a pilha com um unico elemento;
6. voltar do primeiro passo devolve boas-vindas.

Cada navegacao passa por `runOnUiThread` + `waitForIdle`, e a leitura da pilha usa `mapNotNull` porque a
entrada do proprio grafo tem rota nula — medido, nao suposto.

## Provas de vermelho

### Prova 1 — rota tipada: compilador verde, execucao vermelha

Sonda descartavel `RotaTipadaProbeTest.kt` em `app/src/test`, com um destino declarado como objeto anotado
e registrado pela versao parametrizada da funcao de destino, mais o caso 2 do contrato.

- **Compilacao: VERDE.** `compileDebugUnitTestKotlin` executou com **zero linhas de erro** (`grep -c "^e: "`
  devolveu `0`). Nenhum assert de compilacao percebeu qualquer coisa.
- **Execucao: VERMELHA.** `tests="1" failures="1"`, com a falha exata:

  ```
  kotlinx.serialization.SerializationException: Serializer for class 'DestinoTipado' is not found.
  Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
  ```

**Descoberta nova, alem do que a pesquisa mediu:** na primeira tentativa o objeto anotado era `private` e a
falha veio diferente — `java.lang.IllegalAccessException: class kotlinx.serialization.internal.PlatformKt
cannot access a member of class ... DestinoTipado`, de `PlatformKt.findObjectSerializer`. Ainda vermelha em
execucao, mas por acesso reflexivo, e nao pela ausencia do complemento de serializacao. Reproduzir a falha
CERTA exige visibilidade nao privada. Registrado porque uma sonda futura com objeto privado concluiria a
coisa errada sobre a causa.

A sonda foi apagada; ela nunca ficou commitada.

### Prova 2 — pilha sem descarte inclusivo

Removido o `popUpTo(Rotas.BOAS_VINDAS) { inclusive = true }` do caso 5, sobre codigo do proprio caso:

```
java.lang.AssertionError: expected:<[home]> but was:<[boas_vindas, passo_papel, home]>
```

O caso ficou vermelho e o resto da suite continuou verde. Restaurado; suite de volta a `tests="6"
failures="0"`. Este e exatamente o retorno ao onboarding que o produto proibe, e agora ele tem dentes.

Nenhuma das duas provas usou `git checkout` sobre trabalho novo.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `MatchingDeclarationName` do detekt reprovava o par nome-de-arquivo/declaracao**

- **Found during:** Task 2
- **Issue:** o plano fixa o caminho `SentinelaRoutes.kt` e o nome `Rotas`; o detekt cobra que um arquivo
  com uma unica declaracao de topo tenha o nome dela. Build vermelho no Bloco 4 do
  `verify-invariants.sh` (1 issue com peso).
- **Fix:** `@file:Suppress("MatchingDeclarationName")` no proprio arquivo, com justificativa em prosa
  (nome de arquivo em ingles como o resto da camada de interface, vocabulario de dominio em pt-BR).
  Precedente do projeto: `@Suppress` local em vez de afrouxar o `detekt.yml` compartilhado. A anotacao
  precisa ser de ARQUIVO — a versao na declaracao nao surte efeito, porque a regra e avaliada no arquivo.
- **Commit:** `3ca1763`

### Ruido de execucao concorrente (nao e defeito do trabalho)

O plano 07-01 rodava builds ao mesmo tempo e produziu tres falsos vermelhos, todos reproduzidos como
verdes na repeticao imediata: `Cannot access output property ... lookups.tab.values.at` (cache incremental
do Kotlin), `NoClassDefFoundError: ControlesEspiao` com o codigo-fonte intacto, e `mergeExtDexDebug FAILED`.
Nenhuma alteracao de codigo foi necessaria. Registro para que uma execucao futura nao persiga esses
sintomas como bug real.

## Verification

```
./gradlew assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
suite JVM completa: 639 casos, 0 falhas (era 618 na Fase 6; +6 do grafo, +15 de 07-01)
bash scripts/verify-invariants.sh                       todos os invariantes OK (8 blocos)
grep -c 'const val'  SentinelaRoutes.kt                 10
grep -ci serializable SentinelaRoutes.kt                0
```

`koverVerify` nao era cobrado aqui. `SentinelaRoutes.kt` vive em `ui.*`, fora dos pacotes medidos —
nenhum exclude foi acrescentado e o filtro do Kover nao foi tocado. `app/build.gradle.kts` nao foi tocado.

## Notas para os planos seguintes

- Teste de acessibilidade desta fase importa os tres asserts de `org.sentinela.app.ui` — **nunca**
  redeclara nenhum deles.
- Tela nova exige mudar `DESTINOS_ESPERADOS` no contrato do grafo, de proposito: a contagem travada e o
  ponto de revisao da navegacao.
- O plano que ligar a `MainActivity` ao grafo precisa resolver o destino inicial ANTES de compor o
  `NavHost` (recompor com outro `startDestination` nao re-navega, §Q1 da pesquisa).

## Self-Check: PASSED

Os tres arquivos criados existem no disco e os dois commits de tarefa (`c8c230b`, `3ca1763`) estao no
historico. A sonda descartavel nao esta no disco nem no historico.

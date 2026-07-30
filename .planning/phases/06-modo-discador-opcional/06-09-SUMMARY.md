---
phase: 06-modo-discador-opcional
plan: 09
subsystem: testing
tags: [telecom, incallservice, mockk, robolectric, kover]

# Dependency graph
requires:
  - phase: 06-modo-discador-opcional
    provides: TelecomCallControls (costura de comandos), CallSessionCoordinator, excludes do Kover do plano 06-08
provides:
  - "TelecomCallControlsTest: os oito comandos da costura provados NA costura (objeto de chamada e serviço de chamada dublados)"
  - "Delegação de mudo e de troca de rota de áudio afirmada por verificação de chamada, não por ausência de exceção"
  - "audioRoutesFromMask coberto bit a bit, incluindo a máscara medida no emulador (só alto-falante)"
  - "Exclude do Kover da costura REMOVIDO — a classe volta ao denominador do gate com 100% de linhas"
affects: [07-configuracoes-e-onboarding, 09-validacao-fisica]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Costura da plataforma provada por dublê do objeto de chamada e do serviço de chamada (mockk) sob Robolectric — mesmo recurso já usado no caso do serviço de interface de chamada"
    - "Ramo por nível da plataforma provado em classe de teste própria, com o nível como configuração de execução"

key-files:
  created:
    - app/src/test/java/org/sentinela/app/telecom/call/TelecomCallControlsTest.kt
  modified:
    - app/build.gradle.kts

key-decisions:
  - "06-09: a justificativa do exclude anterior era FALSA para metade da classe — mudo e viva-voz operam sobre o serviço de chamada, que este repo já sabe dublar; e o objeto de chamada também é dublável, então atender/recusar/encerrar/tom entraram no mesmo caso"
  - "06-09: o exclude restante (serviço de interface de chamada) é justificado pelo CICLO DE VIDA, não pela impossibilidade de dublar: vínculo, morte do processo e revínculo só se observam de fora do processo"
  - "06-09: caso que só chama mudo e conclui que 'não estourou' é vacuoso por medição — a falha é no-op silencioso; todo caso afirma a delegação com argumento exato"
  - "06-09: prova de vermelho feita sobre código JÁ COMMITADO (sabotagem em três pontos da produção), nunca sobre trabalho fora do índice"

patterns-established:
  - "Exclude do Kover só sobrevive se a justificativa for verdadeira para TODA linha que ele esconde"

requirements-completed: [DIA-02, QLT-06]

# Metrics
duration: 22min
completed: 2026-07-30
---

# Phase 6 Plano 09: Mudo e viva-voz provados na costura da telefonia — Summary

**Mudo, viva-voz e o tradutor da máscara de rotas deixaram de depender de observação manual: 15 casos novos afirmam a delegação ao serviço de chamada e ao objeto de chamada com argumento exato, e o exclude que escondia a costura do gate de cobertura foi removido — a classe fecha em 100% de linhas, ramos e métodos.**

## Performance

- **Duration:** 22 min
- **Tasks:** 2
- **Files modified:** 2 (1 criado, 1 alterado)
- **Testes JVM:** 618 (era 603), zero falhas

## Accomplishments

- `TelecomCallControlsTest` prova **na própria costura** os oito comandos: mudo ligado/desligado chega como `setMuted(true/false)` no serviço de chamada; viva-voz ligado/desligado chega como troca de rota para alto-falante/fone; atender chega como chamada somente de voz; encerrar como desconexão; recusar nas **duas** sobrecargas (com motivo declarado no nível moderno, sem motivo no nível antigo, em classe de teste própria); tom iniciado e encerrado em par.
- Os dois ramos de guarda do viva-voz ficaram explícitos: **sem estado de áudio publicado** e **máscara sem alto-falante** não pedem troca nenhuma (`verify(exactly = 0)`), o que é o que impede a interface de mostrar um estado que o áudio não acompanha.
- O caso medido no emulador desta fase (só o alto-falante exposto) virou caso de teste: a máscara oferece a rota, então a troca **é** pedida; quem decide não oferecer o controle ao usuário é o coordenador, não a costura.
- `audioRoutesFromMask` coberto: máscara vazia, cada um dos quatro bits isolado e a máscara com os quatro juntos.
- Exclude do Kover da costura removido. Medido: **96,69% de linhas** no denominador do gate, com a costura em **100%** de linhas, ramos e métodos (0 perdidos em cada contador). Gate segue em 80 e foi **visto vermelho** com o piso levantado para 99.

## Task Commits

1. **Task 1: provar mudo, viva-voz e rotas de áudio na costura** — `c4e6e08` (test)
2. **Task 2: devolver a costura ao denominador do gate** — `8fd3084` (chore)

## Files Created/Modified

- `app/src/test/java/org/sentinela/app/telecom/call/TelecomCallControlsTest.kt` — 15 casos em duas classes (nível moderno e nível antigo da plataforma); dublê do objeto de chamada e do serviço de chamada, zero reflexão
- `app/build.gradle.kts` — exclude da costura removido; exclude do serviço de interface de chamada mantido com justificativa reescrita para o que ele realmente cobre (ciclo de vida observável só de fora do processo), com a medição nova registrada

## Prova de que os casos não são vacuosos (vermelho executado)

Sabotagem aplicada ao código de produção **já commitado** (`c4e6e08` estava no índice antes), em três pontos ao mesmo tempo:

1. mudo passou a delegar sempre `false`, ignorando o argumento;
2. viva-voz passou a rotear sempre para o fone, ignorando o pedido;
3. o tradutor da máscara passou a ler o bit de bluetooth como se fosse o do alto-falante.

Resultado: **4 casos VERMELHOS** de 15 (`mudo ligado…`, `viva-voz ligado troca a rota…`, `mascara so com alto-falante…`, `cada bit da mascara…`), build FAILED. Sabotagem revertida por `git checkout --` do arquivo de produção (seguro aqui: o arquivo não continha trabalho novo) e a suíte voltou verde.

Nota honesta: `mascara com varios bits…` **não** pegou a sabotagem 3, porque com os quatro bits ligados o conjunto resultante é o mesmo. É o caso `cada bit…` que carrega essa prova — e ele pegou.

## Decisions Made

- O exclude anterior cobria uma classe inteira com uma justificativa verdadeira só para metade dela. Em vez de trocar por um exclude mais estreito (o Kover exclui por classe, não por método), a classe voltou **inteira** ao denominador, porque o objeto de chamada também é dublável — a premissa "nenhum teste em JVM pode construir" era falsa para o efeito que interessa (verificar a delegação, não executar telefonia real).
- O exclude do serviço de interface de chamada continua, com justificativa nova e verdadeira: o que importa nele é ciclo de vida (vínculo pelo sistema, morte do processo no meio da chamada, revínculo ao discador do aparelho), provado de fora do processo por `InCallServiceBindTest`, `InCallServiceDeathTest` e `scripts/verify-dialer-lifecycle.sh`.

## Deviations from Plan

None — a lacuna única apontada em `06-VERIFICATION.md` foi fechada exatamente como descrita, sem tocar no motor de decisão, sem permissão nova, sem biblioteca nova e sem enfraquecer nenhum caso existente. Nenhuma captura de defeito foi introduzida no caminho da chamada.

## Issues Encountered

- As chamadas ao estado de áudio e à troca de rota geram aviso de descontinuação do compilador (a plataforma marcou as sobrecargas por inteiro como legadas). Os avisos são de compilação, não de lint nem de detekt, e a produção usa exatamente as mesmas sobrecargas — trocá-las é decisão de produto, não de teste, e ficaria fora do escopo deste plano.

## Verificação final

- `./gradlew testDebugUnitTest koverVerify lint detekt assembleDebug` — **BUILD SUCCESSFUL** (618 testes JVM, zero falhas; lint e detekt zerados)
- `bash scripts/verify-invariants.sh` — **todos os invariantes OK** (8 blocos; `sentinelaApplicationId` segue aparecendo 3x, o Bloco 2 não foi tocado)

## User Setup Required

None.

## Next Phase Readiness

- A lacuna de verificação da Fase 6 está fechada no que é automatizável. O que resta é físico e já registrado: roteamento real de áudio para o alto-falante (cenários da Phase 9), comportamento do fabricante na troca de discador padrão e o número privado no modo discador (cenário 59) — todos permanecem **NÃO VERIFICADOS** de propósito, não reivindicados.
- Fase 7 herda a costura da chamada com prova automatizada em todos os oito comandos.

---
*Phase: 06-modo-discador-opcional*
*Completed: 2026-07-30*

## Self-Check: PASSED

Arquivos criados conferidos no disco e os dois commits conferidos no histórico.

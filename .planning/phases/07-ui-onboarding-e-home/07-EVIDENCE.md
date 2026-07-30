# Evidência — Phase 7: UI Onboarding e Home

**Coletada:** 2026-07-30
**Metodologia:** `clean` **e** `--no-build-cache`. Reaproveitamento de cache tem exatamente o mesmo
defeito probatório que tarefa considerada atualizada — lição registrada na Phase 1 e aplicada desde
então em todas as fases.

---

## Comandos executados

```bash
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
bash scripts/verify-invariants.sh
bash scripts/run-instrumented-tests.sh
```

## Resultado

```
BUILD SUCCESSFUL in 36s
75 actionable tasks: 75 executed
```

**75 de 75 tarefas EXECUTADAS**, zero reaproveitadas de cache e zero consideradas atualizadas — é o
que torna esta evidência probatória e não decorativa.

| Medição | Valor |
|---|---|
| Testes JVM | **845** casos, 0 falhas, 0 erros |
| Testes instrumentados | **80** casos, 0 falhas, 0 erros (`Medium_Phone_API_35`) |
| Cobertura do gate | **96,6157%** (piso 80) |
| Achados de recursos não usados após o estreitamento | **3** |
| `verify-invariants.sh` | 9 blocos, todos OK |
| `lint` / `detekt` | 0 achados de erro |

Testes JVM ao entrar na fase: 618. Ao sair: 845 — **+227 casos**.

---

## Gate de cobertura visto vermelho

O gate só é aceito depois de quebrar de verdade. Piso subido temporariamente para 99:

```
Rule 'Cobertura minima de dominio, normalizacao e dados' violated:
lines covered percentage is 96.615700, but expected minimum is 99
EXIT=1
```

Piso restaurado para 80; `git diff app/build.gradle.kts` vazio depois da restauração.

**O filtro de cobertura NÃO foi alargado.** A cobertura está bem acima do piso, e trazer `ui.*` para
o denominador seria escopo novo — arrastaria composta e pré-visualização para dentro da conta. Nenhuma
exclusão nova foi criada.

---

## Política de lint: de bloco cego a estreitamento nominal

A Phase 1 desabilitou a regra de recursos não usados inteira. Esta fase reabilitou e estreitou.

**Correção de premissa, medida:** a supressão **nunca** foi o que mantinha o build verde. Com a regra
reabilitada, `lintDebug` sai com código **0** — a severidade é de aviso e a conversão de aviso em erro
não está ligada. Ela servia para o relatório não ficar ilegível enquanto as telas não existiam.

**A pesquisa previu 133 → 81. O resultado real é 3.** As telas desta fase consumiram quase tudo. Os
três achados remanescentes são reais e ficam **visíveis de propósito**:

| Achado | Origem | Conduta |
|---|---|---|
| `R.color.sentinela_primary` | resíduo do esqueleto | deixado visível; a cor viva do tema está em `Color.kt` |
| `R.string.dialpad_title` | Phase 6 | **ponta solta real da Phase 6** — a tela de discagem não consumiu a chave |
| `R.string.dialer_activation_limits_title` | Phase 6 | **ponta solta real da Phase 6** — idem |

As duas últimas não são lacuna desta fase: são chaves que a Phase 6 escreveu e não usou. Ficam
registradas aqui em vez de silenciadas, porque suprimir em bloco é exatamente o que esta task desfez.

**Prova de vermelho do estreitamento** — uma chave com prefixo **desta** fase, não usada, tem de
aparecer:

```
The resource `R.string.home_prova_de_vermelho_temporaria` appears to be unused
```

Chave removida em seguida; `git diff app/src/main/res/values/strings.xml` vazio. O estreitamento
silencia as Phases 8–9 e **não** silencia esta fase.

`app/lint.xml` lista cinco prefixos, cada um numa linha comentada com a fase que o consome, para que
a fase correspondente apague a sua própria linha. Mais o ícone redondo do lançador, ignorado com
motivo nomeado: a arte final é pendência registrada, e apagar recriaria trabalho.

---

## Provas de vermelho da fase inteira

Toda sabotagem incidiu sobre código **já commitado** e foi restaurada por edição manual — nunca por
`git checkout`, depois de um executor da Phase 6 perder 74 strings assim.

| Plano | Prova de vermelho |
|---|---|
| 07-01 | Copy desonesta (fraude, base global, criptografia) derruba a varredura; cor sabotada derruba 2 casos |
| 07-02 | Rota tipada: compilador **verde**, execução **vermelha** (`SerializationException`); `popUpTo` removido devolve `[boas_vindas, passo_papel, home]` |
| 07-03 | Item 56→40dp: ambos os asserts de área de toque ficam verdes e só o de tamanho **desenhado** falha; estado no container que mescla derruba 2 casos |
| 07-04 | 6 provas, incluindo zero mentiroso e papel cacheado |
| 07-05 | Estado desabilitado declarado no container derruba o caso; refutou a própria formulação do critério |
| 07-06 | Opção desabilitada derruba exatamente os 3 estados não concedidos e segue verde em concedido |
| 07-07 | Botão dentro do nó mesclado: `useUnmergedTree` sozinho não prova alcançabilidade — corrigido no próprio caso |
| 07-08 | 4 provas: zero mentiroso (3), fronteira do número (3), estado no container (2), correção sem papel (1) |
| 07-09 | 4 provas: item removido (6), diálogo indevido (1), limpeza sem diálogo (4), estado no filho (1) |
| 07-10 | Nome totalmente qualificado derrubou **dois** invariantes — o Bloco 2 funciona |
| 07-11 | Chave `home_*` não usada aparece no relatório; piso do gate em 99 quebra o build |

---

## Suíte instrumentada: nenhum caso novo, por decisão

Esta fase **não** acrescentou caso instrumentado, e isso é decisão registrada, não lacuna. A pesquisa
mediu que o fluxo multi-tela inteiro roda em JVM com o grafo real sob `createComposeRule` +
Robolectric (`navigate`, `popUpTo`, `currentDestination` e `currentBackStack` verificados). O que
sobra é hardware, e virou cenário numerado no roteiro físico.

A suíte instrumentada **existente** foi executada uma vez para provar que a fase não a quebrou:
**80 casos, 0 falhas**. O emulador foi derrubado ao final.

---

## O que só se prova em aparelho

O critério 4 do ROADMAP diz "TalkBack navega o fluxo inteiro". A parte automatizável foi automatizada:
árvore de semântica mesclada, `contentDescription`, `stateDescription`, ordem de foco declarada,
semântica de cabeçalho e alvo de toque nos dois eixos. O que resta — locução e verbosidade reais do
leitor de tela, gestos de exploração por toque, ordem de foco **efetiva**, contraste sob cor dinâmica
com papel de parede real, escala de fonte a 200%, e a percepção da partida a frio em hardware — está
no roteiro físico como cenário numerado a partir de 61.

A mediana de partida a frio de 680 ms foi medida em **aparelho virtual**, onde a cauda mede o
hospedeiro tanto quanto o aplicativo. O veredito é do aparelho.

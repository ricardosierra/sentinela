---
phase: 07-ui-onboarding-e-home
plan: 11
status: complete
completed: 2026-07-30
tasks: 3
files_changed: 6
---

# 07-11 — Fechamento: política de lint, documentação e evidência

Executado pelo orquestrador, inline, depois de três tentativas de delegação serem interrompidas por
erro transitório de servidor (529). Nenhum trabalho foi perdido: a árvore estava limpa em `fc8b98e`
antes de começar.

## Commits

| Hash | Task | O que |
|---|---|---|
| `0dba83d` | 1 | Estreitamento da supressão de lint por fase + remoção do bloco duplicado |
| `ed4f513` | 2 | `TELAS.md` §1–6 e §10 reescritas como contrato + cenários físicos 61–68 |
| `b3d28dd` | 3 | `07-EVIDENCE.md` e contrato de validação aprovado |

## Task 1 — a previsão estava errada, e para melhor

A pesquisa previu que o estreitamento levaria **133 achados a 81**. O resultado real é **3**.

`UnusedResources` saiu do bloco `disable` em `app/build.gradle.kts`; `app/lint.xml` passou a listar
cinco prefixos, **cada um numa linha comentada com a fase que o consome**, instruindo essa fase a
apagar a própria linha. Mais o ícone redondo do lançador, ignorado com motivo nomeado: a arte final é
pendência registrada, e apagar recriaria trabalho.

**Correção de premissa, medida.** A supressão da Fase 1 **nunca** foi o que mantinha o build verde:
com a regra reabilitada, `lintDebug` sai com código **0** — a severidade é de aviso e a conversão de
aviso em erro não está ligada. Ela servia para o relatório não ficar ilegível enquanto as telas não
existiam. O comentário antigo, que afirmava o contrário, foi reescrito para dizer isso.

**Os três achados remanescentes são reais e ficam visíveis de propósito:**

| Achado | Origem | Nota |
|---|---|---|
| `R.color.sentinela_primary` | resíduo do esqueleto | a cor viva do tema está em `Color.kt` |
| `R.string.dialpad_title` | **Fase 6** | chave escrita pela Fase 6 e não consumida pela própria tela |
| `R.string.dialer_activation_limits_title` | **Fase 6** | idem |

As duas últimas **não são lacuna desta fase** — são pontas soltas da Fase 6. Ficam registradas em vez
de silenciadas, porque suprimir em bloco é exatamente o que esta task desfez.

**Prova de vermelho executada:** uma chave `home_*` desta fase, não usada, apareceu no relatório
(`The resource R.string.home_prova_de_vermelho_temporaria appears to be unused`). Chave removida,
`git diff strings.xml` vazio. O estreitamento silencia as Fases 8–9 e **não** silencia esta fase.
Fazer o bloco `disable` voltar não foi aceito como prova — a medição já mostrou que ele nunca segurava
o build.

O bloco `testImplementation` duplicado saiu (segunda cópia), com o argumento útil do comentário
apagado absorvido no que ficou.

## Task 2 — documentação como contrato

`docs/design/TELAS.md` §1–6 e §10 deixaram de descrever mockups com "adaptações obrigatórias" e
passaram a descrever **as telas que existem**, com nomes reais de arquivo e de componente, no mesmo
padrão que a Fase 6 estabeleceu na §11.

As substituições de copy decididas pelo usuário estão registradas na §1, numa tabela que mostra o que
o mockup prometia, o que a tela diz e **por quê** — apontando
`docs/backlog/capacidades-prometidas-nos-mockups.md` para as cinco capacidades pós-MVP. As duas
imagens de domínio externo viraram superfície tonal, com o motivo dito: o app não declara acesso à
internet.

A §10 ganhou o contrato de comportamento da tela Proteção travado por teste — efeito imediato,
exatamente dois diálogos, completude dos 16 itens — e a armadilha real encontrada durante a execução:
os rótulos das políticas colidem entre os três grupos, então clicar por rótulo atingia o grupo errado.

**Cenários 61–68** acrescentados a `docs/TESTE-FISICO-SAMSUNG.md`, sem lacuna, cobrindo exatamente o
que nenhuma árvore de semântica mede: locução e verbosidade do leitor de tela, exploração por toque e
deslizamento, ordem de foco **efetiva** (a declarada já é automatizada), contraste sob cor dinâmica
com papel de parede real, escala de fonte e de exibição a 200%, partida a frio percebida em hardware,
e o cronômetro do "zero a protegido em menos de 2 minutos". Até rodarem, o critério 4 do ROADMAP fica
**aberto por desenho**.

Uma frase teve de ser reescrita em prosa: a citação do que o mockup prometia continha um termo que o
próprio critério de aceite proíbe por grep, que não distingue citação de afirmação. Precedente já
usado três vezes neste projeto.

## Task 3 — gate e evidência

```
BUILD SUCCESSFUL — 75 actionable tasks: 75 executed
```

Zero reaproveitadas de cache, zero consideradas atualizadas. É o que torna a evidência probatória.

| Medição | Valor |
|---|---|
| Testes JVM | **845** (0 falhas) — 618 ao entrar na fase, **+227** |
| Instrumentados | **80** (0 falhas) em `Medium_Phone_API_35` |
| Cobertura | **96,6157%**, piso 80 |
| Invariantes | 9 blocos, todos OK |

**Gate visto vermelho:** piso em 99 →
`Rule 'Cobertura minima de dominio, normalizacao e dados' violated: lines covered percentage is
96.615700, but expected minimum is 99`, `EXIT=1`. Restaurado em 80 com diff vazio.

**O filtro de cobertura NÃO foi alargado** e nenhuma exclusão nova foi criada. Trazer `ui.*` para o
denominador seria escopo novo e arrastaria composta e pré-visualização para dentro da conta.

**Nenhum caso instrumentado novo, por decisão registrada** — não é lacuna. A pesquisa mediu que o
fluxo multi-tela inteiro roda em JVM com o grafo real. A suíte existente foi executada uma vez para
provar que a fase não a quebrou.

## Requisitos

`UIX-07`, `UIX-09`, `UIX-10` e `UIX-11` só podiam ser fechados aqui — três planos anteriores
(07-01, 07-07, 07-10) se recusaram a marcá-los para não produzir estado falsamente positivo, porque o
Bloco 9.1 guardava apenas as três pastas desta fase e o fechamento de acessibilidade era deste plano.

## Ferramentas com defeito, registrado

`roadmap update-plan-progress` corrompeu a linha de progresso **seis vezes** ao longo da fase,
derrubando a coluna de milestone e mantendo a contagem de campos — o que faz uma checagem de número de
colunas passar com conteúdo errado. `state advance-plan` e `state record-metric` falham contra o
formato deste `STATE.md`. Ambos foram contornados à mão em todos os 11 planos.

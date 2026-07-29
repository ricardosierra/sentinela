# Phase 5 — Evidência de execução

Coletada em 2026-07-29, ao fim do plano 05-07. Vale a mesma regra probatória das Phases 1 e 4:
evidência só conta **depois de `clean` e com `--no-build-cache`** — `FROM-CACHE` tem exatamente
o mesmo defeito de `UP-TO-DATE`.

```bash
./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest \
  koverVerify lint detekt && bash scripts/verify-invariants.sh \
  && bash scripts/run-instrumented-tests.sh
```

## 1. Build pós-clean, sem cache

```
BUILD SUCCESSFUL in 19s
71 actionable tasks: 71 executed
```

**71 de 71 executadas, zero `from cache`, zero `up-to-date`.** Cada tarefa rodou de verdade.

Testes JVM agregados dos XMLs de `app/build/test-results/testDebugUnitTest/`:

```
tests=417 failures=0 errors=0 skipped=0
```

A Phase 5 levou a suíte JVM de **296** (fim da Phase 4) para **417** casos — 121 novos.

## 2. Suíte instrumentada

```
bash scripts/run-instrumented-tests.sh

Starting 53 tests on Medium_Phone_API_35(AVD) - 15
Medium_Phone_API_35(AVD) - 15 Tests 53/53 completed. (0 skipped) (0 failed)
BUILD SUCCESSFUL in 1m 26s
```

O script apaga os `TEST-*.xml` antes de rodar e prova o boot do emulador por
`sys.boot_completed` — relatório antigo tem o mesmo defeito probatório de `UP-TO-DATE`.

## 3. Cobertura e o gate

```
./gradlew koverLog
application line coverage: 97.6351%
koverVerify → BUILD SUCCESSFUL (minBound 80)
```

**Filtro em vigor** (`app/build.gradle.kts`, bloco `kover { }`):

Incluídos no denominador — `org.sentinela.app.` seguido de `domain.*`, `phone.*`, `data.*`,
`settings.*` e, novos nesta fase, `telecom.*`, `notifications.*` e `permissions.*`.

Excluídos, **sempre por nome de classe, jamais por pacote**:

| Exclude | Por quê |
|---|---|
| `data.local.db.*` | gerado pelo Room (KSP); só executa em teste instrumentado |
| `data.contacts.ContactsContractLookupSource` | fonte do provider de contatos; só executa instrumentada |
| `*_Impl`, `*_Impl$*` e o anotado por `Dao`/`Database` | gerado pelo Room |

**Nenhum exclude novo foi acrescentado neste plano.** Ampliar o filtro para a camada de triagem
subiu a cobertura de 96,68% para **97,64%** — ou seja, a camada nova entrou no denominador e
*melhorou* o número. Isso é consequência direta de uma decisão de desenho do plano 05-03: o
`ScreeningCoordinator` é **puro** (zero tipo da plataforma) e a tradução da resposta roda sob
Robolectric, que é teste em JVM e portanto é medido. `ScreeningCoordinator` está no
denominador e **nunca** pode sair dele: se ele aparecer num exclude, a fiação regrediu.

Gate demonstrado falhando antes de ser aceito:

```
minBound(99) → Rule 'Cobertura minima de dominio, normalizacao e dados' violated:
               lines covered percentage is 97.635100, but expected minimum is 99
               > Task :app:koverVerify FAILED
minBound(80) → BUILD SUCCESSFUL
```

## 4. Invariantes

```
bash scripts/verify-invariants.sh   → exit 0  (== todos os invariantes OK ==)
```

Sete blocos. O Bloco 7, criado nesta fase, trava a regra de decisão dentro do motor com cinco
checagens — política por origem fora da camada de telefonia, decisão de bloqueio construída só
no domínio, coordenador sem tipo da plataforma, resposta ao sistema emitida só no serviço, e
ponto de resposta único dentro do serviço.

## 5. Percentis do caminho de decisão (plano 05-06)

| Execução | Frio | p50 | p95 | max |
|---|---|---|---|---|
| Classe isolada, 1ª | 522,9 ms | **28,7 ms** | 106,9 ms | 180,8 ms |
| Classe isolada, 2ª | 666,4 ms | **15,5 ms** | 41,7 ms | 71,6 ms |
| Suíte completa (53 testes) | 23,9 ms | **0,79 ms** | 1,50 ms | 1,92 ms |

Orçamento do produto: 200 ms. Assert do CI: 50 ms sobre a **mediana**, com folga de 4x. p95 e
máximo vão só para o logcat: entre duas execuções da mesma classe, sem uma linha de código
alterada, o p95 variou de 41,7 ms para 106,9 ms — no emulador a cauda mede o scheduler do host
tanto quanto o nosso código. **O veredito da cauda é o cenário 47 da Phase 9.**

## 6. Provas de vermelho executadas na fase, plano a plano

### 05-01 — motor e entrada

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | Regra do bypass de chamada repetida removida | vermelho no caso da janela |
| 2 | Corte da janela invertido (estrito em vez de inclusivo) | vermelho no caso do limite exato |
| 3 | Guarda de rotação (feita além do pedido) | vermelho |

### 05-02 — tradução da resposta ao sistema

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | Testes da `ScreenedCallFactory` contra stub que lança | 10 de 10 vermelhos |
| 2 | Bloco de captura removido da fábrica de entrada | exatamente o teste do normalizador que lança ficou vermelho |
| 3 | Testes da `CallResponseFactory` contra stub que lança | 10 de 10 vermelhos |
| 4 | `Silence` acrescido do pedido de pular a notificação | 5 vermelhos, exceção do construtor real da plataforma |

### 05-03 — coordenador

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | Guarda atômica removida (emitir direto) | **9 de 10** vermelhos |
| 2 | Rede permissiva do bloco final removida | **0** vermelhos — as duas redes são redundantes de propósito |
| 2b | Rede permissiva do bloco final **e** do de captura removidas | **7 de 11** vermelhos |
| 3 | Ordem invertida (trabalho posterior antes da emissão) | **4 de 6** vermelhos |

### 05-04 — notificação

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | Máscara trocada pelo número completo no texto | 2 de 11 vermelhos, incluindo o teste de varredura de todos os campos |

### 05-05 / 05-06 — serviço real, tempo e invariantes

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | Serviço deixado em passagem livre | **5 de 10** vermelhos |
| 2 | Retorno antecipado de chamada de saída removido do serviço | **0** vermelhos — guarda dupla por desenho, o coordenador segura |
| 3 | Condição de disponibilidade removida do pedido do papel | 1 de 6 vermelho, exatamente o caso do aparelho sem o papel |
| 4 | Declaração do serviço removida do manifesto | vínculo devolveu nulo no teste instrumentado |
| 5 | Limite da mediana baixado para 1,0 ms | `AssertionError: p50=10.45 ms` |
| 6 | Direção da chamada medida trocada para saída | o assert **estrutural** pegou, não o cronômetro |
| 7 | Quatro sabotagens contra o Bloco 7 dos invariantes | quatro falhas distintas, uma por checagem |

**Lição registrada duas vezes nesta fase:** as provas 5 e 6 precisam rodar em rodadas separadas.
Quebradas juntas, elas se anulam — com chamada de saída o cronômetro nunca dispara, a mediana
fica em zero e o teste de tempo passa mesmo com o limite em 1 ms.

### 05-07 — gate de cobertura

| # | O que foi quebrado | Saída |
|---|---|---|
| 1 | `minBound` elevado a 99 | `koverVerify FAILED` com o percentual real transcrito na seção 3 |

## 7. Pendências diferidas (não são lacuna desta fase)

**Para a Phase 9 — validação em Samsung físico:** cenários **40 a 51** de
[`docs/TESTE-FISICO-SAMSUNG.md`](../../../docs/TESTE-FISICO-SAMSUNG.md). Carregam os critérios
de aceite 1, 2 e 6 desta fase (bloqueio efetivo ponta a ponta, ausência de aviso nativo de
perdida, comportamento sob Não Perturbe) e o veredito dos percentis de cauda. Nada disso é
provável fora de hardware com chamada real.

**Para a Phase 7 — telas:** o fluxo de opt-in da notificação própria e a tela que detecta e
oferece a correção da perda do papel de triagem. Esta fase entregou a máquina de estado, o
pedido de permissão em runtime, os flags e as strings; a superfície em Compose é da Phase 7 por
desenho.

## 8. Correções de honestidade carregadas pela fase

A pesquisa desta fase, lida na fonte do próprio Android, derrubou três afirmações que o projeto
mantinha escritas e que agora estão corrigidas (plano 05-07, Task 1):

1. Pedir para não registrar a chamada bloqueada no histórico do telefone é **no-op** para
   aplicativos que não sejam de operadora — não é variação de fabricante, e virar discador
   padrão na Fase 6 não destrava.
2. Contatos **chegam** à triagem enquanto a leitura da agenda estiver concedida — a decisão
   sobre contato é nossa desde a Fase 4.
3. O modo "Não Perturbe" **não** é contornável; a opção "Nunca Silenciar" descreve o que o
   Sentinela faz, não o que o sistema faz.

Corrigidos em `docs/LIMITACOES.md` (itens 2, 3, 7 e o novo 8), `docs/ARQUITETURA.md`,
`docs/design/TELAS.md`, `strings.xml`, nos KDocs de domínio e configurações, no `CLAUDE.md` /
`AGENTS.md` e no `.planning/STATE.md`.

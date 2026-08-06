---
phase: 04-contatos-do-aparelho
plan: 04
subsystem: prova-instrumentada-da-consulta-de-contatos
tags: [contatos, testes-instrumentados, performance, CTT-02, provider-real]
requires:
  - ContactsTestFixture + READ_CONTACTS no manifest (plano 04-01)
  - ContactsContractLookupSource, ContactKeyCache, DefaultContactLookupRepository (plano 04-03)
provides:
  - ContactLookupSourceTest (HIT/MISS contra o provider real, incluindo formato nacional)
  - ContactsObserverTest (invalidacao por observer + debounce)
  - ContactLookupPerformanceTest (percentis com 5.000 contatos, assert na mediana)
  - numeros de p50/p95/max arquivados em logcat para a Phase 9
affects:
  - plano 04-05 (invariantes de privacidade e filtro do Kover)
  - Phase 9 (cauda do lookup vira cenario de validacao fisica)
tech-stack:
  added: []
  patterns:
    - decorador contador sobre a fonte real para afirmar estrutura sem cronometro
    - sincronizacao instrumentada por latch e por predicado, nunca por sono fixo
    - fixture pesado em @BeforeClass; assert primario sempre na mediana
key-files:
  created:
    - app/src/androidTest/java/org/sentinela/app/data/contacts/ContactLookupSourceTest.kt
    - app/src/androidTest/java/org/sentinela/app/data/contacts/ContactsObserverTest.kt
    - app/src/androidTest/java/org/sentinela/app/data/contacts/ContactLookupPerformanceTest.kt
  modified: []
decisions:
  - "A sonda dupla foi vista VERMELHA com a segunda sonda desligada: 2 casos caem, incluindo o contato em formato nacional"
  - "O debounce foi visto VERMELHO com DEBOUNCE_MS=0: 11 callbacks viraram 3 reconstrucoes"
  - "Callbacks do provider sao diagnostico em logcat; o assert e sobre reconstrucoes do conjunto"
  - "Assert de tempo so na mediana; p95/max reportados e diferidos para a Phase 9"
  - "Teste instrumentado monta o normalizador com regiao fixa BR: a cascata real leria o SIM us do AVD"
metrics:
  duration: ~25 min
  completed: 2026-07-29
  tasks: 3
  files: 3
  tests_instrumentados: 48
---

# Phase 4 Plano 04: Prova Instrumentada da Consulta de Contatos Summary

Três classes instrumentadas contra o `ContactsContract` real do emulador transformam CTT-02 de
afirmação em prova: a sonda dupla, a invalidação por observer e o orçamento da mediana foram todos
**vistos falhando** antes de serem aceitos verdes.

## O que foi construído

**Task 1 — `ContactLookupSourceTest` (commit `5fe9da5`).** 7 casos, nenhum dependente de tempo
(`grep` de `nanoTime`/`measureTime` retorna 0). Cobre E.164, formato nacional, máscara com espaços,
número fora da agenda, agenda vazia, fixo BR nas duas grafias de gravação, e leitura em lote. O
último assert é sobre o **tamanho do conjunto** de chaves, não sobre a contagem do cursor.

**Task 2 — `ContactsObserverTest` (commit `e604f0a`).** 5 casos: callback recebido, contato novo
vira HIT depois da invalidação, contato removido volta a MISS, rajada de 10 inserções custa **uma**
reconstrução, e `close()` desregistra de fato.

**Task 3 — `ContactLookupPerformanceTest` (commit `038a47b`).** Agenda de 5.000 contatos em
`@BeforeClass` (nenhum `@Before` no arquivo). Mede cache quente e sonda direta com warmup de 100 e
500 amostras, prova o uso do cache por contador, e reporta o custo da construção.

## As três provas de vermelho

**1. Sonda dupla.** Com a segunda sonda desligada em `ContactsContractLookupSource.probe`:

```
contatoGravadoEmFormatoNacionalDaHit  FAILED
fixoBrDaHitNasDuasGrafiasDeGravacao   FAILED
tests="7" failures="2" errors="0"
```

Exatamente o "MISS silencioso e perigoso" do CONTEXT: no AVD (SIM `us`) o contato gravado como
`(11) 91234-5678` fica com o normalizado do provider nulo e **não** é alcançado por uma consulta
iniciada com `+`. Restaurada a segunda sonda: `tests="7" failures="0"`.

**2. Debounce.** Com `ContactKeyCache.DEBOUNCE_MS = 0`:

```
SENTINELA|contacts|rajada|insercoes=10|callbacks=11|reconstrucoes=3   → FAILED
```

Com os 750 ms restaurados, os **mesmos 11 callbacks** produzem **1** reconstrução. A medição
confirma no nosso próprio AVD o que a pesquisa já dizia: a coalescência do provider não é garantida
(11 notificações para 10 transações) — por isso o assert é sobre reconstruções, e a contagem de
callbacks só vai para logcat.

**3. Uso do cache.** Substituindo `cache.get()` por `null` no repositório, o resultado é a lição
inteira da Phase 3 em uma linha:

```
cacheQuenteNaoConsultaOProvider                FAILED
lookupComCacheQuenteCabeNoOrcamentoDaDecisao   PASSOU (6,97 s de execucao)
```

O teste de **tempo continuou verde com o cache removido** — porque a sonda direta também cabe no
orçamento. Quem pega a regressão estrutural é o contador, nunca o cronômetro.

## Números medidos (AVD `Medium_Phone_API_35`, 5.000 contatos)

| Medição | p50 | p95 | max | Assert |
|---|---|---|---|---|
| Lookup com cache quente | **0,029 ms** | 1,08 ms | 11,57 ms | p50 < 10 ms |
| Sonda direta, HIT | **0,394 ms** | 1,00 ms | 14,02 ms | p50 < 50 ms |
| Sonda direta, MISS | **0,923 ms** | 2,62 ms | 30,67 ms | p50 < 50 ms |
| Construção do cache (5.000 chaves) | 2.572 ms | — | — | nenhum |

Todos os p50 ficam três ordens de grandeza abaixo do orçamento de 200 ms da decisão. **p95 e max
são reportados, nunca afirmados**: repetir aqui o erro do p95 da Phase 3 derrubaria o build sem
regressão real. A construção do cache medida em **2,57 s** (acima dos 1,5–1,8 s da pesquisa)
reforça a decisão do plano 04-03 de nunca aguardá-la — seria 12× o orçamento inteiro da decisão.

## Deviations from Plan

### Desvios de arranjo (não de escopo)

**1. Região fixa `BR` no normalizador dos testes.** A cascata real leria o SIM do AVD (`us`) e os
testes passariam a medir a configuração do emulador em vez do comportamento do app. Os testes
injetam `RegionProvider { "BR" }`; a cascata de produção segue intocada.

**2. Membros do `ContactLookupPerformanceTest` marcados `internal`.** O `companion object` precisa
ser público para o JUnit invocar `@BeforeClass`/`@AfterClass` estáticos, e propriedades públicas
não podem expor tipos `internal` (`ContactKeyCache`, `ContactsContractLookupSource`). Erro de
compilação real, resolvido marcando as propriedades e helpers como `internal`.

**3. `ContactKeyCache` próprio no teste de construção não é fechado.** `close()` desregistraria o
observador **compartilhado** da fonte e envenenaria os outros casos da classe. O coletor morre com
o escopo em `@AfterClass`. O motivo está em comentário no código.

**4. Logs de diagnóstico via `android.util.Log`, não `println`.** O `WhitelistPerformanceTest` da
Phase 3 usa `println`, que sai em stdout — o critério de aceite deste plano exige os números nos
arquivos `logcat-*.txt` por teste que o AGP arquiva. Nenhuma alteração no teste da Phase 3.

### Auto-fixed Issues

None — nenhum bug encontrado no código de produção do plano 04-03.

## Escopo preservado

Nada de produção foi alterado: `git diff` sobre `app/src/main` volta limpo depois das três
falsificações. O filtro do Kover **não** foi tocado e nenhum invariante novo foi adicionado — isso
é o plano 04-05.

## Verificação

```
bash scripts/run-instrumented-tests.sh   → BUILD SUCCESSFUL
   tests="48" failures="0" errors="0" skipped="0"
./gradlew detekt lint                    → sem issues
bash scripts/verify-invariants.sh        → == todos os invariantes OK ==
```

Critérios de aceite conferidos por grep: 7 `@Test` na Task 1 e 0 ocorrências de
`nanoTime`/`measureTime`; 5 `@Test` e `CountDownLatch` na Task 2; `@BeforeClass` presente, zero
`@Before`, `5_000` presente e zero asserts sobre p95 na Task 3.

## Self-Check: PASSED

- FOUND: app/src/androidTest/java/org/sentinela/app/data/contacts/ContactLookupSourceTest.kt
- FOUND: app/src/androidTest/java/org/sentinela/app/data/contacts/ContactsObserverTest.kt
- FOUND: app/src/androidTest/java/org/sentinela/app/data/contacts/ContactLookupPerformanceTest.kt
- FOUND: commits 5fe9da5, e604f0a, 038a47b

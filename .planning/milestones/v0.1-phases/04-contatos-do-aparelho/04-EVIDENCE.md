# Phase 4 — Evidência de execução

Coletada em 2026-07-29, ao fim do plano 04-05. Vale a mesma regra probatória da Phase 1:
evidência só conta **depois de `clean` e com `--no-build-cache`** — `FROM-CACHE` tem exatamente
o mesmo defeito de `UP-TO-DATE`.

```bash
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
bash scripts/verify-invariants.sh
bash scripts/run-instrumented-tests.sh
```

## 1. Build pós-clean, sem cache

```
BUILD SUCCESSFUL in 15s
71 actionable tasks: 71 executed
```

**71 de 71 executadas, zero `from cache`, zero `up-to-date`.** Cada tarefa rodou de verdade.

Testes JVM agregados dos 21 XMLs de `app/build/test-results/testDebugUnitTest/`:

```
tests=296 failures=0 errors=0
```

## 2. Cobertura

```
./gradlew koverLog
application line coverage: 96.6759%
koverVerify → BUILD SUCCESSFUL (minBound 80)
```

A cobertura voltou de **87,69%** (estado deixado pelo plano 04-03) para **96,68%** com um único
exclude nomeado, `org.sentinela.app.data.contacts.ContactsContractLookupSource` — a classe que só
executa em teste instrumentado. `ContactKeyCache`, `DefaultContactLookupRepository` e
`ContactsPermissionState` continuam **dentro** do denominador.

Gate demonstrado falhando antes de ser aceito:

```
minBound(99) → Rule 'Cobertura minima de dominio, normalizacao e dados' violated:
               lines covered percentage is 96.675900, but expected minimum is 99
minBound(80) → BUILD SUCCESSFUL
```

## 3. Invariantes

```
bash scripts/verify-invariants.sh   → exit 0

== Bloco 6: dado de contato apenas em memoria ==
ok:   nenhuma coluna de identidade de contato no schema exportado
ok:   provider de contatos so e citado em data/contacts
ok:   nenhuma coluna de identidade do contato projetada em app/src/main/java
ok:   data/contacts sem nenhum mecanismo de persistencia
== todos os invariantes OK ==
```

Bloco 6 visto **vermelho** com uma coluna `display_name` acrescentada a `BlockedCallEntity`:

```
      display_name
FAIL: coluna de identidade de contato no schema exportado — proibido (docs/PRIVACIDADE.md)
      app/src/main/java/.../BlockedCallEntity.kt:28: @ColumnInfo(name = "display_name") ...
FAIL: nome de contato na camada de dados — proibido (docs/PRIVACIDADE.md)
== 2 invariante(s) violado(s) ==

SchemaExportTest > schema nao tem coluna de dado de contato FAILED
  coluna de identidade de contato no schema exportado: [display_name]
```

Entidade e `1.json` regenerado revertidos por `git checkout`; `git diff --exit-code app/schemas`
volta limpo.

## 4. Testes instrumentados

```
bash scripts/run-instrumented-tests.sh → BUILD SUCCESSFUL in 52s
app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_35(AVD) - 15-_app-.xml
   tests="48" failures="0" errors="0" skipped="0"
```

Linhas de medição arquivadas do logcat (AVD `Medium_Phone_API_35`, 5.000 contatos):

```
SENTINELA|contacts|cache-quente|contatos=5000|amostras=500|p50=0.028667|p95=0.280375|max=7.7065
SENTINELA|contacts|sonda-direta-hit|contatos=5000|amostras=500|p50=0.520583|p95=2.455875|max=19.213709
SENTINELA|contacts|sonda-direta-miss|contatos=5000|amostras=500|p50=1.091458|p95=4.73375|max=140.194709
SENTINELA|contacts|construcao|contatos=5000|chaves=5000|ms=2993.110127
SENTINELA|contacts|rajada|insercoes=10|callbacks=11|reconstrucoes=1
```

**Leitura dos números.** Todos os p50 ficam duas a quatro ordens de grandeza abaixo do orçamento
de 200 ms da decisão, e só eles são afirmados no CI. O `max=140 ms` da sonda direta em MISS nesta
execução — contra 30 ms na execução do plano 04-04 — é a razão exata de a cauda ser **reportada e
diferida**, nunca afirmada no emulador: o número dobra e quadruplica sem nenhuma regressão de
código. Veredito da cauda em hardware real, cenário 37 de `docs/TESTE-FISICO-SAMSUNG.md`.

A construção do cache subiu de 2,57 s para **2,99 s**, reforçando pela terceira medição que ela
jamais pode ser aguardada num caminho de consulta.

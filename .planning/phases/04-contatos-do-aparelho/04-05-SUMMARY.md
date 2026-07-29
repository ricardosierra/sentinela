---
phase: 04-contatos-do-aparelho
plan: 05
subsystem: guarda-corpo-de-privacidade-e-wiring-de-contatos
tags: [privacidade, invariantes, kover, appcontainer, evidencia, CTT-01, CTT-02, CTT-04]
requires:
  - ContactsContractLookupSource, ContactKeyCache, DefaultContactLookupRepository (plano 04-03)
  - medicoes instrumentadas de lookup e construcao de cache (plano 04-04)
provides:
  - Bloco 6 do scripts/verify-invariants.sh (4 checagens de privacidade de contato)
  - SchemaExportTest.schema nao tem coluna de dado de contato
  - AppContainer.contactLookupRepository (singleton preguicoso)
  - exclude nomeado no Kover, cobertura de volta a 96,68%
  - cenarios 36-39 de docs/TESTE-FISICO-SAMSUNG.md e 04-EVIDENCE.md
affects:
  - Phase 5 (o Service consome contactLookupRepository do container)
  - Phase 9 (cauda do lookup, agenda real e negacao permanente viram validacao fisica)
tech-stack:
  added: []
  patterns:
    - invariante de privacidade sobre o artefato exportado, nunca sobre a intencao do codigo
    - guarda de shell sempre apontada para fora de scripts/ para nao casar com o proprio padrao
    - exclude de cobertura por classe nomeada, jamais por pacote
key-files:
  created:
    - .planning/phases/04-contatos-do-aparelho/04-EVIDENCE.md
  modified:
    - scripts/verify-invariants.sh
    - app/src/test/java/org/sentinela/app/data/local/db/SchemaExportTest.kt
    - app/src/main/java/org/sentinela/app/AppContainer.kt
    - app/build.gradle.kts
    - docs/TESTE-FISICO-SAMSUNG.md
decisions:
  - "Vazamento de contato para o banco e provado pelo schema EXPORTADO, lido nos VALORES de columnName; casar as CHAVES do JSON daria falso positivo em todo build"
  - "Bloco 6.2 casa o USO do provider (import do pacote ou acesso a membro), nao o nome da classe do app que o encapsula — senao o container nao poderia constru-la"
  - "Exclude do Kover e UMA classe nomeada (ContactsContractLookupSource): cache, repositorio e estado de permissao seguem no denominador"
  - "Cobertura voltou de 87,69% para 96,68% sem afrouxar o gate nem mexer em includes"
  - "max da sonda direta em MISS variou 30 ms -> 140 ms entre execucoes sem mudanca de codigo: prova de que a cauda so tem veredito em hardware real"
  - "Tela de onboarding de contatos e da Phase 7 por desenho, nao lacuna da Fase 4"
metrics:
  duration: ~20 min
  completed: 2026-07-29
  tasks: 3
  files: 5
  testes_jvm: 296
  testes_instrumentados: 48
  cobertura: 96.6759
---

# Phase 4 Plano 05: Guarda-corpo de Privacidade e Wiring de Contatos Summary

Quatro invariantes de shell e um caso de teste transformam "nome de contato não vaza" de promessa
em guarda-corpo automático; o repositório entra no container como singleton preguiçoso e a
cobertura volta de 87,69% para **96,68%** com um único exclude nomeado.

## O que foi construído

**Task 1 — Bloco 6 + `SchemaExportTest` (commit `a252e9a`).** Quatro checagens:

| # | Checagem | Alvo |
|---|---|---|
| 6.1 | nenhuma coluna de identidade de contato no schema exportado | `app/schemas/*/*.json` |
| 6.2 | provider da agenda só é citado em `data/contacts` | `app/src/main/java` |
| 6.3 | nenhuma coluna de identidade do contato projetada | `app/src/main/java` |
| 6.4 | `data/contacts` sem mecanismo de persistência | `data/contacts` |

O quinto item da pesquisa — proibir a permissão de gravação na agenda — **não foi duplicado**: já
vive na variável `FUTURE` do Bloco 1 desde o plano 04-01.

A 6.1 aplica `LEAK_PAT` aos **valores** de `"columnName"`, e o motivo está comentado no script: o
schema exportado é cheio de chaves chamadas `name` (`tableName`, `fields[].name`,
`indices[].name`), e casar contra as chaves daria falso positivo em 100% dos builds. O
`SchemaExportTest` repete a mesma leitura em JVM pura, com um assert de sanidade (`colunas`
não-vazia) para que uma mudança de forma do export não deixe o caso passar por vacuidade.

**Task 2 — wiring + Kover (commit `45512c4`).** `contactLookupRepository` é `by lazy`, monta a
fonte uma única vez e a compartilha com o cache. Nada em `Application.onCreate` e nada de contatos
em `onAppOpened()` — o observador só é registrado na primeira consulta.

**Task 3 — Phase 9 e evidência (commit `cd628b5`).** Cenários 36–39 do roteiro Samsung e
`04-EVIDENCE.md`.

## As duas provas de vermelho

**1. Vazamento no schema.** Com `@ColumnInfo(name = "display_name")` acrescentada a
`BlockedCallEntity` e o KSP re-executado:

```
      display_name
FAIL: coluna de identidade de contato no schema exportado — proibido (docs/PRIVACIDADE.md)
FAIL: nome de contato na camada de dados — proibido (docs/PRIVACIDADE.md)
== 2 invariante(s) violado(s) ==

SchemaExportTest > schema nao tem coluna de dado de contato FAILED
  coluna de identidade de contato no schema exportado: [display_name]
```

Os dois caminhos independentes ficaram vermelhos: o script (a partir do artefato) e o teste (a
partir do artefato, no CI). Revertido por `git checkout` da entidade **e** do `1.json` regenerado;
`git diff --exit-code app/schemas` volta limpo e `verify-invariants.sh` termina em `exit 0` com as
quatro linhas `ok:` do Bloco 6.

**2. Gate de cobertura.**

```
minBound(99) → Rule 'Cobertura minima de dominio, normalizacao e dados' violated:
               lines covered percentage is 96.675900, but expected minimum is 99
minBound(80) → BUILD SUCCESSFUL
```

## Cobertura: 87,69% → 96,68%

O exclude é **uma classe nomeada**, `org.sentinela.app.data.contacts.ContactsContractLookupSource`
— a única que só executa instrumentada, pela mesma razão do código gerado pelo Room.
`ContactKeyCache`, `DefaultContactLookupRepository` e `ContactsPermissionState` continuam **dentro**
do denominador, que é o ponto: a lógica pura de contatos (estado de permissão, cache, decisão
HIT/MISS/UNAVAILABLE) segue cobrada pelo gate. `includes` não foi tocado — `data.*` já cobre
`data.contacts.*`.

## Deviations from Plan

### 1. [Rule 3 - Bloqueio] `ContactKeyCache` explícito no `AppContainer`

O trecho do plano construía `DefaultContactLookupRepository(source, normalizer, scope)`, mas a
assinatura real do plano 04-03 é `(source, cache, normalizer)` — o cache é colaborador, não é
criado internamente, e o repositório não recebe `scope`. O wiring monta a fonte **uma vez** e a
passa ao cache e ao repositório: duas instâncias da fonte registrariam dois observadores sobre o
mesmo provider. O `scope` do processo vai para o cache, que é quem tem coletor.

O KDoc também foi corrigido para descrever `close()` onde ele de fato existe (fonte e cache), em
vez de atribuí-lo ao repositório, e o número citado da construção do cache é o **medido**
(2,57 s no plano 04-04), não o da pesquisa.

### 2. [Rule 3 - Bloqueio] Padrão da checagem 6.2 ajustado

O padrão literal `ContactsContract` casava com o nome da própria classe do app,
`ContactsContractLookupSource` — que o container **precisa** nomear para construí-la. A guarda
derrubaria a Task 2. O padrão passou a casar o **uso** do provider
(`android\.provider\.Contacts|ContactsContract\.`): import do pacote ou acesso a membro. A
fronteira que importa continua trancada — nenhum arquivo fora de `data/contacts` fala com o
provider — e o container pode nomear a fábrica.

### Auto-fixed Issues

Nenhum bug de produção encontrado.

## A tela de onboarding de contatos não é lacuna desta fase

O critério 1 do ROADMAP para CTT-01 menciona a tela que explica o pedido de `READ_CONTACTS`. Ela é
**deliberadamente da Phase 7**, onde vive todo o onboarding. A Fase 4 entrega o que é dela:
permissão no manifest, máquina de estado (`ContactsPermissionState`), o flag
`contacts_permission_asked` e a string pt-BR de explicação — já em `strings.xml`. Nenhuma parada
humana foi criada aqui: a política de validação física do ROADMAP as proíbe nas Fases 1–8, e os
cinco planos da fase têm `autonomous: true` no frontmatter.

## Verificação (pós-`clean`, `--no-build-cache`)

```
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt
   BUILD SUCCESSFUL — 71 actionable tasks: 71 executed
   tests=296 failures=0 errors=0
   koverLog: application line coverage: 96.6759%
bash scripts/verify-invariants.sh     → exit 0, == todos os invariantes OK ==
bash scripts/run-instrumented-tests.sh → tests="48" failures="0" errors="0" skipped="0"
```

**71 de 71 tarefas executadas**, nenhuma `from cache` — a exigência probatória da Phase 1.
Detalhes e linhas de logcat em [`04-EVIDENCE.md`](04-EVIDENCE.md).

Nesta execução o `max` da sonda direta em MISS foi **140 ms**, contra 30 ms no plano 04-04, sem
uma linha de código diferente. É exatamente por isso que a cauda é reportada e nunca afirmada no
emulador — o veredito é o cenário 37, em Samsung físico.

## Self-Check: PASSED

- FOUND: .planning/phases/04-contatos-do-aparelho/04-EVIDENCE.md
- FOUND: scripts/verify-invariants.sh (Bloco 6, LEAK_PAT)
- FOUND: app/src/main/java/org/sentinela/app/AppContainer.kt (contactLookupRepository)
- FOUND: commits a252e9a, 45512c4, cd628b5

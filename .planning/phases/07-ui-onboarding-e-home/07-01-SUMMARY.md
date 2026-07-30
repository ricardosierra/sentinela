---
phase: 07-ui-onboarding-e-home
plan: 01
subsystem: ui-foundation
tags: [design-tokens, copy, honestidade, lint, robolectric]
requires:
  - "ui/theme/Color.kt (tokens Silent Guardian, Fase 1)"
  - "ui/theme/Theme.kt (montagem do esquema, intocado)"
  - "CallStringsTest.kt (precedente da varredura, Fase 6)"
provides:
  - "StatusAttention, OnStatusAttention e StatusBlocked como literais fora do Dynamic Color"
  - "as 44 chaves pt-BR novas da fase (43 <string> + 1 <plurals>)"
  - "Phase7StringsTest: varredura de honestidade das chaves da fase sobre recurso lido"
  - "dialer_activation_unchanged_4 sem porcento cru, com texto visivel intacto"
affects:
  - "todos os planos 03-10 desta fase: nenhuma tela pode ser escrita antes disto"
tech-stack:
  added: []
  patterns:
    - "cor de significado por literal de arquivo, nunca por papel do esquema"
    - "varredura de copy sobre Context.getString, nunca sobre arquivo fonte"
    - "excecao verdadeira removida do texto antes da busca, nunca chave isentada"
key-files:
  created:
    - app/src/test/java/org/sentinela/app/ui/Phase7StringsTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/theme/Color.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt
decisions:
  - "settings_clear_history_confirm virou <plurals> de verdade: e o que reconcilia a contagem de 269 <string name= do plano e o que o lint acusava"
  - "o porcento cru foi corrigido por formatted=false, nao por duplicacao do sinal: getString sem argumento nao formata e o texto visivel mudaria"
  - "settings_fallback_allow e isenta NOMINALMENTE da varredura de pressao: politica de erro nao e opt-in"
  - "UIX-07 e UIX-11 seguem pendentes: marca-las aqui seria estado falsamente positivo"
metrics:
  duration: ~35 min
  tasks: 3
  files: 4
  tests_jvm: 639
  coverage: 96.696%
  completed: 2026-07-30
---

# Phase 7 Plan 01: Fundacao de texto e cor Summary

Tres cores semanticas de estado por literal fora do Dynamic Color, as 44 chaves pt-BR da fase
em `strings.xml`, e uma varredura de honestidade de 11 casos que le o recurso (nunca a fonte) e
impede as cinco capacidades desonestas dos mockups de voltarem pela copy.

## O que foi entregue

### Task 1 — cores fixas fora do Dynamic Color (commit `77814a9`)

`StatusAttention` `#93000A`, `OnStatusAttention` `#FFDAD6` e `StatusBlocked` `#FFB4AB` entraram
como literais no fim de `Color.kt`, com bloco de comentario em portugues explicando o motivo
como requisito de **seguranca de leitura**: `SentinelaTheme` substitui o esquema de cor INTEIRO
por um derivado do papel de parede a partir do nivel 31, entao uma cor lida do esquema deixaria
o papel de parede decidir a diferenca entre protecao ativa e protecao desligada. O estado ativo
reusa `CallAccept`/`OnCallAccept`. Os tres valores sao apelidos SEMANTICOS de `ErrorContainer`,
`OnErrorContainer` e `Error` — iguais digito por digito, e a igualdade e afirmada por teste para
que apelido novo nunca vire cor nova disfarcada (precedente de 06-02).

`ThemeTokensTest` (JVM pura) ganhou tres casos: valor literal, igualdade com o token destrutivo
correspondente, e a garantia de que nenhum dos tres colapsa no verde de ativo.
`CallColorFixationTest` (Robolectric, `@Config(sdk = [35])`) ganhou um caso que monta os tres
esquemas possiveis e afirma que os tres valores nao mudam em nenhum deles.

`Theme.kt` **nao foi editado**: `git diff --stat` do arquivo devolve zero linhas, e nenhum token
anterior mudou.

### Task 2 — as chaves pt-BR e o porcento cru (commit `c5771a4`)

O bloco XML da §10.2 do `07-UI-SPEC.md` foi copiado literalmente, chave por chave, inserido nas
secoes que o arquivo ja usava (boas-vindas, papel, contatos, whitelist, verificacao final,
dashboard, Protecao). Nenhuma frase foi reescrita. `grep -c '<string name='` fecha em **269**.

O diff de `strings.xml` mostra apenas linhas adicionadas, com **uma unica** linha removida: a de
`dialer_activation_unchanged_4`, e a mudanca dela e so o atributo.

### Task 3 — varredura de honestidade (commit `fe00fe8`)

`Phase7StringsTest.kt`, 253 linhas, 11 `@Test`, no molde do `CallStringsTest`: enumera as chaves
por reflexao sobre `R.string`, restrita aos nove prefixos da fase, e compara o texto lido por
`Context.getString` contra seis listas de expressoes proibidas em portugues. Grupos: promessa de
bloqueio, rotulo de risco/spam, base de numeros/nuvem, processamento cifrado, filtro
inteligente/superlativo, pressao de opt-in — mais a regra do mensageiro (citar WhatsApp/Telegram
sem negacao nem "fora do alcance" reprova), completude (nenhuma chave vazia), o porcento e o
plural da confirmacao.

As duas excecoes verdadeiras ("100% offline" e "100% open source") sao **removidas do texto
antes da busca**, nunca isentando a chave inteira — isentar a chave abriria porta para promessa
nova entrar na mesma string.

## Provas de vermelho (todas sobre codigo JA COMMITADO)

Cor, sobre `77814a9` ja commitado — um digito de `StatusAttention` alterado para `0xFF93000B`:

```
CallColorFixationTest > tokens de estado da protecao nao mudam em nenhum dos tres esquemas FAILED
ThemeTokensTest > os tres tokens de estado da protecao valem os literais do contrato FAILED
ThemeTokensTest > os apelidos de estado nao introduziram cor nova na paleta FAILED
18 tests completed, 3 failed
```

Restaurado por edicao manual (nunca `git checkout` — a licao de 06-02).

Copy, sobre `c5771a4` ja commitado. Prova 1, chave que classifica fraude:

```
11 tests completed, 1 failed
FAIL: nenhuma chave rotula risco nem classifica spam
   AssertionError: a string dashboard_teste_desonesto contém a expressão proibida "fraude":
   Provável Fraude Financeira: número denunciado de alto risco.
```

Prova 2, chave que promete base global de milhoes de numeros:

```
11 tests completed, 1 failed
FAIL: nenhuma chave menciona base de numeros nuvem ou servidor
   AssertionError: a string welcome_teste_desonesto contém a expressão proibida "base global":
   Base Global com milhões de números identificados.
```

Prova 3, chave que afirma processamento local criptografado:

```
11 tests completed, 1 failed
FAIL: nenhuma chave afirma processamento criptografado
   AssertionError: a string settings_teste_desonesto contém a expressão proibida "criptografado":
   Processamento local criptografado no seu aparelho.
```

As tres chaves de teste foram removidas e `git status` voltou limpo antes do prosseguimento.

## Deviations from Plan

### 1. [Rule 1 - Bug] O porcento cru nao podia ser corrigido duplicando o sinal

- **Encontrado em:** Task 2
- **Medicao:** o lint acusa `PluralsCandidate` em `dialer_activation_unchanged_4` com a palavra
  `"ffline"` — prova de que ele le `% o` como especificador de formato, exatamente o defeito
  descrito no plano.
- **Por que a correcao ditada estava errada:** `Resources.getString(id)` **nao formata nada**.
  A frase e lida sem argumento em toda a UI, entao duplicar o sinal faria a tela exibir
  `100%% offline`. O plano exige que o texto VISIVEL nao mude, e duplicar o mudaria.
- **Correcao aplicada:** `formatted="false"`, que e o precedente ja usado neste arquivo por
  `contacts_permission_rationale` ("100% local"), `about_opensource_pitch` e `review_prompt_body`
  — as tres tambem dizem "100%" e nenhuma e acusada pelo lint. Texto visivel intacto, lint
  silenciado pelo motivo certo.
- **Consequencia no teste:** o caso do porcento afirma o que e verdade e load-bearing (a frase
  continua dizendo "100% offline" e o sinal aparece **uma unica vez**), em vez de formatar a
  string com argumento — formatar essa frase lancaria, e um caso que exigisse isso estaria
  codificando um uso que a UI nao faz.
- **Commit:** `c5771a4`

### 2. [Rule 1 - Bug] `settings_clear_history_confirm` virou `<plurals>`

- **Encontrado em:** Task 2
- **Medicao:** o lint acusa o segundo `PluralsCandidate` na forma `%1$d registro(s)`.
- **Correcao:** a chave passou a ser um `<plurals>` com `one`/`other`, que e exatamente o que a
  §12.10 do contrato de design manda ("plurais via `plurals`, nao concatenacao"). O nome da chave
  nao mudou.
- **Efeito colateral que valida a leitura:** com isso `grep -c '<string name='` fecha em **269**,
  o numero que o plano pedia. As 44 chaves ditadas na §10.2 sao 43 `<string>` + 1 `<plurals>` —
  a aritmetica de "43 chaves / 269" do plano estava certa; o que estava implicito era que uma
  delas nao seria um `<string>`.
- **Commit:** `c5771a4`

### 3. [Rule 3 - Blocking] `CallColorFixationTest` nao vive no arquivo que o plano nomeia

- **Encontrado em:** Task 1
- **Situacao:** o plano lista `app/src/test/.../theme/CallColorFixationTest.kt` como arquivo. Ele
  nao existe: a classe vive dentro de `ThemeTokensTest.kt`, por decisao de 06-02 (a classe de
  tokens segue em JVM pura, a de fixacao precisa de Robolectric, e as duas convivem no arquivo).
- **Acao:** estendida no lugar, sem criar arquivo novo. Criar o arquivo duplicaria a classe.

### 4. [Rule 2 - Correcao] Isencao nominal de `settings_fallback_allow`

- **Encontrado em:** Task 3
- **Situacao:** a string existente da Fase 1 diz "Permitir chamada (recomendado)", e "recomendado"
  esta na lista de pressao de opt-in ditada pelo plano. Sem tratamento, a varredura ficaria
  vermelha sobre copy honesta ja commitada.
- **Decisao:** isencao **nominal e so da varredura de pressao**, justificada em KDoc: a politica
  de erro nao e opt-in — nao ha nada a ativar ali, e esconder qual das duas alternativas preserva
  a chamada do usuario seria pior do que dize-lo. As outras cinco varreduras continuam valendo
  sobre essa chave.

### 5. Requisitos NAO marcados como completos

`UIX-07` (nenhum texto hardcoded em Kotlin) e `UIX-11` (nenhuma promessa falsa na UI) seguem
pendentes em `REQUIREMENTS.md`, apesar de constarem no frontmatter do plano. Nenhuma tela desta
fase existe ainda: marca-los agora seria o estado falsamente positivo que a §10.3 item 11 do
proprio contrato proibe, e a tabela de rastreabilidade ja aponta as fases 7, 8 e 9. `UIX-08` ja
estava completo desde a Fase 1.

### Nota de ambiente (nao e desvio de codigo)

O plano 07-02 executava em paralelo sobre o mesmo `app/build`. Isso produziu duas falhas de
build sem relacao com o codigo (`java.io.EOFException` no arquivo binario de resultados e um
`createDebugApkListingFileRedirect FAILED`). Resolvido esperando e repetindo, como a instrucao
previa. Nenhum teste chegou a ficar vermelho por causa disso.

## Verification

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                    == todos os invariantes OK ==
./gradlew koverLog                                                   96.696%
```

- 639 testes JVM, 0 falhas (era 628 antes do plano; +11 desta fase, +3 de tokens, +1 de fixacao)
- lint sem nenhum `PluralsCandidate`; os tres achados remanescentes do relatorio sao pre-existentes
  e de outras areas (`NewerVersionAvailable`, `ModifierFactoryExtensionFunction`,
  `AutoboxingStateCreation`)
- detekt zerado
- cobertura inalterada em 96,696%: este plano nao adiciona producao Kotlin medida, e o filtro do
  Kover **nao** foi tocado (07-11 e o dono dele)
- `app/build.gradle.kts` intocado

## Self-Check: PASSED

- `app/src/test/java/org/sentinela/app/ui/Phase7StringsTest.kt` FOUND (253 linhas, 11 `@Test`)
- `app/src/main/java/org/sentinela/app/ui/theme/Color.kt` FOUND com os tres literais
- `app/src/main/res/values/strings.xml` FOUND com 269 `<string name=`
- commits `77814a9`, `c5771a4`, `fe00fe8` FOUND em `git log`

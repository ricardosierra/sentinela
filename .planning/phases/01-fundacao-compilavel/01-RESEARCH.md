# Phase 1: Fundacao Compilavel - Research

**Researched:** 2026-07-29
**Domain:** Toolchain Android (AGP 9 / Gradle 9 / JDK 17), verificação mecânica de manifest e recursos, arquitetura de validação
**Confidence:** HIGH (quase tudo verificado por execução real neste repositório, hoje)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Classificação da fase**
- Fase de infraestrutura pura — goal e os 5 critérios de sucesso são todos técnicos
  (comando roda, APK produzido, manifest declara, teste cobre, config centralizada);
  nenhum descreve comportamento visível ao usuário. Discuss de grey areas dispensado
  conforme `autonomous.md:362`.

**Escopo travado antes da fase**
- Stack já decidido em `PROJECT.md` e `CLAUDE.md`: Kotlin + Compose + Material 3, AGP 9.3.0
  com Kotlin embutido (nunca aplicar `org.jetbrains.kotlin.android`), Gradle KTS + Version
  Catalog, minSdk 29, compileSdk 37, DI manual, JDK 17.
- Permissões: nesta fase o manifest só pode conter `BIND_SCREENING_SERVICE`. `READ_CONTACTS`
  entra na Phase 4; `ROLE_DIALER`/`BIND_INCALL_SERVICE`/`CALL_PHONE` só na Phase 6.
  Antecipar permissão é violação registrada em `docs/PERMISSOES.md`.

**Validação física**
- Nenhum plano desta fase pode emitir `checkpoint:human-action` ou `checkpoint:human-verify`.
  Instalar o APK e conferir o tema no aparelho vira pendência registrada para o roteiro único
  da Phase 9 (`docs/TESTE-FISICO-SAMSUNG.md`).

### Claude's Discretion
- Organização interna dos arquivos Gradle, configuração do detekt e estrutura dos testes ficam
  a critério do executor, desde que os 5 critérios de sucesso passem.

### Deferred Ideas (OUT OF SCOPE)
- Instalação do APK e conferência visual do tema dark em aparelho Samsung — Phase 9.
- Kover e gate de cobertura ≥ 80% — Phase 2 (domínio) e Phase 9 (gate de release).
- `ContactLookupRepository` real com cache e ContentObserver — Phase 4.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PRV-01 | MVP sem permissão de INTERNET no manifest; nenhuma chamada de rede, telemetria, chave ou segredo | Verificado hoje contra o **manifest mesclado** (não só o fonte): 0 ocorrências de `android.permission.INTERNET`. Comando exato em *Code Examples §1*. Achado crítico: o merge injeta `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — o check precisa ser por allowlist, não por contagem (*Pitfall 1*) |
| QLT-02 | Lint + detekt sem issues; builds debug e release compilam | Build limpo verde hoje em **42 s** (56/57 tasks executadas). detekt: **0 issues**. lint: **0 errors, 137 warnings** — a fase passa no critério 1 ("sem erro") mas *não* na letra de QLT-02 ("sem issues"). Todas as 137 caracterizadas e classificadas em *Common Pitfalls §2*; recomendação de bloco `lint {}` em *Code Examples §4* |
| UIX-08 | Dark mode (dark-first) + Dynamic Color, seguindo tokens de `docs/design/DESIGN.md` | `Color.kt` tem os 26 tokens "Silent Guardian"; `Theme.kt` implementa `darkColorScheme` completo + `dynamicDarkColorScheme`/`dynamicLightColorScheme` com guarda `SDK_INT >= S`. Verificação sem aparelho definida em *Architecture Patterns §3* (Robolectric/Compose test) — o resto é Phase 9 |
| UIX-12 | Nome, applicationId, cores e strings centralizados para rebranding | 4 greps mecânicos rodados hoje, todos limpos: nenhum literal `org.sentinela.app` fora de `package`/`import`, nenhum `text = "..."` em Kotlin, nenhum `Color(0x...)` fora de `ui/theme/`, `sentinelaApplicationId` é a única fonte de `namespace` + `applicationId`. Comandos em *Code Examples §2* |
</phase_requirements>

## Summary

Esta fase **não é construção — é verificação**. O esqueleto existe, está completo e foi
re-validado do zero durante esta pesquisa: após `./gradlew clean`, o comando
`./gradlew assembleDebug testDebugUnitTest lint detekt` terminou `BUILD SUCCESSFUL` em **42
segundos**, com 56 das 57 tasks efetivamente executadas, 20/20 testes verdes, 0 issues no
detekt e 0 errors no lint. O APK de debug (33,8 MB) foi produzido. Isso significa que o
critério de sucesso 1 está **provado hoje, em estado limpo**, não apenas herdado do log de
2026-07-28. O planner deve tratar o build como fato verificado e não gastar tarefa alguma
tentando "consertar" a toolchain.

O trabalho real de planejamento está em **três descobertas** que a inspeção mecânica trouxe à
tona e que o CONTEXT.md não previa. Primeira: o lint passa com 0 errors mas emite **137
warnings** — 132 delas `UnusedResources` sobre as strings de `strings.xml` que foram
pré-escritas para as fases 5–9. Essas strings são ativos legítimos e **não podem ser
apagadas**; a fase precisa decidir explicitamente como tratá-las para que QLT-02 seja
verificável em vez de ambíguo. Segunda: o manifest declara `POST_NOTIFICATIONS`, o que
contradiz a frase literal do CONTEXT.md ("nesta fase o manifest só pode conter
`BIND_SCREENING_SERVICE`") mas está **explicitamente autorizado** por `docs/PERMISSOES.md`,
que o `CLAUDE.md` define como fonte canônica de permissões. Terceira: a verificação do
critério 3 precisa rodar contra o **manifest mesclado**, não contra o fonte — porque o merge
do AGP injeta uma permissão de assinatura própria, e um check ingênuo por contagem daria
falso positivo.

Sobre a única pergunta de ferramenta em aberto: **Kover não entra nesta fase.** O ROADMAP já
coloca o gate de cobertura na Phase 2, o Kover 0.9.9 é de 2026-07-17 e a série 0.9.x teve
regressões reais contra a Variant API do AGP 9 (issues #784/#785, corrigidas mas recentes).
Introduzir Kover agora adiciona risco de toolchain a uma fase cujo único produto é justamente
provar que a toolchain está estável.

**Primary recommendation:** Planeje esta fase como *auditoria com evidência arquivada*, em três
frentes: (a) um plano de verificação que roda o build limpo e grava a saída como artefato de
fase; (b) um plano de higiene de lint que converte as 137 warnings numa política declarada no
bloco `lint {}` (disable justificado + 1 correção real de `mipmap-anydpi-v26`), fechando
QLT-02 de forma auditável; (c) um plano de reconciliação documental que resolve o conflito
`POST_NOTIFICATIONS` a favor de `docs/PERMISSOES.md` e corrige a frase do CONTEXT.md. Nenhum
plano toca no `CallDecisionEngine`, no tema ou nas versões do catálogo.

## Verified Current State

Tudo abaixo foi executado neste repositório em **2026-07-29**. Confiança HIGH por observação
direta.

| Verificação | Comando | Resultado |
|-------------|---------|-----------|
| Build limpo completo | `./gradlew clean && ./gradlew assembleDebug testDebugUnitTest lint detekt` | ✅ `BUILD SUCCESSFUL in 42s` — 56/57 tasks executadas (não UP-TO-DATE) |
| Testes unitários | `app/build/test-results/testDebugUnitTest/*.xml` | ✅ `tests="20" skipped="0" failures="0" errors="0"` |
| detekt | `app/build/reports/detekt/detekt.xml` | ✅ 0 `<error>` — `maxIssues: 0` respeitado |
| lint | `app/build/reports/lint-results-debug.xml` | ⚠️ 0 errors, **137 warnings** (ver Pitfall 2) |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` | ✅ 33,8 MB, gerado hoje |
| INTERNET no manifest mesclado | ver *Code Examples §1* | ✅ 0 ocorrências |
| Service + BIND_SCREENING_SERVICE no mesclado | idem | ✅ presente, com `action android.telecom.CallScreeningService` |
| Domínio livre de Android | `grep -rn "^import android" .../domain/` | ✅ nenhum import |
| applicationId literal em Kotlin | ver *Code Examples §2* | ✅ nenhum fora de `package`/`import` |
| String hardcoded em Compose | `grep -rn 'text = "' --include="*.kt"` | ✅ nenhuma |
| Cor hardcoded fora do tema | `grep -rn "Color(0x" \| grep -v /ui/theme/` | ✅ nenhuma |
| Wrapper íntegro e versionado | `git ls-files \| grep gradle` | ✅ `gradle-wrapper.jar` + `.properties` rastreados (`.gitignore` tem a exceção `!gradle/wrapper/gradle-wrapper.jar`) |
| JDK 17 no caminho fixado | `gradle.properties: org.gradle.java.home` | ✅ existe em `/opt/homebrew/opt/openjdk@17/...` (sistema é JDK 25) |
| Platform SDK 37 instalada | `ls ~/Library/Android/sdk/platforms` | ✅ `android-37.0` presente |

**Ambiente:** `ANDROID_HOME=/Users/sierra/Library/Android/sdk` vem do shell; **não existe
`local.properties`** (e ele está corretamente fora do git). Isso funciona hoje, mas é uma
dependência implícita do ambiente — ver *Pitfall 4*.

## Standard Stack

O stack está **travado e verificado**. Esta seção existe para o planner confirmar que nada
deve mudar, não para escolher.

### Core (nenhuma alteração nesta fase)

| Item | Versão pinada | Estado verificado | Por que não mexer |
|------|---------------|-------------------|-------------------|
| Android Gradle Plugin | 9.3.0 | ✅ resolve e builda | Kotlin embutido; combinação inteira validada junto. `lint` sugere 9.3.1 — ver *Open Questions §2* |
| Gradle wrapper | 9.6.1 | ✅ dist em cache local | AGP 9.3.0 exige Gradle ≥ 9.5.0 |
| JDK | 17 (Homebrew) via `org.gradle.java.home` | ✅ caminho existe | JDK 25 do sistema não roda este Gradle |
| compileSdk / targetSdk / minSdk | 37 / 37 / 29 | ✅ `android-37.0` instalada | API máx. do AGP 9.3.0 é 37; minSdk 29 é exigência de `ROLE_CALL_SCREENING` |
| Kotlin (catálogo, para o plugin Compose) | 2.4.10 | ✅ | Só alimenta `org.jetbrains.kotlin.plugin.compose`; **jamais** aplicar `org.jetbrains.kotlin.android` |
| KSP | 2.3.10 | ✅ (tasks SKIPPED — Room ainda sem `@Entity`) | Room chega na Phase 3 |
| Compose BOM | 2026.06.01 | ✅ | |
| detekt | 1.23.8 | ✅ 0 issues | 2.0.0 ainda alpha |

### Deliberadamente ausente nesta fase

| Ferramenta | Versão atual | Decisão | Justificativa |
|------------|--------------|---------|---------------|
| Kover | 0.9.9 (2026-07-17) | **Adiar para Phase 2** | ROADMAP põe o gate de cobertura de domínio na Phase 2 (critério 5) e o gate de release na Phase 9. A série 0.9.x teve regressões contra a Variant API do AGP 9 (issues #784/#785 — fechadas, mas recentes). Nesta fase o produto é "a toolchain é estável"; acrescentar um plugin com histórico recente de atrito com AGP 9 é risco puro sem retorno |
| Room schemas | — | Já configurado, ainda inerte | `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` está no `app/build.gradle.kts` mas `app/schemas/` não existe — **correto**, o diretório só nasce quando houver a primeira `@Database` (Phase 3). Não criar diretório vazio |
| `compose-material-icons-extended` | declarada no catálogo, não usada | Manter declarada | Reservada para as fases de UI; não gera warning por estar fora do `dependencies {}` |

**Instalação:** nenhuma. A fase não adiciona dependências.

## Architecture Patterns

### Padrão 1: Verificar o manifest **mesclado**, não o fonte

O critério 3 é uma afirmação sobre o APK, e o APK carrega o manifest **mesclado**. Bibliotecas
podem injetar `<uses-permission>` via manifest merger sem que nada apareça em
`app/src/main/AndroidManifest.xml`. Este projeto já sofre isso de forma benigna: o merge
adiciona `org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (permissão de assinatura
gerada pelo androidx, não uma capacidade real).

**Quando usar:** sempre que um critério afirmar "o app não tem a permissão X".
**Onde fica o artefato:** `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`
(existe após `assembleDebug`; caminho estável no AGP 9.3.0).

### Padrão 2: Allowlist de permissões, não contagem

Um check do tipo "o manifest mesclado tem exatamente 1 permissão" quebra sozinho no primeiro
androidx novo. O check correto compara o **conjunto** de permissões contra uma allowlist
declarada, que hoje é: `POST_NOTIFICATIONS` + `*_DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
Assim o teste falha exatamente quando deve falhar — quando alguém antecipar `READ_CONTACTS`
(Phase 4) ou `CALL_PHONE` (Phase 6) — e não por ruído de dependência.

### Padrão 3: O que "tema Silent Guardian aplicado" significa **sem aparelho**

O critério 2 é parcialmente físico. A parte não-física é decomponível em quatro asserções
verificáveis em JVM/Robolectric, e é só isso que a fase deve reivindicar:

| Asserção | Como verificar sem aparelho | Já verdadeira? |
|----------|----------------------------|----------------|
| `MainActivity` embrulha o conteúdo em `SentinelaTheme` | inspeção estática / teste Compose | ✅ `setContent { SentinelaTheme { ... } }` |
| `SentinelaTheme` usa `darkColorScheme` com os tokens Silent Guardian | asserção sobre `DarkColors.surface == Color(0xFF081425)` | ✅ 26 tokens em `Color.kt` |
| Dynamic Color só a partir do Android 12 | leitura do guard `SDK_INT >= Build.VERSION_CODES.S` | ✅ presente |
| O tema de janela XML usa a mesma cor de surface | `themes.xml` → `@color/sentinela_surface` = `#081425` = token `Surface` | ✅ consistente |

**O que fica para a Phase 9:** aparência real na tela, contraste percebido, comportamento do
Dynamic Color sob One UI, splash sem flash branco. Registrar como pendência em
`docs/TESTE-FISICO-SAMSUNG.md`, **não** como gap desta fase.

### Anti-padrões a evitar nesta fase

- **Apagar strings "não usadas" para calar o lint.** As 132 `UnusedResources` são strings
  pré-escritas para as fases 5–9, alinhadas com `docs/design/TELAS.md`. Apagá-las destrói
  trabalho e recria custo nas fases de UI. A resposta é política de lint, não deleção.
- **Bumpar versão do catálogo "de brinde".** O valor desta fase é ser a linha de base
  reprodutível. Qualquer bump (AGP 9.3.1 inclusive) invalida a validação que a própria fase
  acabou de produzir.
- **Criar `app/schemas/` vazio.** Room ainda não tem entidade; o diretório vazio não é
  versionável em git e vira ruído.
- **Emitir `checkpoint:human-*`.** Proibido pelo CONTEXT.md e pela política de validação física
  do ROADMAP para as fases 1–8.
- **Expandir `ContactLookupRepository`.** São 16 linhas de contrato/comentário, sem
  `ContactsContract` — verificado. Pertence à Phase 4.

## Don't Hand-Roll

| Problema | Não construir | Usar | Por quê |
|----------|---------------|------|---------|
| Provar ausência de INTERNET | parser de XML próprio sobre o manifest fonte | `grep` no **manifest mesclado** gerado pelo AGP | O fonte mente por omissão: o merger é quem decide o conteúdo final do APK |
| Silenciar warnings de lint | `@Suppress`/`tools:ignore` espalhados nos recursos | bloco `lint { disable += ... }` em `app/build.gradle.kts` | Política central, auditável e reversível num lugar só; anotação espalhada vira dívida invisível |
| Congelar warnings existentes | lista manual de exceções | `lint { baseline = file("lint-baseline.xml") }` | O AGP mantém a linha de base sozinho e passa a reportar só regressões |
| Gate de cobertura | script somando linhas de relatório | Kover 0.9.9 (**na Phase 2**) | Já resolvido pelo ecossistema; só não é problema *desta* fase |
| Detectar string hardcoded | revisão humana de code review | lint `HardcodedText` (já ativo, 0 achados) + grep de reforço | O lint do AGP já cobre; grep serve como evidência arquivável no relatório de fase |

**Key insight:** nesta fase praticamente toda "ferramenta" que se sentiria vontade de escrever
já existe como saída do build. O trabalho é *ler os artefatos certos* (manifest mesclado,
`lint-results-debug.xml`, `detekt.xml`, `TEST-*.xml`) em vez de reimplementar a análise.

## Common Pitfalls

### Pitfall 1: O manifest mesclado tem uma permissão que ninguém escreveu

**O que dá errado:** um check "só pode existir 1 `uses-permission`" falha, ou pior, alguém
"corrige" o projeto tentando remover a permissão fantasma.
**Por que acontece:** o manifest merger injeta
`org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, uma permissão de assinatura
criada pelo androidx-core para receivers dinâmicos. Ela não concede capacidade nenhuma e não é
removível.
**Como evitar:** usar allowlist (*Architecture Pattern §2*), não cardinalidade.
**Sinal de alerta:** teste de manifest que falha logo após um bump de androidx.

### Pitfall 2: lint verde ≠ lint sem issues — os 137 warnings

**O que dá errado:** o critério 1 diz "termina sem erro" (satisfeito: 0 errors, `abortOnError`
default), mas QLT-02 diz "lint sem issues". A fase pode ser declarada completa e reabrir na
Phase 9, quando QLT-02 volta como gate final.
**Composição exata, medida hoje:**

| id | qtd | Natureza | Tratamento recomendado |
|----|-----|----------|------------------------|
| `UnusedResources` | 132 | strings/cores pré-escritas para as fases 5–9 (`welcome_*`, `history_*`, `support_*`, `R.color.sentinela_primary`, `ic_launcher_round`) | `disable` com comentário justificando + reavaliar na Phase 9, quando as telas existirem. **Nunca deletar** |
| `Typos` | 3 | falso positivo de dicionário inglês sobre pt-BR: *"'momento' is a common misspelling; did you mean 'memento'?"* | `disable` — o idioma padrão do app é pt-BR |
| `ObsoleteSdkInt` | 1 | `mipmap-anydpi-v26` é desnecessária com minSdk 29 | **Correção real e trivial:** renomear a pasta para `mipmap-anydpi` |
| `AndroidGradlePluginVersion` | 1 | informa que existe AGP 9.3.1 | `disable` — o pin de versão é decisão de projeto, não achado de qualidade |

**Como evitar:** declarar a política no bloco `lint {}` (*Code Examples §4*). Depois disso o
relatório fica com 0 issues de verdade e QLT-02 vira verificável por comando.
**Sinal de alerta:** ninguém abriu `lint-results-debug.xml` porque o build ficou verde.

### Pitfall 3: `BUILD SUCCESSFUL` UP-TO-DATE não prova nada

**O que dá errado:** rodar o comando de validação sobre um `build/` quente devolve
`BUILD SUCCESSFUL` com 56 tasks UP-TO-DATE — ou seja, o Gradle não compilou, não testou e não
analisou nada. Aconteceu nesta própria pesquisa: a primeira execução levou 3m43s **sem executar
quase nada**; só após `clean` houve execução real (42 s, 56 tasks executadas).
**Por que acontece:** cache de tarefas + `org.gradle.caching=true`.
**Como evitar:** a evidência de fechamento da fase precisa vir de uma execução após `clean`
(ou com `--rerun-tasks`), e o log arquivado deve mostrar `56 executed`, não `up-to-date`.
**Sinal de alerta:** log de validação que termina em `N actionable tasks: 0 executed`.

### Pitfall 4: o build depende de `ANDROID_HOME` do shell

**O que dá errado:** não existe `local.properties` (correto — é arquivo de máquina, fora do
git). O SDK é localizado pela variável `ANDROID_HOME` exportada no ambiente do usuário. Num
shell que não a exporte — CI, cron, agente com env enxuto — o build falha com "SDK location
not found", e o sintoma parece problema de projeto.
**Como evitar:** invocar a validação via `./build.sh`, que já faz
`ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"` e exporta `JAVA_HOME`; ou exportar
explicitamente no plano. Documentar como pré-requisito de ambiente, não corrigir com
`local.properties` versionado.

### Pitfall 5: `gradlew.bat` não existe

**O que dá errado:** clone em Windows não consegue buildar.
**Estado:** apenas `gradlew` está versionado. Irrelevante para o mantenedor (macOS), mas o
projeto será divulgado como open source (`docs/PROMPT-MVP.md`, UIX-13).
**Como evitar:** item de backlog (`docs/backlog/`), **não** trabalho desta fase — gerar o
`.bat` implicaria rodar a task `wrapper`, que pode mexer na versão do wrapper validado.

### Pitfall 6: aviso de deprecation do Gradle 10 vem do detekt

**O que dá errado:** toda build imprime *"Deprecated Gradle features were used… incompatible
with Gradle 10"* e alguém tenta caçar o problema no código do projeto.
**Origem confirmada** (`./gradlew help --warning-mode all`): `ReportingExtension.file(String)`
— API usada pelo plugin detekt 1.23.8, não por script deste repositório.
**Como evitar:** registrar como risco conhecido de upgrade futuro (detekt 2.x sai do alpha) e
seguir. Zero impacto no Gradle 9.6.1.

## Code Examples

### §1 — Critério 3: manifest mesclado sem INTERNET, com o Service registrado

```bash
# Pré-requisito: assembleDebug já rodou (o artefato é gerado pelo build)
M=app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml

# 3a. Nenhuma permissão de INTERNET (deve imprimir 0)
grep -c "android.permission.INTERNET" "$M" || true

# 3b. Allowlist de permissões — imprime o conjunto real para comparação
grep -o 'uses-permission android:name="[^"]*"' "$M"
# Esperado hoje, exatamente:
#   android.permission.POST_NOTIFICATIONS
#   org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION

# 3c. Service de triagem registrado e protegido (deve imprimir >= 1 cada)
grep -c "BIND_SCREENING_SERVICE" "$M"
grep -c "android.telecom.CallScreeningService" "$M"

# 3d. Nenhuma permissão de fase futura antecipada (deve imprimir 0)
grep -cE "READ_CONTACTS|READ_CALL_LOG|READ_PHONE_STATE|CALL_PHONE|BIND_INCALL_SERVICE|SYSTEM_ALERT_WINDOW" "$M" || true
```

> Cuidado de shell: `grep -c` sai com código 1 quando o resultado é 0, o que interrompe
> cadeias com `&&`. Use `|| true` ou `set +e` em script de verificação.

### §2 — Critério 5: centralização para rebranding (UIX-12)

```bash
# 5a. applicationId nunca literal em Kotlin (fora de package/import) → sem saída
grep -rn "org.sentinela.app" app/src/main/java --include="*.kt" | grep -v ":package \|:import "

# 5b. Nenhuma string de UI hardcoded → sem saída
grep -rn 'text = "' app/src/main/java --include="*.kt"

# 5c. Nenhuma cor fora do design system → sem saída
grep -rn "Color(0x" app/src/main/java --include="*.kt" | grep -v "/ui/theme/"

# 5d. Fonte única de identidade no Gradle → 3 linhas (val + namespace + applicationId)
grep -n "sentinelaApplicationId" app/build.gradle.kts

# 5e. Nome do app só existe como resource
grep -n "app_name" app/src/main/res/values/strings.xml
```

Todas as cinco rodaram limpas em 2026-07-29.

### §3 — Critério 4: domínio puro (reforço do que a Phase 2 aprofunda)

```bash
# Nenhum tipo Android no domínio → sem saída
grep -rn "^import android" app/src/main/java/org/sentinela/app/domain/

# Contagem de casos cobertos (hoje: 20)
grep -c "@Test" app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt

# Resultado da última execução
grep -o 'tests="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
  app/build/test-results/testDebugUnitTest/*.xml
```

### §4 — Fechar QLT-02: política de lint declarada

Adicionar em `app/build.gradle.kts`, dentro do bloco `android { }` (DSL `lint {}` do AGP 9 —
o antigo `lintOptions {}` foi removido):

```kotlin
lint {
    abortOnError = true          // explícito: erro de lint quebra o build

    disable += setOf(
        // Strings e cores pré-escritas para as telas das Fases 5-9
        // (docs/design/TELAS.md). Reavaliar na Phase 9, quando a UI existir.
        "UnusedResources",
        // Dicionário do lint é inglês; o idioma padrão do app é pt-BR
        // ("momento" != "memento").
        "Typos",
        // Versão do AGP é decisão de projeto (.planning/research/STACK.md),
        // não achado de qualidade.
        "AndroidGradlePluginVersion",
    )
}
```

Correção real que acompanha (resolve `ObsoleteSdkInt` sem `disable`):

```bash
git mv app/src/main/res/mipmap-anydpi-v26 app/src/main/res/mipmap-anydpi
```

Com isso `lint-results-debug.xml` fica em 0 issues e QLT-02 passa a ser afirmável por comando.

### §5 — Comando de validação da fase (o que arquivar como evidência)

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
./gradlew clean
./gradlew assembleDebug testDebugUnitTest lint detekt
# Evidência aceitável precisa mostrar "BUILD SUCCESSFUL" E "N executed" (não "up-to-date")
```

Baseline medida hoje: **42 s**, `57 actionable tasks: 56 executed, 1 up-to-date`.

## State of the Art

| Antes | Agora | Quando mudou | Impacto nesta fase |
|-------|-------|--------------|--------------------|
| `lintOptions { }` | `lint { }` | AGP 9.0 | O bloco de *Code Examples §4* precisa ser `lint {}` |
| Plugin `org.jetbrains.kotlin.android` obrigatório | Kotlin **embutido** no AGP; aplicar o plugin quebra o build | AGP 9.0 | Já correto no projeto; jamais "consertar" adicionando o plugin |
| `kotlin { jvmToolchain(17) }` | `java { toolchain { languageVersion... } }` | AGP 9.0 (sem KGP não há extensão `kotlin`) | Já correto no `app/build.gradle.kts` |
| KSP `kotlinVersion-kspVersion` | versionamento standalone (`2.3.10`) | KSP 2.3.0 | Catálogo já correto |
| Kover instável com Variant API do AGP 9 | issues #784/#785 corrigidas; 0.9.9 em 2026-07-17 | jul/2026 | Viabiliza Kover na Phase 2 — ainda assim, fora desta fase |

**Descontinuado / a não fazer:**
- detekt 2.0.0 — ainda alpha, não adotar (mantém 1.23.8).
- Robolectric 4.16.1 suporta até SDK 36; com compileSdk 37 exigirá `@Config(sdk = [36])` **quando
  houver o primeiro teste Robolectric** — hoje não há nenhum, os 20 testes são JVM puros.

## Open Questions

1. **`POST_NOTIFICATIONS` no manifest contradiz a letra do CONTEXT.md**
   - **O que se sabe:** o manifest declara `POST_NOTIFICATIONS`. O CONTEXT.md diz "nesta fase o
     manifest só pode conter `BIND_SCREENING_SERVICE`". Mas `docs/PERMISSOES.md` (linha 13)
     registra literalmente: *"`POST_NOTIFICATIONS` | Runtime (API 33+) | **Fase 1 ✓ (manifest)**
     / Fase 5 (pedido)"*. O `CLAUDE.md` define `docs/PERMISSOES.md` como fonte canônica de
     permissões e como leitura bloqueante para mexer em manifest.
   - **O que não está claro:** se o CONTEXT.md quis proibir a *declaração* ou apenas a
     *solicitação em runtime* (NTF-02 exige que o pedido só ocorra no opt-in, o que é Phase 5).
   - **Recomendação:** resolver a favor de `docs/PERMISSOES.md` — **manter** a declaração e
     tratar a frase do CONTEXT.md como imprecisão de redação. Declarar no manifest é
     pré-requisito técnico para pedir depois, e nada é solicitado nesta fase. O plano deve
     corrigir a frase do CONTEXT.md e registrar a reconciliação, para o verifier não marcar
     falso gap. **Não remover a permissão** — isso quebraria a Phase 5.

2. **AGP 9.3.1: bumpar ou não?**
   - **O que se sabe:** o lint reporta *"A newer version of com.android.application than 9.3.0
     is available: 9.3.1"*. A página oficial de release notes do AGP consultada hoje lista
     apenas **9.3.0 como stable** (20/07/2026) e não menciona 9.3.1 — o check do lint consulta
     o repositório Maven, que pode publicar antes da documentação.
   - **O que não está claro:** o que 9.3.1 corrige (sem release notes, confiança LOW).
   - **Recomendação:** **não bumpar nesta fase.** O produto da fase é uma linha de base
     reprodutível e 9.3.0 está validada de ponta a ponta. Registrar como item de manutenção
     para a Phase 9 (release), quando as notas existirem.

3. **`UnusedResources`: `disable` ou `baseline`?**
   - **O que se sabe:** ambos fecham QLT-02. `disable` é legível e auto-documentado; `baseline`
     congela as 132 atuais e ainda pega recurso órfão *novo*.
   - **Recomendação:** `disable` com comentário nesta fase (é discricionariedade do executor
     pelo CONTEXT.md) e reabrir a regra na Phase 9, quando as telas reais consumirem as
     strings e a lista de órfãos passar a ser sinal legítimo. Registrar como pendência
     explícita da Phase 9 para não virar dívida silenciosa.

4. **`gradlew.bat` ausente**
   - **Recomendação:** item de `docs/backlog/`, fora desta fase (ver Pitfall 5).

## Validation Architecture

### Test Framework

| Propriedade | Valor |
|-------------|-------|
| Framework | JUnit4 4.13.2 (JVM puro; MockK 1.14.11, Turbine 1.2.1, Robolectric 4.16.1 disponíveis mas ainda não usados) |
| Config file | `app/build.gradle.kts` (`testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }`) |
| Quick run command | `./gradlew testDebugUnitTest` |
| Full suite command | `./gradlew clean && ./gradlew assembleDebug testDebugUnitTest lint detekt` (42 s medidos em 2026-07-29) |
| Artefatos de evidência | `app/build/test-results/testDebugUnitTest/*.xml`, `app/build/reports/lint-results-debug.xml`, `app/build/reports/detekt/detekt.xml`, `app/build/outputs/apk/debug/app-debug.apk` |

### Phase Requirements → Test Map

Esta é uma fase de infraestrutura: a maior parte da validação é **build-level** e **inspeção
estática**, não teste unitário. Isso é adequado e não é lacuna.

| Critério / Req | Comportamento | Tipo | Comando automatizado | Existe? |
|----------------|---------------|------|----------------------|---------|
| Critério 1 / QLT-02 | Build, teste, lint e detekt passam a partir do zero | build | `./gradlew clean && ./gradlew assembleDebug testDebugUnitTest lint detekt` | ✅ verde, 42 s |
| Critério 1 / QLT-02 | detekt sem issues | build | `grep -c "<error" app/build/reports/detekt/detekt.xml` → `0` | ✅ |
| Critério 1 / QLT-02 | lint sem issues (letra de QLT-02) | build | `grep -c "<issue" app/build/reports/lint-results-debug.xml` → `0` | ❌ **hoje 137** → Wave 0 (bloco `lint {}` + `mipmap-anydpi`) |
| Critério 2 | APK de debug é produzido | build | `test -f app/build/outputs/apk/debug/app-debug.apk` | ✅ 33,8 MB |
| Critério 2 / UIX-08 | `MainActivity` embrulha o conteúdo em `SentinelaTheme` | inspeção estática | `grep -n "SentinelaTheme" app/src/main/java/org/sentinela/app/ui/MainActivity.kt` | ✅ |
| Critério 2 / UIX-08 | Esquema dark usa os tokens Silent Guardian | unit | asserção sobre `DarkColors.surface == Color(0xFF081425)` | ❌ Wave 0 (`ThemeTokensTest`) — opcional, ver nota |
| Critério 2 / UIX-08 | Dynamic Color guardado por `SDK_INT >= S` | inspeção estática | `grep -n "VERSION_CODES.S" .../ui/theme/Theme.kt` | ✅ |
| Critério 2 | Tema **renderiza** corretamente no aparelho | manual | — | ⏭️ **diferido para a Phase 9** |
| Critério 3 / PRV-01 | Manifest mesclado sem INTERNET | inspeção estática | *Code Examples §1a* → `0` | ✅ |
| Critério 3 | Service com `BIND_SCREENING_SERVICE` + action do Telecom | inspeção estática | *Code Examples §1c* | ✅ |
| Critério 3 | Nenhuma permissão de fase futura antecipada | inspeção estática | *Code Examples §1d* → `0` | ✅ |
| Critério 4 / DEC-01..05 | Precedência completa, incluindo políticas por origem | unit | `./gradlew testDebugUnitTest` — `CallDecisionEngineTest` | ✅ 20/20 |
| Critério 4 | Domínio não importa tipo Android | inspeção estática | *Code Examples §3* → sem saída | ✅ |
| Critério 5 / UIX-12 | applicationId, strings, cores centralizados | inspeção estática | *Code Examples §2* (5 comandos) → sem saída | ✅ |

**Nota sobre `ThemeTokensTest`:** um teste unitário assertando os tokens do `darkColorScheme`
é barato (JVM pura, `androidx.compose.ui.graphics.Color` é `value class`, sem Robolectric) e
transforma UIX-08 de "inspeção visual" em regressão detectável. Mas o CONTEXT.md dá
discricionariedade ao executor sobre estrutura de testes; se o planner preferir, a inspeção
estática já satisfaz o critério da fase. Recomendação fraca (nice-to-have), não bloqueante.

### Sampling Rate

- **Por commit de task:** `./gradlew testDebugUnitTest` (segundos; 20 testes)
- **Por merge de wave:** `./gradlew assembleDebug testDebugUnitTest lint detekt`
- **Phase gate:** `./gradlew clean` seguido da suíte completa, com o log arquivado como
  evidência — precisa mostrar `BUILD SUCCESSFUL` **e** `N executed` (ver Pitfall 3). Só então
  `/gsd:verify-work`.

### Wave 0 Gaps

- [ ] Bloco `lint { }` em `app/build.gradle.kts` com `abortOnError = true` e `disable` justificado
      (`UnusedResources`, `Typos`, `AndroidGradlePluginVersion`) — fecha a letra de **QLT-02**
- [ ] `git mv app/src/main/res/mipmap-anydpi-v26 app/src/main/res/mipmap-anydpi` — elimina
      `ObsoleteSdkInt` com correção real em vez de supressão
- [ ] Script/checklist de verificação de manifest com **allowlist** (Code Examples §1) — cobre
      **PRV-01** e criterion 3 de forma repetível e à prova de merge de dependência
- [ ] Reconciliar `POST_NOTIFICATIONS` entre CONTEXT.md e `docs/PERMISSOES.md` (Open Question 1)
- [ ] *(opcional)* `app/src/test/java/org/sentinela/app/ui/theme/ThemeTokensTest.kt` — cobre **UIX-08**
- [ ] Registrar em `docs/TESTE-FISICO-SAMSUNG.md` a pendência física: instalar o APK e conferir
      o tema dark / Dynamic Color sob One UI — **Phase 9**

*Nenhum gap exige framework novo: JUnit4 já está instalado e configurado.*

### Política de validação física (reafirmada)

Conforme o ROADMAP (seção "Política de validação física", 2026-07-28) e o CONTEXT.md desta
fase: **nenhum plano da Phase 1 pode emitir `checkpoint:human-action` ou
`checkpoint:human-verify`.** Todo item dependente de aparelho — instalar o APK, ver o tema na
tela, confirmar o comportamento sob One UI — é registrado como pendência concentrada no roteiro
único da Phase 9. O verifier desta fase deve tratá-los como **"deferred to Phase 9"**, jamais
como gap.

## Sources

### Primary (HIGH confidence)
- **Execução direta neste repositório em 2026-07-29** — `./gradlew clean` + `assembleDebug
  testDebugUnitTest lint detekt` (BUILD SUCCESSFUL, 42 s, 56/57 tasks executadas); leitura dos
  relatórios `lint-results-debug.xml` (137 warnings categorizadas), `detekt.xml` (0), `TEST-*.xml`
  (20/20); manifest mesclado em `merged_manifest/debug/processDebugMainManifest/`; 12 greps de
  inspeção estática; `./gradlew help --warning-mode all` (origem do aviso Gradle 10);
  `./gradlew :app:dependencies --configuration detekt` (detekt-cli 1.23.8)
- `docs/PERMISSOES.md` (linhas 11, 13, 32) — matriz canônica de permissões por fase
- `.planning/research/STACK.md` e `.planning/research/PITFALLS.md` — pesquisa de projeto reutilizada
- `.planning/ROADMAP.md` — critérios da fase e política de validação física
- [Android Gradle Plugin release notes](https://developer.android.com/build/releases/gradle-plugin) — 9.3.0 stable (20/07/2026), Gradle mín. 9.5.0, JDK 17, API máx. 37

### Secondary (MEDIUM confidence)
- [Kover Gradle Plugin no Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.jetbrains.kotlinx.kover) — 0.9.9, publicado 17/07/2026
- [kotlinx-kover issue #785](https://github.com/Kotlin/kotlinx-kover/issues/785) — regressão da Variant API do AGP 9, fechada via PR #787
- [kotlinx-kover issue #784](https://github.com/Kotlin/kotlinx-kover/issues/784) — AGP 9.0.0 changes on Variant API
- [Lint Gradle Plugin DSL](https://googlesamples.github.io/android-custom-lint-rules/usage/agp-dsl.md.html) e [Lint DSL reference](https://developer.android.com/reference/tools/gradle-api/8.3/null/com/android/build/api/dsl/Lint) — semântica de `abortOnError`, `disable`, `baseline`, `warningsAsErrors`
- [Update your Kotlin projects for AGP 9.0 (JetBrains)](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/) — Kotlin embutido, `lintOptions` → `lint`

### Tertiary (LOW confidence — validar antes de agir)
- Existência do AGP **9.3.1**: reportada pelo check `AndroidGradlePluginVersion` do próprio
  lint, **não confirmada** na página oficial de release notes. Motivo para não bumpar agora.

## Metadata

**Confidence breakdown:**
- Estado atual do build/manifest/recursos: **HIGH** — medido por execução real hoje, não inferido
- Composição e tratamento das 137 warnings de lint: **HIGH** — extraídas e classificadas uma a uma do XML
- Conflito `POST_NOTIFICATIONS` (CONTEXT.md × PERMISSOES.md): **HIGH** quanto ao fato; **MEDIUM**
  quanto à intenção original do CONTEXT.md
- Decisão de adiar Kover: **HIGH** — ROADMAP explícito + histórico de issues verificado
- AGP 9.3.1: **LOW** — só o lint afirma; sem release notes

**Research date:** 2026-07-29
**Valid until:** 2026-08-28 (30 dias — toolchain pinada e verificada; reavaliar se o catálogo
de versões for alterado)

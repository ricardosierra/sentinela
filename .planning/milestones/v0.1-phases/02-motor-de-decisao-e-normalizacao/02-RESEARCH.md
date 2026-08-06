# Phase 2: Motor de Decisao e Normalizacao — Research

**Researched:** 2026-07-29
**Domain:** Normalização E.164 (libphonenumber-android em JVM puro), motor de decisão puro, cobertura Kover
**Confidence:** HIGH — todas as 5 perguntas críticas foram resolvidas **empiricamente neste repositório**, não por memória de treino.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Normalização (libphonenumber)**

- **Número BR sem DDD** (ex.: `98765-4321`) → `NormalizationResult.Invalid` com razão explícita.
  Não inferir DDD: inferência errada envenena a whitelist e faz o motor decidir sobre um número
  que não é o do chamador.
- **Celular BR antigo sem o 9** (ex.: `+55 11 8765-4321`) → normalizar acrescentando o 9 quando
  libphonenumber reconhecer o número como válido-com-correção. Caso de teste explícito.
- **Números curtos e de serviço** (`190`, `0800…`, `4004-…`) → `Valid` com o E.164 que
  libphonenumber devolver quando possível. São números discáveis e o usuário pode legitimamente
  querer colocá-los na whitelist. Não criar categoria separada no MVP.
- **Região padrão — DECISÃO DO USUÁRIO (2026-07-29):** *não* travar em `"BR"`. O app precisa
  funcionar no mundo todo. Resolução da região em cascata:
  1. `TelephonyManager` — `simCountryIso`, com `networkCountryIso` como segunda opção.
  2. Região informada pelo próprio usuário nas configurações (DDI/DDD do usuário). O usuário
     aceitou explicitamente o custo de pedir esse dado quando o aparelho não o fornecer.
  3. `"BR"` apenas como último recurso, para não quebrar em aparelho sem SIM.
- A cascata **não** pode violar a arquitetura: `PhoneNumberNormalizer` e o domínio continuam
  sem importar tipo do Android. A leitura do `TelephonyManager` fica atrás de uma interface
  (ex.: `RegionProvider`) com implementação Android injetada pelo `AppContainer`; o domínio vê
  só a interface. Testes usam fake.
- **Verificar antes de implementar:** `simCountryIso`/`networkCountryIso` não podem exigir
  `READ_PHONE_STATE`. Se exigir, a cascata cai direto para o passo 2 e a permissão **não** é
  adicionada. → **RESOLVIDO NESTA PESQUISA: não exigem. Cascata completa liberada.**
- A persistência da região informada pelo usuário é da Phase 3; nesta fase, apenas o contrato
  e o fallback em memória.

**Máscara de exibição**

- Formato canônico: `+55 11 9****-1234` — DDI + DDD + primeiro dígito + `****` + últimos 4.
  Generalizar para outros DDIs mantendo a forma "prefixo do país + área + primeiro dígito +
  asteriscos + últimos 4".
- **Números curtos — DECISÃO DO USUÁRIO (2026-07-29):** número curto demais para ser mascarado
  de forma útil (ex.: `190`) **pode ser exibido na íntegra**. *"essas máscaras não podem
  atrapalhar o usuário"*. Definir um limiar explícito em código e testá-lo.
- Entrada inválida ou não-E.164 passada a `mask()` → devolver máscara genérica segura; **nunca**
  ecoar a entrada crua e nunca lançar exceção.
- Uma única função de máscara serve log e UI.

**Testes e cobertura**

- **Kover**, não JaCoCo. Gate `koverVerify` com regra de ≥ 80% sobre `domain/` e `phone/`,
  **falhando o build**, incluído no comando padrão de validação da fase.
- Precedência testada por **testes parametrizados sobre a matriz completa** (política × origem),
  somados aos casos nomeados existentes.
- libphonenumber: usar a variante JVM pura nos testes unitários se o port `-android` não rodar
  fora do device; runtime continua com o port `-android`. Não introduzir Robolectric.
  → **RESOLVIDO: o port `-android` roda em JVM puro. Não é preciso segunda variante.**

### Claude's Discretion

- Estrutura interna dos arquivos de teste, nomes das classes de fake, e como exatamente a regra
  do Kover é escrita no Gradle ficam a critério do executor, desde que os 5 critérios de
  sucesso do ROADMAP passem.

### Deferred Ideas (OUT OF SCOPE)

- Persistir a região/DDI-DDD informados pelo usuário (DataStore) — Phase 3.
- Tela de configuração para o usuário informar DDI/DDD — Phase 7.
- Uso real do normalizer no `CallScreeningService` — Phase 5.
- Gate de cobertura no pipeline de release — Phase 9.
- Persistência, leitura de contatos, integração Telecom, qualquer UI — Phases 3–8.
- Validação em aparelho físico — Phase 9. **Nenhum plano desta fase pode emitir
  `checkpoint:human-action` ou `checkpoint:human-verify`.**
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Descrição | Suporte da pesquisa |
|----|-----------|---------------------|
| DEC-01 | `CallDecisionEngine` puro concentra toda a regra | Motor já existe e é puro (verificado: nenhum import Android em `domain/`). Fase só completa a cobertura. Ver §Arquitetura → "O motor não muda". |
| DEC-02 | Precedência saída→proteção→privado→contato→whitelist→falha→desconhecido | Já implementada em `CallDecisionEngine.decide()`. Ver §Matriz de precedência para o conjunto exaustivo de casos (54 combinações). |
| DEC-03 | Resultado modelado como domínio (Allow/Silence/Reject/Voicemail/BlockWithoutTrace) | `CallDecision` pronto. Ver §Matriz — atenção ao acoplamento `block()` × `hideFromNativeCallLog`. |
| DEC-04 | Reason codes sem dado pessoal | `DecisionReason` com os 9 códigos exigidos. Teste sugerido: asserção de que nenhum `code` contém dígito. |
| DEC-05 | Fallback configurável em erro inesperado | `FallbackPolicy.ALLOW/BLOCK` implementado; cobrir os 2 ramos × 2 gatilhos (`ContactLookup.UNAVAILABLE`, `WhitelistLookup.LOOKUP_FAILED`). |
| NRM-01 | E.164 com libphonenumber (port Android) | §Standard Stack + §Padrão 1 (MetadataLoader). **Empiricamente comprovado rodando em JVM puro.** |
| NRM-02 | Padrão BR: +55, DDD obrigatório, celular 9 dígitos, fixos | §Tabela de comportamento real do libphonenumber — inclui a **descoberta crítica** sobre o 9º dígito. |
| NRM-03 | Formatação é visual; E.164 é a verdade | `NormalizationResult.Valid(e164)` já modela isso. §Pitfall 2 (números curtos quebram a premissa E.164). |
| NRM-04 | Máscara segura | §Padrão 3 — algoritmo baseado em `getLengthOfNationalDestinationCode`. |
| CTT-03 (lógica) | Política de contatos configurável (4 opções) | `settings.contactsPolicy` já consumido pelo motor; cobrir as 4 `OriginPolicy` na matriz. |
| WLT-08 (lógica) | Tratamento da whitelist configurável (4 opções) | `settings.whitelistPolicy`; idem. |
| QLT-01 (casos de domínio) | Casos obrigatórios da §13 do prompt que são puros | §Matriz + §Casos de normalização. Os casos de repo/timeout/Room ficam nas Phases 3 e 5. |
| QLT-07 (base) | Cobertura ≥ 80% (Kover) | §Padrão 4 — configuração verificada rodando: `koverVerify` **falha o build** corretamente. Baseline medida hoje: **94,74%**. |
</phase_requirements>

---

## Summary

Esta fase tem **três riscos técnicos reais** e todos foram resolvidos por experimento direto no
repositório, não por suposição:

1. **libphonenumber-android roda em teste JVM puro?** **SIM.** O artefato
   `io.michaelrocks:libphonenumber-android` expõe `PhoneNumberUtil.createInstance(MetadataLoader)`
   além do `createInstance(Context)`. Os metadados vivem em `assets/` do AAR, e o AGP os mescla
   em um diretório que o próprio teste unitário consegue localizar via
   `com/android/tools/test_config.properties` (gerado porque `isIncludeAndroidResources = true`
   já está ligado). Executei um `parse("11987654321","BR")` num `testDebugUnitTest` real e
   obtive `+5511987654321 valid=true`. **Sem Robolectric, sem a variante Google JVM, sem device.**

2. **A cascata de região viola `docs/PERMISSOES.md`?** **NÃO.** No AOSP,
   `getSimCountryIso()` e `getNetworkCountryIso()` carregam apenas `@RequiresFeature(...)` —
   **nenhum `@RequiresPermission`**. A cascata de 3 níveis do usuário pode ser implementada
   integralmente sem tocar no manifest.

3. **Kover 0.9.9 funciona com AGP 9.3.0 + Gradle 9.6.1?** **SIM.** Apliquei o plugin, escrevi a
   regra de 80% filtrada em `org.sentinela.app.domain.*` e `org.sentinela.app.phone.*`, e o
   `koverVerify` passou; subindo o bound para 99 ele **falhou o build** com a mensagem correta
   (`lines covered percentage is 94.736800, but expected minimum is 99`). **Porém exige aumento
   de `MaxMetaspaceSize`** — com os 512m atuais o build morre com `Metaspace`.

Além disso a pesquisa encontrou **uma contradição factual dentro do CONTEXT.md** que o planner
precisa resolver antes de escrever tasks: libphonenumber **não** reconhece celular BR antigo sem
o 9 como "válido-com-correção" (retorna `valid=false, type=UNKNOWN`), e **não** produz E.164
utilizável para `190` (retorna `+55190`, `valid=false`, `TOO_SHORT`). Ver §Open Questions.

**Primary recommendation:** implementar `LibPhoneNumberNormalizer` sobre
`PhoneNumberUtil.createInstance(MetadataLoader)` com o loader injetado (produção = assets do
Android; teste = diretório de assets mesclados do AGP), decidir explicitamente as duas regras
brasileiras que o libphonenumber **não** cobre sozinho (9º dígito e números curtos), e adicionar
Kover 0.9.9 com bump de metaspace no `gradle.properties`.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.michaelrocks:libphonenumber-android` | **9.0.34** (já no catalog) | Parse/validação/formatação E.164 | Port oficial-de-facto para Android; metadados em `assets` em vez de resources JVM, ~1 MB menor no APK. 9.0.34 é a **última publicada** (verificado no `maven-metadata.xml` do Central, 2026-07-05). Upstream Google está em 9.0.35 — o port fica ≤ 1 patch atrás, normal. |
| `org.jetbrains.kotlinx.kover` (plugin Gradle) | **0.9.9** | Cobertura + gate | Última versão (Central, 2026-07-17). Compatibilidade com AGP 9.3.0 / Gradle 9.6.1 **verificada executando** neste repo. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit 4 | 4.13.2 (já) | Testes | Toda a fase |
| `org.junit.runners.Parameterized` | vem com JUnit 4 | Matriz política × origem | Decisão do usuário pede testes parametrizados; **não** precisa de dependência nova |
| MockK 1.14.11 (já) | — | Fakes | Prefira fakes escritos à mão para `RegionProvider`/`MetadataLoader` — mais rápidos e mais legíveis que mocks |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `io.michaelrocks:libphonenumber-android` em teste | `com.googlecode.libphonenumber:libphonenumber` como `testImplementation` | **REJEITAR.** Pacotes diferentes (`io.michaelrocks.libphonenumber.android` vs `com.google.i18n.phonenumbers`) — o teste não exercitaria a classe de produção, matando o critério de sucesso 2. Além disso as duas no classpath duplicam metadados. Desnecessário: o port roda em JVM. |
| Kover | JaCoCo | Descartado por decisão do usuário; e JaCoCo tem atrito conhecido com bytecode Kotlin inline. |
| Robolectric para dar `Context` ao `createInstance(Context)` | — | **PROIBIDO** — `01-VALIDATION.md`: Robolectric 4.16.1 não suporta SDK 37. E é desnecessário. |
| `ShortNumberInfo` para tratar `190`/`911` | — | **IMPOSSÍVEL neste artefato.** O construtor de `ShortNumberInfo` é *package-private* e **não há factory pública** no port `-android` (verificado com `javap` sobre o `classes.jar` do AAR 9.0.34). Números curtos precisam de tratamento próprio. |

**Instalação (adições ao catalog):**

```toml
# gradle/libs.versions.toml
[versions]
kover = "0.9.9"

[plugins]
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```
`libphonenumber-android` **já está declarado e já é `implementation`** — nada a adicionar.

**Verificação de versão executada:**
```
maven-metadata.xml io/michaelrocks/libphonenumber-android → última = 9.0.34 (lastUpdated 20260705)
maven-metadata.xml com/googlecode/libphonenumber        → última = 9.0.35 (upstream, não usada)
maven-metadata.xml org/jetbrains/kotlinx/kover-gradle-plugin → última = 0.9.9 (lastUpdated 20260717)
```
> Nota: o índice `search.maven.org` está **desatualizado** para `io.michaelrocks` (reporta 9.0.5
> como máximo). Confie no `maven-metadata.xml`, não no solr.

---

## Architecture Patterns

### Estrutura de arquivos proposta

```
app/src/main/java/org/sentinela/app/
├── domain/            # INALTERADO — puro, sem Android
│   ├── CallDecisionEngine.kt
│   ├── CallDecision.kt / DecisionReason.kt / ScreenedCall.kt
├── phone/
│   ├── PhoneNumberNormalizer.kt      # interface + NormalizationResult (já existe)
│   ├── LibPhoneNumberNormalizer.kt   # NOVO — implementação real, sem import android.*
│   ├── PhoneMetadataLoader.kt        # NOVO — typealias/wrapper do MetadataLoader
│   └── RegionProvider.kt             # NOVO — interface pura (fun currentRegion(): String?)
├── platform/ (ou telecom/)
│   ├── AndroidRegionProvider.kt      # NOVO — único arquivo que importa TelephonyManager
│   └── AssetsPhoneMetadataLoader.kt  # NOVO — único que importa AssetManager
└── AppContainer.kt                   # wiring
```

**Invariante a manter (e a checar em `scripts/verify-invariants.sh`):** nenhum arquivo em
`domain/` **nem em `phone/`** pode conter `import android.`. `LibPhoneNumberNormalizer` importa
apenas `io.michaelrocks.libphonenumber.android.*` — que, apesar do nome do pacote, é código JVM
puro. Sugestão de invariante novo:
```bash
grep -rn "^import android\." app/src/main/java/org/sentinela/app/{domain,phone}/ && exit 1
```

### Pattern 1: `MetadataLoader` injetado — a chave de tudo

**What:** nunca chamar `PhoneNumberUtil.createInstance(context)` dentro do normalizer. O
normalizer recebe um `PhoneNumberUtil` (ou um loader) pronto; quem sabe de `Context` é o
`AppContainer`.

**Why:** é exatamente isso que torna o teste JVM puro possível *e* mantém `phone/` sem Android.

```kotlin
// phone/LibPhoneNumberNormalizer.kt — produção e teste usam a MESMA classe
import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

class LibPhoneNumberNormalizer(
    private val util: PhoneNumberUtil,
    private val regionProvider: RegionProvider,
) : PhoneNumberNormalizer { /* ... */ }

// Fábrica pura, para produção e teste:
fun phoneNumberUtil(loader: MetadataLoader): PhoneNumberUtil =
    PhoneNumberUtil.createInstance(loader)
```

```kotlin
// platform/AssetsPhoneMetadataLoader.kt — ÚNICO ponto com Android
import android.content.Context
import io.michaelrocks.libphonenumber.android.metadata.source.AssetsMetadataLoader

fun assetsMetadataLoader(context: Context) = AssetsMetadataLoader(context.assets)
// (equivalente a PhoneNumberUtil.createInstance(context))
```

```kotlin
// test — helper compartilhado, ex.: app/src/test/.../phone/TestMetadata.kt
// VERIFICADO EXECUTANDO em 2026-07-29 (AGP 9.3.0, Gradle 9.6.1, JDK 17)
object TestMetadata {
    private val assetsDir: File by lazy {
        val props = java.util.Properties()
        checkNotNull(TestMetadata::class.java.classLoader
            .getResourceAsStream("com/android/tools/test_config.properties")) {
            "test_config.properties ausente — exige testOptions.unitTests.isIncludeAndroidResources = true"
        }.use { props.load(it) }
        File(props.getProperty("android_merged_assets"))
    }

    val loader = MetadataLoader { name ->
        File(assetsDir, name.removePrefix("/")).takeIf { it.exists() }?.inputStream()
    }

    fun util(): PhoneNumberUtil = PhoneNumberUtil.createInstance(loader)
}
```
Saída real do experimento:
```
PROBE-B test_config: .../unit_test_config_directory/debugUnitTest/generateDebugUnitTestConfig/out/com/android/tools/test_config.properties
PROBE-B keys: [android_merged_assets, android_resource_apk, android_custom_package, android_merged_manifest]
PROBE-C merged asset BR exists: true at build/intermediates/assets/debug/mergeDebugAssets/io/michaelrocks/libphonenumber/android/data/PhoneNumberMetadataProto_BR
PROBE-D E164=+5511987654321 valid=true
```
> **Detalhe:** `android_merged_assets` vem como caminho **relativo ao diretório do módulo**
> (`app/`), que é o working dir do teste. `File(prop)` resolve corretamente. Não normalize para
> absoluto assumindo a raiz do repo.
>
> **Fallback se o AGP mudar a chave:** o diretório é estável em
> `app/build/intermediates/assets/debug/mergeDebugAssets`. Prefira o `test_config.properties`
> e caia para o caminho fixo só se a propriedade estiver ausente — com mensagem de erro explícita
> (nunca silenciar, senão o teste vira falso-verde por metadados vazios).

### Pattern 2: `RegionProvider` — cascata sem Android no domínio

```kotlin
// phone/RegionProvider.kt — puro
fun interface RegionProvider {
    /** ISO-3166-1 alpha-2 maiúsculo, ou null se indisponível. */
    fun currentRegion(): String?
}

// phone/CascadingRegionProvider.kt — puro, 100% testável com fakes
class CascadingRegionProvider(
    private val device: RegionProvider,        // TelephonyManager (platform)
    private val userPreference: RegionProvider, // config do usuário (Phase 3 persiste)
    private val fallback: String = "BR",
) : RegionProvider {
    override fun currentRegion(): String =
        device.currentRegion()?.normalizeRegion()
            ?: userPreference.currentRegion()?.normalizeRegion()
            ?: fallback
    private fun String.normalizeRegion(): String? =
        trim().uppercase().takeIf { it.length == 2 && it.all(Char::isLetter) }
}
```

```kotlin
// platform/AndroidRegionProvider.kt — ÚNICO arquivo que toca TelephonyManager
class AndroidRegionProvider(private val tm: TelephonyManager?) : RegionProvider {
    override fun currentRegion(): String? = runCatching {
        tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
```
`runCatching` não é paranoia: ambos os métodos lançam `UnsupportedOperationException` em aparelho
sem `FEATURE_TELEPHONY_SUBSCRIPTION` / `FEATURE_TELEPHONY_RADIO_ACCESS` (tablets Wi-Fi-only).
Ambos também retornam **string vazia**, não `null`, quando indisponíveis.

### Pattern 3: máscara generalizada por metadados

`getLengthOfNationalDestinationCode(number)` dá o tamanho do "DDD" em qualquer país — medido:

| Entrada | cc | NSN | ndcLen |
|---------|----|-----|--------|
| `+5511987654321` | 55 | `11987654321` | 2 |
| `+551133334444` | 55 | `1133334444` | 2 |
| `+12125550123` | 1 | `2125550123` | 3 |
| `+442071838750` | 44 | `2071838750` | 2 |
| `+558001234567` (0800) | 55 | `8001234567` | 3 |
| `+5540041234` (4004) | 55 | `40041234` | 4 |
| qualquer número **inválido** | — | — | **0** |

```
mask(e164):
  1. parse(e164, region=null)  → se lançar ou vier vazio, devolve MASCARA_GENERICA ("+** ****")
  2. digits = NSN
  3. se digits.length <= LIMIAR_CURTO (recomendado: 6) → devolve o número na íntegra   # decisão do usuário
  4. ndc = getLengthOfNationalDestinationCode(n); se ndc == 0 → forma degradada: "+cc ****-últimos4"
  5. área = digits[0 until ndc]; resto = digits[ndc..]
  6. se resto.length < 5 → forma degradada (não há o que mascarar com segurança)
  7. => "+$cc $área ${resto.first()}****-${resto.takeLast(4)}"
```
`+5511987654321` → `+55 11 9****-1234` ✅ (formato canônico do CLAUDE.md reproduzido exatamente).

**Testes obrigatórios da máscara:** para toda entrada de teste, assertar
`!masked.contains(nsnCompleto)` **e** que o número de dígitos expostos ≤ (cc + ndc + 5). Um teste
de propriedade sobre a lista de casos vale mais que 10 asserções literais.

### Pattern 4: Kover — configuração exata verificada

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)          // <- novo
}

kover {
    reports {
        filters {
            includes { classes("org.sentinela.app.domain.*", "org.sentinela.app.phone.*") }
        }
        verify {
            rule("Cobertura minima de dominio e normalizacao") {
                minBound(80)
            }
        }
    }
}
```
```properties
# gradle.properties — OBRIGATÓRIO, senão o build morre com "Metaspace"
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

Evidência real do gate funcionando (bound temporariamente em 99):
```
* What went wrong:
Execution failed for task ':app:koverVerify' (registered by plugin 'org.jetbrains.kotlinx.kover').
> Rule 'Cobertura minima de dominio e normalizacao' violated:
  lines covered percentage is 94.736800, but expected minimum is 99
```
**Baseline atual de `domain` + `phone`: 94,74% de linhas** — o gate de 80% já passa hoje; o
trabalho da fase é mantê-lo passando depois de adicionar o normalizer (que é o código não
coberto de verdade).

Tasks disponíveis (confirmadas por `./gradlew tasks --all`): `koverVerify`, `koverVerifyDebug`,
`koverHtmlReport`, `koverXmlReport`, `koverLog`, `koverPrintCoverage`. `koverVerify` agrega as
variantes debug **e** release; se isso incomodar, use `koverVerifyDebug`.

### Anti-Patterns to Avoid

- **`PhoneNumberUtil.createInstance(context)` dentro de `phone/`** — importa `android.content.Context`,
  quebra o critério de sucesso 4 e torna o teste impossível sem Robolectric.
- **`PhoneNumberUtil` como `object`/singleton global** — impede o loader de teste e cria custo de
  init no cold start do Service em local imprevisível. Instância única sim, mas **criada e
  guardada pelo `AppContainer`** (a construção carrega metadados: ~dezenas de ms; a Phase 5 tem
  orçamento p95 < 200 ms — construa fora do `onScreenCall`).
- **`mask()` que lança exceção** — ela roda em caminho de log; uma exceção lá derruba o Service.
- **Confiar em `parse()` ter sucesso como sinal de validade** — medido: `987654321`/BR faz parse
  com sucesso e devolve `+55987654321` com `valid=false`. Gate **sempre** em `isValidNumber`.
- **Duas funções de máscara** (log e UI) — decisão do usuário: uma só.

---

## Don't Hand-Roll

| Problema | Não construa | Use | Por quê |
|----------|--------------|-----|---------|
| Parse/validação internacional | regex de DDI/DDD | `PhoneNumberUtil.parse` + `isValidNumber` | Metadados de 200+ países mudam mensalmente |
| Tamanho do "DDD" para a máscara | tabela BR hardcoded | `getLengthOfNationalDestinationCode` | Funciona para +1, +44, 0800, 4004 (medido) |
| Formatação E.164 | `"+" + cc + nsn` | `format(n, E164)` | Trata prefixos nacionais (`0800…` → `+558001234567`) |
| Só-dígitos | `filter { it.isDigit() }` | `PhoneNumberUtil.normalizeDigitsOnly` (estático) | Trata dígitos não-ASCII/árabes |
| Cobertura | script de contagem | Kover | Gate real, integrado ao Gradle |
| Matriz de precedência | 54 testes copiados/colados | `@RunWith(Parameterized::class)` | Decisão do usuário; e cobre combinações futuras |

**Key insight:** o único lugar onde hand-rolling é **inevitável** nesta fase é a regra do 9º
dígito brasileiro e o tratamento de números curtos — porque o libphonenumber **não** resolve
nenhum dos dois (comprovado abaixo). Isole essas duas regras em funções nomeadas e testadas, com
comentário explicando por que existem.

---

## Comportamento real do libphonenumber (medido, não presumido)

Executado em `testDebugUnitTest` real, artefato `io.michaelrocks:libphonenumber-android:9.0.34`,
2026-07-29:

| Entrada | Região | E.164 devolvido | `isValidNumber` | `isPossibleNumber` | `getNumberType` |
|---------|--------|-----------------|------------------|--------------------|-----------------|
| `11987654321` | BR | `+5511987654321` | **true** | true | MOBILE |
| `+5511987654321` | BR | `+5511987654321` | **true** | true | MOBILE |
| `1133334444` | BR | `+551133334444` | **true** | true | FIXED_LINE |
| `987654321` (sem DDD) | BR | `+55987654321` | **false** | true | UNKNOWN |
| `1187654321` (celular sem o 9) | BR | `+551187654321` | **false** | true | UNKNOWN |
| `+55 11 8765-4321` | BR | `+551187654321` | **false** | true | UNKNOWN |
| `190` | BR | `+55190` | **false** | **false** (`TOO_SHORT`) | UNKNOWN |
| `0800 123 4567` | BR | `+558001234567` | **true** | true | TOLL_FREE |
| `40041234` | BR | `+5540041234` | **true** | true | SHARED_COST |
| `+1 212 555 0123` | BR | `+12125550123` | **true** | true | FIXED_LINE_OR_MOBILE |
| `+44 20 7183 8750` | BR | `+442071838750` | **true** | true | FIXED_LINE |
| `2125550123` | US | `+12125550123` | **true** | true | FIXED_LINE_OR_MOBILE |
| `911` | US | `+1911` | **false** | **false** (`TOO_SHORT`) | UNKNOWN |
| `abc` | BR | — | `NumberParseException(NOT_A_NUMBER)` | — | — |
| `""` | BR | — | `NumberParseException(NOT_A_NUMBER)` | — | — |
| `+999999` | BR | — | `NumberParseException(INVALID_COUNTRY_CODE)` | — | — |

**Leituras obrigatórias desta tabela:**

1. **Celular BR sem o 9 NÃO é auto-corrigido.** O CONTEXT.md diz "quando libphonenumber
   reconhecer o número como válido-com-correção" — essa capacidade **não existe**. Os metadados
   BR atuais não contêm mais o padrão de celular de 8 dígitos. Ver Open Question 1.
2. **`190` não tem E.164 utilizável.** `+55190` é sintaticamente um E.164 mas semanticamente
   lixo — não é discável fora do Brasil e colide entre países. Ver Open Question 2.
3. **0800 e 4004 funcionam perfeitamente** — `valid=true`. A parte da decisão do usuário sobre
   números de serviço "longos" está satisfeita sem esforço.
4. **`parse` sem DDD não falha** — só `isValidNumber` denuncia. Nunca use sucesso de `parse`
   como critério.

---

## Matriz de precedência (o que a Wave de testes precisa cobrir)

O motor tem 7 níveis. A matriz mínima exaustiva:

| # | Cenário | Eixos | Combinações |
|---|---------|-------|-------------|
| 1 | Saída | `direction=OUTGOING` × qualquer settings | 1 (+ 1 provando que ganha de proteção off e de privado) |
| 2 | Proteção off | `protectionEnabled=false` | 1 (+ 1 provando que ganha de privado) |
| 3 | Privado | `blockPrivateNumbers` ∈ {true,false} × `blockMode` {REJECT, VOICEMAIL} × `hideFromNativeCallLog` {t,f} | 1 + 3 |
| 4 | Contato | `contactsPolicy` ∈ 4 `OriginPolicy` × (BLOCK × 3 variações de bloqueio) | 3 + 3 = 6 |
| 5 | Whitelist | `whitelistPolicy` ∈ 4 × idem | 6 |
| 6 | Falha de consulta | {`ContactLookup.UNAVAILABLE`, `WhitelistLookup.LOOKUP_FAILED`} × `FallbackPolicy` {ALLOW, BLOCK} | 4 |
| 7 | Desconhecido | `unknownPolicy` ∈ 4 × idem × número {Valid, Invalid} | 12 |
| — | Precedência entre níveis | contato HIT ganha de whitelist HIT; HIT ganha de UNAVAILABLE | 3 |

**Sub-matriz de `block()`** (fácil de esquecer — é onde `DEC-03` realmente se prova):

| `blockMode` | `hideFromNativeCallLog` | Resultado esperado |
|-------------|--------------------------|--------------------|
| `SILENT_VOICEMAIL` | true | `SendSilentlyToVoicemail` |
| `SILENT_VOICEMAIL` | false | `SendSilentlyToVoicemail` (voicemail tem precedência sobre o flag) |
| `REJECT` | true | `BlockWithoutTrace` |
| `REJECT` | false | `Reject` |

Um `@Parameterized` com `(policy, origin, blockMode, hideLog) → CallDecision esperado` cobre
4×3×2×2 = 48 linhas de tabela e é o formato pedido pelo usuário.

**Teste de reason codes (DEC-04), barato e valioso:**
```kotlin
@Test fun `reason codes nunca carregam dado pessoal`() {
    DecisionReason.entries.forEach { r ->
        assertTrue(r.code.matches(Regex("[a-z_]+")))   // sem dígito, sem espaço, sem acento
    }
}
```

---

## Common Pitfalls

### Pitfall 1: Metaspace estoura ao adicionar Kover
**O que acontece:** `> Failed to notify build listener. > Metaspace` — build falha, sem relação
aparente com cobertura. **Reproduzido hoje** com `MaxMetaspaceSize=512m` (valor atual do repo).
**Como evitar:** subir para `1g` em `gradle.properties` **na mesma task** que adiciona o plugin.
**Sinal de alerta:** falha em `Failed to notify build listener` logo após o plugin entrar.

### Pitfall 2: teste de normalização falso-verde por metadados vazios
**O que acontece:** o `MetadataLoader` de teste devolve `null` para tudo (caminho errado);
`PhoneNumberUtil` trata como região desconhecida e **não lança** — os testes passam validando
comportamento errado. **Como evitar:** o helper de teste deve `check()` que
`PhoneNumberMetadataProto_BR` existe e falhar alto se não. Adicione um teste-sentinela:
`assertTrue(util.isValidNumber(util.parse("+5511987654321", null)))`.

### Pitfall 3: `getLengthOfNationalDestinationCode` = 0 em número inválido
**O que acontece:** `mask()` faz `digits.substring(0, 0)` e produz máscara sem área, ou pior,
lança `StringIndexOutOfBounds` num caminho de log. **Como evitar:** ramo degradado explícito
quando `ndc == 0` ou `resto.length < 5`. Teste com `+55987654321` (ndc=0 medido).

### Pitfall 4: `TelephonyManager` retorna string vazia, não null
**O que acontece:** `simCountryIso` = `""` em aparelho sem SIM → região `""` → `parse` lança
`INVALID_COUNTRY_CODE` em todo número nacional. **Como evitar:** `takeIf { it.isNotBlank() }` em
cada degrau da cascata + validação de formato (2 letras) no `CascadingRegionProvider`. Testar
com fakes que devolvem `""`, `"  "`, `"br"` (minúsculo — o Android devolve **minúsculo**;
libphonenumber espera **maiúsculo**) e `"ZZ"`.

### Pitfall 5: `UnsupportedOperationException` em tablet Wi-Fi-only
**O que acontece:** `@RequiresFeature` não é permissão, mas em Android 14+ os métodos lançam
`UnsupportedOperationException` quando a feature de telefonia falta. **Como evitar:**
`runCatching` no `AndroidRegionProvider` (nunca no `CascadingRegionProvider`, que deve ser puro).

### Pitfall 6: construir `PhoneNumberUtil` no caminho quente
**O que acontece:** `createInstance` carrega e desserializa metadados; feito dentro de
`onScreenCall` come o orçamento de 200 ms da Phase 5. **Como evitar:** instância única no
`AppContainer`, criada preguiçosamente **no `Application.onCreate` ou no primeiro uso fora do
callback**. Não é problema desta fase, mas a API escolhida agora decide isso.

### Pitfall 7: Kover contando código de teste ou de UI
**O que acontece:** o filtro `includes` errado infla ou desinfla a métrica; 80% vira teatro.
**Como evitar:** o filtro `classes("org.sentinela.app.domain.*", "org.sentinela.app.phone.*")`
foi verificado devolvendo 94,74% — número plausível para 4 arquivos de domínio com 24 testes.
Rode `./gradlew koverLog` uma vez e confira que o denominador faz sentido.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 `4.13.2` (+ `org.junit.runners.Parameterized`), testes JVM puros. **Sem Robolectric** (4.16.1 não suporta SDK 37). |
| Config file | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` (**pré-requisito do carregamento de metadados em teste — não remover**) |
| Cobertura | Kover `0.9.9`, regra `minBound(80)` filtrada em `org.sentinela.app.domain.*` + `org.sentinela.app.phone.*` |
| Quick run command | `./gradlew testDebugUnitTest` |
| Full suite command | `./gradlew testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh` |
| Pré-requisitos | `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"`; JDK 17; `MaxMetaspaceSize=1g` em `gradle.properties` |
| Runtime medido | `testDebugUnitTest` ~3–15 s incremental; `koverVerify` ~22 s a frio (medido hoje) |
| Relatórios | `app/build/test-results/testDebugUnitTest/*.xml`, `app/build/reports/kover/html/index.html`, `app/build/reports/kover/report.xml` |

### Phase Requirements → Test Map

| Req | Comportamento | Tipo | Comando automatizado | Arquivo existe? |
|-----|---------------|------|----------------------|-----------------|
| DEC-01 | Domínio sem import Android | invariante | `bash scripts/verify-invariants.sh` (estender p/ `phone/`) | ✅ (estender) |
| DEC-02 | Precedência dos 7 níveis | unit nomeado | `./gradlew testDebugUnitTest --tests "*CallDecisionEngineTest"` | ✅ 24 testes |
| DEC-02/03/CTT-03/WLT-08 | Matriz política × origem × blockMode | unit parametrizado | `./gradlew testDebugUnitTest --tests "*DecisionMatrixTest"` | ❌ Wave 0 |
| DEC-04 | Reason codes sem dado pessoal | unit | `./gradlew testDebugUnitTest --tests "*DecisionReasonTest"` | ❌ Wave 0 |
| DEC-05 | Fallback ALLOW/BLOCK × 2 gatilhos | unit | incluído em `*DecisionMatrixTest` | ❌ Wave 0 |
| NRM-01/02 | E.164 BR e internacional (tabela medida acima) | unit | `./gradlew testDebugUnitTest --tests "*LibPhoneNumberNormalizerTest"` | ❌ Wave 0 |
| NRM-02 | Sem DDD → `Invalid`; 9º dígito; curtos | unit | idem | ❌ Wave 0 |
| NRM-04 | Máscara nunca revela NSN completo, em nenhum formato | unit (property-ish) | `./gradlew testDebugUnitTest --tests "*PhoneMaskTest"` | ❌ Wave 0 |
| Região | Cascata SIM→usuário→BR, com `""`, `"br"`, `null`, exceção | unit c/ fakes | `./gradlew testDebugUnitTest --tests "*CascadingRegionProviderTest"` | ❌ Wave 0 |
| QLT-07 | Cobertura ≥ 80% em `domain` + `phone` | gate | `./gradlew koverVerify` | ❌ Wave 0 (config) |
| QLT-02 | Sem regressão de lint/detekt | static | `./gradlew lint detekt` | ✅ |

Nenhum item desta fase exige aparelho físico. **Nenhum `checkpoint:human-*`.**

### Sampling Rate

- **Por commit de task:** `./gradlew testDebugUnitTest`
- **Por merge de wave:** `./gradlew testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh`
- **Phase gate:** suíte completa verde **pós-`clean`** (`./gradlew clean && ...`), com
  `N actionable tasks: M executed`, M > 0 — a mesma regra probatória fixada na Phase 1
  (`UP-TO-DATE`/`FROM-CACHE` não provam nada).
- **Latência máxima de feedback:** < 60 s.

### Wave 0 Gaps

- [ ] `gradle.properties` — `MaxMetaspaceSize=1g` (**bloqueia tudo o mais; primeiro item**)
- [ ] `gradle/libs.versions.toml` + `app/build.gradle.kts` — plugin Kover 0.9.9 + bloco `kover {}`
- [ ] `app/src/test/java/org/sentinela/app/phone/TestMetadata.kt` — helper do `MetadataLoader`
      (fixture compartilhada de **todos** os testes de normalização; sem ele nada roda)
- [ ] `scripts/verify-invariants.sh` — estender a checagem de pureza a `phone/`
- [ ] Demais arquivos de teste marcados ❌ acima são produto das próprias tasks, não Wave 0.

Instalação de framework: **nenhuma** — JUnit 4 já está configurado e verde.

---

## State of the Art

| Antes | Agora | Impacto |
|-------|-------|---------|
| "libphonenumber-android exige `Context`" (crença comum, e o que o README destaca) | `createInstance(MetadataLoader)` é público e suficiente | Testes JVM puros sem Robolectric — **verificado neste repo** |
| Celular BR de 8 dígitos era metadado válido | Removido dos metadados; hoje `valid=false` | A regra do 9º dígito virou responsabilidade da aplicação |
| `ShortNumberInfo` como solução para `190` | Construtor package-private no port `-android`, sem factory | Números curtos precisam de regra própria |
| Kover 0.7/0.8 com DSL `koverReport {}` | 0.9.x usa `kover { reports { filters/verify } }` | A DSL antiga não compila; use a testada acima |
| JaCoCo | Kover | Decisão do projeto |

**Depreciado / a evitar:** `getSimCountryIso(int subId)` (deprecated desde API 30 e esse **sim**
exige permissão em algumas variantes) — use sempre a sobrecarga **sem argumento**.

---

## Open Questions

### 1. Celular BR sem o 9 — o CONTEXT.md assume uma capacidade que não existe (ALTA prioridade)
- **O que sabemos:** medido — `+55 11 8765-4321` → `isValidNumber=false`, `type=UNKNOWN`,
  `isPossibleNumber=true`. libphonenumber **não** insere o 9.
- **O que não está claro:** o usuário quer (a) `Invalid` também nesse caso, ou (b) uma regra
  brasileira própria que insere o 9.
- **Recomendação:** implementar (b) como regra explícita e estreita, porque a decisão do usuário
  ("normalizar acrescentando o 9… caso de teste explícito") é claramente intencional e o efeito
  prático é grande (números antigos em agendas e listas). Guarda-corpo obrigatório:
  aplicar **somente** se `countryCode == 55` **e** NSN tem 10 dígitos **e** o primeiro dígito do
  assinante ∈ `6..9`; então re-parsear com o 9 inserido e **só aceitar se o resultado for
  `isValidNumber && type == MOBILE`** — caso contrário, `Invalid`. (Verificado: `1187654321` →
  `11987654321` → `valid=true, MOBILE`.) Marcar a função com comentário explicando que ela existe
  porque a biblioteca não cobre o caso.

### 2. `190` / números curtos — E.164 não existe para eles (ALTA prioridade)
- **O que sabemos:** `190`/BR → `+55190`, `valid=false`, `possible=false (TOO_SHORT)`.
  `ShortNumberInfo` é inutilizável neste artefato.
- **O que não está claro:** o CONTEXT diz "`Valid` com o E.164 que libphonenumber devolver quando
  possível" — mas "quando possível" não se aplica a `190`.
- **Recomendação:** classificar como `Valid` usando **os dígitos nacionais crus** como chave
  (`"190"`), não `+55190`, quando: `parse` teve sucesso, `isValidNumber=false`, o motivo é
  `TOO_SHORT`, e a entrada é só-dígitos com ≤ 6 dígitos. Documentar no KDoc de
  `NormalizationResult.Valid` que a chave é E.164 **exceto para códigos curtos**, e usar o mesmo
  limiar (`LIMIAR_CURTO = 6`) da máscara — um só número mágico, uma só constante, testado nos dois
  usos. Isso respeita "não criar categoria separada no MVP" e mantém o número whitelistável.
  **Confirmar com o usuário no plan-check**, pois é uma nuance de contrato de dados que a Phase 3
  vai persistir.

### 3. `koverVerify` agrega debug + release
- **O que sabemos:** existem `koverVerifyDebug` e `koverVerifyRelease`; `koverVerify` cobre ambas
  e passou.
- **Recomendação:** usar `koverVerify` (mais estrito, custo já medido em ~22 s). Se o tempo
  incomodar durante a execução, trocar para `koverVerifyDebug` no comando por-wave e manter
  `koverVerify` no phase gate. Discricionário.

### 4. Estabilidade da chave `android_merged_assets` do AGP
- **O que sabemos:** presente e correta no AGP 9.3.0 (medido). É uma propriedade projetada para
  Robolectric, portanto razoavelmente estável, mas não é API pública documentada.
- **Recomendação:** helper com fallback para o caminho fixo e mensagem de erro explícita.
  Confiança MÉDIA neste ponto específico — é o único item da fase que pode quebrar num bump
  futuro de AGP, e quebra **ruidosamente** (todos os testes de normalização falham), que é o modo
  de falha aceitável.

---

## Sources

### Primary (HIGH confidence)
- **Experimento executado neste repositório** (2026-07-29, AGP 9.3.0 / Gradle 9.6.1 / JDK 17):
  `testDebugUnitTest` com `PhoneNumberUtil.createInstance(MetadataLoader)` — tabela de
  comportamento e prova do carregamento de metadados via `test_config.properties`.
- **Experimento Kover**: plugin 0.9.9 aplicado, `koverVerify` passando em 80% e falhando em 99
  com mensagem citada; falha de Metaspace reproduzida com o valor atual do `gradle.properties`.
  (Ambas as mudanças foram revertidas; `git status` limpo.)
- `javap` sobre `libphonenumber-android-9.0.34.aar/classes.jar` — assinaturas de
  `createInstance`, `MetadataLoader`, `AssetsMetadataLoader`, `ClassPathResourceMetadataLoader` e
  `ShortNumberInfo` (construtor package-private).
- **AOSP** `frameworks/base/telephony/java/android/telephony/TelephonyManager.java`
  (`android-15.0.0_r1`): `getSimCountryIso()` → `@RequiresFeature(FEATURE_TELEPHONY_SUBSCRIPTION)`
  apenas; `getNetworkCountryIso()` e `getNetworkCountryIso(int)` →
  `@RequiresFeature(FEATURE_TELEPHONY_RADIO_ACCESS)` apenas. **Nenhum `@RequiresPermission`.**
  https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/telephony/java/android/telephony/TelephonyManager.java
- `maven-metadata.xml` do Maven Central para os três artefatos (versões e datas).

### Secondary (MEDIUM confidence)
- Estabilidade futura da propriedade `android_merged_assets` — inferida do uso pelo AGP para
  Robolectric, não de contrato documentado.

### Tertiary (LOW confidence)
- Nenhuma afirmação desta pesquisa depende exclusivamente de busca web não verificada.

**Nota metodológica:** os stubs em `android.jar` e `android-stubs-src.jar` do SDK **removem** as
anotações `@RequiresPermission` (verificado: `getImei()` aparece sem anotação alguma). Não use o
SDK local como evidência de permissão — só o AOSP ou a doc oficial servem.

---

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** — versões confirmadas no `maven-metadata.xml`; compatibilidade Kover×AGP×Gradle executada.
- Comportamento do libphonenumber: **HIGH** — tabela medida, não lembrada.
- Permissões da cascata de região: **HIGH** — fonte AOSP citada linha a linha.
- Estratégia de teste JVM puro: **HIGH** para funcionar hoje, **MEDIUM** para estabilidade em bumps futuros do AGP.
- Máscara: **HIGH** no algoritmo (`ndcLen` medido em 6 países/tipos); o limiar de 6 dígitos é recomendação, não fato.
- Precedência/matriz: **HIGH** — lida do código-fonte atual.

**Research date:** 2026-07-29
**Valid until:** 2026-08-28 (30 dias — reavaliar se AGP, Kover ou libphonenumber subirem de minor)

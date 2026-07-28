# Stack — versões estáveis verificadas em 2026-07-27

Conjunto validado entre si: AGP 9.3.0 + Gradle 9.6.1 + Kotlin embutido + KSP 2.3.10 +
Compose BOM 2026.06.01 + compileSdk 37. Espelhado em `gradle/libs.versions.toml`.

## Build

| Item | Versão | Fonte |
|------|--------|-------|
| Android Gradle Plugin | 9.3.0 (Gradle mín. 9.5.0; JDK mín. 17; API máx. 37) | developer.android.com/build/releases/agp-9-3-0-release-notes |
| Gradle (wrapper) | 9.6.1 | gradle.org/releases |
| Kotlin | embutido no AGP 9 (base 2.4.10; catalog mantém a versão para o plugin Compose) | blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/ |
| KSP | 2.3.10 — **standalone**: desde o KSP 2.3.0 não existe mais o formato `kotlinVersion-kspVersion` | kotlinlang.org/docs/ksp-quickstart.html |
| Compose compiler | plugin `org.jetbrains.kotlin.plugin.compose` (mesma versão do Kotlin) | — |
| JDK local | 17 (Homebrew, mesmo setup do dmconecta) — JDK 25 do sistema não roda o Gradle | gradle.properties |
| SDK | compileSdk 37 / targetSdk 37 (Android 17, estável 2026-06-16) / minSdk 29 | developer.android.com/about/versions/17/release-notes |

## Bibliotecas

| Item | Coordenada | Versão |
|------|-----------|--------|
| Compose BOM | androidx.compose:compose-bom | 2026.06.01 (ui 1.11.4, material3 1.4.0) |
| Activity Compose | androidx.activity:activity-compose | 1.13.0 |
| Lifecycle | androidx.lifecycle:lifecycle-*-compose | 2.11.0 |
| Navigation Compose | androidx.navigation:navigation-compose | 2.9.8 |
| Room | androidx.room:room-* | 2.8.4 |
| DataStore Preferences | androidx.datastore:datastore-preferences | 1.2.1 |
| Coroutines | org.jetbrains.kotlinx:kotlinx-coroutines-* | 1.11.0 |
| core-ktx | androidx.core:core-ktx | 1.19.0 (artefato de compat; extensões fundidas no core) |
| libphonenumber (port Android) | io.michaelrocks:libphonenumber-android | 9.0.34 (metadata otimizada p/ Android) |
| detekt | io.gitlab.arturbosch.detekt | 1.23.8 (2.0.0 ainda alpha — não usar) |

## Testes

| Item | Versão | Nota |
|------|--------|------|
| JUnit4 | 4.13.2 | padrão Android |
| MockK | 1.14.11 | |
| Turbine | 1.2.1 | Flows |
| Robolectric | 4.16.1 | suporta até SDK 36 → `@Config(sdk = [36])` |
| androidx.test core/runner/rules | 1.7.0 | exige minSdk ≥ 21 (ok, minSdk 29) |
| Espresso | 3.7.0 | |
| androidx.test.ext:junit | 1.3.0 | |

## Correção pós-pesquisa (validada no build)

A nota "AGP 9.x exige declarar plugin Kotlin explicitamente" da pesquisa web estava **invertida**:
o AGP 9 traz **Kotlin embutido** e falha o build se `org.jetbrains.kotlin.android` for aplicado
("The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0").
O plugin `org.jetbrains.kotlin.plugin.compose` continua sendo aplicado normalmente.

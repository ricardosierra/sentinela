import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// Identidade centralizada para permitir rebranding (ver docs/DECISOES.md)
val sentinelaApplicationId = "org.sentinela.app"

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = sentinelaApplicationId
    compileSdk = 37

    defaultConfig {
        applicationId = sentinelaApplicationId
        minSdk = 29 // ROLE_CALL_SCREENING exige Android 10 (API 29)
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        // Ligado no plano 06-06 para que a chave do extra de ação da notificação de chamada saia
        // do identificador do aplicativo em vez de um literal em Kotlin, que o invariante de
        // rebranding do projeto proíbe. Nenhum campo próprio é declarado aqui.
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // MigrationTestHelper le o JSON exportado pelo Room a partir dos assets do
    // androidTest. Sem este srcDir ele falha com "Cannot find the schema file".
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    lint {
        abortOnError = true // explicito: erro de lint quebra o build

        disable += setOf(
            // Strings e cores pre-escritas para as telas das Fases 5-9
            // (docs/design/TELAS.md). Sao ativos legitimos, nao lixo:
            // apagar destroi trabalho e recria custo nas fases de UI.
            // Reavaliar na Phase 9, quando as telas reais consumirem as strings.
            "UnusedResources",
            // O dicionario do lint e en-US e acusa falso positivo em conteudo
            // pt-BR ("momento" != "memento"). O idioma padrao do app e pt-BR.
            "Typos",
            // A versao do AGP e decisao de projeto (.planning/research/STACK.md):
            // 9.3.0 e a linha de base validada de ponta a ponta. Nao ha release
            // notes publicadas para a 9.3.1.
            "AndroidGradlePluginVersion",
        )
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// SchemaExportTest le `schemas/` direto do disco, e o Gradle nao tem como
// adivinhar isso: sem declarar a pasta como entrada, o teste fica UP-TO-DATE
// (ou FROM-CACHE) e passa VERDE mesmo com o schema apagado — o mesmo defeito
// probatorio que a Phase 1 documentou para o cache de build.
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("schemas"))
        .withPropertyName("roomExportedSchemas")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // AppOpenCounterTest le o fonte Kotlin direto do disco pelo mesmo motivo: o alvo
    // e estrutural (onde a contagem mora), e nenhum assert de valor prova isso.
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
        .withPropertyName("mainKotlinSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // CallStringsTest le os RECURSOS de verdade (varredura de honestidade da copy).
    // Sem declarar res/ como entrada, mudar so o strings.xml deixa o teste
    // UP-TO-DATE e o verde antigo passa a valer para texto novo nunca varrido.
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
        .withPropertyName("mainAndroidResources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
}

// Cobertura das Fases 2-3: gate real sobre motor, normalizacao, dados e configuracoes.
// koverVerify quebra o build abaixo do minimo — o filtro define o denominador.
kover {
    reports {
        filters {
            includes {
                classes(
                    "org.sentinela.app.domain.*",
                    "org.sentinela.app.phone.*",
                    "org.sentinela.app.data.*",
                    "org.sentinela.app.settings.*",
                    // Fase 5: a camada de triagem entra no denominador. O coordenador e puro
                    // e a traducao da resposta roda sob Robolectric, que e teste em JVM e
                    // portanto e medido pelo Kover.
                    "org.sentinela.app.telecom.*",
                    "org.sentinela.app.notifications.*",
                    "org.sentinela.app.permissions.*",
                )
            }
            excludes {
                // Codigo gerado pelo Room (KSP) so executa em teste INSTRUMENTADO,
                // que o Kover nao mede. Incluir no denominador derrubaria o gate com
                // falso-vermelho mesmo com o codigo humano 100% coberto.
                // Fica coberto por connectedDebugAndroidTest (planos 03-04/03-05).
                classes("org.sentinela.app.data.local.db.*")
                // Fase 4: a fonte do provider de contatos so executa em teste INSTRUMENTADO
                // (mesma razao do gerado pelo Room). A logica pura — estado de permissao, cache
                // e decisao HIT/MISS/UNAVAILABLE — fica FORA deste exclude e continua medida
                // pelo gate de 80%. Uma classe nomeada, jamais o pacote inteiro.
                classes("org.sentinela.app.data.contacts.ContactsContractLookupSource")
                // Fase 6, plano 06-09: UMA classe do modo discador — e somente esta — fica fora do
                // denominador. O que a mantem aqui e o CICLO DE VIDA: o servico de interface de
                // chamada so existe quando a propria telefonia o vincula, e os caminhos que
                // importam nele (vinculo, morte do processo no meio da chamada, revinculo ao
                // discador do aparelho) sao observados de fora do processo por
                // InCallServiceBindTest, InCallServiceDeathTest e scripts/verify-dialer-lifecycle.sh.
                // Nenhuma linha dele fica sem prova; a prova simplesmente nao e medivel em JVM.
                //
                // O exclude da costura que traduz comando de interface para telefonia foi REMOVIDO
                // no plano 06-09: a justificativa antiga ("so faz efeito com um objeto de chamada
                // montado pela plataforma") valia para atender, encerrar e tom, mas nao para mudo e
                // viva-voz, que operam sobre o servico de chamada. TelecomCallControlsTest prova os
                // oito comandos na propria costura e foi demonstrado vermelho com a delegacao
                // sabotada, entao a classe volta ao denominador do gate em vez de ficar escondida.
                //
                // Tudo que e PURO no modo discador continua no denominador do gate de 80%: estado e
                // retrato da chamada, tradutor dos codigos da plataforma, tradutor da mascara de
                // rotas de audio, a costura dos controles, o coordenador da sessao e o armazem da
                // sessao. Uma classe nomeada, jamais o pacote inteiro — classe pura com cobertura
                // baixa se resolve escrevendo teste, nunca excluindo.
                //
                // Medido depois de devolver a costura ao denominador: noventa e seis inteiros e 69
                // centesimos por cento de linhas, com a costura em cem por cento de linhas, ramos e
                // metodos. O gate segue em 80 e foi demonstrado falhando com o piso em 99.
                classes("org.sentinela.app.telecom.SentinelaInCallService")
                classes("org.sentinela.app.telecom.SentinelaInCallService\$*")
                classes("*_Impl", "*_Impl\$*")
                annotatedBy("androidx.room.Dao", "androidx.room.Database")
            }
        }
        verify {
            rule("Cobertura minima de dominio, normalizacao e dados") {
                minBound(80)
            }
        }
    }
}

dependencies {
    // O androidTest herda as versoes do runtime principal por resolucao consistente,
    // entao o piso precisa ser declarado aqui e nao so no androidTest. Nao acrescenta
    // biblioteca ao APK: kotlinx-serialization-core ja entra via lifecycle — muda so
    // a versao, do 1.7.3 para o piso que o room-migration exige.
    constraints {
        implementation(libs.kotlinx.serialization.core) {
            because("room-migration 2.8.4 (MigrationTestHelper) quebra com serialization 1.7.3")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    // Reservado no version catalog desde o bootstrap para "as Fases 5-6 (UI)": as telas de
    // chamada e discagem precisam de mic/mic_off, dialpad, call_end, backspace e shield, que
    // nao existem no conjunto nucleo de icones. Entra na versao do Compose BOM.
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.libphonenumber.android)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    // Fase 6: a semantica das telas de chamada (alvo de toque, descricao, ordem de foco) e
    // verificada em JVM sob Robolectric. As duas dependencias ja estavam no version catalog e no
    // conjunto instrumentado desde o bootstrap — aqui elas passam a valer tambem para src/test,
    // porque medir alvo de toque no emulador tornaria o criterio caro demais para rodar sempre.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    // Fase 6: as telas de chamada e discagem sao testadas em JVM sob Robolectric. Nao e
    // biblioteca nova — e a MESMA do androidTest, que o version catalog ja declara e que entra
    // na versao do Compose BOM. Sem ela nao existe regra de teste de composicao no lado JVM.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

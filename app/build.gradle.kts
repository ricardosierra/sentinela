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
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
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

detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
}

// Cobertura da Fase 2: gate real sobre o motor de decisao e a normalizacao.
// koverVerify quebra o build abaixo do minimo — o filtro define o denominador.
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

dependencies {
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

    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

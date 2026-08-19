import java.io.FileInputStream
import java.util.Properties
import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.play.publisher)
}

// Identidade centralizada para permitir rebranding (ver docs/DECISOES.md)
val sentinelaApplicationId = "org.sentinela.app"

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

// O CI injeta a versão a partir da tag vX.Y.Z. Localmente, o valor versionado
// abaixo continua sendo a fonte de verdade para desenvolvimento.
val ciVersionCode = providers.environmentVariable("SENTINELA_VERSION_CODE").orNull
    ?.toIntOrNull()
    ?.also { require(it > 0) { "SENTINELA_VERSION_CODE deve ser positivo" } }
val ciVersionName = providers.environmentVariable("SENTINELA_VERSION_NAME").orNull

// Valores por propriedade deixam a mesma build publicar em tracks diferentes
// sem trocar fonte. O workflow usa draft no canal interno por padrão.
val playTrack = providers.gradleProperty("playTrack").orElse("internal").get()
val playReleaseStatus = providers.gradleProperty("playReleaseStatus").orElse("DRAFT").get()
    .uppercase()
val playUserFraction = providers.gradleProperty("playUserFraction").orNull?.toDoubleOrNull()
val parsedPlayReleaseStatus = runCatching { ReleaseStatus.valueOf(playReleaseStatus) }
    .getOrElse { error("playReleaseStatus inválido: $playReleaseStatus") }

android {
    namespace = sentinelaApplicationId
    compileSdk = 37

    defaultConfig {
        applicationId = sentinelaApplicationId
        minSdk = 29 // ROLE_CALL_SCREENING exige Android 10 (API 29)
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.1"
        ciVersionCode?.let { versionCode = it }
        ciVersionName?.let { versionName = it }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
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
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

            // Configuracao do processo que roda os testes. NAO confundir com o
            // `org.gradle.jvmargs` do gradle.properties: aquele vale para o daemon do Gradle,
            // este para quem executa a suite.
            //
            // O sintoma que isto resolve engana: quando o processo de teste morre, o Gradle NAO
            // reporta falha de teste. Ele reporta `EOFException` — ou `NoSuchFileException` no
            // arquivo de resultados parciais — sem relatorio nenhum, o que parece defeito no
            // codigo testado e nao e. Pior: o arquivo de resultados fica truncado, e ai TODA
            // execucao seguinte falha na leitura, mesmo uma que rodaria bem. Ao investigar,
            // apague `app/build/test-results` antes de cada tentativa, senao o primeiro erro
            // contamina o diagnostico dos proximos.
            //
            // O que foi MEDIDO em 2026-08-06, nesta ordem:
            //  - heap 2g e 3g, com Metaspace 1g e 2g: a suite completa continua morrendo;
            //  - `forkEvery` 15 e 5: continua morrendo;
            //  - `forkEvery` 1: passa inteira.
            //
            // Ou seja, o gatilho NAO e memoria crescendo aos poucos — se fosse, mais heap ou
            // mais Metaspace teria resolvido, e reciclar a cada 5 classes tambem. O que quebra e
            // estado que sobrevive no processo de uma classe para a outra (o Robolectric monta um
            // ambiente Android por classe e o agente do Kover instrumenta cada carregador de
            // classes novo). Um processo por classe e o unico ajuste que fecha o caso.
            //
            // O preco e real e esta anotado de proposito: a suite passa de ~40s para ~8min,
            // porque cada classe paga a partida de uma JVM. E gate de qualidade, nao laco de
            // desenvolvimento; para rodar rapido durante o trabalho, filtre com `--tests`.
            // Reduzir este numero exige medir de novo — nao mexa nele "para acelerar" sem rodar
            // a suite inteira depois.
            all {
                it.maxHeapSize = "2g"
                it.jvmArgs("-XX:MaxMetaspaceSize=1g")
                it.setForkEvery(1)
                it.systemProperty("user.language", "pt")
                it.systemProperty("user.country", "BR")
            }
        }
    }

    // MigrationTestHelper le o JSON exportado pelo Room a partir dos assets do
    // androidTest. Sem este srcDir ele falha com "Cannot find the schema file".
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    lint {
        abortOnError = true // explicito: erro de lint quebra o build

        // A regra de recursos nao usados saiu deste bloco na Phase 7 e passou a ser
        // estreitada nominalmente por fase em app/lint.xml.
        //
        // Correcao de premissa (medida em 2026-07-30): a supressao NUNCA foi o que
        // mantinha o build verde. Com a regra reabilitada, lintDebug sai com codigo 0
        // — a severidade e de aviso e a conversao de aviso em erro nao esta ligada.
        // Ela servia para manter o relatorio limpo enquanto as telas nao existiam.
        // Agora que as telas consomem as chaves, o relatorio volta a ser util.
        disable += setOf(
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

play {
    // A Play aceita apps novos somente como AAB. Nunca publicar APK por engano.
    defaultToAppBundles.set(true)
    track.set(playTrack)
    releaseStatus.set(parsedPlayReleaseStatus)
    if (parsedPlayReleaseStatus == ReleaseStatus.IN_PROGRESS) {
        require(playUserFraction != null && playUserFraction in 0.0..1.0) {
            "playUserFraction entre 0 e 1 é obrigatório para rollout em progresso"
        }
        userFraction.set(playUserFraction)
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
    // Nao e biblioteca nova: e a MESMA do conjunto instrumentado, na versao do Compose BOM.
    // (Este bloco estava DUPLICADO desde a Fase 6, com dois comentarios diferentes; a segunda
    // copia saiu na Fase 7 e o argumento util dela foi absorvido na linha acima.)
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

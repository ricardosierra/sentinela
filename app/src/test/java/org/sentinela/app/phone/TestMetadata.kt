package org.sentinela.app.phone

import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.io.File
import java.util.Properties

/**
 * Fixture compartilhada dos testes de normalizacao: constroi um [PhoneNumberUtil] com os
 * metadados REAIS do libphonenumber em JVM pura, sem Robolectric e sem `Context`.
 *
 * Os metadados vivem em `assets/` do AAR; o AGP os mescla num diretorio que o teste unitario
 * localiza via `com/android/tools/test_config.properties` — gerado porque
 * `testOptions.unitTests.isIncludeAndroidResources = true` esta ligado. Nao remover essa flag.
 *
 * Falha ALTO se os metadados nao existirem: um loader vazio nao faz o libphonenumber lancar,
 * e o resultado seria uma suite falso-verde (Pitfall 2 da pesquisa da Fase 2).
 */
object TestMetadata {

    private const val CONFIG = "com/android/tools/test_config.properties"
    private const val FALLBACK_ASSETS = "build/intermediates/assets/debug/mergeDebugAssets"
    private const val SENTINEL =
        "io/michaelrocks/libphonenumber/android/data/PhoneNumberMetadataProto_BR"

    private val assetsDir: File by lazy {
        val stream = TestMetadata::class.java.classLoader?.getResourceAsStream(CONFIG)
        // `android_merged_assets` vem relativo ao diretorio do modulo (app/), que e o working
        // dir do teste — nao converter para absoluto assumindo a raiz do repo.
        val dir = if (stream != null) {
            val props = Properties()
            stream.use { props.load(it) }
            File(
                checkNotNull(props.getProperty("android_merged_assets")) {
                    "android_merged_assets ausente em $CONFIG (AGP mudou a chave?)"
                },
            )
        } else {
            File(FALLBACK_ASSETS)
        }
        check(File(dir, SENTINEL).exists()) {
            "Metadados do libphonenumber nao encontrados em $dir. " +
                "Exige testOptions.unitTests.isIncludeAndroidResources = true e ./gradlew mergeDebugAssets."
        }
        dir
    }

    val loader = MetadataLoader { name ->
        File(assetsDir, name.removePrefix("/")).takeIf { it.exists() }?.inputStream()
    }

    fun util(): PhoneNumberUtil = PhoneNumberUtil.createInstance(loader)
}

package org.sentinela.app.platform

import android.content.Context
import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.metadata.source.AssetsMetadataLoader

/**
 * Carrega os metadados do libphonenumber dos assets do AAR.
 *
 * Unico ponto Android do caminho de normalizacao: `phone/` permanece JVM puro (invariante do
 * Bloco 3 do `scripts/verify-invariants.sh`) porque recebe o [MetadataLoader] pronto.
 */
fun assetsPhoneMetadataLoader(context: Context): MetadataLoader =
    AssetsMetadataLoader(context.assets)

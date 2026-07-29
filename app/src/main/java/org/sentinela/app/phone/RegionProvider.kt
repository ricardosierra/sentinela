package org.sentinela.app.phone

/** Resolve a regiao padrao (ISO-3166-1 alpha-2 MAIUSCULO) usada quando o numero vem sem DDI. */
fun interface RegionProvider {

    /** Regiao em duas letras maiusculas, ou null quando indisponivel. */
    fun currentRegion(): String?
}

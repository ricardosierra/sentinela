package org.sentinela.app.phone

/**
 * Normalização para E.164 com libphonenumber-android (Fase 2).
 * Regras BR: DDI +55, DDD obrigatório, celular com 9 dígitos, fixo sem 9.
 * Formatação bonita é só visual — E.164 é a fonte de verdade.
 */
interface PhoneNumberNormalizer {

    /** Região padrão quando o número vem sem DDI (MVP: "BR"). */
    fun normalize(raw: String, defaultRegion: String = "BR"): NormalizationResult

    /** Máscara segura para exibição/log (ex.: +55 11 9****-1234). */
    fun mask(e164: String): String
}

sealed interface NormalizationResult {
    data class Valid(val e164: String) : NormalizationResult
    data class Invalid(val reason: String) : NormalizationResult
}

package org.sentinela.app.domain

/**
 * Reason codes internos. Nunca carregam dado pessoal; são o único detalhe
 * de decisão que pode aparecer em log técnico.
 */
enum class DecisionReason(val code: String) {
    OUTGOING_CALL("outgoing_call"),
    PROTECTION_DISABLED("protection_disabled"),
    PRIVATE_NUMBER("private_number"),
    CONTACT("contact"),
    PERSONAL_WHITELIST("personal_whitelist"),
    UNKNOWN_NUMBER("unknown_number"),
    INVALID_NUMBER("invalid_number"),
    LOCAL_LOOKUP_FAILURE("local_lookup_failure"),
    FALLBACK_POLICY("fallback_policy"),
}

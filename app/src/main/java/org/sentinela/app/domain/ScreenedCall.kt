package org.sentinela.app.domain

/** Entrada pura do motor de decisão — nenhum tipo do Telecom vaza para cá. */
data class ScreenedCall(
    val direction: CallDirection,
    val number: ScreenedNumber,
)

enum class CallDirection { INCOMING, OUTGOING }

sealed interface ScreenedNumber {
    /** Número normalizado em E.164 (fonte de verdade). */
    data class Valid(val e164: String) : ScreenedNumber

    /** Chamada privada/restrita/sem handle. */
    data object Private : ScreenedNumber

    /** Handle presente mas impossível de normalizar. */
    data object Invalid : ScreenedNumber
}

/** Resultado da consulta local à whitelist, resolvido pela camada de dados. */
enum class WhitelistLookup { HIT, MISS, LOOKUP_FAILED }

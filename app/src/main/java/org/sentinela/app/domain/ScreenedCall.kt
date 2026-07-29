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

/**
 * Resultado da consulta local aos contatos do aparelho.
 * No modo filtro (sem discador padrão) a plataforma só entrega não-contatos,
 * então o Service passa MISS; UNAVAILABLE = modo discador sem permissão ou
 * consulta falhou — cai na política de fallback.
 */
enum class ContactLookup { HIT, MISS, UNAVAILABLE }

/** Resultado da consulta local à whitelist, resolvido pela camada de dados. */
enum class WhitelistLookup { HIT, MISS, LOOKUP_FAILED }

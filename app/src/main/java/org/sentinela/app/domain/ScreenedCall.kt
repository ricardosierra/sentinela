package org.sentinela.app.domain

/** Entrada pura do motor de decisão — nenhum tipo do Telecom vaza para cá. */
data class ScreenedCall(
    val direction: CallDirection,
    val number: ScreenedNumber,
    val isEmergency: Boolean = false,
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
 *
 * Com a leitura da agenda concedida, chamadas de contatos chegam ao serviço de
 * triagem também no modo filtro: quem decide é o motor, e HIT/MISS são resultado
 * da nossa própria consulta. Sem a leitura concedida, o Android nem aciona o
 * serviço para números já conhecidos da agenda — eles seguem pelo caminho nativo
 * e nunca chegam a este enum.
 *
 * UNAVAILABLE = permissão ausente ou consulta falhou; cai na política de fallback.
 */
enum class ContactLookup { HIT, MISS, UNAVAILABLE }

/** Resultado da consulta local à whitelist, resolvido pela camada de dados. */
enum class WhitelistLookup { HIT, MISS, LOOKUP_FAILED }

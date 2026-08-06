package org.sentinela.app.ui.history

import androidx.annotation.StringRes
import org.sentinela.app.R
import org.sentinela.app.domain.DecisionReason

/**
 * Tradução do que o histórico mostra.
 *
 * Existe para separar duas coisas que estavam grudadas na tela: o **reason code**, que é interno e
 * serve para diagnóstico, e o **texto** que o usuário lê. A versão anterior imprimia
 * `entry.reason.name` direto na lista, ou seja, mostrava `UNKNOWN_NUMBER` para o usuário — código
 * cru, em inglês, fora de `strings.xml` e impossível de traduzir.
 */
@StringRes
internal fun DecisionReason.rotulo(): Int = when (this) {
    DecisionReason.OUTGOING_CALL -> R.string.history_reason_outgoing_call
    DecisionReason.PROTECTION_DISABLED -> R.string.history_reason_protection_disabled
    DecisionReason.PRIVATE_NUMBER -> R.string.history_reason_private_number
    DecisionReason.CONTACT -> R.string.history_reason_contact
    DecisionReason.PERSONAL_WHITELIST -> R.string.history_reason_personal_whitelist
    DecisionReason.EMERGENCY_NUMBER -> R.string.history_reason_emergency_number
    DecisionReason.REPEATED_CALL -> R.string.history_reason_repeated_call
    DecisionReason.UNKNOWN_NUMBER -> R.string.history_reason_unknown_number
    DecisionReason.INVALID_NUMBER -> R.string.history_reason_invalid_number
    DecisionReason.LOCAL_LOOKUP_FAILURE -> R.string.history_reason_local_lookup_failure
    DecisionReason.FALLBACK_POLICY -> R.string.history_reason_fallback_policy
}

/**
 * Faixa de tempo decorrido desde o bloqueio, já resolvida em recurso + argumento.
 *
 * A tela antes mostrava a constante "Agora" em toda entrada, o que fazia um histórico de trinta
 * dias parecer ter acontecido inteiro no último minuto — justamente a informação que dá sentido a
 * auditar bloqueios. Relógio entra por parâmetro para o teste não depender da hora da máquina.
 */
internal data class TempoRelativo(@StringRes val recurso: Int, val quantidade: Int?)

internal fun tempoRelativo(timestampUtcMillis: Long, agoraUtcMillis: Long): TempoRelativo {
    val decorrido = (agoraUtcMillis - timestampUtcMillis).coerceAtLeast(0L)
    val minutos = decorrido / MINUTO_EM_MILLIS
    val horas = decorrido / HORA_EM_MILLIS
    val dias = decorrido / DIA_EM_MILLIS

    return when {
        minutos < 1 -> TempoRelativo(R.string.history_time_now, null)
        horas < 1 -> TempoRelativo(R.string.history_time_minutes, minutos.toInt())
        dias < 1 -> TempoRelativo(R.string.history_time_hours, horas.toInt())
        else -> TempoRelativo(R.string.history_time_days, dias.toInt())
    }
}

private const val MINUTO_EM_MILLIS = 60 * 1000L
private const val HORA_EM_MILLIS = 60 * MINUTO_EM_MILLIS
private const val DIA_EM_MILLIS = 24 * HORA_EM_MILLIS

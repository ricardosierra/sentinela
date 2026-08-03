package org.sentinela.app.telecom

import android.telecom.Call
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * Converte o objeto do Telecom na entrada pura do motor de decisao.
 *
 * Le exatamente dois membros de `Call.Details`: a direcao e o handle. A plataforma so garante
 * um punhado de campos durante a triagem, e todo o resto chega em valor padrao ou nulo — ler
 * qualquer outro membro aqui produziria uma decisao apoiada em dado inventado, que passa em
 * teste e erra no aparelho. Por isso a lista curta e proposital, nao esquecimento.
 *
 * A fabrica tambem nunca deixa excecao escapar: handle ausente, esquema inesperado ou
 * normalizacao impossivel viram uma entrada valida e conservadora para o motor, porque o Service
 * precisa responder ao sistema em qualquer cenario. Nenhum numero completo e registrado aqui —
 * a fabrica nao emite log.
 */
class ScreenedCallFactory(private val normalizer: PhoneNumberNormalizer) {

    fun from(details: Call.Details): ScreenedCall {
        val handle = details.handle?.schemeSpecificPart.orEmpty()
        val isEmergency = android.telephony.PhoneNumberUtils.isEmergencyNumber(handle)
        return ScreenedCall(
            direction = direction(details.callDirection),
            number = number(details),
            isEmergency = isEmergency,
        )
    }

    /** Direcao desconhecida cai em INCOMING: o lado seguro e o que faz a triagem acontecer. */
    private fun direction(raw: Int): CallDirection = when (raw) {
        Call.Details.DIRECTION_OUTGOING -> CallDirection.OUTGOING
        else -> CallDirection.INCOMING
    }

    private fun number(details: Call.Details): ScreenedNumber = runCatching {
        val handle = details.handle ?: return ScreenedNumber.Private
        if (handle.scheme != TEL_SCHEME) return ScreenedNumber.Invalid
        val raw = handle.schemeSpecificPart.orEmpty()
        if (raw.isBlank()) return ScreenedNumber.Invalid

        when (val resultado = normalizer.normalize(raw)) {
            is NormalizationResult.Valid -> ScreenedNumber.Valid(resultado.e164)
            is NormalizationResult.Invalid -> ScreenedNumber.Invalid
        }
    }.getOrElse { ScreenedNumber.Invalid }

    private companion object {
        const val TEL_SCHEME = "tel"
    }
}

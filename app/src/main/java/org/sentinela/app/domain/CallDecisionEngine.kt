package org.sentinela.app.domain

import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Motor de decisão puro e determinístico. Toda a regra de triagem vive aqui —
 * o Service apenas monta a entrada e traduz a saída.
 *
 * Precedência (docs/PROMPT-MVP.md §5):
 *  1. Chamada de saída → não interferir.
 *  2. Proteção desabilitada → permitir.
 *  3. Número privado/oculto → configuração específica (bloqueio por padrão).
 *  4. Whitelist pessoal → permitir.
 *  5. Falha de consulta local → política de fallback explícita.
 *  6. Número desconhecido/inválido → bloquear conforme configuração.
 */
class CallDecisionEngine {

    fun decide(
        call: ScreenedCall,
        settings: ScreeningSettings,
        whitelist: WhitelistLookup,
    ): CallDecision {
        if (call.direction == CallDirection.OUTGOING) {
            return CallDecision.Allow(DecisionReason.OUTGOING_CALL)
        }
        if (!settings.protectionEnabled) {
            return CallDecision.Allow(DecisionReason.PROTECTION_DISABLED)
        }
        if (call.number is ScreenedNumber.Private) {
            return if (settings.blockPrivateNumbers) {
                block(settings, DecisionReason.PRIVATE_NUMBER)
            } else {
                CallDecision.Allow(DecisionReason.PRIVATE_NUMBER)
            }
        }
        return when (whitelist) {
            WhitelistLookup.HIT -> CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST)
            WhitelistLookup.LOOKUP_FAILED -> fallback(settings)
            WhitelistLookup.MISS -> {
                val reason = if (call.number is ScreenedNumber.Invalid) {
                    DecisionReason.INVALID_NUMBER
                } else {
                    DecisionReason.UNKNOWN_NUMBER
                }
                if (settings.blockUnknownNumbers) block(settings, reason) else CallDecision.Allow(reason)
            }
        }
    }

    private fun block(settings: ScreeningSettings, reason: DecisionReason): CallDecision =
        when {
            settings.blockMode == BlockMode.SILENT_VOICEMAIL -> CallDecision.SendSilentlyToVoicemail(reason)
            settings.hideFromNativeCallLog -> CallDecision.BlockWithoutTrace(reason)
            else -> CallDecision.Reject(reason)
        }

    private fun fallback(settings: ScreeningSettings): CallDecision =
        when (settings.fallbackPolicy) {
            FallbackPolicy.ALLOW -> CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE)
            FallbackPolicy.BLOCK -> block(settings, DecisionReason.FALLBACK_POLICY)
        }
}

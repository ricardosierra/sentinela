package org.sentinela.app.domain

import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Motor de decisão puro e determinístico. Toda a regra de triagem vive aqui —
 * o Service apenas monta a entrada e traduz a saída.
 *
 * Precedência (docs/PROMPT-MVP.md §5 + adendos de 2026-07-28):
 *  1. Chamada de saída → não interferir.
 *  2. Proteção desabilitada → permitir.
 *  3. Número privado/oculto → configuração específica (bloqueio por padrão).
 *  4. Contato da agenda → política de contatos (tocar por padrão).
 *  5. Whitelist pessoal → política da whitelist (nunca silenciar por padrão).
 *  6. Falha de consulta local (contatos ou whitelist) → política de fallback.
 *  7. Número desconhecido/inválido → política de desconhecidos (bloquear por padrão).
 *
 * NEVER_SILENCE decide como RING; o bypass de Não Perturbe é responsabilidade
 * da camada telecom/notificações, não do motor.
 */
class CallDecisionEngine {

    fun decide(
        call: ScreenedCall,
        settings: ScreeningSettings,
        contact: ContactLookup,
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
        if (contact == ContactLookup.HIT) {
            return apply(settings.contactsPolicy, settings, DecisionReason.CONTACT)
        }
        if (whitelist == WhitelistLookup.HIT) {
            return apply(settings.whitelistPolicy, settings, DecisionReason.PERSONAL_WHITELIST)
        }
        if (contact == ContactLookup.UNAVAILABLE || whitelist == WhitelistLookup.LOOKUP_FAILED) {
            return fallback(settings)
        }
        val reason = if (call.number is ScreenedNumber.Invalid) {
            DecisionReason.INVALID_NUMBER
        } else {
            DecisionReason.UNKNOWN_NUMBER
        }
        return apply(settings.unknownPolicy, settings, reason)
    }

    private fun apply(policy: OriginPolicy, settings: ScreeningSettings, reason: DecisionReason): CallDecision =
        when (policy) {
            OriginPolicy.RING, OriginPolicy.NEVER_SILENCE -> CallDecision.Allow(reason)
            OriginPolicy.SILENCE -> CallDecision.Silence(reason)
            OriginPolicy.BLOCK -> block(settings, reason)
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

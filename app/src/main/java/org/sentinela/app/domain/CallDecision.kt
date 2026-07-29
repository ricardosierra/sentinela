package org.sentinela.app.domain

/**
 * Resultado de domínio da triagem. A tradução para
 * CallScreeningService.CallResponse acontece só na camada telecom.
 */
sealed interface CallDecision {
    val reason: DecisionReason

    /** Deixa a chamada seguir normalmente. */
    data class Allow(override val reason: DecisionReason) : CallDecision

    /** Toca sem som nem vibração (setSilenceCall); aparece na tela e no log. */
    data class Silence(override val reason: DecisionReason) : CallDecision

    /** Rejeita imediatamente (disallow + reject), sem tocar. */
    data class Reject(override val reason: DecisionReason) : CallDecision

    /** Encaminha em silêncio para a caixa postal, sem tocar nem vibrar. */
    data class SendSilentlyToVoicemail(override val reason: DecisionReason) : CallDecision

    /** Rejeita e suprime notificação nativa e histórico nativo. */
    data class BlockWithoutTrace(override val reason: DecisionReason) : CallDecision
}

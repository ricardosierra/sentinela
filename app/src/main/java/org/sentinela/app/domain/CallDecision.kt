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

/**
 * A decisão barrou a chamada, isto é, impediu o telefone de tocar?
 *
 * Serve ao trabalho posterior — histórico e notificação —, que só faz sentido quando houve
 * bloqueio de verdade. Silenciar não conta: a chamada aconteceu, apareceu na tela e o usuário
 * pôde atender. Vive no domínio de propósito, para que a camada da plataforma não precise
 * carregar uma condição própria sobre o destino de uma chamada.
 */
val CallDecision.blocksCall: Boolean
    get() = when (this) {
        is CallDecision.Allow, is CallDecision.Silence -> false
        is CallDecision.Reject,
        is CallDecision.SendSilentlyToVoicemail,
        is CallDecision.BlockWithoutTrace,
        -> true
    }

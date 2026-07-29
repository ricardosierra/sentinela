package org.sentinela.app.telecom

import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Casos compartilhados entre a classe de teste principal e o espelho no piso do minSdk.
 *
 * Vive fora das classes de teste de proposito: o Robolectric carrega cada SDK num classloader
 * proprio e uma classe de teste nao enxerga membros de outra classe de teste (medido —
 * `NoClassDefFoundError`). Um objeto neutro atravessa os dois sandboxes sem problema.
 */
object ResponseCases {

    val DECISOES: List<CallDecision> = listOf(
        CallDecision.Allow(DecisionReason.CONTACT),
        CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER),
        CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
        CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER),
        CallDecision.BlockWithoutTrace(DecisionReason.PRIVATE_NUMBER),
    )

    val CONFIGURACOES: List<ScreeningSettings> = buildList {
        for (esconder in listOf(true, false)) {
            for (notificacao in listOf(true, false)) {
                for (modo in BlockMode.entries) {
                    for (politica in OriginPolicy.entries) {
                        for (fallback in FallbackPolicy.entries) {
                            add(
                                ScreeningSettings(
                                    hideFromNativeCallLog = esconder,
                                    showOwnNotification = notificacao,
                                    blockMode = modo,
                                    unknownPolicy = politica,
                                    fallbackPolicy = fallback,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

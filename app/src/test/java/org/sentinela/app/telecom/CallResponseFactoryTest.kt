package org.sentinela.app.telecom

import android.telecom.CallScreeningService.CallResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Robolectric e obrigatorio: com `isReturnDefaultValues = true`, montar um `CallResponse` em JVM
 * pura devolveria `null` em cada metodo do Builder. Alem disso o construtor real e quem valida as
 * combinacoes proibidas — sem ele o teste nao provaria nada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallResponseFactoryTest {

    private val factory = CallResponseFactory()

    private fun response(decision: CallDecision, settings: ScreeningSettings = ScreeningSettings()) =
        factory.toResponse(decision, settings)

    private fun assertCampos(
        response: CallResponse,
        disallow: Boolean,
        reject: Boolean,
        silence: Boolean,
        skipCallLog: Boolean,
        skipNotification: Boolean,
    ) {
        assertEquals("disallowCall", disallow, response.disallowCall)
        assertEquals("rejectCall", reject, response.rejectCall)
        assertEquals("silenceCall", silence, response.silenceCall)
        assertEquals("skipCallLog", skipCallLog, response.skipCallLog)
        assertEquals("skipNotification", skipNotification, response.skipNotification)
    }

    @Test
    fun `Allow nao mexe em nenhum campo`() {
        assertCampos(
            response(CallDecision.Allow(DecisionReason.CONTACT)),
            disallow = false,
            reject = false,
            silence = false,
            skipCallLog = false,
            skipNotification = false,
        )
    }

    @Test
    fun `Silence usa somente silenceCall`() {
        assertCampos(
            response(CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER)),
            disallow = false,
            reject = false,
            silence = true,
            skipCallLog = false,
            skipNotification = false,
        )
    }

    @Test
    fun `Reject recusa e suprime a notificacao nativa`() {
        assertCampos(
            response(
                CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            ),
            disallow = true,
            reject = true,
            silence = false,
            skipCallLog = false,
            skipNotification = true,
        )
    }

    @Test
    fun `SendSilentlyToVoicemail usa a mesma combinacao de recusa`() {
        assertCampos(
            response(
                CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            ),
            disallow = true,
            reject = true,
            silence = false,
            skipCallLog = false,
            skipNotification = true,
        )
    }

    @Test
    fun `BlockWithoutTrace pede tambem o descarte do registro nativo`() {
        assertCampos(
            response(
                CallDecision.BlockWithoutTrace(DecisionReason.PRIVATE_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            ),
            disallow = true,
            reject = true,
            silence = false,
            skipCallLog = true,
            skipNotification = true,
        )
    }

    @Test
    fun `skipCallLog de Reject segue a configuracao do usuario`() {
        listOf(true, false).forEach { esconder ->
            val r = response(
                CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = esconder),
            )
            assertEquals("hideFromNativeCallLog=$esconder", esconder, r.skipCallLog)
        }
    }

    @Test
    fun `nenhuma combinacao de decisao e configuracao faz o construtor real lancar`() {
        TODAS_AS_DECISOES.forEach { decision ->
            TODAS_AS_CONFIGURACOES.forEach { settings ->
                val r = factory.toResponse(decision, settings)
                assertTrue("resposta nula para $decision", r != null)
            }
        }
    }

    @Test
    fun `nenhuma resposta combina recusa com silenciamento`() {
        TODAS_AS_DECISOES.forEach { decision ->
            TODAS_AS_CONFIGURACOES.forEach { settings ->
                val r = factory.toResponse(decision, settings)
                assertFalse(
                    "combinacao enganosa em $decision",
                    r.disallowCall && r.silenceCall,
                )
            }
        }
    }

    internal companion object {
        val TODAS_AS_DECISOES: List<CallDecision> = listOf(
            CallDecision.Allow(DecisionReason.CONTACT),
            CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER),
            CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
            CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER),
            CallDecision.BlockWithoutTrace(DecisionReason.PRIVATE_NUMBER),
        )

        val TODAS_AS_CONFIGURACOES: List<ScreeningSettings> = buildList {
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
}

/**
 * Espelho no piso do minSdk: a validacao do construtor de `CallResponse` e a semantica de
 * `setSilenceCall` mudaram entre versoes, e o app declara suporte a partir do Android 10.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CallResponseFactoryMinSdkTest {

    private val factory = CallResponseFactory()

    @Test
    fun `a tabela inteira continua valida no piso do minSdk`() {
        CallResponseFactoryTest.TODAS_AS_DECISOES.forEach { decision ->
            CallResponseFactoryTest.TODAS_AS_CONFIGURACOES.forEach { settings ->
                val r = factory.toResponse(decision, settings)
                assertFalse(r.disallowCall && r.silenceCall)
            }
        }
    }

    @Test
    fun `Silence no piso do minSdk usa somente silenceCall`() {
        val r = factory.toResponse(
            CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER),
            ScreeningSettings(),
        )
        assertTrue(r.silenceCall)
        assertFalse(r.disallowCall)
        assertFalse(r.rejectCall)
        assertFalse(r.skipNotification)
    }
}

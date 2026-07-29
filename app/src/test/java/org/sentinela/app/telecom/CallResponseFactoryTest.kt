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

    /** Ordem fixa: disallow, reject, silence, skipCallLog, skipNotification. */
    private fun campos(response: CallResponse): List<Boolean> = listOf(
        response.disallowCall,
        response.rejectCall,
        response.silenceCall,
        response.skipCallLog,
        response.skipNotification,
    )

    @Test
    fun `Allow nao mexe em nenhum campo`() {
        assertEquals(
            listOf(false, false, false, false, false),
            campos(response(CallDecision.Allow(DecisionReason.CONTACT))),
        )
    }

    @Test
    fun `Silence usa somente silenceCall`() {
        assertEquals(
            listOf(false, false, true, false, false),
            campos(response(CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER))),
        )
    }

    @Test
    fun `Reject recusa e suprime a notificacao nativa`() {
        assertEquals(
            listOf(true, true, false, false, true),
            campos(response(
                CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            )),
        )
    }

    @Test
    fun `SendSilentlyToVoicemail usa a mesma combinacao de recusa`() {
        assertEquals(
            listOf(true, true, false, false, true),
            campos(response(
                CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            )),
        )
    }

    @Test
    fun `BlockWithoutTrace pede tambem o descarte do registro nativo`() {
        assertEquals(
            listOf(true, true, false, true, true),
            campos(response(
                CallDecision.BlockWithoutTrace(DecisionReason.PRIVATE_NUMBER),
                ScreeningSettings(hideFromNativeCallLog = false),
            )),
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
        ResponseCases.DECISOES.forEach { decision ->
            ResponseCases.CONFIGURACOES.forEach { settings ->
                val r = factory.toResponse(decision, settings)
                assertTrue("resposta nula para $decision", r != null)
            }
        }
    }

    @Test
    fun `nenhuma resposta combina recusa com silenciamento`() {
        ResponseCases.DECISOES.forEach { decision ->
            ResponseCases.CONFIGURACOES.forEach { settings ->
                val r = factory.toResponse(decision, settings)
                assertFalse(
                    "combinacao enganosa em $decision",
                    r.disallowCall && r.silenceCall,
                )
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
        ResponseCases.DECISOES.forEach { decision ->
            ResponseCases.CONFIGURACOES.forEach { settings ->
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

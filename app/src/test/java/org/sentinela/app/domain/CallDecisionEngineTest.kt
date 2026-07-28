package org.sentinela.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Cobertura inicial da precedência do motor. A suíte completa exigida pela
 * seção 13 do docs/PROMPT-MVP.md é expandida na Fase 2.
 */
class CallDecisionEngineTest {

    private val engine = CallDecisionEngine()
    private val defaults = ScreeningSettings()

    private fun incoming(number: ScreenedNumber = ScreenedNumber.Valid("+5511912341234")) =
        ScreenedCall(CallDirection.INCOMING, number)

    @Test
    fun `chamada de saida nunca sofre interferencia`() {
        val decision = engine.decide(
            ScreenedCall(CallDirection.OUTGOING, ScreenedNumber.Valid("+5511912341234")),
            defaults,
            WhitelistLookup.MISS,
        )
        assertEquals(CallDecision.Allow(DecisionReason.OUTGOING_CALL), decision)
    }

    @Test
    fun `protecao desabilitada permite tudo`() {
        val decision = engine.decide(
            incoming(),
            defaults.copy(protectionEnabled = false),
            WhitelistLookup.MISS,
        )
        assertEquals(CallDecision.Allow(DecisionReason.PROTECTION_DISABLED), decision)
    }

    @Test
    fun `numero privado e bloqueado por padrao`() {
        val decision = engine.decide(incoming(ScreenedNumber.Private), defaults, WhitelistLookup.MISS)
        assertTrue(decision is CallDecision.BlockWithoutTrace)
        assertEquals(DecisionReason.PRIVATE_NUMBER, decision.reason)
    }

    @Test
    fun `whitelist pessoal permite`() {
        val decision = engine.decide(incoming(), defaults, WhitelistLookup.HIT)
        assertEquals(CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    @Test
    fun `numero desconhecido e bloqueado sem rastro por padrao`() {
        val decision = engine.decide(incoming(), defaults, WhitelistLookup.MISS)
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `desconhecido com historico nativo visivel vira reject simples`() {
        val decision = engine.decide(
            incoming(),
            defaults.copy(hideFromNativeCallLog = false),
            WhitelistLookup.MISS,
        )
        assertEquals(CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `modo silencioso encaminha para caixa postal`() {
        val decision = engine.decide(
            incoming(),
            defaults.copy(blockMode = BlockMode.SILENT_VOICEMAIL),
            WhitelistLookup.MISS,
        )
        assertEquals(CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `numero invalido usa reason proprio`() {
        val decision = engine.decide(incoming(ScreenedNumber.Invalid), defaults, WhitelistLookup.MISS)
        assertEquals(DecisionReason.INVALID_NUMBER, decision.reason)
    }

    @Test
    fun `falha de consulta local aplica fallback allow`() {
        val decision = engine.decide(incoming(), defaults, WhitelistLookup.LOOKUP_FAILED)
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), decision)
    }

    @Test
    fun `falha de consulta com fallback block bloqueia`() {
        val decision = engine.decide(
            incoming(),
            defaults.copy(fallbackPolicy = FallbackPolicy.BLOCK),
            WhitelistLookup.LOOKUP_FAILED,
        )
        assertTrue(decision is CallDecision.BlockWithoutTrace)
        assertEquals(DecisionReason.FALLBACK_POLICY, decision.reason)
    }
}

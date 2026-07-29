package org.sentinela.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Cobertura da precedência do motor: saída → proteção → privado → contato →
 * whitelist → falha de consulta → desconhecido. A suíte completa exigida pela
 * seção 13 do docs/PROMPT-MVP.md é expandida na Fase 2.
 */
class CallDecisionEngineTest {

    private val engine = CallDecisionEngine()
    private val defaults = ScreeningSettings()

    private fun incoming(number: ScreenedNumber = ScreenedNumber.Valid("+5511912341234")) =
        ScreenedCall(CallDirection.INCOMING, number)

    private fun decide(
        call: ScreenedCall = incoming(),
        settings: ScreeningSettings = defaults,
        contact: ContactLookup = ContactLookup.MISS,
        whitelist: WhitelistLookup = WhitelistLookup.MISS,
    ) = engine.decide(call, settings, contact, whitelist)

    // 1. Saída

    @Test
    fun `chamada de saida nunca sofre interferencia`() {
        val decision = decide(ScreenedCall(CallDirection.OUTGOING, ScreenedNumber.Valid("+5511912341234")))
        assertEquals(CallDecision.Allow(DecisionReason.OUTGOING_CALL), decision)
    }

    // 2. Proteção

    @Test
    fun `protecao desabilitada permite tudo`() {
        val decision = decide(settings = defaults.copy(protectionEnabled = false))
        assertEquals(CallDecision.Allow(DecisionReason.PROTECTION_DISABLED), decision)
    }

    // 3. Privado

    @Test
    fun `numero privado e bloqueado por padrao`() {
        val decision = decide(incoming(ScreenedNumber.Private))
        assertTrue(decision is CallDecision.BlockWithoutTrace)
        assertEquals(DecisionReason.PRIVATE_NUMBER, decision.reason)
    }

    @Test
    fun `numero privado permitido quando configurado`() {
        val decision = decide(incoming(ScreenedNumber.Private), defaults.copy(blockPrivateNumbers = false))
        assertEquals(CallDecision.Allow(DecisionReason.PRIVATE_NUMBER), decision)
    }

    // 4. Contato da agenda

    @Test
    fun `contato toca por padrao`() {
        val decision = decide(contact = ContactLookup.HIT)
        assertEquals(CallDecision.Allow(DecisionReason.CONTACT), decision)
    }

    @Test
    fun `contato com politica bloquear e bloqueado`() {
        val decision = decide(
            settings = defaults.copy(contactsPolicy = OriginPolicy.BLOCK),
            contact = ContactLookup.HIT,
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.CONTACT), decision)
    }

    @Test
    fun `contato com politica silenciar toca em silencio`() {
        val decision = decide(
            settings = defaults.copy(contactsPolicy = OriginPolicy.SILENCE),
            contact = ContactLookup.HIT,
        )
        assertEquals(CallDecision.Silence(DecisionReason.CONTACT), decision)
    }

    @Test
    fun `contato com politica nunca silenciar toca`() {
        val decision = decide(
            settings = defaults.copy(contactsPolicy = OriginPolicy.NEVER_SILENCE),
            contact = ContactLookup.HIT,
        )
        assertEquals(CallDecision.Allow(DecisionReason.CONTACT), decision)
    }

    @Test
    fun `contato tem precedencia sobre whitelist`() {
        val decision = decide(
            settings = defaults.copy(whitelistPolicy = OriginPolicy.BLOCK),
            contact = ContactLookup.HIT,
            whitelist = WhitelistLookup.HIT,
        )
        assertEquals(CallDecision.Allow(DecisionReason.CONTACT), decision)
    }

    // 5. Whitelist pessoal

    @Test
    fun `whitelist permite por padrao (nunca silenciar)`() {
        val decision = decide(whitelist = WhitelistLookup.HIT)
        assertEquals(CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    @Test
    fun `whitelist com politica silenciar toca em silencio`() {
        val decision = decide(
            settings = defaults.copy(whitelistPolicy = OriginPolicy.SILENCE),
            whitelist = WhitelistLookup.HIT,
        )
        assertEquals(CallDecision.Silence(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    @Test
    fun `whitelist com politica tocar e permitida`() {
        val decision = decide(
            settings = defaults.copy(whitelistPolicy = OriginPolicy.RING),
            whitelist = WhitelistLookup.HIT,
        )
        assertEquals(CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    @Test
    fun `whitelist com politica bloquear e bloqueada sem rastro`() {
        val decision = decide(
            settings = defaults.copy(whitelistPolicy = OriginPolicy.BLOCK),
            whitelist = WhitelistLookup.HIT,
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    // 6. Falha de consulta local

    @Test
    fun `falha na whitelist aplica fallback allow`() {
        val decision = decide(whitelist = WhitelistLookup.LOOKUP_FAILED)
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), decision)
    }

    @Test
    fun `contatos indisponiveis aplicam fallback allow`() {
        val decision = decide(contact = ContactLookup.UNAVAILABLE)
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), decision)
    }

    @Test
    fun `fallback block bloqueia na falha de consulta`() {
        val decision = decide(
            settings = defaults.copy(fallbackPolicy = FallbackPolicy.BLOCK),
            whitelist = WhitelistLookup.LOOKUP_FAILED,
        )
        assertTrue(decision is CallDecision.BlockWithoutTrace)
        assertEquals(DecisionReason.FALLBACK_POLICY, decision.reason)
    }

    @Test
    fun `whitelist hit vale mesmo com contatos indisponiveis`() {
        val decision = decide(contact = ContactLookup.UNAVAILABLE, whitelist = WhitelistLookup.HIT)
        assertEquals(CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST), decision)
    }

    // 7. Desconhecido / inválido

    @Test
    fun `desconhecido e bloqueado sem rastro por padrao`() {
        val decision = decide()
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `desconhecido com historico nativo visivel vira reject simples`() {
        val decision = decide(settings = defaults.copy(hideFromNativeCallLog = false))
        assertEquals(CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `modo caixa postal encaminha silenciosamente`() {
        val decision = decide(settings = defaults.copy(blockMode = BlockMode.SILENT_VOICEMAIL))
        assertEquals(CallDecision.SendSilentlyToVoicemail(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `desconhecido com politica silenciar toca em silencio`() {
        val decision = decide(settings = defaults.copy(unknownPolicy = OriginPolicy.SILENCE))
        assertEquals(CallDecision.Silence(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `desconhecido com politica tocar e permitido`() {
        val decision = decide(settings = defaults.copy(unknownPolicy = OriginPolicy.RING))
        assertEquals(CallDecision.Allow(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `desconhecido com politica nunca silenciar toca`() {
        val decision = decide(settings = defaults.copy(unknownPolicy = OriginPolicy.NEVER_SILENCE))
        assertEquals(CallDecision.Allow(DecisionReason.UNKNOWN_NUMBER), decision)
    }

    @Test
    fun `numero invalido usa reason proprio`() {
        val decision = decide(incoming(ScreenedNumber.Invalid))
        assertEquals(DecisionReason.INVALID_NUMBER, decision.reason)
    }
}

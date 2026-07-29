package org.sentinela.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/** Origem da chamada do ponto de vista do motor — define qual política é aplicada. */
enum class CallOrigin { CONTACT, WHITELIST, UNKNOWN }

private const val TEST_NUMBER = "+5511912341234"

private fun incoming(number: ScreenedNumber = ScreenedNumber.Valid(TEST_NUMBER)) =
    ScreenedCall(CallDirection.INCOMING, number)

/**
 * Tabela esperada escrita à mão (NÃO derivada do motor): política × modo de
 * bloqueio × ocultação do histórico nativo → tipo de decisão.
 */
private fun expectedDecision(
    policy: OriginPolicy,
    blockMode: BlockMode,
    hideLog: Boolean,
    reason: DecisionReason,
): CallDecision = when (policy) {
    OriginPolicy.RING -> CallDecision.Allow(reason)
    OriginPolicy.NEVER_SILENCE -> CallDecision.Allow(reason)
    OriginPolicy.SILENCE -> CallDecision.Silence(reason)
    OriginPolicy.BLOCK -> when {
        blockMode == BlockMode.SILENT_VOICEMAIL -> CallDecision.SendSilentlyToVoicemail(reason)
        hideLog -> CallDecision.BlockWithoutTrace(reason)
        else -> CallDecision.Reject(reason)
    }
}

private fun settingsFor(
    origin: CallOrigin,
    policy: OriginPolicy,
    blockMode: BlockMode,
    hideLog: Boolean,
): ScreeningSettings {
    val base = ScreeningSettings(blockMode = blockMode, hideFromNativeCallLog = hideLog)
    return when (origin) {
        CallOrigin.CONTACT -> base.copy(contactsPolicy = policy)
        CallOrigin.WHITELIST -> base.copy(whitelistPolicy = policy)
        CallOrigin.UNKNOWN -> base.copy(unknownPolicy = policy)
    }
}

private fun contactLookupFor(origin: CallOrigin) =
    if (origin == CallOrigin.CONTACT) ContactLookup.HIT else ContactLookup.MISS

private fun whitelistLookupFor(origin: CallOrigin) =
    if (origin == CallOrigin.WHITELIST) WhitelistLookup.HIT else WhitelistLookup.MISS

private fun reasonFor(origin: CallOrigin) = when (origin) {
    CallOrigin.CONTACT -> DecisionReason.CONTACT
    CallOrigin.WHITELIST -> DecisionReason.PERSONAL_WHITELIST
    CallOrigin.UNKNOWN -> DecisionReason.UNKNOWN_NUMBER
}

/**
 * Matriz completa origem × política × modo de bloqueio × ocultação do histórico
 * (3 × 4 × 2 × 2 = 48 casos). Prova que nenhuma combinação fica sem cobertura.
 */
@RunWith(Parameterized::class)
class DecisionMatrixTest(
    private val origin: CallOrigin,
    private val policy: OriginPolicy,
    private val blockMode: BlockMode,
    private val hideLog: Boolean,
) {

    @Test
    fun `combinacao da matriz produz a decisao esperada`() {
        val decision = CallDecisionEngine().decide(
            call = incoming(),
            settings = settingsFor(origin, policy, blockMode, hideLog),
            contact = contactLookupFor(origin),
            whitelist = whitelistLookupFor(origin),
        )
        val reason = reasonFor(origin)
        assertEquals(expectedDecision(policy, blockMode, hideLog, reason), decision)
        assertEquals(reason, decision.reason)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} + {1} + {2} + hideLog={3}")
        fun cases(): List<Array<Any>> = CallOrigin.entries.flatMap { origin ->
            OriginPolicy.entries.flatMap { policy ->
                BlockMode.entries.flatMap { blockMode ->
                    listOf(true, false).map { hideLog ->
                        arrayOf<Any>(origin, policy, blockMode, hideLog)
                    }
                }
            }
        }
    }
}

/**
 * Casos que não pertencem à matriz parametrizada: número inválido, número
 * privado e os dois gatilhos de fallback por falha de consulta local.
 */
class DecisionEdgeCasesTest {

    private val engine = CallDecisionEngine()

    private fun decide(
        call: ScreenedCall = incoming(),
        settings: ScreeningSettings = ScreeningSettings(),
        contact: ContactLookup = ContactLookup.MISS,
        whitelist: WhitelistLookup = WhitelistLookup.MISS,
    ) = engine.decide(call, settings, contact, whitelist)

    // Número inválido — reason próprio nas 4 políticas de desconhecido.

    @Test
    fun `numero invalido com politica tocar e permitido`() {
        val decision = decide(
            incoming(ScreenedNumber.Invalid),
            ScreeningSettings(unknownPolicy = OriginPolicy.RING),
        )
        assertEquals(CallDecision.Allow(DecisionReason.INVALID_NUMBER), decision)
    }

    @Test
    fun `numero invalido com politica nunca silenciar e permitido`() {
        val decision = decide(
            incoming(ScreenedNumber.Invalid),
            ScreeningSettings(unknownPolicy = OriginPolicy.NEVER_SILENCE),
        )
        assertEquals(CallDecision.Allow(DecisionReason.INVALID_NUMBER), decision)
    }

    @Test
    fun `numero invalido com politica silenciar toca em silencio`() {
        val decision = decide(
            incoming(ScreenedNumber.Invalid),
            ScreeningSettings(unknownPolicy = OriginPolicy.SILENCE),
        )
        assertEquals(CallDecision.Silence(DecisionReason.INVALID_NUMBER), decision)
    }

    @Test
    fun `numero invalido com politica bloquear e bloqueado sem rastro`() {
        val decision = decide(
            incoming(ScreenedNumber.Invalid),
            ScreeningSettings(unknownPolicy = OriginPolicy.BLOCK),
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.INVALID_NUMBER), decision)
    }

    // Privado — mesma sub-matriz de bloqueio, reason PRIVATE_NUMBER.

    @Test
    fun `privado rejeitado sem rastro quando o historico nativo e oculto`() {
        val decision = decide(
            incoming(ScreenedNumber.Private),
            ScreeningSettings(blockMode = BlockMode.REJECT, hideFromNativeCallLog = true),
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.PRIVATE_NUMBER), decision)
    }

    @Test
    fun `privado vira reject simples quando o historico nativo e visivel`() {
        val decision = decide(
            incoming(ScreenedNumber.Private),
            ScreeningSettings(blockMode = BlockMode.REJECT, hideFromNativeCallLog = false),
        )
        assertEquals(CallDecision.Reject(DecisionReason.PRIVATE_NUMBER), decision)
    }

    @Test
    fun `privado vai para caixa postal mesmo com historico nativo oculto`() {
        val decision = decide(
            incoming(ScreenedNumber.Private),
            ScreeningSettings(blockMode = BlockMode.SILENT_VOICEMAIL, hideFromNativeCallLog = true),
        )
        assertEquals(CallDecision.SendSilentlyToVoicemail(DecisionReason.PRIVATE_NUMBER), decision)
    }

    @Test
    fun `privado vai para caixa postal com historico nativo visivel`() {
        val decision = decide(
            incoming(ScreenedNumber.Private),
            ScreeningSettings(blockMode = BlockMode.SILENT_VOICEMAIL, hideFromNativeCallLog = false),
        )
        assertEquals(CallDecision.SendSilentlyToVoicemail(DecisionReason.PRIVATE_NUMBER), decision)
    }

    @Test
    fun `privado permitido quando o bloqueio de privados esta desligado`() {
        val decision = decide(
            incoming(ScreenedNumber.Private),
            ScreeningSettings(blockPrivateNumbers = false),
        )
        assertEquals(CallDecision.Allow(DecisionReason.PRIVATE_NUMBER), decision)
    }

    // Fallback — 2 gatilhos × 2 políticas.

    @Test
    fun `contatos indisponiveis com fallback allow permitem a chamada`() {
        val decision = decide(
            settings = ScreeningSettings(fallbackPolicy = FallbackPolicy.ALLOW),
            contact = ContactLookup.UNAVAILABLE,
        )
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), decision)
    }

    @Test
    fun `contatos indisponiveis com fallback block bloqueiam por politica`() {
        val decision = decide(
            settings = ScreeningSettings(fallbackPolicy = FallbackPolicy.BLOCK),
            contact = ContactLookup.UNAVAILABLE,
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.FALLBACK_POLICY), decision)
    }

    @Test
    fun `falha da whitelist com fallback allow permite a chamada`() {
        val decision = decide(
            settings = ScreeningSettings(fallbackPolicy = FallbackPolicy.ALLOW),
            whitelist = WhitelistLookup.LOOKUP_FAILED,
        )
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), decision)
    }

    @Test
    fun `falha da whitelist com fallback block bloqueia por politica`() {
        val decision = decide(
            settings = ScreeningSettings(fallbackPolicy = FallbackPolicy.BLOCK),
            whitelist = WhitelistLookup.LOOKUP_FAILED,
        )
        assertEquals(CallDecision.BlockWithoutTrace(DecisionReason.FALLBACK_POLICY), decision)
    }
}

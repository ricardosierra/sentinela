package org.sentinela.app.telecom

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Caminho feliz do orquestrador puro da triagem.
 *
 * O invariante que estas classes protegem é o de resposta única: a pesquisa da fase
 * mediu que responder duas vezes ao sistema não lança e não derruba o processo —
 * apenas emite dois IPCs em silêncio. Portanto a contagem de invocações da costura
 * de resposta é sempre afirmada, nunca só o conteúdo da decisão.
 */
class ScreeningCoordinatorTest {

    private val settings = FakeSettingsRepository()
    private val contatos = FakeContactLookupRepository()
    private val whitelist = FakePersonalWhitelistRepository()
    private val historico = FakeBlockedCallRepository()
    private val resposta = RespostaGravada()

    private fun coordenador(prazoMillis: Long = SCREENING_TIMEOUT_MILLIS) = ScreeningCoordinator(
        settings = settings,
        contacts = contatos,
        whitelist = whitelist,
        blockedCalls = historico,
        engine = CallDecisionEngine(),
        clock = { AGORA },
        timeoutMillis = prazoMillis,
    )

    @Test
    fun `numero desconhecido com politica de bloqueio recebe uma unica recusa`() = runTest {
        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertTrue(resposta.unica is CallDecision.BlockWithoutTrace)
        assertEquals(DecisionReason.UNKNOWN_NUMBER, resposta.unica.reason)
    }

    @Test
    fun `chamada de contato da agenda continua tocando`() = runTest {
        // Com READ_CONTACTS concedida na Fase 4 a plataforma passou a entregar contatos
        // ao aplicativo. Um resultado "ausente" errado aqui bloquearia a ligação de um
        // contato — por isso este caminho tem teste próprio e nomeado.
        contatos.resultado = ContactLookup.HIT
        settings.valor = ScreeningSettings(contactsPolicy = OriginPolicy.RING)

        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.CONTACT), resposta.unica)
    }

    @Test
    fun `chamada de saida nao produz resposta nem consulta local`() = runTest {
        coordenador().screen(
            ScreenedCall(CallDirection.OUTGOING, ScreenedNumber.Valid(NUMERO)),
            resposta.costura(),
        )

        assertEquals(0, resposta.total)
        assertEquals(0, settings.chamadas)
        assertEquals(0, contatos.chamadas)
        assertEquals(0, whitelist.chamadas)
        assertEquals(0, historico.chamadas)
    }

    @Test
    fun `numero na whitelist pessoal e permitido`() = runTest {
        whitelist.presente = true

        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.PERSONAL_WHITELIST), resposta.unica)
    }

    @Test
    fun `segunda chamada do mesmo numero dentro da janela toca`() = runTest {
        historico.resultado = RepeatedCallLookup.HIT

        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.REPEATED_CALL), resposta.unica)
        assertEquals(AGORA, historico.ultimoAgora)
    }

    @Test
    fun `estouro do prazo interno responde uma vez pela politica de fallback`() = runTest {
        contatos.atrasoMillis = ATRASO_LONGO

        coordenador(prazoMillis = 1L).screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), resposta.unica)
    }

    @Test
    fun `a rede permissiva do bloco final nunca produz uma segunda resposta`() = runTest {
        // O coordenador tenta emitir de novo ao final de todo caminho; a guarda atômica
        // é a única coisa que impede a segunda emissão de virar um segundo IPC.
        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
    }

    @Test
    fun `protecao desligada permite a chamada`() = runTest {
        settings.valor = ScreeningSettings(protectionEnabled = false)

        coordenador().screen(entrada(), resposta.costura())

        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.PROTECTION_DISABLED), resposta.unica)
    }

    @Test
    fun `numero sem identificacao nao consulta o historico de bloqueios`() = runTest {
        coordenador().screen(
            ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Private),
            resposta.costura(),
        )

        assertEquals(1, resposta.total)
        assertEquals(DecisionReason.PRIVATE_NUMBER, resposta.unica.reason)
        assertEquals(0, historico.chamadas)
        assertEquals(0, contatos.chamadas)
    }

    @Test
    fun `o trabalho pos-resposta recebe a mesma chamada e a mesma decisao`() = runTest {
        var recebido: Pair<ScreenedCall, CallDecision>? = null
        val chamada = entrada()

        coordenador().screen(chamada, resposta.costura()) { c, d -> recebido = c to d }

        assertEquals(chamada to resposta.unica, recebido)
    }

    private fun entrada() = ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Valid(NUMERO))

    private companion object {
        const val NUMERO = "+5511999998888"
        const val AGORA = 1_700_000_000_000L
        const val ATRASO_LONGO = 50L
    }
}

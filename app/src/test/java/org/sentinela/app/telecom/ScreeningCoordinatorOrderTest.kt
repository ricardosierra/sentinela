package org.sentinela.app.telecom

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber

/**
 * Ordem inegociável: responder ao sistema primeiro, registrar depois.
 *
 * Cronômetro não prova estrutura. É a lição acumulada das Fases 3 e 4 — lá, um teste de
 * tempo continuou verde depois de o índice do banco e o cache de contatos serem removidos,
 * e a mesma medição variou de 30 ms para 140 ms sem uma linha de código mudar. Um teste que
 * afirmasse "a resposta veio antes porque foi mais rápida" mediria o escalonador da máquina,
 * não o desenho do coordenador.
 *
 * Por isso a ordem aqui é provada por POSIÇÃO numa lista de eventos: a costura de resposta e
 * o trabalho posterior anotam sua passagem na mesma lista, e a asserção é sobre os índices.
 * Se alguém inverter as duas chamadas no coordenador, o índice muda e o teste fica vermelho —
 * independentemente de quanto cada etapa demore.
 */
class ScreeningCoordinatorOrderTest {

    private val settings = FakeSettingsRepository()
    private val contatos = FakeContactLookupRepository()
    private val whitelist = FakePersonalWhitelistRepository()
    private val historico = FakeBlockedCallRepository()

    private fun coordenador() = ScreeningCoordinator(
        settings = settings,
        contacts = contatos,
        whitelist = whitelist,
        blockedCalls = historico,
        engine = CallDecisionEngine(),
        clock = { AGORA },
    )

    @Test
    fun `a resposta ao sistema aparece antes do trabalho posterior`() = runTest {
        val eventos = mutableListOf<String>()

        coordenador().screen(
            entrada(),
            { eventos += RESPOSTA },
            { _, _ -> eventos += POSTERIOR },
        )

        assertEquals(listOf(RESPOSTA, POSTERIOR), eventos)
        assertTrue(eventos.indexOf(RESPOSTA) < eventos.indexOf(POSTERIOR))
    }

    @Test
    fun `notificacao e historico so entram na lista depois da resposta`() = runTest {
        val eventos = mutableListOf<String>()

        coordenador().screen(
            entrada(),
            { eventos += RESPOSTA },
            { _, _ ->
                eventos += NOTIFICACAO
                eventos += HISTORICO
            },
        )

        assertEquals(0, eventos.indexOf(RESPOSTA))
        assertTrue(eventos.indexOf(RESPOSTA) < eventos.indexOf(NOTIFICACAO))
        assertTrue(eventos.indexOf(RESPOSTA) < eventos.indexOf(HISTORICO))
    }

    @Test
    fun `trabalho posterior demorado nao atrasa a resposta`() = runTest {
        val eventos = mutableListOf<String>()

        coordenador().screen(
            entrada(),
            { eventos += RESPOSTA },
            { _, _ ->
                delay(DEMORA_LONGA)
                eventos += POSTERIOR
            },
        )

        // A resposta já estava na lista antes de o trabalho posterior começar a suspender:
        // a prova é a posição, não a duração.
        assertEquals(0, eventos.indexOf(RESPOSTA))
        assertEquals(1, eventos.indexOf(POSTERIOR))
    }

    @Test
    fun `trabalho posterior que falha nao muda a contagem de respostas nem propaga`() = runTest {
        val eventos = mutableListOf<String>()

        val execucao = runCatching {
            coordenador().screen(
                entrada(),
                { eventos += RESPOSTA },
                { _, _ -> error("falha injetada no trabalho posterior") },
            )
        }

        assertTrue(execucao.isSuccess)
        assertEquals(listOf(RESPOSTA), eventos)
    }

    @Test
    fun `o trabalho posterior recebe exatamente a decisao que foi respondida`() = runTest {
        val respondidas = mutableListOf<CallDecision>()
        val recebidas = mutableListOf<CallDecision>()

        coordenador().screen(
            entrada(),
            { respondidas += it },
            { _, decisao -> recebidas += decisao },
        )

        assertEquals(respondidas, recebidas)
        assertEquals(1, respondidas.size)
    }

    @Test
    fun `chamada de saida nao gera evento nenhum`() = runTest {
        val eventos = mutableListOf<String>()

        coordenador().screen(
            ScreenedCall(CallDirection.OUTGOING, ScreenedNumber.Valid(NUMERO)),
            { eventos += RESPOSTA },
            { _, _ -> eventos += POSTERIOR },
        )

        assertEquals(emptyList<String>(), eventos)
    }

    private fun entrada() = ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Valid(NUMERO))

    private companion object {
        const val NUMERO = "+5511999998888"
        const val AGORA = 1_700_000_000_000L
        const val DEMORA_LONGA = 500L
        const val RESPOSTA = "resposta"
        const val POSTERIOR = "pos"
        const val NOTIFICACAO = "notificacao"
        const val HISTORICO = "historico"
    }
}

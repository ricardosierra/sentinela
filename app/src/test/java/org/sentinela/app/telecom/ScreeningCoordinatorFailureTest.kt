package org.sentinela.app.telecom

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber

/**
 * Matriz de injeção de defeito: um ponto de falha por vez, em CADA etapa do caminho
 * de triagem, mais duas combinações.
 *
 * O risco desta fase é regressão silenciosa — um caminho que não responde, ou que
 * responde duas vezes, não aparece em teste de caminho feliz. Por isso todo teste aqui
 * afirma DUAS coisas: a contagem exata de invocações da costura de resposta e o tipo da
 * decisão emitida. Contar sem afirmar o tipo deixaria passar um bloqueio causado por
 * defeito, que é exatamente o pior resultado possível para o produto.
 */
class ScreeningCoordinatorFailureTest {

    private val settings = FakeSettingsRepository()
    private val contatos = FakeContactLookupRepository()
    private val whitelist = FakePersonalWhitelistRepository()
    private val historico = FakeBlockedCallRepository()
    private val motor = ExplodingDecisionEngine()
    private val resposta = RespostaGravada()

    private fun coordenador(prazoMillis: Long = SCREENING_TIMEOUT_MILLIS) = ScreeningCoordinator(
        settings = settings,
        contacts = contatos,
        whitelist = whitelist,
        blockedCalls = historico,
        engine = motor,
        clock = { AGORA },
        timeoutMillis = prazoMillis,
    )

    @Test
    fun `falha ao ler as configuracoes deixa a chamada passar`() = runTest {
        settings.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `falha na consulta a agenda deixa a chamada passar`() = runTest {
        contatos.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `falha na consulta a whitelist deixa a chamada passar`() = runTest {
        whitelist.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `falha na consulta de bloqueio recente deixa a chamada passar`() = runTest {
        historico.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `falha do motor de decisao deixa a chamada passar`() = runTest {
        motor.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `dois pontos falhando ao mesmo tempo continuam produzindo uma unica resposta`() = runTest {
        settings.falha = true
        contatos.falha = true
        triar()
        assertPermissivaUnica()
    }

    @Test
    fun `falha da propria costura de resposta nao gera segunda invocacao`() = runTest {
        // A costura registra a decisão e só então explode: a guarda já marcou a triagem
        // como respondida, então nem a rede permissiva do bloco final tenta de novo.
        resposta.falharNaPrimeira = true

        val execucao = runCatching { triar() }

        assertTrue(execucao.isSuccess)
        assertEquals(1, resposta.total)
        assertTrue(resposta.unica is CallDecision.BlockWithoutTrace)
    }

    @Test
    fun `falha do trabalho pos-resposta nao altera a resposta ja emitida`() = runTest {
        val execucao = runCatching {
            coordenador().screen(entrada(), resposta.costura()) { _, _ ->
                error("falha injetada no trabalho pos-resposta")
            }
        }

        assertTrue(execucao.isSuccess)
        assertEquals(1, resposta.total)
        assertTrue(resposta.unica is CallDecision.BlockWithoutTrace)
    }

    @Test
    fun `consulta mais lenta que o prazo interno responde uma vez pela politica de reserva`() = runTest {
        whitelist.atrasoMillis = ATRASO_LONGO

        coordenador(prazoMillis = PRAZO_CURTO).screen(entrada(), resposta.costura())

        assertPermissivaUnica()
    }

    @Test
    fun `com o prazo estourado o trabalho pos-resposta roda no maximo uma vez`() = runTest {
        contatos.atrasoMillis = ATRASO_LONGO
        var posteriores = 0

        coordenador(prazoMillis = PRAZO_CURTO).screen(entrada(), resposta.costura()) { _, _ ->
            posteriores++
        }

        assertEquals(1, resposta.total)
        assertEquals(1, posteriores)
    }

    @Test
    fun `nenhum cenario de defeito propaga excecao para fora da triagem`() = runTest {
        val cenarios: List<Pair<String, () -> Unit>> = listOf(
            "configuracoes" to { settings.falha = true },
            "agenda" to { contatos.falha = true },
            "whitelist" to { whitelist.falha = true },
            "historico" to { historico.falha = true },
            "motor" to { motor.falha = true },
            "costura" to { resposta.falharNaPrimeira = true },
            "agenda lenta" to { contatos.atrasoMillis = ATRASO_LONGO },
            "whitelist lenta" to { whitelist.atrasoMillis = ATRASO_LONGO },
            "configuracoes lentas" to { settings.atrasoMillis = ATRASO_LONGO },
            "tudo junto" to {
                settings.falha = true
                contatos.falha = true
                whitelist.falha = true
                historico.falha = true
                motor.falha = true
            },
        )

        cenarios.forEach { (nome, armar) ->
            limpar()
            armar()
            val gravador = RespostaGravada()
            gravador.falharNaPrimeira = resposta.falharNaPrimeira

            val execucao = runCatching {
                coordenador(prazoMillis = PRAZO_CURTO).screen(entrada(), gravador.costura())
            }

            assertTrue("cenario $nome propagou excecao", execucao.isSuccess)
            assertEquals("cenario $nome respondeu fora da conta", 1, gravador.total)
        }
    }

    private suspend fun triar() = coordenador().screen(entrada(), resposta.costura())

    private fun assertPermissivaUnica() {
        assertEquals(1, resposta.total)
        assertEquals(CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE), resposta.unica)
    }

    private fun limpar() {
        settings.falha = false
        settings.atrasoMillis = 0
        contatos.falha = false
        contatos.atrasoMillis = 0
        whitelist.falha = false
        whitelist.atrasoMillis = 0
        historico.falha = false
        historico.atrasoMillis = 0
        motor.falha = false
        resposta.falharNaPrimeira = false
    }

    private fun entrada() = ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Valid(NUMERO))

    private companion object {
        const val NUMERO = "+5511999998888"
        const val AGORA = 1_700_000_000_000L
        const val ATRASO_LONGO = 50L
        const val PRAZO_CURTO = 1L
    }
}

package org.sentinela.app.ui.call

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.telecom.call.CallOrigin
import org.sentinela.app.telecom.call.CallSessionCoordinator
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Semantica estrutural das telas de chamada: alvo de toque, descricao anunciada e ordem de foco.
 *
 * Robolectric e obrigatorio porque a arvore de semantica so existe com uma composicao de verdade —
 * e o alvo desta suite e justamente o que o leitor de tela e o dedo do usuario alcancam, nao a
 * intencao do codigo.
 *
 * Nenhum caso mede cronometro. Duracao muda a cada segundo e um assert sobre ela mediria o relogio
 * do computador de teste; o que precisa ficar travado aqui e estrutura.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR")
class CallScreenSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    /** Alvo minimo de qualquer controle, do contrato de acessibilidade da fase. */
    private val alvoMinimo = 48.dp

    private val contato = CallIdentity(
        displayName = "Ana Paula Souza",
        fullNumber = "+5511912345678",
        origin = CallOrigin.CONTATO,
    )

    private val retratoAtivo = CallSnapshot(
        state = CallUiState.Active,
        identity = contato,
        startedAtMillis = 0L,
    )

    private fun conteudoRecebida() {
        compose.setContent {
            IncomingCallScreen(identity = contato, onAnswer = {}, onReject = {})
        }
    }

    private fun conteudoAtiva(snapshot: CallSnapshot = retratoAtivo) {
        compose.setContent {
            ActiveCallScreen(
                snapshot = snapshot,
                actions = CallScreenActions(onHangUp = {}),
                now = { 0L },
            )
        }
    }

    @Test
    fun `atender tem alvo de toque acima do minimo`() {
        conteudoRecebida()

        compose.onNodeWithContentDescription("Atender chamada")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `recusar tem alvo de toque acima do minimo`() {
        conteudoRecebida()

        compose.onNodeWithContentDescription("Recusar chamada")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `encerrar tem alvo de toque acima do minimo`() {
        conteudoAtiva()

        compose.onNodeWithContentDescription("Encerrar chamada")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `mudo tem alvo de toque acima do minimo e anuncia o estado desligado`() {
        conteudoAtiva()

        compose.onNodeWithContentDescription("Mudo, desativado")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `viva-voz e teclado tem alvo de toque acima do minimo`() {
        conteudoAtiva()

        compose.onNodeWithContentDescription("Viva-voz, desativado")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
        compose.onNodeWithContentDescription("Teclado numérico, fechado")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `cada tecla do painel de tons tem alvo de toque acima do minimo`() {
        conteudoAtiva(retratoAtivo.copy(keypadOpen = true))

        listOf("Tecla um", "Tecla zero", "Tecla asterisco", "Tecla sustenido").forEach { descricao ->
            // A rolagem faz parte da prova: numa tela baixa a ultima fileira fica sob a dobra, e o
            // que precisa ficar travado e que ela seja ALCANCAVEL e, alcancada, tenha o alvo cheio.
            // Medir sem rolar mediria o recorte da janela, nao o alvo da tecla.
            compose.onNodeWithContentDescription(descricao)
                .performScrollTo()
                .assertTouchHeightIsAtLeast(alvoMinimo)
                .assertTouchWidthIsAtLeast(alvoMinimo)
        }
    }

    @Test
    fun `fechar o painel de tons tem alvo de toque acima do minimo`() {
        conteudoAtiva(retratoAtivo.copy(keypadOpen = true))

        compose.onNodeWithContentDescription("Fechar teclado")
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `todo controle da chamada recebida tem descricao de conteudo propria`() {
        conteudoRecebida()

        val atender = compose.onAllNodesWithContentDescription("Atender chamada")
            .fetchSemanticsNodes()
        val recusar = compose.onAllNodesWithContentDescription("Recusar chamada")
            .fetchSemanticsNodes()
        assertEquals(1, atender.size)
        assertEquals(1, recusar.size)
    }

    @Test
    fun `a ordem de foco da chamada recebida vai da marca as duas acoes`() {
        conteudoRecebida()

        val indices = compose.onRoot().fetchSemanticsNode()
            .let(::coletarIndices)
        // Marca (0), estado (1), identidade (2) e acoes (3): declarada, nao herdada da geometria.
        assertEquals(listOf(0f, 1f, 2f, 3f), indices.sorted())
    }

    @Test
    fun `recusar e anunciado antes de atender na arvore de semantica`() {
        conteudoRecebida()

        val arvore = compose.onRoot().printToString(maxDepth = Int.MAX_VALUE)
        val posicaoRecusar = arvore.indexOf("Recusar chamada")
        val posicaoAtender = arvore.indexOf("Atender chamada")
        assertTrue(posicaoRecusar in 0 until posicaoAtender)
    }

    @Test
    fun `o estado nao suportado mantem o encerrar alcancavel`() {
        conteudoAtiva(retratoAtivo.copy(state = CallUiState.Unsupported(rawState = 99)))

        compose.onNodeWithContentDescription("Encerrar chamada")
            .assertTouchHeightIsAtLeast(alvoMinimo)
    }

    /**
     * A chave do extra vale exatamente o contrato ditado.
     *
     * O valor e montado a partir do identificador do aplicativo no codigo de producao, porque um
     * invariante do projeto proibe esse identificador literal em Kotlin de producao. Aqui, no
     * conjunto de teste, ele pode e DEVE aparecer por extenso: e este caso que impede a montagem de
     * mudar o valor em silencio e quebrar o botao da notificacao.
     */
    @Test
    fun `a chave do extra de acao vale o contrato ditado`() {
        assertEquals("org.sentinela.app.extra.CALL_ACTION", EXTRA_CALL_ACTION)
    }

    @Test
    fun `acao de intencao desconhecida nao envia nenhum comando ao armazem`() {
        val eventos = mutableListOf<String>()
        val sessao = CallSessionCoordinator(controls = ControlesEspiao(eventos))
        sessao.onCallAdded(rawState = ESTADO_TOCANDO, identity = contato)
        eventos.clear()

        applyCallAction(sessao, callActionOf("desligar-tudo"))
        applyCallAction(sessao, callActionOf(null))

        assertEquals(emptyList<String>(), eventos)
    }

    @Test
    fun `acao de intencao conhecida chega ao comando correspondente`() {
        val eventos = mutableListOf<String>()
        val sessao = CallSessionCoordinator(controls = ControlesEspiao(eventos))
        sessao.onCallAdded(rawState = ESTADO_TOCANDO, identity = contato)
        eventos.clear()

        applyCallAction(sessao, callActionOf(CALL_ACTION_ANSWER))

        assertEquals(listOf("answer"), eventos)
    }

    private fun coletarIndices(node: SemanticsNode): List<Float> {
        val proprio = node.config.getOrElseNullable(SemanticsProperties.TraversalIndex) { null }
        val filhos = node.children.flatMap(::coletarIndices)
        return listOfNotNull(proprio) + filhos
    }

    private companion object {
        /** Codigo de chamada de entrada tocando, do mapeamento do plano 06-01. */
        const val ESTADO_TOCANDO = 2
    }
}

/** Costura de teste que so registra o nome do comando pedido, em ordem. */
private class ControlesEspiao(
    private val eventos: MutableList<String>,
) : org.sentinela.app.telecom.call.CallControls {
    override fun answer() { eventos += "answer" }
    override fun reject() { eventos += "reject" }
    override fun hangUp() { eventos += "hangUp" }
    override fun setMuted(muted: Boolean) { eventos += "setMuted:$muted" }
    override fun setSpeakerOn(on: Boolean) { eventos += "setSpeakerOn:$on" }
    override fun playDtmf(digit: Char) { eventos += "playDtmf:$digit" }
    override fun stopDtmf() { eventos += "stopDtmf" }
}

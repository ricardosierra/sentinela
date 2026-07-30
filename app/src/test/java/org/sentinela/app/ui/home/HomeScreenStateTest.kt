package org.sentinela.app.ui.home

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Os oito estados degradados da home, o teto de avisos e os dois eixos de alvo.
 *
 * Os quatro casos do zero ficam LADO A LADO nesta classe de proposito: os tres em que zero e
 * proibido (historico desligado, carregando, falha de leitura) e o unico em que zero e verdade. Quem
 * "consertar" um deles quebrando o outro descobre no mesmo arquivo, e nao numa suite distante.
 *
 * Os qualificadores de tela sao obrigatorios — o aparelho padrao do Robolectric e pequeno demais e
 * reprova por motivo falso, com conteudo fora do viewport (registro da Fase 6). Todo texto de assert
 * vem do recurso em tempo de teste: literal aqui deixaria um caso verde sobre uma frase que a
 * varredura de honestidade nunca viu.
 *
 * Os tres asserts de alvo sao IMPORTADOS do arquivo neutro de apoio; duplica-los e proibido para
 * sempre, e todo caso de alvo afirma os DOIS eixos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenStateTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alvoDaLinha = 72.dp
    private val alvoDoItemDaBarra = 56.dp

    private fun texto(id: Int): String = context.getString(id)

    private val ultima = LastBlockedUi(
        maskedNumber = "+55 11 9****-1234",
        reasonLabelRes = R.string.history_unknown_number,
        timestampUtcMillis = INSTANTE_DA_ULTIMA,
    )

    private val saudavel = HomeUiState(
        protectionEnabled = true,
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        totalBlocked = StatValue.Loaded(42),
        blockedToday = StatValue.Loaded(3),
        lastBlocked = ultima,
        historyEnabled = true,
        readError = false,
    )

    private var correcoesDoPapel = 0
    private var pedidosDeAgenda = 0
    private var ligacoesDoHistorico = 0
    private var tentativasDeLeitura = 0
    private var aberturasDaAtivacao = 0

    private fun compor(state: HomeUiState) {
        compose.setContent {
            org.sentinela.app.ui.theme.SentinelaTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state = state,
                    onProtectionChange = {},
                    onFixRole = { correcoesDoPapel++ },
                    onGrantContacts = { pedidosDeAgenda++ },
                    onOpenAppSettings = {},
                    onEnableHistory = { ligacoesDoHistorico++ },
                    onRetryHistory = { tentativasDeLeitura++ },
                    onOpenSettings = {},
                    onOpenWhitelist = {},
                    onOpenHistory = {},
                    onOpenDialerActivation = { aberturasDaAtivacao++ },
                    nowUtcMillis = AGORA,
                )
            }
        }
    }

    /**
     * Varredura da arvore semantica: todo texto e toda descricao de conteudo de todos os nos.
     *
     * O caso do zero proibido precisa de dentes. Afirmar apenas que o traco aparece seria fraco — a
     * tela poderia exibir os DOIS, o traco e o zero, e o assert continuaria verde. O que a regra diz
     * e que o caractere zero nao existe em lugar nenhum, e e isso que a varredura mede.
     */
    private fun textosDaArvore(mesclada: Boolean): List<String> {
        val raiz = compose.onRoot(useUnmergedTree = !mesclada).fetchSemanticsNode()
        val coletados = mutableListOf<String>()
        fun visitar(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { coletados += it.text }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { coletados += it }
            node.config.getOrNull(SemanticsProperties.StateDescription)?.let { coletados += it }
            node.children.forEach(::visitar)
        }
        visitar(raiz)
        return coletados
    }

    private fun nenhumZeroNaTela() {
        listOf(true, false).forEach { mesclada ->
            val zeros = textosDaArvore(mesclada).filter { it.trim() == "0" }
            assertTrue(
                "a tela renderizou o caractere zero como reserva de contagem ausente " +
                    "(arvore mesclada=$mesclada): $zeros",
                zeros.isEmpty(),
            )
        }
    }

    // ---------------------------------------------------------------- o zero, os quatro casos

    @Test
    fun `historico desligado nao renderiza zero em nenhum cartao de estatistica`() {
        compor(
            saudavel.copy(
                historyEnabled = false,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
                lastBlocked = null,
            ),
        )
        nenhumZeroNaTela()
        assertTrue(
            "o valor indisponivel tem de aparecer no lugar do numero",
            textosDaArvore(mesclada = false).any { it == texto(R.string.dashboard_history_off_value) },
        )
    }

    @Test
    fun `carregando nao renderiza zero em nenhum cartao de estatistica`() {
        compor(saudavel.copy(totalBlocked = StatValue.Loading, blockedToday = StatValue.Loading))
        nenhumZeroNaTela()
        assertTrue(
            "o estado de carregamento tem de ser anunciado",
            textosDaArvore(mesclada = true).any { it.contains(texto(R.string.state_loading)) },
        )
    }

    @Test
    fun `falha de leitura nao renderiza zero em nenhum cartao de estatistica`() {
        compor(
            saudavel.copy(
                readError = true,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
            ),
        )
        nenhumZeroNaTela()
        compose.onNodeWithText(texto(R.string.state_error)).assertExists()
    }

    @Test
    fun `historico ligado com contagem zero renderiza zero e o bloco vazio`() {
        compor(
            saudavel.copy(
                totalBlocked = StatValue.Loaded(0),
                blockedToday = StatValue.Loaded(0),
                lastBlocked = null,
            ),
        )
        val zeros = textosDaArvore(mesclada = false).filter { it.trim() == "0" }
        assertEquals("aqui zero e VERDADE e tem de aparecer nos dois cartoes", 2, zeros.size)
        compose.onNodeWithText(texto(R.string.history_empty)).assertExists()
    }

    // ---------------------------------------------------------------- a ultima bloqueada

    @Test
    fun `historico desligado nao renderiza o bloco da ultima bloqueada`() {
        compor(
            saudavel.copy(
                historyEnabled = false,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
            ),
        )
        compose.onNodeWithText(texto(R.string.dashboard_last_blocked)).assertDoesNotExist()
        compose.onNodeWithText(ultima.maskedNumber).assertDoesNotExist()
    }

    @Test
    fun `historico ligado renderiza o cabecalho e o cartao da ultima bloqueada`() {
        compor(saudavel)
        compose.onNodeWithText(texto(R.string.dashboard_last_blocked)).assertExists()
        compose.onNodeWithText(ultima.maskedNumber, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `o tempo relativo da ultima bloqueada sai da granularidade em minutos`() {
        compor(saudavel)
        val esperado = context.resources.getQuantityString(R.plurals.time_minutes_ago, 15, 15)
        assertTrue(
            "o tempo relativo esperado era $esperado",
            textosDaArvore(mesclada = false).any { it == esperado },
        )
    }

    // ---------------------------------------------------------------- o papel de triagem

    @Test
    fun `papel ausente com papel disponivel exibe o aviso e o botao de correcao`() {
        compor(saudavel.copy(screeningRoleHeld = false, protectionEnabled = false))
        compose.onNodeWithText(texto(R.string.dashboard_role_missing)).assertExists()
        compose.onNodeWithText(texto(R.string.dashboard_fix_configuration)).assertExists()
    }

    @Test
    fun `tocar o botao de correcao emite exatamente uma vez`() {
        compor(saudavel.copy(screeningRoleHeld = false))
        compose.onNodeWithText(texto(R.string.dashboard_fix_configuration))
            .performScrollTo()
            .performClick()
        assertEquals(1, correcoesDoPapel)
    }

    @Test
    fun `papel indisponivel no aparelho nao exibe botao de correcao`() {
        compor(saudavel.copy(screeningRoleHeld = false, screeningRoleAvailable = false))
        compose.onNodeWithText(texto(R.string.dashboard_role_missing)).assertExists()
        compose.onNodeWithText(texto(R.string.dashboard_fix_configuration)).assertDoesNotExist()
    }

    // ---------------------------------------------------------------- a protecao e o interruptor

    @Test
    fun `protecao desligada exibe o cartao em desligado e nenhum aviso de erro`() {
        compor(saudavel.copy(protectionEnabled = false))
        compose.onNodeWithText(texto(R.string.dashboard_protection_inactive)).assertExists()
        compose.onNodeWithText(texto(R.string.dashboard_protection_off_hint)).assertExists()
        compose.onNodeWithText(texto(R.string.dashboard_role_missing)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.state_error)).assertDoesNotExist()
        assertTrue(
            "as estatisticas continuam visiveis quando a protecao e desligada por escolha",
            textosDaArvore(mesclada = false).any { it == "42" },
        )
    }

    @Test
    fun `o interruptor e anunciado com papel de interruptor e descricao de estado`() {
        compor(saudavel)
        val no = compose.onNode(isToggleable()).fetchSemanticsNode()
        assertEquals(
            "o interruptor precisa anunciar papel de interruptor",
            Role.Switch,
            no.config.getOrNull(SemanticsProperties.Role),
        )
        assertEquals(
            "a descricao de estado mora no no do PROPRIO interruptor",
            texto(R.string.state_on),
            no.config.getOrNull(SemanticsProperties.StateDescription),
        )
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun `a descricao de estado do interruptor difere entre ligado e desligado`() {
        compor(saudavel.copy(protectionEnabled = false))
        val no = compose.onNode(isToggleable()).fetchSemanticsNode()
        assertEquals(
            texto(R.string.state_off),
            no.config.getOrNull(SemanticsProperties.StateDescription),
        )
        assertNotEquals(texto(R.string.state_on), texto(R.string.state_off))
        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `o interruptor continua alcancavel na arvore MESCLADA`() {
        compor(saudavel)
        // A arvore nao mesclada mede a ESTRUTURA da composicao; a mesclada mede o que o leitor de
        // tela recebe. Um caso que so olhasse a nao mesclada provaria que o controle existe, nunca
        // que ele e alcancavel.
        assertEquals(
            "o interruptor tem de ser um no unico e alcancavel na arvore mesclada",
            1,
            compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size,
        )
        val estados = textosDaArvore(mesclada = true)
        assertTrue(
            "o estado do interruptor tem de chegar a arvore mesclada",
            estados.any { it == texto(R.string.state_on) },
        )
    }

    // ---------------------------------------------------------------- avisos: agenda, historico, discador

    @Test
    fun `agenda negada uma vez oferece um novo pedido`() {
        compor(saudavel.copy(contactsPermission = ContactsPermissionState.DENIED_ONCE))
        compose.onNodeWithText(texto(R.string.dashboard_contacts_missing)).assertExists()
        compose.onNodeWithText(texto(R.string.dialer_activation_grant_contacts))
            .performScrollTo()
            .performClick()
        assertEquals(1, pedidosDeAgenda)
    }

    @Test
    fun `agenda negada de vez oferece as configuracoes do aplicativo e nenhum pedido`() {
        compor(saudavel.copy(contactsPermission = ContactsPermissionState.DENIED_PERMANENTLY))
        compose.onNodeWithText(texto(R.string.about_open_app_settings)).assertExists()
        compose.onNodeWithText(texto(R.string.dialer_activation_grant_contacts)).assertDoesNotExist()
    }

    @Test
    fun `historico desligado oferece a acao de ligar o historico`() {
        compor(
            saudavel.copy(
                historyEnabled = false,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
                lastBlocked = null,
            ),
        )
        compose.onNodeWithText(texto(R.string.dashboard_history_off)).assertExists()
        compose.onNodeWithText(texto(R.string.dashboard_history_off_action))
            .performScrollTo()
            .performClick()
        assertEquals(1, ligacoesDoHistorico)
    }

    @Test
    fun `falha de leitura oferece tentar de novo`() {
        compor(
            saudavel.copy(
                readError = true,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
            ),
        )
        compose.onNodeWithText(texto(R.string.action_retry)).performScrollTo().performClick()
        assertEquals(1, tentativasDeLeitura)
    }

    @Test
    fun `papel de discador perdido leva a tela de ativacao sem tom de erro`() {
        compor(saudavel.copy(dialerMode = DialerModeState.ROLE_LOST))
        compose.onNodeWithText(texto(R.string.dialer_role_lost_body)).assertExists()
        compose.onNodeWithText(texto(R.string.state_error)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.dialer_role_lost_action))
            .performScrollTo()
            .performClick()
        assertEquals(1, aberturasDaAtivacao)
    }

    @Test
    fun `nunca aparecem mais de dois avisos simultaneos`() {
        compor(
            saudavel.copy(
                screeningRoleHeld = false,
                contactsPermission = ContactsPermissionState.DENIED_ONCE,
                historyEnabled = false,
                dialerMode = DialerModeState.ROLE_LOST,
                totalBlocked = StatValue.Unavailable,
                blockedToday = StatValue.Unavailable,
                lastBlocked = null,
            ),
        )
        val possiveis = listOf(
            R.string.dashboard_role_missing,
            R.string.dashboard_contacts_missing,
            R.string.dashboard_history_off,
            R.string.dialer_role_lost_body,
        )
        val presentes = possiveis.count { id ->
            compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("o teto de avisos simultaneos e dois", 2, presentes)
        compose.onNodeWithText(texto(R.string.dashboard_more_warnings)).assertExists()
        assertTrue(
            "a precedencia manda o papel de triagem primeiro",
            compose.onAllNodesWithText(texto(R.string.dashboard_role_missing))
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertFalse(
            "o excedente nao pode virar um terceiro aviso empilhado",
            compose.onAllNodesWithText(texto(R.string.dialer_role_lost_body))
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    // ---------------------------------------------------------------- alvos de toque, dois eixos

    @Test
    fun `o interruptor passa os dois eixos de alvo`() {
        compor(saudavel)
        compose.onNode(isToggleable())
            .assertLayoutHeightIsAtLeast(alvoMinimo)
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `os atalhos passam os dois eixos de alvo`() {
        compor(saudavel)
        listOf(R.string.dashboard_quick_whitelist, R.string.dashboard_quick_history).forEach { id ->
            compose.onNode(hasText(texto(id)) and hasClickAction())
                .assertLayoutHeightIsAtLeast(alvoDaLinha)
                .assertTouchHeightIsAtLeast(alvoDaLinha)
                .assertTouchWidthIsAtLeast(alvoMinimo)
        }
    }

    @Test
    fun `o cartao da ultima bloqueada passa os dois eixos de alvo`() {
        compor(saudavel)
        compose.onNode(hasText(ultima.maskedNumber, substring = true) and hasClickAction())
            .assertLayoutHeightIsAtLeast(alvoDaLinha)
            .assertTouchHeightIsAtLeast(alvoDaLinha)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `os itens da barra inferior passam os dois eixos de alvo`() {
        compor(saudavel)
        compose.onNode(hasText(texto(R.string.nav_home)) and hasClickAction())
            .assertLayoutHeightIsAtLeast(alvoDoItemDaBarra)
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    private companion object {
        /** 15 minutos antes de [AGORA]: cai na faixa de minutos da granularidade. */
        const val AGORA = 1_700_000_000_000L
        const val INSTANTE_DA_ULTIMA = AGORA - 15 * 60_000L
    }
}

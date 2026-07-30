package org.sentinela.app.ui.onboarding

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Passos 3 e 4 do onboarding: os quatro ramos da permissao da agenda, os dois padroes
 * de politica e os dois eixos de alvo de toque.
 *
 * Os qualificadores de tela sao obrigatorios — o aparelho padrao do Robolectric e
 * pequeno demais e reprova por motivo falso, com o conteudo saindo do viewport
 * (registro da Fase 6). Todo texto de assert vem do recurso em tempo de teste: um
 * literal aqui deixaria um caso verde sobre uma frase que a varredura de honestidade
 * nunca viu.
 *
 * Os tres asserts de alvo sao IMPORTADOS do arquivo neutro de apoio; duplica-los e
 * proibido para sempre. Todo caso de alvo afirma os DOIS eixos, porque o Compose
 * expande sozinho o alvo de toque de qualquer componente interativo ate o minimo da
 * plataforma — sem o eixo do desenho a suite mediria a garantia da biblioteca.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ContactsAndWhitelistStepTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alvoDoCartao = 72.dp
    private val alvoDoBotaoPrincipal = 56.dp

    private fun texto(id: Int): String = context.getString(id)

    private var pedidosDeAgenda = 0
    private var aberturasDeConfiguracoes = 0
    private var politicaEscolhida: OriginPolicy? = null
    private var privadosAlterados: Boolean? = null

    private fun passoDeContatos(
        permission: ContactsPermissionState,
        selected: OriginPolicy = OriginPolicy.RING,
        blockPrivate: Boolean = true,
    ) {
        compose.setContent {
            ContactsPolicyStepScreen(
                permission = permission,
                selected = selected,
                blockPrivate = blockPrivate,
                onSelect = { politicaEscolhida = it },
                onBlockPrivateChange = { privadosAlterados = it },
                onGrantContacts = { pedidosDeAgenda++ },
                onOpenAppSettings = { aberturasDeConfiguracoes++ },
                onNext = {},
                onSkip = {},
            )
        }
    }

    private fun passoDaWhitelist(selected: OriginPolicy = OriginPolicy.NEVER_SILENCE) {
        compose.setContent {
            WhitelistPolicyStepScreen(
                selected = selected,
                onSelect = { politicaEscolhida = it },
                onNext = {},
                onBack = {},
                onSkip = {},
            )
        }
    }

    private fun contarNosComTexto(valor: String): Int =
        compose.onAllNodesWithText(valor).fetchSemanticsNodes().size

    private fun contarCartoesDeOpcao(): Int =
        compose.onAllNodes(isSelectable()).fetchSemanticsNodes().size

    private fun descricaoDeEstadoDoInterruptor(): String? =
        compose.onNode(isToggleable()).fetchSemanticsNode()
            .config.getOrElseNullable(SemanticsProperties.StateDescription) { null }

    // --- Ramo 1: nunca perguntado ------------------------------------------------

    @Test
    fun `nunca perguntado exibe a justificativa e o botao de permitir`() {
        passoDeContatos(ContactsPermissionState.NEVER_ASKED)

        compose.onNodeWithText(texto(R.string.contacts_permission_rationale))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.dialer_activation_grant_contacts))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tocar em permitir emite o pedido da agenda uma unica vez`() {
        passoDeContatos(ContactsPermissionState.NEVER_ASKED)

        compose.onNodeWithText(texto(R.string.dialer_activation_grant_contacts))
            .performScrollTo().performClick()

        assertEquals(1, pedidosDeAgenda)
        assertEquals(0, aberturasDeConfiguracoes)
    }

    // --- Ramo 2: concedido -------------------------------------------------------

    @Test
    fun `concedido exibe o chip de concedido e nenhum botao de permitir`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        compose.onNodeWithText(texto(R.string.contacts_permission_granted))
            .performScrollTo().assertIsDisplayed()
        assertEquals(
            0,
            contarNosComTexto(texto(R.string.dialer_activation_grant_contacts)),
        )
        assertEquals(
            0,
            contarNosComTexto(texto(R.string.contacts_permission_rationale)),
        )
    }

    // --- Ramo 3: negado uma vez --------------------------------------------------

    @Test
    fun `negado uma vez exibe a consequencia honesta com acao de permitir`() {
        passoDeContatos(ContactsPermissionState.DENIED_ONCE)

        compose.onNodeWithText(texto(R.string.contacts_permission_denied))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.dialer_activation_grant_contacts))
            .performScrollTo().performClick()

        assertEquals(1, pedidosDeAgenda)
    }

    // --- Ramo 4: negado definitivamente -----------------------------------------

    @Test
    fun `negado definitivamente exibe o bloqueio com atalho para as configuracoes`() {
        passoDeContatos(ContactsPermissionState.DENIED_PERMANENTLY)

        compose.onNodeWithText(texto(R.string.contacts_permission_blocked))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.about_open_app_settings))
            .performScrollTo().performClick()

        assertEquals(1, aberturasDeConfiguracoes)
    }

    @Test
    fun `negado definitivamente nao oferece nenhum pedido do sistema`() {
        passoDeContatos(ContactsPermissionState.DENIED_PERMANENTLY)

        assertEquals(
            0,
            contarNosComTexto(texto(R.string.dialer_activation_grant_contacts)),
        )
        assertEquals(0, pedidosDeAgenda)
    }

    // --- As opcoes nunca sao desabilitadas por falta de permissao ----------------

    /**
     * Um caso por estado, e nao um laco dentro de um caso: a regra de composicao hospeda
     * UMA arvore por caso, e trocar o estado exigiria compor de novo na mesma regra.
     */
    private fun afirmarQuatroOpcoesHabilitadas() {
        listOf(
            R.string.contacts_option_ring,
            R.string.contacts_option_block,
            R.string.contacts_option_silence,
            R.string.contacts_option_never_silence,
        ).forEach { titulo ->
            compose.onNodeWithText(texto(titulo)).performScrollTo().assertIsEnabled()
        }
    }

    @Test
    fun `nunca perguntado mantem as quatro opcoes habilitadas`() {
        passoDeContatos(ContactsPermissionState.NEVER_ASKED)
        afirmarQuatroOpcoesHabilitadas()
    }

    @Test
    fun `concedido mantem as quatro opcoes habilitadas`() {
        passoDeContatos(ContactsPermissionState.GRANTED)
        afirmarQuatroOpcoesHabilitadas()
    }

    @Test
    fun `negado uma vez mantem as quatro opcoes habilitadas`() {
        passoDeContatos(ContactsPermissionState.DENIED_ONCE)
        afirmarQuatroOpcoesHabilitadas()
    }

    @Test
    fun `negado definitivamente mantem as quatro opcoes habilitadas`() {
        passoDeContatos(ContactsPermissionState.DENIED_PERMANENTLY)
        afirmarQuatroOpcoesHabilitadas()
    }

    // --- Padroes -----------------------------------------------------------------

    @Test
    fun `o passo 3 vem com tocar pre-selecionado`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        compose.onNodeWithText(texto(R.string.contacts_option_ring))
            .performScrollTo().assertIsSelected()
        compose.onNodeWithText(texto(R.string.contacts_option_block))
            .performScrollTo().assertIsNotSelected()
    }

    @Test
    fun `o passo 3 vem com o interruptor de privados ligado`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        compose.onNodeWithText(texto(R.string.settings_block_private))
            .performScrollTo().assertIsDisplayed()
        assertEquals(texto(R.string.state_on), descricaoDeEstadoDoInterruptor())
    }

    @Test
    fun `o passo 4 vem com nunca silenciar pre-selecionado`() {
        passoDaWhitelist()

        compose.onNodeWithText(texto(R.string.whitelist_option_never_silence))
            .performScrollTo().assertIsSelected()
        compose.onNodeWithText(texto(R.string.whitelist_option_ring))
            .performScrollTo().assertIsNotSelected()
    }

    @Test
    fun `o passo 4 exibe o hint na tela e o botao principal e proximo`() {
        passoDaWhitelist()

        compose.onNodeWithText(texto(R.string.whitelist_setup_hint))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.onboarding_next))
            .performScrollTo().assertIsDisplayed()
        assertEquals(0, contarNosComTexto(texto(R.string.onboarding_finish)))
    }

    // --- Interruptor de privados: o ponto de risco (b) da semantica mesclada -----

    @Test
    fun `o interruptor de privados tem papel de interruptor e explicacao em no separado`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        val no = compose.onNode(isToggleable()).fetchSemanticsNode()
        assertEquals(
            androidx.compose.ui.semantics.Role.Switch,
            no.config.getOrElseNullable(SemanticsProperties.Role) { null },
        )
        compose.onNodeWithText(texto(R.string.settings_block_private_desc))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a descricao de estado do interruptor distingue ligado de desligado`() {
        passoDeContatos(ContactsPermissionState.GRANTED, blockPrivate = false)

        assertEquals(texto(R.string.state_off), descricaoDeEstadoDoInterruptor())
    }

    // --- Alvos de toque, sempre nos dois eixos ----------------------------------

    @Test
    fun `cada cartao de opcao do passo 3 tem alvo e desenho acima do minimo`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        listOf(
            R.string.contacts_option_ring,
            R.string.contacts_option_block,
            R.string.contacts_option_silence,
            R.string.contacts_option_never_silence,
        ).forEach { titulo ->
            compose.onNodeWithText(texto(titulo))
                .performScrollTo()
                .assertTouchHeightIsAtLeast(alvoDoCartao)
                .assertTouchWidthIsAtLeast(alvoMinimo)
                .assertLayoutHeightIsAtLeast(alvoDoCartao)
        }
    }

    @Test
    fun `cada cartao de opcao do passo 4 tem alvo e desenho acima do minimo`() {
        passoDaWhitelist()

        listOf(
            R.string.whitelist_option_never_silence,
            R.string.whitelist_option_ring,
            R.string.whitelist_option_block,
            R.string.whitelist_option_silence,
        ).forEach { titulo ->
            compose.onNodeWithText(texto(titulo))
                .performScrollTo()
                .assertTouchHeightIsAtLeast(alvoDoCartao)
                .assertTouchWidthIsAtLeast(alvoMinimo)
                .assertLayoutHeightIsAtLeast(alvoDoCartao)
        }
    }

    @Test
    fun `o interruptor de privados tem alvo e desenho acima do minimo`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        compose.onNode(isToggleable())
            .performScrollTo()
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `o botao principal do passo 3 tem alvo e desenho acima do minimo`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        compose.onNodeWithText(texto(R.string.onboarding_next))
            .assertTouchHeightIsAtLeast(alvoDoBotaoPrincipal)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoDoBotaoPrincipal)
    }

    @Test
    fun `os dois botoes do passo 4 tem alvo e desenho acima do minimo`() {
        passoDaWhitelist()

        compose.onNodeWithText(texto(R.string.onboarding_next))
            .performScrollTo()
            .assertTouchHeightIsAtLeast(alvoDoBotaoPrincipal)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoDoBotaoPrincipal)
        compose.onNodeWithText(texto(R.string.onboarding_back))
            .performScrollTo()
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    // --- Contagem ----------------------------------------------------------------

    @Test
    fun `o passo 3 oferece exatamente quatro opcoes`() {
        passoDeContatos(ContactsPermissionState.GRANTED)

        assertEquals(4, contarCartoesDeOpcao())
    }

    @Test
    fun `o passo 4 oferece exatamente quatro opcoes`() {
        passoDaWhitelist()

        assertEquals(4, contarCartoesDeOpcao())
    }
}

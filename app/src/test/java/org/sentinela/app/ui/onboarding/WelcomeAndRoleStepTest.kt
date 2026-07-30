package org.sentinela.app.ui.onboarding

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Composicao dos tres primeiros ecras do fluxo: boas-vindas, passo 1 de 6 e passo 2 de 6.
 *
 * Os qualificadores de tela sao obrigatorios: o aparelho padrao do Robolectric e pequeno demais e
 * reprova por motivo falso, com o conteudo saindo do viewport — registro da Fase 6.
 *
 * Todo texto de assert vem do recurso lido em tempo de teste, nunca de literal. E o que mantem a
 * varredura de honestidade da copy como unica dona do texto: um literal aqui deixaria um caso verde
 * sobre uma frase que a varredura nunca viu.
 *
 * Todo assert de toque afirma DOIS EIXOS, e os tres helpers vem do arquivo neutro de apoio —
 * duplica-los e proibido. O eixo que tem dentes e o do tamanho DESENHADO: o Compose expande sozinho
 * o alvo de toque de qualquer componente interativo ate o minimo da plataforma, entao um assert que
 * so olhe o alvo mede a garantia da biblioteca, e nao o nosso layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WelcomeAndRoleStepTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alturaDoBotao = 56.dp
    private val alturaDoCartao = 72.dp

    private fun texto(id: Int): String = context.getString(id)

    // ----- boas-vindas -------------------------------------------------------------------------

    private fun comporBoasVindas(onStart: () -> Unit = {}, onAbout: () -> Unit = {}) {
        compose.setContent { WelcomeScreen(onStart = onStart, onAbout = onAbout) }
    }

    @Test
    fun `boas-vindas exibe titulo e subtitulo`() {
        comporBoasVindas()
        compose.onNodeWithText(texto(R.string.welcome_headline)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.welcome_subtitle)).assertIsDisplayed()
    }

    @Test
    fun `boas-vindas exibe os tres cartoes honestos`() {
        comporBoasVindas()
        compose.onNodeWithText(texto(R.string.welcome_feature_local_title)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.welcome_feature_silent_title)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.welcome_feature_offline_title)).assertIsDisplayed()
    }

    @Test
    fun `boas-vindas exibe o selo de codigo aberto`() {
        comporBoasVindas()
        compose.onNodeWithText(texto(R.string.welcome_open_source))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `boas-vindas nao promete base de numeros nem seguranca garantida`() {
        comporBoasVindas()
        compose.onNodeWithText(texto(R.string.welcome_feature_local_desc), substring = false)
            .assertExists()
        compose.onNodeWithText(texto(R.string.welcome_badge_native)).assertDoesNotExist()
    }

    @Test
    fun `botao de boas-vindas passa os dois eixos de toque`() {
        comporBoasVindas()
        compose.onNodeWithText(texto(R.string.welcome_cta))
            .assertHasClickAction()
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alturaDoBotao)
    }

    @Test
    fun `botao de boas-vindas emite exatamente uma vez`() {
        var vezes = 0
        comporBoasVindas(onStart = { vezes++ })
        compose.onNodeWithText(texto(R.string.welcome_cta)).performClick()
        assertEquals(1, vezes)
    }

    // ----- passo 1 de 6 ------------------------------------------------------------------------

    private fun comporPassoDoPapel(
        state: OnboardingUiState = OnboardingUiState(step = 1),
        onRequestRole: () -> Unit = {},
        onNext: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) {
        compose.setContent {
            RoleStepScreen(
                state = state,
                onRequestRole = onRequestRole,
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }

    @Test
    fun `passo do papel exibe as tres frases do cartao de honestidade`() {
        comporPassoDoPapel()
        compose.onNodeWithText(texto(R.string.onboarding_scope_title))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.dialer_activation_unchanged_3)).assertExists()
        compose.onNodeWithText(texto(R.string.onboarding_scope_dnd)).assertExists()
        compose.onNodeWithText(texto(R.string.settings_hide_native_log_desc)).assertExists()
    }

    @Test
    fun `papel nao detido pede o papel e emite exatamente uma vez`() {
        var vezes = 0
        comporPassoDoPapel(onRequestRole = { vezes++ })
        compose.onNodeWithText(texto(R.string.onboarding_role_cta))
            .assertIsEnabled()
            .performClick()
        assertEquals(1, vezes)
    }

    @Test
    fun `pedido em curso deixa o botao anunciado como desabilitado`() {
        comporPassoDoPapel(
            state = OnboardingUiState(step = 1, roleRequestInFlight = true),
        )
        compose.onNodeWithText(texto(R.string.onboarding_role_requesting)).assertIsNotEnabled()
    }

    @Test
    fun `papel negado exibe o aviso com acao e o botao passa a proximo sem desabilitar`() {
        var pedidos = 0
        comporPassoDoPapel(
            state = OnboardingUiState(step = 1, roleDenied = true),
            onRequestRole = { pedidos++ },
        )
        compose.onNodeWithText(texto(R.string.onboarding_role_denied)).assertExists()
        compose.onNodeWithText(texto(R.string.onboarding_next)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.onboarding_role_retry)).performClick()
        assertEquals(1, pedidos)
    }

    @Test
    fun `papel detido exibe o chip de ativo e o avanco nao e automatico`() {
        var avancos = 0
        comporPassoDoPapel(
            state = OnboardingUiState(step = 1, screeningRoleHeld = true),
            onNext = { avancos++ },
        )
        compose.onNodeWithText(texto(R.string.dialer_active_chip)).assertExists()
        assertEquals(0, avancos)
        compose.onNodeWithText(texto(R.string.onboarding_next)).performClick()
        assertEquals(1, avancos)
    }

    @Test
    fun `pular do passo do papel emite exatamente uma vez`() {
        var pulos = 0
        comporPassoDoPapel(onSkip = { pulos++ })
        compose.onNodeWithText(texto(R.string.onboarding_skip)).performClick()
        assertEquals(1, pulos)
    }

    @Test
    fun `botao do passo do papel passa os dois eixos de toque`() {
        comporPassoDoPapel()
        compose.onNodeWithText(texto(R.string.onboarding_role_cta))
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alturaDoBotao)
    }

    // ----- passo 2 de 6 ------------------------------------------------------------------------

    private fun comporPassoDosDesconhecidos(
        selected: OriginPolicy = OriginPolicy.BLOCK,
        onSelect: (OriginPolicy) -> Unit = {},
        onNext: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) {
        compose.setContent {
            UnknownPolicyStepScreen(
                selected = selected,
                onSelect = onSelect,
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }

    @Test
    fun `passo dos desconhecidos vem com bloquear pre-selecionado`() {
        comporPassoDosDesconhecidos()
        compose.onNodeWithText(texto(R.string.unknown_option_block)).assertIsSelected()
        compose.onNodeWithText(texto(R.string.unknown_option_silence)).assertIsNotSelected()
        compose.onNodeWithText(texto(R.string.unknown_option_allow)).assertIsNotSelected()
    }

    @Test
    fun `escolher silenciar emite exatamente uma vez a politica escolhida`() {
        val escolhas = mutableListOf<OriginPolicy>()
        comporPassoDosDesconhecidos(onSelect = { escolhas += it })
        compose.onNodeWithText(texto(R.string.unknown_option_silence)).performClick()
        assertEquals(listOf(OriginPolicy.SILENCE), escolhas)
    }

    @Test
    fun `passo dos desconhecidos nao oferece nunca silenciar`() {
        comporPassoDosDesconhecidos()
        compose.onNodeWithText(texto(R.string.contacts_option_never_silence)).assertDoesNotExist()
    }

    @Test
    fun `cada cartao de opcao passa os dois eixos de toque`() {
        comporPassoDosDesconhecidos()
        listOf(
            R.string.unknown_option_block,
            R.string.unknown_option_silence,
            R.string.unknown_option_allow,
        ).forEach { chave ->
            compose.onNodeWithText(texto(chave))
                .assertTouchHeightIsAtLeast(alvoMinimo)
                .assertTouchWidthIsAtLeast(alvoMinimo)
                .assertLayoutHeightIsAtLeast(alturaDoCartao)
        }
    }

    @Test
    fun `botao do passo dos desconhecidos passa os dois eixos de toque e avanca`() {
        var avancos = 0
        comporPassoDosDesconhecidos(onNext = { avancos++ })
        compose.onNodeWithText(texto(R.string.onboarding_next))
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alturaDoBotao)
            .performClick()
        assertEquals(1, avancos)
    }
}

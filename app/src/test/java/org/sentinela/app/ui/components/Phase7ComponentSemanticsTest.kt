package org.sentinela.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Semantica dos seis componentes compartilhados da fase.
 *
 * Os qualificadores de tela sao obrigatorios: o aparelho padrao do Robolectric e
 * pequeno demais e reprova por motivo falso, com o conteudo saindo do viewport —
 * registro da Fase 6.
 *
 * Todo texto usado nos asserts vem dos recursos, nunca de literal. E o que mantem
 * a varredura de honestidade da copy como unica dona do texto: um literal aqui
 * deixaria um caso verde sobre uma frase que a varredura nunca viu.
 *
 * Todo assert de toque afirma DOIS EIXOS. O Compose expande sozinho o alvo de
 * toque de qualquer componente interativo ate o minimo da plataforma, entao um
 * assert que so olhe o alvo mede a garantia da biblioteca, e nao o nosso layout.
 * Os tres asserts sao importados do arquivo neutro de apoio; duplica-los e
 * proibido.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class Phase7ComponentSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alvoDoCartao = 72.dp
    private val alvoDoItemDeAba = 56.dp

    private fun texto(id: Int): String = context.getString(id)

    private fun tituloBloquear() = texto(R.string.unknown_option_block)
    private fun descricaoBloquear() = texto(R.string.unknown_option_block_desc)
    private fun tituloSilenciar() = texto(R.string.unknown_option_silence)
    private fun descricaoSilenciar() = texto(R.string.unknown_option_silence_desc)
    private fun tituloTocar() = texto(R.string.contacts_option_ring)
    private fun descricaoTocar() = texto(R.string.contacts_option_ring_desc)
    private fun motivoSemAgenda() = texto(R.string.contacts_permission_denied)

    private fun conteudoGrupoDeCartoes(desabilitarTerceiro: Boolean = false) {
        compose.setContent {
            Column(modifier = Modifier.optionCardGroup()) {
                OptionCard(
                    title = tituloBloquear(),
                    description = descricaoBloquear(),
                    icon = Icons.Outlined.Block,
                    selected = true,
                    onClick = {},
                )
                OptionCard(
                    title = tituloSilenciar(),
                    description = descricaoSilenciar(),
                    icon = Icons.Outlined.NotificationsOff,
                    selected = false,
                    onClick = {},
                )
                OptionCard(
                    title = tituloTocar(),
                    description = descricaoTocar(),
                    icon = Icons.Outlined.Phone,
                    selected = false,
                    onClick = {},
                    enabled = !desabilitarTerceiro,
                    unavailableReason = if (desabilitarTerceiro) motivoSemAgenda() else null,
                )
            }
        }
    }

    private fun conteudoLinhaDeInterruptor(
        ligado: Boolean,
        habilitado: Boolean = true,
    ) {
        compose.setContent {
            SettingSwitchRow(
                label = texto(R.string.settings_protection_toggle),
                description = texto(R.string.settings_protection_toggle_desc),
                checked = ligado,
                onCheckedChange = {},
                enabled = habilitado,
                unavailableReason = if (habilitado) null else texto(R.string.nav_unavailable),
            )
        }
    }

    private fun itensDaBarraInferior(): List<BottomBarItem> = listOf(
        BottomBarItem(
            label = texto(R.string.nav_home),
            icon = Icons.Outlined.Home,
            selected = true,
            onClick = {},
        ),
        BottomBarItem(
            label = texto(R.string.nav_whitelist),
            icon = Icons.Outlined.VerifiedUser,
            selected = false,
            onClick = {},
            enabled = false,
            unavailableReason = texto(R.string.nav_unavailable),
        ),
        BottomBarItem(
            label = texto(R.string.nav_history),
            icon = Icons.Outlined.History,
            selected = false,
            onClick = {},
            enabled = false,
            unavailableReason = texto(R.string.nav_unavailable),
        ),
        BottomBarItem(
            label = texto(R.string.nav_settings),
            icon = Icons.Outlined.Settings,
            selected = false,
            onClick = {},
        ),
    )

    private fun papelDe(chave: String): Role? = compose.onNodeWithText(chave)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.Role) { null }

    private fun descricaoDeEstadoDe(chave: String): String? = compose.onNodeWithText(chave)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    // --- cartao de opcao -----------------------------------------------------

    @Test
    fun `cartao selecionado anuncia papel de botao de radio e o estado selecionado`() {
        conteudoGrupoDeCartoes()
        assertEquals(Role.RadioButton, papelDe(tituloBloquear()))
        compose.onNodeWithText(tituloBloquear())
            .assertIsSelected()
            .assertTouchHeightIsAtLeast(alvoDoCartao)
            .assertTouchWidthIsAtLeast(alvoDoCartao)
            .assertLayoutHeightIsAtLeast(alvoDoCartao)
    }

    @Test
    fun `cartao nao selecionado mantem papel e mostra titulo e descricao`() {
        conteudoGrupoDeCartoes()
        assertEquals(Role.RadioButton, papelDe(tituloSilenciar()))
        compose.onNodeWithText(tituloSilenciar())
            .assertIsNotSelected()
            .assertTouchHeightIsAtLeast(alvoDoCartao)
            .assertLayoutHeightIsAtLeast(alvoDoCartao)
        compose.onNodeWithText(descricaoSilenciar()).assertIsDisplayed()
    }

    @Test
    fun `cartao desabilitado e anunciado como desabilitado e carrega o motivo em texto`() {
        conteudoGrupoDeCartoes(desabilitarTerceiro = true)
        compose.onNodeWithText(tituloTocar()).assertIsNotEnabled()
        assertEquals(motivoSemAgenda(), descricaoDeEstadoDe(tituloTocar()))
    }

    @Test
    fun `o grupo de cartoes e selecionavel e tem exatamente um selecionado`() {
        conteudoGrupoDeCartoes()
        val grupos = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .fetchSemanticsNodes()
        assertTrue("o container dos cartoes precisa declarar agrupamento selecionavel", grupos.isNotEmpty())
        val selecionados = compose
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .fetchSemanticsNodes()
        assertEquals(1, selecionados.size)
    }

    // --- cabecalho de passo --------------------------------------------------

    @Test
    fun `o cabecalho de passo mostra o contador formatado pelo recurso`() {
        compose.setContent { StepHeader(step = 2, total = 6) }
        compose.onNodeWithText(context.getString(R.string.onboarding_step_indicator, 2, 6))
            .assertIsDisplayed()
    }

    @Test
    fun `a barra do cabecalho de passo nao aparece como no com informacao`() {
        compose.setContent { StepHeader(step = 2, total = 6) }
        val barras = compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()
        assertTrue("a barra e decorativa e nao pode anunciar progresso", barras.isEmpty())
        val comTexto = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("o unico no de informacao do cabecalho e o contador", 1, comTexto.size)
    }

    // --- linha de interruptor -----------------------------------------------

    @Test
    fun `linha de interruptor ligada anuncia papel de interruptor e o estado ligado`() {
        conteudoLinhaDeInterruptor(ligado = true)
        val no = compose.onNode(isToggleable())
        assertEquals(
            Role.Switch,
            no.fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.Role) { null },
        )
        assertEquals(
            texto(R.string.state_on),
            no.fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
    }

    @Test
    fun `linha de interruptor desligada anuncia o estado desligado`() {
        conteudoLinhaDeInterruptor(ligado = false)
        assertEquals(
            texto(R.string.state_off),
            compose.onNode(isToggleable())
                .fetchSemanticsNode()
                .config
                .getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
    }

    @Test
    fun `a explicacao da linha de interruptor e legivel como no separado`() {
        conteudoLinhaDeInterruptor(ligado = true)
        compose.onNodeWithText(texto(R.string.settings_protection_toggle)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.settings_protection_toggle_desc)).assertIsDisplayed()
    }

    @Test
    fun `o interruptor tem alvo de toque e tamanho desenhado acima do minimo`() {
        conteudoLinhaDeInterruptor(ligado = true)
        compose.onNode(isToggleable())
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `linha de interruptor desabilitada e anunciada como desabilitada com motivo`() {
        conteudoLinhaDeInterruptor(ligado = false, habilitado = false)
        val no = compose.onNode(isToggleable())
        no.assertIsNotEnabled()
        assertEquals(
            texto(R.string.nav_unavailable),
            no.fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.StateDescription) { null },
        )
    }

    // --- linha de verificacao -----------------------------------------------

    @Test
    fun `a linha de verificacao anuncia rotulo seguido do estado`() {
        conteudoLinhaDeVerificacao()
        compose.onNodeWithContentDescription(
            context.getString(
                R.string.state_label_with_value,
                texto(R.string.onboarding_check_role),
                texto(R.string.onboarding_check_missing),
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun `a acao da linha de verificacao e no focavel separado com alvo acima do minimo`() {
        conteudoLinhaDeVerificacao()
        compose.onNodeWithText(texto(R.string.dashboard_fix_configuration))
            .assertHasClickAction()
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    private fun conteudoLinhaDeVerificacao() {
        compose.setContent {
            CheckRow(
                label = texto(R.string.onboarding_check_role),
                stateText = texto(R.string.onboarding_check_missing),
                ok = false,
                actionLabel = texto(R.string.dashboard_fix_configuration),
                onAction = {},
            )
        }
    }

    // --- barra inferior -----------------------------------------------------

    @Test
    fun `a barra inferior tem quatro itens com papel de aba e um selecionado`() {
        compose.setContent { SentinelaBottomBar(items = itensDaBarraInferior()) }
        val abas = compose
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .fetchSemanticsNodes()
        assertEquals(4, abas.size)
        compose.onNodeWithText(texto(R.string.nav_home)).assertIsSelected()
        compose.onNodeWithText(texto(R.string.nav_settings)).assertIsNotSelected()
    }

    @Test
    fun `os destinos que ainda nao existem ficam desabilitados com motivo textual`() {
        compose.setContent { SentinelaBottomBar(items = itensDaBarraInferior()) }
        listOf(R.string.nav_whitelist, R.string.nav_history).forEach { chave ->
            compose.onNodeWithText(texto(chave)).assertIsNotEnabled()
            assertEquals(texto(R.string.nav_unavailable), descricaoDeEstadoDe(texto(chave)))
        }
    }

    @Test
    fun `cada item da barra inferior tem alvo de toque e tamanho desenhado acima do minimo`() {
        compose.setContent { SentinelaBottomBar(items = itensDaBarraInferior()) }
        listOf(
            R.string.nav_home,
            R.string.nav_whitelist,
            R.string.nav_history,
            R.string.nav_settings,
        ).forEach { chave ->
            compose.onNodeWithText(texto(chave))
                .assertTouchHeightIsAtLeast(alvoMinimo)
                .assertTouchWidthIsAtLeast(alvoMinimo)
                .assertLayoutHeightIsAtLeast(alvoDoItemDeAba)
        }
    }

    // --- barra superior -----------------------------------------------------

    @Test
    fun `a barra superior le a marca do recurso e a acao tem alvo acima do minimo`() {
        compose.setContent {
            SentinelaTopBar(
                center = { StepHeader(step = 1, total = 6) },
                actions = {
                    SentinelaTopBarIconAction(
                        icon = Icons.Outlined.Info,
                        contentDescription = texto(R.string.about_title),
                        onClick = {},
                    )
                },
            )
        }
        compose.onNodeWithText(texto(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithContentDescription(texto(R.string.about_title))
            .assertHasClickAction()
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }
}

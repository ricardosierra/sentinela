package org.sentinela.app.ui.onboarding

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Passos 5 e 6 do onboarding, compostos de verdade.
 *
 * Os qualificadores de tela sao obrigatorios: o aparelho padrao do Robolectric e pequeno demais e
 * reprovaria por motivo falso, com o conteudo saindo do viewport — registro da Fase 6.
 *
 * Todo texto vem do recurso, nunca de literal: um literal aqui deixaria um caso verde sobre uma
 * frase que a varredura de honestidade da copy nunca viu.
 *
 * Os tres asserts de alvo de toque sao IMPORTADOS do arquivo neutro de apoio. Duplica-los e
 * proibido, e todo controle e medido nos DOIS eixos — o Compose expande sozinho o alvo de toque de
 * qualquer componente interativo, entao so o eixo do desenho pega um controle encolhido.
 *
 * Dois casos desta classe existem porque e neles que o falso-verde mora: o do veredito parcial
 * afirma o titulo E a existencia do conserto, e o da acao em no separado alcanca o botao na arvore
 * NAO MESCLADA e afirma que ele tem clique proprio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class NotificationAndSummaryStepTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alvoDoCartao = 72.dp
    private val alvoDoBotaoPrincipal = 56.dp

    private fun texto(id: Int): String = context.getString(id)

    private fun tituloMascarada() = texto(R.string.settings_notification_identification_masked)
    private fun tituloAnonima() = texto(R.string.settings_notification_identification_anonymous)
    private fun justificativa() = texto(R.string.notification_permission_rationale)
    private fun rotuloCorrigir() = texto(R.string.dashboard_fix_configuration)
    private fun rotuloPermitirAgenda() = texto(R.string.dialer_activation_grant_contacts)

    private fun anuncioDaLinha(rotulo: Int, estado: Int): String =
        context.getString(R.string.state_label_with_value, texto(rotulo), texto(estado))

    // ------------------------------------------------------------------ passo 5

    private var identificacaoEscolhida: NotificationIdentification? = null
    private var pedidoDeLigar: Boolean? = null

    private fun conteudoDoPasso5(
        ligado: Boolean = false,
        identificacao: NotificationIdentification = NotificationIdentification.MASKED,
        permissao: RuntimePermissionAsk = RuntimePermissionAsk.NEVER_ASKED,
    ) {
        compose.setContent {
            NotificationStepScreen(
                enabled = ligado,
                identification = identificacao,
                permission = permissao,
                onEnabledChange = { pedidoDeLigar = it },
                onIdentificationChange = { identificacaoEscolhida = it },
                onNext = {},
                onSkip = {},
            )
        }
    }

    @Test
    fun `o interruptor da notificacao vem desligado`() {
        conteudoDoPasso5(ligado = false)

        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `com o interruptor desligado as sub-opcoes nao aparecem`() {
        conteudoDoPasso5(ligado = false)

        compose.onNodeWithText(tituloMascarada()).assertDoesNotExist()
        compose.onNodeWithText(tituloAnonima()).assertDoesNotExist()
    }

    @Test
    fun `ligar o interruptor faz as duas sub-opcoes aparecerem`() {
        conteudoDoPasso5(ligado = true)

        compose.onNodeWithText(tituloMascarada()).assertExists()
        compose.onNodeWithText(tituloAnonima()).assertExists()
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun `com o interruptor ligado a identificacao mascarada e a selecionada`() {
        conteudoDoPasso5(ligado = true, identificacao = NotificationIdentification.MASKED)

        compose.onNodeWithText(tituloMascarada()).assertIsSelected()
        compose.onNodeWithText(tituloAnonima()).assertIsNotSelected()
    }

    @Test
    fun `escolher a identificacao anonima informa a escolha para quem chama`() {
        identificacaoEscolhida = null
        conteudoDoPasso5(ligado = true)

        compose.onNodeWithText(tituloAnonima()).performClick()

        assertEquals(NotificationIdentification.ANONYMOUS, identificacaoEscolhida)
    }

    @Test
    fun `negacao simples da permissao exibe a justificativa`() {
        conteudoDoPasso5(permissao = RuntimePermissionAsk.DENIED_ONCE)

        compose.onNodeWithText(justificativa()).assertExists()
    }

    @Test
    fun `antes do primeiro pedido a justificativa nao aparece`() {
        conteudoDoPasso5(permissao = RuntimePermissionAsk.NEVER_ASKED)

        compose.onNodeWithText(justificativa()).assertDoesNotExist()
    }

    @Test
    fun `tocar no interruptor pede para ligar, e nao grava nada por conta propria`() {
        pedidoDeLigar = null
        conteudoDoPasso5(ligado = false)

        compose.onNode(isToggleable()).performClick()

        assertEquals(true, pedidoDeLigar)
    }

    @Test
    fun `o interruptor da notificacao passa os dois eixos de toque`() {
        conteudoDoPasso5(ligado = false)

        compose.onNode(isToggleable())
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `as duas sub-opcoes passam os dois eixos de toque`() {
        conteudoDoPasso5(ligado = true)

        compose.onNodeWithText(tituloMascarada())
            .assertTouchHeightIsAtLeast(alvoDoCartao)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoDoCartao)
        compose.onNodeWithText(tituloAnonima())
            .assertTouchHeightIsAtLeast(alvoDoCartao)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoDoCartao)
    }

    // ------------------------------------------------------------------ passo 6

    private fun conteudoDoPasso6(
        papelDetido: Boolean,
        agenda: ContactsPermissionState = ContactsPermissionState.GRANTED,
        desconhecidos: OriginPolicy = OriginPolicy.BLOCK,
        contatos: OriginPolicy = OriginPolicy.RING,
        whitelist: OriginPolicy = OriginPolicy.NEVER_SILENCE,
    ) {
        compose.setContent {
            SummaryStepScreen(
                roleHeld = papelDetido,
                contactsPermission = agenda,
                unknownPolicy = desconhecidos,
                contactsPolicy = contatos,
                whitelistPolicy = whitelist,
                onFixRole = {},
                onGrantContacts = {},
                onFinish = {},
            )
        }
    }

    @Test
    fun `papel detido produz o titulo de tudo pronto e nenhuma acao de correcao`() {
        conteudoDoPasso6(papelDetido = true)

        compose.onNodeWithText(texto(R.string.onboarding_summary_title_ok)).assertExists()
        compose.onNodeWithText(texto(R.string.onboarding_summary_title_partial))
            .assertDoesNotExist()
        compose.onNodeWithText(rotuloCorrigir()).assertDoesNotExist()
    }

    @Test
    fun `papel ausente produz o titulo de quase pronto E a acao de correcao na primeira linha`() {
        conteudoDoPasso6(papelDetido = false)

        // As duas afirmacoes precisam viver no MESMO caso: afirmar so o titulo deixaria passar uma
        // tela que diz "quase pronto" e nao oferece conserto nenhum.
        compose.onNodeWithText(texto(R.string.onboarding_summary_title_partial)).assertExists()
        compose.onNodeWithText(texto(R.string.onboarding_summary_title_ok)).assertDoesNotExist()
        compose.onNodeWithText(rotuloCorrigir()).assertExists().assertHasClickAction()
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_role, R.string.onboarding_check_missing),
        ).assertExists()
    }

    @Test
    fun `agenda nao concedida produz a acao de permitir na segunda linha`() {
        conteudoDoPasso6(papelDetido = true, agenda = ContactsPermissionState.DENIED_ONCE)

        compose.onNodeWithText(rotuloPermitirAgenda()).assertExists().assertHasClickAction()
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_contacts, R.string.onboarding_check_missing),
        ).assertExists()
    }

    @Test
    fun `agenda concedida nao oferece a acao de permitir`() {
        conteudoDoPasso6(papelDetido = true, agenda = ContactsPermissionState.GRANTED)

        compose.onNodeWithText(rotuloPermitirAgenda()).assertDoesNotExist()
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_contacts, R.string.onboarding_check_granted),
        ).assertExists()
    }

    @Test
    fun `as linhas de politica exibem os rotulos das opcoes efetivamente escolhidas`() {
        conteudoDoPasso6(
            papelDetido = true,
            desconhecidos = OriginPolicy.SILENCE,
            contatos = OriginPolicy.NEVER_SILENCE,
            whitelist = OriginPolicy.BLOCK,
        )

        val desconhecidos = context.getString(
            R.string.state_label_with_value,
            texto(R.string.onboarding_check_unknown),
            texto(R.string.unknown_option_silence),
        )
        val origens = context.getString(
            R.string.state_label_with_value,
            texto(R.string.onboarding_check_origins),
            context.getString(
                R.string.state_label_with_value,
                texto(R.string.contacts_option_never_silence),
                texto(R.string.whitelist_option_block),
            ),
        )
        compose.onNodeWithContentDescription(desconhecidos).assertExists()
        compose.onNodeWithContentDescription(origens).assertExists()
    }

    @Test
    fun `o passo 6 nao oferece a acao de pular`() {
        conteudoDoPasso6(papelDetido = true)

        compose.onNodeWithText(texto(R.string.onboarding_skip)).assertDoesNotExist()
    }

    @Test
    fun `as quatro linhas anunciam rotulo e estado`() {
        conteudoDoPasso6(papelDetido = true, agenda = ContactsPermissionState.GRANTED)

        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_role, R.string.onboarding_check_granted),
        ).assertExists()
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_contacts, R.string.onboarding_check_granted),
        ).assertExists()
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_unknown, R.string.unknown_option_block),
        ).assertExists()
    }

    @Test
    fun `a acao de correcao e no focavel separado do no mesclado da linha`() {
        conteudoDoPasso6(papelDetido = false)

        // O no da linha anuncia rotulo e estado e NAO carrega clique nenhum: se o botao estivesse
        // dentro do no mesclado, o clique seria dele e o botao ficaria inalcancavel pelo leitor.
        compose.onNodeWithContentDescription(
            anuncioDaLinha(R.string.onboarding_check_role, R.string.onboarding_check_missing),
        ).assertHasNoClickAction()

        // O botao existe como no PROPRIO na arvore que o leitor de tela consome — a mesclada. Este
        // e o assert que tem dentes, e ele foi acrescentado depois de uma prova de vermelho falhar:
        // com o botao movido para dentro do no mesclado da linha, os dois asserts sobre a arvore NAO
        // MESCLADA continuavam VERDES, porque a arvore nao mesclada preserva o no do botao mesmo
        // quando o ancestral limpa a semantica. So a arvore mesclada mede o que o usuario alcanca.
        compose.onAllNodesWithText(rotuloCorrigir()).assertCountEquals(1)
        compose.onNodeWithText(rotuloCorrigir()).assertHasClickAction()

        // E o clique e do PROPRIO botao, nao herdado de um ancestral: alcancado na arvore NAO
        // MESCLADA, onde o no do botao aparece sozinho. Encontra-lo pelo texto nao provaria isso.
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true)
            .filterToOne(hasAnyDescendant(hasText(rotuloCorrigir())))
            .assertExists()
    }

    @Test
    fun `os dois botoes de acao das linhas passam os dois eixos de toque`() {
        conteudoDoPasso6(papelDetido = false, agenda = ContactsPermissionState.DENIED_ONCE)

        compose.onNodeWithText(rotuloCorrigir())
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
        compose.onNodeWithText(rotuloPermitirAgenda())
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
    }

    @Test
    fun `o botao de concluir passa os dois eixos de toque`() {
        conteudoDoPasso6(papelDetido = true)

        compose.onNodeWithText(texto(R.string.onboarding_finish))
            .performScrollTo()
            .assertTouchHeightIsAtLeast(alvoDoBotaoPrincipal)
            .assertTouchWidthIsAtLeast(alvoMinimo)
            .assertLayoutHeightIsAtLeast(alvoDoBotaoPrincipal)
    }
}

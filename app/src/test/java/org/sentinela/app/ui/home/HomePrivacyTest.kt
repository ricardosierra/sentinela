package org.sentinela.app.ui.home

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.phone.TestMetadata
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * A fronteira do numero na home.
 *
 * A mascara e produzida pela mascara UNICA do aplicativo sobre um numero conhecido, e o veredito vem
 * de uma varredura da arvore semantica INTEIRA — textos, descricoes de conteudo e descricoes de
 * estado de todos os nos. Varrer o codigo-fonte seria a prova errada: o que importa e o que a tela
 * COMPOE e o que o leitor de tela recebe, nao o que o arquivo escreve.
 *
 * A sequencia completa de digitos so pode existir nas telas de chamada e de discagem — a fronteira
 * herdada da Fase 6. Nesta tela ela nao existe nem por descuido, porque o estado nao a carrega.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HomePrivacyTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val util = TestMetadata.util()

    /** Numero de teste; e dado DO TESTE e nunca chega ao estado da tela em forma bruta. */
    private val numeroCompleto = "+5511912341234"
    private val digitosCompletos = "5511912341234"
    private val mascara = PhoneMask.mask(util, numeroCompleto)

    private val state = HomeUiState(
        protectionEnabled = true,
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        totalBlocked = StatValue.Loaded(7),
        blockedToday = StatValue.Loaded(2),
        lastBlocked = LastBlockedUi(
            maskedNumber = mascara,
            reasonLabelRes = R.string.history_unknown_number,
            timestampUtcMillis = AGORA - 60 * 60_000L,
        ),
        historyEnabled = true,
        readError = false,
    )

    private fun compor() {
        compose.setContent {
            SentinelaTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state = state,
                    onProtectionChange = {},
                    onFixRole = {},
                    onGrantContacts = {},
                    onOpenAppSettings = {},
                    onEnableHistory = {},
                    onRetryHistory = {},
                    onOpenSettings = {},
                    onOpenWhitelist = {},
                    onOpenHistory = {},
                    onOpenDialerActivation = {},
                    nowUtcMillis = AGORA,
                )
            }
        }
    }

    private fun varrerArvore(mesclada: Boolean): List<String> {
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

    @Test
    fun `a mascara do aplicativo esconde o meio do numero`() {
        assertEquals("+55 11 9****-1234", mascara)
    }

    @Test
    fun `a home exibe o texto mascarado da ultima bloqueada`() {
        compor()
        assertTrue(
            "a mascara $mascara tem de aparecer na tela",
            varrerArvore(mesclada = false).any { it.contains(mascara) },
        )
    }

    @Test
    fun `nenhum no da arvore contem a sequencia completa de digitos`() {
        compor()
        listOf(true, false).forEach { mesclada ->
            val vazamentos = varrerArvore(mesclada).filter { texto ->
                val digitos = texto.filter(Char::isDigit)
                digitos.contains(digitosCompletos) || texto.contains(numeroCompleto)
            }
            assertTrue(
                "a sequencia completa de digitos vazou para a tela (mesclada=$mesclada): $vazamentos",
                vazamentos.isEmpty(),
            )
        }
    }

    @Test
    fun `a descricao de conteudo da ultima bloqueada carrega mascara motivo e tempo`() {
        compor()
        val descricoes = varrerArvore(mesclada = true)
        val esperada = context.getString(
            R.string.dashboard_last_blocked_description,
            mascara,
            context.getString(R.string.history_unknown_number),
            context.resources.getQuantityString(R.plurals.time_hours_ago, 1, 1),
        )
        assertTrue("a leitura esperada era: $esperada", descricoes.any { it == esperada })
    }

    @Test
    fun `nenhum rotulo de risco ou classificacao aparece na home`() {
        compor()
        val proibidos = listOf("fraude", "risco", "spam", "golpe", "perigo")
        val achados = varrerArvore(mesclada = false).filter { texto ->
            proibidos.any { texto.lowercase().contains(it) }
        }
        assertTrue("o aplicativo nao classifica chamada: $achados", achados.isEmpty())
    }

    private companion object {
        const val AGORA = 1_700_000_000_000L
    }
}

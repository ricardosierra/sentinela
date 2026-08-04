package org.sentinela.app.ui.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.dialer.DialerActivationScreen
import org.sentinela.app.ui.navigation.Rotas

/**
 * A rota do modo discador nos cinco ramos de estado.
 *
 * Este e o caso que fecha a pendencia registrada em 06-05: a tela de ativacao existia, com os cinco
 * ramos provados, e nao tinha ponto de entrada nenhum no aplicativo. O que se prova aqui e o ponto de
 * entrada — que a linha da tela Protecao ALCANCA o destino nos quatro ramos em que ha algo a oferecer,
 * e que no ramo indisponivel ela e anunciada desabilitada e NAO navega.
 *
 * O ramo indisponivel e o que tem dentes: um aparelho que nao oferece o papel de telefone padrao
 * levaria o usuario a uma tela que so pode dizer que nada pode ser feito.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DialerRouteTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun texto(id: Int): String = context.getString(id)

    private lateinit var nav: NavHostController

    private fun comporGrafo(modo: DialerModeState) {
        compose.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Rotas.PROTECAO) {
                composable(Rotas.PROTECAO) {
                    TelaDeProtecao(
                        state = SettingsUiState(
                            screeningRoleHeld = true,
                            screeningRoleAvailable = true,
                            dialerMode = modo,
                            loading = false,
                        ),
                        onOpenDialerActivation = { nav.navigate(Rotas.MODO_DISCADOR) },
                    )
                }
                composable(Rotas.MODO_DISCADOR) {
                    DialerActivationScreen(
                        state = modo,
                        onRequestRole = { },
                        onRevert = { },
                        onGrantContacts = { },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tocarNaLinhaDoModoDiscador() {
        compose.onNodeWithText(texto(R.string.settings_dialer_mode))
            .performScrollTo()
            .performClick()
        compose.waitForIdle()
    }

    @Test
    fun `modo oferecido navega para a ativacao`() {
        comporGrafo(DialerModeState.OFFERED)

        tocarNaLinhaDoModoDiscador()

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
    }

    @Test
    fun `modo ativo navega para a ativacao`() {
        comporGrafo(DialerModeState.ACTIVE)

        tocarNaLinhaDoModoDiscador()

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
    }

    @Test
    fun `modo barrado pela agenda navega para a ativacao`() {
        comporGrafo(DialerModeState.BLOCKED_BY_CONTACTS)

        tocarNaLinhaDoModoDiscador()

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
    }

    @Test
    fun `papel perdido navega para a ativacao`() {
        comporGrafo(DialerModeState.ROLE_LOST)

        tocarNaLinhaDoModoDiscador()

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
    }

    @Test
    fun `modo indisponivel e anunciado desabilitado e nao navega`() {
        comporGrafo(DialerModeState.UNAVAILABLE)

        compose.onNodeWithText(texto(R.string.settings_dialer_mode))
            .performScrollTo()
            .assertIsNotEnabled()
        tocarNaLinhaDoModoDiscador()

        assertEquals(Rotas.PROTECAO, nav.currentDestination?.route)
    }

    @Test
    fun `voltar da ativacao devolve a tela Protecao`() {
        comporGrafo(DialerModeState.OFFERED)
        tocarNaLinhaDoModoDiscador()

        compose.runOnUiThread { nav.popBackStack() }
        compose.waitForIdle()

        assertEquals(Rotas.PROTECAO, nav.currentDestination?.route)
    }
}

/**
 * A tela Protecao real com os dezenove retornos de chamada; so o do modo discador interessa aqui, e os
 * outros dezoito ja tem suite propria.
 */
@Composable
private fun TelaDeProtecao(state: SettingsUiState, onOpenDialerActivation: () -> Unit) {
    SettingsScreen(
        state = state,
        onBack = { },
        onProtectionChange = { },
        onFixRole = { },
        onUnknownPolicy = { },
        onContactsPolicy = { },
        onWhitelistPolicy = { },
        onBlockPrivateChange = { },
        onBlockMode = { },
        onHideNativeLogChange = { },
        onNotificationChange = { },
        onNotificationIdentification = { },
        onOpenDialerActivation = onOpenDialerActivation,
        onRepeatedCallChange = { },
        onHistoryEnabledChange = { },
        onRetention = { },
        onClearHistory = { },
        onFallback = { },
        onOpenAbout = { },
        bottomBar = { },
    )
}

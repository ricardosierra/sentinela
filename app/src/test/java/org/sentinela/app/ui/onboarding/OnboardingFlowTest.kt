package org.sentinela.app.ui.onboarding

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.ui.dialer.DialerActivationScreen
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.home.HomeScreen
import org.sentinela.app.ui.home.HomeUiState
import org.sentinela.app.ui.home.StatValue
import org.sentinela.app.ui.navigation.PASSOS_DO_ONBOARDING
import org.sentinela.app.ui.navigation.Rotas
import org.sentinela.app.ui.navigation.irParaHome
import org.sentinela.app.ui.settings.SettingsScreen
import org.sentinela.app.ui.settings.SettingsUiState

/**
 * Fluxo de ponta a ponta do onboarding, provado em maquina virtual pura e sem emulador.
 *
 * O grafo composto aqui e o REAL em tudo o que este plano possui: as rotas de texto vem das
 * constantes, a ordem dos passos vem da lista de producao, o desvio de passo e a composta de producao
 * e o descarte inclusivo do "pular" vem da funcao de producao. O que NAO entra e o container de
 * dependencias — a Fase 5 mediu que um segundo container no mesmo processo derruba a aplicacao, e as
 * telas desta fase sao puras justamente para que o estado possa ser um dublê montado a mao.
 *
 * Qualificadores reais de tela sao obrigatorios: o aparelho padrao do Robolectric e pequeno demais e
 * reprova por conteudo fora do viewport, sem defeito nenhum (registro da Fase 6).
 *
 * Todo texto de assert e de toque vem de recurso lido em tempo de teste. Nunca literal, e nunca
 * posicao na tela: e o que faz do caso de travessia uma prova automatizada da parte com dentes do
 * criterio de leitor de tela.
 *
 * Leitura da pilha sempre com filtragem de nulo — a entrada do proprio grafo tem rota nula, medido em
 * 07-02 e nao suposto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun texto(id: Int): String = context.getString(id)

    private lateinit var nav: NavHostController

    /** Estado do onboarding como dublê: papel concedido, que e o caminho felizes dos seis passos. */
    private val estadoInicial = OnboardingUiState(
        screeningRoleHeld = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        settings = ScreeningSettings(),
    )

    private val estadoDaHome = HomeUiState(
        protectionEnabled = true,
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        totalBlocked = StatValue.Loaded(0),
        blockedToday = StatValue.Loaded(0),
    )

    private val estadoDaProtecao = SettingsUiState(
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        dialerMode = DialerModeState.OFFERED,
        loading = false,
    )

    /**
     * Compoe o grafo com os dez destinos, a partir do destino pedido.
     *
     * As acoes de passo sao as de producao em tudo o que e navegacao; o que sobra — pedir papel, pedir
     * agenda, gravar configuracao — nao pertence a este plano e e registrado para inspecao.
     */
    @Suppress("LongMethod")
    private fun comporGrafo(
        inicio: String = Rotas.BOAS_VINDAS,
        estado: OnboardingUiState = estadoInicial,
    ) {
        compose.setContent {
            var atual by remember { mutableStateOf(estado) }
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = inicio) {
                composable(Rotas.BOAS_VINDAS) {
                    WelcomeScreen(
                        onStart = { nav.navigate(Rotas.PASSO_PAPEL) },
                        onAbout = { sobreAberto = true },
                    )
                }
                PASSOS_DO_ONBOARDING.forEachIndexed { indice, rota ->
                    composable(rota) {
                        PassoDoOnboarding(
                            passo = indice,
                            estado = atual,
                            acoes = AcoesDoPasso(
                                pedirPapel = { papeisPedidos += 1 },
                                pedirAgenda = { agendasPedidas += 1 },
                                abrirConfiguracoes = { },
                                gravar = { transform -> atual = atual.copy(settings = transform(atual.settings)) },
                                alternarAviso = { },
                                avancar = {
                                    nav.navigate(
                                        PASSOS_DO_ONBOARDING[
                                            (indice + 1).coerceAtMost(PASSOS_DO_ONBOARDING.lastIndex),
                                        ],
                                    )
                                },
                                voltar = { nav.popBackStack() },
                                pular = { nav.irParaHome() },
                                concluir = { nav.irParaHome() },
                            ),
                        )
                    }
                }
                composable(Rotas.HOME) {
                    HomeScreen(
                        state = estadoDaHome,
                        onProtectionChange = { },
                        onFixRole = { },
                        onGrantContacts = { },
                        onOpenAppSettings = { },
                        onEnableHistory = { },
                        onRetryHistory = { },
                        onOpenSettings = { nav.navigate(Rotas.PROTECAO) },
                        onOpenWhitelist = { },
                        onOpenHistory = { },
                        onOpenDialerActivation = { nav.navigate(Rotas.MODO_DISCADOR) },
                        nowUtcMillis = 1000L,
                        bottomBar = { },
                        onAcceptRating = { },
                        onDismissRating = { },
                    )
                }
                composable(Rotas.PROTECAO) {
                    ProtecaoDeTeste(
                        state = estadoDaProtecao,
                        onBack = { nav.popBackStack() },
                        onOpenDialerActivation = { nav.navigate(Rotas.MODO_DISCADOR) },
                    )
                }
                composable(Rotas.MODO_DISCADOR) {
                    DialerActivationScreen(
                        state = DialerModeState.OFFERED,
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

    private var papeisPedidos = 0
    private var agendasPedidas = 0
    private var sobreAberto = false

    private fun pilha(): List<String> =
        nav.currentBackStack.value.mapNotNull { it.destination.route }

    /**
     * Toca num controle localizado SOMENTE pelo texto de recurso.
     *
     * A rolagem e tentada e o fracasso dela e tolerado de proposito: parte dos controles desta fase vive
     * em rodape fixo, fora de qualquer area rolavel, e exigir rolagem ali reprovaria por motivo falso. O
     * que nao e tolerado e o toque — se o no nao existir ou nao for clicavel, o caso fica vermelho.
     */
    private fun tocar(id: Int) {
        val no = compose.onNodeWithText(texto(id))
        runCatching { no.performScrollTo() }
        no.performClick()
        compose.waitForIdle()
    }

    private fun voltarComGesto() {
        compose.runOnUiThread { nav.popBackStack() }
        compose.waitForIdle()
    }

    /** Percorre boas-vindas ate o passo pedido tocando somente em controles de recurso. */
    private fun avancarAte(passo: Int) {
        tocar(R.string.welcome_cta)
        repeat(passo) { tocar(R.string.onboarding_next) }
    }

    // ----- os seis passos em ordem ---------------------------------------------------------------

    @Test
    fun `boas-vindas leva ao passo 1`() {
        comporGrafo()

        tocar(R.string.welcome_cta)

        assertEquals(Rotas.PASSO_PAPEL, nav.currentDestination?.route)
    }

    @Test
    fun `os seis passos avancam em ordem ate a home`() {
        comporGrafo()

        tocar(R.string.welcome_cta)
        PASSOS_DO_ONBOARDING.forEachIndexed { indice, rota ->
            assertEquals(rota, nav.currentDestination?.route)
            if (indice < PASSOS_DO_ONBOARDING.lastIndex) tocar(R.string.onboarding_next)
        }
        tocar(R.string.onboarding_finish)

        assertEquals(Rotas.HOME, nav.currentDestination?.route)
    }

    @Test
    fun `concluir no ultimo passo deixa a pilha com um unico elemento`() {
        comporGrafo()
        avancarAte(PASSOS_DO_ONBOARDING.lastIndex)

        tocar(R.string.onboarding_finish)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    // ----- pular, de cada um dos cinco passos que o oferecem -------------------------------------

    @Test
    fun `pular no passo 1 leva a home com a pilha de um unico elemento`() {
        comporGrafo()
        avancarAte(0)

        tocar(R.string.onboarding_skip)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    @Test
    fun `pular no passo 2 leva a home com a pilha de um unico elemento`() {
        comporGrafo()
        avancarAte(1)

        tocar(R.string.onboarding_skip)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    @Test
    fun `pular no passo 3 leva a home com a pilha de um unico elemento`() {
        comporGrafo()
        avancarAte(2)

        tocar(R.string.onboarding_skip)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    @Test
    fun `pular no passo 4 leva a home com a pilha de um unico elemento`() {
        comporGrafo()
        avancarAte(3)

        tocar(R.string.onboarding_skip)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    @Test
    fun `pular no passo 5 leva a home com a pilha de um unico elemento`() {
        comporGrafo()
        avancarAte(4)

        tocar(R.string.onboarding_skip)

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    // ----- o retorno que o produto proibe --------------------------------------------------------

    @Test
    fun `o gesto de voltar na home nao devolve o usuario ao onboarding`() {
        comporGrafo()
        avancarAte(2)
        tocar(R.string.onboarding_skip)

        // Antes do gesto a pilha ja tem um unico elemento: o descarte inclusivo apagou o onboarding.
        assertEquals(listOf(Rotas.HOME), pilha())

        voltarComGesto()

        // Depois do gesto NAO existe onboarding para onde voltar. O que o sistema faz a partir daqui e
        // encerrar a tela, e e exatamente esse o comportamento pedido: o gesto de voltar na home sai do
        // aplicativo, nunca reabre o fluxo que o usuario ja respondeu.
        assertTrue(
            "o gesto de voltar levou a um destino de onboarding",
            pilha().none { it in DESTINOS_DE_ONBOARDING },
        )
        assertTrue(
            "o destino corrente e de onboarding depois do gesto de voltar",
            nav.currentDestination?.route !in DESTINOS_DE_ONBOARDING,
        )
    }

    @Test
    fun `voltar no passo 4 devolve o passo 3`() {
        comporGrafo()
        avancarAte(3)

        tocar(R.string.onboarding_back)

        assertEquals(Rotas.PASSO_CONTATOS, nav.currentDestination?.route)
    }

    // ----- papel negado nao trava o fluxo --------------------------------------------------------

    @Test
    fun `papel negado no passo 1 permite avancar ate a home`() {
        comporGrafo(
            estado = estadoInicial.copy(screeningRoleHeld = false, roleDenied = true),
        )

        tocar(R.string.welcome_cta)
        assertEquals(Rotas.PASSO_PAPEL, nav.currentDestination?.route)
        repeat(PASSOS_DO_ONBOARDING.lastIndex) { tocar(R.string.onboarding_next) }
        tocar(R.string.onboarding_finish)

        assertEquals(Rotas.HOME, nav.currentDestination?.route)
    }

    @Test
    fun `papel negado anuncia o que o usuario perde sem barrar o passo`() {
        comporGrafo(
            estado = estadoInicial.copy(screeningRoleHeld = false, roleDenied = true),
        )
        tocar(R.string.welcome_cta)

        compose.onNodeWithText(texto(R.string.onboarding_role_denied)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.onboarding_next)).assertIsDisplayed()
    }

    // ----- home, Protecao e o modo discador ------------------------------------------------------

    @Test
    fun `da home o item de ajustes leva a Protecao e voltar retorna a home`() {
        comporGrafo(inicio = Rotas.HOME)

        tocar(R.string.nav_settings)
        assertEquals(Rotas.PROTECAO, nav.currentDestination?.route)

        voltarComGesto()

        assertEquals(Rotas.HOME, nav.currentDestination?.route)
    }

    @Test
    fun `de Protecao o item do modo discador leva a ativacao`() {
        comporGrafo(inicio = Rotas.PROTECAO)

        tocar(R.string.settings_dialer_mode)

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
    }

    // ----- a travessia inteira somente por texto de recurso --------------------------------------

    @Test
    fun `o caminho inteiro e percorrivel tocando somente em nos com texto de recurso`() {
        comporGrafo()

        tocar(R.string.welcome_cta)
        repeat(PASSOS_DO_ONBOARDING.lastIndex) { tocar(R.string.onboarding_next) }
        tocar(R.string.onboarding_finish)
        tocar(R.string.nav_settings)
        tocar(R.string.settings_dialer_mode)

        assertEquals(Rotas.MODO_DISCADOR, nav.currentDestination?.route)
        assertTrue("a pilha nao pode conter passo de onboarding", pilha().none { it.startsWith(PREFIXO_DE_PASSO) })
    }

    private companion object {
        const val PREFIXO_DE_PASSO = "passo_"

        /** Boas-vindas mais os seis passos: nenhum deles pode ser alcancavel a partir da home. */
        val DESTINOS_DE_ONBOARDING = PASSOS_DO_ONBOARDING + Rotas.BOAS_VINDAS
    }
}

/**
 * A tela Protecao com os dezenove retornos de chamada, dos quais so os dois de navegacao interessam
 * aqui. Os outros dezessete gravam configuracao e ja tem suite propria; anula-los mantem este arquivo
 * sobre o que ele prova, que e navegacao.
 */
@androidx.compose.runtime.Composable
private fun ProtecaoDeTeste(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenDialerActivation: () -> Unit,
) {
    SettingsScreen(
        state = state,
        onBack = onBack,
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

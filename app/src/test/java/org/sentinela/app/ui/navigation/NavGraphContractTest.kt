package org.sentinela.app.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R

/**
 * Contrato do grafo de navegacao da fase, provado COMPONDO o grafo de verdade.
 *
 * Esta suite existe porque o defeito que ela persegue e invisivel para o compilador: a alternativa
 * tipada da biblioteca de navegacao compila limpa neste repositorio e estoura na primeira composicao
 * do grafo, com falha de serializacao, porque o compilador de Kotlin embutido na ferramenta de build
 * nao traz o complemento de serializacao. Um assert de compilacao nao pega isso; um teste que compoe,
 * pega. Por isso nenhum caso aqui se satisfaz com o grafo ser declaravel — todos exigem que ele seja
 * COMPOSTO e navegado.
 *
 * Qualificadores reais de tela sao obrigatorios: o aparelho padrao do Robolectric e pequeno demais e
 * derruba asserts de exibicao por motivo falso (registro da Fase 6).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class NavGraphContractTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController

    /**
     * Cada destino da fase com um recurso de texto so para identifica-lo na arvore.
     *
     * O texto vem de recurso, nunca de literal em Kotlin, para que o teste obedeca a mesma regra de
     * copy do codigo de producao.
     */
    private val destinos: List<Pair<String, Int>> = listOf(
        Rotas.BOAS_VINDAS to R.string.welcome_cta,
        Rotas.PASSO_PAPEL to R.string.onboarding_role_cta,
        Rotas.PASSO_DESCONHECIDOS to R.string.settings_unknown_policy,
        Rotas.PASSO_CONTATOS to R.string.settings_contacts_policy,
        Rotas.PASSO_WHITELIST to R.string.settings_whitelist_policy,
        Rotas.PASSO_NOTIFICACAO to R.string.settings_show_notification,
        Rotas.PASSO_RESUMO to R.string.onboarding_finish,
        Rotas.HOME to R.string.nav_home,
        Rotas.PROTECAO to R.string.settings_title,
        Rotas.MODO_DISCADOR to R.string.settings_dialer_mode,
    )

    private fun comporGrafo() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Rotas.BOAS_VINDAS) {
                destinos.forEach { (rota, texto) ->
                    composable(rota) { Text(text = stringResource(texto)) }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Rotas visiveis na pilha, sem a entrada do proprio grafo — ela tem rota nula, medido. */
    private fun pilha(): List<String> =
        nav.currentBackStack.value.mapNotNull { it.destination.route }

    private fun navegar(bloco: NavHostController.() -> Unit) {
        compose.runOnUiThread { nav.bloco() }
        compose.waitForIdle()
    }

    @Test
    fun `o grafo compoe sem excecao e comeca em boas-vindas`() {
        comporGrafo()

        assertEquals(Rotas.BOAS_VINDAS, nav.currentDestination?.route)
    }

    @Test
    fun `toda rota do grafo e texto nao vazio`() {
        comporGrafo()

        val rotas = nav.graph.map { destino -> destino.route }
        rotas.forEach { rota ->
            assertNotNull("destino do grafo sem rota de texto", rota)
            assertTrue("destino do grafo com rota vazia", rota!!.isNotEmpty())
        }
    }

    @Test
    fun `a contagem de destinos do grafo esta travada em dez`() {
        comporGrafo()

        assertEquals(DESTINOS_ESPERADOS, nav.graph.count())
    }

    @Test
    fun `navegar para o primeiro passo troca o destino corrente`() {
        comporGrafo()

        navegar { navigate(Rotas.PASSO_PAPEL) }

        assertEquals(Rotas.PASSO_PAPEL, nav.currentDestination?.route)
    }

    @Test
    fun `ir para a home com descarte inclusivo deixa a pilha com um unico elemento`() {
        comporGrafo()
        navegar { navigate(Rotas.PASSO_PAPEL) }

        navegar {
            navigate(Rotas.HOME) {
                popUpTo(Rotas.BOAS_VINDAS) { inclusive = true }
                launchSingleTop = true
            }
        }

        assertEquals(listOf(Rotas.HOME), pilha())
    }

    @Test
    fun `voltar do primeiro passo devolve boas-vindas`() {
        comporGrafo()
        navegar { navigate(Rotas.PASSO_PAPEL) }

        navegar { popBackStack() }

        assertEquals(Rotas.BOAS_VINDAS, nav.currentDestination?.route)
    }

    private companion object {
        /**
         * Destino novo exige revisao de navegacao, no molde da contagem travada da Fase 2: a pilha e
         * o "pular onboarding" dependem de quais telas existem, e uma tela acrescentada em silencio
         * pode reintroduzir o retorno ao onboarding que o produto proibe.
         */
        const val DESTINOS_ESPERADOS = 10
    }
}

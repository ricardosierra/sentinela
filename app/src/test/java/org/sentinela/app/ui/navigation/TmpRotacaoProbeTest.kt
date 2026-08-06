package org.sentinela.app.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R

/**
 * SONDA TEMPORARIA (auditoria) — modela a raiz da interface: destino inicial resolvido de forma
 * assincrona (produceState) e so entao o grafo composto. Pergunta: apos recriacao da hospedeira
 * (rotacao), com o MESMO destino inicial "historico" vindo do extra grudado, a pilha volta ou nao?
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class TmpRotacaoProbeTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController

    private val destinos: List<Pair<String, Int>> = listOf(
        Rotas.HOME to R.string.nav_home,
        Rotas.HISTORICO to R.string.nav_history,
        Rotas.WHITELIST to R.string.nav_whitelist,
        Rotas.PROTECAO to R.string.settings_title,
    )

    @Composable
    private fun Raiz(chave: Long?, destinoResolvido: String) {
        // Mesma forma da RaizDaInterface: valor inicial nulo, chaveado pelo registro da notificacao.
        val destinoInicial by produceState<String?>(initialValue = null, chave) {
            value = destinoResolvido
        }
        val destino = destinoInicial
        if (destino == null) {
            Text(text = stringResource(R.string.state_loading))
        } else {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = destino) {
                destinos.forEach { (rota, texto) ->
                    composable(rota) { Text(text = stringResource(texto)) }
                }
            }
        }
    }

    private fun pilha(): List<String> =
        nav.currentBackStack.value.mapNotNull { it.destination.route }

    @Test
    fun `apos recriacao com o mesmo extra a pilha volta ou zera`() {
        val restauracao = StateRestorationTester(compose)
        restauracao.setContent {
            // extra da notificacao presente: destino inicial = historico
            Raiz(chave = 42L, destinoResolvido = Rotas.HISTORICO)
        }
        compose.waitForIdle()
        assertEquals(listOf(Rotas.HISTORICO), pilha())

        compose.runOnUiThread { nav.navigate(Rotas.WHITELIST) }
        compose.waitForIdle()
        assertEquals(listOf(Rotas.HISTORICO, Rotas.WHITELIST), pilha())

        // Rotacao: o extra continua grudado, entao o destino inicial recalculado e o mesmo.
        restauracao.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        assertEquals(
            "PILHA APOS ROTACAO",
            listOf(Rotas.HISTORICO, Rotas.WHITELIST),
            pilha(),
        )
    }
}

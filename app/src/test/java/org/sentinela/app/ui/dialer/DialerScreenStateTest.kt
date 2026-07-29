package org.sentinela.app.ui.dialer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpRect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.phone.TestMetadata
import org.sentinela.app.telecom.PlaceCallResult
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Comportamento da tela de discagem.
 *
 * Robolectric no nivel 35 (teto real do Java do projeto, licao da Fase 5) com a regra de teste de
 * composicao: o alvo aqui e o que o usuario ve e toca, nao o desenho.
 *
 * A formatacao usa os metadados REAIS do libphonenumber, pela fixture da Fase 2 — um formatador
 * dublado deixaria o caso de formatacao progressiva vacuoso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DialerScreenStateTest {

    @get:Rule
    val compose = createComposeRule()

    private val util = TestMetadata.util()

    private val formatador: (String) -> String = { digitos ->
        formatAsYouType(util, CascadingRegionProvider.DEFAULT_REGION, digitos)
    }

    private var originadas = mutableListOf<String>()

    private fun montar(
        initialNumber: String = "",
        resultado: PlaceCallResult = PlaceCallResult.Placed,
        suggestion: String? = null,
    ) {
        compose.setContent {
            SentinelaTheme(darkTheme = true, dynamicColor = false) {
                DialpadScreen(
                    initialNumber = initialNumber,
                    formatNumber = formatador,
                    suggestionFor = { digitos -> suggestion?.takeIf { digitos.isNotEmpty() } },
                    placeCall = { digitos ->
                        originadas += digitos
                        resultado
                    },
                )
            }
        }
    }

    private fun tecla(digito: String) = compose.onNodeWithContentDescription(
        DESCRICOES_DE_TECLA.getValue(digito),
    )

    private fun botaoDeLigar() =
        compose.onNodeWithContentDescription(LIGAR_PARA, substring = true)

    private fun boundsDoLigar(): DpRect = botaoDeLigar().getUnclippedBoundsInRoot()

    @Test
    fun `campo vazio deixa o botao de ligar desabilitado`() {
        montar()

        botaoDeLigar().assertIsNotEnabled()
    }

    @Test
    fun `o primeiro digito habilita o botao de ligar`() {
        montar()

        tecla("1").performClick()

        botaoDeLigar().assertIsEnabled()
    }

    @Test
    fun `digitar formata progressivamente em pt-BR`() {
        montar()

        "11912345678".forEach { digito -> tecla(digito.toString()).performClick() }

        compose.onNodeWithText(FORMATADO).assertIsDisplayed()
    }

    @Test
    fun `apagar remove um digito`() {
        montar(initialNumber = "119")

        compose.onNodeWithContentDescription(APAGAR).performClick()

        compose.onNodeWithText("11").assertIsDisplayed()
    }

    @Test
    fun `toque longo em apagar limpa o numero todo`() {
        montar(initialNumber = "11912345678")

        compose.onNodeWithContentDescription(APAGAR).performTouchInput { longClick() }

        // Campo vazio: o botao de ligar volta a ficar desabilitado.
        botaoDeLigar().assertIsNotEnabled()
    }

    @Test
    fun `com o campo vazio o apagar sai da arvore de acessibilidade mas nao move o ligar`() {
        montar(initialNumber = "119")
        val comDigitos = boundsDoLigar()

        compose.onNodeWithContentDescription(APAGAR).performClick()
        compose.onNodeWithContentDescription(APAGAR).performClick()
        compose.onNodeWithContentDescription(APAGAR).performClick()

        compose.onNodeWithContentDescription(APAGAR).assertDoesNotExist()
        assertEquals(comDigitos, boundsDoLigar())
    }

    @Test
    fun `toque longo na tecla zero insere o sinal de mais`() {
        montar()

        tecla("0").performTouchInput { longClick() }

        compose.onNodeWithText(MAIS).assertIsDisplayed()
    }

    @Test
    fun `toque em ligar origina a chamada com o numero digitado`() {
        montar(initialNumber = "11912345678")

        botaoDeLigar().performClick()

        assertEquals(listOf("11912345678"), originadas)
    }

    @Test
    fun `falha ao originar mostra a mensagem e mantem o numero no campo`() {
        montar(
            initialNumber = "11912345678",
            resultado = PlaceCallResult.PlatformFailure("falha_da_plataforma"),
        )

        botaoDeLigar().performClick()

        compose.onNodeWithText(FALHA).assertIsDisplayed()
        compose.onNodeWithText(TENTAR_DE_NOVO).assertIsDisplayed()
        compose.onNodeWithText(FORMATADO).assertIsDisplayed()
    }

    @Test
    fun `numero pre-preenchido pela intencao aparece formatado sem discar sozinho`() {
        montar(initialNumber = "11912345678")

        compose.onNodeWithText(FORMATADO).assertIsDisplayed()
        assertEquals(emptyList<String>(), originadas)
    }

    @Test
    fun `sugestao de nome aparece quando o numero casa com contato ou lista pessoal`() {
        montar(initialNumber = "11912345678", suggestion = SUGESTAO)

        compose.onNodeWithText(SUGESTAO).assertIsDisplayed()
    }

    private companion object {
        const val FORMATADO = "(11) 91234-5678"
        const val MAIS = "+"
        const val SUGESTAO = "Maria"
        const val LIGAR_PARA = "Ligar para"
        const val APAGAR = "Apagar último dígito"
        const val FALHA = "Não foi possível iniciar a chamada."
        const val TENTAR_DE_NOVO = "Tentar de novo"

        val DESCRICOES_DE_TECLA = mapOf(
            "0" to "Tecla zero",
            "1" to "Tecla um",
            "2" to "Tecla dois",
            "3" to "Tecla três",
            "4" to "Tecla quatro",
            "5" to "Tecla cinco",
            "6" to "Tecla seis",
            "7" to "Tecla sete",
            "8" to "Tecla oito",
            "9" to "Tecla nove",
        )
    }
}

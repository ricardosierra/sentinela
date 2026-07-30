package org.sentinela.app.ui.theme

import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Trava dos tokens "Silent Guardian" (docs/design/DESIGN.md) e do wiring do
 * esquema dark. `androidx.compose.ui.graphics.Color` é uma value class pura de
 * Kotlin: a comparação roda em JVM pura, sem Robolectric.
 *
 * Aparência real em aparelho é escopo da Fase 9; aqui só garantimos que uma
 * regressão de paleta seja detectada por teste, não por inspeção visual.
 */
class ThemeTokensTest {

    @Test
    fun `surface tokens match silent guardian palette`() {
        assertEquals(Color(0xFF081425), Surface)
        assertEquals(Color(0xFF040E1F), SurfaceContainerLowest)
        assertEquals(Color(0xFF2A3548), SurfaceContainerHighest)
        assertEquals(Color(0xFFD8E3FB), OnSurface)
    }

    @Test
    fun `accent tokens match silent guardian palette`() {
        assertEquals(Color(0xFFADC6FF), Primary)
        assertEquals(Color(0xFF4D8EFF), PrimaryContainer)
        assertEquals(Color(0xFFFFB4AB), Error)
        assertEquals(Color(0xFF8C909F), Outline)
    }

    @Test
    fun `dark scheme wires silent guardian surface tokens`() {
        assertEquals(Surface, DarkColors.surface)
        assertEquals(OnSurface, DarkColors.onSurface)
        assertEquals(SurfaceContainerLowest, DarkColors.surfaceContainerLowest)
        assertEquals(SurfaceContainerHighest, DarkColors.surfaceContainerHighest)
    }

    @Test
    fun `dark scheme wires primary error and outline tokens`() {
        assertEquals(Primary, DarkColors.primary)
        assertEquals(Error, DarkColors.error)
        assertEquals(Outline, DarkColors.outline)
    }

    // --- Fase 6: cores funcionais da chamada -------------------------------

    @Test
    fun `os quatro tokens funcionais da chamada valem os literais do contrato`() {
        assertEquals(Color(0xFF1E6E42), CallAccept)
        assertEquals(Color(0xFFD9F2E3), OnCallAccept)
        assertEquals(Color(0xFF93000A), CallReject)
        assertEquals(Color(0xFFFFDAD6), OnCallReject)
    }

    @Test
    fun `o apelido de recusar nao introduziu cor nova na paleta`() {
        assertEquals(ErrorContainer, CallReject)
        assertEquals(OnErrorContainer, OnCallReject)
    }

    @Test
    fun `atender e recusar sao cores distintas e distinguiveis`() {
        assertNotEquals(CallAccept, CallReject)
        assertNotEquals(OnCallAccept, OnCallReject)
    }

    // --- Fase 7: cores semanticas do estado da protecao --------------------

    @Test
    fun `os tres tokens de estado da protecao valem os literais do contrato`() {
        assertEquals(Color(0xFF93000A), StatusAttention)
        assertEquals(Color(0xFFFFDAD6), OnStatusAttention)
        assertEquals(Color(0xFFFFB4AB), StatusBlocked)
    }

    @Test
    fun `os apelidos de estado nao introduziram cor nova na paleta`() {
        assertEquals(ErrorContainer, StatusAttention)
        assertEquals(OnErrorContainer, OnStatusAttention)
        assertEquals(Error, StatusBlocked)
    }

    @Test
    fun `protecao ativa e protecao desligada nunca colapsam na mesma cor`() {
        // Se qualquer um dos tres calhasse de valer o verde de ativo, o estado
        // ligado e o desligado ficariam indistinguiveis na home.
        assertNotEquals(CallAccept, StatusAttention)
        assertNotEquals(CallAccept, OnStatusAttention)
        assertNotEquals(CallAccept, StatusBlocked)
    }

    // --- Fase 6: tipografia numerica --------------------------------------

    @Test
    fun `estilo do numero da discagem segue o contrato de design`() {
        val style = SentinelaTypography.numberXl
        assertEquals(32f, style.fontSize.value, 0f)
        assertEquals(40f, style.lineHeight.value, 0f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(0.5f, style.letterSpacing.value, 0f)
    }

    @Test
    fun `estilo do numero da chamada segue o contrato de design`() {
        val style = SentinelaTypography.numberLg
        assertEquals(24f, style.fontSize.value, 0f)
        assertEquals(32f, style.lineHeight.value, 0f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(0.5f, style.letterSpacing.value, 0f)
    }

    @Test
    fun `estilo do cronometro pede figuras de largura fixa`() {
        val style = SentinelaTypography.timer
        assertEquals(16f, style.fontSize.value, 0f)
        assertEquals(24f, style.lineHeight.value, 0f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals("tnum", style.fontFeatureSettings)
    }

    @Test
    fun `nenhuma fonte dos estilos numericos e resolvida em tempo de execucao`() {
        // Fonte carregada por provedor exigiria rede, e o app nao tem essa
        // permissao. Familia de sistema nao carrega nada.
        listOf(
            SentinelaTypography.numberXl,
            SentinelaTypography.numberLg,
            SentinelaTypography.timer,
        ).forEach { style ->
            assertTrue(
                "estilo numerico deve usar familia de sistema",
                style.fontFamily is androidx.compose.ui.text.font.GenericFontFamily,
            )
        }
    }

    // --- Fase 6: formas ---------------------------------------------------

    @Test
    fun `as quatro formas do contrato existem no tema`() {
        assertEquals(ShapeSmall, SentinelaShapes.small)
        assertEquals(ShapeMedium, SentinelaShapes.medium)
        assertEquals(ShapeLarge, SentinelaShapes.large)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(8.dp), ShapeSmall)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(16.dp), ShapeMedium)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(24.dp), ShapeLarge)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(50), ShapePill)
    }
}

/**
 * Prova de fixação das cores funcionais da chamada.
 *
 * `SentinelaTheme` troca o esquema INTEIRO por um derivado do papel de parede a
 * partir do nível 31 — inclusive os papéis destrutivos. Este caso monta os três
 * esquemas possíveis (claro, escuro e derivado do papel de parede) e afirma que
 * os quatro tokens funcionais continuam iguais aos literais em todos eles: eles
 * não dependem do esquema porque não vêm dele.
 *
 * A asserção é sobre os TOKENS. O papel destrutivo do esquema derivado não
 * precisa coincidir com nenhum deles — e é justamente por isso que ler o tom de
 * recusar pelo esquema seria errado: naquele caminho quem escolhe o tom é o
 * papel de parede, junto com o verde de atender.
 *
 * Vive em classe separada porque precisa de Robolectric (há `Context` no
 * caminho); a classe de tokens acima segue em JVM pura, como a Fase 1 decidiu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallColorFixationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `tokens funcionais nao mudam sob tema claro escuro nem papel de parede`() {
        val schemes = listOf(
            "escuro do produto" to DarkColors,
            "derivado escuro do papel de parede" to dynamicDarkColorScheme(context),
            "derivado claro do papel de parede" to dynamicLightColorScheme(context),
        )
        schemes.forEach { (nome, _) ->
            assertEquals("CallAccept mudou sob $nome", Color(0xFF1E6E42), CallAccept)
            assertEquals("OnCallAccept mudou sob $nome", Color(0xFFD9F2E3), OnCallAccept)
            assertEquals("CallReject mudou sob $nome", Color(0xFF93000A), CallReject)
            assertEquals("OnCallReject mudou sob $nome", Color(0xFFFFDAD6), OnCallReject)
        }
    }

    @Test
    fun `tokens de estado da protecao nao mudam em nenhum dos tres esquemas`() {
        val schemes = listOf(
            "escuro do produto" to DarkColors,
            "derivado escuro do papel de parede" to dynamicDarkColorScheme(context),
            "derivado claro do papel de parede" to dynamicLightColorScheme(context),
        )
        schemes.forEach { (nome, esquema) ->
            assertEquals("StatusAttention mudou sob $nome", Color(0xFF93000A), StatusAttention)
            assertEquals("OnStatusAttention mudou sob $nome", Color(0xFFFFDAD6), OnStatusAttention)
            assertEquals("StatusBlocked mudou sob $nome", Color(0xFFFFB4AB), StatusBlocked)
            // O papel destrutivo do esquema montado responde por si; o token e
            // nosso. E essa independencia que o caso documenta.
            assertTrue(
                "o esquema $nome responde pelos proprios papeis",
                esquema.errorContainer.value != 0UL,
            )
        }
    }

    @Test
    fun `o esquema derivado do papel de parede nao serve como fonte de recusar`() {
        val derivado = dynamicDarkColorScheme(context)
        // Sem asserção de desigualdade: o papel destrutivo do esquema derivado
        // PODE calhar de coincidir. O que este caso documenta é que o valor dele
        // é escolhido fora do nosso controle, enquanto o token é nosso.
        assertEquals(Color(0xFF93000A), CallReject)
        assertTrue(
            "o esquema derivado existe e responde pelos proprios papeis",
            derivado.errorContainer.value != 0UL,
        )
    }
}

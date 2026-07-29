package org.sentinela.app.phone

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato da mascara unica de exibicao (log e UI). Os valores esperados vem da tabela medida
 * em 02-RESEARCH.md; a propriedade de nao-vazamento vale para TODAS as entradas longas.
 */
class PhoneMaskTest {

    private val util: PhoneNumberUtil = TestMetadata.util()

    private fun mask(value: String) = PhoneMask.mask(util, value)

    @Test
    fun `celular BR usa o formato canonico do CLAUDE`() {
        // Formato canonico do CLAUDE.md, assertado literalmente: +55 11 9****-1234
        assertEquals("+55 11 9****-1234", mask("+5511987651234"))
    }

    @Test
    fun `celular BR da tabela medida preserva os quatro ultimos digitos`() {
        assertEquals("+55 11 9****-4321", mask("+5511987654321"))
    }

    @Test
    fun `fixo BR e mascarado com DDD preservado`() {
        assertEquals("+55 11 3****-4444", mask("+551133334444"))
    }

    @Test
    fun `numero dos EUA usa ndc de 3 digitos`() {
        assertEquals("+1 212 5****-0123", mask("+12125550123"))
    }

    @Test
    fun `numero do Reino Unido usa ndc de 2 digitos`() {
        assertEquals("+44 20 7****-8750", mask("+442071838750"))
    }

    @Test
    fun `0800 usa ndc de 3 digitos`() {
        assertEquals("+55 800 1****-4567", mask("+558001234567"))
    }

    @Test
    fun `codigo curto 190 e exibido na integra`() {
        assertEquals("190", mask("190"))
    }

    @Test
    fun `codigo curto 911 e exibido na integra`() {
        assertEquals("911", mask("911"))
    }

    @Test
    fun `fronteira do limiar - 123456 tem exatamente LIMIAR_CURTO digitos e nao e exibido`() {
        // LIMIAR_CURTO e comparado com `<` estrito, identico ao normalizer.
        assertEquals(PhoneNumbers.LIMIAR_CURTO, "123456".length)
        assertFalse(mask("123456") == "123456")
    }

    @Test
    fun `numero invalido sem ndc cai na forma degradada sem excecao`() {
        assertEquals("+55 ****-4321", mask("+55987654321"))
    }

    @Test
    fun `entradas nao numericas viram mascara generica`() {
        assertEquals(PhoneMask.MASCARA_GENERICA, mask("abc"))
        assertEquals(PhoneMask.MASCARA_GENERICA, mask(""))
        assertEquals(PhoneMask.MASCARA_GENERICA, mask("+999999"))
    }

    @Test
    fun `mascara nunca contem o nsn completo`() {
        val longos = listOf(
            "+5511987654321",
            "+551133334444",
            "+12125550123",
            "+442071838750",
            "+558001234567",
            "+5540041234",
        )
        longos.forEach { e164 ->
            val parsed = util.parse(e164, null)
            val nsn = util.getNationalSignificantNumber(parsed)
            val masked = mask(e164)
            assertFalse("vazou o NSN de $e164", masked.contains(nsn))
        }
    }

    @Test
    fun `mascara expoe no maximo cc + ndc + 5 digitos`() {
        val longos = listOf(
            "+5511987654321",
            "+551133334444",
            "+12125550123",
            "+442071838750",
            "+558001234567",
            "+5540041234",
        )
        longos.forEach { e164 ->
            val parsed = util.parse(e164, null)
            val ndc = util.getLengthOfNationalDestinationCode(parsed)
            val teto = parsed.countryCode.toString().length + ndc + 5
            val expostos = mask(e164).count(Char::isDigit)
            assertTrue("$e164 expos $expostos digitos (teto $teto)", expostos <= teto)
        }
    }

    @Test
    fun `mascara nunca lanca e nunca ecoa a entrada crua`() {
        val hostis = listOf(
            "",
            " ",
            "+",
            "++55",
            "55 11",
            "٠١",
            "9".repeat(400),
        )
        hostis.forEach { entrada ->
            // A chamada nao lancar ja e metade do contrato; a outra metade e nao vazar.
            val masked = mask(entrada)
            val digitos = PhoneNumberUtil.normalizeDigitsOnly(entrada)
            if (digitos.length >= PhoneNumbers.LIMIAR_CURTO) {
                assertFalse("vazou os digitos de: $entrada", masked.contains(digitos))
            }
        }
    }
}

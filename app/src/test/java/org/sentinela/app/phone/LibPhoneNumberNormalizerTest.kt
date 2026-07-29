package org.sentinela.app.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Casos da tabela MEDIDA em 02-RESEARCH.md (2026-07-29). Se um destes quebrar apos um bump do
 * libphonenumber, a tabela e que precisa ser remedida — nao o teste relaxado.
 */
class LibPhoneNumberNormalizerTest {

    private val util = TestMetadata.util()

    private fun normalizer(region: String? = "BR") =
        LibPhoneNumberNormalizer(util, RegionProvider { region })

    private fun valid(result: NormalizationResult): String {
        assertTrue("esperava Valid, veio $result", result is NormalizationResult.Valid)
        return (result as NormalizationResult.Valid).e164
    }

    private fun invalid(result: NormalizationResult): String {
        assertTrue("esperava Invalid, veio $result", result is NormalizationResult.Invalid)
        return (result as NormalizationResult.Invalid).reason
    }

    @Test
    fun `celular BR nacional vira E164`() {
        assertEquals("+5511987654321", valid(normalizer().normalize("11987654321")))
    }

    @Test
    fun `celular BR ja formatado vira E164`() {
        assertEquals("+5511987654321", valid(normalizer().normalize("+55 11 98765-4321")))
    }

    @Test
    fun `fixo BR vira E164`() {
        assertEquals("+551133334444", valid(normalizer().normalize("1133334444")))
    }

    @Test
    fun `0800 vira E164`() {
        assertEquals("+558001234567", valid(normalizer().normalize("0800 123 4567")))
    }

    @Test
    fun `4004 vira E164`() {
        assertEquals("+5540041234", valid(normalizer().normalize("40041234")))
    }

    @Test
    fun `DDI explicito vence a regiao resolvida`() {
        assertEquals("+12125550123", valid(normalizer().normalize("+1 212 555 0123")))
    }

    @Test
    fun `numero do Reino Unido vira E164`() {
        assertEquals("+442071838750", valid(normalizer().normalize("+44 20 7183 8750")))
    }

    @Test
    fun `regiao explicita no parametro sobrepoe o provider`() {
        assertEquals("+12125550123", valid(normalizer().normalize("2125550123", region = "US")))
    }

    @Test
    fun `regiao vinda do provider e usada quando o parametro e nulo`() {
        assertEquals("+12125550123", valid(normalizer("US").normalize("2125550123")))
    }

    @Test
    fun `provider sem regiao cai no fallback BR`() {
        assertEquals("+5511987654321", valid(normalizer(null).normalize("11987654321")))
    }

    @Test
    fun `numero BR sem DDD e rejeitado - inferir DDD envenenaria a whitelist`() {
        val reason = invalid(normalizer().normalize("987654321"))
        assertTrue(reason, reason.contains("sem_ddd") || reason.contains("invalido"))
    }

    @Test
    fun `texto nao numerico e rejeitado`() {
        assertTrue(invalid(normalizer().normalize("abc")).contains("nao_e_numero"))
    }

    @Test
    fun `entrada vazia e rejeitada`() {
        assertTrue(invalid(normalizer().normalize("")).contains("nao_e_numero"))
    }

    @Test
    fun `DDI inexistente e rejeitado`() {
        assertTrue(invalid(normalizer().normalize("+999999")).contains("ddi_invalido"))
    }

    @Test
    fun `mask delega a PhoneMask e devolve o formato canonico`() {
        assertEquals("+55 11 9****-1234", normalizer().mask("+5511987651234"))
    }

    // --- nationalDigits: a SEGUNDA sonda da consulta a agenda (Fase 4) ---------------------
    // Motivo medido em 04-RESEARCH.md: contato gravado em formato nacional de outra regiao fica
    // com o normalizado do provider nulo e NAO e alcancado por uma consulta iniciada com "+".

    @Test
    fun `nationalDigits de celular BR tira DDI e o mais`() {
        assertEquals("11912345678", normalizer().nationalDigits("+5511912345678"))
    }

    @Test
    fun `nationalDigits de numero dos EUA tira o DDI 1`() {
        assertEquals("4155552671", normalizer().nationalDigits("+14155552671"))
    }

    @Test
    fun `nationalDigits de fixo BR preserva o DDD`() {
        assertEquals("2132165498", normalizer().nationalDigits("+552132165498"))
    }

    @Test
    fun `nationalDigits de codigo curto e nulo`() {
        assertNull(normalizer().nationalDigits("190"))
    }

    @Test
    fun `nationalDigits de texto nao numerico e nulo e nunca lanca`() {
        assertNull(normalizer().nationalDigits("nao-e-numero"))
    }

    @Test
    fun `nationalDigits de entrada vazia e nulo`() {
        assertNull(normalizer().nationalDigits(""))
    }

    @Test
    fun `nationalDigits nunca devolve o mais nem espaco`() {
        val n = normalizer()
        listOf("+5511912345678", "+14155552671", "+442071838750").forEach { e164 ->
            val nacional = n.nationalDigits(e164)
            assertTrue("nacional inesperado: $nacional", nacional!!.matches(Regex("[0-9]+")))
        }
    }

    @Test
    fun `nenhum reason de Invalid contem dado pessoal`() {
        val entradas = listOf("987654321", "abc", "", "+999999", "1", "+55")
        val n = normalizer()
        entradas.forEach { raw ->
            val result = n.normalize(raw)
            if (result is NormalizationResult.Invalid) {
                assertTrue(
                    "reason com formato suspeito: ${result.reason}",
                    result.reason.matches(Regex("[a-z_]+")),
                )
            }
        }
    }
}

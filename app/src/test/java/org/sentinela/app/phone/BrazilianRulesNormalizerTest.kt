package org.sentinela.app.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As DUAS regras que libphonenumber nao cobre e que o usuario decidiu implementar a mao
 * (02-CONTEXT.md): o 9o digito do celular BR antigo e os codigos curtos de servico.
 * Cada uma tem caso negativo, porque e o caso negativo que protege a whitelist.
 */
class BrazilianRulesNormalizerTest {

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

    // ---------------------------------------------------------------- 9o digito

    @Test
    fun `celular BR de 8 digitos nacional ganha o nono digito`() {
        assertEquals("+5511987654321", valid(normalizer().normalize("1187654321")))
    }

    @Test
    fun `celular BR de 8 digitos com DDI ganha o nono digito`() {
        assertEquals("+5511987654321", valid(normalizer().normalize("+55 11 8765-4321")))
    }

    @Test
    fun `fixo BR iniciando em 2 nao recebe o nono digito`() {
        // Assinante comeca em 2: e fixo valido, a regra nao pode disparar.
        assertEquals("+551127654321", valid(normalizer().normalize("+55 11 2765-4321")))
    }

    @Test
    fun `correcao que nao revalida como MOBILE e recusada`() {
        // Guarda-corpo obrigatorio: inserir o 9 nao basta, libphonenumber precisa confirmar.
        // DDD 10 nao existe no Brasil — nem o original nem o corrigido sao validos.
        assertEquals("nono_digito_nao_revalida", invalid(normalizer().normalize("1087654321")))
    }

    @Test
    fun `outro DDD inexistente tambem e recusado apos a tentativa de correcao`() {
        assertEquals("nono_digito_nao_revalida", invalid(normalizer().normalize("2087654321")))
    }

    @Test
    fun `a regra do nono digito nao dispara fora do DDI 55`() {
        assertEquals("+12125550123", valid(normalizer().normalize("+1 212 555 0123")))
    }

    // ------------------------------------------------------------- codigos curtos

    @Test
    fun `190 e Valid com os digitos crus e nunca um E164 falso`() {
        val chave = valid(normalizer().normalize("190"))
        assertEquals("190", chave)
        assertNotEquals("+55190", chave)
    }

    @Test
    fun `911 e Valid com os digitos crus mesmo em outra regiao`() {
        assertEquals("911", valid(normalizer("US").normalize("911", region = "US")))
    }

    @Test
    fun `193 e whitelistavel com a mesma chave que o usuario digitou`() {
        assertEquals("193", valid(normalizer().normalize("193")))
    }

    @Test
    fun `fronteira do limiar - 123456 tem LIMIAR_CURTO digitos e nao e codigo curto`() {
        assertEquals(PhoneNumbers.LIMIAR_CURTO, "123456".length)
        val result = normalizer().normalize("123456")
        assertNotEquals(NormalizationResult.Valid("123456"), result)
    }

    @Test
    fun `normalize e mask compartilham LIMIAR_CURTO e concordam em 190`() {
        val n = normalizer()
        assertEquals(valid(n.normalize("190")), n.mask("190"))
    }
}

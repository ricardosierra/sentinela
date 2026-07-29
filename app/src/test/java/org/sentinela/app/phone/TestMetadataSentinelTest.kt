package org.sentinela.app.phone

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sentinela do Pitfall 2 da pesquisa: um MetadataLoader que devolve null para tudo
 * NAO faz o libphonenumber lancar — os testes de normalizacao passariam validando
 * comportamento errado. Estes asserts provam que os metadados REAIS carregaram.
 */
class TestMetadataSentinelTest {

    @Test
    fun `metadados reais carregam e validam celular BR em E164`() {
        val util = TestMetadata.util()
        assertTrue(util.isValidNumber(util.parse("+5511987654321", null)))
    }

    @Test
    fun `numero nacional BR e formatado para E164`() {
        val util = TestMetadata.util()
        val parsed = util.parse("11987654321", "BR")
        assertEquals("+5511987654321", util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164))
    }

    @Test
    fun `metadados fixos BR tambem estao presentes`() {
        val util = TestMetadata.util()
        assertTrue(util.isValidNumber(util.parse("1133334444", "BR")))
    }
}

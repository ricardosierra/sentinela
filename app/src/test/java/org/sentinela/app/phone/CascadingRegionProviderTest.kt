package org.sentinela.app.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cascata de resolução de região: aparelho → preferência do usuário → fallback.
 * Fakes escritos à mão via SAM da `fun interface RegionProvider` — sem MockK.
 */
class CascadingRegionProviderTest {

    private val nullDevice = RegionProvider { null }
    private val nullPreference = RegionProvider { null }

    private fun provider(
        device: String?,
        preference: String? = null,
        fallback: String = CascadingRegionProvider.DEFAULT_REGION,
    ) = CascadingRegionProvider(
        device = RegionProvider { device },
        userPreference = RegionProvider { preference },
        fallback = fallback,
    )

    @Test
    fun `regiao do aparelho em minusculo vira maiuscula`() {
        assertEquals("BR", provider(device = "br").currentRegion())
    }

    @Test
    fun `regiao do aparelho ja maiuscula e preservada`() {
        assertEquals("US", provider(device = "US").currentRegion())
    }

    @Test
    fun `espacos em volta da regiao do aparelho sao removidos`() {
        assertEquals("PT", provider(device = " pt ").currentRegion())
    }

    @Test
    fun `regiao vazia do aparelho cai para a preferencia do usuario`() {
        assertEquals("PT", provider(device = "", preference = "pt").currentRegion())
    }

    @Test
    fun `regiao so com espacos cai para a preferencia do usuario`() {
        assertEquals("PT", provider(device = "  ", preference = "pt").currentRegion())
    }

    @Test
    fun `regiao nula do aparelho cai para a preferencia do usuario`() {
        assertEquals("PT", provider(device = null, preference = "pt").currentRegion())
    }

    @Test
    fun `regiao com tres letras e invalida e cai para a preferencia`() {
        assertEquals("PT", provider(device = "ZZZ", preference = "pt").currentRegion())
    }

    @Test
    fun `regiao com digito e invalida e cai para a preferencia`() {
        assertEquals("PT", provider(device = "1A", preference = "pt").currentRegion())
    }

    @Test
    fun `preferencia do usuario em minusculo vira maiuscula`() {
        assertEquals("PT", provider(device = null, preference = "pt").currentRegion())
    }

    @Test
    fun `sem aparelho e sem preferencia usa o fallback padrao BR`() {
        assertEquals("BR", provider(device = null, preference = null).currentRegion())
    }

    @Test
    fun `preferencia vazia usa o fallback padrao BR`() {
        assertEquals("BR", provider(device = null, preference = "").currentRegion())
    }

    @Test
    fun `preferencia invalida usa o fallback padrao BR`() {
        assertEquals("BR", provider(device = null, preference = "ZZZ").currentRegion())
    }

    @Test
    fun `fallback customizado e respeitado`() {
        val custom = CascadingRegionProvider(
            device = nullDevice,
            userPreference = nullPreference,
            fallback = "US",
        )
        assertEquals("US", custom.currentRegion())
    }

    @Test
    fun `aparelho tem precedencia sobre a preferencia do usuario`() {
        assertEquals("US", provider(device = "us", preference = "pt").currentRegion())
    }

    @Test
    fun `DEFAULT_REGION e BR`() {
        assertEquals("BR", CascadingRegionProvider.DEFAULT_REGION)
    }
}

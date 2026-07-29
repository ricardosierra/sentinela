package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.Call
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.TestMetadata

/**
 * Robolectric aqui existe por causa de `Uri.parse` — a normalizacao em si roda em JVM pura com
 * os metadados reais do libphonenumber ([TestMetadata]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenedCallFactoryTest {

    private val normalizer = LibPhoneNumberNormalizer(
        TestMetadata.util(),
        RegionProvider { CascadingRegionProvider.DEFAULT_REGION },
    )

    private val factory = ScreenedCallFactory(normalizer)

    private fun screen(direction: Int = Call.Details.DIRECTION_INCOMING, handle: String?) =
        factory.from(fakeCallDetails(direction, handle?.let(Uri::parse)))

    @Test
    fun `direcao de entrada vira INCOMING`() {
        assertEquals(
            CallDirection.INCOMING,
            screen(Call.Details.DIRECTION_INCOMING, "tel:+5511999998888").direction,
        )
    }

    @Test
    fun `direcao de saida vira OUTGOING`() {
        assertEquals(
            CallDirection.OUTGOING,
            screen(Call.Details.DIRECTION_OUTGOING, "tel:+5511999998888").direction,
        )
    }

    @Test
    fun `direcao desconhecida cai no lado seguro INCOMING`() {
        assertEquals(
            CallDirection.INCOMING,
            screen(Call.Details.DIRECTION_UNKNOWN, "tel:+5511999998888").direction,
        )
    }

    @Test
    fun `handle nulo vira Private`() {
        assertEquals(ScreenedNumber.Private, screen(handle = null).number)
    }

    @Test
    fun `handle tel vazio vira Invalid`() {
        assertEquals(ScreenedNumber.Invalid, screen(handle = "tel:").number)
    }

    @Test
    fun `handle tel sem digito vira Invalid`() {
        assertEquals(ScreenedNumber.Invalid, screen(handle = "tel:abc").number)
    }

    @Test
    fun `handle em E164 vira Valid com a mesma chave`() {
        assertEquals(
            ScreenedNumber.Valid("+5511999998888"),
            screen(handle = "tel:+5511999998888").number,
        )
    }

    @Test
    fun `handle em grafia nacional e resolvido pela cascata de regiao`() {
        assertEquals(
            ScreenedNumber.Valid("+5511999998888"),
            screen(handle = "tel:11999998888").number,
        )
    }

    @Test
    fun `scheme diferente de tel vira Invalid`() {
        assertEquals(ScreenedNumber.Invalid, screen(handle = "sip:alguem@exemplo.org").number)
    }

    @Test
    fun `normalizador que lanca vira Invalid em vez de propagar`() {
        val explosivo = object : PhoneNumberNormalizer {
            override fun normalize(raw: String, region: String?): NormalizationResult =
                error("normalizador quebrado")

            override fun nationalDigits(e164: String): String? = null

            override fun mask(e164: String): String = ""
        }

        val resultado = ScreenedCallFactory(explosivo)
            .from(fakeCallDetails(Call.Details.DIRECTION_INCOMING, Uri.parse("tel:+5511999998888")))

        assertEquals(ScreenedNumber.Invalid, resultado.number)
        assertEquals(CallDirection.INCOMING, resultado.direction)
    }
}

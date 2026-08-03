package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.TelecomManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Origem de chamada de saida.
 *
 * Robolectric porque a montagem do endereco de telefone e a unica coisa da plataforma que esta
 * classe usa de verdade; o gerenciador de telecomunicacoes e dublado. Nivel 35 fixo, teto real do
 * Java do projeto (licao da Fase 5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DialerPlaceCallTest {

    private val telecom = mockk<TelecomManager>(relaxed = true)

    /** Normalizador dublado: um numero conhecido vale, qualquer outro nao. */
    private val normalizador = object : PhoneNumberNormalizer {
        override fun normalize(raw: String, region: String?): NormalizationResult =
            if (raw == NACIONAL) {
                NormalizationResult.Valid(E164)
            } else {
                NormalizationResult.Invalid("nao_e_numero")
            }

        override fun nationalDigits(e164: String): String? = NACIONAL

        override fun mask(e164: String): String = MASCARA
    }

    private fun placer(
        granted: Boolean = true,
        manager: TelecomManager? = telecom,
    ) = OutgoingCallPlacer(
        telecomManager = manager,
        normalizer = normalizador,
        callPhoneGranted = { granted },
    )

    @Test
    fun `numero valido com permissao concedida e originado`() {
        assertEquals(PlaceCallResult.Placed, placer().place(NACIONAL))
    }

    @Test
    fun `a chamada nasce no gerenciador de telecomunicacoes`() {
        placer().place(NACIONAL)

        verify(exactly = 1) { telecom.placeCall(any(), any()) }
    }

    @Test
    fun `o endereco usa o esquema de telefone e o numero normalizado`() {
        val endereco = slot<Uri>()
        every { telecom.placeCall(capture(endereco), any()) } returns Unit

        placer().place(NACIONAL)

        assertEquals("tel", endereco.captured.scheme)
        assertEquals(E164, endereco.captured.schemeSpecificPart)
    }

    @Test
    fun `sem a permissao concedida devolve falha e nao lanca`() {
        assertEquals(
            PlaceCallResult.PermissionMissing,
            placer(granted = false).place(NACIONAL),
        )
    }

    @Test
    fun `sem a permissao concedida nada e pedido a plataforma`() {
        placer(granted = false).place(NACIONAL)

        verify(exactly = 0) { telecom.placeCall(any(), any()) }
    }

    @Test
    fun `numero que nao normaliza devolve numero invalido sem tocar a plataforma`() {
        assertEquals(PlaceCallResult.InvalidNumber, placer().place("abc"))

        verify(exactly = 0) { telecom.placeCall(any(), any()) }
    }

    @Test
    fun `codigo ussd com asterisco ou cerquilha e bloqueado de imediato`() {
        assertEquals(PlaceCallResult.InvalidNumber, placer().place("*#06#"))

        verify(exactly = 0) { telecom.placeCall(any(), any()) }
    }

    @Test
    fun `aparelho sem gerenciador de telecomunicacoes devolve falha de plataforma`() {
        assertEquals(
            PlaceCallResult.PlatformFailure(OutgoingCallPlacer.SEM_TELEFONIA),
            placer(manager = null).place(NACIONAL),
        )
    }

    @Test
    fun `excecao de seguranca da plataforma vira falha e nao propaga`() {
        every { telecom.placeCall(any(), any()) } throws SecurityException("negada pelo sistema")

        assertEquals(
            PlaceCallResult.PlatformFailure(OutgoingCallPlacer.FALHA_DA_PLATAFORMA),
            placer().place(NACIONAL),
        )
    }

    private companion object {
        const val NACIONAL = "11912345678"
        const val E164 = "+5511912345678"
        const val MASCARA = "+55 11 9****-5678"
    }
}

package org.sentinela.app.telecom.call

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A costura entre a interface de chamada e a telefonia, verificada na PRÓPRIA costura.
 *
 * Motivo de existir: mudo e viva-voz falham SILENCIOSAMENTE. Pedir mudo a um serviço de chamada
 * que a plataforma não vinculou não estoura defeito nenhum — simplesmente não acontece nada. Por
 * isso um caso que apenas chama o método e conclui que "não estourou" não prova nada aqui; cada
 * caso abaixo afirma a DELEGAÇÃO, isto é, que o objeto de chamada ou o serviço de chamada
 * receberam exatamente o comando esperado, com exatamente o argumento esperado.
 *
 * O dublê do objeto de chamada e do serviço segue o mesmo recurso já usado no caso do serviço de
 * interface de chamada desta fase: nenhuma reflexão, nenhum objeto montado à mão.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [NIVEL_MODERNO])
class TelecomCallControlsTest {

    private val call = mockk<Call>(relaxed = true)
    private val service = mockk<InCallService>(relaxed = true)
    private val costura = TelecomCallControls(call, service)

    private fun rotas(mask: Int, speakerLigado: Boolean = false) {
        every { service.callAudioState } returns CallAudioState(
            false,
            if (speakerLigado) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
            mask,
        )
    }

    @Test
    fun `mudo ligado chega ao servico de chamada como mudo ligado`() {
        costura.setMuted(true)

        verify(exactly = 1) { service.setMuted(true) }
    }

    @Test
    fun `mudo desligado chega ao servico de chamada como mudo desligado`() {
        costura.setMuted(false)

        verify(exactly = 1) { service.setMuted(false) }
    }

    @Test
    fun `viva-voz ligado troca a rota de audio para o alto-falante`() {
        rotas(CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)

        costura.setSpeakerOn(true)

        verify(exactly = 1) { service.setAudioRoute(CallAudioState.ROUTE_SPEAKER) }
    }

    @Test
    fun `viva-voz desligado devolve a rota de audio ao fone`() {
        rotas(CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER, speakerLigado = true)

        costura.setSpeakerOn(false)

        verify(exactly = 1) { service.setAudioRoute(CallAudioState.ROUTE_EARPIECE) }
    }

    /** Máscara sem alto-falante: pedir a troca não teria efeito, então nem se pede. */
    @Test
    fun `sem alto-falante na mascara nenhuma troca de rota e pedida`() {
        rotas(CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_WIRED_HEADSET)

        costura.setSpeakerOn(true)

        verify(exactly = 0) { service.setAudioRoute(any()) }
    }

    /** Sem estado de áudio publicado ainda, não há máscara para consultar. */
    @Test
    fun `sem estado de audio nenhuma troca de rota e pedida`() {
        every { service.callAudioState } returns null

        costura.setSpeakerOn(true)

        verify(exactly = 0) { service.setAudioRoute(any()) }
    }

    /**
     * Caso medido no emulador desta fase: só o alto-falante é exposto. A troca continua sendo
     * pedida, porque a máscara de fato oferece a rota — o que a interface não deve oferecer, por
     * não haver segunda rota, é decidido no coordenador, não aqui.
     */
    @Test
    fun `mascara so com alto-falante ainda delega a troca de rota`() {
        rotas(CallAudioState.ROUTE_SPEAKER, speakerLigado = true)

        costura.setSpeakerOn(true)

        verify(exactly = 1) { service.setAudioRoute(CallAudioState.ROUTE_SPEAKER) }
    }

    @Test
    fun `atender chega ao objeto de chamada como chamada somente de voz`() {
        costura.answer()

        verify(exactly = 1) { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
    }

    @Test
    fun `encerrar chega ao objeto de chamada como desconexao`() {
        costura.hangUp()

        verify(exactly = 1) { call.disconnect() }
    }

    @Test
    fun `recusar declara o motivo quando a versao do aparelho oferece a sobrecarga`() {
        costura.reject()

        verify(exactly = 1) { call.reject(Call.REJECT_REASON_DECLINED) }
    }

    @Test
    fun `tom iniciado e encerrado chegam ao objeto de chamada em par`() {
        costura.playDtmf('5')
        costura.stopDtmf()

        verify(exactly = 1) { call.playDtmfTone('5') }
        verify(exactly = 1) { call.stopDtmfTone() }
    }

    @Test
    fun `mascara vazia nao oferece nenhuma rota`() {
        assertEquals(emptySet<CallAudioRoute>(), audioRoutesFromMask(0))
    }

    @Test
    fun `cada bit da mascara vira a rota nomeada correspondente`() {
        assertEquals(setOf(CallAudioRoute.FONE), audioRoutesFromMask(CallAudioState.ROUTE_EARPIECE))
        assertEquals(
            setOf(CallAudioRoute.VIVA_VOZ),
            audioRoutesFromMask(CallAudioState.ROUTE_SPEAKER),
        )
        assertEquals(
            setOf(CallAudioRoute.BLUETOOTH),
            audioRoutesFromMask(CallAudioState.ROUTE_BLUETOOTH),
        )
        assertEquals(
            setOf(CallAudioRoute.FONE_DE_OUVIDO),
            audioRoutesFromMask(CallAudioState.ROUTE_WIRED_HEADSET),
        )
    }

    @Test
    fun `mascara com varios bits vira o conjunto com todas as rotas`() {
        val mask = CallAudioState.ROUTE_EARPIECE or
            CallAudioState.ROUTE_SPEAKER or
            CallAudioState.ROUTE_BLUETOOTH or
            CallAudioState.ROUTE_WIRED_HEADSET

        assertEquals(
            setOf(
                CallAudioRoute.FONE,
                CallAudioRoute.VIVA_VOZ,
                CallAudioRoute.BLUETOOTH,
                CallAudioRoute.FONE_DE_OUVIDO,
            ),
            audioRoutesFromMask(mask),
        )
    }
}

/**
 * Abaixo do nível que aceita motivo declarado só existe a sobrecarga antiga. O ramo por versão
 * mora numa classe própria porque o nível da plataforma é configuração de execução do caso, não
 * argumento de método.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [NIVEL_ANTIGO])
class TelecomCallControlsRecusaAntigaTest {

    @Test
    fun `recusar usa a sobrecarga sem motivo em aparelho anterior`() {
        val call = mockk<Call>(relaxed = true)

        TelecomCallControls(call, mockk(relaxed = true)).reject()

        verify(exactly = 1) { call.reject(false, null) }
    }
}

private const val NIVEL_MODERNO = 35
private const val NIVEL_ANTIGO = 33

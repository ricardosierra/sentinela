package org.sentinela.app.telecom.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Códigos de estado da telefonia usados pelos testes, escritos à mão. */
internal const val TOCANDO_ENTRADA = 2
internal const val ATIVA = 4
internal const val DESCONECTADA = 7
internal const val DISCANDO = 1
internal const val EM_ESPERA = 3

internal const val AGORA_CHAMADA = 1_700_000_000_000L

/**
 * Dublê da costura que registra os comandos numa LISTA ORDENADA.
 *
 * Contador solto não distingue "tom iniciado e encerrado" de "dois tons iniciados": a ordem
 * é justamente o que precisa ser provado nesta fase. Cada método pode receber um defeito
 * injetado, usado pela matriz de falhas.
 */
internal class RecordingCallControls : CallControls {

    val eventos = mutableListOf<String>()
    var falharEm: String? = null

    private fun registrar(evento: String) {
        eventos += evento
        if (falharEm == evento.substringBefore(':')) {
            error("defeito injetado em $evento")
        }
    }

    override fun answer() = registrar("answer")
    override fun reject() = registrar("reject")
    override fun hangUp() = registrar("hangUp")
    override fun setMuted(muted: Boolean) = registrar("mute:$muted")
    override fun setSpeakerOn(on: Boolean) = registrar("speaker:$on")
    override fun playDtmf(digit: Char) = registrar("dtmfStart:$digit")
    override fun stopDtmf() = registrar("dtmfStop")
}

internal fun identidadeDesconhecida() = CallIdentity(
    fullNumber = "+5511999998888",
    origin = CallOrigin.DESCONHECIDO,
)

class CallSessionCoordinatorTest {

    private val controls = RecordingCallControls()
    private val coordenador = CallSessionCoordinator(
        controls = controls,
        clock = { AGORA_CHAMADA },
    )

    private fun chamadaRecebida() =
        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())

    @Test
    fun `chamada recebida produz estado de chamada recebida com a identidade`() {
        chamadaRecebida()
        assertEquals(CallUiState.Incoming, coordenador.state.value.state)
        assertEquals(CallOrigin.DESCONHECIDO, coordenador.state.value.identity.origin)
    }

    @Test
    fun `atender chega uma unica vez a costura e o estado ativo marca o inicio`() {
        chamadaRecebida()
        coordenador.answer()
        coordenador.onStateChanged(ATIVA)
        assertEquals(listOf("answer"), controls.eventos)
        assertEquals(CallUiState.Active, coordenador.state.value.state)
        assertEquals(AGORA_CHAMADA, coordenador.state.value.startedAtMillis)
    }

    @Test
    fun `recusar chega uma unica vez a costura`() {
        chamadaRecebida()
        coordenador.reject()
        assertEquals(listOf("reject"), controls.eventos)
    }

    @Test
    fun `atender numa chamada que nao esta tocando nao chega a costura`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.answer()
        coordenador.reject()
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `encerrar chega a costura em chamada ativa e tambem em estado nao suportado`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.hangUp()
        coordenador.onStateChanged(EM_ESPERA)
        assertEquals(CallUiState.Unsupported(EM_ESPERA), coordenador.state.value.state)
        coordenador.hangUp()
        assertEquals(listOf("hangUp", "hangUp"), controls.eventos)
    }

    @Test
    fun `encerrar nao chega a costura depois da chamada terminar`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.onStateChanged(DESCONECTADA)
        coordenador.hangUp()
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `mudo ligado e desligado altera o estado e chega a costura`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.toggleMute()
        assertTrue(coordenador.state.value.muted)
        coordenador.toggleMute()
        assertFalse(coordenador.state.value.muted)
        assertEquals(listOf("mute:true", "mute:false"), controls.eventos)
    }

    @Test
    fun `mudo ja ligado nao reenvia comando`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.setMuted(true)
        coordenador.setMuted(true)
        assertEquals(listOf("mute:true"), controls.eventos)
    }

    @Test
    fun `viva-voz ligado e desligado altera o estado e chega a costura`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.onAudioStateChanged(
            muted = false,
            speakerOn = false,
            supportedRoutes = setOf(CallAudioRoute.FONE, CallAudioRoute.VIVA_VOZ),
        )
        coordenador.toggleSpeaker()
        assertTrue(coordenador.state.value.speakerOn)
        coordenador.toggleSpeaker()
        assertFalse(coordenador.state.value.speakerOn)
        assertEquals(listOf("speaker:true", "speaker:false"), controls.eventos)
    }

    @Test
    fun `rota unica reporta viva-voz indisponivel e nao envia comando`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.onAudioStateChanged(
            muted = false,
            speakerOn = true,
            supportedRoutes = setOf(CallAudioRoute.VIVA_VOZ),
        )
        assertFalse(coordenador.state.value.speakerAvailable)
        coordenador.toggleSpeaker()
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `estado de audio da plataforma reflete mudo sem passar pela costura`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.onAudioStateChanged(
            muted = true,
            speakerOn = false,
            supportedRoutes = setOf(CallAudioRoute.FONE, CallAudioRoute.VIVA_VOZ),
        )
        assertTrue(coordenador.state.value.muted)
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `abrir e fechar o teclado altera o estado e nao envia nenhum tom`() {
        coordenador.onCallAdded(ATIVA, identidadeDesconhecida())
        coordenador.toggleKeypad()
        assertTrue(coordenador.state.value.keypadOpen)
        coordenador.toggleKeypad()
        assertFalse(coordenador.state.value.keypadOpen)
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `chamada removida encerra a sessao e fecha o teclado`() {
        coordenador.onCallAdded(DISCANDO, identidadeDesconhecida())
        coordenador.toggleKeypad()
        coordenador.onCallRemoved()
        assertEquals(CallUiState.Ended, coordenador.state.value.state)
        assertFalse(coordenador.state.value.keypadOpen)
    }
}

package org.sentinela.app.telecom.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Matriz de injeção de defeito na costura: um comando por vez, e a exceção precisa ESCAPAR.
 *
 * Este é o **inverso deliberado** da rede permissiva da triagem da Fase 5, e o motivo é
 * medição. Quando o processo do aplicativo morre no meio de uma chamada, o sistema de
 * telefonia detecta, registra a desconexão do nosso serviço e assume a chamada com o discador
 * do aparelho — a ligação não cai e a chamada seguinte volta para nós. Morrer, aqui, é seguro.
 *
 * O que não é seguro é continuar vivo e travado: uma interface vinculada e congelada não é
 * detectada por ninguém, o sistema não troca de discador e o usuário fica olhando uma tela
 * morta com a chamada acontecendo. Engolir exceção produziria exatamente essa situação — o
 * comando não teria efeito, o estado seguiria em frente inconsistente e nada avisaria.
 *
 * Cada teste afirma duas coisas: que a exceção subiu e que o estado observável **não** avançou
 * como se o comando tivesse funcionado.
 */
class CallSessionFailureTest {

    private val controls = RecordingCallControls()
    private val coordenador = CallSessionCoordinator(
        controls = controls,
        clock = { AGORA_CHAMADA },
    )

    private fun chamadaRecebida() =
        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())

    private fun emChamada() = coordenador.onCallAdded(ATIVA, identidadeDesconhecida())

    @Test
    fun `defeito ao atender propaga para quem chamou`() {
        chamadaRecebida()
        controls.falharEm = "answer"
        assertThrows(IllegalStateException::class.java) { coordenador.answer() }
        assertEquals(CallUiState.Incoming, coordenador.state.value.state)
    }

    @Test
    fun `defeito ao recusar propaga para quem chamou`() {
        chamadaRecebida()
        controls.falharEm = "reject"
        assertThrows(IllegalStateException::class.java) { coordenador.reject() }
    }

    @Test
    fun `defeito ao encerrar propaga para quem chamou`() {
        emChamada()
        controls.falharEm = "hangUp"
        assertThrows(IllegalStateException::class.java) { coordenador.hangUp() }
    }

    @Test
    fun `defeito ao mudar o mudo propaga e nao altera o estado`() {
        emChamada()
        controls.falharEm = "mute"
        assertThrows(IllegalStateException::class.java) { coordenador.toggleMute() }
        assertEquals(false, coordenador.state.value.muted)
    }

    @Test
    fun `defeito ao mudar o viva-voz propaga e nao altera o estado`() {
        emChamada()
        controls.falharEm = "speaker"
        assertThrows(IllegalStateException::class.java) { coordenador.toggleSpeaker() }
        assertEquals(false, coordenador.state.value.speakerOn)
    }

    @Test
    fun `defeito ao iniciar o tom do teclado propaga e nao registra o digito`() {
        emChamada()
        controls.falharEm = "dtmfStart"
        assertThrows(IllegalStateException::class.java) { coordenador.pressDigit('4') }
        assertEquals("", coordenador.state.value.sentDigits)
    }

    @Test
    fun `defeito ao encerrar o tom do teclado propaga`() {
        emChamada()
        coordenador.pressDigit('4')
        controls.falharEm = "dtmfStop"
        assertThrows(IllegalStateException::class.java) { coordenador.releaseDigit() }
    }

    @Test
    fun `defeito ao encerrar o tom durante a saida da sessao propaga`() {
        emChamada()
        coordenador.pressDigit('8')
        controls.falharEm = "dtmfStop"
        assertThrows(IllegalStateException::class.java) { coordenador.onCallRemoved() }
    }

    @Test
    fun `defeito na traducao de estado propaga`() {
        val quebrado = CallSessionCoordinator(
            controls = controls,
            mapper = { codigo -> error("defeito injetado na traducao de $codigo") },
            clock = { AGORA_CHAMADA },
        )
        assertThrows(IllegalStateException::class.java) {
            quebrado.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())
        }
        assertThrows(IllegalStateException::class.java) { quebrado.onStateChanged(ATIVA) }
    }
}

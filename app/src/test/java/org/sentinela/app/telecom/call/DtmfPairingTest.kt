package org.sentinela.app.telecom.call

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pareamento do tom de teclado: todo tom iniciado tem encerramento, e nenhum tom novo começa
 * antes de o anterior parar. As asserções são por ÍNDICE na sequência de eventos, porque
 * contagem não distingue par correto de dois tons abertos ao mesmo tempo.
 */
class DtmfPairingTest {

    private val controls = RecordingCallControls()
    private val coordenador = CallSessionCoordinator(
        controls = controls,
        clock = { AGORA_CHAMADA },
    )

    private fun emChamada() = coordenador.onCallAdded(ATIVA, identidadeDesconhecida())

    @Test
    fun `um digito produz o par iniciar e encerrar na ordem`() {
        emChamada()
        coordenador.pressDigit('5')
        coordenador.releaseDigit()
        assertEquals(2, controls.eventos.size)
        assertEquals("dtmfStart:5", controls.eventos[0])
        assertEquals("dtmfStop", controls.eventos[1])
    }

    @Test
    fun `digito novo sem encerrar o anterior encerra o anterior primeiro`() {
        emChamada()
        coordenador.pressDigit('1')
        coordenador.pressDigit('2')
        assertEquals(3, controls.eventos.size)
        assertEquals("dtmfStart:1", controls.eventos[0])
        assertEquals("dtmfStop", controls.eventos[1])
        assertEquals("dtmfStart:2", controls.eventos[2])
    }

    @Test
    fun `encerrar sem tom em curso nao envia nada`() {
        emChamada()
        coordenador.releaseDigit()
        coordenador.releaseDigit()
        assertEquals(emptyList<String>(), controls.eventos)
    }

    @Test
    fun `encerrar duas vezes o mesmo tom envia um unico encerramento`() {
        emChamada()
        coordenador.pressDigit('9')
        coordenador.releaseDigit()
        coordenador.releaseDigit()
        assertEquals(listOf("dtmfStart:9", "dtmfStop"), controls.eventos)
    }

    @Test
    fun `sair da sessao com um tom em curso encerra o tom`() {
        emChamada()
        coordenador.pressDigit('7')
        coordenador.onCallRemoved()
        assertEquals(listOf("dtmfStart:7", "dtmfStop"), controls.eventos)
    }

    @Test
    fun `transicao para encerrada com um tom em curso encerra o tom`() {
        emChamada()
        coordenador.pressDigit('0')
        coordenador.onStateChanged(DESCONECTADA)
        assertEquals(listOf("dtmfStart:0", "dtmfStop"), controls.eventos)
    }

    @Test
    fun `sequencia de tres digitos alterna inicio e encerramento sem sobreposicao`() {
        emChamada()
        listOf('1', '2', '3').forEach { digito ->
            coordenador.pressDigit(digito)
            coordenador.releaseDigit()
        }
        assertEquals(
            listOf(
                "dtmfStart:1", "dtmfStop",
                "dtmfStart:2", "dtmfStop",
                "dtmfStart:3", "dtmfStop",
            ),
            controls.eventos,
        )
        assertEquals("123", coordenador.state.value.sentDigits)
    }
}

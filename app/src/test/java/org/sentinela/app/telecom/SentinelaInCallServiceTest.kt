package org.sentinela.app.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.sentinela.app.SentinelaApp
import org.sentinela.app.telecom.call.CallSessionStore
import org.sentinela.app.telecom.call.CallUiState

/**
 * O serviço da interface de chamada precisa ser fino, e "fino" aqui é verificável: ele registra e
 * remove o observador da chamada, repassa o que a telefonia informa e não constrói container
 * próprio. Nenhum destes casos usa reflexão — a pesquisa desta fase mediu que ela é desnecessária,
 * e reflexão que apareça neste arquivo é sinal de desenho errado, não de teste esperto.
 *
 * Os casos evitam de propósito deixar uma chamada recebida sem confirmação de apresentação: o
 * vigia do plano 06-01 falharia ALTO em dois segundos, que é exatamente o comportamento desejado
 * em produção e ruído inútil aqui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SentinelaInCallServiceTest {

    private val store: CallSessionStore
        get() = (RuntimeEnvironment.getApplication() as SentinelaApp).container.callSessionStore

    private fun service(): SentinelaInCallService =
        Robolectric.buildService(SentinelaInCallService::class.java).create().get()

    private fun chamada(estado: Int, numero: String = "+5511999998888"): Call {
        val details = mockk<Call.Details>(relaxed = true)
        every { details.handle } returns android.net.Uri.fromParts("tel", numero, null)
        every { details.callerDisplayName } returns null
        every { details.handlePresentation } returns 1
        return mockk<Call>(relaxed = true) {
            every { state } returns estado
            every { this@mockk.details } returns details
        }
    }

    /** O espelho do armazém vive num escopo real; a espera é limitada e sem dormida fixa. */
    private fun aguardarEstado(esperado: CallUiState) {
        val limite = System.currentTimeMillis() + ESPERA_MAXIMA_MILLIS
        while (System.currentTimeMillis() < limite && store.state.value.state != esperado) {
            Thread.yield()
        }
        assertEquals(esperado, store.state.value.state)
    }

    @Test
    fun `receber a chamada registra o observador da chamada`() {
        val call = chamada(ESTADO_ATIVA)

        service().onCallAdded(call)

        verify(exactly = 1) { call.registerCallback(any()) }
    }

    @Test
    fun `perder a chamada remove o observador da chamada`() {
        val svc = service()
        val call = chamada(ESTADO_ATIVA)
        svc.onCallAdded(call)

        svc.onCallRemoved(call)

        verify(exactly = 1) { call.unregisterCallback(any()) }
    }

    @Test
    fun `receber a chamada publica um estado observavel no armazem`() {
        service().onCallAdded(chamada(ESTADO_ATIVA))

        assertEquals(CallUiState.Active, store.session?.state?.value?.state)
        aguardarEstado(CallUiState.Active)
    }

    @Test
    fun `perder a chamada limpa o armazem`() {
        val svc = service()
        val call = chamada(ESTADO_ATIVA)
        svc.onCallAdded(call)

        svc.onCallRemoved(call)

        assertNull(store.session)
        assertNull(store.controls)
        assertNull(store.opaqueCallId)
        assertEquals(CallUiState.Ended, store.state.value.state)
    }

    @Test
    fun `o armazem obtido pela aplicacao e o mesmo objeto em duas leituras`() {
        val app = RuntimeEnvironment.getApplication() as SentinelaApp

        assertSame(app.container.callSessionStore, app.container.callSessionStore)
    }

    @Test
    fun `o servico nao constroi container proprio e usa o da aplicacao`() {
        val call = chamada(ESTADO_ATIVA)

        service().onCallAdded(call)

        // Se o serviço tivesse construído um container próprio, a costura teria sido vinculada
        // num armazém invisível para a aplicação e este armazém continuaria vazio.
        assertNotNull(store.controls)
    }

    @Test
    fun `o objeto de chamada da plataforma nao sai da camada de telefonia`() {
        val numero = "+5511912345678"

        service().onCallAdded(chamada(ESTADO_ATIVA, numero))

        val opaco = store.opaqueCallId
        assertNotNull(opaco)
        assertNotEquals(numero, opaco)
        assertTrue(opaco!!.none { it == '+' })
    }

    @Test
    fun `mudanca de estado do audio chega ao armazem`() {
        val svc = service()
        svc.onCallAdded(chamada(ESTADO_ATIVA))

        svc.onCallAudioStateChanged(
            CallAudioState(
                true,
                CallAudioState.ROUTE_SPEAKER,
                CallAudioState.ROUTE_SPEAKER or CallAudioState.ROUTE_EARPIECE,
            ),
        )

        val retrato = store.session?.state?.value
        assertEquals(true, retrato?.muted)
        assertEquals(true, retrato?.speakerOn)
        assertEquals(true, retrato?.speakerAvailable)
    }

    @Test
    fun `chamada de saida discando chega ao armazem como discagem`() {
        service().onCallAdded(chamada(ESTADO_DISCANDO))

        assertEquals(CallUiState.Dialing, store.session?.state?.value?.state)
    }

    private companion object {
        const val ESTADO_DISCANDO = 1
        const val ESTADO_ATIVA = 4
        const val ESPERA_MAXIMA_MILLIS = 2_000L
    }
}

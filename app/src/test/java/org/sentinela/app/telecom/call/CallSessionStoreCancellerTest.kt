package org.sentinela.app.telecom.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressão: o cancelamento da notificação de chamada acontece no armazém, não apenas no
 * serviço da plataforma.
 *
 * Motivo: o `SentinelaInCallService.onCallRemoved` cancela o aviso, mas o estado terminal é
 * publicado pelo armazém antes disso. Se o processo for reciclado entre os dois momentos, o
 * serviço não chega a chamar `cancel()` e a notificação fica presa na barra. O armazém cancela
 * no instante da transição, independentemente do ciclo de vida do serviço.
 *
 * `advanceUntilIdle()` é necessário porque o `espelho` roda no despachante de teste, que não
 * avança automaticamente: a coleta do flow só ocorre depois de o despachante ser avançado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CallSessionStoreCancellerTest {

    private val controls = RecordingCallControls()
    private var cancelChamado = false
    private val canceller = CallNotificationCanceller { cancelChamado = true }

    private fun kotlinx.coroutines.test.TestScope.store() = CallSessionStore(
        scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        canceller = canceller,
    )

    @Test
    fun `transicao para Ended cancela a notificacao`() = runTest {
        val store = store()
        store.attach(controls)
        store.onCallAdded(ATIVA, identidadeDesconhecida(), "id1")

        store.onCallRemoved()
        advanceUntilIdle() // deixa o espelho processar o estado Ended

        assertEquals(true, cancelChamado)
    }

    @Test
    fun `transicao para Unsupported cancela a notificacao`() = runTest {
        val store = store()
        store.attach(controls)
        store.onCallAdded(ATIVA, identidadeDesconhecida(), "id1")

        store.onStateChanged(EM_ESPERA) // EM_ESPERA → Unsupported
        advanceUntilIdle() // deixa o espelho processar o estado Unsupported

        assertEquals(true, cancelChamado)
    }

    @Test
    fun `canceller nao e chamado mais de uma vez na mesma transicao terminal`() = runTest {
        var contagem = 0
        val store = CallSessionStore(
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            canceller = CallNotificationCanceller { contagem++ },
        )
        store.attach(controls)
        store.onCallAdded(ATIVA, identidadeDesconhecida(), "id1")

        store.onStateChanged(DESCONECTADA)
        advanceUntilIdle() // primeiro evento: cancel chamado → contagem = 1

        store.onCallRemoved() // segundo evento: mesmo estado Ended → flow não reemite (data class igual)
        advanceUntilIdle()

        assertEquals(1, contagem)
    }

    @Test
    fun `sem canceller configurado nao lanca excecao em estado terminal`() = runTest {
        val store = CallSessionStore(
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )
        store.attach(controls)
        store.onCallAdded(ATIVA, identidadeDesconhecida(), "id1")

        store.onCallRemoved()
        advanceUntilIdle()
        // Sem canceller — não deve lançar nada
    }
}

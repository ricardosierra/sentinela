package org.sentinela.app.telecom.call

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prazo de apresentação: uma chamada recebida que não consegue aparecer na tela dentro do
 * prazo falha alto, em vez de deixar o usuário diante de uma tela que nunca chega.
 *
 * Tempo aqui é relógio virtual do despachante de teste. Cronômetro de verdade não prova
 * estrutura e ainda mede o escalonador da máquina que roda o teste.
 */
class CallSessionWatchdogTest {

    private val controls = RecordingCallControls()

    @Test
    fun `prazo vencido sem confirmacao de apresentacao falha alto`() = runTest {
        var falha: Throwable? = null
        val coordenador = coordenador(falha = { falha = it })

        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS + 1)

        assertNotNull("o prazo deveria ter estourado", falha)
        assertTrue(falha is CallPresentationTimeoutException)
    }

    @Test
    fun `confirmacao dentro do prazo nao produz falha`() = runTest {
        var falha: Throwable? = null
        val coordenador = coordenador(falha = { falha = it })

        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS / 2)
        coordenador.confirmPresented()
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS * 2)

        assertNull(falha)
    }

    @Test
    fun `chamada de saida nao arma o prazo`() = runTest {
        var falha: Throwable? = null
        val coordenador = coordenador(falha = { falha = it })

        coordenador.onCallAdded(DISCANDO, identidadeDesconhecida())
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS * 2)

        assertNull(falha)
    }

    @Test
    fun `encerramento antes do prazo desarma o prazo`() = runTest {
        var falha: Throwable? = null
        val coordenador = coordenador(falha = { falha = it })

        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())
        coordenador.onStateChanged(DESCONECTADA)
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS * 2)

        assertNull(falha)
    }

    @Test
    fun `chamada removida antes do prazo desarma o prazo`() = runTest {
        var falha: Throwable? = null
        val coordenador = coordenador(falha = { falha = it })

        coordenador.onCallAdded(TOCANDO_ENTRADA, identidadeDesconhecida())
        coordenador.onCallRemoved()
        advanceTimeBy(PRESENTATION_DEADLINE_MILLIS * 2)

        assertNull(falha)
    }

    private fun kotlinx.coroutines.test.TestScope.coordenador(
        falha: (Throwable) -> Unit,
    ): CallSessionCoordinator {
        val escopo = CoroutineScope(
            StandardTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, erro -> falha(erro) },
        )
        return CallSessionCoordinator(
            controls = controls,
            clock = { AGORA_CHAMADA },
            scope = escopo,
        )
    }
}

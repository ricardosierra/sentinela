package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.domain.ContactLookup
import java.lang.reflect.Proxy

/**
 * Hospeda o `UnknownCallScreeningService` de verdade dentro da JVM e captura cada resposta que
 * ele entrega ao sistema.
 *
 * Como funciona: o Robolectric constroi o Service com um contexto valido e o harness troca, por
 * reflexao, o campo privado `mCallScreeningAdapter` por um `Proxy` da interface interna
 * `com.android.internal.telecom.ICallScreeningAdapter`. Toda chamada a `respondToCall` acaba em
 * `onScreeningResponse` nesse proxy, e o terceiro argumento e a resposta empacotada. E assim que
 * o harness enxerga tanto o conteudo da resposta quanto a quantidade de respostas emitidas — o
 * segundo ponto importa porque responder duas vezes nao lanca excecao nenhuma, apenas emite dois
 * IPCs em silencio.
 *
 * Ressalva deliberada: isto e codigo de teste apoiado num campo privado e numa interface interna
 * da plataforma, e pode quebrar numa atualizacao do Robolectric. Por isso a logica de verdade da
 * triagem nao mora no Service, e sim num colaborador puro (plano 05-03) que recebe a costura de
 * resposta por parametro. Se o harness quebrar, perdem-se poucos testes de ligacao, nunca a
 * suite de comportamento.
 *
 * O SDK fica fixado em 35 de proposito: 36 exige Java 21 e o projeto esta preso ao JDK 17.
 */
class ScreeningTestHarness {

    private val captured = mutableListOf<Any>()

    private val service: UnknownCallScreeningService by lazy { hostService() }

    /** Cada resposta emitida pelo Service, na ordem em que foi entregue ao sistema. */
    val responses: List<Any> get() = captured

    fun service(): UnknownCallScreeningService = service

    fun screen(direction: Int = Call.Details.DIRECTION_INCOMING, handle: Uri? = null) {
        service().onScreenCall(fakeCallDetails(direction, handle))
    }

    fun disallow(index: Int): Boolean = flag(index, "shouldDisallowCall")

    fun reject(index: Int): Boolean = flag(index, "shouldRejectCall")

    fun silence(index: Int): Boolean = flag(index, "shouldSilenceCall")

    fun skipCallLog(index: Int): Boolean = flag(index, "shouldSkipCallLog")

    fun skipNotification(index: Int): Boolean = flag(index, "shouldSkipNotification")

    private fun flag(index: Int, getter: String): Boolean {
        val response = captured[index]
        val method = response.javaClass.getMethod(getter).apply { isAccessible = true }
        return method.invoke(response) as Boolean
    }

    private fun hostService(): UnknownCallScreeningService {
        val svc = Robolectric.buildService(UnknownCallScreeningService::class.java).create().get()
        val adapterCls = Class.forName(ADAPTER_INTERFACE)
        val adapter = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(adapterCls),
        ) { _, method, args ->
            if (method.name == RESPONSE_CALLBACK) {
                args?.getOrNull(RESPONSE_ARG_INDEX)?.let { captured.add(it) }
            }
            null
        }
        CallScreeningService::class.java
            .getDeclaredField("mCallScreeningAdapter")
            .apply { isAccessible = true }
            .set(svc, adapter)
        return svc
    }

    private companion object {
        const val ADAPTER_INTERFACE = "com.android.internal.telecom.ICallScreeningAdapter"
        const val RESPONSE_CALLBACK = "onScreeningResponse"

        /** (callId, componentName, response) — a resposta e o terceiro argumento. */
        const val RESPONSE_ARG_INDEX = 2
    }
}

/**
 * Teste de fumaca do proprio harness: prova que a captura funciona, e que ela enxerga os cinco
 * campos da resposta. A chamada usada e a de um contato da agenda, que passa sem interferencia —
 * assim os cinco campos ficam falsos e o que esta sendo medido e a captura, nao a decisao.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreeningTestHarnessSmokeTest {

    private fun harness(): ScreeningTestHarness {
        val ambiente = AmbienteDeTriagem().apply { contatos.resultado = ContactLookup.HIT }
        return ScreeningTestHarness().also { it.service().dependencies = ambiente }
    }

    @Test
    fun `chamada de entrada produz exatamente uma resposta capturada`() {
        val harness = harness()

        harness.screen(handle = Uri.parse("tel:+5511999998888"))

        assertEquals(1, harness.responses.size)
    }

    @Test
    fun `a resposta capturada expoe os cinco campos da API`() {
        val harness = harness()

        harness.screen(handle = Uri.parse("tel:+5511999998888"))

        assertEquals(false, harness.disallow(0))
        assertEquals(false, harness.reject(0))
        assertEquals(false, harness.silence(0))
        assertEquals(false, harness.skipCallLog(0))
        assertEquals(false, harness.skipNotification(0))
    }
}

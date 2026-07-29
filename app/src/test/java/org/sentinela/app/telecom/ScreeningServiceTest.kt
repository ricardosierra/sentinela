package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.Call
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.settings.ScreeningSettings

/**
 * Prova de ligação do serviço de verdade: o que o sistema recebe é exatamente o que o motor
 * decidiu, uma única vez por chamada.
 *
 * A afirmação é sempre sobre a resposta capturada pelo adaptador real, nunca sobre uma variável
 * interna do serviço. Contar as respostas importa tanto quanto conferir o conteúdo: responder
 * duas vezes não lança exceção nenhuma, apenas emite dois avisos em silêncio, e só a contagem
 * denuncia isso.
 *
 * O SDK fica em 35 porque 36 exige Java 21 e o projeto está preso ao JDK 17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreeningServiceTest {

    private val ambiente = AmbienteDeTriagem()

    private fun harness(): ScreeningTestHarness = ScreeningTestHarness().also {
        it.service().dependencies = ambiente
    }

    @Test
    fun `numero desconhecido e barrado com uma unica resposta`() {
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(1, harness.responses.size)
        assertTrue(harness.disallow(0))
        assertTrue(harness.reject(0))
        assertTrue(harness.skipNotification(0))
    }

    @Test
    fun `chamada de quem esta na agenda toca`() {
        ambiente.contatos.resultado = ContactLookup.HIT
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(1, harness.responses.size)
        assertEquals(false, harness.disallow(0))
        assertEquals(false, harness.reject(0))
        assertEquals(false, harness.silence(0))
    }

    @Test
    fun `chamada de saida nao produz resposta nenhuma`() {
        val harness = harness()

        harness.screen(direction = Call.Details.DIRECTION_OUTGOING, handle = Uri.parse(TEL))

        assertEquals(0, harness.responses.size)
    }

    @Test
    fun `chamada sem identificacao responde uma vez e nao derruba o servico`() {
        val harness = harness()

        harness.screen(handle = null)

        assertEquals(1, harness.responses.size)
        assertTrue(harness.disallow(0))
    }

    @Test
    fun `falha interna ainda responde uma vez, e de forma permissiva`() {
        val ambienteComDefeito = AmbienteDeTriagem(motorComDefeito = true)
        val harness = ScreeningTestHarness().also { it.service().dependencies = ambienteComDefeito }

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(1, harness.responses.size)
        assertEquals(false, harness.disallow(0))
    }

    @Test
    fun `duas chamadas seguidas produzem duas respostas, uma para cada`() {
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))
        harness.screen(handle = Uri.parse(TEL))

        assertEquals(2, harness.responses.size)
    }

    @Test
    fun `historico guarda a chamada barrada quando esta habilitado`() {
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(1, ambiente.historico.gravados.size)
        assertEquals(MASCARA_DE_TESTE, ambiente.historico.gravados.first().maskedNumber)
    }

    @Test
    fun `nada e guardado quando a chamada apenas passa`() {
        ambiente.contatos.resultado = ContactLookup.HIT
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(0, ambiente.historico.gravados.size)
    }

    @Test
    fun `notificacao propria nao acontece com o interruptor desligado`() {
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(0, ambiente.notificador.enviadas.size)
    }

    @Test
    fun `notificacao propria acontece com o interruptor ligado, depois da resposta`() {
        ambiente.settings.valor = ScreeningSettings(showOwnNotification = true)
        ambiente.historico.idGravado = ID_GRAVADO
        val harness = harness()

        harness.screen(handle = Uri.parse(TEL))

        assertEquals(1, harness.responses.size)
        assertEquals(1, ambiente.notificador.enviadas.size)
        assertEquals(ID_GRAVADO, ambiente.notificador.enviadas.first().id)
        assertTrue(ambiente.notificador.enviadas.first().notificationShown)
    }

    private companion object {
        const val TEL = "tel:+5511999998888"
        const val ID_GRAVADO = 42L
    }
}

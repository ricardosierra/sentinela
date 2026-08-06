package org.sentinela.app.data.contacts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.TestMetadata
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Prazo da sonda da agenda.
 *
 * O defeito que estes testes trancam: `probe` é uma chamada BLOQUEANTE ao provider de contatos, sem
 * ponto de suspensão. Cancelamento de corrotina é cooperativo, então o `withTimeout` de um segundo
 * do coordenador de triagem não a interrompia — com o provider travado, a resposta ao sistema de
 * telefonia furava o limite de cinco segundos da plataforma e a triagem inteira era descartada.
 *
 * Estes testes usam relógio REAL de propósito. Com relógio virtual (`runTest`) a espera seria
 * instantânea e o teste passaria mesmo com a sonda presa — provaria o oposto do que se quer.
 */
class ContactProbeDeadlineTest {

    private val normalizer =
        LibPhoneNumberNormalizer(TestMetadata.util(), RegionProvider { "BR" })

    private val celular = "+5511987654321"

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Solta a sonda presa mesmo se o teste falhar, para nenhuma thread ficar pendurada. */
    private val liberacao = CountDownLatch(1)

    @After
    fun soltar() {
        liberacao.countDown()
        escopo.cancel()
    }

    /** Fonte cuja sonda trava até alguém soltar — o provider de agenda pendurado. */
    private inner class FonteTravada : ContactNumberSource {
        val sondaEntrou = CountDownLatch(1)

        override fun hasPermission() = true

        override fun probe(e164: String, nationalDigits: String?): Boolean {
            sondaEntrou.countDown()
            liberacao.await(SEGURANCA_SEGUNDOS, TimeUnit.SECONDS)
            return true
        }

        override fun allRawNumbers(): List<String> = emptyList()
        override fun observeChanges(onChange: () -> Unit) = Unit
        override fun close() = Unit
    }

    @Test
    fun `sonda travada nao segura a consulta alem do prazo`() {
        val fonte = FonteTravada()
        val repo = DefaultContactLookupRepository(
            source = fonte,
            cache = ContactKeyCache(fonte, normalizer, escopo),
            normalizer = normalizer,
            scope = escopo,
            probeTimeoutMillis = PRAZO_MILLIS,
        )

        val comeco = System.nanoTime()
        val resultado = runBlocking { repo.lookup(celular) }
        val decorridoMillis = (System.nanoTime() - comeco) / NANOS_POR_MILLI

        assertTrue("a sonda precisa ter sido de fato chamada", fonte.sondaEntrou.await(1, TimeUnit.SECONDS))
        // Indisponível, e NUNCA `MISS`: tratar provider travado como "não está na agenda" faria o
        // motor barrar a ligação de alguém que está nos contatos do usuário.
        assertEquals(ContactLookup.UNAVAILABLE, resultado)
        assertTrue(
            "a consulta levou $decorridoMillis ms — o prazo de $PRAZO_MILLIS ms nao venceu, " +
                "entao a sonda bloqueante continua segurando a resposta ao sistema",
            decorridoMillis < TETO_MILLIS,
        )
    }

    private companion object {
        const val PRAZO_MILLIS = 100L

        /** Folga generosa: o que se afirma é que não esperou a sonda, não uma marca de tempo. */
        const val TETO_MILLIS = 2_000L
        const val SEGURANCA_SEGUNDOS = 10L
        const val NANOS_POR_MILLI = 1_000_000L
    }
}

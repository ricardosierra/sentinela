package org.sentinela.app.data.contacts

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.platform.assetsPhoneMetadataLoader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Invalidacao do cache por `ContentObserver` e debounce, provados contra o provider real.
 *
 * Duas regras de arranjo, ambas herdadas de erro ja cometido no repo:
 *
 *  1. **Sincronizacao por condicao, nunca por `Thread.sleep` fixo.** Latch com prazo para o que
 *     deve acontecer, espera por predicado para o que precisa se estabilizar.
 *  2. **A contagem de callbacks do provider e diagnostico, jamais assert.** A pesquisa da fase
 *     mediu 51 callbacks para 50 transacoes: a coalescencia existe mas nao e contratual. O que
 *     esta suite afirma e o numero de RECONSTRUCOES do conjunto — esse sim e responsabilidade do
 *     app, e e ele que o debounce controla.
 */
@RunWith(AndroidJUnit4::class)
class ContactsObserverTest {

    private lateinit var resolver: ContentResolver
    private lateinit var real: ContactsContractLookupSource
    private lateinit var contada: FonteContada
    private lateinit var normalizer: PhoneNumberNormalizer
    private lateinit var scope: CoroutineScope
    private lateinit var cache: ContactKeyCache
    private lateinit var repo: DefaultContactLookupRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ContactsTestFixture.adoptShell()
        resolver = ctx.contentResolver
        ContactsTestFixture.wipe(resolver)
        normalizer = LibPhoneNumberNormalizer(
            util = phoneNumberUtil(assetsPhoneMetadataLoader(ctx)),
            regionProvider = RegionProvider { REGIAO },
        )
        real = ContactsContractLookupSource(ctx)
        contada = FonteContada(real)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        cache = ContactKeyCache(contada, normalizer, scope)
        repo = DefaultContactLookupRepository(contada, cache, normalizer)
    }

    @After
    fun tearDown() {
        Log.i(
            TAG,
            "SENTINELA|contacts|observer|callbacks=${contada.callbacks.get()}" +
                "|reconstrucoes=${contada.reconstrucoes.get()}",
        )
        ContactsTestFixture.wipe(resolver)
        cache.close()
        scope.cancel()
        ContactsTestFixture.dropShell()
    }

    @Test
    fun observadorRecebeCallbackAposInsercao() {
        aquecerAtePronto()
        val latch = CountDownLatch(1)
        contada.latch = latch

        ContactsTestFixture.insert(resolver, "Ana", CELULAR_A)

        assertTrue(
            "nenhum callback do provider em ${PRAZO_S}s",
            latch.await(PRAZO_S, TimeUnit.SECONDS),
        )
    }

    @Test
    fun contatoNovoViraHitDepoisDaInvalidacao() {
        aquecerAtePronto()
        assertEquals(ContactLookup.MISS, consultar(CELULAR_A))

        ContactsTestFixture.insert(resolver, "Bruno", CELULAR_A)
        aguardarInvalidacao()

        assertTrue("chave nova ausente do conjunto", CELULAR_A in aquecerAtePronto())
        assertEquals(ContactLookup.HIT, consultar(CELULAR_A))
    }

    @Test
    fun contatoRemovidoVoltaAMissDepoisDaInvalidacao() {
        ContactsTestFixture.insert(resolver, "Carla", CELULAR_A)
        aquecerAtePronto()
        assertEquals(ContactLookup.HIT, consultar(CELULAR_A))

        ContactsTestFixture.wipe(resolver)
        aguardarInvalidacao()

        assertFalse("chave removida ainda no conjunto", CELULAR_A in aquecerAtePronto())
        assertEquals(ContactLookup.MISS, consultar(CELULAR_A))
    }

    /**
     * O teste que o debounce existe para passar.
     *
     * Um aquecedor fica batendo em `warmInBackground` durante toda a rajada. Como o aquecimento so
     * reconstroi quando o conjunto foi derrubado, a contagem de reconstrucoes durante a janela e
     * exatamente o numero de INVALIDACOES que escaparam. Sem debounce, cada uma das dezenas de
     * notificacoes produziria a sua — foi assim que a garantia foi falsificada antes de ser aceita.
     */
    @Test
    fun rajadaDeInsercoesGeraUmaUnicaReconstrucao() = runBlocking {
        aquecerAtePronto()
        contada.reconstrucoes.set(0)
        val aquecedor = scope.launch {
            while (isActive) {
                cache.warmInBackground()
                delay(INTERVALO_AQUECEDOR_MS)
            }
        }

        repeat(RAJADA) { i -> ContactsTestFixture.insert(resolver, "Rajada $i", celular(i)) }
        aguardarSilencioDoProvider()
        aquecerAtePronto()
        aquecedor.cancel()

        Log.i(
            TAG,
            "SENTINELA|contacts|rajada|insercoes=$RAJADA" +
                "|callbacks=${contada.callbacks.get()}|reconstrucoes=${contada.reconstrucoes.get()}",
        )
        assertEquals(
            "a rajada deveria custar UMA reconstrucao (callbacks=${contada.callbacks.get()})",
            1,
            contada.reconstrucoes.get(),
        )
    }

    @Test
    fun closeDesregistraOObservador() {
        aquecerAtePronto()
        cache.close()

        val latch = CountDownLatch(1)
        contada.latch = latch
        ContactsTestFixture.insert(resolver, "Depois do close", CELULAR_A)

        assertFalse(
            "observador continuou registrado depois do close",
            latch.await(PRAZO_NEGATIVO_S, TimeUnit.SECONDS),
        )
    }

    private fun consultar(e164: String): ContactLookup = runBlocking { repo.lookup(e164) }

    /**
     * Dispara o aquecimento e espera o conjunto existir — nunca um sono fixo. Devolve o conjunto
     * OBSERVADO no instante em que ficou pronto: reler `cache.get()` depois seria uma corrida com
     * um callback atrasado do provider, e o teste ficaria falso-vermelho por defeito do arranjo.
     */
    private fun aquecerAtePronto(): Set<String> {
        cache.warmInBackground()
        var pronto: Set<String>? = null
        aguardarAte("cache nunca ficou pronto") {
            pronto = cache.get()
            pronto != null
        }
        return pronto.orEmpty()
    }

    private fun aguardarInvalidacao() =
        aguardarAte("cache nunca foi invalidado") { cache.get() == null }

    /** Espera o provider parar de notificar por mais de duas janelas de debounce. */
    private fun aguardarSilencioDoProvider() = aguardarAte("provider nunca silenciou") {
        val ultimo = contada.ultimoCallbackMs.get()
        ultimo != 0L && System.currentTimeMillis() - ultimo > ContactKeyCache.DEBOUNCE_MS * 2
    }

    private fun aguardarAte(mensagem: String, condicao: () -> Boolean) {
        val limite = System.currentTimeMillis() + PRAZO_S * MILIS_POR_S
        while (System.currentTimeMillis() < limite) {
            if (condicao()) return
            Thread.sleep(PASSO_MS)
        }
        throw AssertionError("$mensagem em ${PRAZO_S}s")
    }

    /**
     * Decorador que conta o que o teste precisa afirmar. Nao muda comportamento: cada metodo
     * delega. `allRawNumbers` e a unica porta de reconstrucao do conjunto, entao conta-la e contar
     * reconstrucoes.
     */
    private class FonteContada(private val delegate: ContactNumberSource) : ContactNumberSource {

        val callbacks = AtomicInteger(0)
        val reconstrucoes = AtomicInteger(0)
        val ultimoCallbackMs = AtomicLong(0)

        @Volatile
        var latch: CountDownLatch? = null

        override fun hasPermission(): Boolean = delegate.hasPermission()

        override fun probe(e164: String, nationalDigits: String?): Boolean =
            delegate.probe(e164, nationalDigits)

        override fun allRawNumbers(): List<String> {
            reconstrucoes.incrementAndGet()
            return delegate.allRawNumbers()
        }

        override fun observeChanges(onChange: () -> Unit) = delegate.observeChanges {
            callbacks.incrementAndGet()
            ultimoCallbackMs.set(System.currentTimeMillis())
            latch?.countDown()
            onChange()
        }

        override fun close() = delegate.close()
    }

    private companion object {
        const val TAG = "SentinelaContacts"
        const val REGIAO = "BR"
        const val CELULAR_A = "+5511912345678"
        const val RAJADA = 10
        const val PRAZO_S = 10L
        const val PRAZO_NEGATIVO_S = 3L
        const val PASSO_MS = 25L
        const val INTERVALO_AQUECEDOR_MS = 25L
        const val MILIS_POR_S = 1_000L

        fun celular(i: Int) = "+551199999%04d".format(i)
    }
}

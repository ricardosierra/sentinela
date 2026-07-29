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
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.platform.assetsPhoneMetadataLoader
import java.util.concurrent.atomic.AtomicInteger

/**
 * CTT-02 com agenda de 5.000 contatos, em tres provas de naturezas diferentes:
 *
 *  1. **Orcamento — quem afirma e a MEDIANA.** So o p50 quebra o build. A Phase 3 ja tirou um
 *     p95 do emulador depois de ve-lo falhar 2 de 8 execucoes sem nenhuma regressao real: o
 *     percentil de cauda mede o scheduler do host tanto quanto o codigo. p95 e max sao medidos,
 *     logados e cobrados em aparelho fisico na Phase 9 — nao afrouxados, mudados de lugar.
 *  2. **Uso do cache — assert por CONTADOR.** Quantas vezes o provider foi consultado, nunca
 *     quanto tempo levou. Se a implementacao perder o cache, este teste fica vermelho mesmo com o
 *     cronometro bonito; um teste de tempo, no lugar dele, seria falso-verde.
 *  3. **Custo da construcao — so reportado.** E o numero que justifica o cache NUNCA ser aguardado.
 *
 * O fixture pesado vive em `@BeforeClass`: inserir 5.000 contatos custa 7–14 s, e por teste a
 * suite viraria minutos.
 */
@RunWith(AndroidJUnit4::class)
class ContactLookupPerformanceTest {

    @Test
    fun lookupComCacheQuenteCabeNoOrcamentoDaDecisao() {
        aquecerAtePronto()

        repeat(WARMUP) { consultar(HIT_E164) }
        val amostras = DoubleArray(SAMPLES) {
            val inicio = System.nanoTime()
            consultar(HIT_E164)
            (System.nanoTime() - inicio) / NANOS_POR_MS
        }

        val p50 = reportar("cache-quente", amostras)
        assertTrue(
            "p50=$p50 ms — sinal estavel, esperado < $P50_CACHE_MAX_MS ms",
            p50 < P50_CACHE_MAX_MS,
        )
    }

    @Test
    fun sondaDiretaCabeNoOrcamentoDaDecisao() {
        val nacionalHit = normalizer.nationalDigits(HIT_E164)
        val nacionalMiss = normalizer.nationalDigits(MISS_E164)

        repeat(WARMUP) { contada.probe(HIT_E164, nacionalHit) }
        val hits = DoubleArray(SAMPLES) {
            val inicio = System.nanoTime()
            contada.probe(HIT_E164, nacionalHit)
            (System.nanoTime() - inicio) / NANOS_POR_MS
        }
        val misses = DoubleArray(SAMPLES) {
            val inicio = System.nanoTime()
            contada.probe(MISS_E164, nacionalMiss)
            (System.nanoTime() - inicio) / NANOS_POR_MS
        }

        val p50Hit = reportar("sonda-direta-hit", hits)
        val p50Miss = reportar("sonda-direta-miss", misses)
        assertTrue("p50 HIT=$p50Hit ms, esperado < $P50_SONDA_MAX_MS ms", p50Hit < P50_SONDA_MAX_MS)
        assertTrue("p50 MISS=$p50Miss ms, esperado < $P50_SONDA_MAX_MS ms", p50Miss < P50_SONDA_MAX_MS)
    }

    /**
     * A prova de que o cache e usado. Cronometro nao serve: com 5.000 contatos a sonda direta
     * tambem cabe no orcamento, entao um teste de tempo continuaria VERDE com o cache removido.
     */
    @Test
    fun cacheQuenteNaoConsultaOProvider() {
        aquecerAtePronto()
        contada.sondas.set(0)
        contada.leiturasEmLote.set(0)

        repeat(LOOKUPS_ESTRUTURAIS) {
            assertEquals(ContactLookup.HIT, consultar(HIT_E164))
            assertEquals(ContactLookup.MISS, consultar(MISS_E164))
        }

        assertEquals("cache quente sondou o provider", 0, contada.sondas.get())
        assertEquals("cache quente releu a agenda", 0, contada.leiturasEmLote.get())
    }

    /**
     * Numero reportado, sem assert: e ele que explica por que `warmInBackground` nunca e aguardado
     * por quem consulta. Um cache proprio, para nao derrubar o que os outros testes aqueceram.
     */
    @Test
    fun custoDaConstrucaoDoCacheEReportado() {
        // NAO fechado de proposito: `close()` desregistraria o observador COMPARTILHADO da fonte.
        // O coletor morre junto com o escopo, em @AfterClass.
        val proprio = ContactKeyCache(contada, normalizer, scope)
        val inicio = System.nanoTime()
        proprio.warmInBackground()
        aguardarAte("cache proprio nunca ficou pronto") { proprio.get() != null }
        val ms = (System.nanoTime() - inicio) / NANOS_POR_MS
        Log.i(
            TAG,
            "SENTINELA|contacts|construcao|contatos=$ENTRIES|chaves=${proprio.get()?.size}|ms=$ms",
        )
    }

    companion object {
        const val TAG = "SentinelaContacts"
        const val REGIAO = "BR"
        const val ENTRIES = 5_000
        const val WARMUP = 100
        const val SAMPLES = 500
        const val LOOKUPS_ESTRUTURAIS = 200
        const val NANOS_POR_MS = 1_000_000.0
        const val PRAZO_MS = 60_000L
        const val PASSO_MS = 25L

        /** Medido 1,08 µs no cache quente: folga de ~4 ordens de grandeza. */
        const val P50_CACHE_MAX_MS = 10.0

        /** Medido 1,95 ms (HIT) e 2,45 ms (MISS) com 5.000 contatos. */
        const val P50_SONDA_MAX_MS = 50.0

        const val HIT_E164 = "+5511990002500"
        const val MISS_E164 = "+5511977776666"

        internal lateinit var resolver: ContentResolver
        internal lateinit var real: ContactsContractLookupSource
        internal lateinit var contada: FonteContada
        internal lateinit var normalizer: PhoneNumberNormalizer
        internal lateinit var scope: CoroutineScope
        internal lateinit var cache: ContactKeyCache
        internal lateinit var repo: DefaultContactLookupRepository

        @BeforeClass
        @JvmStatic
        fun agendaGrande() {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            ContactsTestFixture.adoptShell()
            resolver = ctx.contentResolver
            ContactsTestFixture.wipe(resolver)
            val inicio = System.nanoTime()
            ContactsTestFixture.insertMany(resolver, ENTRIES) { i -> "+551199%07d".format(i) }
            Log.i(
                TAG,
                "SENTINELA|contacts|fixture|contatos=$ENTRIES" +
                    "|ms=${(System.nanoTime() - inicio) / NANOS_POR_MS}",
            )
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

        @AfterClass
        @JvmStatic
        fun limpar() {
            ContactsTestFixture.wipe(resolver)
            cache.close()
            scope.cancel()
            ContactsTestFixture.dropShell()
        }

        internal fun consultar(e164: String): ContactLookup = runBlocking { repo.lookup(e164) }

        internal fun aquecerAtePronto() {
            cache.warmInBackground()
            aguardarAte("cache nunca ficou pronto") { cache.get() != null }
        }

        internal fun aguardarAte(mensagem: String, condicao: () -> Boolean) {
            val limite = System.currentTimeMillis() + PRAZO_MS
            while (System.currentTimeMillis() < limite) {
                if (condicao()) return
                Thread.sleep(PASSO_MS)
            }
            throw AssertionError("$mensagem em $PRAZO_MS ms")
        }

        /**
         * Devolve a MEDIANA e loga a cauda. A cauda sai daqui como diagnostico e vira cenario da
         * validacao fisica da Phase 9 — nenhum chamador desta funcao afirma percentil de cauda.
         */
        internal fun reportar(rotulo: String, amostras: DoubleArray): Double {
            amostras.sort()
            val p50 = amostras[SAMPLES / 2]
            val cauda95 = amostras[(SAMPLES * 0.95).toInt()]
            val maximo = amostras.last()
            Log.i(
                TAG,
                "SENTINELA|contacts|$rotulo|contatos=$ENTRIES|amostras=$SAMPLES" +
                    "|p50=$p50|p95=$cauda95|max=$maximo",
            )
            return p50
        }
    }

    /** Conta consultas ao provider. E este contador, nao o relogio, que prova o cache. */
    internal class FonteContada(private val delegate: ContactNumberSource) : ContactNumberSource {

        val sondas = AtomicInteger(0)
        val leiturasEmLote = AtomicInteger(0)

        override fun hasPermission(): Boolean = delegate.hasPermission()

        override fun probe(e164: String, nationalDigits: String?): Boolean {
            sondas.incrementAndGet()
            return delegate.probe(e164, nationalDigits)
        }

        override fun allRawNumbers(): List<String> {
            leiturasEmLote.incrementAndGet()
            return delegate.allRawNumbers()
        }

        override fun observeChanges(onChange: () -> Unit) = delegate.observeChanges(onChange)

        override fun close() = delegate.close()
    }
}

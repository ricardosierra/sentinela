package org.sentinela.app.data.contacts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.TestMetadata

/**
 * A chave do cache sai do normalizador do PROPRIO app, nunca da coluna normalizada do provider —
 * medida nula para contato estrangeiro e ate errada (um fixo do Rio virou numero dos EUA num
 * aparelho com chip americano). Por isso o teste usa o normalizador real, com os metadados reais.
 *
 * Toda afirmacao de "usou o cache" e feita por CONTADOR. Cronometro nao prova estrutura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactKeyCacheTest {

    private val normalizer =
        LibPhoneNumberNormalizer(TestMetadata.util(), RegionProvider { "BR" })

    /**
     * O cache recebe um escopo proprio sobre o relogio de teste. NAO usar `backgroundScope`:
     * medido nesta versao das coroutines, `advanceUntilIdle` nao despacha o que foi lancado la e a
     * construcao nunca rodaria — a suite ficaria falso-vermelha por defeito do arranjo.
     */
    private fun comCache(
        vararg rawNumbers: String,
        falhaNaLeitura: Boolean = false,
        body: suspend TestScope.(FakeContactNumberSource, ContactKeyCache) -> Unit,
    ) = runTest {
        val source = FakeContactNumberSource().apply {
            this.rawNumbers = rawNumbers.toList()
            this.failRawNumbers = falhaNaLeitura
        }
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        try {
            body(source, ContactKeyCache(source, normalizer, scope))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `get devolve nulo antes de qualquer aquecimento`() = comCache() { source, cache ->
        assertNull(cache.get())
        assertEquals(0, source.rawNumbersCount)
    }

    @Test
    fun `a construcao nunca e aguardada - get segue nulo na mesma volta`() =
        comCache("11987654321") { source, cache ->
            cache.warmInBackground()

            // Custa ~1,5 s com 5.000 contatos: quem chamou nao pode ter esperado nada.
            assertNull(cache.get())
            assertEquals(0, source.rawNumbersCount)
        }

    @Test
    fun `numero nacional da agenda vira a MESMA chave E164 da whitelist`() =
        comCache("(11) 98765-4321", "11 3333-4444") { _, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            val keys = cache.get()
            assertNotNull(keys)
            assertEquals(setOf("+5511987654321", "+551133334444"), keys)
        }

    @Test
    fun `numero cru invalido e descartado sem derrubar a construcao`() =
        comCache("11987654321", "abc", "", "+999999") { _, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            assertEquals(setOf("+5511987654321"), cache.get())
        }

    @Test
    fun `falha na leitura em lote deixa o cache nulo sem lancar`() =
        comCache(falhaNaLeitura = true) { _, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            assertNull(cache.get())
        }

    @Test
    fun `cache quente nao reconstroi - o contador de leitura em lote nao sobe`() =
        comCache("11987654321") { source, cache ->
            cache.warmInBackground()
            advanceUntilIdle()
            repeat(5) { cache.warmInBackground() }
            advanceUntilIdle()

            // Prova estrutural por CONTADOR, jamais por tempo.
            assertEquals(1, source.rawNumbersCount)
        }

    @Test
    fun `rajada de notificacoes gera uma unica reconstrucao`() =
        comCache("11987654321") { source, cache ->
            cache.warmInBackground()
            advanceUntilIdle()
            assertEquals(1, source.rawNumbersCount)

            // Medido: 51 callbacks para 50 transacoes. Sem debounce seriam 20 reconstrucoes.
            repeat(20) { source.notifyChange() }
            advanceTimeBy(ContactKeyCache.DEBOUNCE_MS + 1)
            advanceUntilIdle()

            assertNull("a invalidacao e preguicosa: derruba o conjunto", cache.get())

            cache.warmInBackground()
            advanceUntilIdle()
            assertEquals(2, source.rawNumbersCount)
        }

    @Test
    fun `notificacao antes do debounce nao invalida o cache`() =
        comCache("11987654321") { source, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            source.notifyChange()
            advanceTimeBy(ContactKeyCache.DEBOUNCE_MS - 1)

            assertEquals(setOf("+5511987654321"), cache.get())
        }

    @Test
    fun `o observador e registrado uma unica vez`() =
        comCache("11987654321") { source, cache ->
            repeat(3) { cache.warmInBackground() }
            advanceUntilIdle()

            assertEquals(1, source.observerCount)
        }

    @Test
    fun `a construcao usa a leitura em lote e nunca a sonda direta`() =
        comCache("11987654321") { source, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            // Prova por CONTADOR: o cache le a agenda de uma vez so. Sondar numero a numero seriam
            // 5.000 consultas ao provider — o oposto do que o conjunto em memoria existe para fazer.
            assertEquals(0, source.probeCount)
            assertEquals(1, source.rawNumbersCount)
        }

    @Test
    fun `o cache guarda somente chaves canonicas, sem duplicata de formato`() =
        comCache("11987654321", "+5511987654321", "1133334444") { _, cache ->
            cache.warmInBackground()
            advanceUntilIdle()

            val keys = cache.get()!!
            assertEquals(2, keys.size)
            assertTrue(keys.all { it.startsWith("+") })
        }
}

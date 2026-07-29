package org.sentinela.app.data.contacts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.TestMetadata

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultContactLookupRepositoryTest {

    private val normalizer =
        LibPhoneNumberNormalizer(TestMetadata.util(), RegionProvider { "BR" })

    private val celular = "+5511987654321"

    /**
     * Escopo proprio sobre o relogio de teste. NAO usar `backgroundScope`: medido nesta versao das
     * coroutines, `advanceUntilIdle` nao despacha o que foi lancado la e o cache nunca aqueceria —
     * os testes de caminho quente passariam por engano, pelo motivo errado.
     */
    private fun comRepo(
        source: FakeContactNumberSource,
        body: suspend TestScope.(FakeContactNumberSource, ContactLookupRepository) -> Unit,
    ) = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        try {
            val cache = ContactKeyCache(source, normalizer, scope)
            body(source, DefaultContactLookupRepository(source, cache, normalizer))
        } finally {
            scope.cancel()
        }
    }

    // --- permissao ----------------------------------------------------------------------

    @Test
    fun `sem permissao devolve UNAVAILABLE, nunca MISS`() =
        comRepo(FakeContactNumberSource().apply { granted = false }) { source, repository ->
        assertEquals(ContactLookup.UNAVAILABLE, repository.lookup(celular))
    }

    @Test
    fun `sem permissao a fonte nao e consultada`() =
        comRepo(FakeContactNumberSource().apply { granted = false }) { source, repository ->
        repository.lookup(celular)
        advanceUntilIdle()

        assertEquals(0, source.probeCount)
        assertEquals(0, source.rawNumbersCount)
    }

    // --- caminho frio: sonda direta -----------------------------------------------------

    @Test
    fun `cache frio responde HIT pela sonda direta`() =
        comRepo(FakeContactNumberSource().apply { probeHits = setOf(celular) }) { source, repository ->
        assertEquals(ContactLookup.HIT, repository.lookup(celular))
        assertEquals(1, source.probeCount)
    }

    @Test
    fun `cache frio responde MISS pela sonda direta`() =
        comRepo(FakeContactNumberSource()) { source, repository ->
        assertEquals(ContactLookup.MISS, repository.lookup(celular))
        assertEquals(1, source.probeCount)
    }

    @Test
    fun `a construcao do cache nao e aguardada no caminho de resposta`() =
        comRepo(FakeContactNumberSource().apply { rawNumbers = listOf("11987654321") }) { source, repository ->
        repository.lookup(celular)

        // A resposta ja voltou e a leitura em lote (medida em ~1,5 s) ainda nem comecou.
        assertEquals(0, source.rawNumbersCount)
    }

    @Test
    fun `a sonda recebe tambem o numero nacional`() =
        comRepo(FakeContactNumberSource()) { source, repository ->
        repository.lookup(celular)

        assertEquals(celular to "11987654321", source.lastProbeArgs)
    }

    @Test
    fun `codigo curto nao tem numero nacional e a segunda sonda vem nula`() =
        comRepo(FakeContactNumberSource()) { source, repository ->
        repository.lookup("190")

        assertEquals("190" to null, source.lastProbeArgs)
    }

    // --- falhas -------------------------------------------------------------------------

    @Test
    fun `falha na sonda devolve UNAVAILABLE, nunca MISS`() =
        comRepo(FakeContactNumberSource().apply { failProbe = true }) { source, repository ->
        assertEquals(ContactLookup.UNAVAILABLE, repository.lookup(celular))
    }

    @Test
    fun `falha na leitura em lote nao impede a resposta pela sonda direta`() = comRepo(FakeContactNumberSource().apply {
            failRawNumbers = true
            probeHits = setOf(celular)
        }) { source, repository ->
        assertEquals(ContactLookup.HIT, repository.lookup(celular))
        advanceUntilIdle()
        assertEquals(ContactLookup.HIT, repository.lookup(celular))
    }

    // --- caminho quente: prova por CONTADOR, jamais por cronometro -----------------------

    @Test
    fun `cache quente devolve HIT sem tocar o provider`() = comRepo(FakeContactNumberSource().apply {
            rawNumbers = listOf("11987654321")
            probeHits = setOf(celular)
        }) { source, repository ->
        repository.lookup(celular)
        advanceUntilIdle()
        val depoisDoAquecimento = source.probeCount

        assertEquals(ContactLookup.HIT, repository.lookup(celular))
        assertEquals(depoisDoAquecimento, source.probeCount)
    }

    @Test
    fun `cache quente devolve MISS sem tocar o provider`() =
        comRepo(FakeContactNumberSource().apply { rawNumbers = listOf("1133334444") }) {
            source, repository ->
        repository.lookup(celular)
        advanceUntilIdle()
        val depoisDoAquecimento = source.probeCount

        assertEquals(ContactLookup.MISS, repository.lookup(celular))
        assertEquals(depoisDoAquecimento, source.probeCount)
    }

    @Test
    fun `chave da agenda em formato nacional casa com a consulta em E164`() =
        // O provider normalizaria isto com o pais do APARELHO e poderia errar; quem normaliza aqui
        // e o normalizador do app, o mesmo da whitelist.
        comRepo(FakeContactNumberSource().apply { rawNumbers = listOf("(11) 98765-4321") }) {
            _, repository ->
        repository.lookup(celular)
        advanceUntilIdle()

        assertEquals(ContactLookup.HIT, repository.lookup(celular))
    }

    @Test
    fun `permissao revogada depois do aquecimento volta a UNAVAILABLE`() =
        comRepo(FakeContactNumberSource().apply { rawNumbers = listOf("11987654321") }) {
            source, repository ->
        repository.lookup(celular)
        advanceUntilIdle()
        source.granted = false

        assertEquals(ContactLookup.UNAVAILABLE, repository.lookup(celular))
    }
}

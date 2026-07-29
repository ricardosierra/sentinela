package org.sentinela.app.settings

import app.cash.turbine.test
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * ENG-01: o contador de aberturas mora no DataStore, junto das configuracoes — o
 * convite de avaliacao da Fase 9 le daqui. Nenhum dado pessoal envolvido.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppOpenCounterTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var contador = 0

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    private fun novoArquivo(): File =
        tmp.newFile("counter-${contador++}.preferences_pb").also { it.delete() }

    private fun TestScope.novoScope(): CoroutineScope =
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes += it }

    private fun dataStore(scope: CoroutineScope, file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    private fun TestScope.repo(file: File = novoArquivo()): DataStoreSettingsRepository {
        val scope = novoScope()
        return DataStoreSettingsRepository(dataStore(scope, file), scope)
    }

    @Test
    fun `contador comeca em zero num arquivo novo`() = runTest {
        assertEquals(0, repo().appOpenCount.first())
    }

    @Test
    fun `primeiro incremento devolve um`() = runTest {
        assertEquals(1, repo().incrementAppOpenCount())
    }

    @Test
    fun `incrementos sucessivos devolvem a sequencia`() = runTest {
        val repo = repo()

        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).map { repo.incrementAppOpenCount() })
    }

    @Test
    fun `valor sobrevive a recriacao do repositorio sobre o mesmo arquivo`() = runTest {
        val file = novoArquivo()
        val scopeA = novoScope()
        val primeiro = DataStoreSettingsRepository(dataStore(scopeA, file), scopeA)
        primeiro.incrementAppOpenCount()
        primeiro.incrementAppOpenCount()
        scopeA.cancel()

        val scopeB = novoScope()
        val segundo = DataStoreSettingsRepository(dataStore(scopeB, file), scopeB)

        assertEquals(3, segundo.incrementAppOpenCount())
    }

    @Test
    fun `Flow do contador emite o valor atualizado apos o incremento`() = runTest {
        val repo = repo()

        repo.appOpenCount.test {
            assertEquals(0, awaitItem())
            repo.incrementAppOpenCount()
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `contador nao interfere nas configuracoes de triagem`() = runTest {
        val repo = repo()

        repo.incrementAppOpenCount()

        assertEquals(ScreeningSettings(), repo.snapshot())
    }
}

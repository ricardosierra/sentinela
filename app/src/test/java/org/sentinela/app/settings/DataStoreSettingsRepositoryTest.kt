package org.sentinela.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Suite JVM pura: PreferenceDataStoreFactory.create recebe um File arbitrario e
 * dispensa Context, entao nada de instrumentacao nem de sandbox de recursos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var contador = 0

    @After
    fun tearDown() {
        // Sem cancelar o scope, o arquivo continua travado e o proximo teste bate em
        // "There are multiple DataStores active for the same file".
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    private fun novoArquivo(): File =
        tmp.newFile("settings-${contador++}.preferences_pb").also { it.delete() }

    private fun repo(scope: CoroutineScope, file: File = novoArquivo()): DataStoreSettingsRepository {
        scopes += scope
        return DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { file },
            scope,
        )
    }

    @Test
    fun `arquivo inexistente devolve os defaults do MVP`() = runTest {
        val repo = repo(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        assertEquals(ScreeningSettings(), repo.snapshot())
    }

    @Test
    fun `protectionEnabled sobrevive ao round-trip`() = runTest {
        val repo = repo(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        repo.update { it.copy(protectionEnabled = false) }

        assertEquals(false, repo.snapshot().protectionEnabled)
    }

    @Test
    fun `enum invalido no arquivo cai no default do campo`() = runTest {
        val file = novoArquivo()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        dataStore.edit { it[stringPreferencesKey("unknown_policy")] = "POLITICA_DO_FUTURO" }

        val repo = DataStoreSettingsRepository(dataStore, scope)

        assertEquals(OriginPolicy.BLOCK, repo.snapshot().unknownPolicy)
    }

    @Test
    fun `tipo errado gravado na chave nao derruba a leitura`() = runTest {
        val file = novoArquivo()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        dataStore.edit { it[booleanPreferencesKey("protection_enabled")] = false }

        val repo = DataStoreSettingsRepository(dataStore, scope)

        assertEquals(false, repo.snapshot().protectionEnabled)
    }

    @Test
    fun `snapshot repetido serve do cache em memoria`() = runTest {
        val repo = repo(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        val primeiro = repo.snapshot()
        val segundo = repo.snapshot()

        assertEquals(primeiro, segundo)
    }
}

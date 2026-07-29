package org.sentinela.app.telecom

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.permissions.canRequest
import org.sentinela.app.permissions.runtimePermissionAsk
import org.sentinela.app.permissions.shouldOfferSystemSettings
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.ScreeningSettings
import java.io.File

/**
 * Permissao de originar chamada: a MESMA regra de quatro estados da agenda e das notificacoes,
 * mais o sinalizador persistido.
 *
 * A permissao de originar chamada foi medida como NAO concedida no install (ao contrario da de
 * intencao de tela cheia), portanto precisa de pedido em runtime — e o sinalizador tem de ser
 * gravado no momento do disparo, nunca no retorno do dialogo.
 *
 * JVM pura: `PreferenceDataStoreFactory.create { file }` dispensa Context. Um arquivo por teste e
 * o escopo cancelado no @After, senao o arquivo continua travado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CallPhonePermissionTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var contador = 0

    @After
    fun tearDown() {
        scopes.forEach { scope -> scope.cancel() }
        scopes.clear()
    }

    private fun novoArquivo(): File =
        tmp.newFile("call-phone-${contador++}.preferences_pb").also { it.delete() }

    private fun TestScope.repo(): DataStoreSettingsRepository {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes += it }
        val store: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope) { novoArquivo() }
        return DataStoreSettingsRepository(store, scope)
    }

    @Test
    fun `concedida nao pede nada`() {
        val estado = runtimePermissionAsk(granted = true, alreadyAsked = true, rationale = false)

        assertEquals(RuntimePermissionAsk.GRANTED, estado)
        assertFalse(estado.canRequest)
        assertFalse(estado.shouldOfferSystemSettings)
    }

    @Test
    fun `nunca pedida permite disparar o dialogo do sistema`() {
        val estado = runtimePermissionAsk(granted = false, alreadyAsked = false, rationale = false)

        assertEquals(RuntimePermissionAsk.NEVER_ASKED, estado)
        assertTrue(estado.canRequest)
    }

    @Test
    fun `negada uma vez ainda aceita novo pedido com explicacao`() {
        val estado = runtimePermissionAsk(granted = false, alreadyAsked = true, rationale = true)

        assertEquals(RuntimePermissionAsk.DENIED_ONCE, estado)
        assertTrue(estado.canRequest)
        assertFalse(estado.shouldOfferSystemSettings)
    }

    @Test
    fun `negada de vez so oferece as configuracoes do sistema`() {
        val estado = runtimePermissionAsk(granted = false, alreadyAsked = true, rationale = false)

        assertEquals(RuntimePermissionAsk.DENIED_PERMANENTLY, estado)
        assertFalse(estado.canRequest)
        assertTrue(estado.shouldOfferSystemSettings)
    }

    @Test
    fun `o sinalizador comeca falso`() = runTest {
        assertFalse(repo().callPhonePermissionAsked.first())
    }

    @Test
    fun `o sinalizador gravado no disparo sobrevive a morte do processo antes do retorno`() =
        runTest {
            val repositorio = repo()

            // Simula o disparo do launcher: marcamos ANTES de qualquer retorno do dialogo.
            repositorio.markCallPhonePermissionAsked()

            // Nenhum callback aconteceu; ainda assim o app sabe que ja perguntou.
            assertTrue(repositorio.callPhonePermissionAsked.first())
            assertEquals(
                RuntimePermissionAsk.DENIED_PERMANENTLY,
                runtimePermissionAsk(
                    granted = false,
                    alreadyAsked = repositorio.callPhonePermissionAsked.first(),
                    rationale = false,
                ),
            )
        }

    @Test
    fun `marcar duas vezes e idempotente`() = runTest {
        val repositorio = repo()

        repositorio.markCallPhonePermissionAsked()
        repositorio.markCallPhonePermissionAsked()

        assertTrue(repositorio.callPhonePermissionAsked.first())
    }

    @Test
    fun `o sinalizador nao entra nas configuracoes de triagem`() = runTest {
        val repositorio = repo()
        val antes: ScreeningSettings = repositorio.snapshot()

        repositorio.markCallPhonePermissionAsked()

        assertEquals(antes, repositorio.snapshot())
    }
}

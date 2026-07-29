package org.sentinela.app.settings

import app.cash.turbine.test
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Suite JVM pura: `PreferenceDataStoreFactory.create { file }` recebe um File arbitrario
 * e dispensa Context, entao nada de instrumentacao nem de sandbox de recursos.
 *
 * Regra de ouro desta suite: UM arquivo novo por teste e o scope cancelado no @After.
 * O runtime do DataStore trava o arquivo por processo e lanca se houver duas instancias
 * ativas sobre o mesmo caminho — foi o modo de falha reproduzido na pesquisa da fase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var contador = 0

    @After
    fun tearDown() {
        // Sem o scope.cancel() o arquivo continua travado e o proximo teste quebra.
        scopes.forEach { scope -> scope.cancel() }
        scopes.clear()
    }

    private fun novoArquivo(): File =
        tmp.newFile("settings-${contador++}.preferences_pb").also { it.delete() }

    private fun TestScope.novoScope(): CoroutineScope =
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes += it }

    private fun dataStore(scope: CoroutineScope, file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    private fun TestScope.repo(file: File = novoArquivo()): DataStoreSettingsRepository {
        val scope = novoScope()
        return DataStoreSettingsRepository(dataStore(scope, file), scope)
    }

    /** Round-trip generico: grava um valor NAO-default e confere na releitura. */
    private fun <T> roundTrip(
        transform: (ScreeningSettings) -> ScreeningSettings,
        leitura: (ScreeningSettings) -> T,
        esperado: T,
    ) = runTest {
        val repo = repo()
        repo.update(transform)
        assertEquals(esperado, leitura(repo.snapshot()))
    }

    // --- defaults -----------------------------------------------------------

    @Test
    fun `arquivo inexistente devolve exatamente os defaults do MVP`() = runTest {
        assertEquals(ScreeningSettings(), repo().snapshot())
    }

    @Test
    fun `defaults do MVP sao desconhecido bloqueado contato tocando e whitelist protegida`() =
        runTest {
            val padrao = repo().snapshot()

            assertEquals(OriginPolicy.BLOCK, padrao.unknownPolicy)
            assertEquals(OriginPolicy.RING, padrao.contactsPolicy)
            assertEquals(OriginPolicy.NEVER_SILENCE, padrao.whitelistPolicy)
            assertEquals(true, padrao.historyEnabled)
            assertEquals(RetentionPolicy.DAYS_30, padrao.retentionPolicy)
        }

    // --- round-trip dos 11 campos -------------------------------------------

    @Test
    fun `round-trip de protectionEnabled`() =
        roundTrip({ it.copy(protectionEnabled = false) }, { it.protectionEnabled }, false)

    @Test
    fun `round-trip de unknownPolicy`() =
        roundTrip(
            { it.copy(unknownPolicy = OriginPolicy.SILENCE) },
            { it.unknownPolicy },
            OriginPolicy.SILENCE,
        )

    @Test
    fun `round-trip de contactsPolicy`() =
        roundTrip(
            { it.copy(contactsPolicy = OriginPolicy.BLOCK) },
            { it.contactsPolicy },
            OriginPolicy.BLOCK,
        )

    @Test
    fun `round-trip de whitelistPolicy`() =
        roundTrip(
            { it.copy(whitelistPolicy = OriginPolicy.RING) },
            { it.whitelistPolicy },
            OriginPolicy.RING,
        )

    @Test
    fun `round-trip de blockPrivateNumbers`() =
        roundTrip({ it.copy(blockPrivateNumbers = false) }, { it.blockPrivateNumbers }, false)

    @Test
    fun `round-trip de blockMode`() =
        roundTrip(
            { it.copy(blockMode = BlockMode.SILENT_VOICEMAIL) },
            { it.blockMode },
            BlockMode.SILENT_VOICEMAIL,
        )

    @Test
    fun `round-trip de hideFromNativeCallLog`() =
        roundTrip({ it.copy(hideFromNativeCallLog = false) }, { it.hideFromNativeCallLog }, false)

    @Test
    fun `round-trip de showOwnNotification`() =
        roundTrip({ it.copy(showOwnNotification = true) }, { it.showOwnNotification }, true)

    @Test
    fun `round-trip de fallbackPolicy`() =
        roundTrip(
            { it.copy(fallbackPolicy = FallbackPolicy.BLOCK) },
            { it.fallbackPolicy },
            FallbackPolicy.BLOCK,
        )

    @Test
    fun `round-trip de historyEnabled`() =
        roundTrip({ it.copy(historyEnabled = false) }, { it.historyEnabled }, false)

    @Test
    fun `round-trip de retentionPolicy`() =
        roundTrip(
            { it.copy(retentionPolicy = RetentionPolicy.DAYS_90) },
            { it.retentionPolicy },
            RetentionPolicy.DAYS_90,
        )

    @Test
    fun `excecao de chamada repetida vem ligada em arquivo inexistente`() = runTest {
        assertEquals(true, repo().snapshot().repeatedCallBypassEnabled)
    }

    @Test
    fun `round-trip de repeatedCallBypassEnabled`() =
        roundTrip(
            { it.copy(repeatedCallBypassEnabled = false) },
            { it.repeatedCallBypassEnabled },
            false,
        )

    // --- persistencia real, sem depender da posicao da constante -------------

    @Test
    fun `enum vai ao disco pelo nome e retencao pelo id`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)
        DataStoreSettingsRepository(ds, scope).update {
            it.copy(unknownPolicy = OriginPolicy.SILENCE, retentionPolicy = RetentionPolicy.DAYS_7)
        }

        val prefs = ds.data.first()

        assertEquals("SILENCE", prefs[stringPreferencesKey("unknown_policy")])
        assertEquals("7d", prefs[stringPreferencesKey("retention_policy")])
    }

    @Test
    fun `todos os campos sobrevivem a recriacao do repositorio sobre o mesmo arquivo`() = runTest {
        val file = novoArquivo()
        val scopeA = novoScope()
        val desejado = ScreeningSettings(
            protectionEnabled = false,
            unknownPolicy = OriginPolicy.SILENCE,
            contactsPolicy = OriginPolicy.BLOCK,
            whitelistPolicy = OriginPolicy.RING,
            blockPrivateNumbers = false,
            blockMode = BlockMode.SILENT_VOICEMAIL,
            hideFromNativeCallLog = false,
            showOwnNotification = true,
            fallbackPolicy = FallbackPolicy.BLOCK,
            historyEnabled = false,
            retentionPolicy = RetentionPolicy.MANUAL,
        )
        DataStoreSettingsRepository(dataStore(scopeA, file), scopeA).update { desejado }
        scopeA.cancel()

        val scopeB = novoScope()
        val relido = DataStoreSettingsRepository(dataStore(scopeB, file), scopeB).snapshot()

        assertEquals(desejado, relido)
    }

    // --- tolerancia a dado invalido -----------------------------------------

    @Test
    fun `enum desconhecido no arquivo cai no default do campo sem lancar`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)
        ds.edit {
            it[stringPreferencesKey("unknown_policy")] = "POLITICA_DO_FUTURO"
            it[stringPreferencesKey("block_mode")] = "???"
            it[stringPreferencesKey("fallback_policy")] = ""
            it[stringPreferencesKey("retention_policy")] = "eterno"
        }

        val lido = DataStoreSettingsRepository(ds, scope).snapshot()

        assertEquals(OriginPolicy.BLOCK, lido.unknownPolicy)
        assertEquals(BlockMode.REJECT, lido.blockMode)
        assertEquals(FallbackPolicy.ALLOW, lido.fallbackPolicy)
        assertEquals(RetentionPolicy.DAYS_30, lido.retentionPolicy)
    }

    @Test
    fun `arquivo com lixo binario cai nos defaults seguros em vez de derrubar o Flow`() = runTest {
        val file = tmp.newFile("corrompido-${contador++}.preferences_pb")
        file.writeBytes(byteArrayOf(0x42, 0x13, 0x37, 0x00, 0x7F))
        val scope = novoScope()

        val lido = DataStoreSettingsRepository(dataStore(scope, file), scope).settings.first()

        assertEquals(ScreeningSettings(), lido)
    }

    // --- composicao e emissao ------------------------------------------------

    @Test
    fun `updates sucessivos compoem`() = runTest {
        val repo = repo()

        repo.update { it.copy(unknownPolicy = OriginPolicy.SILENCE) }
        repo.update { it.copy(historyEnabled = false) }

        val lido = repo.snapshot()
        assertEquals(OriginPolicy.SILENCE, lido.unknownPolicy)
        assertEquals(false, lido.historyEnabled)
    }

    @Test
    fun `settings emite o valor novo apos o update`() = runTest {
        val repo = repo()

        repo.settings.test {
            assertEquals(ScreeningSettings(), awaitItem())
            repo.update { it.copy(retentionPolicy = RetentionPolicy.NEVER_STORE) }
            assertEquals(RetentionPolicy.NEVER_STORE, awaitItem().retentionPolicy)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snapshot repetido devolve a MESMA instancia do cache em memoria`() = runTest {
        val repo = repo()

        val primeiro = repo.snapshot()
        val segundo = repo.snapshot()

        assertNotNull(primeiro)
        assertSame(primeiro, segundo)
    }

    // --- CTT-01: flag contacts_permission_asked ------------------------------

    @Test
    fun `contactsPermissionAsked comeca falso em arquivo inexistente`() = runTest {
        assertEquals(false, repo().contactsPermissionAsked.first())
    }

    @Test
    fun `markContactsPermissionAsked faz o Flow emitir verdadeiro`() = runTest {
        val repo = repo()

        repo.markContactsPermissionAsked()

        assertEquals(true, repo.contactsPermissionAsked.first())
    }

    @Test
    fun `marcar duas vezes e idempotente`() = runTest {
        val repo = repo()

        repo.markContactsPermissionAsked()
        repo.markContactsPermissionAsked()

        assertEquals(true, repo.contactsPermissionAsked.first())
    }

    @Test
    fun `o flag sobrevive a recriacao do repositorio sobre o mesmo arquivo`() = runTest {
        val file = novoArquivo()
        val scopeA = novoScope()
        DataStoreSettingsRepository(dataStore(scopeA, file), scopeA).markContactsPermissionAsked()
        scopeA.cancel()

        val scopeB = novoScope()
        val relido = DataStoreSettingsRepository(dataStore(scopeB, file), scopeB)

        assertEquals(true, relido.contactsPermissionAsked.first())
    }

    @Test
    fun `o flag vai ao disco com a chave textual contratada`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)

        DataStoreSettingsRepository(ds, scope).markContactsPermissionAsked()

        assertEquals(true, ds.data.first()[booleanPreferencesKey("contacts_permission_asked")])
    }

    @Test
    fun `arquivo corrompido devolve falso em vez de derrubar o Flow do flag`() = runTest {
        val file = tmp.newFile("corrompido-flag-${contador++}.preferences_pb")
        file.writeBytes(byteArrayOf(0x42, 0x13, 0x37, 0x00, 0x7F))
        val scope = novoScope()

        val repo = DataStoreSettingsRepository(dataStore(scope, file), scope)

        assertEquals(false, repo.contactsPermissionAsked.first())
    }

    @Test
    fun `o flag de permissao nao contamina as configuracoes de triagem`() = runTest {
        val repo = repo()

        repo.markContactsPermissionAsked()

        // Nao e configuracao de triagem: nao pode entrar no snapshot do caminho quente.
        assertEquals(ScreeningSettings(), repo.snapshot())
    }

    // --- NTF: identificacao da notificacao e flag de pedido ------------------

    @Test
    fun `identificacao da notificacao comeca mascarada`() = runTest {
        assertEquals(
            NotificationIdentification.MASKED,
            repo().snapshot().notificationIdentification,
        )
    }

    @Test
    fun `identificacao anonima faz round-trip pelo disco`() = runTest {
        val file = novoArquivo()
        val scopeA = novoScope()
        DataStoreSettingsRepository(dataStore(scopeA, file), scopeA)
            .update { it.copy(notificationIdentification = NotificationIdentification.ANONYMOUS) }
        scopeA.cancel()

        val scopeB = novoScope()
        val relido = DataStoreSettingsRepository(dataStore(scopeB, file), scopeB)

        assertEquals(
            NotificationIdentification.ANONYMOUS,
            relido.snapshot().notificationIdentification,
        )
    }

    @Test
    fun `identificacao e persistida pelo nome do enum e nao pela posicao`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)

        DataStoreSettingsRepository(ds, scope)
            .update { it.copy(notificationIdentification = NotificationIdentification.ANONYMOUS) }

        assertEquals(
            "ANONYMOUS",
            ds.data.first()[stringPreferencesKey("notification_identification")],
        )
    }

    @Test
    fun `identificacao desconhecida gravada em disco cai no padrao mascarado`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)
        ds.edit { it[stringPreferencesKey("notification_identification")] = "FULL_NUMBER" }

        val repo = DataStoreSettingsRepository(ds, scope)

        assertEquals(
            NotificationIdentification.MASKED,
            repo.snapshot().notificationIdentification,
        )
    }

    @Test
    fun `notificationPermissionAsked comeca falso e sobrevive a recriacao`() = runTest {
        val file = novoArquivo()
        val scopeA = novoScope()
        val primeiro = DataStoreSettingsRepository(dataStore(scopeA, file), scopeA)
        assertEquals(false, primeiro.notificationPermissionAsked.first())
        primeiro.markNotificationPermissionAsked()
        scopeA.cancel()

        val scopeB = novoScope()
        val relido = DataStoreSettingsRepository(dataStore(scopeB, file), scopeB)

        assertEquals(true, relido.notificationPermissionAsked.first())
    }

    @Test
    fun `o flag de notificacao vai ao disco com a chave textual contratada`() = runTest {
        val file = novoArquivo()
        val scope = novoScope()
        val ds = dataStore(scope, file)

        DataStoreSettingsRepository(ds, scope).markNotificationPermissionAsked()

        assertEquals(
            true,
            ds.data.first()[booleanPreferencesKey("notification_permission_asked")],
        )
    }

    @Test
    fun `o flag de notificacao nao contamina as configuracoes de triagem`() = runTest {
        val repo = repo()

        repo.markNotificationPermissionAsked()

        assertEquals(ScreeningSettings(), repo.snapshot())
    }

    @Test
    fun `a notificacao propria continua desligada por padrao`() = runTest {
        assertEquals(false, repo().snapshot().showOwnNotification)
    }

    @Test
    fun `retencao MANUAL persistida nao produz cutoff`() = runTest {
        val repo = repo()

        repo.update { it.copy(retentionPolicy = RetentionPolicy.MANUAL) }

        assertNull(repo.snapshot().retentionPolicy.cutoffUtcMillis(1_000_000L))
    }
}

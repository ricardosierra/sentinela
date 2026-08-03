package org.sentinela.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.telecom.call.DialerModeState

/**
 * Contrato da tela Proteção.
 *
 * Os casos de EFEITO IMEDIATO correm contra o repositório REAL sobre pasta temporária — é o que prova
 * que o retrato servido à triagem passa a reportar o valor novo. Dublê de repositório provaria apenas
 * que a função foi chamada, e "foi chamada" não é a promessa; a promessa é que a triagem vê.
 *
 * Armadilha herdada de 06-08, e ela é do TESTE, nunca do produto: gravar e consultar o retrato no
 * instante seguinte é CORRIDA, porque o retrato vem de um cache mantido por coletor assíncrono
 * (Fase 3). Por isso cada caso ESPERA o valor gravado ser reportado, em vez de ler logo depois.
 *
 * Armadilha herdada da Fase 3: `@get:Rule` combinado com `@JvmField` desliga a regra do JUnit e a
 * pasta temporária nunca é criada. Aqui há só `@get:Rule`, de propósito.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var contador = 0
    private val podas = mutableListOf<Long>()
    private var limpezas = 0

    @Before
    fun ligarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun desligar() {
        Dispatchers.resetMain()
        // Sem cancelar, o arquivo do DataStore continua travado e o próximo caso quebra.
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    // ------------------------------------------------------------------------------------------
    // Efeito imediato, contra o repositório real.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `trocar a politica de desconhecidos grava na hora e o retrato passa a reporta-la`() =
        runTest {
            val repo = repositorioReal()
            val vm = criar(repo)

            vm.definirPoliticaDeDesconhecidos(OriginPolicy.SILENCE)

            // Espera o valor gravado ser REPORTADO: ler o retrato no instante seguinte é corrida.
            val reportado = repo.settings.first { it.unknownPolicy == OriginPolicy.SILENCE }
            assertEquals(OriginPolicy.SILENCE, reportado.unknownPolicy)
            assertEquals(OriginPolicy.SILENCE, repo.snapshot().unknownPolicy)
        }

    @Test
    fun `desligar a protecao vale na triagem sem nenhum botao salvar`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo)

        vm.definirProtecao(false)

        assertFalse(repo.settings.first { !it.protectionEnabled }.protectionEnabled)
        assertFalse(repo.snapshot().protectionEnabled)
    }

    @Test
    fun `trocar o modo de bloqueio grava na hora`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo)

        vm.definirModoDeBloqueio(BlockMode.SILENT_VOICEMAIL)

        assertEquals(
            BlockMode.SILENT_VOICEMAIL,
            repo.settings.first { it.blockMode == BlockMode.SILENT_VOICEMAIL }.blockMode,
        )
    }

    @Test
    fun `duas trocas seguidas chegam as duas - nada e acumulado para gravar depois`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo)

        vm.definirPoliticaDeContatos(OriginPolicy.SILENCE)
        vm.definirBloqueioDePrivados(false)

        val reportado = repo.settings.first {
            it.contactsPolicy == OriginPolicy.SILENCE && !it.blockPrivateNumbers
        }
        assertEquals(OriginPolicy.SILENCE, reportado.contactsPolicy)
        assertFalse(reportado.blockPrivateNumbers)
    }

    @Test
    fun `nao existe funcao de salvar - a ausencia dela e o contrato`() {
        val nomes = SettingsViewModel::class.java.methods.map { it.name.lowercase() }

        assertTrue(
            "o dono de estado precisa ter funções de gravar por item",
            nomes.any { it.startsWith("definir") },
        )
        assertFalse(
            "uma função de salvar reintroduziria a divergência entre tela e triagem: $nomes",
            nomes.any { it == "salvar" || it == "save" || it == "commit" || it == "aplicar" },
        )
    }

    // ------------------------------------------------------------------------------------------
    // Retenção e histórico: só o que perde dado poda.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `escolher nao guardar na retencao poda os registros existentes`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo, agora = 555_000L)

        vm.definirRetencao(RetentionPolicy.NEVER_STORE)

        repo.settings.first { it.retentionPolicy == RetentionPolicy.NEVER_STORE }
        assertEquals("a poda precisa acontecer, com o agora do relógio", listOf(555_000L), podas)
    }

    @Test
    fun `escolher uma janela que guarda NAO poda nada`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo)

        vm.definirRetencao(RetentionPolicy.DAYS_7)

        repo.settings.first { it.retentionPolicy == RetentionPolicy.DAYS_7 }
        assertEquals(emptyList<Long>(), podas)
    }

    @Test
    fun `desligar o historico NAO apaga registro algum`() = runTest {
        val repo = repositorioReal()
        val vm = criar(repo)

        vm.definirHistoricoLigado(false)

        assertFalse(repo.settings.first { !it.historyEnabled }.historyEnabled)
        assertEquals("desligar não é apagar", emptyList<Long>(), podas)
        assertEquals(0, limpezas)
    }

    @Test
    fun `limpar historico apaga todos os registros`() = runTest {
        val vm = criar(repositorioReal())

        vm.limparHistorico()

        assertEquals(1, limpezas)
    }

    @Test
    fun `o estado reporta quantos registros existem para a confirmacao poder dizer`() = runTest {
        val vm = criar(repositorioReal(), total = 7L)
        val estado = observar(vm)

        assertEquals(7L, estado().historyRecordCount)
    }

    // ------------------------------------------------------------------------------------------
    // Papéis: derivados de consulta viva.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `o estado do modo discador e derivado das consultas ao sistema`() = runTest {
        discadorDetido = false
        agendaConcedida = true
        val vm = criar(repositorioReal())
        val estado = observar(vm)
        assertEquals(DialerModeState.OFFERED, estado().dialerMode)

        discadorDetido = true
        vm.reconsultarPapeis()

        assertEquals(DialerModeState.ACTIVE, estado().dialerMode)
    }

    @Test
    fun `agenda negada bloqueia a oferta do modo discador sem valor gravado no meio`() = runTest {
        discadorDetido = false
        agendaConcedida = false
        val vm = criar(repositorioReal())
        val estado = observar(vm)

        assertEquals(DialerModeState.BLOCKED_BY_CONTACTS, estado().dialerMode)
    }

    @Test
    fun `papel de triagem reconsultado aparece no estado`() = runTest {
        triagemDetida = false
        val vm = criar(repositorioReal())
        val estado = observar(vm)
        assertFalse(estado().screeningRoleHeld)

        triagemDetida = true
        vm.reconsultarPapeis()

        assertTrue(estado().screeningRoleHeld)
    }

    // ------------------------------------------------------------------------------------------
    // Apoio.
    // ------------------------------------------------------------------------------------------

    private var triagemDetida = false
    private var discadorDetido = false
    private var agendaConcedida = true

    private fun TestScope.observar(vm: SettingsViewModel): () -> SettingsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect { } }
        return { vm.estado.value }
    }

    private fun TestScope.repositorioReal(): DataStoreSettingsRepository {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes += it }
        val file = tmp.newFile("protecao-${contador++}.preferences_pb").also { it.delete() }
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }
        return DataStoreSettingsRepository(ds, scope)
    }

    private fun criar(
        repo: DataStoreSettingsRepository,
        total: Long = 0L,
        agora: Long = 0L,
    ) = SettingsViewModel(
        settings = repo,
        history = historico(total),
        roleHeld = { triagemDetida },
        roleAvailable = { true },
        requestRoleIntent = { null },
        dialerRoleHeld = { discadorDetido },
        dialerRoleAvailable = { true },
        contactsGranted = { agendaConcedida },
        dialerOptedIn = flowOf(false),
        clock = { agora },
    )

    private fun historico(total: Long) = object : BlockedCallRepository {
        override suspend fun record(entry: BlockedCallEntry): Long = 0L
        override fun observeRecent() = MutableStateFlow(emptyList<BlockedCallEntry>())
        override fun observeTotalCount() = MutableStateFlow(total)
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun clearAll() {
            limpezas++
        }
        override suspend fun updateClassification(
            id: Long,
            classification: org.sentinela.app.data.local.CallClassification
        ) = Unit

        override suspend fun pruneOlderThan(utcMillis: Long) {
            podas += utcMillis
        }

        override suspend fun hasRecentBlock(
            numberE164: String?,
            nowUtcMillis: Long,
        ): RepeatedCallLookup = RepeatedCallLookup.MISS
    }

}

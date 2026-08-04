package org.sentinela.app.ui.home

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Contrato do dono de estado da home.
 *
 * O caso do zero VERDADEIRO e o do zero PROIBIDO ficam lado a lado nesta classe de propósito: quem
 * "consertar" um deles mais tarde vai ver o outro ficar vermelho no mesmo arquivo, em vez de
 * descobrir o defeito na tela do usuário.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun ligarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun desligarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------------------------------
    // O zero mentiroso: os dois lados da regra, no mesmo lugar.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `historico desligado deixa as duas contagens indisponiveis e nao publica a ultima`() =
        runTest {
            val vm = criar(
                config = ScreeningSettings(historyEnabled = false),
                total = 42L,
                recentes = listOf(registro()),
            )
            val estado = observar(vm)

            assertEquals(StatValue.Unavailable, estado().totalBlocked)
            assertEquals(StatValue.Unavailable, estado().blockedToday)
            assertNull("histórico desligado não publica última bloqueada", estado().lastBlocked)
            assertFalse("desligar não é erro", estado().readError)
        }

    @Test
    fun `retencao que nao guarda deixa as duas contagens indisponiveis`() = runTest {
        val vm = criar(
            config = ScreeningSettings(retentionPolicy = RetentionPolicy.NEVER_STORE),
            total = 42L,
            recentes = listOf(registro()),
        )
        val estado = observar(vm)

        assertEquals(StatValue.Unavailable, estado().totalBlocked)
        assertEquals(StatValue.Unavailable, estado().blockedToday)
        assertFalse(estado().historyEnabled)
    }

    @Test
    fun `historico ligado com contagem zero carrega zero porque ali zero e verdade`() = runTest {
        val vm = criar(config = ScreeningSettings(), total = 0L, recentes = emptyList())
        val estado = observar(vm)

        assertEquals(StatValue.Loaded(0L), estado().totalBlocked)
        assertEquals(StatValue.Loaded(0L), estado().blockedToday)
        assertTrue(estado().historyEnabled)
    }

    @Test
    fun `antes da primeira emissao as contagens ficam carregando e nunca em zero`() = runTest {
        // Fluxo de configurações que nunca emite: é o primeiro quadro, antes de o disco responder.
        val vm = criar(
            configFlow = MutableSharedFlow(),
            total = 42L,
            recentes = listOf(registro()),
        )
        val estado = observar(vm)

        assertEquals(StatValue.Loading, estado().totalBlocked)
        assertEquals(StatValue.Loading, estado().blockedToday)
        assertNull(estado().lastBlocked)
    }

    @Test
    fun `falha de leitura do historico deixa contagens indisponiveis e marca o erro`() = runTest {
        val vm = criar(
            config = ScreeningSettings(),
            totalFlow = flow { throw IllegalStateException("disco") },
            recentes = emptyList(),
        )
        val estado = observar(vm)

        assertEquals(StatValue.Unavailable, estado().totalBlocked)
        assertEquals(StatValue.Unavailable, estado().blockedToday)
        assertTrue("a falha precisa ser visível na tela", estado().readError)
        assertNull(estado().lastBlocked)
    }

    // ------------------------------------------------------------------------------------------
    // Fronteira do número.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `ultima bloqueada e publicada mascarada e o numero completo nao existe no estado`() =
        runTest {
            val vm = criar(
                config = ScreeningSettings(),
                total = 1L,
                recentes = listOf(registro(numero = CRU, mascara = "lixo antigo")),
            )
            val estado = observar(vm)

            val ultima = estado().lastBlocked
            assertNotNull("a última bloqueada precisa ser publicada", ultima)
            assertEquals(MASCARA, ultima?.maskedNumber)
            assertFalse(
                "o estado não pode carregar a sequência completa de dígitos",
                ultima.toString().contains(CRU),
            )
            assertEquals(0L, estado().lastBlocked?.timestampUtcMillis)
            assertFalse(estado().toString().contains(CRU))
        }

    @Test
    fun `motivo real da decisao vira rotulo sem nenhuma classificacao de risco`() = runTest {
        val vm = criar(
            config = ScreeningSettings(),
            total = 1L,
            recentes = listOf(registro(reason = DecisionReason.PRIVATE_NUMBER)),
        )
        val estado = observar(vm)

        assertEquals(R.string.history_private_number, estado().lastBlocked?.reasonLabelRes)
        assertEquals(
            R.string.history_unknown_number,
            reasonLabelRes(DecisionReason.UNKNOWN_NUMBER),
        )
        assertEquals(R.string.call_origin_contact, reasonLabelRes(DecisionReason.CONTACT))
        assertEquals(
            R.string.call_origin_whitelist,
            reasonLabelRes(DecisionReason.PERSONAL_WHITELIST),
        )
    }

    // ------------------------------------------------------------------------------------------
    // Derivações.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `contagem do dia sai do relogio injetado e ignora o que e de ontem`() = runTest {
        val agora = 1_800_000_000_000L
        val ontem = agora - RetentionPolicy.MILLIS_PER_DAY
        val vm = criar(
            config = ScreeningSettings(),
            total = 3L,
            recentes = listOf(registro(quando = agora), registro(quando = agora - 1_000L), registro(quando = ontem)),
            clock = { agora },
        )
        val estado = observar(vm)

        assertEquals(StatValue.Loaded(3L), estado().totalBlocked)
        assertEquals(StatValue.Loaded(2L), estado().blockedToday)
    }

    @Test
    fun `estado reflete protecao papel e permissao de agenda`() = runTest {
        val vm = criar(
            config = ScreeningSettings(protectionEnabled = false),
            total = 0L,
            recentes = emptyList(),
            roleHeld = { true },
            roleAvailable = { true },
            contactsState = { ContactsPermissionState.DENIED_PERMANENTLY },
        )
        val estado = observar(vm)

        assertFalse(estado().protectionEnabled)
        assertTrue(estado().screeningRoleHeld)
        assertTrue(estado().screeningRoleAvailable)
        assertEquals(ContactsPermissionState.DENIED_PERMANENTLY, estado().contactsPermission)
    }

    @Test
    fun `intencao de pedido do papel e nula quando o aparelho nao oferece o papel`() = runTest {
        val vm = criar(config = ScreeningSettings(), total = 0L, recentes = emptyList())

        assertNull(vm.intencaoDePedidoDoPapel())
    }

    // ------------------------------------------------------------------------------------------
    // Apoio.
    // ------------------------------------------------------------------------------------------

    private fun TestScope.observar(vm: HomeViewModel): () -> HomeUiState {
        // Assinatura viva: sem coletor, a partilha com sobrevida nunca liga o fluxo de origem.
        // O coletor precisa rodar no despachante NÃO CONFINADO — o escopo de fundo do runTest usa
        // o despachante padrão, que só entrega quando o tempo virtual avança, e aí o valor lido
        // logo depois seria sempre o inicial. Com o não confinado tudo é entregue em linha e a
        // leitura é determinística, sem nenhum avanço de tempo virtual.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect { } }
        return { vm.estado.value }
    }

    @Suppress("LongParameterList")
    private fun criar(
        config: ScreeningSettings? = null,
        configFlow: Flow<ScreeningSettings>? = null,
        total: Long = 0L,
        totalFlow: Flow<Long>? = null,
        recentes: List<BlockedCallEntry> = emptyList(),
        roleHeld: () -> Boolean = { false },
        roleAvailable: () -> Boolean = { false },
        requestRoleIntent: () -> android.content.Intent? = { null },
        contactsState: () -> ContactsPermissionState = { ContactsPermissionState.GRANTED },
        clock: () -> Long = { 0L },
    ) = HomeViewModel(
        settings = repositorioDeConfiguracoes(configFlow ?: flowOf(config ?: ScreeningSettings())),
        history = historico(totalFlow ?: flowOf(total), recentes),
        roleHeld = roleHeld,
        roleAvailable = roleAvailable,
        requestRoleIntent = requestRoleIntent,
        contactsState = contactsState,
        dialerRoleHeld = { false },
        dialerRoleAvailable = { false },
        dialerOptedIn = { false },
        mask = { if (it == CRU) MASCARA else "+** ****" },
        clock = clock,
    )

    private companion object {

        const val CRU = "+5511912345678"
        const val MASCARA = "+55 11 9****-5678"

        fun repositorioDeConfiguracoes(fluxo: Flow<ScreeningSettings>) =
            mockk<DataStoreSettingsRepository> {
                every { settings } returns fluxo
                every { appOpenCount } returns kotlinx.coroutines.flow.flowOf(0)
                every { ratingAccepted } returns kotlinx.coroutines.flow.flowOf(false)
            }

        fun historico(total: Flow<Long>, recentes: List<BlockedCallEntry>) =
            object : BlockedCallRepository {
                override suspend fun record(entry: BlockedCallEntry): Long = 0L
                override fun observeRecent() = MutableStateFlow(recentes)
                override fun observeTotalCount() = total
                override suspend fun deleteById(id: Long) = Unit
                override suspend fun clearAll() = Unit
                override suspend fun updateClassification(
                    id: Long,
                    classification: org.sentinela.app.data.local.CallClassification
                ) = Unit
                override suspend fun pruneOlderThan(utcMillis: Long) = Unit
                override suspend fun hasRecentBlock(
                    numberE164: String?,
                    nowUtcMillis: Long,
                ): RepeatedCallLookup = RepeatedCallLookup.MISS
            }

        fun registro(
            numero: String? = CRU,
            mascara: String = MASCARA,
            quando: Long = 0L,
            reason: DecisionReason = DecisionReason.UNKNOWN_NUMBER,
        ) = BlockedCallEntry(
            maskedNumber = mascara,
            numberE164 = numero,
            timestampUtcMillis = quando,
            reason = reason,
            notificationShown = false,
        )
    }
}

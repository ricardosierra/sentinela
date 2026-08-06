package org.sentinela.app.ui.history

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * O critério 3 da Fase 8 pede filtro por período **e** por decisão. Só o de período existia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDecisionFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<BlockedCallRepository>()
    private val whitelistRepository = mockk<PersonalWhitelistRepository>()
    private val normalizer = mockk<PhoneNumberNormalizer>()

    private val agora = 1_000_000_000L

    private fun entrada(id: Long, classificacao: CallClassification) = BlockedCallEntry(
        id = id,
        maskedNumber = "+55 11 9****-1234",
        numberE164 = "+551199999$id",
        timestampUtcMillis = agora,
        reason = DecisionReason.UNKNOWN_NUMBER,
        notificationShown = false,
        classification = classificacao,
    )

    private val registros = listOf(
        entrada(1L, CallClassification.UNCLASSIFIED),
        entrada(2L, CallClassification.LEGITIMATE),
        entrada(3L, CallClassification.UNWANTED),
        entrada(4L, CallClassification.UNWANTED),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeRecent() } returns flowOf(registros)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HistoryViewModel(
        repository = repository,
        whitelistRepository = whitelistRepository,
        normalizer = normalizer,
        clock = { agora },
    )

    @Test
    fun `sem filtro de decisao a lista vem inteira`() = runTest {
        viewModel().uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())
            assertEquals(4, (awaitItem() as HistoryUiState.Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtro de indesejadas devolve so as marcadas assim`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())
            awaitItem()

            vm.setDecisionFilter(HistoryDecisionFilter.UNWANTED)
            val conteudo = awaitItem() as HistoryUiState.Content

            assertEquals(2, conteudo.items.size)
            assertEquals(listOf(3L, 4L), conteudo.items.map { it.id })
            assertEquals(HistoryDecisionFilter.UNWANTED, conteudo.decisionFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtro de legitimas devolve so a marcada assim`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())
            awaitItem()

            vm.setDecisionFilter(HistoryDecisionFilter.LEGITIMATE)
            val conteudo = awaitItem() as HistoryUiState.Content

            assertEquals(listOf(2L), conteudo.items.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Os dois eixos são independentes: escolher decisão não pode zerar o período escolhido. */
    @Test
    fun `periodo e decisao valem ao mesmo tempo`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())
            awaitItem()

            vm.setFilter(HistoryFilter.TODAY)
            awaitItem()
            vm.setDecisionFilter(HistoryDecisionFilter.UNWANTED)
            val conteudo = awaitItem() as HistoryUiState.Content

            assertEquals(HistoryFilter.TODAY, conteudo.filter)
            assertEquals(HistoryDecisionFilter.UNWANTED, conteudo.decisionFilter)
            assertEquals(2, conteudo.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Combinação sem resultado precisa devolver vazio honesto, e não a lista inteira de volta —
     * que é o modo silencioso de um filtro parecer funcionar sem filtrar.
     */
    @Test
    fun `combinacao sem resultado devolve vazio`() = runTest {
        val antigo = entrada(9L, CallClassification.LEGITIMATE)
            .copy(timestampUtcMillis = agora - 10 * 24 * 60 * 60 * 1000L)
        every { repository.observeRecent() } returns flowOf(listOf(antigo))

        val vm = viewModel()
        vm.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())
            // Sozinho, o registro antigo aparece.
            assertEquals(1, (awaitItem() as HistoryUiState.Content).items.size)

            // Ele é legítimo, mas está fora de "hoje": os dois eixos juntos não deixam nada.
            vm.setFilter(HistoryFilter.TODAY)
            assertEquals(HistoryUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

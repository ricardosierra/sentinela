package org.sentinela.app.ui.whitelist

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.db.WhitelistEntity
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

@OptIn(ExperimentalCoroutinesApi::class)
class WhitelistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<PersonalWhitelistRepository>()
    private val normalizer = mockk<PhoneNumberNormalizer> {
        every { normalize(any(), any()) } answers { 
            val input = firstArg<String>()
            if (input == "invalid") NormalizationResult.Invalid("invalid") 
            else NormalizationResult.Valid(input)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading and emits Empty when repository is empty`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())
        val viewModel = WhitelistViewModel(repository, normalizer)

        viewModel.uiState.test {
            assertEquals(WhitelistUiState.Loading, awaitItem()) // initial state
            assertEquals(WhitelistUiState.Empty, awaitItem()) // after observeAll
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Content when repository has items`() = runTest {
        val items = listOf(org.sentinela.app.data.local.WhitelistEntry(1L, "+5511999999999", null, true, 0L))
        every { repository.observeAll() } returns flowOf(items)
        val viewModel = WhitelistViewModel(repository, normalizer)

        viewModel.uiState.test {
            assertEquals(WhitelistUiState.Loading, awaitItem())
            val content = awaitItem() as WhitelistUiState.Content
            assertEquals(1, content.items.size)
            assertEquals("", content.query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search query triggers repository search`() = runTest {
        val allItems = listOf(org.sentinela.app.data.local.WhitelistEntry(1L, "+5511999999999", "Test", true, 0L))
        val searchItems = listOf(org.sentinela.app.data.local.WhitelistEntry(1L, "+5511999999999", "Test", true, 0L))
        
        every { repository.observeAll() } returns flowOf(allItems)
        every { repository.search("Test") } returns flowOf(searchItems)
        
        val viewModel = WhitelistViewModel(repository, normalizer)

        viewModel.uiState.test {
            assertEquals(WhitelistUiState.Loading, awaitItem())
            awaitItem() // Content from observeAll

            viewModel.onSearchQueryChanged("Test")
            val searchedContent = awaitItem() as WhitelistUiState.Content
            
            assertEquals(1, searchedContent.items.size)
            assertEquals("Test", searchedContent.query)
            assertTrue(searchedContent.isSearching)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addOrEdit inserts valid new number`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())
        coEvery { repository.contains("+5511999999999") } returns false
        coEvery { repository.upsert(any()) } returns Unit
        
        val viewModel = WhitelistViewModel(repository, normalizer)
        viewModel.addOrEdit("+5511999999999", "Desc", true)
        
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.upsert(any()) }
    }

    @Test
    fun `delete removes item`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())
        coEvery { repository.delete(1L) } returns Unit
        
        val viewModel = WhitelistViewModel(repository, normalizer)
        viewModel.delete(1L)
        
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.delete(1L) }
    }
}

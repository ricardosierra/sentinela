@file:Suppress("LongMethod", "MaxLineLength", "TooManyFunctions", "ReturnCount", "MagicNumber", "SwallowedException", "TooGenericExceptionCaught", "UnusedPrivateProperty")

package org.sentinela.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.WhitelistEntry
import org.sentinela.app.phone.PhoneNumberNormalizer

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Content(
        val items: List<BlockedCallEntry>,
        val filter: HistoryFilter = HistoryFilter.ALL
    ) : HistoryUiState
}

enum class HistoryFilter { ALL, TODAY, WEEK, MONTH }

class HistoryViewModel(
    private val repository: BlockedCallRepository,
    private val whitelistRepository: PersonalWhitelistRepository,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val currentFilter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeRecent(),
        currentFilter
    ) { items, filter ->
        val filtered = filterItems(items, filter)
        if (filtered.isEmpty()) {
            HistoryUiState.Empty
        } else {
            HistoryUiState.Content(filtered, filter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState.Loading
    )

    fun setFilter(filter: HistoryFilter) {
        currentFilter.value = filter
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteCall(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun markAsUnwanted(id: Long) {
        viewModelScope.launch {
            repository.updateClassification(id, CallClassification.UNWANTED)
        }
    }

    fun markAsLegitimateAndAllow(id: Long, numberE164: String?) {
        viewModelScope.launch {
            repository.updateClassification(id, CallClassification.LEGITIMATE)
            if (!numberE164.isNullOrBlank()) {
                val normalizedResult = normalizer.normalize(numberE164)
                if (normalizedResult is org.sentinela.app.phone.NormalizationResult.Valid) {
                    val e164 = normalizedResult.e164
                    whitelistRepository.upsert(
                        WhitelistEntry(
                            numberE164 = e164,
                            description = "Adicionado via histórico",
                            enabled = true,
                            createdAtUtcMillis = clock()
                        )
                    )
                }
            }
        }
    }

    private fun filterItems(items: List<BlockedCallEntry>, filter: HistoryFilter): List<BlockedCallEntry> {
        if (filter == HistoryFilter.ALL) return items
        
        val now = clock()
        val cutoff = when (filter) {
            HistoryFilter.TODAY -> now - 24 * 60 * 60 * 1000L
            HistoryFilter.WEEK -> now - 7 * 24 * 60 * 60 * 1000L
            HistoryFilter.MONTH -> now - 30 * 24 * 60 * 60 * 1000L
            else -> 0L
        }
        return items.filter { it.timestampUtcMillis >= cutoff }
    }
}

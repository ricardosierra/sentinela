@file:Suppress("MagicNumber")

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
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Content(
        val items: List<BlockedCallEntry>,
        val filter: HistoryFilter = HistoryFilter.ALL,
        val decisionFilter: HistoryDecisionFilter = HistoryDecisionFilter.ALL,
    ) : HistoryUiState
}

enum class HistoryFilter { ALL, TODAY, WEEK, MONTH }

/**
 * Filtro por decisão, exigido junto com o de período pelo critério 3 da Fase 8.
 *
 * O eixo é a classificação que o **usuário** deu ao registro, e não o tipo da decisão do motor.
 * A razão é o formato do dado: o trabalho posterior à resposta só grava chamada efetivamente
 * barrada (`PostScreeningWork` sai cedo quando a decisão não bloqueia), então um filtro
 * bloqueada/silenciada/permitida teria duas gavetas sempre vazias e mentiria sobre o conteúdo.
 * O que de fato varia aqui é o veredito do usuário — e é ele que as ações do registro alteram.
 */
enum class HistoryDecisionFilter { ALL, UNCLASSIFIED, LEGITIMATE, UNWANTED }

class HistoryViewModel(
    private val repository: BlockedCallRepository,
    private val whitelistRepository: PersonalWhitelistRepository,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val currentFilter = MutableStateFlow(HistoryFilter.ALL)
    private val currentDecisionFilter = MutableStateFlow(HistoryDecisionFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeRecent(),
        currentFilter,
        currentDecisionFilter,
    ) { items, filter, decisionFilter ->
        val filtered = filterByDecision(filterItems(items, filter), decisionFilter)
        if (filtered.isEmpty()) {
            HistoryUiState.Empty
        } else {
            HistoryUiState.Content(filtered, filter, decisionFilter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = HistoryUiState.Loading
    )

    fun setFilter(filter: HistoryFilter) {
        currentFilter.value = filter
    }

    fun setDecisionFilter(filter: HistoryDecisionFilter) {
        currentDecisionFilter.value = filter
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

    /**
     * @param description rótulo do registro criado na whitelist. Vem de fora porque o texto mora
     *   em `strings.xml` e este dono de estado não enxerga recursos — e porque ele é gravado no
     *   banco, então precisa nascer traduzível em vez de literal em Kotlin.
     */
    fun markAsLegitimateAndAllow(id: Long, numberE164: String?, description: String) {
        viewModelScope.launch {
            repository.updateClassification(id, CallClassification.LEGITIMATE)
            if (!numberE164.isNullOrBlank()) {
                val normalizedResult = normalizer.normalize(numberE164)
                if (normalizedResult is NormalizationResult.Valid) {
                    whitelistRepository.upsert(
                        WhitelistEntry(
                            numberE164 = normalizedResult.e164,
                            description = description,
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
            HistoryFilter.ALL -> 0L
            HistoryFilter.TODAY -> now - DIA_EM_MILLIS
            HistoryFilter.WEEK -> now - 7 * DIA_EM_MILLIS
            HistoryFilter.MONTH -> now - 30 * DIA_EM_MILLIS
        }
        return items.filter { it.timestampUtcMillis >= cutoff }
    }

    private fun filterByDecision(
        items: List<BlockedCallEntry>,
        filter: HistoryDecisionFilter,
    ): List<BlockedCallEntry> = when (filter) {
        HistoryDecisionFilter.ALL -> items
        HistoryDecisionFilter.UNCLASSIFIED ->
            items.filter { it.classification == CallClassification.UNCLASSIFIED }
        HistoryDecisionFilter.LEGITIMATE ->
            items.filter { it.classification == CallClassification.LEGITIMATE }
        HistoryDecisionFilter.UNWANTED ->
            items.filter { it.classification == CallClassification.UNWANTED }
    }

    private companion object {
        const val DIA_EM_MILLIS = 24 * 60 * 60 * 1000L
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}

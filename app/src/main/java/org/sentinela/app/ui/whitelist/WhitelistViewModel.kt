package org.sentinela.app.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WhitelistViewModel(
    private val repository: PersonalWhitelistRepository,
    private val normalizer: PhoneNumberNormalizer,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<WhitelistUiState> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.observeAll().map { list ->
                    if (list.isEmpty()) WhitelistUiState.Empty else WhitelistUiState.Content(list, query = "")
                }
            } else {
                repository.search(query).map { list ->
                    WhitelistUiState.Content(list, query = query, isSearching = true)
                }
            }
        }
        .catch { emit(WhitelistUiState.Error("Falha ao carregar a whitelist")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WhitelistUiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    /**
     * Retorna a chave do número se válido e não duplicado, ou null em caso de erro.
     * Os erros podem ser de formatação (null) ou de duplicidade (lança exceção ou apenas ignora).
     */
    fun addOrEdit(rawNumber: String, description: String?, enabled: Boolean, existingId: Long? = null) {
        viewModelScope.launch {
            val normalizedResult = normalizer.normalize(rawNumber)
            if (normalizedResult !is NormalizationResult.Valid) {
                // TODO: Notificar UI de número inválido
                return@launch
            }
            val key = normalizedResult.e164
            // Verifica duplicidade apenas se for uma nova adição (id nulo)
            if (existingId == null && repository.contains(key)) {
                // TODO: Notificar duplicidade
                return@launch
            }
            repository.upsert(
                org.sentinela.app.data.local.WhitelistEntry(
                    id = existingId ?: 0L,
                    numberE164 = key,
                    description = description,
                    enabled = enabled,
                    createdAtUtcMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}

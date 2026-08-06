package org.sentinela.app.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.WhitelistEntry
import org.sentinela.app.data.local.db.WhitelistEntity
import org.sentinela.app.data.local.export.ImportResult
import org.sentinela.app.data.local.export.WhitelistExporter
import org.sentinela.app.data.local.export.WhitelistImporter
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * O que aconteceu na última ação do usuário.
 *
 * Existe porque a versão anterior descartava em silêncio tanto número inválido quanto duplicado:
 * o diálogo fechava, nada era gravado e o usuário ficava achando que tinha dado certo. O critério
 * 3 da Fase 8 pede explicitamente que o duplicado seja **detectado** — detectar sem contar não
 * serve de nada.
 */
sealed interface WhitelistFeedback {
    data object InvalidNumber : WhitelistFeedback
    data object Duplicate : WhitelistFeedback
    data object Exported : WhitelistFeedback
    data object ExportFailed : WhitelistFeedback
    data class Imported(val added: Int, val duplicates: Int, val invalid: Int) : WhitelistFeedback
    data object ImportFailed : WhitelistFeedback
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WhitelistViewModel(
    private val repository: PersonalWhitelistRepository,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val _feedback = MutableStateFlow<WhitelistFeedback?>(null)
    val feedback: StateFlow<WhitelistFeedback?> = _feedback.asStateFlow()

    val uiState: StateFlow<WhitelistUiState> = searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS)
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
        .catch { emit(WhitelistUiState.Error) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = WhitelistUiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    /** Consome o aviso depois que a tela o mostrou, para ele não reaparecer na recomposição. */
    fun onFeedbackShown() {
        _feedback.value = null
    }

    fun addOrEdit(rawNumber: String, description: String?, enabled: Boolean, existingId: Long? = null) {
        viewModelScope.launch {
            val normalizedResult = normalizer.normalize(rawNumber)
            if (normalizedResult !is NormalizationResult.Valid) {
                _feedback.value = WhitelistFeedback.InvalidNumber
                return@launch
            }
            val key = normalizedResult.e164
            if (existingId == null && repository.contains(key)) {
                _feedback.value = WhitelistFeedback.Duplicate
                return@launch
            }
            repository.upsert(
                WhitelistEntry(
                    id = existingId ?: 0L,
                    numberE164 = key,
                    description = description,
                    enabled = enabled,
                    createdAtUtcMillis = clock(),
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    /**
     * Serializa a whitelist inteira. A versão anterior gravava a constante `{"whitelist":[]}` no
     * arquivo escolhido pelo usuário — um "backup" sempre vazio, que é pior do que não ter backup,
     * porque o usuário confia nele.
     */
    suspend fun exportJson(): String {
        val entries = repository.observeAll().first()
        return WhitelistExporter.exportToJson(entries.map(::toEntity))
    }

    fun onExportFinished(sucesso: Boolean) {
        _feedback.value = if (sucesso) WhitelistFeedback.Exported else WhitelistFeedback.ExportFailed
    }

    /**
     * Mescla o arquivo com a lista atual sem duplicar. A validação de formato, de limite e a
     * contagem de descartados vêm do [WhitelistImporter], que já é testado desde a Fase 8.
     */
    fun import(json: String) {
        viewModelScope.launch {
            val existentes = repository.observeAll().first()
            val resultado: ImportResult = WhitelistImporter.parseJson(
                jsonString = json,
                existingKeys = existentes.map { it.numberE164 }.toSet(),
                normalizer = normalizer,
                nowUtcMillis = clock(),
            )
            resultado.newEntities.forEach { entity ->
                repository.upsert(
                    WhitelistEntry(
                        numberE164 = entity.numberKey,
                        description = entity.description,
                        enabled = entity.enabled,
                        createdAtUtcMillis = entity.createdAtUtcMillis,
                    )
                )
            }
            _feedback.value = WhitelistFeedback.Imported(
                added = resultado.newEntities.size,
                duplicates = resultado.duplicatesSkipped,
                invalid = resultado.invalidSkipped,
            )
        }
    }

    fun onImportFailed() {
        _feedback.value = WhitelistFeedback.ImportFailed
    }

    private fun toEntity(entry: WhitelistEntry) = WhitelistEntity(
        id = entry.id,
        numberKey = entry.numberE164,
        description = entry.description,
        enabled = entry.enabled,
        createdAtUtcMillis = entry.createdAtUtcMillis,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}

package org.sentinela.app.ui.whitelist

import org.sentinela.app.data.local.WhitelistEntry

sealed interface WhitelistUiState {
    data object Loading : WhitelistUiState
    data object Empty : WhitelistUiState
    data class Content(
        val items: List<WhitelistEntry>,
        val query: String = "",
        val isSearching: Boolean = false
    ) : WhitelistUiState
    /** O texto do erro sai de `strings.xml` na tela — mensagem em Kotlin não é traduzível. */
    data object Error : WhitelistUiState
}

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
    data class Error(val message: String) : WhitelistUiState
}

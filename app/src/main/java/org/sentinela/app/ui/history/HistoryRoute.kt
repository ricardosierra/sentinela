package org.sentinela.app.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.sentinela.app.AppContainer

@Composable
fun HistoryRoute(
    container: AppContainer,
    bottomBar: @Composable () -> Unit,
) {
    // In a real app this would be injected via a ViewModel factory using the container.
    // We are simulating this for the scope of the screen.
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<HistoryViewModel>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(
                    repository = container.blockedCallRepository,
                    whitelistRepository = container.personalWhitelistRepository,
                    normalizer = container.phoneNumberNormalizer
                ) as T
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        onFilterChanged = viewModel::setFilter,
        onClearAll = viewModel::clearAll,
        onAllowNumber = viewModel::markAsLegitimateAndAllow,
        onMarkUnwanted = viewModel::markAsUnwanted,
        onDeleteEntry = viewModel::deleteCall,
        bottomBar = bottomBar,
    )
}

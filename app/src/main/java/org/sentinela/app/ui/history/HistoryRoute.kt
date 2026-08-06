package org.sentinela.app.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sentinela.app.AppContainer
import org.sentinela.app.R

@Composable
fun HistoryRoute(
    container: AppContainer,
    bottomBar: @Composable () -> Unit,
) {
    val viewModel = viewModel<HistoryViewModel>(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(
                    repository = container.blockedCallRepository,
                    whitelistRepository = container.whitelistRepository,
                    normalizer = container.phoneNumberNormalizer
                ) as T
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // O rótulo do registro criado na whitelist é gravado no banco, então sai daqui — de
    // `strings.xml` — e não de um literal dentro do dono de estado.
    val rotuloOrigem = stringResource(R.string.whitelist_added_from_history)

    HistoryScreen(
        state = state,
        onFilterChanged = viewModel::setFilter,
        onDecisionFilterChanged = viewModel::setDecisionFilter,
        onClearAll = viewModel::clearAll,
        onAllowNumber = { id, numero ->
            viewModel.markAsLegitimateAndAllow(id, numero, rotuloOrigem)
        },
        onMarkUnwanted = viewModel::markAsUnwanted,
        onDeleteEntry = viewModel::deleteCall,
        bottomBar = bottomBar,
    )
}

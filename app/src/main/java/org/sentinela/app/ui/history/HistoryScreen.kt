@file:Suppress("LongMethod", "TooManyFunctions", "LongParameterList")

package org.sentinela.app.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sentinela.app.R
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.ui.components.CheckRow
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
import org.sentinela.app.ui.theme.GeistMono
import org.sentinela.app.ui.theme.ShapeMedium

private val ScreenHorizontalPadding = 16.dp
private val ItemPadding = 16.dp
private val AvatarSize = 40.dp
private val IconGap = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onFilterChanged: (HistoryFilter) -> Unit,
    onClearAll: () -> Unit,
    onAllowNumber: (Long, String?) -> Unit,
    onMarkUnwanted: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SentinelaTopBar(
                center = {
                    Text(
                        text = stringResource(R.string.nav_history),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    SentinelaTopBarIconAction(
                        icon = Icons.Outlined.FilterList,
                        contentDescription = "Filtrar",
                        onClick = { showFilterSheet = true },
                    )
                    SentinelaTopBarIconAction(
                        icon = Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.history_clear_all),
                        onClick = { showClearConfirm = true },
                    )
                }
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is HistoryUiState.Loading -> {
                    // Loading state
                }
                is HistoryUiState.Empty -> {
                    HistoryEmptyState()
                }
                is HistoryUiState.Content -> {
                    HistoryContent(
                        items = state.items,
                        onAllowNumber = onAllowNumber,
                        onMarkUnwanted = onMarkUnwanted,
                        onDeleteEntry = onDeleteEntry
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = stringResource(R.string.history_clear_all)) },
            text = { Text(text = stringResource(R.string.settings_clear_history_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearAll()
                }) {
                    Text(text = stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                val currentFilter = (state as? HistoryUiState.Content)?.filter ?: HistoryFilter.ALL
                Text(
                    text = "Período",
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CheckRow(
                    label = "Todo o histórico",
                    checked = currentFilter == HistoryFilter.ALL,
                    onCheckedChange = {
                        onFilterChanged(HistoryFilter.ALL)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                    }
                )
                CheckRow(
                    label = "Hoje",
                    checked = currentFilter == HistoryFilter.TODAY,
                    onCheckedChange = {
                        onFilterChanged(HistoryFilter.TODAY)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                    }
                )
                CheckRow(
                    label = "Últimos 7 dias",
                    checked = currentFilter == HistoryFilter.WEEK,
                    onCheckedChange = {
                        onFilterChanged(HistoryFilter.WEEK)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                    }
                )
                CheckRow(
                    label = "Últimos 30 dias",
                    checked = currentFilter == HistoryFilter.MONTH,
                    onCheckedChange = {
                        onFilterChanged(HistoryFilter.MONTH)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryContent(
    items: List<BlockedCallEntry>,
    onAllowNumber: (Long, String?) -> Unit,
    onMarkUnwanted: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            HistoryItem(
                entry = item,
                onAllowNumber = { onAllowNumber(item.id, item.numberE164) },
                onMarkUnwanted = { onMarkUnwanted(item.id) },
                onDeleteEntry = { onDeleteEntry(item.id) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
private fun HistoryItem(
    entry: BlockedCallEntry,
    onAllowNumber: () -> Unit,
    onMarkUnwanted: () -> Unit,
    onDeleteEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val icon = when (entry.reason) {
        org.sentinela.app.telecom.CallDecision.Reason.BLOCKED_PRIVATE -> Icons.Outlined.VisibilityOff
        else -> Icons.Outlined.Block
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showMenu = true },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenHorizontalPadding, vertical = ItemPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.size(IconGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.maskedNumber,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = GeistMono),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(entry.reasonLabelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Temporarily omitting real relative time for brevity
            Text(
                text = "Agora",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Permitir (Whitelist)") },
                    onClick = {
                        showMenu = false
                        onAllowNumber()
                    },
                    leadingIcon = { Icon(Icons.Outlined.VerifiedUser, null) }
                )
                DropdownMenuItem(
                    text = { Text("Marcar Indesejado") },
                    onClick = {
                        showMenu = false
                        onMarkUnwanted()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Report, null) }
                )
                DropdownMenuItem(
                    text = { Text("Excluir") },
                    onClick = {
                        showMenu = false
                        onDeleteEntry()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                )
            }
        }
    }
}

@file:Suppress("LongMethod")

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.ui.components.CheckRow
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
import org.sentinela.app.ui.theme.ShapeMedium

/**
 * Os dois eixos do filtro, em pares (valor, rótulo). Ficam em lista para que acrescentar uma
 * opção não signifique repetir o bloco de clique — foi assim que o "fecha a folha ao escolher"
 * acabou colado em cada linha na versão anterior, impedindo combinar período com decisão.
 */
private val PERIODOS = listOf(
    HistoryFilter.ALL to R.string.history_filter_all,
    HistoryFilter.TODAY to R.string.history_filter_today,
    HistoryFilter.WEEK to R.string.history_filter_7days,
    HistoryFilter.MONTH to R.string.history_filter_30days,
)

private val DECISOES = listOf(
    HistoryDecisionFilter.ALL to R.string.history_decision_all,
    HistoryDecisionFilter.UNCLASSIFIED to R.string.history_decision_unclassified,
    HistoryDecisionFilter.LEGITIMATE to R.string.history_decision_legitimate,
    HistoryDecisionFilter.UNWANTED to R.string.history_decision_unwanted,
)

private val ScreenHorizontalPadding = 16.dp
private val ItemPadding = 16.dp
private val AvatarSize = 40.dp
private val IconGap = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    hasWhatsApp: Boolean = false,
    onFilterChanged: (HistoryFilter) -> Unit,
    onDecisionFilterChanged: (HistoryDecisionFilter) -> Unit,
    onClearAll: () -> Unit,
    onAllowNumber: (Long, String?) -> Unit,
    onMarkUnwanted: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    // O relógio entra por parâmetro para o tempo relativo de cada linha ser testável sem
    // depender da hora da máquina que roda a suíte.
    agoraUtcMillis: Long = System.currentTimeMillis(),
    registroEmDestaque: Long? = null,
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
                        hasWhatsApp = hasWhatsApp,
                        onAllowNumber = onAllowNumber,
                        onMarkUnwanted = onMarkUnwanted,
                        onDeleteEntry = onDeleteEntry,
                        agoraUtcMillis = agoraUtcMillis,
                        registroEmDestaque = registroEmDestaque,
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = stringResource(R.string.history_clear_all)) },
            text = { Text(text = stringResource(R.string.history_clear_confirm)) },
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
                val conteudo = state as? HistoryUiState.Content
                val currentFilter = conteudo?.filter ?: HistoryFilter.ALL
                val currentDecision = conteudo?.decisionFilter ?: HistoryDecisionFilter.ALL

                Text(
                    text = stringResource(R.string.history_filter_period),
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PERIODOS.forEach { (filtro, rotulo) ->
                    FilterRow(
                        label = stringResource(rotulo),
                        selected = currentFilter == filtro,
                        onClick = { onFilterChanged(filtro) }
                    )
                }

                Text(
                    text = stringResource(R.string.history_filter_decision),
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                DECISOES.forEach { (filtro, rotulo) ->
                    FilterRow(
                        label = stringResource(rotulo),
                        selected = currentDecision == filtro,
                        onClick = { onDecisionFilterChanged(filtro) }
                    )
                }

                TextButton(
                    onClick = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion { showFilterSheet = false }
                    },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
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
    hasWhatsApp: Boolean,
    onAllowNumber: (Long, String?) -> Unit,
    onMarkUnwanted: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    agoraUtcMillis: Long,
    registroEmDestaque: Long?,
) {
    val listState = rememberLazyListState()

    // Rola até o registro que a notificação apontou. Só uma vez por identificador, e só se ele
    // ainda existir na lista — o usuário pode ter apagado o registro antes de tocar no aviso.
    LaunchedEffect(registroEmDestaque, items) {
        val alvo = registroEmDestaque ?: return@LaunchedEffect
        val posicao = items.indexOfFirst { it.id == alvo }
        if (posicao >= 0) listState.animateScrollToItem(posicao)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            HistoryItem(
                entry = item,
                hasWhatsApp = hasWhatsApp,
                agoraUtcMillis = agoraUtcMillis,
                onAllowNumber = { onAllowNumber(item.id, item.numberE164) },
                onMarkUnwanted = { onMarkUnwanted(item.id) },
                onDeleteEntry = { onDeleteEntry(item.id) },
            )
        }
    }
}

@Composable
private fun HistoryItem(
    entry: BlockedCallEntry,
    hasWhatsApp: Boolean,
    agoraUtcMillis: Long,
    onAllowNumber: () -> Unit,
    onMarkUnwanted: () -> Unit,
    onDeleteEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val icon = when (entry.reason) {
        DecisionReason.PRIVATE_NUMBER -> Icons.Outlined.VisibilityOff
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(entry.reason.rotulo()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val tempo = tempoRelativo(entry.timestampUtcMillis, agoraUtcMillis)
            Text(
                text = tempo.quantidade
                    ?.let { stringResource(tempo.recurso, it) }
                    ?: stringResource(tempo.recurso),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history_action_allow_whitelist)) },
                    onClick = {
                        showMenu = false
                        onAllowNumber()
                    },
                    leadingIcon = { Icon(Icons.Outlined.VerifiedUser, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history_mark_unwanted)) },
                    onClick = {
                        showMenu = false
                        onMarkUnwanted()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Report, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history_action_delete)) },
                    onClick = {
                        showMenu = false
                        onDeleteEntry()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                )
                if (hasWhatsApp && entry.reason == DecisionReason.UNKNOWN_NUMBER) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = stringResource(R.string.whatsapp_history_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        onClick = { },
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

package org.sentinela.app.ui.whitelist

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sentinela.app.AppContainer
import org.sentinela.app.R
import org.sentinela.app.data.local.WhitelistEntry

/**
 * Limite de leitura do arquivo importado, em caracteres.
 *
 * O importador já corta em 10.000 entradas, mas esse corte só acontece DEPOIS de o arquivo inteiro
 * virar String na memória. Sem um teto aqui, escolher um arquivo grande no seletor derruba o
 * aplicativo antes de qualquer validação — e o seletor do sistema aceita qualquer arquivo.
 */
private const val MAX_IMPORT_CHARS = 4 * 1024 * 1024

private const val NOME_PADRAO_BACKUP = "sentinela_whitelist_backup.json"
private const val TIPO_BACKUP = "application/json"

@Composable
fun WhitelistRoute(
    container: AppContainer,
    nav: NavController,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(container) {
        WhitelistViewModel(container.whitelistRepository, container.phoneNumberNormalizer)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var entryToEdit by remember { mutableStateOf<WhitelistEntry?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    // Conteúdo lido do arquivo, aguardando confirmação. A mesclagem altera a lista do usuário:
    // perguntar antes é o critério 2 da Fase 8, não cortesia.
    var pendingImport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberExportLauncher(context, scope, viewModel)
    val importLauncher = rememberImportLauncher(context, scope, viewModel) { pendingImport = it }

    WhitelistFeedbackEffect(viewModel, snackbarHostState)

    WhitelistScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSearch = viewModel::onSearchQueryChanged,
        onAdd = {
            entryToEdit = null
            showDialog = true
        },
        onEdit = { entry ->
            entryToEdit = entry
            showDialog = true
        },
        onDelete = viewModel::delete,
        onExport = { exportLauncher.launch(NOME_PADRAO_BACKUP) },
        onImport = { importLauncher.launch(arrayOf(TIPO_BACKUP)) },
        bottomBar = bottomBar,
        onBack = { nav.popBackStack() },
    )

    if (showDialog) {
        WhitelistAddEditDialog(
            initialEntry = entryToEdit,
            onDismissRequest = { showDialog = false },
            onSave = { number, description ->
                viewModel.addOrEdit(number, description, true, entryToEdit?.id)
                showDialog = false
            },
        )
    }

    pendingImport?.let { conteudo ->
        ImportConfirmDialog(
            onConfirm = {
                pendingImport = null
                viewModel.import(conteudo)
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun rememberExportLauncher(
    context: Context,
    scope: CoroutineScope,
    viewModel: WhitelistViewModel,
): ManagedActivityResultLauncher<String, Uri?> =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_BACKUP)) { uri ->
        val destino = uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val sucesso = runCatching {
                val json = viewModel.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destino)?.use { stream ->
                        stream.write(json.toByteArray())
                    } ?: error("sem stream de escrita para o destino escolhido")
                }
            }.isSuccess
            viewModel.onExportFinished(sucesso)
        }
    }

@Composable
private fun rememberImportLauncher(
    context: Context,
    scope: CoroutineScope,
    viewModel: WhitelistViewModel,
    onLido: (String) -> Unit,
): ManagedActivityResultLauncher<Array<String>, Uri?> =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val origem = uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(origem)?.use { stream ->
                        val buffer = CharArray(MAX_IMPORT_CHARS)
                        val lidos = stream.reader().read(buffer)
                        if (lidos <= 0) "" else String(buffer, 0, lidos)
                    } ?: error("sem stream de leitura para o arquivo escolhido")
                }
            }.onSuccess(onLido).onFailure { viewModel.onImportFailed() }
        }
    }

/** Mostra o aviso da última ação e o consome, para ele não reaparecer na recomposição. */
@Composable
private fun WhitelistFeedbackEffect(
    viewModel: WhitelistViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()

    val textoInvalido = stringResource(R.string.whitelist_invalid_number)
    val textoDuplicado = stringResource(R.string.whitelist_duplicate)
    val textoExportado = stringResource(R.string.whitelist_export_ok)
    val textoExportFalhou = stringResource(R.string.whitelist_export_failed)
    val textoImportFalhou = stringResource(R.string.whitelist_import_failed)
    val atual = feedback
    val textoImportado = if (atual is WhitelistFeedback.Imported) {
        stringResource(
            R.string.whitelist_import_result,
            atual.added,
            atual.duplicates,
            atual.invalid,
        )
    } else {
        ""
    }

    LaunchedEffect(atual) {
        val aviso = atual ?: return@LaunchedEffect
        val texto = when (aviso) {
            WhitelistFeedback.InvalidNumber -> textoInvalido
            WhitelistFeedback.Duplicate -> textoDuplicado
            WhitelistFeedback.Exported -> textoExportado
            WhitelistFeedback.ExportFailed -> textoExportFalhou
            WhitelistFeedback.ImportFailed -> textoImportFalhou
            is WhitelistFeedback.Imported -> textoImportado
        }
        snackbarHostState.showSnackbar(texto)
        viewModel.onFeedbackShown()
    }
}

@Composable
private fun ImportConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whitelist_import)) },
        text = { Text(stringResource(R.string.whitelist_import_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

package org.sentinela.app.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Confirmação de perda de dado, uma das DUAS da tela: limpar o histórico.
 *
 * O corpo diz QUANTOS registros serão apagados — "apagar tudo" sem número é pedir consentimento no
 * escuro. O foco inicial vai no cancelar: numa ação irreversível, o gesto reflexo tem de cair na
 * saída segura.
 */
@Composable
internal fun ConfirmacaoDeLimpeza(registros: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.history_clear_all)) },
        text = {
            Text(
                text = pluralStringResource(
                    R.plurals.settings_clear_history_confirm,
                    registros.toInt(),
                    registros.toInt(),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * A segunda e última confirmação: escolher não guardar registro nenhum.
 *
 * Confirma porque a escolha muda o comportamento futuro E poda o que já existe. Nenhuma das outras
 * quatro janelas de retenção apaga nada, e por isso nenhuma delas confirma.
 */
@Composable
internal fun ConfirmacaoDeNaoGuardar(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_retention_never)) },
        text = { Text(text = stringResource(R.string.settings_retention_never_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(widthDp = 411, heightDp = 400)
@Composable
private fun ConfirmacaoDeLimpezaPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ConfirmacaoDeLimpeza(registros = 42, onConfirm = {}, onDismiss = {})
    }
}

@Preview(widthDp = 411, heightDp = 400)
@Composable
private fun ConfirmacaoDeNaoGuardarPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ConfirmacaoDeNaoGuardar(onConfirm = {}, onDismiss = {})
    }
}

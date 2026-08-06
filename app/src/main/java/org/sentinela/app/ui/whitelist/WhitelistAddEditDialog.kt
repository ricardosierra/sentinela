@file:Suppress("LongMethod", "MaxLineLength", "TooManyFunctions", "ReturnCount", "MagicNumber", "SwallowedException", "LoopWithTooManyJumpStatements")

package org.sentinela.app.ui.whitelist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.data.local.WhitelistEntry

@Composable
fun WhitelistAddEditDialog(
    initialEntry: WhitelistEntry? = null,
    onDismissRequest: () -> Unit,
    onSave: (number: String, description: String?) -> Unit
) {
    var number by remember { mutableStateOf(initialEntry?.numberE164 ?: "") }
    var description by remember { mutableStateOf(initialEntry?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (initialEntry == null) stringResource(R.string.whitelist_add_number) else stringResource(R.string.whitelist_edit))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.whitelist_number_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.whitelist_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(number, description.takeIf { it.isNotBlank() }) },
                enabled = number.isNotBlank()
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

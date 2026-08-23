package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sentinela.app.R

private val SheetPadding = 16.dp
private val TitleToBodyGap = 16.dp
private val BodyToButtonGap = 24.dp
private val ButtonGap = 12.dp
private val SheetBottomGap = 32.dp

/**
 * O convite para avaliar o aplicativo.
 *
 * Nao ganha `@Preview`: `ModalBottomSheet` monta a folha numa janela propria, e a ferramenta de
 * pre-visualizacao desenha o vazio atras dela em vez do conteudo. Uma pre-visualizacao que mostra
 * nada e pior que nenhuma — ela afirma que o bloco foi conferido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RatingBottomSheet(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SheetPadding)
                .padding(bottom = SheetBottomGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.review_prompt_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(TitleToBodyGap))
            Text(
                text = stringResource(R.string.review_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(BodyToButtonGap))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.review_prompt_rate))
            }
            Spacer(modifier = Modifier.height(ButtonGap))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.review_prompt_later))
            }
        }
    }
}

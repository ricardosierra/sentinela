package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val QuickActionGap = 12.dp

/** As duas linhas de atalho da home. Nao dependem de estado nenhum: so de para onde levam. */
@Composable
internal fun BlocoDeAtalhos(
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QuickActionGap)) {
        QuickActionRow(
            label = stringResource(R.string.dashboard_quick_whitelist),
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onOpenWhitelist,
        )
        QuickActionRow(
            label = stringResource(R.string.dashboard_quick_history),
            icon = Icons.Outlined.History,
            iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            onClick = onOpenHistory,
        )
    }
}

@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeAtalhosPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BlocoDeAtalhos(onOpenWhitelist = {}, onOpenHistory = {})
        }
    }
}

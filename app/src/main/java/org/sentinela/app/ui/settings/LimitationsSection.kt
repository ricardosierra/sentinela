package org.sentinela.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.ui.components.HonestyCard

/**
 * Item 15 — as quatro honestidades, pelos identificadores de recurso originais.
 *
 * Ver o KDoc de [SettingsScreen]: nenhuma destas frases é reescrita aqui.
 */
@Composable
internal fun CartaoDeLimitacoes() {
    HonestyCard(
        title = stringResource(R.string.dialer_activation_unchanged_title),
        items = listOf(
            stringResource(R.string.dialer_activation_unchanged_1),
            stringResource(R.string.dialer_activation_unchanged_2),
            stringResource(R.string.dialer_activation_unchanged_3),
            stringResource(R.string.dialer_activation_unchanged_4),
        ),
        itemIcon = Icons.Outlined.Info,
    )
}

@Preview(widthDp = 411, heightDp = 400)
@Composable
private fun CartaoDeLimitacoesPreview() {
    SecaoDeExemplo { CartaoDeLimitacoes() }
}

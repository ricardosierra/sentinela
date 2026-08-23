package org.sentinela.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.OriginPolicy

/** Item 4 — as quatro políticas da lista pessoal. */
@Composable
internal fun GrupoDaListaPessoal(state: SettingsUiState, onWhitelistPolicy: (OriginPolicy) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_whitelist_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_never_silence),
                description = stringResource(R.string.whitelist_option_never_silence_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.whitelistPolicy == OriginPolicy.NEVER_SILENCE,
                onClick = { onWhitelistPolicy(OriginPolicy.NEVER_SILENCE) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_ring),
                description = stringResource(R.string.whitelist_option_ring_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.whitelistPolicy == OriginPolicy.RING,
                onClick = { onWhitelistPolicy(OriginPolicy.RING) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_block),
                description = stringResource(R.string.whitelist_option_block_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.whitelistPolicy == OriginPolicy.BLOCK,
                onClick = { onWhitelistPolicy(OriginPolicy.BLOCK) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_silence),
                description = stringResource(R.string.whitelist_option_silence_desc),
                icon = Icons.Outlined.NotificationsOff,
                selected = state.settings.whitelistPolicy == OriginPolicy.SILENCE,
                onClick = { onWhitelistPolicy(OriginPolicy.SILENCE) },
            )
        }
    }
}


@Preview(widthDp = 411, heightDp = 700)
@Composable
private fun GrupoDaListaPessoalPreview() {
    SecaoDeExemplo {
        GrupoDaListaPessoal(state = SettingsUiState(loading = false), onWhitelistPolicy = {})
    }
}

package org.sentinela.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.components.SettingSwitchRow

/** Itens 2, 5 e 6 — o tratamento de quem não está na agenda nem na lista pessoal. */
@Composable
internal fun GrupoDeDesconhecidos(
    state: SettingsUiState,
    onUnknownPolicy: (OriginPolicy) -> Unit,
    onBlockPrivateChange: (Boolean) -> Unit,
    onBlockMode: (BlockMode) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_unknown_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.unknown_option_block),
                description = stringResource(R.string.unknown_option_block_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.unknownPolicy == OriginPolicy.BLOCK,
                onClick = { onUnknownPolicy(OriginPolicy.BLOCK) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.unknown_option_silence),
                description = stringResource(R.string.unknown_option_silence_desc),
                icon = Icons.Outlined.NotificationsOff,
                selected = state.settings.unknownPolicy == OriginPolicy.SILENCE,
                onClick = { onUnknownPolicy(OriginPolicy.SILENCE) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.unknown_option_allow),
                description = stringResource(R.string.unknown_option_allow_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.unknownPolicy == OriginPolicy.RING,
                onClick = { onUnknownPolicy(OriginPolicy.RING) },
            )
        }
        SettingSwitchRow(
            label = stringResource(R.string.settings_block_private),
            description = stringResource(R.string.settings_block_private_desc),
            checked = state.settings.blockPrivateNumbers,
            onCheckedChange = onBlockPrivateChange,
        )
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.settings_mode_reject),
                description = stringResource(R.string.settings_block_mode_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.blockMode == BlockMode.REJECT,
                onClick = { onBlockMode(BlockMode.REJECT) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.settings_mode_silent_voicemail),
                description = stringResource(R.string.settings_block_mode_desc),
                icon = Icons.Outlined.Voicemail,
                selected = state.settings.blockMode == BlockMode.SILENT_VOICEMAIL,
                onClick = { onBlockMode(BlockMode.SILENT_VOICEMAIL) },
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 900)
@Composable
private fun GrupoDeDesconhecidosPreview() {
    SecaoDeExemplo {
        GrupoDeDesconhecidos(
            state = SettingsUiState(loading = false),
            onUnknownPolicy = {},
            onBlockPrivateChange = {},
            onBlockMode = {},
        )
    }
}

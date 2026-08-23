package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.SettingSwitchRow

/** Mascaramento dos numeros na propria interface, com o aviso de alcance da privacidade. */
@Composable
internal fun GrupoDePrivacidade(state: SettingsUiState, onMaskNumbersChange: (Boolean) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_privacy_title)) {
        InfoBanner(
            text = stringResource(R.string.settings_privacy_disclaimer),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        SettingSwitchRow(
            label = stringResource(R.string.settings_mask_numbers),
            description = stringResource(R.string.settings_mask_numbers_desc),
            checked = state.settings.maskNumbers,
            onCheckedChange = onMaskNumbersChange,
        )
    }
}

@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun GrupoDePrivacidadePreview() {
    SecaoDeExemplo {
        GrupoDePrivacidade(state = SettingsUiState(loading = false), onMaskNumbersChange = {})
    }
}

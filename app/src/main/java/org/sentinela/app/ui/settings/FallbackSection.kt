package org.sentinela.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.FallbackPolicy

/** Item 14 — o que fazer quando a consulta local falha, com o custo dos dois lados. */
@Composable
internal fun GrupoDePoliticaDeFalha(state: SettingsUiState, onFallback: (FallbackPolicy) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_fallback_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.settings_fallback_allow),
                description = stringResource(R.string.settings_fallback_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.fallbackPolicy == FallbackPolicy.ALLOW,
                onClick = { onFallback(FallbackPolicy.ALLOW) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.settings_fallback_block),
                description = stringResource(R.string.settings_fallback_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.fallbackPolicy == FallbackPolicy.BLOCK,
                onClick = { onFallback(FallbackPolicy.BLOCK) },
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 400)
@Composable
private fun GrupoDePoliticaDeFalhaPreview() {
    SecaoDeExemplo {
        GrupoDePoliticaDeFalha(state = SettingsUiState(loading = false), onFallback = {})
    }
}

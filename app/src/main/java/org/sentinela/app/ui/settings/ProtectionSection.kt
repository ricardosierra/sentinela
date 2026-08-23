package org.sentinela.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.SettingSwitchRow

/**
 * Itens 1, 1b, 7 e 10 — o que a proteção faz e o que ela honestamente não faz.
 *
 * O grupo fica tingido quando a proteção está desligada. A tinta nunca é o único sinal: o
 * interruptor anuncia o próprio estado e a explicação permanente diz o que "desligado" significa.
 *
 * A linha do papel do sistema é a MESMA verdade da home, colocada no lugar onde se conserta. A ação
 * de correção só aparece quando o aparelho oferece o papel: botão que não pode dar em nada é pior
 * que a ausência dele.
 */
@Composable
internal fun GrupoDeProtecao(
    state: SettingsUiState,
    onProtectionChange: (Boolean) -> Unit,
    onFixRole: () -> Unit,
    onHideNativeLogChange: (Boolean) -> Unit,
    onRepeatedCallChange: (Boolean) -> Unit,
) {
    SettingsGroup(
        title = stringResource(R.string.dashboard_monitoring),
        tinted = !state.settings.protectionEnabled,
    ) {
        SettingSwitchRow(
            label = stringResource(R.string.settings_protection_toggle),
            description = stringResource(R.string.settings_protection_toggle_desc),
            checked = state.settings.protectionEnabled,
            onCheckedChange = onProtectionChange,
        )
        if (!state.screeningRoleHeld) {
            InfoBanner(
                text = stringResource(R.string.dashboard_role_missing),
                actionLabel = stringResource(R.string.dashboard_fix_configuration)
                    .takeIf { state.screeningRoleAvailable },
                onAction = onFixRole.takeIf { state.screeningRoleAvailable },
            )
        }
        SettingSwitchRow(
            label = stringResource(R.string.settings_hide_native_log),
            description = stringResource(R.string.settings_hide_native_log_desc),
            checked = state.settings.hideFromNativeCallLog,
            onCheckedChange = onHideNativeLogChange,
        )
        SettingSwitchRow(
            label = stringResource(R.string.settings_repeated_call),
            description = stringResource(R.string.settings_repeated_call_desc),
            checked = state.settings.repeatedCallBypassEnabled,
            onCheckedChange = onRepeatedCallChange,
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun GrupoDeProtecaoPreview() {
    SecaoDeExemplo {
        GrupoDeProtecao(
            state = SettingsUiState(screeningRoleHeld = true, loading = false),
            onProtectionChange = {},
            onFixRole = {},
            onHideNativeLogChange = {},
            onRepeatedCallChange = {},
        )
    }
}

/** O ramo que a tinta e o aviso de papel ausente cobrem — o unico que muda o desenho do grupo. */
@Preview(widthDp = 411)
@Composable
private fun GrupoDeProtecaoDesligadoPreview() {
    SecaoDeExemplo {
        GrupoDeProtecao(
            state = SettingsUiState(
                settings = ScreeningSettings(protectionEnabled = false),
                screeningRoleAvailable = true,
                loading = false,
            ),
            onProtectionChange = {},
            onFixRole = {},
            onHideNativeLogChange = {},
            onRepeatedCallChange = {},
        )
    }
}

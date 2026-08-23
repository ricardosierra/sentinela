package org.sentinela.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.ui.components.SettingSwitchRow

/**
 * Item 8 — a notificação própria e as duas formas de identificação.
 *
 * As sub-opções só existem com o interruptor ligado: oferecer a escolha de COMO identificar algo que
 * não será mostrado é pedir decisão sem efeito.
 */
@Composable
internal fun GrupoDeNotificacao(
    state: SettingsUiState,
    onNotificationChange: (Boolean) -> Unit,
    onNotificationIdentification: (NotificationIdentification) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_show_notification)) {
        SettingSwitchRow(
            label = stringResource(R.string.settings_notification_enable),
            description = stringResource(R.string.settings_notification_enable_desc),
            checked = state.settings.showOwnNotification,
            onCheckedChange = onNotificationChange,
        )
        if (state.settings.showOwnNotification) {
            EscolhaUnica {
                OpcaoDePolitica(
                    title = stringResource(R.string.settings_notification_identification_masked),
                    description = stringResource(R.string.notification_channel_blocked_desc),
                    icon = Icons.Outlined.Info,
                    selected = state.settings.notificationIdentification ==
                        NotificationIdentification.MASKED,
                    onClick = { onNotificationIdentification(NotificationIdentification.MASKED) },
                )
                OpcaoDePolitica(
                    title = stringResource(R.string.settings_notification_identification_anonymous),
                    description = stringResource(R.string.notification_blocked_anonymous),
                    icon = Icons.Outlined.NotificationsOff,
                    selected = state.settings.notificationIdentification ==
                        NotificationIdentification.ANONYMOUS,
                    onClick = { onNotificationIdentification(NotificationIdentification.ANONYMOUS) },
                )
            }
        }
    }
}


@Preview(widthDp = 411, heightDp = 500)
@Composable
private fun GrupoDeNotificacaoPreview() {
    SecaoDeExemplo {
        GrupoDeNotificacao(
            state = SettingsUiState(loading = false),
            onNotificationChange = {},
            onNotificationIdentification = {},
        )
    }
}

/** Com o interruptor desligado as sub-opcoes somem: escolher COMO identificar o que nao aparece. */
@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun GrupoDeNotificacaoDesligadoPreview() {
    SecaoDeExemplo {
        GrupoDeNotificacao(
            state = SettingsUiState(
                settings = ScreeningSettings(showOwnNotification = false),
                loading = false,
            ),
            onNotificationChange = {},
            onNotificationIdentification = {},
        )
    }
}

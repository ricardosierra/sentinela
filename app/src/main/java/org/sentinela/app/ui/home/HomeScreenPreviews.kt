package org.sentinela.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.BottomBarItem
import org.sentinela.app.ui.components.SentinelaBottomBar
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * A barra inferior COMO A DE VERDADE, so que sem navegacao.
 *
 * A barra que o aplicativo desenha e montada em `SentinelaNavHost`, e a pre-visualizacao nao tem
 * como usa-la: ela depende de um controlador de navegacao vivo. O que existe aqui e um dublê, e a
 * regra do dublê e nao mentir sobre o que ele imita — os quatro destinos existem, os quatro
 * navegam, e nenhum deles fica desabilitado.
 */
@Composable
private fun itensDaBarraDeExemplo(): List<BottomBarItem> = listOf(
    BottomBarItem(
        label = stringResource(R.string.nav_home),
        icon = Icons.Filled.Home,
        selected = true,
        onClick = {},
    ),
    BottomBarItem(
        label = stringResource(R.string.nav_whitelist),
        icon = Icons.Outlined.VerifiedUser,
        selected = false,
        onClick = {},
    ),
    BottomBarItem(
        label = stringResource(R.string.nav_history),
        icon = Icons.Outlined.History,
        selected = false,
        onClick = {},
    ),
    BottomBarItem(
        label = stringResource(R.string.nav_settings),
        icon = Icons.Outlined.Settings,
        selected = false,
        onClick = {},
    ),
)

/** Um estado degradado por pre-visualizacao: os oito da secao 8 do contrato de interface. */
private class HomeStatePreviews : PreviewParameterProvider<HomeUiState> {
    private val ultima = LastBlockedUi(
        maskedNumber = "+55 11 9****-1234",
        reasonLabelRes = R.string.history_unknown_number,
        timestampUtcMillis = 0L,
    )
    private val base = HomeUiState(
        protectionEnabled = true,
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        totalBlocked = StatValue.Loaded(42),
        blockedToday = StatValue.Loaded(3),
        lastBlocked = ultima,
    )
    override val values: Sequence<HomeUiState> = sequenceOf(
        base.copy(screeningRoleHeld = false, protectionEnabled = false),
        base.copy(protectionEnabled = false),
        base.copy(contactsPermission = ContactsPermissionState.DENIED_ONCE),
        base.copy(
            historyEnabled = false,
            totalBlocked = StatValue.Unavailable,
            blockedToday = StatValue.Unavailable,
            lastBlocked = null,
        ),
        base.copy(
            totalBlocked = StatValue.Loaded(0),
            blockedToday = StatValue.Loaded(0),
            lastBlocked = null,
        ),
        base.copy(dialerMode = DialerModeState.ROLE_LOST),
        base.copy(totalBlocked = StatValue.Loading, blockedToday = StatValue.Loading),
        base.copy(
            readError = true,
            totalBlocked = StatValue.Unavailable,
            blockedToday = StatValue.Unavailable,
        ),
    )
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeStatePreviews::class) state: HomeUiState,
) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(
            state = state,
            onProtectionChange = {},
            onFixRole = {},
            onGrantContacts = {},
            onOpenAppSettings = {},
            onEnableHistory = {},
            onRetryHistory = {},
            onOpenSettings = {},
            onOpenWhitelist = {},
            onOpenHistory = {},
            onOpenDialerActivation = {},
            bottomBar = { SentinelaBottomBar(items = itensDaBarraDeExemplo()) },
            onAcceptRating = {},
            onDismissRating = {},
            nowUtcMillis = 0L,
        )
    }
}

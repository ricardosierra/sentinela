package org.sentinela.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.telecom.call.DialerModeState

/**
 * Itens 9 e 16 — as duas linhas que levam para fora desta tela.
 *
 * **Item 9.** A linha navega para a tela de ativação do modo discador, que já existe desde a Fase 6
 * e não é reimplementada nem hospedada aqui: ela é DESTINO de navegação, e a assinatura dela não
 * muda. Em `UNAVAILABLE` a linha fica desabilitada com o motivo; em `BLOCKED_BY_CONTACTS` fica
 * HABILITADA de propósito, porque é a tela de destino que explica o pré-requisito da agenda — barrar
 * a entrada esconderia a explicação de que o usuário precisa.
 *
 * Ativar ou reverter o modo não tem confirmação própria: o seletor do sistema é a confirmação, e
 * esse é contrato da Fase 6.
 *
 * **Item 16.** O destino é da Phase 9. Nesta fase o item existe e navega; o envelope de navegação
 * aponta para um destino de espera que COMUNICA o estado — nunca para tela em branco.
 */
@Composable
internal fun GrupoDeDestinos(
    state: SettingsUiState,
    onOpenDialerActivation: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.nav_settings)) {
        SettingsNavRow(
            label = stringResource(R.string.settings_dialer_mode),
            description = stringResource(R.string.settings_dialer_mode_desc),
            onClick = onOpenDialerActivation,
            enabled = state.dialerMode != DialerModeState.UNAVAILABLE,
            unavailableReason = stringResource(R.string.dialer_activation_unavailable)
                .takeIf { state.dialerMode == DialerModeState.UNAVAILABLE },
            valueText = when (state.dialerMode) {
                DialerModeState.ACTIVE -> stringResource(R.string.state_on)
                DialerModeState.ROLE_LOST -> stringResource(R.string.state_off)
                else -> null
            },
        )
        SettingsNavRow(
            label = stringResource(R.string.about_title),
            description = stringResource(R.string.about_data_local),
            onClick = onOpenAbout,
        )
    }
}

@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun GrupoDeDestinosOferecidoPreview() {
    SecaoDeExemplo {
        GrupoDeDestinos(
            state = SettingsUiState(dialerMode = DialerModeState.OFFERED, loading = false),
            onOpenDialerActivation = {},
            onOpenAbout = {},
        )
    }
}

/** O ramo indisponivel: linha desabilitada com o motivo, o unico em que o toque nao leva a lugar. */
@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun GrupoDeDestinosIndisponivelPreview() {
    SecaoDeExemplo {
        GrupoDeDestinos(
            state = SettingsUiState(dialerMode = DialerModeState.UNAVAILABLE, loading = false),
            onOpenDialerActivation = {},
            onOpenAbout = {},
        )
    }
}

/** Modo discador ativo: a linha ganha o valor a direita, e so este ramo e o proximo o mostram. */
@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun GrupoDeDestinosAtivoPreview() {
    SecaoDeExemplo {
        GrupoDeDestinos(
            state = SettingsUiState(dialerMode = DialerModeState.ACTIVE, loading = false),
            onOpenDialerActivation = {},
            onOpenAbout = {},
        )
    }
}

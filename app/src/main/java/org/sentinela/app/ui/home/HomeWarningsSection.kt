package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.theme.SentinelaTheme

/** No maximo dois avisos ao mesmo tempo. O terceiro empurraria o conteudo real fora da tela. */
private const val MAX_BANNERS = 2

private val WarningGap = 12.dp

/**
 * Os avisos da home, ja em texto e em toque.
 *
 * A LISTA nao e decidida aqui: ela chega pronta de [avisosDaHome], que e funcao pura. O que esta
 * composta faz e o que so composta pode fazer — resolver identificador de recurso em texto, ligar a
 * intencao ao toque e aplicar o teto de dois.
 *
 * Do terceiro aviso em diante o excedente vira uma unica linha que leva a tela de Protecao, em vez de
 * uma pilha que empurra as estatisticas e o cartao principal para fora do primeiro quadro.
 */
@Composable
internal fun BlocoDeAvisos(
    avisos: List<AvisoDaHome>,
    onAcao: (AcaoDoAviso) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WarningGap)) {
        avisos.take(MAX_BANNERS).forEach { aviso ->
            InfoBanner(
                text = stringResource(aviso.textRes),
                actionLabel = aviso.actionLabelRes?.let { stringResource(it) },
                onAction = aviso.acao?.let { acao -> { onAcao(acao) } },
            )
        }
        if (avisos.size > MAX_BANNERS) {
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = stringResource(R.string.dashboard_more_warnings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AvisosDeExemplo(state: HomeUiState) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BlocoDeAvisos(avisos = avisosDaHome(state), onAcao = {}, onOpenSettings = {})
        }
    }
}

/** Papel de triagem ausente COM o papel disponivel: o unico ramo em que a correcao e oferecida. */
@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeAvisosPapelAusentePreview() {
    AvisosDeExemplo(
        HomeUiState(
            screeningRoleHeld = false,
            screeningRoleAvailable = true,
            contactsPermission = ContactsPermissionState.GRANTED,
        ),
    )
}

/** Sem o papel no aparelho o aviso continua, e o botao some: botao inerte e pior que nenhum. */
@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeAvisosPapelIndisponivelPreview() {
    AvisosDeExemplo(
        HomeUiState(
            screeningRoleHeld = false,
            screeningRoleAvailable = false,
            contactsPermission = ContactsPermissionState.GRANTED,
        ),
    )
}

/** Cinco motivos ao mesmo tempo: dois avisos e a linha do excedente, nunca cinco banners. */
@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun BlocoDeAvisosNoTetoPreview() {
    AvisosDeExemplo(
        HomeUiState(
            screeningRoleHeld = false,
            screeningRoleAvailable = true,
            contactsPermission = ContactsPermissionState.DENIED_PERMANENTLY,
            historyEnabled = false,
            readError = true,
            dialerMode = DialerModeState.ROLE_LOST,
        ),
    )
}

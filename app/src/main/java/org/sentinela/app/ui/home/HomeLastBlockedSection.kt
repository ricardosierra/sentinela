package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium

private val HeaderToCardGap = 12.dp
private val IconToTextGap = 16.dp
private val EmptyCardPadding = 20.dp
private val EmptyIconSize = 32.dp

/**
 * Cabecalho e cartao da ultima chamada bloqueada.
 *
 * O bloco so e composto com o historico ligado — quem decide isso e [CorpoDaHome], porque a decisao
 * e sobre a PRESENCA do bloco na tela, nao sobre o desenho dele. Aqui dentro so existem os dois
 * desenhos possiveis com o historico ligado: ha uma ultima bloqueada, ou nao ha nenhuma ainda.
 */
@Composable
internal fun BlocoDaUltimaBloqueada(
    state: HomeUiState,
    nowUtcMillis: Long,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ultima = state.lastBlocked
    val tempo = ultima?.let { relativeTimeLabel(it.timestampUtcMillis, nowUtcMillis) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_last_blocked),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (tempo != null) {
                Text(
                    text = tempo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(HeaderToCardGap))
        if (ultima != null && tempo != null) {
            LastBlockedCard(
                maskedNumber = ultima.maskedNumber,
                reasonLabel = stringResource(ultima.reasonLabelRes),
                relativeTime = tempo,
                onClick = onOpenHistory,
            )
        } else {
            CartaoVazio()
        }
    }
}

/** Estado vazio da ultima bloqueada: historico ligado e nada bloqueado ainda. */
@Composable
private fun CartaoVazio() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(EmptyCardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.requiredSize(EmptyIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_empty),
                modifier = Modifier.padding(start = IconToTextGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UltimaBloqueadaDeExemplo(state: HomeUiState) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BlocoDaUltimaBloqueada(state = state, nowUtcMillis = 0L, onOpenHistory = {})
        }
    }
}

@Preview(widthDp = 411, heightDp = 250)
@Composable
private fun BlocoDaUltimaBloqueadaPreview() {
    UltimaBloqueadaDeExemplo(
        HomeUiState(
            lastBlocked = LastBlockedUi(
                maskedNumber = "+55 11 9****-1234",
                reasonLabelRes = R.string.history_unknown_number,
                timestampUtcMillis = 0L,
            ),
        ),
    )
}

/** Historico ligado e nada bloqueado ainda: o vazio e um estado, e diz isso em texto. */
@Preview(widthDp = 411, heightDp = 250)
@Composable
private fun BlocoDaUltimaBloqueadaVazioPreview() {
    UltimaBloqueadaDeExemplo(HomeUiState(lastBlocked = null))
}

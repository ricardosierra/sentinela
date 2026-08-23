package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val StatGap = 16.dp

/**
 * Os dois cartoes de contagem, lado a lado e com o mesmo peso.
 *
 * Nenhum deles decide se ha numero a mostrar: isso e [StatValue], e o tipo e que fecha o zero
 * mentiroso. Aqui so ha o par e o respiro entre eles.
 */
@Composable
internal fun BlocoDeEstatisticas(state: HomeUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(StatGap)) {
        StatCard(
            label = stringResource(R.string.dashboard_total_blocked),
            value = state.totalBlocked,
            icon = Icons.Outlined.Block,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.dashboard_blocked_today),
            value = state.blockedToday,
            icon = Icons.Outlined.Today,
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EstatisticasDeExemplo(state: HomeUiState) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) { BlocoDeEstatisticas(state) }
    }
}

@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeEstatisticasCarregadoPreview() {
    EstatisticasDeExemplo(
        HomeUiState(totalBlocked = StatValue.Loaded(42), blockedToday = StatValue.Loaded(3)),
    )
}

/** Zero carregado e verdade e pode aparecer — este e o unico ramo em que `0` e honesto. */
@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeEstatisticasZeroPreview() {
    EstatisticasDeExemplo(
        HomeUiState(totalBlocked = StatValue.Loaded(0), blockedToday = StatValue.Loaded(0)),
    )
}

/** Primeiro quadro: esqueleto tonal, jamais `0` como reserva. */
@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeEstatisticasCarregandoPreview() {
    EstatisticasDeExemplo(
        HomeUiState(totalBlocked = StatValue.Loading, blockedToday = StatValue.Loading),
    )
}

/** Historico desligado, retencao que nao guarda ou falha de leitura: nao ha numero a mostrar. */
@Preview(widthDp = 411, heightDp = 200)
@Composable
private fun BlocoDeEstatisticasIndisponivelPreview() {
    EstatisticasDeExemplo(
        HomeUiState(totalBlocked = StatValue.Unavailable, blockedToday = StatValue.Unavailable),
    )
}

package org.sentinela.app.ui.call

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallAudioRoute
import org.sentinela.app.ui.theme.SentinelaTheme

private val SheetCornerRadius = 24.dp
private val SheetPadding = 16.dp
private val RouteRowHeight = 56.dp
private val CheckIconSize = 24.dp
private val TitleToListGap = 8.dp

/** Rotulo de cada rota. Nenhuma rota fica sem nome: rota anonima e rota que ninguem escolhe. */
@StringRes
internal fun audioRouteLabelOf(route: CallAudioRoute): Int = when (route) {
    CallAudioRoute.FONE -> R.string.call_audio_route_earpiece
    CallAudioRoute.VIVA_VOZ -> R.string.call_audio_route_speaker
    CallAudioRoute.BLUETOOTH -> R.string.call_audio_route_bluetooth
    CallAudioRoute.FONE_DE_OUVIDO -> R.string.call_audio_route_headset
}

/**
 * Seletor de rota de audio, como painel ancorado ao rodape.
 *
 * Painel e nao janela flutuante: sobrepor uma chamada em curso com janela do sistema exigiria uma
 * permissao que este aplicativo proibe para sempre.
 *
 * A rota ativa e marcada por radio **e** por marca de conferido **e** por descricao de estado — o
 * mesmo principio de nunca comunicar estado so por cor, aplicado a uma lista.
 */
@Composable
fun AudioRouteSheet(
    routes: Set<CallAudioRoute>,
    activeRoute: CallAudioRoute?,
    onRouteSelected: (CallAudioRoute) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) { onDismiss() }
    val emUso = stringResource(R.string.call_audio_route_active_state)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        ) {
            Column(
                modifier = Modifier.padding(SheetPadding),
                verticalArrangement = Arrangement.spacedBy(TitleToListGap),
            ) {
                Text(
                    text = stringResource(R.string.call_audio_route_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                routes.forEach { rota ->
                    AudioRouteRow(
                        route = rota,
                        selected = rota == activeRoute,
                        activeStateDescription = emUso,
                        onSelect = { onRouteSelected(rota) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioRouteRow(
    route: CallAudioRoute,
    selected: Boolean,
    activeStateDescription: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RouteRowHeight)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics {
                this.selected = selected
                if (selected) stateDescription = activeStateDescription
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TitleToListGap),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(audioRouteLabelOf(route)),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(CheckIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(name = "Rotas de audio", showBackground = true)
@Composable
private fun AudioRouteSheetPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            AudioRouteSheet(
                routes = setOf(
                    CallAudioRoute.FONE,
                    CallAudioRoute.VIVA_VOZ,
                    CallAudioRoute.BLUETOOTH,
                ),
                activeRoute = CallAudioRoute.VIVA_VOZ,
                onRouteSelected = {},
                onDismiss = {},
            )
        }
    }
}

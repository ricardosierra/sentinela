package org.sentinela.app.ui.call

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallAudioRoute
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.theme.SentinelaTheme

private val ControlsRowGap = 24.dp

/** Numero de rotas a partir do qual o toque simples no viva-voz abre o seletor de rota. */
private const val MIN_ROUTES_FOR_TAP_SELECTION = 3

/**
 * Retornos de chamada das telas de chamada, agrupados.
 *
 * Agrupados por um motivo pratico: as telas precisam de sete retornos e uma lista de sete
 * parametros de funcao esconde qual e qual no ponto de uso.
 */
data class CallScreenActions(
    val onHangUp: () -> Unit,
    val onAnswer: () -> Unit = {},
    val onReject: () -> Unit = {},
    val onToggleMute: (Boolean) -> Unit = {},
    val onToggleSpeaker: (Boolean) -> Unit = {},
    val onToggleKeypad: () -> Unit = {},
    val onDigitPressStart: (String) -> Unit = {},
    val onDigitPressEnd: (String) -> Unit = {},
    val onRouteSelected: (CallAudioRoute) -> Unit = {},
)

/**
 * Rotulo do estado. O ramo final e visivel e tem texto proprio: estado sem nome na tela e a pior
 * falha desta fase, porque o sistema de telefonia nao detecta interface viva e travada.
 */
@StringRes
internal fun stateLabelOf(state: CallUiState): Int = when (state) {
    CallUiState.Incoming -> R.string.call_incoming_state
    CallUiState.Dialing -> R.string.call_dialing_state
    CallUiState.Ringing -> R.string.call_ringing_state
    CallUiState.Active -> R.string.call_active_state
    CallUiState.Ended -> R.string.call_ended_state
    CallUiState.Failed -> R.string.call_failed_state
    is CallUiState.Unsupported -> R.string.call_unsupported_state
}

/** O estado ja e final? Nele o cronometro congela e os controles saem de cena. */
internal fun CallUiState.isTerminal(): Boolean =
    this == CallUiState.Ended || this == CallUiState.Failed

/**
 * Chamada ativa: identidade, cronometro, tres controles e encerrar.
 *
 * O ramo do estado nao suportado tem tela propria com texto informativo **e o encerrar
 * habilitado**. Esse ramo e o guarda-corpo da fase: com ele, um estado que esta versao nao desenha
 * produz uma tela sem graca; sem ele, produziria uma tela em branco durante uma ligacao real.
 */
@Composable
internal fun MuteControl(
    snapshot: CallSnapshot,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CallControlButton(
        on = snapshot.muted,
        iconOff = Icons.Filled.Mic,
        iconOn = Icons.Filled.MicOff,
        label = stringResource(R.string.call_control_mute),
        contentDescription = stringResource(
            if (snapshot.muted) {
                R.string.call_control_mute_on_description
            } else {
                R.string.call_control_mute_off_description
            },
        ),
        onToggle = onToggle,
        modifier = modifier,
    )
}

@Composable
internal fun KeypadControl(
    snapshot: CallSnapshot,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CallControlButton(
        on = snapshot.keypadOpen,
        iconOff = Icons.Filled.Dialpad,
        iconOn = Icons.Filled.Dialpad,
        label = stringResource(R.string.call_control_keypad),
        contentDescription = stringResource(
            if (snapshot.keypadOpen) {
                R.string.call_control_keypad_open_description
            } else {
                R.string.call_control_keypad_closed_description
            },
        ),
        onToggle = { onToggle() },
        modifier = modifier,
    )
}

/**
 * Viva-voz. Com mais de uma rota disponivel o mesmo controle abre o seletor de rota.
 *
 * Rota indisponivel deixa o controle desabilitado e anunciado como indisponivel — nunca um
 * dialogo, que sobre uma chamada em curso e sempre a pior escolha.
 */
@Composable
internal fun SpeakerControl(
    snapshot: CallSnapshot,
    actions: CallScreenActions,
    modifier: Modifier = Modifier,
    routes: Set<CallAudioRoute> = emptySet(),
    activeRoute: CallAudioRoute? = null,
) {
    var seletorAberto by remember { mutableStateOf(false) }
    val muitasRotas = routes.size >= MIN_ROUTES_FOR_TAP_SELECTION
    CallControlButton(
        on = snapshot.speakerOn,
        iconOff = Icons.AutoMirrored.Filled.VolumeUp,
        iconOn = Icons.AutoMirrored.Filled.VolumeUp,
        label = stringResource(R.string.call_control_speaker),
        contentDescription = stringResource(
            if (snapshot.speakerOn) {
                R.string.call_control_speaker_on_description
            } else {
                R.string.call_control_speaker_off_description
            },
        ),
        onToggle = { ligado ->
            if (muitasRotas) seletorAberto = true else actions.onToggleSpeaker(ligado)
        },
        modifier = modifier,
        enabled = snapshot.speakerAvailable,
    )
    if (seletorAberto) {
        AudioRouteSheet(
            routes = routes,
            activeRoute = activeRoute,
            onRouteSelected = {
                actions.onRouteSelected(it)
                seletorAberto = false
            },
            onDismiss = { seletorAberto = false },
        )
    }
}


/**
 * Fileira dos tres controles secundarios da chamada.
 *
 * Fica em arquivo proprio porque e reutilizada pela chamada de saida e pela chamada ativa: mudo e
 * viva-voz valem antes de o outro lado atender, e duplicar a fileira faria as duas telas divergirem
 * exatamente no lugar onde a divergencia nao aparece em revisao.
 */
@Composable
internal fun CallControlsRow(
    snapshot: CallSnapshot,
    actions: CallScreenActions,
    modifier: Modifier = Modifier,
    routes: Set<CallAudioRoute> = emptySet(),
    activeRoute: CallAudioRoute? = null,
    includeKeypad: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ControlsRowGap, Alignment.CenterHorizontally),
    ) {
        MuteControl(snapshot = snapshot, onToggle = actions.onToggleMute)
        if (includeKeypad) {
            KeypadControl(snapshot = snapshot, onToggle = actions.onToggleKeypad)
        }
        SpeakerControl(
            snapshot = snapshot,
            actions = actions,
            routes = routes,
            activeRoute = activeRoute,
        )
    }
}

@Preview(name = "Fileira de controles", showBackground = true)
@Composable
private fun CallControlsRowPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            CallControlsRow(
                snapshot = CallSnapshot(state = CallUiState.Active, muted = true),
                actions = CallScreenActions(onHangUp = {}),
            )
        }
    }
}

package org.sentinela.app.ui.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.SentinelaApp
import org.sentinela.app.telecom.call.CallSessionCoordinator
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberLg
import org.sentinela.app.ui.theme.timer

/**
 * Hospedeira da tela de chamada.
 *
 * Ela apenas **observa** o armazém da sessão e comanda pelo coordenador puro. Não conhece nenhum
 * tipo da telefonia: o objeto de chamada da plataforma é um manipulador de comunicação entre
 * processos e nunca atravessa esta fronteira.
 *
 * Não existe estado a restaurar aqui, e isso é medido, não suposto: morrendo o processo no meio de
 * uma ligação, o sistema de telefonia religa a chamada no discador do aparelho sem derrubá-la.
 * Persistir sessão de chamada seria trabalho inútil sobre um estado que já não é nosso.
 *
 * O acabamento visual completo desta tela é do plano 06-04. O que existe aqui já apresenta
 * identidade, estado e ações reais de propósito: uma chamada nunca pode abrir uma tela vazia,
 * porque tela vazia é justamente o modo de falha que ninguém detecta.
 */
class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = (application as SentinelaApp).container.callSessionStore
        setContent {
            SentinelaTheme {
                val snapshot by store.state.collectAsState()
                CallScreen(
                    snapshot = snapshot,
                    session = store.session,
                    onFinish = ::finish,
                )
            }
        }
    }
}

@Composable
private fun CallScreen(
    snapshot: CallSnapshot,
    session: CallSessionCoordinator?,
    onFinish: () -> Unit,
) {
    // Sem esta confirmação o prazo de apresentação vence e a sessão falha alto por desenho: é
    // assim que interface congelada deixa de ser silêncio e passa a ser defeito visível.
    LaunchedEffect(snapshot.identity) { session?.confirmPresented() }

    // Chamada em curso engole o gesto de voltar: sair da tela por acidente com o telefone no
    // ouvido deixaria o usuário sem controle nenhum sobre a ligação.
    BackHandler(enabled = snapshot.state != CallUiState.Ended) { }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            CallHeader(snapshot)
            CallActions(snapshot = snapshot, session = session, onFinish = onFinish)
        }
    }
}

@Composable
private fun CallHeader(snapshot: CallSnapshot) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(stateLabelOf(snapshot.state)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = snapshot.identity.displayName
                ?: snapshot.identity.fullNumber
                ?: stringResource(R.string.call_origin_private),
            style = MaterialTheme.typography.numberLg,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        CallOriginChip(origin = chipOriginOf(snapshot.identity.origin))
        val startedAt = snapshot.startedAtMillis
        if (startedAt != null) {
            Spacer(Modifier.height(12.dp))
            CallTimer(startedAtMillis = startedAt)
        }
        if (snapshot.sentDigits.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = snapshot.sentDigits,
                style = MaterialTheme.typography.timer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallActions(
    snapshot: CallSnapshot,
    session: CallSessionCoordinator?,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (snapshot.state == CallUiState.Active || snapshot.state is CallUiState.Unsupported) {
            OngoingControls(snapshot = snapshot, session = session)
            Spacer(Modifier.height(24.dp))
        }
        if (snapshot.state == CallUiState.Incoming) {
            IncomingActions(session = session)
        } else {
            CallActionButton(
                icon = Icons.Filled.CallEnd,
                label = stringResource(R.string.call_action_hangup),
                contentDescription = stringResource(R.string.call_action_hangup_description),
                colors = callRejectColors(),
                onClick = { session?.hangUp() ?: onFinish() },
                diameter = CallActionDiameterHangup,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IncomingActions(session: CallSessionCoordinator?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CallActionButton(
            icon = Icons.Filled.CallEnd,
            label = stringResource(R.string.call_action_reject),
            contentDescription = stringResource(R.string.call_action_reject_description),
            colors = callRejectColors(),
            onClick = { session?.reject() },
        )
        CallActionButton(
            icon = Icons.Filled.Call,
            label = stringResource(R.string.call_action_answer),
            contentDescription = stringResource(R.string.call_action_answer_description),
            colors = callAcceptColors(),
            onClick = { session?.answer() },
        )
    }
}

@Composable
private fun OngoingControls(snapshot: CallSnapshot, session: CallSessionCoordinator?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
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
            onToggle = { session?.setMuted(it) },
        )
        CallControlButton(
            on = snapshot.speakerOn,
            iconOff = Icons.Filled.VolumeUp,
            iconOn = Icons.Filled.VolumeUp,
            label = stringResource(R.string.call_control_speaker),
            contentDescription = stringResource(
                if (snapshot.speakerOn) {
                    R.string.call_control_speaker_on_description
                } else {
                    R.string.call_control_speaker_off_description
                },
            ),
            onToggle = { session?.setSpeakerOn(it) },
            enabled = snapshot.speakerAvailable,
        )
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
            onToggle = { session?.toggleKeypad() },
        )
    }
}

/** Rótulo do estado. O ramo final é visível: estado sem nome na tela é a pior falha da fase. */
private fun stateLabelOf(state: CallUiState): Int = when (state) {
    CallUiState.Incoming -> R.string.call_incoming_state
    CallUiState.Dialing -> R.string.call_dialing_state
    CallUiState.Ringing -> R.string.call_ringing_state
    CallUiState.Active -> R.string.call_active_state
    CallUiState.Ended -> R.string.call_ended_state
    CallUiState.Failed -> R.string.call_failed_state
    is CallUiState.Unsupported -> R.string.call_unsupported_state
}

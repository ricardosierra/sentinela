package org.sentinela.app.ui.call

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallAudioRoute
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.components.SentinelaWatermark
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.timer
import org.sentinela.app.telecom.call.CallOrigin as DomainCallOrigin

private val ScreenHorizontalMargin = 16.dp
private val TopSlack = 48.dp
private val WatermarkToIdentityGap = 32.dp
private val IdentityToTimerGap = 8.dp
private val ControlsToHangUpGap = 32.dp
private val BottomSlack = 64.dp
private val UnsupportedBodyPadding = 24.dp

/**
 * Tempo que a tela permanece visivel depois do estado final.
 *
 * Fechar no mesmo instante em que a chamada cai esconde do usuario o que aconteceu; esperar demais
 * prende o aparelho numa tela morta. Literal nomeado de proposito: numero solto no meio do layout
 * seria impossivel de encontrar quando este valor precisar mudar.
 */
const val CALL_ENDED_DISMISS_MILLIS: Long = 1_200L

/** Duracao do desaparecimento dos controles quando a chamada termina. */
private const val CONTROLS_FADE_OUT_MILLIS = 200

/** Entrada e saida do painel de tons: entra mais devagar do que sai, como o contrato pede. */
private const val KEYPAD_ENTER_MILLIS = 250
private const val KEYPAD_EXIT_MILLIS = 200

/*
 * Nao existe fracao fixa de altura para a faixa superior quando o teclado de tons esta aberto, e
 * isso foi MEDIDO, nao escolhido: reservar 30% da tela para ela colapsava o botao de encerrar para
 * altura zero numa tela de 470 unidades — exatamente a armadilha que esta fase existe para evitar,
 * e o caso de teste de alvo de toque pegou. Agora a faixa superior toma o tamanho do proprio
 * conteudo e o painel fica com o resto, entao o cronometro e o encerrar continuam visiveis e
 * clicaveis em qualquer altura de tela.
 */

/**
 * Chamada ativa: identidade, cronometro, tres controles e encerrar.
 *
 * O ramo do estado nao suportado tem tela propria com texto informativo **e o encerrar
 * habilitado**. Esse ramo e o guarda-corpo da fase: com ele, um estado que esta versao nao desenha
 * produz uma tela sem graca; sem ele, produziria uma tela em branco durante uma ligacao real — e a
 * pesquisa mediu que o sistema de telefonia nao detecta interface viva e travada.
 */
@Composable
fun ActiveCallScreen(
    snapshot: CallSnapshot,
    actions: CallScreenActions,
    modifier: Modifier = Modifier,
    photo: ImageBitmap? = null,
    routes: Set<CallAudioRoute> = emptySet(),
    activeRoute: CallAudioRoute? = null,
    now: () -> Long = System::currentTimeMillis,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .semantics { isTraversalGroup = true },
        ) {
            ActiveCallContent(
                snapshot = snapshot,
                actions = actions,
                photo = photo,
                routes = routes,
                activeRoute = activeRoute,
                now = now,
                // Com o painel aberto a faixa superior encolhe ao tamanho do conteudo; sem ele,
                // ocupa a tela inteira e empurra as acoes para o rodape.
                modifier = if (snapshot.keypadOpen) Modifier else Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = snapshot.keypadOpen,
                modifier = Modifier.weight(1f, fill = false),
                enter = slideInVertically(
                    animationSpec = tween(KEYPAD_ENTER_MILLIS, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ) + fadeIn(tween(KEYPAD_ENTER_MILLIS)),
                exit = slideOutVertically(
                    animationSpec = tween(KEYPAD_EXIT_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetY = { it },
                ) + fadeOut(tween(KEYPAD_EXIT_MILLIS)),
            ) {
                DtmfKeypadSheet(
                    sentDigits = snapshot.sentDigits,
                    onKeyPressStart = actions.onDigitPressStart,
                    onKeyPressEnd = actions.onDigitPressEnd,
                    onClose = actions.onToggleKeypad,
                )
            }
        }
    }
}

@Composable
private fun ActiveCallContent(
    snapshot: CallSnapshot,
    actions: CallScreenActions,
    photo: ImageBitmap?,
    routes: Set<CallAudioRoute>,
    activeRoute: CallAudioRoute?,
    now: () -> Long,
    modifier: Modifier = Modifier,
) {
    val terminal = snapshot.state.isTerminal()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(TopSlack))
        SentinelaWatermark()
        Spacer(Modifier.height(WatermarkToIdentityGap))
        CallStateLabel(labelRes = stateLabelOf(snapshot.state))
        if (!snapshot.keypadOpen) {
            Spacer(Modifier.height(IdentityToTimerGap))
            CallerIdentity(identity = snapshot.identity, photo = photo)
        }
        Spacer(Modifier.height(IdentityToTimerGap))
        CallDurationLine(snapshot = snapshot, frozen = terminal, now = now)
        if (snapshot.state is CallUiState.Unsupported) {
            Text(
                text = stringResource(R.string.call_unsupported_body),
                modifier = Modifier.padding(UnsupportedBodyPadding),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        // O espacador de peso existe so quando a faixa toma a tela inteira: em modo de conteudo
        // proprio ele valeria zero e so confundiria a leitura do layout.
        if (!snapshot.keypadOpen) Spacer(Modifier.weight(1f))
        // Os controles secundarios saem de cena com fade quando a chamada termina; o encerrar
        // continua no lugar, inclusive no estado nao suportado, onde ele e a unica saida.
        AnimatedVisibility(
            visible = !terminal && !snapshot.keypadOpen,
            exit = fadeOut(tween(CONTROLS_FADE_OUT_MILLIS)),
            enter = fadeIn(tween(CONTROLS_FADE_OUT_MILLIS)),
        ) {
            CallControlsRow(
                snapshot = snapshot,
                actions = actions,
                routes = routes,
                activeRoute = activeRoute,
            )
        }
        Spacer(Modifier.height(ControlsToHangUpGap))
        if (snapshot.hangUpEnabled) {
            HangUpButton(onHangUp = actions.onHangUp)
        }
        if (!snapshot.keypadOpen) Spacer(Modifier.height(BottomSlack))
    }
}

/**
 * Cronometro da chamada, congelado nos estados finais.
 *
 * Congelar em vez de esconder e deliberado: o usuario acabou de desligar e a duracao e a ultima
 * informacao util da tela.
 */
@Composable
private fun CallDurationLine(
    snapshot: CallSnapshot,
    frozen: Boolean,
    now: () -> Long,
) {
    val inicio = snapshot.startedAtMillis ?: return
    if (frozen) {
        val congelado = remember(inicio) { formatCallDuration((now() - inicio) / MILLIS_IN_SECOND) }
        Text(
            text = congelado,
            style = MaterialTheme.typography.timer,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        CallTimer(startedAtMillis = inicio, clock = now)
    }
}

private const val MILLIS_IN_SECOND = 1_000L


private val identidadeDeContato = CallIdentity(
    displayName = "Ana Paula Souza",
    fullNumber = "+5511912345678",
    origin = DomainCallOrigin.CONTATO,
)

private val retratoAtivo = CallSnapshot(
    state = CallUiState.Active,
    identity = identidadeDeContato,
    startedAtMillis = 0L,
)

private val relogioFixo: () -> Long = { 125_000L }

@Preview(name = "Ativa", showBackground = true)
@Composable
private fun ActiveCallPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo,
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - em mudo", showBackground = true)
@Composable
private fun ActiveCallMutedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo.copy(muted = true),
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - viva-voz", showBackground = true)
@Composable
private fun ActiveCallSpeakerPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo.copy(speakerOn = true),
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - teclado aberto", showBackground = true)
@Composable
private fun ActiveCallKeypadPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo.copy(keypadOpen = true, sentDigits = "1234"),
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - encerrada", showBackground = true)
@Composable
private fun ActiveCallEndedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo.copy(state = CallUiState.Ended),
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - estado nao suportado", showBackground = true)
@Composable
private fun ActiveCallUnsupportedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo.copy(state = CallUiState.Unsupported(rawState = 99)),
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

@Preview(name = "Ativa - fonte 200%", fontScale = 2f, showBackground = true)
@Composable
private fun ActiveCallLargeFontPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        ActiveCallScreen(
            snapshot = retratoAtivo,
            actions = CallScreenActions(onHangUp = {}),
            now = relogioFixo,
        )
    }
}

package org.sentinela.app.ui.call

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.components.SentinelaWatermark
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.telecom.call.CallOrigin as DomainCallOrigin

private val ScreenHorizontalMargin = 16.dp
private val TopSlack = 48.dp
private val WatermarkToStateGap = 48.dp
private val StateToProgressGap = 16.dp
private val ProgressToIdentityGap = 24.dp
private val BottomSlack = 64.dp
private val ProgressDotSize = 6.dp
private val ProgressDotGap = 8.dp
private val ControlsToHangUpGap = 32.dp

/** Ciclo completo do fade sequencial dos tres pontos. */
private const val PROGRESS_CYCLE_MILLIS = 1_200
private const val PROGRESS_DOTS = 3
private const val DOT_DIM_ALPHA = 0.25f
private const val DOT_BRIGHT_ALPHA = 1f

/**
 * A reducao de animacoes do sistema esta ligada?
 *
 * Lido da configuracao global de escala de duracao de animacao, que e o interruptor real do
 * aparelho. Zero significa "sem movimento", e nesse caso o fade sequencial e as escalas de toque
 * viram troca instantanea. Nenhuma informacao desta tela depende de animacao: os pontos so dizem
 * "ainda estamos tentando", e o rotulo de estado ja diz isso em texto.
 */
@Composable
internal fun rememberMotionReduced(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Tres pontos com fade sequencial, decorativos e nao focaveis.
 *
 * Deliberadamente nao e um indicador circular girando: em tela cheia, a 1 metro de distancia, o
 * giro compete com a legibilidade da identidade, que e a informacao que importa.
 */
@Composable
private fun DialingProgressDots(modifier: Modifier = Modifier) {
    val reduzido = rememberMotionReduced()
    val cor = MaterialTheme.colorScheme.primary
    val transicao = rememberInfiniteTransition(label = "dialingDots")
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(ProgressDotGap),
    ) {
        repeat(PROGRESS_DOTS) { indice ->
            val fatia = PROGRESS_CYCLE_MILLIS / PROGRESS_DOTS
            val alpha by transicao.animateFloat(
                initialValue = DOT_DIM_ALPHA,
                targetValue = DOT_DIM_ALPHA,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = PROGRESS_CYCLE_MILLIS
                        DOT_DIM_ALPHA at 0 using LinearEasing
                        DOT_BRIGHT_ALPHA at fatia * indice + fatia / 2 using LinearEasing
                        DOT_DIM_ALPHA at PROGRESS_CYCLE_MILLIS using LinearEasing
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dialingDot$indice",
            )
            Box(
                modifier = Modifier
                    .size(ProgressDotSize)
                    .alpha(if (reduzido) DOT_BRIGHT_ALPHA else alpha)
                    .background(cor, CircleShape),
            )
        }
    }
}

/**
 * Chamada de saida: mesmo esqueleto da recebida, com uma unica acao de encerrar.
 *
 * Mudo e viva-voz aparecem **habilitados** antes de o outro lado atender, porque ligar o viva-voz
 * enquanto o telefone chama e comportamento esperado de qualquer discador.
 */
@Composable
fun OutgoingCallScreen(
    snapshot: CallSnapshot,
    actions: CallScreenActions,
    modifier: Modifier = Modifier,
    photo: ImageBitmap? = null,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = ScreenHorizontalMargin)
                .semantics { isTraversalGroup = true },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(TopSlack))
            SentinelaWatermark()
            Spacer(Modifier.height(WatermarkToStateGap))
            CallStateLabel(labelRes = stateLabelOf(snapshot.state))
            Spacer(Modifier.height(StateToProgressGap))
            DialingProgressDots()
            Spacer(Modifier.height(ProgressToIdentityGap))
            CallerIdentity(identity = snapshot.identity, photo = photo)
            Spacer(Modifier.weight(1f))
            // Sem a tecla de tons: nao existe tom a enviar antes de a chamada ser atendida.
            CallControlsRow(snapshot = snapshot, actions = actions, includeKeypad = false)
            Spacer(Modifier.height(ControlsToHangUpGap))
            HangUpButton(onHangUp = actions.onHangUp)
            Spacer(Modifier.height(BottomSlack))
        }
    }
}

@Preview(name = "Saida - chamando", showBackground = true)
@Composable
private fun OutgoingDialingPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        OutgoingCallScreen(
            snapshot = CallSnapshot(
                state = CallUiState.Dialing,
                identity = CallIdentity(
                    fullNumber = "+5511912345678",
                    origin = DomainCallOrigin.DESCONHECIDO,
                ),
            ),
            actions = CallScreenActions(onHangUp = {}),
        )
    }
}

@Preview(name = "Saida - tocando do outro lado", showBackground = true)
@Composable
private fun OutgoingRingingPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        OutgoingCallScreen(
            snapshot = CallSnapshot(
                state = CallUiState.Ringing,
                identity = CallIdentity(
                    displayName = "Ana Paula Souza",
                    fullNumber = "+5511912345678",
                    origin = DomainCallOrigin.CONTATO,
                ),
                speakerOn = true,
            ),
            actions = CallScreenActions(onHangUp = {}),
        )
    }
}

@Preview(name = "Saida - falha da rede", showBackground = true)
@Composable
private fun OutgoingFailedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        OutgoingCallScreen(
            snapshot = CallSnapshot(
                state = CallUiState.Failed,
                identity = CallIdentity(
                    fullNumber = "+5511912345678",
                    origin = DomainCallOrigin.DESCONHECIDO,
                ),
            ),
            actions = CallScreenActions(onHangUp = {}),
        )
    }
}

@Preview(name = "Saida - fonte 200%", fontScale = 2f, showBackground = true)
@Composable
private fun OutgoingLargeFontPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        OutgoingCallScreen(
            snapshot = CallSnapshot(
                state = CallUiState.Dialing,
                identity = CallIdentity(
                    fullNumber = "+5511912345678",
                    origin = DomainCallOrigin.DESCONHECIDO,
                ),
            ),
            actions = CallScreenActions(onHangUp = {}),
        )
    }
}

/** Encerrar de 64dp, centralizado, com a cor destrutiva fixa do contrato. */
@Composable
internal fun HangUpButton(onHangUp: () -> Unit, modifier: Modifier = Modifier) {
    CallActionButton(
        icon = Icons.Filled.CallEnd,
        label = stringResource(R.string.call_action_hangup),
        contentDescription = stringResource(R.string.call_action_hangup_description),
        colors = callRejectColors(),
        onClick = onHangUp,
        modifier = modifier,
        diameter = CallActionDiameterHangup,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

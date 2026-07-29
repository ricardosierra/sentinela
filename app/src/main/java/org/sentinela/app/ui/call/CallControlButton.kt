package org.sentinela.app.ui.call

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

/** Controle secundario da chamada: acima do minimo de 48dp exigido. */
val CallControlDiameter = 56.dp

private val IconSize = 24.dp
private val CircleToLabelGap = 8.dp
private val ActiveRingWidth = 2.dp
private const val ACTIVE_RING_ALPHA = 0.4f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val COLOR_ANIMATION_MILLIS = 150

/** Anel externo que reforca o estado ligado, alem da cor e do icone. */
private fun activeRing(visible: Boolean, accent: Color): Modifier = if (visible) {
    Modifier.border(
        width = ActiveRingWidth,
        color = accent.copy(alpha = ACTIVE_RING_ALPHA),
        shape = CircleShape,
    )
} else {
    Modifier
}

/** Container do controle: baixo quando indisponivel, acento quando ligado. */
@Composable
private fun controlContainerColor(on: Boolean, enabled: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        !enabled -> scheme.surfaceContainerLow
        on -> scheme.primary
        else -> scheme.surfaceContainerHighest
    }
}

/** Icone do controle: reduzido quando indisponivel, sobre acento quando ligado. */
@Composable
private fun controlContentColor(on: Boolean, enabled: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = DISABLED_CONTENT_ALPHA)
        on -> scheme.onPrimary
        else -> scheme.onSurfaceVariant
    }
}

/**
 * Controle de dois estados da chamada ativa (mudo, teclado, viva-voz).
 *
 * O estado ligado nunca e comunicado so por cor: o componente usa
 * `Modifier.toggleable` com [Role.Switch], entao o leitor de tela anuncia
 * ligado/desligado por conta propria, e quem chama ainda fornece um icone
 * distinto por estado quando existe variante (mudo tem, viva-voz nao).
 *
 * Desabilitado significa rota de audio indisponivel; nesse caso o proprio
 * controle anuncia que esta indisponivel, em vez de simplesmente nao responder.
 */
@Composable
fun CallControlButton(
    on: Boolean,
    iconOff: ImageVector,
    iconOn: ImageVector,
    label: String,
    contentDescription: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val unavailable = stringResource(R.string.call_control_unavailable_state)
    val container by animateColorAsState(
        targetValue = controlContainerColor(on, enabled),
        animationSpec = tween(COLOR_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "callControlContainer",
    )
    val content by animateColorAsState(
        targetValue = controlContentColor(on, enabled),
        animationSpec = tween(COLOR_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "callControlContent",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CircleToLabelGap),
    ) {
        Box(
            modifier = Modifier
                .size(CallControlDiameter)
                .background(container, CircleShape)
                .then(activeRing(on && enabled, scheme.primary))
                .toggleable(
                    value = on,
                    enabled = enabled,
                    role = Role.Switch,
                ) { novoEstado ->
                    haptics.performHapticFeedback(
                        if (novoEstado) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onToggle(novoEstado)
                }
                .semantics {
                    this.contentDescription = contentDescription
                    if (!enabled) stateDescription = unavailable
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (on) iconOn else iconOff,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = content,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (on && enabled) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun CallControlButtonPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(CallControlDiameter)) {
                CallControlButton(
                    on = false,
                    iconOff = Icons.Filled.Mic,
                    iconOn = Icons.Filled.MicOff,
                    label = stringResource(R.string.call_control_mute),
                    contentDescription =
                        stringResource(R.string.call_control_mute_off_description),
                    onToggle = {},
                )
                CallControlButton(
                    on = true,
                    iconOff = Icons.Filled.Dialpad,
                    iconOn = Icons.Filled.Dialpad,
                    label = stringResource(R.string.call_control_keypad),
                    contentDescription =
                        stringResource(R.string.call_control_keypad_open_description),
                    onToggle = {},
                )
                CallControlButton(
                    on = false,
                    enabled = false,
                    iconOff = Icons.Filled.VolumeUp,
                    iconOn = Icons.Filled.VolumeUp,
                    label = stringResource(R.string.call_control_speaker),
                    contentDescription =
                        stringResource(R.string.call_control_speaker_off_description),
                    onToggle = {},
                )
            }
        }
    }
}

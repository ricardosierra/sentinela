package org.sentinela.app.ui.dialer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberXl

/** Diametro da tecla, na discagem e no teclado DTMF. */
val DialpadKeyDiameter = 72.dp

private const val PRESSED_SCALE = 0.92f
private const val PRESS_ANIMATION_MILLIS = 100

/**
 * Uma tecla do teclado.
 *
 * `onPressStart` e `onPressEnd` sao separados de proposito: o tom DTMF real e
 * responsabilidade da camada de Telecom e precisa comecar no toque e parar na
 * liberacao. A UI so avisa as duas bordas do gesto.
 *
 * `onLongPress` existe para a tecla `0`, que insere `+`.
 */
@Composable
fun DialpadKey(
    digit: String,
    letters: String?,
    contentDescription: String,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(PRESS_ANIMATION_MILLIS),
        label = "dialpadKeyPressScale",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .size(DialpadKeyDiameter)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .pointerInput(digit) {
                detectTapGestures(
                    onPress = { offset ->
                        pressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onPressStart()
                        tryAwaitRelease()
                        pressed = false
                        onPressEnd()
                    },
                    onLongPress = onLongPress?.let { acao ->
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            acao()
                        }
                    },
                )
            }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                style = MaterialTheme.typography.numberXl,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (letters != null) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DialpadKeyPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            DialpadKey(
                digit = "0",
                letters = "+",
                contentDescription = stringResource(R.string.dialpad_key_0_description),
                onPressStart = {},
                onPressEnd = {},
                onLongPress = {},
            )
        }
    }
}

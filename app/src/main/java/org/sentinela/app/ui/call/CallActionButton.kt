package org.sentinela.app.ui.call

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.CallReject
import org.sentinela.app.ui.theme.OnCallAccept
import org.sentinela.app.ui.theme.OnCallReject
import org.sentinela.app.ui.theme.SentinelaTheme
import androidx.compose.foundation.clickable

/** Diametro de atender e recusar: acima do minimo do design system — errar aqui e caro. */
val CallActionDiameterPrimary = 72.dp

/** Diametro de encerrar na chamada em curso. */
val CallActionDiameterHangup = 64.dp

private val IconSize = 32.dp
private val CircleToLabelGap = 8.dp
private const val PRESSED_SCALE = 0.95f
private const val PRESS_ANIMATION_MILLIS = 120

/**
 * Par de cores de uma acao funcional da chamada. Sempre construido a partir dos
 * literais nomeados do arquivo de cores — nunca de um papel do tema.
 */
data class CallActionColors(val container: Color, val content: Color)

/**
 * Cores de atender.
 *
 * Este e, junto de [callRejectColors], o UNICO caminho pelo qual as telas dos
 * planos 06-04 e 06-05 pintam as tres acoes funcionais. As cores vem de literais
 * do arquivo de tema, e nao de papeis montados em tempo de execucao: o tema do
 * app substitui o conjunto de papeis inteiro por um derivado do papel de parede
 * nos aparelhos novos, e naquele caminho o tom de recusar deixaria de ser nosso.
 * Um papel de parede poderia aproximar os dois botoes e produzir uma recusa
 * acidental de uma chamada real.
 */
fun callAcceptColors(): CallActionColors = CallActionColors(CallAccept, OnCallAccept)

/** Cores de recusar e de encerrar. Mesma justificativa de [callAcceptColors]. */
fun callRejectColors(): CallActionColors = CallActionColors(CallReject, OnCallReject)

/**
 * Botao circular de acao da chamada.
 *
 * A cor chega por parametro por desenho: este componente nao consulta os papeis
 * de cor do tema, nem para atender nem para recusar. O estado nunca e comunicado
 * so por cor — cada botao tem icone distinto e rotulo textual sob o circulo.
 *
 * O toque dispara na liberacao dentro do alvo (comportamento padrao de clique do
 * Compose), o que evita atender por encostar no aparelho ao tira-lo do bolso.
 */
@Composable
fun CallActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    colors: CallActionColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = CallActionDiameterPrimary,
    labelColor: Color = LocalContentColor.current,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(durationMillis = PRESS_ANIMATION_MILLIS),
        label = "callActionPressScale",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CircleToLabelGap),
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                // requiredSize e nao size: com a tela curta ou a fonte em 200% o pai comprimia o
                // circulo — medido em 23dp num alvo de 72dp — e o contrato diz que os botoes NAO
                // reduzem. size() negocia com o pai; requiredSize() nao negocia.
                .requiredSize(diameter)
                .background(colors.container, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                }
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = colors.content,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
        )
    }
}

@Preview
@Composable
private fun CallActionButtonPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(CallActionDiameterHangup),
            ) {
                CallActionButton(
                    icon = Icons.Filled.CallEnd,
                    label = stringResource(R.string.call_action_reject),
                    contentDescription = stringResource(R.string.call_action_reject_description),
                    colors = callRejectColors(),
                    onClick = {},
                )
                CallActionButton(
                    icon = Icons.Filled.Call,
                    label = stringResource(R.string.call_action_answer),
                    contentDescription = stringResource(R.string.call_action_answer_description),
                    colors = callAcceptColors(),
                    onClick = {},
                )
            }
        }
    }
}

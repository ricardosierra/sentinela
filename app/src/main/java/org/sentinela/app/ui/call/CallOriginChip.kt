package org.sentinela.app.ui.call

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapePill

private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp
private val ChipIconSize = 16.dp
private val ChipIconToLabelGap = 4.dp
private const val WHITELIST_CONTAINER_ALPHA = 0.15f

/**
 * Origem de uma chamada, do ponto de vista da decisao do Sentinela.
 *
 * Existe so para a exibicao; a decisao de verdade e do motor.
 */
enum class CallOrigin { CONTACT, WHITELIST, UNKNOWN, PRIVATE }

private data class OriginStyle(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
)

/** Tabela de chips do contrato de design, em um unico ponto. */
@Composable
private fun originStyle(origin: CallOrigin): OriginStyle {
    val scheme = MaterialTheme.colorScheme
    return when (origin) {
        CallOrigin.CONTACT -> OriginStyle(
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            icon = Icons.Outlined.Person,
            labelRes = R.string.call_origin_contact,
        )
        CallOrigin.WHITELIST -> OriginStyle(
            container = scheme.primary.copy(alpha = WHITELIST_CONTAINER_ALPHA),
            content = scheme.primary,
            icon = Icons.Outlined.VerifiedUser,
            labelRes = R.string.call_origin_whitelist,
        )
        CallOrigin.UNKNOWN -> OriginStyle(
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
            icon = Icons.Outlined.HelpOutline,
            labelRes = R.string.call_origin_unknown,
        )
        CallOrigin.PRIVATE -> OriginStyle(
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
            icon = Icons.Outlined.VisibilityOff,
            labelRes = R.string.call_origin_private,
        )
    }
}

/**
 * Pilula passiva que informa POR QUE a chamada esta tocando.
 *
 * Deliberadamente sem `onClick`: mudar politica com o telefone tocando e decisao
 * sob pressao, e qualquer alvo tocavel perto de atender/recusar aumenta a chance
 * de erro irreversivel. Ajuste de politica vive em Protecao e no historico.
 */
@Composable
fun CallOriginChip(origin: CallOrigin, modifier: Modifier = Modifier) {
    val style = originStyle(origin)
    val container = style.container
    val content = style.content
    val icon = style.icon
    val label = stringResource(style.labelRes)
    Surface(modifier = modifier, shape = ShapePill, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(
                horizontal = ChipHorizontalPadding,
                vertical = ChipVerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(ChipIconToLabelGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ChipIconSize),
                tint = content,
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

@Preview
@Composable
private fun CallOriginChipPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(ChipHorizontalPadding)) {
                CallOrigin.entries.forEach { origin -> CallOriginChip(origin) }
            }
        }
    }
}

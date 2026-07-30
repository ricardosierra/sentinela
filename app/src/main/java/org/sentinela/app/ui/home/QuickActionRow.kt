package org.sentinela.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapeSmall

private val RowHeight = 72.dp
private val RowPadding = 20.dp
private val IconContainerSize = 40.dp
private val IconSize = 20.dp
private val IconToLabelGap = 16.dp
private val ChevronSize = 24.dp
private val BorderWidth = 1.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Atalho da home: uma linha inteira como alvo unico.
 *
 * A altura e EXIGIDA e nao negociada com o pai. Na Fase 6 um pai comprimiu um controle de 72dp para
 * 23dp em silencio, porque a altura comum negocia; num contrato de alvo minimo essa negociacao e o
 * proprio defeito, e o eixo do desenho e o unico que pega a compressao — o alvo de toque continua
 * correto porque o Compose o expande sozinho.
 *
 * Quando o atalho chega desabilitado, o motivo vem em TEXTO e fica na descricao de estado do PROPRIO
 * no do controle. Atalho inerte sem explicacao esta proibido pelo requisito de honestidade da
 * interface.
 */
@Composable
fun QuickActionRow(
    label: String,
    icon: ImageVector,
    iconContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unavailableReason: String? = null,
) {
    val corDoConteudo = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(RowHeight)
            .border(BorderWidth, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (unavailableReason != null) stateDescription = unavailableReason
            },
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(IconContainerSize)
                    .background(iconContainerColor, ShapeSmall),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = corDoConteudo,
                )
            }
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = IconToLabelGap),
                style = MaterialTheme.typography.bodyLarge,
                color = corDoConteudo,
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(ChevronSize),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview(widthDp = 411)
@Composable
private fun QuickActionRowPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(RowPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickActionRow(
                    label = stringResource(R.string.dashboard_quick_whitelist),
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = {},
                )
                QuickActionRow(
                    label = stringResource(R.string.dashboard_quick_history),
                    icon = Icons.Outlined.History,
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = {},
                    enabled = false,
                    unavailableReason = stringResource(R.string.nav_unavailable),
                )
            }
        }
    }
}

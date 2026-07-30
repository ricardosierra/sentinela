package org.sentinela.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val BarShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val ItemHeight = 56.dp
private val ItemMinTarget = 48.dp
private val IconSize = 24.dp
private val IconToLabelGap = 2.dp
private val LabelSize = 12.sp

/**
 * Um destino da navegacao inferior.
 *
 * `enabled` e `unavailableReason` existem para que os destinos que ainda nao tem
 * tela cheguem DESABILITADOS e com o motivo em texto. Uma aba que leva a tela em
 * branco sem explicacao esta proibida pelo requisito de honestidade da interface;
 * esconder as abas mudaria o desenho da navegacao no meio do caminho, e o usuario
 * merece saber que elas existem.
 */
data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val unavailableReason: String? = null,
)

/**
 * Navegacao inferior com os quatro destinos do produto.
 *
 * A lista de itens chega por parametro: habilitar os destinos que faltam e mudar
 * a lista na tela que chama, nunca este componente.
 *
 * Cada item usa altura EXIGIDA, e nao altura negociavel. Na Fase 6 um pai
 * comprimiu um circulo de 72dp para 23dp em silencio, porque o tamanho comum
 * negocia com o pai; num contrato de alvo minimo essa negociacao e justamente o
 * defeito. O papel de aba e a selecao ficam no no do proprio item.
 */
@Composable
fun SentinelaBottomBar(
    items: List<BottomBarItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BarShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                ItemDaBarra(item = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ItemDaBarra(
    item: BottomBarItem,
    modifier: Modifier = Modifier,
) {
    val cor = when {
        !item.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        item.selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .requiredSizeIn(minWidth = ItemMinTarget, minHeight = ItemMinTarget)
            .requiredHeight(ItemHeight)
            .selectable(
                selected = item.selected,
                enabled = item.enabled,
                role = Role.Tab,
                onClick = item.onClick,
            )
            .semantics(mergeDescendants = true) {
                if (item.unavailableReason != null) stateDescription = item.unavailableReason
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IconToLabelGap, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = cor,
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = LabelSize),
            color = cor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun SentinelaBottomBarPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        val indisponivel = stringResource(R.string.nav_unavailable)
        SentinelaBottomBar(
            items = listOf(
                BottomBarItem(
                    label = stringResource(R.string.nav_home),
                    icon = Icons.Outlined.Home,
                    selected = true,
                    onClick = {},
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_whitelist),
                    icon = Icons.Outlined.VerifiedUser,
                    selected = false,
                    onClick = {},
                    enabled = false,
                    unavailableReason = indisponivel,
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_history),
                    icon = Icons.Outlined.History,
                    selected = false,
                    onClick = {},
                    enabled = false,
                    unavailableReason = indisponivel,
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Outlined.Settings,
                    selected = false,
                    onClick = {},
                ),
            ),
        )
    }
}

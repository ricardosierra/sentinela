package org.sentinela.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val ItemHeight = 40.dp
private val IconSize = 24.dp

/** Um destino da navegacao inferior. */
data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val unavailableReason: String? = null,
)

/** Versao provisoria: sem papel de aba, sem alvo garantido e sem motivo textual. */
@Composable
fun SentinelaBottomBar(
    items: List<BottomBarItem>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(ItemHeight)
                    .clickable(enabled = item.enabled, onClick = item.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                )
                Text(text = item.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

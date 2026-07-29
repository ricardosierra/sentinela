package org.sentinela.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.SentinelaTheme

private val CardPadding = 16.dp
private val CardBorderWidth = 1.dp
private val ItemGap = 12.dp
private val IconToTextGap = 12.dp
private val TitleToItemsGap = 12.dp
private val ItemIconSize = 20.dp

/**
 * Card de leitura honesta: um titulo e uma lista de afirmacoes, cada uma com
 * icone proprio.
 *
 * Decisao de produto travada: os cards "o que muda" e "o que nao muda" da tela
 * de ativacao usam ESTILO IDENTICO. Custo e beneficio recebem o mesmo peso
 * visual — dar destaque ao beneficio seria pressao de ativacao, que o projeto
 * proibe. O unico eixo que os diferencia e o icone de cada item, escolhido por
 * quem chama.
 *
 * Reutilizavel nas Fases 7 e 8.
 */
@Composable
fun HonestyCard(
    title: String,
    items: List<String>,
    itemIcon: ImageVector,
    modifier: Modifier = Modifier,
    itemIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.padding(top = TitleToItemsGap),
                verticalArrangement = Arrangement.spacedBy(ItemGap),
            ) {
                items.forEach { item ->
                    Row {
                        Icon(
                            imageVector = itemIcon,
                            contentDescription = null,
                            modifier = Modifier.size(ItemIconSize),
                            tint = itemIconTint,
                        )
                        Text(
                            text = item,
                            modifier = Modifier.padding(start = IconToTextGap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun HonestyCardPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(CardPadding),
            ) {
                HonestyCard(
                    title = stringResource(R.string.dialer_activation_changes_title),
                    items = listOf(
                        stringResource(R.string.dialer_activation_change_1),
                        stringResource(R.string.dialer_activation_change_2),
                    ),
                    itemIcon = Icons.Outlined.CheckCircle,
                    itemIconTint = MaterialTheme.colorScheme.primary,
                )
                HonestyCard(
                    title = stringResource(R.string.dialer_activation_unchanged_title),
                    items = listOf(
                        stringResource(R.string.dialer_activation_unchanged_1),
                        stringResource(R.string.dialer_activation_unchanged_2),
                    ),
                    itemIcon = Icons.Outlined.Info,
                )
            }
        }
    }
}

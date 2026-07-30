package org.sentinela.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.StatusAttention

private val GroupPadding = 16.dp
private val ItemGap = 16.dp
private val TitleToItemsGap = 16.dp
private val RowMinHeight = 56.dp
private val RowVerticalPadding = 12.dp
private val ArrowSize = 24.dp
private val LabelToValueGap = 16.dp
private val LabelToDescriptionGap = 4.dp
private const val TINT_ALPHA = 0.15f

/**
 * Agrupamento de itens da tela Proteção: um cartão por grupo, com cabeçalho.
 *
 * ## O cabeçalho semântico não é enfeite
 *
 * O leitor de tela navega POR CABEÇALHO. Sem `heading()` no rótulo do grupo, uma tela de dezesseis
 * itens vira uma lista linear intransitável: quem quer chegar à retenção do histórico precisa passar
 * por todas as políticas antes. Com os cabeçalhos declarados, o salto entre grupos custa um gesto.
 *
 * [tinted] pinta o cartão com a cor de atenção a 15% de alfa — é o tratamento do grupo de proteção
 * quando ela está DESLIGADA. A cor sai por literal do arquivo de cores, e não do esquema, pelo
 * mesmo motivo medido nas Fases 6 e 7: a partir do nível 31 o tema deriva o esquema inteiro do papel
 * de parede, e o papel de parede passaria a decidir a diferença entre protegido e desprotegido.
 * A cor nunca é o único sinal: o interruptor dentro do grupo continua anunciando o próprio estado.
 */
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = if (tinted) {
            StatusAttention.copy(alpha = TINT_ALPHA)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(GroupPadding)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.padding(top = TitleToItemsGap),
                verticalArrangement = Arrangement.spacedBy(ItemGap),
                content = content,
            )
        }
    }
}

/**
 * Linha navegável: rótulo, explicação permanente, valor opcional e a seta ao fim.
 *
 * ## O desabilitado mora no nó da linha, nunca no filho
 *
 * A linha INTEIRA é o alvo, então é ela quem responde às buscas do leitor de tela e dos testes.
 * Declarar o estado desabilitado num filho — ou num ancestral que mescla — deixa o estado onde
 * ninguém consulta, e o resultado medido três vezes neste projeto é sempre o mesmo: um controle
 * desenhado com a opacidade de desabilitado seguindo anunciado como habilitado. Por isso `enabled`
 * entra no `clickable` do próprio nó da linha e o motivo entra como descrição de estado no MESMO nó.
 *
 * A seta é decorativa: quem anuncia que a linha leva a outro lugar é o papel de botão.
 */
@Composable
fun SettingsNavRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unavailableReason: String? = null,
    valueText: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (unavailableReason != null) stateDescription = unavailableReason
            }
            .padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = LabelToDescriptionGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (valueText != null) {
            Text(
                text = valueText,
                modifier = Modifier.padding(start = LabelToValueGap),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .padding(start = LabelToValueGap)
                .size(ArrowSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun SettingsGroupPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(GroupPadding),
                verticalArrangement = Arrangement.spacedBy(GroupPadding),
            ) {
                SettingsGroup(title = stringResource(R.string.settings_unknown_policy)) {
                    Text(text = stringResource(R.string.unknown_option_block_desc))
                }
                SettingsGroup(
                    title = stringResource(R.string.settings_protection_toggle),
                    tinted = true,
                ) {
                    Text(text = stringResource(R.string.settings_protection_toggle_desc))
                }
                SettingsGroup(title = stringResource(R.string.settings_dialer_mode)) {
                    SettingsNavRow(
                        label = stringResource(R.string.settings_dialer_mode),
                        description = stringResource(R.string.settings_dialer_mode_desc),
                        onClick = {},
                        enabled = false,
                        unavailableReason = stringResource(R.string.dialer_activation_unavailable),
                    )
                }
            }
        }
    }
}

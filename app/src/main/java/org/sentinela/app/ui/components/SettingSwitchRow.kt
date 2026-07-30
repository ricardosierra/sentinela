package org.sentinela.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val RowMinHeight = 56.dp
private val RowVerticalPadding = 12.dp
private val RowHorizontalPadding = 16.dp
private val LabelToSwitchGap = 16.dp
private val LabelToDescriptionGap = 4.dp
private val SwitchMinTarget = 48.dp

/**
 * Linha de configuracao com interruptor: rotulo, explicacao permanente e o
 * interruptor.
 *
 * ## Por que a explicacao e IRMA do interruptor, e nunca filha
 *
 * A linha tem TRES nos de leitura — rotulo, interruptor e explicacao — e o
 * interruptor nao entra em nenhum container com semantica de mesclagem. Sao dois
 * motivos, e os dois foram medidos:
 *
 * 1. Mesclada, a linha perde o estado do filho em silencio. Foi assim que um
 *    controle desenhado com a opacidade de desabilitado seguiu sendo anunciado
 *    como habilitado: o no interno do interruptor ja mescla e e ele quem responde
 *    as buscas, entao o estado colocado num ancestral fica onde ninguem consulta.
 * 2. Mesclada, a leitura vira um bloco unico ilegivel — rotulo, explicacao de
 *    duas linhas e estado, tudo numa frase — em vez de "rotulo, desativado"
 *    seguido da explicacao, que e a travessia que o usuario de leitor de tela
 *    espera.
 *
 * A explicacao e PERMANENTE, nunca dica que aparece ao toque: a pessoa precisa
 * saber o que o interruptor faz ANTES de mexer nele.
 *
 * Quando a linha esta desabilitada, o motivo chega como descricao de estado do
 * proprio no do interruptor — estado nunca e comunicado so pela opacidade.
 */
@Composable
fun SettingSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unavailableReason: String? = null,
) {
    val estadoLigado = stringResource(R.string.state_on)
    val estadoDesligado = stringResource(R.string.state_off)
    val descricaoDeEstado = when {
        !enabled && unavailableReason != null -> unavailableReason
        checked -> estadoLigado
        else -> estadoDesligado
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.Top,
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .padding(start = LabelToSwitchGap)
                .requiredSizeIn(minWidth = SwitchMinTarget, minHeight = SwitchMinTarget)
                .semantics {
                    role = Role.Switch
                    stateDescription = descricaoDeEstado
                },
            enabled = enabled,
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun SettingSwitchRowPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(RowVerticalPadding)) {
                SettingSwitchRow(
                    label = stringResource(R.string.settings_protection_toggle),
                    description = stringResource(R.string.settings_protection_toggle_desc),
                    checked = true,
                    onCheckedChange = {},
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_notification_enable),
                    description = stringResource(R.string.settings_notification_enable_desc),
                    checked = false,
                    onCheckedChange = {},
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_dialer_mode),
                    description = stringResource(R.string.settings_dialer_mode_desc),
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    unavailableReason = stringResource(R.string.nav_unavailable),
                )
            }
        }
    }
}

package org.sentinela.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.StatusAttention

private val RowMinHeight = 56.dp
private val RowHorizontalPadding = 16.dp
private val IconSize = 24.dp
private val IconToTextGap = 12.dp
private val ActionMinTarget = 48.dp

/**
 * Linha de verificacao: um item conferido, o estado dele em texto e uma acao
 * opcional de correcao.
 *
 * O estado NUNCA e comunicado so por cor. O icone muda, a cor muda, e o texto de
 * estado esta sempre presente ao lado do rotulo — quem nao distingue as duas cores
 * le a mesma informacao.
 *
 * Ponto de risco de semantica mesclada tratado aqui: quando existe acao, o botao
 * dela e um no FOCAVEL SEPARADO, irmao do no da linha, e nunca filho dele. Dentro
 * do no mesclado o botao ficaria inalcancavel pelo leitor de tela, porque o no da
 * linha responderia por ele. A linha e anunciada como rotulo seguido do estado.
 */
@Composable
fun CheckRow(
    label: String,
    stateText: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val anuncio = stringResource(R.string.state_label_with_value, label, stateText)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = anuncio },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Outlined.Error,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = if (ok) CallAccept else StatusAttention,
            )
            Column(modifier = Modifier.padding(start = IconToTextGap)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.requiredHeight(ActionMinTarget),
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(widthDp = 411)
@Composable
private fun CheckRowPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(IconToTextGap)) {
                CheckRow(
                    label = stringResource(R.string.onboarding_check_role),
                    stateText = stringResource(R.string.onboarding_check_granted),
                    ok = true,
                )
                CheckRow(
                    label = stringResource(R.string.onboarding_check_contacts),
                    stateText = stringResource(R.string.onboarding_check_missing),
                    ok = false,
                    actionLabel = stringResource(R.string.dashboard_fix_configuration),
                    onAction = {},
                )
            }
        }
    }
}

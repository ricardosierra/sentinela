package org.sentinela.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium

private val AccentBarWidth = 4.dp
private val BannerPadding = 16.dp
private val IconToTextGap = 12.dp
private val IconSize = 20.dp

/**
 * Aviso informativo. Usado quando o Sentinela deixa de ser o telefone padrao:
 * nada quebrou, o modo filtro continua funcionando.
 *
 * Tom deliberadamente informativo — barra de acento e icone de informacao,
 * nunca cor destrutiva. Alarmar o usuario por uma mudanca reversivel de papel
 * do sistema seria pressao, nao aviso.
 */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(AccentBarWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Row(modifier = Modifier.padding(BannerPadding)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.padding(start = IconToTextGap)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (actionLabel != null && onAction != null) {
                        TextButton(
                            onClick = onAction,
                            contentPadding = PaddingValues(vertical = 0.dp),
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun InfoBannerPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            InfoBanner(
                modifier = Modifier.padding(BannerPadding),
                text = stringResource(R.string.dialer_role_lost_body),
                actionLabel = stringResource(R.string.dialer_role_lost_action),
                onAction = {},
            )
        }
    }
}

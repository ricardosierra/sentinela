package org.sentinela.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val ShieldSize = 24.dp
private val ShieldToNameGap = 8.dp
private const val WATERMARK_ALPHA = 0.6f

/**
 * Marca d'agua das telas de chamada: escudo + nome do app.
 *
 * Existe para o usuario entender POR QUE a tela de chamada mudou de aparencia
 * quando o modo discador esta ativo. E puramente decorativa: `clearAndSetSemantics`
 * sem conteudo a remove da arvore de acessibilidade, entao o leitor de tela nao
 * gasta um foco nela antes de chegar aos botoes de atender e recusar.
 */
@Composable
fun SentinelaWatermark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .alpha(WATERMARK_ALPHA)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(ShieldToNameGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(ShieldSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun SentinelaWatermarkPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface { SentinelaWatermark() }
    }
}

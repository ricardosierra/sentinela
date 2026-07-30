package org.sentinela.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val BarHeight = 64.dp
private val BarHorizontalPadding = 16.dp
private val BrandIconSize = 28.dp
private val BrandGap = 8.dp
private val ActionMinTarget = 48.dp

/**
 * Barra superior comum ao onboarding, as boas-vindas e a home.
 *
 * A marca vem do recurso de nome do aplicativo, nunca de literal: o nome antigo
 * dos mockups foi eliminado do projeto inteiro e o unico jeito de manter essa
 * eliminacao verificavel e ter uma fonte de verdade. O escudo e decorativo — quem
 * carrega o nome e o texto.
 *
 * A marca e um NO UNICO de texto: escudo e nome mesclados em um so no de leitura,
 * porque anunciar um icone de escudo separado do nome nao acrescenta informacao e
 * cria uma parada extra na travessia.
 *
 * O slot central recebe o cabecalho de passo no onboarding; o slot de acoes
 * recebe a acao de pular ou o icone de ajustes, e cada acao tem alvo minimo de
 * 48dp por 48dp.
 */
@Composable
fun SentinelaTopBar(
    modifier: Modifier = Modifier,
    center: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = BarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(BrandIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.padding(start = BrandGap),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (center != null) center()
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actions != null) actions()
        }
    }
}

/**
 * Acao de texto da barra superior — o "pular" do onboarding — com o alvo minimo
 * garantido pelo tamanho exigido, e nao negociado com o pai.
 */
@Composable
fun SentinelaTopBarTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(ActionMinTarget),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Acao de icone da barra superior, com alvo exigido de 48dp por 48dp. */
@Composable
fun SentinelaTopBarIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.requiredSize(ActionMinTarget),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun SentinelaTopBarPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SentinelaTopBar(
                center = { StepHeader(step = 2, total = 6) },
                actions = {
                    SentinelaTopBarTextAction(
                        label = stringResource(R.string.onboarding_skip),
                        onClick = {},
                    )
                },
            )
        }
    }
}

@Preview(widthDp = 411)
@Composable
private fun SentinelaTopBarBrandOnlyPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SentinelaTopBar(
                actions = {
                    SentinelaTopBarIconAction(
                        icon = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.about_title),
                        onClick = {},
                    )
                },
            )
        }
    }
}

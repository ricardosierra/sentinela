package org.sentinela.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeLarge
import org.sentinela.app.ui.theme.ShapeSmall

private val CardHeight = 128.dp
private val CardPadding = 20.dp
private val IconSize = 24.dp
private val BorderWidth = 1.dp
private val SkeletonHeight = 32.dp
private val SkeletonWidth = 64.dp
private val ValueSize = 32.sp
private val ValueLineHeight = 40.sp

/**
 * Cartao de estatistica da home.
 *
 * O valor entra por [StatValue], e essa e a razao de ser deste arquivo: **e o TIPO que impede
 * renderizar zero como reserva de "ainda nao sei".** O `when` abaixo e exaustivo e nenhuma das
 * variantes de ausencia carrega numero algum, entao nao existe caminho em que a tela mostre `0`
 * sem que a contagem tenha sido efetivamente lida. **Acrescentar aqui um parametro numerico —
 * ainda que com valor padrao, ainda que "so para a pre-visualizacao" — reabre exatamente o defeito
 * que este tipo existe para tornar impossivel.**
 *
 * O vidro do desenho original virou camada tonal com borda de um pixel independente. Desfoque em
 * tempo real na home custa quadro e a interface so existe do nivel 31 em diante; a leitura do
 * cartao nao perde nada.
 *
 * O numero sai em tipografia de titulo grande com 32sp: a classe de tipografia usada no desenho
 * original nao existe na configuracao dele, e o desenho saiu com o tamanho de reserva pequeno.
 *
 * A descricao de conteudo e o rotulo SEGUIDO do valor — ler o numero solto nao significa nada.
 */
@Composable
fun StatCard(
    label: String,
    value: StatValue,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val textoDoValor = when (value) {
        is StatValue.Loaded -> value.count.toString()
        StatValue.Unavailable -> stringResource(R.string.dashboard_history_off_value)
        StatValue.Loading -> stringResource(R.string.state_loading)
    }
    val descricao = stringResource(R.string.state_label_with_value, label, textoDoValor)
    Surface(
        modifier = modifier
            .requiredHeight(CardHeight)
            .border(BorderWidth, MaterialTheme.colorScheme.outlineVariant, ShapeLarge)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = accent,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ValorDaEstatistica(valor = value, texto = textoDoValor, accent = accent)
            }
        }
    }
}

/**
 * O valor, com um ramo por variante e nenhum caminho numerico nas variantes de ausencia — e aqui que
 * o zero mentiroso se torna impossivel em vez de proibido por convencao.
 */
@Composable
private fun ValorDaEstatistica(valor: StatValue, texto: String, accent: Color) {
    when (valor) {
        is StatValue.Loaded -> Text(
            text = texto,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = ValueSize,
                lineHeight = ValueLineHeight,
                fontWeight = FontWeight.SemiBold,
            ),
            color = accent,
        )

        StatValue.Unavailable -> Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StatValue.Loading -> Box(
            modifier = Modifier
                .width(SkeletonWidth)
                .height(SkeletonHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, ShapeSmall)
                .clearAndSetSemantics {},
        )
    }
}

@Preview(widthDp = 411)
@Composable
private fun StatCardPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(CardPadding),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(CardPadding)) {
                    StatCard(
                        label = stringResource(R.string.dashboard_total_blocked),
                        value = StatValue.Loaded(42),
                        icon = Icons.Outlined.Block,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.dashboard_blocked_today),
                        value = StatValue.Unavailable,
                        icon = Icons.Outlined.Today,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                StatCard(
                    label = stringResource(R.string.dashboard_total_blocked),
                    value = StatValue.Loading,
                    icon = Icons.Outlined.Block,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

package org.sentinela.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

private val BarHeight = 4.dp
private val BarWidth = 96.dp
private val TextToBarGap = 6.dp
private val BarShape = RoundedCornerShape(50)

/**
 * Cabecalho de progresso do onboarding: o contador em texto e a barra que o
 * acompanha.
 *
 * A barra e DECORATIVA e tem a semantica limpa de proposito. A informacao de
 * progresso vive no texto, que o leitor de tela ja le; uma barra tambem anunciada
 * repetiria o mesmo dado com vocabulario pior ("40 por cento") e acrescentaria um
 * no de parada sem utilidade na travessia.
 *
 * O total chega por parametro. As telas desta fase passam seis passos: o contrato
 * de design resolveu a contradicao entre os tres mockups em favor de um contador
 * unico, e deixar o total dentro do componente esconderia essa decisao aqui.
 */
@Composable
fun StepHeader(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fracao = if (total <= 0) 0f else (step.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TextToBarGap),
    ) {
        Text(
            text = stringResource(R.string.onboarding_step_indicator, step, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .clearAndSetSemantics {}
                .width(BarWidth)
                .height(BarHeight)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fracao)
                    .fillMaxHeight()
                    .clip(BarShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Preview
@Composable
private fun StepHeaderPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepHeader(step = 1, total = 6)
                StepHeader(step = 4, total = 6)
                StepHeader(step = 6, total = 6)
            }
        }
    }
}

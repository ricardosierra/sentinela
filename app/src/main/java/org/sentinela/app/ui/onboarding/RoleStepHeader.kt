package org.sentinela.app.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.call.rememberMotionReduced
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapePill

private val TopBarToHeroGap = 32.dp
private val HeroCircleSize = 96.dp
private val HeroIconSize = 64.dp
private val HeroToTitleGap = 24.dp
private val TitleToChipGap = 12.dp
private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp
private val ChipIconSize = 16.dp
private val ChipIconToTextGap = 8.dp
private val HeroFloatAmplitude = 4.dp

/** Alfa do container de destaque leve do hero e do chip de ativo. */
private const val CONTAINER_ALPHA = 0.20f

/** Ciclo da flutuacao decorativa do hero, em milissegundos (mockup: 6 s). */
private const val FLOAT_CYCLE_MILLIS = 6_000

@Composable
internal fun HeroDoPapel() {
    val movimentoReduzido = rememberMotionReduced()
    val transicao = rememberInfiniteTransition(label = "flutuacao-do-hero")
    val fase by transicao.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FLOAT_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fase-da-flutuacao",
    )
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(top = TopBarToHeroGap)
            .clearAndSetSemantics {}
            .graphicsLayer {
                translationY = if (movimentoReduzido) 0f else fase * HeroFloatAmplitude.toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(HeroCircleSize),
            shape = CircleShape,
            color = cores.primaryContainer.copy(alpha = CONTAINER_ALPHA),
        ) {}
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = cores.primary,
        )
    }
}

@Composable
internal fun TituloDoPapel() {
    Text(
        text = stringResource(R.string.onboarding_role_title),
        modifier = Modifier
            .padding(top = HeroToTitleGap)
            .semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

/**
 * Chip de papel concedido, anunciado como regiao viva educada.
 *
 * A cor vem de [CallAccept], literal fora do esquema: a partir do nivel 31 o tema troca o esquema
 * INTEIRO por um derivado do papel de parede, e deixar o papel de parede decidir a cor de
 * "concedido" foi o defeito que a Fase 6 mediu.
 */
@Composable
internal fun ChipDePapelAtivo() {
    Surface(
        modifier = Modifier
            .padding(top = TitleToChipGap)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = ShapePill,
        color = CallAccept.copy(alpha = CONTAINER_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ChipHorizontalPadding,
                vertical = ChipVerticalPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(ChipIconSize),
                tint = CallAccept,
            )
            Text(
                text = stringResource(R.string.dialer_active_chip),
                modifier = Modifier.padding(start = ChipIconToTextGap),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** O topo do passo com o papel ainda pendente: hero, titulo e nada mais. */
@Preview(widthDp = 411, heightDp = 350)
@Composable
private fun TopoDoPassoPendentePreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroDoPapel()
                TituloDoPapel()
            }
        }
    }
}

/** Papel concedido: o chip entra sob o titulo, na cor literal que o papel de parede nao decide. */
@Preview(widthDp = 411, heightDp = 400)
@Composable
private fun TopoDoPassoConcedidoPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroDoPapel()
                TituloDoPapel()
                ChipDePapelAtivo()
            }
        }
    }
}

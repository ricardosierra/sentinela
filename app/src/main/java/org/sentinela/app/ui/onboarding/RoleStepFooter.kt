package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapePill

private val ScreenPadding = 16.dp
private val CtaGradientHeight = 32.dp
private val CtaHeight = 56.dp
private val CtaIconGap = 8.dp
private val CtaToDisclaimerGap = 16.dp
private val ProgressIndicatorSize = 20.dp
private val ProgressStrokeWidth = 2.dp
private val BannerToCtaGap = 16.dp
private val FooterBottomGap = 32.dp

/**
 * Rodape fixo: o aviso de papel negado, o botao e o esclarecimento.
 *
 * O botao e filho direto deste [Column], que **nao** mescla semantica. Envolve-lo num container que
 * mescle apagaria em silencio o estado desabilitado do ramo de pedido em curso.
 */
@Composable
internal fun RodapeDoPapel(
    state: OnboardingUiState,
    onRequestRole: () -> Unit,
    onNext: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CtaGradientHeight)
            .background(Brush.verticalGradient(listOf(Color.Transparent, cores.surface))),
    )
    Column(
        modifier = Modifier.padding(horizontal = ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.roleDenied) {
            InfoBanner(
                text = stringResource(R.string.onboarding_role_denied),
                modifier = Modifier
                    .padding(bottom = BannerToCtaGap)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                actionLabel = stringResource(R.string.onboarding_role_retry),
                onAction = onRequestRole,
            )
        }
        BotaoDoPassoDoPapel(state = state, onRequestRole = onRequestRole, onNext = onNext)
        Text(
            text = stringResource(R.string.onboarding_role_disclaimer),
            modifier = Modifier
                .padding(top = CtaToDisclaimerGap, bottom = FooterBottomGap)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = cores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BotaoDoPassoDoPapel(
    state: OnboardingUiState,
    onRequestRole: () -> Unit,
    onNext: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val pedindo = state.roleRequestInFlight
    val avanca = state.screeningRoleHeld || state.roleDenied
    val rotulo = when {
        pedindo -> stringResource(R.string.onboarding_role_requesting)
        avanca -> stringResource(R.string.onboarding_next)
        else -> stringResource(R.string.onboarding_role_cta)
    }
    Button(
        onClick = if (avanca) onNext else onRequestRole,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(CtaHeight),
        enabled = !pedindo,
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = cores.primary,
            contentColor = cores.onPrimary,
        ),
    ) {
        if (pedindo) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(ProgressIndicatorSize)
                    .padding(end = CtaIconGap),
                strokeWidth = ProgressStrokeWidth,
            )
        }
        Text(text = rotulo, style = MaterialTheme.typography.labelLarge)
        if (!pedindo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = CtaIconGap),
            )
        }
    }
}

@Composable
private fun RodapeDeExemplo(state: OnboardingUiState) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column { RodapeDoPapel(state = state, onRequestRole = {}, onNext = {}) }
        }
    }
}

/** Sem papel e sem pedido em curso: o botao PEDE o papel. */
@Preview(widthDp = 411, heightDp = 250)
@Composable
private fun RodapeDoPapelPendentePreview() {
    RodapeDeExemplo(OnboardingUiState())
}

/**
 * Pedido em curso: o botao vira "solicitando" e fica DESABILITADO.
 *
 * E o ramo que mais precisa de imagem propria — o desabilitado desaparece em silencio se alguem
 * envolver o botao num container que mescle semantica, e foi o que as Fases 6 e 7 mediram.
 */
@Preview(widthDp = 411, heightDp = 250)
@Composable
private fun RodapeDoPapelSolicitandoPreview() {
    RodapeDeExemplo(OnboardingUiState(roleRequestInFlight = true))
}

/** Papel concedido: o botao vira avancar, e o avanco nunca e automatico. */
@Preview(widthDp = 411, heightDp = 250)
@Composable
private fun RodapeDoPapelConcedidoPreview() {
    RodapeDeExemplo(OnboardingUiState(screeningRoleHeld = true))
}

/** Papel negado: entra o aviso com "tentar de novo", e o botao TAMBEM vira avancar. */
@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun RodapeDoPapelNegadoPreview() {
    RodapeDeExemplo(OnboardingUiState(roleDenied = true))
}

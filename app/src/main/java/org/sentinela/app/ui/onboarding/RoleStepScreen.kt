@file:Suppress("TooManyFunctions")

// Supressao LOCAL, no molde de DialerActivationScreen.kt, em vez de afrouxar o detekt.yml
// compartilhado: os tres ramos de estado do papel mais os blocos nomeados do contrato de design
// somam funcoes pequenas de proposito. Juntar ramos para caber no limite deixaria a tela pior.

package org.sentinela.app.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import org.sentinela.app.ui.components.HonestyCard
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarTextAction
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapePill

private val ScreenPadding = 16.dp
private val TitleToIntroGap = 16.dp
private val IntroToStripGap = 24.dp
private val StripToHonestyGap = 24.dp
private val StripHeight = 80.dp
private val StripIconSize = 16.dp
private val StripIconToTextGap = 8.dp
private val CtaGradientHeight = 32.dp
private val CtaHeight = 56.dp
private val CtaIconGap = 8.dp
private val CtaToDisclaimerGap = 16.dp
private val ProgressIndicatorSize = 20.dp
private val ProgressStrokeWidth = 2.dp
private val BannerToCtaGap = 16.dp
private val FooterBottomGap = 32.dp

/** Largura do corpo em relacao a tela, conforme o contrato de design. */
private const val INTRO_WIDTH_FRACTION = 0.85f

/** Alfa dos containers de destaque leve — hero, faixa de contexto e chip de ativo. */
private const val CONTAINER_ALPHA = 0.20f

/**
 * Duracao da transicao entre passos do onboarding, em milissegundos.
 *
 * O contrato de design pede deslizamento horizontal mais dissolucao de 250 ms, espelhado no
 * retorno. A transicao **nao** vive dentro desta tela: ela pertence ao envelope de navegacao, que e
 * quem conhece os dois passos envolvidos e o sentido do movimento. O que mora aqui e o numero e a
 * regra de supressao, para que o envelope nao precise redescobrir nenhum dos dois.
 */
const val DURACAO_DA_TRANSICAO_DE_PASSO_MILLIS = 250

/**
 * Duracao efetiva da transicao entre passos: zero quando a reducao de movimento esta ligada.
 *
 * Troca instantanea nao e degradacao — e o comportamento pedido. Nenhuma informacao do onboarding
 * depende da animacao; o contador de passo em texto ja diz onde o usuario esta.
 */
@Composable
fun rememberStepTransitionMillis(): Int =
    if (rememberMotionReduced()) 0 else DURACAO_DA_TRANSICAO_DE_PASSO_MILLIS

/**
 * Passo 1 de 6 — o pedido do papel de filtro de chamadas.
 *
 * Composta **pura**: recebe o estado pronto e devolve intencoes. Nenhum container, nenhum dono de
 * estado e nenhuma leitura de repositorio vivem aqui.
 *
 * ## O aviso obrigatorio da fase
 *
 * O cartao de honestidade desta tela e o aviso de que **so chamada de telefone e filtrada**, e ele
 * nao e rodape em cinza: tem o mesmo peso visual do resto da tela, exatamente como a tela de
 * ativacao do modo discador faz com os dois cartoes de peso igual.
 *
 * As tres frases dele ja existem em recurso, escritas nas Fases 5 e 6 a partir da fonte do proprio
 * Android — chamadas de aplicativo de internet fora do alcance, "Nao Perturbe" do sistema valendo
 * por cima, e o registro no historico do telefone que o Android so omite para aplicativo de
 * operadora. **Reescreve-las e o caminho de volta a promessa falsa e e proibido.**
 *
 * ## Os tres ramos de estado do papel
 *
 * Nenhum deles trava o passo, e nenhum deles repete o dialogo do sistema sem toque explicito:
 *
 * - **sem papel e sem pedido em curso:** o botao pede o papel;
 * - **pedido em curso:** o botao vira "solicitando" e fica desabilitado. Ele esta deliberadamente
 *   FORA de qualquer container com semantica de mesclagem: estado declarado em ancestral fica onde
 *   ninguem consulta, e o desabilitado desapareceria em silencio (medido nas Fases 6 e 7);
 * - **papel concedido:** aparece o chip de ativo sob o titulo e o botao vira avancar. O avanco
 *   **nao** e automatico — o usuario le a confirmacao e toca.
 *
 * Papel negado acrescenta o aviso com acao de tentar de novo, e o botao tambem vira avancar: negar
 * o papel custa a triagem, nunca o resto do onboarding.
 *
 * O chip e o aviso de resultado sao regiao viva **educada**, para anunciar a transicao
 * concedido/negado sem roubar o foco do botao. Modo enfatico seria interrupcao, nao aviso.
 */
@Composable
fun RoleStepScreen(
    state: OnboardingUiState,
    onRequestRole: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(
            center = { StepHeader(step = PASSO_DO_PAPEL, total = state.totalSteps) },
            actions = {
                SentinelaTopBarTextAction(
                    label = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroDoPapel()
            TituloDoPapel()
            if (state.screeningRoleHeld) ChipDePapelAtivo()
            Intro()
            FaixaDeContexto()
            CartaoDeEscopo()
        }
        Rodape(
            state = state,
            onRequestRole = onRequestRole,
            onNext = onNext,
        )
    }
}

/** Numero deste passo no fluxo. O total chega pelo estado, nunca por literal aqui. */
private const val PASSO_DO_PAPEL = 1

@Composable
private fun Intro() {
    Text(
        text = stringResource(R.string.onboarding_role_intro),
        modifier = Modifier
            .padding(top = TitleToIntroGap)
            .fillMaxWidth(INTRO_WIDTH_FRACTION),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Faixa de contexto: o gradiente tonal que substitui a imagem remota do mockup. */
@Composable
private fun FaixaDeContexto() {
    val cores = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .padding(top = IntroToStripGap)
            .fillMaxWidth(),
        shape = ShapeMedium,
        color = cores.surfaceContainerLow,
        contentColor = cores.onSurface,
    ) {
        Row(
            modifier = Modifier
                .height(StripHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            cores.primaryContainer.copy(alpha = CONTAINER_ALPHA),
                        ),
                    ),
                )
                .padding(horizontal = ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(StripIconSize),
                tint = cores.primary,
            )
            Text(
                text = stringResource(R.string.welcome_badge_native),
                modifier = Modifier.padding(start = StripIconToTextGap),
                style = MaterialTheme.typography.labelMedium,
                color = cores.primary,
            )
        }
    }
}

/**
 * O aviso obrigatorio de escopo.
 *
 * As tres frases chegam por identificador de recurso, na ordem do contrato de design. Nenhuma delas
 * e reescrita aqui: elas sao a traducao das medicoes das Fases 5 e 6, e reescreve-las e o caminho de
 * volta a promessa falsa.
 */
@Composable
private fun CartaoDeEscopo() {
    HonestyCard(
        title = stringResource(R.string.onboarding_scope_title),
        items = listOf(
            stringResource(R.string.dialer_activation_unchanged_3),
            stringResource(R.string.onboarding_scope_dnd),
            stringResource(R.string.settings_hide_native_log_desc),
        ),
        itemIcon = Icons.Outlined.Info,
        modifier = Modifier.padding(top = StripToHonestyGap, bottom = FooterBottomGap),
        itemIconTint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Rodape fixo: o aviso de papel negado, o botao e o esclarecimento.
 *
 * O botao e filho direto deste [Column], que **nao** mescla semantica. Envolve-lo num container que
 * mescle apagaria em silencio o estado desabilitado do ramo de pedido em curso.
 */
@Composable
private fun Rodape(
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
        BotaoDoPasso(state = state, onRequestRole = onRequestRole, onNext = onNext)
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
private fun BotaoDoPasso(
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

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepPendingPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = PASSO_DO_PAPEL),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepGrantedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = PASSO_DO_PAPEL, screeningRoleHeld = true),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepDeniedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = PASSO_DO_PAPEL, roleDenied = true),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

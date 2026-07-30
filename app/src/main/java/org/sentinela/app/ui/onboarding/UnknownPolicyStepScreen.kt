package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SimCardAlert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.components.OptionCard
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarTextAction
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.components.optionCardGroup
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapePill

private val ScreenPadding = 16.dp
private val CardMaxWidth = 400.dp
private val CardPadding = 16.dp
private val CardBorderWidth = 1.dp
private val HeroCircleSize = 64.dp
private val HeroIconSize = 32.dp
private val HeroToTitleGap = 4.dp
private val TitleToQuestionGap = 4.dp
private val QuestionToOptionsGap = 24.dp
private val BetweenOptionsGap = 8.dp
private val OptionsToCtaGap = 32.dp
private val CtaHeight = 56.dp
private val CtaIconGap = 8.dp
private val CtaToHintGap = 16.dp

/** Alfa dos containers de icone e da borda do cartao flutuante. */
private const val CONTAINER_ALPHA = 0.20f
private const val BORDER_ALPHA = 0.30f

/** Alfa da microcopy do rodape, conforme o contrato de design. */
private const val HINT_ALPHA = 0.60f

/** Numero deste passo no fluxo. O total chega por parametro, nunca por literal aqui. */
private const val PASSO_DOS_DESCONHECIDOS = 2

/**
 * Passo 2 de 6 — o tratamento das chamadas de numeros que nao estao na agenda.
 *
 * Composta **pura**: recebe a politica escolhida e devolve a intencao de troca. Nenhum container,
 * nenhum dono de estado e nenhuma gravacao de configuracao vivem aqui — quem grava e a rota.
 *
 * O cartao central flutuante e a assinatura visual deste passo no mockup e foi mantido de proposito;
 * os passos seguintes usam pagina cheia.
 *
 * ## Duas restricoes de dominio
 *
 * 1. **A politica que nunca silencia nao e oferecida para desconhecidos.** Ela existe no dominio
 *    para a agenda e para a lista pessoal, onde faz sentido garantir que uma origem confiavel jamais
 *    seja silenciada. Para quem o usuario nao conhece ela nao diria nada, e o mockup tambem nao a
 *    oferece: fidelidade e dominio coincidem aqui.
 * 2. **O estilo do bloqueio nao aparece neste passo.** Escolher entre recusar a chamada e manda-la
 *    para a caixa postal e ajuste fino, vive na tela Protecao, e trazer essa escolha para o
 *    onboarding transformaria um passo de uma pergunta em um passo de duas.
 *
 * O icone colorido de cada opcao vem do mockup e e decoracao redundante, nao portadora de estado:
 * quem carrega o estado e o papel de botao de radio do proprio cartao, a borda e o icone de
 * confirmacao.
 */
@Composable
fun UnknownPolicyStepScreen(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(
            center = { StepHeader(step = PASSO_DOS_DESCONHECIDOS, total = TOTAL_DE_PASSOS) },
            actions = {
                SentinelaTopBarTextAction(
                    label = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
                )
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
            contentAlignment = Alignment.Center,
        ) {
            CartaoDoPasso(selected = selected, onSelect = onSelect, onNext = onNext)
        }
    }
}

@Composable
private fun CartaoDoPasso(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
    onNext: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.widthIn(max = CardMaxWidth),
        shape = ShapeMedium,
        color = cores.surfaceContainerLow,
        contentColor = cores.onSurface,
        border = BorderStroke(
            width = CardBorderWidth,
            color = cores.outlineVariant.copy(alpha = BORDER_ALPHA),
        ),
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroDoPasso()
            Titulo()
            Pergunta()
            Opcoes(selected = selected, onSelect = onSelect)
            BotaoDeAvanco(onNext = onNext)
            Microcopy()
        }
    }
}

@Composable
private fun HeroDoPasso() {
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(HeroCircleSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(HeroCircleSize),
            shape = CircleShape,
            color = cores.primaryContainer.copy(alpha = CONTAINER_ALPHA),
        ) {}
        Icon(
            imageVector = Icons.Outlined.SimCardAlert,
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = cores.primary,
        )
    }
}

@Composable
private fun Titulo() {
    Text(
        text = stringResource(R.string.unknown_title),
        modifier = Modifier.padding(top = HeroToTitleGap),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Pergunta() {
    Text(
        text = stringResource(R.string.unknown_question),
        modifier = Modifier.padding(top = TitleToQuestionGap),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * As tres opcoes, reunidas num grupo de escolha unica para o leitor de tela.
 *
 * Cada cartao e o proprio controle: nenhum controle interativo pode ser filho dele, porque o no do
 * cartao mescla os descendentes e responde por todos.
 */
@Composable
private fun Opcoes(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(top = QuestionToOptionsGap)
            .fillMaxWidth()
            .optionCardGroup(),
        verticalArrangement = Arrangement.spacedBy(BetweenOptionsGap),
    ) {
        OptionCard(
            title = stringResource(R.string.unknown_option_block),
            description = stringResource(R.string.unknown_option_block_desc),
            icon = Icons.Outlined.Block,
            selected = selected == OriginPolicy.BLOCK,
            onClick = { onSelect(OriginPolicy.BLOCK) },
            iconContainerColor = cores.errorContainer.copy(alpha = CONTAINER_ALPHA),
            iconTint = cores.error,
        )
        OptionCard(
            title = stringResource(R.string.unknown_option_silence),
            description = stringResource(R.string.unknown_option_silence_desc),
            icon = Icons.Outlined.NotificationsOff,
            selected = selected == OriginPolicy.SILENCE,
            onClick = { onSelect(OriginPolicy.SILENCE) },
            iconContainerColor = cores.secondaryContainer.copy(alpha = CONTAINER_ALPHA),
            iconTint = cores.secondary,
        )
        OptionCard(
            title = stringResource(R.string.unknown_option_allow),
            description = stringResource(R.string.unknown_option_allow_desc),
            icon = Icons.Outlined.Phone,
            selected = selected == OriginPolicy.RING,
            onClick = { onSelect(OriginPolicy.RING) },
            iconContainerColor = cores.tertiaryContainer.copy(alpha = CONTAINER_ALPHA),
            iconTint = cores.tertiary,
        )
    }
}

/**
 * Botao de avanco.
 *
 * Nunca desabilitado por falta de escolha: este passo sempre chega com uma opcao pre-selecionada, e
 * o padrao de fabrica e bloquear. Fica fora de qualquer container que mescle semantica.
 */
@Composable
private fun BotaoDeAvanco(onNext: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Button(
        onClick = onNext,
        modifier = Modifier
            .padding(top = OptionsToCtaGap)
            .fillMaxWidth()
            .requiredHeight(CtaHeight),
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = cores.primary,
            contentColor = cores.onPrimary,
        ),
    ) {
        Text(
            text = stringResource(R.string.onboarding_next),
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.padding(start = CtaIconGap),
        )
    }
}

@Composable
private fun Microcopy() {
    Text(
        text = stringResource(R.string.onboarding_change_later),
        modifier = Modifier
            .padding(top = CtaToHintGap)
            .fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HINT_ALPHA),
        textAlign = TextAlign.Center,
    )
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun UnknownPolicyStepBlockPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            UnknownPolicyStepScreen(
                selected = OriginPolicy.BLOCK,
                onSelect = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun UnknownPolicyStepSilencePreview() {
    SentinelaTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            UnknownPolicyStepScreen(
                selected = OriginPolicy.SILENCE,
                onSelect = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

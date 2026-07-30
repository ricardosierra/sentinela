package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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

private const val STEP = 4
private const val TOTAL_STEPS = 6

private val ScreenPadding = 24.dp
private val BlockGap = 24.dp
private val TitleToDescGap = 8.dp
private val OptionGap = 16.dp
private val OptionsToCtaGap = 32.dp
private val CtaToBackGap = 8.dp
private val BackToHintGap = 16.dp
private val CircleSize = 80.dp
private val CircleIconSize = 40.dp
private val CtaHeight = 56.dp
private val MinTarget = 48.dp

/**
 * Passo 4 de 6 do onboarding: como o Sentinela trata a whitelist pessoal — a lista
 * de numeros que o usuario marcou dentro do aplicativo, separada da agenda do
 * telefone.
 *
 * Composta PURA, sem dono de estado e sem plataforma.
 *
 * ## Tres adaptacoes do mockup, e o motivo de cada uma
 *
 * 1. **O quadro ilustrado remoto virou cartao tonal.** O mockup carrega uma imagem
 *    de servidor externo. O aplicativo nao declara acesso a internet e nunca vai
 *    declarar no MVP, entao a ilustracao seria um quadro vazio permanente. No lugar
 *    dela entra o cartao explicativo com circulo, escudo e o texto que explica o que
 *    e a whitelist — a informacao que a imagem tentava dar.
 * 2. **O aviso temporizado virou texto permanente.** No mockup o texto de rodape e um
 *    aviso que desliza depois de um segundo. Informacao que aparece por conta propria
 *    e desaparece por conta propria e informacao perdida, e aviso com tempo e hostil
 *    a quem usa leitor de tela: ele pode sumir antes de ser lido, e nao ha como
 *    voltar. Aqui o texto fica FIXO no rodape.
 * 3. **O botao e "Proximo", nao "Finalizar Configuracao".** Os tres mockups do
 *    onboarding se contradiziam quanto ao numero de passos; o contrato de design
 *    resolveu isso com um contador unico de seis passos, e finalizar so existe no
 *    passo 6.
 *
 * Uma quarta correcao, de texto: o mockup rotula o padrao como "padrao do sistema",
 * o que e impreciso — e o padrao do SENTINELA. O selo usado e o de padrao, e a
 * descricao e a honesta que ja existe no recurso, que diz que o "Nao Perturbe" do
 * sistema continua valendo por cima. Nenhuma dessas frases nasce em Kotlin.
 */
@Composable
fun WhitelistPolicyStepScreen(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SentinelaTopBar(
            center = { StepHeader(step = STEP, total = TOTAL_STEPS) },
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
        ) {
            ConteudoDoPassoDaWhitelist(selected = selected, onSelect = onSelect)
            Spacer(modifier = Modifier.height(OptionsToCtaGap))
            RodapeDoPasso(onNext = onNext, onBack = onBack)
        }
    }
}

@Composable
private fun ConteudoDoPassoDaWhitelist(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
) {
    Spacer(modifier = Modifier.height(BlockGap))
    Text(
        text = stringResource(R.string.whitelist_setup_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(TitleToDescGap))
    Text(
        text = stringResource(R.string.whitelist_setup_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(BlockGap))
    CartaoExplicativo()
    Spacer(modifier = Modifier.height(BlockGap))
    Text(
        text = stringResource(R.string.whitelist_setup_question),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(OptionGap))
    OpcoesDaWhitelist(selected = selected, onSelect = onSelect)
}

/**
 * Substitui o quadro ilustrado remoto do mockup. O escudo e decorativo: quem
 * carrega a informacao sao os dois textos.
 */
@Composable
private fun CartaoExplicativo() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(CircleSize),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(CircleSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {}
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(CircleIconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.whitelist_setup_what_title),
                modifier = Modifier.padding(top = OptionGap),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.whitelist_setup_what_desc),
                modifier = Modifier.padding(top = TitleToDescGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * As quatro politicas da whitelist. "Nunca Silenciar" e o PADRAO do Sentinela nesta
 * origem e leva o selo — a ordem dos cartoes coloca o padrao primeiro.
 */
@Composable
private fun OpcoesDaWhitelist(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
) {
    Column(
        modifier = Modifier.optionCardGroup(),
        verticalArrangement = Arrangement.spacedBy(OptionGap),
    ) {
        OptionCard(
            title = stringResource(R.string.whitelist_option_never_silence),
            description = stringResource(R.string.whitelist_option_never_silence_desc),
            icon = Icons.Outlined.NotificationsActive,
            selected = selected == OriginPolicy.NEVER_SILENCE,
            onClick = { onSelect(OriginPolicy.NEVER_SILENCE) },
            badge = stringResource(R.string.contacts_default_badge),
        )
        OptionCard(
            title = stringResource(R.string.whitelist_option_ring),
            description = stringResource(R.string.whitelist_option_ring_desc),
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            selected = selected == OriginPolicy.RING,
            onClick = { onSelect(OriginPolicy.RING) },
        )
        OptionCard(
            title = stringResource(R.string.whitelist_option_block),
            description = stringResource(R.string.whitelist_option_block_desc),
            icon = Icons.Outlined.Block,
            selected = selected == OriginPolicy.BLOCK,
            onClick = { onSelect(OriginPolicy.BLOCK) },
        )
        OptionCard(
            title = stringResource(R.string.whitelist_option_silence),
            description = stringResource(R.string.whitelist_option_silence_desc),
            icon = Icons.Outlined.NotificationsOff,
            selected = selected == OriginPolicy.SILENCE,
            onClick = { onSelect(OriginPolicy.SILENCE) },
        )
    }
}

/**
 * Os dois botoes e o texto de rodape. O texto e PERMANENTE: no mockup ele era um
 * aviso com tempo, e informacao que desaparece sozinha e informacao perdida.
 */
@Composable
private fun RodapeDoPasso(
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .requiredSizeIn(minWidth = MinTarget, minHeight = MinTarget)
            .requiredHeight(CtaHeight),
        shape = ShapePill,
    ) {
        Text(
            text = stringResource(R.string.onboarding_next),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Spacer(modifier = Modifier.height(CtaToBackGap))
    TextButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .requiredSizeIn(minWidth = MinTarget, minHeight = MinTarget)
            .requiredHeight(MinTarget),
    ) {
        Text(
            text = stringResource(R.string.onboarding_back),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(modifier = Modifier.height(BackToHintGap))
    Text(
        text = stringResource(R.string.whitelist_setup_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(BlockGap))
}

/**
 * Uma pre-visualizacao por opcao selecionada, as quatro, vindas de um provedor de
 * parametro — quatro funcoes anotadas estourariam o limite de funcoes por arquivo do
 * detekt, e afrouxar a regra compartilhada por pre-visualizacao seria o preco errado.
 */
private class WhitelistPolicyPreviews : PreviewParameterProvider<OriginPolicy> {
    override val values: Sequence<OriginPolicy> = OriginPolicy.entries.asSequence()
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun WhitelistPolicyStepPreview(
    @PreviewParameter(WhitelistPolicyPreviews::class) selected: OriginPolicy,
) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            WhitelistPolicyStepScreen(
                selected = selected,
                onSelect = {},
                onNext = {},
                onBack = {},
                onSkip = {},
            )
        }
    }
}

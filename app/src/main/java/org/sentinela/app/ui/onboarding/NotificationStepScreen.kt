package org.sentinela.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.ui.call.rememberMotionReduced
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.OptionCard
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarTextAction
import org.sentinela.app.ui.components.SettingSwitchRow
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapePill

private const val STEP_NUMBER = 5
private const val STEP_TOTAL = 6

private val ScreenPadding = 16.dp
private val TopGap = 24.dp
private val TitleToBodyGap = 16.dp
private val BodyToSwitchGap = 24.dp
private val SwitchToOptionsGap = 16.dp
private val BetweenOptionsGap = 12.dp
private val OptionsToCtaGap = 32.dp
private val CtaHeight = 56.dp
private const val EXPAND_ANIMATION_MILLIS = 200

/**
 * Passo 5 de 6 — o aviso próprio de chamada bloqueada, sempre por opt-in.
 *
 * **Este passo não pressiona, e a ausência de pressão é o contrato.** O aviso nasce DESLIGADO
 * porque o valor do produto é justamente não interromper: um app cujo propósito é silenciar o
 * desconhecido não pode entregar, na instalação, uma notificação a cada bloqueio. Por isso aqui
 * não existe palavra de recomendação, não existe destaque visual diferente entre ligar e não
 * ligar, não existe contador, não existe urgência e não existe segunda chance de convencimento.
 * Ligar e seguir sem ligar são caminhos de peso igual — o mesmo princípio que a tela de ativação
 * do modo discador aplica aos cards de custo e benefício.
 *
 * O padrão desligado já está escrito na própria descrição da configuração
 * (`settings_notification_enable_desc`: "Vem desligado"), e é ela que a linha do interruptor
 * mostra de forma permanente. O usuário sabe o que o interruptor faz ANTES de mexer nele.
 *
 * **Nenhuma das duas identificações mostra a sequência completa de dígitos.** A mascarada usa a
 * máscara única do aplicativo, aplicada na camada que grava o histórico, e a anônima não mostra
 * dígito algum. Não existe terceira opção, e nunca poderá existir: o número completo jamais entra
 * no objeto de notificação.
 *
 * **O pedido do sistema não é feito aqui.** A tela apenas chama [onEnabledChange] com `true`; quem
 * grava a marca de "já perguntei" e só então dispara o pedido é o dono de estado, nessa ordem —
 * contrato das Fases 4, 5 e 6, travado em 07-04. O usuário pode matar o app com o diálogo do
 * sistema aberto, e uma marca gravada no retorno faria o app perguntar de novo.
 *
 * Semântica: o interruptor tem papel de interruptor e descrição de estado explícita no PRÓPRIO nó
 * (a explicação permanente é irmã dele, nunca filha mesclada — mesclada, o leitor de tela leria um
 * bloco único ilegível e o estado do filho desapareceria em silêncio). O container das sub-opções
 * é região viva educada, para anunciar que novas opções surgiram sem roubar o foco de quem acabou
 * de tocar no interruptor.
 */
@Composable
fun NotificationStepScreen(
    enabled: Boolean,
    identification: NotificationIdentification,
    permission: RuntimePermissionAsk,
    onEnabledChange: (Boolean) -> Unit,
    onIdentificationChange: (NotificationIdentification) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movimentoReduzido = rememberMotionReduced()
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(
            center = { StepHeader(step = STEP_NUMBER, total = STEP_TOTAL) },
            actions = {
                SentinelaTopBarTextAction(
                    label = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
                )
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TituloEExplicacao()
            SettingSwitchRow(
                label = stringResource(R.string.settings_notification_enable),
                description = stringResource(R.string.settings_notification_enable_desc),
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.padding(top = BodyToSwitchGap),
            )
            if (permission == RuntimePermissionAsk.DENIED_ONCE) {
                InfoBanner(
                    text = stringResource(R.string.notification_permission_rationale),
                    modifier = Modifier.padding(
                        horizontal = ScreenPadding,
                        vertical = TitleToBodyGap,
                    ),
                )
            }
            SubOpcoesDeIdentificacao(
                visible = enabled,
                identification = identification,
                onIdentificationChange = onIdentificationChange,
                instantaneo = movimentoReduzido,
            )
            BotaoDeAvanco(onNext = onNext)
        }
    }
}

/** Título e explicação do passo, sem uma palavra de recomendação em nenhum dos dois. */
@Composable
private fun TituloEExplicacao() {
    Text(
        text = stringResource(R.string.settings_notification_enable),
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = TopGap),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = stringResource(R.string.settings_notification_enable_desc),
        modifier = Modifier.padding(horizontal = ScreenPadding),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Avançar tem o mesmo texto e o mesmo peso com o aviso ligado ou desligado. */
@Composable
private fun BotaoDeAvanco(onNext: () -> Unit) {
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .padding(top = OptionsToCtaGap, bottom = OptionsToCtaGap)
            .height(CtaHeight),
        shape = ShapePill,
    ) {
        Text(
            text = stringResource(R.string.onboarding_next),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * As duas identificações, visíveis somente com o aviso ligado.
 *
 * A região viva educada existe porque as opções SURGEM: sem ela, o leitor de tela não teria como
 * saber que a tela ganhou dois controles depois do toque no interruptor.
 */
@Composable
private fun SubOpcoesDeIdentificacao(
    visible: Boolean,
    identification: NotificationIdentification,
    onIdentificationChange: (NotificationIdentification) -> Unit,
    instantaneo: Boolean,
) {
    val duracao = if (instantaneo) 0 else EXPAND_ANIMATION_MILLIS
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        enter = expandVertically(animationSpec = tween(duracao)) +
            fadeIn(animationSpec = tween(duracao)),
        exit = shrinkVertically(animationSpec = tween(duracao)) +
            fadeOut(animationSpec = tween(duracao)),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = ScreenPadding)
                .padding(top = SwitchToOptionsGap)
                .semantics { selectableGroup() },
            verticalArrangement = Arrangement.spacedBy(BetweenOptionsGap),
        ) {
            OptionCard(
                title = stringResource(R.string.settings_notification_identification_masked),
                description = stringResource(R.string.notification_channel_blocked_desc),
                icon = Icons.Outlined.NotificationsNone,
                selected = identification == NotificationIdentification.MASKED,
                onClick = { onIdentificationChange(NotificationIdentification.MASKED) },
            )
            OptionCard(
                title = stringResource(R.string.settings_notification_identification_anonymous),
                description = stringResource(R.string.notification_blocked_anonymous),
                icon = Icons.Outlined.VisibilityOff,
                selected = identification == NotificationIdentification.ANONYMOUS,
                onClick = { onIdentificationChange(NotificationIdentification.ANONYMOUS) },
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun NotificationStepDesligadoPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            NotificationStepScreen(
                enabled = false,
                identification = NotificationIdentification.MASKED,
                permission = RuntimePermissionAsk.NEVER_ASKED,
                onEnabledChange = {},
                onIdentificationChange = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun NotificationStepMascaradaPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            NotificationStepScreen(
                enabled = true,
                identification = NotificationIdentification.MASKED,
                permission = RuntimePermissionAsk.GRANTED,
                onEnabledChange = {},
                onIdentificationChange = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun NotificationStepAnonimaPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            NotificationStepScreen(
                enabled = true,
                identification = NotificationIdentification.ANONYMOUS,
                permission = RuntimePermissionAsk.DENIED_ONCE,
                onEnabledChange = {},
                onIdentificationChange = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

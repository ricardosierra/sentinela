package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PriorityHigh
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.data.contacts.canRequest
import org.sentinela.app.data.contacts.shouldOfferSystemSettings
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.OptionCard
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarTextAction
import org.sentinela.app.ui.components.SettingSwitchRow
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.components.optionCardGroup
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapePill

private const val STEP = 3
private const val TOTAL_STEPS = 6
private const val GRANTED_CHIP_ALPHA = 0.2f

private val ScreenPadding = 24.dp
private val BlockGap = 24.dp
private val TitleToExplainerGap = 16.dp
private val OptionGap = 16.dp
private val CardPadding = 16.dp
private val IconSize = 24.dp
private val IconToTextGap = 12.dp
private val CtaHeight = 56.dp
private val MinTarget = 48.dp

/**
 * Passo 3 de 6 do onboarding: como o Sentinela trata quem esta na agenda do
 * telefone, e o pedido de leitura da agenda que sustenta essa distincao.
 *
 * Composta PURA: nao conhece dono de estado, nao conhece plataforma e nao pede
 * permissao — quem dispara o launcher e a rota, por [onGrantContacts].
 *
 * ## Os quatro ramos da permissao, e por que sao quatro
 *
 * `shouldShowRequestPermissionRationale` devolve `false` nos DOIS extremos: antes
 * do primeiro pedido e depois da negacao definitiva. Por isso o estado chega aqui
 * ja resolvido em quatro valores por [ContactsPermissionState], apoiado no flag
 * persistido. Esta tela nao reimplementa nenhuma condicao: o ramo sai de um `when`
 * exaustivo sobre o enum, e a oferta de acao usa [canRequest] e
 * [shouldOfferSystemSettings] como estao. Nenhum ramo e mudo — em `NEVER_ASKED`
 * aparece a justificativa com o pedido, em `GRANTED` o cartao colapsa num chip, e
 * as duas negacoes tem aviso proprio. Na negacao definitiva NAO existe botao de
 * pedir: a plataforma nao mostra mais o dialogo, e oferecer o pedido ali seria um
 * toque que nao faz nada.
 *
 * ## A consequencia honesta fica NA TELA
 *
 * Sem a leitura da agenda a consulta responde indisponivel e a chamada cai na
 * politica de erro — ou seja, contatos podem ser tratados como desconhecidos. E
 * exatamente o que diz o texto do ramo de negacao simples, e por isso ele aparece
 * como AVISO na propria tela, nunca como nota de rodape. (O nome do recurso NAO e
 * citado aqui: o criterio de aceite conta ocorrencias por texto e nao distingue
 * KDoc de codigo — a mesma armadilha registrada nas Fases 3 e 5.)
 *
 * ## Alternativa considerada e rejeitada: desabilitar os cartoes
 *
 * As quatro opcoes continuam HABILITADAS e editaveis sem a permissao. A escolha e
 * preferencia persistida e passa a valer no instante em que a permissao for
 * concedida, entao desabilitar nao protegeria nada — apenas pressionaria o usuario
 * a conceder, e desabilitar sem explicar e pior que nao desabilitar. Alem disso o
 * estado desabilitado e justamente onde a semantica mesclada perde informacao: um
 * cartao desenhado em opacidade reduzida segue sendo anunciado como habilitado se o
 * estado nao mora no no do proprio controle.
 *
 * A descricao de "Nunca Silenciar" e a corrigida na Fase 1: o mockup afirmava que a
 * opcao ignora o "Nao Perturbe", e isso e FALSO — medido na fonte do Android na
 * Fase 5. O texto do recurso diz que o "Nao Perturbe" do sistema continua valendo, e
 * nao deve ser reescrito.
 */
@Composable
fun ContactsPolicyStepScreen(
    permission: ContactsPermissionState,
    selected: OriginPolicy,
    blockPrivate: Boolean,
    hasWhatsApp: Boolean = false,
    onSelect: (OriginPolicy) -> Unit,
    onBlockPrivateChange: (Boolean) -> Unit,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onNext: () -> Unit,
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
        ConteudoDoPasso(
            permission = permission,
            selected = selected,
            blockPrivate = blockPrivate,
            hasWhatsApp = hasWhatsApp,
            onSelect = onSelect,
            onBlockPrivateChange = onBlockPrivateChange,
            onGrantContacts = onGrantContacts,
            onOpenAppSettings = onOpenAppSettings,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onNext,
            modifier = Modifier
                .padding(horizontal = ScreenPadding, vertical = CardPadding)
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
    }
}

@Composable
private fun ConteudoDoPasso(
    permission: ContactsPermissionState,
    selected: OriginPolicy,
    blockPrivate: Boolean,
    hasWhatsApp: Boolean,
    onSelect: (OriginPolicy) -> Unit,
    onBlockPrivateChange: (Boolean) -> Unit,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        Spacer(modifier = Modifier.height(BlockGap))
        Text(
            text = stringResource(R.string.contacts_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(TitleToExplainerGap))
        Text(
            text = stringResource(R.string.contacts_explainer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(BlockGap))
        CartaoDePermissao(
            permission = permission,
            onGrantContacts = onGrantContacts,
            onOpenAppSettings = onOpenAppSettings,
        )
        if (hasWhatsApp) {
            Spacer(modifier = Modifier.height(BlockGap))
            InfoBanner(
                text = stringResource(R.string.whatsapp_contacts_warning_desc),
                actionLabel = null,
                onAction = null
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))
        OpcoesDeContatos(selected = selected, onSelect = onSelect)
        Spacer(modifier = Modifier.height(BlockGap))
        SettingSwitchRow(
            label = stringResource(R.string.settings_block_private),
            description = stringResource(R.string.settings_block_private_desc),
            checked = blockPrivate,
            onCheckedChange = onBlockPrivateChange,
        )
        Spacer(modifier = Modifier.height(BlockGap))
    }
}

/**
 * O cartao de permissao, um ramo por estado. O `when` e exaustivo sobre o enum de
 * proposito: estado novo da permissao quebraria a compilacao aqui, em vez de
 * produzir silenciosamente um ramo mudo na tela.
 */
@Composable
private fun CartaoDePermissao(
    permission: ContactsPermissionState,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val podePedir = permission.canRequest
    val ofereceConfiguracoes = permission.shouldOfferSystemSettings
    when (permission) {
        ContactsPermissionState.GRANTED -> ChipDeAgendaConcedida()
        ContactsPermissionState.NEVER_ASKED -> CartaoDeJustificativa(
            onGrantContacts = onGrantContacts.takeIf { podePedir },
        )
        ContactsPermissionState.DENIED_ONCE -> InfoBanner(
            text = stringResource(R.string.contacts_permission_denied),
            actionLabel = stringResource(R.string.dialer_activation_grant_contacts)
                .takeIf { podePedir },
            onAction = onGrantContacts.takeIf { podePedir },
        )
        ContactsPermissionState.DENIED_PERMANENTLY -> InfoBanner(
            text = stringResource(R.string.contacts_permission_blocked),
            actionLabel = stringResource(R.string.about_open_app_settings)
                .takeIf { ofereceConfiguracoes },
            onAction = onOpenAppSettings.takeIf { ofereceConfiguracoes },
        )
    }
}

/** Justificativa antes do primeiro dialogo do sistema, com o pedido de leitura. */
@Composable
private fun CartaoDeJustificativa(onGrantContacts: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(ScreenPadding)) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.contacts_permission_rationale),
                modifier = Modifier.padding(top = IconToTextGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onGrantContacts != null) {
                Button(
                    onClick = onGrantContacts,
                    modifier = Modifier
                        .padding(top = CardPadding)
                        .requiredSizeIn(minWidth = MinTarget, minHeight = MinTarget),
                    shape = ShapePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.dialer_activation_grant_contacts),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** Concedido: o cartao colapsa num chip discreto — nada mais a pedir nem a explicar. */
@Composable
private fun ChipDeAgendaConcedida() {
    Surface(
        shape = ShapePill,
        color = CallAccept.copy(alpha = GRANTED_CHIP_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CardPadding, vertical = IconToTextGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = CallAccept,
            )
            Text(
                text = stringResource(R.string.contacts_permission_granted),
                modifier = Modifier.padding(start = IconToTextGap),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * As quatro politicas de contatos. "Tocar" e o PADRAO do Sentinela e leva o selo;
 * nenhum cartao recebe estado desabilitado, em nenhum estado da permissao.
 */
@Composable
private fun OpcoesDeContatos(
    selected: OriginPolicy,
    onSelect: (OriginPolicy) -> Unit,
) {
    Column(
        modifier = Modifier.optionCardGroup(),
        verticalArrangement = Arrangement.spacedBy(OptionGap),
    ) {
        OptionCard(
            title = stringResource(R.string.contacts_option_ring),
            description = stringResource(R.string.contacts_option_ring_desc),
            icon = Icons.Outlined.NotificationsActive,
            selected = selected == OriginPolicy.RING,
            onClick = { onSelect(OriginPolicy.RING) },
            badge = stringResource(R.string.contacts_default_badge),
        )
        OptionCard(
            title = stringResource(R.string.contacts_option_block),
            description = stringResource(R.string.contacts_option_block_desc),
            icon = Icons.Outlined.Block,
            selected = selected == OriginPolicy.BLOCK,
            onClick = { onSelect(OriginPolicy.BLOCK) },
        )
        OptionCard(
            title = stringResource(R.string.contacts_option_silence),
            description = stringResource(R.string.contacts_option_silence_desc),
            icon = Icons.Outlined.NotificationsOff,
            selected = selected == OriginPolicy.SILENCE,
            onClick = { onSelect(OriginPolicy.SILENCE) },
        )
        OptionCard(
            title = stringResource(R.string.contacts_option_never_silence),
            description = stringResource(R.string.contacts_option_never_silence_desc),
            icon = Icons.Outlined.PriorityHigh,
            selected = selected == OriginPolicy.NEVER_SILENCE,
            onClick = { onSelect(OriginPolicy.NEVER_SILENCE) },
        )
    }
}

/**
 * Uma pre-visualizacao por estado da permissao, as quatro. Vem de um provedor de
 * parametro em vez de quatro funcoes anotadas porque quatro funcoes estouram o
 * limite de funcoes por arquivo do detekt, e afrouxar a regra compartilhada para
 * acomodar pre-visualizacao seria o preco errado.
 */
private class ContactsPermissionPreviews : PreviewParameterProvider<ContactsPermissionState> {
    override val values: Sequence<ContactsPermissionState> =
        ContactsPermissionState.entries.asSequence()
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun ContactsPolicyStepPreview(
    @PreviewParameter(ContactsPermissionPreviews::class) permission: ContactsPermissionState,
) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            ContactsPolicyStepScreen(
                permission = permission,
                selected = OriginPolicy.RING,
                blockPrivate = true,
                hasWhatsApp = true,
                onSelect = {},
                onBlockPrivateChange = {},
                onGrantContacts = {},
                onOpenAppSettings = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

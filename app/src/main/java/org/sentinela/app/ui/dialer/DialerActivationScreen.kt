// Supressao LOCAL, no molde do plano 06-01, em vez de afrouxar o detekt.yml compartilhado: a
// contagem alta aqui e consequencia de duas exigencias do proprio contrato — cada ramo de estado
// vira um composable pequeno e nomeado, e os cinco ramos mais a escala de fonte grande viram seis
// pre-visualizacoes. Juntar ramos para caber no limite deixaria a tela pior.
@file:Suppress("TooManyFunctions")

package org.sentinela.app.ui.dialer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.HonestyCard
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.theme.ShapePill
import org.sentinela.app.ui.theme.SentinelaTheme

private val ScreenPadding = 16.dp
private val IconCircleSize = 96.dp
private val HeroIconSize = 48.dp
private val IconToTitleGap = 24.dp
private val TitleToIntroGap = 8.dp
private val IntroToCardsGap = 24.dp
private val BetweenCardsGap = 16.dp
private val CardsToCtaGap = 32.dp
private val CtaToHintGap = 8.dp
private val CtaHeight = 56.dp
private val ChipPaddingHorizontal = 12.dp
private val ChipPaddingVertical = 6.dp

/** Opacidade dos containers de destaque leve (chip de ativo e card de pré-requisito). */
private const val CONTAINER_ALPHA = 0.15f

/**
 * Ativação e reversão do modo discador.
 *
 * **Esta tela não vende.** Os dois cards — "o que muda" e "o que não muda" — usam o MESMO
 * componente com o MESMO estilo, por decisão de produto: custo e benefício com peso visual
 * idêntico. Nenhum destaque, nenhuma cor e nenhuma ordem favorecem a ativação, e nenhum texto usa
 * urgência, recomendação ou superlativo.
 *
 * Três verdades que a copy desta tela precisa preservar, todas medidas na fonte da plataforma na
 * Fase 5 — mexer no texto sem reler isto é reintroduzir uma promessa falsa:
 *
 * 1. **A chamada continua sendo registrada no histórico do telefone.** Pedir ao sistema para omitir
 *    esse registro é honrado apenas para aplicativo de operadora, e ser telefone padrão **não**
 *    destrava isso. Não existe redação honesta em que este modo esconda a chamada do histórico.
 * 2. **O modo "Não Perturbe" do sistema continua valendo por cima.** Ele é um filtro paralelo, e
 *    contorná-lo exigiria permissão proibida neste projeto.
 * 3. **Chamadas de aplicativos de internet continuam fora do alcance.** Elas não passam pelo
 *    sistema de telefonia.
 *
 * E uma quarta, por omissão deliberada: **nada aqui promete bloquear número privado neste modo.**
 * A hipótese de que o papel de telefone padrão destrave esse caso segue **não verificada**, e
 * prometer o que não foi medido é o mesmo defeito das três acima.
 *
 * Reverter abre o **seletor do sistema** — é ele a confirmação, e por isso não existe diálogo
 * próprio nesta tela. O aplicativo nunca força a troca, nem na ida nem na volta; a intenção do
 * seletor e o pedido do papel vêm de `DialerRoleManager`, e o botão de voltar é sempre tonal, nunca
 * destrutivo: reverter não destrói dado nenhum.
 *
 * A leitura da agenda é pré-requisito, não detalhe: sem saber quem é contato, ativar o modo para
 * aplicar políticas por origem faria o oposto do que promete — todo mundo viraria desconhecido.
 */
@Composable
fun DialerActivationScreen(
    state: DialerModeState,
    onRequestRole: () -> Unit,
    onRevert: () -> Unit,
    onGrantContacts: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        ActivationTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroIcon()
            Text(
                text = stringResource(tituloDoEstado(state)),
                modifier = Modifier.padding(top = IconToTitleGap),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (state == DialerModeState.ACTIVE) {
                ActiveChip()
            }
            Text(
                text = stringResource(introDoEstado(state)),
                modifier = Modifier.padding(top = TitleToIntroGap),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            when (state) {
                DialerModeState.UNAVAILABLE -> Unit
                DialerModeState.ACTIVE -> ActiveBody(onRevert = onRevert)
                DialerModeState.ROLE_LOST -> RoleLostBody(onRequestRole = onRequestRole)
                DialerModeState.BLOCKED_BY_CONTACTS -> OfferBody(
                    contactsGranted = false,
                    onRequestRole = onRequestRole,
                    onGrantContacts = onGrantContacts,
                )
                DialerModeState.OFFERED -> OfferBody(
                    contactsGranted = true,
                    onRequestRole = onRequestRole,
                    onGrantContacts = onGrantContacts,
                )
            }
        }
    }
}

private fun tituloDoEstado(state: DialerModeState): Int = when (state) {
    DialerModeState.ACTIVE -> R.string.dialer_active_title
    DialerModeState.ROLE_LOST -> R.string.dialer_role_lost_title
    else -> R.string.dialer_activation_title
}

private fun introDoEstado(state: DialerModeState): Int = when (state) {
    DialerModeState.UNAVAILABLE -> R.string.dialer_activation_unavailable
    DialerModeState.ACTIVE -> R.string.dialer_revert_hint
    DialerModeState.ROLE_LOST -> R.string.dialer_role_lost_body
    else -> R.string.dialer_activation_intro
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivationTopBar(onBack: (() -> Unit)?) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.settings_dialer_mode)) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
    )
}

@Composable
private fun HeroIcon() {
    Box(
        modifier = Modifier
            .padding(top = IconToTitleGap)
            .size(IconCircleSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(IconCircleSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) { }
        Icon(
            imageVector = Icons.Outlined.Dialpad,
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ActiveChip() {
    Surface(
        modifier = Modifier.padding(top = TitleToIntroGap),
        shape = ShapePill,
        color = MaterialTheme.colorScheme.primary.copy(alpha = CONTAINER_ALPHA),
    ) {
        Text(
            text = stringResource(R.string.dialer_active_chip),
            modifier = Modifier.padding(
                horizontal = ChipPaddingHorizontal,
                vertical = ChipPaddingVertical,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Ramo de oferta: os dois cards de peso idêntico, o card de pré-requisito quando a agenda está
 * negada, e o convite. Com a agenda negada o convite fica desabilitado — não escondido: esconder
 * daria a impressão de que o modo não existe.
 */
@Composable
private fun OfferBody(
    contactsGranted: Boolean,
    onRequestRole: () -> Unit,
    onGrantContacts: () -> Unit,
) {
    ChangesCard(modifier = Modifier.padding(top = IntroToCardsGap))
    UnchangedCard(modifier = Modifier.padding(top = BetweenCardsGap))
    if (!contactsGranted) {
        ContactsPrerequisiteCard(
            modifier = Modifier.padding(top = BetweenCardsGap),
            onGrantContacts = onGrantContacts,
        )
    }
    Button(
        onClick = onRequestRole,
        modifier = Modifier
            .padding(top = CardsToCtaGap)
            .fillMaxWidth()
            .height(CtaHeight),
        enabled = contactsGranted,
        shape = ShapePill,
    ) {
        Text(text = stringResource(R.string.dialer_activation_cta))
    }
    Hint(text = stringResource(R.string.dialer_activation_cta_hint))
}

/**
 * Painel de reversão. Card único, porque o usuário não está mais decidindo se ativa — está lendo o
 * que vale agora. O botão é tonal, nunca destrutivo.
 */
@Composable
private fun ActiveBody(onRevert: () -> Unit) {
    HonestyCard(
        modifier = Modifier.padding(top = IntroToCardsGap),
        title = stringResource(R.string.dialer_active_changes_title),
        items = listOf(
            stringResource(R.string.dialer_activation_change_2),
            stringResource(R.string.dialer_activation_change_3),
            stringResource(R.string.dialer_activation_unchanged_1),
            stringResource(R.string.dialer_activation_limit_calls),
        ),
        itemIcon = Icons.Outlined.Info,
    )
    FilledTonalButton(
        onClick = onRevert,
        modifier = Modifier
            .padding(top = CardsToCtaGap)
            .fillMaxWidth()
            .height(CtaHeight),
        shape = ShapePill,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(text = stringResource(R.string.dialer_revert_cta))
    }
    Hint(text = stringResource(R.string.dialer_activation_cta_hint))
}

/**
 * Papel perdido: aviso **informativo**, nunca erro. Nada quebrou — o modo filtro segue operante e a
 * única coisa que o usuário perdeu foi a tela de chamada própria.
 */
@Composable
private fun RoleLostBody(onRequestRole: () -> Unit) {
    InfoBanner(
        modifier = Modifier.padding(top = IntroToCardsGap),
        text = stringResource(R.string.dialer_role_lost_body),
        actionLabel = stringResource(R.string.dialer_role_lost_action),
        onAction = onRequestRole,
    )
    ChangesCard(modifier = Modifier.padding(top = BetweenCardsGap))
    UnchangedCard(modifier = Modifier.padding(top = BetweenCardsGap))
    Hint(text = stringResource(R.string.dialer_activation_cta_hint))
}

@Composable
private fun ChangesCard(modifier: Modifier = Modifier) {
    HonestyCard(
        modifier = modifier,
        title = stringResource(R.string.dialer_activation_changes_title),
        items = listOf(
            stringResource(R.string.dialer_activation_change_1),
            stringResource(R.string.dialer_activation_change_2),
            stringResource(R.string.dialer_activation_change_3),
        ),
        itemIcon = Icons.Outlined.CheckCircle,
        itemIconTint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun UnchangedCard(modifier: Modifier = Modifier) {
    HonestyCard(
        modifier = modifier,
        title = stringResource(R.string.dialer_activation_unchanged_title),
        items = listOf(
            stringResource(R.string.dialer_activation_unchanged_1),
            stringResource(R.string.dialer_activation_unchanged_2),
            stringResource(R.string.dialer_activation_unchanged_3),
            stringResource(R.string.dialer_activation_unchanged_4),
            stringResource(R.string.dialer_activation_limit_calls),
        ),
        itemIcon = Icons.Outlined.Info,
    )
}

@Composable
private fun ContactsPrerequisiteCard(onGrantContacts: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = CONTAINER_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(ScreenPadding)) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.dialer_activation_contacts_required),
                modifier = Modifier.padding(top = CtaToHintGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onGrantContacts,
                modifier = Modifier.padding(top = BetweenCardsGap),
            ) {
                Text(text = stringResource(R.string.dialer_activation_grant_contacts))
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = CtaToHintGap, bottom = CardsToCtaGap)
            .fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActivationPreview(state: DialerModeState) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            DialerActivationScreen(
                state = state,
                onRequestRole = {},
                onRevert = {},
                onGrantContacts = {},
                onBack = {},
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ActivationOfferedPreview() = ActivationPreview(DialerModeState.OFFERED)

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ActivationBlockedPreview() = ActivationPreview(DialerModeState.BLOCKED_BY_CONTACTS)

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ActivationActivePreview() = ActivationPreview(DialerModeState.ACTIVE)

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ActivationRoleLostPreview() = ActivationPreview(DialerModeState.ROLE_LOST)

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ActivationUnavailablePreview() = ActivationPreview(DialerModeState.UNAVAILABLE)

@Preview(widthDp = 360, heightDp = 1200, fontScale = 2f)
@Composable
private fun ActivationLargeFontPreview() = ActivationPreview(DialerModeState.OFFERED)

// A home e a tela com mais estados do produto: onze compostas privadas pequenas, uma por bloco e uma
// por estado degradado, sao a forma legivel de tratar a secao 8 inteira. Afrouxar as regras
// compartilhadas do detekt por causa desta tela seria o preco errado — a supressao fica no arquivo,
// no precedente que a Fase 7 ja abriu.
@file:Suppress("LongParameterList", "TooManyFunctions")

package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.BottomBarItem
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.SentinelaBottomBar
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium

private val ScreenPadding = 16.dp
private val BlockGap = 24.dp
private val StatGap = 16.dp
private val HeaderToCardGap = 12.dp
private val QuickActionGap = 12.dp
private val BottomGap = 32.dp
private val EmptyCardPadding = 20.dp
private val EmptyIconSize = 32.dp

/** No maximo dois avisos ao mesmo tempo. O terceiro empurraria o conteudo real fora da tela. */
private const val MAX_BANNERS = 2

private const val TRAVERSAL_HERO = 1f
private const val TRAVERSAL_AVISOS = 2f
private const val TRAVERSAL_ESTATISTICAS = 3f
private const val TRAVERSAL_ULTIMA = 4f
private const val TRAVERSAL_ATALHOS = 5f
private const val TRAVERSAL_BARRA_INFERIOR = 6f

/**
 * Ordem de travessia DECLARADA para cada bloco da home.
 *
 * A ordem visual e a ordem de composicao coincidem hoje, mas depender dessa coincidencia deixaria a
 * leitura de tela a merce de qualquer reordenacao de layout futura — inclusive das que a rolagem e as
 * quebras por escala de fonte produzem. Cada bloco e um grupo de travessia com indice proprio; a
 * marca vem antes por estar na barra superior, e o interruptor continua sendo no separado DENTRO do
 * grupo do cartao principal, nunca um estado declarado no grupo.
 */
private fun Modifier.ordemDeTravessia(indice: Float): Modifier = semantics {
    isTraversalGroup = true
    traversalIndex = indice
}

/**
 * A home inteira, composta PURA.
 *
 * Nada aqui conhece dono de estado, montador de dependencias ou ciclo de vida: a reconsulta viva do
 * papel na retomada mora na camada de rota, e a tela recebe o retrato pronto. Isso e o que permite
 * compor os oito estados degradados num teste sem plataforma nenhuma.
 *
 * **Toda a tela ROLA.** Nenhuma tela desta fase pode depender de caber: a escala de fonte do sistema
 * pode dobrar o tamanho de todo texto, e conteudo que "cabe" no aparelho do desenvolvedor deixa de
 * caber no aparelho de quem mais precisa de acessibilidade.
 *
 * **Precedencia dos avisos** e obrigatoria e esta em [avisosDaHome]: papel de triagem ausente, depois
 * leitura da agenda negada, depois historico desligado, depois papel de discador perdido. Do terceiro
 * em diante o excedente vira uma unica linha que leva a tela de Protecao.
 *
 * O desfoque da barra superior do desenho original virou elevacao tonal, e o desfoque dos cartoes
 * virou camada tonal com borda: desfoque em tempo real custa quadro e a interface so existe a partir
 * do nivel 31.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onProtectionChange: (Boolean) -> Unit,
    onFixRole: () -> Unit,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableHistory: () -> Unit,
    onRetryHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDialerActivation: () -> Unit,
    bottomBar: @Composable () -> Unit,
    onAcceptRating: () -> Unit,
    onDismissRating: () -> Unit,
    modifier: Modifier = Modifier,
    nowUtcMillis: Long = System.currentTimeMillis(),
) {
    if (state.showRatingInvitation) {
        RatingBottomSheet(
            onAccept = onAcceptRating,
            onDismiss = onDismissRating
        )
    }

    val avisos = avisosDaHome(
        state = state,
        onFixRole = onFixRole,
        onGrantContacts = onGrantContacts,
        onOpenAppSettings = onOpenAppSettings,
        onEnableHistory = onEnableHistory,
        onRetryHistory = onRetryHistory,
        onOpenDialerActivation = onOpenDialerActivation,
    )
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SentinelaTopBar(
                    actions = {
                        SentinelaTopBarIconAction(
                            icon = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            onClick = onOpenSettings,
                        )
                    },
                )
                CorpoDaHome(
                    state = state,
                    avisos = avisos,
                    nowUtcMillis = nowUtcMillis,
                    onProtectionChange = onProtectionChange,
                    onOpenSettings = onOpenSettings,
                    onOpenWhitelist = onOpenWhitelist,
                    onOpenHistory = onOpenHistory,
                )
            }
            Box(modifier = Modifier.ordemDeTravessia(TRAVERSAL_BARRA_INFERIOR)) {
                bottomBar()
            }
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            )
        }
    }
}

/** Os blocos da home na ordem e com os espacamentos ditados pelo contrato de interface. */
@Composable
private fun CorpoDaHome(
    state: HomeUiState,
    avisos: List<AvisoDaHome>,
    nowUtcMillis: Long,
    onProtectionChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        Spacer(modifier = Modifier.height(BlockGap))
        StatusHeroCard(
            protectionEnabled = state.protectionEnabled,
            onProtectionChange = onProtectionChange,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_HERO),
        )
        if (avisos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(BlockGap))
            BlocoDeAvisos(
                avisos = avisos,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.ordemDeTravessia(TRAVERSAL_AVISOS),
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))
        BlocoDeEstatisticas(
            state = state,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_ESTATISTICAS),
        )
        if (state.historyEnabled) {
            Spacer(modifier = Modifier.height(BlockGap))
            BlocoDaUltimaBloqueada(
                state = state,
                nowUtcMillis = nowUtcMillis,
                onOpenHistory = onOpenHistory,
                modifier = Modifier.ordemDeTravessia(TRAVERSAL_ULTIMA),
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))
        BlocoDeAtalhos(
            onOpenWhitelist = onOpenWhitelist,
            onOpenHistory = onOpenHistory,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_ATALHOS),
        )
        Spacer(modifier = Modifier.height(BottomGap))
    }
}

/** Um aviso da home, ja resolvido em texto e acao. */
private data class AvisoDaHome(
    val text: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/**
 * Os avisos que o estado atual justifica, na ordem de precedencia ditada pelo contrato.
 *
 * Dois ramos merecem registro, porque nao sao acabamento:
 *
 * - **Protecao desligada pelo usuario nao gera aviso.** E escolha, nao erro, e alarmar alguem pela
 *   propria decisao e pressao. O cartao principal ja mostra o estado, e as estatisticas continuam
 *   visiveis.
 * - **Sem o papel disponivel no aparelho, o botao de correcao NAO aparece.** A intencao de pedido nao
 *   existe ali, o toque nao resolveria nada, e oferecer um botao inerte e pior que nao oferecer.
 */
@Composable
private fun avisosDaHome(
    state: HomeUiState,
    onFixRole: () -> Unit,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableHistory: () -> Unit,
    onRetryHistory: () -> Unit,
    onOpenDialerActivation: () -> Unit,
): List<AvisoDaHome> {
    val avisos = mutableListOf<AvisoDaHome>()
    if (!state.screeningRoleHeld) {
        avisos += AvisoDaHome(
            text = stringResource(R.string.dashboard_role_missing),
            actionLabel = if (state.screeningRoleAvailable) {
                stringResource(R.string.dashboard_fix_configuration)
            } else {
                null
            },
            onAction = if (state.screeningRoleAvailable) onFixRole else null,
        )
    }
    if (state.contactsPermission != ContactsPermissionState.GRANTED) {
        val definitiva = state.contactsPermission == ContactsPermissionState.DENIED_PERMANENTLY
        avisos += AvisoDaHome(
            text = stringResource(R.string.dashboard_contacts_missing),
            actionLabel = stringResource(
                if (definitiva) R.string.about_open_app_settings
                else R.string.dialer_activation_grant_contacts,
            ),
            onAction = if (definitiva) onOpenAppSettings else onGrantContacts,
        )
    }
    if (!state.historyEnabled) {
        avisos += AvisoDaHome(
            text = stringResource(R.string.dashboard_history_off),
            actionLabel = stringResource(R.string.dashboard_history_off_action),
            onAction = onEnableHistory,
        )
    }
    if (state.readError) {
        avisos += AvisoDaHome(
            text = stringResource(R.string.state_error),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetryHistory,
        )
    }
    if (state.dialerMode == DialerModeState.ROLE_LOST) {
        avisos += AvisoDaHome(
            text = stringResource(R.string.dialer_role_lost_body),
            actionLabel = stringResource(R.string.dialer_role_lost_action),
            onAction = onOpenDialerActivation,
        )
    }
    return avisos
}

@Composable
private fun BlocoDeAvisos(
    avisos: List<AvisoDaHome>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QuickActionGap)) {
        avisos.take(MAX_BANNERS).forEach { aviso ->
            InfoBanner(
                text = aviso.text,
                actionLabel = aviso.actionLabel,
                onAction = aviso.onAction,
            )
        }
        if (avisos.size > MAX_BANNERS) {
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = stringResource(R.string.dashboard_more_warnings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BlocoDeEstatisticas(state: HomeUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(StatGap)) {
        StatCard(
            label = stringResource(R.string.dashboard_total_blocked),
            value = state.totalBlocked,
            icon = Icons.Outlined.Block,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.dashboard_blocked_today),
            value = state.blockedToday,
            icon = Icons.Outlined.Today,
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BlocoDaUltimaBloqueada(
    state: HomeUiState,
    nowUtcMillis: Long,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ultima = state.lastBlocked
    val tempo = ultima?.let { relativeTimeLabel(it.timestampUtcMillis, nowUtcMillis) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_last_blocked),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (tempo != null) {
                Text(
                    text = tempo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(HeaderToCardGap))
        if (ultima != null && tempo != null) {
            LastBlockedCard(
                maskedNumber = ultima.maskedNumber,
                reasonLabel = stringResource(ultima.reasonLabelRes),
                relativeTime = tempo,
                onClick = onOpenHistory,
            )
        } else {
            CartaoVazio()
        }
    }
}

/** Estado vazio da ultima bloqueada: historico ligado e nada bloqueado ainda. */
@Composable
private fun CartaoVazio() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(EmptyCardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.requiredSize(EmptyIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_empty),
                modifier = Modifier.padding(start = StatGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BlocoDeAtalhos(
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QuickActionGap)) {
        QuickActionRow(
            label = stringResource(R.string.dashboard_quick_whitelist),
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onOpenWhitelist,
        )
        QuickActionRow(
            label = stringResource(R.string.dashboard_quick_history),
            icon = Icons.Outlined.History,
            iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            onClick = onOpenHistory,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBottomSheet(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ScreenPadding)
                .padding(bottom = BottomGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.review_prompt_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(StatGap))
            Text(
                text = stringResource(R.string.review_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(BlockGap))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.review_prompt_rate))
            }
            Spacer(modifier = Modifier.height(QuickActionGap))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.review_prompt_later))
            }
        }
    }
}

@Composable
private fun itensDaBarra(
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
): List<BottomBarItem> {
    val indisponivel = stringResource(R.string.nav_unavailable)
    return listOf(
        BottomBarItem(
            label = stringResource(R.string.nav_home),
            icon = Icons.Filled.Home,
            selected = true,
            onClick = {},
        ),
        BottomBarItem(
            label = stringResource(R.string.nav_whitelist),
            icon = Icons.Outlined.VerifiedUser,
            selected = false,
            onClick = onOpenWhitelist,
            enabled = false,
            unavailableReason = indisponivel,
        ),
        BottomBarItem(
            label = stringResource(R.string.nav_history),
            icon = Icons.Outlined.History,
            selected = false,
            onClick = onOpenHistory,
            enabled = false,
            unavailableReason = indisponivel,
        ),
        BottomBarItem(
            label = stringResource(R.string.nav_settings),
            icon = Icons.Outlined.Settings,
            selected = false,
            onClick = onOpenSettings,
        ),
    )
}

/** Um estado degradado por pre-visualizacao: os oito da secao 8 do contrato de interface. */
private class HomeStatePreviews : PreviewParameterProvider<HomeUiState> {
    private val ultima = LastBlockedUi(
        maskedNumber = "+55 11 9****-1234",
        reasonLabelRes = R.string.history_unknown_number,
        timestampUtcMillis = 0L,
    )
    private val base = HomeUiState(
        protectionEnabled = true,
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        totalBlocked = StatValue.Loaded(42),
        blockedToday = StatValue.Loaded(3),
        lastBlocked = ultima,
    )
    override val values: Sequence<HomeUiState> = sequenceOf(
        base.copy(screeningRoleHeld = false, protectionEnabled = false),
        base.copy(protectionEnabled = false),
        base.copy(contactsPermission = ContactsPermissionState.DENIED_ONCE),
        base.copy(
            historyEnabled = false,
            totalBlocked = StatValue.Unavailable,
            blockedToday = StatValue.Unavailable,
            lastBlocked = null,
        ),
        base.copy(
            totalBlocked = StatValue.Loaded(0),
            blockedToday = StatValue.Loaded(0),
            lastBlocked = null,
        ),
        base.copy(dialerMode = DialerModeState.ROLE_LOST),
        base.copy(totalBlocked = StatValue.Loading, blockedToday = StatValue.Loading),
        base.copy(
            readError = true,
            totalBlocked = StatValue.Unavailable,
            blockedToday = StatValue.Unavailable,
        ),
    )
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeStatePreviews::class) state: HomeUiState,
) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(
            state = state,
            onProtectionChange = {},
            onFixRole = {},
            onGrantContacts = {},
            onOpenAppSettings = {},
            onEnableHistory = {},
            onRetryHistory = {},
            onOpenSettings = {},
            onOpenWhitelist = {},
            onOpenHistory = {},
            onOpenDialerActivation = {},
            bottomBar = {
                SentinelaBottomBar(
                    items = itensDaBarra({}, {}, {})
                )
            },
            onAcceptRating = {},
            onDismissRating = {},
            nowUtcMillis = 0L,
        )
    }
}

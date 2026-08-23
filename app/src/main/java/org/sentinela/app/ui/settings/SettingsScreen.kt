@file:Suppress("TooManyFunctions")

package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sentinela.app.R
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.HonestyCard
import org.sentinela.app.ui.components.InfoBanner
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
import org.sentinela.app.ui.components.SettingSwitchRow
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Tela Proteção: os dezesseis itens da §9 do contrato de interface, agrupados, com explicação
 * permanente sob cada um e efeito imediato na triagem.
 *
 * ## Efeito imediato, e por que não existe passo de confirmação da gravação (§9.1)
 *
 * Cada controle chama seu retorno de chamada no instante do toque. A gravação acontece no
 * repositório de configurações, cujo retrato é servido de cache mantido por coletor desde a Fase 3,
 * e a mudança vale na PRÓXIMA chamada triada. O retorno para o usuário é o próprio controle se
 * mexendo — nada de aviso temporário a cada toque, que viraria ruído numa tela de dezesseis itens.
 *
 * Um botão intermediário de confirmação da gravação tornaria possível a tela e a triagem
 * discordarem: a tela mostraria a escolha nova enquanto a decisão ainda usaria a antiga. Como o
 * retrato já é imediato, esse botão só acrescentaria a janela de divergência.
 *
 * **Nenhuma troca de política pede confirmação.** É reversível, e confirmação excessiva ensina o
 * usuário a tocar em "sim" sem ler — o que corrói justamente as duas confirmações que importam.
 * Só o que PERDE DADO confirma (§9.2), e são exatamente duas: limpar o histórico e escolher não
 * guardar registro nenhum.
 *
 * ## Explicação permanente, nunca dica ao toque
 *
 * O critério de sucesso da fase é o usuário ENTENDER o que cada política faz. Explicação que só
 * aparece ao segurar o dedo é explicação que a maioria nunca lê, e o leitor de tela a perde por
 * completo. Toda descrição aqui é texto irmão do controle, sempre visível.
 *
 * ## Fonte única de verdade das quatro honestidades (item 15)
 *
 * O cartão de limitações reusa, por identificador de recurso, as mesmas quatro frases do primeiro
 * passo do onboarding e da tela de ativação do modo discador. **Reescrevê-las aqui criaria uma
 * segunda versão da verdade**, e a Fase 5 existiu em parte para corrigir exatamente isso: o registro
 * no histórico do telefone sempre acontece (o Android só omite para app de operadora, e o papel de
 * telefone padrão NÃO destrava), o "Não Perturbe" do sistema não é contornável, as chamadas de
 * mensageiro seguem fora do alcance e nada sai do aparelho. Duas cópias divergiriam, e a cópia
 * errada é sempre a que promete demais.
 */
@Suppress("LongMethod")
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    hasWhatsApp: Boolean = false,
    onBack: () -> Unit,
    onProtectionChange: (Boolean) -> Unit,
    onFixRole: () -> Unit,
    onUnknownPolicy: (OriginPolicy) -> Unit,
    onContactsPolicy: (OriginPolicy) -> Unit,
    onWhitelistPolicy: (OriginPolicy) -> Unit,
    onBlockPrivateChange: (Boolean) -> Unit,
    onBlockMode: (BlockMode) -> Unit,
    onHideNativeLogChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onNotificationIdentification: (NotificationIdentification) -> Unit,
    onOpenDialerActivation: () -> Unit,
    onRepeatedCallChange: (Boolean) -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onRetention: (RetentionPolicy) -> Unit,
    onClearHistory: () -> Unit,
    onMaskNumbersChange: (Boolean) -> Unit,
    onFallback: (FallbackPolicy) -> Unit,
    onOpenAbout: () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmarLimpeza by remember { mutableStateOf(false) }
    var confirmarNaoGuardar by remember { mutableStateOf(false) }
    val avisos = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val historicoDesligadoMantem = stringResource(R.string.settings_history_off_kept)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraDaProtecao(onBack = onBack) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(hostState = avisos) },
    ) { areaInterna ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(areaInterna)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            GrupoDeProtecao(
                state = state,
                onProtectionChange = onProtectionChange,
                onFixRole = onFixRole,
                onHideNativeLogChange = onHideNativeLogChange,
                onRepeatedCallChange = onRepeatedCallChange,
            )
            GrupoDeDesconhecidos(
                state = state,
                onUnknownPolicy = onUnknownPolicy,
                onBlockPrivateChange = onBlockPrivateChange,
                onBlockMode = onBlockMode,
            )
            GrupoDeContatos(state = state, hasWhatsApp = hasWhatsApp, onContactsPolicy = onContactsPolicy)
            GrupoDaListaPessoal(state = state, onWhitelistPolicy = onWhitelistPolicy)
            GrupoDeNotificacao(
                state = state,
                onNotificationChange = onNotificationChange,
                onNotificationIdentification = onNotificationIdentification,
            )
            GrupoDeHistorico(
                state = state,
                onHistoryEnabledChange = { ligado ->
                    onHistoryEnabledChange(ligado)
                    // Desligar o histórico NÃO apaga nada, então não confirma; só informa que os
                    // registros já guardados continuam no aparelho.
                    if (!ligado) escopo.launch { avisos.showSnackbar(historicoDesligadoMantem) }
                },
                onRetention = { politica ->
                    if (politica == RetentionPolicy.NEVER_STORE) {
                        confirmarNaoGuardar = true
                    } else {
                        onRetention(politica)
                    }
                },
                onPedirLimpeza = { confirmarLimpeza = true },
            )
            GrupoDePoliticaDeFalha(state = state, onFallback = onFallback)
            GrupoDePrivacidade(state = state, onMaskNumbersChange = onMaskNumbersChange)
            CartaoDeLimitacoes()
            GrupoDeDestinos(
                state = state,
                onOpenDialerActivation = onOpenDialerActivation,
                onOpenAbout = onOpenAbout,
            )
            Spacer(modifier = Modifier.height(BottomGap))
        }
    }

    if (confirmarLimpeza) {
        ConfirmacaoDeLimpeza(
            registros = state.historyRecordCount,
            onConfirm = {
                confirmarLimpeza = false
                onClearHistory()
            },
            onDismiss = { confirmarLimpeza = false },
        )
    }
    if (confirmarNaoGuardar) {
        ConfirmacaoDeNaoGuardar(
            onConfirm = {
                confirmarNaoGuardar = false
                onRetention(RetentionPolicy.NEVER_STORE)
            },
            onDismiss = { confirmarNaoGuardar = false },
        )
    }
}

@Composable
private fun BarraDaProtecao(onBack: () -> Unit) {
    SentinelaTopBar(
        center = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = {
            SentinelaTopBarIconAction(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
        },
    )
}

/** Item 4 — as quatro políticas da lista pessoal. */
@Composable
private fun GrupoDaListaPessoal(state: SettingsUiState, onWhitelistPolicy: (OriginPolicy) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_whitelist_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_never_silence),
                description = stringResource(R.string.whitelist_option_never_silence_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.whitelistPolicy == OriginPolicy.NEVER_SILENCE,
                onClick = { onWhitelistPolicy(OriginPolicy.NEVER_SILENCE) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_ring),
                description = stringResource(R.string.whitelist_option_ring_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.whitelistPolicy == OriginPolicy.RING,
                onClick = { onWhitelistPolicy(OriginPolicy.RING) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_block),
                description = stringResource(R.string.whitelist_option_block_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.whitelistPolicy == OriginPolicy.BLOCK,
                onClick = { onWhitelistPolicy(OriginPolicy.BLOCK) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.whitelist_option_silence),
                description = stringResource(R.string.whitelist_option_silence_desc),
                icon = Icons.Outlined.NotificationsOff,
                selected = state.settings.whitelistPolicy == OriginPolicy.SILENCE,
                onClick = { onWhitelistPolicy(OriginPolicy.SILENCE) },
            )
        }
    }
}

/**
 * Item 8 — a notificação própria e as duas formas de identificação.
 *
 * As sub-opções só existem com o interruptor ligado: oferecer a escolha de COMO identificar algo que
 * não será mostrado é pedir decisão sem efeito.
 */
@Composable
private fun GrupoDeNotificacao(
    state: SettingsUiState,
    onNotificationChange: (Boolean) -> Unit,
    onNotificationIdentification: (NotificationIdentification) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_show_notification)) {
        SettingSwitchRow(
            label = stringResource(R.string.settings_notification_enable),
            description = stringResource(R.string.settings_notification_enable_desc),
            checked = state.settings.showOwnNotification,
            onCheckedChange = onNotificationChange,
        )
        if (state.settings.showOwnNotification) {
            EscolhaUnica {
                OpcaoDePolitica(
                    title = stringResource(R.string.settings_notification_identification_masked),
                    description = stringResource(R.string.notification_channel_blocked_desc),
                    icon = Icons.Outlined.Info,
                    selected = state.settings.notificationIdentification ==
                        NotificationIdentification.MASKED,
                    onClick = { onNotificationIdentification(NotificationIdentification.MASKED) },
                )
                OpcaoDePolitica(
                    title = stringResource(R.string.settings_notification_identification_anonymous),
                    description = stringResource(R.string.notification_blocked_anonymous),
                    icon = Icons.Outlined.NotificationsOff,
                    selected = state.settings.notificationIdentification ==
                        NotificationIdentification.ANONYMOUS,
                    onClick = { onNotificationIdentification(NotificationIdentification.ANONYMOUS) },
                )
            }
        }
    }
}

/** Itens 11, 12 e 13 — o histórico local, a janela de retenção e a limpeza. */
@Composable
private fun GrupoDeHistorico(
    state: SettingsUiState,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onRetention: (RetentionPolicy) -> Unit,
    onPedirLimpeza: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_retention_title)) {
        SettingSwitchRow(
            label = stringResource(R.string.settings_history_enabled),
            description = stringResource(R.string.settings_history_enabled_desc),
            checked = state.settings.historyEnabled,
            onCheckedChange = onHistoryEnabledChange,
        )
        EscolhaUnica {
            RetencaoOpcao(state, RetentionPolicy.NEVER_STORE, R.string.settings_retention_never, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_7, R.string.settings_retention_7, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_30, R.string.settings_retention_30, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_90, R.string.settings_retention_90, onRetention)
            RetencaoOpcao(state, RetentionPolicy.MANUAL, R.string.settings_retention_manual, onRetention)
        }
        NotaDoGrupo(text = stringResource(R.string.about_data_local))
        // Piso EXIGIDO de altura: o botão de texto do Material desenha 40dp, e o alvo de toque que o
        // Compose expande sozinho esconderia isso de qualquer assert de toque. O eixo do desenho
        // pegou — quarta vez neste projeto (Fases 6, 07-03 e aqui). `requiredHeightIn` não negocia
        // com o pai; `heightIn` negociaria e voltaria a 40dp em tela apertada.
        TextButton(
            onClick = onPedirLimpeza,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = DestructiveMinTarget),
        ) {
            Text(
                text = stringResource(R.string.history_clear_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Uma janela de retenção.
 *
 * Estas são as ÚNICAS opções da tela sem descrição própria, e é deliberado. A duração inteira já está
 * dita no rótulo ("7 dias", "Até eu excluir"); a explicação do item vive uma vez, como nota do grupo,
 * logo abaixo das cinco. Repetir cinco parágrafos idênticos sob cinco durações seria enchimento, e
 * inventar um parágrafo diferente para cada duração seria pior: texto sem informação nova.
 *
 * A consequência destrutiva de "Não guardar" não é dita aqui de propósito — ela é o corpo do diálogo
 * de confirmação, que é onde a §9.2 a coloca. Dizê-la nos dois lugares criaria duas cópias da mesma
 * frase, e a cópia esquecida é sempre a que fica errada.
 */
@Composable
private fun RetencaoOpcao(
    state: SettingsUiState,
    politica: RetentionPolicy,
    labelRes: Int,
    onRetention: (RetentionPolicy) -> Unit,
) {
    OpcaoDePolitica(
        title = stringResource(labelRes),
        description = "",
        icon = if (politica == RetentionPolicy.NEVER_STORE) {
            Icons.Outlined.Delete
        } else {
            Icons.Outlined.Schedule
        },
        selected = state.settings.retentionPolicy == politica,
        onClick = { onRetention(politica) },
    )
}

/** Item 14 — o que fazer quando a consulta local falha, com o custo dos dois lados. */
@Composable
private fun GrupoDePoliticaDeFalha(state: SettingsUiState, onFallback: (FallbackPolicy) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_fallback_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.settings_fallback_allow),
                description = stringResource(R.string.settings_fallback_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.fallbackPolicy == FallbackPolicy.ALLOW,
                onClick = { onFallback(FallbackPolicy.ALLOW) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.settings_fallback_block),
                description = stringResource(R.string.settings_fallback_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.fallbackPolicy == FallbackPolicy.BLOCK,
                onClick = { onFallback(FallbackPolicy.BLOCK) },
            )
        }
    }
}

@Composable
private fun GrupoDePrivacidade(state: SettingsUiState, onMaskNumbersChange: (Boolean) -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_privacy_title)) {
        InfoBanner(
            text = stringResource(R.string.settings_privacy_disclaimer),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        SettingSwitchRow(
            label = stringResource(R.string.settings_mask_numbers),
            description = stringResource(R.string.settings_mask_numbers_desc),
            checked = state.settings.maskNumbers,
            onCheckedChange = onMaskNumbersChange,
        )
    }
}

/**
 * Item 15 — as quatro honestidades, pelos identificadores de recurso originais.
 *
 * Ver o KDoc de [SettingsScreen]: nenhuma destas frases é reescrita aqui.
 */
@Composable
private fun CartaoDeLimitacoes() {
    HonestyCard(
        title = stringResource(R.string.dialer_activation_unchanged_title),
        items = listOf(
            stringResource(R.string.dialer_activation_unchanged_1),
            stringResource(R.string.dialer_activation_unchanged_2),
            stringResource(R.string.dialer_activation_unchanged_3),
            stringResource(R.string.dialer_activation_unchanged_4),
        ),
        itemIcon = Icons.Outlined.Info,
    )
}

/**
 * Itens 9 e 16 — as duas linhas que levam para fora desta tela.
 *
 * **Item 9.** A linha navega para a tela de ativação do modo discador, que já existe desde a Fase 6
 * e não é reimplementada nem hospedada aqui: ela é DESTINO de navegação, e a assinatura dela não
 * muda. Em `UNAVAILABLE` a linha fica desabilitada com o motivo; em `BLOCKED_BY_CONTACTS` fica
 * HABILITADA de propósito, porque é a tela de destino que explica o pré-requisito da agenda — barrar
 * a entrada esconderia a explicação de que o usuário precisa.
 *
 * Ativar ou reverter o modo não tem confirmação própria: o seletor do sistema é a confirmação, e
 * esse é contrato da Fase 6.
 *
 * **Item 16.** O destino é da Phase 9. Nesta fase o item existe e navega; o envelope de navegação
 * aponta para um destino de espera que COMUNICA o estado — nunca para tela em branco.
 */
@Composable
private fun GrupoDeDestinos(
    state: SettingsUiState,
    onOpenDialerActivation: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.nav_settings)) {
        SettingsNavRow(
            label = stringResource(R.string.settings_dialer_mode),
            description = stringResource(R.string.settings_dialer_mode_desc),
            onClick = onOpenDialerActivation,
            enabled = state.dialerMode != DialerModeState.UNAVAILABLE,
            unavailableReason = stringResource(R.string.dialer_activation_unavailable)
                .takeIf { state.dialerMode == DialerModeState.UNAVAILABLE },
            valueText = when (state.dialerMode) {
                DialerModeState.ACTIVE -> stringResource(R.string.state_on)
                DialerModeState.ROLE_LOST -> stringResource(R.string.state_off)
                else -> null
            },
        )
        SettingsNavRow(
            label = stringResource(R.string.about_title),
            description = stringResource(R.string.about_data_local),
            onClick = onOpenAbout,
        )
    }
}

/**
 * Confirmação de perda de dado, uma das DUAS da tela: limpar o histórico.
 *
 * O corpo diz QUANTOS registros serão apagados — "apagar tudo" sem número é pedir consentimento no
 * escuro. O foco inicial vai no cancelar: numa ação irreversível, o gesto reflexo tem de cair na
 * saída segura.
 */
@Composable
private fun ConfirmacaoDeLimpeza(registros: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.history_clear_all)) },
        text = {
            Text(
                text = pluralStringResource(
                    R.plurals.settings_clear_history_confirm,
                    registros.toInt(),
                    registros.toInt(),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * A segunda e última confirmação: escolher não guardar registro nenhum.
 *
 * Confirma porque a escolha muda o comportamento futuro E poda o que já existe. Nenhuma das outras
 * quatro janelas de retenção apaga nada, e por isso nenhuma delas confirma.
 */
@Composable
private fun ConfirmacaoDeNaoGuardar(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_retention_never)) },
        text = { Text(text = stringResource(R.string.settings_retention_never_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(widthDp = 411, heightDp = 2400)
@Composable
private fun SettingsScreenPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        TelaDeExemplo(SettingsUiState(screeningRoleHeld = true, dialerMode = DialerModeState.OFFERED))
    }
}

@Preview(widthDp = 411, heightDp = 2400)
@Composable
private fun SettingsScreenProtectionOffPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        TelaDeExemplo(
            SettingsUiState(
                settings = ScreeningSettings(protectionEnabled = false),
                screeningRoleAvailable = true,
            ),
        )
    }
}

@Preview(widthDp = 411, heightDp = 2400)
@Composable
private fun SettingsScreenDialerUnavailablePreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        TelaDeExemplo(
            SettingsUiState(screeningRoleHeld = true, dialerMode = DialerModeState.UNAVAILABLE),
        )
    }
}

@Composable
private fun TelaDeExemplo(state: SettingsUiState) {
    SettingsScreen(
        state = state,
        hasWhatsApp = true,
        onBack = {},
        onProtectionChange = {},
        onFixRole = {},
        onUnknownPolicy = {},
        onContactsPolicy = {},
        onWhitelistPolicy = {},
        onBlockPrivateChange = {},
        onBlockMode = {},
        onHideNativeLogChange = {},
        onNotificationChange = {},
        onNotificationIdentification = {},
        onOpenDialerActivation = {},
        onRepeatedCallChange = {},
        onHistoryEnabledChange = {},
        onRetention = {},
        onClearHistory = {},
        onMaskNumbersChange = {},
        onFallback = {},
        onOpenAbout = {},
        bottomBar = {},
    )
}

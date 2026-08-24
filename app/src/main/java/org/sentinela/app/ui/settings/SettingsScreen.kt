package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.sentinela.app.R
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
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
        ConteudoDaProtecao(
            state = state,
            hasWhatsApp = hasWhatsApp,
            areaInterna = areaInterna,
            onProtectionChange = onProtectionChange,
            onFixRole = onFixRole,
            onUnknownPolicy = onUnknownPolicy,
            onContactsPolicy = onContactsPolicy,
            onWhitelistPolicy = onWhitelistPolicy,
            onBlockPrivateChange = onBlockPrivateChange,
            onBlockMode = onBlockMode,
            onHideNativeLogChange = onHideNativeLogChange,
            onNotificationChange = onNotificationChange,
            onNotificationIdentification = onNotificationIdentification,
            onRepeatedCallChange = onRepeatedCallChange,
            onHistoryEnabledChange = onHistoryEnabledChange,
            onRetention = onRetention,
            onMaskNumbersChange = onMaskNumbersChange,
            onFallback = onFallback,
            onOpenDialerActivation = onOpenDialerActivation,
            onOpenAbout = onOpenAbout,
            onPedirLimpeza = { confirmarLimpeza = true },
            onPedirNaoGuardar = { confirmarNaoGuardar = true },
            onAvisarHistoricoDesligado = { escopo.launch { avisos.showSnackbar(historicoDesligadoMantem) } },
        )
    }

    ConfirmacoesDaProtecao(
        pedindoLimpeza = confirmarLimpeza,
        pedindoNaoGuardar = confirmarNaoGuardar,
        registros = state.historyRecordCount,
        onFecharLimpeza = { confirmarLimpeza = false },
        onFecharNaoGuardar = { confirmarNaoGuardar = false },
        onLimpar = onClearHistory,
        onNaoGuardar = { onRetention(RetentionPolicy.NEVER_STORE) },
    )
}

/**
 * As dez secoes na ordem do contrato de interface, e nada alem disso.
 *
 * Esta composta e SEM ESTADO de proposito: os dois sinalizadores de confirmacao e o hospedeiro de
 * avisos temporarios ficam em [SettingsScreen], porque sao mecanismo de container, e chegam aqui
 * como as tres intencoes [onPedirLimpeza], [onPedirNaoGuardar] e [onAvisarHistoricoDesligado].
 *
 * As duas unicas traducoes de intencao que sobraram aqui sao as do grupo de historico, e as duas
 * dizem a mesma regra por caminhos diferentes: desligar o historico NAO apaga nada, entao avisa em
 * vez de confirmar; escolher nao guardar registro nenhum APAGA, entao pede confirmacao em vez de
 * gravar. Elas moram junto do grupo que as usa, e nao no container, para que a ordem "avisa" versus
 * "confirma" seja legivel ao lado da chamada que a produz.
 */
@Composable
private fun ConteudoDaProtecao(
    state: SettingsUiState,
    hasWhatsApp: Boolean,
    areaInterna: PaddingValues,
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
    onRepeatedCallChange: (Boolean) -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onRetention: (RetentionPolicy) -> Unit,
    onMaskNumbersChange: (Boolean) -> Unit,
    onFallback: (FallbackPolicy) -> Unit,
    onOpenDialerActivation: () -> Unit,
    onOpenAbout: () -> Unit,
    onPedirLimpeza: () -> Unit,
    onPedirNaoGuardar: () -> Unit,
    onAvisarHistoricoDesligado: () -> Unit,
) {
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
                if (!ligado) onAvisarHistoricoDesligado()
            },
            onRetention = { politica ->
                if (politica == RetentionPolicy.NEVER_STORE) onPedirNaoGuardar() else onRetention(politica)
            },
            onPedirLimpeza = onPedirLimpeza,
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

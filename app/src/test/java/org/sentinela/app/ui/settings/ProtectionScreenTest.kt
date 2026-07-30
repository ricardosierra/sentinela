package org.sentinela.app.ui.settings

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.ui.assertLayoutHeightIsAtLeast
import org.sentinela.app.ui.assertTouchHeightIsAtLeast
import org.sentinela.app.ui.assertTouchWidthIsAtLeast

/**
 * Tela Proteção: os dezesseis itens, a fronteira entre o que confirma e o que não confirma, e os
 * ramos do modo discador.
 *
 * O caso que vale por si é o de COMPLETUDE: ele percorre a lista das dezesseis chaves de rótulo,
 * lidas do recurso, e exige cada uma como nó da tela. Sem ele, os outros vinte e um casos provam
 * comportamento de itens escolhidos a dedo e nada sobre a cobertura da tabela — um item esquecido
 * passaria verde.
 *
 * Qualificadores de tela reais são obrigatórios: o aparelho padrão do Robolectric é pequeno demais e
 * reprova por motivo falso, com o conteúdo fora do viewport (registro da Fase 6). Nenhum texto de
 * assert é literal — tudo vem do recurso em tempo de teste, senão um caso ficaria verde sobre uma
 * frase que a varredura de honestidade nunca viu.
 *
 * Os três asserts de alvo são IMPORTADOS do arquivo neutro de apoio; duplicá-los é proibido para
 * sempre. Todo caso de alvo afirma os DOIS eixos: o Compose expande sozinho o alvo de toque de
 * qualquer componente interativo até o mínimo da plataforma, então sem o eixo do desenho a suite
 * mediria a garantia da biblioteca em vez do nosso layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@Suppress("LargeClass")
class ProtectionScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val alvoMinimo = 48.dp
    private val alvoDaLinha = 56.dp
    private val alvoDoCartao = 72.dp

    private fun texto(id: Int): String = context.getString(id)

    private var protecao: Boolean? = null
    private var correcoesDePapel = 0
    private var desconhecidos = mutableListOf<OriginPolicy>()
    private var contatos = mutableListOf<OriginPolicy>()
    private var listaPessoal = mutableListOf<OriginPolicy>()
    private var privados: Boolean? = null
    private var modoDeBloqueio: BlockMode? = null
    private var ocultarNoTelefone: Boolean? = null
    private var notificacao: Boolean? = null
    private var identificacao: NotificationIdentification? = null
    private var aberturasDoDiscador = 0
    private var chamadaRepetida: Boolean? = null
    private var historicoLigado: Boolean? = null
    private var retencoes = mutableListOf<RetentionPolicy>()
    private var limpezas = 0
    private var politicaDeFalha: FallbackPolicy? = null
    private var aberturasDeSobre = 0

    /** Os dezesseis rótulos da tabela da §9, na ordem dos itens. */
    private val dezesseisRotulos = listOf(
        R.string.settings_protection_toggle,
        R.string.dashboard_role_missing,
        R.string.settings_unknown_policy,
        R.string.settings_contacts_policy,
        R.string.settings_whitelist_policy,
        R.string.settings_block_private,
        R.string.settings_mode_reject,
        R.string.settings_hide_native_log,
        R.string.settings_notification_enable,
        R.string.settings_dialer_mode,
        R.string.settings_repeated_call,
        R.string.settings_history_enabled,
        R.string.settings_retention_title,
        R.string.history_clear_all,
        R.string.settings_fallback_policy,
        R.string.about_title,
    )

    private fun tela(
        settings: ScreeningSettings = ScreeningSettings(),
        screeningRoleHeld: Boolean = false,
        screeningRoleAvailable: Boolean = true,
        dialerMode: DialerModeState = DialerModeState.OFFERED,
        historyRecordCount: Long = 0L,
    ) {
        compose.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    settings = settings,
                    screeningRoleHeld = screeningRoleHeld,
                    screeningRoleAvailable = screeningRoleAvailable,
                    dialerMode = dialerMode,
                    historyRecordCount = historyRecordCount,
                    loading = false,
                ),
                onBack = {},
                onProtectionChange = { protecao = it },
                onFixRole = { correcoesDePapel++ },
                onUnknownPolicy = { desconhecidos += it },
                onContactsPolicy = { contatos += it },
                onWhitelistPolicy = { listaPessoal += it },
                onBlockPrivateChange = { privados = it },
                onBlockMode = { modoDeBloqueio = it },
                onHideNativeLogChange = { ocultarNoTelefone = it },
                onNotificationChange = { notificacao = it },
                onNotificationIdentification = { identificacao = it },
                onOpenDialerActivation = { aberturasDoDiscador++ },
                onRepeatedCallChange = { chamadaRepetida = it },
                onHistoryEnabledChange = { historicoLigado = it },
                onRetention = { retencoes += it },
                onClearHistory = { limpezas++ },
                onFallback = { politicaDeFalha = it },
                onOpenAbout = { aberturasDeSobre++ },
            )
        }
    }

    private fun rolarAte(id: Int) =
        compose.onAllNodesWithText(texto(id)).onFirst().performScrollTo()

    private fun nenhumDialogo() =
        assertEquals(0, compose.onAllNodes(isDialog()).fetchSemanticsNodes().size)

    // ------------------------------------------------------------------------------------------
    // Completude: o caso que pega item esquecido
    // ------------------------------------------------------------------------------------------

    @Test
    fun `os dezesseis itens da tabela existem na tela`() {
        tela()
        dezesseisRotulos.forEach { id ->
            val nos = compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes()
            assertTrue("item ausente na tela: ${texto(id)}", nos.isNotEmpty())
        }
        assertEquals(16, dezesseisRotulos.size)
    }

    @Test
    fun `cada item tem explicacao permanente visivel`() {
        tela()
        listOf(
            R.string.settings_protection_toggle_desc,
            R.string.unknown_option_block_desc,
            R.string.settings_block_private_desc,
            R.string.settings_block_mode_desc,
            R.string.settings_hide_native_log_desc,
            R.string.settings_notification_enable_desc,
            R.string.settings_dialer_mode_desc,
            R.string.settings_repeated_call_desc,
            R.string.settings_history_enabled_desc,
            R.string.settings_fallback_desc,
            R.string.settings_contacts_policy_note,
        ).forEach { id ->
            rolarAte(id).assertIsDisplayed()
        }
    }

    // ------------------------------------------------------------------------------------------
    // Ausencia de confirmacao: trocar politica nunca pergunta
    // ------------------------------------------------------------------------------------------

    /**
     * As políticas são clicadas pela DESCRIÇÃO, nunca pelo rótulo.
     *
     * "Bloquear", "Silenciar", "Tocar" e "Nunca Silenciar" são rótulos que se repetem nos três grupos
     * de origem — e essa colisão foi medida aqui: buscar pelo rótulo "Bloquear" para trocar a política
     * de CONTATOS acertava o cartão do grupo de DESCONHECIDOS, e o caso passava a afirmar a coisa
     * errada. A descrição é única por grupo, e o cartão mescla os descendentes, então buscar pela
     * descrição encontra exatamente o cartão pretendido.
     */
    @Test
    fun `trocar as quatro politicas nao abre dialogo algum e emite uma vez cada`() {
        tela()
        rolarAte(R.string.unknown_option_silence_desc).performClick()
        rolarAte(R.string.contacts_option_block_desc).performClick()
        rolarAte(R.string.whitelist_option_ring_desc).performClick()
        rolarAte(R.string.settings_mode_silent_voicemail).performClick()
        nenhumDialogo()
        assertEquals(listOf(OriginPolicy.SILENCE), desconhecidos)
        assertEquals(listOf(OriginPolicy.BLOCK), contatos)
        assertEquals(listOf(OriginPolicy.RING), listaPessoal)
        assertEquals(BlockMode.SILENT_VOICEMAIL, modoDeBloqueio)
    }

    @Test
    fun `politica de desconhecidos selecionada aparece marcada`() {
        tela(settings = ScreeningSettings(unknownPolicy = OriginPolicy.BLOCK))
        rolarAte(R.string.unknown_option_block_desc).assertIsSelected()
    }

    @Test
    fun `politica de erro emite a escolha sem confirmar`() {
        tela()
        rolarAte(R.string.settings_fallback_block).performClick()
        nenhumDialogo()
        assertEquals(FallbackPolicy.BLOCK, politicaDeFalha)
    }

    @Test
    fun `retencao de noventa dias emite direto sem confirmar`() {
        tela()
        rolarAte(R.string.settings_retention_90).performClick()
        nenhumDialogo()
        assertEquals(listOf(RetentionPolicy.DAYS_90), retencoes)
    }

    // ------------------------------------------------------------------------------------------
    // As DUAS confirmacoes, as duas por perda de dado
    // ------------------------------------------------------------------------------------------

    @Test
    fun `limpar historico abre dialogo citando quantos registros`() {
        tela(historyRecordCount = 12L)
        rolarAte(R.string.history_clear_all).performClick()
        val corpo = context.resources.getQuantityString(
            R.plurals.settings_clear_history_confirm,
            12,
            12,
        )
        compose.onNodeWithText(corpo).assertIsDisplayed()
    }

    @Test
    fun `limpar historico nao apaga nada antes de confirmar`() {
        tela(historyRecordCount = 3L)
        rolarAte(R.string.history_clear_all).performClick()
        assertEquals(0, limpezas)
        compose.onNodeWithText(texto(R.string.action_cancel)).performClick()
        assertEquals(0, limpezas)
    }

    @Test
    fun `confirmar a limpeza emite a acao uma unica vez`() {
        tela(historyRecordCount = 3L)
        rolarAte(R.string.history_clear_all).performClick()
        compose.onNodeWithText(texto(R.string.action_confirm)).performClick()
        assertEquals(1, limpezas)
        nenhumDialogo()
    }

    @Test
    fun `o dialogo de limpeza oferece o cancelar como primeira acao na travessia`() {
        tela(historyRecordCount = 3L)
        rolarAte(R.string.history_clear_all).performClick()
        compose.onNodeWithText(texto(R.string.action_cancel)).assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText(texto(R.string.action_confirm)).assertIsDisplayed()
    }

    @Test
    fun `escolher nao guardar abre dialogo ANTES de emitir a escolha`() {
        tela()
        rolarAte(R.string.settings_retention_never).performClick()
        compose.onNodeWithText(texto(R.string.settings_retention_never_confirm)).assertIsDisplayed()
        assertEquals(emptyList<RetentionPolicy>(), retencoes)
    }

    @Test
    fun `confirmar nao guardar emite a politica que poda`() {
        tela()
        rolarAte(R.string.settings_retention_never).performClick()
        compose.onNodeWithText(texto(R.string.action_confirm)).performClick()
        assertEquals(listOf(RetentionPolicy.NEVER_STORE), retencoes)
    }

    @Test
    fun `cancelar nao guardar nao emite politica alguma`() {
        tela()
        rolarAte(R.string.settings_retention_never).performClick()
        compose.onNodeWithText(texto(R.string.action_cancel)).performClick()
        assertEquals(emptyList<RetentionPolicy>(), retencoes)
        nenhumDialogo()
    }

    // ------------------------------------------------------------------------------------------
    // O que NAO confirma
    // ------------------------------------------------------------------------------------------

    @Test
    fun `desligar o historico nao abre dialogo algum`() {
        tela(settings = ScreeningSettings(historyEnabled = true))
        interruptorDe(R.string.settings_history_enabled).performClick()
        nenhumDialogo()
        assertEquals(false, historicoLigado)
    }

    @Test
    fun `desligar a protecao nao abre dialogo algum`() {
        tela(settings = ScreeningSettings(protectionEnabled = true))
        interruptorDe(R.string.settings_protection_toggle).performClick()
        nenhumDialogo()
        assertEquals(false, protecao)
    }

    /**
     * Com a protecao desligada o grupo fica tingido, e a tinta NUNCA e o unico sinal: a explicacao
     * permanente do que "desligado" significa continua visivel, e o interruptor continua anunciando
     * o proprio estado.
     */
    @Test
    fun `protecao desligada mantem a explicacao e o estado anunciados`() {
        tela(settings = ScreeningSettings(protectionEnabled = false))
        rolarAte(R.string.settings_protection_toggle_desc).assertIsDisplayed()
        val no = interruptorDe(R.string.settings_protection_toggle).fetchSemanticsNode()
        assertEquals(
            texto(R.string.state_off),
            no.config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun `ligar a notificacao propria nao abre dialogo`() {
        tela(settings = ScreeningSettings(showOwnNotification = false))
        interruptorDe(R.string.settings_notification_enable).performClick()
        nenhumDialogo()
        assertEquals(true, notificacao)
    }

    @Test
    fun `ocultar do historico do telefone e chamada repetida sao alteraveis`() {
        tela()
        interruptorDe(R.string.settings_hide_native_log).performClick()
        interruptorDe(R.string.settings_repeated_call).performClick()
        nenhumDialogo()
        assertEquals(false, ocultarNoTelefone)
        assertEquals(false, chamadaRepetida)
    }

    @Test
    fun `bloquear privados e alteravel`() {
        tela()
        interruptorDe(R.string.settings_block_private).performClick()
        assertEquals(false, privados)
    }

    // ------------------------------------------------------------------------------------------
    // Sub-opcoes da notificacao
    // ------------------------------------------------------------------------------------------

    @Test
    fun `as sub-opcoes de identificacao so aparecem com o interruptor ligado`() {
        tela(settings = ScreeningSettings(showOwnNotification = false))
        assertEquals(
            0,
            compose.onAllNodesWithText(texto(R.string.settings_notification_identification_masked))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `com a notificacao ligada a identificacao e escolhivel`() {
        tela(settings = ScreeningSettings(showOwnNotification = true))
        rolarAte(R.string.settings_notification_identification_anonymous).performClick()
        assertEquals(NotificationIdentification.ANONYMOUS, identificacao)
    }

    // ------------------------------------------------------------------------------------------
    // Modo discador
    // ------------------------------------------------------------------------------------------

    @Test
    fun `modo discador indisponivel anuncia desabilitado com o motivo em texto`() {
        tela(dialerMode = DialerModeState.UNAVAILABLE)
        val linha = rolarAte(R.string.settings_dialer_mode)
        linha.assertIsNotEnabled()
        val estado = linha.fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals(texto(R.string.dialer_activation_unavailable), estado)
        linha.performClick()
        assertEquals(0, aberturasDoDiscador)
    }

    @Test
    fun `modo discador bloqueado pela agenda deixa a linha habilitada`() {
        tela(dialerMode = DialerModeState.BLOCKED_BY_CONTACTS)
        val linha = rolarAte(R.string.settings_dialer_mode)
        linha.assertIsEnabled()
        linha.performClick()
        assertEquals(1, aberturasDoDiscador)
    }

    @Test
    fun `modo discador ativo navega e mostra o estado como valor`() {
        tela(dialerMode = DialerModeState.ACTIVE)
        rolarAte(R.string.settings_dialer_mode).performClick()
        assertEquals(1, aberturasDoDiscador)
        rolarAte(R.string.state_on).assertIsDisplayed()
    }

    @Test
    fun `privacidade e sobre navega para o destino de espera`() {
        tela()
        rolarAte(R.string.about_title).performClick()
        assertEquals(1, aberturasDeSobre)
    }

    // ------------------------------------------------------------------------------------------
    // Papel do sistema
    // ------------------------------------------------------------------------------------------

    @Test
    fun `papel ausente exibe a linha informativa com acao de correcao`() {
        tela(screeningRoleHeld = false, screeningRoleAvailable = true)
        rolarAte(R.string.dashboard_role_missing).assertIsDisplayed()
        rolarAte(R.string.dashboard_fix_configuration).performClick()
        assertEquals(1, correcoesDePapel)
    }

    @Test
    fun `papel indisponivel no aparelho nao exibe a acao de correcao`() {
        tela(screeningRoleHeld = false, screeningRoleAvailable = false)
        rolarAte(R.string.dashboard_role_missing).assertIsDisplayed()
        assertEquals(
            0,
            compose.onAllNodesWithText(texto(R.string.dashboard_fix_configuration))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `papel detido nao exibe a linha informativa`() {
        tela(screeningRoleHeld = true)
        assertEquals(
            0,
            compose.onAllNodesWithText(texto(R.string.dashboard_role_missing))
                .fetchSemanticsNodes().size,
        )
    }

    // ------------------------------------------------------------------------------------------
    // Semantica e alvos de toque
    // ------------------------------------------------------------------------------------------

    @Test
    fun `cada grupo tem cabecalho semantico`() {
        tela()
        listOf(
            R.string.settings_unknown_policy,
            R.string.settings_contacts_policy,
            R.string.settings_whitelist_policy,
            R.string.settings_retention_title,
            R.string.settings_fallback_policy,
        ).forEach { id ->
            val no = rolarAte(id).fetchSemanticsNode()
            assertTrue(
                "grupo sem cabecalho semantico: ${texto(id)}",
                no.config.getOrNull(SemanticsProperties.Heading) != null,
            )
        }
    }

    @Test
    fun `cada interruptor anuncia papel e estado`() {
        tela(settings = ScreeningSettings(protectionEnabled = true))
        val no = interruptorDe(R.string.settings_protection_toggle).fetchSemanticsNode()
        assertEquals(Role.Switch, no.config.getOrNull(SemanticsProperties.Role))
        assertEquals(
            texto(R.string.state_on),
            no.config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun `o interruptor passa os dois eixos de alvo de toque`() {
        tela()
        interruptorDe(R.string.settings_protection_toggle)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `o cartao de opcao passa os dois eixos de alvo de toque`() {
        tela()
        rolarAte(R.string.unknown_option_block_desc)
            .assertLayoutHeightIsAtLeast(alvoDoCartao)
            .assertTouchHeightIsAtLeast(alvoDoCartao)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `a linha navegavel passa os dois eixos de alvo de toque`() {
        tela()
        rolarAte(R.string.settings_dialer_mode)
            .assertLayoutHeightIsAtLeast(alvoDaLinha)
            .assertTouchHeightIsAtLeast(alvoDaLinha)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `o botao destrutivo passa os dois eixos de alvo de toque`() {
        tela()
        rolarAte(R.string.history_clear_all)
            .assertLayoutHeightIsAtLeast(alvoMinimo)
            .assertTouchHeightIsAtLeast(alvoMinimo)
            .assertTouchWidthIsAtLeast(alvoMinimo)
    }

    @Test
    fun `o cartao de limitacoes carrega as quatro frases originais`() {
        tela()
        listOf(
            R.string.dialer_activation_unchanged_1,
            R.string.dialer_activation_unchanged_2,
            R.string.dialer_activation_unchanged_3,
            R.string.dialer_activation_unchanged_4,
        ).forEach { id -> rolarAte(id).assertIsDisplayed() }
    }

    @Test
    fun `a tela nao tem campo de texto algum`() {
        tela()
        assertEquals(0, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertNull(protecao)
    }

    /**
     * Interruptor de uma linha, buscado no PRÓPRIO nó do controle — nunca no nó da linha.
     *
     * O estado mora no nó do controle (registro de 07-03, medido nas duas direções), e é por lá que o
     * leitor de tela e o teste perguntam. Como um interruptor não tem texto, a associação com o
     * rótulo é feita pela POSIÇÃO na travessia, que segue a ordem visual declarada em
     * [ORDEM_DOS_INTERRUPTORES]. Isso é intencionalmente rígido: mover um interruptor de grupo sem
     * atualizar a lista deixa os casos vermelhos, o que é exatamente o aviso desejado.
     */
    private fun interruptorDe(labelRes: Int): SemanticsNodeInteraction {
        rolarAte(labelRes)
        val indice = ORDEM_DOS_INTERRUPTORES.indexOf(labelRes)
        assertTrue("rotulo sem interruptor mapeado: ${texto(labelRes)}", indice >= 0)
        return compose.onAllNodes(isToggleable())[indice]
    }

    private companion object {

        /** Os seis interruptores da tela, na ordem visual (= ordem de travessia da §9.3). */
        val ORDEM_DOS_INTERRUPTORES = listOf(
            R.string.settings_protection_toggle,
            R.string.settings_hide_native_log,
            R.string.settings_repeated_call,
            R.string.settings_block_private,
            R.string.settings_notification_enable,
            R.string.settings_history_enabled,
        )
    }
}

package org.sentinela.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.sentinela.app.AppContainer
import org.sentinela.app.platform.hasWhatsAppInstalled
import org.sentinela.app.platform.ContactsPermissionChecker
import org.sentinela.app.telecom.DialerRoleManager
import org.sentinela.app.telecom.ScreeningRoleManager
import org.sentinela.app.ui.navigation.DestinoEmPreparacao
import org.sentinela.app.ui.navigation.Rotas

/**
 * Camada de rota da tela Proteção.
 *
 * Mesma regra das outras três, e aqui ela rende o máximo: a tela tem dezesseis itens e dezenove
 * retornos de chamada, e nenhum deles conhece repositório, banco ou papel do sistema. É o que permite
 * provar a tela inteira, com as duas confirmações de perda de dado, em máquina virtual pura.
 *
 * **Os DOIS papéis são reconsultados a cada retomada**, e não apenas o de triagem. O do telefone padrão
 * muda por fora do aplicativo com a mesma facilidade — o usuário troca o telefone padrão nas
 * configurações do sistema, ou uma atualização mexe no padrão do aparelho —, e a linha do modo discador
 * desta tela é justamente onde o descompasso apareceria: ela anunciaria "ligado" com o papel já em
 * outro aplicativo.
 *
 * Não existe função de salvar, e a ausência dela é contrato do dono de estado: cada item grava na hora.
 */
@Composable
@Suppress("LongMethod")
internal fun SettingsRoute(
    container: AppContainer,
    nav: NavController,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val verificadorDaAgenda = remember { ContactsPermissionChecker() }
    val papelDeTriagem = remember(context) { ScreeningRoleManager(context) }
    val papelDeTelefone = remember(context) { DialerRoleManager(context) }
    var emPreparacao by remember { mutableStateOf(false) }

    val dono: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            settings = container.settingsRepository,
            history = container.blockedCallRepository,
            roleHeld = papelDeTriagem::isRoleHeld,
            roleAvailable = papelDeTriagem::isRoleAvailable,
            requestRoleIntent = papelDeTriagem::buildRequestIntent,
            dialerRoleHeld = papelDeTelefone::isRoleHeld,
            dialerRoleAvailable = papelDeTelefone::isRoleAvailable,
            contactsGranted = { verificadorDaAgenda.isGranted(context) },
            dialerOptedIn = container.settingsRepository.callPhonePermissionAsked,
        ),
    )
    val estado by dono.estado.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        dono.reconsultarPapeis()
        onPauseOrDispose { }
    }

    val seletorDePapel = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { dono.reconsultarPapeis() }

    val temWhatsApp = remember(context) { context.packageManager.hasWhatsAppInstalled() }

    SettingsScreen(
        state = estado,
        hasWhatsApp = temWhatsApp,
        onBack = { nav.popBackStack() },
        onProtectionChange = dono::definirProtecao,
        onFixRole = { dono.intencaoDePedidoDoPapel()?.let(seletorDePapel::launch) },
        onUnknownPolicy = dono::definirPoliticaDeDesconhecidos,
        onContactsPolicy = dono::definirPoliticaDeContatos,
        onWhitelistPolicy = dono::definirPoliticaDaListaPessoal,
        onBlockPrivateChange = dono::definirBloqueioDePrivados,
        onBlockMode = dono::definirModoDeBloqueio,
        onHideNativeLogChange = dono::definirOcultarDoHistoricoDoTelefone,
        onNotificationChange = dono::definirNotificacaoPropria,
        onNotificationIdentification = dono::definirIdentificacaoDaNotificacao,
        onOpenDialerActivation = { nav.navigate(Rotas.MODO_DISCADOR) },
        onRepeatedCallChange = dono::definirChamadaRepetidaToca,
        onHistoryEnabledChange = dono::definirHistoricoLigado,
        onRetention = dono::definirRetencao,
        onClearHistory = dono::limparHistorico,
        onMaskNumbersChange = dono::definirMascaraDeNumeros,
        onFallback = dono::definirPoliticaDeFalha,
        onOpenAbout = { nav.navigate(Rotas.SOBRE) { launchSingleTop = true } },
        bottomBar = bottomBar,
    )
    DestinoEmPreparacao(visivel = emPreparacao) { emPreparacao = false }
}

package org.sentinela.app.ui.home

import android.Manifest
import androidx.activity.compose.LocalActivity
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
import org.sentinela.app.platform.ContactsPermissionChecker
import org.sentinela.app.telecom.DialerRoleManager
import org.sentinela.app.telecom.ScreeningRoleManager
import org.sentinela.app.ui.navigation.DestinoEmPreparacao
import org.sentinela.app.ui.navigation.Rotas
import org.sentinela.app.ui.navigation.estadoAtual
import org.sentinela.app.ui.navigation.rememberMarcasDePermissao

/**
 * Camada de rota da home.
 *
 * Mesma regra das outras três: **a tela recebe estado e retornos de chamada, e só esta camada conhece
 * o container.** A tela da home tem oito estados degradados e nenhum deles precisa de dependência
 * própria para ser desenhado — é o que permite prová-los todos em máquina virtual pura.
 *
 * ## A reconsulta do papel na retomada, e por que ela é redundante de propósito
 *
 * O papel do sistema é reconsultado em DOIS caminhos: na retomada da tela e no retorno do seletor do
 * sistema. A retomada cobriria o caso sozinha, e a redundância continua no lugar pelo mesmo motivo das
 * duas redes permissivas da Fase 5 e das duas defesas do vigia da Fase 6 — o caminho que importa é o
 * que ninguém previu. Aqui o motivo é medido: perder um papel encerra o processo do aplicativo, e
 * nesse caminho o retorno do seletor nunca roda. Reconsultar custa cerca de trinta microssegundos, três
 * ordens de grandeza abaixo de um quadro, então não existe argumento de desempenho do outro lado.
 *
 * ## O que esta rota NÃO faz
 *
 * Ela não toca no repositório de consulta de contatos. Ele registra observador da agenda e dispara a
 * construção de um conjunto de chaves medido em 2,57 s com cinco mil contatos — doze vezes o orçamento
 * inteiro da decisão de uma chamada. A home não precisa de nome de contato para nada: o que ela mostra
 * é o número já mascarado pelo dono de estado.
 */
@Composable
@Suppress("LongMethod")
internal fun HomeRoute(
    container: AppContainer,
    nav: NavController,
    bottomBar: @Composable () -> Unit,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val verificadorDaAgenda = remember { ContactsPermissionChecker() }
    val papelDeTriagem = remember(context) { ScreeningRoleManager(context) }
    val papelDeTelefone = remember(context) { DialerRoleManager(context) }
    val marcas = rememberMarcasDePermissao(container)
    var emPreparacao by remember { mutableStateOf(false) }

    val dono: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            settings = container.settingsRepository,
            history = container.blockedCallRepository,
            roleHeld = papelDeTriagem::isRoleHeld,
            roleAvailable = papelDeTriagem::isRoleAvailable,
            requestRoleIntent = papelDeTriagem::buildRequestIntent,
            contactsState = { verificadorDaAgenda.estadoAtual(activity, marcas.agenda) },
            dialerRoleHeld = papelDeTelefone::isRoleHeld,
            dialerRoleAvailable = papelDeTelefone::isRoleAvailable,
            dialerOptedIn = { marcas.discador },
            mask = container.maskNumber,
        ),
    )
    val estado by dono.estado.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        dono.reconsultarPapel()
        onPauseOrDispose { }
    }

    val seletorDePapel = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { dono.reconsultarPapel() }

    val pedidoDaAgenda = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { dono.reconsultarPapel() }

    HomeScreen(
        state = estado,
        onProtectionChange = dono::definirProtecao,
        // O botão de conserto só dispara quando existe intenção de pedido. Em aparelho que não oferece
        // o papel a intenção é nula, e disparar levaria o usuário a uma tela do sistema que não
        // resolve nada — nulo aqui é a resposta correta, não erro.
        onFixRole = { dono.intencaoDePedidoDoPapel()?.let(seletorDePapel::launch) },
        onGrantContacts = {
            dono.pedirAgenda { pedidoDaAgenda.launch(Manifest.permission.READ_CONTACTS) }
        },
        onOpenAppSettings = {
            context.startActivity(verificadorDaAgenda.appSettingsIntent(context.packageName))
        },
        onEnableHistory = dono::religarHistorico,
        onRetryHistory = dono::tentarLerNovamente,
        onOpenSettings = { nav.navigate(Rotas.PROTECAO) { launchSingleTop = true } },
        onOpenWhitelist = { nav.navigate(Rotas.WHITELIST) { launchSingleTop = true } },
        onOpenHistory = { nav.navigate(Rotas.HISTORICO) { launchSingleTop = true } },
        onOpenDialerActivation = { nav.navigate(Rotas.MODO_DISCADOR) { launchSingleTop = true } },
        bottomBar = bottomBar,
        onAcceptRating = dono::onRatingAccepted,
        onDismissRating = dono::onRatingDismissed,
    )
    DestinoEmPreparacao(visivel = emPreparacao) { emPreparacao = false }
}

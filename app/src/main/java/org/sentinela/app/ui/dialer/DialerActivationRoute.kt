package org.sentinela.app.ui.dialer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.sentinela.app.AppContainer
import org.sentinela.app.platform.ContactsPermissionChecker
import org.sentinela.app.telecom.DialerRoleManager
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.telecom.call.dialerModeState
import org.sentinela.app.ui.navigation.rememberMarcasDePermissao

/**
 * Camada de rota da ativação do modo discador.
 *
 * Ela fecha a pendência registrada em 06-05: a tela de ativação existia, com os cinco ramos de estado
 * provados, e **não tinha ponto de entrada nenhum** no aplicativo. Agora ela é destino do grafo,
 * alcançável pela linha do modo discador da tela Proteção e pelo aviso de papel perdido da home.
 *
 * A composta de tela NÃO é tocada por este trabalho: a assinatura dela é fechada, e os quatro retornos
 * de chamada dela são exatamente o que esta camada liga.
 *
 * ## Reverter é abrir a tela de escolha do sistema
 *
 * Não existe API pública para um aplicativo remover o próprio papel — a que existe é de uso do sistema
 * e exige permissão proibida neste projeto. E há uma proibição permanente, que não é questão de estilo:
 * **nunca** desligar o modo desabilitando componente próprio pelo gerenciador de pacotes. A plataforma
 * verifica os requisitos do papel continuamente; deixar de cumpri-los faz o sistema remover o papel **e
 * encerrar o aplicativo**, com o usuário segurando o aparelho. É checagem 8.2 do script de invariantes.
 *
 * ## Por que o estado é derivado aqui, sem dono de estado próprio
 *
 * O estado do modo vem da função pura de precedência da Fase 6, com nove casos travados por teste, e a
 * tela não precisa de mais nada: quatro sinais entram, um estado sai. Um dono de estado só para
 * repassá-los acrescentaria uma camada sem regra. Os quatro sinais são reconsultados a cada retomada,
 * porque o papel muda por fora do aplicativo e a plataforma não avisa aplicativo comum.
 */
@Composable
internal fun DialerActivationRoute(container: AppContainer, nav: NavController) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val verificadorDaAgenda = remember { ContactsPermissionChecker() }
    val papelDeTelefone = remember(context) { DialerRoleManager(context) }
    val marcas = rememberMarcasDePermissao(container)

    var estado by remember { mutableStateOf(DialerModeState.UNAVAILABLE) }
    val reconsultar = {
        estado = dialerModeState(
            roleAvailable = papelDeTelefone.isRoleAvailable(),
            roleHeld = papelDeTelefone.isRoleHeld(),
            contactsGranted = verificadorDaAgenda.isGranted(context),
            userOptedIn = marcas.discador,
        )
    }

    LifecycleResumeEffect(Unit) {
        reconsultar()
        onPauseOrDispose { }
    }

    val seletorDePapel = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { reconsultar() }

    val pedidoDaAgenda = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { reconsultar() }

    DialerActivationScreen(
        state = estado,
        onRequestRole = { papelDeTelefone.buildRequestIntent()?.let(seletorDePapel::launch) },
        onRevert = { seletorDePapel.launch(papelDeTelefone.buildRevertIntent()) },
        // Marca gravada ao DISPARAR, jamais no retorno: o usuário pode encerrar o aplicativo com o
        // diálogo do sistema aberto, e a marca é a única coisa que separa "nunca perguntamos" de
        // "negaram de vez". Mesma ordem das Fases 4, 5 e 6.
        onGrantContacts = {
            escopo.launch { container.settingsRepository.markContactsPermissionAsked() }
            pedidoDaAgenda.launch(Manifest.permission.READ_CONTACTS)
        },
        onBack = { nav.popBackStack() },
    )
}

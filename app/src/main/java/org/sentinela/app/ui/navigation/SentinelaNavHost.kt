package org.sentinela.app.ui.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.sentinela.app.AppContainer
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.platform.ContactsPermissionChecker
import org.sentinela.app.platform.NotificationPermissionChecker
import org.sentinela.app.ui.dialer.DialerActivationRoute
import org.sentinela.app.ui.home.HomeRoute
import org.sentinela.app.ui.onboarding.OnboardingRoute
import org.sentinela.app.ui.onboarding.WelcomeRoute
import org.sentinela.app.ui.onboarding.rememberStepTransitionMillis
import org.sentinela.app.ui.settings.SettingsRoute
import org.sentinela.app.ui.about.AboutRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.navigation.compose.currentBackStackEntryAsState
import org.sentinela.app.ui.components.BottomBarItem
import org.sentinela.app.ui.components.SentinelaBottomBar
import org.sentinela.app.ui.history.HistoryRoute
import org.sentinela.app.ui.whitelist.WhitelistRoute
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Os seis passos do onboarding na ordem em que o usuário os percorre.
 *
 * A posição na lista **é** o número do passo, e a lista é o que liga cada destino ao seguinte. A
 * consequência de desenho é o ponto central desta fiação: **a pilha de navegação é a única fonte da
 * verdade do passo corrente.** O dono de estado do onboarding também sabe contar passos, mas quem
 * responde ao gesto de voltar do sistema é a pilha — manter duas contagens vivas produziria a tela de
 * um passo com o cabeçalho de outro no primeiro descompasso entre elas.
 */
internal val PASSOS_DO_ONBOARDING = listOf(
    Rotas.PASSO_PAPEL,
    Rotas.PASSO_DESCONHECIDOS,
    Rotas.PASSO_CONTATOS,
    Rotas.PASSO_WHITELIST,
    Rotas.PASSO_NOTIFICACAO,
    Rotas.PASSO_RESUMO,
)

private typealias Entrada = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?
private typealias Saida = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?

/**
 * Grafo de navegação do aplicativo, com os dez destinos da fase.
 *
 * Recebe o destino inicial JÁ DECIDIDO, e isso não é preferência de estilo: trocar o destino inicial
 * depois de o grafo estar composto **não re-navega** — a recomposição encontra o grafo montado e não
 * refaz a pilha. Quem resolve a pergunta "primeira abertura ou não" é a hospedeira, antes de compor.
 *
 * Todas as rotas são TEXTO, vindas das constantes. A alternativa tipada da biblioteca de navegação
 * compila limpa neste repositório e estoura na primeira composição do grafo, por falta do complemento
 * de serialização no compilador embutido na ferramenta de build — reproduzido duas vezes aqui, e é
 * por isso que o guarda-corpo dessa decisão é um teste que COMPÕE o grafo, nunca um assert de
 * compilação.
 *
 * Os dez destinos estão escritos um por um, sem laço, de propósito: a contagem de destinos é ponto de
 * revisão de navegação nesta base de código, e um laço a esconderia de quem lê o arquivo e de quem o
 * verifica de fora.
 *
 * A transição entre passos do onboarding vive aqui, como animação de destino, e não dentro das telas:
 * a tela não sabe de onde o usuário veio nem para onde vai. A duração vem de
 * [rememberStepTransitionMillis], que devolve zero quando a redução de movimento está ligada nas
 * configurações de acessibilidade do aparelho — supressão da animação, e não animação mais curta.
 */
@Composable
@Suppress("LongMethod")
internal fun SentinelaNavHost(
    container: AppContainer,
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    /** Registro do histórico que veio do toque na notificação, quando houver. */
    registroEmDestaque: Long? = null,
) {
    val duracao = rememberStepTransitionMillis()
    val entra: Entrada = { entradaDePasso(duracao) }
    val sai: Saida = { saidaDePasso(duracao) }
    val entraDeVolta: Entrada = { entradaDeVolta(duracao) }
    val saiDeVolta: Saida = { saidaDeVolta(duracao) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBar: @Composable () -> Unit = {
        SentinelaBottomBar(
            items = listOf(
                BottomBarItem(
                    label = stringResource(R.string.nav_home),
                    icon = Icons.Filled.Home,
                    selected = currentRoute == Rotas.HOME,
                    onClick = { 
                        if (currentRoute != Rotas.HOME) {
                            navController.navigate(Rotas.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_whitelist),
                    icon = Icons.Outlined.VerifiedUser,
                    selected = currentRoute == Rotas.WHITELIST,
                    onClick = { 
                        if (currentRoute != Rotas.WHITELIST) {
                            navController.navigate(Rotas.WHITELIST) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_history),
                    icon = Icons.Outlined.History,
                    selected = currentRoute == Rotas.HISTORICO,
                    onClick = { 
                        if (currentRoute != Rotas.HISTORICO) {
                            navController.navigate(Rotas.HISTORICO) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                ),
                BottomBarItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Outlined.Settings,
                    selected = currentRoute == Rotas.PROTECAO,
                    onClick = { 
                        if (currentRoute != Rotas.PROTECAO) {
                            navController.navigate(Rotas.PROTECAO) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                ),
            )
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Rotas.BOAS_VINDAS) {
            WelcomeRoute(nav = navController)
        }
        composable(
            route = Rotas.PASSO_PAPEL,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 0)
        }
        composable(
            route = Rotas.PASSO_DESCONHECIDOS,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 1)
        }
        composable(
            route = Rotas.PASSO_CONTATOS,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 2)
        }
        composable(
            route = Rotas.PASSO_WHITELIST,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 3)
        }
        composable(
            route = Rotas.PASSO_NOTIFICACAO,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 4)
        }
        composable(
            route = Rotas.PASSO_RESUMO,
            enterTransition = entra,
            exitTransition = sai,
            popEnterTransition = entraDeVolta,
            popExitTransition = saiDeVolta,
        ) {
            OnboardingRoute(container = container, nav = navController, passo = 5)
        }
        composable(Rotas.HOME) {
            HomeRoute(container = container, nav = navController, bottomBar = bottomBar)
        }
        composable(Rotas.PROTECAO) {
            SettingsRoute(container = container, nav = navController, bottomBar = bottomBar)
        }
        composable(Rotas.WHITELIST) {
            WhitelistRoute(container = container, nav = navController, bottomBar = bottomBar)
        }
        composable(Rotas.HISTORICO) {
            HistoryRoute(
                container = container,
                bottomBar = bottomBar,
                registroEmDestaque = registroEmDestaque,
            )
        }
        composable(Rotas.MODO_DISCADOR) {
            DialerActivationRoute(container = container, nav = navController)
        }
        composable(Rotas.SOBRE) {
            AboutRoute(container = container, nav = navController)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// A transição de passo: deslizamento horizontal mais dissolução, espelhada no retorno.
// ------------------------------------------------------------------------------------------------

private fun AnimatedContentTransitionScope<NavBackStackEntry>.entradaDePasso(
    duracao: Int,
): EnterTransition =
    slideInHorizontally(animationSpec = tween(duracao)) { largura -> largura } +
        fadeIn(animationSpec = tween(duracao))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.saidaDePasso(
    duracao: Int,
): ExitTransition =
    slideOutHorizontally(animationSpec = tween(duracao)) { largura -> -largura } +
        fadeOut(animationSpec = tween(duracao))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.entradaDeVolta(
    duracao: Int,
): EnterTransition =
    slideInHorizontally(animationSpec = tween(duracao)) { largura -> -largura } +
        fadeIn(animationSpec = tween(duracao))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.saidaDeVolta(
    duracao: Int,
): ExitTransition =
    slideOutHorizontally(animationSpec = tween(duracao)) { largura -> largura } +
        fadeOut(animationSpec = tween(duracao))

// ------------------------------------------------------------------------------------------------
// Fiação compartilhada pelas quatro camadas de rota.
// ------------------------------------------------------------------------------------------------

/**
 * Vai para a home descartando o onboarding INTEIRO, com entrada única.
 *
 * O descarte inclusivo é o que impede o gesto de voltar de devolver o usuário ao onboarding — o
 * retorno que o produto proíbe, e que tem teste com dentes desde o contrato do grafo.
 */
internal fun NavController.irParaHome() {
    navigate(Rotas.HOME) {
        popUpTo(Rotas.BOAS_VINDAS) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Aviso de destino que ainda não existe: as listas de permitidos e de histórico chegam na fase
 * seguinte, e a tela de privacidade e sobre na fase depois dela.
 *
 * Ele existe porque a alternativa seria pior de duas maneiras ao mesmo tempo. Um destino registrado
 * com corpo vazio daria tela em branco, que o contrato de interface proíbe; um controle que não faz
 * nada ao ser tocado é um defeito silencioso, e o usuário conclui que o aplicativo travou. Aqui o
 * aplicativo diz o que sabe, com a frase que já existe em recurso para exatamente este caso, e o
 * usuário volta ao que estava fazendo.
 *
 * Os atalhos das duas listas na home já nascem anunciados como indisponíveis desde a tela; este aviso
 * é a rede da linha de privacidade e sobre, que fica ativa na tela Proteção.
 */
@Composable
internal fun DestinoEmPreparacao(visivel: Boolean, onDismiss: () -> Unit) {
    if (!visivel) return
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(R.string.nav_unavailable)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_back))
            }
        },
    )
}

/**
 * As marcas persistidas de "já perguntei", lidas de forma contínua e NUNCA bloqueante.
 *
 * Elas existem porque a consulta de justificativa da plataforma responde negativo nos dois extremos
 * — antes do primeiro pedido e depois da negação definitiva —, e sem a marca gravada em disco os dois
 * estados seriam indistinguíveis. A leitura mora aqui, na camada de rota, porque o dono de estado
 * recebe o estado da permissão como função e jamais toca na plataforma.
 *
 * Nada aqui espera disco: enquanto a primeira leitura não chega, o valor é o conservador (nunca
 * perguntamos), que no pior caso mostra o pedido normal em vez do atalho para as Configurações.
 */
internal class MarcasDePermissao {

    var agenda by mutableStateOf(false)

    var aviso by mutableStateOf(false)

    /**
     * Marca de que este aplicativo já foi telefone padrão alguma vez nesta instalação.
     *
     * É derivada da marca do pedido da permissão de originar chamada, e a derivação é honesta pelo
     * caminho de código: essa permissão só é pedida no toque em ligar da tela de discagem própria, e
     * o sistema só encaminha a ação de discagem a este aplicativo quando ele detém o papel de
     * telefone padrão. Verdadeiro aqui significa, portanto, "o usuário já usou o modo discador" — que
     * é exatamente a intenção gravada de que a função de precedência do modo precisa para distinguir
     * papel PERDIDO de modo apenas OFERECIDO.
     *
     * Chave dedicada seria mais direta e está registrada como item adiado. Ela não entra agora porque
     * o estado do modo discador é derivado por decisão da Fase 6: uma marca persistida dizendo "modo
     * ligado" vira mentira no instante em que o usuário troca o telefone padrão nas configurações do
     * sistema, e a partir dali o aplicativo mostraria uma tela que não corresponde ao aparelho.
     */
    var discador by mutableStateOf(false)
}

@Composable
internal fun rememberMarcasDePermissao(container: AppContainer): MarcasDePermissao {
    val marcas = remember { MarcasDePermissao() }
    LaunchedEffect(container) {
        val repositorio = container.settingsRepository
        launch { repositorio.contactsPermissionAsked.collect { marcas.agenda = it } }
        launch { repositorio.notificationPermissionAsked.collect { marcas.aviso = it } }
        launch { repositorio.callPhonePermissionAsked.collect { marcas.discador = it } }
    }
    return marcas
}

/**
 * Estado da permissão de leitura da agenda. Exige `Activity`, e não `Context`, porque a consulta de
 * justificativa da plataforma só existe em `Activity` — motivo pelo qual esta leitura vive na camada
 * de rota e desce para o dono de estado como função já fechada sobre a hospedeira.
 *
 * Sem `Activity` — caso que não acontece em produção, mas acontece em pré-visualização — a resposta é
 * a conservadora: nunca perguntamos.
 */
internal fun ContactsPermissionChecker.estadoAtual(
    activity: Activity?,
    jaPerguntou: Boolean,
): ContactsPermissionState =
    activity?.let { state(it, jaPerguntou) } ?: ContactsPermissionState.NEVER_ASKED

/** Espelho de [estadoAtual] para a permissão do aviso próprio, com o mesmo argumento. */
internal fun NotificationPermissionChecker.estadoAtual(
    activity: Activity?,
    jaPerguntou: Boolean,
): RuntimePermissionAsk =
    activity?.let { state(it, jaPerguntou) } ?: RuntimePermissionAsk.NEVER_ASKED

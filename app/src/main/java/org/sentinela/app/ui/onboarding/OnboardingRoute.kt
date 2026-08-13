package org.sentinela.app.ui.onboarding

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
import org.sentinela.app.platform.hasWhatsAppInstalled
import org.sentinela.app.AppContainer
import org.sentinela.app.platform.ContactsPermissionChecker
import org.sentinela.app.platform.NotificationPermissionChecker
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.ScreeningRoleManager
import org.sentinela.app.ui.navigation.DestinoEmPreparacao
import org.sentinela.app.ui.navigation.PASSOS_DO_ONBOARDING
import org.sentinela.app.ui.navigation.Rotas
import org.sentinela.app.ui.navigation.estadoAtual
import org.sentinela.app.ui.navigation.irParaHome
import org.sentinela.app.ui.navigation.rememberMarcasDePermissao

/**
 * Tela de boas-vindas, destino zero do fluxo.
 *
 * Não recebe o container porque não precisa dele: nada nesta tela consulta configuração, permissão ou
 * papel do sistema. Ela é a única camada de rota da fase sem dono de estado.
 */
@Composable
internal fun WelcomeRoute(nav: NavController) {
    WelcomeScreen(
        onStart = { nav.navigate(Rotas.PASSO_PAPEL) },
        // A tela Sobre existe desde a Fase 9 e já é alcançável pela tela Proteção. Este ponto
        // continuava no aviso genérico de "em preparação", deixando um beco sem saída logo na
        // primeira tela do app para um destino que funciona.
        onAbout = { nav.navigate(Rotas.SOBRE) },
    )
}

/**
 * Camada de rota dos seis passos do onboarding.
 *
 * **Ela é fina de propósito, e a regra que a mantém fina é o que torna a fase inteira testável em
 * máquina virtual pura: a composta de TELA recebe estado e retornos de chamada, e nunca o container.
 * Só esta camada conhece o container.** A tela de chamada e a de ativação do modo discador já seguem o
 * mesmo padrão desde a Fase 6, e a Fase 5 mediu que um segundo container no mesmo processo derruba a
 * aplicação — o que faz de "a tela não constrói dependência" um contrato de execução, não um gosto.
 *
 * O passo chega como parâmetro, vindo do destino, e não do dono de estado. A pilha de navegação é a
 * fonte da verdade do passo porque é ela quem responde ao gesto de voltar do sistema; o contador do
 * dono de estado continuaria existindo em paralelo e divergiria no primeiro retorno.
 *
 * Três leituras da plataforma moram aqui, e nenhuma delas pode descer para o dono de estado:
 *
 * - o estado das duas permissões, porque a consulta de justificativa da plataforma só existe em
 *   `Activity`;
 * - o disparo dos diálogos do sistema, que exige o registro de resultado da composição;
 * - o seletor de papel, **cujo retorno apenas reconsulta**. O código de resultado nunca é a fonte da
 *   verdade: perder um papel encerra o processo do aplicativo (medido três vezes na Fase 6), e nesse
 *   caminho — o que mais importa — o retorno simplesmente não roda.
 *
 * Nenhum passo é bloqueante. Papel negado, agenda negada e aviso negado seguem para o passo seguinte:
 * negar custa a capacidade correspondente, nunca o resto do onboarding.
 */
@Composable
internal fun OnboardingRoute(container: AppContainer, nav: NavController, passo: Int) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val verificadorDaAgenda = remember { ContactsPermissionChecker() }
    val verificadorDoAviso = remember { NotificationPermissionChecker() }
    val papelDeTriagem = remember(context) { ScreeningRoleManager(context) }
    val marcas = rememberMarcasDePermissao(container)

    val dono: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(
            settings = container.settingsRepository,
            roleHeld = papelDeTriagem::isRoleHeld,
            roleAvailable = papelDeTriagem::isRoleAvailable,
            requestRoleIntent = papelDeTriagem::buildRequestIntent,
            contactsState = { verificadorDaAgenda.estadoAtual(activity, marcas.agenda) },
            notificationState = { verificadorDoAviso.estadoAtual(activity, marcas.aviso) },
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
    ) { dono.reconsultarPermissoes() }

    val pedidoDoAviso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { dono.reconsultarPermissoes() }

    AcoesDoPasso(
        pedirPapel = { dono.intencaoDePedidoDoPapel()?.let(seletorDePapel::launch) },
        pedirAgenda = {
            dono.pedirAgenda { pedidoDaAgenda.launch(Manifest.permission.READ_CONTACTS) }
        },
        abrirConfiguracoes = {
            context.startActivity(verificadorDaAgenda.appSettingsIntent(context.packageName))
        },
        gravar = dono::gravarConfiguracao,
        alternarAviso = { ligado ->
            aderirAoAviso(dono, ligado) {
                pedidoDoAviso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        avancar = { nav.navigate(PASSOS_DO_ONBOARDING[(passo + 1).coerceAtMost(ULTIMO_PASSO)]) },
        voltar = { nav.popBackStack() },
        pular = { dono.pular(); nav.irParaHome() },
        concluir = { dono.concluir(); nav.irParaHome() },
    ).let { acoes ->
        val temWhatsApp = remember(context) { context.packageManager.hasWhatsAppInstalled() }
        PassoDoOnboarding(passo = passo, estado = estado, acoes = acoes, temWhatsApp = temWhatsApp)
    }
}

/**
 * As nove intenções que um passo do onboarding pode emitir, reunidas para poder atravessar a fronteira
 * entre a fiação e o desvio de passo em um único objeto.
 *
 * A razão de existir é que ela separa o QUE cada passo faz do COMO isso é executado, e é o que permite
 * ao teste de fluxo compor o desvio REAL de produção — com navegação de verdade e sem container.
 */
@Suppress("LongParameterList")
internal class AcoesDoPasso(
    val pedirPapel: () -> Unit,
    val pedirAgenda: () -> Unit,
    val abrirConfiguracoes: () -> Unit,
    val gravar: ((ScreeningSettings) -> ScreeningSettings) -> Unit,
    val alternarAviso: (Boolean) -> Unit,
    val avancar: () -> Unit,
    val voltar: () -> Unit,
    val pular: () -> Unit,
    val concluir: () -> Unit,
)

/**
 * Desvio de passo: recebe o passo e o estado, e desenha uma das seis compostas puras que já existem.
 *
 * Nenhuma delas conhece navegação, container ou plataforma — o que entra é estado, o que sai é intenção.
 */
@Composable
@Suppress("LongMethod")
internal fun PassoDoOnboarding(passo: Int, estado: OnboardingUiState, acoes: AcoesDoPasso, temWhatsApp: Boolean) {
    val config = estado.settings
    when (passo) {
        PASSO_DO_PAPEL -> RoleStepScreen(
            state = estado,
            onRequestRole = acoes.pedirPapel,
            onNext = acoes.avancar,
            onSkip = acoes.pular,
        )

        PASSO_DOS_DESCONHECIDOS -> UnknownPolicyStepScreen(
            selected = config.unknownPolicy,
            onSelect = { politica -> acoes.gravar { it.copy(unknownPolicy = politica) } },
            onNext = acoes.avancar,
            onSkip = acoes.pular,
        )

        PASSO_DOS_CONTATOS -> ContactsPolicyStepScreen(
            permission = estado.contactsPermission,
            selected = config.contactsPolicy,
            blockPrivate = config.blockPrivateNumbers,
            hasWhatsApp = temWhatsApp,
            onSelect = { politica -> acoes.gravar { it.copy(contactsPolicy = politica) } },
            onBlockPrivateChange = { bloquear ->
                acoes.gravar { it.copy(blockPrivateNumbers = bloquear) }
            },
            onGrantContacts = acoes.pedirAgenda,
            onOpenAppSettings = acoes.abrirConfiguracoes,
            onNext = acoes.avancar,
            onSkip = acoes.pular,
        )

        PASSO_DA_WHITELIST -> WhitelistPolicyStepScreen(
            selected = config.whitelistPolicy,
            onSelect = { politica -> acoes.gravar { it.copy(whitelistPolicy = politica) } },
            onNext = acoes.avancar,
            onBack = acoes.voltar,
            onSkip = acoes.pular,
        )

        PASSO_DO_AVISO -> NotificationStepScreen(
            enabled = config.showOwnNotification,
            identification = config.notificationIdentification,
            permission = estado.notificationPermission,
            onEnabledChange = acoes.alternarAviso,
            onIdentificationChange = { identificacao ->
                acoes.gravar { it.copy(notificationIdentification = identificacao) }
            },
            onNext = acoes.avancar,
            onSkip = acoes.pular,
        )

        else -> SummaryStepScreen(
            roleHeld = estado.screeningRoleHeld,
            contactsPermission = estado.contactsPermission,
            unknownPolicy = config.unknownPolicy,
            contactsPolicy = config.contactsPolicy,
            whitelistPolicy = config.whitelistPolicy,
            onFixRole = acoes.pedirPapel,
            onGrantContacts = acoes.pedirAgenda,
            onFinish = acoes.concluir,
        )
    }
}

/**
 * Adesão ao aviso próprio de bloqueio.
 *
 * A configuração é gravada nos dois sentidos, e o diálogo do sistema só é disparado quando o usuário
 * LIGA a opção — pedir permissão de aviso a quem acabou de desligar o aviso seria pedir por nada. A
 * marca de "já perguntei" é gravada pelo dono de estado no instante do disparo, jamais no retorno.
 */
private fun aderirAoAviso(
    dono: OnboardingViewModel,
    ligado: Boolean,
    dispararPedido: () -> Unit,
) {
    dono.gravarConfiguracao { it.copy(showOwnNotification = ligado) }
    if (ligado) dono.pedirNotificacao(dispararPedido)
}

/**
 * Identificação dos passos. Constantes com nome em vez de números soltos no desvio: o número do passo
 * aparece em três lugares desta fiação (destino, lista de rotas e desvio), e um deles errado produz a
 * tela de um passo dentro do destino de outro.
 */
private const val PASSO_DO_PAPEL = 0
private const val PASSO_DOS_DESCONHECIDOS = 1
private const val PASSO_DOS_CONTATOS = 2
private const val PASSO_DA_WHITELIST = 3
private const val PASSO_DO_AVISO = 4
private const val ULTIMO_PASSO = TOTAL_DE_PASSOS - 1

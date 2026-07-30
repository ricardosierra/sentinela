package org.sentinela.app.ui.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.ScreeningSettings

/**
 * Dono de estado do onboarding.
 *
 * Colaboradores por parâmetro, container nunca — mesmo argumento do dono de estado da home.
 *
 * O passo corrente é estado deste objeto, e isso já cobre rotação de tela e a recriação de Activity
 * provocada pelo diálogo do sistema, que são os dois casos reais desta fase. O apoio de estado salvo
 * fica FORA por decisão deste plano: cobrir morte de processo antes de existir um caso concreto de
 * perda de progresso é escopo novo, e a pesquisa da fase recomendou começar sem.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class OnboardingViewModel(
    private val settings: DataStoreSettingsRepository,
    private val roleHeld: () -> Boolean,
    private val roleAvailable: () -> Boolean,
    private val requestRoleIntent: () -> Intent?,
    private val contactsState: () -> ContactsPermissionState,
    private val notificationState: () -> RuntimePermissionAsk,
) : ViewModel() {

    private val passo = MutableStateFlow(0)
    private val pedidoEmCurso = MutableStateFlow(false)
    private val papelNegado = MutableStateFlow(false)

    /** Resultado publicado da última consulta ao sistema; nunca lido em vez de consultar. */
    private val consulta = MutableStateFlow(consultarAgora())

    val estado: StateFlow<OnboardingUiState> =
        combine(
            settings.settings,
            passo,
            consulta,
            combine(pedidoEmCurso, papelNegado) { emCurso, negado -> emCurso to negado },
        ) { config, passoAtual, sistema, pedido ->
            OnboardingUiState(
                step = passoAtual,
                screeningRoleHeld = sistema.papelDetido,
                roleRequestInFlight = pedido.first,
                roleDenied = pedido.second,
                contactsPermission = sistema.agenda,
                notificationPermission = sistema.notificacao,
                settings = config,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TEMPO_DE_SOBREVIDA_MILLIS),
            initialValue = OnboardingUiState(),
        )

    fun avancar() {
        passo.value = (passo.value + 1).coerceAtMost(TOTAL_DE_PASSOS - 1)
    }

    fun voltar() {
        passo.value = (passo.value - 1).coerceAtLeast(0)
    }

    /**
     * Pular grava a marca de concluído igual a concluir: nos dois casos o usuário já viu o
     * onboarding, e mostrá-lo de novo na próxima abertura seria insistir com quem já respondeu.
     */
    fun pular() {
        viewModelScope.launch { settings.markOnboardingCompleted() }
    }

    fun concluir() {
        viewModelScope.launch { settings.markOnboardingCompleted() }
    }

    fun reconsultarPapel() {
        val antes = consulta.value.papelDetido
        val agora = consultarAgora()
        consulta.value = agora
        pedidoEmCurso.value = false
        // Pedimos e voltou sem o papel: o usuário recusou no seletor do sistema. Isso NÃO barra o
        // avanço — só permite à tela explicar o que ele perde.
        if (!antes && !agora.papelDetido && papelFoiPedido) papelNegado.value = true
        if (agora.papelDetido) papelNegado.value = false
    }

    /**
     * Intenção de pedido do papel, ou nulo quando o aparelho não oferece o papel — caso em que a
     * tela não pode nem exibir o botão.
     */
    fun intencaoDePedidoDoPapel(): Intent? = requestRoleIntent()?.also {
        papelFoiPedido = true
        pedidoEmCurso.value = true
    }

    /**
     * Pede a leitura da agenda.
     *
     * A ORDEM aqui é CONTRATO, não estilo, e as Fases 4, 5 e 6 já a estabeleceram três vezes: a
     * marca é gravada e SÓ ENTÃO o disparo acontece, na sequência, sem esperar a escrita concluir.
     * É o mesmo padrão que a tela de ativação do modo discador já usa em produção.
     *
     * O motivo: a consulta de justificativa da plataforma responde `false` nos DOIS extremos — antes
     * do primeiro pedido e depois da negação definitiva —, então a única coisa que separa "nunca
     * perguntamos" de "negaram de vez" é esta marca gravada em disco. E o usuário pode encerrar o
     * aplicativo com o diálogo do sistema aberto: se a marca dependesse do retorno, o aplicativo
     * voltaria achando que nunca perguntou e pediria de novo a cada abertura, para sempre.
     */
    fun pedirAgenda(dispararLauncher: () -> Unit) {
        viewModelScope.launch { settings.markContactsPermissionAsked() }
        dispararLauncher()
    }

    /** Mesma ordem e mesmo motivo de [pedirAgenda]: marca primeiro, disparo depois. */
    fun pedirNotificacao(dispararLauncher: () -> Unit) {
        viewModelScope.launch { settings.markNotificationPermissionAsked() }
        dispararLauncher()
    }

    /**
     * Gravação de uma configuração dos passos 2 a 5. Grava na hora, como a tela Proteção: não existe
     * acumular para gravar no fim, e pular no meio não pode perder o que já foi escolhido.
     */
    fun gravarConfiguracao(transform: (ScreeningSettings) -> ScreeningSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun reconsultarPermissoes() {
        consulta.value = consultarAgora()
    }

    private var papelFoiPedido = false

    private fun consultarAgora() = ConsultaDoSistema(
        papelDetido = roleHeld(),
        papelDisponivel = roleAvailable(),
        agenda = contactsState(),
        notificacao = notificationState(),
    )

    private data class ConsultaDoSistema(
        val papelDetido: Boolean,
        val papelDisponivel: Boolean,
        val agenda: ContactsPermissionState,
        val notificacao: RuntimePermissionAsk,
    )

    companion object {

        private const val TEMPO_DE_SOBREVIDA_MILLIS = 5_000L

        /** Fábrica manual com a sobrecarga que recebe os extras — a não deprecada da biblioteca. */
        @Suppress("LongParameterList")
        fun factory(
            settings: DataStoreSettingsRepository,
            roleHeld: () -> Boolean,
            roleAvailable: () -> Boolean,
            requestRoleIntent: () -> Intent?,
            contactsState: () -> ContactsPermissionState,
            notificationState: () -> RuntimePermissionAsk,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                OnboardingViewModel(
                    settings = settings,
                    roleHeld = roleHeld,
                    roleAvailable = roleAvailable,
                    requestRoleIntent = requestRoleIntent,
                    contactsState = contactsState,
                    notificationState = notificationState,
                ) as T
        }
    }
}

package org.sentinela.app.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.settings.BlockMode
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.FallbackPolicy
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.telecom.call.dialerModeState

/**
 * Estado da tela Proteção.
 *
 * [historyCount] existe por causa da confirmação da §9.2: o diálogo de limpar histórico precisa
 * dizer QUANTOS registros serão apagados. "Apagar tudo" sem número é pedir consentimento no escuro.
 */
data class SettingsUiState(
    val settings: ScreeningSettings = ScreeningSettings(),
    val screeningRoleHeld: Boolean = false,
    val screeningRoleAvailable: Boolean = false,
    val dialerMode: DialerModeState = DialerModeState.UNAVAILABLE,
    val historyCount: Long = 0L,
)

/**
 * Dono de estado da tela Proteção.
 *
 * **Não existe função de salvar, e a ausência dela é o contrato.** Cada item da tela grava no
 * repositório na hora; o cache mantido por coletor da Fase 3 leva o valor à triagem, e a mudança
 * vale na próxima chamada. Um botão salvar tornaria possível a interface e a triagem discordarem —
 * a tela mostraria a escolha nova enquanto a decisão ainda usaria a antiga —, e o retrato já é
 * imediato, então o botão só acrescentaria a janela de divergência. O retorno para o usuário é o
 * próprio controle se mexendo: sem aviso de "salvo", que a cada toque viraria ruído.
 *
 * Nenhuma troca de política pede confirmação: é reversível, e confirmação excessiva ensina o usuário
 * a tocar em "sim" sem ler. Só o que PERDE DADO confirma, e isso é da tela (§9.2) — aqui já se
 * assume decidido.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class SettingsViewModel(
    private val settings: DataStoreSettingsRepository,
    private val history: BlockedCallRepository,
    private val roleHeld: () -> Boolean,
    private val roleAvailable: () -> Boolean,
    private val requestRoleIntent: () -> Intent?,
    private val dialerRoleHeld: () -> Boolean,
    private val dialerRoleAvailable: () -> Boolean,
    private val contactsGranted: () -> Boolean,
    dialerOptedIn: Flow<Boolean>,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Resultado publicado da última consulta ao sistema; nunca lido em vez de consultar. */
    private val consulta = MutableStateFlow(consultarAgora())

    val estado: StateFlow<SettingsUiState> =
        combine(
            settings.settings,
            history.observeTotalCount().catch { emit(0L) },
            consulta,
            dialerOptedIn,
        ) { config, quantos, sistema, optou ->
            SettingsUiState(
                settings = config,
                screeningRoleHeld = sistema.triagemDetida,
                screeningRoleAvailable = sistema.triagemDisponivel,
                // Função pura que já existe desde a Fase 6, e que NÃO é reescrita aqui: a
                // precedência do modo discador tem nove casos travados por teste, e uma segunda
                // cópia dela divergiria da primeira.
                dialerMode = dialerModeState(
                    roleAvailable = sistema.discadorDisponivel,
                    roleHeld = sistema.discadorDetido,
                    contactsGranted = sistema.agendaConcedida,
                    userOptedIn = optou,
                ),
                historyCount = quantos,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TEMPO_DE_SOBREVIDA_MILLIS),
            initialValue = SettingsUiState(),
        )

    // ----------------------------------------------------------------------------------------------
    // Um item da §9 por função. Todas com a MESMA forma: gravar agora, no repositório, e nada mais.
    // ----------------------------------------------------------------------------------------------

    fun definirProtecao(ligada: Boolean) = gravar { it.copy(protectionEnabled = ligada) }

    fun definirPoliticaDeDesconhecidos(politica: OriginPolicy) =
        gravar { it.copy(unknownPolicy = politica) }

    fun definirPoliticaDeContatos(politica: OriginPolicy) =
        gravar { it.copy(contactsPolicy = politica) }

    fun definirPoliticaDaListaPessoal(politica: OriginPolicy) =
        gravar { it.copy(whitelistPolicy = politica) }

    fun definirBloqueioDePrivados(bloquear: Boolean) =
        gravar { it.copy(blockPrivateNumbers = bloquear) }

    fun definirModoDeBloqueio(modo: BlockMode) = gravar { it.copy(blockMode = modo) }

    fun definirOcultarDoHistoricoDoTelefone(ocultar: Boolean) =
        gravar { it.copy(hideFromNativeCallLog = ocultar) }

    fun definirNotificacaoPropria(mostrar: Boolean) =
        gravar { it.copy(showOwnNotification = mostrar) }

    fun definirIdentificacaoDaNotificacao(identificacao: NotificationIdentification) =
        gravar { it.copy(notificationIdentification = identificacao) }

    fun definirChamadaRepetidaToca(ligado: Boolean) =
        gravar { it.copy(repeatedCallBypassEnabled = ligado) }

    fun definirPoliticaDeFalha(politica: FallbackPolicy) =
        gravar { it.copy(fallbackPolicy = politica) }

    /**
     * Desligar o histórico **não apaga nada**. Apagar é ação própria ([limparHistorico]), e juntar as
     * duas coisas faria o usuário perder registros ao mexer num interruptor que não promete isso.
     */
    fun definirHistoricoLigado(ligado: Boolean) = gravar { it.copy(historyEnabled = ligado) }

    /**
     * Retenção. Escolher "não guardar" grava E poda o que existe, na mesma corotina e nessa ordem —
     * a configuração precisa estar valendo antes de o dado sumir.
     *
     * É exatamente por perder dado que a §9.2 manda a TELA confirmar antes de chamar aqui. Quando
     * esta função roda, a decisão já foi tomada.
     */
    fun definirRetencao(politica: RetentionPolicy) {
        viewModelScope.launch {
            settings.update { it.copy(retentionPolicy = politica) }
            if (!politica.shouldStore) {
                // Podar tudo o que já existe. O corte de "não guardar" é nulo por desenho da Fase 3
                // (a política nem grava), então o instante-limite aqui é o agora do relógio
                // injetado, e não um corte derivado da política.
                history.pruneOlderThan(clock())
            }
        }
    }

    /** Apaga todos os registros. A tela confirma antes, dizendo quantos (§9.2). */
    fun limparHistorico() {
        viewModelScope.launch { history.clearAll() }
    }

    /**
     * Reconsulta os DOIS papéis agora, sem guardar resposta — mesma doutrina do dono de estado da
     * home, e pelo mesmo motivo medido: perder um papel encerra o processo, e a plataforma não avisa
     * mudança de detentor a aplicativo comum.
     */
    fun reconsultarPapeis() {
        consulta.value = consultarAgora()
    }

    fun intencaoDePedidoDoPapel(): Intent? = requestRoleIntent()

    private fun gravar(transform: (ScreeningSettings) -> ScreeningSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun consultarAgora() = ConsultaDoSistema(
        triagemDetida = roleHeld(),
        triagemDisponivel = roleAvailable(),
        discadorDetido = dialerRoleHeld(),
        discadorDisponivel = dialerRoleAvailable(),
        agendaConcedida = contactsGranted(),
    )

    private data class ConsultaDoSistema(
        val triagemDetida: Boolean,
        val triagemDisponivel: Boolean,
        val discadorDetido: Boolean,
        val discadorDisponivel: Boolean,
        val agendaConcedida: Boolean,
    )

    companion object {

        private const val TEMPO_DE_SOBREVIDA_MILLIS = 5_000L

        /** Fábrica manual com a sobrecarga que recebe os extras — a não deprecada da biblioteca. */
        @Suppress("LongParameterList")
        fun factory(
            settings: DataStoreSettingsRepository,
            history: BlockedCallRepository,
            roleHeld: () -> Boolean,
            roleAvailable: () -> Boolean,
            requestRoleIntent: () -> Intent?,
            dialerRoleHeld: () -> Boolean,
            dialerRoleAvailable: () -> Boolean,
            contactsGranted: () -> Boolean,
            dialerOptedIn: Flow<Boolean>,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                SettingsViewModel(
                    settings = settings,
                    history = history,
                    roleHeld = roleHeld,
                    roleAvailable = roleAvailable,
                    requestRoleIntent = requestRoleIntent,
                    dialerRoleHeld = dialerRoleHeld,
                    dialerRoleAvailable = dialerRoleAvailable,
                    contactsGranted = contactsGranted,
                    dialerOptedIn = dialerOptedIn,
                ) as T
        }
    }
}

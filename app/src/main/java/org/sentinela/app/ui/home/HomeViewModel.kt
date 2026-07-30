package org.sentinela.app.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.telecom.call.dialerModeState
import java.time.Instant
import java.time.ZoneId

/**
 * Dono de estado da home.
 *
 * Recebe cada colaborador por PARÂMETRO e jamais constrói o container de dependências: a Fase 5
 * mediu que um segundo container no mesmo processo derruba a aplicação, e um dono de estado que
 * construísse o seu próprio só seria testável ligando banco, arquivo de preferências e agenda.
 *
 * As consultas de papel do sistema chegam como FUNÇÕES, e não como valores, exatamente para que o
 * teste possa contá-las: o que prova que o estado é vivo é o **contador de invocações**, nunca um
 * cronômetro. Cronômetro mede o ambiente e passa verde com qualquer cache dentro.
 */
// Os quatro comandos que a home ganhou quando a tela passou a existir (07-10) levaram a classe ao
// limite de funções do analisador. Suprimido AQUI, no ponto de uso, em vez de afrouxar o limite na
// configuração compartilhada — precedente das Fases 3 e 6 e do repositório de configurações.
// A reinscrição do fluxo de leitura usa um operador ainda marcado como experimental na biblioteca de
// corotinas; ele é estável em uso há várias versões e o projeto já o consome na camada de contatos.
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "TooManyFunctions")
class HomeViewModel(
    private val settings: DataStoreSettingsRepository,
    history: BlockedCallRepository,
    private val roleHeld: () -> Boolean,
    private val roleAvailable: () -> Boolean,
    private val requestRoleIntent: () -> Intent?,
    private val contactsState: () -> ContactsPermissionState,
    private val dialerRoleHeld: () -> Boolean,
    private val dialerRoleAvailable: () -> Boolean,
    private val dialerOptedIn: () -> Boolean,
    private val mask: (String) -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /**
     * Resultado PUBLICADO da última consulta ao sistema — jamais lido como substituto de consultar.
     *
     * A diferença não é sutil: [reconsultarPapel] chama as funções de papel **toda** vez que é
     * chamada e sobrescreve isto; nada aqui é consultado para decidir se vale a pena perguntar. Este
     * canal existe só porque o estado da tela é um fluxo e o resultado precisa de um lugar por onde
     * entrar nele.
     *
     * Por que nunca pode virar cache, em três fatos medidos:
     * - Perder um papel do sistema **encerra o processo** do aplicativo — medido três vezes na
     *   Fase 6, e vale igual quando é o usuário que troca nas configurações. O retorno do seletor do
     *   sistema, por isso, nunca é a fonte da verdade: no caminho que importa ele não roda.
     * - A plataforma não oferece aviso de mudança de detentor de papel para aplicativo comum. A
     *   verificação só pode ser pergunta pontual, feita quando a tela volta ao primeiro plano.
     * - O trio de consultas custa metade de um milésimo de milissegundo, medido na pesquisa da fase
     *   (p50 de 30 µs, três ordens de grandeza abaixo de um quadro). **Não existe argumento de
     *   desempenho para cachear** — só o risco de guardar uma resposta que já virou mentira.
     */
    private val ultimaConsulta = MutableStateFlow(consultarAgora())

    /**
     * Contador de tentativas de leitura do histórico. Cada incremento REINSCREVE o fluxo de origem, e é
     * isso que dá dentes ao botão de tentar de novo do estado de falha.
     */
    private val tentativa = MutableStateFlow(0)

    /**
     * Leitura do histórico já com a falha domesticada.
     *
     * `catch` aqui é o oposto DELIBERADO do caminho da chamada da Fase 6, onde a exceção propaga de
     * propósito: lá, processo morto é detectado pelo sistema de telefonia, que religa o discador do
     * aparelho. Aqui a tela é o consumidor final, e uma exceção derrubaria a home — uma home
     * congelada ninguém detecta exceto o usuário, e ele não tem a quem reclamar. Então a falha vira
     * estado visível ([HomeUiState.readError]) com as contagens indisponíveis, e não sobe.
     */
    private val leitura: Flow<LeituraHistorico> =
        tentativa.flatMapLatest {
            combine(history.observeTotalCount(), history.observeRecent()) { total, recentes ->
                LeituraHistorico.Ok(total, recentes) as LeituraHistorico
            }
                .catch { emit(LeituraHistorico.Falha) }
                .onStart { emit(LeituraHistorico.Carregando) }
        }

    /**
     * Estado da tela.
     *
     * Valor inicial explícito com as duas contagens em [StatValue.Loading]: a coleta com ciclo de
     * vida exige valor inicial, e o primeiro quadro NUNCA espera disco. Bloquear a corotina para ter
     * o retrato pronto antes de compor trocaria um esqueleto tonal de alguns milissegundos por uma
     * tela branca de duração desconhecida.
     */
    val estado: StateFlow<HomeUiState> =
        combine(settings.settings, leitura, ultimaConsulta, ::montar)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TEMPO_DE_SOBREVIDA_MILLIS),
                initialValue = montar(
                    ScreeningSettings(),
                    LeituraHistorico.Carregando,
                    ultimaConsulta.value,
                ),
            )

    /**
     * Reconsulta o sistema AGORA e publica o resultado. Chamada pela retomada da tela.
     *
     * Duas chamadas produzem duas consultas de cada sinal, sempre — é isso que o contador de
     * invocações do teste trava.
     */
    fun reconsultarPapel() {
        ultimaConsulta.value = consultarAgora()
    }

    /**
     * Intenção de pedido do papel de triagem, ou nulo quando o aparelho não oferece o papel.
     *
     * Nulo não é erro: é a resposta correta, e a tela não pode nem exibir o botão de conserto nesse
     * caso — disparar levaria o usuário a uma tela do sistema que não resolve nada.
     */
    fun intencaoDePedidoDoPapel(): Intent? = requestRoleIntent()

    /**
     * Liga e desliga a proteção pelo interruptor do cartão de status. Grava na hora, sem botão salvar,
     * exatamente como cada item da tela Proteção: o cache mantido por coletor leva o valor à triagem e
     * a mudança vale na próxima chamada.
     */
    fun definirProtecao(ligada: Boolean) {
        viewModelScope.launch { settings.update { it.copy(protectionEnabled = ligada) } }
    }

    /**
     * Religa o histórico a partir do aviso da home.
     *
     * **Duas configurações o desligam, e mexer em uma só deixaria o aviso reaparecer no quadro
     * seguinte:** o interruptor do histórico e a retenção "não guardar", que mantém o interruptor
     * ligado e ainda assim não grava nada. Um usuário que toca em "ativar histórico" e vê o mesmo aviso
     * de novo conclui, com razão, que o botão não funciona. Por isso a retenção volta ao padrão do
     * produto quando a que estava valendo não guardava.
     *
     * Nada é apagado aqui, em nenhum dos dois caminhos.
     */
    fun religarHistorico() {
        viewModelScope.launch {
            settings.update { atual ->
                atual.copy(
                    historyEnabled = true,
                    retentionPolicy = if (atual.retentionPolicy.shouldStore) {
                        atual.retentionPolicy
                    } else {
                        ScreeningSettings().retentionPolicy
                    },
                )
            }
        }
    }

    /**
     * Tenta ler o histórico de novo depois de uma falha.
     *
     * A releitura é uma reinscrição de verdade, e não um recálculo do mesmo resultado: o fluxo de
     * origem é refeito, passando pelo estado de carregamento antes de publicar o novo resultado. Um
     * botão de tentar de novo que não refaz a leitura é pior do que nenhum — ele devolve a mesma falha
     * e ensina o usuário a desconfiar do aplicativo.
     */
    fun tentarLerNovamente() {
        tentativa.value += 1
    }

    /**
     * Pede a leitura da agenda a partir do aviso da home.
     *
     * A ORDEM é contrato, estabelecido três vezes desde a Fase 4: a marca é gravada e SÓ ENTÃO o
     * diálogo é disparado, sem esperar a escrita concluir. O usuário pode encerrar o aplicativo com o
     * diálogo do sistema aberto; se a marca dependesse do retorno, o aplicativo voltaria achando que
     * nunca perguntou e pediria de novo a cada abertura, para sempre.
     */
    fun pedirAgenda(dispararLauncher: () -> Unit) {
        viewModelScope.launch { settings.markContactsPermissionAsked() }
        dispararLauncher()
    }

    private fun consultarAgora(): ConsultaDePapel {
        val agenda = contactsState()
        return ConsultaDePapel(
            triagemDetida = roleHeld(),
            triagemDisponivel = roleAvailable(),
            agenda = agenda,
            modoDiscador = dialerModeState(
                roleAvailable = dialerRoleAvailable(),
                roleHeld = dialerRoleHeld(),
                contactsGranted = agenda == ContactsPermissionState.GRANTED,
                userOptedIn = dialerOptedIn(),
            ),
        )
    }

    private fun montar(
        config: ScreeningSettings,
        leitura: LeituraHistorico,
        papel: ConsultaDePapel,
    ): HomeUiState {
        // Duas configurações desligam o histórico, e a segunda é fácil de esquecer: escolher "não
        // guardar" na retenção deixa `historyEnabled` ligado e ainda assim nada é gravado.
        val historicoLigado = config.historyEnabled && config.retentionPolicy.shouldStore
        val base = HomeUiState(
            protectionEnabled = config.protectionEnabled,
            screeningRoleHeld = papel.triagemDetida,
            screeningRoleAvailable = papel.triagemDisponivel,
            contactsPermission = papel.agenda,
            dialerMode = papel.modoDiscador,
            historyEnabled = historicoLigado,
        )
        return when {
            // Precedência: desligado vence falha e vence carregando. Quem desligou o histórico não
            // precisa ver aviso de erro de leitura de algo que não deveria existir.
            !historicoLigado -> base.comContagensIndisponiveis(erro = false)
            leitura is LeituraHistorico.Falha -> base.comContagensIndisponiveis(erro = true)
            leitura is LeituraHistorico.Ok -> base.copy(
                totalBlocked = StatValue.Loaded(leitura.total),
                blockedToday = StatValue.Loaded(contarDeHoje(leitura.recentes)),
                lastBlocked = leitura.recentes.firstOrNull()?.paraInterface(),
            )
            else -> base
        }
    }

    private fun HomeUiState.comContagensIndisponiveis(erro: Boolean) = copy(
        totalBlocked = StatValue.Unavailable,
        blockedToday = StatValue.Unavailable,
        // Sem contagem confiável não existe "última bloqueada" honesta a exibir.
        lastBlocked = null,
        readError = erro,
    )

    private fun contarDeHoje(recentes: List<BlockedCallEntry>): Long {
        val inicio = inicioDoDia(clock())
        return recentes.count { it.timestampUtcMillis >= inicio }.toLong()
    }

    /**
     * A máscara é aplicada AQUI, e este é o último ponto do aplicativo em que os dígitos existem.
     * O objeto devolvido não os carrega, por desenho do próprio tipo.
     */
    private fun BlockedCallEntry.paraInterface() = LastBlockedUi(
        maskedNumber = numberE164?.let(mask)
            ?: maskedNumber.ifBlank { PhoneMask.MASCARA_GENERICA },
        reasonLabelRes = reasonLabelRes(reason),
        timestampUtcMillis = timestampUtcMillis,
    )

    private data class ConsultaDePapel(
        val triagemDetida: Boolean,
        val triagemDisponivel: Boolean,
        val agenda: ContactsPermissionState,
        val modoDiscador: DialerModeState,
    )

    private sealed interface LeituraHistorico {
        data object Carregando : LeituraHistorico
        data object Falha : LeituraHistorico
        data class Ok(val total: Long, val recentes: List<BlockedCallEntry>) : LeituraHistorico
    }

    companion object {

        /** Sobrevida da coleta a uma rotação de tela; abaixo disso a home recarregaria do zero. */
        private const val TEMPO_DE_SOBREVIDA_MILLIS = 5_000L

        private fun inicioDoDia(agora: Long): Long =
            Instant.ofEpochMilli(agora)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        /**
         * Fábrica manual, com a sobrecarga que recebe os extras — a não deprecada da biblioteca de
         * ciclo de vida.
         *
         * Recebe os colaboradores um por um, e não o container: a montagem a partir do container é
         * da rota que hospeda a tela, e manter o container fora deste arquivo é o que permite
         * construir o dono de estado inteiro num teste de JVM.
         */
        @Suppress("LongParameterList")
        fun factory(
            settings: DataStoreSettingsRepository,
            history: BlockedCallRepository,
            roleHeld: () -> Boolean,
            roleAvailable: () -> Boolean,
            requestRoleIntent: () -> Intent?,
            contactsState: () -> ContactsPermissionState,
            dialerRoleHeld: () -> Boolean,
            dialerRoleAvailable: () -> Boolean,
            dialerOptedIn: () -> Boolean,
            mask: (String) -> String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                HomeViewModel(
                    settings = settings,
                    history = history,
                    roleHeld = roleHeld,
                    roleAvailable = roleAvailable,
                    requestRoleIntent = requestRoleIntent,
                    contactsState = contactsState,
                    dialerRoleHeld = dialerRoleHeld,
                    dialerRoleAvailable = dialerRoleAvailable,
                    dialerOptedIn = dialerOptedIn,
                    mask = mask,
                ) as T
        }
    }
}

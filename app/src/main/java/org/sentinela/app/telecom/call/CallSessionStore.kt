package org.sentinela.app.telecom.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Observador do retrato da sessão, para quem precisa reagir a cada mudança de estado sem
 * manter um coletor próprio.
 *
 * Esta é a costura deliberada por onde a notificação de chamada entra: quem quiser postar,
 * atualizar ou cancelar aviso conforme o estado da chamada se registra aqui, e não dentro do
 * serviço da plataforma. O serviço continua fino e o armazém continua sendo o único lugar que
 * sabe quando o estado mudou.
 */
fun interface CallSessionObserver {
    fun onSnapshot(snapshot: CallSnapshot)
}

/**
 * Identidade da chamada como ela pode aparecer em **aviso do sistema**: nome que a própria ligação
 * informou e número já **mascarado**.
 *
 * Existe como tipo próprio para que a fronteira seja visível no código: quem publica notificação
 * recebe este objeto e não tem como obter a sequência completa de dígitos, que só vive em
 * [CallIdentity] e só aparece na tela de chamada.
 */
data class MaskedCallIdentity(
    val displayName: String? = null,
    val maskedNumber: String? = null,
)

/**
 * Costura do aviso de chamada em curso.
 *
 * Ela existe aqui, e não no serviço da plataforma, porque **este** arquivo é o único que sabe
 * quando o retrato mudou: a troca do aviso de chamada recebida pelo aviso de chamada em curso é
 * exatamente a transição para o estado ativo, e criar um segundo caminho para o mesmo fato é como
 * se produz aviso que não troca nunca ou troca duas vezes.
 *
 * Pura de propósito: quem sabe postar notificação é a camada de plataforma.
 */
fun interface OngoingCallNotifier {
    fun notifyOngoing(identity: MaskedCallIdentity)
}

/**
 * Instância única do processo com o estado observável da chamada em curso.
 *
 * Existe porque o serviço da plataforma e a tela de chamada são dois componentes com ciclos de
 * vida independentes, e a chamada sobrevive à tela: girar o aparelho, sair da tela ou perder o
 * processo não encerra a ligação. Amarrar um ao outro é a origem de todo vazamento em aplicativo
 * de telefone. Aqui a tela apenas observa um fluxo de estado de domínio e comanda por uma
 * costura; nada da plataforma atravessa esta fronteira.
 *
 * **Nenhum objeto de chamada da plataforma entra neste arquivo.** O que a telefonia entrega são
 * inteiros de estado, dados de identidade já resolvidos e um identificador opaco para
 * diagnóstico. O objeto de chamada da plataforma é um manipulador de comunicação entre processos:
 * guardá-lo fora da camada de telefonia é vazamento garantido.
 *
 * Não existe persistência de chamada e ela não é necessária: a pesquisa desta fase mediu que,
 * morrendo o processo, o sistema de telefonia religa a chamada no discador do aparelho sem
 * derrubar a ligação, e reentrega a chamada seguinte a este aplicativo normalmente.
 */
class CallSessionStore(
    private val scope: CoroutineScope,
    private val notifications: OngoingCallNotifier? = null,
    private val maskNumber: (String) -> String? = { null },
    /**
     * Relógio repassado à sessão para marcar o início da chamada.
     *
     * Chega por parâmetro para o coordenador continuar puro — ele não pode importar tipo da
     * plataforma, e é o `AppContainer` que injeta o relógio monotônico. Duração pede relógio que
     * não ande para trás: com relógio de parede, um ajuste de hora pela operadora no meio da
     * ligação fazia o cronômetro saltar ou exibir tempo negativo.
     */
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val retrato = MutableStateFlow(CallSnapshot())

    /** Fluxo observável da sessão. Nunca ausente: sem chamada, vale o retrato encerrado. */
    val state: StateFlow<CallSnapshot> = retrato.asStateFlow()

    /**
     * Sessão corrente. A tela usa isto para comandar e para confirmar a apresentação; é a
     * máquina de estado **pura** do plano 06-01, sem nenhum tipo da plataforma.
     */
    var session: CallSessionCoordinator? = null
        private set

    /** Costura da sessão corrente, ou ausência quando nenhuma chamada está vinculada. */
    var controls: CallControls? = null
        private set

    /**
     * Identificador opaco da chamada corrente, só para diagnóstico e para a notificação
     * distinguir uma chamada da seguinte. Não carrega número nem nome.
     */
    var opaqueCallId: String? = null
        private set

    private val observadores = mutableSetOf<CallSessionObserver>()

    private var espelho: Job? = null

    fun addObserver(observer: CallSessionObserver) {
        observadores += observer
        observer.onSnapshot(retrato.value)
    }

    fun removeObserver(observer: CallSessionObserver) {
        observadores -= observer
    }

    /**
     * Vincula a costura da telefonia e abre uma sessão nova.
     *
     * O escopo real é passado adiante de propósito: sem escopo, o vigia do prazo de apresentação
     * do plano 06-01 fica desligado, e é justamente ele que transforma tela congelada — o único
     * modo de falha desta fase que ninguém detecta — em falha alta e visível.
     */
    fun attach(controls: CallControls) {
        detach()
        val nova = CallSessionCoordinator(controls = controls, clock = clock, scope = scope)
        this.controls = controls
        session = nova
        espelho = scope.launch { nova.state.collect(::publicar) }
    }

    /**
     * Desvincula a sessão, publicando antes o retrato final.
     *
     * A publicação síncrona não é zelo: o espelho corre num escopo real, e cancelá-lo logo depois
     * de a sessão ter ido para o estado final produzia uma corrida em que o último retrato — o
     * encerramento — às vezes nunca chegava a quem observa. Quem estivesse com a tela aberta
     * ficaria olhando uma chamada ativa que já terminou, que é a categoria de falha que esta fase
     * inteira existe para evitar.
     */
    fun detach() {
        session?.state?.value?.let { publicar(it) }
        espelho?.cancel()
        espelho = null
        session = null
        controls = null
        opaqueCallId = null
    }

    private var estadoAnterior: CallUiState = CallSnapshot().state

    private fun publicar(novo: CallSnapshot) {
        retrato.value = novo
        val anterior = estadoAnterior
        estadoAnterior = novo.state
        observadores.toList().forEach { it.onSnapshot(novo) }
        // Só na transição, nunca a cada retrato: mudo, viva-voz e teclado republicam o estado
        // ativo várias vezes por chamada, e republicar o aviso a cada um faria a barra de avisos
        // piscar durante a ligação.
        if (novo.state == CallUiState.Active && anterior != CallUiState.Active) {
            notifications?.notifyOngoing(avisoDe(novo.identity))
        }
    }

    /**
     * Identidade como ela pode aparecer em aviso do sistema. A máscara entra por costura porque a
     * máscara única do projeto depende dos metadados de telefone, que não podem atravessar esta
     * fronteira. Sem a costura, o aviso sai **sem número** — degradação segura; o que nunca
     * acontece é a sequência completa de dígitos chegar à notificação.
     */
    private fun avisoDe(identity: CallIdentity) = MaskedCallIdentity(
        displayName = identity.displayName,
        maskedNumber = identity.fullNumber?.let(maskNumber),
    )

    // --- entradas repassadas pela camada de telefonia ---------------------------------

    fun onCallAdded(rawState: Int, identity: CallIdentity, opaqueId: String) {
        opaqueCallId = opaqueId
        session?.onCallAdded(rawState, identity)
    }

    fun onStateChanged(rawState: Int) {
        session?.onStateChanged(rawState)
    }

    fun onAudioStateChanged(
        muted: Boolean,
        speakerOn: Boolean,
        supportedRoutes: Set<CallAudioRoute>,
    ) {
        session?.onAudioStateChanged(muted, speakerOn, supportedRoutes)
    }

    fun onCallRemoved() {
        session?.onCallRemoved()
    }
}

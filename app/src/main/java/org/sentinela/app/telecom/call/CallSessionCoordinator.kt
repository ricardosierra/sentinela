package org.sentinela.app.telecom.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Prazo para a interface confirmar que apresentou uma chamada recebida.
 *
 * Cinco segundos são o teto razoável para garantir que aparelhos com cold start lento (RAM baixa,
 * processo morto) e casos onde a `fullScreenIntent` é bloqueada por DND ainda consigam apresentar
 * a tela antes do prazo. Uma chamada toca por dezenas de segundos, então 5 s são aceitáveis para
 * o usuário e suficientes para cobrir os casos medidos em aparelhos reais.
 *
 * Vencido o prazo sem confirmação a sessão falha ALTO: interface vinculada e congelada é o único
 * modo de falha desta fase que ninguém detecta — nem o sistema de telefonia, nem o usuário.
 */
const val PRESENTATION_DEADLINE_MILLIS: Long = 5_000L

/** Falha alta: a chamada recebida não conseguiu ser apresentada dentro do prazo. */
class CallPresentationTimeoutException(rawState: Int) : IllegalStateException(
    "sessao de chamada nao apresentou interface no prazo; estado recebido: $rawState",
)

/** O estado encerra a sessão? Usado para desarmar tom de teclado e teclado aberto. */
private val CallUiState.terminal: Boolean
    get() = this == CallUiState.Ended || this == CallUiState.Failed

/**
 * Máquina de estado **pura** da sessão de chamada.
 *
 * Precedente arquitetural: o coordenador de triagem da Fase 5 — puro, com prazo interno,
 * guarda local e costura de saída. A diferença deliberada é o tratamento de falha. Na
 * triagem, defeito deixava a chamada passar por uma rede permissiva ampla. Aqui **não existe
 * rede permissiva**: exceção da costura sobe para quem chamou.
 *
 * O motivo é medição, não gosto: o sistema de telefonia percebe quando o processo do
 * aplicativo morre no meio de uma chamada e religa a chamada no discador do aparelho, sem
 * derrubar a ligação. O que ele **não** percebe é uma interface vinculada e congelada — nesse
 * caso ninguém substitui ninguém e o usuário fica preso numa tela morta. Portanto o guarda-
 * corpo desta fase é falhar rápido e alto, nunca engolir defeito e seguir vivo.
 *
 * Mudo e viva-voz são delegados à costura e provados nela, porque pedir mudo ao serviço da
 * plataforma sem telefone vinculado é operação sem efeito e sem erro — provar ali daria teste
 * verde sem prova nenhuma.
 *
 * Nenhuma regra de bloqueio de chamada mora aqui: quem decide o destino de uma ligação é o
 * motor de decisão, e a triagem já rodou antes desta sessão existir.
 */
// A superfície larga é o contrato desta fase, não descuido: são quatro entradas vindas da
// telefonia, oito comandos da interface e a confirmação de apresentação. Reduzir o número de
// funções aqui só seria possível agrupando comandos num objeto de intenção, o que esconderia
// a guarda por estado corrente — justamente o que precisa ficar visível e testável.
@Suppress("TooManyFunctions")
class CallSessionCoordinator(
    private val controls: CallControls,
    private val mapper: CallStateMapper = PlatformCallStateMapper(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope? = null,
    private val presentationDeadlineMillis: Long = PRESENTATION_DEADLINE_MILLIS,
) {

    private val estado = MutableStateFlow(CallSnapshot())

    /** Fluxo observável do retrato da sessão; nunca emite ausência de estado. */
    val state: StateFlow<CallSnapshot> = estado.asStateFlow()

    /** Dígito de teclado em curso. Local à sessão, nunca compartilhado entre chamadas. */
    private var digitoEmCurso: Char? = null

    /** Vigia do prazo de apresentação. Local à sessão, como a guarda da triagem. */
    private var vigiaDeApresentacao: Job? = null
    private var apresentada: Boolean = false

    // --- entradas vindas da telefonia -------------------------------------------------

    fun onCallAdded(rawState: Int, identity: CallIdentity) {
        val novo = mapper.map(rawState)
        estado.value = CallSnapshot(
            state = novo,
            identity = identity,
            startedAtMillis = if (novo == CallUiState.Active) clock() else null,
        )
        if (novo == CallUiState.Incoming) armarPrazo(rawState) else desarmarPrazo()
    }

    /**
     * Chamada pela camada de interface no momento em que a tela da chamada recebida está
     * efetivamente na frente do usuário. Sem esta confirmação, o prazo vence e a sessão falha.
     */
    fun confirmPresented() {
        apresentada = true
        desarmarPrazo()
    }

    private fun armarPrazo(rawState: Int) {
        val escopo = scope ?: return
        apresentada = false
        desarmarPrazo()
        vigiaDeApresentacao = escopo.launch {
            delay(presentationDeadlineMillis)
            if (!apresentada) throw CallPresentationTimeoutException(rawState)
        }
    }

    private fun desarmarPrazo() {
        vigiaDeApresentacao?.cancel()
        vigiaDeApresentacao = null
    }

    fun onStateChanged(rawState: Int) {
        val novo = mapper.map(rawState)
        if (novo.terminal) {
            encerrarTomPendente()
            desarmarPrazo()
        }
        val atual = estado.value
        estado.value = atual.copy(
            state = novo,
            startedAtMillis = atual.startedAtMillis
                ?: if (novo == CallUiState.Active) clock() else null,
            keypadOpen = !novo.terminal && atual.keypadOpen,
        )
    }

    fun onAudioStateChanged(
        muted: Boolean,
        speakerOn: Boolean,
        supportedRoutes: Set<CallAudioRoute>,
    ) {
        estado.value = estado.value.copy(
            muted = muted,
            speakerOn = speakerOn,
            speakerAvailable = supportedRoutes.size > 1,
        )
    }

    fun onCallRemoved() {
        encerrarTomPendente()
        desarmarPrazo()
        estado.value = estado.value.copy(state = CallUiState.Ended, keypadOpen = false)
    }

    // --- comandos vindos da interface -------------------------------------------------

    /** Só chega à costura quando a chamada está de fato tocando. */
    fun answer() {
        if (estado.value.state == CallUiState.Incoming) controls.answer()
    }

    /** Só chega à costura quando a chamada está de fato tocando. */
    fun reject() {
        if (estado.value.state == CallUiState.Incoming) controls.reject()
    }

    /** Chega à costura em qualquer estado com encerramento habilitado, inclusive não suportado. */
    fun hangUp() {
        if (estado.value.hangUpEnabled) controls.hangUp()
    }

    fun setMuted(muted: Boolean) {
        if (estado.value.muted == muted) return
        controls.setMuted(muted)
        estado.value = estado.value.copy(muted = muted)
    }

    fun toggleMute() = setMuted(!estado.value.muted)

    fun setSpeakerOn(on: Boolean) {
        val atual = estado.value
        if (!atual.speakerAvailable || atual.speakerOn == on) return
        controls.setSpeakerOn(on)
        estado.value = atual.copy(speakerOn = on)
    }

    fun toggleSpeaker() = setSpeakerOn(!estado.value.speakerOn)

    /** Abrir e fechar o teclado é mudança de tela: nenhum tom é enviado. */
    fun toggleKeypad() {
        estado.value = estado.value.copy(keypadOpen = !estado.value.keypadOpen)
    }

    /**
     * Envia um tom de teclado, garantindo o pareamento: um tom novo encerra o anterior antes
     * de começar. Invariante de fase, no mesmo nível da resposta única da triagem — tom
     * iniciado e nunca encerrado deixa o áudio da chamada preso num bipe contínuo.
     */
    fun pressDigit(digit: Char) {
        encerrarTomPendente()
        controls.playDtmf(digit)
        digitoEmCurso = digit
        estado.value = estado.value.copy(sentDigits = estado.value.sentDigits + digit)
    }

    fun releaseDigit() = encerrarTomPendente()

    private fun encerrarTomPendente() {
        if (digitoEmCurso == null) return
        digitoEmCurso = null
        controls.stopDtmf()
    }
}

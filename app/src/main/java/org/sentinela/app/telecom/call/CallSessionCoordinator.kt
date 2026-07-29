package org.sentinela.app.telecom.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
class CallSessionCoordinator(
    private val controls: CallControls,
    private val mapper: CallStateMapper = PlatformCallStateMapper(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val estado = MutableStateFlow(CallSnapshot())

    /** Fluxo observável do retrato da sessão; nunca emite ausência de estado. */
    val state: StateFlow<CallSnapshot> = estado.asStateFlow()

    /** Dígito de teclado em curso. Local à sessão, nunca compartilhado entre chamadas. */
    private var digitoEmCurso: Char? = null

    // --- entradas vindas da telefonia -------------------------------------------------

    fun onCallAdded(rawState: Int, identity: CallIdentity) {
        val novo = mapper.map(rawState)
        estado.value = CallSnapshot(
            state = novo,
            identity = identity,
            startedAtMillis = if (novo == CallUiState.Active) clock() else null,
        )
    }

    fun onStateChanged(rawState: Int) {
        val novo = mapper.map(rawState)
        if (novo.terminal) encerrarTomPendente()
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

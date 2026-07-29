package org.sentinela.app.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import org.sentinela.app.AppContainer
import org.sentinela.app.SentinelaApp
import org.sentinela.app.notifications.IncomingCallNotifier
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.telecom.call.CallOrigin
import org.sentinela.app.telecom.call.CallSessionStore
import org.sentinela.app.telecom.call.MaskedCallIdentity
import org.sentinela.app.telecom.call.TelecomCallControls
import org.sentinela.app.telecom.call.audioRoutesFromMask

/**
 * Ponto de entrada da interface de chamada. Camada fina de propósito, no mesmo molde do serviço
 * de triagem: aqui só se vincula a costura, se repassa o que a telefonia informa e se registra e
 * remove o observador da chamada. Toda a máquina de estado vive no coordenador puro do plano
 * 06-01, onde ela é testável sem plataforma.
 *
 * **Nenhum defeito é interceptado neste arquivo, e isso é o guarda-corpo, não descuido.** A
 * regra é o inverso exato da triagem, e o motivo é medição. O sistema de telefonia percebe
 * quando o processo do aplicativo morre no meio de uma ligação: ele desfaz o vínculo e assume a
 * chamada com o discador que veio no aparelho, sem derrubar a ligação e avisando o usuário. O que
 * ele **não** tem como perceber é uma interface vinculada e congelada — nesse caso ninguém
 * substitui ninguém, e o usuário fica olhando uma tela parada com o telefone tocando. Interceptar
 * defeito aqui converteria a falha que a plataforma sabe consertar exatamente naquela que ela não
 * sabe. Falhar alto e rápido é a degradação correta desta camada.
 *
 * Um aviso para quem for depurar isto e se assustar com o diagnóstico: o sistema vincula
 * **vários** serviços de chamada à mesma ligação — o de acessórios sem fio, o de detecção de
 * fraude, o de legendas. Isso é normal e não é conflito. Apenas quem declara no manifest a
 * substituição da interface de chamada apresenta tela ao usuário; os demais só observam.
 *
 * Um segundo aviso, também medido: chamada barrada pela triagem **nunca** chega aqui. O filtro de
 * triagem roda antes de a interface de chamada ser informada, então esta classe não precisa e não
 * deve ter nenhum ramo sobre chamada bloqueada.
 */
class SentinelaInCallService : InCallService() {

    private val container: AppContainer
        get() = (application as SentinelaApp).container

    private val store: CallSessionStore
        get() = container.callSessionStore

    /**
     * Aviso da chamada. Vem do container da aplicação, como todo o resto: o serviço não constrói
     * colaborador próprio e nem sabe criar canal de notificação.
     */
    private val notifier: IncomingCallNotifier
        get() = container.incomingCallNotifier

    /**
     * Observador da chamada. Registrado ao receber a chamada e removido ao perdê-la: manter o
     * registro depois do fim é o vazamento clássico desta camada.
     */
    private val observador = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = store.onStateChanged(state)
    }

    override fun onCallAdded(call: Call) {
        call.registerCallback(observador)
        // O aviso é publicado ANTES de qualquer abertura de tela, e é ele que abre a tela quando o
        // aparelho está bloqueado: é o caminho oficial da plataforma para isso. A troca para o
        // aviso de chamada em curso não está aqui de propósito — quem sabe que o estado mudou é o
        // armazém, e ela vive lá.
        if (call.state == Call.STATE_RINGING) {
            notifier.notifyIncoming(maskedIdentityOf(call, container.maskNumber))
        }
        store.attach(TelecomCallControls(call, this))
        store.onCallAdded(call.state, identityOf(call), opaqueIdOf(call))
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(observador)
        notifier.cancel()
        store.onCallRemoved()
        store.detach()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        store.onAudioStateChanged(
            muted = audioState.isMuted,
            speakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER,
            supportedRoutes = audioRoutesFromMask(audioState.supportedRouteMask),
        )
    }
}

/**
 * Identidade apresentável, resolvida uma vez na fronteira. O refinamento da origem — distinguir
 * contato de número liberado — é da tela do plano 06-04, que consulta a agenda em memória; aqui
 * só se sabe o que a própria ligação informa.
 */
private fun identityOf(call: Call): CallIdentity {
    val details = call.details
    val restrito = details.handlePresentation == TelecomManager.PRESENTATION_RESTRICTED
    return CallIdentity(
        displayName = details.callerDisplayName?.takeIf { it.isNotBlank() },
        fullNumber = details.handle?.schemeSpecificPart,
        origin = if (restrito) CallOrigin.PRIVADO else CallOrigin.DESCONHECIDO,
    )
}

/**
 * A mesma identidade, na forma que pode aparecer em **aviso do sistema**: nome que a ligação
 * informou e número mascarado pela máscara única do projeto. A máscara é aplicada aqui, na
 * fronteira, exatamente como no trabalho pós-resposta da triagem — o notificador nunca recebe
 * número cru.
 */
private fun maskedIdentityOf(call: Call, mask: (String) -> String): MaskedCallIdentity {
    val details = call.details
    return MaskedCallIdentity(
        displayName = details.callerDisplayName?.takeIf { it.isNotBlank() },
        maskedNumber = details.handle?.schemeSpecificPart?.let(mask),
    )
}

/**
 * Identificador opaco da ligação, para diagnóstico e para a notificação distinguir uma chamada da
 * seguinte. Deriva da identidade do objeto em memória: não carrega número, nome nem nada do
 * usuário, e deixa de valer quando o processo termina — que é exatamente o tempo de vida desejado.
 */
private fun opaqueIdOf(call: Call): String = System.identityHashCode(call).toString(HEX)

private const val HEX: Int = 16

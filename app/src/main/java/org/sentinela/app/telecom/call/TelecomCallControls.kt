package org.sentinela.app.telecom.call

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi

/**
 * Nível da plataforma em que recusar uma chamada passou a aceitar um motivo declarado.
 *
 * O piso deste projeto é bem mais baixo, então o ramo por versão não é zelo excessivo: sem ele,
 * aparelhos anteriores estouram com erro de método ausente na hora exata em que o usuário tenta
 * recusar uma ligação — e nenhum emulador moderno mostra isso.
 */
private const val NIVEL_COM_MOTIVO_DE_RECUSA: Int = 34

/**
 * Traduz cada comando da interface para a telefonia.
 *
 * Nenhum destes métodos exige permissão: a autorização vem do vínculo privilegiado que o sistema
 * de telefonia faz com o serviço de chamada, não de uma permissão do aplicativo.
 *
 * Nenhuma captura de defeito aqui, por medição: processo que morre no meio de uma chamada é
 * detectado pelo sistema, que assume a ligação com o discador do aparelho sem derrubá-la. Já
 * interface vinculada e congelada não é detectada por ninguém. Engolir defeito converte a falha
 * que a plataforma sabe consertar naquela que ela não sabe.
 */
class TelecomCallControls(
    private val call: Call,
    private val service: InCallService,
) : CallControls {

    override fun answer() = call.answer(VideoProfile.STATE_AUDIO_ONLY)

    override fun reject() = rejectCall(call)

    override fun hangUp() = call.disconnect()

    override fun setMuted(muted: Boolean) = service.setMuted(muted)

    /**
     * Só delega quando a máscara de rotas do momento de fato oferece o alto-falante. Em aparelho
     * ou emulador que só suporta uma rota, pedir a troca não teria efeito e a interface ficaria
     * mostrando um estado que o áudio não acompanha.
     */
    override fun setSpeakerOn(on: Boolean) {
        val rotas = service.callAudioState ?: return
        if (rotas.supportedRouteMask and CallAudioState.ROUTE_SPEAKER == 0) return
        service.setAudioRoute(
            if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
        )
    }

    override fun playDtmf(digit: Char) = call.playDtmfTone(digit)

    override fun stopDtmf() = call.stopDtmfTone()
}

/**
 * Recusa a chamada pela sobrecarga que a versão do aparelho oferece.
 *
 * A sobrecarga com motivo declarado é melhor quando existe: ela informa ao sistema que a recusa
 * foi do usuário, e não uma desistência de quem ligou. Abaixo desse nível existe apenas a
 * sobrecarga antiga, que recusa sem mensagem de texto acompanhando.
 */
internal fun rejectCall(call: Call) {
    if (Build.VERSION.SDK_INT >= NIVEL_COM_MOTIVO_DE_RECUSA) {
        rejectComMotivo(call)
    } else {
        call.reject(false, null)
    }
}

@RequiresApi(NIVEL_COM_MOTIVO_DE_RECUSA)
private fun rejectComMotivo(call: Call) = call.reject(Call.REJECT_REASON_DECLINED)

/**
 * Traduz a máscara de rotas de áudio da plataforma no conjunto nomeado do domínio.
 *
 * A tradução existe para que a disponibilidade do alto-falante seja um dado de domínio, e não uma
 * consulta à telefonia feita de dentro da interface.
 */
internal fun audioRoutesFromMask(mask: Int): Set<CallAudioRoute> = buildSet {
    if (mask and CallAudioState.ROUTE_EARPIECE != 0) add(CallAudioRoute.FONE)
    if (mask and CallAudioState.ROUTE_SPEAKER != 0) add(CallAudioRoute.VIVA_VOZ)
    if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) add(CallAudioRoute.BLUETOOTH)
    if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) add(CallAudioRoute.FONE_DE_OUVIDO)
}

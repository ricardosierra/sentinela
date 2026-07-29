package org.sentinela.app.telecom

import android.telecom.CallScreeningService.CallResponse
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.settings.ScreeningSettings

/**
 * Traduz a decisao de dominio para a resposta que o Telecom entende.
 *
 * Esta e a unica classe do app autorizada a montar um `CallResponse`, e existem tres fatos
 * desconfortaveis que precisam ficar registrados aqui em vez de virarem promessa em outro lugar.
 *
 * Primeiro, pedir para pular o registro no historico nativo nao produz efeito para um aplicativo
 * de triagem escolhido pelo usuario: o proprio Android reserva esse pedido a outra categoria de
 * aplicativo. Nao e limitacao de fabricante e nao ha contorno legitimo. O campo continua sendo
 * enviado apenas como declaracao de intencao, e nenhuma tela, texto ou comentario pode afirmar ao
 * usuario que a chamada desaparece do historico do aparelho.
 *
 * Segundo, encaminhar a chamada para a caixa postal depende inteiramente da operadora. A API
 * oferece uma unica combinacao de recusa, e o destino final dela varia de linha para linha. Por
 * isso a decisao de encaminhar em silencio e traduzida exatamente como a recusa comum, e a caixa
 * postal nunca e prometida.
 *
 * Terceiro, combinar recusa com silenciamento e aceito pelo construtor, mas e enganoso: a
 * plataforma avalia a recusa primeiro e simplesmente ignora o silenciamento. Uma resposta assim
 * daria a impressao de duas protecoes quando so uma acontece, entao ela e proibida aqui — o
 * silenciamento sai sozinho, sem nenhum outro campo, porque qualquer acompanhante faz o
 * construtor real lancar.
 */
class CallResponseFactory {

    fun toResponse(decision: CallDecision, settings: ScreeningSettings): CallResponse =
        when (decision) {
            is CallDecision.Allow -> CallResponse.Builder().build()

            is CallDecision.Silence -> CallResponse.Builder()
                .setSilenceCall(true)
                .build()

            is CallDecision.Reject ->
                recusa(skipCallLog = settings.hideFromNativeCallLog)

            is CallDecision.SendSilentlyToVoicemail ->
                recusa(skipCallLog = settings.hideFromNativeCallLog)

            is CallDecision.BlockWithoutTrace ->
                recusa(skipCallLog = true)
        }

    /**
     * Unica forma de recusa emitida pelo app. `setDisallowCall(true)` e obrigatorio: sem ele o
     * construtor lanca ao ver qualquer um dos outros campos ligados.
     */
    private fun recusa(skipCallLog: Boolean): CallResponse = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(skipCallLog)
        .setSkipNotification(true)
        .build()
}

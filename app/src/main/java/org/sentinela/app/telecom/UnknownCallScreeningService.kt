package org.sentinela.app.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import org.sentinela.app.SentinelaApp
import org.sentinela.app.domain.CallDirection

/**
 * Ponto de entrada do sistema de telefonia. Camada fina de propósito: aqui só se monta a
 * chamada, se delega ao coordenador e se entrega ao sistema a resposta traduzida. Nenhuma
 * condição que decida o destino de uma chamada pode nascer neste arquivo — toda regra vive no
 * motor de decisão, e qualquer sinal contrário significa que algo foi parar no lugar errado.
 *
 * Um ponto que costumava estar escrito errado aqui: não é verdade que este método receba apenas
 * números fora da agenda. A plataforma dispensa a triagem de quem está na agenda somente quando
 * o aplicativo não tem como consultá-la, e este aplicativo passou a ter. Ou seja, chamadas de
 * pessoas conhecidas chegam até aqui, e a consulta à agenda é obrigatória: um resultado errado
 * barraria a ligação de alguém da lista do usuário, que é a pior falha possível deste produto.
 *
 * Invariantes desta classe (não relaxar):
 *  - a resposta ao sistema acontece exatamente uma vez em todos os caminhos;
 *  - ela sai muito antes do limite da plataforma, com folga de cinco vezes;
 *  - nenhuma consulta sai do aparelho;
 *  - histórico e notificação próprios só existem depois da resposta.
 */
class UnknownCallScreeningService : CallScreeningService() {

    /**
     * Costura de teste. Em produção fica nula e os colaboradores vêm do container único do
     * processo; a suíte a preenche com dublês para hospedar este serviço dentro da JVM.
     */
    internal var dependencies: ScreeningDependencies? = null

    override fun onScreenCall(callDetails: Call.Details) {
        val deps = dependencies ?: (application as SentinelaApp).container
        val chamada = deps.screenedCallFactory.from(callDetails)

        // Chamada de saída: quem responde é o próprio sistema, e o que sairia daqui seria
        // descartado. Sair antes de tudo evita gastar consulta local por nada.
        if (chamada.direction == CallDirection.OUTGOING) return

        // A triagem chega na thread principal e as consultas locais suspendem: o trabalho vai
        // para o escopo do processo, e a resposta ao sistema acontece dentro dele.
        deps.launchAfterResponse {
            deps.screeningCoordinator.screen(
                call = chamada,
                respond = { decisao, configuracoes ->
                    respondToCall(
                        callDetails,
                        deps.callResponseFactory.toResponse(decisao, configuracoes),
                    )
                },
                afterResponse = deps.postScreeningWork::run,
            )
        }
    }
}

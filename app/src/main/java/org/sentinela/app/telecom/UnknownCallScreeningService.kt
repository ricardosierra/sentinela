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
 * Sobre chamadas de saída, um ponto que precisa ficar escrito com precisão, porque a formulação
 * fácil ("responder sempre") está errada. Quem entrega a triagem a este método é um tratador de
 * mensagens da classe base da plataforma, e o código dele, lido na fonte do próprio sistema, faz o
 * seguinte: chama este método e, logo depois de ele retornar, se a chamada for de saída, envia
 * sozinho ao serviço de telefonia uma resposta vazia. A documentação da própria função de resposta
 * confirma o outro lado: pedidos feitos por ela são ignorados quando a chamada não é de entrada, e
 * o prazo de cinco segundos é cobrado apenas de chamadas de entrada. Ou seja, para uma chamada de
 * saída não existe prazo estourando, nem aviso, nem punição por não responder — responder é que
 * seria errado, porque somaria uma resposta descartada à resposta automática do sistema. Por isso o
 * retorno antecipado é o comportamento correto, e não um atalho.
 *
 * Invariantes desta classe (não relaxar):
 *  - em toda chamada de entrada a resposta ao sistema acontece exatamente uma vez, em todos os
 *    caminhos, inclusive nos de falha;
 *  - em chamada de saída este arquivo não responde nada, pela razão acima, e não interfere;
 *  - a resposta sai muito antes do limite da plataforma, com folga de cinco vezes;
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

        // Chamada de saída: a classe base responde por conta própria assim que este método
        // retorna, e o que saísse daqui seria descartado. Sair antes de tudo evita gastar
        // consulta local por nada. Justificativa completa no comentário da classe.
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

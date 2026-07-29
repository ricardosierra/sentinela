package org.sentinela.app.telecom

/**
 * O que o serviço de triagem precisa receber pronto para responder a uma chamada.
 *
 * Existe por dois motivos. O primeiro é de execução: o container do aplicativo é único no
 * processo, e o serviço precisa de um jeito de pedir colaboradores sem nunca construir um
 * segundo — duas instâncias sobre o mesmo arquivo de configurações derrubam o processo, o que
 * já foi medido. O segundo é de teste: com um contrato pequeno, a suíte hospeda o serviço de
 * verdade e entrega dublês, em vez de tentar montar a infraestrutura inteira dentro da JVM.
 *
 * Nada aqui decide o destino de uma chamada. Toda regra vive no motor de decisão.
 */
interface ScreeningDependencies {

    val screenedCallFactory: ScreenedCallFactory

    val callResponseFactory: CallResponseFactory

    val screeningCoordinator: ScreeningCoordinator

    val postScreeningWork: PostScreeningWork

    /**
     * Executa em segundo plano o trabalho da triagem e o que vem depois da resposta.
     *
     * A triagem chega na thread principal e as consultas locais suspendem; bloquear ali seria
     * bloquear a interface do telefone durante uma chamada. O escopo é o do processo, criado
     * uma única vez: um escopo novo a cada chamada vazaria, e agendador de trabalho em segundo
     * plano é proibido neste projeto por custo de partida a frio.
     */
    fun launchAfterResponse(block: suspend () -> Unit)
}

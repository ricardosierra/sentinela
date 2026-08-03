// Nome do arquivo em ingles, como o resto da camada de interface, e declaracao em pt-BR, como todo o
// vocabulario de dominio do projeto. A regra que cobra a coincidencia dos dois e desligada AQUI, no
// arquivo, em vez de afrouxada na configuracao compartilhada do analisador.
@file:Suppress("MatchingDeclarationName")

package org.sentinela.app.ui.navigation

/**
 * Rotas do grafo de navegacao do Sentinela.
 *
 * As rotas deste projeto sao TEXTO por medicao, nao por gosto. A alternativa tipada oferecida pela
 * biblioteca de navegacao — declarar o destino como objeto anotado e registra-lo pela versao
 * parametrizada da funcao de destino — compila limpa neste repositorio e estoura na primeira
 * composicao do grafo, com falha de serializacao. A causa foi verificada na classe compilada: o
 * compilador de Kotlin embutido na ferramenta de build nao traz o complemento de serializacao, e a
 * anotacao compila vazia, sem serializador algum. O compilador fica satisfeito e o aplicativo quebra
 * ao abrir a primeira tela.
 *
 * Consequencia de desenho, e nao apenas de estilo: o guarda-corpo desta decisao e um teste que
 * COMPOE o grafo de verdade e o navega, porque nenhum assert de compilacao pega esse defeito. Trocar
 * qualquer destino daqui pela forma anotada deixa a suite de contrato do grafo vermelha em EXECUCAO.
 *
 * Nenhuma tela desta fase recebe parametro de rota, por isso nao existe barra nem argumento em
 * nenhum valor. A contagem de destinos tambem esta travada por teste: tela nova exige revisao de
 * navegacao, porque a pilha e o "pular onboarding" dependem de quais telas existem.
 */
internal object Rotas {

    /** Tela 0 do fluxo, sem contador de passos. */
    const val BOAS_VINDAS = "boas_vindas"

    /** Passo 1 de 6 — pedido do papel de triagem ao sistema. */
    const val PASSO_PAPEL = "passo_papel"

    /** Passo 2 de 6 — politica para numeros desconhecidos. */
    const val PASSO_DESCONHECIDOS = "passo_desconhecidos"

    /** Passo 3 de 6 — politica para contatos da agenda, com o pedido de leitura da agenda. */
    const val PASSO_CONTATOS = "passo_contatos"

    /** Passo 4 de 6 — politica para a whitelist pessoal. */
    const val PASSO_WHITELIST = "passo_whitelist"

    /** Passo 5 de 6 — adesao opcional ao aviso proprio de bloqueio. */
    const val PASSO_NOTIFICACAO = "passo_notificacao"

    /** Passo 6 de 6 — verificacao final antes da home. */
    const val PASSO_RESUMO = "passo_resumo"

    /** Home do aplicativo, destino do fluxo concluido e tambem do "pular". */
    const val HOME = "home"

    /** Tela Protecao, onde todas as politicas continuam acessiveis depois do fluxo. */
    const val PROTECAO = "protecao"

    /** Ativacao do modo discador — opcional, fora do onboarding por decisao de produto. */
    const val MODO_DISCADOR = "modo_discador"

    /** Tela de Permissões / Whitelist Pessoal */
    const val WHITELIST = "whitelist"

    /** Tela do Histórico de Bloqueios */
    const val HISTORICO = "historico"
}

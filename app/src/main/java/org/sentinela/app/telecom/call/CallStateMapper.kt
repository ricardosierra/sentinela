package org.sentinela.app.telecom.call

/**
 * Códigos inteiros de estado que a telefonia entrega ao aplicativo, na numeração pública
 * estável da versão 35 do Android. São copiados como valores porque a tradução precisa ser
 * testável em máquina virtual pura, sem nenhum tipo da plataforma neste pacote.
 *
 * A numeração pública tem buracos (dois valores antigos hoje reservados). Eles caem no
 * ramo final por desenho.
 */
private const val CODIGO_NOVA: Int = 0
private const val CODIGO_DISCANDO: Int = 1
private const val CODIGO_TOCANDO_ENTRADA: Int = 2
private const val CODIGO_EM_ESPERA: Int = 3
private const val CODIGO_ATIVA: Int = 4
private const val CODIGO_DESCONECTADA: Int = 7
private const val CODIGO_ESCOLHA_DE_CHIP: Int = 8
private const val CODIGO_CONECTANDO: Int = 9
private const val CODIGO_DESCONECTANDO: Int = 10
private const val CODIGO_TRANSFERINDO_APARELHO: Int = 11
private const val CODIGO_PROCESSAMENTO_DE_AUDIO: Int = 12
private const val CODIGO_TOCANDO_SIMULADO: Int = 13

/**
 * Traduz o código inteiro de estado da telefonia num estado nomeado da interface.
 *
 * É um contrato funcional para que o teste possa injetar defeito na tradução sem reflexão
 * e sem biblioteca de simulação.
 */
fun interface CallStateMapper {
    fun map(rawState: Int): CallUiState
}

/**
 * Tradução exaustiva dos códigos de estado da telefonia.
 *
 * O ramo final **nunca** é vazio e nunca devolve ausência de estado. A pesquisa desta fase
 * mediu que o sistema de telefonia percebe o processo morto e assume a chamada com o
 * discador do aparelho, mas **não** percebe interface viva e congelada — nesse caso ninguém
 * substitui ninguém e o usuário fica olhando uma tela parada. Um código de estado que este
 * aplicativo não conhece precisa, portanto, aparecer na tela com nome e com o encerramento
 * habilitado, em vez de virar silêncio.
 */
class PlatformCallStateMapper : CallStateMapper {

    override fun map(rawState: Int): CallUiState = when (rawState) {
        CODIGO_NOVA, CODIGO_DISCANDO, CODIGO_CONECTANDO -> CallUiState.Dialing
        CODIGO_TOCANDO_ENTRADA -> CallUiState.Incoming
        CODIGO_TOCANDO_SIMULADO -> CallUiState.Ringing
        CODIGO_ATIVA -> CallUiState.Active
        CODIGO_DESCONECTADA, CODIGO_DESCONECTANDO -> CallUiState.Ended
        CODIGO_EM_ESPERA,
        CODIGO_ESCOLHA_DE_CHIP,
        CODIGO_TRANSFERINDO_APARELHO,
        CODIGO_PROCESSAMENTO_DE_AUDIO,
        -> CallUiState.Unsupported(rawState)
        else -> codigoNaoDocumentado(rawState)
    }

    /** Ramo final nomeado: estado visível, com o código bruto preservado para diagnóstico. */
    private fun codigoNaoDocumentado(rawState: Int): CallUiState = CallUiState.Unsupported(rawState)
}

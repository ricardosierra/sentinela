package org.sentinela.app.telecom.call

/**
 * Origem da chamada, espelho exato dos quatro chips do contrato de design.
 *
 * Vive no domínio da chamada e não carrega nenhum dado da agenda: quem resolve a
 * identidade é a camada de interface, em memória, no instante da chamada.
 */
enum class CallOrigin {
    CONTATO,
    PERMITIDO,
    DESCONHECIDO,
    PRIVADO,
}

/**
 * Rota de áudio da chamada. Existe para que a disponibilidade do viva-voz seja um dado
 * de domínio, e não uma consulta à plataforma feita de dentro da interface.
 */
enum class CallAudioRoute {
    FONE,
    VIVA_VOZ,
    BLUETOOTH,
    FONE_DE_OUVIDO,
}

/**
 * Identidade apresentável da chamada.
 *
 * O número aqui é completo de propósito: na tela de chamada o número é o produto e o
 * usuário precisa dele inteiro para decidir se atende. A máscara continua obrigatória em
 * registro de execução, notificação, histórico e relatório de falha — a fronteira é a
 * tela, não este objeto.
 */
data class CallIdentity(
    val displayName: String? = null,
    val fullNumber: String? = null,
    val origin: CallOrigin = CallOrigin.DESCONHECIDO,
)

/**
 * Estado nomeado da chamada, sem nenhum tipo da plataforma.
 *
 * Não existe estado nulo e não existe estado anônimo: todo código de estado que a
 * telefonia entregar chega aqui com nome. A pesquisa desta fase mediu que o sistema de
 * telefonia **não** detecta interface viva e congelada e, nesse caso, não passa a chamada
 * para o discador do aparelho — enquanto o processo morto **é** detectado e substituído.
 * Logo, tela em branco por estado não previsto é a pior falha possível da fase, e por isso
 * até o código desconhecido produz um estado visível com o encerramento habilitado.
 */
sealed interface CallUiState {

    /** O encerramento continua disponível ao usuário neste estado? */
    val hangUpEnabled: Boolean

    /** Chamada de entrada tocando: os dois botões da tela cheia valem aqui. */
    data object Incoming : CallUiState {
        override val hangUpEnabled: Boolean = false
    }

    /** Chamada de saída sendo estabelecida. */
    data object Dialing : CallUiState {
        override val hangUpEnabled: Boolean = true
    }

    /** Chamada de saída já tocando do outro lado. */
    data object Ringing : CallUiState {
        override val hangUpEnabled: Boolean = true
    }

    /** Chamada em curso, com áudio. */
    data object Active : CallUiState {
        override val hangUpEnabled: Boolean = true
    }

    /** Chamada encerrada ou encerrando. */
    data object Ended : CallUiState {
        override val hangUpEnabled: Boolean = false
    }

    /** Falha na sessão de chamada; a tela diz o que houve em vez de ficar vazia. */
    data object Failed : CallUiState {
        override val hangUpEnabled: Boolean = false
    }

    /**
     * Estado que esta versão do aplicativo não desenha (espera, escolha de chip,
     * processamento de áudio, transferência entre aparelhos) ou código não documentado.
     *
     * Carrega o código bruto recebido para diagnóstico — é um inteiro da plataforma, sem
     * nenhum dado pessoal — e mantém o encerramento habilitado, para que o usuário nunca
     * fique preso numa tela sem saída.
     */
    data class Unsupported(val rawState: Int) : CallUiState {
        override val hangUpEnabled: Boolean = true
    }
}

/**
 * Retrato completo da sessão para a interface: o estado nomeado mais os dados de
 * apresentação e de controle.
 */
data class CallSnapshot(
    val state: CallUiState = CallUiState.Ended,
    val identity: CallIdentity = CallIdentity(),
    val muted: Boolean = false,
    val speakerOn: Boolean = false,
    val speakerAvailable: Boolean = true,
    val keypadOpen: Boolean = false,
    val startedAtMillis: Long? = null,
    val sentDigits: String = "",
) {
    val hangUpEnabled: Boolean get() = state.hangUpEnabled
}

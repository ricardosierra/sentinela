package org.sentinela.app.telecom.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tabela ESCRITA A MÃO: cada linha foi digitada a partir da numeração pública da plataforma
 * e do contrato de rótulos da interface, jamais derivada da própria tradução. Tabela derivada
 * do código sob teste concorda com qualquer defeito que o código tenha.
 */
@RunWith(Parameterized::class)
class CallStateMapperTest(
    private val descricao: String,
    private val codigo: Int,
    private val esperado: CallUiState,
) {

    private val mapper = PlatformCallStateMapper()

    @Test
    fun `codigo de estado produz o estado nomeado esperado`() {
        val obtido = mapper.map(codigo)
        assertEquals(descricao, esperado, obtido)
    }

    @Test
    fun `nenhum codigo de estado deixa o usuario sem estado`() {
        val obtido: CallUiState? = mapper.map(codigo)
        assertTrue(descricao, obtido != null)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = listOf(
            arrayOf("nova (0) vira discando", 0, CallUiState.Dialing),
            arrayOf("discando (1) vira discando", 1, CallUiState.Dialing),
            arrayOf("tocando de entrada (2) vira chamada recebida", 2, CallUiState.Incoming),
            arrayOf("em espera (3) vira nao suportado", 3, CallUiState.Unsupported(3)),
            arrayOf("ativa (4) vira em chamada", 4, CallUiState.Active),
            arrayOf("reservado (5) vira nao suportado", 5, CallUiState.Unsupported(5)),
            arrayOf("reservado (6) vira nao suportado", 6, CallUiState.Unsupported(6)),
            arrayOf("desconectada (7) vira encerrada", 7, CallUiState.Ended),
            arrayOf("escolha de chip (8) vira nao suportado", 8, CallUiState.Unsupported(8)),
            arrayOf("conectando (9) vira discando", 9, CallUiState.Dialing),
            arrayOf("desconectando (10) vira encerrada", 10, CallUiState.Ended),
            arrayOf("transferindo aparelho (11) vira nao suportado", 11, CallUiState.Unsupported(11)),
            arrayOf(
                "processamento de audio (12) vira nao suportado",
                12,
                CallUiState.Unsupported(12),
            ),
            arrayOf("tocando simulado (13) vira tocando", 13, CallUiState.Ringing),
            arrayOf("codigo nao documentado (99) vira nao suportado", 99, CallUiState.Unsupported(99)),
            arrayOf("codigo negativo (-1) vira nao suportado", -1, CallUiState.Unsupported(-1)),
        )
    }
}

/**
 * Regras que valem para o conjunto todo, e não para uma linha da tabela.
 */
class CallStateMapperContractTest {

    private val mapper = PlatformCallStateMapper()

    @Test
    fun `estado nao suportado carrega o codigo bruto recebido`() {
        val obtido = mapper.map(12) as CallUiState.Unsupported
        assertEquals(12, obtido.rawState)
    }

    @Test
    fun `estado nao suportado mantem o encerramento habilitado`() {
        listOf(3, 8, 11, 12, 99).forEach { codigo ->
            assertTrue("codigo $codigo", mapper.map(codigo).hangUpEnabled)
        }
    }

    @Test
    fun `nenhum codigo numa faixa larga produz ausencia de estado`() {
        (-50..200).forEach { codigo ->
            assertTrue("codigo $codigo", mapper.map(codigo) as CallUiState? != null)
        }
    }
}

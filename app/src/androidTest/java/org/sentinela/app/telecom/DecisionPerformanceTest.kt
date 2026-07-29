package org.sentinela.app.telecom

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp
import org.sentinela.app.data.contacts.ContactsTestFixture
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.domain.blocksCall

/**
 * SCR-11: quanto custa, do inicio da triagem ate a resposta sair, o caminho mais longo que este
 * aplicativo sabe percorrer — configuracoes, agenda, whitelist, historico recente, motor de
 * decisao e traducao da resposta, tudo com o container real do processo.
 *
 * **Por que so a mediana quebra o build.** Esta e a terceira fase a medir tempo em aparelho
 * virtual e a terceira a chegar na mesma conclusao. A Fase 3 precisou tirar um percentil de cauda
 * do conjunto de assercoes depois de ve-lo falhar em cerca de uma execucao em cinco, sem nenhuma
 * regressao real por tras. A Fase 4 mediu o mesmo trecho de codigo custar trinta milissegundos numa
 * execucao e cento e quarenta na seguinte, sem uma linha alterada entre as duas. O que a cauda mede
 * num emulador e o agendador do computador que o hospeda, tanto quanto o codigo do aplicativo; um
 * limite ali nao protege o produto, so ensina a equipe a reexecutar o teste ate ficar verde, que e
 * exatamente como um teste deixa de valer alguma coisa. A cauda continua sendo medida e registrada
 * a cada execucao, e o veredito sobre ela pertence a validacao em aparelho fisico da Fase 9 —
 * mudou de lugar, nao foi afrouxada.
 *
 * **O que este arquivo nao prova.** Cronometro nao e prova de estrutura. Nada aqui afirma que uma
 * consulta aconteceu, que o cache foi usado ou que a decisao veio do motor; um teste de tempo
 * continuaria verde com metade do caminho arrancada. Quem garante estrutura sao os testes de
 * comportamento em JVM, a contagem de consultas ao provider da Fase 4 e o script de invariantes.
 * A unica afirmacao estrutural feita aqui e sobre o que foi medido: se a decisao medida nao tivesse
 * barrado a chamada, o caminho cronometrado seria mais curto que o pretendido e o numero mediria
 * outra coisa.
 */
@RunWith(AndroidJUnit4::class)
class DecisionPerformanceTest {

    @Test
    fun caminhoDeDecisaoCabeNoOrcamentoComFolga() {
        val frio = medirUmaTriagem()
        Log.i(TAG, "SENTINELA|decision|frio|ms=${frio.millis}")

        repeat(WARMUP) { medirUmaTriagem() }

        val amostras = DoubleArray(SAMPLES) { medirUmaTriagem().millis }
        amostras.sort()
        val p50 = amostras[SAMPLES / 2]
        val cauda95 = amostras[(SAMPLES * FATOR_CAUDA).toInt()]
        val maximo = amostras.last()

        Log.i(
            TAG,
            "SENTINELA|decision|aquecido|amostras=$SAMPLES" +
                "|p50=$p50|p95=$cauda95|max=$maximo",
        )

        assertTrue(
            "p50=$p50 ms — sinal estavel do caminho de decisao, esperado < $P50_MAX_MS ms " +
                "(orcamento declarado do produto: $ORCAMENTO_MS ms)",
            p50 < P50_MAX_MS,
        )
    }

    /**
     * A triagem medida precisa ser a que barra a chamada: e o caminho mais longo, o unico que
     * percorre as quatro consultas locais ate o fim antes de responder. Sem esta verificacao o
     * numero acima poderia estar cronometrando uma decisao curta e ninguem notaria.
     */
    @Test
    fun aTriagemMedidaEADeCaminhoMaisLongo() {
        val medida = medirUmaTriagem()

        assertNotNull("a triagem medida nao produziu decisao alguma", medida.decision)
        assertTrue(
            "a triagem medida devolveu ${medida.decision} — o caminho cronometrado precisa ser " +
                "o de bloqueio, que percorre as consultas locais ate o fim",
            medida.decision?.blocksCall == true,
        )
        assertNotNull("a decisao medida nao foi traduzida em resposta ao sistema", medida.respondeu)
    }

    private data class Medida(
        val millis: Double,
        val decision: CallDecision?,
        val respondeu: Any?,
    )

    private fun medirUmaTriagem(): Medida {
        var millis = 0.0
        var decisao: CallDecision? = null
        var resposta: Any? = null

        val inicio = System.nanoTime()
        runBlocking {
            container.screeningCoordinator.screen(
                call = CHAMADA,
                respond = { d, configuracoes ->
                    resposta = container.callResponseFactory.toResponse(d, configuracoes)
                    millis = (System.nanoTime() - inicio) / NANOS_POR_MS
                    decisao = d
                },
            )
        }
        return Medida(millis, decisao, resposta)
    }

    companion object {
        const val TAG = "SentinelaDecision"
        const val WARMUP = 20
        const val SAMPLES = 100
        const val NANOS_POR_MS = 1_000_000.0
        const val FATOR_CAUDA = 0.95

        /** Orcamento declarado do produto para o caminho de decisao. */
        const val ORCAMENTO_MS = 200.0

        /** Medido 23,3 ms na pesquisa desta fase, no mesmo aparelho virtual: folga de 4x. */
        const val P50_MAX_MS = 50.0

        /** Numero fora da agenda e fora da whitelist: politica padrao manda barrar. */
        val CHAMADA = ScreenedCall(
            direction = CallDirection.INCOMING,
            number = ScreenedNumber.Valid("+5511977776666"),
        )

        internal val container get() =
            ApplicationProvider.getApplicationContext<SentinelaApp>().container

        /**
         * A consulta a agenda sem permissao devolve indisponivel e cai na politica de reserva, que
         * deixa passar — outro caminho, mais curto. Adotar a identidade de shell da instrumentacao
         * e a unica rota que concede a leitura aqui, como ja registrado na Fase 4.
         */
        @BeforeClass
        @JvmStatic
        fun concederLeituraDaAgenda() {
            ContactsTestFixture.adoptShell()
        }

        @AfterClass
        @JvmStatic
        fun devolverIdentidade() {
            ContactsTestFixture.dropShell()
        }
    }
}

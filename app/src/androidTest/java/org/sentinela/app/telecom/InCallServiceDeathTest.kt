package org.sentinela.app.telecom

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp

/**
 * Chamada de saida de verdade no aparelho virtual, e o contorno da prova de que morrer no meio de
 * uma chamada e seguro.
 *
 * **O que a pesquisa mediu, e que este arquivo NAO reproduz de dentro do processo.** Com o
 * aplicativo como telefone padrao e uma chamada ativa, encerrar o processo do aplicativo **nao
 * derrubou a chamada**: o sistema de telefonia detectou a desconexao do nosso servico, religou
 * sozinho no discador que vem no aparelho e as chamadas em curso continuaram. Morrer e seguro. O
 * modo de falha realmente perigoso e o oposto — ficar vivo com a interface travada, porque disso o
 * sistema nao tem como perceber e ninguem substitui ninguem; esse caminho esta coberto pelo prazo
 * de apresentacao provado em maquina virtual no plano 06-01.
 *
 * A assercao correta e sobre a sobrevivencia da CHAMADA, nunca sobre a sobrevivencia do nosso
 * processo. E por isso mesmo ela nao cabe aqui: a instrumentacao roda dentro do processo que precisa
 * morrer. Um teste que se encerrasse a si mesmo terminaria em execucao interrompida, sem assercao
 * alguma — foi medido neste plano, e vale igualmente para a perda de papel do sistema. A prova vive
 * em `scripts/verify-dialer-lifecycle.sh`, dirigida pelo computador, que origina a chamada, espera o
 * estado em curso, encerra o processo, confere no diagnostico do sistema de telefonia que a chamada
 * CONTINUA, confere a religacao no discador de fabrica e confere que a chamada seguinte volta a ser
 * entregue ao aplicativo.
 *
 * O que este arquivo prova, e que aquele script pressupoe: que uma chamada de saida de verdade pode
 * ser originada neste aparelho, que ela chega ao sistema de telefonia com o aplicativo como telefone
 * padrao, e que a limpeza de chamadas presas devolve o aparelho ao estado neutro — sem ela, cada
 * execucao contaminaria a seguinte com uma ligacao pendurada.
 */
@RunWith(AndroidJUnit4::class)
class InCallServiceDeathTest {

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()

    @Before
    fun garantirPapelEAparelhoLimpo() {
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, app.packageName)
        TelecomShell.cleanupStuckCalls()
    }

    /** Roda dando certo ou dando errado: chamada pendurada contamina toda execucao seguinte. */
    @After
    fun limparChamadasPresas() {
        TelecomShell.cleanupStuckCalls()
    }

    @Test
    fun chamadaDeSaidaDeVerdadeChegaAoSistemaDeTelefonia() {
        TelecomShell.placeOutgoingCall(NUMERO_DE_TESTE)

        assertTrue(
            "nenhuma chamada em curso apareceu no diagnostico do sistema de telefonia depois de " +
                "originar uma chamada de saida — sem isso o cenario da morte no meio da chamada " +
                "nem pode ser montado",
            esperarChamadaEmCurso(),
        )
    }

    @Test
    fun oAplicativoEOTelefonePadraoQuandoAChamadaSai() {
        TelecomShell.placeOutgoingCall(NUMERO_DE_TESTE)
        esperarChamadaEmCurso()

        assertTrue(
            "a chamada saiu sem o aplicativo deter o papel de telefone padrao — a interface de " +
                "chamada propria nao seria acionada e o cenario mediria o discador de fabrica",
            DialerRoleManager(app).isRoleHeld(),
        )
    }

    @Test
    fun limparChamadasPresasDevolveOAparelhoAoEstadoNeutro() {
        TelecomShell.placeOutgoingCall(NUMERO_DE_TESTE)
        esperarChamadaEmCurso()

        TelecomShell.cleanupStuckCalls()

        assertFalse(
            "sobrou chamada em curso apos a limpeza — a proxima execucao herdaria uma ligacao " +
                "pendurada e mediria o lixo da anterior",
            esperarChamadaEmCurso(tentativas = 3),
        )
    }

    /**
     * Trava a forma do comando de limpeza. Ele e o comando verificado na pesquisa desta fase; um
     * comando diferente deixaria chamadas penduradas e cada execucao mediria o lixo da anterior.
     */
    @Test
    fun aLimpezaUsaOComandoVerificadoDoSistemaDeTelefonia() {
        assertEquals(
            "a forma do comando de limpeza de chamadas presas mudou",
            "telecom cleanup-stuck-calls",
            TelecomShell.CLEANUP_STUCK_CALLS_COMMAND,
        )
    }

    private fun esperarChamadaEmCurso(tentativas: Int = TENTATIVAS): Boolean {
        repeat(tentativas) {
            val diagnostico = TelecomShell.telecomDump()
            if (ESTADOS_EM_CURSO.any { diagnostico.contains(it) }) return true
            Thread.sleep(INTERVALO_MILLIS)
        }
        return false
    }

    private companion object {
        const val NUMERO_DE_TESTE = "5551234"
        const val TENTATIVAS = 20
        const val INTERVALO_MILLIS = 1_000L
        val ESTADOS_EM_CURSO = listOf("state=ACTIVE", "state=DIALING", "state=CONNECTING")
    }
}

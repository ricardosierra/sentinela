package org.sentinela.app.telecom

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp

/**
 * O aparelho aceita o Sentinela como telefone padrao? Esta e a pergunta de elegibilidade do
 * requisito DIA-01, e ela nao tem resposta em maquina virtual: quem decide e o servico de papeis
 * do sistema, lendo o manifesto instalado.
 *
 * **O achado central da pesquisa desta fase, que este arquivo protege.** Com apenas a tela de
 * discagem declarada, o pedido do papel FALHOU: o sistema devolveu excecao e o papel permaneceu
 * com o discador que vem no aparelho. Declarando tambem o servico de interface de chamada, o
 * mesmo pedido PASSOU. Nao foi deducao a partir de documentacao: foram duas construcoes, duas
 * instalacoes e dois resultados. Ou seja, o servico de interface de chamada faz parte da
 * qualificacao verificada pelo sistema, e nao de uma recomendacao. Os dois filtros de discagem —
 * o de esquema vazio e o de esquema de telefone — participam da mesma qualificacao, porque a
 * listagem de discadores instalados aplica os dois em sequencia.
 *
 * **Por que o caminho usado aqui e o unico que serve de prova.** Existe um comando de
 * configuracao de telefonia capaz de apontar o discador padrao para um pacote qualquer, e ele
 * funciona no aparelho virtual. Ele **bypassa a qualificacao**: um aplicativo inelegivel tambem
 * seria aceito por ele. Usa-lo aqui produziria um teste que fica verde mesmo com o manifesto
 * quebrado, exatamente a classe de falso-verde que este projeto ja pagou tres vezes. Este arquivo
 * usa somente o caminho de concessao de papel, que verifica a elegibilidade, e por isso o nome
 * daquele outro comando nao aparece em lugar algum deste arquivo — nem em prosa.
 *
 * **Por que nenhum teste daqui DEVOLVE o papel.** Medido neste plano: quando o aplicativo perde um
 * papel do sistema, a plataforma revoga as permissoes que vinham com ele e ENCERRA o processo do
 * aplicativo — o proprio sistema registra o encerramento como mudanca de permissao. A
 * instrumentacao roda dentro desse processo. Um teste que devolvesse o proprio papel morreria
 * junto com o que quer observar, e o resultado seria uma execucao interrompida, nunca uma
 * assercao. A prova da reversao pertence, por isso, a quem esta fora do processo:
 * `scripts/verify-dialer-lifecycle.sh` executa o ciclo completo — conceder, usar, devolver — com
 * codigos de saida de verdade, dirigido pelo computador. O encerramento no encerramento deste
 * arquivo, portanto, so age quando ha o que restaurar sem provocar a propria morte.
 */
@RunWith(AndroidJUnit4::class)
class DialerRoleEligibilityTest {

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()
    private val pacote: String get() = app.packageName

    private var detentorOriginal: String = ""

    @Before
    fun capturarDetentorOriginal() {
        detentorOriginal = TelecomShell.roleHolders(TelecomShell.ROLE_DIALER)
    }

    /**
     * Roda dando certo ou dando errado. Restaura o detentor original **apenas** quando o
     * aplicativo nao ficou com o papel: devolve-lo aqui encerraria o processo da instrumentacao no
     * meio da suite, pelo motivo registrado na documentacao desta classe. Deixar o aparelho virtual
     * com o aplicativo como telefone padrao e inofensivo — nenhuma outra suite depende de quem
     * detem o papel, e o aparelho virtual e destruido ao fim da execucao.
     */
    @After
    fun restaurarDetentorOriginalQuandoPossivel() {
        if (TelecomShell.holds(TelecomShell.ROLE_DIALER, pacote)) return
        if (detentorOriginal.isNotBlank()) {
            TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, detentorOriginal)
        }
    }

    @Test
    fun aparelhoOfereceOPapelDeTelefonePadraoEAlguemODetem() {
        assertTrue(
            "o aparelho nao oferece o papel de telefone padrao — sem telefonia o modo discador " +
                "nao existe e o cenario desta suite nao vale",
            DialerRoleManager(app).isRoleAvailable(),
        )
        assertTrue(
            "o aparelho nao reportou nenhum detentor do papel de telefone padrao",
            detentorOriginal.isNotBlank(),
        )
    }

    @Test
    fun concessaoQueVerificaElegibilidadeTerminaSemErro() {
        val resultado = TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, pacote)

        assertTrue(
            "a concessao do papel de telefone padrao falhou: ${resultado.error.trim()} — o " +
                "manifesto perdeu o servico de interface de chamada ou um dos dois filtros de " +
                "discagem, e a elegibilidade depende dos tres",
            resultado.succeeded,
        )
    }

    @Test
    fun depoisDaConcessaoOAplicativoEODetentorDoPapel() {
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, pacote)

        assertEquals(
            "a consulta de detentores nao devolveu o pacote do aplicativo depois de uma " +
                "concessao aceita",
            pacote,
            TelecomShell.roleHolders(TelecomShell.ROLE_DIALER),
        )
    }

    @Test
    fun oAplicativoConcordaComOSistemaSobreDeterOPapel() {
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, pacote)

        assertTrue(
            "o sistema diz que o aplicativo detem o papel, mas a consulta do proprio aplicativo " +
                "diz o contrario — a interface mostraria um estado que o aparelho nao tem",
            DialerRoleManager(app).isRoleHeld(),
        )
    }

    /**
     * Trava a forma exata do comando usado para conceder o papel. E o comando de concessao de
     * papel, aquele que roda a verificacao de elegibilidade — nunca o atalho de configuracao de
     * telefonia, que aceitaria um aplicativo inelegivel e deixaria esta suite verde com o
     * manifesto quebrado.
     */
    @Test
    fun aConcessaoUsaOComandoQueVerificaElegibilidade() {
        assertEquals(
            "a forma do comando de concessao mudou — se ele deixar de ser o comando de papel, " +
                "esta suite para de provar elegibilidade",
            "cmd role add-role-holder ${TelecomShell.ROLE_DIALER} $pacote",
            TelecomShell.addRoleHolderCommand(TelecomShell.ROLE_DIALER, pacote),
        )
    }

    /**
     * Os dois papeis do produto sao independentes e convivem: e isso que sustenta a triagem no
     * modo discador e, do outro lado, a sobrevivencia do modo filtro quando o usuario devolve o
     * telefone padrao.
     */
    @Test
    fun oPapelDeTriagemEODeTelefonePadraoConvivemNoMesmoAplicativo() {
        TelecomShell.addRoleHolder(TelecomShell.ROLE_SCREENING, pacote)
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, pacote)

        assertTrue(
            "o aplicativo nao detem o papel de triagem junto do de telefone padrao",
            TelecomShell.holds(TelecomShell.ROLE_SCREENING, pacote),
        )
        assertTrue(
            "o aplicativo nao detem o papel de telefone padrao junto do de triagem",
            TelecomShell.holds(TelecomShell.ROLE_DIALER, pacote),
        )
    }
}

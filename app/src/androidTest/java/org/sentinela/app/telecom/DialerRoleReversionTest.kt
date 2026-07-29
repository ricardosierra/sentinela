package org.sentinela.app.telecom

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.telecom.call.DialerModeState
import org.sentinela.app.telecom.call.dialerModeState

/**
 * Reversao do modo discador: devolver o telefone padrao nunca pode quebrar a telefonia nem o modo
 * filtro (DIA-05).
 *
 * **A medicao que define a forma deste arquivo.** Quando o aplicativo PERDE um papel do sistema, a
 * plataforma revoga as permissoes que vinham com o papel e ENCERRA o processo do aplicativo; o
 * proprio sistema registra o encerramento como mudanca de permissao. Isso vale tanto para o papel
 * de telefone padrao quanto para o de triagem, e vale igual quando o usuario faz a troca nas
 * configuracoes do sistema. Como a instrumentacao roda DENTRO do processo do aplicativo, um teste
 * que devolvesse o proprio papel morreria junto com o que quer observar — o resultado seria uma
 * execucao interrompida, jamais uma assercao. Por isso a metade da reversao que exige a perda do
 * papel e provada de FORA do processo, por `scripts/verify-dialer-lifecycle.sh`, que executa o
 * ciclo completo com codigos de saida de verdade: conceder, usar, matar no meio da chamada,
 * devolver, e conferir que o discador de fabrica volta, que o papel de triagem sobrevive e que a
 * plataforma encerrou o aplicativo.
 *
 * **E ela nao e um contratempo: ela e o argumento do desenho.** Ser encerrado ao perder o papel e
 * exatamente o motivo pelo qual o estado do modo discador e DERIVADO de perguntas ao sistema e
 * nunca de valor gravado — o aplicativo sempre volta em processo novo, e um valor gravado dizendo
 * "modo discador ligado" seria mentira desde o primeiro instante. E e tambem o motivo pelo qual
 * desligar o modo desabilitando componente proprio e proibido para sempre.
 *
 * O que sobra para este arquivo e tudo o que e observavel sem a perda do papel, e nao e pouco: a
 * independencia dos dois papeis, a precedencia do papel detido sobre a intencao gravada, a triagem
 * que decide o mesmo com e sem o papel, e a reversao ser um convite ao seletor do sistema em vez de
 * uma troca forcada.
 */
@RunWith(AndroidJUnit4::class)
class DialerRoleReversionTest {

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()
    private val roles: DialerRoleManager get() = DialerRoleManager(app)

    @Before
    fun garantirOsDoisPapeis() {
        TelecomShell.addRoleHolder(TelecomShell.ROLE_SCREENING, app.packageName)
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, app.packageName)
    }

    @Test
    fun osDoisPapeisSaoIndependentesEConvivem() {
        assertTrue(
            "o aplicativo nao detem o papel de telefone padrao",
            TelecomShell.holds(TelecomShell.ROLE_DIALER, app.packageName),
        )
        assertTrue(
            "o papel de triagem nao esta com o aplicativo enquanto o de telefone padrao esta — a " +
                "independencia dos dois e o que faz o modo filtro sobreviver a reversao",
            TelecomShell.holds(TelecomShell.ROLE_SCREENING, app.packageName),
        )
    }

    @Test
    fun comOPapelDetidoOEstadoDoModoEAtivoMesmoSemIntencaoGravada() {
        assertEquals(
            "com o papel detido, o estado do modo tinha de ser ativo sem depender de nenhuma " +
                "intencao gravada",
            DialerModeState.ACTIVE,
            dialerModeState(
                roleAvailable = roles.isRoleAvailable(),
                roleHeld = roles.isRoleHeld(),
                contactsGranted = true,
                userOptedIn = false,
            ),
        )
    }

    @Test
    fun semOPapelOEstadoDeixaDeSerAtivoAindaQueAIntencaoGravadaDigaOContrario() {
        val estado = dialerModeState(
            roleAvailable = roles.isRoleAvailable(),
            roleHeld = false,
            contactsGranted = true,
            userOptedIn = true,
        )

        assertNotEquals(
            "o estado continuou ativo sem o papel — a interface mostraria um modo que o aparelho " +
                "nao tem, e e exatamente isso que um valor gravado como fonte da verdade causaria",
            DialerModeState.ACTIVE,
            estado,
        )
        assertEquals(
            "perder o papel depois de ter optado pelo modo precisa aparecer como aviso, nunca " +
                "como erro nem como modo ativo",
            DialerModeState.ROLE_LOST,
            estado,
        )
    }

    @Test
    fun aTriagemBarraDesconhecidoENaoDependeDoPapelDeTelefonePadrao() {
        val decisao = runBlocking {
            var produzida: CallDecision? = null
            app.container.screeningCoordinator.screen(
                call = ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Valid(DESCONHECIDO_E164)),
                respond = { d, _ -> produzida = d },
            )
            produzida
        }

        assertEquals(
            "a triagem deixou de barrar desconhecido — a decisao nao muda com o papel de telefone " +
                "padrao, e por isso devolver o papel nao pode desligar o modo filtro",
            DecisionReason.UNKNOWN_NUMBER,
            decisao?.reason,
        )
    }

    @Test
    fun reverterEAbrirOSeletorDoSistemaENuncaForcarATroca() {
        assertEquals(
            "a reversao deixou de ser a tela de escolha de aplicativos padrao do sistema — nao " +
                "existe API publica para um aplicativo remover o proprio papel, e desabilitar " +
                "componente proprio faria a plataforma encerrar o aplicativo na mao do usuario",
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            roles.buildRevertIntent().action,
        )
    }

    @Test
    fun oPedidoDoPapelSoExisteQuandoOAparelhoOOferece() {
        assertTrue("o aparelho nao oferece o papel de telefone padrao", roles.isRoleAvailable())
        assertTrue(
            "o aparelho oferece o papel mas nao houve intencao de pedido",
            roles.buildRequestIntent() != null,
        )
    }

    private companion object {
        const val DESCONHECIDO_E164 = "+5511912345678"
    }
}

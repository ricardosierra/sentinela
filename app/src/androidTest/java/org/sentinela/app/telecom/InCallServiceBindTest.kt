package org.sentinela.app.telecom

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp

/**
 * Prova, no aparelho virtual, do vinculo ao servico de interface de chamada — irmao exato do
 * teste de vinculo da triagem, escrito na Fase 5, e pelo mesmo motivo.
 *
 * O vinculo depende de tres coisas que moram fora do Kotlin e que nenhum compilador confere: a
 * declaracao do componente no manifesto, a exportacao dele e a permissao que restringe quem pode
 * se ligar. Um teste em maquina virtual pura jamais notaria a remocao de qualquer uma das tres;
 * este notaria, porque pede o vinculo de verdade e espera receber de volta um canal de
 * comunicacao. E essa declaracao nao e detalhe de acabamento: a pesquisa desta fase mediu que, sem
 * o servico de interface de chamada declarado, o proprio pedido do papel de telefone padrao e
 * NEGADO pelo sistema.
 *
 * **O que este arquivo deliberadamente NAO faz: exercitar uma chamada.** O canal devolvido pelo
 * vinculo so aceita objetos de chamada que a plataforma monta internamente e nao oferece a
 * aplicativo nenhum; forja-los exigiria falsificar tipo interno do sistema. A maquina de estado,
 * o prazo de apresentacao, mudo, viva-voz e o pareamento do tom de teclado sao cobertos pelo
 * nucleo puro do plano 06-01, em maquina virtual, onde cada evento e observavel em ordem. Este
 * arquivo cobre exatamente o que aquele nao alcanca.
 *
 * **A segunda prova e a unicidade do conjunto de colaboradores.** O processo tem um unico
 * conjunto, e isso e contrato de execucao: um segundo criaria um segundo armazenamento de
 * preferencias sobre o mesmo arquivo, e o runtime derruba o processo quando isso acontece. Aqui a
 * exigencia e ainda mais direta que na Fase 5 — o servico de chamada e a tela de chamada precisam
 * olhar para o MESMO armazem de sessao, senao a tela mostra uma ligacao e o usuario comanda outra.
 * Por isso nenhum teste deste arquivo constroi colaborador proprio: se o fizesse, provaria o
 * contrario do que quer provar.
 */
@RunWith(AndroidJUnit4::class)
class InCallServiceBindTest {

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule()

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()

    @Test
    fun sistemaConsegueSeVincularAoServicoDeInterfaceDeChamada() {
        val binder = serviceRule.bindService(
            Intent(app, SentinelaInCallService::class.java),
        )

        assertNotNull(
            "vinculo ao servico de interface de chamada devolveu canal nulo — declaracao, " +
                "exportacao ou permissao quebrada no manifesto, e sem isso o papel de telefone " +
                "padrao tambem e negado",
            binder,
        )
    }

    @Test
    fun armazemDaSessaoDeChamadaEOMesmoObjetoEmDuasLeituras() {
        val primeira = app.container.callSessionStore
        val segunda = app.container.callSessionStore

        assertSame(
            "duas leituras devolveram armazens de sessao diferentes — a tela de chamada " +
                "mostraria uma ligacao e o usuario comandaria outra",
            primeira,
            segunda,
        )
    }

    @Test
    fun conjuntoDeColaboradoresEOMesmoObjetoEmDuasLeituras() {
        val primeira = app.container
        val segunda = app.container

        assertSame(
            "duas leituras devolveram conjuntos de colaboradores diferentes — um segundo " +
                "armazenamento de preferencias sobre o mesmo arquivo derruba o processo",
            primeira,
            segunda,
        )
    }

    @Test
    fun servicoDeChamadaEDeTriagemConvivemNoMesmoProcesso() {
        val chamada = serviceRule.bindService(Intent(app, SentinelaInCallService::class.java))
        val triagem = serviceRule.bindService(
            Intent(app, UnknownCallScreeningService::class.java),
        )

        assertNotNull("vinculo ao servico de interface de chamada devolveu canal nulo", chamada)
        assertNotNull(
            "vinculo ao servico de triagem devolveu canal nulo com o servico de chamada tambem " +
                "vinculado — os dois papeis conviverem e o que sustenta a triagem no modo discador",
            triagem,
        )
    }
}

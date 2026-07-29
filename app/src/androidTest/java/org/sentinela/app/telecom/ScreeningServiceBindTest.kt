package org.sentinela.app.telecom

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp

/**
 * Prova, no aparelho virtual, das duas condicoes sem as quais nada mais desta fase funciona.
 *
 * **A primeira e o vinculo.** O sistema de telefonia so consegue triar uma chamada se conseguir
 * se ligar ao servico declarado pelo aplicativo. Esse vinculo depende de tres coisas que moram
 * fora do Kotlin — a declaracao no manifesto, a exportacao do componente e a permissao que
 * restringe quem pode se ligar — e nenhuma delas e verificada pelo compilador. Um teste em JVM
 * pura jamais notaria a remocao da declaracao; este notaria, porque pede o vinculo de verdade e
 * espera receber de volta um canal de comunicacao.
 *
 * **A segunda e a unicidade do container.** O processo tem um unico conjunto de colaboradores, e
 * isso e contrato de execucao, nao estilo: um segundo conjunto criaria um segundo armazenamento
 * de preferencias sobre o mesmo arquivo, e o runtime derruba o processo quando isso acontece —
 * reproduzido na pesquisa da Fase 3. Por isso o teste compara identidade entre duas leituras e le
 * as configuracoes pelo conjunto do aplicativo, em vez de montar um proprio. Nenhum teste deste
 * arquivo constroi colaborador algum: se ele o fizesse, provaria o contrario do que quer provar.
 *
 * **O que este arquivo deliberadamente NAO faz:** exercitar a triagem. O canal devolvido pelo
 * vinculo so aceita um objeto de chamada que a plataforma monta internamente e nao oferece a
 * aplicativo nenhum; forjar um exigiria falsificar tipo interno do sistema. O comportamento da
 * triagem — quantas respostas saem, qual decisao chega ao sistema, o que acontece depois — e
 * coberto pelo hospedeiro em JVM do plano 05-02, que captura o adaptador real do proprio servico.
 * Este arquivo cobre exatamente o que aquele nao alcanca.
 */
@RunWith(AndroidJUnit4::class)
class ScreeningServiceBindTest {

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule()

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()

    @Test
    fun sistemaConsegueSeVincularAoServicoDeTriagem() {
        val binder = serviceRule.bindService(
            Intent(app, UnknownCallScreeningService::class.java),
        )

        assertNotNull(
            "vinculo ao servico de triagem devolveu canal nulo — declaracao ou exportacao " +
                "quebrada no manifesto",
            binder,
        )
    }

    @Test
    fun containerDoAplicativoEOMesmoObjetoEmDuasLeituras() {
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
    fun configuracoesSaoLidasPeloContainerDoAplicativo() {
        val configuracoes = runBlocking { app.container.settingsRepository.snapshot() }

        assertNotNull("leitura das configuracoes pelo container do aplicativo falhou", configuracoes)
        assertSame(
            "o repositorio de configuracoes tambem precisa ser instancia unica do processo",
            app.container.settingsRepository,
            app.container.settingsRepository,
        )
    }
}

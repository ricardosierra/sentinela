package org.sentinela.app.telecom

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.SentinelaApp
import org.sentinela.app.data.contacts.ContactsTestFixture
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.platform.assetsPhoneMetadataLoader
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * DIA-04 provado no aparelho virtual: com o papel de telefone padrao detido, a triagem passa a
 * cobrir tambem quem esta na agenda, e a politica escolhida para contatos vale de fato.
 *
 * **Este requisito e PROVADO, nao implementado, e a diferenca importa.** O achado medido na
 * pesquisa desta fase e que segurar o papel de telefone padrao, sozinho, ja faz o sistema vincular
 * o servico de triagem — o proprio sistema monta um filtro de triagem para o telefone padrao,
 * independente do papel de triagem. Nao existe, portanto, um "modo discador" a ser codificado na
 * decisao: a decisao sobre contato existe no motor desde a Fase 2, com 48 casos parametrizados, e o
 * que muda e apenas QUEM chega a ela. Um ramo novo no motor nesta fase seria erro de desenho, e o
 * criterio de aceite deste plano e justamente o motor nao ter mudado uma linha na fase inteira.
 *
 * Dois fatos medidos completam o quadro e explicam ausencias que poderiam parecer lacuna:
 * a triagem roda **antes** de a chamada ser informada a interface de chamada, entao uma chamada
 * bloqueada nunca alcanca a tela de chamada — a tela nao precisa e nao deve ter ramo sobre chamada
 * bloqueada; e segurar os **dois** papeis produz um **unico** vinculo de triagem, sem triagem
 * dupla e sem risco de duas respostas concorrentes ao sistema.
 *
 * **O que este arquivo exercita e o que ele nao alcanca.** Ele exercita o coordenador de triagem
 * REAL do conjunto de colaboradores do aplicativo, com a agenda REAL do aparelho preparada por
 * fixture, com o papel de telefone padrao REALMENTE detido. O que ele nao faz e originar a chamada
 * de entrada pelo radio simulado: isso exige o console do aparelho virtual, cujo segredo de acesso
 * vive no diretorio pessoal de quem roda o teste e nao e alcancavel de dentro do processo de teste.
 * O caminho da plataforma ate o servico de triagem ja esta provado pela Fase 5 e pela pesquisa
 * desta fase; o cenario correspondente em aparelho fisico e da Phase 9.
 *
 * **Sobre a forma do bloqueio afirmada aqui.** Com as configuracoes de fabrica, bloquear vem
 * acompanhado do pedido de nao registrar a chamada no historico do proprio telefone, e por isso a
 * decisao esperada e a variante que carrega esse pedido, nao a rejeicao simples. O pedido em si o
 * Android so atende para aplicativo de operadora — medido na Fase 5, e o papel de telefone padrao
 * NAO destrava isso —, mas a decisao de dominio continua sendo a que a configuracao pede. Trocar
 * uma pela outra aqui seria descrever o produto errado.
 *
 * A regiao e fixada em BR de proposito, precedente da Fase 4: a cascata real leria o chip do
 * aparelho virtual, e o teste passaria a medir a configuracao do emulador em vez do comportamento
 * do aplicativo.
 */
@RunWith(AndroidJUnit4::class)
class DialerScreeningIntegrationTest {

    private val app: SentinelaApp get() = ApplicationProvider.getApplicationContext()
    private lateinit var resolver: ContentResolver
    private lateinit var normalizer: PhoneNumberNormalizer

    @Before
    fun prepararCenario() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ContactsTestFixture.adoptShell()
        resolver = ctx.contentResolver
        ContactsTestFixture.wipe(resolver)
        ContactsTestFixture.insert(resolver, NOME_DO_CONTATO, CONTATO_E164)
        normalizer = LibPhoneNumberNormalizer(
            util = phoneNumberUtil(assetsPhoneMetadataLoader(ctx)),
            regionProvider = RegionProvider { REGIAO },
        )
        TelecomShell.addRoleHolder(TelecomShell.ROLE_DIALER, ctx.packageName)
        esperarContatoVisivelAoAplicativo()
    }

    /**
     * Espera o contato preparado ficar visivel para o repositorio de contatos do aplicativo.
     *
     * Isto nao e paciencia decorativa: o repositorio do conjunto de colaboradores e unico no
     * processo e mantem um conjunto de chaves reconstruido por observador com atraso proposital,
     * medido em 750 ms na Fase 4. Quando outra suite da execucao ja aqueceu esse conjunto, uma
     * consulta feita no instante seguinte a insercao responde pelo conjunto ANTIGO e devolve
     * ausencia — e os casos de politica de contato ficariam vermelhos por um motivo falso, que nao
     * tem nada a ver com o modo discador. Falhar aqui, com mensagem propria, e melhor que falhar la.
     */
    private fun esperarContatoVisivelAoAplicativo() {
        repeat(TENTATIVAS_DE_AGENDA) {
            val encontrado = runBlocking {
                app.container.contactLookupRepository.lookup(CONTATO_E164)
            }
            if (encontrado == ContactLookup.HIT) return
            Thread.sleep(INTERVALO_DE_AGENDA_MILLIS)
        }
        throw AssertionError(
            "o contato preparado nao ficou visivel para o repositorio do aplicativo — sem contato " +
                "na agenda nenhum caso de politica de contato tem cenario",
        )
    }

    @After
    fun limparCenario() {
        runBlocking { app.container.settingsRepository.update { ScreeningSettings() } }
        ContactsTestFixture.wipe(resolver)
        ContactsTestFixture.dropShell()
        // O papel de telefone padrao NAO e devolvido aqui: perder um papel do sistema encerra o
        // processo do aplicativo, e a instrumentacao roda dentro dele. A prova da reversao e do
        // script de ciclo de vida, dirigido de fora do aparelho.
    }

    @Test
    fun oAplicativoDetemOPapelDeTelefonePadraoDuranteOsCasos() {
        assertTrue(
            "o cenario nao pode ser montado: o aplicativo nao ficou com o papel de telefone " +
                "padrao, e sem ele a triagem nao veria contato algum",
            DialerRoleManager(app).isRoleHeld(),
        )
    }

    @Test
    fun contatoComPoliticaBloquearEBloqueado() {
        politicaDeContatos(OriginPolicy.BLOCK)

        val decisao = triar(CONTATO_E164)

        assertEquals(
            "numero na agenda com politica de contatos Bloquear nao foi barrado",
            CallDecision.BlockWithoutTrace(DecisionReason.CONTACT),
            decisao,
        )
    }

    @Test
    fun contatoComPoliticaTocarEPermitido() {
        politicaDeContatos(OriginPolicy.RING)

        assertEquals(
            "numero na agenda com politica de contatos Tocar nao foi permitido",
            CallDecision.Allow(DecisionReason.CONTACT),
            triar(CONTATO_E164),
        )
    }

    @Test
    fun contatoComPoliticaSilenciarESilenciado() {
        politicaDeContatos(OriginPolicy.SILENCE)

        assertEquals(
            "numero na agenda com politica de contatos Silenciar nao foi silenciado",
            CallDecision.Silence(DecisionReason.CONTACT),
            triar(CONTATO_E164),
        )
    }

    @Test
    fun contatoComPoliticaNuncaSilenciarToca() {
        politicaDeContatos(OriginPolicy.NEVER_SILENCE)

        assertEquals(
            "numero na agenda com politica Nunca Silenciar nao foi permitido",
            CallDecision.Allow(DecisionReason.CONTACT),
            triar(CONTATO_E164),
        )
    }

    @Test
    fun numeroForaDaAgendaContinuaBloqueadoComOPapelAtivo() {
        politicaDeContatos(OriginPolicy.RING)

        assertEquals(
            "com o papel de telefone padrao ativo, desconhecido deixou de ser bloqueado — o " +
                "valor central do produto e exatamente este",
            CallDecision.BlockWithoutTrace(DecisionReason.UNKNOWN_NUMBER),
            triar(DESCONHECIDO_E164),
        )
    }

    /**
     * Ativar o modo discador, sozinho, nao pode comecar a bloquear ninguem da agenda. O padrao de
     * fabrica da politica de contatos e Tocar, e este caso o afirma nas duas pontas: no valor
     * padrao do tipo de configuracao e na leitura do repositorio depois de as configuracoes
     * voltarem ao padrao.
     */
    @Test
    fun padraoDeFabricaDaPoliticaDeContatosEToquem() {
        assertEquals(
            "o padrao do tipo de configuracao deixou de ser Tocar",
            OriginPolicy.RING,
            ScreeningSettings().contactsPolicy,
        )

        runBlocking { app.container.settingsRepository.update { ScreeningSettings() } }

        assertEquals(
            "o repositorio de configuracoes nao reportou Tocar como politica de contatos padrao",
            OriginPolicy.RING,
            runBlocking { app.container.settingsRepository.snapshot() }.contactsPolicy,
        )
        assertEquals(
            "com as configuracoes no padrao de fabrica, um numero da agenda foi barrado — ativar " +
                "o modo discador teria virado o oposto do que ele promete",
            CallDecision.Allow(DecisionReason.CONTACT),
            triar(CONTATO_E164),
        )
    }

    private fun politicaDeContatos(politica: OriginPolicy) = runBlocking {
        app.container.settingsRepository.update { it.copy(contactsPolicy = politica) }
    }

    /** Triagem pelo coordenador REAL do conjunto de colaboradores do aplicativo. */
    private fun triar(numeroBruto: String): CallDecision = runBlocking {
        val e164 = (normalizer.normalize(numeroBruto) as NormalizationResult.Valid).e164
        var decisao: CallDecision? = null
        app.container.screeningCoordinator.screen(
            call = ScreenedCall(CallDirection.INCOMING, ScreenedNumber.Valid(e164)),
            respond = { produzida, _ -> decisao = produzida },
        )
        requireNotNull(decisao) { "a triagem nao produziu decisao alguma" }
    }

    private companion object {
        const val TENTATIVAS_DE_AGENDA = 20
        const val INTERVALO_DE_AGENDA_MILLIS = 500L
        const val REGIAO = "BR"
        const val NOME_DO_CONTATO = "Ana da Agenda"
        const val CONTATO_E164 = "+5511987654321"
        const val DESCONHECIDO_E164 = "+5511912345678"
    }
}

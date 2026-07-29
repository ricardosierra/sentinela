package org.sentinela.app.data.contacts

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.platform.assetsPhoneMetadataLoader

/**
 * HIT/MISS contra o `ContactsContract` REAL do emulador — nenhuma fonte falsa aqui.
 *
 * O caso que justifica o arquivo inteiro e [contatoGravadoEmFormatoNacionalDaHit]: numa
 * implementacao de sonda UNICA por E.164 ele fica vermelho, porque o provider deixa
 * `NORMALIZED_NUMBER` nulo para contato gravado em formato nacional "estrangeiro" (o AVD roda com
 * SIM `us`) e uma consulta iniciada por `+` casa somente por igualdade dessa coluna. Sem este
 * teste, a sonda dupla seria uma afirmacao; com ele, e prova.
 *
 * **Nenhum assert deste arquivo depende de tempo.** A fonte e consultada de forma sincrona e o
 * cache nunca entra em cena: o repositorio e montado com um cache que ainda nao aqueceu, entao
 * toda resposta vem da sonda direta.
 */
@RunWith(AndroidJUnit4::class)
class ContactLookupSourceTest {

    private lateinit var resolver: ContentResolver
    private lateinit var source: ContactsContractLookupSource
    private lateinit var normalizer: PhoneNumberNormalizer
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ContactsTestFixture.adoptShell()
        resolver = ctx.contentResolver
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ContactsTestFixture.wipe(resolver)
        source = ContactsContractLookupSource(ctx)
        // Regiao fixa em BR de proposito: a cascata real leria o SIM do AVD (us) e o teste
        // passaria a medir a configuracao do emulador em vez do comportamento do app.
        normalizer = LibPhoneNumberNormalizer(
            util = phoneNumberUtil(assetsPhoneMetadataLoader(ctx)),
            regionProvider = RegionProvider { REGIAO },
        )
    }

    @After
    fun tearDown() {
        ContactsTestFixture.wipe(resolver)
        source.close()
        scope.cancel()
        ContactsTestFixture.dropShell()
    }

    @Test
    fun contatoGravadoEmE164DaHit() {
        ContactsTestFixture.insert(resolver, "Ana", CELULAR_E164)
        assertEquals(ContactLookup.HIT, consultar(CELULAR_E164))
    }

    /**
     * O teste da sonda dupla. Contato gravado em formato nacional BR num aparelho com SIM `us`:
     * o provider nao consegue normalizar, grava nulo, e a sonda por `+55...` NAO alcanca a linha.
     * Quem alcanca e a segunda sonda, com os digitos nacionais.
     */
    @Test
    fun contatoGravadoEmFormatoNacionalDaHit() {
        ContactsTestFixture.insert(resolver, "Bruno", "(11) 91234-5678")
        assertEquals(ContactLookup.HIT, consultar(CELULAR_E164))
    }

    @Test
    fun contatoGravadoComMascaraEEspacosDaHit() {
        ContactsTestFixture.insert(resolver, "Carla", "+55 11 91234-5678")
        assertEquals(ContactLookup.HIT, consultar(CELULAR_E164))
    }

    @Test
    fun numeroForaDaAgendaDaMiss() {
        ContactsTestFixture.insert(resolver, "Diego", CELULAR_E164)
        // MISS, jamais UNAVAILABLE: ha permissao e a consulta respondeu.
        assertEquals(ContactLookup.MISS, consultar(OUTRO_CELULAR_E164))
    }

    @Test
    fun agendaVaziaDaMiss() {
        assertEquals(ContactLookup.MISS, consultar(CELULAR_E164))
    }

    @Test
    fun fixoBrDaHitNasDuasGrafiasDeGravacao() {
        ContactsTestFixture.insert(resolver, "Elisa", "(21) 3216-5498")
        assertEquals(ContactLookup.HIT, consultar(FIXO_E164))

        ContactsTestFixture.wipe(resolver)
        ContactsTestFixture.insert(resolver, "Elisa", FIXO_E164)
        assertEquals(ContactLookup.HIT, consultar(FIXO_E164))
    }

    /**
     * O assert e sobre o TAMANHO DO CONJUNTO de chaves, nunca sobre a contagem do cursor: a
     * pesquisa da fase viu uma corrida devolver contagem divergente, e o que importa para a
     * decisao e quantas chaves distintas existem.
     */
    @Test
    fun leituraEmLoteProduzConjuntoDeChavesDoTamanhoEsperado() {
        val numeros = listOf(CELULAR_E164, OUTRO_CELULAR_E164, FIXO_E164, "+5511988887777")
        numeros.forEachIndexed { i, n -> ContactsTestFixture.insert(resolver, "Contato $i", n) }

        val crus = source.allRawNumbers()
        val chaves = crus
            .mapNotNull { (normalizer.normalize(it) as? NormalizationResult.Valid)?.e164 }
            .toSet()

        assertEquals(numeros.size, crus.size)
        assertEquals(numeros.size, chaves.size)
        assertEquals(numeros.toSet(), chaves)
    }

    /** Repositorio com cache frio: toda resposta vem da sonda direta, sem nenhuma espera. */
    private fun consultar(e164: String): ContactLookup = runBlocking {
        DefaultContactLookupRepository(
            source = source,
            cache = ContactKeyCache(source, normalizer, scope),
            normalizer = normalizer,
        ).lookup(e164)
    }

    private companion object {
        const val REGIAO = "BR"
        const val CELULAR_E164 = "+5511912345678"
        const val OUTRO_CELULAR_E164 = "+5511999998888"
        const val FIXO_E164 = "+552132165498"
    }
}

package org.sentinela.app.ui.whitelist

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.WhitelistEntry
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * O backup da whitelist existia como classe testada e não existia como funcionalidade: o
 * `WhitelistExporter` e o `WhitelistImporter` estavam corretos, mas ninguém os chamava. A rota
 * gravava a constante `{"whitelist":[]}` no arquivo do usuário e, na importação, lia o arquivo e
 * descartava o conteúdo. Estes casos cobrem a **ligação**, que é onde estava o buraco.
 *
 * Roda sob Robolectric porque `org.json` é apenas um esqueleto sem implementação na JVM pura — os
 * mesmos motivos do `WhitelistImportExportTest`. SDK 35 pelo teto do JDK 17 do projeto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhitelistBackupWiringTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<PersonalWhitelistRepository>()
    private val normalizer = mockk<PhoneNumberNormalizer> {
        every { normalize(any(), any()) } answers {
            val entrada = firstArg<String>()
            if (entrada.startsWith("+")) NormalizationResult.Valid(entrada)
            else NormalizationResult.Invalid(entrada)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exportacao carrega os numeros reais da lista`() = runTest {
        val items = listOf(
            WhitelistEntry(1L, "+5511999999999", "Mae", true, 10L),
            WhitelistEntry(2L, "+5511888887777", null, true, 20L),
        )
        every { repository.observeAll() } returns flowOf(items)

        val json = WhitelistViewModel(repository, normalizer).exportJson()

        assertTrue(json.contains("+5511999999999"))
        assertTrue(json.contains("+5511888887777"))
        assertTrue(json.contains("Mae"))
    }

    /** Lista vazia gera arquivo válido e vazio — não pode explodir nem inventar entrada. */
    @Test
    fun `exportacao de lista vazia gera arquivo valido`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())

        val json = WhitelistViewModel(repository, normalizer).exportJson()

        assertTrue(json.contains("whitelist"))
        assertTrue(json.contains("version"))
    }

    @Test
    fun `importacao mescla os novos e ignora os que ja existem`() = runTest {
        val existentes = listOf(WhitelistEntry(1L, "+5511999999999", null, true, 10L))
        every { repository.observeAll() } returns flowOf(existentes)
        coEvery { repository.upsert(any()) } returns Unit

        val json = """
            {"version":1,"whitelist":[
              {"numberKey":"+5511999999999","enabled":true,"createdAtUtcMillis":10},
              {"numberKey":"+5511777776666","enabled":true,"createdAtUtcMillis":20}
            ]}
        """.trimIndent()

        val viewModel = WhitelistViewModel(repository, normalizer)
        viewModel.import(json)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.upsert(match { it.numberE164 == "+5511777776666" }) }
        coVerify(exactly = 0) { repository.upsert(match { it.numberE164 == "+5511999999999" }) }
        assertEquals(
            WhitelistFeedback.Imported(added = 1, duplicates = 1, invalid = 0),
            viewModel.feedback.value,
        )
    }

    /**
     * Arquivo corrompido não pode gravar nada, não pode derrubar a tela — e precisa AVISAR.
     *
     * A versão anterior deste teste exigia `Imported(0, 0, 0)`, ou seja, fixava o defeito: quem
     * escolhesse o arquivo errado lia "0 adicionados, 0 já existiam, 0 inválidos", que é a mesma
     * frase de um backup válido e vazio. O invariante do projeto manda erro de importação avisar na
     * tela, então o resultado correto é falha explícita.
     */
    @Test
    fun `importacao de arquivo malformado avisa falha e nao grava nada`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())
        coEvery { repository.upsert(any()) } returns Unit

        val viewModel = WhitelistViewModel(repository, normalizer)
        viewModel.import("isto não é json")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.upsert(any()) }
        assertEquals(WhitelistFeedback.ImportFailed, viewModel.feedback.value)
    }

    /** Backup legítimo e vazio continua sendo sucesso — não pode virar aviso de falha. */
    @Test
    fun `importacao de backup valido e vazio nao vira aviso de falha`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())
        coEvery { repository.upsert(any()) } returns Unit

        val viewModel = WhitelistViewModel(repository, normalizer)
        viewModel.import("""{ "version": 1, "whitelist": [] }""")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.upsert(any()) }
        assertEquals(
            WhitelistFeedback.Imported(added = 0, duplicates = 0, invalid = 0),
            viewModel.feedback.value,
        )
    }
}

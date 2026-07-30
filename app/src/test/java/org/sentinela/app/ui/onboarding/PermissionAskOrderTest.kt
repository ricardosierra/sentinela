package org.sentinela.app.ui.onboarding

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.settings.ScreeningSettings

/**
 * Guarda-corpo da ordem "marca antes do disparo", no molde de 05-03 e 06-01: o instrumento é uma
 * **lista ordenada de eventos**, e o assert é sobre a ordem exata.
 *
 * Por que a ordem precisa de teste próprio, e não de revisão de código: as duas escritas produzem o
 * mesmo estado final em disco, então nenhum assert sobre o valor gravado distingue "marcou antes" de
 * "marcou no retorno". Só a sequência distingue — e é justamente a sequência que quebra quando
 * alguém move a gravação para o callback do launcher, achando que é o lugar natural dela.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionAskOrderTest {

    private val eventos = mutableListOf<String>()
    private val configuracoes = MutableStateFlow(ScreeningSettings())

    @Before
    fun ligarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun desligarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------------------------------
    // A ordem.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `a marca de agenda e gravada ANTES de o pedido ser disparado`() = runTest {
        val vm = criar()

        vm.pedirAgenda { eventos += DISPAROU_AGENDA }

        assertEquals(listOf(MARCOU_AGENDA, DISPAROU_AGENDA), eventos)
    }

    @Test
    fun `a marca de notificacao e gravada ANTES de o pedido ser disparado`() = runTest {
        val vm = criar()

        vm.pedirNotificacao { eventos += DISPAROU_NOTIFICACAO }

        assertEquals(listOf(MARCOU_NOTIFICACAO, DISPAROU_NOTIFICACAO), eventos)
    }

    @Test
    fun `pedir agenda e depois notificacao mantem cada marca antes do seu disparo`() = runTest {
        val vm = criar()

        vm.pedirAgenda { eventos += DISPAROU_AGENDA }
        vm.pedirNotificacao { eventos += DISPAROU_NOTIFICACAO }

        assertEquals(
            listOf(MARCOU_AGENDA, DISPAROU_AGENDA, MARCOU_NOTIFICACAO, DISPAROU_NOTIFICACAO),
            eventos,
        )
    }

    // ------------------------------------------------------------------------------------------
    // Onboarding concluído.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `concluir grava a marca de onboarding concluido`() = runTest {
        val vm = criar()

        vm.concluir()

        assertEquals(listOf(MARCOU_ONBOARDING), eventos)
    }

    @Test
    fun `pular grava a marca de onboarding concluido igual a concluir`() = runTest {
        val vm = criar()

        vm.pular()

        assertEquals(listOf(MARCOU_ONBOARDING), eventos)
    }

    // ------------------------------------------------------------------------------------------
    // O padrão vem do repositório, não da tela.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `cada passo reflete o padrao do repositorio em vez de redefinir o seu`() = runTest {
        val vm = criar()
        val estado = observar(vm)

        // Os padrões de fábrica são os dos mockups. Pular sem escrever nada deixa exatamente isto.
        assertEquals(OriginPolicy.BLOCK, estado().settings.unknownPolicy)
        assertEquals(OriginPolicy.RING, estado().settings.contactsPolicy)
        assertEquals(OriginPolicy.NEVER_SILENCE, estado().settings.whitelistPolicy)
        assertTrue(estado().settings.blockPrivateNumbers)
        assertEquals(false, estado().settings.showOwnNotification)
    }

    @Test
    fun `configuracao trocada no repositorio aparece no passo sem a tela redefinir padrao`() =
        runTest {
            val vm = criar()
            val estado = observar(vm)

            configuracoes.value = ScreeningSettings(unknownPolicy = OriginPolicy.SILENCE)

            assertEquals(OriginPolicy.SILENCE, estado().settings.unknownPolicy)
        }

    // ------------------------------------------------------------------------------------------
    // Navegação entre passos.
    // ------------------------------------------------------------------------------------------

    @Test
    fun `o passo corrente sobrevive a uma reemissao das configuracoes`() = runTest {
        val vm = criar()
        val estado = observar(vm)
        vm.avancar()
        vm.avancar()
        assertEquals(2, estado().step)

        // É o que uma recriação de Activity pelo diálogo do sistema provoca: o repositório reemite.
        configuracoes.value = ScreeningSettings(blockPrivateNumbers = false)

        assertEquals("trocar configuração não pode reiniciar o onboarding", 2, estado().step)
    }

    @Test
    fun `avancar e voltar respeitam os limites dos seis passos`() = runTest {
        val vm = criar()
        val estado = observar(vm)

        repeat(TOTAL_DE_PASSOS + 3) { vm.avancar() }
        assertEquals(TOTAL_DE_PASSOS - 1, estado().step)

        repeat(TOTAL_DE_PASSOS + 3) { vm.voltar() }
        assertEquals(0, estado().step)
    }

    @Test
    fun `nenhum passo e bloqueante - papel negado nao impede avancar`() = runTest {
        val vm = criar(papelDetido = false)
        val estado = observar(vm)

        vm.reconsultarPapel()
        vm.avancar()

        assertEquals(1, estado().step)
        assertEquals(false, estado().screeningRoleHeld)
    }

    // ------------------------------------------------------------------------------------------
    // Apoio.
    // ------------------------------------------------------------------------------------------

    private fun TestScope.observar(vm: OnboardingViewModel): () -> OnboardingUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect { } }
        return { vm.estado.value }
    }

    private fun criar(papelDetido: Boolean = false) = OnboardingViewModel(
        settings = mockk<DataStoreSettingsRepository> {
            every { settings } returns configuracoes
            coEvery { markContactsPermissionAsked() } answers { eventos += MARCOU_AGENDA }
            coEvery { markNotificationPermissionAsked() } answers {
                eventos += MARCOU_NOTIFICACAO
            }
            coEvery { markOnboardingCompleted() } answers { eventos += MARCOU_ONBOARDING }
        },
        roleHeld = { papelDetido },
        roleAvailable = { true },
        requestRoleIntent = { null },
        contactsState = { ContactsPermissionState.NEVER_ASKED },
        notificationState = { RuntimePermissionAsk.NEVER_ASKED },
    )

    private companion object {
        const val MARCOU_AGENDA = "marcou agenda"
        const val DISPAROU_AGENDA = "disparou agenda"
        const val MARCOU_NOTIFICACAO = "marcou notificacao"
        const val DISPAROU_NOTIFICACAO = "disparou notificacao"
        const val MARCOU_ONBOARDING = "marcou onboarding concluido"
    }
}

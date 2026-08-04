package org.sentinela.app.ui.home

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState

/**
 * Prova de que o papel do sistema é estado VIVO.
 *
 * O instrumento é um **contador de invocações**, jamais um cronômetro. Essa escolha é a lição mais
 * repetida do projeto: nas Fases 3 e 4, remover o cache de contatos deixou o teste de TEMPO verde e
 * só o contador ficou vermelho. Cronômetro mede o ambiente; contador mede a estrutura, que é o que
 * está sendo prometido aqui.
 *
 * O que um cache introduzido depois faria: reduziria as invocações por reconsulta (primeiro caso) e
 * congelaria o estado publicado quando o sistema passasse a responder o contrário (segundo caso).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoleLiveStateTest {

    private var consultasDeDeter = 0
    private var consultasDeDisponibilidade = 0
    private var consultasDeAgenda = 0
    private var consultasDoDiscador = 0

    private var papelDetido = false
    private var agenda = ContactsPermissionState.GRANTED
    private var discadorDetido = false

    @Before
    fun ligarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun desligarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    @Test
    fun `duas reconsultas produzem duas consultas de cada sinal do sistema`() = runTest {
        val vm = criar()
        zerarContadores()

        vm.reconsultarPapel()
        vm.reconsultarPapel()

        assertEquals("papel de triagem detido", 2, consultasDeDeter)
        assertEquals("papel de triagem disponível", 2, consultasDeDisponibilidade)
        assertEquals("permissão de agenda", 2, consultasDeAgenda)
        assertEquals("papel de discador detido", 2, consultasDoDiscador)
    }

    @Test
    fun `cinco reconsultas produzem cinco consultas - nenhuma resposta e reaproveitada`() =
        runTest {
            val vm = criar()
            zerarContadores()

            repeat(5) { vm.reconsultarPapel() }

            assertEquals(5, consultasDeDeter)
            assertEquals(5, consultasDeDisponibilidade)
        }

    @Test
    fun `o estado publicado muda quando o sistema passa a responder o contrario`() = runTest {
        papelDetido = false
        val vm = criar()
        val estado = observar(vm)
        assertFalse("estado inicial reflete o sistema", estado().screeningRoleHeld)

        // O usuário concedeu o papel fora do aplicativo. Nenhum retorno de seletor vai avisar.
        papelDetido = true
        vm.reconsultarPapel()

        assertTrue("um cache deixaria isto falso para sempre", estado().screeningRoleHeld)

        // E a perda também precisa aparecer — é o caminho que encerra o processo em produção.
        papelDetido = false
        vm.reconsultarPapel()

        assertFalse(estado().screeningRoleHeld)
    }

    @Test
    fun `permissao de agenda revogada fora do aplicativo aparece na reconsulta`() = runTest {
        agenda = ContactsPermissionState.GRANTED
        val vm = criar()
        val estado = observar(vm)
        assertEquals(ContactsPermissionState.GRANTED, estado().contactsPermission)

        agenda = ContactsPermissionState.DENIED_PERMANENTLY
        vm.reconsultarPapel()

        assertEquals(ContactsPermissionState.DENIED_PERMANENTLY, estado().contactsPermission)
    }

    @Test
    fun `modo discador e derivado da consulta viva e nunca de valor gravado`() = runTest {
        discadorDetido = false
        val vm = criar()
        val estado = observar(vm)
        assertEquals(DialerModeState.OFFERED, estado().dialerMode)

        discadorDetido = true
        vm.reconsultarPapel()

        assertEquals(DialerModeState.ACTIVE, estado().dialerMode)
    }

    // ----------------------------------------------------------------------------------------------

    private fun zerarContadores() {
        consultasDeDeter = 0
        consultasDeDisponibilidade = 0
        consultasDeAgenda = 0
        consultasDoDiscador = 0
    }

    private fun kotlinx.coroutines.test.TestScope.observar(
        vm: HomeViewModel,
    ): () -> HomeUiState {
        // Não confinado de propósito: o escopo de fundo do runTest usa o despachante padrão, que
        // só entrega ao avançar o tempo virtual, e a leitura logo após a reconsulta veria o valor
        // antigo — o teste ficaria vermelho por motivo falso.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect { } }
        return { vm.estado.value }
    }

    private fun criar() = HomeViewModel(
        settings = mockk<DataStoreSettingsRepository> {
            every { settings } returns flowOf(ScreeningSettings())
            every { appOpenCount } returns kotlinx.coroutines.flow.flowOf(0)
            every { ratingAccepted } returns kotlinx.coroutines.flow.flowOf(false)
        },
        history = historicoVazio(),
        roleHeld = { consultasDeDeter++; papelDetido },
        roleAvailable = { consultasDeDisponibilidade++; true },
        requestRoleIntent = { null },
        contactsState = { consultasDeAgenda++; agenda },
        dialerRoleHeld = { consultasDoDiscador++; discadorDetido },
        dialerRoleAvailable = { true },
        dialerOptedIn = { false },
        mask = { "+** ****" },
        clock = { 0L },
    )

    private fun historicoVazio() = object : BlockedCallRepository {
        override suspend fun record(entry: BlockedCallEntry): Long = 0L
        override fun observeRecent() = MutableStateFlow(emptyList<BlockedCallEntry>())
        override fun observeTotalCount() = MutableStateFlow(0L)
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun clearAll() = Unit
        override suspend fun updateClassification(
            id: Long,
            classification: org.sentinela.app.data.local.CallClassification
        ) = Unit
        override suspend fun pruneOlderThan(utcMillis: Long) = Unit
        override suspend fun hasRecentBlock(
            numberE164: String?,
            nowUtcMillis: Long,
        ): RepeatedCallLookup = RepeatedCallLookup.MISS
    }
}

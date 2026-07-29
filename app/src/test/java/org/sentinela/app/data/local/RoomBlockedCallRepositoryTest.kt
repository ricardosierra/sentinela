package org.sentinela.app.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.REPEATED_CALL_WINDOW_MILLIS
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.settings.SettingsRepository

/**
 * O relogio e SEMPRE injetado (`clock = { AGORA }`). Um teste de retencao que le
 * System.currentTimeMillis() fica verde hoje e vermelho amanha, e o cutoff seria
 * inverificavel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoomBlockedCallRepositoryTest {

    private companion object {
        const val AGORA = 1_700_000_000_000L
        const val UM_DIA = 86_400_000L
    }

    private class FakeSettingsRepository(initial: ScreeningSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<ScreeningSettings> = state
        override suspend fun snapshot(): ScreeningSettings = state.value
        override suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings) {
            state.update(transform)
        }
    }

    private fun entry(
        masked: String = "+55 11 9****-1234",
        e164: String? = "+5511999991234",
        timestamp: Long = AGORA,
        reason: DecisionReason = DecisionReason.UNKNOWN_NUMBER,
        notificationShown: Boolean = false,
    ) = BlockedCallEntry(
        maskedNumber = masked,
        numberE164 = e164,
        timestampUtcMillis = timestamp,
        reason = reason,
        notificationShown = notificationShown,
        // Toda gravacao nasce UNCLASSIFIED: classificar e ato do usuario, nunca do Service.
        classification = CallClassification.UNCLASSIFIED,
    )

    private fun fixture(
        historyEnabled: Boolean = true,
        retention: RetentionPolicy = RetentionPolicy.DAYS_30,
    ): Pair<FakeBlockedCallDao, RoomBlockedCallRepository> {
        val dao = FakeBlockedCallDao()
        val settings = FakeSettingsRepository(
            ScreeningSettings(historyEnabled = historyEnabled, retentionPolicy = retention),
        )
        return dao to RoomBlockedCallRepository(
            dao = dao,
            settings = settings,
            clock = { AGORA },
            io = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `historico desligado nao grava nada`() = runTest {
        val (dao, repo) = fixture(historyEnabled = false)
        repo.record(entry())
        assertEquals(0, dao.recordCallCount)
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun `historico desligado tambem nao poda`() = runTest {
        // Sem gravacao nao ha o que podar: record() sai antes de tocar no DAO.
        val (dao, repo) = fixture(historyEnabled = false)
        repo.record(entry())
        assertEquals(0, dao.pruneCallCount)
    }

    @Test
    fun `NEVER_STORE nao grava nada`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.NEVER_STORE)
        repo.record(entry())
        assertEquals(0, dao.recordCallCount)
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun `DAYS_30 grava e poda no cutoff de trinta dias`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.DAYS_30)
        repo.record(entry())
        assertEquals(1, dao.recordCallCount)
        assertEquals(1, dao.pruneCallCount)
        assertEquals(AGORA - 30 * UM_DIA, dao.lastPruneCutoff)
    }

    @Test
    fun `MANUAL grava e nunca poda`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry())
        assertEquals(1, dao.recordCallCount)
        assertEquals(0, dao.pruneCallCount)
        assertNull(dao.lastPruneCutoff)
    }

    @Test
    fun `a poda apaga o vencido e preserva o que esta dentro da retencao`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.DAYS_7)
        val cutoff = AGORA - 7 * UM_DIA
        // Gravado direto no DAO para nascer com timestamp antigo, sem passar pela poda.
        dao.record(entryEntity(timestamp = cutoff - 1))
        dao.record(entryEntity(timestamp = cutoff))
        repo.record(entry(timestamp = AGORA))
        assertEquals(listOf(AGORA, cutoff), dao.entities.map { it.timestampUtcMillis }.sortedDescending())
    }

    @Test
    fun `pruneNow poda sem gravar`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.DAYS_30)
        repo.pruneNow()
        assertEquals(0, dao.recordCallCount)
        assertEquals(1, dao.pruneCallCount)
        assertEquals(AGORA - 30 * UM_DIA, dao.lastPruneCutoff)
    }

    @Test
    fun `pruneNow com MANUAL nao toca no banco`() = runTest {
        val (dao, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.pruneNow()
        assertEquals(0, dao.pruneCallCount)
    }

    @Test
    fun `pruneOlderThan explicito usa o cutoff recebido`() = runTest {
        val (dao, repo) = fixture()
        repo.pruneOlderThan(AGORA - UM_DIA)
        assertEquals(AGORA - UM_DIA, dao.lastPruneCutoff)
    }

    @Test
    fun `observeRecent preserva mascara E164 motivo e notificacao`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(
            entry(
                masked = "+55 11 9****-5678",
                e164 = "+5511999995678",
                reason = DecisionReason.PRIVATE_NUMBER,
                notificationShown = true,
            ),
        )
        val lido = repo.observeRecent().first().single()
        assertEquals("+55 11 9****-5678", lido.maskedNumber)
        // HST-04: o E.164 completo tem de sobreviver para a Fase 8 oferecer "adicionar a whitelist".
        assertEquals("+5511999995678", lido.numberE164)
        assertEquals(DecisionReason.PRIVATE_NUMBER, lido.reason)
        assertTrue(lido.notificationShown)
        assertEquals(CallClassification.UNCLASSIFIED, lido.classification)
    }

    @Test
    fun `observeRecent vem do mais recente para o mais antigo`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry(timestamp = AGORA - 2 * UM_DIA))
        repo.record(entry(timestamp = AGORA))
        repo.record(entry(timestamp = AGORA - UM_DIA))
        assertEquals(
            listOf(AGORA, AGORA - UM_DIA, AGORA - 2 * UM_DIA),
            repo.observeRecent().first().map { it.timestampUtcMillis },
        )
    }

    @Test
    fun `numero sem E164 e registrado so com a mascara`() = runTest {
        // Numero privado: nao ha E.164 para guardar, e a mascara basta.
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry(masked = "Privado", e164 = null, reason = DecisionReason.PRIVATE_NUMBER))
        assertNull(repo.observeRecent().first().single().numberE164)
    }

    @Test
    fun `observeTotalCount acompanha as gravacoes`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        assertEquals(0L, repo.observeTotalCount().first())
        repo.record(entry())
        repo.record(entry())
        assertEquals(2L, repo.observeTotalCount().first())
    }

    @Test
    fun `deleteById remove apenas o registro pedido`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry(masked = "A"))
        repo.record(entry(masked = "B"))
        val alvo = repo.observeRecent().first().first { it.maskedNumber == "A" }
        repo.deleteById(alvo.id)
        assertEquals(listOf("B"), repo.observeRecent().first().map { it.maskedNumber })
    }

    @Test
    fun `clearAll zera o historico`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry())
        repo.record(entry())
        repo.clearAll()
        assertTrue(repo.observeRecent().first().isEmpty())
        assertEquals(0L, repo.observeTotalCount().first())
    }

    @Test
    fun `updateClassification persiste UNWANTED`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry())
        val id = repo.observeRecent().first().single().id
        repo.updateClassification(id, CallClassification.UNWANTED)
        assertEquals(CallClassification.UNWANTED, repo.observeRecent().first().single().classification)
    }

    @Test
    fun `updateClassification persiste LEGITIMATE`() = runTest {
        val (_, repo) = fixture(retention = RetentionPolicy.MANUAL)
        repo.record(entry())
        val id = repo.observeRecent().first().single().id
        repo.updateClassification(id, CallClassification.LEGITIMATE)
        assertEquals(CallClassification.LEGITIMATE, repo.observeRecent().first().single().classification)
    }

    @Test
    fun `falha do DAO em record propaga para o chamador`() = runTest {
        // O repositorio nao mente: quem decide engolir a falha e o Service da Fase 5,
        // que precisa saber que o historico nao registrou.
        val (dao, repo) = fixture()
        dao.recordFailure = IllegalStateException("disco cheio")
        var capturada: Throwable? = null
        try {
            repo.record(entry())
        } catch (e: IllegalStateException) {
            capturada = e
        }
        assertEquals("disco cheio", capturada?.message)
        assertEquals(0, dao.pruneCallCount)
    }

    private fun entryEntity(timestamp: Long) = org.sentinela.app.data.local.db.BlockedCallEntity(
        maskedNumber = "+55 11 9****-0000",
        numberE164 = "+5511999990000",
        timestampUtcMillis = timestamp,
        reasonCode = DecisionReason.UNKNOWN_NUMBER.code,
        notificationShown = false,
        classification = CallClassification.UNCLASSIFIED.name,
    )

    // --- SCR-12: consulta de bloqueio recente --------------------------------

    private val numeroDeTeste = "+5511999991234"

    @Test
    fun `numero bloqueado dentro da janela responde HIT`() = runTest {
        val (_, repo) = fixture()
        repo.record(entry(e164 = numeroDeTeste, timestamp = AGORA - 1_000L))
        assertEquals(RepeatedCallLookup.HIT, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `numero bloqueado antes da janela responde MISS`() = runTest {
        val (_, repo) = fixture()
        repo.record(entry(e164 = numeroDeTeste, timestamp = AGORA - REPEATED_CALL_WINDOW_MILLIS - 1L))
        assertEquals(RepeatedCallLookup.MISS, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `registro exatamente no limite da janela ainda conta como repeticao`() = runTest {
        val (_, repo) = fixture()
        repo.record(entry(e164 = numeroDeTeste, timestamp = AGORA - REPEATED_CALL_WINDOW_MILLIS))
        assertEquals(RepeatedCallLookup.HIT, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `numero sem nenhum registro responde MISS`() = runTest {
        val (_, repo) = fixture()
        assertEquals(RepeatedCallLookup.MISS, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `registro recente de outro numero nao vira repeticao`() = runTest {
        val (_, repo) = fixture()
        repo.record(entry(e164 = "+5511988887777", timestamp = AGORA - 1_000L))
        assertEquals(RepeatedCallLookup.MISS, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `numero nulo ou em branco responde MISS sem tocar no banco`() = runTest {
        val (dao, repo) = fixture()
        dao.countFailure = IllegalStateException("o DAO nao pode ser chamado")
        assertEquals(RepeatedCallLookup.MISS, repo.hasRecentBlock(null, AGORA))
        assertEquals(RepeatedCallLookup.MISS, repo.hasRecentBlock("   ", AGORA))
        assertNull(dao.lastCountSince)
    }

    @Test
    fun `falha do DAO vira LOOKUP_FAILED em vez de propagar`() = runTest {
        val (dao, repo) = fixture()
        dao.countFailure = IllegalStateException("banco indisponivel")
        assertEquals(RepeatedCallLookup.LOOKUP_FAILED, repo.hasRecentBlock(numeroDeTeste, AGORA))
    }

    @Test
    fun `o corte enviado ao DAO e o agora menos a janela nomeada`() = runTest {
        val (dao, repo) = fixture()
        repo.hasRecentBlock(numeroDeTeste, AGORA)
        assertEquals(AGORA - REPEATED_CALL_WINDOW_MILLIS, dao.lastCountSince)
    }
}

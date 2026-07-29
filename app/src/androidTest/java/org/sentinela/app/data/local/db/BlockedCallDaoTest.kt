package org.sentinela.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.domain.DecisionReason

/**
 * Registro, ordenacao, poda, limpeza e classificacao contra o SQLite de verdade.
 *
 * Banco em memoria: rapido e isolado por teste. O builder NAO libera consulta na
 * main thread — os testes rodam na thread de instrumentacao, que nao e a main
 * thread do app, entao afrouxar essa checagem so esconderia uso indevido.
 */
@RunWith(AndroidJUnit4::class)
class BlockedCallDaoTest {

    private companion object {
        const val AGORA = 1_700_000_000_000L
        const val UM_DIA = 86_400_000L
    }

    private lateinit var db: SentinelaDatabase
    private lateinit var dao: BlockedCallDao

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SentinelaDatabase::class.java).build()
        dao = db.blockedCallDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(
        masked: String = "+55 11 9****-1234",
        e164: String? = "+5511999991234",
        timestamp: Long = AGORA,
        reason: DecisionReason = DecisionReason.UNKNOWN_NUMBER,
        notificationShown: Boolean = false,
    ) = BlockedCallEntity(
        maskedNumber = masked,
        numberE164 = e164,
        timestampUtcMillis = timestamp,
        reasonCode = reason.code,
        notificationShown = notificationShown,
        classification = CallClassification.UNCLASSIFIED.name,
    )

    @Test
    fun registroSobreviveAoRoundTripCompleto() = runBlocking {
        dao.record(
            entity(
                masked = "+55 11 9****-5678",
                e164 = "+5511999995678",
                reason = DecisionReason.PRIVATE_NUMBER,
                notificationShown = true,
            ),
        )
        val lido = dao.observeRecent().first().single()
        assertEquals("+55 11 9****-5678", lido.maskedNumber)
        // HST-04: o E.164 completo precisa voltar intacto para a Fase 8 poder
        // oferecer "adicionar a whitelist" a partir do historico.
        assertEquals("+5511999995678", lido.numberE164)
        assertEquals(AGORA, lido.timestampUtcMillis)
        assertEquals(DecisionReason.PRIVATE_NUMBER.code, lido.reasonCode)
        assertTrue(lido.notificationShown)
    }

    @Test
    fun reasonCodeVoltaComoOMesmoDecisionReason() = runBlocking {
        val converters = Converters()
        DecisionReason.entries.forEach { reason ->
            dao.clearAll()
            dao.record(entity(reason = reason))
            val lido = dao.observeRecent().first().single()
            assertEquals(reason, converters.toDecisionReason(lido.reasonCode))
        }
    }

    @Test
    fun numeroPrivadoEGravadoSemE164() = runBlocking {
        dao.record(entity(masked = "Privado", e164 = null, reason = DecisionReason.PRIVATE_NUMBER))
        assertNull(dao.observeRecent().first().single().numberE164)
    }

    @Test
    fun observeRecentVemDoMaisRecenteParaOMaisAntigo() = runBlocking {
        dao.record(entity(masked = "meio", timestamp = AGORA - UM_DIA))
        dao.record(entity(masked = "velho", timestamp = AGORA - 2 * UM_DIA))
        dao.record(entity(masked = "novo", timestamp = AGORA))
        assertEquals(
            listOf("novo", "meio", "velho"),
            dao.observeRecent().first().map { it.maskedNumber },
        )
    }

    @Test
    fun podaApagaOVencidoEPreservaODentroDaRetencao() = runBlocking {
        val cutoff = AGORA - 30 * UM_DIA
        dao.record(entity(masked = "anterior", timestamp = cutoff - 1))
        dao.record(entity(masked = "no limite", timestamp = cutoff))
        dao.record(entity(masked = "posterior", timestamp = cutoff + 1))

        val apagados = dao.pruneOlderThan(cutoff)

        // O limite e ESTRITAMENTE `< cutoff`: quem esta exatamente no cutoff sobrevive.
        // Trocar por `<=` apagaria um registro que o usuario ainda tem direito de ver.
        assertEquals(1, apagados)
        assertEquals(
            listOf("posterior", "no limite"),
            dao.observeRecent().first().map { it.maskedNumber },
        )
    }

    @Test
    fun podaSemNadaVencidoNaoApagaNada() = runBlocking {
        dao.record(entity(timestamp = AGORA))
        assertEquals(0, dao.pruneOlderThan(AGORA - UM_DIA))
        assertEquals(1L, dao.observeTotalCount().first())
    }

    @Test
    fun deleteByIdRemoveApenasOAlvo() = runBlocking {
        val alvo = dao.record(entity(masked = "alvo"))
        dao.record(entity(masked = "sobrevivente"))
        dao.deleteById(alvo)
        assertEquals(listOf("sobrevivente"), dao.observeRecent().first().map { it.maskedNumber })
        assertNull(dao.findById(alvo))
    }

    @Test
    fun clearAllZeraATabelaEOContador() = runBlocking {
        dao.record(entity())
        dao.record(entity())
        assertEquals(2L, dao.observeTotalCount().first())
        dao.clearAll()
        assertTrue(dao.observeRecent().first().isEmpty())
        assertEquals(0L, dao.observeTotalCount().first())
    }

    @Test
    fun classificacaoUnwantedSobreviveAReleitura() = runBlocking {
        val id = dao.record(entity())
        dao.updateClassification(id, CallClassification.UNWANTED.name)
        assertEquals(CallClassification.UNWANTED.name, dao.findById(id)?.classification)
        assertEquals(CallClassification.UNWANTED.name, dao.observeRecent().first().single().classification)
    }

    @Test
    fun classificacaoLegitimateSobreviveAReleitura() = runBlocking {
        val id = dao.record(entity())
        dao.updateClassification(id, CallClassification.LEGITIMATE.name)
        assertEquals(CallClassification.LEGITIMATE.name, dao.findById(id)?.classification)
    }

    @Test
    fun classificarUmRegistroNaoAfetaOsOutros() = runBlocking {
        val alvo = dao.record(entity(masked = "alvo"))
        val outro = dao.record(entity(masked = "outro"))
        dao.updateClassification(alvo, CallClassification.UNWANTED.name)
        assertEquals(CallClassification.UNCLASSIFIED.name, dao.findById(outro)?.classification)
    }

    @Test
    fun cadaRegistroRecebeUmIdProprio() = runBlocking {
        // `@Insert` sem REPLACE: dois registros do mesmo numero sao dois eventos
        // distintos no historico, cada um com id estavel para a UI da Fase 8.
        val primeiro = dao.record(entity())
        val segundo = dao.record(entity())
        assertTrue(segundo > primeiro)
        assertEquals(2L, dao.observeTotalCount().first())
    }
}

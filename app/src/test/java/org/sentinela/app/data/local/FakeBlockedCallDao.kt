@file:Suppress("LongMethod", "MaxLineLength", "TooManyFunctions", "ReturnCount", "MagicNumber", "SwallowedException", "LoopWithTooManyJumpStatements")

package org.sentinela.app.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.sentinela.app.data.local.db.BlockedCallDao
import org.sentinela.app.data.local.db.BlockedCallEntity

/**
 * DAO em memoria para os testes JVM do repositorio. Nao imita o SQLite — imita o
 * CONTRATO do DAO, incluindo a ordenacao decrescente e o corte estrito da poda.
 *
 * O que o Room faz de verdade (indice, `<` do cutoff, round-trip das colunas) e
 * provado no BlockedCallDaoTest instrumentado; aqui o alvo e a decisao do
 * repositorio: gravar ou nao, podar ou nao.
 */
class FakeBlockedCallDao : BlockedCallDao {

    private val rows = MutableStateFlow<List<BlockedCallEntity>>(emptyList())
    private var nextId = 1L

    /** Contadores usados como assert: o teste afirma que a poda ACONTECEU (ou nao). */
    var pruneCallCount: Int = 0
        private set
    var lastPruneCutoff: Long? = null
        private set
    var recordCallCount: Int = 0
        private set

    /** Quando setado, `record` lanca — o repositorio nao pode engolir a excecao. */
    var recordFailure: Throwable? = null

    val entities: List<BlockedCallEntity> get() = rows.value

    override suspend fun record(entity: BlockedCallEntity): Long {
        recordCallCount++
        recordFailure?.let { throw it }
        val id = if (entity.id == 0L) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id
    }

    override fun observeRecent(): Flow<List<BlockedCallEntity>> =
        rows.map { list -> list.sortedByDescending { it.timestampUtcMillis } }

    override fun observeTotalCount(): Flow<Long> = rows.map { it.size.toLong() }

    override fun observeByDecision(decision: String): Flow<List<BlockedCallEntity>> =
        rows.map { list -> list.filter { it.classification == decision }.sortedByDescending { it.timestampUtcMillis } }

    override fun observeByPeriod(sinceUtcMillis: Long): Flow<List<BlockedCallEntity>> =
        rows.map { list -> list.filter { it.timestampUtcMillis >= sinceUtcMillis }.sortedByDescending { it.timestampUtcMillis } }

    override suspend fun findById(id: Long): BlockedCallEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }

    override suspend fun pruneOlderThan(cutoffUtcMillis: Long): Int {
        pruneCallCount++
        lastPruneCutoff = cutoffUtcMillis
        val antes = rows.value.size
        // Corte ESTRITO: um registro exatamente no cutoff sobrevive.
        rows.value = rows.value.filterNot { it.timestampUtcMillis < cutoffUtcMillis }
        return antes - rows.value.size
    }

    /** Quando setado, `countBlockedSince` lanca — o repositorio precisa degradar. */
    var countFailure: Throwable? = null

    var lastCountSince: Long? = null
        private set

    override suspend fun countBlockedSince(numberE164: String, sinceUtcMillis: Long): Int {
        countFailure?.let { throw it }
        lastCountSince = sinceUtcMillis
        // Corte INCLUSIVO: um registro exatamente no limite conta como dentro da janela.
        return rows.value.count {
            it.numberE164 == numberE164 && it.timestampUtcMillis >= sinceUtcMillis
        }
    }

    override suspend fun updateClassification(id: Long, classification: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(classification = classification) else it }
    }
}

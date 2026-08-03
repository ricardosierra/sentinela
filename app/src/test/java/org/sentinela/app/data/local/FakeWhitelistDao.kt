package org.sentinela.app.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.sentinela.app.data.local.db.WhitelistDao
import org.sentinela.app.data.local.db.WhitelistEntity

/**
 * DAO em memoria para testes JVM puros. Honra o que o banco garante de verdade:
 * `number_key` e UNICO — inserir a mesma chave com id 0 duas vezes e um erro, do
 * mesmo jeito que o SQLite lanca SQLiteConstraintException. Assim o fake nao
 * "conserta" silenciosamente o defeito que o repositorio precisa evitar.
 */
class FakeWhitelistDao : WhitelistDao {

    private val rows = MutableStateFlow<List<WhitelistEntity>>(emptyList())
    private var nextId = 1L

    /** Proxima chamada de DAO lanca isto (uma vez). Cobre "falha de repositorio" (QLT-01). */
    var failNext: Throwable? = null

    /** Quantas vezes containsBlocking foi chamado — prova que o caminho quente usa o DAO. */
    var containsCalls = 0
        private set

    val snapshot: List<WhitelistEntity> get() = rows.value

    private fun maybeFail() {
        failNext?.let {
            failNext = null
            throw it
        }
    }

    override fun containsBlocking(key: String): Boolean {
        maybeFail()
        containsCalls++
        return rows.value.any { it.numberKey == key && it.enabled }
    }

    override suspend fun findIdByKey(key: String): Long? {
        maybeFail()
        return rows.value.firstOrNull { it.numberKey == key }?.id
    }

    override suspend fun upsert(entity: WhitelistEntity) {
        maybeFail()
        val current = rows.value
        val byId = current.firstOrNull { entity.id != 0L && it.id == entity.id }
        if (byId != null) {
            rows.value = current.map { if (it.id == entity.id) entity else it }
            return
        }
        val clash = current.firstOrNull { it.numberKey == entity.numberKey }
        require(clash == null) { "UNIQUE constraint failed: whitelist.number_key" }
        rows.value = current + entity.copy(id = nextId++)
    }

    override suspend fun delete(entity: WhitelistEntity) {
        maybeFail()
        rows.value = rows.value.filterNot { it.id == entity.id }
    }

    override suspend fun deleteById(id: Long) {
        maybeFail()
        rows.value = rows.value.filterNot { it.id == id }
    }

    override fun observeAll(): Flow<List<WhitelistEntity>> =
        rows.map { list -> list.sortedByDescending { it.createdAtUtcMillis } }

    override fun count(): Flow<Int> = rows.map { it.size }

    override fun search(query: String): Flow<List<WhitelistEntity>> =
        rows.map { list ->
            list.filter {
                it.numberKey.contains(query, ignoreCase = true) ||
                    it.description?.contains(query, ignoreCase = true) == true
            }.sortedByDescending { it.createdAtUtcMillis }
        }

    override suspend fun findByKey(key: String): WhitelistEntity? {
        maybeFail()
        return rows.value.firstOrNull { it.numberKey == key }
    }
}

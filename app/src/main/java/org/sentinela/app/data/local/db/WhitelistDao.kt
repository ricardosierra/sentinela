package org.sentinela.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    /**
     * CAMINHO QUENTE do CallScreeningService (Fase 5) — deliberadamente NAO-suspend.
     * Medido na pesquisa da Fase 3 (emulador API 35, 1.000 entradas):
     *   suspend      p50 1,46 ms | p95 9,12 ms | p99 26,39 ms  -> ESTOURA o alvo
     *   nao-suspend  p50 0,20 ms | p95 3,59 ms | p99  5,46 ms  -> dentro
     * O custo e o dispatch de corrotina do Room, nao o SQLite. O chamador e
     * responsavel por nao invocar na main thread.
     *
     * `enabled = 1`: desabilitar equivale funcionalmente a remover (decisao travada).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE number_key = :key AND enabled = 1)")
    fun containsBlocking(key: String): Boolean

    @Query("SELECT id FROM whitelist WHERE number_key = :key")
    suspend fun findIdByKey(key: String): Long?

    @Upsert
    suspend fun upsert(entity: WhitelistEntity)

    @Delete
    suspend fun delete(entity: WhitelistEntity)

    @Query("DELETE FROM whitelist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM whitelist ORDER BY created_at_utc_millis DESC")
    fun observeAll(): Flow<List<WhitelistEntity>>

    @Query(
        "SELECT * FROM whitelist " +
            "WHERE number_key LIKE '%' || :query || '%' " +
            "OR description LIKE '%' || :query || '%' " +
            "ORDER BY created_at_utc_millis DESC",
    )
    fun search(query: String): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist WHERE number_key = :key")
    suspend fun findByKey(key: String): WhitelistEntity?
}

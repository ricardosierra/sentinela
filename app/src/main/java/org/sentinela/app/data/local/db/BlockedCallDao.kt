package org.sentinela.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    /**
     * `@Insert` sem `onConflict = REPLACE` de proposito: REPLACE deleta e reinsere,
     * trocando o id da linha — a UI da Fase 8 perderia a referencia do item.
     */
    @Insert
    suspend fun record(entity: BlockedCallEntity): Long

    @Query("SELECT * FROM blocked_call ORDER BY timestamp_utc_millis DESC")
    fun observeRecent(): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(*) FROM blocked_call")
    fun observeTotalCount(): Flow<Long>

    @Query("SELECT * FROM blocked_call WHERE id = :id")
    suspend fun findById(id: Long): BlockedCallEntity?

    @Query("DELETE FROM blocked_call WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_call")
    suspend fun clearAll()

    /** Poda por retencao: roda na abertura do app e apos cada gravacao (sem WorkManager). */
    @Query("DELETE FROM blocked_call WHERE timestamp_utc_millis < :cutoffUtcMillis")
    suspend fun pruneOlderThan(cutoffUtcMillis: Long): Int

    /** HST-05: o usuario classifica a chamada como legitima ou indesejada. */
    @Query("UPDATE blocked_call SET classification = :classification WHERE id = :id")
    suspend fun updateClassification(id: Long, classification: String)
}

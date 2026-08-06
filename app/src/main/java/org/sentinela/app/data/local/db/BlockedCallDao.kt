@file:Suppress("TooManyFunctions", "MaxLineLength")

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

    @Query("SELECT * FROM blocked_call ORDER BY timestamp_utc_millis DESC LIMIT 200")
    fun observeRecent(): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(*) FROM blocked_call")
    fun observeTotalCount(): Flow<Long>

    @Query("SELECT * FROM blocked_call WHERE classification = :decision ORDER BY timestamp_utc_millis DESC")
    fun observeByDecision(decision: String): Flow<List<BlockedCallEntity>>

    @Query("SELECT * FROM blocked_call WHERE timestamp_utc_millis >= :sinceUtcMillis ORDER BY timestamp_utc_millis DESC")
    fun observeByPeriod(sinceUtcMillis: Long): Flow<List<BlockedCallEntity>>

    @Query("SELECT * FROM blocked_call WHERE id = :id")
    suspend fun findById(id: Long): BlockedCallEntity?

    @Query("DELETE FROM blocked_call WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_call")
    suspend fun clearAll()

    /** Poda por retencao: roda na abertura do app e apos cada gravacao (sem WorkManager). */
    @Query("DELETE FROM blocked_call WHERE timestamp_utc_millis < :cutoffUtcMillis")
    suspend fun pruneOlderThan(cutoffUtcMillis: Long): Int

    /**
     * SCR-12: quantas vezes este numero ja foi bloqueado a partir do instante de corte.
     *
     * Corte INCLUSIVO (`>=`): um registro exatamente no limite conta como dentro da
     * janela — espelho coerente do corte estrito da poda, que usa `<`.
     *
     * Nenhum indice novo foi criado de proposito: acrescentar indice mudaria o schema e
     * exigiria migracao para a versao 2, enquanto a tabela permanece pequena por causa da
     * politica de retencao. Se algum dia a medicao em aparelho mostrar custo relevante,
     * o indice entra junto com a migracao — nunca sozinho.
     */
    @Query(
        "SELECT COUNT(*) FROM blocked_call " +
            "WHERE number_e164 = :numberE164 AND timestamp_utc_millis >= :sinceUtcMillis",
    )
    suspend fun countBlockedSince(numberE164: String, sinceUtcMillis: Long): Int

    /** HST-05: o usuario classifica a chamada como legitima ou indesejada. */
    @Query("UPDATE blocked_call SET classification = :classification WHERE id = :id")
    suspend fun updateClassification(id: Long, classification: String)
}

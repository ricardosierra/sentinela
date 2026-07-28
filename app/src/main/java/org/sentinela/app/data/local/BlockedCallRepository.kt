package org.sentinela.app.data.local

import kotlinx.coroutines.flow.Flow
import org.sentinela.app.domain.DecisionReason

/**
 * Histórico interno opcional de chamadas bloqueadas (Room na Fase 3).
 * Registro acontece somente se habilitado e sempre DEPOIS do respondToCall.
 */
interface BlockedCallRepository {

    suspend fun record(entry: BlockedCallEntry)

    fun observeRecent(): Flow<List<BlockedCallEntry>>

    fun observeTotalCount(): Flow<Long>

    suspend fun deleteById(id: Long)

    suspend fun clearAll()

    /** Aplica a política de retenção configurada (7/30/90 dias ou manual). */
    suspend fun pruneOlderThan(utcMillis: Long)
}

data class BlockedCallEntry(
    val id: Long = 0,
    /** Número mascarado para exibição (ex.: +55 11 9****-1234); nunca o completo em logs. */
    val maskedNumber: String,
    val numberE164: String?,
    val timestampUtcMillis: Long,
    val reason: DecisionReason,
    val notificationShown: Boolean,
    val classification: CallClassification = CallClassification.UNCLASSIFIED,
)

enum class CallClassification { UNCLASSIFIED, LEGITIMATE, UNWANTED }

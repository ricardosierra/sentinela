package org.sentinela.app.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Whitelist pessoal local (Room na Fase 3). A interface fica estável para,
 * no futuro (v2), aceitar uma fonte remota sem tocar no domínio.
 */
interface PersonalWhitelistRepository {

    /** Consulta usada na decisão — precisa ser O(1)/indexada e sem rede. */
    suspend fun contains(numberE164: String): Boolean

    fun observeAll(): Flow<List<WhitelistEntry>>

    suspend fun upsert(entry: WhitelistEntry)

    suspend fun delete(id: Long)
}

data class WhitelistEntry(
    val id: Long = 0,
    val numberE164: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val createdAtUtcMillis: Long,
)

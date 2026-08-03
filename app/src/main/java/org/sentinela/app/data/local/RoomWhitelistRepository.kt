package org.sentinela.app.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.sentinela.app.data.local.db.WhitelistDao
import org.sentinela.app.data.local.db.WhitelistEntity

/**
 * Implementacao Room da whitelist pessoal.
 *
 * A chave persistida e a saida do PhoneNumberNormalizer (contrato da Fase 2):
 * E.164 para numeros normais, digitos crus abaixo de PhoneNumbers.LIMIAR_CURTO (6).
 * A normalizacao acontece ANTES de chegar aqui — este repositorio nao normaliza.
 *
 * Nenhum log nesta classe: a chave e um numero de telefone completo.
 */
class RoomWhitelistRepository(
    private val dao: WhitelistDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PersonalWhitelistRepository {

    /**
     * Caminho quente: delega ao DAO NAO-suspend, evitando o hop interno de
     * dispatcher do Room (medido: p95 9,12 ms suspend vs 3,59 ms nao-suspend).
     */
    override suspend fun contains(numberE164: String): Boolean =
        withContext(io) { dao.containsBlocking(numberE164) }

    override fun observeAll(): Flow<List<WhitelistEntry>> =
        dao.observeAll().map { list -> list.map(WhitelistEntity::toDomain) }.conflate()

    override fun search(query: String): Flow<List<WhitelistEntry>> =
        dao.search(query).map { list -> list.map(WhitelistEntity::toDomain) }.conflate()

    /**
     * Dedup (WLT-04): o indice UNICO em number_key garante a atomicidade, mas
     * @Upsert com id = 0 nao casa pelo indice — ele tentaria UPDATE por chave
     * primaria, nao acharia linha e cairia num INSERT que viola a constraint.
     * Por isso o id existente e resolvido antes: duplicata vira atualizacao
     * silenciosa, sem erro visivel ao usuario.
     */
    override suspend fun upsert(entry: WhitelistEntry): Unit = withContext(io) {
        val existing = dao.findByKey(entry.numberE164)
        dao.upsert(
            entry.toEntity(
                id = existing?.id ?: entry.id,
                // createdAt original preservado: reeditar nao "rejuvenesce" a entrada.
                createdAtUtcMillis = existing?.createdAtUtcMillis ?: entry.createdAtUtcMillis,
            ),
        )
    }

    override suspend fun delete(id: Long): Unit = withContext(io) { dao.deleteById(id) }
}

internal fun WhitelistEntity.toDomain(): WhitelistEntry = WhitelistEntry(
    id = id,
    numberE164 = numberKey,
    description = description,
    enabled = enabled,
    createdAtUtcMillis = createdAtUtcMillis,
)

internal fun WhitelistEntry.toEntity(
    id: Long = this.id,
    createdAtUtcMillis: Long = this.createdAtUtcMillis,
): WhitelistEntity = WhitelistEntity(
    id = id,
    numberKey = numberE164,
    description = description,
    enabled = enabled,
    createdAtUtcMillis = createdAtUtcMillis,
)

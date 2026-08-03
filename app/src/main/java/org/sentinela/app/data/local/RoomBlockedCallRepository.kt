package org.sentinela.app.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.sentinela.app.data.local.db.BlockedCallDao
import org.sentinela.app.data.local.db.BlockedCallEntity
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.REPEATED_CALL_WINDOW_MILLIS
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.settings.SettingsRepository

/**
 * Implementacao Room do historico local (HST-01..05).
 *
 * Este e o UNICO lugar do app onde o numero completo e persistido — a coluna
 * `number_e164` existe para a Fase 8 oferecer "adicionar a whitelist" a partir do
 * historico. Nada aqui pode virar registro em texto: esta classe nao chama o logger
 * da plataforma nem escreve na saida padrao, e o que sai para notificacao e
 * diagnostico tecnico e sempre a mascara.
 *
 * O relogio e injetado para a retencao ser testavel de forma determinista.
 */
class RoomBlockedCallRepository(
    private val dao: BlockedCallDao,
    private val settings: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : BlockedCallRepository {

    /**
     * HST-01/HST-02. Nenhum agendador em segundo plano: a poda roda logo apos cada
     * gravacao bem-sucedida (e na abertura do app, via `pruneNow`). A tabela e local
     * e pequena; a dependencia nova custaria cold start do Service sem ganho.
     *
     * Excecao do DAO propaga de proposito — quem decide engolir e o Service da
     * Fase 5, que precisa saber que o registro nao aconteceu.
     */
    override suspend fun record(entry: BlockedCallEntry): Long = withContext(io) {
        val current = settings.snapshot()
        if (!current.historyEnabled || !current.retentionPolicy.shouldStore) {
            return@withContext SEM_REGISTRO
        }
        val id = dao.record(entry.toEntity())
        pruneAccordingTo(current.retentionPolicy)
        id
    }

    /** Chamada tambem na abertura do app (AppContainer). */
    suspend fun pruneNow(): Unit = withContext(io) {
        pruneAccordingTo(settings.snapshot().retentionPolicy)
    }

    private suspend fun pruneAccordingTo(policy: RetentionPolicy) {
        // cutoff nulo = MANUAL (nunca poda) ou NEVER_STORE (nem grava).
        policy.cutoffUtcMillis(clock())?.let { dao.pruneOlderThan(it) }
    }

    override fun observeRecent(): Flow<List<BlockedCallEntry>> =
        dao.observeRecent().map { list -> list.map(BlockedCallEntity::toDomain) }.conflate()

    override fun observeTotalCount(): Flow<Long> = dao.observeTotalCount()

    override suspend fun deleteById(id: Long): Unit = withContext(io) { dao.deleteById(id) }

    override suspend fun clearAll(): Unit = withContext(io) { dao.clearAll() }

    override suspend fun pruneOlderThan(utcMillis: Long): Unit =
        withContext(io) { dao.pruneOlderThan(utcMillis) }

    /**
     * SCR-12. Unica excecao ao contrato desta classe de propagar a excecao do DAO: a
     * decisao de triagem nao pode falhar por causa do historico. Se a consulta quebrar,
     * o motor recebe LOOKUP_FAILED, trata como ausencia de repeticao e aplica a politica
     * normal — nunca bloqueia por causa da falha.
     */
    override suspend fun hasRecentBlock(
        numberE164: String?,
        nowUtcMillis: Long,
    ): RepeatedCallLookup {
        if (numberE164.isNullOrBlank()) return RepeatedCallLookup.MISS
        return withContext(io) {
            runCatching {
                val since = nowUtcMillis - REPEATED_CALL_WINDOW_MILLIS
                dao.countBlockedSince(numberE164, since)
            }.fold(
                onSuccess = { count ->
                    if (count > 0) RepeatedCallLookup.HIT else RepeatedCallLookup.MISS
                },
                onFailure = { RepeatedCallLookup.LOOKUP_FAILED },
            )
        }
    }

    /** HST-05: o usuario marca a chamada como legitima ou indesejada. */
    override suspend fun updateClassification(id: Long, classification: CallClassification): Unit =
        withContext(io) { dao.updateClassification(id, classification.name) }

    private companion object {
        /** Nada foi guardado: a configuracao do usuario manda nao deixar rastro local. */
        const val SEM_REGISTRO = 0L
    }
}

/**
 * Mapeamento entidade->dominio. Enums voltam pelo `code`/`name` estavel e a leitura
 * e tolerante: uma linha com valor desconhecido cai no fallback em vez de derrubar
 * a tela inteira do historico.
 */
internal fun BlockedCallEntity.toDomain(): BlockedCallEntry = BlockedCallEntry(
    id = id,
    maskedNumber = maskedNumber,
    numberE164 = numberE164,
    timestampUtcMillis = timestampUtcMillis,
    reason = DecisionReason.entries.firstOrNull { it.code == reasonCode }
        ?: DecisionReason.UNKNOWN_NUMBER,
    notificationShown = notificationShown,
    classification = CallClassification.entries.firstOrNull { it.name == classification }
        ?: CallClassification.UNCLASSIFIED,
)

internal fun BlockedCallEntry.toEntity(): BlockedCallEntity = BlockedCallEntity(
    id = id,
    maskedNumber = maskedNumber,
    numberE164 = numberE164,
    timestampUtcMillis = timestampUtcMillis,
    reasonCode = reason.code,
    notificationShown = notificationShown,
    classification = classification.name,
)

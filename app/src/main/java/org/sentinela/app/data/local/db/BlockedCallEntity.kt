package org.sentinela.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historico local. Guarda o E.164 completo em coluna propria porque a Fase 8
 * precisa dele para "adicionar a whitelist a partir do historico" (HST-04).
 * Isso NAO afrouxa a privacidade: log, notificacao e crash report continuam
 * usando somente `masked_number`, e o banco fica fora do backup (PRV-03).
 *
 * NUNCA adicionar coluna de nome de contato aqui.
 */
@Entity(
    tableName = "blocked_call",
    indices = [Index(value = ["timestamp_utc_millis"])],
)
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "masked_number") val maskedNumber: String,
    @ColumnInfo(name = "number_e164") val numberE164: String? = null,
    @ColumnInfo(name = "timestamp_utc_millis") val timestampUtcMillis: Long,
    @ColumnInfo(name = "reason_code") val reasonCode: String,
    @ColumnInfo(name = "notification_shown") val notificationShown: Boolean,
    @ColumnInfo(name = "classification") val classification: String,
)

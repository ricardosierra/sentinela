package org.sentinela.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist",
    indices = [Index(value = ["number_key"], unique = true)],
)
data class WhitelistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /**
     * Chave devolvida pelo PhoneNumberNormalizer (contrato fechado na Fase 2):
     * E.164 para numeros normais, DIGITOS CRUS abaixo de PhoneNumbers.LIMIAR_CURTO (6),
     * para que codigos curtos como "190" possam entrar na whitelist.
     * A normalizacao acontece ANTES de chegar ao repositorio.
     */
    @ColumnInfo(name = "number_key") val numberKey: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long,
)

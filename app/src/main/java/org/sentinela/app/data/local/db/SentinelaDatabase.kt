package org.sentinela.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Unico banco do app. Uma cadeia de migracao, um schema exportado em `app/schemas/`.
 * `exportSchema = true` nao e estilo: o JSON e o oraculo dos testes de migracao.
 */
@Database(
    entities = [WhitelistEntity::class, BlockedCallEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SentinelaDatabase : RoomDatabase() {

    abstract fun whitelistDao(): WhitelistDao

    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        const val NAME = "sentinela.db"
    }
}

package org.sentinela.app.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QLT-03. Com apenas a versao 1 nao existe migracao a testar — o que se prova aqui
 * e que o HARNESS funciona: o JSON exportado em `app/schemas/` e consumivel pelo
 * MigrationTestHelper, entao a PRIMEIRA migracao real ja nasce verificavel.
 * Inventar uma migracao falsa so para ter um teste nao provaria nada.
 *
 * Se aparecer "Cannot find the schema file", a causa e o sourceSets do plano 03-01:
 * `getByName("androidTest").assets.srcDir("$projectDir/schemas")`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationHarnessTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /**
     * Sobrecarga NAO-deprecada do Room 2.8.4: `(Instrumentation, Class<out RoomDatabase>)`.
     * As que recebem `assetsFolder: String` estao marcadas como deprecated nesta versao.
     */
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation,
        SentinelaDatabase::class.java,
    )

    @After
    fun tearDown() {
        instrumentation.targetContext.deleteDatabase(PROD_CHECK_DB)
    }

    @Test
    fun schemaV1AbrePeloHelper() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            val tables = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
                while (c.moveToNext()) tables += c.getString(0)
            }
            assertTrue("tabela whitelist ausente na v1: $tables", "whitelist" in tables)
            assertTrue("tabela blocked_call ausente na v1: $tables", "blocked_call" in tables)
        }
    }

    /**
     * O banco de producao — mesma classe, mesma cadeia de migracoes que o AppContainer
     * monta — abre sem erro de validacao de schema e reporta a versao 1. Se a entidade
     * divergir do schema exportado, o Room recusa a abertura aqui.
     */
    @Test
    fun bancoDeProducaoAbreNaVersao1() {
        val ctx = instrumentation.targetContext
        val db = Room.databaseBuilder(ctx, SentinelaDatabase::class.java, PROD_CHECK_DB)
            .addMigrations(*SENTINELA_MIGRATIONS)
            .build()
        try {
            assertEquals(1, db.openHelper.writableDatabase.version)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-harness.db"
        const val PROD_CHECK_DB = "migration-prod-check.db"
    }
}

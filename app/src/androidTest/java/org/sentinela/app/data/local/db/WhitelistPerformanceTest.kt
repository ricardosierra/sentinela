package org.sentinela.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "perf-test.db"
private const val ENTRIES = 1_000
private const val WARMUP = 300
private const val SAMPLES = 500
private const val NANOS_POR_MS = 1_000_000.0
private const val P50_MAX_MS = 1.0
private const val P95_MAX_MS = 5.0
private const val INDEX_NAME = "index_whitelist_number_key"

/**
 * WLT-07 em duas provas separadas, porque elas provam coisas diferentes:
 *
 * 1. O INDICE e provado por `EXPLAIN QUERY PLAN` — deterministico. O cronometro NAO
 *    prova indice: medido na pesquisa da fase, full scan com 1.000 linhas da p50
 *    0,047 ms contra 0,032 ms do indexado, indistinguivel de ruido. Um teste de
 *    tempo que se proponha a falhar quando o indice sumir e falso-verde.
 * 2. O ORCAMENTO e medido por percentis com warmup, e so isso. Aqui no emulador so o
 *    p50 quebra o build; o p95 e reportado e cobrado em aparelho fisico na Phase 9.
 *
 * Banco EM ARQUIVO, nao em memoria: o plano de query e o custo de I/O precisam ser
 * realistas.
 */
@RunWith(AndroidJUnit4::class)
class WhitelistPerformanceTest {

    private lateinit var db: SentinelaDatabase
    private lateinit var dao: WhitelistDao

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(TEST_DB)
        db = Room.databaseBuilder(ctx, SentinelaDatabase::class.java, TEST_DB).build()
        dao = db.whitelistDao()
        db.openHelper.writableDatabase.run {
            beginTransaction()
            try {
                repeat(ENTRIES) { i ->
                    execSQL(
                        "INSERT INTO whitelist (number_key, description, enabled, created_at_utc_millis) " +
                            "VALUES ('+55119${"%08d".format(i)}', NULL, 1, 0)",
                    )
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DB)
    }

    @Test
    fun containsUsaIndiceEmVezDeFullScan() {
        val sql = "SELECT EXISTS(SELECT 1 FROM whitelist WHERE number_key = ? AND enabled = 1)"
        val plan = buildString {
            db.openHelper.writableDatabase
                .query("EXPLAIN QUERY PLAN $sql", arrayOf("+5511900000500"))
                .use { c -> while (c.moveToNext()) appendLine(c.getString(c.columnCount - 1)) }
        }
        println("SENTINELA|EQP|$plan")

        assertTrue("plano sem indice:\n$plan", plan.contains("USING INDEX $INDEX_NAME"))
        assertFalse("full scan detectado:\n$plan", plan.contains("SCAN whitelist"))
    }

    @Test
    fun containsCabeNoOrcamentoMedido() {
        val chave = "+5511900000500"
        repeat(WARMUP) { dao.containsBlocking(chave) }

        val amostras = DoubleArray(SAMPLES) {
            val inicio = System.nanoTime()
            dao.containsBlocking(chave)
            (System.nanoTime() - inicio) / NANOS_POR_MS
        }
        amostras.sort()
        val p50 = amostras[SAMPLES / 2]
        val p95 = amostras[(SAMPLES * 0.95).toInt()]
        val p99 = amostras[(SAMPLES * 0.99).toInt()]
        println("SENTINELA|contains|entries=$ENTRIES|p50=$p50|p95=$p95|p99=$p99")

        assertTrue("p50=$p50 ms — sinal estavel, esperado < $P50_MAX_MS ms", p50 < P50_MAX_MS)

        // DECISAO HUMANA (2026-07-29): o p95 NAO quebra mais o build AQUI, e o numero
        // de 5 ms NAO foi afrouxado — ele continua sendo o compromisso de produto, so
        // que verificado em aparelho fisico na Phase 9 (docs/TESTE-FISICO-SAMSUNG.md).
        // Motivo: no emulador o p95 falhou 2 de 8 execucoes sem nenhuma regressao real,
        // porque mede o scheduler do host tanto quanto o SQLite; aumentar a amostragem
        // piorou (6,21 ms com 1.000/2.000). Um teste que fica vermelho sem regressao
        // corroi a confianca na suite inteira. O p50 e o sinal estavel e segue quebrando
        // o build, e a garantia do indice continua sendo o EXPLAIN QUERY PLAN acima —
        // que e quem realmente pega a regressao estrutural.
        if (p95 >= P95_MAX_MS) {
            println(
                "SENTINELA|contains|AVISO p95=$p95 ms >= $P95_MAX_MS ms " +
                    "(nao falha no emulador; veredito na Phase 9 em aparelho fisico)",
            )
        }
    }
}

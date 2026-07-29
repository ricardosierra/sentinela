package org.sentinela.app.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sentinela.app.data.local.RoomWhitelistRepository
import org.sentinela.app.data.local.WhitelistEntry

/**
 * CRUD, busca, dedup e codigo curto contra o SQLite de verdade.
 *
 * Banco em memoria: rapido e isolado por teste. O builder NAO libera consulta na
 * main thread — `containsBlocking` roda na thread do teste, que nao e a main thread
 * do app, entao afrouxar essa checagem so esconderia uso indevido na producao.
 * O invariante do Bloco 5 casa ate em comentario, e isso e proposital.
 */
@RunWith(AndroidJUnit4::class)
class WhitelistDaoTest {

    private lateinit var db: SentinelaDatabase
    private lateinit var dao: WhitelistDao
    private lateinit var repo: RoomWhitelistRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SentinelaDatabase::class.java).build()
        dao = db.whitelistDao()
        repo = RoomWhitelistRepository(dao, Dispatchers.IO)
    }

    @After
    fun tearDown() = db.close()

    private fun entry(
        key: String,
        description: String? = null,
        enabled: Boolean = true,
        createdAt: Long = 1_000L,
    ) = WhitelistEntry(
        numberE164 = key,
        description = description,
        enabled = enabled,
        createdAtUtcMillis = createdAt,
    )

    @Test
    fun insertEObserveAllDevolveAEntrada() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao"))

        val all = repo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("+5511999998888", all.single().numberE164)
        assertEquals("Joao", all.single().description)
        assertTrue("id autogerado esperado", all.single().id > 0)
    }

    @Test
    fun updateDeDescricaoPersiste() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao"))
        val existente = repo.observeAll().first().single()

        repo.upsert(existente.copy(description = "Joao do gas"))

        val all = repo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Joao do gas", all.single().description)
        assertEquals(existente.id, all.single().id)
    }

    @Test
    fun toggleDeEnabledPersisteSemPerderORegistro() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao"))
        val existente = repo.observeAll().first().single()

        repo.upsert(existente.copy(enabled = false))

        assertFalse(repo.observeAll().first().single().enabled)
        assertEquals("desabilitar nao pode apagar", 1, repo.observeAll().first().size)
    }

    @Test
    fun deleteByIdRemove() = runBlocking {
        repo.upsert(entry("+5511999998888"))
        repo.upsert(entry("+5511911112222", createdAt = 2_000L))
        val alvo = repo.observeAll().first().first { it.numberE164 == "+5511911112222" }

        repo.delete(alvo.id)

        val restante = repo.observeAll().first()
        assertEquals(1, restante.size)
        assertEquals("+5511999998888", restante.single().numberE164)
    }

    @Test
    fun searchAchaPorDescricao() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao"))
        repo.upsert(entry("+5511911112222", "Maria", createdAt = 2_000L))

        val achados = repo.search("Joao").first()
        assertEquals(1, achados.size)
        assertEquals("+5511999998888", achados.single().numberE164)
    }

    @Test
    fun searchAchaPorTrechoDoNumero() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao"))
        repo.upsert(entry("+5511911112222", "Maria", createdAt = 2_000L))

        val achados = repo.search("9999").first()
        assertEquals(1, achados.size)
        assertEquals("Joao", achados.single().description)
        assertTrue(repo.search("naoexiste").first().isEmpty())
    }

    @Test
    fun mesmaChaveDuasVezesViaRepositorioNaoDuplica() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao", createdAt = 1_000L))
        repo.upsert(entry("+5511999998888", "Joao do gas", createdAt = 9_999L))

        val all = repo.observeAll().first()
        assertEquals("dedup falhou: duas linhas para a mesma chave", 1, all.size)
        assertEquals("Joao do gas", all.single().description)
        assertEquals("createdAt original perdido", 1_000L, all.single().createdAtUtcMillis)
    }

    @Test
    fun insertDuplicadoDiretoNoBancoViolaAConstraint() {
        // A garantia real e do banco, nao do repositorio: o indice UNICO precisa
        // recusar a segunda linha com a mesma chave. INSERT cru de proposito —
        // @Upsert intercepta a excecao (ver o teste seguinte) e esconderia a prova.
        val sql = "INSERT INTO whitelist (number_key, description, enabled, created_at_utc_millis) " +
            "VALUES ('+5511999998888', NULL, 1, ?)"
        db.openHelper.writableDatabase.execSQL(sql, arrayOf<Any>(1_000L))

        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL(sql, arrayOf<Any>(2_000L))
        }
    }

    @Test
    fun upsertComIdZeroNaChaveDuplicadaPerdeAAtualizacaoSilenciosamente() = runBlocking {
        // Este e o motivo de RoomWhitelistRepository resolver o id ANTES do upsert.
        // @Upsert captura a violacao do indice e cai num UPDATE por chave primaria;
        // com id = 0 nenhuma linha casa e a atualizacao evapora sem erro.
        dao.upsert(WhitelistEntity(numberKey = "190", description = "Policia", createdAtUtcMillis = 1_000L))

        dao.upsert(WhitelistEntity(numberKey = "190", description = "Emergencia", createdAtUtcMillis = 2_000L))

        val all = dao.observeAll().first()
        assertEquals("indice unico nao segurou a duplicata", 1, all.size)
        assertEquals("descricao antiga sobreviveu — dedup so funciona via repositorio", "Policia", all.single().description)
    }

    @Test
    fun containsBlockingAceitaCodigoCurto() = runBlocking {
        repo.upsert(entry("190", "Policia"))

        assertTrue(dao.containsBlocking("190"))
        assertNotNull(dao.findByKey("190"))
    }

    @Test
    fun containsBlockingFalseQuandoDesabilitado() = runBlocking {
        repo.upsert(entry("+5511999998888", "Joao", enabled = false))

        assertFalse(dao.containsBlocking("+5511999998888"))
        assertFalse(repo.contains("+5511999998888"))
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun containsBlockingFalseParaChaveAusente() = runBlocking {
        repo.upsert(entry("+5511999998888"))

        assertFalse(dao.containsBlocking("+5511900000000"))
    }
}

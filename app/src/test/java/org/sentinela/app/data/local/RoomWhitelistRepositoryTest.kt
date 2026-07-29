package org.sentinela.app.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomWhitelistRepositoryTest {

    private val dao = FakeWhitelistDao()
    private val repo = RoomWhitelistRepository(dao, UnconfinedTestDispatcher())

    private fun entry(
        key: String = "+5511999998888",
        description: String? = null,
        enabled: Boolean = true,
        createdAt: Long = 1_000L,
        id: Long = 0,
    ) = WhitelistEntry(
        id = id,
        numberE164 = key,
        description = description,
        enabled = enabled,
        createdAtUtcMillis = createdAt,
    )

    @Test
    fun `upsert de chave inedita cria uma linha`() = runTest {
        repo.upsert(entry(description = "Joao"))

        val all = repo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("+5511999998888", all.single().numberE164)
        assertEquals("Joao", all.single().description)
    }

    @Test
    fun `upsert da mesma chave nao duplica e atualiza a descricao`() = runTest {
        repo.upsert(entry(description = "Joao", createdAt = 1_000L))
        repo.upsert(entry(description = "Joao do gas", createdAt = 9_999L))

        val all = repo.observeAll().first()
        assertEquals("duplicata criou linha nova", 1, all.size)
        assertEquals("Joao do gas", all.single().description)
    }

    @Test
    fun `upsert duplicado preserva o createdAt original`() = runTest {
        repo.upsert(entry(description = "Joao", createdAt = 1_000L))
        repo.upsert(entry(description = "Joao do gas", createdAt = 9_999L))

        assertEquals(1_000L, repo.observeAll().first().single().createdAtUtcMillis)
    }

    @Test
    fun `upsert duplicado reaproveita o id existente`() = runTest {
        repo.upsert(entry())
        val id = repo.observeAll().first().single().id

        repo.upsert(entry(description = "renomeado"))

        assertEquals(id, repo.observeAll().first().single().id)
    }

    @Test
    fun `contains devolve true para chave presente e habilitada`() = runTest {
        repo.upsert(entry())

        assertTrue(repo.contains("+5511999998888"))
    }

    @Test
    fun `contains devolve false quando a entrada esta desabilitada`() = runTest {
        repo.upsert(entry(enabled = false))

        assertFalse(repo.contains("+5511999998888"))
        assertEquals("entrada desabilitada nao pode sumir", 1, repo.observeAll().first().size)
    }

    @Test
    fun `contains devolve false para chave ausente`() = runTest {
        repo.upsert(entry())

        assertFalse(repo.contains("+5511900000000"))
    }

    @Test
    fun `contains aceita codigo curto persistido como digitos crus`() = runTest {
        repo.upsert(entry(key = "190", description = "Policia"))

        assertTrue(repo.contains("190"))
    }

    @Test
    fun `contains usa o DAO nao-suspend do caminho quente`() = runTest {
        repo.upsert(entry())

        repo.contains("+5511999998888")

        assertEquals(1, dao.containsCalls)
    }

    @Test
    fun `mapper e round-trip fiel entre dominio e entidade`() = runTest {
        repo.upsert(entry(description = "Maria", enabled = false, createdAt = 4_242L))

        val domain = repo.observeAll().first().single()
        assertEquals("+5511999998888", domain.numberE164)
        assertEquals("Maria", domain.description)
        assertFalse(domain.enabled)
        assertEquals(4_242L, domain.createdAtUtcMillis)
        assertTrue("id do banco deve chegar ao dominio", domain.id > 0)
    }

    @Test
    fun `delete remove apenas a entrada indicada`() = runTest {
        repo.upsert(entry(key = "+5511999998888"))
        repo.upsert(entry(key = "190", createdAt = 2_000L))
        val alvo = repo.observeAll().first().first { it.numberE164 == "190" }

        repo.delete(alvo.id)

        val restante = repo.observeAll().first()
        assertEquals(1, restante.size)
        assertEquals("+5511999998888", restante.single().numberE164)
    }

    @Test
    fun `search acha por descricao e por trecho do numero`() = runTest {
        repo.upsert(entry(key = "+5511999998888", description = "Joao"))
        repo.upsert(entry(key = "+5511911112222", description = "Maria", createdAt = 2_000L))

        assertEquals("Joao", repo.search("Joao").first().single().description)
        assertEquals("+5511911112222", repo.search("1111").first().single().numberE164)
        assertTrue(repo.search("inexistente").first().isEmpty())
    }

    @Test
    fun `falha do DAO propaga em vez de virar falso-verde`() = runTest {
        dao.failNext = IllegalStateException("disco cheio")

        val erro = runCatching { repo.contains("190") }.exceptionOrNull()

        assertTrue("excecao do DAO foi engolida: $erro", erro is IllegalStateException)
        assertEquals("disco cheio", erro?.message)
    }

    @Test
    fun `falha do DAO no upsert propaga e nada e gravado`() = runTest {
        dao.failNext = IllegalStateException("banco somente leitura")

        val erro = runCatching { repo.upsert(entry()) }.exceptionOrNull()

        assertTrue("excecao do DAO foi engolida: $erro", erro is IllegalStateException)
        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun `observeAll vazio quando nada foi gravado`() = runTest {
        assertTrue(repo.observeAll().first().isEmpty())
        assertTrue(dao.snapshot.isEmpty())
    }
}

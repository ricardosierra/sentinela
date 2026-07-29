package org.sentinela.app.data.local.db

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O schema exportado e um CONTRATO versionado, nao um subproduto do build:
 * e o oraculo contra o qual todo teste de migracao futuro compara. Se
 * `exportSchema` for desligado, ou se o indice unico da whitelist sumir, este
 * teste fica vermelho — nao adianta a inspecao manual passar.
 *
 * JVM puro de proposito: sem Room, sem Android, sem Robolectric. Le o diretorio
 * `schemas/` relativo ao modulo `app/`, que e o working dir do teste unitario.
 */
class SchemaExportTest {

    private val schemasDir = File("schemas")

    private val schemaV1: File
        get() = requireNotNull(
            schemasDir.listFiles()
                ?.filter { it.isDirectory }
                ?.map { File(it, "1.json") }
                ?.firstOrNull { it.isFile },
        ) { "nenhum 1.json sob ${schemasDir.absolutePath} — exportSchema desligado?" }

    @Test
    fun `schemas exportados existem`() {
        assertTrue(
            "diretorio de schemas ausente em ${schemasDir.absolutePath}",
            schemasDir.isDirectory,
        )
        val dirs = schemasDir.listFiles()?.filter { it.isDirectory }.orEmpty()
        assertTrue(
            "nenhum diretorio de banco sob schemas/ — o KSP nao exportou nada",
            dirs.isNotEmpty(),
        )
        assertTrue(
            "1.json ausente: o schema da v1 precisa estar versionado",
            schemaV1.isFile,
        )
        assertTrue(
            "1.json esta vazio — export truncado nao serve de oraculo de migracao",
            schemaV1.length() > 0,
        )
    }

    @Test
    fun `schema v1 declara versao 1`() {
        assertTrue(
            "1.json nao declara \"version\": 1",
            schemaV1.readText().contains("\"version\": 1"),
        )
    }

    @Test
    fun `schema v1 tem as duas tabelas`() {
        val json = schemaV1.readText()
        assertTrue(
            "tabela whitelist ausente do schema exportado",
            json.contains("\"tableName\": \"whitelist\""),
        )
        assertTrue(
            "tabela blocked_call ausente do schema exportado",
            json.contains("\"tableName\": \"blocked_call\""),
        )
    }

    @Test
    fun `whitelist tem indice unico na chave`() {
        val json = schemaV1.readText()
        assertTrue(
            "indice index_whitelist_number_key ausente — dedup deixaria de ser garantida pelo banco",
            json.contains("index_whitelist_number_key"),
        )
        assertTrue(
            "nenhum indice marcado como \"unique\": true no schema",
            json.contains("\"unique\": true"),
        )
    }

    /**
     * Fase 4: o app passou a ler a agenda do aparelho, e a regra dura e que nada de identidade de
     * terceiro chegue ao disco. Este caso le os VALORES de `columnName` de todos os schemas
     * exportados — nunca as chaves do JSON, que incluem `name` em varios lugares e casariam com o
     * padrao em qualquer build. Uma coluna nova de nome, foto ou chave de agenda fica vermelha
     * aqui antes de existir migracao para ela.
     */
    @Test
    fun `schema nao tem coluna de dado de contato`() {
        val vazamento = Regex("(^|_)(name|display|contact|photo|lookup|nome|agenda)")
        val colunas = schemasDir.listFiles()
            ?.filter { it.isDirectory }
            ?.flatMap { dir -> dir.listFiles()?.filter { it.isFile }.orEmpty() }
            .orEmpty()
            .flatMap { arquivo ->
                COLUNA.findAll(arquivo.readText()).map { it.groupValues[1] }
            }
            .distinct()

        assertTrue(
            "nenhum columnName encontrado nos schemas — o regex ou o export mudou de forma",
            colunas.isNotEmpty(),
        )
        val suspeitas = colunas.filter { vazamento.containsMatchIn(it) }
        assertTrue(
            "coluna de identidade de contato no schema exportado: $suspeitas — " +
                "contato so pode existir em memoria (docs/PRIVACIDADE.md)",
            suspeitas.isEmpty(),
        )
    }

    @Test
    fun `nenhum schema de rascunho versionado`() {
        val dirs = schemasDir.listFiles()?.filter { it.isDirectory }.orEmpty()
        assertEquals(
            "so pode haver um banco exportado; sobra de banco de rascunho: " +
                dirs.joinToString { it.name },
            1,
            dirs.size,
        )
        assertTrue(
            "o diretorio exportado deveria ser o do SentinelaDatabase, e nao ${dirs.first().name}",
            dirs.first().name.contains("SentinelaDatabase"),
        )
    }

    private companion object {
        val COLUNA = Regex("\"columnName\":\\s*\"([^\"]*)\"")
    }
}

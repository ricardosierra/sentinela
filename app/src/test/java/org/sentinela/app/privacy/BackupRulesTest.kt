package org.sentinela.app.privacy

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * PRV-03 / HST-06: whitelist, historico e configuracoes nunca saem do aparelho
 * por backup em nuvem ou device-transfer. Este teste LE o XML — declarar nao basta.
 *
 * Working dir do teste JVM = diretorio do modulo `app/` (fato fixado na Phase 2).
 */
class BackupRulesTest {

    private fun parse(file: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/xml/$file"))

    private fun element(file: String, tag: String): Element {
        val nodes = parse(file).getElementsByTagName(tag)
        assertEquals("elemento <$tag> nao encontrado em $file", 1, nodes.length)
        return nodes.item(0) as Element
    }

    private fun excludes(file: String, tag: String): Set<Pair<String, String>> {
        val nodes = element(file, tag).getElementsByTagName("exclude")
        return (0 until nodes.length).map { i ->
            val e = nodes.item(i) as Element
            e.getAttribute("domain") to e.getAttribute("path")
        }.toSet()
    }

    private fun includeCount(file: String): Int = parse(file).getElementsByTagName("include").length

    @Test
    fun cloudBackupExcluiDadosSensiveis() {
        assertTrue(
            "cloud-backup nao exclui tudo: ${excludes(RULES, "cloud-backup")}",
            excludes(RULES, "cloud-backup").containsAll(REQUIRED),
        )
    }

    @Test
    fun deviceTransferExcluiDadosSensiveis() {
        assertTrue(
            "device-transfer nao exclui tudo: ${excludes(RULES, "device-transfer")}",
            excludes(RULES, "device-transfer").containsAll(REQUIRED),
        )
    }

    @Test
    fun fullBackupLegadoExcluiDadosSensiveis() {
        assertTrue(
            "full-backup-content nao exclui tudo: ${excludes(LEGACY, "full-backup-content")}",
            excludes(LEGACY, "full-backup-content").containsAll(REQUIRED),
        )
    }

    @Test
    fun nenhumIncludeReintroduzDadoExcluido() {
        assertEquals("<include> em $RULES reintroduziria dado excluido", 0, includeCount(RULES))
        assertEquals("<include> em $LEGACY reintroduziria dado excluido", 0, includeCount(LEGACY))
    }

    @Test
    fun manifestApontaParaAsDuasRegras() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "manifest sem android:dataExtractionRules",
            manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""),
        )
        assertTrue(
            "manifest sem android:fullBackupContent (necessario em API 29-30)",
            manifest.contains("android:fullBackupContent=\"@xml/full_backup_content\""),
        )
    }

    private companion object {
        const val RULES = "data_extraction_rules.xml"
        const val LEGACY = "full_backup_content.xml"
        val REQUIRED = setOf(
            "database" to ".",
            "sharedpref" to ".",
            "file" to "datastore",
        )
    }
}

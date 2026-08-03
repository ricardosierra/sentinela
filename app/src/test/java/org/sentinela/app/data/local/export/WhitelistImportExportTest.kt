package org.sentinela.app.data.local.export

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.data.local.db.WhitelistEntity
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.NormalizationResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhitelistImportExportTest {

    private val normalizer = mockk<PhoneNumberNormalizer> {
        every { normalize(any(), any()) } answers { 
            val input = firstArg<String>()
            if (input.startsWith("+")) NormalizationResult.Valid(input) else NormalizationResult.Valid("+5511999999999")
        }
        every { normalize("+12345", any()) } returns NormalizationResult.Valid("+12345")
        every { normalize("+67890", any()) } returns NormalizationResult.Valid("+67890")
        every { normalize("invalid", any()) } returns NormalizationResult.Invalid("invalid")
    }

    @Test
    fun `export toJson creates correct structure`() {
        val entities = listOf(
            WhitelistEntity(numberKey = "+12345", description = "Test 1", enabled = true, createdAtUtcMillis = 1000L),
            WhitelistEntity(numberKey = "+67890", description = null, enabled = false, createdAtUtcMillis = 2000L)
        )
        val json = WhitelistExporter.exportToJson(entities)
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"+12345\""))
        assertTrue(json.contains("\"Test 1\""))
        assertTrue(json.contains("\"+67890\""))
        assertTrue(!json.contains("\"description\": null")) // Should omit null description
    }

    @Test
    fun `import parses valid json correctly`() {
        val json = """
            {
              "version": 1,
              "whitelist": [
                {
                  "numberKey": "+12345",
                  "description": "Test 1",
                  "enabled": true,
                  "createdAtUtcMillis": 1000
                },
                {
                  "numberKey": "+67890",
                  "enabled": false,
                  "createdAtUtcMillis": 2000
                }
              ]
            }
        """.trimIndent()
        
        val result = WhitelistImporter.parseJson(json, emptySet(), normalizer, 3000L)
        assertEquals(2, result.newEntities.size)
        assertEquals(0, result.duplicatesSkipped)
        assertEquals(0, result.invalidSkipped)
        
        assertEquals("+12345", result.newEntities[0].numberKey)
        assertEquals("Test 1", result.newEntities[0].description)
        assertTrue(result.newEntities[0].enabled)
        
        assertEquals("+67890", result.newEntities[1].numberKey)
        assertEquals(null, result.newEntities[1].description)
        assertTrue(!result.newEntities[1].enabled)
    }

    @Test
    fun `import handles empty json gracefully`() {
        val result = WhitelistImporter.parseJson("", emptySet(), normalizer, 0L)
        assertTrue(result.newEntities.isEmpty())
    }

    @Test
    fun `import handles malformed json gracefully`() {
        val result = WhitelistImporter.parseJson("{ malformed ", emptySet(), normalizer, 0L)
        assertTrue(result.newEntities.isEmpty())
    }

    @Test
    fun `import skips duplicates and invalid`() {
        val json = """
            {
              "version": 1,
              "whitelist": [
                { "numberKey": "+12345" },
                { "numberKey": "+12345" }, 
                { "numberKey": "+67890" },
                { "numberKey": "invalid" }
              ]
            }
        """.trimIndent()
        
        val result = WhitelistImporter.parseJson(json, setOf("+67890"), normalizer, 0L)
        assertEquals(1, result.newEntities.size) // Only first +12345
        assertEquals(2, result.duplicatesSkipped) // Second +12345 and +67890
        assertEquals(1, result.invalidSkipped) // "invalid"
    }

    @Test
    fun `import respects MAX_IMPORT_LIMIT`() {
        val limit = WhitelistImporter.MAX_IMPORT_LIMIT
        val sb = StringBuilder()
        sb.append("{ \"version\": 1, \"whitelist\": [")
        for (i in 0 until limit + 5) {
            sb.append("{ \"numberKey\": \"+$i\" }")
            if (i < limit + 4) sb.append(",")
        }
        sb.append("]}")
        
        val localNormalizer = mockk<PhoneNumberNormalizer> {
            every { normalize(any(), any()) } answers { NormalizationResult.Valid(firstArg<String>()) }
        }
        
        val result = WhitelistImporter.parseJson(sb.toString(), emptySet(), localNormalizer, 0L)
        assertEquals(limit, result.newEntities.size)
    }
}

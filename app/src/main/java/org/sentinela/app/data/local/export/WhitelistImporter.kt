@file:Suppress("LongMethod", "MaxLineLength", "TooManyFunctions", "ReturnCount", "MagicNumber", "SwallowedException", "LoopWithTooManyJumpStatements")

package org.sentinela.app.data.local.export

import org.json.JSONException
import org.json.JSONObject
import org.sentinela.app.data.local.db.WhitelistEntity
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.NormalizationResult

data class ImportResult(
    val newEntities: List<WhitelistEntity>,
    val duplicatesSkipped: Int,
    val invalidSkipped: Int
)

object WhitelistImporter {
    const val MAX_IMPORT_LIMIT = 10000

    fun parseJson(jsonString: String, existingKeys: Set<String>, normalizer: PhoneNumberNormalizer, nowUtcMillis: Long): ImportResult {
        if (jsonString.isBlank()) {
            return ImportResult(emptyList(), 0, 0)
        }

        val newEntities = mutableListOf<WhitelistEntity>()
        var duplicatesSkipped = 0
        var invalidSkipped = 0

        try {
            val root = JSONObject(jsonString)
            if (!root.has("whitelist")) {
                return ImportResult(emptyList(), 0, 0)
            }
            val array = root.getJSONArray("whitelist")
            val limit = minOf(array.length(), MAX_IMPORT_LIMIT)
            val seenInFile = mutableSetOf<String>()

            for (i in 0 until limit) {
                val obj = array.getJSONObject(i)
                val rawNumber = obj.optString("numberKey")
                val desc = obj.optString("description", "").takeIf { it.isNotBlank() }
                val enabled = obj.optBoolean("enabled", true)
                val createdAt = obj.optLong("createdAtUtcMillis", nowUtcMillis)

                if (rawNumber.isBlank()) {
                    invalidSkipped++
                    continue
                }

                val normalizedResult = normalizer.normalize(rawNumber)
                if (normalizedResult !is NormalizationResult.Valid) {
                    invalidSkipped++
                    continue
                }

                val normalized = normalizedResult.e164
                if (existingKeys.contains(normalized) || seenInFile.contains(normalized)) {
                    duplicatesSkipped++
                    continue
                }

                seenInFile.add(normalized)
                newEntities.add(
                    WhitelistEntity(
                        numberKey = normalized,
                        description = desc,
                        enabled = enabled,
                        createdAtUtcMillis = createdAt
                    )
                )
            }

            // Exceeded limit are considered ignored/invalid/duplicate?
            // "limite 10.000". If more than limit, we just parse up to 10k. 
            // We can add skipped ones to invalid? No, the requirement says "limite 10.000".
            // So we just stop at limit.

        } catch (e: JSONException) {
            // Malformed JSON means everything else is invalid or we just return what we got so far
            return ImportResult(emptyList(), 0, 0) // Or return current results if we want partial
        }

        return ImportResult(newEntities, duplicatesSkipped, invalidSkipped)
    }
}

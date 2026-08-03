package org.sentinela.app.data.local.export

import org.json.JSONArray
import org.json.JSONObject
import org.sentinela.app.data.local.db.WhitelistEntity

object WhitelistExporter {
    fun exportToJson(entities: List<WhitelistEntity>): String {
        val jsonArray = JSONArray()
        for (entity in entities) {
            val jsonObject = JSONObject()
            jsonObject.put("numberKey", entity.numberKey)
            entity.description?.let { jsonObject.put("description", it) }
            jsonObject.put("enabled", entity.enabled)
            jsonObject.put("createdAtUtcMillis", entity.createdAtUtcMillis)
            jsonArray.put(jsonObject)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("whitelist", jsonArray)
        return root.toString(2)
    }
}

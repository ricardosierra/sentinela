// REFACTOR: esta lista de 7 supressoes foi copiada igual para 9 arquivos do projeto. Aqui ela
//  desliga `TooManyFunctions` e `LongMethod` num arquivo de UMA funcao, e `SwallowedException`
//  bem onde o `catch (e: JSONException)` abaixo engole o erro que o usuario precisava ver.
//  Suprimir regra por regra, no ponto de uso, como o resto do projeto ja faz.
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
                // TODO: `getJSONObject` lanca quando o elemento nao e objeto, e a captura la embaixo
                //  descarta TODAS as entradas ja lidas — um item torto na posicao 500 joga fora os
                //  499 validos. Usar `optJSONObject`, contar como invalido e seguir o laco.
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

            // REFACTOR: comentarios de rascunho em ingles, com pergunta em aberto, num projeto cujo
            //  codigo e todo comentado em portugues. Decidir e escrever a decisao, ou apagar.
            // TODO: o que passa de MAX_IMPORT_LIMIT some sem contagem e sem aviso — um arquivo com
            //  15.000 numeros importa 10.000 e o usuario acredita que levou a lista inteira.
            //  Devolver o excedente no resultado para a tela poder avisar.

        } catch (e: JSONException) {
            // TODO: arquivo corrompido devolve exatamente o mesmo que um arquivo vazio,
            //  `(emptyList(), 0, 0)`, entao o ViewModel emite `Imported(0,0,0)` e o usuario le
            //  "0 adicionados, 0 invalidos" — mensagem de SUCESSO para um arquivo ilegivel.
            //  `WhitelistFeedback.ImportFailed` existe mas so e alcancado por falha de leitura do
            //  stream, nunca por erro de formato. Viola o invariante do projeto de que erro de
            //  importacao sempre avisa na tela. Precisa de um sinal de "malformado" no ImportResult.
            return ImportResult(emptyList(), 0, 0) // Or return current results if we want partial
        }

        return ImportResult(newEntities, duplicatesSkipped, invalidSkipped)
    }
}

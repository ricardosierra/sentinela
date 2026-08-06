@file:Suppress("LoopWithTooManyJumpStatements")

package org.sentinela.app.data.local.export

import org.json.JSONException
import org.json.JSONObject
import org.sentinela.app.data.local.db.WhitelistEntity
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * Resultado da leitura de um arquivo de backup.
 *
 * [malformed] existe para separar duas situações que antes chegavam à tela como a MESMA coisa: um
 * arquivo que não é um backup deste aplicativo (ou está corrompido) e um backup válido que não
 * tinha nada a acrescentar. As duas produziam `(0, 0, 0)`, a tela anunciava "0 adicionados,
 * 0 inválidos" — mensagem de sucesso — e o usuário não tinha como descobrir que escolheu o arquivo
 * errado. O invariante do projeto exige aviso visível para erro de importação.
 *
 * [ignoredOverLimit] conta o que ficou de fora por causa de [WhitelistImporter.MAX_IMPORT_LIMIT].
 * Descartar em silêncio faria o usuário acreditar que importou a lista inteira.
 */
data class ImportResult(
    val newEntities: List<WhitelistEntity>,
    val duplicatesSkipped: Int,
    val invalidSkipped: Int,
    val ignoredOverLimit: Int = 0,
    val malformed: Boolean = false,
)

object WhitelistImporter {
    const val MAX_IMPORT_LIMIT = 10_000

    /** Nada foi lido. A tela precisa dizer isso, em vez de anunciar uma importação vazia. */
    private val ARQUIVO_INVALIDO = ImportResult(
        newEntities = emptyList(),
        duplicatesSkipped = 0,
        invalidSkipped = 0,
        malformed = true,
    )

    fun parseJson(
        jsonString: String,
        existingKeys: Set<String>,
        normalizer: PhoneNumberNormalizer,
        nowUtcMillis: Long,
    ): ImportResult {
        if (jsonString.isBlank()) return ARQUIVO_INVALIDO

        // A moldura do arquivo é a única coisa cuja falha invalida tudo: sem a lista não há o que
        // ler. Daqui para baixo, defeito é sempre local a um item e nunca derruba o resto.
        val array = try {
            val root = JSONObject(jsonString)
            if (!root.has(CAMPO_LISTA)) return ARQUIVO_INVALIDO
            root.getJSONArray(CAMPO_LISTA)
        } catch (_: JSONException) {
            return ARQUIVO_INVALIDO
        }

        val newEntities = mutableListOf<WhitelistEntity>()
        val seenInFile = mutableSetOf<String>()
        var duplicatesSkipped = 0
        var invalidSkipped = 0
        val limit = minOf(array.length(), MAX_IMPORT_LIMIT)

        for (i in 0 until limit) {
            // Elemento que não é objeto conta como inválido e a leitura CONTINUA. Abortar aqui
            // descartava todos os números já lidos: um item torto na posição 500 jogava fora os
            // 499 válidos que vieram antes dele.
            val obj = array.optJSONObject(i)
            if (obj == null) {
                invalidSkipped++
                continue
            }

            val rawNumber = obj.optString(CAMPO_NUMERO)
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
                    description = obj.optString(CAMPO_DESCRICAO, "").takeIf { it.isNotBlank() },
                    enabled = obj.optBoolean(CAMPO_ATIVO, true),
                    createdAtUtcMillis = obj.optLong(CAMPO_CRIADO_EM, nowUtcMillis),
                ),
            )
        }

        return ImportResult(
            newEntities = newEntities,
            duplicatesSkipped = duplicatesSkipped,
            invalidSkipped = invalidSkipped,
            ignoredOverLimit = array.length() - limit,
        )
    }

    private const val CAMPO_LISTA = "whitelist"
    private const val CAMPO_NUMERO = "numberKey"
    private const val CAMPO_DESCRICAO = "description"
    private const val CAMPO_ATIVO = "enabled"
    private const val CAMPO_CRIADO_EM = "createdAtUtcMillis"
}

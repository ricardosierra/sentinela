package org.sentinela.app.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.domain.DecisionReason

/**
 * O historico do usuario e persistido por `code`/`name`, NUNCA por `ordinal`:
 * reordenar um enum nao pode reescrever o passado dele.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round-trip de todas as entradas de DecisionReason`() {
        DecisionReason.entries.forEach { reason ->
            val stored = converters.fromDecisionReason(reason)
            assertEquals(
                "DecisionReason deve ser persistido pelo code estavel",
                reason.code,
                stored,
            )
            assertEquals(
                "round-trip de $reason deve devolver a mesma entrada",
                reason,
                converters.toDecisionReason(stored),
            )
        }
    }

    @Test
    fun `round-trip de todas as entradas de CallClassification`() {
        CallClassification.entries.forEach { classification ->
            val stored = converters.fromCallClassification(classification)
            assertEquals(
                "CallClassification deve ser persistida pelo name",
                classification.name,
                stored,
            )
            assertEquals(
                "round-trip de $classification deve devolver a mesma entrada",
                classification,
                converters.toCallClassification(stored),
            )
        }
    }

    @Test
    fun `valor desconhecido de DecisionReason cai no fallback sem lancar`() {
        assertEquals(
            "code desconhecido deve virar UNKNOWN_NUMBER, nunca excecao",
            DecisionReason.UNKNOWN_NUMBER,
            converters.toDecisionReason("code_que_nao_existe_mais"),
        )
        assertEquals(
            "null deve virar UNKNOWN_NUMBER",
            DecisionReason.UNKNOWN_NUMBER,
            converters.toDecisionReason(null),
        )
    }

    @Test
    fun `valor desconhecido de CallClassification cai no fallback sem lancar`() {
        assertEquals(
            "name desconhecido deve virar UNCLASSIFIED, nunca excecao",
            CallClassification.UNCLASSIFIED,
            converters.toCallClassification("SPAM_INVENTADO"),
        )
        assertEquals(
            "null deve virar UNCLASSIFIED",
            CallClassification.UNCLASSIFIED,
            converters.toCallClassification(null),
        )
    }

    @Test
    fun `nenhum code de DecisionReason colide`() {
        val codes = DecisionReason.entries.map { it.code }
        assertEquals(
            "cada DecisionReason precisa de um code unico, senao o round-trip perde entrada",
            codes.size,
            codes.toSet().size,
        )
    }

    @Test
    fun `o valor persistido nunca e o ordinal`() {
        // Se alguem trocar o conversor por `ordinal`, o valor gravado viraria digito.
        DecisionReason.entries.forEach { reason ->
            assertFalse(
                "code de $reason nao pode ser numerico (seria ordinal disfarcado)",
                converters.fromDecisionReason(reason).all { it.isDigit() },
            )
        }
        CallClassification.entries.forEach { classification ->
            assertFalse(
                "name de $classification nao pode ser numerico",
                converters.fromCallClassification(classification).all { it.isDigit() },
            )
        }
    }
}

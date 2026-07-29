package org.sentinela.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-04: reason codes são o único detalhe de decisão que pode aparecer em log
 * técnico — precisam ser opacos, estáveis e livres de qualquer dado pessoal.
 */
class DecisionReasonTest {

    @Test
    fun `todo reason code usa apenas letras minusculas e underscore`() {
        val allowed = Regex("[a-z_]+")
        DecisionReason.entries.forEach { reason ->
            assertTrue(
                "reason code fora do formato opaco: ${reason.name}=${reason.code}",
                allowed.matches(reason.code),
            )
        }
    }

    @Test
    fun `nenhum reason code e vazio e todos sao unicos`() {
        val codes = DecisionReason.entries.map { it.code }
        assertTrue("existe reason code vazio", codes.none { it.isEmpty() })
        assertEquals("reason codes duplicados", DecisionReason.entries.size, codes.distinct().size)
    }

    @Test
    fun `nenhum reason code contem digito`() {
        DecisionReason.entries.forEach { reason ->
            assertTrue(
                "reason code com dígito pode carregar dado de telefone: ${reason.name}",
                reason.code.none { it.isDigit() },
            )
        }
    }

    @Test
    fun `o conjunto de reason codes permanece com dez entradas`() {
        assertEquals(
            "reason code novo exige revisão de privacidade antes de entrar",
            EXPECTED_REASON_COUNT,
            DecisionReason.entries.size,
        )
    }

    private companion object {
        const val EXPECTED_REASON_COUNT = 10
    }
}

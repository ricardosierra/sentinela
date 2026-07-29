package org.sentinela.app.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A regra de permissao em runtime e UMA so no app. Esta suite trava a regra generica;
 * a suite de contatos (Fase 4) trava que a fachada antiga continua devolvendo o mesmo.
 */
class RuntimePermissionAskTest {

    @Test
    fun `concedida e GRANTED mesmo sem nunca termos perguntado`() {
        assertEquals(
            RuntimePermissionAsk.GRANTED,
            runtimePermissionAsk(granted = true, alreadyAsked = false, rationale = false),
        )
    }

    @Test
    fun `concedida vence os outros dois sinais`() {
        assertEquals(
            RuntimePermissionAsk.GRANTED,
            runtimePermissionAsk(granted = true, alreadyAsked = true, rationale = true),
        )
    }

    @Test
    fun `sem permissao e sem nunca ter perguntado e NEVER_ASKED`() {
        assertEquals(
            RuntimePermissionAsk.NEVER_ASKED,
            runtimePermissionAsk(granted = false, alreadyAsked = false, rationale = false),
        )
    }

    @Test
    fun `rationale verdadeiro sem alreadyAsked continua NEVER_ASKED`() {
        assertEquals(
            RuntimePermissionAsk.NEVER_ASKED,
            runtimePermissionAsk(granted = false, alreadyAsked = true.not(), rationale = true),
        )
    }

    @Test
    fun `negada uma vez com rationale e DENIED_ONCE`() {
        assertEquals(
            RuntimePermissionAsk.DENIED_ONCE,
            runtimePermissionAsk(granted = false, alreadyAsked = true, rationale = true),
        )
    }

    @Test
    fun `ja perguntamos e a plataforma nao quer rationale e DENIED_PERMANENTLY`() {
        assertEquals(
            RuntimePermissionAsk.DENIED_PERMANENTLY,
            runtimePermissionAsk(granted = false, alreadyAsked = true, rationale = false),
        )
    }

    @Test
    fun `rationale falso sozinho e ambiguo e so o flag persistido decide`() {
        val semFlag = runtimePermissionAsk(false, alreadyAsked = false, rationale = false)
        val comFlag = runtimePermissionAsk(false, alreadyAsked = true, rationale = false)
        assertEquals(RuntimePermissionAsk.NEVER_ASKED, semFlag)
        assertEquals(RuntimePermissionAsk.DENIED_PERMANENTLY, comFlag)
    }

    @Test
    fun `a tabela das oito combinacoes booleanas nao tem buraco nem surpresa`() {
        val esperado = listOf(
            Triple(false, false, false) to RuntimePermissionAsk.NEVER_ASKED,
            Triple(false, false, true) to RuntimePermissionAsk.NEVER_ASKED,
            Triple(false, true, false) to RuntimePermissionAsk.DENIED_PERMANENTLY,
            Triple(false, true, true) to RuntimePermissionAsk.DENIED_ONCE,
            Triple(true, false, false) to RuntimePermissionAsk.GRANTED,
            Triple(true, false, true) to RuntimePermissionAsk.GRANTED,
            Triple(true, true, false) to RuntimePermissionAsk.GRANTED,
            Triple(true, true, true) to RuntimePermissionAsk.GRANTED,
        )
        esperado.forEach { (entrada, saida) ->
            val (granted, alreadyAsked, rationale) = entrada
            assertEquals(
                "granted=$granted alreadyAsked=$alreadyAsked rationale=$rationale",
                saida,
                runtimePermissionAsk(granted, alreadyAsked, rationale),
            )
        }
    }

    @Test
    fun `canRequest e verdadeiro so em NEVER_ASKED e DENIED_ONCE`() {
        assertTrue(RuntimePermissionAsk.NEVER_ASKED.canRequest)
        assertTrue(RuntimePermissionAsk.DENIED_ONCE.canRequest)
        assertFalse(RuntimePermissionAsk.GRANTED.canRequest)
        assertFalse(RuntimePermissionAsk.DENIED_PERMANENTLY.canRequest)
    }

    @Test
    fun `shouldOfferSystemSettings e verdadeiro so em DENIED_PERMANENTLY`() {
        assertTrue(RuntimePermissionAsk.DENIED_PERMANENTLY.shouldOfferSystemSettings)
        RuntimePermissionAsk.entries
            .filter { it != RuntimePermissionAsk.DENIED_PERMANENTLY }
            .forEach { assertFalse(it.name, it.shouldOfferSystemSettings) }
    }

    @Test
    fun `nunca oferecemos pedir e ir as configuracoes ao mesmo tempo`() {
        RuntimePermissionAsk.entries.forEach {
            assertFalse(it.name, it.canRequest && it.shouldOfferSystemSettings)
        }
    }

    @Test
    fun `o enum tem exatamente quatro entradas na ordem contratada`() {
        assertEquals(4, RuntimePermissionAsk.entries.size)
        assertEquals(
            listOf("GRANTED", "NEVER_ASKED", "DENIED_ONCE", "DENIED_PERMANENTLY"),
            RuntimePermissionAsk.entries.map { it.name },
        )
    }
}

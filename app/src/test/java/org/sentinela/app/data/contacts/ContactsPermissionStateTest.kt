package org.sentinela.app.data.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava a maquina de estado da permissao de contatos (CTT-01).
 *
 * A tabela abaixo e escrita a mao, nunca derivada da funcao sob teste: e a mesma regra
 * usada na matriz de decisao da Fase 2. O caso que justifica a fase inteira e o par
 * (alreadyAsked=false, rationale=false) contra (alreadyAsked=true, rationale=false):
 * a plataforma devolve o MESMO rationale=false nos dois, e so o flag persistido separa
 * "nunca perguntamos" de "negada permanentemente".
 */
class ContactsPermissionStateTest {

    // --- granted vence tudo --------------------------------------------------

    @Test
    fun `concedida e GRANTED mesmo sem nunca termos perguntado`() {
        assertEquals(
            ContactsPermissionState.GRANTED,
            contactsPermissionState(granted = true, alreadyAsked = false, rationale = false),
        )
    }

    @Test
    fun `concedida e GRANTED mesmo com alreadyAsked e rationale verdadeiros`() {
        assertEquals(
            ContactsPermissionState.GRANTED,
            contactsPermissionState(granted = true, alreadyAsked = true, rationale = true),
        )
    }

    // --- o flag persistido vence o rationale ---------------------------------

    @Test
    fun `sem permissao e sem nunca ter perguntado e NEVER_ASKED`() {
        assertEquals(
            ContactsPermissionState.NEVER_ASKED,
            contactsPermissionState(granted = false, alreadyAsked = false, rationale = false),
        )
    }

    @Test
    fun `rationale verdadeiro sem alreadyAsked continua NEVER_ASKED`() {
        assertEquals(
            ContactsPermissionState.NEVER_ASKED,
            contactsPermissionState(granted = false, alreadyAsked = false, rationale = true),
        )
    }

    @Test
    fun `negada uma vez com rationale e DENIED_ONCE`() {
        assertEquals(
            ContactsPermissionState.DENIED_ONCE,
            contactsPermissionState(granted = false, alreadyAsked = true, rationale = true),
        )
    }

    @Test
    fun `ja perguntamos e a plataforma nao quer rationale e DENIED_PERMANENTLY`() {
        assertEquals(
            ContactsPermissionState.DENIED_PERMANENTLY,
            contactsPermissionState(granted = false, alreadyAsked = true, rationale = false),
        )
    }

    @Test
    fun `rationale falso sozinho e ambiguo e so o flag decide o estado`() {
        val semFlag = contactsPermissionState(false, alreadyAsked = false, rationale = false)
        val comFlag = contactsPermissionState(false, alreadyAsked = true, rationale = false)

        assertEquals(ContactsPermissionState.NEVER_ASKED, semFlag)
        assertEquals(ContactsPermissionState.DENIED_PERMANENTLY, comFlag)
    }

    @Test
    fun `a tabela das oito combinacoes booleanas nao tem buraco nem surpresa`() {
        val esperado = mapOf(
            Triple(true, false, false) to ContactsPermissionState.GRANTED,
            Triple(true, false, true) to ContactsPermissionState.GRANTED,
            Triple(true, true, false) to ContactsPermissionState.GRANTED,
            Triple(true, true, true) to ContactsPermissionState.GRANTED,
            Triple(false, false, false) to ContactsPermissionState.NEVER_ASKED,
            Triple(false, false, true) to ContactsPermissionState.NEVER_ASKED,
            Triple(false, true, true) to ContactsPermissionState.DENIED_ONCE,
            Triple(false, true, false) to ContactsPermissionState.DENIED_PERMANENTLY,
        )

        esperado.forEach { (entrada, saida) ->
            val (granted, alreadyAsked, rationale) = entrada
            assertEquals(
                "granted=$granted alreadyAsked=$alreadyAsked rationale=$rationale",
                saida,
                contactsPermissionState(granted, alreadyAsked, rationale),
            )
        }
    }

    // --- derivados que a Fase 7 consome --------------------------------------

    @Test
    fun `canRequest e verdadeiro so em NEVER_ASKED e DENIED_ONCE`() {
        assertTrue(ContactsPermissionState.NEVER_ASKED.canRequest)
        assertTrue(ContactsPermissionState.DENIED_ONCE.canRequest)
        assertFalse(ContactsPermissionState.GRANTED.canRequest)
        assertFalse(ContactsPermissionState.DENIED_PERMANENTLY.canRequest)
    }

    @Test
    fun `shouldOfferSystemSettings e verdadeiro so em DENIED_PERMANENTLY`() {
        assertTrue(ContactsPermissionState.DENIED_PERMANENTLY.shouldOfferSystemSettings)
        assertFalse(ContactsPermissionState.GRANTED.shouldOfferSystemSettings)
        assertFalse(ContactsPermissionState.NEVER_ASKED.shouldOfferSystemSettings)
        assertFalse(ContactsPermissionState.DENIED_ONCE.shouldOfferSystemSettings)
    }

    @Test
    fun `nunca oferecemos pedir e ir as configuracoes ao mesmo tempo`() {
        ContactsPermissionState.entries.forEach { estado ->
            assertFalse(
                "estado $estado oferece pedido E atalho — o usuario seria insistido",
                estado.canRequest && estado.shouldOfferSystemSettings,
            )
        }
    }

    // --- lista travada, como DecisionReasonTest da Fase 2 ---------------------

    @Test
    fun `o enum tem exatamente quatro entradas na ordem contratada`() {
        assertEquals(4, ContactsPermissionState.entries.size)
        assertEquals(
            listOf("GRANTED", "NEVER_ASKED", "DENIED_ONCE", "DENIED_PERMANENTLY"),
            ContactsPermissionState.entries.map { it.name },
        )
    }
}

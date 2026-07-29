package org.sentinela.app.telecom.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O estado do modo discador é derivado do sistema, nunca lido de um valor gravado. Estes casos
 * travam a precedência inteira, incluindo o ponto que mais facilmente se inverte por descuido: o
 * papel realmente detido vence qualquer intenção que o aplicativo tenha guardado.
 */
class DialerModeStateTest {

    @Test
    fun `sem o papel disponivel no aparelho o modo e indisponivel`() {
        assertEquals(
            DialerModeState.UNAVAILABLE,
            dialerModeState(
                roleAvailable = false,
                roleHeld = false,
                contactsGranted = true,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `aparelho sem telefonia continua indisponivel mesmo com tudo mais favoravel`() {
        assertEquals(
            DialerModeState.UNAVAILABLE,
            dialerModeState(
                roleAvailable = false,
                roleHeld = true,
                contactsGranted = true,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `com o papel detido o modo esta ativo`() {
        assertEquals(
            DialerModeState.ACTIVE,
            dialerModeState(
                roleAvailable = true,
                roleHeld = true,
                contactsGranted = true,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `o papel detido vence a intencao gravada em sentido contrario`() {
        // O usuário pode ter escolhido o aplicativo direto nas configurações do sistema, sem
        // passar pela tela de ativação. Perguntar ao sistema é a única fonte de verdade.
        assertEquals(
            DialerModeState.ACTIVE,
            dialerModeState(
                roleAvailable = true,
                roleHeld = true,
                contactsGranted = true,
                userOptedIn = false,
            ),
        )
    }

    @Test
    fun `com a leitura da agenda negada o modo nao e oferecido`() {
        assertEquals(
            DialerModeState.BLOCKED_BY_CONTACTS,
            dialerModeState(
                roleAvailable = true,
                roleHeld = false,
                contactsGranted = false,
                userOptedIn = false,
            ),
        )
    }

    @Test
    fun `agenda negada bloqueia mesmo com intencao gravada`() {
        assertEquals(
            DialerModeState.BLOCKED_BY_CONTACTS,
            dialerModeState(
                roleAvailable = true,
                roleHeld = false,
                contactsGranted = false,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `papel perdido com intencao gravada produz o estado de papel perdido`() {
        assertEquals(
            DialerModeState.ROLE_LOST,
            dialerModeState(
                roleAvailable = true,
                roleHeld = false,
                contactsGranted = true,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `sem intencao gravada e sem o papel o modo apenas e oferecido`() {
        assertEquals(
            DialerModeState.OFFERED,
            dialerModeState(
                roleAvailable = true,
                roleHeld = false,
                contactsGranted = true,
                userOptedIn = false,
            ),
        )
    }

    @Test
    fun `papel detido com agenda negada continua ativo, porque quem manda e o sistema`() {
        // Situação real e desconfortável: o usuário revogou a agenda depois de ativar o modo. O
        // aplicativo continua sendo o telefone padrão de fato, e mentir sobre isso deixaria a tela
        // divergente do aparelho. Quem avisa sobre a agenda é a tela, não este estado.
        assertEquals(
            DialerModeState.ACTIVE,
            dialerModeState(
                roleAvailable = true,
                roleHeld = true,
                contactsGranted = false,
                userOptedIn = true,
            ),
        )
    }

    @Test
    fun `reverter cancela o aviso de chamada e limpa o armazem da sessao`() {
        val escopo = CoroutineScope(Dispatchers.Unconfined)
        val store = CallSessionStore(escopo)
        store.attach(ControlesQueContam())
        store.onCallAdded(ESTADO_ATIVA, CallIdentity(fullNumber = "+5511999998888"), "abc")
        var cancelou = false

        onDialerModeReverted(store) { cancelou = true }

        assertTrue(cancelou)
        assertNull(store.session)
        assertNull(store.controls)
        assertNull(store.opaqueCallId)
    }

    @Test
    fun `reverter sem aviso postado nao exige costura de notificacao`() {
        val store = CallSessionStore(CoroutineScope(Dispatchers.Unconfined))
        store.attach(ControlesQueContam())

        onDialerModeReverted(store)

        assertNull(store.session)
    }

    private class ControlesQueContam : CallControls {
        override fun answer() = Unit
        override fun reject() = Unit
        override fun hangUp() = Unit
        override fun setMuted(muted: Boolean) = Unit
        override fun setSpeakerOn(on: Boolean) = Unit
        override fun playDtmf(digit: Char) = Unit
        override fun stopDtmf() = Unit
    }

    private companion object {
        const val ESTADO_ATIVA = 4
    }
}

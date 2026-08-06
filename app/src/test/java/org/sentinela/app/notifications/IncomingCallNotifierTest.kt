package org.sentinela.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.sentinela.app.telecom.call.MaskedCallIdentity

/**
 * O que este arquivo prova é o objeto **realmente publicado** — não a intenção do código. Por isso
 * Robolectric e o gerenciador de notificações sombreado: canal, pedido de tela cheia, ações e
 * conteúdo são lidos de volta do que foi postado.
 *
 * A capacidade de ocupar a tela entra por costura (`fullScreenAllowed`) em vez de ser lida do
 * ambiente sombreado: a consulta da versão 34 não tem sombra nesta versão do Robolectric, e o caso
 * de degradação — o mais importante desta suíte — precisa ser exercitável na máquina virtual.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IncomingCallNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private fun notifier(telaCheiaPermitida: Boolean = true) =
        IncomingCallNotifier(context) { telaCheiaPermitida }

    private val identidade = MaskedCallIdentity(maskedNumber = MASCARA)

    private fun publicarRecebida(
        identity: MaskedCallIdentity = identidade,
        telaCheiaPermitida: Boolean = true,
    ): Notification {
        notifier(telaCheiaPermitida).notifyIncoming(identity)
        return shadowOf(manager).allNotifications.last()
    }

    private fun publicarEmCurso(identity: MaskedCallIdentity = identidade): Notification {
        notifier().notifyOngoing(identity)
        return shadowOf(manager).allNotifications.last()
    }

    /** Todo texto legível do objeto: extras, versão pública e ticker. */
    private fun textos(notification: Notification): List<String> {
        val de = { bundle: Bundle? ->
            bundle?.keySet().orEmpty().mapNotNull { bundle?.get(it)?.toString() }
        }
        return de(notification.extras) +
            de(notification.publicVersion?.extras) +
            listOfNotNull(notification.tickerText?.toString())
    }

    /** Valor do extra de ação de cada ação publicada, na ordem em que a plataforma as devolveu. */
    private fun acoesDe(notification: Notification): List<String?> =
        notification.actions.orEmpty().map { acao ->
            shadowOf(acao.actionIntent).savedIntents.firstOrNull()
                ?.getStringExtra(IncomingCallNotifier.EXTRA_CALL_ACTION)
        }

    private fun componentesDe(notification: Notification): List<String?> =
        notification.actions.orEmpty().map { acao ->
            shadowOf(acao.actionIntent).savedIntents.firstOrNull()?.component?.className
        }

    @Test
    fun `o canal de chamada e criado com importancia alta`() {
        publicarRecebida()

        val canal = manager.getNotificationChannel(CallNotificationChannels.CALL_CHANNEL_ID)
        assertNotNull(canal)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, canal.importance)
    }

    @Test
    fun `o canal de chamada nao e o canal de chamada bloqueada`() {
        publicarRecebida()

        assertNotEquals(
            AndroidBlockedCallNotifier.CHANNEL_ID,
            CallNotificationChannels.CALL_CHANNEL_ID,
        )
        val canal = manager.getNotificationChannel(AndroidBlockedCallNotifier.CHANNEL_ID)
        assertNull("o canal discreto da fase anterior nao pode ser criado por aqui", canal)
    }

    @Test
    fun `a chamada recebida e publicada com intencao de tela cheia`() {
        val notification = publicarRecebida()

        assertNotNull(notification.fullScreenIntent)
        assertEquals(Notification.CATEGORY_CALL, notification.category)
        assertEquals(CallNotificationChannels.CALL_CHANNEL_ID, notification.channelId)
    }

    @Test
    fun `sem a capacidade de tela cheia a chamada continua publicada e com duas acoes`() {
        val notification = publicarRecebida(telaCheiaPermitida = false)

        assertNull(notification.fullScreenIntent)
        val acoes = acoesDe(notification)
        assertTrue("esperadas as duas acoes, obtidas $acoes", acoes.size >= 2)
        assertTrue(acoes.contains(IncomingCallNotifier.ACTION_ANSWER))
        assertTrue(acoes.contains(IncomingCallNotifier.ACTION_REJECT))
    }

    @Test
    fun `cada acao da chamada recebida resolve para a tela de chamada`() {
        val notification = publicarRecebida()

        val componentes = componentesDe(notification)
        assertTrue("nenhuma acao publicada: $componentes", componentes.isNotEmpty())
        componentes.forEach { classe ->
            assertEquals("org.sentinela.app.ui.call.CallActivity", classe)
        }
    }

    @Test
    fun `a intencao de tela cheia abre a tela de chamada sem pedir acao nenhuma`() {
        val notification = publicarRecebida()

        val sombra = shadowOf(notification.fullScreenIntent)
        assertTrue(sombra.isActivityIntent)
        val intent = sombra.savedIntents.single()
        assertEquals("org.sentinela.app.ui.call.CallActivity", intent.component?.className)
        // O sistema dispara esta intencao SOZINHO com o aparelho bloqueado. Qualquer acao no extra
        // seria executada sem toque do usuario — com ACTION_ANSWER, toda chamada recebida com a tela
        // travada era atendida automaticamente. Ausencia de extra e o que impede isso.
        assertNull(
            "a intencao de tela cheia nao pode carregar acao: o sistema a dispara sem toque",
            intent.getStringExtra(IncomingCallNotifier.EXTRA_CALL_ACTION),
        )
    }

    @Test
    fun `as intencoes pendentes publicadas sao imutaveis`() {
        val notification = publicarRecebida()

        assertTrue(shadowOf(notification.fullScreenIntent).isImmutable)
        assertTrue(shadowOf(notification.contentIntent).isImmutable)
        notification.actions.orEmpty().forEach { acao ->
            assertTrue(shadowOf(acao.actionIntent).isImmutable)
        }
    }

    @Test
    fun `a chamada em curso permite encerrar`() {
        val notification = publicarEmCurso()

        val acoes = acoesDe(notification)
        assertTrue("esperada a acao de encerrar, obtidas $acoes", acoes.contains(HANGUP))
    }

    @Test
    fun `a chamada em curso substitui o aviso da chamada recebida em vez de somar`() {
        val n = notifier()

        n.notifyIncoming(identidade)
        n.notifyOngoing(identidade)

        assertEquals(1, shadowOf(manager).activeNotifications.size)
    }

    @Test
    fun `nenhum campo da notificacao carrega a sequencia completa do numero`() {
        listOf(
            identidade,
            MaskedCallIdentity(displayName = "Fulano", maskedNumber = MASCARA),
            MaskedCallIdentity(),
        ).forEach { identity ->
            val publicados = listOf(publicarRecebida(identity), publicarEmCurso(identity))
            val varridos = publicados.flatMap { notification ->
                textos(notification) + acoesDe(notification).map { it.orEmpty() }
            }
            varridos.forEach { texto -> semSequenciaCompleta("$identity: $texto", texto) }
        }
    }

    private fun semSequenciaCompleta(mensagem: String, texto: String) {
        SEQUENCIAS_PROIBIDAS.forEach { sequencia ->
            assertFalse(mensagem, texto.contains(sequencia))
        }
    }

    @Test
    fun `a versao publica nao identifica quem esta ligando`() {
        val notification = publicarRecebida(
            MaskedCallIdentity(displayName = "Fulano", maskedNumber = MASCARA),
        )

        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        val publica = notification.publicVersion
        assertNotNull(publica)
        publica?.let { pub ->
            textos(pub).forEach {
                assertFalse(it, it.contains("Fulano"))
                assertFalse(it, it.contains(MASCARA))
            }
        }
    }

    @Test
    fun `identidade vazia nao vira aviso vazio`() {
        val notification = publicarRecebida(MaskedCallIdentity())

        val esperado = context.getString(
            org.sentinela.app.R.string.notification_call_unknown_caller,
        )
        assertTrue(textos(notification).any { it.contains(esperado) })
    }

    @Test
    fun `cancelar remove o aviso da chamada`() {
        val n = notifier()
        n.notifyIncoming(identidade)

        n.cancel()

        assertEquals(0, shadowOf(manager).activeNotifications.size)
    }

    @Test
    fun `a costura de reversao do modo discador cancela o aviso`() {
        val n = notifier()
        n.notifyIncoming(identidade)

        n.cancelCallNotification()

        assertEquals(0, shadowOf(manager).activeNotifications.size)
    }

    @Test
    @Config(sdk = [29])
    fun `no piso da plataforma a chamada recebida sai com prioridade alta e as duas acoes`() {
        val notification = publicarRecebida()

        assertEquals(Notification.PRIORITY_HIGH, notification.priority)
        assertNotNull(notification.fullScreenIntent)
        val acoes = acoesDe(notification)
        assertTrue("esperadas as duas acoes, obtidas $acoes", acoes.size >= 2)
        assertTrue(acoes.contains(IncomingCallNotifier.ACTION_ANSWER))
        assertTrue(acoes.contains(IncomingCallNotifier.ACTION_REJECT))
        textos(notification).forEach { texto -> semSequenciaCompleta(texto, texto) }
    }

    @Test
    @Config(sdk = [29])
    fun `no piso da plataforma a chamada em curso permite encerrar`() {
        val notification = publicarEmCurso()

        assertTrue(acoesDe(notification).contains(HANGUP))
    }

    private companion object {
        /**
         * Número de teste cuja sequência de dígitos não aparece por acidente em nenhum outro texto
         * desta suíte, e a máscara correspondente — a única forma dele que pode ser publicada.
         */
        const val MASCARA = "+55 11 9****-4173"
        const val HANGUP = "hangup"

        val SEQUENCIAS_PROIBIDAS = listOf(
            "5511987654173",
            "11987654173",
            "987654173",
        )
    }
}

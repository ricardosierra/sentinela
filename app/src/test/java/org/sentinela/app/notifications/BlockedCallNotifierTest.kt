package org.sentinela.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.ScreeningSettings

/**
 * Robolectric e obrigatorio: o gerenciador de notificacoes sombreado e a unica forma de ler o
 * objeto realmente postado — e o teste central desta suite e sobre o CONTEUDO postado, nao
 * sobre a intencao do codigo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BlockedCallNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private var settings = ScreeningSettings()

    private fun notifier() = AndroidBlockedCallNotifier(context) { settings }

    private fun entry(
        maskedNumber: String = "+55 11 9****-8888",
        numberE164: String? = "+5511999998888",
        id: Long = 7L,
    ) = BlockedCallEntry(
        id = id,
        maskedNumber = maskedNumber,
        numberE164 = numberE164,
        timestampUtcMillis = 1_700_000_000_000L,
        reason = DecisionReason.UNKNOWN_NUMBER,
        notificationShown = true,
    )

    private fun postar(entry: BlockedCallEntry = entry()): Notification {
        val notifier = notifier()
        notifier.ensureChannel()
        notifier.notifyBlocked(entry)
        return shadowOf(manager).allNotifications.last()
    }

    /** Todo texto legivel do objeto: extras, versao publica e ticker. */
    private fun textos(notification: Notification): List<String> {
        val de = { bundle: Bundle? ->
            bundle?.keySet().orEmpty().mapNotNull { bundle?.get(it)?.toString() }
        }
        return de(notification.extras) +
            de(notification.publicVersion?.extras) +
            listOfNotNull(notification.tickerText?.toString())
    }

    @Test
    fun `ensureChannel cria o canal com importancia baixa`() {
        notifier().ensureChannel()

        val canal = shadowOf(manager).notificationChannels.single()
        assertEquals(NotificationManager.IMPORTANCE_LOW, canal.importance)
    }

    @Test
    fun `o canal nao faz som nem vibra`() {
        notifier().ensureChannel()

        val canal = manager.notificationChannels.single()
        assertNull(canal.sound)
        assertFalse(canal.shouldVibrate())
    }

    @Test
    fun `chamar ensureChannel duas vezes nao duplica o canal`() {
        val notifier = notifier()

        notifier.ensureChannel()
        notifier.ensureChannel()

        assertEquals(1, manager.notificationChannels.size)
    }

    @Test
    fun `com identificacao mascarada o texto usa a mascara do registro`() {
        settings = settings.copy(
            notificationIdentification = NotificationIdentification.MASKED,
        )

        val notification = postar()

        assertTrue(
            textos(notification).toString(),
            textos(notification).any { it.contains("+55 11 9****-8888") },
        )
    }

    @Test
    fun `com identificacao anonima nenhum texto contem digito algum do numero`() {
        settings = settings.copy(
            notificationIdentification = NotificationIdentification.ANONYMOUS,
        )

        val notification = postar()

        textos(notification).forEach { texto ->
            assertFalse(texto, texto.contains("8888"))
            assertFalse(texto, texto.contains("9999"))
        }
    }

    @Test
    fun `nenhum campo da notificacao carrega a sequencia completa do numero`() {
        listOf(NotificationIdentification.MASKED, NotificationIdentification.ANONYMOUS)
            .forEach { identificacao ->
                settings = settings.copy(notificationIdentification = identificacao)

                val notification = postar()

                textos(notification).forEach { texto ->
                    assertFalse("$identificacao: $texto", texto.contains("5511999998888"))
                    assertFalse("$identificacao: $texto", texto.contains("11999998888"))
                    assertFalse("$identificacao: $texto", texto.contains("999998888"))
                }
            }
    }

    @Test
    fun `a notificacao nao tem som nem vibracao nem intencao de tela cheia`() {
        val notification = postar()

        assertNull(notification.sound)
        assertNull(notification.vibrate)
        assertNull(notification.fullScreenIntent)
    }

    @Test
    fun `a notificacao e privada na tela bloqueada e tem versao publica sem identificacao`() {
        settings = settings.copy(
            notificationIdentification = NotificationIdentification.MASKED,
        )

        val notification = postar()

        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        val publica = notification.publicVersion
        assertNotNull(publica)
        publica?.let { pub ->
            textos(pub).forEach { assertFalse(it, it.contains("8888")) }
        }
    }

    @Test
    fun `a intencao pendente e imutavel e carrega o identificador do registro`() {
        val notification = postar(entry(id = 42L))

        val pending = notification.contentIntent
        assertNotNull(pending)
        assertTrue(shadowOf(pending).isActivityIntent)
        val intent = shadowOf(pending).savedIntents.single()
        assertEquals(42L, intent.getLongExtra(AndroidBlockedCallNotifier.EXTRA_ENTRY_ID, -1L))
    }

    @Test
    fun `notifyBlocked nao lanca com registro sem numero e sem mascara`() {
        val notification = postar(entry(maskedNumber = "", numberE164 = null, id = 0L))

        assertNotNull(notification)
        textos(notification).forEach { assertFalse(it, it.contains("8888")) }
    }

    @Test
    fun `notifyBlocked sem canal criado antes nao derruba o caminho da triagem`() {
        val notifier = notifier()

        notifier.notifyBlocked(entry())

        assertEquals(1, shadowOf(manager).allNotifications.size)
    }
}

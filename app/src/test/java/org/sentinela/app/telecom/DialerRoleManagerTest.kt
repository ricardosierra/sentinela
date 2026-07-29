package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
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

/**
 * O papel de telefone padrão é a chave do modo discador inteiro, e ele pode sumir sem aviso:
 * outro aplicativo assume, o usuário troca nas configurações do sistema, a atualização mexe no
 * padrão. Não existe observador dessa mudança para aplicativo comum, então a única coisa possível
 * é perguntar — e é esta consulta que a tela inicial usará em toda retomada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DialerRoleManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val shadow get() = shadowOf(context.getSystemService(RoleManager::class.java))

    private val manager = DialerRoleManager(context)

    @Test
    fun `com o papel concedido a consulta responde que o aplicativo o detem`() {
        shadow.addAvailableRole(RoleManager.ROLE_DIALER)
        shadow.addHeldRole(RoleManager.ROLE_DIALER)

        assertTrue(manager.isRoleHeld())
    }

    @Test
    fun `sem o papel a consulta responde que o aplicativo nao o detem`() {
        shadow.addAvailableRole(RoleManager.ROLE_DIALER)

        assertFalse(manager.isRoleHeld())
    }

    @Test
    fun `o papel disponivel no aparelho e reconhecido e existe pedido a oferecer`() {
        shadow.addAvailableRole(RoleManager.ROLE_DIALER)

        assertTrue(manager.isRoleAvailable())
        assertNotNull(manager.buildRequestIntent())
    }

    @Test
    fun `em aparelho sem telefonia nao ha papel nem pedido a oferecer`() {
        assertFalse(manager.isRoleAvailable())
        assertNull(manager.buildRequestIntent())
    }

    @Test
    fun `a reversao aponta para a tela de escolha de aplicativos padrao do sistema`() {
        val intent = manager.buildRevertIntent()

        assertEquals(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, intent.action)
    }

    @Test
    fun `a reversao existe mesmo sem o papel detido, porque quem decide e o usuario`() {
        // Não existe API pública de auto-remoção do papel: a reversão é sempre a mesma tela,
        // independentemente do que o aplicativo detenha no momento.
        assertNotNull(manager.buildRevertIntent().action)
    }

    @Test
    fun `sem servico de sistema tudo responde negativo e nada lanca`() {
        val semServico = mockk<Context>(relaxed = true) {
            every { getSystemService(RoleManager::class.java) } returns null
        }
        val semPapel = DialerRoleManager(semServico)

        assertFalse(semPapel.isRoleAvailable())
        assertFalse(semPapel.isRoleHeld())
        assertNull(semPapel.buildRequestIntent())
    }
}

package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
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
 * O papel de triagem pode sumir a qualquer momento: outro aplicativo assume, o usuário troca nas
 * configurações do sistema, a atualização mexe no padrão. Não existe aviso disso para
 * aplicativos comuns, então tudo o que resta é perguntar — e é justamente esta consulta que a
 * tela inicial vai usar em toda retomada, na Fase 7.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreeningRoleManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val shadow get() = shadowOf(context.getSystemService(RoleManager::class.java))

    private val manager = ScreeningRoleManager(context)

    @Test
    fun `com o papel concedido a consulta responde que o aplicativo o detem`() {
        shadow.addAvailableRole(RoleManager.ROLE_CALL_SCREENING)
        shadow.addHeldRole(RoleManager.ROLE_CALL_SCREENING)

        assertTrue(manager.isRoleHeld())
    }

    @Test
    fun `sem o papel a consulta responde que o aplicativo nao o detem`() {
        shadow.addAvailableRole(RoleManager.ROLE_CALL_SCREENING)

        assertFalse(manager.isRoleHeld())
    }

    @Test
    fun `o papel disponivel no aparelho e reconhecido`() {
        shadow.addAvailableRole(RoleManager.ROLE_CALL_SCREENING)

        assertTrue(manager.isRoleAvailable())
    }

    @Test
    fun `com o papel disponivel existe uma intencao de pedido`() {
        shadow.addAvailableRole(RoleManager.ROLE_CALL_SCREENING)

        assertNotNull(manager.buildRequestIntent())
    }

    @Test
    fun `em aparelho sem o papel a disponibilidade e negativa e nao ha pedido a oferecer`() {
        assertFalse(manager.isRoleAvailable())
        assertNull(manager.buildRequestIntent())
    }

    @Test
    fun `sem servico de sistema tudo responde negativo e nada lanca`() {
        val semServico = mockk<Context> {
            every { getSystemService(RoleManager::class.java) } returns null
        }
        val semPapel = ScreeningRoleManager(semServico)

        assertFalse(semPapel.isRoleAvailable())
        assertFalse(semPapel.isRoleHeld())
        assertNull(semPapel.buildRequestIntent())
    }
}

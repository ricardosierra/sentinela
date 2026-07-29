package org.sentinela.app.data.contacts

import android.content.ContentResolver
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prova de que da para preparar e limpar a agenda em teste instrumentado sem nenhuma
 * permissao nova em manifest algum.
 */
@RunWith(AndroidJUnit4::class)
class ContactsFixtureSmokeTest {

    private lateinit var cr: ContentResolver

    @Before
    fun setUp() {
        ContactsTestFixture.adoptShell()
        cr = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        ContactsTestFixture.wipe(cr)
    }

    @After
    fun tearDown() {
        ContactsTestFixture.wipe(cr)
        ContactsTestFixture.dropShell()
    }

    @Test
    fun insereTresContatosEConsultaEnxergaOsTres() {
        ContactsTestFixture.insert(cr, "Ana", "+5511911111111")
        ContactsTestFixture.insert(cr, "Bruno", "+5511922222222")
        ContactsTestFixture.insert(cr, "Carla", "+5511933333333")

        assertEquals(3, contarTelefones())
    }

    @Test
    fun wipeApagaTudo() {
        ContactsTestFixture.insert(cr, "Ana", "+5511911111111")
        ContactsTestFixture.insert(cr, "Bruno", "+5511922222222")

        ContactsTestFixture.wipe(cr)

        assertEquals(0, contarTelefones())
    }

    /** Projeta APENAS o numero: nome de contato nunca sai do provider nestes testes. */
    private fun contarTelefones(): Int =
        cr.query(Phone.CONTENT_URI, arrayOf(Phone.NUMBER), null, null, null)
            .use { cursor -> cursor?.count ?: -1 }
}

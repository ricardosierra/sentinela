package org.sentinela.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prova de que o ciclo instrumentado da Fase 3 esta de pe: runner, emulador e
 * assets do androidTest (onde vivem os schemas do Room para o MigrationTestHelper).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentationSmokeTest {

    @Test
    fun runnerInstrumentadoEnxergaOAppReal() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.sentinela.app", ctx.packageName)
    }

    @Test
    fun assetsDoAndroidTestExistem() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        assertNotNull("assets do androidTest inacessiveis", assets.list(""))
    }
}

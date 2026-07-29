package org.sentinela.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trava dos tokens "Silent Guardian" (docs/design/DESIGN.md) e do wiring do
 * esquema dark. `androidx.compose.ui.graphics.Color` é uma value class pura de
 * Kotlin: a comparação roda em JVM pura, sem Robolectric.
 *
 * Aparência real em aparelho é escopo da Fase 9; aqui só garantimos que uma
 * regressão de paleta seja detectada por teste, não por inspeção visual.
 */
class ThemeTokensTest {

    @Test
    fun `surface tokens match silent guardian palette`() {
        assertEquals(Color(0xFF081425), Surface)
        assertEquals(Color(0xFF040E1F), SurfaceContainerLowest)
        assertEquals(Color(0xFF2A3548), SurfaceContainerHighest)
        assertEquals(Color(0xFFD8E3FB), OnSurface)
    }

    @Test
    fun `accent tokens match silent guardian palette`() {
        assertEquals(Color(0xFFADC6FF), Primary)
        assertEquals(Color(0xFF4D8EFF), PrimaryContainer)
        assertEquals(Color(0xFFFFB4AB), Error)
        assertEquals(Color(0xFF8C909F), Outline)
    }

    @Test
    fun `dark scheme wires silent guardian surface tokens`() {
        assertEquals(Surface, DarkColors.surface)
        assertEquals(OnSurface, DarkColors.onSurface)
        assertEquals(SurfaceContainerLowest, DarkColors.surfaceContainerLowest)
        assertEquals(SurfaceContainerHighest, DarkColors.surfaceContainerHighest)
    }

    @Test
    fun `dark scheme wires primary error and outline tokens`() {
        assertEquals(Primary, DarkColors.primary)
        assertEquals(Error, DarkColors.error)
        assertEquals(Outline, DarkColors.outline)
    }
}

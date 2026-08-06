package org.sentinela.app.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import org.sentinela.app.R
import org.sentinela.app.domain.DecisionReason

/**
 * A tela do histórico mostrava a constante "Agora" em toda linha e o reason code cru
 * (`UNKNOWN_NUMBER`) como legenda. As duas coisas tiravam do histórico exatamente aquilo que o
 * torna auditável: quando a chamada aconteceu e por que ela foi barrada, em português.
 */
class HistoryFormattingTest {

    private val agora = 1_000_000_000L

    @Test
    fun `menos de um minuto e agora`() {
        val faixa = tempoRelativo(agora - 30_000L, agora)
        assertEquals(R.string.history_time_now, faixa.recurso)
        assertEquals(null, faixa.quantidade)
    }

    @Test
    fun `minutos aparecem em minutos`() {
        val faixa = tempoRelativo(agora - 5 * 60_000L, agora)
        assertEquals(R.string.history_time_minutes, faixa.recurso)
        assertEquals(5, faixa.quantidade)
    }

    @Test
    fun `horas aparecem em horas`() {
        val faixa = tempoRelativo(agora - 3 * 60 * 60_000L, agora)
        assertEquals(R.string.history_time_hours, faixa.recurso)
        assertEquals(3, faixa.quantidade)
    }

    @Test
    fun `dias aparecem em dias`() {
        val faixa = tempoRelativo(agora - 4 * 24 * 60 * 60_000L, agora)
        assertEquals(R.string.history_time_days, faixa.recurso)
        assertEquals(4, faixa.quantidade)
    }

    /** Relógio que anda para trás (fuso, ajuste manual) não pode virar "há -3 h". */
    @Test
    fun `registro no futuro nao produz quantidade negativa`() {
        val faixa = tempoRelativo(agora + 60_000L, agora)
        assertEquals(R.string.history_time_now, faixa.recurso)
        assertEquals(null, faixa.quantidade)
    }

    /**
     * Completude: todo motivo do domínio tem rótulo próprio. Sem este caso, um valor novo no enum
     * passaria a exibir o texto de outro motivo sem ninguém perceber.
     */
    @Test
    fun `todo motivo tem rotulo proprio`() {
        val rotulos = DecisionReason.entries.map { it.rotulo() }
        assertEquals(DecisionReason.entries.size, rotulos.distinct().size)
        assertEquals(0, rotulos.count { it == 0 })
    }
}

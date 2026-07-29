package org.sentinela.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retencao e regra PURA (HST-02): nenhum Android, nenhum relogio real.
 * O `now` e sempre injetado — teste de retencao que le o relogio do sistema
 * fica verde hoje e vermelho amanha.
 */
class RetentionPolicyTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `a lista de politicas tem exatamente 5 entradas`() {
        // Politica nova = decisao de produto + revisao de privacidade (quanto tempo
        // o E.164 completo fica no aparelho). Este assert forca a conversa.
        assertEquals(5, RetentionPolicy.entries.size)
    }

    @Test
    fun `os ids persistidos sao estaveis`() {
        // O id vai para o disco. Mudar um destes literais reinterpreta silenciosamente
        // a configuracao ja gravada do usuario.
        assertEquals(
            listOf("never", "7d", "30d", "90d", "manual"),
            RetentionPolicy.entries.map { it.id },
        )
    }

    @Test
    fun `NEVER_STORE nao deve gravar e nao tem cutoff`() {
        assertFalse(RetentionPolicy.NEVER_STORE.shouldStore)
        assertNull(RetentionPolicy.NEVER_STORE.cutoffUtcMillis(now))
    }

    @Test
    fun `MANUAL grava e nunca poda`() {
        assertTrue(RetentionPolicy.MANUAL.shouldStore)
        assertNull(RetentionPolicy.MANUAL.cutoffUtcMillis(now))
    }

    @Test
    fun `DAYS_7 corta em sete dias atras`() {
        assertEquals(now - 7 * 86_400_000L, RetentionPolicy.DAYS_7.cutoffUtcMillis(now))
    }

    @Test
    fun `DAYS_30 corta em trinta dias atras`() {
        assertEquals(now - 30 * 86_400_000L, RetentionPolicy.DAYS_30.cutoffUtcMillis(now))
    }

    @Test
    fun `DAYS_90 corta em noventa dias atras`() {
        assertEquals(now - 90 * 86_400_000L, RetentionPolicy.DAYS_90.cutoffUtcMillis(now))
    }

    @Test
    fun `todas as politicas menos NEVER_STORE gravam`() {
        val gravam = RetentionPolicy.entries.filter { it.shouldStore }
        assertEquals(
            listOf(
                RetentionPolicy.DAYS_7,
                RetentionPolicy.DAYS_30,
                RetentionPolicy.DAYS_90,
                RetentionPolicy.MANUAL,
            ),
            gravam,
        )
    }

    @Test
    fun `fromId devolve a politica correspondente a cada id`() {
        RetentionPolicy.entries.forEach { policy ->
            assertEquals(policy, RetentionPolicy.fromId(policy.id))
        }
    }

    @Test
    fun `fromId nulo cai no padrao de 30 dias sem lancar`() {
        assertEquals(RetentionPolicy.DAYS_30, RetentionPolicy.fromId(null))
    }

    @Test
    fun `fromId desconhecido cai no padrao de 30 dias sem lancar`() {
        // Configuracao gravada por uma versao mais nova do app nao pode derrubar a antiga.
        assertEquals(RetentionPolicy.DAYS_30, RetentionPolicy.fromId("valor_desconhecido"))
    }

    @Test
    fun `o cutoff e monotonico no tempo`() {
        val depois = RetentionPolicy.DAYS_30.cutoffUtcMillis(now + 1_000L)!!
        assertTrue(depois > RetentionPolicy.DAYS_30.cutoffUtcMillis(now)!!)
    }

    @Test
    fun `os padroes travados do MVP sao historico ligado e retencao de 30 dias`() {
        val padrao = ScreeningSettings()
        assertTrue(padrao.historyEnabled)
        assertEquals(RetentionPolicy.DAYS_30, padrao.retentionPolicy)
    }
}

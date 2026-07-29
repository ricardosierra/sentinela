package org.sentinela.app.telecom

import android.telecom.Call
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.telecom.call.rejectCall

/**
 * Recusar chamada com motivo declarado só existe a partir do nível 34 da plataforma, e o piso deste
 * projeto é o nível 29. Sem ramo por versão, o aplicativo estoura com erro de método ausente
 * exatamente quando o usuário tenta recusar uma ligação num aparelho de duas ou três versões atrás
 * — e nenhum emulador moderno mostra isso. Por isso os dois lados do ramo são cobertos, no molde
 * que a Fase 5 usou para a tradução de respostas.
 *
 * As duas configurações vivem em classes separadas de propósito: cada nível de plataforma roda numa
 * caixa isolada do Robolectric, e uma classe não enxerga membros da outra.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallRejectCompatTest {

    @Test
    fun `na plataforma moderna a recusa declara o motivo`() {
        val call = mockk<Call>(relaxed = true)

        rejectCall(call)

        verify(exactly = 1) { call.reject(Call.REJECT_REASON_DECLINED) }
    }

    @Test
    fun `na plataforma moderna a sobrecarga antiga nao e usada`() {
        val call = mockk<Call>(relaxed = true)

        rejectCall(call)

        verify(exactly = 0) { call.reject(any<Boolean>(), any()) }
    }
}

/** O piso real do projeto: aqui só existe a sobrecarga antiga. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CallRejectLegacyCompatTest {

    @Test
    fun `no piso da plataforma a recusa usa a sobrecarga antiga`() {
        val call = mockk<Call>(relaxed = true)

        rejectCall(call)

        verify(exactly = 1) { call.reject(false, null) }
    }

    /**
     * A sobrecarga com motivo declarado **não existe** neste nível de plataforma: pedir para
     * verificá-la aqui estoura com erro de método ausente, que é precisamente o defeito que o ramo
     * por versão evita em produção. A prova correta é, então, afirmar que nenhuma outra interação
     * com a chamada aconteceu além da recusa antiga.
     */
    @Test
    fun `no piso da plataforma nenhuma outra forma de recusa e tentada`() {
        val call = mockk<Call>(relaxed = true)

        rejectCall(call)

        verify(exactly = 1) { call.reject(false, null) }
        confirmVerified(call)
    }
}

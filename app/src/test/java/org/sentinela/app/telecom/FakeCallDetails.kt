package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.Call
import io.mockk.every
import io.mockk.mockk

/**
 * Dupla de teste de `Call.Details`.
 *
 * So os dois campos que o app tem direito de ler sao programados: a direcao da chamada e o
 * handle. Qualquer outro membro fica no comportamento relaxado do MockK de proposito — se um
 * dia o codigo de producao passar a ler um campo nao garantido, o teste nao vai avisar por
 * acidente, e a revisao continua sendo a barreira. Ler campo que a plataforma nao garante em
 * `onScreenCall` e bug latente (Pitfall 4 da pesquisa da fase).
 */
fun fakeCallDetails(
    direction: Int = Call.Details.DIRECTION_INCOMING,
    handle: Uri? = null,
): Call.Details = mockk(relaxed = true) {
    every { callDirection } returns direction
    every { this@mockk.handle } returns handle
}

package org.sentinela.app.telecom.call

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.notifications.AndroidBlockedCallNotifier
import org.sentinela.app.notifications.BlockedCallNotifier
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.settings.SettingsRepository
import org.sentinela.app.telecom.PostScreeningWork

/**
 * Fronteira do numero, provada nas DUAS direcoes.
 *
 * O contrato desta fase e assimetrico de proposito, e a assimetria e o que precisa ficar travado:
 *
 * - **na tela de chamada o numero e completo**, porque ali o numero e o produto e o usuario precisa
 *   dele inteiro para decidir se atende. Um discador que mascara o numero que esta tocando e
 *   inutil;
 * - **em registro de execucao, notificacao e no que o historico exibe o numero e mascarado**,
 *   sempre, pela mascara unica do aplicativo.
 *
 * A varredura opera sobre OBJETOS CONSTRUIDOS EM TEMPO DE TESTE — o retrato da sessao e o objeto de
 * notificacao realmente postado —, nunca sobre o texto do arquivo fonte. Isso e deliberado: um
 * criterio que lesse o fonte cairia por causa de um comentario como este, e provaria ausencia de
 * texto em vez de ausencia de comportamento.
 *
 * A coluna do historico que guarda o numero em forma canonica e um dado local declarado, fora do
 * backup automatico, que existe para a whitelist da Fase 8. O que este teste trava e que ela nunca
 * atravesse para o que e exibido ou notificado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallLoggingPrivacyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private val util: PhoneNumberUtil = PhoneNumberUtil.createInstance(context)

    /**
     * Numero de teste distinto de qualquer outro usado na suite, para que um encontro na varredura
     * so possa ter vindo deste caso.
     */
    private val numeroCompleto = "+5511976543210"

    private val digitosCompletos = numeroCompleto.filter { it.isDigit() }

    private fun mascarar(valor: String) = PhoneMask.mask(util, valor)

    // --- direcao 1: a tela recebe o numero completo -----------------------------------

    @Test
    fun `o retrato da sessao carrega o numero completo para a tela`() {
        val retrato = CallSnapshot(
            state = CallUiState.Incoming,
            identity = CallIdentity(
                fullNumber = numeroCompleto,
                origin = CallOrigin.DESCONHECIDO,
            ),
        )

        assertEquals(numeroCompleto, retrato.identity.fullNumber)
        assertTrue(retrato.identity.fullNumber!!.contains(digitosCompletos.takeLast(TAMANHO_LOCAL)))
    }

    @Test
    fun `os digitos de tom enviados na chamada acumulam a sequencia completa`() {
        val eventos = mutableListOf<String>()
        val sessao = CallSessionCoordinator(controls = ControlesSilenciosos(eventos))
        sessao.onCallAdded(rawState = ESTADO_ATIVA, identity = CallIdentity())
        "1234".forEach { digito ->
            sessao.pressDigit(digito)
            sessao.releaseDigit()
        }

        assertEquals("1234", sessao.state.value.sentDigits)
    }

    // --- direcao 2: nenhuma camada de saida recebe o numero completo -------------------

    @Test
    fun `a mascara nunca devolve a sequencia completa do numero`() {
        val mascarado = mascarar(numeroCompleto)

        assertFalse(mascarado.contains(digitosCompletos))
        assertFalse(mascarado.filter { it.isDigit() }.contains(digitosCompletos))
        assertTrue(mascarado.endsWith(digitosCompletos.takeLast(TAMANHO_LOCAL)))
    }

    @Test
    fun `nenhum campo do objeto de notificacao carrega a sequencia completa`() = runTest {
        val registrada = executarTrabalhoPosTriagem(notificar = true)
        val postada = shadowOf(manager).allNotifications.last()

        textosDe(postada).forEach { texto ->
            assertFalse(texto.contains(digitosCompletos))
            assertFalse(texto.contains(numeroCompleto))
        }
        // E a prova positiva: a mascara ESTA la, entao a varredura nao passou por vacuidade.
        assertTrue(textosDe(postada).any { it.contains(mascarar(numeroCompleto)) })
        assertEquals(mascarar(numeroCompleto), registrada.maskedNumber)
    }

    @Test
    fun `a versao publica da notificacao tambem nao carrega a sequencia completa`() = runTest {
        executarTrabalhoPosTriagem(notificar = true, identificar = false)
        val postada = shadowOf(manager).allNotifications.last()

        val publica = postada.publicVersion
        val textos = camposDe(publica?.extras)
        assertTrue(textos.isNotEmpty())
        textos.forEach { assertFalse(it.contains(digitosCompletos)) }
    }

    @Test
    fun `o que o historico exibe passa pela mascara, nao pela forma canonica`() = runTest {
        val registrada = executarTrabalhoPosTriagem(notificar = false)

        assertFalse(registrada.maskedNumber.contains(digitosCompletos))
        assertEquals(mascarar(numeroCompleto), registrada.maskedNumber)
        // A coluna canonica e o dado local declarado; ela existe e nao e exibida por ninguem.
        assertEquals(numeroCompleto, registrada.numberE164)
    }

    // --- costuras de teste -------------------------------------------------------------

    private suspend fun executarTrabalhoPosTriagem(
        notificar: Boolean,
        identificar: Boolean = true,
    ): BlockedCallEntry {
        val configuracoes = ScreeningSettings(
            showOwnNotification = notificar,
            notificationIdentification = if (identificar) {
                NotificationIdentification.MASKED
            } else {
                NotificationIdentification.ANONYMOUS
            },
        )
        val historico = HistoricoEspiao()
        val notificador: BlockedCallNotifier =
            AndroidBlockedCallNotifier(context) { configuracoes }
        PostScreeningWork(
            settings = ConfiguracoesFixas(configuracoes),
            history = historico,
            notifier = notificador,
            mask = ::mascarar,
            clock = { INSTANTE_FIXO },
        ).run(
            call = ScreenedCall(
                direction = CallDirection.INCOMING,
                number = ScreenedNumber.Valid(numeroCompleto),
            ),
            decision = CallDecision.Reject(DecisionReason.UNKNOWN_NUMBER),
        )
        return historico.registradas.single()
    }

    /** Todo texto legivel do objeto postado: extras, versao publica e faixa de aviso. */
    private fun textosDe(notification: Notification): List<String> =
        camposDe(notification.extras) +
            camposDe(notification.publicVersion?.extras) +
            listOfNotNull(notification.tickerText?.toString())

    private fun camposDe(bundle: Bundle?): List<String> =
        bundle?.keySet().orEmpty().mapNotNull { bundle?.get(it)?.toString() }

    private companion object {
        const val TAMANHO_LOCAL = 4
        const val ESTADO_ATIVA = 4
        const val INSTANTE_FIXO = 1_700_000_000_000L
    }
}

private class ConfiguracoesFixas(
    private val valor: ScreeningSettings,
) : SettingsRepository {
    override val settings = kotlinx.coroutines.flow.flowOf(valor)
    override suspend fun snapshot(): ScreeningSettings = valor
    override suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings) = Unit
}

/**
 * Historico de teste que so guarda o que foi mandado registrar.
 *
 * Os demais membros do contrato nao participam desta prova: a fronteira medida aqui e o CONTEUDO da
 * entrada gravada, e um duble que fizesse mais do que guardar a entrada abriria espaco para o teste
 * medir a si mesmo.
 */
private class HistoricoEspiao : BlockedCallRepository {
    val registradas = mutableListOf<BlockedCallEntry>()

    override suspend fun record(entry: BlockedCallEntry): Long {
        registradas += entry
        return registradas.size.toLong()
    }

    override fun observeRecent(): kotlinx.coroutines.flow.Flow<List<BlockedCallEntry>> =
        kotlinx.coroutines.flow.flowOf(registradas.toList())

    override fun observeTotalCount(): kotlinx.coroutines.flow.Flow<Long> =
        kotlinx.coroutines.flow.flowOf(registradas.size.toLong())

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun clearAll() = registradas.clear()

    override suspend fun pruneOlderThan(utcMillis: Long) = Unit

    override suspend fun hasRecentBlock(
        numberE164: String?,
        nowUtcMillis: Long,
    ): org.sentinela.app.domain.RepeatedCallLookup =
        org.sentinela.app.domain.RepeatedCallLookup.MISS
}

private class ControlesSilenciosos(
    private val eventos: MutableList<String>,
) : CallControls {
    override fun answer() { eventos += "answer" }
    override fun reject() { eventos += "reject" }
    override fun hangUp() { eventos += "hangUp" }
    override fun setMuted(muted: Boolean) { eventos += "setMuted" }
    override fun setSpeakerOn(on: Boolean) { eventos += "setSpeakerOn" }
    override fun playDtmf(digit: Char) { eventos += "playDtmf:$digit" }
    override fun stopDtmf() { eventos += "stopDtmf" }
}

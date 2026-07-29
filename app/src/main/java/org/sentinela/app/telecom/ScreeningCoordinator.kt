package org.sentinela.app.telecom

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.sentinela.app.data.contacts.ContactLookupRepository
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.domain.CallDirection
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.domain.WhitelistLookup
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.settings.SettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Prazo interno do conjunto das consultas locais.
 *
 * A plataforma dá 5 segundos para o aplicativo responder a uma triagem; um segundo
 * deixa 5x de folga. A folga não é luxo: o limite anunciado pode ser encurtado por
 * fabricante ou por operadora, e o custo de estourar é a decisão ser descartada e a
 * chamada tocar sem passar pelo aplicativo. Ao estourar, o coordenador decide com os
 * resultados degradados e a política de reserva — já testada desde a Fase 2 — resolve.
 */
const val SCREENING_TIMEOUT_MILLIS: Long = 1_000L

/**
 * Orquestra a triagem: reúne as quatro consultas locais dentro do prazo, chama o
 * motor uma única vez, garante que o sistema receba no máximo uma decisão e só
 * então dispara o trabalho posterior (notificação e histórico).
 *
 * A classe é deliberadamente **pura**: nenhum tipo da plataforma entra aqui. A
 * costura de saída é uma função que recebe a decisão de domínio; quem a traduz para
 * a resposta da plataforma é a fábrica de respostas, chamada pelo Service.
 *
 * Nenhuma regra de triagem mora neste arquivo. Toda condição que decide o destino de
 * uma chamada vive no motor de decisão, por exigência do CLAUDE.md.
 *
 * Duas garantias duras:
 *  - **Resposta única.** A guarda atômica é local a cada triagem (dois cartões SIM
 *    podem triar duas chamadas ao mesmo tempo) e é a única proteção existente:
 *    responder duas vezes ao sistema não lança nem derruba o processo, apenas emite
 *    dois avisos em silêncio.
 *  - **Defeito deixa passar.** Qualquer falha inesperada resulta em permitir a
 *    chamada. Barrar uma ligação por causa de um defeito é pior que deixar uma
 *    passar: o usuário perde uma chamada importante e não tem como descobrir por quê.
 */
// A injeção de dependência do projeto é manual, sem framework: os colaboradores chegam
// por construtor e são exatamente os cinco de que a triagem precisa, mais o relógio e o
// prazo, que existem para o teste poder controlar tempo sem cronômetro.
// As duas capturas amplas são deliberadas e documentadas no corpo: a rede permissiva
// existe justamente para que NENHUMA falha, conhecida ou não, deixe a chamada sem resposta.
@Suppress("LongParameterList", "TooGenericExceptionCaught", "SwallowedException")
class ScreeningCoordinator(
    private val settings: SettingsRepository,
    private val contacts: ContactLookupRepository,
    private val whitelist: PersonalWhitelistRepository,
    private val blockedCalls: BlockedCallRepository,
    private val engine: CallDecisionEngine,
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeoutMillis: Long = SCREENING_TIMEOUT_MILLIS,
) {

    /**
     * @param respond costura de saída; chamada no máximo uma vez por triagem.
     * @param afterResponse trabalho desacoplado (notificação, histórico) executado
     *   somente **depois** da resposta e incapaz de atrasá-la ou derrubá-la.
     */
    suspend fun screen(
        call: ScreenedCall,
        respond: (CallDecision) -> Unit,
        afterResponse: suspend (ScreenedCall, CallDecision) -> Unit = { _, _ -> },
    ) {
        // Chamada de saída não é triada: sai antes de qualquer consulta e sem emitir nada.
        if (call.direction == CallDirection.OUTGOING) return

        val responded = AtomicBoolean(false)
        var emitted: CallDecision? = null

        fun emit(decision: CallDecision) {
            if (responded.compareAndSet(false, true)) {
                emitted = decision
                runCatching { respond(decision) }
            }
        }

        try {
            emit(decide(call))
        } catch (error: Throwable) {
            emit(permissive())
        } finally {
            // Rede permissiva: se nada respondeu até aqui, a chamada passa.
            emit(permissive())
            emitted?.let { decision -> runCatching { afterResponse(call, decision) } }
        }
    }

    private suspend fun decide(call: ScreenedCall): CallDecision {
        val key = (call.number as? ScreenedNumber.Valid)?.e164
        return try {
            withTimeout(timeoutMillis) {
                coroutineScope {
                    val configuracoes = async { settings.snapshot() }
                    val contato = async { key?.let { contacts.lookup(it) } ?: ContactLookup.MISS }
                    val lista = async {
                        key?.let { toLookup(whitelist.contains(it)) } ?: WhitelistLookup.MISS
                    }
                    val repetida = async {
                        if (key == null) {
                            RepeatedCallLookup.MISS
                        } else {
                            blockedCalls.hasRecentBlock(key, clock())
                        }
                    }
                    engine.decide(
                        call = call,
                        settings = configuracoes.await(),
                        contact = contato.await(),
                        whitelist = lista.await(),
                        repeatedCall = repetida.await(),
                    )
                }
            }
        } catch (expired: TimeoutCancellationException) {
            engine.decide(
                call = call,
                settings = ScreeningSettings(),
                contact = ContactLookup.UNAVAILABLE,
                whitelist = WhitelistLookup.LOOKUP_FAILED,
                repeatedCall = RepeatedCallLookup.LOOKUP_FAILED,
            )
        }
    }

    private fun toLookup(present: Boolean): WhitelistLookup =
        if (present) WhitelistLookup.HIT else WhitelistLookup.MISS

    private fun permissive(): CallDecision =
        CallDecision.Allow(DecisionReason.LOCAL_LOOKUP_FAILURE)
}

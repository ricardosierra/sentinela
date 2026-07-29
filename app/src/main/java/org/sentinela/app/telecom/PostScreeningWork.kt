package org.sentinela.app.telecom

import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.ScreenedNumber
import org.sentinela.app.domain.blocksCall
import org.sentinela.app.notifications.BlockedCallNotifier
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.settings.SettingsRepository

/**
 * Tudo o que só pode acontecer **depois** de o sistema já ter recebido a resposta: o registro no
 * histórico local e a notificação própria.
 *
 * A ordem não é preferência, é contrato: qualquer trabalho feito antes da resposta rouba
 * orçamento do prazo que a plataforma concede, e o preço de estourar esse prazo é a decisão ser
 * descartada. Por isso este colaborador nunca é chamado pelo caminho da decisão — quem o aciona
 * é o gancho posterior do coordenador.
 *
 * Nenhuma regra de triagem mora aqui. O que este arquivo faz é decidir se há **rastro** a
 * guardar: só chamada efetivamente barrada vira registro, e a notificação depende do
 * interruptor do usuário, que nasce desligado.
 *
 * O número completo entra apenas na coluna do histórico que existe para a whitelist da Fase 8.
 * Tudo o que pode ser exibido sai da máscara única do aplicativo.
 */
class PostScreeningWork(
    private val settings: SettingsRepository,
    private val history: BlockedCallRepository,
    private val notifier: BlockedCallNotifier,
    private val mask: (String) -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun run(call: ScreenedCall, decision: CallDecision) {
        if (!decision.blocksCall) return

        val configuracoes = settings.snapshot()
        val numero = (call.number as? ScreenedNumber.Valid)?.e164
        val entrada = BlockedCallEntry(
            maskedNumber = numero?.let { runCatching { mask(it) }.getOrNull() }
                ?: PhoneMask.MASCARA_GENERICA,
            numberE164 = numero,
            timestampUtcMillis = clock(),
            reason = decision.reason,
            notificationShown = configuracoes.showOwnNotification,
        )

        // O identificador real só existe depois da gravação, e é ele que faz a notificação abrir
        // o registro certo. Histórico desligado devolve zero, e a notificação segue possível.
        val id = history.record(entrada)

        if (configuracoes.showOwnNotification) {
            notifier.notifyBlocked(entrada.copy(id = id))
        }
    }
}

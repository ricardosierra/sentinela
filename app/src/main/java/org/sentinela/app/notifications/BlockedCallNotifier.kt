package org.sentinela.app.notifications

import org.sentinela.app.data.local.BlockedCallEntry

/**
 * Notificação própria, silenciosa e opcional (Fase 5, NTF-01..NTF-05). Canal
 * "Chamadas bloqueadas" com importância baixa: sem som, vibração, heads-up ou
 * tela cheia. Só é chamada DEPOIS do respondToCall e somente se o usuário
 * habilitou — a opção nasce desligada, porque o valor do produto é não interromper.
 */
interface BlockedCallNotifier {

    fun ensureChannel()

    /** Mostra número mascarado ou nenhuma identificação, conforme configuração. */
    fun notifyBlocked(entry: BlockedCallEntry)
}

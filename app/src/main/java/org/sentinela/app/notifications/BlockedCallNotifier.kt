package org.sentinela.app.notifications

import org.sentinela.app.data.local.BlockedCallEntry

/**
 * Notificação própria, silenciosa e opcional (Fase 4). Canal "Chamadas
 * bloqueadas" com IMPORTANCE_LOW: sem som, vibração, heads-up ou full-screen.
 * Só é chamada DEPOIS do respondToCall e somente se o usuário habilitou.
 */
interface BlockedCallNotifier {

    fun ensureChannel()

    /** Mostra número mascarado ou nenhuma identificação, conforme configuração. */
    fun notifyBlocked(entry: BlockedCallEntry)
}

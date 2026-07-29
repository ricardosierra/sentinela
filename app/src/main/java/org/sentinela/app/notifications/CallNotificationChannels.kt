package org.sentinela.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import org.sentinela.app.R

/**
 * Canal de notificação da chamada do modo discador.
 *
 * Ele é um canal **novo e distinto**, e isso não é organização: é a única forma de a chamada
 * recebida poder ocupar a tela bloqueada. A plataforma só honra o pedido de tela cheia quando a
 * notificação vem por um canal de importância alta ou maior, e a importância de um canal é
 * **imutável depois de criado** — o usuário pode baixá-la, o aplicativo nunca pode subi-la.
 *
 * Por consequência, o canal discreto que a fase anterior criou para o aviso de chamada bloqueada
 * **não serve aqui e nunca poderá servir**: ele nasceu com importância baixa por decisão de
 * produto (aviso que não toca, não vibra e não ocupa a tela), e reaproveitá-lo produziria um
 * pedido de tela cheia que jamais dispara — a pior categoria de defeito desta fase, porque o
 * código parece correto e o usuário simplesmente não vê a chamada chegando.
 *
 * Os dois canais são criados em pontos distintos de propósito: o discreto no momento em que o
 * usuário liga o aviso de bloqueio, este no momento em que a primeira chamada chega. Nada aqui
 * roda na partida do processo.
 */
object CallNotificationChannels {

    /** Identificador próprio, diferente do identificador do canal de chamada bloqueada. */
    const val CALL_CHANNEL_ID: String = "ongoing_calls"

    /**
     * Idempotente por contrato da plataforma: recriar um canal com o mesmo identificador não
     * duplica nada nem sobrescreve o que o usuário ajustou.
     */
    fun ensureCallChannel(context: Context) {
        val canal = NotificationChannel(
            CALL_CHANNEL_ID,
            context.getString(R.string.notification_channel_call),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_call_desc)
            // Uma chamada precisa ser vista na hora: crachá no ícone e conteúdo na tela
            // bloqueada. O conteúdo continua mascarado — a privacidade é garantida pelo texto
            // que entra na notificação, nunca pela visibilidade escolhida.
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }
}

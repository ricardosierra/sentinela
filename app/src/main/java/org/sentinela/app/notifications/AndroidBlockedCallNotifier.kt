package org.sentinela.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.sentinela.app.R
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.settings.NotificationIdentification
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.ui.MainActivity

/**
 * Notificação própria (NTF-01..NTF-05), silenciosa e opcional.
 *
 * A garantia de privacidade aqui NÃO é a visibilidade escolhida pelo usuário: é o fato de o
 * número completo nunca entrar no objeto de notificação. O conteúdo sai de
 * [BlockedCallEntry.maskedNumber], produzido pela máscara única [PhoneMask] — o campo com os
 * dígitos completos do registro jamais é lido nesta classe.
 *
 * Todo o corpo roda dentro de `runCatching`: esta classe é chamada DEPOIS do respondToCall, e
 * uma falha ao notificar nunca pode escapar para o caminho da triagem.
 *
 * @param settingsProvider snapshot já em memória; a configuração é lida no momento do envio
 * para que ligar/desligar a identificação valha na próxima chamada, sem recriar o notificador.
 */
class AndroidBlockedCallNotifier(
    private val context: Context,
    private val settingsProvider: () -> ScreeningSettings,
) : BlockedCallNotifier {

    private val manager: NotificationManager?
        get() = context.getSystemService(NotificationManager::class.java)

    /**
     * Idempotente por contrato da plataforma: recriar um canal com o mesmo id não duplica nem
     * sobrescreve as escolhas do usuário. Chamado no opt-in e antes de cada envio — NUNCA em
     * `Application.onCreate`, que é orçamento de cold start do Service.
     */
    override fun ensureChannel() {
        runCatching {
            val canal = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_blocked),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_blocked_desc)
                // Reforços coerentes com a importância baixa: nada de som, vibração ou luz.
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            manager?.createNotificationChannel(canal)
        }
    }

    override fun notifyBlocked(entry: BlockedCallEntry) {
        runCatching {
            ensureChannel()
            val identificacao = settingsProvider().notificationIdentification
            val texto = when {
                identificacao == NotificationIdentification.ANONYMOUS ->
                    context.getString(R.string.notification_blocked_anonymous)

                entry.maskedNumber.isBlank() ->
                    // Registro sem máscara utilizável: cai no texto genérico da máscara,
                    // nunca em eco da entrada crua.
                    context.getString(R.string.notification_blocked_masked, PhoneMask.MASCARA_GENERICA)

                else ->
                    context.getString(R.string.notification_blocked_masked, entry.maskedNumber)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notification_blocked_title))
                .setContentText(texto)
                .setSilent(true)
                .setVibrate(null)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setPublicVersion(versaoPublica())
                .setContentIntent(intencao(entry.id))
                .build()

            manager?.notify(notificationId(entry.id), notification)
        }
    }

    /** O que a tela bloqueada mostra quando o usuário esconde conteúdo sensível: nada além do fato. */
    private fun versaoPublica() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.notification_blocked_title))
        .setContentText(context.getString(R.string.notification_blocked_anonymous))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    /**
     * FLAG_IMMUTABLE é obrigatório desde a API 31 e correto sempre: nenhum outro app pode
     * reescrever o destino. O extra carrega só o id interno do registro — nunca o número.
     */
    private fun intencao(entryId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ENTRY_ID, entryId)
        return PendingIntent.getActivity(
            context,
            notificationId(entryId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Id derivado do registro: chamadas distintas não se sobrescrevem, e o valor cabe em Int. */
    private fun notificationId(entryId: Long): Int =
        NOTIFICATION_ID_BASE + (entryId % NOTIFICATION_ID_RANGE).toInt()

    companion object {
        const val CHANNEL_ID = "blocked_calls"

        /** Id interno do registro no histórico local. Nunca carrega dado pessoal. */
        const val EXTRA_ENTRY_ID = "entry_id"

        private const val NOTIFICATION_ID_BASE = 1000
        private const val NOTIFICATION_ID_RANGE = 1000L
    }
}

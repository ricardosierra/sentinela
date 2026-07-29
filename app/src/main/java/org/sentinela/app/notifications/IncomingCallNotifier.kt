package org.sentinela.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import org.sentinela.app.BuildConfig
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallNotificationCanceller
import org.sentinela.app.telecom.call.MaskedCallIdentity
import org.sentinela.app.ui.call.CallActivity

/**
 * Aviso de chamada do modo discador: a chamada recebida em tela cheia sobre a tela bloqueada e o
 * aviso da chamada em curso.
 *
 * Este é o caminho **oficial** da plataforma para telefonia, e é por isso que a permissão de
 * desenhar janela sobre os outros aplicativos continua proibida no Sentinela: ela não é necessária
 * para nada disto, e um bloqueador de chamadas que pede permissão de sobreposição de tela é
 * indistinguível de um aplicativo abusivo.
 *
 * ## Contrato do extra de ação (consumido pela tela de chamada)
 *
 * Cada ação desta notificação é uma intenção pendente de **Activity** que abre a tela de chamada
 * levando, num extra, a ação pretendida pelo usuário. Quem lê o extra é a tela de chamada, que
 * traduz o valor em comando da sessão. O contrato, literal:
 *
 * - chave do extra: o identificador do aplicativo seguido de `.extra.CALL_ACTION` — que é
 *   exatamente o valor de [EXTRA_CALL_ACTION], e é dele que a tela deve ler, nunca de um literal
 *   repetido. O identificador não aparece escrito aqui porque literal do identificador em Kotlin é
 *   proibido pelo invariante de rebranding do projeto: ele sai da configuração de compilação.
 * - valores possíveis: `answer` (atender), `reject` (recusar), `hangup` (encerrar) — que são
 *   [ACTION_ANSWER], [ACTION_REJECT] e [ACTION_HANGUP]
 *
 * Nenhum receptor de transmissão é criado e o manifest não é tocado por causa disto. O motivo é
 * técnico, não de gosto: o serviço da interface de chamada é **vinculado** pela plataforma de
 * telefonia e iniciá-lo por intenção pendente não é ponto de entrada legítimo dele; um receptor
 * exigiria declaração própria no manifest; e a tela de chamada já é declarada com modo de
 * lançamento de topo único, exibição sobre a tela bloqueada e acender a tela — tudo o que este
 * caminho precisa. Efeito colateral desejável: atender pela ação da notificação abre a interface
 * da chamada, que é o que o usuário quer numa chamada de voz de qualquer forma.
 *
 * ## Privacidade
 *
 * A identidade chega **já mascarada** por quem chama, exatamente como no aviso de chamada
 * bloqueada da fase anterior: esta classe não recebe normalizador de telefone, não conhece a forma
 * internacional do número e não tem como ecoar dígito completo. A sequência inteira aparece apenas
 * na tela de chamada — ali o número é o produto —, nunca na notificação nem na tela bloqueada.
 *
 * ## Degradação
 *
 * A permissão de ocupar a tela é concedida na instalação a aplicativo de chamada, mas o usuário
 * pode revogá-la nas Configurações. Por isso a capacidade é **consultada antes de usar** e, quando
 * negada, a chamada continua sendo publicada como aviso comum com as ações de atender e recusar —
 * o que ainda é caminho funcional. O que nunca acontece: deixar de avisar, e insistir levando o
 * usuário às Configurações.
 */
class IncomingCallNotifier(
    private val context: Context,
    private val fullScreenAllowed: () -> Boolean = { fullScreenIntentAllowed(context) },
) : CallNotificationCanceller {

    private val manager: NotificationManager?
        get() = context.getSystemService(NotificationManager::class.java)

    /** Chamada recebida: tela cheia quando permitido, sempre com atender e recusar. */
    fun notifyIncoming(identity: MaskedCallIdentity) {
        CallNotificationChannels.ensureCallChannel(context)
        val builder = base(R.string.notification_call_incoming_title, identity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    pessoa(identity),
                    intencaoDeAcao(ACTION_REJECT, REQUEST_REJECT),
                    intencaoDeAcao(ACTION_ANSWER, REQUEST_ANSWER),
                ),
            )
        } else {
            // Piso da plataforma: o estilo de chamada não existe e as duas ações são adicionadas
            // à mão. Sem elas, um usuário com a tela cheia revogada veria o aviso e não teria como
            // atender por ele.
            acoesDeChamadaRecebida(builder)
        }
        if (fullScreenAllowed()) {
            builder.setFullScreenIntent(intencaoDeAcao(ACTION_ANSWER, REQUEST_FULL_SCREEN), true)
        }
        manager?.notify(CALL_NOTIFICATION_ID, builder.build())
    }

    /** Chamada em curso: aviso persistente com a ação de encerrar. */
    fun notifyOngoing(identity: MaskedCallIdentity) {
        CallNotificationChannels.ensureCallChannel(context)
        val builder = base(R.string.notification_call_ongoing_title, identity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    pessoa(identity),
                    intencaoDeAcao(ACTION_HANGUP, REQUEST_HANGUP),
                ),
            )
        } else {
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.call_action_hangup),
                intencaoDeAcao(ACTION_HANGUP, REQUEST_HANGUP),
            )
        }
        manager?.notify(CALL_NOTIFICATION_ID, builder.build())
    }

    /** Some com o aviso. Chamado ao perder a chamada e ao reverter o modo discador. */
    fun cancel() {
        manager?.cancel(CALL_NOTIFICATION_ID)
    }

    override fun cancelCallNotification() = cancel()

    private fun base(titulo: Int, identity: MaskedCallIdentity): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CallNotificationChannels.CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(titulo))
            .setContentText(rotulo(identity))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            // Chamada não se descarta com um toque e não sai da barra sozinha: quem a remove é o
            // fim da ligação.
            .setOngoing(true)
            .setAutoCancel(false)
            .setPublicVersion(versaoPublica(titulo))
            .setContentIntent(intencaoDeAcao(null, REQUEST_CONTENT))

    private fun acoesDeChamadaRecebida(builder: NotificationCompat.Builder) {
        builder.addAction(
            R.drawable.ic_launcher_foreground,
            context.getString(R.string.call_action_reject),
            intencaoDeAcao(ACTION_REJECT, REQUEST_REJECT),
        )
        builder.addAction(
            R.drawable.ic_launcher_foreground,
            context.getString(R.string.call_action_answer),
            intencaoDeAcao(ACTION_ANSWER, REQUEST_ANSWER),
        )
    }

    /** O que a tela bloqueada mostra quando o usuário esconde conteúdo sensível: só o fato. */
    private fun versaoPublica(titulo: Int) =
        NotificationCompat.Builder(context, CallNotificationChannels.CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(titulo))
            .setContentText(context.getString(R.string.notification_call_unknown_caller))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    /**
     * Nome do contato quando a própria ligação o informa; senão o número **mascarado**; senão o
     * texto genérico. Nunca a sequência completa: ela não chega a esta classe.
     */
    private fun rotulo(identity: MaskedCallIdentity): String =
        identity.displayName?.takeIf { it.isNotBlank() }
            ?: identity.maskedNumber?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_call_unknown_caller)

    private fun pessoa(identity: MaskedCallIdentity): Person =
        Person.Builder().setName(rotulo(identity)).setImportant(true).build()

    /**
     * Intenção pendente de **Activity** para a tela de chamada, com a ação pretendida no extra.
     * Imutável: nenhum outro aplicativo pode reescrever o destino nem o extra. Cada ação usa um
     * código de pedido próprio, senão a plataforma devolveria a mesma intenção para todas.
     */
    private fun intencaoDeAcao(acao: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        if (acao != null) intent.putExtra(EXTRA_CALL_ACTION, acao)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        /**
         * Chave do extra de ação, consumida pela tela de chamada.
         *
         * Composta a partir do identificador do aplicativo da configuração de compilação em vez de
         * escrita como literal: o invariante de rebranding do projeto proíbe o identificador
         * literal em Kotlin, e compor a chave faz ela acompanhar qualquer renomeação futura.
         */
        val EXTRA_CALL_ACTION: String = "${BuildConfig.APPLICATION_ID}.extra.CALL_ACTION"

        const val ACTION_ANSWER: String = "answer"
        const val ACTION_REJECT: String = "reject"
        const val ACTION_HANGUP: String = "hangup"

        /**
         * Identificador único: existe no máximo uma chamada apresentada por vez, e a troca da
         * chamada recebida para a chamada em curso é uma **substituição** do mesmo aviso — dois
         * identificadores deixariam duas notificações de chamada na barra.
         * Fora da faixa usada pelo aviso de chamada bloqueada.
         */
        const val CALL_NOTIFICATION_ID: Int = 2000

        private const val REQUEST_CONTENT = 2001
        private const val REQUEST_ANSWER = 2002
        private const val REQUEST_REJECT = 2003
        private const val REQUEST_HANGUP = 2004
        private const val REQUEST_FULL_SCREEN = 2005
    }
}

/**
 * Consulta a capacidade de ocupar a tela. A consulta só existe a partir da versão 34; abaixo dela
 * a permissão é normal e concedida na instalação, sem meio de revogação pelo usuário.
 */
private fun fullScreenIntentAllowed(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        podeOcuparATela(manager)
    } else {
        true
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun podeOcuparATela(manager: NotificationManager): Boolean = manager.canUseFullScreenIntent()

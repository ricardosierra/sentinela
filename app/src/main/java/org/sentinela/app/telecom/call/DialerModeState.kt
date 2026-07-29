package org.sentinela.app.telecom.call

/**
 * Estado do modo discador conforme a interface precisa apresentá-lo.
 *
 * É deliberadamente um estado **derivado**, nunca um valor gravado. Uma marca persistida dizendo
 * "modo discador ligado" vira mentira no instante em que o usuário troca o aplicativo de telefone
 * nas configurações do sistema, e a partir daí o aplicativo mostra uma tela que não corresponde à
 * realidade do aparelho.
 */
enum class DialerModeState {

    /** O aparelho não oferece o papel de telefone padrão. Nada a oferecer ao usuário. */
    UNAVAILABLE,

    /**
     * O papel existe, mas a leitura da agenda foi negada. O modo discador não é oferecido: sem
     * saber quem é contato, o aplicativo passaria a tratar todo mundo como desconhecido, e ativar
     * o modo justamente para aplicar políticas por origem viraria o oposto do que promete.
     */
    BLOCKED_BY_CONTACTS,

    /** Pode ser oferecido ao usuário. */
    OFFERED,

    /** O aplicativo detém o papel: o modo discador está valendo. */
    ACTIVE,

    /**
     * O usuário havia ativado o modo e o papel não é mais deste aplicativo — outro aplicativo
     * assumiu, o usuário trocou nas configurações, uma atualização mexeu no padrão.
     *
     * A interface apresenta isto como **aviso informativo**, nunca como erro: nada quebrou, o modo
     * filtro continua operante e a única coisa que o usuário perdeu foi a tela de chamada própria.
     */
    ROLE_LOST,
}

/**
 * Deriva o estado do modo discador de três sinais do sistema mais a intenção gravada do usuário.
 *
 * A precedência é o conteúdo desta função, e o ponto central dela é: **o papel detido sempre vence
 * a intenção gravada.** Se o sistema diz que este aplicativo é o telefone padrão, o modo está
 * ativo, mesmo que nenhuma intenção tenha sido registrada — o usuário pode ter escolhido o
 * aplicativo direto nas configurações do sistema, sem passar pela tela de ativação.
 */
fun dialerModeState(
    roleAvailable: Boolean,
    roleHeld: Boolean,
    contactsGranted: Boolean,
    userOptedIn: Boolean,
): DialerModeState = when {
    !roleAvailable -> DialerModeState.UNAVAILABLE
    roleHeld -> DialerModeState.ACTIVE
    !contactsGranted -> DialerModeState.BLOCKED_BY_CONTACTS
    userOptedIn -> DialerModeState.ROLE_LOST
    else -> DialerModeState.OFFERED
}

/**
 * Costura de cancelamento do aviso de chamada.
 *
 * Pura de propósito: quem sabe postar e cancelar notificação é a camada de plataforma, e este
 * arquivo precisa continuar testável sem ela. O plano 06-06 fornece a implementação.
 */
fun interface CallNotificationCanceller {
    fun cancelCallNotification()
}

/**
 * Limpeza da reversão. A lista é curta e isso não é descuido: as chamadas pertencem ao sistema de
 * telefonia, não a este aplicativo; não há conta de telefone registrada por nós e nenhum estado de
 * telefonia persistido. Só sobra o que é nosso — o aviso ainda postado e a sessão em memória.
 *
 * O que esta função **nunca** faz: desabilitar componente próprio pelo gerenciador de pacotes. A
 * plataforma removeria o papel e encerraria o aplicativo.
 */
fun onDialerModeReverted(
    store: CallSessionStore,
    notifications: CallNotificationCanceller? = null,
) {
    notifications?.cancelCallNotification()
    store.detach()
}

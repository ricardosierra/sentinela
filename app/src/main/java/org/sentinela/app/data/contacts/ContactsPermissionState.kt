package org.sentinela.app.data.contacts

/**
 * Estado da permissao READ_CONTACTS do ponto de vista do usuario (CTT-01).
 *
 * Por que existe um flag persistido no meio disso:
 * `shouldShowRequestPermissionRationale` devolve `false` nos DOIS extremos — antes do
 * primeiro pedido e depois da negacao permanente ("nao perguntar de novo"). Nao existe API
 * publica que separe os dois casos. Sem o flag `contacts_permission_asked` gravado em disco,
 * o app so teria duas condutas possiveis, ambas erradas: repedir a permissao a cada abertura
 * para quem ja negou de vez, ou nunca oferecer o atalho para as Configuracoes do sistema.
 *
 * Quando gravar o flag: **no momento em que o launcher da permissao e disparado**, nunca no
 * callback. O usuario pode matar o app com o dialogo do sistema aberto — se o flag dependesse
 * do callback, o app voltaria achando que nunca perguntou e pediria de novo.
 *
 * Esta fase entrega apenas o contrato. A tela que consome estes estados (explicacao antes do
 * dialogo, atalho para as Configuracoes) e da Fase 7; nenhuma UI nasce aqui.
 */
enum class ContactsPermissionState {
    /** Concedida: o lookup de contatos pode acontecer. */
    GRANTED,

    /** Nunca pedimos. Unico estado em que o dialogo do sistema aparece pela primeira vez. */
    NEVER_ASKED,

    /** Negada, mas a plataforma ainda aceita um novo pedido com explicacao. */
    DENIED_ONCE,

    /** Negada de vez: o dialogo nao aparece mais. So restam as Configuracoes do sistema. */
    DENIED_PERMANENTLY,
}

/**
 * Faz sentido disparar o launcher da permissao? So antes do primeiro pedido e depois de uma
 * negacao simples — em `DENIED_PERMANENTLY` o dialogo nao aparece e a chamada seria um no-op
 * silencioso para o usuario.
 */
val ContactsPermissionState.canRequest: Boolean
    get() = this == ContactsPermissionState.NEVER_ASKED ||
        this == ContactsPermissionState.DENIED_ONCE

/**
 * O atalho para as Configuracoes do app aparece SO na negacao permanente. Mostra-lo antes
 * disso seria insistir com quem ainda nem foi perguntado — proibido pelo CONTEXT da fase.
 */
val ContactsPermissionState.shouldOfferSystemSettings: Boolean
    get() = this == ContactsPermissionState.DENIED_PERMANENTLY

/**
 * Regra pura e determinística: recebe os tres sinais ja coletados e devolve o estado.
 *
 * Deliberadamente sem nenhum `import android.*` — a leitura de `checkSelfPermission` e de
 * `shouldShowRequestPermissionRationale` fica na camada fina `platform/ContactsPermissionChecker`,
 * de modo que toda a regra seja testavel em JVM e medida pelo Kover.
 *
 * @param granted resultado de `checkSelfPermission(READ_CONTACTS) == PERMISSION_GRANTED`.
 * @param alreadyAsked flag persistido: ja disparamos o launcher alguma vez nesta instalacao.
 * @param rationale resultado de `shouldShowRequestPermissionRationale`, ambiguo sozinho.
 */
fun contactsPermissionState(
    granted: Boolean,
    alreadyAsked: Boolean,
    rationale: Boolean,
): ContactsPermissionState = when {
    granted -> ContactsPermissionState.GRANTED
    // rationale e falso aqui porque nunca perguntamos, nao porque negaram de vez.
    !alreadyAsked -> ContactsPermissionState.NEVER_ASKED
    rationale -> ContactsPermissionState.DENIED_ONCE
    else -> ContactsPermissionState.DENIED_PERMANENTLY
}

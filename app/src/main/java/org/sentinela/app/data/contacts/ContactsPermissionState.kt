package org.sentinela.app.data.contacts

import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.permissions.runtimePermissionAsk

/**
 * Estado da permissao READ_CONTACTS do ponto de vista do usuario (CTT-01).
 *
 * Fachada nomeada da regra generica [RuntimePermissionAsk], que a Fase 5 extraiu para servir
 * tambem a permissao de notificacoes. Este enum e mantido porque a agenda ja tem contrato
 * publico com ele; a regra, porem, vive num lugar so.
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
 * Delega inteiramente a [runtimePermissionAsk] — nenhuma condicao propria mora aqui, para que
 * agenda e notificacoes nunca possam divergir. Deliberadamente sem nenhum `import android.*`:
 * a leitura de `checkSelfPermission` e de `shouldShowRequestPermissionRationale` fica na camada
 * fina `platform/ContactsPermissionChecker`, testavel em JVM e medida pelo Kover.
 *
 * @param granted resultado de `checkSelfPermission(READ_CONTACTS) == PERMISSION_GRANTED`.
 * @param alreadyAsked flag persistido: ja disparamos o launcher alguma vez nesta instalacao.
 * @param rationale resultado de `shouldShowRequestPermissionRationale`, ambiguo sozinho.
 */
fun contactsPermissionState(
    granted: Boolean,
    alreadyAsked: Boolean,
    rationale: Boolean,
): ContactsPermissionState = when (runtimePermissionAsk(granted, alreadyAsked, rationale)) {
    RuntimePermissionAsk.GRANTED -> ContactsPermissionState.GRANTED
    RuntimePermissionAsk.NEVER_ASKED -> ContactsPermissionState.NEVER_ASKED
    RuntimePermissionAsk.DENIED_ONCE -> ContactsPermissionState.DENIED_ONCE
    RuntimePermissionAsk.DENIED_PERMANENTLY -> ContactsPermissionState.DENIED_PERMANENTLY
}

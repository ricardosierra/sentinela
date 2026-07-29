package org.sentinela.app.permissions

/**
 * Estado de uma permissao de runtime do ponto de vista do usuario — regra generica, valida
 * para READ_CONTACTS (Fase 4), POST_NOTIFICATIONS (Fase 5) e qualquer permissao futura.
 *
 * Por que existe um flag persistido no meio disso:
 * `shouldShowRequestPermissionRationale` devolve `false` nos DOIS extremos — antes do
 * primeiro pedido e depois da negacao permanente ("nao perguntar de novo"). Nao existe API
 * publica que separe os dois casos. Sem um flag "ja perguntei" gravado em disco, o app so
 * teria duas condutas possiveis, ambas erradas: repedir a permissao a cada abertura para
 * quem ja negou de vez, ou nunca oferecer o atalho para as Configuracoes do sistema.
 *
 * Quando gravar o flag: **no momento em que o launcher da permissao e disparado**, nunca no
 * callback. O usuario pode matar o app com o dialogo do sistema aberto — se o flag dependesse
 * do callback, o app voltaria achando que nunca perguntou e pediria de novo.
 *
 * Deliberadamente sem nenhuma importacao da plataforma: a leitura de `checkSelfPermission` e de
 * `shouldShowRequestPermissionRationale` vive nas camadas finas de `platform/`, de modo que
 * toda a regra seja testavel em JVM e medida pelo Kover.
 */
enum class RuntimePermissionAsk {
    /** Concedida: a capacidade pode ser usada. */
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
 * negacao simples — em [RuntimePermissionAsk.DENIED_PERMANENTLY] o dialogo nao aparece e a
 * chamada seria um no-op silencioso para o usuario.
 */
val RuntimePermissionAsk.canRequest: Boolean
    get() = this == RuntimePermissionAsk.NEVER_ASKED || this == RuntimePermissionAsk.DENIED_ONCE

/**
 * O atalho para as Configuracoes do app aparece SO na negacao permanente. Mostra-lo antes
 * disso seria insistir com quem ainda nem foi perguntado.
 */
val RuntimePermissionAsk.shouldOfferSystemSettings: Boolean
    get() = this == RuntimePermissionAsk.DENIED_PERMANENTLY

/**
 * Regra pura e deterministica: recebe os tres sinais ja coletados e devolve o estado.
 *
 * @param granted resultado de `checkSelfPermission(permissao) == PERMISSION_GRANTED`.
 * @param alreadyAsked flag persistido: ja disparamos o launcher alguma vez nesta instalacao.
 * @param rationale resultado de `shouldShowRequestPermissionRationale`, ambiguo sozinho.
 */
fun runtimePermissionAsk(
    granted: Boolean,
    alreadyAsked: Boolean,
    rationale: Boolean,
): RuntimePermissionAsk = when {
    granted -> RuntimePermissionAsk.GRANTED
    // rationale e falso aqui porque nunca perguntamos, nao porque negaram de vez.
    !alreadyAsked -> RuntimePermissionAsk.NEVER_ASKED
    rationale -> RuntimePermissionAsk.DENIED_ONCE
    else -> RuntimePermissionAsk.DENIED_PERMANENTLY
}

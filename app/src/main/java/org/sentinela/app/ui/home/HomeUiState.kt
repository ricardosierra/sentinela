package org.sentinela.app.ui.home

import androidx.annotation.StringRes
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.telecom.call.DialerModeState

/**
 * Valor de uma estatística da home, fechado nos três estados que ela pode ter de verdade.
 *
 * Este tipo é o guarda-corpo do **zero mentiroso**, e a regra deixou de ser convenção para virar
 * tipo: não existe caminho em que a interface renderize `0` sem que a contagem tenha sido
 * efetivamente carregada, porque não existe número algum na assinatura de [Unavailable] nem de
 * [Loading] para a tela renderizar. Histórico desligado, retenção que não guarda, primeiro quadro
 * antes da primeira emissão e falha de leitura são estados — e um `0` no lugar deles seria pior que
 * número nenhum, porque afirmaria ao usuário que nada foi bloqueado.
 *
 * **Proibido para sempre** acrescentar aqui um construtor que aceite número sem a garantia de
 * carregamento — nem com valor padrão, nem "só para a pré-visualização". Um segundo caminho com
 * número devolve exatamente o defeito que este tipo existe para tornar impossível.
 */
sealed interface StatValue {

    /** A contagem foi lida. Aqui, e só aqui, `0` é verdade e pode ser exibido. */
    data class Loaded(val count: Long) : StatValue

    /** Não existe contagem a mostrar: histórico desligado, retenção que não guarda, ou falha. */
    data object Unavailable : StatValue

    /** Ainda não sabemos. Esqueleto tonal na tela, nunca `0` como reserva. */
    data object Loading : StatValue
}

/**
 * Última chamada bloqueada como a home precisa apresentá-la.
 *
 * A fronteira de privacidade herdada da Fase 6 vale aqui inteira: **nenhum campo deste registro
 * carrega a sequência completa de dígitos.** O que chega é o texto já mascarado, produzido pela
 * máscara única do aplicativo antes de o estado existir; quem monta este objeto é o dono de estado,
 * e a tela não tem como obter o número original nem se quiser. Acrescentar aqui um campo com os
 * dígitos crus vazaria o número para o estado de interface, para as pré-visualizações e para
 * qualquer relatório de falha que serialize o estado.
 */
data class LastBlockedUi(
    /** Texto já mascarado, na forma `+55 11 9****-1234`. */
    val maskedNumber: String,
    /** Rótulo do motivo REAL da decisão. Ver [reasonLabelRes]. */
    @StringRes val reasonLabelRes: Int,
    val timestampUtcMillis: Long,
)

/**
 * Estado completo da home.
 *
 * O papel de triagem e o modo discador aparecem aqui como resultado de **consulta viva**, feita a
 * cada retomada da tela — nunca de valor guardado em disco. A plataforma não oferece aviso de
 * mudança de detentor de papel para aplicativo comum, e perder um papel encerra o processo do
 * aplicativo (medido três vezes na Fase 6): estado persistido sobre papel mente por construção.
 */
data class HomeUiState(
    val protectionEnabled: Boolean = true,
    val screeningRoleHeld: Boolean = false,
    val screeningRoleAvailable: Boolean = false,
    val contactsPermission: ContactsPermissionState = ContactsPermissionState.NEVER_ASKED,
    val dialerMode: DialerModeState = DialerModeState.UNAVAILABLE,
    val totalBlocked: StatValue = StatValue.Loading,
    val blockedToday: StatValue = StatValue.Loading,
    val lastBlocked: LastBlockedUi? = null,
    val historyEnabled: Boolean = true,
    val readError: Boolean = false,
)

/**
 * Rótulo do motivo da decisão, resolvido para recurso de texto.
 *
 * O aplicativo **não classifica spam** e não tem esse dado: não existe rótulo de risco, de fraude
 * ou de "spam conhecido" como opção aqui, e acrescentar um exigiria inventar informação que o
 * aparelho nunca teve. Os motivos que jamais chegam a virar registro de bloqueio — chamada de
 * saída, proteção desligada, chamada repetida que faz tocar — caem no rótulo de desconhecido por
 * segurança, em vez de num texto próprio que prometeria uma classificação inexistente.
 */
@StringRes
fun reasonLabelRes(reason: DecisionReason): Int = when (reason) {
    DecisionReason.PRIVATE_NUMBER -> R.string.history_private_number
    DecisionReason.CONTACT -> R.string.call_origin_contact
    DecisionReason.PERSONAL_WHITELIST -> R.string.call_origin_whitelist
    DecisionReason.UNKNOWN_NUMBER,
    DecisionReason.INVALID_NUMBER,
    DecisionReason.LOCAL_LOOKUP_FAILURE,
    DecisionReason.FALLBACK_POLICY,
    DecisionReason.OUTGOING_CALL,
    DecisionReason.PROTECTION_DISABLED,
    DecisionReason.REPEATED_CALL,
    DecisionReason.EMERGENCY_NUMBER,
    -> R.string.history_unknown_number
}

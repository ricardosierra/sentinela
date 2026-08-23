package org.sentinela.app.ui.home

import androidx.annotation.StringRes
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState

/**
 * A intencao que um aviso da home oferece, quando oferece alguma.
 *
 * E um tipo fechado, e nao uma funcao guardada dentro do aviso, porque a precedencia dos avisos
 * precisa ser decidida SEM Compose — ver [avisosDaHome]. Funcao de retorno so existe onde ha
 * container para dispara-la, e a regra de precedencia nao tem nem precisa de container.
 */
internal enum class AcaoDoAviso {
    CORRIGIR_PAPEL,
    PEDIR_AGENDA,
    ABRIR_CONFIGURACOES_DO_APLICATIVO,
    LIGAR_HISTORICO,
    TENTAR_LEITURA_DE_NOVO,
    ABRIR_ATIVACAO_DO_DISCADOR,
}

/**
 * Um aviso da home, ainda em identificador de recurso e intencao — nunca em texto resolvido.
 *
 * Guardar o texto ja resolvido aqui obrigaria a lista inteira a nascer dentro de uma composta, que e
 * exatamente o que tirava a precedencia do alcance de um teste comum.
 */
internal data class AvisoDaHome(
    @StringRes val textRes: Int,
    @StringRes val actionLabelRes: Int?,
    val acao: AcaoDoAviso?,
)

/**
 * Os avisos que o estado atual justifica, na ordem de precedencia ditada pelo contrato: papel de
 * triagem ausente, leitura da agenda negada, historico desligado, falha de leitura e papel de
 * discador perdido.
 *
 * Funcao PURA, sem Compose e sem plataforma. Ela era o corpo de uma composta, e so era composta
 * porque resolvia texto de recurso ali mesmo; a precedencia em si nunca dependeu de composicao. Como
 * funcao comum, cada ramo desta lista pode ser afirmado em teste de JVM, sem Robolectric e sem arvore
 * de semantica — que e a diferenca entre provar a REGRA e provar o desenho dela.
 *
 * Dois ramos merecem registro, porque nao sao acabamento:
 *
 * - **Protecao desligada pelo usuario nao gera aviso.** E escolha, nao erro, e alarmar alguem pela
 *   propria decisao e pressao. O cartao principal ja mostra o estado, e as estatisticas continuam
 *   visiveis.
 * - **Sem o papel disponivel no aparelho, o botao de correcao NAO aparece.** A intencao de pedido nao
 *   existe ali, o toque nao resolveria nada, e oferecer um botao inerte e pior que nao oferecer.
 */
internal fun avisosDaHome(state: HomeUiState): List<AvisoDaHome> {
    val avisos = mutableListOf<AvisoDaHome>()
    if (!state.screeningRoleHeld) {
        avisos += AvisoDaHome(
            textRes = R.string.dashboard_role_missing,
            actionLabelRes = R.string.dashboard_fix_configuration.takeIf { state.screeningRoleAvailable },
            acao = AcaoDoAviso.CORRIGIR_PAPEL.takeIf { state.screeningRoleAvailable },
        )
    }
    if (state.contactsPermission != ContactsPermissionState.GRANTED) {
        val definitiva = state.contactsPermission == ContactsPermissionState.DENIED_PERMANENTLY
        avisos += AvisoDaHome(
            textRes = R.string.dashboard_contacts_missing,
            actionLabelRes = if (definitiva) {
                R.string.about_open_app_settings
            } else {
                R.string.dialer_activation_grant_contacts
            },
            acao = if (definitiva) {
                AcaoDoAviso.ABRIR_CONFIGURACOES_DO_APLICATIVO
            } else {
                AcaoDoAviso.PEDIR_AGENDA
            },
        )
    }
    if (!state.historyEnabled) {
        avisos += AvisoDaHome(
            textRes = R.string.dashboard_history_off,
            actionLabelRes = R.string.dashboard_history_off_action,
            acao = AcaoDoAviso.LIGAR_HISTORICO,
        )
    }
    if (state.readError) {
        avisos += AvisoDaHome(
            textRes = R.string.state_error,
            actionLabelRes = R.string.action_retry,
            acao = AcaoDoAviso.TENTAR_LEITURA_DE_NOVO,
        )
    }
    if (state.dialerMode == DialerModeState.ROLE_LOST) {
        avisos += AvisoDaHome(
            textRes = R.string.dialer_role_lost_body,
            actionLabelRes = R.string.dialer_role_lost_action,
            acao = AcaoDoAviso.ABRIR_ATIVACAO_DO_DISCADOR,
        )
    }
    return avisos
}

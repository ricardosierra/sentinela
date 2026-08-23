package org.sentinela.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.telecom.call.DialerModeState

/**
 * A precedencia dos avisos da home, provada como REGRA e nao como desenho.
 *
 * `HomeScreenStateTest` ja compoe a tela e conta banners na arvore de semantica; ele continua sendo
 * a prova de que a regra chega ao usuario. O que este arquivo acrescenta e a prova da regra em si,
 * sem Robolectric e sem arvore: enquanto a precedencia morava dentro de uma composta, afirmar a
 * ORDEM dos cinco motivos exigia montar a tela e ler texto, e o teto de dois escondia do teste
 * qualquer coisa a partir do terceiro. Aqui a lista inteira e visivel, inclusive o que o teto corta.
 *
 * O teto de dois NAO e afirmado aqui de proposito: ele e decisao de apresentacao, mora em
 * `BlocoDeAvisos` e continua coberto pelo caso de arvore. Esta funcao devolve tudo que o estado
 * justifica; quem corta e a composta.
 */
class HomeWarningsTest {

    private val saudavel = HomeUiState(
        screeningRoleHeld = true,
        screeningRoleAvailable = true,
        contactsPermission = ContactsPermissionState.GRANTED,
        dialerMode = DialerModeState.OFFERED,
        historyEnabled = true,
        readError = false,
    )

    @Test
    fun `estado saudavel nao justifica aviso nenhum`() {
        assertEquals(emptyList<AvisoDaHome>(), avisosDaHome(saudavel))
    }

    @Test
    fun `protecao desligada pelo usuario nao e erro e nao gera aviso`() {
        assertEquals(emptyList<AvisoDaHome>(), avisosDaHome(saudavel.copy(protectionEnabled = false)))
    }

    @Test
    fun `os cinco motivos saem na ordem de precedencia do contrato`() {
        val avisos = avisosDaHome(
            saudavel.copy(
                screeningRoleHeld = false,
                contactsPermission = ContactsPermissionState.DENIED_ONCE,
                historyEnabled = false,
                readError = true,
                dialerMode = DialerModeState.ROLE_LOST,
            ),
        )
        assertEquals(
            listOf(
                R.string.dashboard_role_missing,
                R.string.dashboard_contacts_missing,
                R.string.dashboard_history_off,
                R.string.state_error,
                R.string.dialer_role_lost_body,
            ),
            avisos.map { it.textRes },
        )
    }

    @Test
    fun `papel ausente com papel disponivel oferece a correcao`() {
        val aviso = avisosDaHome(saudavel.copy(screeningRoleHeld = false)).single()
        assertEquals(R.string.dashboard_fix_configuration, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.CORRIGIR_PAPEL, aviso.acao)
    }

    @Test
    fun `papel indisponivel no aparelho avisa sem oferecer botao inerte`() {
        val aviso = avisosDaHome(
            saudavel.copy(screeningRoleHeld = false, screeningRoleAvailable = false),
        ).single()
        assertEquals(R.string.dashboard_role_missing, aviso.textRes)
        assertNull("botao que nao pode dar em nada e pior que a ausencia dele", aviso.acao)
        assertNull(aviso.actionLabelRes)
    }

    @Test
    fun `agenda negada uma vez oferece novo pedido`() {
        val aviso = avisosDaHome(
            saudavel.copy(contactsPermission = ContactsPermissionState.DENIED_ONCE),
        ).single()
        assertEquals(R.string.dialer_activation_grant_contacts, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.PEDIR_AGENDA, aviso.acao)
    }

    @Test
    fun `agenda negada de vez manda para as configuracoes do aplicativo`() {
        val aviso = avisosDaHome(
            saudavel.copy(contactsPermission = ContactsPermissionState.DENIED_PERMANENTLY),
        ).single()
        assertEquals(R.string.about_open_app_settings, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.ABRIR_CONFIGURACOES_DO_APLICATIVO, aviso.acao)
    }

    @Test
    fun `agenda nunca pedida tambem justifica o aviso`() {
        val aviso = avisosDaHome(
            saudavel.copy(contactsPermission = ContactsPermissionState.NEVER_ASKED),
        ).single()
        assertEquals(R.string.dashboard_contacts_missing, aviso.textRes)
        assertEquals(AcaoDoAviso.PEDIR_AGENDA, aviso.acao)
    }

    @Test
    fun `historico desligado oferece liga-lo`() {
        val aviso = avisosDaHome(saudavel.copy(historyEnabled = false)).single()
        assertEquals(R.string.dashboard_history_off_action, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.LIGAR_HISTORICO, aviso.acao)
    }

    @Test
    fun `falha de leitura oferece tentar de novo`() {
        val aviso = avisosDaHome(saudavel.copy(readError = true)).single()
        assertEquals(R.string.action_retry, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.TENTAR_LEITURA_DE_NOVO, aviso.acao)
    }

    @Test
    fun `papel de discador perdido leva a tela de ativacao`() {
        val aviso = avisosDaHome(saudavel.copy(dialerMode = DialerModeState.ROLE_LOST)).single()
        assertEquals(R.string.dialer_role_lost_action, aviso.actionLabelRes)
        assertEquals(AcaoDoAviso.ABRIR_ATIVACAO_DO_DISCADOR, aviso.acao)
    }

    @Test
    fun `modo discador apenas oferecido ou ativo nao justifica aviso`() {
        listOf(
            DialerModeState.UNAVAILABLE,
            DialerModeState.OFFERED,
            DialerModeState.ACTIVE,
        ).forEach { modo ->
            assertEquals(
                "so ROLE_LOST vira aviso; $modo nao",
                emptyList<AvisoDaHome>(),
                avisosDaHome(saudavel.copy(dialerMode = modo)),
            )
        }
    }
}

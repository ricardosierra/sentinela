package org.sentinela.app.ui.settings

import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.telecom.call.DialerModeState

/**
 * Estado da tela Proteção.
 *
 * O tipo nasceu dentro de `SettingsViewModel.kt` (plano 07-04) e mudou de casa para cá quando a tela
 * passou a existir: quem consome o estado é a composta, e o dono de estado é apenas um dos produtores
 * possíveis dele. Com o tipo em arquivo próprio, cada pré-visualização e cada caso de teste da tela
 * monta o estado direto, sem instanciar repositório, banco ou consulta ao sistema.
 *
 * [historyRecordCount] existe por causa da confirmação da §9.2: o diálogo de limpar histórico precisa
 * dizer QUANTOS registros serão apagados. "Apagar tudo" sem número é pedir consentimento no escuro.
 *
 * [loading] é o primeiro quadro, antes de o retrato do repositório chegar. Ele existe para a tela
 * poder dizer que ainda não sabe, em vez de desenhar os padrões de fábrica como se fossem a escolha
 * do usuário — o mesmo argumento que fez `StatValue` fechar o zero mentiroso em 07-04.
 */
data class SettingsUiState(
    val settings: ScreeningSettings = ScreeningSettings(),
    val screeningRoleHeld: Boolean = false,
    val screeningRoleAvailable: Boolean = false,
    val dialerMode: DialerModeState = DialerModeState.UNAVAILABLE,
    val historyRecordCount: Long = 0L,
    val loading: Boolean = true,
)

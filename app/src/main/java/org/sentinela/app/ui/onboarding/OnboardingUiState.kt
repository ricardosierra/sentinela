package org.sentinela.app.ui.onboarding

import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.settings.ScreeningSettings

/** Quantidade de passos do onboarding, do primeiro de explicação ao de verificação final. */
const val TOTAL_DE_PASSOS = 6

/**
 * Estado do onboarding.
 *
 * O ponto de desenho mais importante deste tipo é o que ele **não** faz: o padrão de cada passo não
 * é redefinido aqui. Ele vem inteiro de [settings], e os valores de fábrica de [ScreeningSettings]
 * já são exatamente os dos mockups — bloquear desconhecidos, tocar para contatos, nunca silenciar a
 * lista pessoal, bloquear números privados ligado e notificação própria desligada.
 *
 * A consequência é a que interessa: o onboarding **reflete** o repositório em vez de redefinir
 * padrão, e é por isso que pular todos os passos aplica os padrões corretos sem escrever uma única
 * configuração. Uma segunda lista de padrões aqui divergiria da primeira no primeiro ajuste de
 * produto, e o usuário que pulasse acabaria com uma configuração diferente da de quem avançasse
 * aceitando tudo.
 *
 * Nenhum passo é bloqueante: papel negado, agenda negada e notificação negada avançam normalmente.
 * [roleDenied] existe para a tela poder explicar o que o usuário perde, nunca para barrá-lo.
 */
data class OnboardingUiState(
    val step: Int = 0,
    val totalSteps: Int = TOTAL_DE_PASSOS,
    val screeningRoleHeld: Boolean = false,
    val roleRequestInFlight: Boolean = false,
    val roleDenied: Boolean = false,
    val contactsPermission: ContactsPermissionState = ContactsPermissionState.NEVER_ASKED,
    val notificationPermission: RuntimePermissionAsk = RuntimePermissionAsk.NEVER_ASKED,
    val settings: ScreeningSettings = ScreeningSettings(),
)

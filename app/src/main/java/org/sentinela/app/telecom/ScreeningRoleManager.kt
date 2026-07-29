package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/**
 * Encapsula o gerenciador de papéis do sistema para a interface nunca falar com a telefonia
 * diretamente.
 *
 * O papel de triagem pode ser perdido a qualquer momento: outro aplicativo assume, o usuário
 * troca a escolha nas configurações do sistema, uma atualização mexe no padrão. E **não existe**
 * aviso dessa mudança para um aplicativo comum — o único ouvinte oferecido pela plataforma é de
 * uso do próprio sistema e exige uma permissão que está fora da lista permitida deste projeto.
 * Nenhum esforço deve ser gasto procurando um observador que não existe.
 *
 * A consequência prática é simples: a verificação só pode ser uma pergunta pontual, feita quando
 * a tela volta ao primeiro plano (Fase 7), e a interface oferece o caminho de corrigir a
 * configuração quando a resposta for negativa.
 */
class ScreeningRoleManager(private val context: Context) {

    private val roleManager: RoleManager?
        get() = context.getSystemService(RoleManager::class.java)

    fun isRoleAvailable(): Boolean =
        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isRoleHeld(): Boolean =
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    /**
     * Intenção para o onboarding e para a correção de configuração. Só existe quando o aparelho
     * de fato oferece o papel: em aparelho que não o oferece, o sistema ainda devolve uma
     * intenção, e disparar essa intenção levaria o usuário a uma tela que não resolve nada.
     */
    fun buildRequestIntent(): Intent? =
        if (isRoleAvailable()) {
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            null
        }
}

package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/**
 * Base comum das consultas de papel do sistema, parametrizada pelo papel.
 *
 * Existe porque o modo discador precisa exatamente das mesmas três perguntas que a triagem já
 * fazia — o aparelho oferece este papel, este aplicativo o detém, e qual é a intenção de pedido —
 * mudando só qual papel se consulta. Duplicar essa forma para o segundo papel seria repetir também
 * cada armadilha dela.
 *
 * A armadilha principal, comum aos dois papéis: **não existe aviso de mudança de detentor** para um
 * aplicativo comum. O único ouvinte que a plataforma oferece é de uso do próprio sistema e exige
 * uma permissão fora da lista permitida deste projeto. A verificação só pode ser pergunta pontual,
 * feita quando a tela volta ao primeiro plano. Nenhum esforço deve ser gasto procurando um
 * observador que não existe.
 */
abstract class SystemRoleGate(
    private val context: Context,
    private val role: String,
) {

    private val roleManager: RoleManager?
        get() = context.getSystemService(RoleManager::class.java)

    fun isRoleAvailable(): Boolean = roleManager?.isRoleAvailable(role) == true

    fun isRoleHeld(): Boolean = roleManager?.isRoleHeld(role) == true

    /**
     * Intenção de pedido do papel. Só existe quando o aparelho de fato o oferece: em aparelho que
     * não o oferece, o sistema ainda devolve uma intenção, e dispará-la levaria o usuário a uma
     * tela que não resolve nada.
     */
    fun buildRequestIntent(): Intent? =
        if (isRoleAvailable()) roleManager?.createRequestRoleIntent(role) else null
}

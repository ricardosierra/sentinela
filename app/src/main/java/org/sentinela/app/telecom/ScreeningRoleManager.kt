package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/**
 * Encapsula o RoleManager para a UI nunca falar com o Telecom diretamente.
 * O papel pode ser perdido a qualquer momento (outro app assumiu) — a home
 * verifica em toda retomada e oferece o botão de corrigir configuração.
 */
class ScreeningRoleManager(private val context: Context) {

    private val roleManager: RoleManager?
        get() = context.getSystemService(RoleManager::class.java)

    fun isRoleAvailable(): Boolean =
        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isRoleHeld(): Boolean =
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    /** Intent para ActivityResultLauncher no onboarding/correção. */
    fun buildRequestIntent(): Intent? =
        roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
}

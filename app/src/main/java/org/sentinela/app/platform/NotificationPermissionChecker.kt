package org.sentinela.app.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.permissions.runtimePermissionAsk

/**
 * Unico arquivo do app que toca ContextCompat/ActivityCompat para POST_NOTIFICATIONS.
 *
 * Espelha [ContactsPermissionChecker]: nenhuma regra propria mora aqui — toda decisao e
 * delegada a funcao pura [runtimePermissionAsk], que roda em JVM e e medida pelo Kover.
 * Vive em `platform/` para que uma camada sem teste unitario nao derrube o gate de cobertura.
 *
 * A permissao ja esta DECLARADA no manifest desde a Fase 1; esta classe existe para o pedido
 * em runtime, que so acontece no momento do opt-in do usuario — nunca no onboarding.
 *
 * minSdk 29: abaixo da API 33 a permissao nao existe e a notificacao funciona sem pedido
 * algum, entao [isGranted] responde `true` sem consultar nada.
 */
class NotificationPermissionChecker {

    fun isGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

    /**
     * Exige Activity, nao Context: `shouldShowRequestPermissionRationale` so existe em Activity.
     *
     * @param alreadyAsked vem de `DataStoreSettingsRepository.notificationPermissionAsked` — sem
     * ele o rationale e ambiguo e NEVER_ASKED e DENIED_PERMANENTLY seriam indistinguiveis.
     */
    fun state(activity: Activity, alreadyAsked: Boolean): RuntimePermissionAsk =
        runtimePermissionAsk(
            granted = isGranted(activity),
            alreadyAsked = alreadyAsked,
            rationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
        )

    /**
     * Atalho para a tela de detalhes do app nas Configuracoes do sistema. Usar SOMENTE quando
     * `shouldOfferSystemSettings` for verdadeiro.
     */
    fun appSettingsIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
}

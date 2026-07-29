package org.sentinela.app.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.sentinela.app.permissions.RuntimePermissionAsk
import org.sentinela.app.permissions.runtimePermissionAsk

/**
 * Unico arquivo do app que toca ContextCompat/ActivityCompat para a permissao de originar chamada.
 *
 * Molde exato de [NotificationPermissionChecker] e de [ContactsPermissionChecker]: camada fina, sem
 * regra propria — a decisao inteira e delegada a funcao pura [runtimePermissionAsk], que roda em
 * JVM e e medida pelo Kover. Vive em `platform/` pelo precedente da Fase 4, para que uma camada sem
 * teste unitario nao derrube o gate de cobertura sem exclude novo.
 *
 * A permissao ja esta DECLARADA no manifest desde o plano 06-03. A medicao da pesquisa: ela chega
 * ao aparelho NAO concedida no install, ao contrario da permissao de intencao de tela cheia — ou
 * seja, precisa de pedido em runtime, no momento do uso.
 *
 * Quem grava o sinalizador de "ja perguntei" e a tela, no instante em que dispara o launcher, nunca
 * no retorno do dialogo: o usuario pode matar o app com o dialogo do sistema aberto.
 */
class CallPhonePermissionChecker {

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Exige Activity, nao Context: `shouldShowRequestPermissionRationale` so existe em Activity.
     *
     * @param alreadyAsked vem de `DataStoreSettingsRepository.callPhonePermissionAsked` — sem ele o
     * rationale e ambiguo e NEVER_ASKED e DENIED_PERMANENTLY seriam indistinguiveis.
     */
    fun state(activity: Activity, alreadyAsked: Boolean): RuntimePermissionAsk =
        runtimePermissionAsk(
            granted = isGranted(activity),
            alreadyAsked = alreadyAsked,
            rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CALL_PHONE,
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

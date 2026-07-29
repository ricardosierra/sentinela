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
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.data.contacts.contactsPermissionState

/**
 * Unico arquivo do app que toca ContextCompat/ActivityCompat para a permissao de contatos.
 *
 * Vive em `platform/` de proposito, seguindo o precedente do [AndroidRegionProvider]: aqui nao
 * ha nenhuma regra propria — toda a decisao e delegada a funcao pura
 * [contactsPermissionState], que roda em JVM e e medida pelo Kover. Esta camada nao tem teste
 * unitario porque nao ha o que testar alem da chamada de plataforma; mante-la fora de
 * `data/contacts/` evita que um arquivo sem teste derrube o gate de cobertura.
 *
 * Nao pede a permissao e nao desenha nada: o launcher e a tela com a explicacao
 * (`R.string.contacts_permission_rationale`, ja existente) sao da Fase 7.
 */
class ContactsPermissionChecker {

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Exige Activity, nao Context: `shouldShowRequestPermissionRationale` so existe em Activity.
     *
     * @param alreadyAsked vem de `DataStoreSettingsRepository.contactsPermissionAsked` — sem ele
     * o rationale e ambiguo e os estados NEVER_ASKED e DENIED_PERMANENTLY seriam indistinguiveis.
     */
    fun state(activity: Activity, alreadyAsked: Boolean): ContactsPermissionState =
        contactsPermissionState(
            granted = isGranted(activity),
            alreadyAsked = alreadyAsked,
            rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_CONTACTS,
            ),
        )

    /**
     * Atalho para a tela de detalhes do app nas Configuracoes do sistema. Usar SOMENTE quando
     * `shouldOfferSystemSettings` for verdadeiro — oferece-lo antes disso e insistir com quem
     * ainda nem foi perguntado.
     */
    fun appSettingsIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
}

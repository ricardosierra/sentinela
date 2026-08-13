package org.sentinela.app.platform

import android.content.pm.PackageManager

/**
 * Utilitários para verificar a instalação de pacotes de terceiros.
 */
fun PackageManager.hasWhatsAppInstalled(): Boolean {
    return isPackageInstalled("com.whatsapp") || isPackageInstalled("com.whatsapp.w4b")
}

private fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0)
        true
    } catch (ignored: PackageManager.NameNotFoundException) {
        false
    }
}

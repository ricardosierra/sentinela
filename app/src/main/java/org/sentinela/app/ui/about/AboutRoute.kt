package org.sentinela.app.ui.about

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.sentinela.app.AppContainer
import org.sentinela.app.ui.navigation.Rotas

@Composable
internal fun AboutRoute(
    container: AppContainer,
    nav: NavController,
) {
    val context = LocalContext.current
    val dono: AboutViewModel = viewModel(
        factory = AboutViewModel.factory(container)
    )

    AboutScreen(
        onBack = { nav.popBackStack() },
        onClearAllData = {
            dono.clearAllData {
                // Return to welcome screen after clearing
                nav.navigate(Rotas.BOAS_VINDAS) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
        onOpenAppSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        },
        onOpenChannelSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    )
}

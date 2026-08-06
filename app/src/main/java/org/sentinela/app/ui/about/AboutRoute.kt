package org.sentinela.app.ui.about

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.sentinela.app.AppContainer
import org.sentinela.app.R
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

    // Aviso de falha da limpeza. Mora aqui, e não na tela, porque quem conhece o resultado da
    // operação é a rota — a tela só dispara a ação e não tem como saber se ela deu certo.
    var falhaAoLimpar by remember { mutableStateOf(false) }

    AboutScreen(
        onBack = { nav.popBackStack() },
        onClearAllData = {
            dono.clearAllData { sucesso ->
                if (sucesso) {
                    // Volta ao início SÓ quando apagou de verdade: navegar depois de uma falha
                    // faria a tela inicial servir de confirmação de algo que não aconteceu.
                    nav.navigate(Rotas.BOAS_VINDAS) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    falhaAoLimpar = true
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

    if (falhaAoLimpar) {
        AlertDialog(
            onDismissRequest = { falhaAoLimpar = false },
            title = { Text(stringResource(R.string.about_clear_failed_title)) },
            text = { Text(stringResource(R.string.about_clear_failed_body)) },
            confirmButton = {
                TextButton(onClick = { falhaAoLimpar = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
        )
    }
}

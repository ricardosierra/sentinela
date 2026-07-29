package org.sentinela.app.ui.dialer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.sentinela.app.R
import org.sentinela.app.ui.call.CallActionButton
import org.sentinela.app.ui.call.callAcceptColors
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberXl

/**
 * Hospedeira da tela de discagem.
 *
 * Ela existe por dois motivos ao mesmo tempo. O primeiro é funcional: um telefone padrão precisa
 * oferecer teclado de discagem. O segundo é de elegibilidade — o sistema só aceita este aplicativo
 * como telefone padrão se os dois filtros de ação de discagem estiverem declarados no manifest, e
 * eles precisam apontar para uma tela de verdade.
 *
 * Declarar esses filtros **não** faz o aplicativo se intrometer na discagem de quem não ativou o
 * modo discador: sem o papel, a ação de discagem continua sendo resolvida para o discador do
 * aparelho. Isso foi medido, não suposto.
 *
 * A chamada é originada pelo gerenciador de telecomunicações, nunca pela ação direta de ligar: num
 * discador que não veio instalado no aparelho, a ação direta é reencaminhada ao discador do sistema
 * para confirmação, o que resulta numa experiência pior e num caminho que este aplicativo não
 * controla. O acabamento visual completo, o pedido de permissão em runtime e o tratamento de erro
 * de discagem são do plano 06-05.
 */
class DialerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SentinelaTheme {
                DialerScreen(onPlaceCall = ::placeCall)
            }
        }
    }

    /**
     * Origina a chamada apenas quando a permissão já está concedida. O pedido em runtime é do
     * plano 06-05: pedir permissão de dentro de um tratador de toque, sem tela que explique o
     * motivo, é exatamente o padrão que este produto não usa.
     */
    private fun placeCall(number: String) {
        val concedida = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
        if (concedida != PackageManager.PERMISSION_GRANTED) return
        val telecom = getSystemService(TelecomManager::class.java) ?: return
        telecom.placeCall(Uri.fromParts("tel", number, null), null)
    }
}

@Composable
private fun DialerScreen(onPlaceCall: (String) -> Unit) {
    var digitos by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            DialerNumberLine(
                digits = digitos,
                onDelete = { digitos = digitos.dropLast(1) },
            )
            DialpadGrid(
                onKeyPressStart = { tecla -> digitos += tecla },
                onKeyPressEnd = { },
                onPlusInserted = { digitos += "+" },
            )
            Spacer(Modifier.height(16.dp))
            CallActionButton(
                icon = Icons.Filled.Call,
                label = stringResource(R.string.dialpad_call),
                contentDescription = stringResource(R.string.dialpad_call_description, digitos),
                colors = callAcceptColors(),
                onClick = { if (digitos.isNotEmpty()) onPlaceCall(digitos) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DialerNumberLine(digits: String, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = digits.ifEmpty { stringResource(R.string.dialpad_title) },
            style = MaterialTheme.typography.numberXl,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (digits.isNotEmpty()) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Backspace,
                    contentDescription = stringResource(R.string.dialpad_delete_description),
                )
            }
        }
    }
}

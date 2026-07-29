package org.sentinela.app.ui.call

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.dialer.DialpadGrid
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberLg

private val SheetTopCornerRadius = 24.dp
private val SheetHorizontalPadding = 16.dp
private val CloseButtonSize = 48.dp
private val DigitLineToGridGap = 24.dp
private val GridBottomSlack = 24.dp

/**
 * Painel de tons ancorado ao rodape, ocupando a altura que a faixa superior deixa livre.
 *
 * **Nao e modal do sistema de proposito.** O cronometro e o botao de encerrar continuam visiveis e
 * clicaveis na faixa superior da tela: quem abriu o teclado para navegar num menu de atendimento
 * eletronico nao pode perder o acesso ao encerrar por causa disso.
 *
 * A grade e a mesma da tela de discagem ([DialpadGrid]), com as duas bordas do gesto propagadas
 * separadamente. Este painel **nao decide nada sobre tom**: o pareamento entre iniciar e encerrar o
 * tom vive no coordenador puro, que garante que nenhum tom fica preso tocando.
 *
 * A linha de digitos enviados nao tem texto de exemplo. Um exemplo ali seria indistinguivel de tom
 * realmente enviado, e o usuario nao tem como conferir o que a operadora recebeu.
 */
@Composable
fun DtmfKeypadSheet(
    sentDigits: String,
    onKeyPressStart: (String) -> Unit,
    onKeyPressEnd: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O gesto de voltar fecha o painel em vez de sair da chamada — a chamada continua em curso.
    BackHandler(enabled = true) { onClose() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(
            topStart = SheetTopCornerRadius,
            topEnd = SheetTopCornerRadius,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Rolagem vertical: numa tela baixa a ultima fileira de teclas ficaria fora do
                // recorte do painel, e tecla inalcancavel e falha de acessibilidade silenciosa —
                // medida por caso de teste, nao imaginada.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SheetHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(CloseButtonSize)) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.call_keypad_close_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SentDigitsLine(sentDigits)
            Spacer(Modifier.height(DigitLineToGridGap))
            DialpadGrid(
                onKeyPressStart = onKeyPressStart,
                onKeyPressEnd = onKeyPressEnd,
            )
            Spacer(Modifier.height(GridBottomSlack))
        }
    }
}

/** Digitos ja enviados, alinhados a direita e com rolagem horizontal. Vazia no inicio. */
@Composable
private fun SentDigitsLine(sentDigits: String) {
    Text(
        text = sentDigits,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        style = MaterialTheme.typography.numberLg,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
    )
}

@Preview(name = "Tons - sem digito enviado", showBackground = true)
@Composable
private fun DtmfKeypadSheetEmptyPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.Bottom) {
                DtmfKeypadSheet(
                    sentDigits = "",
                    onKeyPressStart = {},
                    onKeyPressEnd = {},
                    onClose = {},
                )
            }
        }
    }
}

@Preview(name = "Tons - com digitos enviados", showBackground = true)
@Composable
private fun DtmfKeypadSheetFilledPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(verticalArrangement = Arrangement.Bottom) {
                DtmfKeypadSheet(
                    sentDigits = "1204#",
                    onKeyPressStart = {},
                    onKeyPressEnd = {},
                    onClose = {},
                )
            }
        }
    }
}

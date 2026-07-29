package org.sentinela.app.ui.dialer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

/** Espaco entre teclas. */
private val KeyGap = 8.dp

/** Caractere inserido pelo toque longo na tecla zero. */
const val DIALPAD_PLUS = "+"

/**
 * Uma tecla do teclado, como dado: digito, letras impressas e o recurso de
 * descricao falada.
 */
data class DialpadKeySpec(
    val digit: String,
    val letters: String?,
    @StringRes val descriptionRes: Int,
)

/**
 * Ordem canonica do teclado telefonico. As letras seguem o padrao E.161, e as
 * teclas de servico nao tem letras.
 */
val DialpadLayout: List<List<DialpadKeySpec>> = listOf(
    listOf(
        DialpadKeySpec("1", null, R.string.dialpad_key_1_description),
        DialpadKeySpec("2", "ABC", R.string.dialpad_key_2_description),
        DialpadKeySpec("3", "DEF", R.string.dialpad_key_3_description),
    ),
    listOf(
        DialpadKeySpec("4", "GHI", R.string.dialpad_key_4_description),
        DialpadKeySpec("5", "JKL", R.string.dialpad_key_5_description),
        DialpadKeySpec("6", "MNO", R.string.dialpad_key_6_description),
    ),
    listOf(
        DialpadKeySpec("7", "PQRS", R.string.dialpad_key_7_description),
        DialpadKeySpec("8", "TUV", R.string.dialpad_key_8_description),
        DialpadKeySpec("9", "WXYZ", R.string.dialpad_key_9_description),
    ),
    listOf(
        DialpadKeySpec("*", null, R.string.dialpad_key_star_description),
        DialpadKeySpec("0", DIALPAD_PLUS, R.string.dialpad_key_0_description),
        DialpadKeySpec("#", null, R.string.dialpad_key_hash_description),
    ),
)

/**
 * Grade 3x4 do teclado, compartilhada pela tela de discagem e pelo painel DTMF
 * da chamada ativa.
 *
 * As duas bordas do gesto sao propagadas separadamente ([onKeyPressStart] e
 * [onKeyPressEnd]) porque o tom DTMF precisa acompanhar o dedo. Toque longo na
 * tecla zero insere `+`; toque longo na tecla um nao faz nada nesta versao.
 */
@Composable
fun DialpadGrid(
    onKeyPressStart: (String) -> Unit,
    onKeyPressEnd: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPlusInserted: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DialpadLayout.forEach { fileira ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                fileira.forEach { tecla ->
                    DialpadKey(
                        digit = tecla.digit,
                        letters = tecla.letters,
                        contentDescription = stringResource(tecla.descriptionRes),
                        onPressStart = { onKeyPressStart(tecla.digit) },
                        onPressEnd = { onKeyPressEnd(tecla.digit) },
                        onLongPress = if (tecla.digit == "0") onPlusInserted else null,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DialpadGridPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            DialpadGrid(onKeyPressStart = {}, onKeyPressEnd = {})
        }
    }
}

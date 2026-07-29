package org.sentinela.app.ui.dialer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberXl

/** Espaco entre o numero e a sugestao de nome. */
private val NumberToSuggestionGap = 8.dp

/** Autodimensionamento em degraus: 32sp cheio, 26sp apertado, 20sp piso. */
private const val LIMITE_TAMANHO_CHEIO = 12
private const val LIMITE_TAMANHO_MEDIO = 17
private const val NUMBER_MAX_LINES = 2

/**
 * Formata digitos crus como o usuario digita, delegando ao formatador progressivo da propria
 * biblioteca de telefonia.
 *
 * Nenhuma expressao regular nova e nenhum palpite de formato: quando nao existe padrao que case
 * para a regiao, a biblioteca simplesmente devolve os digitos crus, que e exatamente a conduta que
 * o contrato pede — mostrar o que foi digitado em vez de adivinhar.
 *
 * O formatador da biblioteca guarda estado interno entre digitos, por isso ele e limpo e
 * realimentado a cada composicao: assim o resultado depende somente do valor atual do campo, e
 * apagar um digito nunca deixa resto de formatacao anterior na tela.
 */
fun formatAsYouType(util: PhoneNumberUtil, region: String, digits: String): String {
    if (digits.isEmpty()) return ""
    return numeroCompleto(util, region, digits) ?: parcial(util, region, digits)
}

/**
 * Numero ja completo e valido: vale a forma nacional canonica da biblioteca, que para o Brasil
 * traz os parenteses do DDD — `(11) 91234-5678`. O formatador progressivo, sozinho, para em
 * `11 91234-5678`: ele e otimizado para nao mexer no que o dedo acabou de digitar, e por isso nao
 * fecha parenteses retroativamente. Trocar de forma no instante em que o numero fica valido e o
 * unico jeito de honrar o contrato de design sem escrever formatacao propria.
 */
private fun numeroCompleto(util: PhoneNumberUtil, region: String, digits: String): String? =
    runCatching {
        val parsed = util.parse(digits, region)
        if (util.isValidNumber(parsed)) {
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } else {
            null
        }
    }.getOrNull()

/** Digitacao em curso: o formatador progressivo da propria biblioteca, digito a digito. */
private fun parcial(util: PhoneNumberUtil, region: String, digits: String): String {
    val formatador = util.getAsYouTypeFormatter(region)
    var saida = digits
    runCatching {
        digits.forEach { caractere -> saida = formatador.inputDigit(caractere) }
    }
    return saida
}

/**
 * Campo do numero digitado. **Somente saida:** nao e um campo de texto editavel e nunca abre o
 * teclado do sistema — quem edita e o teclado numerico da propria tela, e um cursor piscando
 * convidaria a um gesto que nao existe aqui.
 *
 * Campo vazio nao mostra **nada**: nem exemplo, nem dica, nem cursor. Um numero de exemplo na tela
 * de discagem e lido como numero de verdade por quem esta com pressa.
 *
 * Esta e a **unica** tela do aplicativo que mostra o numero inteiro, e isso e proposital: aqui o
 * numero e o produto — o usuario acabou de digita-lo. Em todo o resto (historico, aviso, registro)
 * vale a mascara.
 *
 * Para o leitor de tela o campo e uma regiao viva **educada**: anuncia o numero quando ele muda,
 * sem interromper o que estiver sendo falado. A descricao de conteudo separa os digitos por espaco
 * para que o numero seja lido digito a digito, e nao como uma quantidade.
 *
 * @param suggestion nome de contato ou de numero liberado, quando o valor digitado casa com um
 * deles. Nunca inventado: vem de consulta local em memoria e chega nulo quando nao houve casamento.
 */
@Composable
fun NumberDisplay(
    formattedNumber: String,
    modifier: Modifier = Modifier,
    suggestion: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formattedNumber,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = formattedNumber.digitosSeparados()
                    liveRegion = LiveRegionMode.Polite
                },
            style = MaterialTheme.typography.numberXl.copy(
                fontSize = tamanhoDoNumero(formattedNumber.length),
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = NUMBER_MAX_LINES,
        )
        if (suggestion != null) {
            Text(
                text = suggestion,
                modifier = Modifier.padding(top = NumberToSuggestionGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Degraus em vez de medicao continua: o resultado e deterministico, identico em qualquer
 * densidade de tela e testavel sem renderizar. Piso de 20sp, nunca menor.
 */
private fun tamanhoDoNumero(comprimento: Int) = when {
    comprimento <= LIMITE_TAMANHO_CHEIO -> 32.sp
    comprimento <= LIMITE_TAMANHO_MEDIO -> 26.sp
    else -> 20.sp
}

/**
 * Leitor de tela le "onze" para `11`; separado por espacos ele le "um um". Numero de telefone e
 * sequencia de digitos, nao quantidade.
 */
private fun String.digitosSeparados(): String = toCharArray().joinToString(" ")

@Preview(widthDp = 360)
@Composable
private fun NumberDisplayEmptyPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface { NumberDisplay(formattedNumber = "") }
    }
}

@Preview(widthDp = 360)
@Composable
private fun NumberDisplayWithSuggestionPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            NumberDisplay(formattedNumber = "(11) 91234-5678", suggestion = "Maria")
        }
    }
}

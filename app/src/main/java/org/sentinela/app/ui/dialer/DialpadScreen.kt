package org.sentinela.app.ui.dialer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.PlaceCallResult
import org.sentinela.app.ui.call.CallActionButton
import org.sentinela.app.ui.call.callAcceptColors
import org.sentinela.app.ui.theme.SentinelaTheme

private val TopBarHeight = 64.dp
private val ScreenPadding = 16.dp
private val NumberToGridGap = 24.dp
private val GridToActionsGap = 24.dp
private val ActionRowBottomGap = 32.dp
private val IconToTitleGap = 8.dp
private val TopBarIconSize = 24.dp

/** Diametro do botao de apagar. Ele ocupa este espaco mesmo invisivel. */
val DialpadSecondaryDiameter = 56.dp

/** Opacidade do botao de ligar enquanto o campo esta vazio (regra do Material para desabilitado). */
private const val DISABLED_ALPHA = 0.38f

/**
 * Tela de discagem.
 *
 * Ela existe por dois motivos ao mesmo tempo, e o segundo costuma surpreender: um telefone padrao
 * precisa oferecer teclado de discagem, **e** o sistema so aceita este aplicativo como telefone
 * padrao se essa tela existir de verdade e estiver declarada como alvo da acao de discagem. Sem
 * ela, o pedido do papel falha — foi medido, nao suposto.
 *
 * A tela tambem funciona sem o papel: quem abre o aplicativo e disca continua discando. O que muda e
 * que, sem o papel, a acao de discagem do sistema nao e resolvida para nos. Nao existe guarda aqui
 * contra "ser sequestrado" porque nao ha nada a guardar; o que a tela precisa e nao travar e nao
 * prometer o que nao controla.
 *
 * Falha ao originar **nunca** apaga o numero: o usuario acabou de digita-lo, e limpar o campo em
 * cima de um erro nosso o obrigaria a digitar tudo de novo.
 *
 * @param placeCall devolve o resultado de forma sincrona; a tela so decide o que mostrar.
 * @param suggestionFor consulta local, em memoria, do nome de contato ou de numero liberado. Nunca
 * inventa nome: devolve nulo quando nao houve casamento.
 */
@Composable
fun DialpadScreen(
    placeCall: (String) -> PlaceCallResult,
    modifier: Modifier = Modifier,
    initialNumber: String = "",
    formatNumber: (String) -> String = { digitos -> digitos },
    suggestionFor: (String) -> String? = { null },
) {
    // `rememberSaveable` com [initialNumber] de chave resolve dois defeitos de uma vez, e os dois
    // são visíveis: sem persistência, girar o aparelho apagava o número já digitado; sem a chave,
    // uma segunda ação de discagem (a tela é `singleTop`, então ela é reaproveitada) mantinha o
    // número antigo no campo e ignorava o novo em silêncio.
    var digitos by rememberSaveable(initialNumber) { mutableStateOf(initialNumber) }
    var falhas by rememberSaveable { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val mensagemDeFalha = stringResource(R.string.dialpad_error_failed)
    val acaoDeFalha = stringResource(R.string.dialpad_error_retry)

    LaunchedEffect(falhas) {
        if (falhas == 0) return@LaunchedEffect
        val resultado = snackbar.showSnackbar(
            message = mensagemDeFalha,
            actionLabel = acaoDeFalha,
            duration = SnackbarDuration.Short,
        )
        if (resultado == SnackbarResult.ActionPerformed && placeCall(digitos) != PlaceCallResult.Placed) {
            falhas++
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insetsDoScaffold ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetsDoScaffold)
                .safeDrawingPadding()
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialpadTopBar()
            Spacer(Modifier.weight(1f))
            NumberDisplay(
                formattedNumber = formatNumber(digitos),
                suggestion = suggestionFor(digitos),
            )
            Spacer(Modifier.height(NumberToGridGap))
            DialpadGrid(
                onKeyPressStart = { tecla -> digitos += tecla },
                onKeyPressEnd = { },
                onPlusInserted = { digitos += DIALPAD_PLUS },
            )
            Spacer(Modifier.height(GridToActionsGap))
            DialpadActionRow(
                enabled = digitos.isNotEmpty(),
                numberForSpeech = formatNumber(digitos),
                onCall = { if (placeCall(digitos) != PlaceCallResult.Placed) falhas++ },
                onDelete = { digitos = digitos.dropLast(1) },
                onClear = { digitos = "" },
            )
            Spacer(Modifier.height(ActionRowBottomGap))
        }
    }
}

@Composable
private fun DialpadTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(TopBarIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(start = IconToTitleGap),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Linha de acao: um vao, o botao de ligar e o de apagar.
 *
 * O vao da esquerda e o botao de apagar tem o mesmo diametro de proposito. Com o campo vazio o
 * apagar fica **invisivel mas presente**, para que a grade e o botao de ligar nao pulem de lugar
 * quando o primeiro digito aparecer — botao que se move sob o dedo faz o usuario errar a tecla.
 * Invisivel, ele tambem sai da arvore de acessibilidade: um controle que nao faz nada nao deve ser
 * anunciado.
 */
@Composable
private fun DialpadActionRow(
    enabled: Boolean,
    numberForSpeech: String,
    onCall: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(DialpadSecondaryDiameter))
        val descricaoDoLigar =
            stringResource(R.string.dialpad_call_description, numberForSpeech)
        Box(
            // `clearAndSetSemantics` e a unica forma de o estado desabilitado valer para o leitor
            // de tela sem editar o componente compartilhado do botao de acao: ele define UM no
            // com descricao, papel e estado, em vez de deixar o no interno do botao responder por
            // um estado que ele nao conhece. A acao de clique e redeclarada aqui para nao
            // desaparecer da arvore de acessibilidade.
            modifier = Modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clearAndSetSemantics {
                    contentDescription = descricaoDoLigar
                    role = Role.Button
                    if (enabled) {
                        onClick {
                            onCall()
                            true
                        }
                    } else {
                        disabled()
                    }
                },
        ) {
            CallActionButton(
                icon = Icons.Filled.Call,
                label = stringResource(R.string.dialpad_call),
                contentDescription = descricaoDoLigar,
                colors = callAcceptColors(),
                onClick = { if (enabled) onCall() },
            )
        }
        DeleteKey(visible = enabled, onDelete = onDelete, onClear = onClear)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeleteKey(visible: Boolean, onDelete: () -> Unit, onClear: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    if (!visible) {
        // Ocupa o espaco e desaparece da arvore de acessibilidade no mesmo gesto.
        Spacer(
            Modifier
                .size(DialpadSecondaryDiameter)
                .clearAndSetSemantics { },
        )
        return
    }
    Box(
        modifier = Modifier
            .size(DialpadSecondaryDiameter)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.dialpad_delete_description),
                onLongClickLabel = stringResource(R.string.dialpad_clear_description),
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClear()
                },
                onClick = onDelete,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Backspace,
            contentDescription = stringResource(R.string.dialpad_delete_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DialpadScreenEmptyPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            DialpadScreen(placeCall = { PlaceCallResult.Placed })
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DialpadScreenPartialPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        DialpadScreen(initialNumber = "1191", placeCall = { PlaceCallResult.Placed })
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DialpadScreenFormattedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        DialpadScreen(
            initialNumber = "11912345678",
            formatNumber = { PREVIEW_FORMATTED },
            placeCall = { PlaceCallResult.Placed },
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DialpadScreenSuggestionPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        DialpadScreen(
            initialNumber = "11912345678",
            formatNumber = { PREVIEW_FORMATTED },
            suggestionFor = { PREVIEW_SUGGESTION },
            placeCall = { PlaceCallResult.Placed },
        )
    }
}

@Preview(widthDp = 360, heightDp = 900, fontScale = 2f)
@Composable
private fun DialpadScreenLargeFontPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        DialpadScreen(
            initialNumber = "11912345678",
            formatNumber = { PREVIEW_FORMATTED },
            placeCall = { PlaceCallResult.Placed },
        )
    }
}

private const val PREVIEW_FORMATTED = "(11) 91234-5678"
private const val PREVIEW_SUGGESTION = "Maria"

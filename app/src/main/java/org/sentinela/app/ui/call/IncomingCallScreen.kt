package org.sentinela.app.ui.call

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.ui.components.SentinelaWatermark
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.telecom.call.CallOrigin as DomainCallOrigin

private val ScreenHorizontalMargin = 16.dp
private val TopSlack = 48.dp
private val WatermarkToStateGap = 48.dp
private val StateToIdentityGap = 24.dp
private val BottomSlack = 64.dp
private val LandscapeColumnGap = 24.dp

/**
 * Ordem de foco do leitor de tela, declarada e nao herdada da arvore.
 *
 * A ordem importa porque o layout coloca as acoes no rodape com peso, e a travessia geometrica
 * poderia alcancar um botao antes de o usuario ter ouvido quem esta ligando. Recusar vem antes de
 * atender porque e a ordem visual, e inverter os dois entre voz e tela seria a receita de recusar
 * uma chamada querendo atende-la.
 */
private const val TRAVERSAL_WATERMARK = 0f
private const val TRAVERSAL_STATE = 1f
private const val TRAVERSAL_IDENTITY = 2f
private const val TRAVERSAL_ACTIONS = 3f

/**
 * Rotulo do estado da chamada, como regiao viva educada.
 *
 * Educada e nao assertiva de proposito: a transicao recebida -> ativa -> encerrada precisa ser
 * anunciada, mas nunca roubando o foco de quem esta com o dedo sobre atender ou recusar.
 */
@Composable
internal fun CallStateLabel(labelRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(labelRes),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Chamada recebida em tela cheia.
 *
 * Tela de maior risco do aplicativo: o erro possivel aqui — recusar querendo atender — nao tem
 * desfazer. Por isso as duas acoes tem 72dp, ficam separadas pela folga contratada, tem icone e
 * rotulo distintos, e a tela **nao expoe nenhum controle de politica**. O chip de origem e
 * passivo: informa por que esta tocando e nada mais. Mudar politica com o telefone tocando seria
 * decisao sob pressao, e qualquer alvo tocavel perto de atender/recusar aumenta a chance de erro.
 *
 * A marca d'agua explica por que a tela de chamada mudou de aparencia, e nao e focavel.
 */
@Composable
fun IncomingCallScreen(
    identity: CallIdentity,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    photo: ImageBitmap? = null,
    landscape: Boolean = false,
    stateLabelRes: Int = R.string.call_incoming_state,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = ScreenHorizontalMargin)
                .semantics { isTraversalGroup = true },
        ) {
            if (landscape) {
                IncomingLandscape(identity, photo, stateLabelRes, onAnswer, onReject)
            } else {
                IncomingPortrait(identity, photo, stateLabelRes, onAnswer, onReject)
            }
        }
    }
}

@Composable
private fun IncomingPortrait(
    identity: CallIdentity,
    photo: ImageBitmap?,
    stateLabelRes: Int,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(TopSlack))
        SentinelaWatermark(modifier = Modifier.semantics { traversalIndex = TRAVERSAL_WATERMARK })
        Spacer(Modifier.height(WatermarkToStateGap))
        CallStateLabel(
            labelRes = stateLabelRes,
            modifier = Modifier.semantics { traversalIndex = TRAVERSAL_STATE },
        )
        Spacer(Modifier.height(StateToIdentityGap))
        CallerIdentity(
            identity = identity,
            photo = photo,
            modifier = Modifier.semantics { traversalIndex = TRAVERSAL_IDENTITY },
        )
        Spacer(Modifier.weight(1f))
        AnswerRejectBar(
            onAnswer = onAnswer,
            onReject = onReject,
            modifier = Modifier.semantics { traversalIndex = TRAVERSAL_ACTIONS },
        )
        Spacer(Modifier.height(BottomSlack))
    }
}

/**
 * Paisagem: identidade a esquerda, acoes a direita.
 *
 * Empilhar tudo em coluna nesta orientacao cortaria os botoes de acao fora da tela, que e a unica
 * falha de layout desta tela capaz de deixar o usuario sem como atender nem recusar.
 */
@Composable
private fun IncomingLandscape(
    identity: CallIdentity,
    photo: ImageBitmap?,
    stateLabelRes: Int,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(LandscapeColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StateToIdentityGap),
        ) {
            SentinelaWatermark(
                modifier = Modifier.semantics { traversalIndex = TRAVERSAL_WATERMARK },
            )
            CallStateLabel(
                labelRes = stateLabelRes,
                modifier = Modifier.semantics { traversalIndex = TRAVERSAL_STATE },
            )
            CallerIdentity(
                identity = identity,
                photo = photo,
                modifier = Modifier.semantics { traversalIndex = TRAVERSAL_IDENTITY },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics { traversalIndex = TRAVERSAL_ACTIONS },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnswerRejectBar(
                onAnswer = onAnswer,
                onReject = onReject,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val contatoComNome = CallIdentity(
    displayName = "Ana Paula Souza",
    fullNumber = "+5511912345678",
    origin = DomainCallOrigin.CONTATO,
)

@Preview(name = "Recebida - contato", showBackground = true)
@Composable
private fun IncomingContactPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(identity = contatoComNome, onAnswer = {}, onReject = {})
    }
}

@Preview(name = "Recebida - permitido", showBackground = true)
@Composable
private fun IncomingWhitelistPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(
            identity = CallIdentity(
                displayName = "Escola do Pedro",
                fullNumber = "+551133334444",
                origin = DomainCallOrigin.PERMITIDO,
            ),
            onAnswer = {},
            onReject = {},
        )
    }
}

@Preview(name = "Recebida - desconhecido", showBackground = true)
@Composable
private fun IncomingUnknownPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(
            identity = CallIdentity(
                fullNumber = "+5511912345678",
                origin = DomainCallOrigin.DESCONHECIDO,
            ),
            onAnswer = {},
            onReject = {},
        )
    }
}

@Preview(name = "Recebida - privado", showBackground = true)
@Composable
private fun IncomingPrivatePreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(
            identity = CallIdentity(origin = DomainCallOrigin.PRIVADO),
            onAnswer = {},
            onReject = {},
        )
    }
}

@Preview(name = "Recebida - fonte 200%", fontScale = 2f, showBackground = true)
@Composable
private fun IncomingLargeFontPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(identity = contatoComNome, onAnswer = {}, onReject = {})
    }
}

@Preview(name = "Recebida - paisagem", widthDp = 720, heightDp = 360, showBackground = true)
@Composable
private fun IncomingLandscapePreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        IncomingCallScreen(
            identity = contatoComNome,
            onAnswer = {},
            onReject = {},
            landscape = true,
        )
    }
}

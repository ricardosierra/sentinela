package org.sentinela.app.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Distancia minima entre recusar e atender.
 *
 * Nao e espaco decorativo: errar recusar por atender e irreversivel, e nenhuma acao destrutiva
 * desta tela pode ficar a menos disso de outra acao.
 */
val AnswerRejectMinGap = 24.dp

/**
 * Linha de acoes da chamada recebida: recusar a esquerda, atender a direita.
 *
 * Cada botao tem o diametro primario do contrato — `CallActionDiameterPrimary`, que vale 72.dp —,
 * cor funcional fixa vinda por parametro, icone distinto, rotulo textual sob o circulo e descricao
 * propria: o estado nunca e comunicado so por cor.
 *
 * A separacao usa `SpaceBetween` mais uma folga minima declarada em [AnswerRejectMinGap]. Na
 * largura minima suportada pelo aplicativo os dois circulos somam 144dp, entao a sobra distribuida
 * fica sempre acima da folga contratada; a folga explicita existe para que reduzir a largura no
 * futuro nao encoste os dois botoes em silencio.
 *
 * **Nao existe gesto de arrastar para atender nesta versao, e isso e decisao, nao falta.** Um
 * gesto sem indicacao visual clara e pior que dois botoes grandes: o usuario descobre a mecanica
 * errando, e o erro aqui derruba uma chamada de verdade. Alem disso, arrastar simplesmente nao
 * existe para quem usa leitor de tela, que precisa de dois alvos anunciaveis e distintos.
 *
 * O toque dispara na LIBERACAO dentro do alvo, comportamento que [CallActionButton] garante: e o
 * que evita atender por encostar no aparelho ao tira-lo do bolso.
 */
@Composable
fun AnswerRejectBar(
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        CallActionButton(
            icon = Icons.Filled.CallEnd,
            label = stringResource(R.string.call_action_reject),
            contentDescription = stringResource(R.string.call_action_reject_description),
            colors = callRejectColors(),
            onClick = onReject,
            diameter = CallActionDiameterPrimary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Folga minima explicita entre as duas acoes, alem da sobra que SpaceBetween distribui.
        Spacer(modifier = Modifier.widthIn(min = AnswerRejectMinGap))
        CallActionButton(
            icon = Icons.Filled.Call,
            label = stringResource(R.string.call_action_answer),
            contentDescription = stringResource(R.string.call_action_answer_description),
            colors = callAcceptColors(),
            onClick = onAnswer,
            diameter = CallActionDiameterPrimary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun AnswerRejectBarPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface { AnswerRejectBar(onAnswer = {}, onReject = {}) }
    }
}

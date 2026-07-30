package org.sentinela.app.ui.home

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.OnStatusAttention
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeLarge
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.StatusAttention

private val CardPadding = 24.dp
private val TitleToStatusGap = 4.dp
private val StatusDotSize = 12.dp
private val DotToLabelGap = 8.dp
private val StripTopGap = 16.dp
private val StripPadding = 16.dp
private val StripIconSize = 20.dp
private val StripIconGap = 12.dp
private val SwitchMinTarget = 48.dp
private const val STRIP_ALPHA = 0.10f
private const val PULSE_MIN_ALPHA = 0.35f
private const val PULSE_MILLIS = 900

/**
 * Cartao principal da home: o estado da protecao e o interruptor que a alterna.
 *
 * **O interruptor alterna a PREFERENCIA de protecao, nunca o papel do sistema.** O motivo e
 * concreto e foi medido tres vezes na Fase 6: perder um papel do sistema encerra o processo do
 * aplicativo. Um interruptor que revogasse o papel faria o aplicativo fechar sozinho no instante em
 * que o usuario o desligasse — e o aplicativo nem sequer pode revogar um papel por conta propria. O
 * papel e estado SOMENTE-LEITURA, consultado vivo e mostrado no aviso separado, com o botao de
 * correcao que abre o seletor do sistema. Assim o interruptor nunca fica em desacordo com o mundo
 * real: o papel se consulta, a preferencia se alterna.
 *
 * As duas cores de significado sao LITERAIS e nao saem do esquema. A partir do nivel 31 o esquema
 * inteiro pode vir do papel de parede, e um papel de parede alaranjado deixaria protegido e
 * desprotegido visualmente indistinguiveis — o mesmo argumento que a Fase 6 usou para atender e
 * recusar.
 *
 * **Semantica.** O interruptor e no PROPRIO, com papel de interruptor e descricao de estado, e o
 * cartao nao mescla descendentes. A armadilha ja foi medida nas duas direcoes na Fase 7: envolver o
 * controle nao derruba nada por si, o que derruba e DECLARAR o estado no container — o no do proprio
 * controle e fronteira de mesclagem e continua respondendo as buscas, entao o estado mora sempre no
 * no do controle. Aqui nao existe declaracao de estado fora do interruptor.
 */
@Composable
fun StatusHeroCard(
    protectionEnabled: Boolean,
    onProtectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fundo = if (protectionEnabled) MaterialTheme.colorScheme.primaryContainer else StatusAttention
    val conteudo =
        if (protectionEnabled) MaterialTheme.colorScheme.onPrimaryContainer else OnStatusAttention
    val descricao = stringResource(
        if (protectionEnabled) R.string.dashboard_protection_active
        else R.string.dashboard_protection_inactive,
    )
    val estadoDoInterruptor = stringResource(
        if (protectionEnabled) R.string.state_on else R.string.state_off,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        color = fundo,
        contentColor = conteudo,
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = descricao },
                ) {
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.padding(top = TitleToStatusGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PontoDeEstado(ativo = protectionEnabled)
                        Text(
                            text = stringResource(
                                if (protectionEnabled) R.string.dashboard_monitoring
                                else R.string.dashboard_protection_off_hint,
                            ),
                            modifier = Modifier.padding(start = DotToLabelGap),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Switch(
                    checked = protectionEnabled,
                    onCheckedChange = onProtectionChange,
                    modifier = Modifier
                        .requiredSizeIn(minWidth = SwitchMinTarget, minHeight = SwitchMinTarget)
                        .semantics { stateDescription = estadoDoInterruptor },
                )
            }
            FaixaDeReforco(conteudo = conteudo)
        }
    }
}

/** Faixa interna do cartao: reforca em uma frase o que a protecao faz de fato. */
@Composable
private fun FaixaDeReforco(conteudo: Color) {
    Surface(
        modifier = Modifier
            .padding(top = StripTopGap)
            .fillMaxWidth(),
        shape = ShapeMedium,
        color = conteudo.copy(alpha = STRIP_ALPHA),
        contentColor = conteudo,
    ) {
        Row(
            modifier = Modifier.padding(StripPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(StripIconSize),
            )
            Text(
                text = stringResource(R.string.dashboard_device_safe),
                modifier = Modifier.padding(start = StripIconGap),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Ponto de estado, decorativo. A pulsacao e suprimida quando o aparelho pede menos animacao, e o
 * ponto e limpo da arvore semantica: a informacao ja esta no titulo e no rotulo ao lado.
 */
@Composable
private fun PontoDeEstado(ativo: Boolean) {
    val reduzirMovimento = movimentoReduzido()
    val cor = if (ativo) CallAccept else MaterialTheme.colorScheme.outline
    val opacidade = if (ativo && !reduzirMovimento) {
        val transicao = rememberInfiniteTransition(label = "pulso")
        transicao.animateFloat(
            initialValue = 1f,
            targetValue = PULSE_MIN_ALPHA,
            animationSpec = infiniteRepeatable(
                animation = tween(PULSE_MILLIS),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulsoAlpha",
        ).value
    } else {
        1f
    }
    Box(
        modifier = Modifier
            .size(StatusDotSize)
            .alpha(opacidade)
            .background(color = cor, shape = CircleShape)
            .clearAndSetSemantics {},
    )
}

@Composable
private fun movimentoReduzido(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

@Preview(widthDp = 411)
@Composable
private fun StatusHeroCardOnPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(CardPadding),
            ) {
                StatusHeroCard(protectionEnabled = true, onProtectionChange = {})
                StatusHeroCard(protectionEnabled = false, onProtectionChange = {})
            }
        }
    }
}

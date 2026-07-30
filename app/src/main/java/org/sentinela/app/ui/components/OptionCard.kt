package org.sentinela.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.call.rememberMotionReduced
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapePill

private val CardMinHeight = 72.dp
private val CardPadding = 16.dp
private val IconContainerSize = 40.dp
private val IconSize = 20.dp
private val IconToTextGap = 16.dp
private val TrailingIconSize = 24.dp
private val TitleToBadgeGap = 8.dp
private val TitleToDescriptionGap = 4.dp
private val BadgeHorizontalPadding = 8.dp
private val BadgeVerticalPadding = 2.dp
private val UnselectedBorderWidth = 1.dp
private val SelectedBorderWidth = 2.dp
private const val PRESSED_SCALE = 0.98f
private const val PRESS_ANIMATION_MILLIS = 100

/**
 * Cartao de opcao: o padrao unico de escolha desta fase (numeros desconhecidos,
 * contatos da agenda e whitelist pessoal).
 *
 * Nenhum texto nasce aqui dentro. Titulo, descricao, selo e motivo de
 * indisponibilidade chegam por parametro, sempre resolvidos por recurso na tela
 * que chama — e o que mantem a varredura de honestidade da copy como unica dona
 * do texto.
 *
 * ## Semantica, e e aqui que a fase ganha ou perde acessibilidade
 *
 * A linha INTEIRA e um alvo unico, com papel de botao de radio. Duas
 * consequencias sao obrigatorias, e as duas foram medidas na Fase 6:
 *
 * (a) **Controle interativo nunca pode ser filho deste cartao**, porque o cartao
 * E o controle. Um botao ou interruptor colocado aqui dentro fica inalcancavel
 * pelo leitor de tela: o no do cartao mescla os descendentes e responde por
 * todos eles.
 *
 * (b) **Declarar o estado desabilitado envolvendo o componente com semantica de
 * mesclagem NAO funciona.** O no interno do cartao ja mescla, e e ele quem
 * responde as buscas dos testes e do leitor de tela; o estado colocado num
 * ancestral fica onde ninguem consulta. Foi assim que um controle desenhado com
 * a opacidade de desabilitado continuou sendo anunciado como habilitado. Por
 * isso o desabilitado vai no modificador do PROPRIO no do cartao, e o motivo
 * vai como descricao de estado no mesmo no.
 *
 * O icone de confirmacao e decorativo: quem anuncia a selecao e o papel de botao
 * de radio, nunca o icone. No estado nao selecionado ele OCUPA espaco e fica
 * invisivel, para o layout nao pular no instante da escolha.
 */
@Composable
fun OptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: Color? = null,
    iconTint: Color? = null,
    badge: String? = null,
    enabled: Boolean = true,
    unavailableReason: String? = null,
) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val movimentoReduzido = rememberMotionReduced()
    val escala by animateFloatAsState(
        targetValue = if (pressionado && !movimentoReduzido) PRESSED_SCALE else 1f,
        animationSpec = tween(durationMillis = PRESS_ANIMATION_MILLIS),
        label = "escala-do-cartao",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .selectable(
                selected = selected,
                interactionSource = interacao,
                indication = ripple(),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                if (unavailableReason != null) stateDescription = unavailableReason
            },
        shape = ShapeMedium,
        color = if (selected) cores.surfaceContainerHigh else cores.surfaceContainerLow,
        contentColor = cores.onSurface,
        border = BorderStroke(
            width = if (selected) SelectedBorderWidth else UnselectedBorderWidth,
            color = if (selected) cores.primary else cores.outlineVariant,
        ),
    ) {
        ConteudoDoCartao(
            title = title,
            description = description,
            icon = icon,
            selected = selected,
            iconContainerColor = iconContainerColor,
            iconTint = iconTint,
            badge = badge,
        )
    }
}

@Composable
private fun ConteudoDoCartao(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    iconContainerColor: Color?,
    iconTint: Color?,
    badge: String?,
) {
    Row(
        modifier = Modifier
            .heightIn(min = CardMinHeight)
            .padding(CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeDaOpcao(
            icon = icon,
            selected = selected,
            containerColor = iconContainerColor,
            tint = iconTint,
        )
        TextoDaOpcao(
            title = title,
            description = description,
            badge = badge,
            modifier = Modifier
                .padding(start = IconToTextGap)
                .weight(1f),
        )
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(TrailingIconSize)
                .alpha(if (selected) 1f else 0f),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun IconeDaOpcao(
    icon: ImageVector,
    selected: Boolean,
    containerColor: Color?,
    tint: Color?,
) {
    val cores = MaterialTheme.colorScheme
    val fundo = containerColor
        ?: if (selected) cores.secondaryContainer else cores.surfaceContainerHighest
    val cor = tint
        ?: if (selected) cores.onSecondaryContainer else cores.onSurfaceVariant
    Box(
        modifier = Modifier.size(IconContainerSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = fundo, modifier = Modifier.size(IconContainerSize)) {}
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = cor,
        )
    }
}

@Composable
private fun TextoDaOpcao(
    title: String,
    description: String,
    badge: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (badge != null) {
                Surface(
                    modifier = Modifier.padding(start = TitleToBadgeGap),
                    shape = ShapePill,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(
                            horizontal = BadgeHorizontalPadding,
                            vertical = BadgeVerticalPadding,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Text(
            text = description,
            modifier = Modifier.padding(top = TitleToDescriptionGap),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Modificador do grupo de cartoes de opcao: declara ao leitor de tela que os
 * cartoes reunidos ali sao alternativas de uma escolha unica.
 */
fun Modifier.optionCardGroup(): Modifier = this.selectableGroup()

@Preview(widthDp = 360)
@Composable
private fun OptionCardPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier
                    .padding(CardPadding)
                    .optionCardGroup(),
                verticalArrangement = Arrangement.spacedBy(CardPadding),
            ) {
                OptionCard(
                    title = stringResource(R.string.unknown_option_block),
                    description = stringResource(R.string.unknown_option_block_desc),
                    icon = Icons.Outlined.Block,
                    selected = true,
                    onClick = {},
                )
                OptionCard(
                    title = stringResource(R.string.unknown_option_silence),
                    description = stringResource(R.string.unknown_option_silence_desc),
                    icon = Icons.Outlined.NotificationsOff,
                    selected = false,
                    onClick = {},
                    badge = stringResource(R.string.contacts_default_badge),
                )
                OptionCard(
                    title = stringResource(R.string.contacts_option_ring),
                    description = stringResource(R.string.contacts_option_ring_desc),
                    icon = Icons.Outlined.Phone,
                    selected = false,
                    onClick = {},
                    enabled = false,
                    unavailableReason = stringResource(R.string.contacts_permission_denied),
                )
            }
        }
    }
}

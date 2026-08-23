package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.sentinela.app.ui.components.OptionCard
import org.sentinela.app.ui.components.optionCardGroup
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * As peças que TODA seção da tela Proteção reusa: as medidas do contrato de interface e os três
 * envoltórios de apresentação.
 *
 * Elas viviam no topo de `SettingsScreen.kt` enquanto a tela inteira era um arquivo só. Quando cada
 * seção virou arquivo próprio, deixá-las lá obrigaria cada seção a conhecer o arquivo da tela — e
 * copiá-las para cada seção criaria dez versões da mesma medida, que é exatamente como um espaçamento
 * passa a divergir entre dois grupos sem ninguém perceber.
 */
internal val ScreenHorizontalPadding = 16.dp
internal val GroupGap = 24.dp
internal val OptionGap = 16.dp
internal val BottomGap = 32.dp
internal val DestructiveMinTarget = 48.dp

/**
 * Grupo de alternativas de escolha única. Usa o modificador compartilhado de 07-03 — reescrever o
 * declarador de grupo selecionável aqui seria a duplicação que aquele plano proibiu.
 */
@Composable
internal fun EscolhaUnica(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.optionCardGroup(),
        verticalArrangement = Arrangement.spacedBy(OptionGap),
        content = content,
    )
}

@Composable
internal fun OpcaoDePolitica(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OptionCard(
        title = title,
        description = description,
        icon = icon,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
internal fun NotaDoGrupo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Moldura das pré-visualizações de seção: tema escuro sem cor dinâmica e o mesmo respiro lateral que
 * a tela dá a cada grupo.
 *
 * Existe para que a pré-visualização de uma seção mostre a seção no contexto em que ela é desenhada,
 * e não sobre fundo transparente com as cores erradas. Repetir o bloco de tema em cada uma das dez
 * seções seria dez lugares para esquecer de atualizar quando o tema mudar.
 */
@Composable
internal fun SecaoDeExemplo(content: @Composable () -> Unit) {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(ScreenHorizontalPadding)) { content() }
        }
    }
}

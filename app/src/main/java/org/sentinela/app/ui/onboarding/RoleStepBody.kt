package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.components.HonestyCard
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium

private val ScreenPadding = 16.dp
private val TitleToIntroGap = 16.dp
private val IntroToStripGap = 24.dp
private val StripToHonestyGap = 24.dp
private val StripHeight = 80.dp
private val StripIconSize = 16.dp
private val StripIconToTextGap = 8.dp
private val BodyBottomGap = 32.dp

/** Largura do corpo em relacao a tela, conforme o contrato de design. */
private const val INTRO_WIDTH_FRACTION = 0.85f

/** Alfa do container de destaque leve da faixa de contexto. */
private const val CONTAINER_ALPHA = 0.20f

@Composable
internal fun IntroDoPapel() {
    Text(
        text = stringResource(R.string.onboarding_role_intro),
        modifier = Modifier
            .padding(top = TitleToIntroGap)
            .fillMaxWidth(INTRO_WIDTH_FRACTION),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Faixa de contexto: o gradiente tonal que substitui a imagem remota do mockup. */
@Composable
internal fun FaixaDeContextoDoPapel() {
    val cores = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .padding(top = IntroToStripGap)
            .fillMaxWidth(),
        shape = ShapeMedium,
        color = cores.surfaceContainerLow,
        contentColor = cores.onSurface,
    ) {
        Row(
            modifier = Modifier
                .height(StripHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            cores.primaryContainer.copy(alpha = CONTAINER_ALPHA),
                        ),
                    ),
                )
                .padding(horizontal = ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(StripIconSize),
                tint = cores.primary,
            )
            Text(
                text = stringResource(R.string.welcome_badge_native),
                modifier = Modifier.padding(start = StripIconToTextGap),
                style = MaterialTheme.typography.labelMedium,
                color = cores.primary,
            )
        }
    }
}

/**
 * O aviso obrigatorio de escopo.
 *
 * As tres frases chegam por identificador de recurso, na ordem do contrato de design. Nenhuma delas
 * e reescrita aqui: elas sao a traducao das medicoes das Fases 5 e 6, e reescreve-las e o caminho de
 * volta a promessa falsa.
 */
@Composable
internal fun CartaoDeEscopoDoPapel() {
    HonestyCard(
        title = stringResource(R.string.onboarding_scope_title),
        items = listOf(
            stringResource(R.string.dialer_activation_unchanged_3),
            stringResource(R.string.onboarding_scope_dnd),
            stringResource(R.string.settings_hide_native_log_desc),
        ),
        itemIcon = Icons.Outlined.Info,
        modifier = Modifier.padding(top = StripToHonestyGap, bottom = BodyBottomGap),
        itemIconTint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** O corpo do passo: a introducao, a faixa de contexto e o aviso obrigatorio de escopo. */
@Preview(widthDp = 411, heightDp = 500)
@Composable
private fun CorpoDoPassoDoPapelPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IntroDoPapel()
                FaixaDeContextoDoPapel()
                CartaoDeEscopoDoPapel()
            }
        }
    }
}

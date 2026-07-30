// Supressao LOCAL, no molde de DialerActivationScreen.kt, em vez de afrouxar o detekt.yml
// compartilhado: a contagem alta aqui e consequencia do proprio contrato de design — cada bloco da
// tela vira um composable pequeno e nomeado, e as duas pre-visualizacoes exigidas somam mais duas
// funcoes. Juntar blocos para caber no limite deixaria a tela pior de ler.
@file:Suppress("TooManyFunctions")

package org.sentinela.app.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sentinela.app.R
import org.sentinela.app.ui.call.rememberMotionReduced
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.ShapePill

private val ScreenPadding = 16.dp
private val TopBarToHeroGap = 32.dp
private val HeroCircleSize = 128.dp
private val HeroBorderWidth = 2.dp
private val HeroIconSize = 64.dp
private val HeroToTitleGap = 32.dp
private val TitleToSubtitleGap = 16.dp
private val SubtitleToCardsGap = 48.dp
private val CardsToBadgeGap = 24.dp
private val CardGap = 16.dp
private val CardPadding = 16.dp
private val CardBorderWidth = 1.dp
private val CardIconContainerSize = 40.dp
private val CardIconSize = 20.dp
private val CardIconToTitleGap = 12.dp
private val CardTitleToDescGap = 4.dp
private val TitleMaxWidth = 280.dp
private val SubtitleMaxWidth = 320.dp
private val BadgeIconSize = 16.dp
private val BadgeIconToTextGap = 8.dp
private val BadgeHorizontalPadding = 12.dp
private val BadgeVerticalPadding = 8.dp
private val CtaGradientHeight = 32.dp
private val CtaHeight = 56.dp
private val CtaIconGap = 8.dp
private val CtaToHintGap = 16.dp
private val FooterBottomGap = 32.dp
private val HeroFloatAmplitude = 4.dp

private val TitleFontSize = 28.sp
private val TitleLineHeight = 36.sp

/** Alfa da borda do hero e do fundo tonal que substitui a imagem remota do mockup. */
private const val HERO_BORDER_ALPHA = 0.30f
private const val TONAL_SURFACE_ALPHA = 0.20f

/** Ciclo da flutuacao decorativa do hero, em milissegundos (mockup: 6 s). */
private const val FLOAT_CYCLE_MILLIS = 6_000

/**
 * Boas-vindas — tela 0 do fluxo, antes do passo 1 de 6.
 *
 * Composta **pura**: nenhum container, nenhum dono de estado e nenhuma leitura de repositorio
 * vivem aqui. O que a tela sabe fazer e chamar [onStart] e [onAbout]; quem os liga e a rota.
 *
 * ## Tres adaptacoes obrigatorias do mockup, decisao do usuario
 *
 * As tres estao registradas em `docs/backlog/capacidades-prometidas-nos-mockups.md`, que e o
 * arquivo de registro pos-lancamento das capacidades que os mockups prometeram e o MVP nao tem:
 *
 * 1. **O cartao de base global de numeros NAO entra.** O aplicativo nao consulta base alguma: a
 *    decisao sai de agenda, lista pessoal e configuracao, tudo local. O selo de codigo aberto ocupa
 *    o lugar do antigo selo de "protecao ativa" — protecao ativa nesta tela seria afirmacao falsa,
 *    porque o papel de triagem ainda nao foi concedido quando ela aparece.
 * 2. **A imagem fotografica remota do mockup vira superficie tonal.** Nao e escolha de estilo: o
 *    aplicativo nao declara acesso a internet, entao carregar imagem de servidor e impossivel por
 *    construcao. O lugar dela recebe um gradiente tonal mais um icone vetorial.
 * 3. **A sobreposicao de "preparando escudo" sai.** Nao existe trabalho a fazer entre esta tela e o
 *    passo 1, e barra de progresso que mede nada e padrao escuro — proibido pelo projeto.
 *
 * O paralaxe do mockup tambem sai, por motivo mais simples: ele segue o cursor, e nao existe cursor
 * em Android.
 *
 * ## Acessibilidade
 *
 * Ordem de leitura: marca, titulo, subtitulo, cartoes, selo, botao, microcopy. O titulo e cabecalho
 * declarado. O hero e a flutuacao tem a semantica limpa — sao decoracao, e anunciar um circulo com
 * escudo depois da marca repetiria a mesma informacao com vocabulario pior.
 */
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(
            actions = {
                SentinelaTopBarIconAction(
                    icon = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.about_title),
                    onClick = onAbout,
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hero()
            Titulo()
            Subtitulo()
            CartoesHonestos()
            SeloDeCodigoAberto()
        }
        Column(
            modifier = Modifier.padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Rodape(onStart = onStart)
        }
    }
}

/** Circulo do hero, decorativo, com a flutuacao de amplitude reduzida do contrato. */
@Composable
private fun Hero() {
    val movimentoReduzido = rememberMotionReduced()
    val transicao = rememberInfiniteTransition(label = "flutuacao-do-hero")
    val fase by transicao.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FLOAT_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fase-da-flutuacao",
    )
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(top = TopBarToHeroGap)
            .clearAndSetSemantics {}
            .graphicsLayer {
                translationY = if (movimentoReduzido) 0f else fase * HeroFloatAmplitude.toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(HeroCircleSize),
            shape = CircleShape,
            color = cores.surfaceContainer,
            border = BorderStroke(
                width = HeroBorderWidth,
                color = cores.primary.copy(alpha = HERO_BORDER_ALPHA),
            ),
        ) {}
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = cores.primary,
        )
    }
}

@Composable
private fun Titulo() {
    Text(
        text = stringResource(R.string.welcome_headline),
        modifier = Modifier
            .padding(top = HeroToTitleGap)
            .widthIn(max = TitleMaxWidth)
            .semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        fontSize = TitleFontSize,
        lineHeight = TitleLineHeight,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Subtitulo() {
    Text(
        text = stringResource(R.string.welcome_subtitle),
        modifier = Modifier
            .padding(top = TitleToSubtitleGap)
            .widthIn(max = SubtitleMaxWidth),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Arranjo assimetrico dos tres cartoes: um largo ocupando a linha inteira e dois de meia largura.
 *
 * Os tres afirmam apenas o que o aplicativo faz — bloqueio local, silencio e ausencia de internet.
 * Nenhum deles fala de base de numeros, de nuvem ou de inteligencia: essa era a promessa do mockup
 * que esta fase substituiu.
 */
@Composable
private fun CartoesHonestos() {
    val cores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(top = SubtitleToCardsGap)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        CartaoHonesto(
            title = stringResource(R.string.welcome_feature_local_title),
            description = stringResource(R.string.welcome_feature_local_desc),
            icon = Icons.Outlined.VerifiedUser,
            iconTint = cores.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
            CartaoHonesto(
                title = stringResource(R.string.welcome_feature_silent_title),
                description = stringResource(R.string.welcome_feature_silent_desc),
                icon = Icons.Outlined.NotificationsOff,
                iconTint = cores.secondary,
                modifier = Modifier.weight(1f),
            )
            CartaoHonesto(
                title = stringResource(R.string.welcome_feature_offline_title),
                description = stringResource(R.string.welcome_feature_offline_desc),
                icon = Icons.Outlined.CloudOff,
                iconTint = cores.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Cartao de caracteristica.
 *
 * O fundo e a superficie tonal que substitui a imagem remota do mockup (adaptacao 2 do KDoc da
 * tela): gradiente de `SurfaceContainerLow` ao container primario a 20% de alfa.
 */
@Composable
private fun CartaoHonesto(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = ShapeMedium,
        color = cores.surfaceContainerLow,
        contentColor = cores.onSurface,
        border = BorderStroke(CardBorderWidth, cores.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            cores.primaryContainer.copy(alpha = TONAL_SURFACE_ALPHA),
                        ),
                    ),
                )
                .padding(CardPadding),
        ) {
            Box(
                modifier = Modifier.size(CardIconContainerSize),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(CardIconContainerSize),
                    shape = CircleShape,
                    color = cores.surfaceContainerHighest,
                ) {}
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(CardIconSize),
                    tint = iconTint,
                )
            }
            Text(
                text = title,
                modifier = Modifier.padding(top = CardIconToTitleGap),
                style = MaterialTheme.typography.labelLarge,
                color = cores.onSurface,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = CardTitleToDescGap),
                style = MaterialTheme.typography.labelMedium,
                color = cores.onSurfaceVariant,
            )
        }
    }
}

/** Selo de codigo aberto — ocupa o lugar do selo de "protecao ativa" do mockup. */
@Composable
private fun SeloDeCodigoAberto() {
    Surface(
        modifier = Modifier.padding(top = CardsToBadgeGap),
        shape = ShapePill,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = BadgeHorizontalPadding,
                vertical = BadgeVerticalPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null,
                modifier = Modifier.size(BadgeIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.welcome_open_source),
                modifier = Modifier.padding(start = BadgeIconToTextGap),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Rodape: gradiente de 32dp, botao de largura total e a microcopy.
 *
 * O botao fica FORA de qualquer container com semantica de mesclagem — a licao medida nas Fases 6 e
 * 7 e que estado declarado no ancestral fica onde ninguem consulta.
 */
@Composable
private fun Rodape(onStart: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CtaGradientHeight)
            .background(Brush.verticalGradient(listOf(Color.Transparent, cores.surface))),
    )
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(CtaHeight),
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = cores.primary,
            contentColor = cores.onPrimary,
        ),
    ) {
        Text(
            text = stringResource(R.string.welcome_cta),
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.padding(start = CtaIconGap),
        )
    }
    Text(
        text = stringResource(R.string.welcome_cta_hint),
        modifier = Modifier
            .padding(top = CtaToHintGap, bottom = FooterBottomGap)
            .fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = cores.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun WelcomeScreenLightPreview() {
    SentinelaTheme(darkTheme = false, dynamicColor = false) {
        Surface { WelcomeScreen(onStart = {}, onAbout = {}) }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun WelcomeScreenDarkPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface { WelcomeScreen(onStart = {}, onAbout = {}) }
    }
}

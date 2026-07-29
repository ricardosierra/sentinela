package org.sentinela.app.ui.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.telecom.call.CallIdentity
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.numberLg
import org.sentinela.app.telecom.call.CallOrigin as DomainCallOrigin

/** Avatar da identidade na tela de chamada. */
val CallerAvatarDiameter = 96.dp

private val AvatarRingWidth = 2.dp
private val AvatarIconSize = 44.dp
private val AvatarToPrimaryGap = 16.dp
private val PrimaryToSecondaryGap = 8.dp
private const val WHITELIST_AVATAR_ALPHA = 0.15f
private const val MONOGRAM_MAX_LETTERS = 2
private const val PREFIXO_BR = "+55"
private const val DIGITOS_BR_COM_DDD_MIN = 10
private const val DIGITOS_BR_COM_DDD_MAX = 11
private const val TAMANHO_DDD = 2
private const val SUFIXO_LOCAL = 4

/**
 * Texto das duas linhas de identidade, resolvido a partir do retrato da chamada.
 *
 * Separado da composicao de proposito: a escolha de qual linha o numero ocupa e regra, e regra
 * precisa ser legivel e testavel sem inflar arvore de composicao.
 */
internal data class CallerIdentityText(val primary: String, val secondary: String?)

/**
 * Origem efetiva da identidade.
 *
 * Contato sem nome resolvido significa leitura da agenda revogada ou contato removido no meio da
 * ligacao. Nesse caso a tela **degrada** para desconhecido, sem erro e sem alerta: um aviso de
 * permissao no meio de uma chamada seria a pior hora possivel de pedir qualquer coisa.
 */
internal fun effectiveOrigin(identity: CallIdentity): DomainCallOrigin =
    if (identity.origin == DomainCallOrigin.CONTATO && identity.displayName.isNullOrBlank()) {
        DomainCallOrigin.DESCONHECIDO
    } else {
        identity.origin
    }

/**
 * Agrupa o numero para exibicao **sem nunca inventar informacao**.
 *
 * So o formato brasileiro com codigo do pais e agrupado, porque e o unico cujo desenho de DDD o
 * aplicativo conhece com certeza. Qualquer outro numero aparece exatamente como a telefonia
 * entregou — adivinhar agrupamento de pais desconhecido produziria numero visualmente errado, que
 * e pior do que numero sem espacos.
 *
 * Regiao e operadora **nao** sao exibidas em caso nenhum: o aplicativo nao as conhece.
 */
internal fun formatFullNumberForDisplay(number: String): String {
    if (!number.startsWith(PREFIXO_BR)) return number
    val nacional = number.removePrefix(PREFIXO_BR).filter { it.isDigit() }
    if (nacional.length !in DIGITOS_BR_COM_DDD_MIN..DIGITOS_BR_COM_DDD_MAX) return number
    val ddd = nacional.take(TAMANHO_DDD)
    val resto = nacional.drop(TAMANHO_DDD)
    val inicio = resto.dropLast(SUFIXO_LOCAL)
    val fim = resto.takeLast(SUFIXO_LOCAL)
    return "$PREFIXO_BR $ddd $inicio-$fim"
}

/**
 * Descricao falada do numero: digito a digito, nunca como valor numerico.
 *
 * Sem isso o leitor de tela le "onze bilhoes e ..." e o usuario nao consegue conferir para quem
 * esta atendendo, que e justamente a decisao que esta tela existe para apoiar.
 */
internal fun spokenDigits(text: String): String = text.map { it }.joinToString(" ")

/** Monograma de reserva quando o contato nao tem foto na agenda. */
internal fun monogramOf(name: String): String = name
    .split(' ', '\t')
    .filter { it.isNotBlank() }
    .take(MONOGRAM_MAX_LETTERS)
    .map { it.first().uppercaseChar() }
    .joinToString("")

@Composable
private fun identityTextOf(identity: CallIdentity): CallerIdentityText {
    val privado = stringResource(R.string.history_private_number)
    val idOculto = stringResource(R.string.history_private_id)
    val numero = identity.fullNumber?.takeIf { it.isNotBlank() }
        ?.let(::formatFullNumberForDisplay)
    return when (effectiveOrigin(identity)) {
        DomainCallOrigin.PRIVADO -> CallerIdentityText(privado, idOculto)
        DomainCallOrigin.DESCONHECIDO ->
            // Unico caso em que o numero e promovido a linha primaria. Nada de secundaria: nem
            // regiao nem operadora, porque o aplicativo nao as conhece.
            CallerIdentityText(numero ?: privado, null)
        DomainCallOrigin.CONTATO, DomainCallOrigin.PERMITIDO -> {
            val primaria = identity.displayName?.takeIf { it.isNotBlank() } ?: numero ?: privado
            CallerIdentityText(primaria, numero?.takeIf { it != primaria })
        }
    }
}

/** Traducao entre a origem do dominio e o vocabulario do chip visual. */
internal fun chipOriginOf(origin: DomainCallOrigin): CallOrigin = when (origin) {
    DomainCallOrigin.CONTATO -> CallOrigin.CONTACT
    DomainCallOrigin.PERMITIDO -> CallOrigin.WHITELIST
    DomainCallOrigin.DESCONHECIDO -> CallOrigin.UNKNOWN
    DomainCallOrigin.PRIVADO -> CallOrigin.PRIVATE
}

private data class AvatarStyle(val container: Color, val content: Color, val icon: ImageVector?)

@Composable
private fun avatarStyle(origin: DomainCallOrigin): AvatarStyle {
    val scheme = MaterialTheme.colorScheme
    return when (origin) {
        DomainCallOrigin.CONTATO ->
            AvatarStyle(scheme.secondaryContainer, scheme.onSecondaryContainer, null)
        DomainCallOrigin.PERMITIDO -> AvatarStyle(
            scheme.primary.copy(alpha = WHITELIST_AVATAR_ALPHA),
            scheme.primary,
            Icons.Outlined.VerifiedUser,
        )
        DomainCallOrigin.DESCONHECIDO -> AvatarStyle(
            scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant,
            Icons.Outlined.Person,
        )
        DomainCallOrigin.PRIVADO -> AvatarStyle(
            scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant,
            Icons.Outlined.VisibilityOff,
        )
    }
}

/**
 * Avatar de 96dp. A foto chega **ja carregada em memoria** por parametro.
 *
 * Este composable nao acessa a agenda e nao guarda nada: a foto do contato e lida no instante da
 * chamada e jamais cacheada em disco. O anel de acento marca o contato conhecido.
 */
@Composable
private fun CallerAvatar(
    identity: CallIdentity,
    origin: DomainCallOrigin,
    photo: ImageBitmap?,
) {
    val estilo = avatarStyle(origin)
    val anel = if (origin == DomainCallOrigin.CONTATO) {
        Modifier.border(AvatarRingWidth, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(CallerAvatarDiameter)
            .clip(CircleShape)
            .background(estilo.container, CircleShape)
            .then(anel),
        contentAlignment = Alignment.Center,
    ) {
        val nome = identity.displayName
        when {
            photo != null -> Image(
                bitmap = photo,
                contentDescription = null,
                modifier = Modifier.size(CallerAvatarDiameter).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            estilo.icon != null -> Icon(
                imageVector = estilo.icon,
                contentDescription = null,
                modifier = Modifier.size(AvatarIconSize),
                tint = estilo.content,
            )
            nome != null -> Text(
                text = monogramOf(nome),
                style = MaterialTheme.typography.headlineMedium,
                color = estilo.content,
            )
        }
    }
}

/**
 * Bloco de identidade de quem esta na chamada, nas quatro variantes do contrato de design.
 *
 * O numero aparece **completo**: nesta tela o numero e o produto e o usuario precisa dele inteiro
 * para decidir se atende. A mascara continua obrigatoria em registro de execucao, notificacao,
 * historico e relatorio de falha — a fronteira e esta tela, nao o dado.
 */
@Composable
fun CallerIdentity(
    identity: CallIdentity,
    modifier: Modifier = Modifier,
    photo: ImageBitmap? = null,
) {
    val origem = effectiveOrigin(identity)
    val texto = identityTextOf(identity)
    val faladoPrimario = spokenDigits(texto.primary)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PrimaryToSecondaryGap),
    ) {
        CallerAvatar(identity = identity, origin = origem, photo = photo)
        Text(
            text = texto.primary,
            modifier = Modifier
                .padding(top = AvatarToPrimaryGap)
                .semantics {
                    // Numero na linha primaria e lido digito a digito; nome proprio e lido como
                    // texto normal, e por isso a descricao so e substituida quando ha digitos.
                    if (texto.primary.any { it.isDigit() }) contentDescription = faladoPrimario
                },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        val secundaria = texto.secondary
        if (secundaria != null) {
            val faladoSecundario = spokenDigits(secundaria)
            Text(
                text = secundaria,
                modifier = Modifier.semantics {
                    if (secundaria.any { it.isDigit() }) contentDescription = faladoSecundario
                },
                style = MaterialTheme.typography.numberLg,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        CallOriginChip(origin = chipOriginOf(origem))
    }
}

@Preview
@Composable
private fun CallerIdentityVariantsPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(AvatarToPrimaryGap)) {
                CallerIdentity(
                    CallIdentity(
                        displayName = "Ana Paula Souza",
                        fullNumber = "+5511912345678",
                        origin = DomainCallOrigin.CONTATO,
                    ),
                )
                CallerIdentity(
                    CallIdentity(
                        fullNumber = "+5511912345678",
                        origin = DomainCallOrigin.DESCONHECIDO,
                    ),
                )
                CallerIdentity(CallIdentity(origin = DomainCallOrigin.PRIVADO))
            }
        }
    }
}

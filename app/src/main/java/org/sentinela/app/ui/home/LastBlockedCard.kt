package org.sentinela.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapeMedium
import org.sentinela.app.ui.theme.StatusBlocked

private val CardPadding = 16.dp
private val CardMinTarget = 72.dp
private val AvatarSize = 48.dp
private val AvatarIconSize = 24.dp
private val AvatarToTextGap = 16.dp
private val ChevronSize = 24.dp
private val BorderWidth = 1.dp
private const val AVATAR_ALPHA = 0.30f

/**
 * Ultima chamada bloqueada.
 *
 * A assinatura recebe o numero **JA MASCARADO** e nao tem parametro de numero em bruto: a fronteira
 * de privacidade herdada da Fase 6 esta expressa no tipo. Este cartao nao mascara nada, nao conhece
 * a mascara do aplicativo e nao teria como recuperar os digitos completos nem se quisesse — quem
 * mascara e o dono de estado, antes de o estado existir. A sequencia completa vive somente nas telas
 * de chamada e de discagem.
 *
 * **Nao existe rotulo de risco aqui, e isso e permanente.** O desenho original trazia uma
 * classificacao de fraude e um icone de informacao que abriria o detalhe dessa classificacao. O
 * aplicativo nao classifica chamada, nao conhece base de numeros indesejados e nao tem esse dado —
 * inventa-lo seria mentir ao usuario com aparencia de certeza. O motivo mostrado e o motivo REAL da
 * decisao, e os unicos rotulos permitidos sao os de numero desconhecido, numero privado, identidade
 * oculta, contato e permitido. O icone de informacao saiu junto: nao ha nada a explicar que a tela
 * de historico ja nao diga, e por isso o cartao inteiro leva ao historico.
 */
@Composable
fun LastBlockedCard(
    maskedNumber: String,
    reasonLabel: String,
    relativeTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricao = stringResource(
        R.string.dashboard_last_blocked_description,
        maskedNumber,
        reasonLabel,
        relativeTime,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .requiredSizeIn(minHeight = CardMinTarget)
            .border(BorderWidth, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(AvatarSize)
                    .background(StatusBlocked.copy(alpha = AVATAR_ALPHA), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(AvatarIconSize),
                    tint = StatusBlocked,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AvatarToTextGap),
            ) {
                Text(
                    text = maskedNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = reasonLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(ChevronSize),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Tempo relativo com a granularidade ditada pelo contrato de design: "agora" abaixo de um minuto,
 * minutos abaixo de uma hora, horas abaixo de um dia, "ontem" no dia anterior, e depois data curta.
 *
 * As duas faixas com contagem saem de recursos de PLURAL, nunca de concatenacao — montar o texto
 * somando numero e sufixo produz a construcao que o lint acusa e que o contrato de design proibe.
 * O relogio entra por parametro, no padrao de toda regra dependente de tempo neste projeto: sem
 * isso o proprio teste nao teria como fixar o instante.
 */
@Composable
fun relativeTimeLabel(timestampUtcMillis: Long, nowUtcMillis: Long): String {
    val decorrido = (nowUtcMillis - timestampUtcMillis).coerceAtLeast(0L)
    val minutos = decorrido / MILLIS_POR_MINUTO
    val horas = decorrido / MILLIS_POR_HORA
    val dias = decorrido / MILLIS_POR_DIA
    return when {
        minutos < 1L -> stringResource(R.string.time_now)
        horas < 1L -> pluralStringResource(
            R.plurals.time_minutes_ago,
            minutos.toInt(),
            minutos.toInt(),
        )

        dias < 1L -> pluralStringResource(R.plurals.time_hours_ago, horas.toInt(), horas.toInt())
        dias < 2L -> stringResource(R.string.time_yesterday)
        else -> {
            val padrao = stringResource(R.string.time_date_short_pattern)
            remember(padrao, timestampUtcMillis) {
                SimpleDateFormat(padrao, Locale.getDefault()).format(Date(timestampUtcMillis))
            }
        }
    }
}

private const val MILLIS_POR_MINUTO = 60_000L
private const val MILLIS_POR_HORA = 3_600_000L
private const val MILLIS_POR_DIA = 86_400_000L

@Preview(widthDp = 411)
@Composable
private fun LastBlockedCardPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(CardPadding),
            ) {
                LastBlockedCard(
                    maskedNumber = "+55 11 9****-1234",
                    reasonLabel = stringResource(R.string.history_unknown_number),
                    relativeTime = stringResource(R.string.time_now),
                    onClick = {},
                )
            }
        }
    }
}

package org.sentinela.app.ui.call

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.sentinela.app.R
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.timer

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
private const val TICK_MILLIS = 250L

/**
 * Formata a duracao decorrida. MM:SS ate uma hora, H:MM:SS a partir dela.
 *
 * Funcao pura para permitir teste sem relogio de sistema.
 */
internal fun formatCallDuration(elapsedSeconds: Long): String {
    val seguros = elapsedSeconds.coerceAtLeast(0)
    val horas = seguros / SECONDS_PER_HOUR
    val minutos = (seguros % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val segundos = seguros % SECONDS_PER_MINUTE
    return if (horas > 0) {
        "%d:%02d:%02d".format(horas, minutos, segundos)
    } else {
        "%02d:%02d".format(minutos, segundos)
    }
}

/**
 * Cronometro da chamada ativa.
 *
 * Usa o estilo de cronometro do tema, que pede figuras de largura fixa: sem
 * isso o texto muda de largura a cada segundo e desloca o layout inteiro.
 *
 * O relogio e injetado para permitir teste determinístico. A regiao viva e
 * educada de proposito: o leitor de tela anuncia a duracao quando houver folga,
 * sem roubar o foco do botao de encerrar.
 */
@Composable
fun CallTimer(
    startedAtMillis: Long,
    modifier: Modifier = Modifier,
    // TODO: relogio de PAREDE para medir duracao. Ajuste de hora pela operadora ou por NTP no meio
    //  da ligacao faz o cronometro pular, andar para tras ou exibir duracao negativa. Duracao pede
    //  relogio monotonico (`SystemClock.elapsedRealtime`), com `startedAtMillis` na mesma base.
    clock: () -> Long = System::currentTimeMillis,
) {
    var agora by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            agora = clock()
            delay(TICK_MILLIS)
        }
    }
    val texto by remember(startedAtMillis) {
        derivedStateOf { 
            val decorrido = (agora - startedAtMillis) / MILLIS_PER_SECOND
            formatCallDuration(decorrido) 
        }
    }
    val descricao = stringResource(R.string.call_timer_description, texto)
    Text(
        text = texto,
        modifier = modifier.semantics {
            contentDescription = descricao
            liveRegion = LiveRegionMode.Polite
        },
        style = MaterialTheme.typography.timer,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun CallTimerPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            CallTimer(startedAtMillis = 0L, clock = { 3_725_000L })
        }
    }
}

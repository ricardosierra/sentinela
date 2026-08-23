package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sentinela.app.R
import org.sentinela.app.settings.RetentionPolicy
import org.sentinela.app.ui.components.SettingSwitchRow

/** Itens 11, 12 e 13 — o histórico local, a janela de retenção e a limpeza. */
@Composable
internal fun GrupoDeHistorico(
    state: SettingsUiState,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onRetention: (RetentionPolicy) -> Unit,
    onPedirLimpeza: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_retention_title)) {
        SettingSwitchRow(
            label = stringResource(R.string.settings_history_enabled),
            description = stringResource(R.string.settings_history_enabled_desc),
            checked = state.settings.historyEnabled,
            onCheckedChange = onHistoryEnabledChange,
        )
        EscolhaUnica {
            RetencaoOpcao(state, RetentionPolicy.NEVER_STORE, R.string.settings_retention_never, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_7, R.string.settings_retention_7, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_30, R.string.settings_retention_30, onRetention)
            RetencaoOpcao(state, RetentionPolicy.DAYS_90, R.string.settings_retention_90, onRetention)
            RetencaoOpcao(state, RetentionPolicy.MANUAL, R.string.settings_retention_manual, onRetention)
        }
        NotaDoGrupo(text = stringResource(R.string.about_data_local))
        // Piso EXIGIDO de altura: o botão de texto do Material desenha 40dp, e o alvo de toque que o
        // Compose expande sozinho esconderia isso de qualquer assert de toque. O eixo do desenho
        // pegou — quarta vez neste projeto (Fases 6, 07-03 e aqui). `requiredHeightIn` não negocia
        // com o pai; `heightIn` negociaria e voltaria a 40dp em tela apertada.
        TextButton(
            onClick = onPedirLimpeza,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = DestructiveMinTarget),
        ) {
            Text(
                text = stringResource(R.string.history_clear_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Uma janela de retenção.
 *
 * Estas são as ÚNICAS opções da tela sem descrição própria, e é deliberado. A duração inteira já está
 * dita no rótulo ("7 dias", "Até eu excluir"); a explicação do item vive uma vez, como nota do grupo,
 * logo abaixo das cinco. Repetir cinco parágrafos idênticos sob cinco durações seria enchimento, e
 * inventar um parágrafo diferente para cada duração seria pior: texto sem informação nova.
 *
 * A consequência destrutiva de "Não guardar" não é dita aqui de propósito — ela é o corpo do diálogo
 * de confirmação, que é onde a §9.2 a coloca. Dizê-la nos dois lugares criaria duas cópias da mesma
 * frase, e a cópia esquecida é sempre a que fica errada.
 */
@Composable
private fun RetencaoOpcao(
    state: SettingsUiState,
    politica: RetentionPolicy,
    labelRes: Int,
    onRetention: (RetentionPolicy) -> Unit,
) {
    OpcaoDePolitica(
        title = stringResource(labelRes),
        description = "",
        icon = if (politica == RetentionPolicy.NEVER_STORE) {
            Icons.Outlined.Delete
        } else {
            Icons.Outlined.Schedule
        },
        selected = state.settings.retentionPolicy == politica,
        onClick = { onRetention(politica) },
    )
}


@Preview(widthDp = 411, heightDp = 800)
@Composable
private fun GrupoDeHistoricoPreview() {
    SecaoDeExemplo {
        GrupoDeHistorico(
            state = SettingsUiState(loading = false),
            onHistoryEnabledChange = {},
            onRetention = {},
            onPedirLimpeza = {},
        )
    }
}

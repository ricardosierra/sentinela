package org.sentinela.app.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.domain.DecisionReason
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * A notificação de bloqueio sempre carregou o identificador do registro na intenção, e ninguém
 * lia esse valor: tocar nela abria a Home. O requisito NTF-05 promete abrir **o registro
 * correspondente**, então a lista precisa parar nele — e não apenas existir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryHighlightTest {

    @get:Rule
    val compose = createComposeRule()

    private val agora = 1_000_000_000L

    private fun entrada(id: Long) = BlockedCallEntry(
        id = id,
        maskedNumber = "+55 11 9****-${1000 + id}",
        numberE164 = "+5511999990$id",
        timestampUtcMillis = agora - id * 60_000L,
        reason = DecisionReason.UNKNOWN_NUMBER,
        notificationShown = true,
    )

    private val registros = (1L..30L).map(::entrada)

    private fun compor(destaque: Long?) {
        compose.setContent {
            SentinelaTheme {
                HistoryScreen(
                    state = HistoryUiState.Content(registros),
                    onFilterChanged = {},
                    onDecisionFilterChanged = {},
                    onClearAll = {},
                    onAllowNumber = { _, _ -> },
                    onMarkUnwanted = {},
                    onDeleteEntry = {},
                    bottomBar = {},
                    agoraUtcMillis = agora,
                    registroEmDestaque = destaque,
                )
            }
        }
    }

    @Test
    fun `lista rola ate o registro apontado pela notificacao`() {
        compor(destaque = 28L)

        compose.onNodeWithText(entrada(28L).maskedNumber).assertIsDisplayed()
    }

    /** Sem destaque, a lista abre no topo — comportamento normal de quem entrou pela aba. */
    @Test
    fun `sem destaque a lista comeca do inicio`() {
        compor(destaque = null)

        compose.onNodeWithText(entrada(1L).maskedNumber).assertIsDisplayed()
    }

    /** O usuário pode ter apagado o registro antes de tocar no aviso: não pode quebrar. */
    @Test
    fun `destaque inexistente nao derruba a tela`() {
        compor(destaque = 9_999L)

        compose.onNodeWithText(entrada(1L).maskedNumber).assertIsDisplayed()
    }
}

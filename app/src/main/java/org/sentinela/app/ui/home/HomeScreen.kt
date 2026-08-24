package org.sentinela.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarIconAction

private val ScreenPadding = 16.dp
private val BlockGap = 24.dp
private val BottomGap = 32.dp

private const val TRAVERSAL_HERO = 1f
private const val TRAVERSAL_AVISOS = 2f
private const val TRAVERSAL_ESTATISTICAS = 3f
private const val TRAVERSAL_ULTIMA = 4f
private const val TRAVERSAL_ATALHOS = 5f
private const val TRAVERSAL_BARRA_INFERIOR = 6f

/**
 * Ordem de travessia DECLARADA para cada bloco da home.
 *
 * A ordem visual e a ordem de composicao coincidem hoje, mas depender dessa coincidencia deixaria a
 * leitura de tela a merce de qualquer reordenacao de layout futura — inclusive das que a rolagem e as
 * quebras por escala de fonte produzem. Cada bloco e um grupo de travessia com indice proprio; a
 * marca vem antes por estar na barra superior, e o interruptor continua sendo no separado DENTRO do
 * grupo do cartao principal, nunca um estado declarado no grupo.
 */
private fun Modifier.ordemDeTravessia(indice: Float): Modifier = semantics {
    isTraversalGroup = true
    traversalIndex = indice
}

/**
 * A home inteira, composta PURA.
 *
 * Nada aqui conhece dono de estado, montador de dependencias ou ciclo de vida: a reconsulta viva do
 * papel na retomada mora na camada de rota, e a tela recebe o retrato pronto. Isso e o que permite
 * compor os oito estados degradados num teste sem plataforma nenhuma.
 *
 * **Toda a tela ROLA.** Nenhuma tela desta fase pode depender de caber: a escala de fonte do sistema
 * pode dobrar o tamanho de todo texto, e conteudo que "cabe" no aparelho do desenvolvedor deixa de
 * caber no aparelho de quem mais precisa de acessibilidade.
 *
 * **Precedencia dos avisos** e obrigatoria e esta em [avisosDaHome]: papel de triagem ausente, depois
 * leitura da agenda negada, depois historico desligado, depois papel de discador perdido. Do terceiro
 * em diante o excedente vira uma unica linha que leva a tela de Protecao.
 *
 * O desfoque da barra superior do desenho original virou elevacao tonal, e o desfoque dos cartoes
 * virou camada tonal com borda: desfoque em tempo real custa quadro e a interface so existe a partir
 * do nivel 31.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onProtectionChange: (Boolean) -> Unit,
    onFixRole: () -> Unit,
    onGrantContacts: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableHistory: () -> Unit,
    onRetryHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDialerActivation: () -> Unit,
    bottomBar: @Composable () -> Unit,
    onAcceptRating: () -> Unit,
    onDismissRating: () -> Unit,
    modifier: Modifier = Modifier,
    nowUtcMillis: Long = System.currentTimeMillis(),
) {
    if (state.showRatingInvitation) {
        RatingBottomSheet(
            onAccept = onAcceptRating,
            onDismiss = onDismissRating
        )
    }

    val avisos = avisosDaHome(state)
    val executarAviso: (AcaoDoAviso) -> Unit = { acao ->
        when (acao) {
            AcaoDoAviso.CORRIGIR_PAPEL -> onFixRole()
            AcaoDoAviso.PEDIR_AGENDA -> onGrantContacts()
            AcaoDoAviso.ABRIR_CONFIGURACOES_DO_APLICATIVO -> onOpenAppSettings()
            AcaoDoAviso.LIGAR_HISTORICO -> onEnableHistory()
            AcaoDoAviso.TENTAR_LEITURA_DE_NOVO -> onRetryHistory()
            AcaoDoAviso.ABRIR_ATIVACAO_DO_DISCADOR -> onOpenDialerActivation()
        }
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SentinelaTopBar(
                    actions = {
                        SentinelaTopBarIconAction(
                            icon = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            onClick = onOpenSettings,
                        )
                    },
                )
                CorpoDaHome(
                    state = state,
                    avisos = avisos,
                    onAcaoDeAviso = executarAviso,
                    nowUtcMillis = nowUtcMillis,
                    onProtectionChange = onProtectionChange,
                    onOpenSettings = onOpenSettings,
                    onOpenWhitelist = onOpenWhitelist,
                    onOpenHistory = onOpenHistory,
                )
            }
            Box(modifier = Modifier.ordemDeTravessia(TRAVERSAL_BARRA_INFERIOR)) {
                bottomBar()
            }
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            )
        }
    }
}

/** Os blocos da home na ordem e com os espacamentos ditados pelo contrato de interface. */
@Composable
private fun CorpoDaHome(
    state: HomeUiState,
    avisos: List<AvisoDaHome>,
    onAcaoDeAviso: (AcaoDoAviso) -> Unit,
    nowUtcMillis: Long,
    onProtectionChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        Spacer(modifier = Modifier.height(BlockGap))
        StatusHeroCard(
            protectionEnabled = state.protectionEnabled,
            onProtectionChange = onProtectionChange,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_HERO),
        )
        if (avisos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(BlockGap))
            BlocoDeAvisos(
                avisos = avisos,
                onAcao = onAcaoDeAviso,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.ordemDeTravessia(TRAVERSAL_AVISOS),
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))
        BlocoDeEstatisticas(
            state = state,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_ESTATISTICAS),
        )
        if (state.historyEnabled) {
            Spacer(modifier = Modifier.height(BlockGap))
            BlocoDaUltimaBloqueada(
                state = state,
                nowUtcMillis = nowUtcMillis,
                onOpenHistory = onOpenHistory,
                modifier = Modifier.ordemDeTravessia(TRAVERSAL_ULTIMA),
            )
        }
        Spacer(modifier = Modifier.height(BlockGap))
        BlocoDeAtalhos(
            onOpenWhitelist = onOpenWhitelist,
            onOpenHistory = onOpenHistory,
            modifier = Modifier.ordemDeTravessia(TRAVERSAL_ATALHOS),
        )
        Spacer(modifier = Modifier.height(BottomGap))
    }
}


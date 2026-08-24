package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.ui.call.rememberMotionReduced
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.SentinelaTopBarTextAction
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.theme.SentinelaTheme

private val ScreenPadding = 16.dp

/**
 * Duracao da transicao entre passos do onboarding, em milissegundos.
 *
 * O contrato de design pede deslizamento horizontal mais dissolucao de 250 ms, espelhado no
 * retorno. A transicao **nao** vive dentro desta tela: ela pertence ao envelope de navegacao, que e
 * quem conhece os dois passos envolvidos e o sentido do movimento. O que mora aqui e o numero e a
 * regra de supressao, para que o envelope nao precise redescobrir nenhum dos dois.
 */
const val DURACAO_DA_TRANSICAO_DE_PASSO_MILLIS = 250

/**
 * Duracao efetiva da transicao entre passos: zero quando a reducao de movimento esta ligada.
 *
 * Troca instantanea nao e degradacao — e o comportamento pedido. Nenhuma informacao do onboarding
 * depende da animacao; o contador de passo em texto ja diz onde o usuario esta.
 */
@Composable
fun rememberStepTransitionMillis(): Int =
    if (rememberMotionReduced()) 0 else DURACAO_DA_TRANSICAO_DE_PASSO_MILLIS

/**
 * Passo 1 de 6 — o pedido do papel de filtro de chamadas.
 *
 * Composta **pura**: recebe o estado pronto e devolve intencoes. Nenhum container, nenhum dono de
 * estado e nenhuma leitura de repositorio vivem aqui.
 *
 * ## O aviso obrigatorio da fase
 *
 * O cartao de honestidade desta tela e o aviso de que **so chamada de telefone e filtrada**, e ele
 * nao e rodape em cinza: tem o mesmo peso visual do resto da tela, exatamente como a tela de
 * ativacao do modo discador faz com os dois cartoes de peso igual.
 *
 * As tres frases dele ja existem em recurso, escritas nas Fases 5 e 6 a partir da fonte do proprio
 * Android — chamadas de aplicativo de internet fora do alcance, "Nao Perturbe" do sistema valendo
 * por cima, e o registro no historico do telefone que o Android so omite para aplicativo de
 * operadora. **Reescreve-las e o caminho de volta a promessa falsa e e proibido.**
 *
 * ## Os tres ramos de estado do papel
 *
 * Nenhum deles trava o passo, e nenhum deles repete o dialogo do sistema sem toque explicito:
 *
 * - **sem papel e sem pedido em curso:** o botao pede o papel;
 * - **pedido em curso:** o botao vira "solicitando" e fica desabilitado. Ele esta deliberadamente
 *   FORA de qualquer container com semantica de mesclagem: estado declarado em ancestral fica onde
 *   ninguem consulta, e o desabilitado desapareceria em silencio (medido nas Fases 6 e 7);
 * - **papel concedido:** aparece o chip de ativo sob o titulo e o botao vira avancar. O avanco
 *   **nao** e automatico — o usuario le a confirmacao e toca.
 *
 * Papel negado acrescenta o aviso com acao de tentar de novo, e o botao tambem vira avancar: negar
 * o papel custa a triagem, nunca o resto do onboarding.
 *
 * O chip e o aviso de resultado sao regiao viva **educada**, para anunciar a transicao
 * concedido/negado sem roubar o foco do botao. Modo enfatico seria interrupcao, nao aviso.
 */
@Composable
fun RoleStepScreen(
    state: OnboardingUiState,
    onRequestRole: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(
            center = { StepHeader(step = NUMERO_DE_EXIBICAO_DO_PASSO, total = state.totalSteps) },
            actions = {
                SentinelaTopBarTextAction(
                    label = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
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
            HeroDoPapel()
            TituloDoPapel()
            if (state.screeningRoleHeld) ChipDePapelAtivo()
            IntroDoPapel()
            FaixaDeContextoDoPapel()
            CartaoDeEscopoDoPapel()
        }
        RodapeDoPapel(
            state = state,
            onRequestRole = onRequestRole,
            onNext = onNext,
        )
    }
}

/**
 * Numero DE EXIBICAO deste passo, contado a partir de 1. O total chega pelo estado, nunca por
 * literal aqui.
 *
 * Ele se chamava `PASSO_DO_PAPEL`, e `OnboardingRoute.kt` tem um `PASSO_DO_PAPEL` proprio que vale
 * 0 — aquele e o INDICE do passo no fluxo, contado a partir de zero. Dois nomes iguais com valores
 * diferentes no mesmo pacote eram um convite a erro; este aqui e o "1" de "passo 1 de 6" que o
 * usuario le.
 */
private const val NUMERO_DE_EXIBICAO_DO_PASSO = 1

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepPendingPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = NUMERO_DE_EXIBICAO_DO_PASSO),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepGrantedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = NUMERO_DE_EXIBICAO_DO_PASSO, screeningRoleHeld = true),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun RoleStepDeniedPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            RoleStepScreen(
                state = OnboardingUiState(step = NUMERO_DE_EXIBICAO_DO_PASSO, roleDenied = true),
                onRequestRole = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

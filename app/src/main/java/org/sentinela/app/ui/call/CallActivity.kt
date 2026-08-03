package org.sentinela.app.ui.call

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.sentinela.app.BuildConfig
import org.sentinela.app.SentinelaApp
import org.sentinela.app.telecom.call.CallSessionCoordinator
import org.sentinela.app.telecom.call.CallSessionStore
import org.sentinela.app.telecom.call.CallSnapshot
import org.sentinela.app.telecom.call.CallUiState
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Chave do extra que carrega a acao pedida pela notificacao de chamada.
 *
 * O valor textual e contrato ditado e vale exatamente o identificador do aplicativo seguido de
 * `.extra.CALL_ACTION`. Ele e **montado** a partir do identificador em vez de escrito por extenso
 * porque um invariante do projeto proibe identificador do aplicativo literal em Kotlin — o projeto
 * precisa poder ser rebatizado num unico lugar. O valor resultante e o mesmo, byte por byte, e o
 * teste desta fase o afirma por extenso (no conjunto de teste, onde o invariante nao se aplica).
 *
 * Mora aqui, num unico lugar, porque escrever a chave duas vezes nao quebra compilacao: so faz o
 * botao da notificacao parar de funcionar em silencio.
 */
const val EXTRA_CALL_ACTION = "${BuildConfig.APPLICATION_ID}.extra.CALL_ACTION"

/** Valores aceitos no extra de acao. */
const val CALL_ACTION_ANSWER = "answer"
const val CALL_ACTION_REJECT = "reject"
const val CALL_ACTION_HANGUP = "hangup"

/** Acao pedida de fora da tela. Nomeada: valor bruto de intencao nunca circula pelo codigo. */
internal enum class CallIntentAction { ANSWER, REJECT, HANGUP }

/**
 * Traduz o valor bruto do extra.
 *
 * Valor ausente ou desconhecido devolve **nenhuma acao**, e isso e regra de seguranca, nao
 * tolerancia: qualquer aplicativo pode montar uma intencao para esta tela, e uma intencao malformada
 * jamais pode atender nem recusar uma chamada de verdade por acidente. O pior que ela consegue fazer
 * e abrir a tela que a chamada abriria de qualquer forma.
 */
internal fun callActionOf(raw: String?): CallIntentAction? = when (raw) {
    CALL_ACTION_ANSWER -> CallIntentAction.ANSWER
    CALL_ACTION_REJECT -> CallIntentAction.REJECT
    CALL_ACTION_HANGUP -> CallIntentAction.HANGUP
    else -> null
}

/**
 * Aplica a acao no coordenador da sessao.
 *
 * A tela **nao fala com a plataforma de telefonia**: ela pede ao coordenador, que e quem conhece a
 * guarda por estado corrente. Este e o unico caminho das acoes da notificacao — a fase nao cria
 * receptor de transmissao.
 */
internal fun applyCallAction(session: CallSessionCoordinator?, action: CallIntentAction?) {
    when (action) {
        CallIntentAction.ANSWER -> session?.answer()
        CallIntentAction.REJECT -> session?.reject()
        CallIntentAction.HANGUP -> session?.hangUp()
        null -> Unit
    }
}

/**
 * Hospedeira da tela de chamada.
 *
 * Ela apenas **observa** o armazem da sessao e comanda pelo coordenador puro. Nao conhece nenhum
 * tipo da telefonia: o objeto de chamada da plataforma e um manipulador de comunicacao entre
 * processos e nunca atravessa esta fronteira.
 *
 * Nao existe estado a restaurar aqui, e isso e medido, nao suposto: morrendo o processo no meio de
 * uma ligacao, o sistema de telefonia religa a chamada no discador do aparelho sem derruba-la.
 *
 * A foto do contato e a lista de rotas de audio ainda nao chegam a esta tela: a identidade que o
 * servico resolve carrega so o que a propria ligacao informa. Ambas entram por parametro dos
 * composables quando a resolucao de identidade em memoria existir — nenhuma delas e cacheada.
 */
class CallActivity : ComponentActivity() {

    private lateinit var store: CallSessionStore

    /** Acao ainda nao aplicada, guardada como estado para sobreviver a uma sessao ainda nula. */
    private var acaoPendente by mutableStateOf<CallIntentAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        store = (application as SentinelaApp).container.callSessionStore
        // A intencao INICIAL conta: quando a tela ainda nao existe, o toque na notificacao chega por
        // onCreate e nunca por onNewIntent.
        acaoPendente = callActionOf(intent?.getStringExtra(EXTRA_CALL_ACTION))
        setContent {
            SentinelaTheme {
                val snapshot by store.state.collectAsState()
                CallHost(
                    snapshot = snapshot,
                    session = store.session,
                    pendingAction = acaoPendente,
                    onActionApplied = { acaoPendente = null },
                    onFinish = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acaoPendente = callActionOf(intent.getStringExtra(EXTRA_CALL_ACTION))
    }
}

/**
 * Escolhe a tela pelo estado e fecha a si mesma depois do estado final.
 *
 * O `when` sobre o estado e **exaustivo**, inclusive no ramo nao suportado: tela em branco durante
 * uma ligacao e a unica falha desta fase que ninguem detecta — nem o usuario, que ve a tela, nem o
 * sistema de telefonia, que mediu-se nao perceber interface viva e travada.
 */
@Composable
internal fun CallHost(
    snapshot: CallSnapshot,
    session: CallSessionCoordinator?,
    pendingAction: CallIntentAction?,
    onActionApplied: () -> Unit,
    onFinish: () -> Unit,
) {
    // Sem esta confirmacao o prazo de apresentacao vence e a sessao falha alto por desenho: e assim
    // que interface congelada deixa de ser silencio e passa a ser defeito visivel.
    LaunchedEffect(session) { session?.confirmPresented() }

    LaunchedEffect(pendingAction, session) {
        if (pendingAction != null && session != null) {
            applyCallAction(session, pendingAction)
            onActionApplied()
        }
    }

    // Chamada em curso engole o gesto de voltar: sair da tela por acidente com o telefone no ouvido
    // deixaria o usuario sem controle nenhum sobre a ligacao.
    BackHandler(enabled = !snapshot.state.isTerminal()) { }

    LaunchedEffect(snapshot.state) {
        if (snapshot.state.isTerminal()) {
            delay(CALL_ENDED_DISMISS_MILLIS)
            onFinish()
        }
    }

    val actions = CallScreenActions(
        onHangUp = { session?.hangUp() ?: onFinish() },
        onAnswer = { session?.answer() },
        onReject = { session?.reject() },
        onToggleMute = { session?.setMuted(it) },
        onToggleSpeaker = { session?.setSpeakerOn(it) },
        onToggleKeypad = { session?.toggleKeypad() },
        onDigitPressStart = { digito -> digito.firstOrNull()?.let { session?.pressDigit(it) } },
        onDigitPressEnd = { session?.releaseDigit() },
    )

    when (snapshot.state) {
        CallUiState.Incoming -> IncomingCallScreen(
            identity = snapshot.identity,
            onAnswer = actions.onAnswer,
            onReject = actions.onReject,
        )
        CallUiState.Dialing,
        CallUiState.Ringing,
        -> OutgoingCallScreen(snapshot = snapshot, actions = actions)
        CallUiState.Active,
        CallUiState.Ended,
        CallUiState.Failed,
        is CallUiState.Unsupported,
        -> ActiveCallScreen(snapshot = snapshot, actions = actions)
    }
}

package org.sentinela.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.sentinela.app.AppContainer
import org.sentinela.app.R
import org.sentinela.app.SentinelaApp
import org.sentinela.app.ui.navigation.Rotas
import org.sentinela.app.ui.navigation.SentinelaNavHost
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Hospedeira do grafo de navegação, e única `Activity` de interface do fluxo principal.
 *
 * ## Nada de identificador nem de nome visível como literal
 *
 * O identificador do aplicativo e o nome que o usuário lê **nunca** aparecem escritos neste arquivo,
 * nem em código nem nesta documentação. O identificador vem da declaração de pacote e das importações;
 * o nome visível vem sempre de recurso. O motivo é concreto: renomear o produto não pode exigir tocar
 * em código Kotlin, e a verificação de rebranding do script de invariantes reprova o identificador
 * literal em qualquer posição de um arquivo Kotlin — inclusive dentro de comentário e inclusive
 * disfarçado de nome totalmente qualificado. É por isso que tudo aqui é dito em prosa e resolvido por
 * importação.
 *
 * ## O destino inicial é resolvido ANTES de o grafo existir
 *
 * A pergunta "primeira abertura ou não" é respondida pela chave de onboarding concluído, e a resposta
 * precisa estar pronta antes de compor o grafo. Não é preciosismo: **trocar o destino inicial depois de
 * o grafo estar composto não re-navega** — a recomposição encontra a pilha montada e o usuário fica na
 * tela errada. Enquanto a resposta não chega, a hospedeira mostra um estado de carregamento explícito.
 *
 * Resolver isso bloqueando a thread principal está PROIBIDO, e as duas razões apontam para a mesma
 * solução: a primeira leitura do repositório de configurações custa disco (cerca de onze milissegundos,
 * medido na Fase 3), e bloquear a partida a frio é o jeito mais provável de estragar justamente o
 * orçamento que esta base de código protege desde a primeira fase. A correção do defeito e a correção
 * de desempenho são a mesma linha de código.
 *
 * ## A guarda de estado salvo permanece
 *
 * A contagem de abertura só acontece quando não há estado salvo. Início de processo pelo sistema de
 * telecomunicações não é abertura — não há ninguém olhando a tela —, e rotação de aparelho não é
 * abertura nova. Sem a guarda, o convite de avaliação, que se apoia nessa contagem, chegaria ao usuário
 * por girar o telefone.
 *
 * Nada é construído aqui além do que já era: todo colaborador do container continua preguiçoso, e a
 * partida da aplicação continua sem leitura de disco síncrona.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SentinelaApp).container
        // ENG-01: abertura de verdade é esta — há Activity e alguém olhando. A guarda de estado salvo
        // evita contar duas vezes numa simples rotação de tela.
        if (savedInstanceState == null) {
            container.onAppOpened()
        }
        setContent {
            SentinelaTheme {
                RaizDaInterface(container)
            }
        }
    }
}

/**
 * Raiz da interface: espera pelo destino inicial e só então compõe o grafo.
 *
 * A espera é um estado de verdade, com anúncio para leitor de tela, e não uma tela em branco — que o
 * contrato de interface proíbe em qualquer tela do aplicativo.
 */
@Composable
private fun RaizDaInterface(container: AppContainer) {
    val destinoInicial by produceState<String?>(initialValue = null, container) {
        value = if (container.settingsRepository.onboardingCompleted.first()) {
            Rotas.HOME
        } else {
            Rotas.BOAS_VINDAS
        }
    }

    val destino = destinoInicial
    if (destino == null) {
        EsperaDoDestinoInicial()
    } else {
        SentinelaNavHost(container = container, startDestination = destino)
    }
}

@Composable
private fun EsperaDoDestinoInicial() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(HorizontalPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(IndicatorToLabelGap))
            Text(
                text = stringResource(R.string.state_loading),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val HorizontalPadding = 24.dp
private val IndicatorToLabelGap = 16.dp

package org.sentinela.app.data.contacts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Conjunto de chaves E.164 dos numeros da agenda, em memoria.
 *
 * **Este cache NAO existe por velocidade.** A sonda direta ao provider ja entrega p50 ~2 ms com
 * 5.000 contatos, muito dentro do orcamento de 200 ms da decisao. Ele existe por dois outros
 * motivos, e e assim que deve ser lido:
 *
 *  1. **Correcao de chave** — e o unico lugar onde o numero cru da agenda passa pelo normalizador
 *     do proprio app. O valor normalizado do provider e calculado com o pais do APARELHO e foi
 *     medido nulo para contato estrangeiro e ate errado (um fixo do Rio virou numero dos EUA).
 *     Normalizando aqui, a chave fica identica a que a whitelist usa.
 *  2. **Corte da cauda** — a cauda medida da consulta direta (max 35–74 ms) e do binder mais o
 *     SQLite do provider; o conjunto em memoria a elimina.
 *
 * Nao repetir o erro da Fase 3 de tratar cronometro como prova de estrutura: quem prova que o cache
 * e usado e o contador de consultas ao provider, nunca o tempo.
 *
 * Guarda **somente** `Set<String>` de chaves. Nunca nome, foto ou identificador de contato.
 */
internal class ContactKeyCache(
    private val source: ContactNumberSource,
    private val normalizer: PhoneNumberNormalizer,
    private val scope: CoroutineScope,
) {

    @Volatile
    private var keys: Set<String>? = null

    private val construindo = AtomicBoolean(false)
    private val observando = AtomicBoolean(false)

    /**
     * Buffer generoso e descarte do mais antigo: uma sincronizacao de conta pode empilhar dezenas
     * de notificacoes, e nenhuma delas carrega informacao alem de "algo mudou".
     */
    private val invalidacoes = MutableSharedFlow<Unit>(
        extraBufferCapacity = BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val coletor = scope.launch {
        // Invalidacao PREGUICOSA: derruba o conjunto e vai embora. Quem paga a reconstrucao e o
        // proximo aquecimento em background, nunca o observador e nunca uma consulta.
        invalidacoes.debounce(DEBOUNCE_MS).collect { keys = null }
    }

    /**
     * Conjunto pronto, ou `null` enquanto nao houver um.
     *
     * **Jamais** aguarda a construcao: ela foi medida em 1,5–1,8 s com 5.000 contatos, contra um
     * orcamento de 200 ms da decisao de chamada. Devolver `null` e o comportamento correto — quem
     * chama responde pela sonda direta.
     */
    fun get(): Set<String>? = keys

    /** Dispara a construcao em background, no maximo uma por vez. Retorna imediatamente. */
    fun warmInBackground() {
        registrarObservador()
        if (keys != null || !construindo.compareAndSet(false, true)) return
        scope.launch {
            try {
                val construido = runCatching { construir() }.getOrNull()
                if (construido != null) keys = construido
            } finally {
                construindo.set(false)
            }
        }
    }

    private fun registrarObservador() {
        if (observando.compareAndSet(false, true)) {
            source.observeChanges { invalidacoes.tryEmit(Unit) }
        }
    }

    /**
     * Numero cru que nao normaliza e simplesmente descartado: um contato invalido na agenda nao
     * pode derrubar a construcao inteira nem entrar no conjunto como chave torta.
     */
    private fun construir(): Set<String> = buildSet {
        source.allRawNumbers().forEach { raw ->
            (normalizer.normalize(raw) as? NormalizationResult.Valid)?.e164?.let(::add)
        }
    }

    /** Ver KDoc de `close` na fonte: em producao nunca e chamado; existe para os testes. */
    fun close() {
        coletor.cancel()
        source.close()
    }

    companion object {
        /**
         * Debounce e OBRIGATORIO, nao otimizacao. A coalescencia do provider existe mas nao e
         * garantida: medido 51 callbacks para 50 transacoes. Sem ele, cada notificacao custaria
         * uma reconstrucao de ~1,5 s.
         */
        const val DEBOUNCE_MS = 750L

        private const val BUFFER = 64
    }
}

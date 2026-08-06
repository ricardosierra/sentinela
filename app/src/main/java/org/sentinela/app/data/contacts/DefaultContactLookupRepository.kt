package org.sentinela.app.data.contacts

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * Implementacao real do [ContactLookupRepository].
 *
 * A ordem das quatro linhas de [lookup] e o contrato inteiro desta classe:
 *
 *  1. **Permissao primeiro.** Sem ela, `UNAVAILABLE` — jamais `MISS`. Tratar ausencia de permissao
 *     como "nao esta nos contatos" transformaria todo contato conhecido em desconhecido.
 *  2. **Cache quente responde sozinho**, sem tocar o provider.
 *  3. **Aquecimento assincrono**, nunca aguardado.
 *  4. **Sonda direta** como caminho frio, com a falha caindo em `UNAVAILABLE`. A permissao pode ser
 *     revogada entre a verificacao e a consulta — por isso o `runCatching` existe mesmo com a
 *     verificacao explicita, e por isso excecao nunca e usada como detector de permissao.
 */
internal class DefaultContactLookupRepository(
    private val source: ContactNumberSource,
    private val cache: ContactKeyCache,
    private val normalizer: PhoneNumberNormalizer,
    /**
     * Escopo de PROCESSO, deliberadamente não filho de quem chama.
     *
     * É o que torna a sonda abandonável — ver [lookup]. Escopo nulo desliga o abandono e roda a
     * sonda direto na linha, forma que os testes de lógica pura usam.
     */
    private val scope: CoroutineScope? = null,
    private val probeTimeoutMillis: Long = PROBE_TIMEOUT_MILLIS,
) : ContactLookupRepository {

    override suspend fun lookup(numberE164: String): ContactLookup {
        if (!source.hasPermission()) return registrar(ContactLookup.UNAVAILABLE, null)

        cache.get()?.let { chaves ->
            val resultado = if (numberE164 in chaves) ContactLookup.HIT else ContactLookup.MISS
            return registrar(resultado, chaves.size)
        }

        cache.warmInBackground()

        val resultado = sondar(numberE164)
        return registrar(
            when (resultado) {
                true -> ContactLookup.HIT
                false -> ContactLookup.MISS
                null -> ContactLookup.UNAVAILABLE
            },
            null,
        )
    }

    /**
     * Consulta o provider com prazo que REALMENTE vence.
     *
     * O ponto sutil, e o motivo de esta função existir: `source.probe` é uma chamada bloqueante ao
     * provider da agenda (comunicação entre processos mais o banco do provider) e **não tem ponto de
     * suspensão**. Cancelamento de corrotina é cooperativo, então um `withTimeout` em volta dela não
     * interrompe nada — ele só teria efeito quando a chamada já tivesse retornado, que é exatamente
     * quando não é mais preciso. Com o provider travado, o prazo de um segundo da triagem passava
     * direto e a resposta ao sistema de telefonia estourava o limite da plataforma.
     *
     * A saída é a sonda não ser filha de quem espera: ela roda no escopo do processo, e aqui só se
     * espera pelo resultado. Vencido o prazo, o resultado é ABANDONADO — a thread termina sozinha e
     * o valor é descartado —, e a decisão segue com `UNAVAILABLE`, que o motor já sabe tratar.
     *
     * Sem escopo (testes de lógica pura), roda na linha: sem concorrência, não há o que abandonar.
     */
    private suspend fun sondar(numberE164: String): Boolean? {
        val nacional = runCatching { normalizer.nationalDigits(numberE164) }.getOrNull()
        val escopo = scope ?: return runCatching { source.probe(numberE164, nacional) }.getOrNull()

        // `async` no escopo do processo: a falha fica guardada no próprio objeto e o `runCatching`
        // interno garante que ela nunca chegue ao escopo, que é compartilhado com a triagem.
        val sonda = escopo.async { runCatching { source.probe(numberE164, nacional) }.getOrNull() }
        return withTimeoutOrNull(probeTimeoutMillis) { sonda.await() }
    }

    /**
     * Um unico log por consulta, com o resultado e a cardinalidade do conjunto. Nunca o numero,
     * nunca o nome — nem mascarados: o resultado ja e tudo o que o app precisa saber depois.
     */
    private fun registrar(resultado: ContactLookup, chaves: Int?): ContactLookup {
        Log.d(TAG, "consulta a agenda: $resultado, chaves em cache=${chaves ?: "sem cache"}")
        return resultado
    }

    private companion object {
        const val TAG = "ContactLookup"

        /**
         * Prazo da sonda, folgado dentro do prazo de 1 s da triagem: a medição da Fase 4 pôs a
         * consulta direta em p50 de 2 ms e cauda máxima entre 35 e 74 ms com 5.000 contatos, então
         * 300 ms só vence quando o provider está de fato travado — nunca no caminho normal.
         */
        const val PROBE_TIMEOUT_MILLIS = 300L
    }
}

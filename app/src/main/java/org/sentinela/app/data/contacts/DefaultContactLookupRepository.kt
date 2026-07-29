package org.sentinela.app.data.contacts

import android.util.Log
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
) : ContactLookupRepository {

    override suspend fun lookup(numberE164: String): ContactLookup {
        if (!source.hasPermission()) return registrar(ContactLookup.UNAVAILABLE, null)

        cache.get()?.let { chaves ->
            val resultado = if (numberE164 in chaves) ContactLookup.HIT else ContactLookup.MISS
            return registrar(resultado, chaves.size)
        }

        cache.warmInBackground()

        return runCatching { source.probe(numberE164, normalizer.nationalDigits(numberE164)) }
            .fold(
                onSuccess = { registrar(if (it) ContactLookup.HIT else ContactLookup.MISS, null) },
                onFailure = { registrar(ContactLookup.UNAVAILABLE, null) },
            )
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
    }
}

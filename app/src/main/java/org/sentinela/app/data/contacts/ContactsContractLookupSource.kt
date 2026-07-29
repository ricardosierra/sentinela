package org.sentinela.app.data.contacts

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull

/**
 * UNICA classe do app que conhece o provider de contatos do sistema.
 *
 * Fina de proposito: nao decide nada, nao guarda nada, nao formata nada. Quem transforma o
 * resultado em `ContactLookup` e o repositorio; quem guarda chaves e o cache.
 *
 * **Privacidade — regra dura deste arquivo:** nunca projetar nem ler nome de exibicao, foto ou
 * chave de identificacao do contato. Nada de identidade atravessa esta fronteira: as consultas de
 * presenca leem somente a cardinalidade do cursor, e a leitura em lote projeta somente a coluna de
 * numero. Todo log daqui carrega apenas cardinalidade e resultado.
 */
internal class ContactsContractLookupSource(
    context: Context,
) : ContactNumberSource {

    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    private var handlerThread: HandlerThread? = null
    private var observer: ContentObserver? = null

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Sonda dupla. Motivo medido (`04-RESEARCH.md`, emulador API 35): o provider normaliza o
     * numero na ESCRITA usando o pais do aparelho. Uma consulta iniciada por `+` casa so por
     * igualdade do valor normalizado e nao alcanca linha cujo normalizado ficou nulo — o caso do
     * contato gravado em formato nacional estrangeiro. A consulta nacional alcanca essa linha, mas
     * nao alcanca a gravada em E.164 estrangeiro. As duas juntas cobrem a matriz inteira, a ~2 ms
     * cada.
     */
    override fun probe(e164: String, nationalDigits: String?): Boolean {
        val sondas = buildList {
            add(e164)
            if (nationalDigits != null && nationalDigits != e164) add(nationalDigits)
        }
        return sondas.any { casa(it) }
    }

    /**
     * `Uri.encode` e obrigatorio: um numero contendo `#` truncaria a URI no fragment e a consulta
     * viraria outra coisa em silencio.
     *
     * A projecao e exatamente o identificador da linha. O provider devolve tambem uma coluna de
     * numero mesmo sem ser pedida — por isso o codigo NUNCA le indice de coluna aqui. So a
     * contagem importa.
     */
    private fun casa(valor: String): Boolean {
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(valor))
        return resolver.query(uri, arrayOf(PhoneLookup._ID), null, null, null)
            ?.use { it.count > 0 }
            ?: false
    }

    /**
     * Numeros CRUS da agenda, projetando SOMENTE a coluna de numero.
     *
     * PROIBIDO projetar ou ler a coluna de numero normalizado do provider: ela foi medida nula
     * para contato gravado em formato nacional estrangeiro e, pior, silenciosamente ERRADA — um
     * fixo do Rio virou um numero dos EUA num aparelho com chip americano. Um cache construido
     * sobre ela produziria acerto e erro falsos. A normalizacao correta e a do proprio app, feita
     * pelo cache, que de quebra da paridade de chave com a whitelist.
     */
    override fun allRawNumbers(): List<String> {
        val numeros = resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER),
            null,
            null,
            null,
        )?.use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    cursor.getStringOrNull(0)?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
        }.orEmpty()
        Log.d(TAG, "leitura em lote da agenda: ${numeros.size} numeros")
        return numeros
    }

    /**
     * Observa a RAIZ do provider com `notifyForDescendants = true` — unica combinacao que pegou
     * todas as notificacoes na medicao, porque o provider notifica na raiz. Observar uma URI filha
     * com descendentes desligados corre risco real de nunca disparar.
     *
     * O `Handler` vem de um `HandlerThread` dedicado, NUNCA do looper principal: o callback abriria
     * a porta para trabalho de agenda na thread de UI.
     */
    override fun observeChanges(onChange: () -> Unit) {
        if (observer != null) return
        val thread = HandlerThread(THREAD_NAME).also { it.start() }
        handlerThread = thread
        val novo = object : ContentObserver(Handler(thread.looper)) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // Nenhum dado da notificacao e logado: a URI pode conter id de contato.
                onChange()
            }
        }
        observer = novo
        resolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, novo)
    }

    /**
     * Em PRODUCAO nunca e chamado, e isso e proposital, nao esquecimento: o repositorio e
     * singleton de processo e o registro vive no servico de conteudo do sistema, chaveado pelo
     * processo, morrendo junto com ele. Existe para que os testes instrumentados nao vazem
     * observadores de um caso para o outro.
     */
    override fun close() {
        observer?.let { resolver.unregisterContentObserver(it) }
        observer = null
        handlerThread?.quitSafely()
        handlerThread = null
    }

    private companion object {
        const val TAG = "ContactsSource"
        const val THREAD_NAME = "contacts-observer"
    }
}

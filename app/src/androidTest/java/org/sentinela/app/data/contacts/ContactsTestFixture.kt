package org.sentinela.app.data.contacts

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Fixture de contatos para testes instrumentados.
 *
 * Medido na pesquisa da Fase 4: declarar a permissao de gravacao da agenda no manifest de
 * androidTest NAO funciona (a instrumentacao roda no uid do app sob teste) e a
 * GrantPermissionRule tambem nao concede o que o pacote nao declara. A unica rota que
 * funciona e adotar a identidade de shell da instrumentacao — por isso nenhum manifest do
 * repo ganha permissao nova, e este arquivo, em `app/src/androidTest/`, e o unico lugar
 * onde o identificador de escrita da agenda aparece.
 */
object ContactsTestFixture {

    /** Limite conservador de operacoes por applyBatch: lote unico estoura o binder. */
    private const val TAMANHO_LOTE = 300

    private val uiAutomation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    fun adoptShell() {
        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS,
        )
    }

    fun dropShell() {
        uiAutomation.dropShellPermissionIdentity()
    }

    /** Insere um contato com nome e um telefone. */
    fun insert(cr: ContentResolver, displayName: String, number: String) {
        val ops = ArrayList<ContentProviderOperation>(3)
        appendContact(ops, displayName, number)
        cr.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    /**
     * Insere [count] contatos em lotes de [TAMANHO_LOTE] operacoes.
     * Medido: 5.000 contatos custam 7–14 s — nunca use em `@Before` de teste de comportamento.
     */
    fun insertMany(cr: ContentResolver, count: Int, numberAt: (Int) -> String) {
        var ops = ArrayList<ContentProviderOperation>(TAMANHO_LOTE)
        for (i in 0 until count) {
            appendContact(ops, "Contato $i", numberAt(i))
            if (ops.size >= TAMANHO_LOTE) {
                cr.applyBatch(ContactsContract.AUTHORITY, ops)
                ops = ArrayList(TAMANHO_LOTE)
            }
        }
        if (ops.isNotEmpty()) {
            cr.applyBatch(ContactsContract.AUTHORITY, ops)
        }
    }

    /** Apaga todos os raw contacts; as linhas de Data caem em cascata. Devolve a contagem. */
    fun wipe(cr: ContentResolver): Int = cr.delete(RawContacts.CONTENT_URI, null, null)

    private fun appendContact(
        ops: MutableList<ContentProviderOperation>,
        displayName: String,
        number: String,
    ) {
        val base = ops.size
        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build(),
        )
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, base)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, displayName)
                .build(),
        )
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, base)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build(),
        )
    }
}

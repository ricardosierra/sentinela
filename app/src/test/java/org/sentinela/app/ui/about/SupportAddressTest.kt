package org.sentinela.app.ui.about

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R

/**
 * Trava os endereços de doação publicados na tela Sobre.
 *
 * A v0.1.0 publicou um endereço placeholder que ninguém conferia: doação para endereço errado é
 * irreversível, e nenhum outro teste do projeto olhava para essa string. Aqui o checksum é
 * recalculado sobre o valor que vai para o APK — um caractere trocado na hora de colar reprova o
 * build antes de virar dinheiro na carteira de um estranho.
 *
 * O que é verificado em cada endereço: alfabeto, ausência de mistura de maiúscula e minúscula,
 * checksum, prefixo de rede (mainnet) e tamanho do payload.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SupportAddressTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `endereco Bitcoin publicado e um P2WPKH de mainnet com checksum valido`() {
        val address = context.getString(R.string.support_bitcoin_address)
        val decoded = decode(address, Encoding.BECH32)

        assertEquals("prefixo de rede", "bc", decoded.hrp)
        assertEquals("versao de testemunha", 0, decoded.witnessVersion)
        assertEquals("programa de testemunha (P2WPKH = 20 bytes)", 20, decoded.payload.size)
    }

    @Test
    fun `endereco Liquid publicado e um endereco confidencial de mainnet com checksum valido`() {
        val address = context.getString(R.string.support_liquid_address)
        val decoded = decode(address, Encoding.BLECH32)

        assertEquals("prefixo de rede", "lq", decoded.hrp)
        assertEquals("versao de testemunha", 0, decoded.witnessVersion)
        // Endereço confidencial = chave de ofuscação de 33 bytes + programa de 20 bytes.
        assertEquals("chave de ofuscacao + programa", 53, decoded.payload.size)
    }

    @Test
    fun `o placeholder da v0-1-0 nao voltou para a tela`() {
        val placeholder = "1LHayBbJ6chRa3QmZPCGVogzX4uUjspUB8"
        assertNotEquals(placeholder, context.getString(R.string.support_bitcoin_address))
        assertNotEquals(placeholder, context.getString(R.string.support_liquid_address))
    }

    @Test
    fun `um caractere trocado no endereco reprova`() {
        // Prova que o teste morde: o mesmo endereço com um caractere alterado tem de falhar.
        val address = context.getString(R.string.support_bitcoin_address)
        val corrupted = address.dropLast(1) + if (address.last() == 'm') 'n' else 'm'

        val falhou = runCatching { decode(corrupted, Encoding.BECH32) }.isFailure
        assertTrue("checksum corrompido deveria reprovar", falhou)
    }

    // --- bech32 (BIP-173) e blech32 (endereço confidencial da Liquid) ---

    private enum class Encoding(val generator: LongArray, val shift: Int, val checksumChars: Int) {
        BECH32(
            longArrayOf(0x3b6a57b2L, 0x26508e6dL, 0x1ea119faL, 0x3d4233ddL, 0x2a1462b3L),
            shift = 25,
            checksumChars = 6,
        ),
        BLECH32(
            longArrayOf(
                0x7d52fba40bd886L, 0x5e8dbf1a03950cL, 0x1c3a3c74072a18L,
                0x385d72fa0e5139L, 0x7093e5a608865bL,
            ),
            shift = 55,
            checksumChars = 12,
        );

        val mask: Long get() = (1L shl shift) - 1
    }

    private data class Decoded(val hrp: String, val witnessVersion: Int, val payload: ByteArray)

    private fun decode(address: String, encoding: Encoding): Decoded {
        require(address == address.lowercase()) { "endereco com mistura de maiuscula e minuscula" }
        val separator = address.lastIndexOf('1')
        require(separator > 0) { "endereco sem separador" }

        val hrp = address.substring(0, separator)
        val data = address.substring(separator + 1).map {
            val index = CHARSET.indexOf(it)
            require(index >= 0) { "caractere '$it' fora do alfabeto bech32" }
            index
        }
        require(data.size > encoding.checksumChars) { "endereco curto demais" }
        require(polymod(hrpExpand(hrp) + data, encoding) == 1L) { "checksum invalido" }

        val words = data.subList(1, data.size - encoding.checksumChars)
        return Decoded(hrp, data.first(), convertBits(words))
    }

    private fun hrpExpand(hrp: String): List<Int> =
        hrp.map { it.code shr 5 } + 0 + hrp.map { it.code and 31 }

    private fun polymod(values: List<Int>, encoding: Encoding): Long {
        var checksum = 1L
        for (value in values) {
            val top = checksum ushr encoding.shift
            checksum = ((checksum and encoding.mask) shl 5) xor value.toLong()
            for (i in encoding.generator.indices) {
                if ((top ushr i) and 1L == 1L) checksum = checksum xor encoding.generator[i]
            }
        }
        return checksum
    }

    /** Junta os grupos de 5 bits em bytes e recusa sobra de bit diferente de zero. */
    private fun convertBits(words: List<Int>): ByteArray {
        var accumulator = 0
        var bits = 0
        val out = ArrayList<Byte>(words.size)
        for (word in words) {
            accumulator = (accumulator shl 5) or word
            bits += 5
            while (bits >= 8) {
                bits -= 8
                out.add(((accumulator shr bits) and 0xff).toByte())
            }
        }
        require(bits < 5 && (accumulator shl (8 - bits)) and 0xff == 0) { "padding invalido" }
        return out.toByteArray()
    }

    private companion object {
        const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    }
}

package org.sentinela.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R

/**
 * Varredura de honestidade sobre as strings das telas de chamada e discagem
 * (Fase 6).
 *
 * A varredura opera sobre o TEXTO DOS RECURSOS lido em tempo de teste, nunca
 * sobre o arquivo fonte. Isso é deliberado: um critério que casasse o `.xml`
 * cairia por causa de um comentário — a lição registrada nas Fases 3 e 5.
 *
 * `res/` é declarado como entrada das tasks de teste em `app/build.gradle.kts`;
 * sem isso este teste ficaria UP-TO-DATE quando só o arquivo de strings mudasse
 * e daria falso verde.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallStringsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Prefixos das chaves desta fase. Restringir a varredura a elas é
     * necessário: strings de fases anteriores descrevem opções de bloqueio com
     * vocabulário que aqui seria proibido ("Bloqueia todas as chamadas" é o
     * rótulo legítimo de uma política em Proteção).
     */
    private val prefixosDaFase = listOf("call_", "dialpad_", "dialer_")

    /** Expressões proibidas na copy desta fase — dados do teste, em português. */
    private val promessasProibidas = listOf(
        "bloqueio garantido",
        "garantia de bloqueio",
        "100% garantido",
        "cem por cento garantido",
        "bloqueio total",
        "totalmente bloqueado",
        "bloqueio completo",
        "infalível",
        "infalivel",
        "bloqueia tudo",
        "à prova de falhas",
        "nunca mais será incomodado",
    )

    private val superlativosDeMarketing = listOf(
        "o melhor",
        "a melhor",
        "definitivo",
        "poderoso",
        "revolucionário",
        "imbatível",
        "incrível",
    )

    private val pressoesDeUrgencia = listOf(
        "ative agora",
        "ativar agora",
        "recomendado",
        "não perca",
        "última chance",
        "aproveite",
        "agora mesmo",
    )

    private val promessasSobreOHistorico = listOf(
        "esconde a chamada",
        "oculta a chamada",
        "não aparece no histórico",
        "nao aparece no historico",
        "sem registro no histórico",
        "omite o registro",
        "deixa de registrar",
    )

    private val promessasSobreVoip = listOf(
        "bloqueia o whatsapp",
        "filtra o whatsapp",
        "bloqueia chamadas de whatsapp",
        "filtragem de whatsapp",
        "bloqueia chamadas de internet",
        "filtra chamadas de internet",
    )

    private fun stringsDaFase(): Map<String, String> =
        R.string::class.java.fields
            .filter { campo -> prefixosDaFase.any { campo.name.startsWith(it) } }
            .associate { campo ->
                campo.name to context.getString(campo.getInt(null))
            }

    private fun varrer(proibidas: List<String>) {
        stringsDaFase().forEach { (chave, texto) ->
            val minusculo = texto.lowercase()
            proibidas.forEach { expressao ->
                assertFalse(
                    "a string $chave contém a expressão proibida \"$expressao\": $texto",
                    minusculo.contains(expressao),
                )
            }
        }
    }

    @Test
    fun `o contrato de copy desta fase existe por inteiro nos recursos`() {
        val encontradas = stringsDaFase()
        assertTrue(
            "esperado no mínimo 46 chaves desta fase, encontradas ${encontradas.size}",
            encontradas.size >= 46,
        )
    }

    @Test
    fun `nenhuma string desta fase esta vazia`() {
        stringsDaFase().forEach { (chave, texto) ->
            assertTrue("a string $chave está vazia", texto.isNotBlank())
        }
    }

    @Test
    fun `nenhuma string promete bloqueio garantido total ou infalivel`() {
        varrer(promessasProibidas)
    }

    @Test
    fun `nenhuma string promete filtragem de aplicativo de mensagem por internet`() {
        varrer(promessasSobreVoip)
    }

    @Test
    fun `nenhuma string sugere que o registro no historico do telefone deixa de acontecer`() {
        varrer(promessasSobreOHistorico)
    }

    @Test
    fun `nenhuma string usa superlativo de marketing`() {
        varrer(superlativosDeMarketing)
    }

    @Test
    fun `nenhuma string pressiona a ativacao do modo discador`() {
        varrer(pressoesDeUrgencia)
    }

    @Test
    fun `a copy afirma que o registro no historico do telefone continua`() {
        val texto = context.getString(R.string.dialer_activation_unchanged_1)
        assertTrue(
            "o bloco de honestidade precisa afirmar o registro, não negá-lo: $texto",
            texto.contains("continuam sendo registradas"),
        )
        assertTrue(
            "a copy precisa dizer por que o registro não pode ser omitido: $texto",
            texto.contains("apps de operadora"),
        )
    }

    @Test
    fun `a copy coloca chamadas de internet fora do alcance em vez de promete-las`() {
        val texto = context.getString(R.string.dialer_activation_unchanged_3)
        assertTrue(
            "a copy precisa dizer que essas chamadas ficam fora do alcance: $texto",
            texto.contains("fora do alcance"),
        )
    }

    @Test
    fun `a copy afirma que o nao perturbe do sistema continua valendo`() {
        val texto = context.getString(R.string.dialer_activation_unchanged_2)
        assertTrue(
            "o modo não perturbe não é contornável e a copy precisa dizer isso: $texto",
            texto.contains("continua valendo"),
        )
    }

    @Test
    fun `as descricoes de atender e recusar existem e sao distintas`() {
        val atender = context.getString(R.string.call_action_answer_description)
        val recusar = context.getString(R.string.call_action_reject_description)
        val encerrar = context.getString(R.string.call_action_hangup_description)
        assertTrue(atender.isNotBlank() && recusar.isNotBlank() && encerrar.isNotBlank())
        assertNotEquals(atender, recusar)
        assertNotEquals(recusar, encerrar)
        assertNotEquals(atender, encerrar)
    }

    @Test
    fun `cada tecla do teclado tem descricao falada propria`() {
        val descricoes = listOf(
            R.string.dialpad_key_1_description,
            R.string.dialpad_key_2_description,
            R.string.dialpad_key_3_description,
            R.string.dialpad_key_4_description,
            R.string.dialpad_key_5_description,
            R.string.dialpad_key_6_description,
            R.string.dialpad_key_7_description,
            R.string.dialpad_key_8_description,
            R.string.dialpad_key_9_description,
            R.string.dialpad_key_0_description,
            R.string.dialpad_key_star_description,
            R.string.dialpad_key_hash_description,
        ).map(context::getString)
        assertEquals("cada tecla precisa de descrição única", 12, descricoes.toSet().size)
    }

    @Test
    fun `os controles com dois estados anunciam o estado em cada descricao`() {
        val pares = listOf(
            R.string.call_control_mute_off_description to R.string.call_control_mute_on_description,
            R.string.call_control_speaker_off_description to
                R.string.call_control_speaker_on_description,
            R.string.call_control_keypad_closed_description to
                R.string.call_control_keypad_open_description,
        )
        pares.forEach { (desligado, ligado) ->
            assertNotEquals(context.getString(desligado), context.getString(ligado))
        }
    }
}

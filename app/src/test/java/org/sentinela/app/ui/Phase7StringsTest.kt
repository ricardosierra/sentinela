package org.sentinela.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sentinela.app.R

/**
 * Varredura de honestidade sobre as strings das telas de onboarding, home e
 * Proteção (Fase 7).
 *
 * A varredura opera sobre o TEXTO DOS RECURSOS lido em tempo de teste, nunca
 * sobre o arquivo fonte. Isso é deliberado e é a razão de o critério sobreviver:
 * um critério que casasse o `.xml` (ou qualquer fonte) cairia por causa de um
 * comentário ou de um KDoc — inclusive por causa deste próprio arquivo, que
 * precisa citar as frases proibidas para poder proibi-las. É a lição registrada
 * nas Fases 3, 5 e 6.
 *
 * Os mockups entregues afirmam cinco capacidades que o aplicativo não tem: base
 * global com milhões de números, processamento local criptografado, filtros
 * inteligentes, classificação de provável fraude e segurança contra spam
 * conhecido. Elas estão registradas como candidatas a versões futuras em
 * `docs/backlog/capacidades-prometidas-nos-mockups.md`. O que esta classe
 * garante é que nenhuma delas volte pela copy sem um caso ficar vermelho.
 *
 * `res/` é declarado como entrada das tasks de teste em `app/build.gradle.kts`;
 * sem isso este teste ficaria UP-TO-DATE quando só o arquivo de strings mudasse
 * e daria falso verde.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zz")
class Phase7StringsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Prefixos das chaves desta fase. Restringir a varredura a eles é
     * obrigatório: rótulos legítimos de outras fases usam vocabulário que aqui
     * seria proibido.
     */
    private val prefixosDaFase = listOf(
        "welcome_",
        "onboarding_",
        "unknown_",
        "contacts_",
        "dashboard_",
        "settings_",
        "nav_",
        "action_",
        "state_",
    )

    /**
     * Duas expressões verdadeiras e verificáveis no manifest: o aplicativo não
     * declara permissão de rede, e o código é aberto. Elas são removidas do
     * texto ANTES da busca, e nunca a chave inteira é isentada — isentar a chave
     * abriria a porta para uma promessa nova entrar na mesma string.
     */
    private val excecoesVerdadeiras = listOf("100% offline", "100% open source")

    private val promessasDeBloqueio = listOf(
        "bloqueio garantido",
        "garantia de bloqueio",
        "100% garantido",
        "bloqueio total",
        "totalmente bloqueado",
        "bloqueio completo",
        "infalível",
        "infalivel",
        "bloqueia tudo",
        "à prova de falhas",
        "nunca mais será incomodado",
    )

    private val rotulosDeRisco = listOf(
        "fraude",
        "alto risco",
        "spam conhecido",
        "número denunciado",
        "provável fraude",
        "seguro contra spam",
    )

    private val baseDeDadosOuNuvem = listOf(
        "base global",
        "milhões de números",
        "banco de números",
        "na nuvem",
        "servidor",
        "base de dados",
    )

    private val processamentoCifrado = listOf(
        "criptografado",
        "criptografada",
        "encriptado",
    )

    private val filtroInteligenteOuSuperlativo = listOf(
        "filtros inteligentes",
        "filtro inteligente",
        "inteligente",
        "o melhor",
        "a melhor",
        "definitivo",
        "poderoso",
        "revolucionário",
    )

    private val pressaoDeOptIn = listOf(
        "ative agora",
        "ativar agora",
        "recomendado",
        "não perca",
        "urgente",
    )

    /**
     * Isenção NOMINAL da varredura de pressão, e só dela: a política de erro
     * rotula qual das duas alternativas é a mais segura, e isso não é opt-in
     * nenhum — não há nada a ativar ali, e esconder qual delas preserva a
     * chamada do usuário seria pior do que dizê-lo.
     */
    private val isentasDePressao = setOf("settings_fallback_allow")

    /** Aplicativos de mensagem e chamada por internet, que ficam fora do alcance. */
    private val mensageiros = listOf("whatsapp", "telegram", "chamadas de internet")

    private fun textoVarrivel(bruto: String): String {
        var texto = bruto.lowercase()
        excecoesVerdadeiras.forEach { verdade -> texto = texto.replace(verdade, " ") }
        return texto
    }

    private fun stringsDaFase(): Map<String, String> =
        R.string::class.java.fields
            .filter { campo -> prefixosDaFase.any { campo.name.startsWith(it) } }
            .associate { campo -> campo.name to context.getString(campo.getInt(null)) }

    private fun varrer(proibidas: List<String>, isentas: Set<String> = emptySet()) {
        stringsDaFase()
            .filterKeys { it !in isentas }
            .forEach { (chave, bruto) ->
                val texto = textoVarrivel(bruto)
                proibidas.forEach { expressao ->
                    assertFalse(
                        "a string $chave contém a expressão proibida \"$expressao\": $bruto",
                        texto.contains(expressao),
                    )
                }
            }
    }

    @Test
    fun `as chaves desta fase existem por inteiro nos recursos`() {
        val encontradas = stringsDaFase()
        assertTrue(
            "esperado no mínimo 44 chaves de prefixo desta fase, encontradas ${encontradas.size}",
            encontradas.size >= 44,
        )
    }

    @Test
    fun `nenhuma chave desta fase resolve para texto vazio`() {
        stringsDaFase().forEach { (chave, texto) ->
            assertTrue("a string $chave está vazia", texto.isNotBlank())
        }
    }

    @Test
    fun `nenhuma chave promete bloqueio total ou garantido`() {
        varrer(promessasDeBloqueio)
    }

    @Test
    fun `nenhuma chave rotula risco nem classifica spam`() {
        varrer(rotulosDeRisco)
    }

    @Test
    fun `nenhuma chave menciona base de numeros nuvem ou servidor`() {
        varrer(baseDeDadosOuNuvem)
    }

    @Test
    fun `nenhuma chave afirma processamento criptografado`() {
        varrer(processamentoCifrado)
    }

    @Test
    fun `nenhuma chave promete filtro inteligente nem usa superlativo`() {
        varrer(filtroInteligenteOuSuperlativo)
    }

    @Test
    fun `nenhuma chave pressiona o usuario a aceitar um opt-in`() {
        varrer(pressaoDeOptIn, isentasDePressao)
    }

    @Test
    fun `chave que cita mensageiro ou chamada por internet os coloca fora do alcance`() {
        stringsDaFase().forEach { (chave, bruto) ->
            val texto = bruto.lowercase()
            if (mensageiros.any { texto.contains(it) }) {
                assertTrue(
                    "a string $chave cita aplicativo de mensagem sem dizer que ele fica " +
                        "fora do alcance, o que o transforma em promessa: $bruto",
                    texto.contains("não") || texto.contains("fora do alcance"),
                )
            }
        }
    }

    @Test
    fun `a copy que diz cem por cento offline sobrevive a leitura sem argumento`() {
        // O defeito medido nesta fase: o sinal de porcento estava cru e o lint
        // lia "% o" como especificador de formato. A correção é declarativa
        // (formatted="false"), justamente para o texto VISÍVEL não mudar —
        // getString sem argumento não formata, e duplicar o sinal apareceria
        // na tela.
        val texto = context.getString(R.string.dialer_activation_unchanged_4)
        assertTrue("a frase precisa continuar dizendo 100% offline: $texto", texto.contains("100% offline"))
        assertTrue(
            "o sinal de porcento tem de aparecer uma unica vez: $texto",
            texto.count { it == '%' } == 1,
        )
    }

    @Test
    fun `a confirmacao de limpar historico formata a contagem sem lancar`() {
        val umRegistro = context.resources.getQuantityString(
            R.plurals.settings_clear_history_confirm,
            1,
            1,
        )
        val muitos = context.resources.getQuantityString(
            R.plurals.settings_clear_history_confirm,
            7,
            7,
        )
        assertTrue("a forma singular precisa da contagem: $umRegistro", umRegistro.contains("1"))
        assertTrue("a forma plural precisa da contagem: $muitos", muitos.contains("7"))
        assertTrue(
            "a confirmacao precisa avisar que a acao e irreversivel: $muitos",
            muitos.contains("Não é possível desfazer"),
        )
    }
}

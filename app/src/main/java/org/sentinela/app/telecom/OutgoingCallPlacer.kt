package org.sentinela.app.telecom

import android.net.Uri
import android.telecom.TelecomManager
import org.sentinela.app.phone.NormalizationResult
import org.sentinela.app.phone.PhoneNumberNormalizer

/**
 * Resultado de uma tentativa de originar chamada. Nomeado, nunca booleano: a interface precisa
 * distinguir "falta permissão" de "número inválido" de "a plataforma recusou", porque cada um pede
 * uma conduta diferente na tela.
 *
 * Nenhuma variante carrega número, nome ou qualquer dado pessoal — só código interno de motivo,
 * pela mesma regra dos códigos de motivo da decisão.
 */
sealed interface PlaceCallResult {

    /** O pedido foi entregue ao sistema de telefonia. */
    data object Placed : PlaceCallResult

    /** A permissão de originar chamada não está concedida. A tela precisa pedi-la em runtime. */
    data object PermissionMissing : PlaceCallResult

    /** O que o usuário digitou não é um número discável. */
    data object InvalidNumber : PlaceCallResult

    /** O sistema de telefonia recusou ou não existe neste aparelho. */
    data class PlatformFailure(val reason: String) : PlaceCallResult
}

/**
 * Origina a chamada de saída pelo caminho oficial da plataforma.
 *
 * **Por que o gerenciador de telecomunicações e não a ação direta de ligar por intenção.** A
 * pesquisa da fase mediu na fonte do Android: para um discador que não vem instalado no aparelho, a
 * ação direta de ligar é reencaminhada ao discador do sistema para confirmação. O usuário veria a
 * própria discagem sair da mão e reaparecer noutro aplicativo, e o caminho a partir dali não é
 * controlado por este aplicativo. O gerenciador de telecomunicações origina a chamada direto, e é
 * ele quem o invariante 8.3 do script exige. A ação direta é proibida em código de produção.
 *
 * **Chamada de emergência.** É sempre atendida pelo discador que vem no aparelho, mesmo enquanto
 * este aplicativo detém o papel de telefone padrão — o sistema desvia o caminho antes de chegar
 * aqui. Este arquivo não trata emergência, e a interface não promete nada sobre ela, justamente
 * porque a promessa não seria nossa de cumprir.
 *
 * **Falha nunca é exceção.** Ao contrário do núcleo da sessão de chamada (que falha alto de
 * propósito), aqui o usuário está com o dedo no botão: uma exceção mataria o processo e o número
 * digitado se perderia. Toda falha volta como [PlaceCallResult], e a tela mantém o número no campo.
 *
 * @param telecomManager nulo em aparelho sem telefonia — estado legítimo, não erro.
 * @param callPhoneGranted lido no momento do toque, nunca guardado: a permissão pode ser revogada
 * nas Configurações enquanto a tela está aberta.
 */
class OutgoingCallPlacer(
    private val telecomManager: TelecomManager?,
    private val normalizer: PhoneNumberNormalizer,
    private val callPhoneGranted: () -> Boolean,
) {

    /**
     * Normaliza e origina. O número cru vai ao normalizador que já existe no projeto — nenhuma
     * expressão regular nova e nenhum palpite sobre formato.
     */
    fun place(rawNumber: String): PlaceCallResult = when {
        !callPhoneGranted() -> PlaceCallResult.PermissionMissing
        rawNumber.any { it == '*' || it == '#' } -> PlaceCallResult.InvalidNumber
        else -> when (val resultado = normalizer.normalize(rawNumber)) {
            is NormalizationResult.Invalid -> PlaceCallResult.InvalidNumber
            is NormalizationResult.Valid -> discar(resultado.e164)
        }
    }

    /**
     * A recusa da plataforma é capturada por tipo, e não por `runCatching`, por dois motivos que
     * apontam para o mesmo lugar: o lint exige tratamento **explícito** da exceção de segurança
     * (a verificação da permissão chega aqui por função injetada, e nenhuma ferramenta consegue
     * enxergar isso), e a exceção não é registrada de propósito — a mensagem da plataforma pode
     * conter o número, e número completo nunca entra em log.
     */
    @Suppress("SwallowedException")
    private fun discar(destino: String): PlaceCallResult {
        val telecom = telecomManager ?: return PlaceCallResult.PlatformFailure(SEM_TELEFONIA)
        val endereco = Uri.fromParts(ESQUEMA_TELEFONE, destino, null)
        return try {
            telecom.placeCall(endereco, null)
            PlaceCallResult.Placed
        } catch (recusa: SecurityException) {
            PlaceCallResult.PlatformFailure(FALHA_DA_PLATAFORMA)
        } catch (indisponivel: IllegalStateException) {
            PlaceCallResult.PlatformFailure(FALHA_DA_PLATAFORMA)
        }
    }

    companion object {
        /** Esquema do endereço de discagem. */
        const val ESQUEMA_TELEFONE = "tel"

        /** Aparelho sem rádio: o modo discador é indisponível por definição. */
        const val SEM_TELEFONIA = "sem_telefonia"

        /** O sistema de telefonia recusou o pedido. Sem detalhe, para não vazar contexto. */
        const val FALHA_DA_PLATAFORMA = "falha_da_plataforma"
    }
}

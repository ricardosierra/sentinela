package org.sentinela.app.phone

import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber

/**
 * Fabrica pura do [PhoneNumberUtil].
 *
 * PROIBIDO criar o util a partir de um `Context` dentro de `phone/`: importaria
 * `android.content.Context`, quebraria a pureza do pacote e exigiria Robolectric no teste.
 * PROIBIDO singleton global: a construcao carrega metadados (dezenas de ms) e a instancia unica
 * e criada e guardada pelo `AppContainer` — nunca dentro de `onScreenCall` (p95 < 200 ms).
 */
fun phoneNumberUtil(loader: MetadataLoader): PhoneNumberUtil =
    PhoneNumberUtil.createInstance(loader)

/**
 * Implementacao real do [PhoneNumberNormalizer] sobre libphonenumber-android.
 *
 * Regra estrutural: `parse()` ter sucesso NUNCA e criterio de validade — medido, `987654321`/BR
 * faz parse e devolve `+55987654321` com `isValid=false`. O gate e sempre `isValidNumber`.
 */
class LibPhoneNumberNormalizer(
    private val util: PhoneNumberUtil,
    private val regionProvider: RegionProvider,
) : PhoneNumberNormalizer {

    override fun normalize(raw: String, region: String?): NormalizationResult {
        // (1) Codigo curto ANTES de qualquer parse: `190`/BR viraria `+55190`, um E.164 falso.
        codigoCurto(raw)?.let { return it }

        val regiao = region
            ?: regionProvider.currentRegion()
            ?: CascadingRegionProvider.DEFAULT_REGION

        val parsed = try {
            util.parse(raw, regiao)
        } catch (e: NumberParseException) {
            return NormalizationResult.Invalid(e.errorType.toReasonCode())
        }

        // (2) O gate e isValidNumber, nunca o sucesso do parse.
        if (util.isValidNumber(parsed)) {
            return NormalizationResult.Valid(
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164),
            )
        }

        // (3) So agora a regra brasileira do 9o digito, com revalidacao obrigatoria.
        corrigirNonoDigitoBr(parsed)?.let { return it }

        return NormalizationResult.Invalid(motivoDeInvalido(parsed, regiao))
    }

    override fun mask(e164: String): String = PhoneMask.mask(util, e164)

    /**
     * Codigos curtos (190, 911) nao tem E.164: `190`/BR vira `+55190`, isValid=false, TOO_SHORT,
     * e `ShortNumberInfo` e inconstruivel no port -android (construtor package-private,
     * confirmado por `javap` na pesquisa) — por isso a regra e feita aqui, a mao.
     * Chave = digitos crus, a mesma que o usuario digitou e consegue pos na whitelist.
     * Limiar compartilhado com a mascara: [PhoneNumbers.LIMIAR_CURTO], com o MESMO operador `<`.
     *
     * @return `Valid` com os digitos crus, ou `null` quando nao e codigo curto.
     */
    private fun codigoCurto(raw: String): NormalizationResult? {
        // normalizeDigitsOnly (e nao filter { isDigit() }) para tratar digitos nao-ASCII.
        val digitos = PhoneNumberUtil.normalizeDigitsOnly(raw)
        if (digitos.isEmpty() || digitos.length >= PhoneNumbers.LIMIAR_CURTO) return null
        // So digitos e separadores: `+55` nao pode virar a chave curta "55".
        if (raw.any { it.isLetter() || it == '+' }) return null
        return NormalizationResult.Valid(digitos)
    }

    /**
     * Insere o 9 em celular BR antigo de 8 digitos. Existe porque os metadados BR atuais do
     * libphonenumber removeram o padrao de 8 digitos: `+55 11 8765-4321` retorna
     * isValid=false / type=UNKNOWN (medido em 2026-07-29). Guarda-corpo obrigatorio: o
     * resultado corrigido so e aceito se libphonenumber revalidar como MOBILE — sem isso a
     * regra transformaria lixo em numero "valido" e envenenaria a whitelist.
     *
     * @return `Valid` corrigido, `Invalid("nono_digito_nao_revalida")` quando a regra se aplicava
     *   mas a correcao nao revalidou, ou `null` quando a regra nao se aplica.
     */
    private fun corrigirNonoDigitoBr(parsed: PhoneNumber): NormalizationResult? {
        if (parsed.countryCode != DDI_BR) return null
        val nsn = util.getNationalSignificantNumber(parsed)
        if (nsn.length != NSN_BR_SEM_NONO) return null
        // Celular antigo comecava em 6..9; assinante iniciando em 2..5 e fixo e nao se corrige.
        if (nsn[DDD_LEN] !in PRIMEIRO_DIGITO_CELULAR) return null

        val candidato = "${nsn.take(DDD_LEN)}9${nsn.substring(DDD_LEN)}"
        val revalidado = runCatching { util.parse(candidato, CascadingRegionProvider.DEFAULT_REGION) }
            .getOrNull()
        val aceito = revalidado != null &&
            util.isValidNumber(revalidado) &&
            util.getNumberType(revalidado) == PhoneNumberUtil.PhoneNumberType.MOBILE

        return if (aceito) {
            NormalizationResult.Valid(
                util.format(revalidado, PhoneNumberUtil.PhoneNumberFormat.E164),
            )
        } else {
            NormalizationResult.Invalid("nono_digito_nao_revalida")
        }
    }

    /**
     * Razao de recusa como codigo interno `[a-z_]+`. NUNCA embutir o numero: `reason` vai para
     * log. `sem_ddd` e distinguido porque e o erro que o usuario consegue corrigir sozinho.
     */
    private fun motivoDeInvalido(
        parsed: PhoneNumber,
        regiao: String,
    ): String {
        val nsn = util.getNationalSignificantNumber(parsed)
        val brSemDdd = regiao == CascadingRegionProvider.DEFAULT_REGION &&
            parsed.countryCode == DDI_BR &&
            nsn.length < NSN_BR_MINIMO
        return if (brSemDdd) "sem_ddd" else "invalido"
    }

    private fun NumberParseException.ErrorType.toReasonCode(): String = when (this) {
        NumberParseException.ErrorType.NOT_A_NUMBER -> "nao_e_numero"
        NumberParseException.ErrorType.INVALID_COUNTRY_CODE -> "ddi_invalido"
        else -> "invalido"
    }

    private companion object {
        const val DDI_BR = 55

        /** Menor NSN brasileiro completo (DDD + 8 digitos). Abaixo disso, falta o DDD. */
        const val NSN_BR_MINIMO = 10

        /** DDD + 8 digitos: o formato do celular antigo, anterior ao 9o digito. */
        const val NSN_BR_SEM_NONO = 10

        const val DDD_LEN = 2

        /** Faixa do primeiro digito do assinante em celular BR antigo. */
        val PRIMEIRO_DIGITO_CELULAR = '6'..'9'
    }
}

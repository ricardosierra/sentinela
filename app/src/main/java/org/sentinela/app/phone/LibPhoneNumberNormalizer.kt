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
        val regiao = region
            ?: regionProvider.currentRegion()
            ?: CascadingRegionProvider.DEFAULT_REGION

        val parsed = try {
            util.parse(raw, regiao)
        } catch (e: NumberParseException) {
            return NormalizationResult.Invalid(e.errorType.toReasonCode())
        }

        if (util.isValidNumber(parsed)) {
            return NormalizationResult.Valid(
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164),
            )
        }

        // TODO(Task 3): antes de desistir, tentar corrigirNonoDigitoBr(parsed).
        return NormalizationResult.Invalid(motivoDeInvalido(parsed, regiao))
    }

    override fun mask(e164: String): String = PhoneMask.mask(util, e164)

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
    }
}

package org.sentinela.app.phone

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

/**
 * Mascara UNICA de exibicao — serve log e UI (decisao do usuario em 02-CONTEXT.md).
 * Duas implementacoes divergiriam e uma delas acabaria vazando o numero.
 *
 * Forma canonica do CLAUDE.md, generalizada para qualquer DDI via metadados:
 * `+DDI DDD <primeiro digito>****-<ultimos 4>` — ex.: `+55 11 9****-1234`.
 *
 * Invariantes:
 * - NUNCA lanca. A mascara roda em caminho de log e uma excecao ali derruba o Service.
 * - NUNCA ecoa a entrada crua quando nao consegue interpretar: devolve [MASCARA_GENERICA].
 * - Numero com menos de [PhoneNumbers.LIMIAR_CURTO] digitos e exibido na integra
 *   (numero publico de servico nao e dado pessoal).
 */
object PhoneMask {

    /** Devolvida quando a entrada nao e interpretavel. Nunca contem dado da entrada. */
    const val MASCARA_GENERICA = "+** ****"

    private const val ULTIMOS = 4
    private const val MIN_RESTO = 5

    fun mask(util: PhoneNumberUtil, value: String): String = runCatching {
        // 1. Codigo curto ANTES do parse: `parse("190", null)` lanca INVALID_COUNTRY_CODE e
        //    cairia na mascara generica, escondendo um numero que o usuario precisa ver.
        val digitosCrus = PhoneNumberUtil.normalizeDigitsOnly(value)
        if (digitosCrus.isEmpty()) return@runCatching MASCARA_GENERICA
        if (digitosCrus.length < PhoneNumbers.LIMIAR_CURTO) return@runCatching digitosCrus

        val parsed = runCatching { util.parse(value, null) }.getOrNull()
            ?: return@runCatching MASCARA_GENERICA
        val digitos = util.getNationalSignificantNumber(parsed)
        if (digitos.isEmpty()) return@runCatching MASCARA_GENERICA

        val degradada = "+${parsed.countryCode} ****-${digitos.takeLast(ULTIMOS)}"
        // ndc == 0 em qualquer numero invalido (medido): sem area confiavel, forma degradada.
        val ndc = util.getLengthOfNationalDestinationCode(parsed)
        if (ndc == 0 || ndc >= digitos.length) return@runCatching degradada

        val resto = digitos.substring(ndc)
        // TODO: com resto de exatamente 5 digitos a mascara nao esconde nada — `first()` mostra o
        //  digito 1 e `takeLast(4)` mostra os digitos 2 a 5, ou seja o numero nacional INTEIRO, com
        //  os asteriscos servindo so de enfeite. O piso precisa ser ULTIMOS + 2 (6 digitos) para
        //  sobrar ao menos um digito coberto. Vale para log e UI, entao e vazamento de dado pessoal.
        if (resto.length < MIN_RESTO) return@runCatching degradada

        "+${parsed.countryCode} ${digitos.substring(0, ndc)} " +
            "${resto.first()}****-${resto.takeLast(ULTIMOS)}"
    }.getOrDefault(MASCARA_GENERICA)
}

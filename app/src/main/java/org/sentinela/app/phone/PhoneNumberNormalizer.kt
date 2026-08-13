package org.sentinela.app.phone

/**
 * Normalização para E.164 com libphonenumber-android (Fase 2).
 * Regras BR: DDI +55, DDD obrigatório, celular com 9 dígitos, fixo sem 9.
 * Formatação bonita é só visual — E.164 é a fonte de verdade.
 */
interface PhoneNumberNormalizer {

    /**
     * Normaliza [raw] para a chave canônica.
     *
     * @param region região ISO-3166-1 alpha-2 usada quando o número vem sem DDI. Quando `null`,
     *   a resolução é delegada ao `RegionProvider` injetado (aparelho → usuário → BR). Não
     *   travar em `"BR"`: o app precisa funcionar fora do Brasil.
     */
    fun normalize(raw: String, region: String? = null): NormalizationResult

    /**
     * Dígitos do número nacional significativo (DDD + número, sem DDI e sem `+`), ou `null`
     * quando [e164] não parseia ou é código curto.
     *
     * Existe para a SEGUNDA sonda da consulta à agenda (Fase 4): um contato gravado em formato
     * nacional de outra região fica com o valor normalizado do provider nulo e **não** é
     * alcançado por uma consulta iniciada com `+` — medido em `04-RESEARCH.md`. Nunca lança.
     */
    fun nationalDigits(e164: String): String?

    /** Máscara segura para exibição/log (ex.: +55 11 9****-1234). Nunca lança. */
    fun mask(e164: String): String

    /** Formata o número em Padrão Internacional. Nunca lança. */
    fun formatInternational(e164: String): String
}

sealed interface NormalizationResult {

    /**
     * Chave canônica do número.
     *
     * [e164] é E.164 (`+5511987654321`) **exceto para códigos curtos** — valores com menos de
     * [PhoneNumbers.LIMIAR_CURTO] dígitos (`190`, `911`), cujo valor são os **dígitos crus**.
     * Códigos curtos não têm E.164: `190`/BR produziria `+55190`, inválido e falso.
     * Este é o contrato de dados que a Fase 3 persiste na whitelist e no histórico.
     */
    data class Valid(val e164: String) : NormalizationResult

    /** [reason] é código interno `[a-z_]+`, usado em log — nunca contém o número. */
    data class Invalid(val reason: String) : NormalizationResult
}

package org.sentinela.app.phone

/** Constantes compartilhadas por normalizacao e mascara. Fonte unica — nao duplicar. */
object PhoneNumbers {

    /**
     * Abaixo deste numero de digitos o valor e tratado como codigo curto de servico
     * (190, 911): a chave e o digito cru, nao um E.164, e a exibicao e integral.
     * Decisao do usuario (02-CONTEXT.md): "essas mascaras nao podem atrapalhar o usuario" —
     * numero publico de servico nao e dado pessoal.
     *
     * A comparacao e SEMPRE estritamente menor (`<`), nos dois lados: `PhoneMask.mask` e
     * `LibPhoneNumberNormalizer.codigoCurto`. A constante so e fonte unica se o operador
     * tambem for o mesmo — `"123456"` (6 digitos) nao e curto em lugar nenhum.
     */
    const val LIMIAR_CURTO = 6
}

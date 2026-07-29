package org.sentinela.app.phone

/**
 * Cascata de resolucao da regiao padrao, na ordem decidida com o usuario:
 *
 * 1. **Aparelho** — SIM e, na falta dele, a rede (ver `platform/AndroidRegionProvider`).
 * 2. **Preferencia do usuario** — DDI/DDD informado nas configuracoes. Nesta fase existe
 *    apenas o contrato e um valor em memoria; a persistencia (DataStore) entra na Fase 3 e a
 *    tela que coleta o dado, na Fase 7.
 * 3. **[fallback]** — `[DEFAULT_REGION]` por padrao, para nunca quebrar em aparelho sem SIM.
 *
 * Travar em `"BR"` quebraria a normalizacao de qualquer usuario fora do Brasil, por isso o
 * fallback e o ultimo degrau e nao o primeiro.
 *
 * A classe e pura e deterministica: nao toca plataforma, nao captura excecao e nao devolve null.
 * Cada degrau so e aceito se render exatamente duas letras (apos `trim`), ja normalizadas para
 * maiusculo — o Android entrega a regiao em minusculo e o libphonenumber exige maiusculo.
 */
class CascadingRegionProvider(
    private val device: RegionProvider,
    private val userPreference: RegionProvider,
    private val fallback: String = DEFAULT_REGION,
) : RegionProvider {

    override fun currentRegion(): String =
        device.currentRegion()?.asRegionCode()
            ?: userPreference.currentRegion()?.asRegionCode()
            ?: fallback

    private fun String.asRegionCode(): String? =
        trim().uppercase().takeIf { it.length == REGION_CODE_LENGTH && it.all(Char::isLetter) }

    companion object {
        /** Ultimo recurso quando nem o aparelho nem o usuario informam a regiao. */
        const val DEFAULT_REGION = "BR"

        private const val REGION_CODE_LENGTH = 2
    }
}

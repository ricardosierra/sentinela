package org.sentinela.app.platform

import android.telephony.TelephonyManager
import org.sentinela.app.phone.RegionProvider

/**
 * Le a regiao do aparelho (SIM, depois rede). Unico arquivo do app que toca TelephonyManager.
 *
 * Nao exige permissao: no AOSP (android-15.0.0_r1) os getters sem argumento de simCountryIso e
 * networkCountryIso
 * carregam apenas @RequiresFeature — nenhum @RequiresPermission. READ_PHONE_STATE continua
 * proibida por docs/PERMISSOES.md. Usamos sempre as sobrecargas SEM argumento: a variante
 * variante que recebe um subId esta depreciada desde a API 30 e exige permissao em alguns forks.
 *
 * runCatching e obrigatorio: em aparelho sem FEATURE_TELEPHONY_* os metodos lancam
 * UnsupportedOperationException (tablet Wi-Fi-only). Ambos tambem devolvem string VAZIA,
 * nao null, quando indisponiveis — a validacao de formato fica no CascadingRegionProvider.
 */
class AndroidRegionProvider(
    private val telephonyManager: TelephonyManager?,
) : RegionProvider {

    override fun currentRegion(): String? = runCatching {
        telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

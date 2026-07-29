package org.sentinela.app

import android.content.Context
import android.telephony.TelephonyManager
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.platform.AndroidRegionProvider
import org.sentinela.app.platform.assetsPhoneMetadataLoader

/**
 * Container de dependências manual. As implementações reais (Room, DataStore,
 * notifier) entram nas Fases 3–5 do roadmap; aqui ficam apenas as fábricas para
 * o Service e a UI não instanciarem nada por conta própria.
 */
class AppContainer(
    private val appContext: Context,
) {

    val decisionEngine: CallDecisionEngine by lazy { CallDecisionEngine() }

    private val regionProvider: RegionProvider by lazy {
        CascadingRegionProvider(
            device = AndroidRegionProvider(
                appContext.getSystemService(TelephonyManager::class.java),
            ),
            // Degrau 2 (preferência do usuário) só ganha persistência na Fase 3;
            // aqui o contrato existe com fallback em memória.
            userPreference = RegionProvider { null },
        )
    }

    /**
     * Instância única. `createInstance` desserializa metadados (dezenas de ms): construir aqui,
     * NUNCA dentro de `onScreenCall` — a Fase 5 tem orçamento p95 < 200 ms.
     */
    val phoneNumberNormalizer: PhoneNumberNormalizer by lazy {
        LibPhoneNumberNormalizer(
            util = phoneNumberUtil(assetsPhoneMetadataLoader(appContext)),
            regionProvider = regionProvider,
        )
    }

    // TODO(Fase 3): settingsRepository (DataStore), whitelistRepository,
    //  blockedCallRepository (Room) e contador de aberturas — via interface.
    // TODO(Fase 4): contactLookupRepository (READ_CONTACTS + cache em memória).
    // TODO(Fase 5): blockedCallNotifier (canal silencioso).
    // TODO(Fase 6): componentes do modo discador (InCallService/ROLE_DIALER).
}

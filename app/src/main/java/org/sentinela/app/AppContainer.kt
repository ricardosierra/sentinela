package org.sentinela.app

import android.content.Context
import org.sentinela.app.domain.CallDecisionEngine

/**
 * Container de dependências manual. As implementações reais (Room, DataStore,
 * libphonenumber, notifier) entram nas Fases 2–4 do roadmap; aqui ficam apenas
 * as fábricas para o Service e a UI não instanciarem nada por conta própria.
 */
class AppContainer(
    // Consumido a partir da Fase 3 (DataStore/Room precisam de Context)
    @Suppress("UnusedPrivateProperty")
    private val appContext: Context,
) {

    val decisionEngine: CallDecisionEngine by lazy { CallDecisionEngine() }

    // TODO(Fase 2): phoneNumberNormalizer (libphonenumber-android).
    // TODO(Fase 3): settingsRepository (DataStore), whitelistRepository,
    //  blockedCallRepository (Room) e contador de aberturas — via interface.
    // TODO(Fase 4): contactLookupRepository (READ_CONTACTS + cache em memória).
    // TODO(Fase 5): blockedCallNotifier (canal silencioso).
    // TODO(Fase 6): componentes do modo discador (InCallService/ROLE_DIALER).
}

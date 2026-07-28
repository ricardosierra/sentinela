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

    // TODO(Fase 3): settingsRepository (DataStore), whitelistRepository e
    //  blockedCallRepository (Room) — expostos por interface.
    // TODO(Fase 2): phoneNumberNormalizer (libphonenumber-android).
    // TODO(Fase 4): blockedCallNotifier (canal silencioso).
}

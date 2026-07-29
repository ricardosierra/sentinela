package org.sentinela.app.telecom

import android.telecom.CallScreeningService.CallResponse
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.settings.ScreeningSettings

/** Stub da fase RED — implementacao entra no passo GREEN. */
class CallResponseFactory {

    fun toResponse(decision: CallDecision, settings: ScreeningSettings): CallResponse =
        TODO("GREEN")
}

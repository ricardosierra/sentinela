package org.sentinela.app.telecom

import android.telecom.Call
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.phone.PhoneNumberNormalizer

/** Stub da fase RED — implementacao entra no passo GREEN. */
class ScreenedCallFactory(private val normalizer: PhoneNumberNormalizer) {

    fun from(details: Call.Details): ScreenedCall = TODO("GREEN")
}

package org.sentinela.app.telecom

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Camada fina sobre o Telecom, com dois modos de operação (docs/ARQUITETURA.md):
 *  - Modo filtro (padrão): sem ser o discador padrão, onScreenCall() só recebe
 *    números FORA da agenda — o Service passa ContactLookup.MISS ao motor.
 *  - Modo discador (opcional, Fase 6): com ROLE_DIALER, todas as chamadas passam
 *    por aqui e o ContactLookupRepository decide HIT/MISS/UNAVAILABLE.
 *
 * Invariantes desta classe (não relaxar):
 *  - respondToCall é chamado exatamente uma vez, em todos os caminhos.
 *  - A resposta sai muito antes do limite de 5 s da plataforma (budget p95 < 200 ms).
 *  - Nenhuma consulta fora do processo/dados locais.
 *  - Notificação/histórico próprios só DEPOIS de responder ao sistema.
 */
class UnknownCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Esqueleto (Fase 5 do roadmap): até o motor estar ligado ao Service,
        // a política segura é não interferir em nenhuma chamada.
        respondToCall(callDetails, CallResponse.Builder().build())

        // TODO(Fase 5): montar ScreenedCall (direção via callDetails.callDirection,
        //  normalização via PhoneNumberNormalizer, privado quando handle == null),
        //  consultar SettingsRepository + ContactLookupRepository +
        //  PersonalWhitelistRepository com timeout interno, decidir via
        //  CallDecisionEngine e traduzir CallDecision para CallResponse
        //  (disallow/reject/silence/skipCallLog/skipNotification), com guarda de
        //  resposta única e fallback em qualquer exceção.
    }
}

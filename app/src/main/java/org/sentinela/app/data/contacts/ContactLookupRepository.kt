package org.sentinela.app.data.contacts

import org.sentinela.app.domain.ContactLookup

/**
 * Consulta local aos contatos do aparelho (Fase 4). Regras:
 *  - READ_CONTACTS solicitada em runtime com explicação clara; sem permissão → UNAVAILABLE.
 *  - Lookup por E.164 com cache em memória invalidado por ContentObserver —
 *    precisa caber no orçamento de p95 < 200 ms da decisão.
 *  - Nome e dados do contato NUNCA são persistidos nem saem do processo;
 *    o motor só recebe HIT/MISS/UNAVAILABLE.
 */
interface ContactLookupRepository {

    suspend fun lookup(numberE164: String): ContactLookup
}

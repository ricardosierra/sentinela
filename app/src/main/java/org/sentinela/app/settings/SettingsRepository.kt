package org.sentinela.app.settings

import kotlinx.coroutines.flow.Flow

/**
 * Fonte das configurações de triagem. Implementação com DataStore Preferences
 * na Fase 3; o snapshot precisa ser rápido o bastante para o orçamento de
 * p95 < 200 ms do Service.
 */
interface SettingsRepository {

    val settings: Flow<ScreeningSettings>

    /** Leitura pontual usada pelo CallScreeningService (warm path em memória). */
    suspend fun snapshot(): ScreeningSettings

    suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings)
}

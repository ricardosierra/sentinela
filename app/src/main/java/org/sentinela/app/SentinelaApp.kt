package org.sentinela.app

import android.app.Application

/**
 * DI manual (sem Hilt/Koin): o container é criado sob demanda para não
 * penalizar o cold start do CallScreeningService.
 */
class SentinelaApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Não-bloqueante: `onAppOpened` só lança no escopo de IO. `onCreate` roda na
        // main thread e define o cold start do CallScreeningService — I/O síncrono
        // aqui sairia do orçamento de resposta ao Telecom (Fase 5).
        container.onAppOpened()
    }
}

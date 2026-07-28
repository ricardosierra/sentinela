package org.sentinela.app

import android.app.Application

/**
 * DI manual (sem Hilt/Koin): o container é criado sob demanda para não
 * penalizar o cold start do CallScreeningService.
 */
class SentinelaApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

package org.sentinela.app.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch
import org.sentinela.app.AppContainer

class AboutViewModel(
    private val clearDataFn: suspend () -> Unit
) : ViewModel() {

    // TODO: `viewModelScope` roda em Dispatchers.Main e `clearDataFn` cai em
    //  `SentinelaDatabase.clearAllTables()`, que e bloqueante — e o banco proibe acesso na thread
    //  principal, entao lanca IllegalStateException. Como nao ha try/catch, apagar os dados derruba o
    //  app e `onComplete()` nunca roda; o usuario fica sem apagar nada e sem aviso. Falta
    //  `withContext(Dispatchers.IO)` na origem e tratamento de erro visivel nesta acao irreversivel.
    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            clearDataFn()
            onComplete()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                AboutViewModel(
                    clearDataFn = { container.clearAllData() }
                ) as T
        }
    }
}

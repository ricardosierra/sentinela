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

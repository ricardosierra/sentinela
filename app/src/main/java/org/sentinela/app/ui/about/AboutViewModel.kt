package org.sentinela.app.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.sentinela.app.AppContainer

class AboutViewModel(
    private val clearDataFn: suspend () -> Unit
) : ViewModel() {

    /**
     * Apagar tudo é irreversível e pode falhar (disco cheio, banco ocupado). A falha silenciosa era
     * a pior saída possível: o usuário mandava apagar, a tela voltava ao início e ele acreditava que
     * os dados tinham sumido. Por isso o resultado sai daqui e quem chama decide o que mostrar —
     * navegar só faz sentido quando realmente apagou.
     *
     * A captura ampla é deliberada e o cancelamento é repassado: engolir `CancellationException`
     * quebraria o encerramento do escopo do ViewModel.
     */
    // A exceção não é registrada de propósito: este projeto não escreve log de erro em release e
    // o que o usuário precisa saber já sobe pelo `onResult(false)`, que vira aviso na tela.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun clearAllData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                clearDataFn()
                onResult(true)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Throwable) {
                onResult(false)
            }
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

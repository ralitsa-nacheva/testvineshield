package com.rncoding.testvineshield.core.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppStartupViewModel(
    private val appInitializer: AppInitializer
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppStartupUiState>(AppStartupUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var initializationJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (initializationJob?.isActive == true || _uiState.value == AppStartupUiState.Ready) {
            return
        }
        traceStartup("INITIALIZER launch requested")
        _uiState.value = AppStartupUiState.Loading
        initializationJob = viewModelScope.launch {
            try {
                traceStartup("INITIALIZER begin")
                appInitializer.ensureInitialized()
                traceStartup("INITIALIZER returned")
                currentCoroutineContext().ensureActive()
                _uiState.value = AppStartupUiState.Ready
            } catch (exception: CancellationException) {
                traceStartup("INITIALIZER cancelled")
                throw exception
            } catch (exception: Exception) {
                traceStartup("INITIALIZER failed type=" + (exception::class.simpleName ?: "Exception"))
                currentCoroutineContext().ensureActive()
                // Display a safe message; never expose database exception text.
                _uiState.value = AppStartupUiState.Failed
            }
        }
    }
}

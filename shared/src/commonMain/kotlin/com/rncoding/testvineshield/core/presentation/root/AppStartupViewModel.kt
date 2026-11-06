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
        _uiState.value = AppStartupUiState.Loading
        initializationJob = viewModelScope.launch {
            try {
                appInitializer.ensureInitialized()
                currentCoroutineContext().ensureActive()
                _uiState.value = AppStartupUiState.Ready
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                // Display a safe message; never expose database exception text.
                _uiState.value = AppStartupUiState.Failed
            }
        }
    }
}

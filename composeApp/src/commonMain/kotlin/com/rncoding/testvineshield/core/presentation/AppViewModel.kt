package com.rncoding.testvineshield.core.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.domain.usecases.user.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeAuthState: ObserveAuthStateUseCase
) : ViewModel() {
    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set
    init {
        observeAuth()
    }
    private fun observeAuth() {
        viewModelScope.launch {
            observeAuthState()
                .onStart {
                    authState = AuthState.Loading
                }
                .catch { e ->
                    authState = AuthState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    authState = state
                }
        }
    }
}
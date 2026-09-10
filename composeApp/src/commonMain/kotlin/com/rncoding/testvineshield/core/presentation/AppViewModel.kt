package com.rncoding.testvineshield.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Application-level ViewModel.
 *
 * Its responsibility is to expose global authentication state
 * to the Compose UI.
 */
class AppViewModel(
    private val observeAuthState: ObserveAuthStateUseCase
) : ViewModel() {

    var authState by mutableStateOf<AuthState>(
        AuthState.Loading
    )
        private set

    init {
        observeAuth()
    }

    /**
     * Starts observing global authentication state.
     */
    private fun observeAuth() {

        viewModelScope.launch {

            observeAuthState()
                .onStart {

                    authState =
                        AuthState.Loading
                }
                .catch { throwable ->

                    authState =
                        AuthState.Error(
                            error = UnexpectedError(
                                cause = throwable
                            )
                        )
                }
                .collectLatest { state ->

                    authState = state
                }
        }
    }
}
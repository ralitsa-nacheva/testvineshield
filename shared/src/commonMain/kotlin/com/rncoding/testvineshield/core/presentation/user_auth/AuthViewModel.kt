package com.rncoding.testvineshield.core.presentation.user_auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthSessionCoordinator
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.ObserveAuthStateUseCase
import com.rncoding.testvineshield.core.domain.error.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import com.rncoding.testvineshield.core.domain.security.*
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockRequest
import kotlin.time.Duration.Companion.seconds


class AuthViewModel(
    private val authSessionCoordinator: AuthSessionCoordinator,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val sessionManager: SessionManager,
    private val securitySettingsRepository: SecuritySettingsRepository,
    private val clock: AppClock,
    private val localAuthenticator: LocalAuthenticator
) : ViewModel() {

    // ----------------------------------------------------
    // Global authentication state
    // ----------------------------------------------------

    val authState: StateFlow<AuthState> =
        observeAuthStateUseCase()

    // ----------------------------------------------------
    // Authentication screen UI state
    // ----------------------------------------------------

    private val _uiState =
        MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    private val _events =
        Channel<AuthUiEvent>(
            capacity = Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()


    // ----------------------------------------------------
    // Initialization
    // ----------------------------------------------------

    private val foreground = MutableStateFlow(false)
    private var lastInteraction = 0L
    private var lastPersistedInteraction = 0L
    private var policy = SecuritySettings()
    private var expiresAt = Long.MAX_VALUE
    private val _unlockOptions = MutableStateFlow(UnlockOptions())
    val unlockOptions = _unlockOptions.asStateFlow()

    fun setForeground(value: Boolean) { foreground.value = value }

    fun recordInteraction() {
        if (!foreground.value || authState.value !is AuthState.Authenticated) return
        val now = clock.nowMillis()
        // An event arriving after the deadline cannot silently unlock the session.
        if (now >= expiresAt || (policy.inactivityLockEnabled && (now < lastInteraction || now - lastInteraction >= policy.inactivityTimeoutMillis))) {
            viewModelScope.launch { authSessionCoordinator.checkSession() }
            return
        }
        lastInteraction = now
        if (now - lastPersistedInteraction < 1000) return
        lastPersistedInteraction = now
        viewModelScope.launch {
            when (sessionManager.recordActivity(now)) {
                is Result.Error -> authSessionCoordinator.checkSession()
                is Result.Success -> Unit
            }
        }
    }

    fun refreshUnlockOptions() {
        _unlockOptions.value = UnlockOptions()
        if (authState.value !is AuthState.Locked) return
        viewModelScope.launch {
            try {
                val session = (sessionManager.getSession() as? Result.Success)?.data ?: return@launch
                val saved = (securitySettingsRepository.getSettings(session.userId) as? Result.Success)?.data ?: return@launch
                val biometric = localAuthenticator.isAvailable(LocalUnlockMethod.Biometric)
                val device = localAuthenticator.isAvailable(LocalUnlockMethod.DeviceCredential)
                if (authState.value is AuthState.Locked) _unlockOptions.value = UnlockOptions(
                    biometric = saved.inactivityLockEnabled && saved.biometricEnabled && biometric is Result.Success && biometric.data,
                    deviceCredential = saved.inactivityLockEnabled && saved.deviceCredentialEnabled && device is Result.Success && device.data)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Password remains available when capability checks fail. */ }
        }
    }

    private fun monitorSession() {
        viewModelScope.launch {
            combine(authState, foreground) { auth, visible -> auth to visible }.collectLatest { (auth, visible) ->
                if (!visible || auth !is AuthState.Authenticated) return@collectLatest
                // Retry transient observation/read errors. A successful policy recheck must not
                // accidentally retire the only timer while the account remains authenticated.
                while (isActive) {
                    try {
                        securitySettingsRepository.observeSettings(auth.user.userId).collectLatest settings@{ saved ->
                            policy = saved
                            val session = when (val stored = sessionManager.getSession()) {
                                is Result.Error -> throw IllegalStateException("Unable to read the inactivity session")
                                is Result.Success -> stored.data
                            }
                            if (session == null) {
                                authSessionCoordinator.checkSession()
                                return@settings
                            }
                            lastInteraction = maxOf(lastInteraction.takeIf { session.userId == monitoredUser } ?: 0L, session.lastActiveAt)
                            monitoredUser = session.userId
                            expiresAt = session.expiresAt
                            // Check immediately on resume. Keep checking even if a concurrent
                            // policy change means the authoritative recheck remains active.
                            while (isActive) {
                                val now = clock.nowMillis()
                                if (now >= expiresAt || (policy.inactivityLockEnabled &&
                                    (now < lastInteraction || now - lastInteraction >= policy.inactivityTimeoutMillis))) {
                                    authSessionCoordinator.checkSession()
                                }
                                delay(1.seconds)
                            }
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { authSessionCoordinator.checkSession() }
                    delay(1.seconds)
                }
            }
        }
    }
    private var monitoredUser: Long? = null

    init {
        monitorSession()
        restoreSession()
    }

    // ----------------------------------------------------
    // Form input
    // ----------------------------------------------------

    fun onEmailChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                email = value,
                emailError = null,
                errorMessage = null
            )
    }

    fun onPasswordChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                password = value,
                passwordError = null,
                errorMessage = null
            )
    }

    // ----------------------------------------------------
    // Restore existing session
    // ----------------------------------------------------

    private fun restoreSession() {

        viewModelScope.launch {
            authSessionCoordinator.restoreSession()
        }
    }

    fun retryRestoreSession() {
        restoreSession()
    }

    // ----------------------------------------------------
    // Login
    // ----------------------------------------------------

    fun login() {

        val current = _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    emailError = null,
                    passwordError = null
                )

            when (
                val result =
                    authSessionCoordinator.login(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = null,
                            password = ""
                        )
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Registration
    // ----------------------------------------------------

    fun register() {

        val current = _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    emailError = null,
                    passwordError = null
                )

            when (
                val result =
                    authSessionCoordinator.register(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    /*
                     * Registration does not create a session.
                     *
                     * The user must subsequently log in.
                     */

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = null,
                            password = ""
                        )
                    _events.send(
                        AuthUiEvent.RegistrationSuccess
                    )
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Unlock
    // ----------------------------------------------------

    fun unlock(request: UnlockRequest) {
        if (_uiState.value.isSubmitting || authState.value !is AuthState.Locked) return
        _uiState.value = _uiState.value.copy(isSubmitting = true, password = "", errorMessage = null)
        viewModelScope.launch {
            try {
                // Small per-attempt delay; no automatic destructive lockout of local vineyard data.
                if (request is UnlockRequest.Password) delay(1.seconds)
                when (val result = authSessionCoordinator.unlock(request)) {
                    is Result.Error -> handleOperationError(result.error)
                    is Result.Success -> Unit
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { handleOperationError(com.rncoding.testvineshield.core.domain.error.UnexpectedError()) }
            finally { _uiState.value = _uiState.value.copy(isSubmitting = false, password = "") }
        }
    }

    fun unlockWithPassword() = unlock(UnlockRequest.Password(_uiState.value.password))

    // ----------------------------------------------------
    // Logout
    // ----------------------------------------------------

    fun logout() {

        val current = _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    errorMessage = null
                )

            when (
                val result =
                    authSessionCoordinator.logout()
            ) {

                is Result.Success -> {

                    _uiState.value =
                        AuthUiState()
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Operation error handling
    // ----------------------------------------------------

    private fun handleOperationError(
        error: AppError
    ) {

        _uiState.value =
            _uiState.value.copy(
                isSubmitting = false,
                errorMessage = error.userMessage
            )
    }


}
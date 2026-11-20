package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.auth.*
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockRequest
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AuthErrorStateTest {
    private val coordinator = mockk<AuthSessionCoordinator>()
    private val observed = mockk<ObserveAuthStateUseCase>()
    private val auth = MutableStateFlow<AuthState>(AuthState.Unauthenticated(AuthState.Unauthenticated.Reason.NotLoggedIn))
    private lateinit var store: ViewModelStore
    private lateinit var dispatcher: TestDispatcher

    @Before fun before() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
        every { observed.invoke() } returns auth
        coEvery { coordinator.restoreSession() } returns Unit
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = AuthViewModel(coordinator, observed, mockk<SessionManager>(),
        mockk<SecuritySettingsRepository>(), mockk<AppClock>(), mockk<LocalAuthenticator>()).also { store.put("auth", it) }

    @Test fun loginRetainsErrorIdentityAndEditingClearsIt() = runTest {
        val failure = UnexpectedError(IllegalStateException("private diagnostic"))
        coEvery { coordinator.login(any(), any()) } returns Result.Error(failure)
        val vm = vm(); runCurrent()
        vm.onEmailChanged("grower@example.com"); vm.onPasswordChanged("Password1")
        vm.login(); runCurrent()
        assertSame(failure, vm.uiState.value.operationError)
        assertFalse(vm.uiState.value.isSubmitting)
        vm.onEmailChanged("other@example.com")
        assertNull(vm.uiState.value.operationError)
        vm.login(); runCurrent()
        assertSame(failure, vm.uiState.value.operationError)
        vm.onPasswordChanged("Different1")
        assertNull(vm.uiState.value.operationError)
    }

    @Test fun registrationRetainsValidationIdentityAndSuccessfulRetryClearsErrorAndSecret() = runTest {
        val failure = ValidationError.InvalidFormat(ValidationError.Field.EMAIL)
        coEvery { coordinator.register(any(), any()) } returns Result.Error(failure)
        val vm = vm(); runCurrent()
        vm.onEmailChanged("invalid"); vm.onPasswordChanged("Password1")
        vm.register(); runCurrent()
        assertSame(failure, vm.uiState.value.operationError)
        assertFalse(vm.uiState.value.isSubmitting)
        coEvery { coordinator.register(any(), any()) } returns Result.Success(UserDomainModel(1, "grower@example.com"))
        vm.register(); runCurrent()
        assertNull(vm.uiState.value.operationError)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test fun platformFailureCanBeFollowedByAccountPasswordUnlock() = runTest {
        val locked = AuthState.Locked(AuthState.Locked.LockReason.InactivityTimeout)
        auth.value = locked
        coEvery { coordinator.unlock(any()) } returns Result.Error(SecurityError.BiometricNotEnrolled)
        val vm = vm(); runCurrent()
        vm.unlock(UnlockRequest.Platform(LocalUnlockMethod.Biometric)); runCurrent()
        assertSame(SecurityError.BiometricNotEnrolled, vm.uiState.value.operationError)
        assertFalse(vm.uiState.value.isSubmitting)
        val unlocked = AuthState.Authenticated(UserDomainModel(1, "grower@example.com"))
        coEvery { coordinator.unlock(UnlockRequest.Password("Password1")) } coAnswers {
            auth.value = unlocked
            Result.Success(unlocked)
        }
        vm.onPasswordChanged("Password1"); vm.unlockWithPassword()
        assertNull(vm.uiState.value.operationError)
        assertEquals("", vm.uiState.value.password)
        advanceTimeBy(1.seconds); runCurrent()
        assertEquals(unlocked, auth.value)
        assertNull(vm.uiState.value.operationError)
        assertFalse(vm.uiState.value.isSubmitting)
        coVerify(exactly = 1) { coordinator.unlock(UnlockRequest.Password("Password1")) }
    }
}

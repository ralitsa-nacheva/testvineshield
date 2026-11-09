package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.auth.*
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import io.mockk.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class InactivityMonitorTest {
    private suspend fun TestScope.scenario(lock: Boolean, block: suspend TestScope.(AuthViewModel, MutableStateFlow<AuthState>, AuthSessionCoordinator) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val user = UserDomainModel(1, "grower@example.com")
        val auth = MutableStateFlow<AuthState>(AuthState.Authenticated(user))
        val observed = mockk<ObserveAuthStateUseCase>()
        every { observed.invoke() } returns auth

        val coordinator = mockk<AuthSessionCoordinator>()
        coEvery { coordinator.restoreSession() } returns Unit
        coEvery { coordinator.checkSession() } coAnswers {
            auth.value = AuthState.Locked(
                AuthState.Locked.LockReason.InactivityTimeout
            )
        }

        var session = SessionDomainModel(1, 1, 1, 10_000_000)

        val manager = mockk<SessionManager>()
        coEvery { manager.getSession() } answers {
            Result.Success(session)
        }
        coEvery { manager.recordActivity(any()) } answers {
            session = session.copy(lastActiveAt = firstArg<Long>())
            Result.Success(Unit)
        }

        val saved = MutableStateFlow(
            SecuritySettings(
                inactivityLockEnabled = lock,
                inactivityTimeoutMillis = 60_000L
            )
        )

        val settings = mockk<SecuritySettingsRepository>()
        every { settings.observeSettings(1L) } returns saved

        val clock = mockk<AppClock>()
        every { clock.nowMillis() } answers {
            1L + testScheduler.currentTime
        }
        val local = mockk<LocalAuthenticator>()
        val vm = AuthViewModel(coordinator, observed, manager, settings, clock, local)
        store.put("auth", vm)
        try { vm.setForeground(true); runCurrent(); block(vm, auth, coordinator) }
        finally {
            try {
                // Cancel the ViewModel and its monitoring coroutines.
                store.clear()

                // Finish queued cancellation work while Main is still installed.
                runCurrent()
                advanceUntilIdle()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }
    @Test fun foregroundInactivityLocksWithoutRestart() = runTest { scenario(true) { _, auth, _ ->
        advanceTimeBy(1.minutes); runCurrent(); assertIs<AuthState.Locked>(auth.value)
    } }
    @Test fun optOutDoesNotLockAfterOldFifteenMinutePeriod() = runTest { scenario(false) { _, auth, coordinator ->
        advanceTimeBy(16.minutes); runCurrent(); assertIs<AuthState.Authenticated>(auth.value)
        coVerify(exactly = 0) { coordinator.checkSession() }
    } }
    @Test fun interactionResetsForegroundDeadline() = runTest { scenario(true) { vm, auth, _ ->
        advanceTimeBy(30.seconds); vm.recordInteraction(); runCurrent()
        advanceTimeBy(30.seconds); runCurrent(); assertIs<AuthState.Authenticated>(auth.value)
        advanceTimeBy(30.seconds); runCurrent(); assertIs<AuthState.Locked>(auth.value)
    } }
    @Test fun backgroundTimeCountsAndResumeChecksImmediately() = runTest { scenario(true) { vm, auth, _ ->
        vm.setForeground(false); runCurrent(); advanceTimeBy(61.seconds)
        assertIs<AuthState.Authenticated>(auth.value)
        vm.setForeground(true); runCurrent(); assertIs<AuthState.Locked>(auth.value)
    } }
    @Test fun activeRecheckDoesNotRetireTheTimer() = runTest { scenario(true) { _, auth, coordinator ->
        coEvery { coordinator.checkSession() } returns Unit
        advanceTimeBy(60.seconds); runCurrent(); assertIs<AuthState.Authenticated>(auth.value)
        advanceTimeBy(5.seconds); runCurrent()
        coVerify(atLeast = 2) { coordinator.checkSession() }
    } }

}

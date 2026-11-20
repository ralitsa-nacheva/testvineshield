package com.rncoding.testvineshield.core.presentation.account

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.auth.*
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountLogoutTest {
    private val coordinator = mockk<AuthSessionCoordinator>()
    private val observe = mockk<ObserveAuthStateUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore

    @Before fun before() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
        every { observe() } returns MutableStateFlow<AuthState>(AuthState.Authenticated(UserDomainModel(1, "local@example.com")))
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = AccountViewModel(coordinator, observe).also { store.put("account", it) }

    @Test fun rapidDuplicateTapIsRejectedAndSuccessClearsDraft() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { coordinator.logout() } coAnswers { gate.await(); Result.Success(Unit) }
        val v = vm(); runCurrent()
        v.onCurrentPasswordChanged("secret")
        v.onNewPasswordChanged("new-secret")
        v.onConfirmNewPasswordChanged("new-secret")
        v.logout(); v.logout()
        assertTrue(v.uiState.value.isLoggingOut)
        assertEquals("", v.uiState.value.currentPassword)
        assertEquals("", v.uiState.value.newPassword)
        assertEquals("", v.uiState.value.confirmNewPassword)
        runCurrent(); coVerify(exactly = 1) { coordinator.logout() }
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(AccountUiState(), v.uiState.value)
    }

    @Test fun typedFailureReleasesGuardAndAllowsRetry() = runTest {
        coEvery { coordinator.logout() } returns Result.Error(AuthError.NoActiveSession)
        val v = vm(); runCurrent(); v.logout(); advanceUntilIdle()
        assertFalse(v.uiState.value.isLoggingOut)
        assertEquals(AccountIssue.Failure(AuthError.NoActiveSession), v.uiState.value.operationIssue)
        coEvery { coordinator.logout() } returns Result.Success(Unit)
        v.logout(); advanceUntilIdle()
        assertEquals(AccountUiState(), v.uiState.value)
        coVerify(exactly = 2) { coordinator.logout() }
    }

    @Test fun unexpectedFailureDoesNotLeaveLogoutBusy() = runTest {
        coEvery { coordinator.logout() } throws IllegalStateException("Storage unavailable")
        val v = vm(); runCurrent(); v.logout(); advanceUntilIdle()
        assertFalse(v.uiState.value.isLoggingOut)
        assertNotNull(v.uiState.value.operationIssue)
    }

    @Test fun passwordMismatchIsLocalAndEditingClearsItWithoutDomainCall() = runTest {
        val v = vm(); runCurrent()
        v.onNewPasswordChanged("Password1"); v.onConfirmNewPasswordChanged("Different1")
        v.updateAccount(); runCurrent()
        assertEquals(AccountIssue.PasswordMismatch, v.uiState.value.operationIssue)
        assertFalse(v.uiState.value.isUpdating)
        coVerify(exactly = 0) { coordinator.updateAccount(any(), any(), any()) }
        v.onConfirmNewPasswordChanged("Password1")
        assertNull(v.uiState.value.operationIssue)
    }

    @Test fun viewModelCancellationPropagatesAndReleasesGuard() = runTest {
        var cleaned = false
        coEvery { coordinator.logout() } coAnswers {
            try { awaitCancellation() } finally { cleaned = true }
        }
        val v = vm(); runCurrent(); v.logout(); runCurrent()
        store.clear(); advanceUntilIdle()
        assertTrue(cleaned)
        assertFalse(v.uiState.value.isLoggingOut)
        assertNull(v.uiState.value.operationIssue)
    }
    @Test fun updateAndDeleteKeepTypedFailuresAndReleaseTheirGuards() = runTest {
        val failure = DatabaseError.Unknown(DatabaseError.Operation.UPDATE, IllegalStateException("private diagnostic"))
        coEvery { coordinator.updateAccount(any(), any(), any()) } returns Result.Error(failure)
        coEvery { coordinator.deleteAccount(any()) } returns Result.Error(AuthError.InvalidCredentials)
        val v = vm(); runCurrent()
        v.onCurrentPasswordChanged("incorrect")
        v.updateAccount(); advanceUntilIdle()
        assertSame(failure, assertIs<AccountIssue.Failure>(v.uiState.value.operationIssue).error)
        assertFalse(v.uiState.value.isUpdating)
        v.clearError(); assertNull(v.uiState.value.operationIssue)
        v.deleteAccount(); advanceUntilIdle()
        assertEquals(AccountIssue.Failure(AuthError.InvalidCredentials), v.uiState.value.operationIssue)
        assertFalse(v.uiState.value.isDeleting)
    }

}

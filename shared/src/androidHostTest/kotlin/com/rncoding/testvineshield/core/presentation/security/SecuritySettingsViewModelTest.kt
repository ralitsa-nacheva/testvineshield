package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SecuritySettingsViewModelTest {
    private val repository = mockk<SecuritySettingsRepository>()
    private val authenticator = mockk<LocalAuthenticator>()
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        store = ViewModelStore()
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(false, false))
        coEvery { authenticator.isAvailable() } returns Result.Success(true)
        coEvery { authenticator.authenticate(any()) } returns Result.Success(Unit)
        coEvery { repository.setBiometricEnabled(1L, any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel() = SecuritySettingsViewModel(1L, repository, authenticator)
        .also { store.put("settings", it) }

    @Test
    fun loadUsesTypedSettingsAndAvailability() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(true, true))
        val vm = viewModel()
        vm.load()
        vm.load()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertTrue(vm.state.value.pinEnabled)
        assertTrue(vm.state.value.biometricAvailable)
        assertFalse(vm.state.value.isLoading)
        coVerify(exactly = 1) { repository.getSettings(1L) }
    }

    @Test
    fun availabilityErrorPreservesStoredSettings() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(true, true))
        coEvery { authenticator.isAvailable() } returns Result.Error(SecurityError.SecurityUpdateRequired)
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertTrue(vm.state.value.pinEnabled)
        assertFalse(vm.state.value.biometricAvailable)
        assertEquals(SecurityError.SecurityUpdateRequired.userMessage, vm.state.value.errorMessage)
    }

    @Test
    fun unavailableBiometricsCannotBeEnabled() = runTest {
        coEvery { authenticator.isAvailable() } returns Result.Success(false)
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.biometricEnabled)
        coVerify(exactly = 0) { authenticator.authenticate(any()) }
        coVerify(exactly = 0) { repository.setBiometricEnabled(any(), any()) }
    }

    @Test
    fun enablingWaitsForAuthenticationAndPersistence() = runTest {
        val authenticated = CompletableDeferred<Unit>()
        val saved = CompletableDeferred<Unit>()
        coEvery { authenticator.authenticate(any()) } coAnswers {
            authenticated.await()
            Result.Success(Unit)
        }
        coEvery { repository.setBiometricEnabled(1L, true) } coAnswers {
            saved.await()
            Result.Success(Unit)
        }
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        vm.setBiometricEnabled(true)
        runCurrent()
        assertTrue(vm.state.value.isUpdating)
        assertFalse(vm.state.value.biometricEnabled)
        coVerify(exactly = 0) { repository.setBiometricEnabled(any(), any()) }
        authenticated.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.biometricEnabled)
        saved.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertFalse(vm.state.value.isUpdating)
        coVerify(exactly = 1) { authenticator.authenticate(any()) }
        coVerify(exactly = 1) { repository.setBiometricEnabled(1L, true) }
    }

    @Test
    fun cancelledPromptDoesNotEnableOrSave() = runTest {
        coEvery { authenticator.authenticate(any()) } returns Result.Error(SecurityError.AuthenticationCancelled)
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.biometricEnabled)
        assertFalse(vm.state.value.isUpdating)
        assertEquals(SecurityError.AuthenticationCancelled.userMessage, vm.state.value.errorMessage)
        coVerify(exactly = 0) { repository.setBiometricEnabled(any(), any()) }
    }

    @Test
    fun persistenceFailureKeepsPreviousToggleValue() = runTest {
        coEvery { repository.setBiometricEnabled(1L, true) } returns Result.Error(UnexpectedError())
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.biometricEnabled)
        assertFalse(vm.state.value.isUpdating)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun disablingDoesNotRequireBiometricAuthentication() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(true, false))
        coEvery { authenticator.isAvailable() } returns Result.Success(false)
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        assertFalse(vm.state.value.biometricAvailable)
        vm.setBiometricEnabled(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.biometricEnabled)
        coVerify(exactly = 0) { authenticator.authenticate(any()) }
        coVerify(exactly = 1) { repository.setBiometricEnabled(1L, false) }
    }

    @Test
    fun clearingViewModelCancelsPromptWithoutSaving() = runTest {
        var cancelled = false
        coEvery { authenticator.authenticate(any()) } coAnswers {
            try { awaitCancellation() } finally { cancelled = true }
        }
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        runCurrent()
        store.clear()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertFalse(vm.state.value.biometricEnabled)
        assertFalse(vm.state.value.isUpdating)
        coVerify(exactly = 0) { repository.setBiometricEnabled(any(), any()) }
    }

    @Test
    fun unexpectedLoadFailureReleasesLoadingGuard() = runTest {
        coEvery { repository.getSettings(1L) } throws IllegalStateException("Private database text")
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertEquals(UnexpectedError().userMessage, vm.state.value.errorMessage)
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(false, false))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricAvailable)
    }

    @Test
    fun platformAvailabilityExceptionDoesNotEraseSavedPreferences() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(true, false))
        coEvery { authenticator.isAvailable() } throws IllegalStateException("Private native text")
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertFalse(vm.state.value.isLoading)
        assertEquals(UnexpectedError().userMessage, vm.state.value.errorMessage)
    }

    @Test
    fun typedAvailabilityFailureDoesNotAuthenticateOrSave() = runTest {
        coEvery { authenticator.isAvailable() } returns Result.Error(SecurityError.SecurityUpdateRequired)
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        assertEquals(SecurityError.SecurityUpdateRequired.userMessage, vm.state.value.errorMessage)
        assertFalse(vm.state.value.isUpdating)
        coVerify(exactly = 0) { authenticator.authenticate(any()) }
        coVerify(exactly = 0) { repository.setBiometricEnabled(any(), any()) }
    }

    @Test
    fun cancelledAttemptCanBeRetriedAndOnlySuccessfulAttemptSaves() = runTest {
        coEvery { authenticator.authenticate(any()) } returns Result.Error(SecurityError.AuthenticationCancelled)
        val vm = viewModel()
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        coEvery { authenticator.authenticate(any()) } returns Result.Success(Unit)
        vm.setBiometricEnabled(true)
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertEquals(null, vm.state.value.errorMessage)
        coVerify(exactly = 2) { authenticator.authenticate(any()) }
        coVerify(exactly = 1) { repository.setBiometricEnabled(1L, true) }
    }

    @Test
    fun unavailableAppPinCannotBeNewlyEnabled() = runTest {
        val vm = viewModel()
        vm.setPinEnabled(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.pinEnabled)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { repository.setPinEnabled(any(), any()) }
    }

    @Test
    fun existingPinPreferenceCanBeDisabledWithoutDuplicateWrites() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(false, true))
        coEvery { repository.setPinEnabled(1L, false) } returns Result.Success(Unit)
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        vm.setPinEnabled(false)
        vm.setPinEnabled(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.pinEnabled)
        coVerify(exactly = 1) { repository.setPinEnabled(1L, false) }
    }

    @Test
    fun repositoryReadFailureIsRetryable() = runTest {
        coEvery { repository.getSettings(1L) } returns Result.Error(UnexpectedError())
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.errorMessage)
        coEvery { repository.getSettings(1L) } returns Result.Success(SecuritySettings(true, false))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.biometricEnabled)
        assertEquals(null, vm.state.value.errorMessage)
    }

}

package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SecuritySettingsViewModelTest {
    private val repo = mockk<SecuritySettingsRepository>()
    private val local = mockk<LocalAuthenticator>()
    private lateinit var store: ViewModelStore
    private var saved = SecuritySettings()
    @Before fun before() {
        Dispatchers.setMain(StandardTestDispatcher()); store = ViewModelStore()
        coEvery { repo.getSettings(1) } answers { Result.Success(saved) }
        coEvery { repo.saveSettings(1, any()) } answers { saved = secondArg(); Result.Success(Unit) }
        coEvery { local.isAvailable(any()) } returns Result.Success(true)
        coEvery { local.authenticate(any(), any()) } returns Result.Success(Unit)
    }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun vm() = SecuritySettingsViewModel(1, repo, local).also { store.put("settings", it) }
    @Test fun defaultsOffAndCannotEditBeforeLoad() = runTest {
        val v = vm(); v.setLockEnabled(true); assertFalse(v.state.value.settings.inactivityLockEnabled)
        v.load(); advanceUntilIdle(); assertTrue(v.state.value.canEdit); assertFalse(v.state.value.settings.inactivityLockEnabled)
    }
    @Test fun lockCanEnableWithPasswordAlone() = runTest {
        coEvery { local.isAvailable(any()) } returns Result.Success(false)
        val v = vm(); v.load(); advanceUntilIdle(); v.setLockEnabled(true); advanceUntilIdle()
        assertTrue(v.state.value.settings.inactivityLockEnabled); assertFalse(v.state.value.settings.biometricEnabled)
    }
    @Test fun disablingSavesAllMethodsOffInOneWrite() = runTest {
        saved = SecuritySettings(true, false, true, 60_000, true)
        val v = vm(); v.load(); advanceUntilIdle(); v.setLockEnabled(false); v.setLockEnabled(false); advanceUntilIdle()
        assertEquals(saved, saved.normalized()); assertFalse(saved.biometricEnabled); assertFalse(saved.deviceCredentialEnabled)
        coVerify(exactly = 1) { repo.saveSettings(1, any()) }
    }
    @Test fun optionalMethodsCannotEnableWhileLockOff() = runTest {
        val v = vm(); v.load(); advanceUntilIdle(); v.setBiometricEnabled(true); v.setDeviceCredentialEnabled(true); advanceUntilIdle()
        coVerify(exactly = 0) { local.authenticate(any(), any()) }
        coVerify(exactly = 0) { repo.saveSettings(any(), any()) }
    }
    @Test fun unavailableMethodNeverAuthenticatesOrSaves() = runTest {
        saved = SecuritySettings(inactivityLockEnabled = true)
        coEvery { local.isAvailable(any()) } returns Result.Success(false)
        val v = vm(); v.load(); advanceUntilIdle(); v.setBiometricEnabled(true); advanceUntilIdle()
        assertNotNull(v.state.value.errorMessage); coVerify(exactly = 0) { repo.saveSettings(any(), any()) }
    }
    @Test fun enablingWaitsForSuccessfulPromptAndSave() = runTest {
        saved = SecuritySettings(inactivityLockEnabled = true)
        val prompt = CompletableDeferred<Unit>()
        coEvery { local.authenticate(any(), any()) } coAnswers { prompt.await(); Result.Success(Unit) }
        val v = vm(); v.load(); advanceUntilIdle(); v.setDeviceCredentialEnabled(true); v.setDeviceCredentialEnabled(true); runCurrent()
        assertTrue(v.state.value.isUpdating); assertFalse(v.state.value.settings.deviceCredentialEnabled)
        prompt.complete(Unit); advanceUntilIdle(); assertTrue(v.state.value.settings.deviceCredentialEnabled)
        coVerify(exactly = 1) { repo.saveSettings(1, any()) }
    }
    @Test fun cancelledPromptDoesNotSave() = runTest {
        saved = SecuritySettings(inactivityLockEnabled = true)
        coEvery { local.authenticate(any(), any()) } returns Result.Error(SecurityError.AuthenticationCancelled)
        val v = vm(); v.load(); advanceUntilIdle(); v.setBiometricEnabled(true); advanceUntilIdle()
        assertFalse(v.state.value.settings.biometricEnabled); coVerify(exactly = 0) { repo.saveSettings(any(), any()) }
    }
    @Test fun failedSaveKeepsPreviousPolicy() = runTest {
        coEvery { repo.saveSettings(1, any()) } returns Result.Error(UnexpectedError())
        val v = vm(); v.load(); advanceUntilIdle(); v.setLockEnabled(true); advanceUntilIdle()
        assertFalse(v.state.value.settings.inactivityLockEnabled); assertNotNull(v.state.value.errorMessage)
    }
    @Test fun supportedTimeoutPersistsInvalidIsIgnored() = runTest {
        saved = SecuritySettings(inactivityLockEnabled = true)
        val v = vm(); v.load(); advanceUntilIdle(); v.setTimeout(0); v.setTimeout(2); advanceUntilIdle()
        assertEquals(120_000L, saved.inactivityTimeoutMillis); coVerify(exactly = 1) { repo.saveSettings(1, any()) }
    }
    @Test fun clearingVmCancelsPendingSetupWithoutSaving() = runTest {
        saved = SecuritySettings(inactivityLockEnabled = true)
        coEvery { local.authenticate(any(), any()) } coAnswers { awaitCancellation() }
        val v = vm(); v.load(); advanceUntilIdle(); v.setBiometricEnabled(true); runCurrent(); store.clear(); advanceUntilIdle()
        coVerify(exactly = 0) { repo.saveSettings(any(), any()) }; assertFalse(v.state.value.isUpdating)
    }
    @Test fun failedLoadCanRetry() = runTest {
        coEvery { repo.getSettings(1) } returns Result.Error(UnexpectedError())
        val v = vm(); v.load(); advanceUntilIdle(); assertFalse(v.state.value.canEdit)
        coEvery { repo.getSettings(1) } returns Result.Success(SecuritySettings())
        v.load(); advanceUntilIdle(); assertTrue(v.state.value.canEdit)
    }
    @Test fun disablingUnavailableMethodDoesNotPrompt() = runTest {
        saved = SecuritySettings(true, false, true)
        coEvery { local.isAvailable(any()) } returns Result.Success(false)
        val v = vm(); v.load(); advanceUntilIdle(); v.setBiometricEnabled(false); advanceUntilIdle()
        assertFalse(saved.biometricEnabled); coVerify(exactly = 0) { local.authenticate(any(), any()) }
    }
}

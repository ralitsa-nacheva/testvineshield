package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.auth.*
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.security.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.*

class UnlockFallbackTest {
    private val session = SessionDomainModel(1, 1, 1, 1_000_000)
    private val user = UserDomainModel(1, "grower@example.com")
    private val manager = mockk<SessionManager> {
        coEvery { getSession() } returns Result.Success(session)
        coEvery { getSessionState() } returns Result.Success(SessionState.TimedOut)
        coEvery { refreshSession(any()) } returns Result.Success(session.copy(lastActiveAt = 60_001))
    }
    private val users = mockk<UserRepository> {
        coEvery { getUserById(1) } returns Result.Success(user)
        coEvery { login(user.userEmail, "correct") } returns Result.Success(user)
        coEvery { login(user.userEmail, "wrong") } returns Result.Error(AuthError.InvalidCredentials)
    }
    private val local = mockk<LocalAuthenticator>()
    private val settings = mockk<SecuritySettingsRepository> {
        coEvery { getSettings(1) } returns Result.Success(SecuritySettings(inactivityLockEnabled = true))
    }
    private val unlock = UnlockSessionUseCase(manager, users, local, settings)

    @Test fun passwordWorksWithNoBiometricsOrDevicePin() = runTest {
        val result = assertIs<Result.Success<AuthState>>(unlock(UnlockRequest.Password("correct")))
        assertEquals(AuthState.Authenticated(user), result.data)
        coVerify(exactly = 0) { local.authenticate(any(), any()) }
    }
    @Test fun wrongPasswordDoesNotRefresh() = runTest {
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Password("wrong")))
        coVerify(exactly = 0) { manager.refreshSession(any()) }
    }
    @Test fun disabledBiometricsCannotBypassSetting() = runTest {
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Platform(LocalUnlockMethod.Biometric)))
        coVerify(exactly = 0) { local.authenticate(any(), any()) }
    }
    @Test fun unavailableEnabledDeviceMethodCanFallBackToPassword() = runTest {
        coEvery { settings.getSettings(1) } returns Result.Success(SecuritySettings(inactivityLockEnabled = true, deviceCredentialEnabled = true))
        coEvery { local.isAvailable(LocalUnlockMethod.DeviceCredential) } returns Result.Success(false)
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Platform(LocalUnlockMethod.DeviceCredential)))
        assertIs<Result.Success<AuthState>>(unlock(UnlockRequest.Password("correct")))
    }
    @Test fun enabledDeviceMethodAuthenticatesAndRefreshes() = runTest {
        coEvery { settings.getSettings(1) } returns Result.Success(SecuritySettings(inactivityLockEnabled = true, deviceCredentialEnabled = true))
        coEvery { local.isAvailable(LocalUnlockMethod.DeviceCredential) } returns Result.Success(true)
        coEvery { local.authenticate(any(), LocalUnlockMethod.DeviceCredential) } returns Result.Success(Unit)
        assertIs<Result.Success<AuthState>>(unlock(UnlockRequest.Platform(LocalUnlockMethod.DeviceCredential)))
    }
    @Test fun cancelledPromptLeavesSessionLocked() = runTest {
        coEvery { settings.getSettings(1) } returns Result.Success(SecuritySettings(true, false, true))
        coEvery { local.isAvailable(LocalUnlockMethod.Biometric) } returns Result.Success(true)
        coEvery { local.authenticate(any(), any()) } returns Result.Error(SecurityError.AuthenticationCancelled)
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Platform(LocalUnlockMethod.Biometric)))
        coVerify(exactly = 0) { manager.refreshSession(any()) }
    }
    @Test fun expiredSessionDoesNotAcceptPasswordOrPrompt() = runTest {
        coEvery { manager.getSessionState() } returns Result.Success(SessionState.Expired)
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Password("correct")))
        coVerify(exactly = 0) { users.login(any(), any()) }
    }
    @Test fun staleCompletionAfterLogoutCannotRefreshAnotherSession() = runTest {
        coEvery { manager.getSession() } returnsMany listOf(Result.Success(session), Result.Success(null))
        assertIs<Result.Error<AppError>>(unlock(UnlockRequest.Password("correct")))
        coVerify(exactly = 0) { manager.refreshSession(any()) }
    }
    @Test fun cancellationPropagatesWithoutRefreshing() = runTest {
        coEvery { users.login(any(), any()) } throws CancellationException()
        assertFailsWith<CancellationException> { unlock(UnlockRequest.Password("correct")) }
        coVerify(exactly = 0) { manager.refreshSession(any()) }
    }
}

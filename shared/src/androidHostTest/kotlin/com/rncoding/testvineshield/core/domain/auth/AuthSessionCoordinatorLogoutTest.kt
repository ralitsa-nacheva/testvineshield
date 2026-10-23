
package com.rncoding.testvineshield.core.domain.auth

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryBackgroundWorkCoordinator
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.usecases.user.DeleteUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UpdateUserUseCase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.runBlocking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthSessionCoordinatorLogoutTest {

    private val restoreSessionUseCase =
        mockk<RestoreSessionUseCase>()

    private val unlockSessionUseCase =
        mockk<UnlockSessionUseCase>()

    private val loginUserUseCase =
        mockk<LoginUserUseCase>()

    private val registerUserUseCase =
        mockk<RegisterUserUseCase>()

    private val logoutUserUseCase =
        mockk<LogoutUserUseCase>()

    private val updateUserUseCase =
        mockk<UpdateUserUseCase>()

    private val deleteUserUseCase =
        mockk<DeleteUserUseCase>()

    private val backgroundWorkCoordinator =
        mockk<WeatherHistoryBackgroundWorkCoordinator>(relaxed = true)

    private val scheduler =
        mockk<WeatherHistoryRiskScheduler>(relaxed = true)

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun createCoordinator() = AuthSessionCoordinator(
        restoreSessionUseCase = restoreSessionUseCase,
        unlockSessionUseCase = unlockSessionUseCase,
        loginUserUseCase = loginUserUseCase,
        registerUserUseCase = registerUserUseCase,
        logoutUserUseCase = logoutUserUseCase,
        updateUserUseCase = updateUserUseCase,
        deleteUserUseCase = deleteUserUseCase,
        backgroundWorkCoordinator = backgroundWorkCoordinator,
        weatherHistoryRiskScheduler = scheduler
    )

    private suspend fun authenticate(
        coordinator: AuthSessionCoordinator
    ) {
        coEvery {
            loginUserUseCase(
                email = "test@example.com",
                password = "password123"
            )
        } returns Result.Success(user)

        val result = coordinator.login(
            email = "test@example.com",
            password = "password123"
        )

        assertIs<Result.Success<UserDomainModel>>(result)
        assertIs<AuthState.Authenticated>(coordinator.authState.value)
    }

    @Test
    fun successfulLogoutCancelsAllUserWork(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            logoutUserUseCase()
        } returns Result.Success(Unit)

        val result = coordinator.logout()

        assertIs<Result.Success<Unit>>(result)

        val state =
            assertIs<AuthState.Unauthenticated>(
                coordinator.authState.value
            )

        assertEquals(
            AuthState.Unauthenticated.Reason.LoggedOut,
            state.reason
        )

        coVerify(exactly = 1) {
            logoutUserUseCase()
        }

        verify(exactly = 1) {
            scheduler.cancelAllForUser(userId = 1L)
        }
    }

    @Test
    fun failedLogoutPreservesAuthenticationAndDoesNotCancelWork(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        val logoutError: AppError = AuthError.NoActiveSession

        coEvery {
            logoutUserUseCase()
        } returns Result.Error(logoutError)

        val result = coordinator.logout()

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(logoutError, error.error)

        val state =
            assertIs<AuthState.Authenticated>(
                coordinator.authState.value
            )

        assertEquals(1L, state.user.userId)

        verify(exactly = 0) {
            scheduler.cancelAllForUser(any())
        }
    }

    @Test
    fun schedulerFailureDoesNotChangeSuccessfulLogout(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            logoutUserUseCase()
        } returns Result.Success(Unit)

        every {
            scheduler.cancelAllForUser(userId = 1L)
        } throws IllegalStateException("Cancellation failed")

        val result = coordinator.logout()

        assertIs<Result.Success<Unit>>(result)

        assertIs<AuthState.Unauthenticated>(
            coordinator.authState.value
        )

        verify(exactly = 1) {
            scheduler.cancelAllForUser(userId = 1L)
        }
    }

    @Test
    fun logoutWithoutAuthenticatedUserDoesNotCancelWork(): Unit = runBlocking {
        val coordinator = createCoordinator()

        // The initial state is AuthState.Loading.
        assertIs<AuthState.Loading>(
            coordinator.authState.value
        )

        coEvery {
            logoutUserUseCase()
        } returns Result.Success(Unit)

        val result = coordinator.logout()

        assertIs<Result.Success<Unit>>(result)

        assertIs<AuthState.Unauthenticated>(
            coordinator.authState.value
        )

        verify(exactly = 0) {
            scheduler.cancelAllForUser(any())
        }
    }

    @Test
    fun logoutCancelsOnlyThePreviouslyAuthenticatedUser(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            logoutUserUseCase()
        } returns Result.Success(Unit)

        coordinator.logout()

        verify(exactly = 1) {
            scheduler.cancelAllForUser(1L)
        }

        verify(exactly = 0) {
            scheduler.cancelAllForUser(2L)
        }
    }
}


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

class AuthSessionCoordinatorDeleteAccountTest {

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
    fun successfulAccountDeletionCancelsAllUserWork(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            deleteUserUseCase(
                userId = 1L,
                currentPassword = "password123"
            )
        } returns Result.Success(Unit)

        val result = coordinator.deleteAccount(
            currentPassword = "password123"
        )

        assertIs<Result.Success<Unit>>(result)

        val state = assertIs<AuthState.Unauthenticated>(
            coordinator.authState.value
        )

        assertEquals(
            AuthState.Unauthenticated.Reason.LoggedOut,
            state.reason
        )

        coVerify(exactly = 1) {
            deleteUserUseCase(
                userId = 1L,
                currentPassword = "password123"
            )
        }

        verify(exactly = 1) {
            scheduler.cancelAllForUser(userId = 1L)
        }
    }

    @Test
    fun failedAccountDeletionPreservesAuthenticationAndWork(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        val deletionError: AppError = AuthError.NoActiveSession

        coEvery {
            deleteUserUseCase(
                userId = 1L,
                currentPassword = "wrong-password"
            )
        } returns Result.Error(deletionError)

        val result = coordinator.deleteAccount(
            currentPassword = "wrong-password"
        )

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(deletionError, error.error)

        val state = assertIs<AuthState.Authenticated>(
            coordinator.authState.value
        )

        assertEquals(1L, state.user.userId)

        verify(exactly = 0) {
            scheduler.cancelAllForUser(any())
        }
    }

    @Test
    fun schedulerFailureDoesNotChangeSuccessfulAccountDeletion(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            deleteUserUseCase(
                userId = 1L,
                currentPassword = "password123"
            )
        } returns Result.Success(Unit)

        every {
            scheduler.cancelAllForUser(userId = 1L)
        } throws IllegalStateException("Cancellation failed")

        val result = coordinator.deleteAccount(
            currentPassword = "password123"
        )

        assertIs<Result.Success<Unit>>(result)

        assertIs<AuthState.Unauthenticated>(
            coordinator.authState.value
        )

        verify(exactly = 1) {
            scheduler.cancelAllForUser(userId = 1L)
        }
    }

    @Test
    fun missingAuthenticationPreventsAccountDeletionAndCancellation(): Unit = runBlocking {
        val coordinator = createCoordinator()

        assertIs<AuthState.Loading>(
            coordinator.authState.value
        )

        val result = coordinator.deleteAccount(
            currentPassword = "password123"
        )

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            deleteUserUseCase(any(), any())
        }

        verify(exactly = 0) {
            scheduler.cancelAllForUser(any())
        }
    }

    @Test
    fun accountDeletionCancelsOnlyTheDeletedUsersWork(): Unit = runBlocking {
        val coordinator = createCoordinator()
        authenticate(coordinator)

        coEvery {
            deleteUserUseCase(
                userId = 1L,
                currentPassword = "password123"
            )
        } returns Result.Success(Unit)

        val result = coordinator.deleteAccount(
            currentPassword = "password123"
        )

        assertIs<Result.Success<Unit>>(result)

        verify(exactly = 1) {
            scheduler.cancelAllForUser(1L)
        }

        verify(exactly = 0) {
            scheduler.cancelAllForUser(2L)
        }
    }
}

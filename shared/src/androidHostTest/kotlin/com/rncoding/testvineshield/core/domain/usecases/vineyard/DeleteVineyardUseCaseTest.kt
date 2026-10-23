
package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.runBlocking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeleteVineyardUseCaseTest {

    private val repository = mockk<VineyardRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val scheduler =
        mockk<WeatherHistoryRiskScheduler>(relaxed = true)

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun createUseCase() = DeleteVineyardUseCase(
        repository = repository,
        authenticatedUserProvider = authenticatedUserProvider,
        weatherHistoryRiskScheduler = scheduler
    )

    private fun prepareAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user
    }

    @Test
    fun successfulDeletionCancelsBackgroundWork(): Unit = runBlocking {
        prepareAuthenticatedUser()

        coEvery {
            repository.deleteVineyard(
                userId = 1L,
                vineyardId = 100L
            )
        } returns Result.Success(Unit)

        val result = createUseCase()(vineyardId = 100L)

        assertIs<Result.Success<Unit>>(result)

        coVerify(exactly = 1) {
            repository.deleteVineyard(
                userId = 1L,
                vineyardId = 100L
            )
        }

        verify(exactly = 1) {
            scheduler.cancel(
                userId = 1L,
                vineyardId = 100L
            )
        }
    }

    @Test
    fun repositoryFailureDoesNotCancelBackgroundWork(): Unit = runBlocking {
        prepareAuthenticatedUser()

        val repositoryError: AppError = AuthError.NoActiveSession

        coEvery {
            repository.deleteVineyard(
                userId = 1L,
                vineyardId = 100L
            )
        } returns Result.Error(repositoryError)

        val result = createUseCase()(vineyardId = 100L)

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(repositoryError, error.error)

        verify(exactly = 0) {
            scheduler.cancel(any(), any())
        }
    }

    @Test
    fun schedulerFailureDoesNotChangeSuccessfulDeletion(): Unit = runBlocking {
        prepareAuthenticatedUser()

        coEvery {
            repository.deleteVineyard(
                userId = 1L,
                vineyardId = 200L
            )
        } returns Result.Success(Unit)

        every {
            scheduler.cancel(
                userId = 1L,
                vineyardId = 200L
            )
        } throws IllegalStateException("Cancellation failed")

        val result = createUseCase()(vineyardId = 200L)

        assertIs<Result.Success<Unit>>(result)

        verify(exactly = 1) {
            scheduler.cancel(
                userId = 1L,
                vineyardId = 200L
            )
        }
    }

    @Test
    fun missingAuthenticationPreventsDeletionAndCancellation(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = createUseCase()(vineyardId = 100L)

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            repository.deleteVineyard(any(), any())
        }

        verify(exactly = 0) {
            scheduler.cancel(any(), any())
        }
    }
}

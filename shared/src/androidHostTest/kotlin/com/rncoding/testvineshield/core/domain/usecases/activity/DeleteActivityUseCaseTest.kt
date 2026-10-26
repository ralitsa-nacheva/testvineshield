
package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDeletionOutcome
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DeleteActivityUseCaseTest {

    private val activityRepository = mockk<ActivityRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val useCase = DeleteActivityUseCase(
        activityRepository = activityRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun givenAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user
    }

    private fun givenOwnedVineyard() {
        givenAuthenticatedUser()

        coEvery {
            vineyardRepository.getVineyard(
                userId = 1L,
                vineyardId = 10L
            )
        } returns Result.Success(
            mockk<VineyardDomainModel>()
        )
    }

    @Test
    fun unauthenticatedUserCannotDeleteActivity(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            AuthError.NoActiveSession,
            error.error
        )

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            activityRepository.deleteActivity(any(), any())
        }
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            vineyardId = 0L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            activityRepository.deleteActivity(any(), any())
        }
    }

    @Test
    fun invalidActivityIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            vineyardId = 10L,
            activityId = 0L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            activityRepository.deleteActivity(any(), any())
        }
    }

    @Test
    fun vineyardOwnershipErrorIsPropagated(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Error(AuthError.NoActiveSession)

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            AuthError.NoActiveSession,
            error.error
        )

        coVerify(exactly = 0) {
            activityRepository.deleteActivity(any(), any())
        }
    }

    @Test
    fun deletesActivitySuccessfully(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.deleteActivity(
                vineyardId = 10L,
                activityId = 20L
            )
        } returns ActivityDeletionOutcome.DELETED

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        assertIs<Result.Success<*>>(result)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 1) {
            activityRepository.deleteActivity(10L, 20L)
        }
    }

    @Test
    fun missingActivityReturnsNotFound(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.deleteActivity(10L, 20L)
        } returns ActivityDeletionOutcome.NOT_FOUND

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            ActivityError.NotFound,
            error.error
        )

        coVerify(exactly = 1) {
            activityRepository.deleteActivity(10L, 20L)
        }
    }

    @Test
    fun linkedActivityCannotBeDeleted(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.deleteActivity(10L, 20L)
        } returns ActivityDeletionOutcome.LINKED_TO_DISEASE

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            ActivityError.LinkedToDisease,
            error.error
        )

        coVerify(exactly = 1) {
            activityRepository.deleteActivity(10L, 20L)
        }
    }

    @Test
    fun unexpectedRepositoryExceptionReturnsUnexpectedError(): Unit =
        runBlocking {
            givenOwnedVineyard()

            coEvery {
                activityRepository.deleteActivity(10L, 20L)
            } throws IllegalStateException("Database unavailable")

            val result = useCase(
                vineyardId = 10L,
                activityId = 20L
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<UnexpectedError>(error.error)
        }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.deleteActivity(10L, 20L)
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                vineyardId = 10L,
                activityId = 20L
            )
        }
    }
}

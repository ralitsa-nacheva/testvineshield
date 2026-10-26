
package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
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
import kotlin.test.assertSame

class GetActivityDetailsUseCaseTest {

    private val activityRepository =
        mockk<ActivityRepository>()

    private val vineyardRepository =
        mockk<VineyardRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val useCase = GetActivityDetailsUseCase(
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
    fun unauthenticatedUserCannotReadActivity() = runBlocking {
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
            activityRepository.getActivity(any(), any())
        }
    }

    @Test
    fun invalidVineyardIdIsRejected() = runBlocking {
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
    }

    @Test
    fun invalidActivityIdIsRejected() = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            vineyardId = 10L,
            activityId = 0L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            activityRepository.getActivity(any(), any())
        }
    }

    @Test
    fun vineyardOwnershipErrorIsPropagated() = runBlocking {
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
            activityRepository.getActivity(any(), any())
        }
    }

    @Test
    fun existingActivityIsReturnedSuccessfully() = runBlocking {
        givenOwnedVineyard()

        val expectedActivity = mockk<ActivityDomainModel>()

        coEvery {
            activityRepository.getActivity(
                vineyardId = 10L,
                activityId = 20L
            )
        } returns expectedActivity

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val success = assertIs<Result.Success<*>>(result)

        assertSame(
            expectedActivity,
            success.data
        )

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 1) {
            activityRepository.getActivity(10L, 20L)
        }
    }

    @Test
    fun missingActivityReturnsNotFound() = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.getActivity(10L, 20L)
        } returns null

        val result = useCase(
            vineyardId = 10L,
            activityId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            ActivityError.NotFound,
            error.error
        )
    }

    @Test
    fun unexpectedRepositoryExceptionReturnsUnexpectedError(): Unit =
        runBlocking {
            givenOwnedVineyard()

            coEvery {
                activityRepository.getActivity(10L, 20L)
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
            activityRepository.getActivity(10L, 20L)
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                vineyardId = 10L,
                activityId = 20L
            )
        }
    }
}

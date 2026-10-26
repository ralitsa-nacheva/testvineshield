
package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.ActivityValidator

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UpdateActivityUseCaseTest {

    private val activityRepository = mockk<ActivityRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()
    private val clock = mockk<AppClock>()
    private val validator = ActivityValidator()

    private val useCase = UpdateActivityUseCase(
        activityRepository = activityRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider,
        clock = clock,
        validator = validator
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun activity(
        activityId: Long = 20L,
        vineyardId: Long = 10L,
        activityType: String = "Pruning"
    ) = ActivityDomainModel(
        activityId = activityId,
        vineyardId = vineyardId,
        blockId = null,
        activityType = activityType,
        activityDescription = "Winter pruning",
        season = "2026",
        phenologicalStage = PhenologicalStage.DORMANCY.dbValue,
        priority = ActivityPriority.MEDIUM.dbValue,
        status = ActivityStatus.PLANNED.dbValue,
        plannedAt = LocalDate(2026, 10, 1),
        completedAt = null,
        tool = null,
        notes = null,
        createdAt = 1000L,
        updatedAt = 2000L,
        treatmentProduct = null,
        treatmentProductQuantity = null,
        treatmentProductUnits = null,
        waterQuantity = null,
        treatmentProductRate = null
    )

    private fun givenAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user
    }

    private fun givenOwnedVineyard() {
        givenAuthenticatedUser()

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Success(mockk<VineyardDomainModel>())
    }

    private fun givenExistingActivity(
        existing: ActivityDomainModel = activity()
    ) {
        givenOwnedVineyard()

        coEvery {
            activityRepository.getActivity(10L, 20L)
        } returns existing
    }

    @Test
    fun unauthenticatedUserCannotUpdateActivity(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            activityRepository.updateActivity(any(), any())
        }
    }

    @Test
    fun blankActivityTypeIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            activity(activityType = "   ")
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            activityRepository.updateActivity(any(), any())
        }
    }

    @Test
    fun invalidActivityIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            activity(activityId = 0L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            activity(vineyardId = 0L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<ActivityError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            activityRepository.updateActivity(any(), any())
        }
    }

    @Test
    fun vineyardOwnershipErrorIsPropagated(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Error(AuthError.NoActiveSession)

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            activityRepository.updateActivity(any(), any())
        }
    }

    @Test
    fun missingActivityReturnsNotFound(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            activityRepository.getActivity(10L, 20L)
        } returns null

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(ActivityError.NotFound, error.error)

        coVerify(exactly = 0) {
            activityRepository.updateActivity(any(), any())
        }
    }

    @Test
    fun updatesActivityAndPreservesCreationTimestamp(): Unit = runBlocking {
        givenExistingActivity(
            activity().copy(
                createdAt = 500L,
                updatedAt = 2000L
            )
        )

        every { clock.nowMillis() } returns 3000L

        val captured = slot<ActivityDomainModel>()

        coEvery {
            activityRepository.updateActivity(
                vineyardId = 10L,
                activity = capture(captured)
            )
        } returns true

        val result = useCase(
            activity().copy(
                activityType = "  Pruning  ",
                activityDescription = "  Winter pruning  ",
                season = "  2026  ",
                createdAt = 9999L,
                updatedAt = 9999L
            )
        )

        assertIs<Result.Success<*>>(result)

        val saved = captured.captured

        assertEquals(500L, saved.createdAt)
        assertEquals(3000L, saved.updatedAt)
        assertEquals("Pruning", saved.activityType)
        assertEquals("Winter pruning", saved.activityDescription)
        assertEquals("2026", saved.season)

        coVerify(exactly = 1) {
            activityRepository.updateActivity(10L, any())
        }
    }

    @Test
    fun updatedAtIncreasesWhenClockHasNotAdvanced(): Unit = runBlocking {
        givenExistingActivity(
            activity().copy(
                createdAt = 500L,
                updatedAt = 5000L
            )
        )

        every { clock.nowMillis() } returns 4000L

        val captured = slot<ActivityDomainModel>()

        coEvery {
            activityRepository.updateActivity(
                10L,
                capture(captured)
            )
        } returns true

        val result = useCase(activity())

        assertIs<Result.Success<*>>(result)
        assertEquals(5001L, captured.captured.updatedAt)
        assertEquals(500L, captured.captured.createdAt)
    }

    @Test
    fun failedRepositoryUpdateReturnsNotFound(): Unit = runBlocking {
        givenExistingActivity()

        every { clock.nowMillis() } returns 3000L

        coEvery {
            activityRepository.updateActivity(10L, any())
        } returns false

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(ActivityError.NotFound, error.error)
    }

    @Test
    fun unexpectedRepositoryExceptionReturnsUnexpectedError(): Unit =
        runBlocking {
            givenExistingActivity()

            every { clock.nowMillis() } returns 3000L

            coEvery {
                activityRepository.updateActivity(10L, any())
            } throws IllegalStateException("Database unavailable")

            val result = useCase(activity())

            val error = assertIs<Result.Error<*>>(result)
            assertIs<UnexpectedError>(error.error)
        }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingActivity()

        every { clock.nowMillis() } returns 3000L

        coEvery {
            activityRepository.updateActivity(10L, any())
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(activity())
        }
    }
}


package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.ActivityValidator
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import io.mockk.slot

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateActivityUseCaseTest {

    private val activityRepository =
        mockk<ActivityRepository>()

    private val vineyardRepository =
        mockk<VineyardRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val clock = mockk<AppClock>()

    private val validator = ActivityValidator()

    private val useCase = CreateActivityUseCase(
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
        activityId: Long = 0L,
        vineyardId: Long = 10L,
        blockId: Long? = null,
        activityType: String = "Pruning"
    ) = ActivityDomainModel(
        activityId = activityId,
        vineyardId = vineyardId,
        blockId = blockId,
        activityType = activityType,
        activityDescription = "Test description",
        season = "2026",
        phenologicalStage =
            PhenologicalStage.DORMANCY.dbValue,
        priority = ActivityPriority.MEDIUM.dbValue,
        status = ActivityStatus.PLANNED.dbValue,
        plannedAt = LocalDate(2026, 10, 1),
        completedAt = null,
        tool = null,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
        treatmentProduct = null,
        treatmentProductQuantity = null,
        treatmentProductUnits = null,
        waterQuantity = null,
        treatmentProductRate = null
    )

    @Test
    fun unauthenticatedUserCannotCreateActivity() =
        runBlocking {
            every {
                authenticatedUserProvider.currentUserOrNull()
            } returns null

            val result = useCase(activity())

            val error = assertIs<Result.Error<*>>(result)
            assertEquals(
                AuthError.NoActiveSession,
                error.error
            )

            coVerify(exactly = 0) {
                activityRepository.addActivity(any())
            }
        }

    @Test
    fun invalidActivityTypeIsRejected() =
        runBlocking {
            every {
                authenticatedUserProvider.currentUserOrNull()
            } returns user

            val result = useCase(
                activity(activityType = "   ")
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<ActivityError.InvalidInput>(
                error.error
            )

            coVerify(exactly = 0) {
                activityRepository.addActivity(any())
            }
        }

    @Test
    fun existingActivityIdIsRejected() =
        runBlocking {
            every {
                authenticatedUserProvider.currentUserOrNull()
            } returns user

            val result = useCase(
                activity(activityId = 55L)
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<ActivityError.InvalidInput>(
                error.error
            )

            coVerify(exactly = 0) {
                activityRepository.addActivity(any())
            }
        }

    @Test
    fun createsVineyardLevelActivitySuccessfully() = runBlocking {
        givenAuthenticatedUserAndOwnedVineyard()

        every { clock.nowMillis() } returns 123_456L

        val capturedActivity = slot<ActivityDomainModel>()

        coEvery {
            activityRepository.addActivity(capture(capturedActivity))
        } returns 50L

        val result = useCase(
            activity(
                activityType = "  Pruning  "
            ).copy(
                activityDescription = "  Winter pruning  ",
                season = "  2026  "
            )
        )

        assertIs<Result.Success<*>>(result)

        val saved = capturedActivity.captured

        assertEquals(0L, saved.activityId)
        assertEquals(10L, saved.vineyardId)
        assertEquals(null, saved.blockId)
        assertEquals("Pruning", saved.activityType)
        assertEquals("Winter pruning", saved.activityDescription)
        assertEquals("2026", saved.season)
        assertEquals(123_456L, saved.createdAt)
        assertEquals(123_456L, saved.updatedAt)

        coVerify(exactly = 1) {
            activityRepository.addActivity(any())
        }

        coVerify(exactly = 0) {
            activityRepository.belongsToVineyard(any(), any())
        }
    }

    @Test
    fun createsBlockLevelActivitySuccessfully() = runBlocking {
        givenAuthenticatedUserAndOwnedVineyard()

        coEvery {
            activityRepository.belongsToVineyard(
                vineyardId = 10L,
                blockId = 20L
            )
        } returns true

        every { clock.nowMillis() } returns 5000L

        val capturedActivity = slot<ActivityDomainModel>()

        coEvery {
            activityRepository.addActivity(capture(capturedActivity))
        } returns 51L

        val result = useCase(
            activity(blockId = 20L)
        )

        assertIs<Result.Success<*>>(result)
        assertEquals(20L, capturedActivity.captured.blockId)

        coVerify(exactly = 1) {
            activityRepository.belongsToVineyard(10L, 20L)
        }

        coVerify(exactly = 1) {
            activityRepository.addActivity(any())
        }
    }

    @Test
    fun rejectsBlockBelongingToAnotherVineyard() = runBlocking {
        givenAuthenticatedUserAndOwnedVineyard()

        coEvery {
            activityRepository.belongsToVineyard(
                vineyardId = 10L,
                blockId = 99L
            )
        } returns false

        val result = useCase(
            activity(blockId = 99L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(ActivityError.InvalidBlock, error.error)

        coVerify(exactly = 0) {
            activityRepository.addActivity(any())
        }
    }

    @Test
    fun propagatesVineyardOwnershipError() = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user

        val ownershipError: AppError = AuthError.NoActiveSession

        coEvery {
            vineyardRepository.getVineyard(
                userId = user.userId,
                vineyardId = 10L
            )
        } returns Result.Error(ownershipError)

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(ownershipError, error.error)

        coVerify(exactly = 0) {
            activityRepository.addActivity(any())
        }
    }

    @Test
    fun returnsInvalidBlockWhenInsertionIsRejected() = runBlocking {
        givenAuthenticatedUserAndOwnedVineyard()

        every { clock.nowMillis() } returns 5000L

        coEvery {
            activityRepository.addActivity(any())
        } returns null

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(ActivityError.InvalidBlock, error.error)

        coVerify(exactly = 1) {
            activityRepository.addActivity(any())
        }
    }

    @Test
    fun convertsUnexpectedRepositoryExceptionToError() = runBlocking {
        givenAuthenticatedUserAndOwnedVineyard()

        every { clock.nowMillis() } returns 5000L

        coEvery {
            activityRepository.addActivity(any())
        } throws IllegalStateException("Database unavailable")

        val result = useCase(activity())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)

        coVerify(exactly = 1) {
            activityRepository.addActivity(any())
        }
    }

    private fun givenAuthenticatedUserAndOwnedVineyard() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user

        coEvery {
            vineyardRepository.getVineyard(
                userId = user.userId,
                vineyardId = 10L
            )
        } returns Result.Success(
            mockk<VineyardDomainModel>()
        )
    }

}

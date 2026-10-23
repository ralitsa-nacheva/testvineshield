
package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateVineyardUseCaseTest {

    private val repository = mockk<VineyardRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val scheduler =
        mockk<WeatherHistoryRiskScheduler>(relaxed = true)

    private val clock = mockk<AppClock>()

    private val validator = VineyardValidator()

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private val today = LocalDate(2026, 10, 22)

    private fun createUseCase() = CreateVineyardUseCase(
        repository = repository,
        authenticatedUserProvider = authenticatedUserProvider,
        validator = validator,
        clock = clock,
        weatherHistoryRiskScheduler = scheduler
    )

    private fun validInput() = VineyardInput(
        name = "Test Vineyard",
        size = 2.5,
        country = "Bulgaria",
        city = "Plovdiv",
        latitude = 42.1354,
        longitude = 24.7453,
        timeZone = "Europe/Sofia",
        elevation = 160
    )

    private fun prepareAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user
    }

    private fun prepareSuccessfulCreation(
        vineyardId: Long = 100L
    ) {
        coEvery {
            repository.getNextSortOrder(userId = user.userId)
        } returns Result.Success(0)

        every {
            clock.today("Europe/Sofia")
        } returns today

        coEvery {
            repository.createVineyard(any())
        } returns Result.Success(vineyardId)
    }

    @Test
    fun successfulCreationSchedulesBackgroundWork(): Unit = runBlocking {
        prepareAuthenticatedUser()
        prepareSuccessfulCreation(vineyardId = 100L)

        val vineyardSlot = slot<VineyardDomainModel>()

        coEvery {
            repository.createVineyard(capture(vineyardSlot))
        } returns Result.Success(100L)

        val result = createUseCase()(validInput())

        val success = assertIs<Result.Success<Long>>(result)

        assertEquals(100L, success.data)

        assertEquals(1L, vineyardSlot.captured.userId)
        assertEquals("Test Vineyard", vineyardSlot.captured.name)
        assertEquals(0L, vineyardSlot.captured.vineyardId)
        assertEquals(today, vineyardSlot.captured.createdAt)

        verify(exactly = 1) {
            scheduler.schedule(
                userId = 1L,
                vineyardId = 100L
            )
        }

        coVerify(exactly = 1) {
            repository.createVineyard(any())
        }
    }

    @Test
    fun repositoryFailureDoesNotScheduleBackgroundWork(): Unit = runBlocking {
        prepareAuthenticatedUser()

        coEvery {
            repository.getNextSortOrder(userId = user.userId)
        } returns Result.Success(0)

        every {
            clock.today("Europe/Sofia")
        } returns today

        val repositoryError: AppError = AuthError.NoActiveSession

        coEvery {
            repository.createVineyard(any())
        } returns Result.Error(repositoryError)

        val result = createUseCase()(validInput())

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(repositoryError, error.error)

        verify(exactly = 0) {
            scheduler.schedule(any(), any())
        }
    }

    @Test
    fun schedulerFailureDoesNotChangeSuccessfulCreation(): Unit = runBlocking {
        prepareAuthenticatedUser()
        prepareSuccessfulCreation(vineyardId = 200L)

        every {
            scheduler.schedule(
                userId = 1L,
                vineyardId = 200L
            )
        } throws IllegalStateException("Scheduling failed")

        val result = createUseCase()(validInput())

        val success = assertIs<Result.Success<Long>>(result)

        assertEquals(200L, success.data)

        verify(exactly = 1) {
            scheduler.schedule(
                userId = 1L,
                vineyardId = 200L
            )
        }
    }

    @Test
    fun missingAuthenticationPreventsCreationAndScheduling(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = createUseCase()(validInput())

        val error = assertIs<Result.Error<AppError>>(result)

        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            repository.createVineyard(any())
        }

        verify(exactly = 0) {
            scheduler.schedule(any(), any())
        }
    }

    @Test
    fun invalidInputPreventsCreationAndScheduling(): Unit = runBlocking {
        prepareAuthenticatedUser()

        val invalidInput = validInput().copy(
            name = "   "
        )

        val result = createUseCase()(invalidInput)

        assertIs<Result.Error<AppError>>(result)

        coVerify(exactly = 0) {
            repository.createVineyard(any())
        }

        verify(exactly = 0) {
            scheduler.schedule(any(), any())
        }
    }
}

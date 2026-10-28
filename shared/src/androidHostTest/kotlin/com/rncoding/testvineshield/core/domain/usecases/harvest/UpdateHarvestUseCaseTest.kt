
package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.HarvestError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.validation.HarvestValidator

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.coroutines.cancellation.CancellationException

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UpdateHarvestUseCaseTest {

    private val harvestRepository = mockk<HarvestRepository>()
    private val blockRepository = mockk<BlockRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val validator = HarvestValidator()

    private val useCase = UpdateHarvestUseCase(
        harvestRepository = harvestRepository,
        blockRepository = blockRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider,
        validator = validator
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun harvest(
        harvestId: Long = 100L,
        blockId: Long = 20L,
        quantity: Double = 1250.75,
        brixSugarContent: Double = 22.5
    ) = HarvestDomainModel(
        harvestId = harvestId,
        blockId = blockId,
        season = "2026",
        harvestDate = LocalDate(2026, 10, 1),
        units = "kg",
        quantity = quantity,
        brixSugarContent = brixSugarContent
    )

    private fun givenOwnedVineyard() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Success(
            mockk<VineyardDomainModel>()
        )
    }

    private fun givenExistingHarvest() {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns mockk<BlockDomainModel> {
            every { blockId } returns 20L
        }

        coEvery {
            harvestRepository.getHarvest(20L, 100L)
        } returns harvest()
    }

    @Test
    fun existingHarvestIsUpdatedSuccessfully(): Unit = runBlocking {
        givenExistingHarvest()

        val updatedHarvest = harvest(
            quantity = 1450.25,
            brixSugarContent = 23.75
        )

        val captured = slot<HarvestDomainModel>()

        coEvery {
            harvestRepository.updateHarvest(capture(captured))
        } returns true

        val result = useCase(
            harvest = updatedHarvest,
            vineyardId = 10L
        )

        assertIs<Result.Success<Unit>>(result)

        assertEquals(100L, captured.captured.harvestId)
        assertEquals(20L, captured.captured.blockId)
        assertEquals(1450.25, captured.captured.quantity)
        assertEquals(23.75, captured.captured.brixSugarContent)

        coVerify(exactly = 1) {
            harvestRepository.updateHarvest(any())
        }
    }

    @Test
    fun missingHarvestCannotBeUpdated(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns mockk<BlockDomainModel> {
            every { blockId } returns 20L
        }

        coEvery {
            harvestRepository.getHarvest(20L, 100L)
        } returns null

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(HarvestError.NotFound, error.error)

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun invalidHarvestIdsAreRejected(): Unit = runBlocking {
        givenAuthenticatedUser()
        val invalidIds = listOf(0L, -1L)

        for (harvestId in invalidIds) {
            val result = useCase(
                harvest = harvest(harvestId = harvestId),
                vineyardId = 10L
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<HarvestError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun invalidQuantitiesAreRejected(): Unit = runBlocking {
        givenAuthenticatedUser()
        val invalidQuantities = listOf(
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        )

        for (quantity in invalidQuantities) {
            val result = useCase(
                harvest = harvest(quantity = quantity),
                vineyardId = 10L
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<HarvestError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }



    @Test
    fun invalidBrixValuesAreRejected(): Unit = runBlocking {
        givenAuthenticatedUser()
        val invalidBrixValues = listOf(
            -0.1,
            100.1,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        )

        for (brix in invalidBrixValues) {
            val result = useCase(
                harvest = harvest(brixSugarContent = brix),
                vineyardId = 10L
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<HarvestError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()
        val result = useCase(
            harvest = harvest(),
            vineyardId = 0L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<HarvestError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun unauthenticatedUserCannotUpdateHarvest(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun missingBlockPreventsHarvestUpdate(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns null

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(BlockError.NotFound, error.error)

        coVerify(exactly = 0) {
            harvestRepository.getHarvest(any(), any())
        }

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun failedAtomicUpdateReturnsNotFound(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.updateHarvest(any())
        } returns false

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(HarvestError.NotFound, error.error)

        coVerify(exactly = 1) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.updateHarvest(any())
        } throws CancellationException("Update cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                harvest = harvest(),
                vineyardId = 10L
            )
        }
    }


    @Test
    fun vineyardAccessFailurePreventsHarvestUpdate(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user

        val vineyardError = mockk<AppError>()

        coEvery {
            vineyardRepository.getVineyard(
                userId = 1L,
                vineyardId = 10L
            )
        } returns Result.Error(vineyardError)

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(vineyardError, error.error)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
        }

        coVerify(exactly = 0) {
            harvestRepository.updateHarvest(any())
        }
    }


    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.updateHarvest(any())
        } throws IllegalStateException("Database update failed")

        val result = useCase(
            harvest = harvest(),
            vineyardId = 10L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertIs<UnexpectedError>(error.error)

        coVerify(exactly = 1) {
            harvestRepository.updateHarvest(any())
        }
    }




    private fun givenAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns user
    }



}

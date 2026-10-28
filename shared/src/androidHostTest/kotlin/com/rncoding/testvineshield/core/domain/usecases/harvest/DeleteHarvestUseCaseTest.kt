
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

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.coroutines.cancellation.CancellationException

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DeleteHarvestUseCaseTest {

    private val harvestRepository = mockk<HarvestRepository>()
    private val blockRepository = mockk<BlockRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val useCase = DeleteHarvestUseCase(
        harvestRepository = harvestRepository,
        blockRepository = blockRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun harvest() = HarvestDomainModel(
        harvestId = 100L,
        blockId = 20L,
        season = "2026",
        harvestDate = LocalDate(2026, 10, 1),
        units = "kg",
        quantity = 1250.75,
        brixSugarContent = 22.5
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
    fun existingHarvestIsDeletedSuccessfully(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.deleteHarvest(20L, 100L)
        } returns true

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        assertIs<Result.Success<Unit>>(result)

        coVerify(exactly = 1) {
            harvestRepository.deleteHarvest(20L, 100L)
        }
    }

    @Test
    fun unauthenticatedUserCannotDeleteHarvest(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            harvestRepository.deleteHarvest(any(), any())
        }
    }

    @Test
    fun missingHarvestCannotBeDeleted(): Unit = runBlocking {
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
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(HarvestError.NotFound, error.error)

        coVerify(exactly = 0) {
            harvestRepository.deleteHarvest(any(), any())
        }
    }


    @Test
    fun invalidIdentifiersAreRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val invalidRequests = listOf(
            Triple(0L, 20L, 100L),
            Triple(-1L, 20L, 100L),
            Triple(10L, 0L, 100L),
            Triple(10L, -1L, 100L),
            Triple(10L, 20L, 0L),
            Triple(10L, 20L, -1L)
        )

        for ((vineyardId, blockId, harvestId) in invalidRequests) {
            val result = useCase(
                vineyardId = vineyardId,
                blockId = blockId,
                harvestId = harvestId
            )

            val error = assertIs<Result.Error<*>>(result)

            assertIs<HarvestError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            harvestRepository.deleteHarvest(any(), any())
        }
    }


    @Test
    fun missingBlockPreventsHarvestDeletion(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns null

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(BlockError.NotFound, error.error)

        coVerify(exactly = 0) {
            harvestRepository.getHarvest(any(), any())
        }

        coVerify(exactly = 0) {
            harvestRepository.deleteHarvest(any(), any())
        }
    }


    @Test
    fun vineyardAccessFailurePreventsHarvestDeletion(): Unit = runBlocking {
        givenAuthenticatedUser()

        val vineyardError = mockk<AppError>()

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Error(vineyardError)

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(vineyardError, error.error)

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
        }

        coVerify(exactly = 0) {
            harvestRepository.deleteHarvest(any(), any())
        }
    }


    @Test
    fun failedAtomicDeletionReturnsNotFound(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.deleteHarvest(20L, 100L)
        } returns false

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(HarvestError.NotFound, error.error)

        coVerify(exactly = 1) {
            harvestRepository.deleteHarvest(20L, 100L)
        }
    }


    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.deleteHarvest(20L, 100L)
        } throws IllegalStateException("Database deletion failed")

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L,
            harvestId = 100L
        )

        val error = assertIs<Result.Error<*>>(result)

        assertIs<UnexpectedError>(error.error)
    }


    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingHarvest()

        coEvery {
            harvestRepository.deleteHarvest(20L, 100L)
        } throws CancellationException("Deletion cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                vineyardId = 10L,
                blockId = 20L,
                harvestId = 100L
            )
        }
    }



}

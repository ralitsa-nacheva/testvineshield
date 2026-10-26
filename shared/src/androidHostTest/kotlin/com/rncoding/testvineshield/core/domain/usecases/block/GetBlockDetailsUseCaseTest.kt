
package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class GetBlockDetailsUseCaseTest {

    private val blockRepository = mockk<BlockRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val useCase = GetBlockDetailsUseCase(
        blockRepository = blockRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun block() = BlockDomainModel(
        blockId = 20L,
        vineyardId = 10L,
        name = "North Block",
        area = 1000,
        vineVariety = "Merlot",
        color = "Red",
        rootStock = "SO4",
        rowCount = 20,
        rowSpacing = 250,
        plantedAt = LocalDate(2020, 4, 15),
        phenologicalStage = null
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
    fun unauthenticatedUserCannotReadBlock(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
        }
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            vineyardId = 0L,
            blockId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
        }
    }

    @Test
    fun invalidBlockIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            vineyardId = 10L,
            blockId = 0L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
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
            blockId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 0) {
            blockRepository.getBlock(any(), any())
        }
    }

    @Test
    fun existingBlockIsReturnedSuccessfully(): Unit = runBlocking {
        givenOwnedVineyard()

        val expectedBlock = block()

        coEvery {
            blockRepository.getBlock(
                vineyardId = 10L,
                blockId = 20L
            )
        } returns expectedBlock

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L
        )

        val success = assertIs<Result.Success<*>>(result)

        assertSame(expectedBlock, success.data)
        assertEquals(20L, expectedBlock.blockId)
        assertEquals(10L, expectedBlock.vineyardId)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 1) {
            blockRepository.getBlock(10L, 20L)
        }
    }

    @Test
    fun missingBlockReturnsNotFound(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns null

        val result = useCase(
            vineyardId = 10L,
            blockId = 20L
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(BlockError.NotFound, error.error)

        coVerify(exactly = 1) {
            blockRepository.getBlock(10L, 20L)
        }
    }

    @Test
    fun unexpectedRepositoryExceptionReturnsUnexpectedError(): Unit =
        runBlocking {
            givenOwnedVineyard()

            coEvery {
                blockRepository.getBlock(10L, 20L)
            } throws IllegalStateException("Database unavailable")

            val result = useCase(
                vineyardId = 10L,
                blockId = 20L
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<UnexpectedError>(error.error)
        }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                vineyardId = 10L,
                blockId = 20L
            )
        }
    }
}


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
import com.rncoding.testvineshield.core.domain.validation.BlockValidator

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

class CreateBlockUseCaseTest {

    private val blockRepository = mockk<BlockRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val validator = BlockValidator()

    private val useCase = CreateBlockUseCase(
        blockRepository = blockRepository,
        vineyardRepository = vineyardRepository,
        authenticatedUserProvider = authenticatedUserProvider,
        validator = validator
    )

    private val user = UserDomainModel(
        userId = 1L,
        userEmail = "test@example.com"
    )

    private fun block(
        blockId: Long = 0L,
        vineyardId: Long = 10L,
        name: String = "North Block",
        area: Int = 1000,
        rowCount: Int = 20,
        rowSpacing: Int = 250
    ) = BlockDomainModel(
        blockId = blockId,
        vineyardId = vineyardId,
        name = name,
        area = area,
        vineVariety = "Merlot",
        color = "Red",
        rootStock = "SO4",
        rowCount = rowCount,
        rowSpacing = rowSpacing,
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
    fun unauthenticatedUserCannotCreateBlock(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun blankBlockNameIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(name = "   "))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(vineyardId = 0L))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardRepository.getVineyard(any(), any())
        }
    }

    @Test
    fun existingBlockIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(blockId = 5L))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun invalidRowCountIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(rowCount = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun invalidAreaIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(area = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun invalidRowSpacingIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(rowSpacing = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun vineyardOwnershipErrorIsPropagated(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Error(AuthError.NoActiveSession)

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun createsBlockAndReturnsGeneratedId(): Unit = runBlocking {
        givenOwnedVineyard()

        val captured = slot<BlockDomainModel>()

        coEvery {
            blockRepository.createBlock(capture(captured))
        } returns 25L

        val result = useCase(
            block(name = "  North Block  ").copy(
                vineVariety = "  Merlot  ",
                color = "  Red  ",
                rootStock = "  SO4  "
            )
        )

        val success = assertIs<Result.Success<*>>(result)
        assertEquals(25L, success.data)

        val saved = captured.captured

        assertEquals(0L, saved.blockId)
        assertEquals(10L, saved.vineyardId)
        assertEquals("North Block", saved.name)
        assertEquals("Merlot", saved.vineVariety)
        assertEquals("Red", saved.color)
        assertEquals("SO4", saved.rootStock)
        assertEquals(20, saved.rowCount)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 1) {
            blockRepository.createBlock(any())
        }
    }

    @Test
    fun nullInsertionResultReturnsUnexpectedError(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.createBlock(any())
        } returns null

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun invalidGeneratedIdReturnsUnexpectedError(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.createBlock(any())
        } returns 0L

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun unexpectedRepositoryExceptionReturnsUnexpectedError(): Unit =
        runBlocking {
            givenOwnedVineyard()

            coEvery {
                blockRepository.createBlock(any())
            } throws IllegalStateException("Database unavailable")

            val result = useCase(block())

            val error = assertIs<Result.Error<*>>(result)
            assertIs<UnexpectedError>(error.error)
        }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.createBlock(any())
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(block())
        }
    }
}

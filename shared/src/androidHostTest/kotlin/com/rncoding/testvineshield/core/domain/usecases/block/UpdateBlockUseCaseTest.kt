
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

class UpdateBlockUseCaseTest {

    private val blockRepository = mockk<BlockRepository>()
    private val vineyardRepository = mockk<VineyardRepository>()
    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val validator = BlockValidator()

    private val useCase = UpdateBlockUseCase(
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
        blockId: Long = 20L,
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
            vineyardRepository.getVineyard(1L, 10L)
        } returns Result.Success(
            mockk<VineyardDomainModel>()
        )
    }

    private fun givenExistingBlock() {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns block()
    }

    @Test
    fun unauthenticatedUserCannotUpdateBlock(): Unit = runBlocking {
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
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun blankBlockNameIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(name = "   "))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
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
    fun invalidBlockIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(blockId = 0L))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun invalidRowCountIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(rowCount = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun invalidAreaIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(area = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun invalidRowSpacingIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(block(rowSpacing = 0))

        val error = assertIs<Result.Error<*>>(result)
        assertIs<BlockError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
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
            blockRepository.getBlock(any(), any())
        }

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun missingBlockReturnsNotFound(): Unit = runBlocking {
        givenOwnedVineyard()

        coEvery {
            blockRepository.getBlock(10L, 20L)
        } returns null

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(BlockError.NotFound, error.error)

        coVerify(exactly = 0) {
            blockRepository.updateBlock(any())
        }
    }

    @Test
    fun existingBlockIsUpdatedSuccessfully(): Unit = runBlocking {
        givenExistingBlock()

        coEvery {
            blockRepository.updateBlock(any())
        } returns true

        val result = useCase(block(name = "South Block"))

        assertIs<Result.Success<Unit>>(result)

        coVerify(exactly = 1) {
            vineyardRepository.getVineyard(1L, 10L)
        }

        coVerify(exactly = 1) {
            blockRepository.getBlock(10L, 20L)
        }

        coVerify(exactly = 1) {
            blockRepository.updateBlock(
                match {
                    it.blockId == 20L &&
                            it.vineyardId == 10L &&
                            it.name == "South Block"
                }
            )
        }
    }

    @Test
    fun textFieldsAreNormalizedBeforeUpdate(): Unit = runBlocking {
        givenExistingBlock()

        val captured = slot<BlockDomainModel>()

        coEvery {
            blockRepository.updateBlock(capture(captured))
        } returns true

        val input = block(name = "  North Block  ").copy(
            vineVariety = "  Merlot  ",
            color = "  Red  ",
            rootStock = "  SO4  "
        )

        val result = useCase(input)

        assertIs<Result.Success<Unit>>(result)

        val saved = captured.captured

        assertEquals("North Block", saved.name)
        assertEquals("Merlot", saved.vineVariety)
        assertEquals("Red", saved.color)
        assertEquals("SO4", saved.rootStock)

        assertEquals(20L, saved.blockId)
        assertEquals(10L, saved.vineyardId)
    }

    @Test
    fun failedUpdateReturnsNotFound(): Unit = runBlocking {
        givenExistingBlock()

        coEvery {
            blockRepository.updateBlock(any())
        } returns false

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(BlockError.NotFound, error.error)
    }

    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingBlock()

        coEvery {
            blockRepository.updateBlock(any())
        } throws IllegalStateException("Database unavailable")

        val result = useCase(block())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingBlock()

        coEvery {
            blockRepository.updateBlock(any())
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(block())
        }
    }
}

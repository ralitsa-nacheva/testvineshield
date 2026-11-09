package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.validation.BlockValidator
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.*

class BlockOptionalContractTest {
    private val auth = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val blocks = mockk<BlockRepository>()
    private val validator = BlockValidator()
    private val create = CreateBlockUseCase(blocks, vineyards, auth, validator)
    private val update = UpdateBlockUseCase(blocks, vineyards, auth, validator)
    private val get = GetBlockDetailsUseCase(blocks, vineyards, auth)
    private val unknown = BlockDomainModel(0, 10, "North", null, "Merlot", "red", "", null, null, null, null)
    init {
        every { auth.currentUserOrNull() } returns UserDomainModel(1, "owner@example.com")
        coEvery { vineyards.getVineyard(1, 10) } returns Result.Success(mockk<VineyardDomainModel>())
    }
    @Test fun createForwardsUnknownsWithoutDefaults() = runTest {
        coEvery { blocks.createBlock(any()) } returns 20L
        assertIs<Result.Success<Long>>(create(unknown))
        coVerify { blocks.createBlock(match { it.area == null && it.rowSpacing == null && it.rowCount == null && it.plantedAt == null }) }
    }
    @Test fun updatePreservesFractionalValues() = runTest {
        val fraction = unknown.copy(blockId = 20, area = 1.25, rowSpacing = 2.45)
        coEvery { blocks.getBlock(10, 20) } returns unknown.copy(blockId = 20)
        coEvery { blocks.updateBlock(any()) } returns true
        assertIs<Result.Success<Unit>>(update(fraction))
        coVerify { blocks.updateBlock(fraction) }
    }
    @Test fun explicitClearIsForwardedAndReadBackAsUnknown() = runTest {
        val cleared = unknown.copy(blockId = 20)
        coEvery { blocks.getBlock(10, 20) } returns cleared
        coEvery { blocks.updateBlock(any()) } returns true
        assertIs<Result.Success<Unit>>(update(cleared))
        val loaded = assertIs<Result.Success<BlockDomainModel>>(get(10, 20)).data
        assertNull(loaded.plantedAt); assertNull(loaded.area); assertNull(loaded.rowCount); assertNull(loaded.rowSpacing)
    }
    @Test fun invalidKnownValuesDoNotReachRepository() = runTest {
        assertIs<Result.Error<BlockError.InvalidInput>>(create(unknown.copy(area = Double.NaN)))
        coVerify(exactly = 0) { blocks.createBlock(any()) }
    }
    @Test fun unknownFieldsDoNotBypassVineyardOwnership() = runTest {
        coEvery { vineyards.getVineyard(1, 10) } returns Result.Error(DatabaseError.NotFound(entity = "Vineyard"))
        assertIs<Result.Error<*>>(create(unknown))
        coVerify(exactly = 0) { blocks.createBlock(any()) }
    }
    @Test fun cancelledCreateStillPropagatesCancellation() = runTest {
        coEvery { blocks.createBlock(any()) } throws CancellationException()
        assertFailsWith<CancellationException> { create(unknown) }
    }
}

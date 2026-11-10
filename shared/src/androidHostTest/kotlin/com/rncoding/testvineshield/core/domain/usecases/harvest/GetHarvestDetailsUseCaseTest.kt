package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

class GetHarvestDetailsUseCaseTest {
    private val block = mockk<GetBlockDetailsUseCase>()
    private val harvests = mockk<HarvestRepository>()
    private val get = GetHarvestDetailsUseCase(block, harvests)
    private val scope = HarvestScope(10, 20)
    @Test fun deniedParentPreventsRecordLookup() = runTest {
        coEvery { block(10, 20) } returns Result.Error(AuthError.NoActiveSession)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(get(scope, 40)).error)
        verify { harvests wasNot Called }
    }
    @Test fun lookupUsesBothBlockAndHarvestIdsAndPreservesUnknownBrix() = runTest {
        coEvery { block(10, 20) } returns Result.Success(mockk<BlockDomainModel>())
        val record = HarvestDomainModel(40, 20, "2026", LocalDate(2026, 11, 9), "kg", 12.5, null)
        coEvery { harvests.getHarvest(20, 40) } returns record
        assertEquals(record, assertIs<Result.Success<HarvestDomainModel>>(get(scope, 40)).data)
        coVerify(exactly = 1) { harvests.getHarvest(20, 40) }
    }
    @Test fun missingRecordAndCancellationAreDistinct() = runTest {
        coEvery { block(10, 20) } returns Result.Success(mockk<BlockDomainModel>())
        coEvery { harvests.getHarvest(20, 40) } returns null
        assertEquals(HarvestError.NotFound, assertIs<Result.Error<*>>(get(scope, 40)).error)
        coEvery { harvests.getHarvest(20, 40) } throws CancellationException("cancelled")
        assertFailsWith<CancellationException> { get(scope, 40) }
    }
}

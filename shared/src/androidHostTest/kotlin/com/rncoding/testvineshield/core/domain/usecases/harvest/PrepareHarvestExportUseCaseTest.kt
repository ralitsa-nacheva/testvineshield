package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

class PrepareHarvestExportUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val getBlock = mockk<GetBlockDetailsUseCase>()
    private val harvests = mockk<HarvestRepository>()
    private val encoder = mockk<HarvestExportEncoder>()
    private val clock = mockk<AppClock>()
    private val useCase = PrepareHarvestExportUseCase(users, vineyards, getBlock, harvests, encoder, clock)
    private val scope = HarvestScope(10, 20)
    private val user = UserDomainModel(1, "owner@example.com")
    private val record = HarvestDomainModel(40, 20, "2026/2027", LocalDate(2026, 11, 9), "baskets", 12.5, null)
    private val document = PreparedHarvestExport("export.json", "application/json", "{}", 1)
    private fun owned() {
        every { users.currentUserOrNull() } returns user
        val parent = mockk<VineyardDomainModel>(); every { parent.name } returns "My vines"
        val child = mockk<BlockDomainModel>(); every { child.name } returns "North"
        coEvery { vineyards.getVineyard(1, 10) } returns Result.Success(parent)
        coEvery { getBlock(10, 20) } returns Result.Success(child)
        every { harvests.observeHarvestsForBlock(20) } returns flowOf(listOf(record, record.copy(harvestId = 41, season = "2025")))
        every { clock.nowMillis() } returns 1234L
        coEvery { encoder.encode(any(), any()) } returns document
    }
    @Test fun noSessionDoesNotReadOrEncode() = runTest {
        every { users.currentUserOrNull() } returns null
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON)).error)
        verify { harvests wasNot Called; encoder wasNot Called }
    }
    @Test fun absentOwnedParentDoesNotReadChildData() = runTest {
        owned()
        coEvery { vineyards.getVineyard(1, 10) } returns Result.Error(DatabaseError.NotFound("Vineyard"))
        assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON))
        verify { harvests wasNot Called; encoder wasNot Called }
    }
    @Test fun exactSeasonScopeAndMetadataReachEncoderWithoutInventingBrix() = runTest {
        owned()
        val captured = slot<HarvestExportSnapshot>()
        coEvery { encoder.encode(capture(captured), HarvestExportFormat.JSON) } returns document
        assertEquals(document, assertIs<Result.Success<PreparedHarvestExport>>(useCase(scope, "2026/2027", HarvestExportFormat.JSON)).data)
        assertEquals(HarvestExportSnapshot(scope, "My vines", "North", "2026/2027", 1234, listOf(record)), captured.captured)
    }
    @Test fun blockAuthorizationFailurePreventsHarvestRead() = runTest {
        owned()
        coEvery { getBlock(10, 20) } returns Result.Error(BlockError.NotFound)
        assertEquals(BlockError.NotFound, assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON)).error)
        verify { harvests wasNot Called; encoder wasNot Called }
    }
    @Test fun emptySeasonAndForeignBlockRowsAreNotExported() = runTest {
        owned()
        assertIs<Result.Error<*>>(useCase(scope, "missing", HarvestExportFormat.CSV))
        every { harvests.observeHarvestsForBlock(20) } returns flowOf(listOf(record.copy(blockId = 999)))
        assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON))
        verify { encoder wasNot Called }
    }
    @Test fun accountChangeDuringPreparationDiscardsDocument() = runTest {
        owned()
        every { users.currentUserOrNull() } returnsMany listOf(user, null)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON)).error)
    }
    @Test fun limitsAreActionableAndCancellationPropagates() = runTest {
        owned()
        coEvery { encoder.encode(any(), any()) } throws HarvestExportLimitException()
        assertIs<HarvestError.InvalidInput>(assertIs<Result.Error<*>>(useCase(scope, null, HarvestExportFormat.JSON)).error)
        coEvery { encoder.encode(any(), any()) } throws CancellationException("cancelled")
        assertFailsWith<CancellationException> { useCase(scope, null, HarvestExportFormat.JSON) }
    }
}

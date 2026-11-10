package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

class PrepareSprayExportUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val get = mockk<GetSprayRecordUseCase>()
    private val vineyard = mockk<GetVineyardUseCase>()
    private val block = mockk<GetBlockDetailsUseCase>()
    private val encoder = mockk<SprayExportEncoder>()
    private val clock = mockk<AppClock>()
    private val useCase = PrepareSprayExportUseCase(users, get, vineyard, block, encoder, clock)
    private val key = SprayKey(10, 30)
    private val user = UserDomainModel(1, "owner@example.com")
    private val date = LocalDate(2026, 11, 10)
    private val activity = ActivityDomainModel(30, 10, null, "Spray", "", "2026", null, "medium", "completed", date, date, null, null, 1, 2, "Legacy", null, null, null, 0.125)
    private val record = SprayRecord(30, date, null, "UTC", null, null, null, null, null, 1, listOf(SprayProductLine(5, "A")), "a".repeat(32))
    private val document = PreparedDocument("spray.json", "application/json", "{}", 1)
    private fun owned() {
        every { users.currentUserOrNull() } returns user
        coEvery { get(key) } returns Result.Success(SpraySnapshot(activity, "UTC", record))
        val parent = mockk<VineyardDomainModel>(); every { parent.name } returns "My vines"
        coEvery { vineyard(10) } returns Result.Success(parent)
        every { clock.nowMillis() } returns 1234L
        coEvery { encoder.encode(any(), any()) } returns document
    }
    @Test fun noSessionOrDeniedRecordNeverEncodes() = runTest {
        every { users.currentUserOrNull() } returns null
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.JSON)).error)
        owned(); coEvery { get(key) } returns Result.Error(AuthError.NoActiveSession)
        assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.JSON)); verify { encoder wasNot Called }
    }
    @Test fun freshSavedSnapshotKeepsWholeVineyardScopeAndFlagsExcludedLegacyFields() = runTest {
        owned()
        val captured = slot<SprayExportSnapshot>()
        coEvery { encoder.encode(capture(captured), DocumentExportFormat.JSON) } returns document
        assertEquals(document, assertIs<Result.Success<PreparedDocument>>(useCase(key, DocumentExportFormat.JSON)).data)
        assertEquals(SprayExportSnapshot(10, "My vines", null, null, "Spray", "2026", 1234, true, record), captured.captured)
        verify { block wasNot Called }
    }
    @Test fun missingSavedRecordOrUnauthorizedBlockDoesNotExport() = runTest {
        owned(); coEvery { get(key) } returns Result.Success(SpraySnapshot(activity, "UTC", null))
        assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.JSON))
        coEvery { get(key) } returns Result.Success(SpraySnapshot(activity.copy(blockId = 20), "UTC", record))
        coEvery { block(10, 20) } returns Result.Error(BlockError.NotFound)
        assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.JSON)); verify { encoder wasNot Called }
    }
    @Test fun accountChangeDiscardsPreparedDocument() = runTest {
        owned(); every { users.currentUserOrNull() } returnsMany listOf(user, null)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.JSON)).error)
    }
    @Test fun boundsAreActionableAndCancellationPropagates() = runTest {
        owned(); coEvery { encoder.encode(any(), any()) } throws SprayExportLimitException()
        assertIs<ActivityError.InvalidInput>(assertIs<Result.Error<*>>(useCase(key, DocumentExportFormat.CSV)).error)
        coEvery { encoder.encode(any(), any()) } throws CancellationException("cancelled")
        assertFailsWith<CancellationException> { useCase(key, DocumentExportFormat.JSON) }
    }
}

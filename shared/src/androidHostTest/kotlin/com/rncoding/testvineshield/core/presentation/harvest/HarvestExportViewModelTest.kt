package com.rncoding.testvineshield.core.presentation.harvest

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.HarvestValidationReason
import com.rncoding.testvineshield.core.domain.error.HarvestError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.usecases.harvest.*
import com.rncoding.testvineshield.core.presentation.export.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class HarvestExportViewModelTest {
    private val prepare = mockk<PrepareHarvestExportUseCase>()
    private val saver = mockk<DocumentSaver>()
    private val scope = HarvestScope(10, 20)
    private val document = PreparedHarvestExport("export.json", "application/json", "{}", 2)
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        coEvery { prepare(scope, "2026", HarvestExportFormat.JSON) } returns Result.Success(document)
        coEvery { saver.save(document) } returns DocumentSaveResult.Saved
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = HarvestExportViewModel(prepare).also { store.put("export", it) }
    @Test fun successfulSaveReportsCountAndRejectsDuplicateTapWhilePending() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { saver.save(document) } coAnswers { gate.await(); DocumentSaveResult.Saved }
        val v = vm()
        v.export(scope, "2026", HarvestExportFormat.JSON, saver)
        v.export(scope, "2026", HarvestExportFormat.JSON, saver)
        runCurrent(); assertTrue(v.state.value.busy)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(HarvestExportNotice.Saved(2), v.state.value.notice)
        assertFalse(v.state.value.busy)
        coVerify(exactly = 1) { prepare(scope, "2026", HarvestExportFormat.JSON) }
        coVerify(exactly = 1) { saver.save(document) }
    }
    @Test fun userPickerCancellationIsNotSuccessOrError() = runTest {
        coEvery { saver.save(document) } returns DocumentSaveResult.Cancelled
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals(HarvestExportNotice.Cancelled, v.state.value.notice)
        assertNull(v.state.value.error); assertFalse(v.state.value.busy)
    }
    @Test fun preparationFailureNeverOpensNativeSaver() = runTest {
        coEvery { prepare(scope, "2026", HarvestExportFormat.JSON) } returns Result.Error(HarvestError.InvalidInput("No records"))
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals(HarvestIssue.Failure(HarvestError.InvalidInput("No records")), v.state.value.error)
        verify { saver wasNot Called }
    }
    @Test fun providerFailureAllowsRetryWithoutReportingSaved() = runTest {
        coEvery { saver.save(document) } returns DocumentSaveResult.Failed("Disk full")
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals(HarvestIssue.DocumentSaveFailed, v.state.value.error); assertNull(v.state.value.notice)
        coEvery { saver.save(document) } returns DocumentSaveResult.Saved
        v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals(HarvestExportNotice.Saved(2), v.state.value.notice)
    }
    @Test fun explicitCancellationReleasesGuardAndRunsSaverCleanup() = runTest {
        var cleaned = false
        coEvery { saver.save(document) } coAnswers {
            try { awaitCancellation() } finally { cleaned = true }
        }
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); runCurrent()
        v.cancel(); advanceUntilIdle()
        assertTrue(cleaned); assertFalse(v.state.value.busy)
        assertEquals(HarvestExportNotice.Interrupted, v.state.value.notice)
    }
    @Test fun retryClearsPreviousNoticeWhileSaveIsPending() = runTest {
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals(HarvestExportNotice.Saved(2), v.state.value.notice)
        val gate = CompletableDeferred<Unit>()
        coEvery { saver.save(document) } coAnswers { gate.await(); DocumentSaveResult.Cancelled }
        v.export(scope, "2026", HarvestExportFormat.JSON, saver); runCurrent()
        assertTrue(v.state.value.busy)
        assertNull(v.state.value.notice)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(HarvestExportNotice.Cancelled, v.state.value.notice)
        assertFalse(v.state.value.busy)
    }

    @Test fun typedPreparationReasonReachesUiAndNeverOpensSaver() = runTest {
        val failure = HarvestError.InvalidInput("diagnostic fallback", HarvestValidationReason.ExportTooLarge)
        coEvery { prepare(scope, "2026", HarvestExportFormat.JSON) } returns Result.Error(failure)
        val v = vm(); v.export(scope, "2026", HarvestExportFormat.JSON, saver); advanceUntilIdle()
        assertSame(failure, assertIs<HarvestIssue.Failure>(v.state.value.error).error)
        assertNull(v.state.value.notice); assertFalse(v.state.value.busy)
        verify { saver wasNot Called }
    }
}

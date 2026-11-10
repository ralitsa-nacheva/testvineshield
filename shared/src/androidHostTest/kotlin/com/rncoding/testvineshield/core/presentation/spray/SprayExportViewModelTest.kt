package com.rncoding.testvineshield.core.presentation.spray

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import com.rncoding.testvineshield.core.presentation.export.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SprayExportViewModelTest {
    private val get = mockk<GetSprayRecordUseCase>()
    private val prepare = mockk<PrepareSprayExportUseCase>()
    private val saver = mockk<DocumentSaver>()
    private val key = SprayKey(10, 30)
    private val document = PreparedDocument("spray.json", "application/json", "{}", 2)
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        val record = SprayRecord(30, LocalDate(2026, 11, 10), null, "UTC", null, null, null, null, null, products = listOf(SprayProductLine(productName = "A")))
        coEvery { get(key) } returns Result.Success(SpraySnapshot(mockk<ActivityDomainModel>(), "UTC", record))
        coEvery { prepare(key, DocumentExportFormat.JSON) } returns Result.Success(document)
        coEvery { saver.save(document) } returns DocumentSaveResult.Saved
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = SprayExportViewModel(key, get, prepare).also { store.put("export", it) }
    @Test fun duplicateExportIsBlockedUntilNativeSaveCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { saver.save(document) } coAnswers { gate.await(); DocumentSaveResult.Saved }
        val v = vm(); advanceUntilIdle(); v.export(DocumentExportFormat.JSON, saver); v.export(DocumentExportFormat.JSON, saver); runCurrent()
        assertTrue(v.state.value.busy); gate.complete(Unit); advanceUntilIdle()
        coVerify(exactly = 1) { saver.save(document) }
        assertEquals("Saved one spray application with 2 product lines.", v.state.value.message)
    }
    @Test fun failedPreparationNeverOpensSaverAndPickerCancelIsNotSuccess() = runTest {
        coEvery { prepare(key, DocumentExportFormat.JSON) } returns Result.Error(ActivityError.NotFound)
        val v = vm(); advanceUntilIdle(); v.export(DocumentExportFormat.JSON, saver); advanceUntilIdle(); verify { saver wasNot Called }
        coEvery { prepare(key, DocumentExportFormat.JSON) } returns Result.Success(document)
        coEvery { saver.save(document) } returns DocumentSaveResult.Cancelled
        v.export(DocumentExportFormat.JSON, saver); advanceUntilIdle(); assertEquals("Export cancelled.", v.state.value.message)
    }
    @Test fun providerFailureAllowsRetry() = runTest {
        coEvery { saver.save(document) } returns DocumentSaveResult.Failed("Provider unavailable")
        val v = vm(); advanceUntilIdle(); v.export(DocumentExportFormat.JSON, saver); advanceUntilIdle()
        assertEquals("Provider unavailable", v.state.value.error)
        coEvery { saver.save(document) } returns DocumentSaveResult.Saved
        v.export(DocumentExportFormat.JSON, saver); advanceUntilIdle(); assertNotNull(v.state.value.message)
    }
    @Test fun cancellationRunsSaverCleanupAndNeverReportsSaved() = runTest {
        var cleaned = false
        coEvery { saver.save(document) } coAnswers { try { awaitCancellation() } finally { cleaned = true } }
        val v = vm(); advanceUntilIdle(); v.export(DocumentExportFormat.JSON, saver); runCurrent(); v.cancelExport(); advanceUntilIdle()
        assertTrue(cleaned); assertFalse(v.state.value.busy)
        assertTrue(assertNotNull(v.state.value.message).startsWith("Export interrupted"))
    }
}

package com.rncoding.testvineshield.core.presentation.alerts

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.alerts.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiseaseAlertsViewModelTest {
    private val observe = mockk<ObserveOwnedDiseaseAlertsUseCase>()
    private val acknowledge = mockk<AcknowledgeDiseaseAlertUseCase>()
    private val row = DiseaseAlertInboxItem(30, 10, "Vineyard", null, null, 3, null, "Disease", LocalDate(2026, 11, 25), "Saved", AlertStatus.ACTIVE, AlertSeverity.HIGH, PhenologicalStage.FLOWERING)
    private val source = MutableStateFlow<Result<List<DiseaseAlertInboxItem>, AppError>>(Result.Success(listOf(row)))
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        every { observe(1) } returns source
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = DiseaseAlertsViewModel(1, observe, acknowledge).also { store.put("alerts", it) }
    @Test fun pendingReviewRejectsDuplicateAndStatusComesOnlyFromStorage() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { acknowledge(1, row) } coAnswers { gate.await(); Result.Success(Unit) }
        val v = vm(); runCurrent(); v.markReviewed(row); v.markReviewed(row); runCurrent()
        assertEquals(30L, v.state.value.savingId)
        gate.complete(Unit); runCurrent()
        assertNull(v.state.value.savingId)
        assertEquals(AlertStatus.ACTIVE, v.state.value.rows.single().status)
        source.value = Result.Success(listOf(row.copy(status = AlertStatus.ACKNOWLEDGED))); runCurrent()
        assertEquals(AlertStatus.ACKNOWLEDGED, v.state.value.rows.single().status)
        coVerify(exactly = 1) { acknowledge(1, row) }
    }
    @Test fun sessionLossClearsRowsAndCancelsPendingReview() = runTest {
        var cleaned = false
        coEvery { acknowledge(1, row) } coAnswers { try { awaitCancellation() } finally { cleaned = true } }
        val v = vm(); runCurrent(); v.markReviewed(row); runCurrent()
        source.value = Result.Error(AuthError.NoActiveSession); runCurrent()
        assertTrue(cleaned); assertTrue(v.state.value.rows.isEmpty())
        assertNull(v.state.value.savingId); assertEquals(AuthError.NoActiveSession, v.state.value.error)
    }
}

package com.rncoding.testvineshield.core.presentation.harvest

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.harvest.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class HarvestDetailsViewModelTest {
    private val get = mockk<GetHarvestDetailsUseCase>()
    private val delete = mockk<DeleteHarvestUseCase>()
    private val key = HarvestKey(HarvestScope(10, 20), 40)
    private val record = HarvestDomainModel(40, 20, "2026", LocalDate(2026, 11, 9), "kg", 12.5, null)
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        coEvery { get(key.scope, 40) } returns Result.Success(record)
        coEvery { delete(10, 20, 40) } returns Result.Success(Unit)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = HarvestDetailsViewModel(key, get, delete).also { store.put("details", it) }
    @Test fun deleteNeedsExplicitConfirmationAndRejectsDuplicateTap() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { delete(10, 20, 40) } coAnswers { gate.await(); Result.Success(Unit) }
        val v = vm(); advanceUntilIdle(); v.confirmDelete()
        verify { delete wasNot Called }
        v.requestDelete(); v.cancelDelete(); v.confirmDelete(); verify { delete wasNot Called }
        v.requestDelete(); v.confirmDelete(); v.confirmDelete(); runCurrent()
        assertTrue(v.state.value.deleting)
        gate.complete(Unit); advanceUntilIdle()
        coVerify(exactly = 1) { delete(10, 20, 40) }
        assertTrue(v.state.value.deleted); assertNull(v.state.value.record)
    }
    @Test fun deletionFailureKeepsRecordVisible() = runTest {
        coEvery { delete(10, 20, 40) } returns Result.Error(UnexpectedError())
        val v = vm(); advanceUntilIdle(); v.requestDelete(); v.confirmDelete(); advanceUntilIdle()
        assertFalse(v.state.value.deleted); assertEquals(record, v.state.value.record); assertNotNull(v.state.value.error)
    }
}

package com.rncoding.testvineshield.core.presentation.harvest

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
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
class HarvestEditorViewModelTest {
    private val block = mockk<GetBlockDetailsUseCase>()
    private val get = mockk<GetHarvestDetailsUseCase>()
    private val create = mockk<CreateHarvestUseCase>()
    private val update = mockk<UpdateHarvestUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    private val scope = HarvestScope(10, 20)
    private val draft = HarvestDraft("2026", "2026-11-09", "12,5", "kg")
    private val old = HarvestDomainModel(40, 20, "2026", LocalDate(2026, 11, 9), "baskets", 12.5, 0.0)
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        val child = mockk<BlockDomainModel>()
        every { child.name } returns "North"
        coEvery { block(10, 20) } returns Result.Success(child)
        coEvery { get(scope, 40) } returns Result.Success(old)
        coEvery { create(any(), 10) } returns Result.Success(41L)
        coEvery { update(any(), 10) } returns Result.Success(Unit)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm(request: HarvestEditorRequest = HarvestEditorRequest(HarvestKey(scope, 0), draft)) =
        HarvestEditorViewModel(request, block, get, create, update).also { store.put("editor", it) }
    @Test fun createUsesExplicitScopeAndUnknownBrix() = runTest {
        val v = vm(); advanceUntilIdle(); v.save(); advanceUntilIdle()
        coVerify(exactly = 1) { create(HarvestDomainModel(0, 20, "2026", LocalDate(2026, 11, 9), "kg", 12.5, null), 10) }
        assertTrue(v.state.value.saved)
    }
    @Test fun restoredDraftCannotBypassOwnership() = runTest {
        coEvery { block(10, 20) } returns Result.Error(BlockError.NotFound)
        val v = vm(); advanceUntilIdle(); v.save()
        assertFalse(v.state.value.ready)
        verify { create wasNot Called; update wasNot Called }
    }
    @Test fun loadingSavingAndTerminalSavedStatePreventMutationsAndDuplicateWrites() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { create(any(), 10) } coAnswers { gate.await(); Result.Success(41L) }
        val v = vm(); v.units("lb"); v.text(HarvestField.Quantity, "999"); advanceUntilIdle()
        assertEquals(draft, v.state.value.draft)
        v.save(); v.save(); v.units("lb"); runCurrent()
        assertTrue(v.state.value.saving); assertEquals(draft, v.state.value.draft)
        gate.complete(Unit); advanceUntilIdle(); v.save()
        coVerify(exactly = 1) { create(any(), 10) }
    }
    @Test fun existingRecordPreservesLegacyUnitAndZeroBrixUntilExplicitlyCleared() = runTest {
        val v = vm(HarvestEditorRequest(HarvestKey(scope, 40))); advanceUntilIdle()
        assertEquals("baskets", v.state.value.legacyUnit)
        assertEquals("0.0", v.state.value.draft.brix)
        v.text(HarvestField.Brix, ""); v.save(); advanceUntilIdle()
        coVerify(exactly = 1) { update(old.copy(brixSugarContent = null), 10) }
    }
    @Test fun invalidInputDoesNotWriteAndSaveFailureRetainsDraftForRetry() = runTest {
        val v = vm(); advanceUntilIdle(); v.units(null); v.save(); advanceUntilIdle()
        assertNotNull(v.state.value.unitError); verify { create wasNot Called }
        v.units("kg")
        coEvery { create(any(), 10) } returns Result.Error(UnexpectedError())
        v.save(); advanceUntilIdle(); assertTrue(v.state.value.canEdit); assertEquals(draft, v.state.value.draft)
        coEvery { create(any(), 10) } returns Result.Success(41L)
        v.save(); advanceUntilIdle(); assertTrue(v.state.value.saved)
    }
    @Test fun cancelledSaveDoesNotPublishSuccessNavigation() = runTest {
        coEvery { create(any(), 10) } coAnswers { awaitCancellation() }
        val v = vm(); advanceUntilIdle(); v.save(); runCurrent(); store.clear(); advanceUntilIdle()
        assertFalse(v.state.value.saved); assertFalse(v.state.value.saving)
    }
}

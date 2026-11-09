package com.rncoding.testvineshield.core.presentation.blocks

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.block.*
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BlockEditorViewModelTest {
    private val create = mockk<CreateBlockUseCase>()
    private val update = mockk<UpdateBlockUseCase>()
    private val get = mockk<GetBlockDetailsUseCase>()
    private val parent = mockk<GetVineyardUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    private val old = BlockDomainModel(20, 10, "Old", 1.25, "Merlot", "red", "", 5, 2.45, null, null, AreaUnit.HECTARE, LengthUnit.METRE)
    @Before fun before() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
        coEvery {parent(10)} returns Result.Success(mockk<VineyardDomainModel>())
        coEvery {get(10, 20)} returns Result.Success(old)
        coEvery {create(any())} returns Result.Success(21L)
        coEvery {update(any())} returns Result.Success(Unit)
    }
    @After fun after() { try {store.clear()
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.advanceUntilIdle()} finally {Dispatchers.resetMain()} }
    private fun vm(request: BlockEditorRequest = BlockEditorRequest(10)) = BlockEditorViewModel(request, create, update, get, parent).also {store.put("editor", it)}
    private fun fill(v: BlockEditorViewModel) {v.text(BlockField.Name, "North")
        v.text(BlockField.Variety, "Merlot")}
    @Test fun cannotEditWhileLoading() = runTest {
        val v = vm()
        v.text(BlockField.Name, "Ignored")
        v.areaUnit(AreaUnit.ACRE)
        advanceUntilIdle()
        assertEquals("", v.state.value.draft.name)
        assertNull(v.state.value.draft.areaUnit)
    }
    @Test fun createSavesUnknownsAsNull() = runTest {
        val v = vm()
        advanceUntilIdle()
        fill(v)
        v.save()
        advanceUntilIdle()
        coVerify {create(match {it.area==null && it.rowSpacing==null && it.rowCount==null && it.plantedAt==null})}
        assertEquals(21L, v.state.value.savedId)
    }
    @Test fun malformedInputDoesNotReachUseCase() = runTest {
        val v = vm()
        advanceUntilIdle()
        fill(v)
        v.text(BlockField.Area, "1,234.5")
        v.save()
        advanceUntilIdle()
        assertTrue(BlockField.Area in v.state.value.errors)
        coVerify(exactly = 0) {create(any())}
    }
    @Test fun duplicateSaveAndMutationsAreBlockedUntilCompletion() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery {create(any())} coAnswers {gate.await()
            Result.Success(21L)}
        val v = vm()
        advanceUntilIdle()
        fill(v)
        v.save()
        v.save()
        v.text(BlockField.Name, "Ignored")
        runCurrent()
        assertTrue(v.state.value.saving)
        assertEquals("North", v.state.value.draft.name)
        gate.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) {create(any())}
    }
    @Test fun updateRetainsValuesAndUnitUntilExplicitClear() = runTest {
        val v = vm(BlockEditorRequest(10, 20))
        advanceUntilIdle()
        v.text(BlockField.Name, "Renamed")
        v.save()
        advanceUntilIdle()
        coVerify {update(old.copy(name = "Renamed"))}
        assertEquals(20L, v.state.value.savedId)
    }
    @Test fun explicitClearDoesNotChangeIndependentUnitMetadata() = runTest {
        val v = vm(BlockEditorRequest(10, 20))
        advanceUntilIdle()
        v.text(BlockField.Area, "")
        v.text(BlockField.Spacing, "")
        v.save()
        advanceUntilIdle()
        coVerify {update(match {it.area==null && it.rowSpacing==null && it.areaUnit==AreaUnit.HECTARE && it.rowSpacingUnit==LengthUnit.METRE})}
    }
    @Test fun restoredDraftStillRequiresOwnedParent() = runTest {
        coEvery {parent(10)} returns Result.Error(DatabaseError.NotFound("Vineyard"))
        val v = vm(BlockEditorRequest(10, 0, BlockDraft(name = "Restored", variety = "Merlot")))
        advanceUntilIdle()
        v.save()
        assertFalse(v.state.value.ready)
        coVerify(exactly = 0) {create(any())}
    }
    @Test fun restorePreservesUnfinishedTextAfterOwnershipCheck() = runTest {
        val draft = BlockDraft(name = "Draft", variety = "Merlot", area = "unfinished")
        val v = vm(BlockEditorRequest(10, 20, draft))
        advanceUntilIdle()
        assertEquals(draft, v.state.value.draft)
    }
    @Test fun failedSaveKeepsDraftForRetry() = runTest {
        coEvery {create(any())} returns Result.Error(UnexpectedError())
        val v = vm()
        advanceUntilIdle()
        fill(v)
        v.save()
        advanceUntilIdle()
        assertTrue(v.state.value.canEdit)
        assertEquals("North", v.state.value.draft.name)
        assertNull(v.state.value.savedId)
        coEvery {create(any())} returns Result.Success(21L)
        v.save()
        advanceUntilIdle()
        assertEquals(21L, v.state.value.savedId)
    }
    @Test fun cancellationDoesNotPublishSavedNavigation() = runTest {
        coEvery {create(any())} coAnswers {awaitCancellation()}
        val v = vm()
        advanceUntilIdle()
        fill(v)
        v.save()
        runCurrent()
        store.clear()
        advanceUntilIdle()
        assertNull(v.state.value.savedId)
        assertFalse(v.state.value.saving)
    }
}

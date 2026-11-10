package com.rncoding.testvineshield.core.presentation.spray

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SprayViewModelTest {
    private val get = mockk<GetSprayRecordUseCase>()
    private val save = mockk<SaveSprayRecordUseCase>()
    private val delete = mockk<DeleteSprayRecordUseCase>()
    private val key = SprayKey(10, 30)
    private val draft = SprayDraft(date = "2026-11-10", zone = "UTC", lines = listOf(SprayLineDraft(name = "A")))
    private val record = SprayRecord(30, LocalDate(2026, 11, 10), null, "UTC", null, null, null, null, null, 1,
        listOf(SprayProductLine(5, "A")), "a".repeat(32))
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        coEvery { get(key) } returns Result.Success(SpraySnapshot(mockk<ActivityDomainModel>(), "UTC", null))
        coEvery { save(key, any()) } returns Result.Success(Unit)
        coEvery { delete(key, any(), any()) } returns Result.Success(Unit)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm(restored: SprayDraft? = draft) = SprayViewModel(SprayRequest(key, restored), get, save, delete).also { store.put("spray", it) }
    @Test fun restoredDraftDoesNotBypassOwnership() = runTest {
        coEvery { get(key) } returns Result.Error(AuthError.NoActiveSession)
        val v = vm(); advanceUntilIdle(); v.save()
        assertFalse(v.state.value.canEdit); verify { save wasNot Called }
    }
    @Test fun noDateIsFabricatedAndInitialZoneComesFromOwnedVineyard() = runTest {
        val v = vm(null); advanceUntilIdle()
        assertEquals("UTC", v.state.value.draft.zone); assertEquals("", v.state.value.draft.date)
        v.save(); advanceUntilIdle(); assertTrue("date" in v.state.value.errors)
        verify { save wasNot Called }
    }
    @Test fun loadingSavingAndDuplicateOperationsAreGuarded() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { save(key, any()) } coAnswers { gate.await(); Result.Success(Unit) }
        val v = vm(); v.text(SprayField.Date, "Ignored"); advanceUntilIdle(); assertEquals(draft, v.state.value.draft)
        v.addLine(); v.lineText(1, SprayLineField.Name, "B"); v.save(); v.save(); v.removeLine(0); runCurrent()
        assertTrue(v.state.value.saving); assertEquals(2, v.state.value.draft.lines.size)
        gate.complete(Unit); advanceUntilIdle(); assertTrue(v.state.value.saved)
        coVerify(exactly = 1) { save(key, match { it.products.size == 2 && it.products.all { line -> line.quantity == null } }) }
    }
    @Test fun staleRestoredDraftKeepsItsBaseVersionUntilExplicitReload() = runTest {
        coEvery { get(key) } returns Result.Success(SpraySnapshot(mockk<ActivityDomainModel>(), "UTC", record))
        val v = vm(draft.copy(baseRevision = 1, baseGeneration = "b".repeat(32))); advanceUntilIdle()
        assertNotNull(v.state.value.error); assertEquals("b".repeat(32), v.state.value.draft.baseGeneration)
        v.load(discardDraft = true); advanceUntilIdle()
        assertFalse(v.state.value.editing); assertEquals(record.generation, v.state.value.draft.baseGeneration)
    }
    @Test fun deleteRequiresConfirmationAndKeepsVersionIdentity() = runTest {
        coEvery { get(key) } returns Result.Success(SpraySnapshot(mockk<ActivityDomainModel>(), "UTC", record))
        val v = vm(null); advanceUntilIdle(); v.confirmDelete(); verify { delete wasNot Called }
        v.requestDelete(); v.confirmDelete(); v.confirmDelete(); advanceUntilIdle()
        coVerify(exactly = 1) { delete(key, 1, "a".repeat(32)) }; assertTrue(v.state.value.removed)
    }
    @Test fun failedOrCancelledSaveDoesNotReportSuccessAndKeepsDraft() = runTest {
        coEvery { save(key, any()) } returns Result.Error(ActivityError.InvalidInput("Conflict"))
        val v = vm(); advanceUntilIdle(); v.save(); advanceUntilIdle()
        assertFalse(v.state.value.saved); assertEquals(draft, v.state.value.draft); assertTrue(v.state.value.canEdit)
        coEvery { save(key, any()) } coAnswers { awaitCancellation() }
        v.save(); runCurrent(); store.clear(); advanceUntilIdle()
        assertFalse(v.state.value.saved); assertFalse(v.state.value.saving)
    }
}

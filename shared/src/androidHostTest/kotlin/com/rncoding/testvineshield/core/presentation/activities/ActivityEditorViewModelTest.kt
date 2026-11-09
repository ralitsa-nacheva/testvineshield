package com.rncoding.testvineshield.core.presentation.activities

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityEditorViewModelTest {
    private val create=mockk<CreateActivityUseCase>()
    private val update=mockk<UpdateActivityUseCase>()
    private val get=mockk<GetActivityDetailsUseCase>()
    private val parent=mockk<GetVineyardUseCase>()
    private val block=mockk<GetBlockDetailsUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    private val draft=ActivityDraft(type="Pruning",season="2026",planned="2026-11-09")
    private val old=assertNotNull(draft.parsed(10,null,null).record).copy(activityId=30,treatmentProduct="Preserved product",treatmentProductQuantity=0.5,createdAt=1,updatedAt=2)
    @Before fun before() {
        dispatcher=StandardTestDispatcher();Dispatchers.setMain(dispatcher);store=ViewModelStore()
        coEvery {parent(10)} returns Result.Success(mockk<VineyardDomainModel>())
        coEvery {get(10,30)} returns Result.Success(old)
        coEvery {create(any())} returns Result.Success(Unit)
        coEvery {update(any())} returns Result.Success(Unit)
    }
    @After fun after() {try {store.clear();dispatcher.scheduler.runCurrent();dispatcher.scheduler.advanceUntilIdle()} finally {Dispatchers.resetMain()} }
    private fun vm(request: ActivityEditorRequest=ActivityEditorRequest(ActivityScope(10),restoredDraft=draft))=
        ActivityEditorViewModel(request,create,update,get,parent,block).also {store.put("editor",it)}
    @Test fun createUsesUnknownStageAndExplicitScope()=runTest {
        val v=vm();advanceUntilIdle();v.save();advanceUntilIdle()
        coVerify {create(match {it.vineyardId==10L && it.blockId==null && it.phenologicalStage==null})}
        assertTrue(v.state.value.saved)
    }
    @Test fun loadingRejectsMutationsAndDuplicateSaveIsBlocked()=runTest {
        val gate=CompletableDeferred<Unit>();coEvery {create(any())} coAnswers {gate.await();Result.Success(Unit)}
        val v=vm();v.text(ActivityField.Type,"Ignored");advanceUntilIdle();assertEquals("Pruning",v.state.value.draft.type)
        v.save();v.save();v.text(ActivityField.Type,"Ignored");runCurrent();assertTrue(v.state.value.saving)
        gate.complete(Unit);advanceUntilIdle();coVerify(exactly=1) {create(any())}
    }
    @Test fun invalidCompletionDoesNotCallUpdate()=runTest {
        val v=vm(ActivityEditorRequest(ActivityScope(10),30,complete=true));advanceUntilIdle()
        assertEquals(ActivityStatus.COMPLETED,v.state.value.draft.status)
        v.save();advanceUntilIdle();assertTrue(ActivityField.Completed in v.state.value.errors)
        coVerify(exactly=0) {update(any())}
    }
    @Test fun completionWithDatePreservesHiddenTreatmentData()=runTest {
        val v=vm(ActivityEditorRequest(ActivityScope(10),30,complete=true));advanceUntilIdle()
        v.text(ActivityField.Completed,"2026-11-10");v.save();advanceUntilIdle()
        coVerify {update(match {it.status=="completed" && it.completedAt?.toString()=="2026-11-10" && it.treatmentProduct=="Preserved product" && it.treatmentProductQuantity==0.5})}
        assertTrue(v.state.value.saved)
    }
    @Test fun restoredDraftCannotBypassParentOwnership()=runTest {
        coEvery {parent(10)} returns Result.Error(DatabaseError.NotFound("Vineyard"))
        val v=vm();advanceUntilIdle();v.save();assertFalse(v.state.value.ready)
        coVerify(exactly=0) {create(any())}
    }
    @Test fun invalidBlockScopeDoesNotCreateWholeVineyardRecord()=runTest {
        coEvery {block(10,20)} returns Result.Error(BlockError.NotFound)
        val v=vm(ActivityEditorRequest(ActivityScope(10,20),restoredDraft=draft));advanceUntilIdle();v.save()
        assertFalse(v.state.value.ready);coVerify(exactly=0) {create(any())}
    }
    @Test fun saveFailureKeepsDraftForRetry()=runTest {
        coEvery {create(any())} returns Result.Error(UnexpectedError())
        val v=vm();advanceUntilIdle();v.save();advanceUntilIdle();assertTrue(v.state.value.canEdit);assertEquals(draft,v.state.value.draft)
        coEvery {create(any())} returns Result.Success(Unit)
        v.save();advanceUntilIdle();assertTrue(v.state.value.saved)
    }
    @Test fun cancelDoesNotPublishSavedNavigation()=runTest {
        coEvery {create(any())} coAnswers {awaitCancellation()}
        val v=vm();advanceUntilIdle();v.save();runCurrent();store.clear();advanceUntilIdle()
        assertFalse(v.state.value.saved);assertFalse(v.state.value.saving)
    }
}

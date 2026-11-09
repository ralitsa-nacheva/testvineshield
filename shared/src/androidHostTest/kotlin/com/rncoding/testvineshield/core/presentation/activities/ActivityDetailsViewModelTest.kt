package com.rncoding.testvineshield.core.presentation.activities

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailsViewModelTest {
    private val get=mockk<GetActivityDetailsUseCase>()
    private val delete=mockk<DeleteActivityUseCase>()
    private val block=mockk<GetBlockDetailsUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher=StandardTestDispatcher();Dispatchers.setMain(dispatcher);store=ViewModelStore()
        val record=assertNotNull(ActivityDraft(type="Pruning",season="2026",planned="2026-11-09").parsed(10,null,null).record).copy(activityId=30)
        coEvery {get(10,30)} returns Result.Success(record)
        coEvery {delete(10,30)} returns Result.Success(Unit)
    }
    @After fun after() {try {store.clear();dispatcher.scheduler.runCurrent();dispatcher.scheduler.advanceUntilIdle()} finally {Dispatchers.resetMain()} }
    private fun vm()=ActivityDetailsViewModel(ActivityKey(10,30),get,delete,block).also {store.put("details",it)}
    @Test fun noDeleteWithoutConfirmationAndCancelPreservesRecord()=runTest {
        val v=vm();advanceUntilIdle();v.confirmDelete();v.requestDelete();v.cancelDelete();v.confirmDelete();advanceUntilIdle()
        coVerify(exactly=0) {delete(any(),any())};assertNotNull(v.state.value.record)
    }
    @Test fun diseaseLinkedDeleteShowsErrorWithoutNavigating()=runTest {
        coEvery {delete(10,30)} returns Result.Error(ActivityError.LinkedToDisease)
        val v=vm();advanceUntilIdle();v.requestDelete();v.confirmDelete();advanceUntilIdle()
        assertFalse(v.state.value.deleted);assertNotNull(v.state.value.record)
        assertEquals(ActivityError.LinkedToDisease.userMessage,v.state.value.error)
    }
    @Test fun duplicateDeleteSubmitsOnce()=runTest {
        val gate=CompletableDeferred<Unit>();coEvery {delete(10,30)} coAnswers {gate.await();Result.Success(Unit)}
        val v=vm();advanceUntilIdle();v.requestDelete();v.confirmDelete();v.confirmDelete();runCurrent()
        gate.complete(Unit);advanceUntilIdle();assertTrue(v.state.value.deleted);coVerify(exactly=1) {delete(10,30)}
    }
}

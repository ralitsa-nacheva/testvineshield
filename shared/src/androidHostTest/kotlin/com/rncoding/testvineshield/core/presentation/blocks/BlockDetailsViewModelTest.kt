package com.rncoding.testvineshield.core.presentation.blocks

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.usecases.block.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BlockDetailsViewModelTest {
    private val get = mockk<GetBlockDetailsUseCase>()
    private val delete = mockk<DeleteBlockUseCase>()
    private val history = mockk<GetRecentBlockStageObservationsUseCase>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    private val block = BlockDomainModel(20, 10, "North", null, "Merlot", "", "", null, null, null, null)
    @Before fun before() {dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
        coEvery {get(10, 20)} returns Result.Success(block)
        coEvery {delete(10, 20)} returns Result.Success(Unit)
        coEvery {history(10, 20)} returns Result.Success(emptyList())
    }
    @After fun after() {try {store.clear()
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.advanceUntilIdle()} finally {Dispatchers.resetMain()} }
    private fun vm() = BlockDetailsViewModel(BlockKey(10, 20), get, delete, history).also {store.put("detail", it)}
    @Test fun deleteRequiresExplicitConfirmationAndCancelDoesNotDelete() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.confirmDelete()
        v.requestDelete()
        v.cancelDelete()
        v.confirmDelete()
        advanceUntilIdle()
        coVerify(exactly = 0) {delete(any(), any())}
        assertNotNull(v.state.value.block)
    }
    @Test fun pendingDeleteCannotDuplicateAndSuccessIsTerminal() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery {delete(10, 20)} coAnswers {gate.await()
            Result.Success(Unit)}
        val v = vm()
        advanceUntilIdle()
        v.requestDelete()
        v.confirmDelete()
        v.confirmDelete()
        runCurrent()
        assertTrue(v.state.value.deleting)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(v.state.value.deleted)
        assertNull(v.state.value.block)
        coVerify(exactly = 1) {delete(10, 20)}
    }
    @Test fun failedDeleteRetainsRecordAndDoesNotNavigate() = runTest {
        coEvery {delete(10, 20)} returns Result.Error(BlockError.NotFound)
        val v = vm()
        advanceUntilIdle()
        v.requestDelete()
        v.confirmDelete()
        advanceUntilIdle()
        assertFalse(v.state.value.deleted)
        assertNotNull(v.state.value.block)
        assertNotNull(v.state.value.error)
    }
}

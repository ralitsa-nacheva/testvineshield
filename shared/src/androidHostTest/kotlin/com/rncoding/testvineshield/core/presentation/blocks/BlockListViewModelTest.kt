package com.rncoding.testvineshield.core.presentation.blocks

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.block.ObserveOwnedBlocksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BlockListViewModelTest {
    @Test fun authenticationErrorClearsPreviouslyVisibleBlocks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val block = BlockDomainModel(20, 10, "North", null, "Merlot", "", "", null, null, null, null)
            val updates = MutableStateFlow<Result<List<BlockDomainModel>, AppError>>(Result.Success(listOf(block)))
            val observe = mockk<ObserveOwnedBlocksUseCase>()
            every {observe(10)} returns updates
            val vm = BlockListViewModel(10, observe)
            store.put("list", vm)
            runCurrent()
            assertEquals(listOf(block), vm.state.value.blocks)
            updates.value = Result.Error(AuthError.NoActiveSession)
            runCurrent()
            assertTrue(vm.state.value.blocks.isEmpty())
            assertNotNull(vm.state.value.error)
        } finally {
            try {store.clear()
                runCurrent()
                advanceUntilIdle()} finally {Dispatchers.resetMain()}
        }
    }
    @Test fun retryRestartsReadAfterError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val updates = MutableStateFlow<Result<List<BlockDomainModel>, AppError>>(Result.Error(AuthError.NoActiveSession))
            val observe = mockk<ObserveOwnedBlocksUseCase>()
            every {observe(10)} returns updates
            val vm = BlockListViewModel(10, observe)
            store.put("list", vm)
            runCurrent()
            assertNotNull(vm.state.value.error)
            updates.value = Result.Success(emptyList())
            vm.reload()
            runCurrent()
            assertNull(vm.state.value.error)
            assertFalse(vm.state.value.loading)
        } finally {
            try {store.clear()
                runCurrent()
                advanceUntilIdle()} finally {Dispatchers.resetMain()}
        }
    }
}

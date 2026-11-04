package com.rncoding.testvineshield.core.presentation.root

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupViewModelTest {
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(initializer: AppInitializer) =
        AppStartupViewModel(initializer).also { store.put("startup", it) }

    @Test
    fun remainsLoadingUntilInitializationCompletes() = runTest {
        val release = CompletableDeferred<Unit>()
        val vm = viewModel {release.await()}
        runCurrent()
        assertEquals(AppStartupUiState.Loading, vm.uiState.value)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(AppStartupUiState.Ready, vm.uiState.value)
    }

    @Test
    fun failureCanBeRetriedWithoutStartingConcurrentAttempts() = runTest {
        var calls = 0
        val release = CompletableDeferred<Unit>()
        val vm = viewModel {
            calls++
            if (calls == 1) {
                throw IllegalStateException("Database details")
            }
            release.await()
        }
        advanceUntilIdle()
        assertEquals(AppStartupUiState.Failed, vm.uiState.value)
        vm.retry()
        vm.retry()
        runCurrent()
        assertEquals(2, calls)
        assertEquals(AppStartupUiState.Loading, vm.uiState.value)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(AppStartupUiState.Ready, vm.uiState.value)
        vm.retry()
        advanceUntilIdle()
        assertEquals(2, calls)
    }

    @Test
    fun clearingViewModelCancelsInitializationWithoutDisplayingFailure() = runTest {
        var cancelled = false
        val vm = viewModel {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        runCurrent()
        store.clear()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertEquals(AppStartupUiState.Loading, vm.uiState.value)
    }
}

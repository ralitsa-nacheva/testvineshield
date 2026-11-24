package com.rncoding.testvineshield.core.presentation.activities

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityListErrorTest {
    @Test fun typedFailureClearsSnapshotAndReloadCanRecover() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val observe = mockk<ObserveActivityNotebookUseCase>()
            val record = mockk<ActivityDomainModel>()
            val source = MutableStateFlow<Result<ActivityNotebookSnapshot, AppError>>(Result.Success(ActivityNotebookSnapshot(activities = listOf(record))))
            val scope = ActivityScope(10)
            every { observe(scope) } returns source
            val vm = ActivityListViewModel(scope, observe); store.put("list", vm); runCurrent()
            assertEquals(listOf(record), vm.state.value.snapshot.activities)
            source.value = Result.Error(AuthError.NoActiveSession); runCurrent()
            assertSame(AuthError.NoActiveSession, vm.state.value.error)
            assertEquals(ActivityNotebookSnapshot(), vm.state.value.snapshot)
            source.value = Result.Success(ActivityNotebookSnapshot()); vm.reload(); runCurrent()
            assertNull(vm.state.value.error); assertFalse(vm.state.value.loading)
        } finally {
            try { store.clear(); runCurrent(); advanceUntilIdle() }
            finally { Dispatchers.resetMain() }
        }
    }
}

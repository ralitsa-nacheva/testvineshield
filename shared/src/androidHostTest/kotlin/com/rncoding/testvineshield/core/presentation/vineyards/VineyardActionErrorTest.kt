package com.rncoding.testvineshield.core.presentation.vineyards

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDetailedSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import com.rncoding.testvineshield.core.presentation.vineyard_details.*
import com.rncoding.testvineshield.core.presentation.vineyard_list.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VineyardActionErrorTest {
    private val lists = mockk<ObserveVineyardSummariesUseCase>()
    private val details = mockk<ObserveVineyardDetailsUseCase>()
    private val delete = mockk<DeleteVineyardUseCase>()
    private val reorder = mockk<ReorderVineyardsUseCase>()
    private val listMapper = mockk<VineyardListMapper>()
    private val detailsMapper = mockk<VineyardDetailsUiMapper>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        every { lists() } returns flowOf(emptyList<VineyardSummary>())
        every { details(10) } returns flowOf<VineyardDetailedSummary?>(null)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun listVm() = VineyardListViewModel(lists, delete, reorder, listMapper).also { store.put("list", it) }
    private fun detailsVm() = VineyardDetailsViewModel(10, details, delete, detailsMapper).also { store.put("details", it) }

    @Test fun listDeletionRetainsTypedErrorAndCanBeRetried() = runTest {
        val failure = DatabaseError.Unknown(DatabaseError.Operation.DELETE, IllegalStateException("private diagnostic"))
        coEvery { delete(10) } returns Result.Error(failure)
        val vm = listVm(); runCurrent()
        vm.requestDelete(10); vm.confirmDelete(); runCurrent()
        assertSame(failure, assertIs<VineyardListEvent.ShowSnackbar>(vm.events.first()).error)
        assertNull(vm.state.value.vineyardPendingDeletion)
        coEvery { delete(10) } returns Result.Success(Unit)
        vm.requestDelete(10); vm.confirmDelete(); runCurrent()
        assertEquals(VineyardListEvent.VineyardDeleted, vm.events.first())
    }
    @Test fun detailsFailureReleasesGuardWithoutDeletionEvent() = runTest {
        coEvery { delete(10) } returns Result.Error(AuthError.NoActiveSession)
        val vm = detailsVm(); runCurrent()
        vm.requestDelete(); vm.confirmDelete(); runCurrent()
        assertSame(AuthError.NoActiveSession, assertIs<VineyardDetailsEvent.ShowSnackbar>(vm.events.first()).error)
        assertFalse(vm.state.value.isDeleting); assertFalse(vm.state.value.showDeleteConfirmation)
        coEvery { delete(10) } returns Result.Success(Unit)
        vm.requestDelete(); vm.confirmDelete(); runCurrent()
        assertEquals(VineyardDetailsEvent.VineyardDeleted, vm.events.first())
    }
    @Test fun reorderFailureKeepsOrderAndReleasesGuard() = runTest {
        val a = mockk<VineyardSummary>(); val b = mockk<VineyardSummary>()
        val first = mockk<VineyardListItemUiModel>(); val second = mockk<VineyardListItemUiModel>()
        every { first.vineyardId } returns 10L; every { second.vineyardId } returns 20L
        every { lists() } returns flowOf(listOf(a, b))
        every { listMapper.toUiModel(a) } returns first; every { listMapper.toUiModel(b) } returns second
        val failure = DatabaseError.ConstraintViolation(DatabaseError.Operation.UPDATE, "private diagnostic")
        coEvery { reorder(listOf(20L, 10L)) } returns Result.Error(failure)
        val vm = listVm(); runCurrent(); vm.reorder(10, 1); runCurrent()
        assertSame(failure, assertIs<VineyardListEvent.ShowSnackbar>(vm.events.first()).error)
        assertEquals(listOf(10L, 20L), vm.state.value.vineyards.map { it.vineyardId })
        assertFalse(vm.state.value.isReordering)
    }
}

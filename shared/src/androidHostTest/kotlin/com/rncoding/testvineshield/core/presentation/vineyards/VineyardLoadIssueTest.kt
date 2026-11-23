package com.rncoding.testvineshield.core.presentation.vineyards

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDetailedSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import com.rncoding.testvineshield.core.presentation.vineyard_details.*
import com.rncoding.testvineshield.core.presentation.vineyard_list.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VineyardLoadIssueTest {
    private val lists = mockk<ObserveVineyardSummariesUseCase>()
    private val details = mockk<ObserveVineyardDetailsUseCase>()
    private val delete = mockk<DeleteVineyardUseCase>()
    private val reorder = mockk<ReorderVineyardsUseCase>()
    private val listMapper = mockk<VineyardListMapper>()
    private val detailsMapper = mockk<VineyardDetailsUiMapper>()
    private lateinit var store: ViewModelStore
    private lateinit var dispatcher: TestDispatcher
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun listVm() = VineyardListViewModel(lists, delete, reorder, listMapper).also { store.put("list", it) }
    private fun detailsVm() = VineyardDetailsViewModel(10, details, delete, detailsMapper).also { store.put("details", it) }

    @Test fun failedObservationsUseSafeIssueInsteadOfExceptionText() = runTest {
        every { lists() } returns flow<List<VineyardSummary>> { throw IllegalStateException("private SQL details") }
        every { details(10) } returns flow<VineyardDetailedSummary?> { throw IllegalStateException("private path") }
        val list = listVm(); val detail = detailsVm(); runCurrent()
        assertEquals(VineyardLoadIssue.Failed, list.state.value.loadIssue)
        assertEquals(VineyardLoadIssue.Failed, detail.state.value.loadIssue)
        assertFalse(list.state.value.isLoading); assertFalse(detail.state.value.isLoading)
    }
    @Test fun emptyListIsNotAReadFailure() = runTest {
        every { lists() } returns flowOf(emptyList<VineyardSummary>())
        val vm = listVm(); runCurrent()
        assertNull(vm.state.value.loadIssue); assertTrue(vm.state.value.vineyards.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }
    @Test fun missingDetailsAreDistinctAndLaterDataClearsIssue() = runTest {
        val source = MutableStateFlow<VineyardDetailedSummary?>(null)
        every { details(10) } returns source
        val summary = mockk<VineyardDetailedSummary>()
        val ui = mockk<VineyardDetailsUiModel>()
        every { detailsMapper.toUi(summary) } returns ui
        val vm = detailsVm(); runCurrent()
        assertEquals(VineyardLoadIssue.NotFound, vm.state.value.loadIssue)
        source.value = summary; runCurrent()
        assertNull(vm.state.value.loadIssue); assertSame(ui, vm.state.value.details)
    }
    @Test fun listReadFailureRetainsAlreadyMappedRecords() = runTest {
        val summary = mockk<VineyardSummary>()
        val ui = mockk<VineyardListItemUiModel>()
        every { listMapper.toUiModel(summary) } returns ui
        every { lists() } returns flow<List<VineyardSummary>> {
            emit(listOf(summary)); throw IllegalStateException("private diagnostic")
        }
        val vm = listVm(); runCurrent()
        assertEquals(VineyardLoadIssue.Failed, vm.state.value.loadIssue)
        assertEquals(listOf(ui), vm.state.value.vineyards)
    }
    @Test fun cancellationDoesNotBecomeALoadFailure() = runTest {
        every { lists() } returns flow<List<VineyardSummary>> { throw CancellationException("cancelled") }
        every { details(10) } returns flow<VineyardDetailedSummary?> { throw CancellationException("cancelled") }
        val list = listVm(); val detail = detailsVm(); runCurrent()
        assertNull(list.state.value.loadIssue); assertNull(detail.state.value.loadIssue)
    }
}

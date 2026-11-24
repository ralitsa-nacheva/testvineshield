package com.rncoding.testvineshield.core.presentation.disease_catalogue

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiseaseCatalogueViewModelTest {
    private val repository = mockk<DiseaseRepository>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    @Test fun catalogueFailureRemainsTypedAndRetryRecovers() = runTest {
        every { repository.observePredefinedDiseases() } returns flow { throw IllegalStateException("storage") }
        val v = DiseaseCatalogueViewModel(repository).also { store.put("catalogue", it) }
        advanceUntilIdle(); assertEquals(DiseaseCatalogueState.Unavailable, v.state.value)
        every { repository.observePredefinedDiseases() } returns flowOf(emptyList())
        v.reload(); advanceUntilIdle()
        assertEquals(DiseaseCatalogueState.Loaded(emptyList()), v.state.value)
    }
    @Test fun cancellingCatalogueLoadDoesNotBecomeUnavailable() = runTest {
        var cleaned = false
        every { repository.observePredefinedDiseases() } returns flow<List<DiseaseDomainModel>> {
            try { awaitCancellation() } finally { cleaned = true }
        }
        val v = DiseaseCatalogueViewModel(repository).also { store.put("catalogue", it) }
        runCurrent(); store.clear(); advanceUntilIdle()
        assertTrue(cleaned); assertEquals(DiseaseCatalogueState.Loading, v.state.value)
    }
}

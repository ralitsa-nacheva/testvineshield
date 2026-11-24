package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.usecases.disease.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BlockDiseaseObservationsViewModelTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val repository = mockk<DiseaseRepository>()
    private val create = mockk<CreateDiseaseOccurrenceUseCase>()
    private val status = mockk<ChangeDiseaseOccurrenceStatusUseCase>()
    private val details = mockk<EditDiseaseOccurrenceDetailsUseCase>()
    private val symptom = mockk<RecordReferenceSymptomUseCase>()
    private val links = mockk<ObservationActivityLinkUseCase>()
    private val delete = mockk<DeleteOwnedBlockOccurrenceUseCase>()
    private val currentUser = MutableStateFlow<UserDomainModel?>(UserDomainModel(1, "local@example.invalid"))
    private val date = LocalDate(2026, 11, 24)
    private val observation = BlockDiseaseObservation(40, "Local", date, 10, DiseaseOccurrenceStatus.ACTIVE, null, null, 1, null)
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore

    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        every { users.observeUser() } returns currentUser
        every { repository.observeOwnedBlockOccurrences(1, 10, 20) } returns flowOf(listOf(observation))
        val disease = mockk<DiseaseDomainModel>()
        every { disease.diseaseId } returns 3L
        every { repository.observePredefinedDiseases() } returns flowOf(listOf(disease))
        every { repository.observeCompletedActivitiesForLink(1, 10, 20) } returns flowOf(emptyList())
        every { repository.observeOwnedOccurrenceActivityLinks(1, 10, 20) } returns flowOf(emptyList())
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = BlockDiseaseObservationsViewModel(DiseaseObservationsKey(1, 10, 20), users, repository, create, status, details, symptom, links, delete).also { store.put("observations", it) }

    @Test fun loadFailureIsTypedAndRetryCanRecover() = runTest {
        every { repository.observeOwnedBlockOccurrences(1, 10, 20) } returns flow { throw IllegalStateException("storage") }
        val v = vm(); advanceUntilIdle()
        assertEquals(ObservationIssue.LoadFailed, v.state.value.loadError)
        assertFalse(v.state.value.loading)
        every { repository.observeOwnedBlockOccurrences(1, 10, 20) } returns flowOf(listOf(observation))
        v.reload(); advanceUntilIdle()
        assertNull(v.state.value.loadError); assertEquals(listOf(observation), v.state.value.observations)
    }
    @Test fun createFailurePreservesTypedReasonAndDoesNotPublishSuccess() = runTest {
        val failure = DiseaseError.InvalidInput("fallback", DiseaseValidationReason.ObservationFuture)
        coEvery { create(1, 10, 20, 3, date, 10, "notes") } returns Result.Error(failure)
        val v = vm(); advanceUntilIdle(); v.save(3, date, 10, "notes"); advanceUntilIdle()
        assertSame(failure, assertIs<ObservationIssue.Failure>(v.state.value.saveError).error)
        assertNull(v.state.value.lastSavedId); assertFalse(v.state.value.saving)
    }
    @Test fun dialogFailureRetainsDialogAndOriginalError() = runTest {
        val failure = DiseaseError.InvalidInput("fallback", DiseaseValidationReason.ObservationChanged)
        coEvery { details(1, 10, 20, observation, date, 10, "notes") } returns Result.Error(failure)
        val v = vm(); advanceUntilIdle(); v.openDialog(40, ObservationDialog.Kind.DETAILS)
        v.saveDetails(date, 10, "notes"); advanceUntilIdle()
        assertNotNull(v.state.value.dialog)
        assertSame(failure, assertIs<ObservationIssue.Failure>(v.state.value.dialogError).error)
        assertFalse(v.state.value.saving); assertNull(v.state.value.saveError)
    }
    @Test fun unexpectedCreateExceptionBecomesTypedFailureAndReleasesGuard() = runTest {
        coEvery { create(1, 10, 20, 3, date, 10, "notes") } throws IllegalStateException("adapter")
        val v = vm(); advanceUntilIdle(); v.save(3, date, 10, "notes"); advanceUntilIdle()
        assertIs<UnexpectedError>(assertIs<ObservationIssue.Failure>(v.state.value.saveError).error)
        assertFalse(v.state.value.saving); assertNull(v.state.value.lastSavedId)
    }
    @Test fun sessionExpiryCancelsPendingWriteAndDoesNotCreateErrorOrSuccess() = runTest {
        var cleaned = false
        coEvery { create(1, 10, 20, 3, date, 10, "notes") } coAnswers {
            try { awaitCancellation() } finally { cleaned = true }
        }
        val v = vm(); advanceUntilIdle(); v.save(3, date, 10, "notes"); runCurrent()
        assertTrue(v.state.value.saving)
        currentUser.value = null; advanceUntilIdle()
        assertTrue(cleaned); assertEquals(ObservationIssue.SessionUnavailable, v.state.value.loadError)
        assertNull(v.state.value.saveError); assertNull(v.state.value.lastSavedId); assertFalse(v.state.value.saving)
    }
}

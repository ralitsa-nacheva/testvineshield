package com.rncoding.testvineshield.core.domain.usecases.alerts

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.DiseaseAlertRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiseaseAlertUseCasesTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val repository = mockk<DiseaseAlertRepository>()
    private val row = DiseaseAlertInboxItem(30, 10, "Vineyard", null, null, 3, null, "Disease", LocalDate(2026, 11, 25), "Saved text", AlertStatus.ACTIVE, AlertSeverity.HIGH, PhenologicalStage.FLOWERING)

    @Test fun wrongSessionCannotObserveOrWrite() = runTest {
        every { users.observeUser() } returns flowOf(UserDomainModel(2, "other@example.invalid"))
        every { users.currentUserOrNull() } returns UserDomainModel(2, "other@example.invalid")
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(ObserveOwnedDiseaseAlertsUseCase(users, repository)(1).first()).error)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(AcknowledgeDiseaseAlertUseCase(users, repository)(1, row)).error)
        verify { repository wasNot Called }
    }
    @Test fun expiryClearsPriorRowsFromTheObservedResult() = runTest {
        val user = MutableStateFlow<UserDomainModel?>(UserDomainModel(1, "owner@example.invalid"))
        val records = MutableStateFlow(listOf(row))
        every { users.observeUser() } returns user
        every { repository.observeOwnedInbox(1) } returns records
        val results = mutableListOf<Result<List<DiseaseAlertInboxItem>, AppError>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { ObserveOwnedDiseaseAlertsUseCase(users, repository)(1).collect { results.add(it) } }
        runCurrent(); assertEquals(listOf(row), assertIs<Result.Success<*>>(results.last()).data)
        user.value = null; runCurrent()
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(results.last()).error)
        job.cancelAndJoin()
    }
    @Test fun staleReviewDoesNotReportSuccessAndCancellationPropagates() = runTest {
        every { users.currentUserOrNull() } returns UserDomainModel(1, "owner@example.invalid")
        coEvery { repository.acknowledgeOwned(1, row) } returns false
        assertIs<DatabaseError.ConstraintViolation>(assertIs<Result.Error<*>>(AcknowledgeDiseaseAlertUseCase(users, repository)(1, row)).error)
        coEvery { repository.acknowledgeOwned(1, row) } throws CancellationException("cancel")
        assertFailsWith<CancellationException> { AcknowledgeDiseaseAlertUseCase(users, repository)(1, row) }
    }
    @Test fun successfulReviewIsRecheckedAgainstCurrentSession() = runTest {
        every { users.currentUserOrNull() } returnsMany listOf(UserDomainModel(1, "owner@example.invalid"), null)
        coEvery { repository.acknowledgeOwned(1, row) } returns true
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(AcknowledgeDiseaseAlertUseCase(users, repository)(1, row)).error)
    }
}

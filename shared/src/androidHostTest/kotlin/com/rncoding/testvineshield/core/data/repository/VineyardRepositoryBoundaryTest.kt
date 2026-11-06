package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.InvalidVineyardOrderException
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.mappers.VineyardMapper
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class VineyardRepositoryBoundaryTest {
    private val dao = mockk<VineyardDao>()
    private val mapper = VineyardMapper()
    private val repository = VineyardRepositoryImpl(
        vineyardDao = dao, vineyardMapper = mapper, vineyardSummaryMapper = mockk(),
        blockDao = mockk(), weatherHistoryDao = mockk(), weatherCalculationsDao = mockk(),
        diseaseOccurrenceDao = mockk(), diseaseAlertDao = mockk(), blockSummaryMapper = mockk(),
        vineyardWeatherSummaryMapper = mockk(), vineyardWeatherRiskSummaryMapper = mockk(),
        diseaseOccurrenceSummaryMapper = mockk(), diseaseAlertSummaryMapper = mockk()
    )
    private val entity = VineyardEntity(
        vineyardId = 10, userId = 1, name = "Vineyard", size = 1.5, country = "Bulgaria", city = "Plovdiv",
        latitude = 42.1, longitude = 24.7, timeZone = "Europe/Sofia", elevation = 200.0,
        sortOrder = 0, createdAt = LocalDate(2026, 11, 4), updatedAt = LocalDate(2026, 11, 4)
    )
    private val vineyard = mapper.entityToDomain(entity)

    @Test fun absentLookupIsNotFound(): Unit = runBlocking {
        coEvery { dao.getVineyard(1, 10) } returns null
        assertEquals(Result.Error(DatabaseError.NotFound("Vineyard")), repository.getVineyard(1, 10))
    }

    @Test fun successfulLookupPreservesFields(): Unit = runBlocking {
        coEvery { dao.getVineyard(1, 10) } returns entity
        assertEquals(Result.Success(vineyard), repository.getVineyard(1, 10))
    }

    @Test fun genuineReadFailureRemainsDatabaseFailure(): Unit = runBlocking {
        val cause = IllegalStateException("failure")
        coEvery { dao.getVineyard(1, 10) } throws cause
        val result = assertIs<Result.Error<*>>(repository.getVineyard(1, 10))
        val error = assertIs<DatabaseError.Unknown>(result.error)
        assertEquals(DatabaseError.Operation.READ, error.operation)
        assertSame(cause, error.cause)
    }

    @Test fun reorderUsesTheTransactionalDaoBoundary(): Unit = runBlocking {
        coEvery { dao.reorderVineyards(1, listOf(20, 10)) } returns Unit
        assertEquals(Result.Success(Unit), repository.reorderVineyards(1, listOf(20, 10)))
        coVerify(exactly = 1) { dao.reorderVineyards(1, listOf(20, 10)) }
        coVerify(exactly = 0) { dao.updateSortOrder(any(), any(), any()) }
    }

    @Test fun invalidOrderIsAConstraintError(): Unit = runBlocking {
        coEvery { dao.reorderVineyards(1, any()) } throws InvalidVineyardOrderException()
        val result = assertIs<Result.Error<*>>(repository.reorderVineyards(1, listOf(10, 10)))
        val error = assertIs<DatabaseError.ConstraintViolation>(result.error)
        assertEquals(DatabaseError.Operation.UPDATE, error.operation)
    }

    @Test fun reorderStorageFailureIsNotSuccessOrValidationFailure(): Unit = runBlocking {
        coEvery { dao.reorderVineyards(1, any()) } throws IllegalStateException("disk")
        val result = assertIs<Result.Error<*>>(repository.reorderVineyards(1, listOf(10)))
        assertIs<DatabaseError.Unknown>(result.error)
    }

    @Test fun everySuspendRepositoryOperationPropagatesCancellation(): Unit = runBlocking {
        val cancelled = CancellationException("test cancellation")
        coEvery { dao.getVineyard(any(), any()) } throws cancelled
        coEvery { dao.insertVineyard(any()) } throws cancelled
        coEvery { dao.updateVineyard(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws cancelled
        coEvery { dao.deleteVineyard(any(), any()) } throws cancelled
        coEvery { dao.reorderVineyards(any(), any()) } throws cancelled
        coEvery { dao.getNextSortOrder(any()) } throws cancelled
        val operations: List<suspend () -> Any> = listOf(
            { repository.getVineyard(1, 10) }, { repository.createVineyard(vineyard.copy(vineyardId = 0)) },
            { repository.updateVineyard(vineyard) }, { repository.deleteVineyard(1, 10) },
            { repository.reorderVineyards(1, listOf(10)) }, { repository.getNextSortOrder(1) }
        )
        for (operation in operations) {
            val thrown = assertFailsWith<CancellationException> { operation() }
            assertSame(cancelled, thrown)
        }
    }
}

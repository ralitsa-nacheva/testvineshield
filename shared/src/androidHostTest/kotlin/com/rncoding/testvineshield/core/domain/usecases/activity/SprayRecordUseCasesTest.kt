package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SprayRepository
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import com.rncoding.testvineshield.core.domain.validation.SprayValidator
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

class SprayRecordUseCasesTest {
    private val activity = mockk<GetActivityDetailsUseCase>()
    private val vineyard = mockk<GetVineyardUseCase>()
    private val repository = mockk<SprayRepository>()
    private val validator = SprayValidator(VineyardTimeZoneCatalog(setOf("UTC")))
    private val get = GetSprayRecordUseCase(activity, vineyard, repository)
    private val save = SaveSprayRecordUseCase(activity, repository, validator)
    private val delete = DeleteSprayRecordUseCase(activity, repository)
    private val key = SprayKey(10, 30)
    private fun record() = SprayRecord(30, LocalDate(2026, 11, 10), null, "UTC", null, null, null, null, null,
        products = listOf(SprayProductLine(productName = " Product A ")))
    private fun owned() {
        coEvery { activity(10, 30) } returns Result.Success(mockk<ActivityDomainModel>())
    }
    @Test fun deniedParentCannotReadOrWriteSpray() = runTest {
        coEvery { activity(10, 30) } returns Result.Error(AuthError.NoActiveSession)
        assertIs<Result.Error<*>>(get(key)); assertIs<Result.Error<*>>(save(key, record()))
        assertIs<Result.Error<*>>(delete(key, 1, "a".repeat(32)))
        verify { repository wasNot Called }
    }
    @Test fun noExistingSprayIsDistinctFromMissingActivity() = runTest {
        owned()
        val parent = mockk<VineyardDomainModel>(); every { parent.timeZone } returns "UTC"
        coEvery { vineyard(10) } returns Result.Success(parent)
        coEvery { repository.get(10, 30) } returns null
        val snapshot = assertIs<Result.Success<SpraySnapshot>>(get(key)).data
        assertNull(snapshot.record); assertEquals("UTC", snapshot.vineyardTimeZone)
    }
    @Test fun atomicWriteReceivesAllLinesAndUnknownAmountsWithoutLegacyMutation() = runTest {
        owned()
        val captured = slot<SprayRecord>()
        coEvery { repository.save(10, capture(captured)) } returns SprayWriteOutcome.SAVED
        val input = record().copy(products = record().products + SprayProductLine(productName = "B", quantity = 0.5, unit = SprayQuantityUnit.KILOGRAM))
        assertIs<Result.Success<Unit>>(save(key, input))
        assertEquals(listOf("Product A", "B"), captured.captured.products.map { it.productName })
        assertNull(captured.captured.products.first().quantity)
    }
    @Test fun identityAndUnitValidationPreventWrites() = runTest {
        assertIs<Result.Error<*>>(save(key, record().copy(activityId = 31)))
        assertIs<Result.Error<*>>(save(key, record().copy(waterQuantity = 10.0)))
        verify { repository wasNot Called }
    }
    @Test fun staleWriteIsAnActionableErrorAndDeleteUsesVersionAndGeneration() = runTest {
        owned()
        coEvery { repository.save(10, any()) } returns SprayWriteOutcome.CONFLICT
        assertIs<ActivityError.InvalidInput>(assertIs<Result.Error<*>>(save(key, record())).error)
        coEvery { repository.delete(10, 30, 3, "a".repeat(32)) } returns SprayWriteOutcome.SAVED
        assertIs<Result.Success<Unit>>(delete(key, 3, "a".repeat(32)))
        coVerify(exactly = 1) { repository.delete(10, 30, 3, "a".repeat(32)) }
    }
    @Test fun storageErrorIsTypedAndCancellationPropagates() = runTest {
        owned()
        coEvery { repository.save(10, any()) } throws IllegalStateException("storage failure")
        assertIs<UnexpectedError>(assertIs<Result.Error<*>>(save(key, record())).error)
        coEvery { repository.save(10, any()) } throws CancellationException("cancelled")
        assertFailsWith<CancellationException> { save(key, record()) }
    }
}

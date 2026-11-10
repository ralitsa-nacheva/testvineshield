package com.rncoding.testvineshield.core.presentation.spray

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SprayRateViewModelTest {
    private val get = mockk<GetSprayRecordUseCase>()
    private val save = mockk<SaveSprayRecordUseCase>()
    private val delete = mockk<DeleteSprayRecordUseCase>()
    private val key = SprayKey(10, 30)
    private val record = SprayRecord(30, LocalDate(2026, 11, 10), null, "UTC", null, null, null, null, null, 2,
        listOf(SprayProductLine(5, "A", 0.5, SprayQuantityUnit.KILOGRAM, rate = SprayRate(2.0, SprayQuantityUnit.KILOGRAM, 1.0, SprayRateBasis.HECTARE))), "a".repeat(32))
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        coEvery { get(key) } returns Result.Success(SpraySnapshot(mockk<ActivityDomainModel>(), "UTC", record))
        coEvery { save(key, any()) } returns Result.Success(Unit)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = SprayViewModel(SprayRequest(key), get, save, delete).also { store.put("spray", it) }
    @Test fun clearRateRetainsActualQuantityAndBaseVersion() = runTest {
        val v = vm(); advanceUntilIdle(); v.edit(); v.clearRate(0); v.save(); advanceUntilIdle()
        coVerify(exactly = 1) { save(key, record.copy(products = record.products.map { it.copy(rate = null) })) }
    }
    @Test fun unitOnlyChangeKeepsBothNumericAmountsAndLineIdentity() = runTest {
        val v = vm(); advanceUntilIdle(); v.edit(); v.lineRateBasis(0, SprayRateBasis.ACRE); v.save(); advanceUntilIdle()
        coVerify(exactly = 1) { save(key, match { it.products.single().lineId == 5L && it.products.single().rate == SprayRate(2.0, SprayQuantityUnit.KILOGRAM, 1.0, SprayRateBasis.ACRE) }) }
        assertTrue(v.state.value.saved)
    }
}

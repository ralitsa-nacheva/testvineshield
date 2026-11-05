package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VineyardEditorUnitTest {
    private val dispatcher = StandardTestDispatcher()
    private val create = mockk<CreateVineyardUseCase>()
    private val update = mockk<UpdateVineyardUseCase>()
    private val get = mockk<GetVineyardUseCase>()
    private val date = LocalDate(2026, 11, 5)
    private val vineyard = VineyardDomainModel(10, 1, "Vineyard", 1.25, "BG", "Plovdiv", 42.1, 24.7,
        "Europe/Sofia", 200, 0, date, date, AreaUnit.HECTARE)
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun editingUnrelatedFieldRetainsSavedUnitAndNumber(): Unit = runTest(dispatcher) {
        coEvery { get(10) } returns Result.Success(vineyard)
        coEvery { update(10, any()) } returns Result.Success(Unit)
        val vm = VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get)
        runCurrent()
        assertEquals(AreaUnit.HECTARE, vm.state.value.sizeUnit)
        vm.onNameChange("Renamed"); vm.save(); runCurrent()
        coVerify { update(10, match { it.sizeUnit == AreaUnit.HECTARE && it.size == 1.25 }) }
    }
    @Test fun changingUnitDoesNotSilentlyConvertTheEnteredValue(): Unit = runTest(dispatcher) {
        coEvery { get(10) } returns Result.Success(vineyard)
        coEvery { update(10, any()) } returns Result.Success(Unit)
        val vm = VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get)
        runCurrent(); vm.onSizeUnitChange(AreaUnit.ACRE); vm.save(); runCurrent()
        coVerify { update(10, match { it.sizeUnit == AreaUnit.ACRE && it.size == 1.25 }) }
    }
    @Test fun migratedUnspecifiedUnitIsNotDefaultedToHectares(): Unit = runTest(dispatcher) {
        coEvery { get(10) } returns Result.Success(vineyard.copy(sizeUnit = null))
        coEvery { update(10, any()) } returns Result.Success(Unit)
        val vm = VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get)
        runCurrent(); assertNull(vm.state.value.sizeUnit)
        vm.save(); runCurrent()
        coVerify { update(10, match { it.sizeUnit == null && it.size == 1.25 }) }
    }
    @Test fun userCanExplicitlyClearTheUnitWithoutChangingTheNumber(): Unit = runTest(dispatcher) {
        coEvery { get(10) } returns Result.Success(vineyard)
        coEvery { update(10, any()) } returns Result.Success(Unit)
        val vm = VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get)
        runCurrent(); vm.onSizeUnitChange(null); vm.save(); runCurrent()
        coVerify { update(10, match { it.sizeUnit == null && it.size == 1.25 }) }
    }

}

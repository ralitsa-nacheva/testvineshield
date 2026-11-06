package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VineyardLocationEditorTest {
    private val dispatcher = StandardTestDispatcher()
    private val create = mockk<CreateVineyardUseCase>()
    private val update = mockk<UpdateVineyardUseCase>()
    private val get = mockk<GetVineyardUseCase>()
    private val date = LocalDate(2026, 11, 6)
    private val vineyard = VineyardDomainModel(10, 1, "Vineyard", 1.25, "BG", "Plovdiv", 42.1, 24.7,
        "Europe/Sofia", 123.45, 0, date, date)
    private val catalog = VineyardTimeZoneCatalog(setOf("Europe/Sofia", "Europe/Athens"))
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }
    private fun editor(value: VineyardDomainModel = vineyard): VineyardEditorViewModel {
        coEvery { get(10) } returns Result.Success(value)
        coEvery { update(10, any()) } returns Result.Success(Unit)
        return VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get, catalog)
    }
    @Test fun loadsAndSavesFractionWithoutRounding(): Unit = runTest(dispatcher) {
        val vm = editor(); runCurrent()
        assertEquals("123.45", vm.state.value.elevation)
        vm.save(); runCurrent()
        coVerify { update(10, match { it.elevation == 123.45 }) }
    }
    @Test fun commaAndNegativeElevationReachUseCase(): Unit = runTest(dispatcher) {
        val vm = editor(); runCurrent(); vm.onElevationChange("-12,5"); vm.save(); runCurrent()
        coVerify { update(10, match { it.elevation == -12.5 }) }
    }
    @Test fun blankElevationPersistsAsNull(): Unit = runTest(dispatcher) {
        val vm = editor(); runCurrent(); vm.onElevationChange(""); vm.save(); runCurrent()
        coVerify { update(10, match { it.elevation == null }) }
    }
    @Test fun unknownLoadsAsBlankAndInvalidTextNeverSaves(): Unit = runTest(dispatcher) {
        val vm = editor(vineyard.copy(elevation = null)); runCurrent()
        assertEquals("", vm.state.value.elevation)
        vm.onElevationChange("1,234.5"); vm.save(); runCurrent()
        assertNotNull(vm.state.value.elevationError)
        coVerify(exactly = 0) { update(any(), any()) }
    }
    @Test fun zoneSelectionStoresOnlyOfferedId(): Unit = runTest(dispatcher) {
        val vm = editor(); runCurrent()
        vm.onTimeZoneChange("search text"); assertEquals("Europe/Sofia", vm.state.value.timeZone)
        vm.onTimeZoneChange("Europe/Athens"); vm.save(); runCurrent()
        coVerify { update(10, match { it.timeZone == "Europe/Athens" }) }
    }
    @Test fun existingZoneIsNotOverwrittenByCatalogOrOtherEdit(): Unit = runTest(dispatcher) {
        val vm = editor(vineyard.copy(timeZone = "America/New_York")); runCurrent()
        vm.onNameChange("Renamed"); vm.save(); runCurrent()
        coVerify { update(10, match { it.timeZone == "America/New_York" }) }
    }
    @Test fun loadingAndSavingBlockZoneAndElevationChanges(): Unit = runTest(dispatcher) {
        val vm = editor()
        vm.onTimeZoneChange("Europe/Athens"); vm.onElevationChange("55.5")
        assertEquals("", vm.state.value.timeZone); assertEquals("", vm.state.value.elevation)
        runCurrent()
        coEvery { update(10, any()) } coAnswers { delay(100); Result.Success(Unit) }
        vm.save(); runCurrent()
        vm.onTimeZoneChange("Europe/Athens"); vm.onElevationChange("55.5")
        assertEquals("Europe/Sofia", vm.state.value.timeZone); assertEquals("123.45", vm.state.value.elevation)
        advanceTimeBy(100); runCurrent()
    }
    @Test fun tinySavedFractionCanBeSavedWithoutEditing(): Unit = runTest(dispatcher) {
        val vm = editor(vineyard.copy(elevation = 0.0000001)); runCurrent()
        assertFalse(vm.state.value.elevation.contains('E', ignoreCase = true))
        vm.save(); runCurrent()
        coVerify { update(10, match { it.elevation == 0.0000001 }) }
    }

}

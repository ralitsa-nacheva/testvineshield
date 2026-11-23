package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VineyardEditorErrorTest {
    private val create = mockk<CreateVineyardUseCase>()
    private val update = mockk<UpdateVineyardUseCase>()
    private val get = mockk<GetVineyardUseCase>()
    private val day = LocalDate(2026, 11, 23)
    private val vineyard = VineyardDomainModel(10, 1, "Test", 1.0, "BG", "Plovdiv", 42.1, 24.7,
        "Europe/Sofia", null, 0, day, day)
    private lateinit var store: ViewModelStore
    private lateinit var dispatcher: TestDispatcher
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        coEvery { get(10) } returns Result.Success(vineyard)
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = VineyardEditorViewModel(VineyardEditorMode.Edit(10), create, update, get,
        VineyardTimeZoneCatalog(setOf("Europe/Sofia"))).also { store.put("editor", it) }

    @Test fun typedLoadFailureKeepsEditorDisabled() = runTest {
        val failure = DatabaseError.NotFound("Vineyard")
        coEvery { get(10) } returns Result.Error(failure)
        val vm = vm(); runCurrent()
        assertSame(failure, vm.state.value.loadError)
        assertFalse(vm.state.value.isLoading); assertFalse(vm.state.value.canEdit)
    }
    @Test fun numberParseFailureHasReasonAndNeverCallsSave() = runTest {
        val vm = vm(); runCurrent(); vm.onSizeChange("invalid"); vm.save(); runCurrent()
        assertEquals(VineyardValidationReason.SizeNumber, vm.state.value.sizeError?.reason)
        assertFalse(vm.state.value.isSaving)
        coVerify(exactly = 0) { update(any(), any()) }
    }
    @Test fun domainFieldErrorRetainsIdentityAndEditClearsIt() = runTest {
        val failure = VineyardValidationError(VineyardField.NAME, "Safe fallback", reason = VineyardValidationReason.NameRequired)
        coEvery { update(10, any()) } returns Result.Error(failure)
        val vm = vm(); runCurrent(); vm.save(); runCurrent()
        assertSame(failure, vm.state.value.nameError); assertFalse(vm.state.value.isSaving)
        vm.onNameChange("Changed"); assertNull(vm.state.value.nameError)
    }
    @Test fun nonValidationSaveFailureRemainsTypedAndKeepsDraft() = runTest {
        val failure = DatabaseError.Unknown(DatabaseError.Operation.UPDATE, IllegalStateException("private diagnostic"))
        coEvery { update(10, any()) } returns Result.Error(failure)
        val vm = vm(); runCurrent(); vm.onNameChange("Changed"); vm.save(); runCurrent()
        assertSame(failure, assertIs<VineyardEditorEvent.ShowSnackbar>(vm.events.first()).error)
        assertEquals("Changed", vm.state.value.name); assertFalse(vm.state.value.isSaving)
        assertTrue(vm.state.value.canEdit)
    }
}

package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.vineyard.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*
@OptIn(ExperimentalCoroutinesApi::class)
class VineyardMapDraftTest {
    private val create=mockk<CreateVineyardUseCase>(); private val update=mockk<UpdateVineyardUseCase>(); private val get=mockk<GetVineyardUseCase>()
    private lateinit var dispatcher: TestDispatcher; private lateinit var store: ViewModelStore
    @Before fun before() { dispatcher=StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store=ViewModelStore() }
    @After fun after() { try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() } finally { Dispatchers.resetMain() } }
    private fun vm(mode: VineyardEditorMode=VineyardEditorMode.Create)=VineyardEditorViewModel(mode,create,update,get).also { store.put("editor",it) }
    @Test fun usePointChangesDraftAtomicallyWithoutSavingOrChangingOtherFields() = runTest {
        val v=vm(); v.onNameChange("North"); v.onCountryChange("Bulgaria"); v.onCityChange("Plovdiv"); v.onTimeZoneChange("Europe/Sofia")
        v.applyMapPoint(assertNotNull(VineyardMapPoint.from(-33.9,-70.7)),null)
        assertEquals(-33.9,v.state.value.latitude.toDouble()); assertEquals(-70.7,v.state.value.longitude.toDouble(),0.000000001)
        assertEquals("North",v.state.value.name); assertEquals("Bulgaria",v.state.value.country); assertEquals("Plovdiv",v.state.value.city)
        assertEquals("Europe/Sofia",v.state.value.timeZone); verify { create wasNot Called; update wasNot Called }
    }
    @Test fun explicitValidZoneIsAppliedButUnsupportedZoneIsNot() = runTest {
        val v=vm(); val p=assertNotNull(VineyardMapPoint.from(42.7,23.3))
        v.applyMapPoint(p,"Europe/Sofia"); assertEquals("Europe/Sofia",v.state.value.timeZone)
        v.applyMapPoint(p,"Invalid/Zone"); assertEquals("Europe/Sofia",v.state.value.timeZone)
    }
    @Test fun loadingEditorCannotAcceptMapChanges() = runTest {
        coEvery { get(10) } coAnswers { awaitCancellation() }
        val v=vm(VineyardEditorMode.Edit(10)); v.applyMapPoint(assertNotNull(VineyardMapPoint.from(42.7,23.3)),"Europe/Sofia")
        assertEquals("",v.state.value.latitude); assertTrue(v.state.value.isLoading)
    }
    @Test fun savingEditorCannotAcceptMapChanges() = runTest {
        coEvery { create(any()) } coAnswers { awaitCancellation() }
        val v=vm(); v.onNameChange("North"); v.onSizeChange("1"); v.onCountryChange("BG"); v.onCityChange("Sofia")
        v.onLatitudeChange("42.7"); v.onLongitudeChange("23.3"); v.onTimeZoneChange("Europe/Sofia")
        v.save(); runCurrent(); assertTrue(v.state.value.isSaving)
        v.applyMapPoint(assertNotNull(VineyardMapPoint.from(-33.9,-70.7)),null)
        assertEquals("42.7",v.state.value.latitude); assertEquals("23.3",v.state.value.longitude)
    }

}

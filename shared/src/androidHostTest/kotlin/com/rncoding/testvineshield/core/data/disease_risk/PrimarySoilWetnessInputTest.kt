package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class PrimarySoilWetnessInputTest {
    @Test
    fun enabledPrimaryConfigurationSkipsBeforeReadingAnyWeatherOrHostEvidence() = runTest {
        val monitoring = mockk<DiseaseRiskMonitoringConfigurationRepository>()
        val vineyard = mockk<DiseaseRiskVineyardContextProvider>()
        val windows = mockk<DiseaseRiskWeatherWindowPolicy>()
        val weather = mockk<DiseaseRiskWeatherProvider>()
        val host = mockk<DiseaseRiskHostContextProvider>()
        val model = DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY
        coEvery { monitoring.getConfiguration(10, 20, 30, model) } returns
            DiseaseRiskMonitoringConfiguration(10, 20, 30, model, LocalDate(2026, 6, 15))

        val factory = DiseaseRiskInputFactoryImpl(monitoring, vineyard, windows, weather, host)
        val result = factory.createInput(model, 30, 10, 20, 1_800_000_000_000L, null)

        assertEquals(DiseaseRiskInputResult.SoilWetnessEvidenceUnavailable, result)
        verify {
            vineyard wasNot Called
            windows wasNot Called
            weather wasNot Called
            host wasNot Called
        }
    }
}

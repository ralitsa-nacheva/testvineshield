package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

data class DownyMildewPrimaryInput(
    val weather: List<DiseaseRiskWeatherPoint>,
    val soilWetnessEvidence: SoilWetnessEvidence,
    val previousState: DownyMildewPrimaryState =
        DownyMildewPrimaryState()
)
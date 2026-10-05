package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

data class DownyMildewSecondaryInput(
    val weather: List<DiseaseRiskWeatherPoint>,
    val activeOilSpotsObserved: Boolean
)
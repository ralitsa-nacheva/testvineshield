package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

data class PowderyMildewAscosporeInput(
    val weather: List<DiseaseRiskWeatherPoint>
)
package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

data class BotrytisRiskInput(
    val weather: List<DiseaseRiskWeatherPoint>,
    val hostTissue: BotrytisHostTissue
)
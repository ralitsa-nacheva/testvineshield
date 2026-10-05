package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate

data class DiseaseRiskDailyWeather(
    val date: LocalDate,
    val meanLeafWetnessProbabilityPercent: Double?
)
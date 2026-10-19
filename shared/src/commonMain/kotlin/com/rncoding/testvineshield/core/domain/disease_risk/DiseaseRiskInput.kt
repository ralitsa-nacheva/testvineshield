package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate

data class DiseaseRiskInput(
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long,
    val timeZone: String,
    val hourlyWeather: List<DiseaseRiskWeatherPoint>,
    val dailyWeather: List<DiseaseRiskDailyWeather>,
    val hostContext: DiseaseRiskHostContext,
    val monitoringStartDate: LocalDate? = null
)